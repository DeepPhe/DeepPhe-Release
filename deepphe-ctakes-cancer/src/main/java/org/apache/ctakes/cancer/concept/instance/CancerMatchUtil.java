package org.apache.ctakes.cancer.concept.instance;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.cancer.summary.CiSummaryUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.healthnlp.deepphe.neo4j.RelationConstants.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.HAS_CLINICAL_N;
import static org.healthnlp.deepphe.neo4j.RelationConstants.HAS_PATHOLOGIC_N;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/28/2019
 */
@Immutable
final public class CancerMatchUtil {

   static private final Logger LOGGER = Logger.getLogger( "CancerMatcher" );

   private CancerMatchUtil() {}

   public enum MatchType {
      MATCH,
      MISMATCH,
      EMPTY
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Assign undiagnosed tumors by the best stage, histology and type match.
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////


   /**
    *
    * @param neoplasms -
    * @param diagnosisMap -
    * @param usedInstances -
    * @return true if all instances have been assigned to the diagnosis map as cancers or tumors
    */
   static public boolean matchPrimaryBest( final Collection<ConceptInstance> neoplasms,
                                            final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
                                            final Collection<ConceptInstance> usedInstances ) {
      if ( usedInstances.size() == neoplasms.size() ) {
         return true;
      }
      for ( ConceptInstance neoplasm : neoplasms ) {
         // Do not assign metastases by location
         if ( usedInstances.contains( neoplasm ) ) {
            continue;
         }
         final Collection<ConceptInstance> candidateLocations
               = neoplasm.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            continue;
         }
         final Collection<ConceptInstance> candidateLateralities
               = neoplasm.getRelated().get( HAS_LATERALITY );
         final String candidateHistology = CiSummaryUtil.getHistology( neoplasm.getUri() );
         final String candidateType = CiSummaryUtil.getCancerType( neoplasm.getUri() );

         final TumorCancerMatch matcher = new TumorCancerMatch();
         // Go through all of the cancers and populate a tumor to cancer match object
         for ( Map.Entry<ConceptInstance,Collection<ConceptInstance>> cancerTumors : diagnosisMap.entrySet() ) {
            final ConceptInstance cancer = cancerTumors.getKey();
            final Collection<ConceptInstance> tumors = cancerTumors.getValue();
            final Collection<ConceptInstance> cancerLocations
                  = cancer.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
            if ( !ConceptInstanceUtil.isUriBranchMatch( candidateLocations, cancerLocations ) ) {
               continue;
            }
            matcher._locationMatches.add( cancer );
            if ( ConceptInstanceUtil.anyUriMatch( candidateLateralities,
                  cancer.getRelated().get( HAS_LATERALITY ) ) ) {
               matcher._lateralityMatches.add( cancer );
            }
            if ( isStageMatch( neoplasm, cancer, tumors ) ) {
               matcher._stageMatches.add( cancer );
            }
            if ( isHistologyMatch( candidateHistology, cancer, tumors ) ) {
               matcher._histologyMatches.add( cancer );
            }
            if ( isCancerTypeMatch( candidateType, cancer, tumors ) ) {
               matcher._typeMatches.add( cancer );
            }
         }
         Collection<ConceptInstance> bestMatches = matcher.getBestCancers();
         if ( bestMatches.isEmpty() ) {
            bestMatches = matcher.getCoLocatedCancers();
         }
         if ( !bestMatches.isEmpty() ) {
            bestMatches.stream().map( diagnosisMap::get ).forEach( t -> t.add( neoplasm ) );
            usedInstances.add( neoplasm );
         }
      }
      return usedInstances.size() == neoplasms.size();
   }


