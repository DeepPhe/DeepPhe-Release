package org.apache.ctakes.cancer.concept.instance;

import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/6/2018
 */
final public class ConceptInstanceMerger {

   static private final Logger LOGGER = Logger.getLogger( "ConceptInstanceMerger" );

   private ConceptInstanceMerger() {
   }


   /**
    * Use relations and attributes to combine ConceptInstances
    *
    * @param oldInstances -
    * @return -
    */
   static public Map<String, Collection<ConceptInstance>> createMergedInstances(
         final Collection<ConceptInstance> oldInstances ) {
      final Map<String, Collection<ConceptInstance>> uriInstances = new HashMap<>();
      for ( ConceptInstance conceptInstance : oldInstances ) {
         uriInstances.computeIfAbsent( conceptInstance.getUri(), c -> new ArrayList<>() ).add( conceptInstance );
      }
      return createMergedInstances( uriInstances );
   }

   /**
    * Use relations and attributes to combine ConceptInstances
    *
    * @param oldUriInstances -
    * @return -
    */
   static public Map<String, Collection<ConceptInstance>> createMergedInstances(
         final Map<String, Collection<ConceptInstance>> oldUriInstances ) {
      LOGGER.info( "Merging Concept Instances ..." );
      final Map<String, Collection<String>> uriBranches
            = oldUriInstances.keySet().stream()
                             .collect( Collectors
                                   .toMap( Function.identity(), Neo4jOntologyConceptUtil::getBranchUris ) );
      // Map of neg_unc tag to uriBanch to CIs with that branch
      final Map<String, Map<String, Collection<ConceptInstance>>> negUncUriInstances = new HashMap<>();
      for ( Map.Entry<String, Collection<ConceptInstance>> entry : oldUriInstances.entrySet() ) {
         final String uri = entry.getKey();
         for ( ConceptInstance conceptInstance : entry.getValue() ) {
            final String dtr = conceptInstance.getDocTimeRel();
            // tag groups alike negation, uncertain, plus begin vs. after vs. [begin/overlap and overlap]
            final String tag = Boolean.toString( conceptInstance.isNegated() )
                               + '_' + Boolean.toString( conceptInstance.isUncertain() )
                               + '_' + (dtr.length() >= 5 ? dtr.substring( dtr.length() - 5 ).toUpperCase() : "")
                               + '_' + conceptInstance.getSubject();
            negUncUriInstances.computeIfAbsent( tag, c -> new HashMap<>() )
                              .computeIfAbsent( uri, c -> new ArrayList<>() )
                              .add( conceptInstance );
         }
      }
      final Map<String, Collection<ConceptInstance>> newUriInstances = new HashMap<>();
      final Map<ConceptInstance, ConceptInstance> oldToNewInstances = new HashMap<>();

      for ( Map<String, Collection<ConceptInstance>> uriInstances : negUncUriInstances.values() ) {
         final Map<Integer, Collection<String>> depthMap = new HashMap<>();
         for ( String uriKey : uriInstances.keySet() ) {
            final Collection<String> branch = uriBranches.get( uriKey );
            depthMap.computeIfAbsent( branch.size(), c -> new ArrayList<>() ).add( uriKey );
         }
         final List<Integer> depths = new ArrayList<>( depthMap.keySet() );
         depths.sort( Integer::compareTo );
         final Collection<String> removalUris = new ArrayList<>();
         final Map<String, Collection<String>> bestUris = new HashMap<>();
         for ( int i = 0; i < depths.size() - 1; i++ ) {
            for ( String smallUri : depthMap.get( depths.get( i ) ) ) {
               if ( removalUris.contains( smallUri ) ) {
                  continue;
               }
               final Collection<String> smallBranch = uriBranches.get( smallUri );
               for ( int j = i + 1; j < depths.size(); j++ ) {
                  for ( String bigUri : depthMap.get( depths.get( j ) ) ) {
                     if ( removalUris.contains( bigUri ) ) {
                        continue;
                     }
                     final Collection<String> bigBranch = uriBranches.get( bigUri );
                     if ( bigBranch.containsAll( smallBranch ) ) {
                        bestUris.computeIfAbsent( smallUri, c -> new ArrayList<>() ).add( bigUri );
                        removalUris.add( bigUri );
                     }
                  }
               }
            }
         }
         for ( String uri : uriBranches.keySet() ) {
            if ( !removalUris.contains( uri ) ) {
               bestUris.put( uri, Collections.singletonList( uri ) );
            }
         }
         // Now have a map of the best uris to all of their parent uris for concept instances with the same negation and uncertainty
         for ( Map.Entry<String, Collection<String>> bestEntry : bestUris.entrySet() ) {
            final String bestUri = bestEntry.getKey();
            final Collection<ConceptInstance> oldInstances1 = uriInstances.get( bestUri );
            if ( oldInstances1 == null ) {
               LOGGER.error( "No concept instances for " + bestUri + " in " +
                             uriBranches.keySet().stream().collect( Collectors.joining( "," ) ) );
               continue;
            }
            final Collection<ConceptInstance> oldInstances = new ArrayList<>( oldInstances1 );
            final String patientId = oldInstances.stream().map( ConceptInstance::getPatientId ).findAny()
                                                 .orElse( "Generic" );
            bestEntry.getValue().stream()
                     .map( uriInstances::get ).forEach( oldInstances::addAll );

            final Map<String, Collection<IdentifiedAnnotation>> docAnnotations = mergeDocAnnotations( oldInstances );
            final ConceptInstance newInstance
                  = ConceptInstanceFactory.createConceptInstance( patientId, bestUri, docAnnotations );
            newUriInstances.computeIfAbsent( bestUri, c -> new ArrayList<>() ).add( newInstance );
            oldInstances.forEach( ci -> oldToNewInstances.put( ci, newInstance ) );
         }
      }
      for ( Map.Entry<ConceptInstance, ConceptInstance> oldNewEntry : oldToNewInstances.entrySet() ) {
         final ConceptInstance oldInstance = oldNewEntry.getKey();
         final ConceptInstance newInstance = oldNewEntry.getValue();
         for ( Map.Entry<String, Collection<ConceptInstance>> oldRelated : oldInstance.getRelated().entrySet() ) {
            final String type = oldRelated.getKey();
            for ( ConceptInstance oldRelatedCi : oldRelated.getValue() ) {
               newInstance.addRelated( type, oldToNewInstances.get( oldRelatedCi ) );
            }
         }
      }
      return newUriInstances;
   }


   static public Map<String, Collection<IdentifiedAnnotation>> mergeDocAnnotations(
         final Collection<ConceptInstance> concepts ) {
      final Map<String, Collection<IdentifiedAnnotation>> docAnnotations = new HashMap<>();
      for ( ConceptInstance conceptInstance : concepts ) {
         for ( IdentifiedAnnotation annotation : conceptInstance.getAnnotations() ) {
            final String documentId = conceptInstance.getDocumentId( annotation );
            final Collection<IdentifiedAnnotation> annotations
                  = docAnnotations.computeIfAbsent( documentId, d -> new ArrayList<>() );
            annotations.add( annotation );
         }
      }
      return docAnnotations;
   }

}
