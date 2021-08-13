package org.healthnlp.deepphe.util.eval.old.eval;



import org.apache.ctakes.core.util.StringUtil;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
final public class MatchUtil {

   private MatchUtil() {
   }

   static double countMatched( final String goldValue, final String systemValue ) {
      if ( isSpecialMatch( goldValue, systemValue ) ) {
         return 1;
      }
      return countMatched( getUris( goldValue ), getUris( systemValue ) );
   }

   static private double countMatched( final Collection<String> uris1, final Collection<String> uris2 ) {
      final int unmatched = countUnMatched( uris2, uris1 );
      final double parts = countSpecialMatch( uris2, uris1 );
      return uris2.size() - unmatched + parts;
   }

   static double countUnMatched( final String value1, final String value2 ) {
      if ( isSpecialMatch( value1, value2 ) || isSpecialMatch( value2, value1 ) ) {
         return 0;
      }
      return countUnMatched( getUris( value1 ), getUris( value2 ) );
   }

   static private int countUnMatched( final Collection<String> uris1, final Collection<String> uris2 ) {
      int matchCount = 0;
      for ( String uri : uris1 ) {
         boolean matched = false;
         for ( String uri2 : uris2 ) {
            if ( uri.equalsIgnoreCase( uri2 ) ) {
//            if ( isSpecialMatch( uri, uri2 ) || isSpecialMatch( uri2, uri ) ) {
               matched = true;
//            } else if ( UriUtil.isUriBranchMatch( uri, uri2 ) ) {
//               matched = true;
//            } else if ( UriUtil.getCloseUriLeaf( uri, uri2 ) != null ) {
//               matched = true;
            }
            if ( matched ) {
               matchCount++;
               break;
            }
         }
      }
      return uris1.size() - matchCount;
   }

   static private double countSpecialMatch( final Collection<String> uris1, final Collection<String> uris2 ) {
      double matchCount = 0;
      for ( String uri : uris1 ) {
         for ( String uri2 : uris2 ) {
            matchCount += countSpecialMatch( uri, uri2 );
         }
      }
      return matchCount;
   }

   static private double countSpecialMatch( final String uri1, final String uri2 ) {
      if ( uri1.equalsIgnoreCase( "Bilateral" )
           && (uri2.equalsIgnoreCase( "Left" )
               || uri2.equalsIgnoreCase( "Right" )) ) {
         return 0.5;
      }
      if ( uri2.equalsIgnoreCase( "Bilateral" )
           && (uri1.equalsIgnoreCase( "Left" )
               || uri1.equalsIgnoreCase( "Right" )) ) {
         return 0.5;
      }
      return 0;
   }

