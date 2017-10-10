package org.apache.ctakes.dictionary.lookup2.util;

import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/5/2016
 */
final public class ValidTextUtil {

   private ValidTextUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "ValidTextUtil" );


   /**
    * @param line -
    * @return true if line starts with "//" or "#"
    */
   static public boolean isCommentLine( final String line ) {
      return line.startsWith( "//" ) || line.startsWith( "#" );
   }

   /**
    * 496k Total Terms -> 386k Terms w/o 8+ words -> 305k w/o redundants -> 301k w/o body side, quadrant
    *
    * @param text term text
    * @return false if the text is not desirable for dictionary lookup
    */
   static public boolean isValidText( final String text ) {
      return text.split( " " ).length < 8 && !isRedundant( text ) && !isPatientCode( text ) && !text.equals( "for" );
   }

   /**
    * Gets rid of ~65,000 unwanted body locations terms, ~35,000 medication terms, ~41,000 procedure terms,
    *
    * @param text term text
    * @return false if the text contains self-descriptors
    */
   static private boolean isRedundant( final String text ) {
      return text.startsWith( "entire " ) || text.startsWith( "structure of " ) || text.endsWith( ")" ) || text.endsWith( "]" );
   }

   /**
    * Gets rid of 4 breast quadrants
    *
    * @param text -
    * @return true if the text contains ^Patient
    */
   static private boolean isPatientCode( final String text ) {
      return text.contains( "^patient" );
   }

}