   static public boolean matchMetastasisBest( final Collection<ConceptInstance> instances,
                                               final Map<ConceptInstance, Collection<ConceptInstance>> diagnosisMap,
                                               final Collection<ConceptInstance> usedInstances ) {
      if ( usedInstances.size() == instances.size() ) {
         return true;
      }
      for ( ConceptInstance candidate : instances ) {
         // Do not assign metastases by location
         if ( usedInstances.contains( candidate ) ) {
            continue;
         }
         final String candidateHistology = CiSummaryUtil.getHistology( candidate.getUri() );
         final String candidateType = CiSummaryUtil.getCancerType( candidate.getUri() );

         final TumorCancerMatch matcher = new TumorCancerMatch();
         // Go through all of the cancers and populate a tumor to cancer match object
         for ( Map.Entry<ConceptInstance,Collection<ConceptInstance>> cancerTumors : diagnosisMap.entrySet() ) {
            final ConceptInstance cancer = cancerTumors.getKey();
            final Collection<ConceptInstance> tumors = cancerTumors.getValue();
            if ( isStageMatch( candidate, cancer, tumors ) ) {
               matcher._stageMatches.add( cancer );
            }
            if ( isHistologyMatch( candidateHistology, cancer, tumors ) ) {
               matcher._histologyMatches.add( cancer );
            }
            if ( isCancerTypeMatch( candidateType, cancer, tumors ) ) {
               matcher._typeMatches.add( cancer );
            }
         }
         final Collection<ConceptInstance> bestMatches = matcher.getBestCancers();
         if ( !bestMatches.isEmpty() ) {
            bestMatches.stream().map( diagnosisMap::get ).forEach( t -> t.add( candidate ) );
            usedInstances.add( candidate );
         }
      }
      return usedInstances.size() == instances.size();
   }

   static private boolean isStageMatch( final ConceptInstance candidate,
                                        final ConceptInstance cancer,
                                        final Collection<ConceptInstance> tumors ) {
      if ( isStageMatch( candidate, cancer ) ) {
         return true;
      }
      return isStageMatch( candidate, tumors );
   }

   static public boolean isStageMatch( final ConceptInstance candidate,
                                       final Collection<ConceptInstance> tumors ) {
      return tumors.stream().anyMatch( t -> isStageMatch( candidate, t ) );
   }

   static public boolean countStageMatch( final ConceptInstance candidate,
                                          final Collection<ConceptInstance> tumors ) {
      return tumors.stream().anyMatch( t -> isStageMatch( candidate, t ) );
   }

   static private boolean isStageMatch( final ConceptInstance candidate, final ConceptInstance cancerTumor ) {
      final Map<String,Collection<ConceptInstance>> related = candidate.getRelated();
      final Collection<ConceptInstance> t
            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
      final Collection<ConceptInstance> n
            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
      return isStageMatch( related.get( HAS_STAGE ), t, n, cancerTumor );
   }

   static public MatchType getTnmMatchType( final ConceptInstance candidate,
                                            final Collection<ConceptInstance> tumors ) {
      final Map<String,Collection<ConceptInstance>> related = candidate.getRelated();
      final Collection<ConceptInstance> t
            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
      final Collection<ConceptInstance> n
            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
      MatchType tnmMatch = MatchType.MISMATCH;
      for ( ConceptInstance tumor : tumors ) {
         final MatchType match = getTnmMatchType( t, n, tumor );
         if ( match == MatchType.EMPTY ) {
            tnmMatch = MatchType.EMPTY;
         } else if ( match == MatchType.MATCH ) {
            return MatchType.MATCH;
         }
      }
      return tnmMatch;
   }

   static public int getTnmMatchCount( final ConceptInstance candidate,
                                            final Collection<ConceptInstance> tumors ) {
      final Map<String,Collection<ConceptInstance>> related = candidate.getRelated();
      final Collection<ConceptInstance> t
            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
      final Collection<ConceptInstance> n
            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
      int matchCount = 0;
      for ( ConceptInstance tumor : tumors ) {
         final MatchType match = getTnmMatchType( t, n, tumor );
         if ( match == MatchType.MATCH ) {
            matchCount++;
         }
      }
      return matchCount;
   }

