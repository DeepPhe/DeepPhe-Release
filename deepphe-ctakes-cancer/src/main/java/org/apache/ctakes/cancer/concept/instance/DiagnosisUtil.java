package org.apache.ctakes.cancer.concept.instance;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.cancer.concept.instance.ConceptInstanceUtil.NeoplasmType.CANCER;
import static org.apache.ctakes.cancer.concept.instance.ConceptInstanceUtil.NeoplasmType.PRIMARY;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/28/2019
 */
@Immutable
final public class DiagnosisUtil {

   static private final Logger LOGGER = Logger.getLogger( "DiagnosisUtil" );


   private DiagnosisUtil() {}




   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Create Cancer -to- Tumor Diagnosis Concept Instances Entry Point
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////






   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Separate Cancers from Tumors by Diagnosis; Concept Instances
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////


   /**
    * Creates a tree of diagnoses, populating diagnosisMap simply by declared diagnoses.
    * @param neoplasms neoplasm instances
    * @param diagnosisMap map of primaries to collection of associated neoplasms.  Empty.
    * @param diagnoses map of neoplasms to their diagnoses, one level.
    */
   static private void getDeepDiagnoses( final Collection<ConceptInstance> neoplasms,
                                         final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
                                         final Map<ConceptInstance,Collection<ConceptInstance>> diagnoses ) {
      // create a map of each neoplasm to its best primary
      final Map<ConceptInstance,ConceptInstance> bestPrimaries = new HashMap<>( neoplasms.size() );
      for ( ConceptInstance neoplasm : neoplasms ) {
         final Collection<ConceptInstance> checkedNeoplasms = new ArrayList<>();
         getDeepDiagnosis( neoplasm, diagnoses, bestPrimaries, checkedNeoplasms );
      }
      // map from neoplasm to primary to primary with neoplasms
      for ( Map.Entry<ConceptInstance,ConceptInstance> bestPrimary : bestPrimaries.entrySet() ) {
         diagnosisMap.computeIfAbsent( bestPrimary.getValue(), p -> new HashSet<>() ).add( bestPrimary.getKey() );
      }
   }


   static private ConceptInstance getDeepDiagnosis( final ConceptInstance neoplasm,
                                                    final Map<ConceptInstance,Collection<ConceptInstance>> diagnoses,
                                                    final Map<ConceptInstance,ConceptInstance> bestPrimaries,
                                                    final Collection<ConceptInstance> currentBranch ) {
      final ConceptInstance alreadyKnown = bestPrimaries.get( neoplasm );
      if ( alreadyKnown != null ) {
         // We already have a best primary for this instance.
         return alreadyKnown;
      }
      final Collection<ConceptInstance> neoplasmDiagnoses = diagnoses.get( neoplasm );
      if ( neoplasmDiagnoses == null || neoplasmDiagnoses.isEmpty()
           || ( neoplasmDiagnoses.size() == 1 && neoplasmDiagnoses.contains( neoplasm ) ) ) {
         // the primary has no diagnoses, so it is the end of this little journey.
         bestPrimaries.put( neoplasm, neoplasm );
         return neoplasm;
      }
      if ( neoplasmDiagnoses.size() > 1 ) {
         // Multiple diagnoses.  Follow each one and get its deepest diagnosis.
         final Collection<ConceptInstance> deepDiagnoses = new HashSet<>();
         for ( ConceptInstance diagnosis : neoplasmDiagnoses ) {
            if ( currentBranch.contains( diagnosis ) ) {
               deepDiagnoses.add( neoplasm );
               continue;
            }
            currentBranch.add( diagnosis );
            final ConceptInstance deepCancer
                  = getDeepDiagnosis( diagnosis, diagnoses, bestPrimaries, currentBranch );
            deepDiagnoses.add( deepCancer );
         }
         final ConceptInstance bestPrimary = getBestPrimary( deepDiagnoses );
         bestPrimaries.put( neoplasm, bestPrimary );
         return bestPrimary;
      }
      final ConceptInstance singleDiagnosis = new ArrayList<>( neoplasmDiagnoses ).get( 0 );
      if ( currentBranch.contains( singleDiagnosis ) ) {
         // This neoplasm points to a diagnosis higher in the tree.  mass -> actinic -> melanoma -> actinic
         bestPrimaries.put( neoplasm, neoplasm );
         bestPrimaries.put( singleDiagnosis, neoplasm );
         return neoplasm;
      }
      currentBranch.add( neoplasm );
      currentBranch.add( singleDiagnosis );
      final ConceptInstance deepDiagnosis
            = getDeepDiagnosis( singleDiagnosis, diagnoses, bestPrimaries, currentBranch );
      bestPrimaries.put( neoplasm, deepDiagnosis );
      return deepDiagnosis;
   }

