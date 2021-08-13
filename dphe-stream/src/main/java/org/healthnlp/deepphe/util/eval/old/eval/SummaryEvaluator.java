package org.healthnlp.deepphe.util.eval.old.eval;


import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE;


/**
 * See https://www.aclweb.org/anthology/S16-1165
 *
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/9/2019
 */
final public class SummaryEvaluator {

   static private final Logger LOGGER = Logger.getLogger( "SummaryEvaluator" );

   public static void main( final String... args ) {
      if ( args == null || args.length != 2 ) {
         System.err.println( "Example: java SummaryEvaluator <gold_summary_file> <system_summary_file>" );
         System.exit(-1);
      }
      try {
         final File goldFile = FileLocator.getFile( args[ 0 ] );
         final File systemFile = FileLocator.getFile( args[ 1 ] );
         scoreSystem( goldFile.getName(), goldFile, systemFile );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit(-1);
      }
   }


   static private void scoreSystem( final String neoplasmType, final File goldFile, final File systemFile ) {
      final List<String> goldProperties = NeoplasmSummaryReader.readHeader( goldFile );
      final Collection<String> requiredNames = NeoplasmSummaryReader.getRequiredNames( goldProperties );
      final Collection<String> scoringNames = NeoplasmSummaryReader.getScoringNames( goldProperties );
      final Map<String, Integer> goldIndices = NeoplasmSummaryReader.mapNameIndices( goldProperties );
      final List<String> systemProperties = NeoplasmSummaryReader.readHeader( systemFile );
      final Map<String, Integer> systemIndices = NeoplasmSummaryReader.mapNameIndices( systemProperties );

      final Map<String,Collection<NeoplasmSummary>> goldSummaries
            = NeoplasmSummaryReader.readSummaries( goldFile, requiredNames, scoringNames, goldIndices );

      final Map<String,Collection<NeoplasmSummary>> systemSummaries
            = NeoplasmSummaryReader.readSummaries( systemFile, requiredNames, scoringNames, systemIndices );

      final Collection<EvalPatient> bestPatients = getBestPatients( goldSummaries, systemSummaries );
      final EvalCorpus corpus = new EvalCorpus( bestPatients );

      mergeAttributeNames( scoringNames );
      ScoreWriter.saveScoreFile( neoplasmType, requiredNames, scoringNames, systemFile, corpus );
   }



   static private Collection<EvalPatient> getBestPatients( final Map<String,Collection<NeoplasmSummary>> goldMap,
                                                           final Map<String,Collection<NeoplasmSummary>> systemMap ) {
      final Collection<String> patientIds = new HashSet<>( goldMap.keySet() );
      patientIds.addAll( systemMap.keySet() );
      final List<String> patientIdList = new ArrayList<>( patientIds );
      Collections.sort( patientIdList );
      final Collection<EvalPatient> bestPatients = new ArrayList<>( patientIdList.size() );
      for ( String patientId : patientIdList ) {
         final EvalPatient bestPatient = getBestPatient( patientId, goldMap, systemMap );
         if ( bestPatient != null ) {
            bestPatients.add( bestPatient );
         }
      }
      return bestPatients;
   }

   static private EvalPatient getBestPatient( final String patientId,
                                              final Map<String,Collection<NeoplasmSummary>> goldMap,
                                              final Map<String,Collection<NeoplasmSummary>> systemMap ) {
      final Collection<NeoplasmSummary> gold = goldMap.get( patientId );
      final Collection<NeoplasmSummary> system = systemMap.get( patientId );
      if ( gold == null || gold.size() == 0 ) {
         LOGGER.warn( "Gold is missing annotations for patient " + patientId );
         return null;
      }
      if ( system == null || system.size() == 0 ) {
         LOGGER.warn( "System is missing output for patient " + patientId );
         final Collection<EvalNeoplasm> pairs = gold.stream()
                                                    .map( g -> new EvalNeoplasm( g, null ) )
                                                    .collect( Collectors.toList());
         return new EvalPatient( patientId, pairs );
      }

      final List<NeoplasmSummary> goldList = new ArrayList<>( gold );
      final List<NeoplasmSummary> systemList = new ArrayList<>( system );

      return getBestEvalPatientQuicker( patientId, goldList, systemList );
   }


