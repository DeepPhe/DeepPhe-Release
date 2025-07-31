package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final class LengthPenalty {
   static private final double SMALL_LENGTH_PENALTY = 20;

   private LengthPenalty() {}

   static double getPenalty( final IdentifiedAnnotation annotation ) {
      if ( annotation.getCoveredText().length() <= 3 ) {
         return SMALL_LENGTH_PENALTY;
      }
      return 0;
   }


}
