package org.healthnlp.deepphe.util;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;


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


//   /**
//    *
//    * @param neoplasms -
//    * @param diagnosisMap -
//    * @param usedInstances -
//    * @return true if all instances have been assigned to the diagnosis map as cancers or tumors
//    */
//   static public boolean matchPrimaryBest( final Collection<ConceptAggregate> neoplasms,
//                                            final Map<ConceptAggregate, Collection<ConceptAggregate>> diagnosisMap,
//                                            final Collection<ConceptAggregate> usedInstances ) {
//      if ( usedInstances.size() == neoplasms.size() ) {
//         return true;
//      }
//      for ( ConceptAggregate neoplasm : neoplasms ) {
//         // Do not assign metastases by location
//         if ( usedInstances.contains( neoplasm ) ) {
//            continue;
//         }
//         final Collection<ConceptAggregate> candidateLocations = neoplasm.getRelatedSites();
//         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
//            // no location to use for orphan.
//            continue;
//         }
//         final Collection<ConceptAggregate> candidateLateralities = neoplasm.getRelated( HAS_LATERALITY );
//         final String candidateHistology = CiSummaryUtil.getHistology( neoplasm.getUri() );
//         final String candidateType = CiSummaryUtil.getCancerType( neoplasm.getUri() );
//
//         final TumorCancerMatch matcher = new TumorCancerMatch();
//         // Go through all of the cancers and populate a tumor to cancer match object
//         for ( Map.Entry<ConceptAggregate,Collection<ConceptAggregate>> cancerTumors : diagnosisMap.entrySet() ) {
//            final ConceptAggregate cancer = cancerTumors.getKey();
//            final Collection<ConceptAggregate> tumors = cancerTumors.getValue();
//            final Collection<ConceptAggregate> cancerLocations = cancer.getRelatedSites();
//            if ( !ConceptAggregateUtil.isUriBranchMatch( candidateLocations, cancerLocations ) ) {
//               continue;
//            }
//            matcher._locationMatches.add( cancer );
//            if ( ConceptAggregateUtil.anyUriMatch( candidateLateralities,
//                  cancer.getRelated( HAS_LATERALITY ) ) ) {
//               matcher._lateralityMatches.add( cancer );
//            }
//            if ( isStageMatch( neoplasm, cancer, tumors ) ) {
//               matcher._stageMatches.add( cancer );
//            }
//            if ( isHistologyMatch( candidateHistology, cancer, tumors ) ) {
//               matcher._histologyMatches.add( cancer );
//            }
//            if ( isCancerTypeMatch( candidateType, cancer, tumors ) ) {
//               matcher._typeMatches.add( cancer );
//            }
//         }
//         Collection<ConceptAggregate> bestMatches = matcher.getBestCancers();
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = matcher.getCoLocatedCancers();
//         }
//         if ( !bestMatches.isEmpty() ) {
//            bestMatches.stream().map( diagnosisMap::get ).forEach( t -> t.add( neoplasm ) );
//            usedInstances.add( neoplasm );
//         }
//      }
//      return usedInstances.size() == neoplasms.size();
//   }
//
//
//   static public boolean matchMetastasisBest( final Collection<ConceptAggregate> instances,
//                                               final Map<ConceptAggregate, Collection<ConceptAggregate>> diagnosisMap,
//                                               final Collection<ConceptAggregate> usedInstances ) {
//      if ( usedInstances.size() == instances.size() ) {
//         return true;
//      }
//      for ( ConceptAggregate candidate : instances ) {
//         // Do not assign metastases by location
//         if ( usedInstances.contains( candidate ) ) {
//            continue;
//         }
//         final String candidateHistology = CiSummaryUtil.getHistology( candidate.getUri() );
//         final String candidateType = CiSummaryUtil.getCancerType( candidate.getUri() );
//
//         final TumorCancerMatch matcher = new TumorCancerMatch();
//         // Go through all of the cancers and populate a tumor to cancer match object
//         for ( Map.Entry<ConceptAggregate,Collection<ConceptAggregate>> cancerTumors : diagnosisMap.entrySet() ) {
//            final ConceptAggregate cancer = cancerTumors.getKey();
//            final Collection<ConceptAggregate> tumors = cancerTumors.getValue();
//            if ( isStageMatch( candidate, cancer, tumors ) ) {
//               matcher._stageMatches.add( cancer );
//            }
//            if ( isHistologyMatch( candidateHistology, cancer, tumors ) ) {
//               matcher._histologyMatches.add( cancer );
//            }
//            if ( isCancerTypeMatch( candidateType, cancer, tumors ) ) {
//               matcher._typeMatches.add( cancer );
//            }
//         }
//         final Collection<ConceptAggregate> bestMatches = matcher.getBestCancers();
//         if ( !bestMatches.isEmpty() ) {
//            bestMatches.stream().map( diagnosisMap::get ).forEach( t -> t.add( candidate ) );
//            usedInstances.add( candidate );
//         }
//      }
//      return usedInstances.size() == instances.size();
//   }
//
//   static private boolean isStageMatch( final ConceptAggregate candidate,
//                                        final ConceptAggregate cancer,
//                                        final Collection<ConceptAggregate> tumors ) {
//      if ( isStageMatch( candidate, cancer ) ) {
//         return true;
//      }
//      return isStageMatch( candidate, tumors );
//   }
//
//   static public boolean isStageMatch( final ConceptAggregate candidate,
//                                       final Collection<ConceptAggregate> tumors ) {
//      return tumors.stream().anyMatch( t -> isStageMatch( candidate, t ) );
//   }
//
//   static public boolean countStageMatch( final ConceptAggregate candidate,
//                                          final Collection<ConceptAggregate> tumors ) {
//      return tumors.stream().anyMatch( t -> isStageMatch( candidate, t ) );
//   }
//
//   static private boolean isStageMatch( final ConceptAggregate candidate, final ConceptAggregate cancerTumor ) {
//      final Map<String,Collection<ConceptAggregate>> related = candidate.getRelatedConceptMap();
//      final Collection<ConceptAggregate> t
//            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
//      final Collection<ConceptAggregate> n
//            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
//      return isStageMatch( related.get( HAS_STAGE ), t, n, cancerTumor );
//   }
//
//   static public MatchType getTnmMatchType( final ConceptAggregate candidate,
//                                            final Collection<ConceptAggregate> tumors ) {
//      final Map<String,Collection<ConceptAggregate>> related = candidate.getRelatedConceptMap();
//      final Collection<ConceptAggregate> t
//            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
//      final Collection<ConceptAggregate> n
//            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
//      MatchType tnmMatch = MatchType.MISMATCH;
//      for ( ConceptAggregate tumor : tumors ) {
//         final MatchType match = getTnmMatchType( t, n, tumor );
//         if ( match == MatchType.EMPTY ) {
//            tnmMatch = MatchType.EMPTY;
//         } else if ( match == MatchType.MATCH ) {
//            return MatchType.MATCH;
//         }
//      }
//      return tnmMatch;
//   }
//
//   static public int getTnmMatchCount( final ConceptAggregate candidate,
//                                            final Collection<ConceptAggregate> tumors ) {
//      final Map<String,Collection<ConceptAggregate>> related = candidate.getRelatedConceptMap();
//      final Collection<ConceptAggregate> t
//            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
//      final Collection<ConceptAggregate> n
//            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
//      int matchCount = 0;
//      for ( ConceptAggregate tumor : tumors ) {
//         final MatchType match = getTnmMatchType( t, n, tumor );
//         if ( match == MatchType.MATCH ) {
//            matchCount++;
//         }
//      }
//      return matchCount;
//   }
//
//   static private MatchType getTnmMatchType( final Collection<ConceptAggregate> tnmTs,
//                                             final Collection<ConceptAggregate> tnmNs,
//                                             final ConceptAggregate cancerTumor ) {
//      final Map<String,Collection<ConceptAggregate>> related = cancerTumor.getRelatedConceptMap();
//      boolean haveEmptyT = tnmTs == null || tnmTs.isEmpty();
//      MatchType tMatch = haveEmptyT ? MatchType.EMPTY : MatchType.MISMATCH;
//      if ( !haveEmptyT ) {
//         final Collection<ConceptAggregate> cancerTs
//               = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
//         if ( cancerTs == null || cancerTs.isEmpty() ) {
//            tMatch = MatchType.EMPTY;
//         } else if ( ConceptAggregateUtil.isUriBranchMatch( tnmTs, cancerTs ) ) {
//            tMatch = MatchType.MATCH;
//         }
//      }
//      if ( tMatch == MatchType.MISMATCH ) {
//         return MatchType.MISMATCH;
//      }
//      if ( tnmNs == null || tnmNs.isEmpty() ) {
//         return tMatch;
//      }
//      final Collection<ConceptAggregate> cancerNs
//            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
//      if ( cancerNs == null || cancerNs.isEmpty() ) {
//         return tMatch;
//      } else if ( ConceptAggregateUtil.isUriBranchMatch( tnmNs, cancerNs ) ) {
//         return MatchType.MATCH;
//      }
//      return MatchType.MISMATCH;
//   }
//
//
//   static private Collection<ConceptAggregate> getTNM( final Map<String,Collection<ConceptAggregate>> related,
//                                                      final String pName,
//                                                      final String cName ) {
//      final Collection<ConceptAggregate> tnm = new ArrayList<>();
//      final Collection<ConceptAggregate> p = related.get( pName );
//      final Collection<ConceptAggregate> c = related.get( cName );
//      if ( p != null ) {
//         tnm.addAll( p );
//      }
//      if ( c != null ) {
//         tnm.addAll( c );
//      }
//      if ( !tnm.isEmpty() ) {
//         return tnm;
//      }
//      return null;
//   }

