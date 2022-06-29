package org.healthnlp.deepphe.summary.engine;

import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @since {3/16/2022}
 */
public final class LongNumSuffixComparator {
   private static final Logger LOGGER = Logger.getLogger( "LongNumSuffixComparator" );
   public int compare(String text1, String text2) {
      final int len1 = text1.length();
      final int len2 = text2.length();
      int i1 = 0;
      int i2 = 0;
      while ( i1 < len1 && i2 < len2 ) {
         final char c1 = text1.charAt( i1 );
         final char c2 = text2.charAt( i2 );
         if ( Character.isDigit(c1) && Character.isDigit(c2) ) {
            final String numText1 = getLongText( text1, i1 );
            final String numText2 = getLongText( text2, i2 );
            int value = numText1.length() - numText2.length();
            if ( value != 0 ) {
               return value;
            }
            value = compareLongNumText( numText1, numText2 );
            if (value != 0) {
               return value;
            }
            i1 += numText1.length();
            i2 += numText2.length();
         } else {
            final int value = Character.compare( c1, c2 );
            if  ( value != 0 ) {
               return value;
            }
            ++i1;
            ++i2;
         }
      }

      if (len1 < len2) {
         return -1;
      } else {
         return len2 < len1 ? 1 : 0;
      }
   }

   private static String getLongText(String text, int index) {
      int i = index;
      final StringBuilder sb = new StringBuilder();
      for(int length = text.length(); i < length; ++i) {
         char c = text.charAt(i);
         if (!Character.isDigit(c)) {
            return sb.toString();
         }
         sb.append(c);
      }
      return sb.toString();
   }

   private static int compareLongNumText( final String numText1, final String numText2 ) {
      try {
         final long num1 = Long.parseUnsignedLong( numText1 );
         final long num2 = Long.parseUnsignedLong( numText2 );
         return Long.compare( num1, num2 );
      } catch ( NumberFormatException nfE ) {
         LOGGER.warn( nfE.getMessage(), nfE );
         return 0;
      }
   }

}
