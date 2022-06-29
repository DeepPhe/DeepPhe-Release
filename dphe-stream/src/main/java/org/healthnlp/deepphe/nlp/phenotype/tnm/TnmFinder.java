package org.healthnlp.deepphe.nlp.phenotype.tnm;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.uri.UriAnnotationFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/26/2017
 */
public enum TnmFinder {
   INSTANCE;

   static public TnmFinder getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "TnmFinder" );


   static private final Collection<Character> PREFIX_CHARS = Arrays.asList( 'c', 'C', 'p', 'P', 'y', 'r', 'a', 'u' );
   static private final String PREFIX_REGEX = "[cpyrau]?";
   static private final String T_REGEX
         = "T=? {0,2}\\t?(?:x|is|a|(?: ?n\\/a)|(?:[I]{1,3}V?)|(?:[0-4][a-z]?))(?![- ](?:weighted|axial))(?:\\((?:m|\\d+)?,?(?:is)?\\))?";
   static private final String N_REGEX = "N=? {0,2}\\t?(?:x|(?: ?n\\/a)|(?:[I]{1,3})|(?:[0-3][a-z]?))";
   static private final String M_REGEX = "M=? {0,2}\\t?(?:x|I|(?: ?n\\/a)|(?:[0-1][a-z]?))";

   static private final String FULL_T_REGEX = "\\b(?:" + PREFIX_REGEX + T_REGEX + ")"
         + "(?:" + PREFIX_REGEX + N_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + M_REGEX + ")?\\b";

   static private final String FULL_N_REGEX = "\\b(?:" + PREFIX_REGEX + T_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + N_REGEX + ")"
         + "(?:" + PREFIX_REGEX + M_REGEX + ")?\\b";

   static private final String FULL_M_REGEX = "\\b"
         + "(?:" + PREFIX_REGEX + T_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + N_REGEX + ")?"
         + "(?:" + PREFIX_REGEX + M_REGEX + ")\\b";

   static private final String FULL_REGEX = "(?:" + FULL_T_REGEX + ")|(?:" + FULL_N_REGEX + ")|(?:" + FULL_M_REGEX + ")";


   static private final Pattern T_PATTERN = Pattern.compile( T_REGEX, Pattern.CASE_INSENSITIVE );
   static private final Pattern N_PATTERN = Pattern.compile( N_REGEX, Pattern.CASE_INSENSITIVE );
   static private final Pattern M_PATTERN = Pattern.compile( M_REGEX, Pattern.CASE_INSENSITIVE );

   static private final Pattern FULL_PATTERN = Pattern.compile( FULL_REGEX, Pattern.CASE_INSENSITIVE );


   private final Object LOCK = new Object();

   TnmFinder() {
   }