//   static private boolean isStageMatch( final Collection<ConceptAggregate> stages,
//                                        final Collection<ConceptAggregate> tnmTs,
//                                        final Collection<ConceptAggregate> tnmNs,
//                                        final ConceptAggregate cancerTumor ) {
//      final Map<String,Collection<ConceptAggregate>> related = cancerTumor.getRelatedConceptMap();
//      if ( ConceptAggregateUtil.isUriBranchMatch( stages, related.get( HAS_STAGE ) ) ) {
//         // A matching summary stage is good enough
//         return true;
//      }
//      final Collection<ConceptAggregate> cancerTs
//            = getTNM( related, HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
//      if ( tnmTs != null && cancerTs != null && !ConceptAggregateUtil.isUriBranchMatch( tnmTs, cancerTs ) ) {
//         // If T doesn't match then don't bother checking N
//         return false;
//      }
//      final Collection<ConceptAggregate> cancerNs
//            = getTNM( related, HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
//      if ( tnmNs == null && cancerNs == null ) {
//         // T matched, no Ns
//         return true;
//      }
//      return ConceptAggregateUtil.isUriBranchMatch( tnmNs, cancerNs );
//      //  Don't bother with M
//   }
//
//
//
//   static private boolean isHistologyMatch( final String candidateHistology,
//                                            final ConceptAggregate cancer,
//                                            final Collection<ConceptAggregate> tumors ) {
//      if ( candidateHistology.isEmpty() ) {
//         return false;
//      }
//      if ( candidateHistology.equals( CiSummaryUtil.getHistology( cancer.getUri() ) ) ) {
//         return true;
//      }
//      return tumors.stream()
//                   .map( ConceptAggregate::getUri )
//                   .map( CiSummaryUtil::getHistology )
//                   .anyMatch( candidateHistology::equals );
//   }

   static public MatchType getHistologyMatchType( final ConceptAggregate candidate, final Collection<ConceptAggregate> tumors ) {
      final String candidateHistology = CiSummaryUtil.getHistology( candidate.getUri() );
      if ( candidateHistology.isEmpty() ) {
         return MatchType.EMPTY;
      }
      boolean haveEmpty = false;
      for ( ConceptAggregate tumor : tumors ) {
         final String histology = CiSummaryUtil.getHistology( tumor.getUri() );
         if ( histology.isEmpty() ) {
            haveEmpty = true;
         } else if ( histology.equals( candidateHistology ) ) {
            return MatchType.MATCH;
         }
      }
      return haveEmpty ? MatchType.EMPTY : MatchType.MISMATCH;
   }

