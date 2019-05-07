package org.apache.ctakes.cancer.ae;


import org.apache.ctakes.cancer.document.SectionType;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.Neo4jRelationUtil;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ctakes.typesystem.type.constants.CONST.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/27/2017
 */
final public class ByUriRelationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "ByUriRelationFinder" );


   static private final String SITE_MIN = "SiteMinimum";
   static private final String SITE_MIN_DESC = "Minimmum number of anatomical sites that must be related to be valid.";

   @ConfigurationParameter(
         name = SITE_MIN,
         description = SITE_MIN_DESC,
         mandatory = false,
         defaultValue = "2"
   )
   private int _siteMin = 2;

   static private final String DEVIATION_DIV = "DeviationDivider";
   static private final String DEVIATION_DIV_DESC = "Number by which to divide the standard deviation for more lenient site acceptance.";

   @ConfigurationParameter(
         name = DEVIATION_DIV,
         description = DEVIATION_DIV_DESC,
         mandatory = false,
         defaultValue = "2"
   )
   private int _devDiv = 2;

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );

   // TODO refactor to new Uri Constants, api

   static private final Collection<Integer> VALID_SOURCES
         = Arrays.asList( NE_TYPE_ID_DRUG, NE_TYPE_ID_DISORDER, NE_TYPE_ID_FINDING, NE_TYPE_ID_PROCEDURE,
                          NE_TYPE_ID_ANATOMICAL_SITE, NE_TYPE_ID_CLINICAL_ATTRIBUTE, NE_TYPE_ID_DEVICE,
                          NE_TYPE_ID_PHENOMENA, NE_TYPE_ID_GENERIC_EVENT, NE_TYPE_ID_GENERIC_ENTITY,
         NE_TYPE_ID_LAB, NE_TYPE_ID_GENERIC_MODIFIER, NE_TYPE_ID_LAB_VALUE_MODIFIER );


      /**
       * Adds relation types registered in the ontology for neoplasms
       * {@inheritDoc}
       */
      @Override
      public void process( final JCas jCas ) throws AnalysisEngineProcessException {
         LOGGER.info( "Finding Relations defined in the Ontology ..." );
        final Map<Segment, Collection<Paragraph>> sectionParagraphMap
               = JCasUtil.indexCovered( jCas, Segment.class, Paragraph.class );
         final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap = JCasUtil.indexCovered( jCas,
               Paragraph.class,
               IdentifiedAnnotation.class );
         final Collection<String> sourceUris = paragraphAnnotationMap.values()
                                                                     .stream()
                                                                     .flatMap( Collection::stream )
                                                                     .filter(
                                                                           a -> VALID_SOURCES.contains( a.getTypeID() ) )
                                                                     .map( Neo4jOntologyConceptUtil::getUris )
                                                                     .flatMap( Collection::stream )
                                                                     .collect( Collectors.toSet() );
         final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
         // Map of source URI to Map of Relation name to target URIs
         final Map<String, Map<String, Collection<String>>> uriRelationMap = new HashMap<>();
         for ( String uri : sourceUris ) {
            // Map of relation name to target URIs
            final Map<String, Collection<String>> uriRelations = Neo4jRelationUtil.getRelatedClassUris( graphDb, uri );
            if ( Neo4jOntologyConceptUtil.getRootUris( uri ).contains( "Skin_Neoplasm" ) ) {
               // Skin_Neoplasms (and Skin_Diseases) only have Skin as a site.  Add our valid sites.
               uriRelations.computeIfAbsent( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                     a -> new ArrayList<>() )
                           .addAll( Arrays.asList( UriConstants.ANATOMIES ) );
            }
            if ( uriRelations.isEmpty() ) {
               continue;
            }
            uriRelationMap.put( uri, uriRelations );
         }
         // Map of Map of  Map of Relation name to target URIs to collection of source URIs
         final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap = new HashMap<>();
         for ( Map.Entry<String, Map<String, Collection<String>>> uriRelation : uriRelationMap.entrySet() ) {
            relationToSourceMap.computeIfAbsent( uriRelation.getValue(), u -> new ArrayList<>() )
                               .add( uriRelation.getKey() );
         }

         final Map<String, Collection<IdentifiedAnnotation>> allUriAnnotationMap = new HashMap<>();
         for ( Map.Entry<Segment, Collection<Paragraph>> sectionParagraphs : sectionParagraphMap.entrySet() ) {
            // Are we sure that we don't want to include Findings ???
            if ( SectionType.Microscopic.isThisSectionType( sectionParagraphs.getKey() )
                 || SectionType.Finding.isThisSectionType( sectionParagraphs.getKey() )
                 || SectionType.HistologySummary.isThisSectionType( sectionParagraphs.getKey() ) ) {
               continue;
            }
            // Make locations first because in this step annotations can be duplicated
            final Collection<IdentifiedAnnotation> located = new ArrayList<>();
            for ( Paragraph paragraph : sectionParagraphs.getValue() ) {
               final Collection<IdentifiedAnnotation> paragraphAnnotations = paragraphAnnotationMap.get( paragraph );
               if ( paragraphAnnotations != null && !paragraphAnnotations.isEmpty() ) {
                  final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap
                        = Neo4jOntologyConceptUtil.mapUriAnnotations( paragraphAnnotations );
                  makeParagraphLocations( jCas, uriAnnotationMap, relationToSourceMap, located );
               }
            }

            // Need to fetch this again because there may now be duplicates.
            // TODO improve, maybe return paragraph annotations from called methods?
            // TODO medications seem to be missing for some duplicate location tumors
            final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap2 = JCasUtil.indexCovered( jCas,
                  Paragraph.class,
                  IdentifiedAnnotation.class );

            for ( Paragraph paragraph : sectionParagraphs.getValue() ) {
               final Collection<IdentifiedAnnotation> paragraphAnnotations = paragraphAnnotationMap2.get( paragraph );
               if ( paragraphAnnotations != null && !paragraphAnnotations.isEmpty() ) {
                  final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap
                        = Neo4jOntologyConceptUtil.mapUriAnnotations( paragraphAnnotations );
                  makeParagraphRelations( jCas, uriAnnotationMap, relationToSourceMap );
                  for ( Map.Entry<String,Collection<IdentifiedAnnotation>> uriAnnotations : uriAnnotationMap.entrySet() ) {
                     allUriAnnotationMap.computeIfAbsent( uriAnnotations.getKey(), k -> new ArrayList<>() )
                                        .addAll( uriAnnotations.getValue() );
                  }
               }
            }
         }

         makeLateralities( jCas, allUriAnnotationMap, uriRelationMap );

         LOGGER.info( "Finished Processing" );
   }


   /**
    * TODO move to a utility class
    * Given any collection of uris, collates and returns those uris in related branches.
    *
    * @param allUris some collection of uris
    * @return a map of branch root uris and the collection of child uris under that branch root
    */
   static public Map<String, Collection<String>> collateUris( final Collection<String> allUris ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      final Map<String, Collection<String>> uriBranches
            = allUris.stream()
                     .distinct()
                     .collect( Collectors.toMap( Function.identity(), u -> SearchUtil.getBranchUris( graphDb, u ) ) );

      final Map<String, String> uriBestRootMap = new HashMap<>( uriBranches.size() );
      for ( Map.Entry<String, Collection<String>> uriBranch : uriBranches.entrySet() ) {
         final String uri = uriBranch.getKey();
         uriBestRootMap.put( uri, uri );
         int longestBranch = uriBranch.getValue().size();
         for ( Map.Entry<String, Collection<String>> testUriBranch : uriBranches.entrySet() ) {
            final Collection<String> branch = testUriBranch.getValue();
            if ( branch.size() > longestBranch && branch.contains( uri ) ) {
               uriBestRootMap.put( uri, testUriBranch.getKey() );
               longestBranch = branch.size();
            }
         }
      }
      final Map<String, Collection<String>> branchMembers = new HashMap<>();
      for ( Map.Entry<String, String> uriBestRoot : uriBestRootMap.entrySet() ) {
         branchMembers.computeIfAbsent( uriBestRoot.getValue(), u -> new ArrayList<>() ).add( uriBestRoot.getKey() );
      }
      return branchMembers;
   }




   /**
    * @param jCas                 -
    * @param uriAnnotationMap annotations in paragraph
    */
   static private void makeParagraphRelations( final JCas jCas,
                                               final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                               final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap ) {
      if ( uriAnnotationMap.isEmpty() ) {
         return;
      }
      for ( Map.Entry<Map<String, Collection<String>>, Collection<String>> relationToSources : relationToSourceMap
            .entrySet() ) {
         final Map<String, Collection<String>> collatedSources = collateUris( relationToSources.getValue() );
         for ( Collection<String> sources : collatedSources.values() ) {
            // second to last (relationToSources.getKey()) is a map of relationName to possible target uris.
            makeUriRelations( jCas, sources, uriAnnotationMap, relationToSources.getKey() );
         }
      }
   }

   /**
    * @param jCas ye olde ...
    * @param uriAnnotationMap Map of URI to all paragraph Annotations that have the URI
    * @param relationToSourceMap Map of Map of  Map of Relation name to target URIs to collection of source URIs
    * @param located annotations that already have locations
    */
   static private void makeParagraphLocations( final JCas jCas,
                                               final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                               final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap,
                                               final Collection<IdentifiedAnnotation> located ) {
      if ( uriAnnotationMap.isEmpty() ) {
         return;
      }
      final Map<String, Collection<IdentifiedAnnotation>> uriOkLocationMap = new HashMap<>( uriAnnotationMap );
      uriOkLocationMap.keySet().removeAll( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.BODY_MODIFIER ) );
      // todo only bother for location relations
      uriOkLocationMap.keySet().removeAll( UriConstants.getUnwantedAnatomyUris() );

      for ( Map.Entry<Map<String, Collection<String>>, Collection<String>> relationToSources : relationToSourceMap
            .entrySet() ) {
         final Map<String, Collection<String>> collatedSources = collateUris( relationToSources.getValue() );
         for ( Collection<String> sources : collatedSources.values() ) {
            // second to last (relationToSources.getKey()) is a map of relationName to possible target uris.
            makeUriLocations( jCas, sources, uriAnnotationMap, relationToSources.getKey(), uriOkLocationMap, located );
         }
      }
   }

   /**
    * @param jCas                 -
    * @param uriAnnotationMap annotations in paragraph
    */
   static private void makeLateralities( final JCas jCas,
                                         final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
//                                         final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap ) {
                                         final Map<String, Map<String, Collection<String>>> uriRelationMap )  {
      if ( uriAnnotationMap.isEmpty() ) {
         return;
      }
      final Collection<String> hasLateralityUri = new ArrayList<>();
      for ( Map.Entry<String, Map<String, Collection<String>>> uriRelations : uriRelationMap.entrySet() ) {
         if ( uriRelations.getValue().containsKey( RelationConstants.HAS_LATERALITY ) ) {
            hasLateralityUri.add( uriRelations.getKey() );
         }
         // second to last (relationToSources.getKey()) is a map of relationName to possible target uris.
      }
      makeUriLateralities( jCas, uriAnnotationMap, hasLateralityUri );
   }



   static private void makeUriLocations( final JCas jCas,
                                         final Collection<String> sourceUris,
                                         final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                         final Map<String, Collection<String>> uriRelations,
                                         final Map<String, Collection<IdentifiedAnnotation>> uriOkLocationMap,
                                         final Collection<IdentifiedAnnotation> located ) {
      for ( Map.Entry<String, Collection<String>> relation : uriRelations.entrySet() ) {
         final String relationName = relation.getKey();
         if ( !relationName.equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE ) ) {
            continue;
         }
         final Collection<String> relationTargetUris = relation.getValue();
         final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
         final Collection<String> targetUris = Neo4jRelationUtil.getRelatableUris( graphDb, uriOkLocationMap.keySet(),
                  relationTargetUris );
         if ( targetUris.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> sourceList
               = sourceUris.stream()
                           .map( uriAnnotationMap::get )
                           .filter( Objects::nonNull )
                           .flatMap( Collection::stream )
                           .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                           .collect( Collectors.toList() );
         if ( sourceList.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> targetList
               = targetUris.stream()
                           .map( uriAnnotationMap::get )
                           .flatMap( Collection::stream )
                           .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                           .collect( Collectors.toList() );
         createLocations( jCas, relationName, sourceList, targetList, located );
      }
   }

   static private void makeUriLateralities( final JCas jCas,
                                            final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                            final Collection<String> hasLateralityUris ) {
      if ( hasLateralityUris == null || hasLateralityUris.isEmpty() ) {
         return;
      }
      final Collection<IdentifiedAnnotation> lateralities = new ArrayList<>();
      lateralities.addAll( getLaterality( UriConstants.LEFT, uriAnnotationMap ) );
      lateralities.addAll( getLaterality( UriConstants.RIGHT, uriAnnotationMap ) );
      lateralities.addAll( getLaterality( UriConstants.BILATERAL, uriAnnotationMap ) );
      if ( lateralities.isEmpty() ) {
         return;
      }
      final Collection<LocationOfTextRelation> locations = JCasUtil.select( jCas, LocationOfTextRelation.class );
      if ( locations.isEmpty() ) {
         return;
      }
      final String docText = jCas.getDocumentText();
      for ( LocationOfTextRelation locationRelation : locations ) {
         final IdentifiedAnnotation mainEntity = (IdentifiedAnnotation)locationRelation.getArg1().getArgument();
         if ( !hasLateralityUris.contains( Neo4jOntologyConceptUtil.getUri( mainEntity ) ) ) {
            continue;
         }
         final IdentifiedAnnotation location = (IdentifiedAnnotation)locationRelation.getArg2().getArgument();
         for ( IdentifiedAnnotation laterality : lateralities ) {
            if ( laterality.getEnd() <= location.getEnd() && laterality.getEnd() > location.getBegin() - 20 ) {
               if ( laterality.getEnd() >= location.getBegin() ) {
                  // Laterality is part of location, e.g. "Left_Arm" synonyms
                  RelationUtil.createRelation( jCas, mainEntity, laterality, RelationConstants.HAS_LATERALITY );
                  break;
               }
               final String textBetween = docText.substring( laterality.getEnd(), location.getBegin() );
               final String[] splits = WHITESPACE.split( textBetween );
               if ( splits.length <= 5
                    && !textBetween.contains( "," ) && !textBetween.toLowerCase().contains( " and " ) ) {
                  RelationUtil.createRelation( jCas, mainEntity, laterality, RelationConstants.HAS_LATERALITY );
                  break;
               }
            }
         }
      }
   }

   static private Collection<IdentifiedAnnotation> getLaterality( final String uri,
         final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap ) {
      final Collection<IdentifiedAnnotation> laterality = uriAnnotationMap.get( uri );
      if ( laterality != null ) {
         return laterality;
      }
      return Collections.emptyList();
   }

   static private void makeUriRelations( final JCas jCas,
                                         final Collection<String> sourceUris,
                                         final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                         final Map<String, Collection<String>> uriRelations ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      // relation is a name
      for ( Map.Entry<String, Collection<String>> relation : uriRelations.entrySet() ) {
         final String relationName = relation.getKey();
         if ( relationName.equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE )
              || relationName.equals( RelationConstants.HAS_LATERALITY ) ) {
            continue;
         }
         final Collection<String> relationTargetUris = relation.getValue();
         final Collection<String> targetUris = Neo4jRelationUtil.getRelatableUris( graphDb, uriAnnotationMap.keySet(),
                  relationTargetUris );
         if ( targetUris.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> sourceList
               = sourceUris.stream()
                           .map( uriAnnotationMap::get )
                           .filter( Objects::nonNull )
                           .flatMap( Collection::stream )
                           .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                           .collect( Collectors.toList() );
         if ( sourceList.isEmpty() ) {
            continue;
         }
         final List<IdentifiedAnnotation> targetList
               = targetUris.stream()
                           .map( uriAnnotationMap::get )
                           .flatMap( Collection::stream )
                           .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                           .collect( Collectors.toList() );
         createRelations( jCas, relationName, sourceList, targetList );
      }
   }



   static private void createRelations( final JCas jCas,
                                        final String relationName,
                                        final List<IdentifiedAnnotation> sources,
                                        final List<IdentifiedAnnotation> targets ) {
      if ( sources.isEmpty() || targets.isEmpty() ) {
         return;
      }
      Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargetMap;
      if ( relationName.equals( RelationConstants.HAS_TREATMENT ) ) {
         // TODO : Should this be RelationUtil.createSourceTargetMap( sources, targets, false? )
         sourceTargetMap = new HashMap<>( RelationUtil.createReverseAttributeMapSingle( sources, targets, true ) );
      } else {
         sourceTargetMap = new HashMap<>( RelationUtil.createSourceTargetMap( sources, targets, true ) );
      }
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : sourceTargetMap.entrySet() ) {
         final IdentifiedAnnotation owner = entry.getKey();
         entry.getValue()
              .forEach( n -> RelationUtil.createRelation( jCas, owner, n, relationName ) );
      }
   }

   static private void createLocations( final JCas jCas,
                                        final String relationName,
                                        final List<IdentifiedAnnotation> sources,
                                        final List<IdentifiedAnnotation> targets,
                                        final Collection<IdentifiedAnnotation> located ) {
      if ( sources.isEmpty() || targets.isEmpty() ) {
         return;
      }
      final List<IdentifiedAnnotation> goodSources = new ArrayList<>( sources.size() );
      for ( IdentifiedAnnotation source : sources ) {
         if ( !located.contains( source ) ) {
            goodSources.add( source );
         }
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargetMap
            = new HashMap<>( RelationUtil.createReverseAttributeMap( jCas, goodSources, targets, true ) );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : sourceTargetMap.entrySet() ) {
         final IdentifiedAnnotation owner = entry.getKey();
         entry.getValue()
              .forEach( n -> RelationUtil.createRelation( jCas, owner, n, relationName ) );
      }
   }

   static private boolean notLocated( final IdentifiedAnnotation annotation,
                                      final Collection<LocationOfTextRelation> locations,
                                      final Collection<String> anatomyUris ) {
      final String uri = Neo4jOntologyConceptUtil.getUri( annotation );
      LOGGER.info( uri + " notLocated, locations.isEmpty: " + locations.isEmpty() );
      LOGGER.info( uri + " notLocated, anatomyUris.contains( Neo4jOntologyConceptUtil.getUri( annotation ) ): " + anatomyUris.contains( Neo4jOntologyConceptUtil.getUri( annotation ) ) );
      LOGGER.info( uri + " notLocated, RelationUtil.getAllRelated( locations, annotation ).isEmpty(): " + RelationUtil.getAllRelated( locations, annotation ).isEmpty() );
      return locations.isEmpty()
             || anatomyUris.contains( Neo4jOntologyConceptUtil.getUri( annotation ) )
             || RelationUtil.getAllRelated( locations, annotation ).isEmpty();
   }

}
