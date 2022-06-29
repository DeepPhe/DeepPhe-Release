package org.apache.ctakes.core.util.owner;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public interface ScoreOwner {
   int getScore();

   default boolean isScoreWanted() {
      return true;
   }

}