//   static private boolean isCancerTypeMatch( final String candidateType,
//                                             final ConceptAggregate cancer,
//                                             final Collection<ConceptAggregate> tumors ) {
//      if ( candidateType.isEmpty() ) {
//         return false;
//      }
//      if ( candidateType.equals( CiSummaryUtil.getCancerType( cancer.getUri() ) ) ) {
//         return true;
//      }
//      return tumors.stream()
//                   .map( ConceptAggregate::getUri )
//                   .map( CiSummaryUtil::getCancerType )
//                   .anyMatch( candidateType::equals );
//   }

   static public MatchType getCancerTypeMatchType( final ConceptAggregate candidate,
                                                   final Collection<ConceptAggregate> tumors ) {
      final String candidateType = CiSummaryUtil.getCancerType( candidate.getUri() );
      if ( candidateType.isEmpty() ) {
         return MatchType.EMPTY;
      }
      boolean haveEmpty = false;
      for ( ConceptAggregate tumor : tumors ) {
         final String cancerType = CiSummaryUtil.getCancerType( tumor.getUri() );
         if ( cancerType.isEmpty() ) {
            haveEmpty = true;
         } else if ( cancerType.equals( candidateType ) ) {
            return MatchType.MATCH;
         }
      }
      return haveEmpty ? MatchType.EMPTY : MatchType.MISMATCH;
   }



