package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.owner.TextsOwner;


/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public interface TextListNormal extends Normal, TextsOwner {

   String[] getTexts();

   default Pair<Integer> getSpan( final String text, final int startIndex ) {
      final String lowerText = text.toLowerCase();
      for ( String normal : getTexts() ) {
         int index = lowerText.indexOf( normal, startIndex );
         if ( index < 0 ) {
            continue;
         }
         if ( index == 0 ) {
            if ( lowerText.length() == normal.length()
                 || !Character.isLetter( lowerText.charAt( normal.length() ) ) ) {
               return new Pair<>( 0, normal.length() );
            }
         } else if ( !Character.isLetter( lowerText.charAt( index - 1 ) ) ) {
            if ( text.length() == index + normal.length()
                 || !Character.isLetter( lowerText.charAt( index + normal.length() ) ) ) {
               return new Pair<>( index, index + normal.length() );
            }
         }
      }
      return NormalizeUtil.SPAN_NOT_FOUND;
   }


}