   static private MatchType getTnmMatchType( final Collection<ConceptInstance> tnmTs,
                                             final Collection<ConceptInstance> tnmNs,
                                             final ConceptInstance cancerTumor ) {
      final Map<String,Collection<ConceptInstance>> related = cancerTumor.getRelated();
      boolean haveEmptyT = tnmTs == null || tnmTs.isEmpty();
      MatchType tMatch = haveEmptyT ? MatchType.EMPTY : MatchType.MISMATCH;
      if ( !haveEmptyT ) {
         final Collection<ConceptInstance> cancerTs
               = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
         if ( cancerTs == null || cancerTs.isEmpty() ) {
            tMatch = MatchType.EMPTY;
         } else if ( ConceptInstanceUtil.isUriBranchMatch( tnmTs, cancerTs ) ) {
            tMatch = MatchType.MATCH;
         }
      }
      if ( tMatch == MatchType.MISMATCH ) {
         return MatchType.MISMATCH;
      }
      if ( tnmNs == null || tnmNs.isEmpty() ) {
         return tMatch;
      }
      final Collection<ConceptInstance> cancerNs
            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
      if ( cancerNs == null || cancerNs.isEmpty() ) {
         return tMatch;
      } else if ( ConceptInstanceUtil.isUriBranchMatch( tnmNs, cancerNs ) ) {
         return MatchType.MATCH;
      }
      return MatchType.MISMATCH;
   }


   static private Collection<ConceptInstance> getTNM( final Map<String,Collection<ConceptInstance>> related,
                                                      final String pName,
                                                      final String cName ) {
      final Collection<ConceptInstance> tnm = new ArrayList<>();
      final Collection<ConceptInstance> p = related.get( pName );
      final Collection<ConceptInstance> c = related.get( cName );
      if ( p != null ) {
         tnm.addAll( p );
      }
      if ( c != null ) {
         tnm.addAll( c );
      }
      if ( !tnm.isEmpty() ) {
         return tnm;
      }
      return null;
   }

   static private boolean isStageMatch( final Collection<ConceptInstance> stages,
                                        final Collection<ConceptInstance> tnmTs,
                                        final Collection<ConceptInstance> tnmNs,
                                        final ConceptInstance cancerTumor ) {
      final Map<String,Collection<ConceptInstance>> related = cancerTumor.getRelated();
      if ( ConceptInstanceUtil.isUriBranchMatch( stages, related.get( HAS_STAGE ) ) ) {
         // A matching summary stage is good enough
         return true;
      }
      final Collection<ConceptInstance> cancerTs
            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
      if ( tnmTs != null && cancerTs != null && !ConceptInstanceUtil.isUriBranchMatch( tnmTs, cancerTs ) ) {
         // If T doesn't match then don't bother checking N
         return false;
      }
      final Collection<ConceptInstance> cancerNs
            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
      if ( tnmNs == null && cancerNs == null ) {
         // T matched, no Ns
         return true;
      }
      return ConceptInstanceUtil.isUriBranchMatch( tnmNs, cancerNs );
      //  Don't bother with M
   }



   static private boolean isHistologyMatch( final String candidateHistology,
                                            final ConceptInstance cancer,
                                            final Collection<ConceptInstance> tumors ) {
      if ( candidateHistology.isEmpty() ) {
         return false;
      }
      if ( candidateHistology.equals( CiSummaryUtil.getHistology( cancer.getUri() ) ) ) {
         return true;
      }
      return tumors.stream()
                   .map( ConceptInstance::getUri )
                   .map( CiSummaryUtil::getHistology )
                   .anyMatch( candidateHistology::equals );
   }

