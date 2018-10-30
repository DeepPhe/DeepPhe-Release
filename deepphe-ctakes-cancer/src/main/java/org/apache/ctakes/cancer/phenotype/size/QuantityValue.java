package org.apache.ctakes.cancer.phenotype.size;

import org.apache.ctakes.cancer.phenotype.property.Value;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/29/2016
 */
public class QuantityValue implements Value {

   static private final Logger LOGGER = Logger.getLogger( "QuantityValue" );

   static QuantityValue UNKNOWN = new QuantityValue( "" ) {
      public String getTitle() {
         return "Unknown";
      }

      public String getUri() {
         return "";
      }

      public String getCui() {
         return "";
      }

      public Matcher getMatcher( final CharSequence lookupWindow ) {
         return null;
      }
   };

   static private final String DIMENSION_REGEX = "\\d+(\\.\\d+)?";

   static final String NUMERIC_VALUE_URI = "NumericModifier";
   static private final Pattern PATTERN = Pattern.compile( DIMENSION_REGEX, Pattern.CASE_INSENSITIVE );
   private final String _value;


   QuantityValue( final String value ) {
      _value = value;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTitle() {
      return _value;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getUri() {
      return "Property_or_Attribute";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Matcher getMatcher( final CharSequence lookupWindow ) {
      return PATTERN.matcher( lookupWindow );
   }

}
