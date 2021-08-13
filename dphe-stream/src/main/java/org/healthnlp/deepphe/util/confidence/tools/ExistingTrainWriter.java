package org.healthnlp.deepphe.util.confidence.tools;

import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.*;


/**
 * Combines score output from the DeepPhe eval tool with feature output from a dphe-stream run.
 */
public class ExistingTrainWriter {

   static private final String DATA_DIRECTORY = "C:\\Spiffy\\output\\dphe_output\\dphe_5\\01_14_2020\\xlated\\kcr";

   static private final String ATTRIBUTE_NAME = "topography_major";
   static private final String ATTRIBUTE_SCORE = "topography__ICD_O_co";

   // 418 train 01/01/2021
   static private final String TRAIN_SPLIT = "C:\\Spiffy\\data\\dphe_data\\datasets\\KCR\\preexisting_gold_annotations"
                            + "\\TRAIN_split.bsv";
   // 108 dev 01/14/2021
   static private final String DEV_SPLIT = "C:\\Spiffy\\data\\dphe_data\\datasets\\KCR\\preexisting_gold_annotations"
                                             + "\\DEV_split.bsv";
   // 290 test 01/01/2021   ...  816 total
   static private final String TEST_SPLIT = "C:\\Spiffy\\data\\dphe_data\\datasets\\KCR\\preexisting_gold_annotations"
                                             + "\\TEST_split.bsv";


   static private final Collection<String> DATASETS = Arrays.asList( "text_1_90",
                                                                 "text_91_300",
                                                                 "text_301_600",
                                                                 "preexisting_gold_annotations" );

   public static void main( String[] args ) {
      final Map<String,String[]> features = new HashMap<>();
      for ( String dataset : DATASETS ) {
         final String setFile = DATA_DIRECTORY + "/" + dataset + "/features/" + ATTRIBUTE_NAME;
         features.putAll( readFeatures( setFile ) );
      }

      final Map<String,String[]> scores = new HashMap<>();
      for ( String dataset : DATASETS ) {
         final String setFile = DATA_DIRECTORY + "/" + dataset + "/feature_score/" + ATTRIBUTE_SCORE;
         scores.putAll( readScores( setFile ) );
      }

      writeFeaturesAndScores( TRAIN_SPLIT, DATA_DIRECTORY + "/TRAIN_feature_score.bsv", scores, features );
      writeFeaturesAndScores( DEV_SPLIT, DATA_DIRECTORY + "/DEV_feature_score.bsv", scores, features );
      writeFeaturesAndScores( TEST_SPLIT, DATA_DIRECTORY + "/TEST_feature_score.bsv", scores, features );
   }


   static private Collection<String> getPatientNames( final String splitPath ) {
      final Collection<String> patients = new HashSet<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( splitPath ) ) ) {
         int lineNum = 0;
         String line = reader.readLine();
         while ( line != null ) {
            lineNum++;
            if ( line.isEmpty() || lineNum < 5 ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length > 3 ) {
               patients.add( splits[ 1 ] );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
      return patients;
   }


   static private Map<String,String[]> readFeatures( final String setFile ) {
      final Map<String,String[]> features = new HashMap<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( setFile ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            if ( line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length > 4 ) {
               final String patient = splits[ 0 ];
               features.put( patient, splits );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
      return features;
   }

   static private Map<String,String[]> readScores( final String scoreFile ) {
      final Map<String,String[]> scores = new HashMap<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( scoreFile ) ) ) {
         int lineNum = 0;
         String line = reader.readLine();
         while ( line != null ) {
            lineNum++;
            if ( line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length >= 4 ) {
               final String patient = splits[ 0 ];
               scores.put( patient, splits );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
      return scores;
   }


   static private void writeFeaturesAndScores( final String splitFile,
                                              final String featureScoreFile,
                                             final Map<String,String[]> scoresMap,
                                             final Map<String,String[]> featuresMap ) {
      final Collection<String> patients = getPatientNames( splitFile );
      new File( featureScoreFile ).getParentFile().mkdirs();
      try ( Writer writer = new FileWriter( featureScoreFile ) ) {
         for ( String patient : patients ) {
            final String[] features = featuresMap.get( patient );
            if ( features == null ) {
               System.err.println( "No System Features for " + patient );
               continue;
            }
            if ( features[ 5 ].equals( "-1" ) ) {
               System.out.println( "No attribute determined, skipping patient " + patient );
               continue;
            }
            final String[] scores = scoresMap.get( patient );
            if ( scores == null ) {
               System.err.println( "No Gold Score for " + patient );
               continue;
            }
            writer.write( patient + "|" + features[ 1 ] + "|" + scores[ 1 ] + "|" + scores[ 2 ] + "|"
                        + scores[ 3 ] );
            for ( int i = 3; i < features.length; i++ ) {
               writer.write(  "|" + features[ i ] );
            }
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }


}