   static private EvalPatient getBestEvalPatientQuicker( final String patientId,
                                                         final List<NeoplasmSummary> goldList,
                                                         final List<NeoplasmSummary> systemList ) {
      final Collection<EvalNeoplasm> neoplasms = new ArrayList<>();
      final Collection<NeoplasmSummary> usedGold = new HashSet<>();
      final Collection<NeoplasmSummary> usedSystem = new HashSet<>();
      for ( int i = 0; i < goldList.size(); i++ ) {
         if ( usedGold.contains( goldList.get( i ) ) ) {
            continue;
         }
         final Collection<NeoplasmSummary> matchGoldSet = new HashSet<>();
         final Collection<NeoplasmSummary> matchSystemSet = new HashSet<>();
         for ( int j = 0; j < systemList.size(); j++ ) {
            if ( usedSystem.contains( systemList.get( j ) ) ) {
               continue;
            }
            if ( new EvalNeoplasm( goldList.get( i ), systemList.get( j ) ).isValid() ) {
               matchGoldSet.add( goldList.get( i ) );
               matchSystemSet.add( systemList.get( j ) );
               for ( int k = i + 1; k < goldList.size(); k++ ) {
                  if ( usedGold.contains( goldList.get( k ) ) ) {
                     continue;
                  }
                  if ( new EvalNeoplasm( goldList.get( k ), systemList.get( j ) ).isValid() ) {
                     matchGoldSet.add( goldList.get( k ) );
                  }
               }
            }
         }
         if ( !matchGoldSet.isEmpty() && !matchSystemSet.isEmpty() ) {
            final List<NeoplasmSummary> matchableGolds = new ArrayList<>( matchGoldSet );
            final List<NeoplasmSummary> matchableSystems = new ArrayList<>( matchSystemSet );
            matchableGolds.sort( Comparator.comparing( NeoplasmSummary::getId ) );
            matchableSystems.sort( Comparator.comparing( NeoplasmSummary::getId ) );
            if ( matchableGolds.size() == 1 && matchableSystems.size() == 1 ) {
               LOGGER.info(
                     "Evaluating " + patientId + " with array sized " + goldList.size() + "," + systemList.size() +
                     " using singular" );
               neoplasms.add( new EvalNeoplasm( matchableGolds.get( 0 ), matchableSystems.get( 0 ) ) );
            } else {
               final Collection<NeoplasmSummary> whittledGold = new ArrayList<>();
               final Collection<NeoplasmSummary> whittledSystem = new ArrayList<>();
               while ( ((matchableGolds.size() - whittledGold.size() > 3) ||
                        (matchableSystems.size() - whittledSystem.size() > 3))
                       && (matchableGolds.size() - whittledGold.size() > 0)
                       && (matchableSystems.size() - whittledSystem.size() > 0) ) {
                  LOGGER.info(
                        "Evaluating " + patientId + " with array sized " + goldList.size() + "," + systemList.size() +
                        " whittling array sized " + (matchableGolds.size() - whittledGold.size()) + "," +
                        (matchableSystems.size() - whittledSystem.size()) );
                  EvalNeoplasm bestMatch = new EvalNeoplasm( matchableGolds.get( 0 ), matchableSystems.get( 0 ) );
                  for ( int m = 1; m < matchableGolds.size(); m++ ) {
                     if ( whittledGold.contains( matchableGolds.get( m ) ) ) {
                        continue;
                     }
                     for ( int n = 0; n < matchableSystems.size(); n++ ) {
                        if ( whittledSystem.contains( matchableSystems.get( n ) ) ) {
                           continue;
                        }
                        final EvalNeoplasm candidate
                              = new EvalNeoplasm( matchableGolds.get( m ), matchableSystems.get( n ) );
                        if ( candidate.getFullScore() > bestMatch.getFullScore() ) {
                           bestMatch = candidate;
                        }
                     }
                  }
                  neoplasms.add( bestMatch );
                  whittledGold.add( bestMatch.getGoldSummary() );
                  whittledSystem.add( bestMatch.getSystemSummary() );
               }
               usedGold.addAll( whittledGold );
               usedSystem.addAll( whittledSystem );
               matchableGolds.removeAll( whittledGold );
               matchableSystems.removeAll( whittledSystem );
               if ( !matchableGolds.isEmpty() && !matchableSystems.isEmpty() ) {
                  LOGGER.info(
                        "Evaluating " + patientId + " with array sized " + goldList.size() + "," + systemList.size() +
                        " using array sized " + matchableGolds.size() + "," + matchableSystems.size() );
                  final EvalPatient partPatient
                        = getBestEvalPatient( patientId, matchableGolds, matchableSystems );
                  neoplasms.addAll( partPatient.getNeoplasmPairs() );
               }
            }
            usedGold.addAll( matchableGolds );
            usedSystem.addAll( matchableSystems );
            LOGGER.info(
                  "Matchable System for " + goldList.get( i ).getAttribute( DISEASE_HAS_PRIMARY_ANATOMIC_SITE ) );
         }
      }
      if ( usedGold.isEmpty() ) {
         return createTotalMissPatient( patientId, goldList, systemList );
      }

      for ( NeoplasmSummary gold : goldList ) {
         if ( !usedGold.contains( gold ) ) {
            neoplasms.add( new EvalNeoplasm( gold, null ) );
         }
      }
      for ( NeoplasmSummary system : systemList ) {
         if ( !usedSystem.contains( system ) ) {
            neoplasms.add( new EvalNeoplasm( null, system ) );
         }
      }
      return new EvalPatient( patientId, neoplasms );
   }


