package org.apache.ctakes.cancer.phenotype.stage;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/26/2017
 */
public enum StageFinder {
   INSTANCE;

   static public StageFinder getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "StageFinder" );


   static private final String TYPE_REGEX = "Stage";
   static private final String SHORT_VALUE = "(?:[I]{1,3}|IV|IS|[0-4])[a-c]?";
   static private final String LONG_VALUE = "N/?A\\b|recurrent|unknown|unspecified|indeterminate|(not\\s+assessed)";
   static private final String NON_STAGE = "(?:in\\s+situ)|unknown|indeterminate|unspecified|recurrent\\s+Breast\\s+Carcinoma";

   // Order is very important
   static private final String FULL_REGEX = "\\b" + TYPE_REGEX + "\\s*"
         + "(?:(?:" + SHORT_VALUE + "\\b)"
         + "|(?:" + LONG_VALUE + "))";
   static private final Pattern FULL_PATTERN = Pattern.compile( FULL_REGEX, Pattern.CASE_INSENSITIVE );


   private final Object LOCK = new Object();

   StageFinder() {
   }

   public Collection<SignSymptomMention> findStages( final JCas jcas, final AnnotationFS lookupWindow ) {
      final String lookupWindowText = lookupWindow.getCoveredText();
      if ( lookupWindowText.length() < 5 ) {
         return Collections.emptyList();
      }
      final Collection<SignSymptomMention> stages = new ArrayList<>();
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
      return stages;
   }

}
