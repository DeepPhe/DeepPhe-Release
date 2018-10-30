package org.apache.ctakes.cancer.phenotype.size;

import org.apache.ctakes.cancer.phenotype.property.Type;
import org.apache.ctakes.cancer.phenotype.property.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/29/2016
 */
enum QuantityUnit implements Type {
      CENTIMETER( "centimeter", "Centimeter", "cm" ),
   MILLIMETER( "millimeter", "Millimeter", "mm" );


   final private String _title;
   final private String _uri;
   final private Pattern _pattern;

   QuantityUnit( final String title, final String uri, final String regex ) {
      _title = title;
      _uri = uri;
      _pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTitle() {
      return _title;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getUri() {
      return _uri;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getCui( final Value stageValue ) {
      return stageValue.getCui();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Matcher getMatcher( final CharSequence lookupWindow ) {
      return _pattern.matcher( lookupWindow );
   }
}
