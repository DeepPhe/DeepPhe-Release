package org.apache.ctakes.cancer.util;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/1/2017
 */
public class LsuBronzeXlater {

   static private final Logger LOGGER = Logger.getLogger( "LsuBronzeXlater" );

   static private final String LSU_BRONZE_IN = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\BronzeStandardDataItems.csv";
   static private final String LSU_BRCA_CANCER_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\DeepPhe_BrCa_Gold_Cancer.csv";
   static private final String LSU_BRCA_TUMOR_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\DeepPhe_BrCa_Gold_Tumor.csv";
   static private final String LSU_SKIN_CANCER_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\DeepPhe_Skin_Gold_Cancer.csv";
   static private final String LSU_SKIN_TUMOR_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\DeepPhe_Skin_Gold_Tumor.csv";

   static private final int CANCER_COLUMN_COUNT = 15;
   static private final int TUMOR_COLUMN_COUNT = 25;



   public static void main( final String... args ) {
      // FROM LSU BRONZE
//            1.       Type / diagnosis – Histology
//            2.       Location – Primary Site
//            3.       Stage – SEER Summary Stage 2000
//            4.       Tnm – Clin T, Clin N, Clin M, Clin Stage Group, Path T, Path N, Path M, Path Stage Group
//            5.       Metastases – Clin M, Path M
//            6.       metastasis location – We have fields for these, but we just started collecting them.  Since these are older cases, none of them have it filled out
//            7.       Laterality - Laterality
//            8.       Clockface and quadrant (for breast cancer) – Primary Site text Description
//                0        1             2            3          4            5             6          7            8             9                       10                     11                    12          13           14          15             16          17              18              19              20         21             22           23          24            25           26               27
//            substring,primary_site,primary_site,laterality,laterality,histology_icdo3,histology,behavior_icdo3,behavior,date_of_diagnosis_yyyy,seer_summary_stage_2000,seer_summary_stage_2000,clin_t_value,clin_t_value,clin_n_value,clin_n_value,clin_m_value,clin_m_value,clin_stage_group,clin_stage_group,path_t_value,path_t_value,path_n_value,path_n_value,path_m_value,path_m_value,path_stage_group,path_stage_group

      // TO OUR -GOLD- CANCER ANNOTATIONS
//                0           1            2                3                   4             5                    6                         7                       8                      9                10                    11                         12                             13                    14              15
//            *cancer ID|*patient ID|body location|body location laterality|Temporality|clinical stage|clinical T classification|clinical N classification|clinical M classification|-clinical prefix|-clinical suffix|pathologic T classification|pathologic N classification|pathologic M classification|-pathologic prefix|-pathologic suffix

      // TO OUR -GOLD- TUMOR ANNOTATIONS
//                         0                     1            2             3                  4                            5                              6               7          8          9             10               11               12                       13                      14                   15                         16                  17                    18                             19                         20                           21                             22                                23                               24                       25
//      -geographically determined (yes/no)|*patient ID|*cancer  link|*body location|*body location laterality|body location clockface position|body location quadrant|Diagnosis|tumor type|-cancer type|-histologic type|-tumor extent|er status interpretation|-er status numeric value|-er status method|pr status interpretation|-pr status numeric value|-pr status method|her2neu status interpretation|-her2neu status numeric value|-her2neu status method|-radiologic tumor size (mm)|-radiologic tumor size procedure method|-pathologic tumor size (mm)|-pathologic aggregate tumor size (mm)|calcifications

      final Map<Integer,Integer> cancerMap = new HashMap<>();
      cancerMap.put( 5, 27 );   // Stage
      cancerMap.put( 6, 13 );   // cT
      cancerMap.put( 7, 15 );   // cN
      cancerMap.put( 8, 17 );   // cM
      cancerMap.put( 11, 21 );  // pT
      cancerMap.put( 12, 23 );  // pN
      cancerMap.put( 13, 25 );  // pM

      boolean firstLine = true;
      try ( final BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( LSU_BRONZE_IN ) ) );
            final BufferedWriter brcaCancerWriter
                  = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( LSU_BRCA_CANCER_OUT ) ) );
            final BufferedWriter brcaTumorWriter
                  = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( LSU_BRCA_TUMOR_OUT ) ) );
         final BufferedWriter skinCancerWriter
               = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( LSU_SKIN_CANCER_OUT ) ) );
         final BufferedWriter skinTumorWriter
               = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( LSU_SKIN_TUMOR_OUT ) ) ) ) {
         brcaCancerWriter.write( "*cancer ID,*patient ID,body location,body location laterality,Temporality,clinical stage,clinical T classification,clinical N classification,clinical M classification,-clinical prefix,-clinical suffix,pathologic T classification,pathologic N classification,pathologic M classification,-pathologic prefix,-pathologic suffix\n" );
         brcaTumorWriter.write( "-geographically determined (yes/no),*patient ID,*cancer  link,*body location,*body location laterality,body location clockface position,body location quadrant,Diagnosis,tumor type,-cancer type,-histologic type,-tumor extent,er status interpretation,-er status numeric value,-er status method,pr status interpretation,-pr status numeric value,-pr status method,her2neu status interpretation,-her2neu status numeric value,-her2neu status method,-radiologic tumor size (mm),-radiologic tumor size procedure method,-pathologic tumor size (mm),-pathologic aggregate tumor size (mm),calcifications\n" );
         skinCancerWriter.write( "*cancer ID,*patient ID,body location,body location laterality,Temporality,clinical stage,clinical T classification,clinical N classification,clinical M classification,-clinical prefix,-clinical suffix,pathologic T classification,pathologic N classification,pathologic M classification,-pathologic prefix,-pathologic suffix\n" );
         skinTumorWriter.write( "-geographically determined (yes/no),*patient ID,*cancer  link,*body location,*body location laterality,body location clockface position,body location quadrant,Diagnosis,tumor type,-cancer type,-histologic type,-tumor extent,er status interpretation,-er status numeric value,-er status method,pr status interpretation,-pr status numeric value,-pr status method,her2neu status interpretation,-her2neu status numeric value,-her2neu status method,-radiologic tumor size (mm),-radiologic tumor size procedure method,-pathologic tumor size (mm),-pathologic aggregate tumor size (mm),calcifications\n" );
         String line = reader.readLine();
         final StringBuilder sb = new StringBuilder();
         while ( line != null ) {
            if ( firstLine || line.trim().isEmpty() ) {
               firstLine = false;
               line = reader.readLine();
               continue;
            }
            final String[] bronzeColumns = StringUtil.fastSplit( line, ',' );
            final String location = getLocation( bronzeColumns[ 2 ] );
            if ( bronzeColumns[ 10 ].isEmpty() ) {
               // Cancer
               sb.append( getCancerId( bronzeColumns ) );
               sb.append( ",patient" );
               sb.append( bronzeColumns[ 0 ] );
               sb.append( ',' );
               sb.append( location );
               sb.append( ',' );
               sb.append( getLaterality( bronzeColumns[ 4 ] ) );
               sb.append( ",Current" );
               for ( int i = 5; i < CANCER_COLUMN_COUNT; i++ ) {
                  sb.append( ',' );
                  final int bronzeColumn = cancerMap.getOrDefault( i, -1 );
                  if ( bronzeColumn >= 0 && bronzeColumn < bronzeColumns.length ) {
                     sb.append( bronzeColumns[ bronzeColumn ] );
                  }
               }
               sb.append( '\n' );
               if ( location.equals( "Breast" ) ) {
                  brcaCancerWriter.write( sb.toString() );
               } else {
                  skinCancerWriter.write( sb.toString() );
               }
            } else {
               // Metastasis
               sb.append( "yes," );
               sb.append( "patient" );
               sb.append( bronzeColumns[ 0 ] );
               sb.append( ',' );
               sb.append( getTumorId( bronzeColumns ) );
               sb.append( ',' );
               sb.append( location );
               sb.append( ',' );
               sb.append( getLaterality( bronzeColumns[ 4 ] ) );
               sb.append( ",," );
               sb.append( getQuadrant( bronzeColumns[ 2 ] ) );
               sb.append( ',' );
               sb.append( getDiagnosis( bronzeColumns[ 6 ] ) );
               sb.append( ',' );
               sb.append( getTumorType( bronzeColumns[ 11 ] ) );
               sb.append( ',' );
               sb.append( getDiagnosis( bronzeColumns[ 6 ] ) );
               sb.append( ',' );
               sb.append( getDuctLob( bronzeColumns[ 6 ] ) );
               sb.append( ',' );
               sb.append( getInvasion( bronzeColumns[ 6 ] ) );
               sb.append( '\n' );
               if ( location.equals( "Breast" ) ) {
                  brcaTumorWriter.write( sb.toString() );
               } else {
                  skinTumorWriter.write( sb.toString() );
               }
            }
            sb.setLength( 0 );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.severe( ioE.getMessage() );
      }
   }

   static private String getCancerId( final String[] bronzeColumns ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "cancer_patient" );
      sb.append( bronzeColumns[ 0 ] );
      sb.append( '_' );
      sb.append( getLaterality( bronzeColumns[ 4 ] ) );
      sb.append( '_' );
      sb.append( getLocation( bronzeColumns[ 2 ] ) );
      sb.append( "_Current" );
      return sb.toString();
   }

   static private String getTumorId( final String[] bronzeColumns ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "cancer_patient" );
      sb.append( bronzeColumns[ 0 ] );
      sb.append( '_' );
      sb.append( getLaterality( bronzeColumns[ 4 ] ) );
      sb.append( '_' );
      sb.append( getLocation( bronzeColumns[ 2 ] ) );
      sb.append( "_Current" );
      return sb.toString();
   }


   static private String getLocation( final String fullLocation ) {
      String postOf = getPostOf( fullLocation );
      String preSemi = getPreSemiColon( postOf );
      final StringBuilder sb = new StringBuilder();
      for ( int i=0; i<preSemi.length(); i++ ) {
         if ( i==0 || preSemi.charAt( i-1 ) == ' ' ) {
            sb.append( Character.toUpperCase( preSemi.charAt( i ) ) );
         } else {
            sb.append( preSemi.charAt( i ) );
         }
      }
      String spaceLess = sb.toString().replaceAll( " ", "" );
      return spaceLess;
   }

   static private String getLaterality( final String fullLaterality ) {
      if ( fullLaterality.equals( "Left" ) || fullLaterality.equals( "Right" ) ) {
         return fullLaterality;
      }
      return "";
   }

   static private String getDiagnosis( final String fullDiagnosis ) {
      String postOf = getPostOf( fullDiagnosis );
      String preSemi = getPreSemiColon( postOf );
      String preParenth = getPreParenthesis( preSemi );
      return preParenth;
   }

   static private String getQuadrant( final String fullLocation ) {
      int index = fullLocation.indexOf( "quadrant" );
      if ( index < 0 ) {
         return "";
      }
      return fullLocation.substring( 0, index + 8 ).trim();
   }

   // Distant, Regional; bla Nodes only, Localized, [empty]
   static private String getTumorType( final String lsuTumor ) {
      if ( lsuTumor.equals( "Localized" ) ) {
         return "PrimaryTumor";
      } else if ( lsuTumor.startsWith( "Regional" ) ) {
         return "Regional_Metastasis";
      } else if ( lsuTumor.equals( "Distant" ) ) {
         return "Metastasis";
      }
      return "";
   }

   static private String getDuctLob( final String diagnosis ) {
      if ( diagnosis.toLowerCase().contains( "duct" ) ) {
         return "Ductal";
      } else if ( diagnosis.toLowerCase().startsWith( "lobular" ) ) {
         return "Lobular";
      }
      return "";
   }

   static private String getInvasion( final String diagnosis ) {
      if ( diagnosis.toLowerCase().contains( "spreading" ) ) {
         return "Invasive_Lesion";
      } else if ( diagnosis.toLowerCase().startsWith( "infiltrating" ) ) {
         return "Invasive_Lesion";
      }
      return "In_Situ_Lesion";
   }

   static private String getPostOf( final String fullLocation ) {
      int index = fullLocation.indexOf( " of " );
      if ( index < 0 ) {
         return fullLocation;
      }
      return fullLocation.substring( index + 4 ).trim();
   }

   static private String getPreSemiColon( final String fullText ) {
      int index = fullText.indexOf( ";" );
      if ( index < 0 ) {
         return fullText;
      }
      return fullText.substring( 0, index );
   }

   static private String getPreParenthesis( final String fullText ) {
      int index = fullText.indexOf( '(' );
      if ( index < 0 ) {
         return fullText;
      }
      return fullText.substring( 0, index );
   }

}
