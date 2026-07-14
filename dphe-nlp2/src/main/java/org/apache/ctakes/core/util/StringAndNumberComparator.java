package org.apache.ctakes.core.util;

import org.apache.log4j.Logger;

import java.util.Comparator;

/**
 * TODO add to ctakes
 * @author SPF , chip-nlp
 * @since {9/20/2023}
 */
final public class StringAndNumberComparator implements Comparator<String> {
   static private final Logger LOGGER = Logger.getLogger( "StringAndNumberComparator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public int compare( final String text1, final String text2 ) {
      final int numSuffixIndex1 = getNumSuffixIndex( text1 );
      final int numSuffixIndex2 = getNumSuffixIndex( text2 );
      final String prefix1 = text1.substring( 0, numSuffixIndex1+1 );
      final String prefix2 = text2.substring( 0, numSuffixIndex2+1 );
      if ( !prefix1.equals( prefix2 ) ) {
         return String.CASE_INSENSITIVE_ORDER.compare( text1, text2 );
      }
      final String suffix1 = text1.substring( numSuffixIndex1 );
      final String suffix2 = text2.substring( numSuffixIndex2 );
      return compareNumText( suffix1, suffix2 );
   }
   
   static private int getNumSuffixIndex( final String text ) {
      for ( int i=text.length(); i > 0; i-- ) {
         if ( !Character.isDigit( text.charAt( i-1 ) ) ) {
            return i;
         }
      }
      return 0;
   }

   /**
    * @param numText1 filled with digits
    * @param numText2 filled with digits
    * @return -1, 0, 1 if first number less than, equal to, greater than second number
    */
   static private int compareNumText( final String numText1, final String numText2 ) {
      try {
         final long num1 = Long.parseUnsignedLong( numText1 );
         final long num2 = Long.parseUnsignedLong( numText2 );
         return Long.compare( num1, num2 );
      } catch ( NumberFormatException nfE ) {
         LOGGER.debug( nfE.getMessage(), nfE );
      }
      return 0;
   }


}
