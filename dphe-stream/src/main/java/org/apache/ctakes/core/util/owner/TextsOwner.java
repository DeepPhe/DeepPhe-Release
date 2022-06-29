package org.apache.ctakes.core.util.owner;

import org.apache.ctakes.core.util.Pair;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public interface TextsOwner extends SpanFinder {

   String[] getTexts();

   default Pair<Integer> findSpanInText( final String text, final int startIndex ) {
      final String lowerText = text.toLowerCase();
      for ( String ownerText : getTexts() ) {
         int index = lowerText.indexOf( ownerText, startIndex );
         if ( isValidSpan( text, index, index+ownerText.length() ) ) {
            return new Pair<>( index, index + ownerText.length() );
         }
      }
      return NO_SPAN;
   }


}
