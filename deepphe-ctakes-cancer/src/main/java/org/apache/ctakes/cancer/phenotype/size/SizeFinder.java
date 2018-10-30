package org.apache.ctakes.cancer.phenotype.size;

import org.apache.ctakes.cancer.uri.UriAnnotationFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

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
   static private final String UNIT_REGEX = "\\s*(?:mm|cm)";
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
   static private final int STOP_DISTANCE = 15;

   static public void addSentenceSizes( final JCas jcas, final Sentence sentence ) {
      final String sentenceText = sentence.getCoveredText().toLowerCase();
      final Collection<Integer> stopIndices = Arrays.stream( SIZE_STOP_WORD_ARRAY )
            .map( sentenceText::indexOf )
            .filter( i -> i >= 0 )
            .collect( Collectors.toList() );
      final int windowStartOffset = sentence.getBegin();
      final Matcher fullMatcher = FULL_PATTERN.matcher( sentenceText );
      while ( fullMatcher.find() ) {
         boolean skip = false;
         for ( int stopIndex : stopIndices ) {
            if ( Math.abs( fullMatcher.start() + (fullMatcher.end() - fullMatcher.start()) / 2 - stopIndex ) < STOP_DISTANCE ) {
               skip = true;
               break;
            }
         }
         if ( !skip ) {
            UriAnnotationFactory.createIdentifiedAnnotations( jcas,
                  windowStartOffset + fullMatcher.start(),
                  windowStartOffset + fullMatcher.end(), UriConstants.SIZE,
                  sentenceText.substring( fullMatcher.start(), fullMatcher.end() ) );
         }
      }
   }


static public final class SizeFinderTester {
   static private final String MULTIPLE_MENTION_SENTENCE_1 = "INVASIVE DUCTAL CARCINOMA, 2.1 CM LEFT BREAST_URI;";
   static private final String MULTIPLE_MENTION_SENTENCE_2 = " DUCTAL CARCINOMA IN SITU, 900mm. RIGHT BREAST_URI 12:00.";
   static private final String MULTIPLE_MENTION_SENTENCE_3 = "  Total Tumor size 1.4x1.9 cm";
   static private final String MULTIPLE_MENTION_SENTENCE_4 = "INVASIVE DUCTAL CARCINOMA, 2.8 * 1.4 * 1.9 mm";

   static private final String MULTIPLE_MENTION_SENTENCE_5
         = "INVASIVE DUCTAL CARCINOMA, 2.8*1.4*1.9 mm, DUCTAL CARCINOMA IN SITU (2cm).";

   static private final String SENTENCE_4 = "There are three contrast enhancing lesions in the right parietal lobe,"
         + " the largest measuring 0.9 cm, suggestive of metastatic tumor.";

   public SizeFinderTester() {}

   @Test
   public void testCapUnitMention() {
      final Matcher fullMatcher = FULL_PATTERN.matcher( MULTIPLE_MENTION_SENTENCE_1 );
      int count = 0;
      while ( fullMatcher.find() ) {
         System.out.println( MULTIPLE_MENTION_SENTENCE_1.substring( fullMatcher.start(), fullMatcher.end() ) );
         count++;
      }
      assertEquals( "Expect one Cancer Size in " + MULTIPLE_MENTION_SENTENCE_1, 1, count );
   }

   @Test
   public void testNoTimeMention() {
      final Matcher fullMatcher = FULL_PATTERN.matcher( MULTIPLE_MENTION_SENTENCE_2 );
      int count = 0;
      while ( fullMatcher.find() ) {
         System.out.println( MULTIPLE_MENTION_SENTENCE_2.substring( fullMatcher.start(), fullMatcher.end() ) );
         count++;
      }
      assertEquals( "Expect one Cancer Size in " + MULTIPLE_MENTION_SENTENCE_2, 1, count );
   }


   @Test
   public void testSingleMention() {
      final Matcher fullMatcher = FULL_PATTERN.matcher( SENTENCE_4 );
      int count = 0;
      while ( fullMatcher.find() ) {
         System.out.println( SENTENCE_4.substring( fullMatcher.start(), fullMatcher.end() ) );
         count++;
      }
      assertEquals( "Expect one Cancer Size in " + SENTENCE_4, 1, count );
   }

   @Test
   public void testDoubleMention() {
      final Matcher fullMatcher = FULL_PATTERN.matcher( MULTIPLE_MENTION_SENTENCE_3 );
      int count = 0;
      while ( fullMatcher.find() ) {
         System.out.println( MULTIPLE_MENTION_SENTENCE_3.substring( fullMatcher.start(), fullMatcher.end() ) );
         count++;
      }
      assertEquals( "Expect one Cancer Size in " + MULTIPLE_MENTION_SENTENCE_3, 1, count );
   }

   @Test
   public void testTripleMention() {
      final Matcher fullMatcher = FULL_PATTERN.matcher( MULTIPLE_MENTION_SENTENCE_4 );
      int count = 0;
      while ( fullMatcher.find() ) {
         System.out.println( MULTIPLE_MENTION_SENTENCE_4.substring( fullMatcher.start(), fullMatcher.end() ) );
         count++;
      }
      assertEquals( "Expect one Cancer Size in " + MULTIPLE_MENTION_SENTENCE_4, 1, count );
   }

   @Test
   public void testTripleAndMention() {
      final Matcher fullMatcher = FULL_PATTERN.matcher( MULTIPLE_MENTION_SENTENCE_5 );
      int count = 0;
      while ( fullMatcher.find() ) {
         System.out.println( MULTIPLE_MENTION_SENTENCE_5.substring( fullMatcher.start(), fullMatcher.end() ) );
         count++;
      }
      assertEquals( "Expect two Cancer Sizes in " + MULTIPLE_MENTION_SENTENCE_5, 2, count );
   }
}


}
