//package org.healthnlp.deepphe.util.eval;
//
//
//import org.apache.ctakes.core.util.StringUtil;
//import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
//import org.healthnlp.deepphe.neo4j.constant.UriConstants;
//import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
//import org.neo4j.graphdb.GraphDatabaseService;
//
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.stream.Collectors;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 5/23/2019
// */
//final public class MatchUtil {
//
//   private MatchUtil() {
//   }
//
//   static double countMatched( final String goldValue, final String systemValue ) {
//      if ( isSpecialMatch( goldValue, systemValue ) ) {
//         return 1;
//      }
//      return countMatched( getUris( goldValue ), getUris( systemValue ) );
//   }
//
//   static private double countMatched( final Collection<String> uris1, final Collection<String> uris2 ) {
//      final int unmatched = countUnMatched( uris2, uris1 );
//      final double parts = countSpecialMatch( uris2, uris1 );
//      return uris2.size() - unmatched + parts;
//   }
//
//   static double countUnMatched( final String value1, final String value2 ) {
//      if ( isSpecialMatch( value1, value2 ) || isSpecialMatch( value2, value1 ) ) {
//         return 0;
//      }
//      return countUnMatched( getUris( value1 ), getUris( value2 ) );
//   }
//
//   static private int countUnMatched( final Collection<String> uris1, final Collection<String> uris2 ) {
//      int matchCount = 0;
//      for ( String uri : uris1 ) {
//         boolean matched = false;
//         for ( String uri2 : uris2 ) {
//            if ( uri.equalsIgnoreCase( uri2 ) ) {
//               matched = true;
//            } else if ( EvalUriUtil.isUriBranchMatch( uri, uri2 ) ) {
//               matched = true;
//            } else if ( EvalUriUtil.getCloseUriLeaf( uri, uri2 ) != null ) {
//               matched = true;
//            }
//            if ( matched ) {
//               matchCount++;
//               break;
//            }
//         }
//      }
//      return uris1.size() - matchCount;
//   }
//
//   static private double countSpecialMatch( final Collection<String> uris1, final Collection<String> uris2 ) {
//      double matchCount = 0;
//      for ( String uri : uris1 ) {
//         for ( String uri2 : uris2 ) {
//            matchCount += countSpecialMatch( uri, uri2 );
//         }
//      }
//      return matchCount;
//   }
//
//   static private double countSpecialMatch( final String uri1, final String uri2 ) {
//      if ( uri1.equalsIgnoreCase( "Bilateral" )
//           && (uri2.equalsIgnoreCase( "Left" )
//               || uri2.equalsIgnoreCase( "Right" )) ) {
//         return 0.5;
//      }
//      if ( uri2.equalsIgnoreCase( "Bilateral" )
//           && (uri1.equalsIgnoreCase( "Left" )
//               || uri1.equalsIgnoreCase( "Right" )) ) {
//         return 0.5;
//      }
//      return 0;
//   }
//
//   static boolean isEmptyMatch( final String goldValue, final String systemValue ) {
//      if ( goldValue == null ) {
//         return false;
//      }
//      if ( goldValue.equals( "[]" ) ) {
//         // [] is used for gold when there should be a value but none was determined.  e.g. hasHistologicType
//         return true;
//      } else if ( goldValue.toLowerCase().contains( "results not available" )
//                  || goldValue.toLowerCase().contains( "results not reported" ) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Absent" )
//           && (systemValue == null || systemValue.isEmpty() || systemValue.equalsIgnoreCase( "Absent" )) ) {
//         return true;
//      }
//      return false;
//   }
//
//   static private boolean isSpecialMatch( final String goldValue, final String systemValue ) {
//      if ( goldValue == null ) {
//         return false;
//      }
//      if ( goldValue.equals( "[]" ) ) {
//         // [] is used for gold when there should be a value but none was determined.  e.g. hasHistologicType
//         return true;
//      }
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
//      if ( goldValue.equalsIgnoreCase( "PrimaryTumor" ) ) {
//         if ( systemValue != null && !systemValue.isEmpty() &&
//              !UriConstants.getMetastasisUris( graphDb ).contains( systemValue ) ) {
//            return true;
//         }
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
//           && systemValue.contains( "Positive" ) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Negative" )
//           && systemValue != null
//           && systemValue.contains( "Negative" ) ) {
//         return true;
//      }
//      if ( goldValue.equalsIgnoreCase( "Unknown" )
//           && systemValue != null
//           && (systemValue.isEmpty() || systemValue.contains( "Unknown" )) ) {
//         return true;
//      }
//      return false;
//   }
//
//   static boolean isAnyMatch( final String goldValue, final String systemValue ) {
//      if ( isSpecialMatch( goldValue, systemValue ) ) {
//         return true;
//      }
//      if ( goldValue == null || goldValue.isEmpty() ) {
//         return systemValue == null || systemValue.isEmpty();
//      }
//      final Collection<String> goldUris = getUris( goldValue );
//      final Collection<String> systemUris = getUris( systemValue );
//      for ( String goldUri : goldUris ) {
//         for ( String systemUri : systemUris ) {
//            if ( goldUri.equalsIgnoreCase( systemUri ) ) {
//               return true;
//            }
//         }
//      }
//      if ( EvalUriUtil.isUriBranchMatch( goldUris, systemUris ) ) {
//         return true;
//      }
//      for ( String goldUri : goldUris ) {
//         final Collection<String> goldRoots = Neo4jOntologyConceptUtil.getRootUris( goldUri );
//         for ( String systemUri : systemUris ) {
//            final Collection<String> systemRoots = Neo4jOntologyConceptUtil.getRootUris( systemUri );
//            if ( EvalUriUtil.getCloseUriLeaf( goldUri, systemUri, goldRoots, systemRoots ) != null ) {
//               return true;
//            }
//         }
//      }
//      return false;
//   }
//
//   static private Collection<String> getUris( final String uriGroup ) {
//      if ( uriGroup == null || uriGroup.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      return Arrays.stream( StringUtil.fastSplit( uriGroup.trim(), ';' ) )
//                   .map( MatchUtil::getAdjustedUri )
//                   .map( String::trim )
//                   .filter( s -> !s.isEmpty() )
//                   .distinct()
//                   .sorted()
//                   .collect( Collectors.toList() );
//   }
//
//   static private String getAdjustedUri( final String uri ) {
//      if ( uri.endsWith( "_Stage_Finding" ) && (uri.startsWith( "c" ) || uri.startsWith( "p" )) ) {
//         return uri.substring( 1 );
//      }
//      return uri;
//   }
//
//}
