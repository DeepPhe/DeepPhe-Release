package org.apache.ctakes.cancer.phenotype.tnm;


import org.apache.ctakes.cancer.uri.UriAnnotationFactory;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

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


   static private final Collection<Character> PREFIX_CHARS = Arrays.asList( 'c', 'p', 'y', 'r', 'a', 'u' );
   static private final String PREFIX_REGEX = "(?:c|p|y|r|a|u)?";
   static private final String T_REGEX
         = "T=?(?:x|is|a|(?: ?n/a)|(?:[I]{1,3}V?)|(?:[0-4][a-z]?))(?![- ](?:weighted|axial))(?:\\((?:m|\\d+)?,?(?:is)?\\))?";
   static private final String N_REGEX = "N=?(?:x|(?: ?n/a)|(?:[I]{1,3})|(?:[0-3][a-z]?))";
   static private final String M_REGEX = "M=?(?:x|I|(?: ?n/a)|(?:[0-1][a-z]?))";

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


   static private final Pattern T_PATTERN = Pattern.compile( T_REGEX );
   static private final Pattern N_PATTERN = Pattern.compile( N_REGEX );
   static private final Pattern M_PATTERN = Pattern.compile( M_REGEX );

   static private final Pattern FULL_PATTERN = Pattern.compile( FULL_REGEX, Pattern.CASE_INSENSITIVE );


   private final Object LOCK = new Object();

   TnmFinder() {
   }


   static private final class SimpleTnm {
      private final int _begin;
      private final int _end;
      private final String _uri;

      private SimpleTnm( final char prefix, final int begin, final int end, String uri ) {
         _begin = begin;
         _end = end;
         if ( uri.endsWith( "1m" ) ) {
            // The ontology has T1mi and N1mi but not m alone
            uri = uri + "i";
         }
         final String fullUri = prefix + getRomanNumber( uri ) + "_Stage_Finding";
         _uri = fullUri.trim().replace( 'x', 'X' );
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
   }


   static public void addTnms( final JCas jcas, final AnnotationFS lookupWindow ) {
      final String windowText = lookupWindow.getCoveredText();
      final List<SimpleTnm> tnms = getTnms( windowText );
      if ( tnms.isEmpty() ) {
         return;
      }
      final int windowStartOffset = lookupWindow.getBegin();
      for ( SimpleTnm tnm : tnms ) {
         UriAnnotationFactory.createIdentifiedAnnotations( jcas,
               windowStartOffset + tnm._begin,
               windowStartOffset + tnm._end, tnm._uri, SemanticGroup.FINDING, "T033" );
      }
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
         char prefix = ' ';
         int pOffset = 0;
         final char pMatch = tnm.charAt( 0 );
         if ( PREFIX_CHARS.contains( pMatch ) ) {
            prefix = pMatch;
            pOffset = -1;
         }
         final Matcher tMatcher = T_PATTERN.matcher( tnm );
         if ( tMatcher.find() ) {
            tnms.add( new SimpleTnm( prefix,
                  fullMatchStart + tMatcher.start() + pOffset,
                  fullMatchStart + tMatcher.end(), tnm.substring( tMatcher.start(), tMatcher.end() ) ) );
         }
         final Matcher nMatcher = N_PATTERN.matcher( tnm );
         if ( nMatcher.find() ) {
            tnms.add( new SimpleTnm( prefix,
                  fullMatchStart + nMatcher.start() + pOffset,
                  fullMatchStart + nMatcher.end(), tnm.substring( nMatcher.start(), nMatcher.end() ) ) );
         }
         final Matcher mMatcher = M_PATTERN.matcher( tnm );
         if ( mMatcher.find() ) {
            tnms.add( new SimpleTnm( prefix,
                  fullMatchStart + mMatcher.start() + pOffset,
                  fullMatchStart + mMatcher.end(), tnm.substring( mMatcher.start(), mMatcher.end() ) ) );
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