//   static private final class SimpleTnm {
//      private final int _begin;
//      private final int _end;
//      private String _uriSeed;
//      private boolean _isPathologic;
//      private SimpleTnm( final char prefix, final int begin, final int end, String uri ) {
//         _begin = begin;
//         _end = end;
//         if ( prefix == 'p' ) {
//            _isPathologic = true;
//         }
//         setUriSeed( uri );
//      }
//      private void setPathologic() {
//         _isPathologic = true;
//      }
//      private void setUriSeed( final String uriSeed ) {
//         String uri = uriSeed;
//         if ( uri.contains( "T" ) && uri.endsWith( "N" ) ) {
//            // Fix erroneous T1N from T1N1
//            uri = uri.substring( 0, uri.length() - 1 );
//         } else if ( uri.contains( "N" ) && uri.endsWith( "M" ) ) {
//            // Fix erroneous N1M from N1M1
//            uri = uri.substring( 0, uri.length() - 1 );
//         }
//         if ( uri.endsWith( "1m" ) ) {
//            // The ontology has T1mi and N1mi but not m alone
//            uri = uri + "i";
//         } else if ( uri.toLowerCase().endsWith( "n/a" ) ) {
//            uri = uri.substring( 0, uri.length() - 3 ) + "x";
//         }
//         final String partUri = getRomanNumber( uri );
//         _uriSeed = partUri.trim()
//                          .replace( 'x', 'X' )
//                          .replace( 'A', 'a' )
//                          .replace( 'B', 'b' )
//                          .replace( 'C', 'c' )
//                          .replace( "=", "" )
//                          .replace( " ", "" );
//      }
//      public String getUri() {
//         String fullUri = "";
//         if ( _isPathologic ) {
//            fullUri = _uriSeed + "_Stage_Finding";
//         } else {
//            if ( _uriSeed.startsWith( "T" ) ) {
//               fullUri = "Tumor_stage_" + _uriSeed;
//            } else if ( _uriSeed.startsWith( "N" ) ) {
//               fullUri = "Node_stage_" + _uriSeed;
//            } else if ( _uriSeed.startsWith( "M" ) ) {
//               fullUri = "Metastasis_stage_" + _uriSeed;
//            }
//         }
//         return fullUri;
//      }
//      static private String getRomanNumber( final String text ) {
//         if ( text.charAt( 1 ) != 'I' ) {
//            return text;
//         }
//         return text.replace( "IV", "4" )
//                    .replace( "III" ,"3" )
//                    .replace( "II", "2" )
//                    .replace( "I", "1" );
//      }
//
//      static private boolean isWanted( final char prefix, final String uri ) {
//         return !uri.equals( "TA" ) && !(prefix == 'a' && uri.equals( "ni" ));
//      }
//   }

   static private final class SimpleTnm {
      private final int _begin;
      private final int _end;
      private String _uriSeed;
      private boolean _isPathologic;

      private SimpleTnm( final char prefix, final int begin, final int end, String uri ) {
         _begin = begin;
         _end = end;
         if ( prefix == 'p' || prefix == 'P' ) {
            _isPathologic = true;
         }
         setUriSeed( uri );
      }

      private void setPathologic() {
         _isPathologic = true;
      }

      private void setUriSeed( final String uriSeed ) {
//         String uri = uriSeed;
         String uri = uriSeed.substring( 0, 2 ).toUpperCase();
         if ( uriSeed.length() > 2 ) {
            uri = uri + uriSeed.substring( 2 );
         }
         if ( uri.contains( "T" ) && uri.endsWith( "N" ) ) {
            // Fix erroneous T1N from T1N1
            uri = uri.substring( 0, uri.length() - 1 );
         } else if ( uri.contains( "N" ) && uri.endsWith( "M" ) ) {
            // Fix erroneous N1M from N1M1
            uri = uri.substring( 0, uri.length() - 1 );
         }
         if ( uri.endsWith( "1m" ) ) {
            // The ontology has T1mi and N1mi but not m alone
            uri = uri + "i";
         } else if ( uri.toLowerCase().endsWith( "n/a" ) ) {
            uri = uri.substring( 0, uri.length() - 3 ) + "x";
         }
         final String partUri = getRomanNumber( uri );
         _uriSeed = partUri.trim()
                           .replace( 'X', 'x' )
                           .replace( 'A', 'a' )
                           .replace( 'B', 'b' )
                           .replace( 'C', 'c' )
                           .replace( "=", "" )
                           .replace( "(", "" )
                           .replace( ")", "" )
                           .replace( " ", "" );
      }

      public String getUri() {
         if ( _isPathologic ) {
//            return "p" + _uriSeed + "_Stage";
            return "P" + _uriSeed + "_Stage";  // As of ontology 62 all class names are capitalized
         }
         return _uriSeed + "_Stage";
      }
      static private String getRomanNumber( final String text ) {
         if ( text.charAt( 1 ) != 'I' ) {
            return text;
         }
         return text.replace( "IV", "4" )
                    .replace( "III" ,"3" )
                    .replace( "II", "2" )
                    .replace( "I", "1" );
      }

      static private boolean isWanted( final char prefix, final String uri ) {
         return !uri.equals( "TA" ) && !(prefix == 'a' && uri.equals( "ni" ));
      }
   }

   static public List<IdentifiedAnnotation> addTnms( final JCas jcas, final AnnotationFS lookupWindow ) {
      final String windowText = lookupWindow.getCoveredText();
      final List<SimpleTnm> tnms = getTnms( windowText );
      if ( tnms.isEmpty() ) {
         return Collections.emptyList();
      }
      final boolean isPathologic = windowText.toLowerCase().contains( "patholog" );
      if ( isPathologic ) {
         tnms.forEach( SimpleTnm::setPathologic );
      }
      final int windowStartOffset = lookupWindow.getBegin();
      final List<IdentifiedAnnotation> tnmAnnotations = new ArrayList<>();
      for ( SimpleTnm tnm : tnms ) {
         final Collection<IdentifiedAnnotation> annotations
               = UriAnnotationFactory.createIdentifiedAnnotations( jcas,
               windowStartOffset + tnm._begin,
               windowStartOffset + tnm._end, tnm.getUri(), SemanticGroup.FINDING, "T033" );
         tnmAnnotations.addAll( annotations );
      }
      return tnmAnnotations;
   }


   static List<SimpleTnm> getTnms( final String lookupWindow ) {
      if ( lookupWindow.length() < 3 ) {
         return new ArrayList<>();
      }
      final List<SimpleTnm> tnms = new ArrayList<>();
      final Matcher fullMatcher = FULL_PATTERN.matcher( lookupWindow );
      while ( fullMatcher.find() ) {
         final int fullMatchStart = fullMatcher.start();
         final String tnm = lookupWindow.substring( fullMatchStart, fullMatcher.end() );
         if ( tnm.startsWith( "at " ) || tnm.startsWith( "an " ) || tnm.startsWith( "am " ) ) {
            continue;
         }
         char prefix = ' ';
         int pOffset = 0;
         final char pMatch = tnm.charAt( 0 );
         if ( PREFIX_CHARS.contains( pMatch ) ) {
            prefix = pMatch;
            pOffset = -1;
         }
         final Matcher tMatcher = T_PATTERN.matcher( tnm );
         if ( tMatcher.find()
              && SimpleTnm.isWanted( prefix, tnm.substring( tMatcher.start(), tMatcher.end() ) ) ) {
            tnms.add( new SimpleTnm( prefix,
                  fullMatchStart + tMatcher.start() + pOffset,
                  fullMatchStart + tMatcher.end(), tnm.substring( tMatcher.start(), tMatcher.end() ) ) );
         }
         final Matcher nMatcher = N_PATTERN.matcher( tnm );
         if ( nMatcher.find()
              && SimpleTnm.isWanted( prefix, tnm.substring( nMatcher.start(), nMatcher.end() ) ) ) {
            tnms.add( new SimpleTnm( prefix,
                  fullMatchStart + nMatcher.start() + pOffset,
                  fullMatchStart + nMatcher.end(), tnm.substring( nMatcher.start(), nMatcher.end() ) ) );
         }
         final Matcher mMatcher = M_PATTERN.matcher( tnm );
         if ( mMatcher.find() ) {
            if ( mMatcher.start() >= 2 && tnm.substring( mMatcher.start()-2, mMatcher.start()+2 ).equals( "N1mi" ) ) {
               // do nothing
            } else {
               tnms.add( new SimpleTnm( prefix,
                                        fullMatchStart + mMatcher.start() + pOffset,
                                        fullMatchStart + mMatcher.end(),
                                        tnm.substring( mMatcher.start(), mMatcher.end() ) ) );
            }
         }
      }
      return tnms;
   }

   public Collection<SignSymptomMention> findTnms( final JCas jcas, final AnnotationFS lookupWindow ) {
      final String lookupWindowText = lookupWindow.getCoveredText();
      if ( lookupWindowText.length() < 2 ) {
         return Collections.emptyList();
      }
      final Collection<SignSymptomMention> tnms = new ArrayList<>();
      final int windowStartOffset = lookupWindow.getBegin();
      try ( RegexSpanFinder finder = new RegexSpanFinder( FULL_PATTERN ) ) {
         final List<Pair<Integer>> fullSpans = finder.findSpans( lookupWindowText );
         for ( Pair<Integer> fullSpan : fullSpans ) {
            final String matchWindow = lookupWindowText.substring( fullSpan.getValue1(), fullSpan.getValue2() );
            if ( matchWindow.trim().isEmpty() ) {
               continue;
            }
         }
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.warn( iaE.getMessage() );
      }
      return tnms;
   }


}
