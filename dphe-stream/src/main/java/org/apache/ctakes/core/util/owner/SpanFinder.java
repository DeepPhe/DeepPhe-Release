package org.apache.ctakes.core.util.owner;

import org.apache.ctakes.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {3/15/2022}
 */
public interface SpanFinder {

   Pair<Integer> NO_SPAN = new Pair<>( -1, -1 );

   default Pair<Integer> findSpanInText( final String text ) {
      return findSpanInText( text, 0 );
   }

   Pair<Integer> findSpanInText( String text, int startIndex );

   default List<Pair<Integer>> findSpansInText( final String text ) {
      return findSpansInText( text, 0 );
   }

   default List<Pair<Integer>> findSpansInText( final String text, final int startIndex ) {
      final List<Pair<Integer>> spans = new ArrayList<>();
      Pair<Integer> span = findSpanInText( text, startIndex );
      if ( span.getValue1() < 0 ) {
         return Collections.emptyList();
      }
      int previousEnd = 0;
      while ( span.getValue1() >= 0 ) {
         spans.add( span );
         previousEnd = span.getValue2();
         span = findSpanInText( text, previousEnd );
      }
      return spans;
   }

   default boolean isPrecedingNumberAllowed() {
      return false;
   }

   default boolean isFollowingNumberAllowed() {
      return false;
   }

   default boolean isPrecedingSymbolAllowed() {
      return false;
   }

   default boolean isFollowingSymbolAllowed() {
      return false;
   }

   default boolean areBoundariesValid( final String text, final Pair<Integer> span ) {
      return areBoundariesValid( text, span.getValue1(), span.getValue2() );
   }

   default boolean areBoundariesValid( final String text, final int begin, final int end ) {
      if ( begin < 0 || begin >= end || end > text.length() ) {
         return false;
      }
      if ( begin > 0 ) {
         final char c = text.charAt( begin-1 );
         if ( Character.isLetter( c ) ) {
            return false;
         }
         if ( !( Character.isWhitespace( c )
                  || (Character.isDigit( c ) && isPrecedingNumberAllowed() )
                  || (!Character.isLetterOrDigit( c ) && isPrecedingSymbolAllowed()) ) ) {
            return false;
         }
      }
      if ( end == text.length() ) {
         return true;
      }
      final char c = text.charAt( end );
      if ( Character.isLetter( c ) ) {
         return false;
      }
      return ( Character.isWhitespace( c )
               || (Character.isDigit( c ) && isFollowingNumberAllowed() )
               || (!Character.isLetterOrDigit( c ) && isFollowingSymbolAllowed() ) );
   }

   default boolean isValidSpan( final String text, final Pair<Integer> span ) {
      return areBoundariesValid( text, span );
   }

   default boolean isValidSpan( final String text, final int begin, final int end ) {
      return areBoundariesValid( text, begin, end );
   }

   /**
    *
    * @param text -
    * @param span -
    * @return true if the span represents one word, rather than part of one or more words.
    */
   default boolean isWholeWord( final String text, final Pair<Integer> span ) {
      return isWholeWord( text, span.getValue1(), span.getValue2() );
   }

   /**
    *
    * @param text -
    * @param begin -
    * @param end -
    * @return true if the span represents one word, rather than part of one or more words.
    */
   default boolean isWholeWord( final String text, final int begin, final int end ) {
      if ( begin > 0 ) {
         if ( Character.isLetterOrDigit( text.charAt( begin-1 ) ) ) {
            return false;
         }
      }
      if ( end == text.length() ) {
         return true;
      }
      return !Character.isLetterOrDigit( text.charAt( end ) );
   }

}