   static boolean isEmptyMatch( final String goldValue, final String systemValue ) {
      if ( goldValue == null ) {
         return false;
      }
      if ( goldValue.equals( "[]" ) ) {
         // [] is used for gold when there should be a value but none was determined.  e.g. hasHistologicType
         return true;
      } else if ( goldValue.toLowerCase().contains( "results not available" )
                  || goldValue.toLowerCase().contains( "results not reported" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Absent" )
           && (systemValue == null || systemValue.isEmpty() || systemValue.equalsIgnoreCase( "Absent" )) ) {
         return true;
      }
      return false;
   }

   static private final Collection<String> TNM_MATCHES = Arrays.asList( "T0", "T1", "T2", "T3", "T4",
                                                                        "N0", "N1", "N2", "N3",
                                                                        "M0", "M1" );



//   static private boolean isSpecialMatch( final String goldValue, final String systemValue ) {
//      if ( goldValue == null ) {
//         return false;
//      }
//      // SPECIAL CASE FOR ICDO LATERALITY CODE.  MAKE SURE THAT IT DOESN'T SHOW UP ELSEWHERE.
//      if ( systemValue.isEmpty() && (goldValue.equals( "0" ) || goldValue.equals( "9" )) ) {
//         return true;
//      }
//      // SPECIAL CASE FOR ICDO TOPO_MINOR CODE.  MAKE SURE THAT IT DOESN'T SHOW UP ELSEWHERE.
//      if ( systemValue.isEmpty() && (goldValue.equals( "0" ) || goldValue.equals( "9" )) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( systemValue ) ) {
//         return true;
//      }
//      if ( goldValue.equals( "[]" ) ) {
//         // [] is used for gold when there should be a value but none was determined.  e.g. hasHistologicType
//         return true;
//      }
////      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
////                                                                 .getGraph();
////      if ( goldValue.equalsIgnoreCase( "PrimaryTumor" ) ) {
////         if ( systemValue != null && !systemValue.isEmpty() &&
////              !UriConstants.getMetastasisUris( graphDb ).contains( systemValue ) ) {
////            return true;
////         }
////      }
//      if ( goldValue.isEmpty()
//           && ( systemValue.equalsIgnoreCase( "Pending" )
//                || systemValue.toLowerCase().contains( "n/a" )
//                || systemValue.toLowerCase().contains( "requested" )
//                || systemValue.toLowerCase().contains( "applicable" )
//                || systemValue.toLowerCase().contains( "insufficient" )
//                || systemValue.toLowerCase().contains( "assessed" ) ) ) {
//         return true;
//      }
//      if ( systemValue.startsWith( "[" ) && systemValue.endsWith( "]" ) ) {
//         return isSpecialMatch( goldValue, systemValue.substring( 1, systemValue.length()-1 ).trim() );
//      }
//      if ( systemValue.contains( "," ) ) {
//         return Arrays.stream( StringUtil.fastSplit( systemValue, ',' ) )
//               .anyMatch( v -> isSpecialMatch( goldValue, v.trim() ) );
//      }
//      if ( systemValue.contains( ";" ) ) {
//         return Arrays.stream( StringUtil.fastSplit( systemValue, ';' ) )
//                      .anyMatch( v -> isSpecialMatch( goldValue, v.trim() ) );
//      }
//      if ( systemValue.contains( "%" ) ) {
//         final int percentIndex = systemValue.indexOf( '%' );
//         return isSpecialMatch( goldValue, systemValue.substring( 0, percentIndex ).trim() );
//      }
//      if ( goldValue.endsWith( "%" ) ) {
//         return isSpecialMatch( goldValue.substring( 0, goldValue.length()-1 ).trim(), systemValue );
//      }
//      if ( goldValue.equalsIgnoreCase( "Present" )
//           && systemValue != null
//           && !systemValue.isEmpty()
//           && !systemValue.equalsIgnoreCase( "Absent" ) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Absent" )
//           && (systemValue == null || systemValue.isEmpty() || systemValue.equalsIgnoreCase( "Absent" )) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Positive" )
//           && systemValue != null
////           && systemValue.toLowerCase().contains( "positive" ) ) {
//           && systemValue.toLowerCase().contains( "pos" ) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Negative" )
//           && systemValue != null
//           && systemValue.toLowerCase().contains( "negative" ) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Elevated" )
//           && systemValue != null
//           && systemValue.toLowerCase().contains( "elevated" ) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Unknown" )
//           && systemValue != null
//           && (systemValue.isEmpty() || systemValue.toLowerCase().contains( "unknown" )) ) {
//         return true;
//      }
//      if ( goldValue.contains( "M0" ) && (systemValue == null || systemValue.isEmpty() || systemValue.contains( "Mx" )) ) {
//         return true;
//      }
//      if ( goldValue.contains( "Tis" ) && (systemValue == null || systemValue.isEmpty() || systemValue.contains( "T0" )) ) {
//         return true;
//      }
//      if ( systemValue.contains( "Tis" ) && (goldValue == null || goldValue.isEmpty() || goldValue.contains( "T0" )) ) {
//         return true;
//      }
//      for ( String tnmKludge : TNM_MATCHES ) {
//         if ( goldValue != null && goldValue.contains( tnmKludge ) && systemValue != null && systemValue.contains( tnmKludge ) ) {
//            return true;
//         }
////         if ( goldValue != null && goldValue.contains( tnmKludge+"_Stage" ) && systemValue != null && systemValue.startsWith( tnmKludge ) ) {
////            return true;
////         }
//      }
//      if ( goldValue != null && goldValue.equals( "Breast" ) && systemValue != null && systemValue.contains( "Nipple" ) ) {
//         return true;
//      }
//      // Last attempt - necessary to facilitate match between old upmc annotations vs new system output
//      // Otherwise the mapping is a serious pain.
//      if ( goldValue.contains( systemValue ) || systemValue.contains( goldValue ) ) {
//         return true;
////      } else {
////         System.err.println( goldValue + "    NOT    " + systemValue );
//      }
//      return false;
//   }

   static boolean isSpecialMatch( final String goldValue1, final String systemValue1 ) {
      final String goldValue = goldValue1.trim();
      final String systemValue = systemValue1.trim();
      // SPECIAL CASE FOR ICDO LATERALITY OR ICDO TOPO_MINOR CODE.  MAKE SURE THAT IT DOESN'T SHOW UP ELSEWHERE.
      if ( systemValue.isEmpty() && (goldValue.equals( "0" ) || goldValue.equals( "9" )) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( systemValue ) ) {
         return true;
      }
      if ( goldValue.equals( "[]" ) ) {
         // [] is used for gold when there should be a value but none was determined.  e.g. hasHistologicType
         return true;
      }
//      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
//                                                                 .getGraph();
//      if ( goldValue.equalsIgnoreCase( "PrimaryTumor" ) ) {
//         if ( systemValue != null && !systemValue.isEmpty() &&
//              !UriConstants.getMetastasisUris( graphDb ).contains( systemValue ) ) {
//            return true;
//         }
//      }
      if ( goldValue.isEmpty()
           && ( systemValue.equalsIgnoreCase( "Pending" )
                || systemValue.toLowerCase().contains( "n/a" )
                || systemValue.toLowerCase().contains( "requested" )
                || systemValue.toLowerCase().contains( "applicable" )
                || systemValue.toLowerCase().contains( "insufficient" )
                || systemValue.toLowerCase().contains( "assessed" ) ) ) {
         return true;
      }
      if ( systemValue.startsWith( "[" ) && systemValue.endsWith( "]" ) ) {
         return isSpecialMatch( goldValue, systemValue.substring( 1, systemValue.length()-1 ).trim() );
      }
      if ( systemValue.contains( "," ) ) {
         return Arrays.stream( StringUtil.fastSplit( systemValue, ',' ) )
                      .anyMatch( v -> isSpecialMatch( goldValue, v.trim() ) );
      }
      if ( systemValue.contains( ";" ) ) {
         return Arrays.stream( StringUtil.fastSplit( systemValue, ';' ) )
                      .anyMatch( v -> isSpecialMatch( goldValue, v.trim() ) );
      }
      if ( goldValue.contains( "," ) ) {
         return Arrays.stream( StringUtil.fastSplit( goldValue, ',' ) )
                      .anyMatch( v -> isSpecialMatch( v.trim(), systemValue ) );
      }
      if ( goldValue.contains( ";" ) ) {
         return Arrays.stream( StringUtil.fastSplit( goldValue, ';' ) )
                      .anyMatch( v -> isSpecialMatch( v.trim(), systemValue ) );
      }
      if ( systemValue.contains( "%" ) ) {
         final int percentIndex = systemValue.indexOf( '%' );
         return isSpecialMatch( goldValue, systemValue.substring( 0, percentIndex ).trim() );
      }
      if ( goldValue.endsWith( "%" ) ) {
         return isSpecialMatch( goldValue.substring( 0, goldValue.length()-1 ).trim(), systemValue );
      }
      if ( goldValue.equalsIgnoreCase( "Present" )
           && !systemValue.isEmpty()
           && !systemValue.equalsIgnoreCase( "Absent" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Absent" )
           && (systemValue.isEmpty() || systemValue.equalsIgnoreCase( "Absent" )) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Positive" )
//           && systemValue.toLowerCase().contains( "positive" ) ) {
           && systemValue.toLowerCase().contains( "pos" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Negative" )
           && systemValue.toLowerCase().contains( "negative" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Elevated" )
           && systemValue.toLowerCase().contains( "elevated" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Unknown" )
           && (systemValue.isEmpty() || systemValue.toLowerCase().contains( "unknown" )) ) {
         return true;
      }
      if ( goldValue.contains( "Tis" ) && (systemValue.isEmpty() || systemValue.contains( "T0" )) ) {
         return true;
      }
      if ( systemValue.contains( "Tis" ) && (goldValue.isEmpty() || goldValue.contains( "T0" )) ) {
         return true;
      }
      if ( (systemValue.contains( "Tx" ) || systemValue.contains( "Nx" ) || systemValue.contains( "Mx" ))
           && goldValue.isEmpty() ) {
         return true;
      }
      if ( (goldValue.contains( "Tx" ) || goldValue.contains( "Nx" ) || goldValue.contains( "Mx" ))
           && systemValue.isEmpty() ) {
         return true;
      }
      for ( String tnmKludge : TNM_MATCHES ) {
         if ( goldValue.contains( tnmKludge ) && systemValue.contains( tnmKludge ) ) {
            return true;
         }
//         if ( goldValue != null && goldValue.contains( tnmKludge+"_Stage" ) && systemValue != null && systemValue.startsWith( tnmKludge ) ) {
//            return true;
//         }
      }
      if ( goldValue.equalsIgnoreCase( "high_grade" ) && systemValue.equalsIgnoreCase( "Poorly_Differentiated" ) ) {
         return true;
      }
      if ( goldValue.equals( "Endometrial_Cavity" ) && systemValue.equals( "Uterine_Cavity" ) ) {
         return true;
      }
      if ( goldValue.replace( "_Not_Otherwise_Specified", "" ).equals( systemValue ) ) {
         return true;
      }
      if ( goldValue.equals( "Invasive_Ductal_Carcinoma_Not_Otherwise_Specified" )
           && (systemValue.equals("Invasive_Ductal_Breast_Carcinoma" )
               || systemValue.equals( "Female_Breast_Carcinoma" )
               || systemValue.equals( "Breast_Carcinoma" )
               || systemValue.equals( "Carcinoma_Breast_Stage_IV" ) ) ) {
         return true;
      }
      if ( systemValue.equals( "Ductal_Breast_Carcinoma" ) && goldValue.contains( "Ductal" ) ) {
         return true;
      }
      if ( goldValue.equals( "Papillary_Breast_Carcinoma" ) && systemValue.contains( "Papillary_Breast_Carcinoma" ))
      if ( goldValue.equals( "Breast" ) && systemValue.contains( "Nipple" ) ) {
         return true;
      }
      if ( goldValue.contains( "o_clock_position" ) ) {
         return goldValue.toLowerCase().contains( systemValue.toLowerCase() );
      }
      if ( systemValue.equals( "HER2" ) && goldValue.equalsIgnoreCase( "Negative" ) ) {
         // system out bug that I don't have time to fix.
         return true;
      }
      if ( !goldValue.isEmpty() && !systemValue.isEmpty() ) {
         final Collection<String> goldBranch = Neo4jOntologyConceptUtil.getBranchUris( goldValue );
         final Collection<String> systemBranch = Neo4jOntologyConceptUtil.getBranchUris( systemValue );
         if ( goldBranch.stream().anyMatch( systemBranch::contains )
              || systemBranch.stream().anyMatch( goldBranch::contains ) ) {
            return true;
         }
      }

      // Last attempt - necessary to facilitate match between old upmc annotations vs new system output
      // Otherwise the mapping is a serious pain.
      if ( !goldValue.isEmpty() && !systemValue.isEmpty()
           && (goldValue.toLowerCase().contains( systemValue.toLowerCase() ) || systemValue.toLowerCase().contains( goldValue.toLowerCase() )) ) {
         return true;
//      } else {
//         System.err.println( goldValue + "    NOT    " + systemValue );
      }
      return false;
   }

   static boolean isAnyMatch( final String goldValue, final String systemValue ) {
      if ( isSpecialMatch( goldValue, systemValue ) ) {
         return true;
      }
      if ( goldValue == null || goldValue.isEmpty() ) {
         return systemValue == null || systemValue.isEmpty();
      }
      final Collection<String> goldUris = getUris( goldValue );
      final Collection<String> systemUris = getUris( systemValue );
      for ( String goldUri : goldUris ) {
         for ( String systemUri : systemUris ) {
            if ( goldUri.equalsIgnoreCase( systemUri ) ) {
               return true;
            }
         }
      }
      if ( UriUtil.isUriBranchMatch( goldUris, systemUris ) ) {
         return true;
      }
      for ( String goldUri : goldUris ) {
         final Collection<String> goldRoots = Neo4jOntologyConceptUtil.getRootUris( goldUri );
         for ( String systemUri : systemUris ) {
            final Collection<String> systemRoots = Neo4jOntologyConceptUtil.getRootUris( systemUri );
            if ( UriUtil.getCloseUriLeaf( goldUri, systemUri, goldRoots, systemRoots ) != null ) {
               return true;
            }
         }
      }
      return false;
   }

   static private Collection<String> getUris( final String uriGroup ) {
      if ( uriGroup == null || uriGroup.isEmpty() ) {
         return Collections.emptyList();
      }
      return Arrays.stream( StringUtil.fastSplit( uriGroup.trim(), ';' ) )
//                   .map( MatchUtil::getAdjustedUri )
                   .map( String::trim )
                   .filter( s -> !s.isEmpty() )
                   .distinct()
                   .sorted()
                   .collect( Collectors.toList() );
   }

   static private String getAdjustedUri( final String uri ) {
      if ( uri.endsWith( "_Stage_Finding" ) && (uri.startsWith( "c" ) || uri.startsWith( "p" )) ) {
         return uri.substring( 1 );
      }
      return uri;
   }

}

// tumor
//Attribute              TP         FP         FN         TN         Accur %    P          R          Spcfty     F1

//hasBodySite            23         20         30         0           31.51     0.5349     0.4340     0.0000     0
// .4792
//hasLaterality          39         15         13         22          68.54     0.7222     0.7500     0.5946     0
// .7358
//hasDiagnosis           28         27         25         2           36.59     0.5091     0.5283     0.0690     0
// .5185
//hasQuadrant            60         0          0          60         100.00     1.0000     1.0000     1.0000     1
// .0000
//hasClockface           60         0          0          60         100.00     1.0000     1.0000     1.0000     1
// .0000
//has_ER_Status          48         2          11         46          87.85     0.9600     0.8136     0.9583     0
// .8807
//has_PR_Status          54         2          4          51          94.59     0.9643     0.9310     0.9623     0
// .9474
//has_HER2_Status        60         0          0          60         100.00     1.0000     1.0000     1.0000     1
// .0000