//
//   static private final class TumorCancerMatch {
//      private Collection<ConceptAggregate> _locationMatches = new ArrayList<>();
//      private Collection<ConceptAggregate> _lateralityMatches = new ArrayList<>();
//      private Collection<ConceptAggregate> _stageMatches = new ArrayList<>();
//      private Collection<ConceptAggregate> _histologyMatches = new ArrayList<>();
//      private Collection<ConceptAggregate> _typeMatches = new ArrayList<>();
//      private TumorCancerMatch() {}
//
//      private Collection<ConceptAggregate> getCoLocatedCancers() {
//         Collection<ConceptAggregate> bestMatches = getBestMatches( _locationMatches, _lateralityMatches );
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = getBestMatches( _locationMatches );
//         }
//         return bestMatches;
//      }
//
//      private Collection<ConceptAggregate> getBestCancers() {
//         Collection<ConceptAggregate> bestMatches = getBestMatches( _stageMatches, _histologyMatches, _typeMatches );
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = getBestMatches( _stageMatches, _histologyMatches );
//         }
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = getBestMatches( _stageMatches, _typeMatches );
//         }
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = getBestMatches( _histologyMatches, _typeMatches );
//         }
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = getBestMatches( _stageMatches );
//         }
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = getBestMatches( _histologyMatches );
//         }
//         if ( bestMatches.isEmpty() ) {
//            bestMatches = getBestMatches( _typeMatches );
//         }
//         return bestMatches;
//      }
//
//      @SafeVarargs
//      static private Collection<ConceptAggregate> getBestMatches( final Collection<ConceptAggregate> ... matches ) {
//         final Collection<ConceptAggregate> bestMatches = new ArrayList<>( matches[ 0 ] );
//         for ( int i=1; i<matches.length; i++ ) {
//            bestMatches.retainAll( matches[ i ] );
//            if ( bestMatches.isEmpty() ) {
//               break;
//            }
//         }
//         return bestMatches;
//      }
//   }


}
