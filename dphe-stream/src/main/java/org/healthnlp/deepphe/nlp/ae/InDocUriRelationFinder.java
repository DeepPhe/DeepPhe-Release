package org.healthnlp.deepphe.nlp.ae;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.core.document.SectionType;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.relation.RelationUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.Neo4jRelationUtil;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ctakes.typesystem.type.constants.CONST.NE_DISCOVERY_TECH_EXPLICIT_AE;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/27/2017
 */
final public class InDocUriRelationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "InDocUriRelationFinder" );


   static private final String SITE_MIN = "SiteMinimum";
   static private final String SITE_MIN_DESC = "Minimum number of anatomical sites that must be related to be valid.";

   @ConfigurationParameter(
         name = SITE_MIN,
         description = SITE_MIN_DESC,
         mandatory = false,
         defaultValue = "2"
   )
   private int _siteMin = 2;

   static private final String DEVIATION_DIV = "DeviationDivider";
   static private final String DEVIATION_DIV_DESC
         = "Number by which to divide the standard deviation for more lenient site acceptance.";

   @ConfigurationParameter(
         name = DEVIATION_DIV,
         description = DEVIATION_DIV_DESC,
         mandatory = false,
         defaultValue = "2"
   )
   private int _devDiv = 2;

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );

//   static private Collection<String> QUADRANT_URIS;


   static private final Object LOCK = new Object();
   // 1 Hour
   static private final long TIMEOUT = 1000 * 60 * 60;

   static private final long PERIOD = TIMEOUT / 4;
   static private final long START = TIMEOUT + PERIOD;

   private final Map<String, Collection<String>> _uriBranchMap = new ConcurrentHashMap<>();
   private final Map<String, Long> _timeMap = new ConcurrentHashMap<>();
   private final Map<String, Map<String, Collection<String>>> _uriRelationMap = new ConcurrentHashMap<>();
   private ScheduledExecutorService _cacheCleaner;


   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _cacheCleaner = Executors.newScheduledThreadPool( 1 );
      _cacheCleaner.scheduleAtFixedRate( new CacheCleaner(), START, PERIOD, TimeUnit.MILLISECONDS );
