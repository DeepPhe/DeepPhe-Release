package org.apache.ctakes.cancer.phenotype.size;

import org.apache.ctakes.cancer.util.SpanOffsetComparator;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/7/2015
 */
final public class SizeFinder {

   private SizeFinder() {
   }

   static private final Logger LOGGER = Logger.getLogger( "SizeFinder" );

   static private final String VALUE_REGEX = "\\d+(?:\\.\\d+)?";
   static private final Pattern VALUE_PATTERN = Pattern.compile( VALUE_REGEX );

   static private final String FRONT_REGEX = "(?:\\b|\\(|\\[|\\{)";
   static private final String BACK_REGEX = "(?:\\b|,|\\.|\\?|!|\\)|\\]|\\})";
   static private final String DIM_REGEX = "\\s*(?:x|\\*)\\s*";
   static private final String UNIT_REGEX = "\\s*(?:\\bmm|cm\\b)";
   static private final String ONE_D_REGEX = FRONT_REGEX + VALUE_REGEX
         + UNIT_REGEX + BACK_REGEX;
   static private final String TWO_D_REGEX = FRONT_REGEX + VALUE_REGEX
         + DIM_REGEX + VALUE_REGEX
         + UNIT_REGEX + BACK_REGEX;
   static private final String THREE_D_REGEX = FRONT_REGEX + VALUE_REGEX
         + DIM_REGEX + VALUE_REGEX
         + DIM_REGEX + VALUE_REGEX
         + UNIT_REGEX + BACK_REGEX;

   static private final String FULL_REGEX = FRONT_REGEX + VALUE_REGEX
         + "(?:" + DIM_REGEX + VALUE_REGEX + "){0,2}+"
         + UNIT_REGEX + BACK_REGEX;

   static private final Pattern FULL_VALUE_PATTERN
         = Pattern.compile( VALUE_REGEX + "(?:" + DIM_REGEX + VALUE_REGEX + "){0,2}+" );

   static private final Pattern FULL_PATTERN = Pattern.compile( "(?:" + FULL_REGEX + ")", Pattern.CASE_INSENSITIVE );


   static private final String[] SIZE_STOP_WORD_ARRAY = { "distance", "from", "superior", "inferior", "anterior", "posterior" };
   static private final Collection<String> SIZE_STOP_WORDS;

   static {
      SIZE_STOP_WORDS = Arrays.asList( SIZE_STOP_WORD_ARRAY );
   }


   static public void addSentenceSizes( final JCas jcas, final Sentence sentence ) {
      final String sentenceText = sentence.getCoveredText().toLowerCase();
      final boolean hasStop = SIZE_STOP_WORDS.stream().anyMatch( sentenceText::contains );
      if ( hasStop ) {
         return;
      }
      final Collection<Quantity> quantities = getQuantities( sentenceText );
      if ( quantities.isEmpty() ) {
         return;
      }
      final int windowStartOffset = sentence.getBegin();
      for ( Quantity quantity : quantities ) {
         SizePhenotypeFactory.getInstance().createPhenotype( jcas, windowStartOffset, quantity );
      }
   }

   static List<Quantity> getQuantities( final String lookupWindow ) {
      if ( lookupWindow.length() < 3 ) {
         return Collections.emptyList();
      }
      final List<Quantity> quantities = new ArrayList<>();
      final Matcher fullMatcher = FULL_PATTERN.matcher( lookupWindow );
      while ( fullMatcher.find() ) {
         final String matchWindow = lookupWindow.substring( fullMatcher.start(), fullMatcher.end() );
         for ( QuantityUnit unitType : QuantityUnit.values() ) {
            final Matcher unitMatcher = unitType.getMatcher( matchWindow );
            if ( unitMatcher.find() ) {
               final int unitStart = fullMatcher.start() + unitMatcher.start();
               final int unitEnd = fullMatcher.start() + unitMatcher.end();
               final String unit = matchWindow.substring( unitMatcher.start(), unitMatcher.end() );
               final SpannedQuantityUnit spannedUnit = new SpannedQuantityUnit( unitType, unitStart, unitEnd );
               int bestStart = fullMatcher.end();
               int bestEnd = fullMatcher.start();
               final Matcher valueMatcher = FULL_VALUE_PATTERN.matcher( matchWindow );
               while ( valueMatcher.find() ) {
                  final int valueStart = fullMatcher.start() + valueMatcher.start();
                  final int valueEnd = fullMatcher.start() + valueMatcher.end();
                  bestStart = Math.min( bestStart, valueStart );
                  bestEnd = Math.max( bestEnd, valueEnd );
               }
               final QuantityValue value = new QuantityValue( lookupWindow.substring( bestStart, bestEnd ) );
               final Quantity quantity
                     = new Quantity( spannedUnit, new SpannedQuantityValue( value, bestStart, bestEnd ), unit );
               quantities.add( quantity );
               break;
            }
         }
      }
      quantities.sort( SpanOffsetComparator.getInstance() );
      return quantities;
   }


}
