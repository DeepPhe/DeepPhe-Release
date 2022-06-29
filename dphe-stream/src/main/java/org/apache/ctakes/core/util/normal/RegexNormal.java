package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.owner.PatternOwner;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public interface RegexNormal extends Normal, PatternOwner {

   Pattern getPattern();

   default Pair<Integer> getSpan( final String text, final int startIndex ) {
      final Pattern pattern = getPattern();
      final String searchText = text.substring( startIndex );
      try ( RegexSpanFinder finder = new RegexSpanFinder( pattern ) ) {
         return finder.findSpans( searchText )
                      .stream()
                      .filter( s -> isWholeWord( text, s ) )
                      .findAny()
                      .orElse( NormalizeUtil.SPAN_NOT_FOUND );
      } catch ( IllegalArgumentException iaE ) {
//         LOGGER.warn( iaE.getMessage() );
      }
      return NormalizeUtil.SPAN_NOT_FOUND;
   }

   default List<Pair<Integer>> getSpans( final String text ) {
      final Pattern pattern = getPattern();
      try ( RegexSpanFinder finder = new RegexSpanFinder( pattern ) ) {
         return finder.findSpans( text )
                      .stream()
                      .filter( s -> isWholeWord( text, s ) )
                      .collect( Collectors.toList() );
      } catch ( IllegalArgumentException iaE ) {
//         LOGGER.warn( iaE.getMessage() );
      }
      return Collections.emptyList();
   }


}
