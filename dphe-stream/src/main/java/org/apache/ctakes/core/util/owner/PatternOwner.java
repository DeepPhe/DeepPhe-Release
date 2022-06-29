package org.apache.ctakes.core.util.owner;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public interface PatternOwner extends SpanFinder {

   Pattern getPattern();

   default Pair<Integer> findSpanInText( final String text, final int startIndex ) {
      final Pattern pattern = getPattern();
      final String searchText = text.substring( startIndex );
      try ( RegexSpanFinder finder = new RegexSpanFinder( pattern ) ) {
         return finder.findSpans( searchText )
                      .stream()
                      .filter( s -> isValidSpan( text, s ) )
                      .findAny()
                      .orElse( NO_SPAN );
      } catch ( IllegalArgumentException iaE ) {
//         LOGGER.warn( iaE.getMessage() );
      }
      return NO_SPAN;
   }

   default List<Pair<Integer>> findSpansInText( final String text ) {
      final Pattern pattern = getPattern();
      try ( RegexSpanFinder finder = new RegexSpanFinder( pattern ) ) {
         return finder.findSpans( text )
                      .stream()
                      .filter( s -> isValidSpan( text, s ) )
                      .collect( Collectors.toList() );
      } catch ( IllegalArgumentException iaE ) {
//         LOGGER.warn( iaE.getMessage() );
      }
      return Collections.emptyList();
   }

   default List<Pair<Integer>> findSpansInText( final String text, final int startIndex ) {
      final Pattern pattern = getPattern();
      final String searchText = text.substring( startIndex );
      try ( RegexSpanFinder finder = new RegexSpanFinder( pattern ) ) {
         return finder.findSpans( searchText )
                      .stream()
                      .filter( s -> isValidSpan( text, s ) )
                      .collect( Collectors.toList() );
      } catch ( IllegalArgumentException iaE ) {
//         LOGGER.warn( iaE.getMessage() );
      }
      return Collections.emptyList();
   }

}
