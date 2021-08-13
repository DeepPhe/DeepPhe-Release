package org.healthnlp.deepphe.util.eval.old.eval;

import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.*;

// First run dphe5Evaluator
public class FeatureCranker {

   // 418 train 01/01/2021
   static private final String TRAIN_SPLIT = "C:\\Spiffy\\data\\dphe_data\\kcr\\TRAIN_split.bsv";
   // 108 dev 01/14/2021
   static private final String DEV_SPLIT = "C:\\Spiffy\\data\\dphe_data\\kcr\\DEV_split.bsv";
   // 290 test 01/01/2021   ...  816 total
   static private final String TEST_SPLIT = "C:\\Spiffy\\data\\dphe_data\\kcr\\TEST_split.bsv";

   public static void main( String ... args ) {
      final File evalOutDir = new File( args[ 0 ] );
      final File systemDir = new File( evalOutDir, "features" );
      final File evalDir = new File( evalOutDir, "feature_score" );
      final File outDir = new File( evalOutDir, "class_feature" );
      outDir.mkdirs();
      final String[] systemFeatures = { "topography_major",
                                        "topography_minor",
                                        "histology",
                                        "behavior",
                                        "laterality",
                                        "grade",
                                        "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
                                        "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" };
      final String[] evalFeatures = { "topography_major",
                                        "topography_minor",
                                        "histology",
                                        "behavior",
                                        "laterality",
                                        "grade",
                                      "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
                                      "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" };
//      final String[] evalFeatures = { "topography__ICD_O_code__MAJOR_",
//                                      "topography__ICD_O_code__SUBSIT",
//                                      "histology__ICD_O_code",
//                                      "behavior__ICD_O_code",
//                                      "laterality__ICD_O_code",
//                                      "grade__ICD_O_code" };
      final Collection<String> trainees = getPatientNames( TRAIN_SPLIT );
      final Collection<String> devees = getPatientNames( DEV_SPLIT );
      final Collection<String> testees = getPatientNames( TEST_SPLIT );

      for ( int i = 0; i < systemFeatures.length; i++ ) {
         writeFeatureFile( systemDir, evalDir, systemFeatures[ i ], evalFeatures[ i ], outDir, trainees, devees,
                           testees );
      }


   }

   static private void writeFeatureFile( final File systemDir, final File evalDir,
                                         final String featureName, final String evalName,
                                         final File outDir,
                                         final Collection<String> trainees,
                                         final Collection<String> devees,
                                         final Collection<String> testees ) {
      final Map<String,String[]> systemLines = readSourceFile( new File( systemDir, featureName ) );
      final Map<String,String[]> evalLines = readSourceFile( new File( evalDir, evalName ) );
      writeFeatureFile( systemLines, evalLines, featureName, outDir, "train", trainees );
      writeFeatureFile( systemLines, evalLines, featureName, outDir, "dev", devees );
      writeFeatureFile( systemLines, evalLines, featureName, outDir, "test", testees );
   }

   static private void writeFeatureFile( final Map<String,String[]> systemLines,
                                         final Map<String,String[]> evalLines,
                                         final String featureName,
                                         final File outDir,
                                         final String splitName,
                                         final Collection<String> splitPatients ) {
      final File splitDir = new File( outDir, splitName );
      splitDir.mkdirs();
      final List<String> patients = new ArrayList<>( systemLines.keySet() );
      patients.retainAll( splitPatients );
      Collections.sort( patients );
      try ( Writer writer = new FileWriter( new File( splitDir, featureName ) ) ) {
         for ( String name : patients ) {
            final String[] system = systemLines.get( name );
            writer.write( system[0] + "|" + system[1] + "|" + system[2] + "|" );
            final String[] eval = evalLines.get( name );
            if ( eval == null ) {
               writer.write( "|0|" );
            } else {
               writer.write( eval[2] + "|" + eval[3] + "|" );
            }
            final List<String> features = new ArrayList<>();
            for ( int i=3; i<system.length; i++ ) {
               if ( !system[ i ].isEmpty() ) {
                  features.add( system[ i ] );
               }
            }
            writer.write( String.join("|", features ) + "\n" );
         }
      } catch ( IOException ioE ) {
         System.err.println( "Couldn't Write " + featureName + " " + ioE.getMessage() );
      }
   }

   static private Map<String,String[]> readSourceFile( final File file ) {
      final Map<String,String[]> evalLines = new HashMap<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] splits = StringUtil.fastSplit( line, '|' );
            evalLines.put( splits[ 0 ], splits );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( file.getName() + " " + ioE.getMessage() );
         System.exit( 1 );
      }
      return evalLines;
   }

   static private Collection<String> getPatientNames( final String splitPath ) {
      return getPatientNames( new File( splitPath ) );
   }

   static public Collection<String> getPatientNames( final File splitFile ) {
      final Collection<String> patients = new HashSet<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( splitFile ) ) ) {
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


}
