package org.apache.ctakes.core.util.owner;

import org.apache.ctakes.core.util.Pair;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public interface SpanOwner {
   Pair<Integer> NO_SPAN = new Pair<>( -1, -1 );

   Pair<Integer> getSpan();

   int getScore();

}
