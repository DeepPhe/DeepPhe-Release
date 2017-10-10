package org.apache.ctakes.cancer.util;

/**
 * A Spanned Entity (mention or other) with offsets for the beginning and ending location within text
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/20/2015
 */
public interface SpannedEntity {

   /**
    * @return offset of the first character within text
    */
   int getStartOffset();

   /**
    * @return offset of the last character (+1) within text
    */
   int getEndOffset();

}