//      if ( QUADRANT_URIS == null ) {
//         QUADRANT_URIS = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT ) );
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Nipple" ) );
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Areola" ) );
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Central_Portion_Of_The_Breast" ) );
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Subareolar_Region" ) );
//      }
   }

   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      _cacheCleaner.shutdown();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {

      // Map of paragraphs in each section.
      final Map<Segment, Collection<Paragraph>> sectionParagraphMap
            = new HashMap<>( JCasUtil.indexCovered( jCas, Segment.class, Paragraph.class ) );

      // Map of all annotations per paragraph.
      final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap
            = new HashMap<>( JCasUtil.indexCovered( jCas, Paragraph.class, IdentifiedAnnotation.class ) );

      // Clean up the sections and paragraphs here to prevent unnecessary processing.
      final Collection<Segment> removalSections = new HashSet<>();
      for ( Map.Entry<Segment, Collection<Paragraph>> sectionParagraphs : sectionParagraphMap.entrySet() ) {
         if ( isUnwantedSection( sectionParagraphs.getKey() ) ) {
            removalSections.add( sectionParagraphs.getKey() );
            paragraphAnnotationMap.keySet().removeAll( sectionParagraphs.getValue() );
         }
      }
      sectionParagraphMap.keySet().removeAll( removalSections );

      // All unique URIs for all annotations in the entire document.
      final Collection<String> allUris = getAllUris( paragraphAnnotationMap );
      if ( allUris.size() < 2 ) {
         return;
      }
      // Map of source URI to Map of Relation name to MOST SPECIFIC target URIs.  e.g. brca only to breast as location.
      final Map<String, Map<String, Collection<String>>> sourceToRelationNamesTargetsMap
            = mapSourceToRelationNamesTargets( allUris );

      // Map of Map of  Map of Relation name to target URIs to collection of source URIs.
      // MOST SPECIFIC relation target uris.  e.g. (location, [breast]),[brca, dcis] ;
      final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources
            = mapRelationToSources( sourceToRelationNamesTargetsMap );

      // Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
      final Map<String,Collection<String>> branchMap = createBranchMap( allUris, relationNameTargetsToSources );

      // Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
//      annotations and uris
      final Map<Paragraph,Map<String,Collection<IdentifiedAnnotation>>> paragraphUriAnnotationsMap = new HashMap<>();
      for ( Map.Entry<Paragraph,Collection<IdentifiedAnnotation>> paragraphAnnotations : paragraphAnnotationMap.entrySet() ) {
         final Collection<IdentifiedAnnotation> annotations = paragraphAnnotations.getValue();
         if ( annotations == null || annotations.isEmpty() ) {
            continue;
         }
         paragraphUriAnnotationsMap.put( paragraphAnnotations.getKey(),
               Neo4jOntologyConceptUtil.mapUriAnnotations( annotations ) );
      }

      // Process the document with all the generated Maps.
      processDoc( jCas,
                  sourceToRelationNamesTargetsMap,
                  relationNameTargetsToSources,
                  sectionParagraphMap,
            branchMap,
            paragraphUriAnnotationsMap );
   }

   /**
    * Process entire document,
    * @param jCas                         ye olde ...
    * @param sourceToRelationNamesTargetsMap Map of source URI to Map of Relation name to MOST SPECIFIC target URIs.
    *                                        e.g. brca only to breast as location.
    * @param relationNameTargetsToSources Map of Map of Map of Relation name to target URIs to set of source URIs.
    *                                     MOST SPECIFIC relation target uris.  e.g. (location, [breast]),[brca, dcis]
    * @param sectionParagraphMap Map of paragraphs in each section.
//    * @param paragraphAnnotationMap Map of all annotations per paragraph.
    */
   static public void processDoc( final JCas jCas,
                                  final Map<String, Map<String, Collection<String>>> sourceToRelationNamesTargetsMap,
                                  final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
                                  final Map<Segment, Collection<Paragraph>> sectionParagraphMap,
                                  final Map<String, Collection<String>> branchMap,
                                  final Map<Paragraph,Map<String,Collection<IdentifiedAnnotation>>> paragraphUriAnnotationsMap ) {

      // Make locations first because in this step annotations can be duplicated.
      final Collection<BinaryTextRelation> locations =
            processDocLocations( jCas,
                                 relationNameTargetsToSources,
                                 sectionParagraphMap,
                  branchMap,
                  paragraphUriAnnotationsMap );

      // Creating location relations may have required creating new annotations for things like " and ".
      //  TODO - make lateralities was moved to processDocRelations.  This probably needs to be moved.
      final Map<Paragraph, Collection<IdentifiedAnnotation>> newParagraphAnnotationMap
            = new HashMap<>( JCasUtil.indexCovered( jCas, Paragraph.class, IdentifiedAnnotation.class ) );
      newParagraphAnnotationMap.keySet().retainAll( paragraphUriAnnotationsMap.keySet() );

      // Make all relations that are not locations.
      processDocRelations( jCas,
                           sourceToRelationNamesTargetsMap,
                           relationNameTargetsToSources,
                           sectionParagraphMap,
                           newParagraphAnnotationMap,
                           branchMap,
                           paragraphUriAnnotationsMap,
                           locations );
   }



   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                                     LOCATIONS
   //
   //////////////////////////////////////////////////////////////////////////////////////////////


   /**
    *
    * @param jCas                         ye olde ...
    * @param relationNameTargetsToSources Map of Map of  Map of Relation name to target URIs to collection of source URIs.
    * @param sectionParagraphMap Map of paragraphs in each section.
//    * @param paragraphAnnotationMap Map of all annotations per paragraph.
    * @param branchMap                    Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
    * @return Collection of created Location Relations.
    */
   static private Collection<BinaryTextRelation> processDocLocations( final JCas jCas,
                                            final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
                                            final Map<Segment, Collection<Paragraph>> sectionParagraphMap,
                                           final Map<String, Collection<String>> branchMap,
                                           final Map<Paragraph,Map<String,Collection<IdentifiedAnnotation>>> paragraphUriAnnotationsMap ) {
      LOGGER.info( "Finding Locations ..." );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      final Collection<String> allLocationUris = new HashSet<>( UriConstants.getLocationUris( graphDb ) );
//      allLocationUris.removeAll( QUADRANT_URIS );
      final Collection<BinaryTextRelation> locations = new HashSet<>();
      final Map<IdentifiedAnnotation, Collection<String>> relationsDone = new HashMap<>();

      for ( Map.Entry<Segment, Collection<Paragraph>> sectionParagraphs : sectionParagraphMap.entrySet() ) {

         final Collection<Paragraph> paragraphs = sectionParagraphs.getValue();
         for ( Paragraph paragraph : paragraphs ) {
            final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap
            = paragraphUriAnnotationsMap.get( paragraph );
            if ( uriAnnotationMap != null && uriAnnotationMap.size() > 1 ) {
               // Check to see if there are no locations OR there are only locations
               final Collection<String> locationUris = new HashSet<>( uriAnnotationMap.keySet() );
               locationUris.retainAll( allLocationUris );
               if ( !locationUris.isEmpty() && locationUris.size() != uriAnnotationMap.size() ) {
                  locations.addAll(
                        createWindowLocations( jCas,
                              uriAnnotationMap,
                              relationNameTargetsToSources,
                              branchMap,
                              relationsDone ) );
               }
            }
         }
         // We now have locations within paragraphs.  Try locations within the section for any leftovers.
//         createWindowLocations( docCas, sectionUriAnnotationMap, relationNameTargetsToSources, relationsDone );
      }
      return locations;
   }


   /**
    * @param jCas                         ye olde ...
    * @param uriAnnotationMap             Map of URI to all paragraph Annotations that have the URI.
    * @param relationNameTargetsToSources Map of Map of  Map of Relation name to target URIs to collection of source URIs.
    * @param branchMap                    Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
    * @param relationsDone                annotations and relations already completed for those annotations.
    * @return Collection of created Location Relations.
    */
   static private Collection<BinaryTextRelation> createWindowLocations( final JCas jCas,
                                                                        final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                                                        final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
                                                                        final Map<String, Collection<String>> branchMap,
                                                                        final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
      if ( uriAnnotationMap.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<BinaryTextRelation> locations = new HashSet<>();
      for ( Map.Entry<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSourcesEntry
            : relationNameTargetsToSources.entrySet() ) {
         final Map<String, Collection<String>> relationNameTargetsMap = relationNameTargetsToSourcesEntry.getKey();
         // collatedSources ...  This should be ok as each set of sources is unique to relation:target entry key
         // Is this necessary?
         final Map<String, Collection<String>> collatedSourceUris
               = collateUris( relationNameTargetsToSourcesEntry.getValue(), branchMap );
         for ( Collection<String> alikeSourceUris : collatedSourceUris.values() ) {
            locations.addAll(
                  createWindowLocations( jCas,
                        alikeSourceUris,
                        uriAnnotationMap,
                        relationNameTargetsMap,
                        branchMap,
                        relationsDone ) );
         }
      }
      return locations;
   }


   /**
    *
    * TODO
    * TODO        First check for within-sentence "[d/d] in the [site]" "[d/d] on the [site]" and assign location of.
    * TODO
    *
    *
    * @param jCas ye olde ...
    * @param alikeSourceUris Collection of all source URIs.
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI.
    * @param relationNameTargetsMap Map of Relation name to target URIs.
    * @param branchMap Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
    * @param relationsDone annotations and relations already completed for those annotations.
    * @return Collection of created Location Relations.
    */
   static private Collection<BinaryTextRelation> createWindowLocations( final JCas jCas,
                                                                        final Collection<String> alikeSourceUris,
                                                                        final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                                                        final Map<String, Collection<String>> relationNameTargetsMap,
                                                                        final Map<String, Collection<String>> branchMap,
                                                                        final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      // getRelatableUris(..) does not handle all site Uris, so handle specifically
      final Collection<String> allSiteUris = new HashSet<>( uriAnnotationMap.keySet() );
      allSiteUris.retainAll( UriConstants.getLocationUris( graphDb ) );

      if ( allSiteUris.isEmpty() ) {
         return Collections.emptyList();
      }

      final String docText = jCas.getDocumentText();
      final Collection<BinaryTextRelation> locations = new HashSet<>();
      final Map<String, Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>>> relationLocationsMap
            = new HashMap<>();
      for ( Map.Entry<String, Collection<String>> relationNameTargets : relationNameTargetsMap.entrySet() ) {
         final String relationName = relationNameTargets.getKey();
         if ( !RelationConstants.isHasSiteRelation( relationName ) ) {
            continue;
         }
         // getValue is a list.  We only want to resolve unique uris.
         final Collection<String> relationTargetUris = new HashSet<>( relationNameTargets.getValue() );
         final Collection<String> targetUris
               = relationTargetUris.stream()
                                   .map( branchMap::get )
                                   .flatMap( Collection::stream )
                                   .collect( Collectors.toSet() );
         targetUris.retainAll( allSiteUris );

         if ( targetUris.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> sourceList
               = getSourceAnnotations( relationName, alikeSourceUris, uriAnnotationMap, relationsDone );
         if ( sourceList.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> siteList
               = getTargetAnnotations( targetUris, uriAnnotationMap )
               .stream()
               .filter( a -> isSiteActual( docText, a ) )
               .collect( Collectors.toList() );
         relationLocationsMap.put( relationName, mapObjectSites( jCas, sourceList, siteList ) );
      }
      cleanLocations( relationLocationsMap );
      for ( Map.Entry<String, Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>>> relationLocations : relationLocationsMap
            .entrySet() ) {
         final String relationName = relationLocations.getKey();
         final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargetMap
               = relationLocations.getValue();
         locations.addAll( createLocations( jCas, relationName, sourceTargetMap ) );
         sourceTargetMap.keySet().forEach(
               a -> relationsDone.computeIfAbsent( a, r -> new HashSet<>() ).add( relationName ) );
      }
      return locations;
   }


   /**
    *
    * @param jCas ye olde ...
    * @param sitables source annotations that can have Location Relations.   --> has location.
    * @param sites target annotations that can be in Location Relations.  --> sites.
    * @return Map of ( targets to possible sources ?  Double-Check ).
    */
   static private Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> mapObjectSites( final JCas jCas,
                                                                                              final List<IdentifiedAnnotation> sitables,
                                                                                              final List<IdentifiedAnnotation> sites ) {
      if ( sitables.isEmpty() || sites.isEmpty() ) {
         return Collections.emptyMap();
      }
//      return new HashMap<>( RelationUtil.createReverseAttributeMap( jCas, sources, targets, true ) );
      return new HashMap<>( mapSitableSites( jCas, sitables, sites, true ) );
   }

   
   /**
    *
    * @param jCas ye olde ...
    * @param relationName name of Location Relation to create.
    * @param sourceTargetMap map of Relation Sources to all of their related Targets
    * @return Created location relations.
    */
   static private Collection<BinaryTextRelation> createLocations( final JCas jCas,
                                                                  final String relationName,
                                                                  final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargetMap ) {
      final Collection<BinaryTextRelation> locations = new HashSet<>();
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : sourceTargetMap.entrySet() ) {
         final IdentifiedAnnotation owner = entry.getKey();
         for ( IdentifiedAnnotation location : entry.getValue() ) {
            locations.add( RelationUtil.createRelation( jCas, owner, location, relationName ) );
         }
      }
      return locations;
   }

   




   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                                     RELATIONS
   //
   //////////////////////////////////////////////////////////////////////////////////////////////


   /**
    *
    * @param jCas ye olde ...
    * @param sourceToRelationNamesTargetsMap Map of source URI to Map of Relation name to MOST SPECIFIC target URIs.
    *                                        e.g. brca only to breast as location.
    * @param relationNameTargetsToSources Map of Map of  Map of Relation name to target URIs to collection of source URIs.
    * @param sectionParagraphMap Map of paragraphs in each section.
    * @param paragraphAnnotationMap Map of all annotations per paragraph.
    * @param branchMap Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
    * @param locations Collection of created Location Relations.
    */
   static private void processDocRelations( final JCas jCas,
                                            final Map<String, Map<String, Collection<String>>> sourceToRelationNamesTargetsMap,
                                            final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
                                            final Map<Segment, Collection<Paragraph>> sectionParagraphMap,
                                            final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap,
                                            final Map<String, Collection<String>> branchMap,
                                            final Map<Paragraph,Map<String,Collection<IdentifiedAnnotation>>> paragraphUriAnnotationsMap,
                                            final Collection<BinaryTextRelation> locations ) {
      final Map<IdentifiedAnnotation, Collection<String>> relationsDone = new HashMap<>();
      LOGGER.info( "Finding Relations ..." );

      for ( Map.Entry<Segment, Collection<Paragraph>> sectionParagraphs : sectionParagraphMap.entrySet() ) {
         final Map<String, Collection<IdentifiedAnnotation>> sectionUriAnnotationMap = new HashMap<>();

         final Collection<Paragraph> paragraphs = sectionParagraphs.getValue();
         for ( Paragraph paragraph : paragraphs ) {
            final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap
               = paragraphUriAnnotationsMap.get( paragraph );
            if ( uriAnnotationMap == null || uriAnnotationMap.isEmpty() ) {
               continue;
            }
            final Collection<IdentifiedAnnotation> additionalAnnotations = new HashSet<>( paragraphAnnotationMap.get( paragraph ) );
            additionalAnnotations.removeAll( uriAnnotationMap.values().stream()
                                                             .flatMap( Collection::stream )
                                                             .collect( Collectors.toSet() ) );
            for ( IdentifiedAnnotation additional : additionalAnnotations ) {
               uriAnnotationMap.computeIfAbsent( Neo4jOntologyConceptUtil.getUri( additional ), s -> new HashSet<>() )
                               .add( additional );
            }

            createWindowRelations( jCas, uriAnnotationMap, relationNameTargetsToSources, branchMap, relationsDone );
            createWindowLateralities( jCas, uriAnnotationMap, sourceToRelationNamesTargetsMap, relationsDone, locations );

            for ( Map.Entry<String, Collection<IdentifiedAnnotation>> uriAnnotations : uriAnnotationMap.entrySet() ) {
               sectionUriAnnotationMap.computeIfAbsent( uriAnnotations.getKey(),
                     a -> new HashSet<>() ).addAll( uriAnnotations.getValue() );
            }
         }
         // We now have relations within paragraphs.  Try relations within the section for any leftovers.
         createWindowLateralities( jCas, sectionUriAnnotationMap, sourceToRelationNamesTargetsMap, relationsDone, locations );
      }
   }


   /**
    *
    * @param jCas ye olde ...
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI.
    * @param relationNameTargetsToSources Map of Map of  Map of Relation name to target URIs to collection of source URIs.
    * @param branchMap Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
    * @param relationsDone annotations and relations already completed for those annotations.
    */
   static private void createWindowRelations( final JCas jCas,
                                              final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                              final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources,
                                              final Map<String, Collection<String>> branchMap,
                                              final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
      if ( uriAnnotationMap.isEmpty() ) {
         return;
      }
      for ( Map.Entry<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSourcesEntry
            : relationNameTargetsToSources.entrySet() ) {
         final Map<String, Collection<String>> relationNameTargetsMap = relationNameTargetsToSourcesEntry.getKey();
         // collatedSources ...  This should be ok as each set of sources is unique to relation:target entry key
         // Is this necessary?
         final Map<String, Collection<String>> collatedSourceUris
//               = collateUris( relationNameTargetsToSourcesEntry.getValue() );
               = collateUris( relationNameTargetsToSourcesEntry.getValue(), branchMap );
         for ( Collection<String> sourceUris : collatedSourceUris.values() ) {
            createWindowRelations( jCas, sourceUris, uriAnnotationMap, relationNameTargetsMap, branchMap, relationsDone );
         }
      }
   }


   /**
    *
    * @param jCas ye olde ...
    * @param alikeSourceUris Collection of all source URIs.
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI.
    * @param relationNameTargetsMap Map of Relation name to target URIs.
    * @param branchMap Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
    * @param relationsDone annotations and relations already completed for those annotations.
    */
   static private void createWindowRelations( final JCas jCas,
                                              final Collection<String> alikeSourceUris,
                                              final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                              final Map<String, Collection<String>> relationNameTargetsMap,
                                              final Map<String, Collection<String>> branchMap,
                                              final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
      for ( Map.Entry<String, Collection<String>> relationNameTargets : relationNameTargetsMap.entrySet() ) {
         final String relationName = relationNameTargets.getKey();
         if ( RelationConstants.isHasSiteRelation( relationName )
              || relationName.equals( HAS_LATERALITY ) ) {
            continue;
         }
         final Collection<String> relationTargetUris = relationNameTargets.getValue();

         final Collection<String> targetUris
         = relationTargetUris.stream()
                             .map( branchMap::get )
                             .flatMap( Collection::stream )
                             .collect( Collectors.toSet() );
         targetUris.retainAll( uriAnnotationMap.keySet() );

         if ( targetUris.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> sourceList
               = getSourceAnnotations( relationName, alikeSourceUris, uriAnnotationMap, relationsDone );
         if ( sourceList.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> targetList = getTargetAnnotations( targetUris, uriAnnotationMap );
         if ( targetList.isEmpty() ) {
            continue;
         }
         createRelations( jCas, relationName, sourceList, targetList );
         sourceList.forEach( a -> relationsDone.computeIfAbsent( a, r -> new HashSet<>() ).add( relationName ) );
      }
   }






   /**
    * TODO move to a utility class
    * Given any collection of uris, collates and returns those uris in related branches.
    * cancer, brca, dcis, cap   =  cancer, [cancer, brca, dcis, cap]
    * brca, dcis, cap  =  brca, [brca, dcis] ; cap, [cap]
    *
    * @param allUris some collection of uris
    * @return a map of branch root uris and the collection of child uris under that branch root
    */
   static public Map<String, Collection<String>> collateUris( final Collection<String> allUris,
                                                              final Map<String,Collection<String>> branchMap ) {
      final Map<String, Collection<String>> uriBranches
         = new HashMap<>( branchMap );
      uriBranches.keySet().retainAll( allUris );
      final Map<String, String> uriBestRootMap = new HashMap<>( uriBranches.size() );
      for ( Map.Entry<String, Collection<String>> uriBranch : uriBranches.entrySet() ) {
         final String uri = uriBranch.getKey();
         uriBestRootMap.put( uri, uri );
         int longestBranch = uriBranch.getValue().size();
         for ( Map.Entry<String, Collection<String>> testUriBranch : uriBranches.entrySet() ) {
            if ( testUriBranch.getKey().equals( uri ) ) {
               continue;
            }
            final Collection<String> testBranch = testUriBranch.getValue();
            if ( testBranch.size() > longestBranch && testBranch.contains( uri ) ) {
               uriBestRootMap.put( uri, testUriBranch.getKey() );
               longestBranch = testBranch.size();
            }
         }
      }
      final Map<String, Collection<String>> branchMembers = new HashMap<>();
      for ( Map.Entry<String, String> uriBestRoot : uriBestRootMap.entrySet() ) {
         branchMembers.computeIfAbsent( uriBestRoot.getValue(), u -> new ArrayList<>() ).add( uriBestRoot.getKey() );
      }
      return branchMembers;
   }

   static private void createRelations( final JCas jCas,
                                        final String relationName,
                                        final List<IdentifiedAnnotation> sources,
                                        final List<IdentifiedAnnotation> targets ) {
      Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargetMap;
      if ( relationName.equals( HAS_TREATMENT ) ) {
         // TODO : Should this be RelationUtil.createSourceTargetMap( sources, targets, false? )
         sourceTargetMap = new HashMap<>( RelationUtil.createReverseAttributeMapSingle( sources, targets, true ) );
      } else {
         if ( relationName.equals( has_Biomarker ) ) {
            // One neoplasm can have multiple types of biomarker.
            sourceTargetMap = new HashMap<>( RelationUtil.createSourceTargetMap( sources, targets, false ) );
         } else {
            sourceTargetMap = new HashMap<>( RelationUtil.createSourceTargetMap( sources, targets, true ) );
         }
      }
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : sourceTargetMap.entrySet() ) {
         final IdentifiedAnnotation owner = entry.getKey();
         entry.getValue().forEach( n -> RelationUtil. createRelation( jCas, owner, n, relationName ) );
      }
   }


   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                                     LATERALITIES
   //
   //////////////////////////////////////////////////////////////////////////////////////////////


   /**
    *
    * @param jCas ye olde ...
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI.
    * @param sourceToRelationNamesTargetsMap Map of source URI to Map of Relation name to MOST SPECIFIC target URIs.
    *                                        e.g. brca only to breast as location.
    * @param relationsDone annotations and relations already completed for those annotations.
    * @param locations Collection of created Location Relations.
    */
   static private void createWindowLateralities( final JCas jCas,
                                                 final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                                 final Map<String, Map<String, Collection<String>>> sourceToRelationNamesTargetsMap,
                                                 final Map<IdentifiedAnnotation, Collection<String>> relationsDone,
                                                 final Collection<BinaryTextRelation> locations ) {
      if ( uriAnnotationMap.isEmpty() ) {
         return;
      }
      final Collection<String> hasLateralityUri = new ArrayList<>();
      for ( Map.Entry<String, Map<String, Collection<String>>> sourceToRelationNamesTargets : sourceToRelationNamesTargetsMap
            .entrySet() ) {
         if ( sourceToRelationNamesTargets.getValue().containsKey( HAS_LATERALITY ) ) {
            hasLateralityUri.add( sourceToRelationNamesTargets.getKey() );
         }
      }
      makeUriLateralities( jCas, uriAnnotationMap, hasLateralityUri, relationsDone, locations );
   }


   /**
    *
    * @param jCas ye olde ...
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI.
    * @param hasLateralityUris source URIs that are able to have laterality.
    * @param relationsDone annotations and relations already completed for those annotations.
    * @param locations Collection of created Location Relations.
    */
   static private void makeUriLateralities( final JCas jCas,
                                            final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                            final Collection<String> hasLateralityUris,
                                            final Map<IdentifiedAnnotation, Collection<String>> relationsDone,
                                            final Collection<BinaryTextRelation> locations ) {
      if ( hasLateralityUris == null || hasLateralityUris.isEmpty() || locations.isEmpty() ) {
         return;
      }
      final Collection<IdentifiedAnnotation> lateralities = new ArrayList<>();
      lateralities.addAll( getLaterality( UriConstants.LEFT, uriAnnotationMap ) );
      lateralities.addAll( getLaterality( UriConstants.RIGHT, uriAnnotationMap ) );
      lateralities.addAll( getLaterality( UriConstants.BILATERAL, uriAnnotationMap ) );
      // Some classes have abbreviations without simple lateralities.  e.g. "lll" = left lower lobe.
      lateralities.addAll( getMissingLaterality( "Left_Lower_Lung_Lobe", lateralities, uriAnnotationMap ) );
      lateralities.addAll( getMissingLaterality( "Upper_Lobe_Of_The_Left_Lung", lateralities, uriAnnotationMap ) );
      lateralities.addAll( getMissingLaterality( "Middle_Lobe_Of_The_Right_Lung", lateralities, uriAnnotationMap ) );
      lateralities.addAll( getMissingLaterality( "Right_Lower_Lung_Lobe", lateralities, uriAnnotationMap ) );
      lateralities.addAll( getMissingLaterality( "Upper_Lobe_Of_The_Right_Lung", lateralities, uriAnnotationMap ) );
      if ( lateralities.isEmpty() ) {
         return;
      }
      final String docText = jCas.getDocumentText();
      for ( BinaryTextRelation locationRelation : locations ) {
         final IdentifiedAnnotation mainEntity = (IdentifiedAnnotation)locationRelation.getArg1().getArgument();
         if ( !hasLateralityUris.contains( Neo4jOntologyConceptUtil.getUri( mainEntity ) ) ) {
            continue;
         }
         final IdentifiedAnnotation location = (IdentifiedAnnotation)locationRelation.getArg2().getArgument();
         for ( IdentifiedAnnotation laterality : lateralities ) {
            if ( laterality.getEnd() <= location.getEnd() && laterality.getEnd() > location.getBegin() - 20 ) {
               if ( laterality.getEnd() >= location.getBegin() ) {
                  // Laterality is part of location, e.g. "Left_Arm" synonyms
                  RelationUtil.createRelation( jCas, mainEntity, laterality, HAS_LATERALITY );
//                  relationsDone.computeIfAbsent( mainEntity, a -> new HashSet<>() ).add( HAS_LATERALITY );
                  break;
               }
               final String textBetween = docText.substring( laterality.getEnd(), location.getBegin() );
               final String[] splits = WHITESPACE.split( textBetween );
               if ( splits.length <= 5
                    && !textBetween.contains( "," ) && !textBetween.toLowerCase().contains( " and " ) ) {
                  RelationUtil.createRelation( jCas, mainEntity, laterality, HAS_LATERALITY );
                  relationsDone.computeIfAbsent( mainEntity, a -> new HashSet<>() ).add( HAS_LATERALITY );
                  break;
               }
            } else if ( laterality.getBegin() - location.getEnd() == 2
                        && docText.charAt( location.getEnd() ) == ','
                        && ( docText.length() == laterality.getEnd()
                             || docText.charAt( laterality.getEnd() ) == ','
                             || docText.charAt( laterality.getEnd() ) == '\r' )
                             || docText.charAt( laterality.getEnd() ) == '\n') {
               // Laterality is part of comma location, e.g. "Breast, Right"
               RelationUtil.createRelation( jCas, mainEntity, laterality, HAS_LATERALITY );
//               relationsDone.computeIfAbsent( mainEntity, a -> new HashSet<>() ).add( HAS_LATERALITY );
               break;
            }
         }
      }
   }


   /**
    * 
    * @param uri Some desired laterality URI.  e.g. Right.
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI.
    * @return all annotations with the given URI.
    */
   static private Collection<IdentifiedAnnotation> getLaterality( final String uri,
                                                                  final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap ) {
      final Collection<IdentifiedAnnotation> laterality = uriAnnotationMap.get( uri );
      if ( laterality != null ) {
         return laterality;
      }
      return Collections.emptyList();
   }

   /**
    *
    * @param uri Some desired laterality URI.  e.g. Right Lower Lung Lobe.
    * @param existingLateralities regular lateralities already obtained
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI.
    * @return all annotations with the given URI that do not already encapsulate a laterality annotation.
    */
   static private Collection<IdentifiedAnnotation> getMissingLaterality( final String uri,
                                                                         final Collection<IdentifiedAnnotation> existingLateralities,
                                                                  final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap ) {
      final Collection<IdentifiedAnnotation> lateralityAnnotations = getLaterality( uri, uriAnnotationMap );
      if ( lateralityAnnotations.isEmpty() ) {
         return Collections.emptyList();
      }
      if ( existingLateralities.isEmpty() ) {
         return lateralityAnnotations;
      }
      final Collection<IdentifiedAnnotation> newLateralities = new HashSet<>();
      for ( IdentifiedAnnotation annotation : lateralityAnnotations ) {
         final Pair<Integer> span = new Pair<>( annotation.getBegin(), annotation.getEnd() );
         final boolean isNew = existingLateralities.stream()
                         .noneMatch( a -> a.getBegin() <= span.getValue1() && a.getEnd() >= span.getValue2() );
         if ( isNew ) {
            newLateralities.add( annotation );
         }
      }
      return newLateralities;
   }



   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                                     FILTERS
   //
   //////////////////////////////////////////////////////////////////////////////////////////////


   /**
    *
    * @param section section in the document.
    * @return true if the section is NOT wanted for relation resolution.  e.g. Header sections.
    */
   static private boolean isUnwantedSection( final Segment section ) {
      // Are we sure that we don't want to include Findings ???
      final SectionType sectionType = SectionType.getSectionType( section );
      return sectionType == SectionType.Microscopic
//             || sectionType == SectionType.Examination
             || sectionType == SectionType.ReviewSystems
//             || sectionType == SectionType.Finding
             || sectionType == SectionType.FamilyHistory
             || sectionType == SectionType.HistologySummary
             || sectionType == SectionType.PittsburghHeader;
   }



   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                                     MAPPERS
   //
   //////////////////////////////////////////////////////////////////////////////////////////////


   /**
    *
    * @param sourceUris all unique URIs for all annotations in the document.
    * @return Map of source URI to Map of Relation name to MOST SPECIFIC target URIs.  e.g. brca only to breast as location.
    */
   private Map<String, Map<String, Collection<String>>> mapSourceToRelationNamesTargets(
         final Collection<String> sourceUris ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      final Map<String, Map<String, Collection<String>>> uriRelationMap = new HashMap<>();
      for ( String uri : sourceUris ) {
         // Map of relation name to target URIs  This is relation name to MOST SPECIFIC target uri.
         // For instance, cap to prostate ONLY.  brca to breast ONLY.
         Map<String, Collection<String>> uriRelations = getUriRelations( uri );
         if ( uriRelations == null ) {
            uriRelations = Neo4jRelationUtil.getRelatedClassUris( graphDb, uri );
            addUriRelations( uri, uriRelations );
         }
         if ( uriRelations.isEmpty() ) {
            continue;
         }
         uriRelationMap.put( uri, uriRelations );
      }
      return uriRelationMap;
   }

   
   /**
    * 
    * @param sourceToRelationNamesTargetsMap Map of source URI to Map of Relation name to MOST SPECIFIC target URIs.  
    *                                        e.g. brca only to breast as location.
    * @return Map of Map of  Map of Relation name to target URIs to collection of source URIs.  
    * MOST SPECIFIC relation target uris.  e.g. (location, [breast]),[brca, dcis] ;
    * 
    */
   static private Map<Map<String, Collection<String>>, Collection<String>> mapRelationToSources(
         final Map<String, Map<String, Collection<String>>> sourceToRelationNamesTargetsMap ) {
      // Map of Map of  Map of Relation name to target URIs to collection of source URIs
      final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap = new HashMap<>();
      for ( Map.Entry<String, Map<String, Collection<String>>> uriRelation : sourceToRelationNamesTargetsMap.entrySet() ) {
         relationToSourceMap.computeIfAbsent( uriRelation.getValue(), u -> new ArrayList<>() )
                            .add( uriRelation.getKey() );
      }
      return relationToSourceMap;
   }



   /**
    *
    * @param allUris all uris in the document.
    * @param relationNameTargetsToSources Map of Map of Map of Relation name to target URIs to set of source URIs.
    *                                     MOST SPECIFIC relation target uris.  e.g. (location, [breast]),[brca, dcis]
    * @return Cache of root nodes to their branch nodes.  Could be large, but drastically improves speed.
    */
   private Map<String,Collection<String>> createBranchMap( final Collection<String> allUris,
         final Map<Map<String, Collection<String>>, Collection<String>> relationNameTargetsToSources ) {
      final Collection<String> allRelationTargetUris
            = relationNameTargetsToSources.keySet().stream()
                                          .map( Map::values )
                                          .flatMap( Collection::stream )
                                          .flatMap( Collection::stream )
                                          .collect( Collectors.toSet() );

      allRelationTargetUris.addAll( allUris );

      final Map<String,Collection<String>> branchMap = new HashMap<>( allRelationTargetUris.size() );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      for ( String relationTargetUri : allRelationTargetUris ) {
         Collection<String> branchUris = getUriBranch( relationTargetUri );
         if ( branchUris == null ) {
            branchUris = SearchUtil.getBranchUris( graphDb, relationTargetUri );
            addUriBranch( relationTargetUri, branchUris );
         }
         branchMap.put( relationTargetUri, branchUris );
      }
      return branchMap;
   }








   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                                     EXTRACTORS
   //
   //////////////////////////////////////////////////////////////////////////////////////////////

   
   /**
    *
    * @param paragraphAnnotations Map of all annotations per paragraph.
    * @return All unique URIs for all annotations in the entire document.
    */
   static private Collection<String> getAllUris(
         final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotations ) {
      return paragraphAnnotations.values().stream()
                                 .flatMap( Collection::stream )
                                 .map( Neo4jOntologyConceptUtil::getUris )
                                 .flatMap( Collection::stream )
                                 .collect( Collectors.toSet() );
   }




   static private List<IdentifiedAnnotation> getSourceAnnotations( final String relationName,
                                                                   final Collection<String> alikeSourceUris,
                                                                   final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                                                   final Map<IdentifiedAnnotation, Collection<String>> relationsDone ) {
      final Predicate<IdentifiedAnnotation> relationNotDone = a -> relationsDone.get( a ) == null
                                                                   || !relationsDone.get( a ).contains( relationName );
      return alikeSourceUris.stream()
                            .map( uriAnnotationMap::get )
                            .filter( Objects::nonNull )
                            .flatMap( Collection::stream )
                            .filter( relationNotDone )
                            .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                            .collect( Collectors.toList() );
   }


   static private List<IdentifiedAnnotation> getTargetAnnotations( final Collection<String> targetUris,
                                                                   final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap ) {
      return targetUris.stream()
                       .map( uriAnnotationMap::get )
                       .filter( Objects::nonNull )
                       .flatMap( Collection::stream )
                       .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                       .collect( Collectors.toList() );
   }




   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                                     CLEANERS / PRUNERS
   //
   //////////////////////////////////////////////////////////////////////////////////////////////


   static private void cleanLocations(
         final Map<String, Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>>> relationLocationsMap ) {
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> diseasePrimaries
            = relationLocationsMap.getOrDefault( DISEASE_HAS_PRIMARY_ANATOMIC_SITE, new HashMap<>( 0 ) );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> diseaseMetastatics
            = relationLocationsMap.getOrDefault( DISEASE_HAS_METASTATIC_ANATOMIC_SITE, new HashMap<>( 0 ) );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> diseaseAssociated
            = relationLocationsMap.getOrDefault( DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE, new HashMap<>( 0 ) );

      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> diseaseRegions
            = relationLocationsMap.getOrDefault( Disease_Has_Associated_Region, new HashMap<>( 0 ) );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> diseaseCavities
            = relationLocationsMap.getOrDefault( Disease_Has_Associated_Cavity, new HashMap<>( 0 ) );

      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> findingAssociated
            = relationLocationsMap.getOrDefault( Finding_Has_Associated_Site, new HashMap<>( 0 ) );

      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> findingRegions
            = relationLocationsMap.getOrDefault( Finding_Has_Associated_Region, new HashMap<>( 0 ) );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> findingCavities
            = relationLocationsMap.getOrDefault( Finding_Has_Associated_Cavity, new HashMap<>( 0 ) );

      cleanRelations( diseasePrimaries,
            Arrays.asList( diseaseMetastatics, diseaseAssociated, diseaseRegions, diseaseCavities,
                  findingAssociated, findingRegions, findingCavities ) );
      cleanRelations( diseaseMetastatics,
            Arrays.asList( diseaseAssociated, diseaseRegions, diseaseCavities,
                  findingAssociated, findingRegions, findingCavities ) );
      cleanRelations( diseaseAssociated,
            Arrays.asList( diseaseRegions, diseaseCavities,
                  findingAssociated, findingRegions, findingCavities ) );
      cleanRelations( diseaseRegions,
            Arrays.asList( diseaseCavities,
                  findingAssociated, findingRegions, findingCavities ) );

      cleanRelations( findingAssociated, Arrays.asList( findingRegions, findingCavities ) );
      cleanRelations( findingRegions, Collections.singletonList( findingCavities ) );
   }




   static private void cleanRelations( final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> keepMap,
                                       final Collection<Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>>> cleanMaps ) {
      if ( keepMap.isEmpty() ) {
         return;
      }
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> keep : keepMap.entrySet() ) {
         final Collection<IdentifiedAnnotation> keepTargets = keep.getValue();
         if ( keepTargets == null || keepTargets.isEmpty() ) {
            continue;
         }
         final IdentifiedAnnotation keepSource = keep.getKey();
         for ( Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> cleanMap : cleanMaps ) {
            final Collection<IdentifiedAnnotation> cleanTargets = cleanMap.get( keepSource );
            if ( cleanTargets == null || cleanTargets.isEmpty() ) {
               continue;
            }
            cleanTargets.removeAll( keepTargets );
         }
      }
   }


   public void addUriBranch( final String rootUri, final Collection<String> branch ) {
      synchronized ( LOCK ) {
         final long millis = System.currentTimeMillis();
         _timeMap.put( rootUri, millis );
         _uriBranchMap.put( rootUri, branch );
      }
   }

   public Collection<String> getUriBranch( final String rootUri ) {
      synchronized ( LOCK ) {
         return _uriBranchMap.get( rootUri );
      }
   }


   public void addUriRelations( final String rootUri, final Map<String, Collection<String>> uriRelations ) {
      synchronized ( LOCK ) {
         final long millis = System.currentTimeMillis();
         _timeMap.put( rootUri, millis );
         _uriRelationMap.put( rootUri, uriRelations );
      }
   }

   public Map<String, Collection<String>> getUriRelations( final String rootUri ) {
      synchronized ( LOCK ) {
         return _uriRelationMap.get( rootUri );
      }
   }


   private final class CacheCleaner implements Runnable {
      public void run() {
         final long old = System.currentTimeMillis() - TIMEOUT;
         synchronized ( LOCK ) {
            final Collection<String> removals = new ArrayList<>();
            for ( Map.Entry<String, Long> timeEntry : _timeMap.entrySet() ) {
               if ( timeEntry.getValue() < old ) {
                  removals.add( timeEntry.getKey() );
               }
            }
            _uriBranchMap.keySet().removeAll( removals );
            _uriRelationMap.keySet().removeAll( removals );
            _timeMap.keySet().removeAll( removals );
         }
      }
   }



   static private final Collection<String> UNWANTED_SITE_PRECEDENTS = new HashSet<>( Arrays.asList(
         "near ",
         "over ",
         "under ",
         "above ",
         "below ",
         "between ",
//         "in "
         "adjacent to ",
         "anterior to ",
         "superior to "
   ) );



   static private boolean isSiteActual( final String docText, final IdentifiedAnnotation site ) {
      if ( IdentifiedAnnotationUtil.isNegated( site ) ) {
         return false;
      }
      final int begin = Math.max( 0, site.getBegin() - 40 );
      final String preceding = docText.substring( begin, site.getBegin() ).toLowerCase();
      return UNWANTED_SITE_PRECEDENTS.stream().noneMatch( preceding::contains );
   }






   // Attributes is anatomical site (location), owners is finding, disorder, procedure (thing at location)
   // In other words, attributeOwner hasRelationTo attribute
   // TODO for some reason this seems to randomly take a -long- time
   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> mapSitableSites(
         final JCas jcas,
         final List<IdentifiedAnnotation> sitables,
         final List<IdentifiedAnnotation> sites,
         final boolean onlyOneSitable ) {
      if ( sitables.isEmpty() || sites.isEmpty() ) {
         return Collections.emptyMap();
      }
//      final Collection<IdentifiedAnnotation> modifiers
//            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jcas, UriConstants.BODY_MODIFIER );
//      attributeOwners.removeAll( modifiers );
//      if ( attributeOwners.isEmpty() || attributes.isEmpty() ) {
//         return Collections.emptyMap();
//      }

      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> conjoinedSites
            = getConjoinedSites( jcas.getDocumentText(), sites );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sitableMap = new HashMap<>( sitables
            .size() );
      for ( IdentifiedAnnotation sitable : sitables ) {
         if ( onlyOneSitable && sitableMap.containsKey( sitable ) ) {
            continue;
         }
         final String sitableSentenceId = sitable.getSentenceID();
         final Collection<IdentifiedAnnotation> sameSentenceSites = new ArrayList<>();
         for ( IdentifiedAnnotation site : sites ) {
            if ( sitable.equals( site )
                 || (sitable.getSubject() != null && !sitable.getSubject().equals( site.getSubject() )) ) {
               continue;
            }
            if ( sitableSentenceId.equals( site.getSentenceID() ) ) {
               sameSentenceSites.add( site );
            }
         }
         final Collection<IdentifiedAnnotation> assignedSites = new ArrayList<>( sites.size() );
         IdentifiedAnnotation bestSite = null;
         for ( IdentifiedAnnotation site : sameSentenceSites ) {
            // if location is after locatable thing then it is the best location?
            if ( site.getBegin() > sitable.getEnd() ) {
               if ( bestSite == null ) {
                  bestSite = site;
               }
               break;
            }
            bestSite = site;
         }
         if ( bestSite != null ) {
            sitableMap.computeIfAbsent( sitable, a -> new HashSet<>() ).add( bestSite );
            assignedSites.add( bestSite );
            // Check conjoined, adding a duplicate owner to each conjoined attribute
            final Collection<IdentifiedAnnotation> conjoinedList = conjoinedSites.get( bestSite );
            if ( conjoinedList != null ) {
               for ( IdentifiedAnnotation conjoined : conjoinedList ) {
                  if ( !conjoined.equals( bestSite ) ) {
                     sitableMap.get( sitable ).add( conjoined );
                  }
               }
            }
            continue;
         }
         // get best attribute by those in preceding sentences
         for ( IdentifiedAnnotation site : sites ) {
            if ( sitable.equals( site ) || assignedSites.contains( site ) ) {
               continue;
            }
            if ( site.getBegin() > sitable.getEnd() ) {
               break;
            }
            bestSite = site;
         }
         if ( bestSite != null ) {
            sitableMap.computeIfAbsent( sitable, a -> new HashSet<>() ).add( bestSite );
            // Check conjoined, adding a duplicate owner to each conjoined attribute
            final Collection<IdentifiedAnnotation> conjoinedList = conjoinedSites.get( bestSite );
            if ( conjoinedList != null ) {
               for ( IdentifiedAnnotation conjoined : conjoinedList ) {
                  if ( !conjoined.equals( bestSite ) ) {
                     sitableMap.get( sitable ).add( conjoined );
                  }
               }
            }
         }
      }
      return sitableMap;
   }


   static private Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> getConjoinedSites(
         final String docText,
         final List<IdentifiedAnnotation> sites ) {
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> conjoinedSiteLists = new HashMap<>();
      IdentifiedAnnotation previousSite = null;
      String previousSentenceId = null;
      List<IdentifiedAnnotation> conjoinedSites = new ArrayList<>();
      for ( IdentifiedAnnotation site : sites ) {
         if ( previousSite == null ) {
            previousSite = site;
            previousSentenceId = previousSite.getSentenceID();
            continue;
         }
         if ( site.getBegin() <= previousSite.getEnd() ) {
            // The previous site overlaps and is longer than this site.
            // Since the are the same type, this site should be subsumed.  Skip it.
            continue;
         }
         final String siteSentenceId = site.getSentenceID();
         if ( !siteSentenceId.equals( previousSentenceId ) ) {
            if ( !conjoinedSites.isEmpty() ) {
               final Collection<IdentifiedAnnotation> conjoined = new ArrayList<>( conjoinedSites );
//               conjoinedSites.forEach( a -> conjoinedSiteLists.put( a, conjoined ) );
               conjoinedSiteLists.put( conjoinedSites.get( conjoinedSites.size()-1 ), conjoined );
               conjoinedSites = new ArrayList<>();
            }
            previousSite = site;
            previousSentenceId = siteSentenceId;
            continue;
         }
         final String textBetween = docText.substring( previousSite.getEnd(), site.getBegin() );
         final String[] splits = WHITESPACE.split( textBetween );
         if ( splits.length < 5
              && (textBetween.contains( "," ) || textBetween.toLowerCase().contains( " and " )) ) {
            if ( conjoinedSites.isEmpty() ) {
               conjoinedSites.add( previousSite );
            }
            conjoinedSites.add( site );
         } else {
            if ( !conjoinedSites.isEmpty() ) {
               final Collection<IdentifiedAnnotation> conjoined = new ArrayList<>( conjoinedSites );
//               conjoinedSites.forEach( a -> conjoinedSiteLists.put( a, conjoined ) );
               conjoinedSiteLists.put( conjoinedSites.get( conjoinedSites.size()-1 ), conjoined );
               conjoinedSites = new ArrayList<>();
            }
         }
         previousSite = site;
         previousSentenceId = siteSentenceId;
      }
      if ( !conjoinedSites.isEmpty() ) {
         final Collection<IdentifiedAnnotation> conjoined = new ArrayList<>( conjoinedSites );
//         conjoinedSites.forEach( a -> conjoinedSiteLists.put( a, conjoined ) );
         conjoinedSiteLists.put( conjoinedSites.get( conjoinedSites.size()-1 ), conjoined );
      }

      return conjoinedSiteLists;
   }

   static private IdentifiedAnnotation createDuplicate( final JCas jCas, final IdentifiedAnnotation annotation ) {
      final SemanticGroup semanticGroup = SemanticGroup.getBestGroup( annotation );
      final IdentifiedAnnotation duplicate = semanticGroup.getCreator().apply( jCas );
      duplicate.setBegin( annotation.getBegin() );
      duplicate.setEnd( annotation.getEnd() );
      duplicate.setTypeID( annotation.getTypeID() );
      duplicate.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( annotation );
      if ( !umlsConcepts.isEmpty() ) {
         final FSArray conceptArray = new FSArray( jCas, umlsConcepts.size() );
         int i = 0;
         for ( UmlsConcept umlsConcept : umlsConcepts ) {
            conceptArray.set( i, umlsConcept );
            i++;
         }
         duplicate.setOntologyConceptArr( conceptArray );
      }
      duplicate.setConditional( annotation.getConditional() );
      duplicate.setConfidence( annotation.getConfidence() );
      duplicate.setGeneric( annotation.getGeneric() );
      duplicate.setHistoryOf( annotation.getHistoryOf() );
      duplicate.setPolarity( annotation.getPolarity() );
      duplicate.setSegmentID( annotation.getSegmentID() );
      duplicate.setSentenceID( annotation.getSentenceID() );
      duplicate.setSubject( annotation.getSubject() );
      duplicate.setUncertainty( annotation.getUncertainty() );
      duplicate.addToIndexes( jCas );
      return duplicate;
   }


}