   static private ConceptInstance getBestPrimary( final Collection<ConceptInstance> candidates  ) {
      if ( candidates.size() == 1 ) {
         return new ArrayList<>( candidates ).get( 0 );
      }
      Collection<ConceptInstance> bestCandidates = candidates;
      final Collection<ConceptInstance> bestNotEmpty
            = bestCandidates.stream()
                            .filter( DiagnosisUtil::hasWantedRelations )
                            .collect( Collectors.toList());
      if ( !bestNotEmpty.isEmpty() ) {
         bestCandidates = bestNotEmpty;
         if ( bestCandidates.size() == 1 ) {
            return new ArrayList<>( bestCandidates ).get( 0 );
         }
      }
      final Collection<ConceptInstance> primaryCandidates
            = bestCandidates.stream()
                            .filter( c -> PRIMARY == ConceptInstanceUtil.getNeoplasmType( c ) )
                            .collect( Collectors.toList());
      if ( !primaryCandidates.isEmpty() ) {
         bestCandidates = primaryCandidates;
         if ( bestCandidates.size() == 1 ) {
            return new ArrayList<>( bestCandidates ).get( 0 );
         }
      }
      final Collection<ConceptInstance> mostSpecific
            = ConceptInstanceUtil.getMostSpecificInstances( bestCandidates );
      return new ArrayList<>( mostSpecific ).get( 0 );
   }

   static private final Collection<String> UNNECESSARY_RELATIONS
         = Arrays.asList( HAS_TUMOR_EXTENT, HAS_TUMOR_TYPE );

   static private boolean hasWantedRelations( final ConceptInstance neoplasm ) {
      final Map<String,Collection<ConceptInstance>> related = neoplasm.getRelated();
      if ( related.size() > 2 ) {
         return true;
      }
      if ( related.size() < 2 ) {
         return false;
      }
      return related.keySet().stream().anyMatch( r -> !UNNECESSARY_RELATIONS.contains( r ) );
   }







   /**
    * Add unused Cancers
    * @param instances -
   //    * @param neoplasmTypes -
    * @param diagnosisMap -
    * @param usedInstances -
    */
   static private void addUnusedCancers( final Collection<ConceptInstance> instances,
                                         final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
                                         final Collection<ConceptInstance> unLocatedCancers,
                                         final Collection<ConceptInstance> usedInstances ) {
      addUnusedNeoplasms( instances, CANCER, diagnosisMap, unLocatedCancers, usedInstances );
   }

   /**
    * Add unused Primaries as Cancers
    * @param instances -
   //    * @param neoplasmTypes -
    * @param diagnosisMap -
    * @param usedInstances -
    */
   static private void addUnusedPrimaries( final Collection<ConceptInstance> instances,
                                           final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
                                           final Collection<ConceptInstance> unLocatedCancers,
                                           final Collection<ConceptInstance> usedInstances ) {
      addUnusedNeoplasms( instances, PRIMARY, diagnosisMap, unLocatedCancers, usedInstances );
   }

   /**
    * Add unused Neoplasms as Cancers
    * @param instances -
    * @param neoplasmType -
    * @param diagnosisMap -
    * @param usedInstances -
    */
   static private void addUnusedNeoplasms( final Collection<ConceptInstance> instances,
                                           final ConceptInstanceUtil.NeoplasmType neoplasmType,
                                           final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
                                           final Collection<ConceptInstance> unLocatedCancers,
                                           final Collection<ConceptInstance> usedInstances ) {
      for ( ConceptInstance instance : instances ) {
         if ( !usedInstances.contains( instance ) ) {
            final Collection<ConceptInstance> locations = instance.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
            if ( locations == null || locations.isEmpty() ) {
               unLocatedCancers.add( instance );
            } else {
               diagnosisMap.put( instance, new HashSet<>() );
            }
            usedInstances.add( instance );
         }
      }
   }

}
