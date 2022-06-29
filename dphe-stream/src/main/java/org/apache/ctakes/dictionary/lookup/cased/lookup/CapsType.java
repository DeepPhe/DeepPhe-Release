package org.apache.ctakes.dictionary.lookup.cased.lookup;

import java.util.Arrays;

/**
 * @author SPF , chip-nlp
 * @since {2/1/2022}
 */
public enum CapsType {
   CAPITALIZED( 10 ),
   UPPER( 8 ),
   MIXED( 6 ),
   LOWER( 4 ),
   NO_CHARACTER( 0 );

   private final short _number;
   CapsType( final int number ) {
      _number = (short)number;
   }

   public short getNormal() {
      return _number;
   }

   static public CapsType getCapsType( final String[] tokens ) {
      boolean firstCapital = false;
      boolean upper = false;
      boolean lower = false;
      for ( int i=0; i<tokens.length; i++ ) {
         final CapsType capsType = getCapsType( tokens[i] );
         if ( capsType == CAPITALIZED ) {
            if ( i == 0 ) {
               firstCapital = true;
               continue;
            }
            return MIXED;
         } else if ( capsType == MIXED ) {
            return MIXED;
         } else if ( capsType == UPPER ) {
            if ( lower || firstCapital ) {
               return MIXED;
            }
            upper = true;
         } else if ( capsType == LOWER ) {
            if ( upper ) {
               return MIXED;
            }
            lower = true;
         }
      }
      if ( firstCapital ) {
         return CAPITALIZED;
      } else if ( upper ) {
         return UPPER;
      } else if ( lower ) {
         return LOWER;
      }
      return NO_CHARACTER;
   }

   static public CapsType getCapsType( final String text ) {
      boolean firstCapital = false;
      boolean upper = false;
      boolean lower = false;
      final char[] chars = text.toCharArray();
      for ( int i=0; i<chars.length; i++ ) {
         if ( Character.isLowerCase( chars[i] ) ) {
            lower = true;
            if ( upper ) {
               return MIXED;
            }
         } else if ( Character.isUpperCase( chars[i] ) ) {
            if ( i==0 ) {
               firstCapital = true;
               continue;
            }
            upper = true;
            if ( lower ) {
               return MIXED;
            }
         } else if ( firstCapital && Character.isDigit( chars[i] ) ) {
            firstCapital = false;
            upper = true;
            if ( lower ) {
               return MIXED;
            }
         }
      }
      if ( upper ) {
         return UPPER;
      } else if ( lower ) {
         if ( firstCapital ) {
            return CAPITALIZED;
         }
         return LOWER;
      }
      return NO_CHARACTER;
   }

   static public CapsType getCapsType( final short normal ) {
      return Arrays.stream( values() )
                   .filter( c -> c.getNormal() == normal )
                   .findAny()
                   .orElse( NO_CHARACTER );
   }


}
