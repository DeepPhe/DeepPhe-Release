package org.healthnlp.deepphe.util.eval.old.eval;


import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2020
 */
public class IcdoEvaluator {

   static private final Logger LOGGER = Logger.getLogger( "IcdoEvaluator" );

   //  Need to capitalize "before" in topo major system output
   // C:\Spiffy\data\dphe_data\mixed\kcr\mixed_90\pilot\FixFront.bsv
   // C:\Spiffy\data\dphe_data\mixed\kcr\pilot_91_300\DeepPhe_CR_KCR_91_300_epath_GOLD\DeepPhe_CR_KCR_91_300_epath_GOLD_spf.bsv
   // C:\Spiffy\data\dphe_data\datasets\KCR\batch_301_600\DeepPhe_CR_KCR_301_600_GOLD_Annotations_v2.bsv
   // C:\Spiffy\data\dphe_data\datasets\KCR\preexisting_gold_annotations\gold_for_3_cancers_spf.bsv

   // C:\Spiffy\data\dphe_data\CombinedKcrGold.bsv
   public static void main( final String... args ) {
      if ( args == null || args.length < 2 ) {
         System.err.println( "Example: java IcdoEvaluator <gold_summary_file> <system_summary_file>" );
         System.exit( -1 );
      }
      try {
         final File goldFile = FileLocator.getFile( args[ 0 ] );
         final File systemFile = FileLocator.getFile( args[ 1 ] );
         if ( args.length == 3 ) {
            scoreSplits( goldFile.getName(), goldFile, systemFile, args[ 2 ] );
         } else {
            scoreSystem( goldFile.getName(), goldFile, systemFile );
         }
         // Combines the features and scores, divvying them into splits
         FeatureCranker.main( systemFile.getParent() );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
   }


   static private void scoreSplits( final String outName, final File goldFile, final File systemFile,
                                    final String splitDir ) {
      final File trainSplit = new File( splitDir, "TRAIN_split.bsv" );
      final File devSplit = new File( splitDir, "DEV_split.bsv" );
      final File testSplit = new File( splitDir, "TEST_split.bsv" );
      final Collection<String> trainees = FeatureCranker.getPatientNames( trainSplit );
      final Collection<String> devees = FeatureCranker.getPatientNames( devSplit );
      final Collection<String> testees = FeatureCranker.getPatientNames( testSplit );
      if ( !trainees.isEmpty() ) {
         scoreSystem( outName+"_train", goldFile, systemFile, trainees );
      }
      if ( !devees.isEmpty() ) {
         scoreSystem( outName+"_dev", goldFile, systemFile, devees );
      }
      if ( !testees.isEmpty() ) {
         scoreSystem( outName+"_test", goldFile, systemFile, testees );
      }
   }

   static private void scoreSystem( final String neoplasmType, final File goldFile, final File systemFile ) {
      scoreSystem( neoplasmType, goldFile, systemFile, Collections.emptyList() );
   }

   static private void scoreSystem( final String neoplasmType, final File goldFile, final File systemFile,
                                    final Collection<String> patientNames ) {
      // Moved the * and - for required and scoring to NaaccrIcdoBsvWriter so that I don't have to keep changing it in gold
      final List<String> systemProperties = NaaccrSummaryReader.readHeader( systemFile );
      final Collection<String> requiredNames = NaaccrSummaryReader.getRequiredNames( systemProperties );
      final Collection<String> scoringNames = NaaccrSummaryReader.getScoringNames( systemProperties );
      final Map<String, Integer> systemIndices = NaaccrSummaryReader.mapNameIndices( systemProperties );
      final List<String> goldProperties = NaaccrSummaryReader.readHeader( goldFile );
      final Map<String, Integer> goldIndices = NaaccrSummaryReader.mapNameIndices( goldProperties );

      final Map<String, Collection<NeoplasmSummary>> goldSummaries
            = NaaccrSummaryReader.readSummaries( goldFile, requiredNames, scoringNames, goldIndices, patientNames );

      final Map<String, Collection<NeoplasmSummary>> systemSummaries
            = NaaccrSummaryReader.readSummaries( systemFile, requiredNames, scoringNames, systemIndices, patientNames );

      final Collection<EvalPatient> bestPatients = getBestPatients( goldSummaries, systemSummaries );
      final EvalCorpus corpus = new EvalCorpus( bestPatients );

//      mergeAttributeNames( scoringNames );
      ScoreWriter.saveScoreFile( neoplasmType, requiredNames, scoringNames, systemFile, corpus );
   }


   static private Collection<EvalPatient> getBestPatients( final Map<String, Collection<NeoplasmSummary>> goldMap,
                                                           final Map<String, Collection<NeoplasmSummary>> systemMap ) {
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
                                              final Map<String, Collection<NeoplasmSummary>> goldMap,
                                              final Map<String, Collection<NeoplasmSummary>> systemMap ) {
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
                                                    .collect( Collectors.toList() );
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
//            LOGGER.info(
//                  "Matchable System for " + goldList.get( i ).getAttribute( DISEASE_HAS_PRIMARY_ANATOMIC_SITE ) );
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


}