   static public MatchType getHistologyMatchType( final ConceptInstance candidate, final Collection<ConceptInstance> tumors ) {
      final String candidateHistology = CiSummaryUtil.getHistology( candidate.getUri() );
      if ( candidateHistology.isEmpty() ) {
         return MatchType.EMPTY;
      }
      boolean haveEmpty = false;
      for ( ConceptInstance tumor : tumors ) {
         final String histology = CiSummaryUtil.getHistology( tumor.getUri() );
         if ( histology.isEmpty() ) {
            haveEmpty = true;
         } else if ( histology.equals( candidateHistology ) ) {
            return MatchType.MATCH;
         }
      }
      return haveEmpty ? MatchType.EMPTY : MatchType.MISMATCH;
   }

   static private boolean isCancerTypeMatch( final String candidateType,
                                             final ConceptInstance cancer,
                                             final Collection<ConceptInstance> tumors ) {
      if ( candidateType.isEmpty() ) {
         return false;
      }
      if ( candidateType.equals( CiSummaryUtil.getCancerType( cancer.getUri() ) ) ) {
         return true;
      }
      return tumors.stream()
                   .map( ConceptInstance::getUri )
                   .map( CiSummaryUtil::getCancerType )
                   .anyMatch( candidateType::equals );
   }

   static public MatchType getCancerTypeMatchType( final ConceptInstance candidate,
                                                   final Collection<ConceptInstance> tumors ) {
      final String candidateType = CiSummaryUtil.getCancerType( candidate.getUri() );
      if ( candidateType.isEmpty() ) {
         return MatchType.EMPTY;
      }
      boolean haveEmpty = false;
      for ( ConceptInstance tumor : tumors ) {
         final String cancerType = CiSummaryUtil.getCancerType( tumor.getUri() );
         if ( cancerType.isEmpty() ) {
            haveEmpty = true;
         } else if ( cancerType.equals( candidateType ) ) {
            return MatchType.MATCH;
         }
      }
      return haveEmpty ? MatchType.EMPTY : MatchType.MISMATCH;
   }




   static private final class TumorCancerMatch {
      private Collection<ConceptInstance> _locationMatches = new ArrayList<>();
      private Collection<ConceptInstance> _lateralityMatches = new ArrayList<>();
      private Collection<ConceptInstance> _stageMatches = new ArrayList<>();
      private Collection<ConceptInstance> _histologyMatches = new ArrayList<>();
      private Collection<ConceptInstance> _typeMatches = new ArrayList<>();
      private TumorCancerMatch() {}

      private Collection<ConceptInstance> getCoLocatedCancers() {
         Collection<ConceptInstance> bestMatches = getBestMatches( _locationMatches, _lateralityMatches );
         if ( bestMatches.isEmpty() ) {
            bestMatches = getBestMatches( _locationMatches );
         }
         return bestMatches;
      }

      private Collection<ConceptInstance> getBestCancers() {
         Collection<ConceptInstance> bestMatches = getBestMatches( _stageMatches, _histologyMatches, _typeMatches );
         if ( bestMatches.isEmpty() ) {
            bestMatches = getBestMatches( _stageMatches, _histologyMatches );
         }
         if ( bestMatches.isEmpty() ) {
            bestMatches = getBestMatches( _stageMatches, _typeMatches );
         }
         if ( bestMatches.isEmpty() ) {
            bestMatches = getBestMatches( _histologyMatches, _typeMatches );
         }
         if ( bestMatches.isEmpty() ) {
            bestMatches = getBestMatches( _stageMatches );
         }
         if ( bestMatches.isEmpty() ) {
            bestMatches = getBestMatches( _histologyMatches );
         }
         if ( bestMatches.isEmpty() ) {
            bestMatches = getBestMatches( _typeMatches );
         }
         return bestMatches;
      }

      @SafeVarargs
      static private Collection<ConceptInstance> getBestMatches( final Collection<ConceptInstance> ... matches ) {
         final Collection<ConceptInstance> bestMatches = new ArrayList<>( matches[ 0 ] );
         for ( int i=1; i<matches.length; i++ ) {
            bestMatches.retainAll( matches[ i ] );
            if ( bestMatches.isEmpty() ) {
               break;
            }
         }
         return bestMatches;
      }
   }


}
