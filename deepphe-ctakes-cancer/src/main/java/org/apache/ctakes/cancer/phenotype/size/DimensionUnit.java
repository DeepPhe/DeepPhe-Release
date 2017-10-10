package org.apache.ctakes.cancer.phenotype.size;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/20/2015
 */
enum DimensionUnit {
   CENTIMETER( "centimeter", "cm" ),
   MILLIMETER( "millimeter", "mm" );

//   http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl#Unit
//   http://ontologies.dbmi.pitt.edu/deepphe/nlpBreastCancer.owl#Centimeter
//   http://ontologies.dbmi.pitt.edu/deepphe/nlpBreastCancer.owl#Unit_of_Length


   static private final String VALUES_REGEX = "\\d+(\\.\\d+)?( ?(x|\\*) ?\\d+(\\.\\d+)?){0,2} ?";
   static private final String END_REGEX = "(\\b|,|\\.|\\?|!|\\)|\\]|\\})";

   private final String _title;
   final private Pattern _pattern;

   DimensionUnit( final String title, final String regex ) {
      _title = title;
      _pattern = Pattern.compile( VALUES_REGEX + regex + END_REGEX, Pattern.CASE_INSENSITIVE );
   }

   String getTitle() {
      return _title;
   }

   Matcher getMatcher( final CharSequence lookupWindow ) {
      return _pattern.matcher( lookupWindow );
   }

}
