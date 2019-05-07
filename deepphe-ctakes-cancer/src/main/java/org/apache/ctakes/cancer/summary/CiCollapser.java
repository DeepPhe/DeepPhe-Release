package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceMerger;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/10/2018
 * @deprecated
 */
@Deprecated
final public class CiCollapser {

   static private final Logger LOGGER = Logger.getLogger( "CiCollapser" );


   private CiCollapser() {
   }

   static public Collection<ConceptInstance> collapse( final Collection<ConceptInstance> oldInstances ) {
      final Collection<ConceptInstance> allInstances = new HashSet<>( oldInstances );
      for ( ConceptInstance oldInstance : oldInstances ) {
         oldInstance.getRelated().values().forEach( allInstances::addAll );
//         oldInstance.getReverseRelated().values().forEach( allInstances::addAll );
      }
      // Map of all instances to new collapsed instances.
      final Map<ConceptInstance, ConceptInstance> collapsedMap = createCollapsedMap( allInstances );

      final Map<ConceptInstance, Map<String, Collection<ConceptInstance>>> newRelationsMap = new HashMap<>();
      final Map<ConceptInstance, Map<String, Collection<ConceptInstance>>> newReverseMap = new HashMap<>();
      for ( ConceptInstance oldInstance : oldInstances ) {
         final ConceptInstance mainNewInstance = collapsedMap.get( oldInstance );
         final Map<String, Collection<ConceptInstance>> oldRelations = oldInstance.getRelated();
         for ( Map.Entry<String, Collection<ConceptInstance>> oldRelationType : oldRelations.entrySet() ) {
            final String relationName = oldRelationType.getKey();
            final Collection<ConceptInstance> newRelated
                  = oldRelationType.getValue().stream()
                                   .map( collapsedMap::get )
                                   .distinct()
                                   .collect( Collectors.toList() );
            newRelationsMap.computeIfAbsent( mainNewInstance, c -> new HashMap<>() );
            final Map<String, Collection<ConceptInstance>> newRelations = newRelationsMap.get( mainNewInstance );
            newRelations.computeIfAbsent( relationName, r -> new ArrayList<>() ).addAll( newRelated );
         }
      }

      newRelationsMap.forEach( ConceptInstance::setRelated );

      return oldInstances.stream().map( collapsedMap::get ).distinct().collect( Collectors.toList() );
   }

   /**
    * Collapses a collection of concept instances so that alike concepts become one.
    * @param oldInstances All ConceptInstances in the document(s)
    * @return map of all original concept instances to collapsed ConceptInstances
    */
   static private Map<ConceptInstance, ConceptInstance> createCollapsedMap(
         final Collection<ConceptInstance> oldInstances ) {
      final Map<String, Collection<ConceptInstance>> collatedInstances = collateUriConceptsCloseEnough( oldInstances );

      final Map<ConceptInstance, ConceptInstance> toCollapsedMap = new HashMap<>();
      for ( Collection<ConceptInstance> collatedInstance : collatedInstances.values() ) {
         final ConceptInstance newInstance = collapseConcepts( collatedInstance );
         for ( ConceptInstance oldInstance : collatedInstance ) {
            toCollapsedMap.put( oldInstance, newInstance );
         }
      }
      return toCollapsedMap;
   }

   static public ConceptInstance collapseConcepts( final Collection<ConceptInstance> concepts ) {
      final String patient = concepts.stream()
                                     .map( ConceptInstance::getPatientId )
                                     .distinct()
                                     .sorted()
                                     .collect( Collectors.joining( "_" ) );
      final Map<String, Collection<IdentifiedAnnotation>> docAnnotations
            = ConceptInstanceMerger.mergeDocAnnotations( concepts );
      return ConceptInstanceFactory.createConceptInstance( patient, docAnnotations );
   }

   static public ConceptInstance collapseConcepts( final String uri, final Collection<ConceptInstance> concepts ) {
      final String patient = concepts.stream()
                                       .map( ConceptInstance::getPatientId )
                                       .distinct()
                                       .sorted()
                                       .collect( Collectors.joining( "_" ) );
      final Map<String, Collection<IdentifiedAnnotation>> docAnnotations
            = ConceptInstanceMerger.mergeDocAnnotations( concepts );
      return ConceptInstanceFactory.createConceptInstance( patient, uri, docAnnotations );
   }


   static private void collapseRelations( final Map<ConceptInstance,ConceptInstance> collapsedMap ) {

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
//         LOGGER.error( "uriBestRoot " + uriBestRoot.getKey() + " : " + uriBestRoot.getValue() + " has " + uriToConcepts.get( uriBestRoot.getKey() ).size() );
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
         for ( int j=i+1; j<branchUris.size(); j++ ) {
            if ( usedBranches.contains( branchUris.get( j ) ) ) {
               continue;
            }
            final String closeEnough = UriUtil.getCloseUriRoot( branchUris.get( i ), branchUris.get( j ) );
            if ( closeEnough != null ) {
//               LOGGER.info("Merging closeEnough=" + closeEnough + " with branchMembers for " + branchUris.get( i ) + " with branchMembers for " + branchUris.get( j ) );
//               LOGGER.info("  branchMembers: " + branchMembers.get(branchUris.get(i)).stream().map(ConceptInstance::getUri).collect(Collectors.joining("|")));
//               LOGGER.info("  branchMembers: " + branchMembers.get(branchUris.get(j)).stream().map(ConceptInstance::getUri).collect(Collectors.joining("|")));
               final Collection<ConceptInstance> merge = mergedBranches.computeIfAbsent( closeEnough, u ->  new HashSet<>() );
               merge.addAll( branchMembers.get( branchUris.get( i ) ) );
               usedBranches.add( branchUris.get( i ) );
               // if (!haveSameEntries(i,j) {
               merge.addAll( branchMembers.get( branchUris.get( j ) ) );
               usedBranches.add( branchUris.get( j ) );
               //}
//            } else {
//               LOGGER.info("Not merging branchMembers for " + branchUris.get(i) + " with those for " + branchUris.get(j) );
            }
         }
      }
      if ( !mergedBranches.isEmpty() ) {
         branchMembers.keySet().removeAll( usedBranches );
         branchMembers.putAll( mergedBranches );
      }
      return branchMembers;
   }



}