   static private EvalPatient getBestEvalPatient( final String patientId,
                                                  final List<NeoplasmSummary> goldList,
                                                  final List<NeoplasmSummary> systemList ) {
      goldList.sort( Comparator.comparing( NeoplasmSummary::getId ) );
      systemList.sort( Comparator.comparing( NeoplasmSummary::getId ) );
      // Use size * 2 because there need to be possible FP, FN combinations with null;
      final int count = 2 * Math.max( goldList.size(), systemList.size() );
      final EvalPatient bestPatient
            = getBestEvalPatient( patientId, goldList, systemList, 0, count, new ArrayList<>( count ), new ArrayList<>( count ), new ArrayList<>( count ) );
      if ( bestPatient != null ) {
         return bestPatient;
      }
      return createTotalMissPatient( patientId, goldList, systemList );
   }


   static private EvalPatient getBestEvalPatient( final String patientId,
                                                  final List<NeoplasmSummary> goldList,
                                                  final List<NeoplasmSummary> systemList,
                                                  final int i,
                                                  final int count,
                                                  final Collection<Integer> existingJs,
                                                  final Collection<EvalNeoplasm> evalNeoplasms,
                                                  final Collection<Pair<Integer>> combinations ) {
      double bestFullScore = 0;
      double bestF1 = 0;
      EvalPatient bestPatient = null;
      for ( int j = 0; j < count; j++ ) {
         if ( existingJs.contains( j ) ) {
            continue;
         }
         final Collection<Pair<Integer>> newCombinations = new ArrayList<>( combinations );
         newCombinations.add( new Pair<>( i, j ) );
         final EvalNeoplasm evalNeoplasm = createEvalNeoplasm( goldList, systemList, i, j );
         final Collection<EvalNeoplasm> newEvalNeoplasms = new ArrayList<>( evalNeoplasms );
         if ( evalNeoplasm != null ) {
            newEvalNeoplasms.add( evalNeoplasm );
         }
         EvalPatient evalPatient;
         if ( i == count - 1 ) {
            evalPatient = new EvalPatient( patientId, newEvalNeoplasms );
         } else {
            final Collection<Integer> newExistingJs = new ArrayList<>( existingJs );
            newExistingJs.add( j );
            evalPatient = getBestEvalPatient( patientId, goldList, systemList,
                  i + 1, count, newExistingJs, newEvalNeoplasms, newCombinations );
         }
         if ( evalPatient == null ) {
            continue;
         }
         final double evalF1 = evalPatient.getF1();
         if ( evalF1 > bestF1 ) {
            bestPatient = evalPatient;
            bestF1 = evalF1;
            bestFullScore = evalPatient.getFullScore();
//            LOGGER.info( patientId + " Best New Patient " + newCombinations.stream().map( Pair::toString ).collect( Collectors.joining( " " )) + " " + bestF1 + " " + bestFullScore );
         } else if ( evalF1 > 0 && evalF1 == bestF1 ) {
            final double evalFullScore = evalPatient.getFullScore();
            if ( evalFullScore >= bestFullScore ) {
               bestPatient = evalPatient;
               bestFullScore = evalFullScore;
//               LOGGER.info( patientId + " Best New Patient " + newCombinations.stream().map( Pair::toString ).collect( Collectors.joining( " " )) + " " + bestF1 + " " + bestFullScore );
            }
         }
      }
      return bestPatient;
   }


   static private EvalPatient createTotalMissPatient( final String patientId,
                                                      final List<NeoplasmSummary> goldList,
                                                      final List<NeoplasmSummary> systemList ) {
      final Collection<EvalNeoplasm> totalMiss = new ArrayList<>( goldList.size() + systemList.size() );
      goldList.stream().map( g -> new EvalNeoplasm( g, null ) ).forEach( totalMiss::add );
      systemList.stream().map( s -> new EvalNeoplasm( null, s ) ).forEach( totalMiss::add );
      LOGGER.warn( "Total Miss for patient " + patientId );
      return new EvalPatient( patientId, totalMiss );
   }


   static private EvalNeoplasm createEvalNeoplasm( final List<NeoplasmSummary> goldList,
                                                   final List<NeoplasmSummary> systemList,
                                                   final int i, final int j ) {
      NeoplasmSummary gold = null;
      if ( i < goldList.size() ) {
         gold = goldList.get( i );
      }
      NeoplasmSummary system = null;
      if ( j < systemList.size() ) {
         system = systemList.get( j );
      }
      if ( gold == null && system == null ) {
         return null;
      }
      return new EvalNeoplasm( gold, system );
   }


   static private void mergeAttributeNames( final Collection<String> names ) {
//      names.remove( HAS_PATHOLOGIC_T );
//      names.remove( HAS_PATHOLOGIC_N );
//      names.remove( HAS_PATHOLOGIC_M );
   }


}
