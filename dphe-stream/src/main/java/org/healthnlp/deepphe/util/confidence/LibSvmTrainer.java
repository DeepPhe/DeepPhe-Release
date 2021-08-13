package org.healthnlp.deepphe.util.confidence;

import libsvm.*;
import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibSvmTrainer {


   static private final List<Integer> GOOD_FEATURES = Arrays.asList(
         //3.  1 = # site uri in neoplasm/patient / # site uris in patient	(v patient)
//         7,
         //12. # mentions with site uri in neoplasm / # site mentions in neoplasm  (any order v any order)
         16,
         //15. # mentions with site uri in patient / # site mentions in patient  (patient v patient)
         19,
         //28. # mentions in site uri branch in neoplasm / # mentions in site uri branches in patient  (any order v patient)
////         32,
         //31. class depth site uri / greatest class depth site uris in neoplasm  (v any order)
//         36,
         //34. # "HAS_SITE" relations for site uri / # total "HAS_SITE" relations for patient  (v patient)
//         38,
         //??. # runner-up mentions with site uri in patient / # winner site mentions in patient  (patient v patient)  - Could be a different runner-up, could be > 1
         // This is feature40_2, which was preceded by feature40_1, feature39_1, feature37_2, feature37_1, feature36_1
//         50,
         //41.  First Order Depth - {1-5}
//         51,
         //46. # ontology topography codes
////         56,
         //49XX. # topography codes for site uri branch  - Could be 0
////         59,

//      features.add( Math.min( 10, bestInNeoplasmSiteCount ) );
//      features.add( Math.min( 10, bestInPatientSiteCount ) );
//      features.add( Math.min( 10, bestInNeoplasmMentionCount ) );
//      features.add( Math.min( 10, bestInPatientMentionCount ) );
//      features.add( Math.min( 10, site._bestNeoplasmMentionBranchCount ) );
//      features.add( _majorTopoCode.equals( "C80" ) ? 0 : 1 );
//         60,
         61,
         62,
         63, 64,
         65

                                                                   );


   public static void main( String[] args ) {

      final ModelData trainingData = new ModelData();
      trainingData.readFile( args[ 0 ] );
      final svm_model trainedModel = trainModel( trainingData );
//      trainingData.writeFile( args[ 0 ] + ".libsvm_10" );


      final ModelData testingData = new ModelData();
      testingData.readFile( args[ 1 ] );
//      testingData.writeFile( args[ 1 ] + ".libsvm_10" );
      evaluateModel( trainedModel, testingData );

   }

   static private svm_model trainModel( final ModelData modelData ) {
      final svm_problem svmProblem = new svm_problem();
      final int dataCount = modelData._valueArray.size();
      svmProblem.y = new double[ dataCount ];
      svmProblem.l = dataCount;
      svmProblem.x = new svm_node[ dataCount ][];
      for ( int i = 0; i < dataCount; i++ ) {
         svmProblem.y[ i ] = modelData._labels.get( i )
                                              .equals( "1" )
                             ? 1d
                             : 0d;
         final double[] featureValues = toDoubles( modelData._valueArray.get( i ) );
         svmProblem.x[ i ] = new svm_node[ featureValues.length ];
         for ( int j = 0; j < featureValues.length; j++ ) {
            final svm_node featureValue = new svm_node();
            featureValue.index = ( j + 1 );  // libsvm seems to use 1-based index
            featureValue.value = featureValues[ j ];
            svmProblem.x[ i ][ j ] = featureValue;
         }
      }
      final svm_parameter parameters = new svm_parameter();
      parameters.probability = 1;
      parameters.gamma = 0.5;
      parameters.nu = 0.5;
      // C: 1=82,236,3  2=82,230,9
      parameters.C = 2;
      parameters.svm_type = svm_parameter.C_SVC;  // for classes -1 and 1
      parameters.kernel_type = svm_parameter.LINEAR;
      parameters.cache_size = 20000;
      parameters.eps = 0.001;

      do_cross_validation( svmProblem, parameters );

      return svm.svm_train( svmProblem, parameters );
   }

   static private void evaluateModel( final svm_model trainedModel, final ModelData testData ) {
      double posCorrect = 0;
      double negCorrect = 0;
      double total = 0;
      for ( int i = 0; i < testData._valueArray.size(); i++ ) {
         final int eval = evaluateModelInstance( trainedModel, testData, i );
         if ( eval < 0 ) {
            negCorrect++;
         } else if ( eval > 0 ) {
            posCorrect++;
         }
         total++;
      }
      System.out.println( "Correct: " + ( (posCorrect+negCorrect) / total ) + "   Pos correct count: " + posCorrect +
                          "   Neg correct count: " + negCorrect + "    total: " + total );
   }

   static private int evaluateModelInstance( final svm_model trainedModel,
                                                 final ModelData testData,
                                                 final int testIndex ) {
      final double label = toLabel( testData._labels.get( testIndex ) ) * 100;
      final double[] featureValues = toDoubles( testData._valueArray.get( testIndex ) );
      final int featureCount = featureValues.length;
      final svm_node[] nodes = new svm_node[ featureCount ];
      for ( int i = 0; i < featureCount; i++ ) {
         final svm_node node = new svm_node();
         node.index = ( i + 1 );  // libsvm seems to use 1-based index
         node.value = featureValues[ i ];
         nodes[ i ] = node;
      }
      final int totalClasses = svm.svm_get_nr_class( trainedModel );
      final int[] labels = new int[ totalClasses ];
      svm.svm_get_labels( trainedModel, labels );
      final double[] estimates = new double[ totalClasses ];
      final double prediction = svm.svm_predict_probability( trainedModel, nodes, estimates );
      for ( int i = 0; i < totalClasses; i++ ) {
         System.out.print( " " + label + " (" + labels[ i ] + ":" + estimates[ i ] + ")" );
      }
      final boolean posCorrect = label > 0 && prediction > 0;
      final boolean negCorrect = label <= 0 && prediction <= 0;
      final boolean correct = posCorrect || negCorrect;
      System.out.println( " Prediction:" + prediction + "    " + correct );
      return correct ? ( posCorrect ? 1 : -1 ) : 0;
   }


   static private final class ModelData {

      static private final int NLP_VALUE_COLUMN = 2;
      static private final int LABEL_COLUMN = 4;
      static private final int FIRST_FEATURE_COLUMN = 5;
      final List<String> _labels = new ArrayList<>();
      final List<List<String>> _valueArray = new ArrayList<>();

      private void readFile( final String filePath ) {
         try ( BufferedReader reader = new BufferedReader( new FileReader( filePath ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               line = line.trim();
               if ( line.isEmpty() || line.startsWith( "//" ) ) {
                  line = reader.readLine();
                  continue;
               }
               final String[] splits = StringUtil.fastSplit( line, '|' );
               final List<String> values = new ArrayList<>();
               for ( int i = FIRST_FEATURE_COLUMN; i < splits.length; i++ ) {
//                  if ( GOOD_FEATURES.contains( i ) ) {
                     values.add( splits[ i ] );
//                  }
               }
//               values.add( splits[ LABEL_COLUMN ] );
               _valueArray.add( values );
               _labels.add( splits[ LABEL_COLUMN ] );
               line = reader.readLine();
//               System.out.println( splits.length );
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
            System.exit( 1 );
         }
      }

      private void writeFile( final String filePath ) {
         try ( final Writer writer = new FileWriter( filePath ) ) {
            final int lines = _labels.size();
            for ( int i = 0; i < lines; i++ ) {
               final String label = _labels.get( i );
               writer.write( label.equals( "1" )
                             ? "1"
                             : "0" );
               final List<String> values = _valueArray.get( i );
               int index = 1;
               for ( String value : values ) {
                  writer.write( " " + index + ":" + toDouble( value ) );
                  index++;
               }
               writer.write( "\n" );
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
         }
      }
   }


   static private void do_cross_validation( final svm_problem svmProblem, final svm_parameter parameters ) {
      int i;
      int total_correct = 0;
      double total_error = 0;
      double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
      double[] target = new double[ svmProblem.l ];

      final int n_fold = 2;
      System.out.println( "!!! Cross Validation, folds = " + n_fold );
      svm.svm_cross_validation( svmProblem, parameters, n_fold, target );
      if ( parameters.svm_type == svm_parameter.EPSILON_SVR ||
           parameters.svm_type == svm_parameter.NU_SVR ) {
         for ( i = 0; i < svmProblem.l; i++ ) {
            double y = svmProblem.y[ i ];
            double v = target[ i ];
            total_error += ( v - y ) * ( v - y );
            sumv += v;
            sumy += y;
            sumvv += v * v;
            sumyy += y * y;
            sumvy += v * y;
         }
         System.out.print( "!!!  Cross Validation Mean squared error = " + total_error / svmProblem.l + "\n" );
         System.out.print( "!!!  Cross Validation Squared correlation coefficient = " +
                           ( ( svmProblem.l * sumvy - sumv * sumy ) * ( svmProblem.l * sumvy - sumv * sumy ) ) /
                           ( ( svmProblem.l * sumvv - sumv * sumv ) * ( svmProblem.l * sumyy - sumy * sumy ) ) + "\n"
                         );
      } else {
         for ( i = 0; i < svmProblem.l; i++ ) {
            if ( target[ i ] == svmProblem.y[ i ] ) {
               ++total_correct;
            }
         }
         System.out.print( "!!!  Cross Validation Accuracy = " + 100.0 * total_correct / svmProblem.l + "%\n" );
      }
   }


   static private Integer parseInt( final String value ) {
      try {
         return Integer.valueOf( value );
      } catch ( NumberFormatException nfE ) {
         System.err.println( value + " " + nfE.getMessage() );
         System.exit( 1 );
      }
      return -1;
   }


   static private double toLabel( final String value ) {
      try {
         final double actual = Double.parseDouble( value );
         return Math.min( 100, actual ) / 100d;
      } catch ( NumberFormatException nfE ) {
         System.err.println( value + " " + nfE.getMessage() );
         System.exit( 1 );
      }
      return 0;
   }

   static private double toDouble( final String value ) {
      if ( value.toUpperCase()
                .startsWith( "C" ) ) {
         // Handle icdo code
         return toDouble( value.substring( 1 ) );
      }
      try {
         final double actual = Double.parseDouble( value );

         return Math.max( 0, Math.min( 1d, actual / 10d ) );

      } catch ( NumberFormatException nfE ) {
         System.err.println( value + " " + nfE.getMessage() );
         System.exit( 1 );
      }
      return 0;
   }

   static private double[] toDoubles( final List<String> values ) {
      final double[] array = new double[ values.size() ];
      for ( int i = 0; i < values.size(); i++ ) {
         array[ i ] = toDouble( values.get( i ) );
      }
      return array;
   }


}
