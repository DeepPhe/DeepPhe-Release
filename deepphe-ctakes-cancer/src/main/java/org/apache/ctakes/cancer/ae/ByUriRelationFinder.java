package org.apache.ctakes.cancer.ae;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
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

      final Collection<LocationOfTextRelation> locations = JCasUtil.select( jCas, LocationOfTextRelation.class );
      final Collection<IdentifiedAnnotation> located = new ArrayList<>( locations.size() );
         for ( LocationOfTextRelation location : locations ) {
            if ( location != null
                 && location.getArg1() != null
                 && location.getArg1().getArgument() != null ) {
               final Annotation argument = location.getArg1().getArgument();
               if ( IdentifiedAnnotation.class.isInstance( argument ) ) {
                  located.add( (IdentifiedAnnotation)argument );
               }
            }
      }
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
      final Map<String, Map<String, Collection<String>>> uriRelationMap = new HashMap<>();
      for ( String uri : sourceUris ) {
         final Map<String, Collection<String>> uriRelations = Neo4jRelationUtil.getRelatedClassUris( graphDb, uri );
         if ( uriRelations.isEmpty() ) {
            continue;
         }
         uriRelationMap.put( uri, uriRelations );
      }

      final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap = new HashMap<>();
      for ( Map.Entry<String, Map<String, Collection<String>>> uriRelation : uriRelationMap.entrySet() ) {
         relationToSourceMap.computeIfAbsent( uriRelation.getValue(), u -> new ArrayList<>() )
                            .add( uriRelation.getKey() );
      }

      final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences
            = JCasUtil.indexCovering( jCas, IdentifiedAnnotation.class, Sentence.class );

      for ( Collection<IdentifiedAnnotation> paragraphAnnotations : paragraphAnnotationMap.values() ) {
         if ( !paragraphAnnotations.isEmpty() ) {
            makeParagraphRelations( jCas, paragraphAnnotations, coveringSentences, relationToSourceMap, located );
         }
      }
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
    * TODO move to a utility class
    * Given any collection of uris, collates and returns those uris in related branches.
    *
    * @param allConcepts some collection of CocneptInstances
    * @return a map of branch root uris and the collection of child uris under that branch root
    */
   static public Map<String, Collection<ConceptInstance>> collateUriConcepts( final Collection<ConceptInstance> allConcepts ) {
      final Map<String,Collection<ConceptInstance>> uriToConcepts = new HashMap<>();
      for ( ConceptInstance concept : allConcepts ) {
         uriToConcepts.computeIfAbsent( concept.getUri(), c -> new ArrayList<>() ).add( concept );
      }
      return collateUriConcepts( uriToConcepts );
   }

   static public Map<String, Collection<ConceptInstance>> collateUriConcepts( final Map<String,Collection<ConceptInstance>> uriToConcepts ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      final Map<String, Collection<String>> uriBranches
            = uriToConcepts.keySet().stream()
                           .collect( Collectors.toMap( Function.identity(), u -> SearchUtil.getBranchUris( graphDb, u ) ) );
      // Map of each uri to its best parent uri
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
      final Map<String, Collection<ConceptInstance>> branchMembers = new HashMap<>();
      for ( Map.Entry<String, String> uriBestRoot : uriBestRootMap.entrySet() ) {
         branchMembers.computeIfAbsent( uriBestRoot.getValue(), u -> new ArrayList<>() )
                      .addAll( uriToConcepts.get( uriBestRoot.getKey() ) );
      }
      return branchMembers;
   }

   static public Map<String, Collection<ConceptInstance>> collateUriConceptsCloseEnough( final Collection<ConceptInstance> allConcepts ) {
      final Map<String,Collection<ConceptInstance>> uriToConcepts = new HashMap<>();
      for ( ConceptInstance concept : allConcepts ) {
         uriToConcepts.computeIfAbsent( concept.getUri(), c -> new ArrayList<>() ).add( concept );
      }
      return collateUriConceptsCloseEnough( uriToConcepts );
   }

   static public Map<String, Collection<ConceptInstance>> collateUriConceptsCloseEnough( final Map<String,Collection<ConceptInstance>> uriToConcepts ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      final Map<String, Collection<String>> uriBranches
            = uriToConcepts.keySet().stream()
                           .collect( Collectors.toMap( Function.identity(), u -> SearchUtil.getBranchUris( graphDb, u ) ) );
      // Map of each uri to its best parent uri
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
      final Map<String, Collection<ConceptInstance>> branchMembers = new HashMap<>();
      for ( Map.Entry<String, String> uriBestRoot : uriBestRootMap.entrySet() ) {
         branchMembers.computeIfAbsent( uriBestRoot.getValue(), u -> new ArrayList<>() )
                      .addAll( uriToConcepts.get( uriBestRoot.getKey() ) );
      }
      if ( branchMembers.size() < 2 ) {
         return branchMembers;
      }

      final Map<String,Collection<ConceptInstance>> mergedBranches = new HashMap<>();
      final List<String> branchUris = new ArrayList<>( branchMembers.keySet() );
      final Collection<String> usedBranches = new ArrayList<>();
      for ( int i=0; i<branchUris.size()-1; i++ ) {
         if ( usedBranches.contains( branchUris.get( i ) ) ) {
            continue;
         }
         for ( int j=i; j<branchUris.size(); j++ ) {
            if ( usedBranches.contains( branchUris.get( j ) ) ) {
               continue;
            }
            final String closeEnough = closeEnough( branchUris.get( i ), branchUris.get( j ) );
            if ( closeEnough != null ) {
               final Collection<ConceptInstance> merge = mergedBranches.computeIfAbsent( closeEnough, u ->  new HashSet<>() );
               merge.addAll( branchMembers.get( branchUris.get( i ) ) );
               usedBranches.add( branchUris.get( i ) );
                  merge.addAll( branchMembers.get( branchUris.get( j ) ) );
                  usedBranches.add( branchUris.get( j ) );
            }
         }
      }
      if ( !mergedBranches.isEmpty() ) {
         branchMembers.keySet().removeAll( usedBranches );
         branchMembers.putAll( mergedBranches );
      }
      return branchMembers;
   }

   private static Map<String, String> HARDCODED_CLOSE_ENOUGH = new HashMap<>();

   static {
      // Synonyms hard coded for now until get more generalized solution using
      // ontology walking that will deal with these cases
      final String axilArea = "our Axillary_Lymph_Node Axilla area";
      final String breastArea = "our Breast area";
      HARDCODED_CLOSE_ENOUGH.put("Axilla", axilArea);
      HARDCODED_CLOSE_ENOUGH.put("Axillary_Lymph_Node", axilArea);
      HARDCODED_CLOSE_ENOUGH.put("Breast", breastArea);
      HARDCODED_CLOSE_ENOUGH.put("Nipple", breastArea);
      HARDCODED_CLOSE_ENOUGH.put("Duct", breastArea);   // TODO consider doing this one only for BrCa
   }
   static private String closeEnough( final String uri1, final String uri2 ) {
      String lookup1 = HARDCODED_CLOSE_ENOUGH.get(uri1);
      if (!uri1.equals(uri2) && lookup1!=null && lookup1.equals(HARDCODED_CLOSE_ENOUGH.get(uri2))) {
         if (uri1.length() > uri2.length()) {
            return uri1;
         } else {
            return uri2;
         }
      }
      final Collection<String> roots1 = Neo4jOntologyConceptUtil.getRootUris( uri1 );
      if ( roots1.size() < 10 ) { // TODO parameterize this, document this, explain why this value chosen
         return null;
      }
      final Collection<String> roots2 = Neo4jOntologyConceptUtil.getRootUris( uri2 );
      if ( roots2.size() < 10 ) {
         return null;
      }
      final Collection<String> join = new HashSet<>( roots1 );
      join.retainAll( roots2 );
      if ( 2d*(double)join.size()/((double)roots1.size() + (double)roots2.size())  > 0.75 ) {  // TODO parameterize this, document this, explain why this value chosen
         return roots1.size() > roots2.size() ? uri1 : uri2;
      }
      return null;
   }


   /**
    * @param jCas                 -
    * @param paragraphAnnotations annotations in paragraph
    */
   static private void makeParagraphRelations( final JCas jCas,
                                               final Collection<IdentifiedAnnotation> paragraphAnnotations,
                                               final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                               final Map<Map<String, Collection<String>>, Collection<String>> relationToSourceMap,
                                               final Collection<IdentifiedAnnotation> located ) {
      if ( paragraphAnnotations.isEmpty() ) {
         return;
      }
      final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap
            = Neo4jOntologyConceptUtil.mapUriAnnotations( paragraphAnnotations );
      final Map<String, Collection<IdentifiedAnnotation>> uriOkLocationMap = new HashMap<>( uriAnnotationMap );
      uriOkLocationMap.keySet().removeAll( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.BODY_MODIFIER ) );
      uriOkLocationMap.keySet().removeAll( UriConstants.getUnwantedAnatomyUris() );

      for ( Map.Entry<Map<String, Collection<String>>, Collection<String>> relationToSources : relationToSourceMap
            .entrySet() ) {
         final Map<String, Collection<String>> collatedSources = collateUris( relationToSources.getValue() );
         for ( Collection<String> sources : collatedSources.values() ) {
            makeUriRelations( jCas, sources, uriAnnotationMap, coveringSentences, relationToSources.getKey(), uriOkLocationMap, located );
         }
      }
   }


   static private void makeUriRelations( final JCas jCas,
                                         final Collection<String> sourceUris,
                                         final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap,
                                         final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                         final Map<String, Collection<String>> uriRelations,
                                         final Map<String, Collection<IdentifiedAnnotation>> uriOkLocationMap,
                                         final Collection<IdentifiedAnnotation> located ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      // relation is a name
      for ( Map.Entry<String, Collection<String>> relation : uriRelations.entrySet() ) {
         final String relationName = relation.getKey();
         final Collection<String> relationTargetUris = relation.getValue();
         Collection<String> targetUris;
         if ( relationName.equals( RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE ) ) {
            targetUris = Neo4jRelationUtil.getRelatableUris( graphDb, uriOkLocationMap.keySet(),
                  relationTargetUris );
         } else {
            targetUris = Neo4jRelationUtil.getRelatableUris( graphDb, uriAnnotationMap.keySet(),
                  relationTargetUris );
         }
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
         createRelations( jCas, coveringSentences, relationName, sourceList, targetList, located );
      }
   }

   static private void createRelations( final JCas jCas,
                                        final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                        final String relationName,
                                        final List<IdentifiedAnnotation> sources,
                                        final List<IdentifiedAnnotation> targets,
                                        final Collection<IdentifiedAnnotation> located ) {
      if ( sources.isEmpty() || targets.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargetMap = new HashMap<>();
      if ( RelationConstants.getReverseRelations().contains( relationName ) ) {
         final List<IdentifiedAnnotation> goodSources = new ArrayList<>( sources.size() );
         for ( IdentifiedAnnotation source : sources ) {
            if ( !located.contains( source ) ) {
               goodSources.add( source );
            }
         }
         sourceTargetMap.putAll( RelationUtil.createReverseAttributeMap( jCas, coveringSentences, goodSources, targets, true ) );
      } else {
         sourceTargetMap.putAll( RelationUtil.createSourceTargetMap( jCas, coveringSentences, sources, targets, true ) );
      }
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
