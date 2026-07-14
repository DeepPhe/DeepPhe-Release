package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final class AssertionPenalty {
   static private final double NEGATED_PENALTY = 30;
   static private final double UNCERTAIN_PENALTY = 5;

   private AssertionPenalty() {}

   static double getPenalty( final IdentifiedAnnotation target ) {
      double penalty = 0;
      if ( IdentifiedAnnotationUtil.isNegated( target ) ) {
         penalty += NEGATED_PENALTY;
      }
      if ( IdentifiedAnnotationUtil.isUncertain( target ) ) {
         penalty += UNCERTAIN_PENALTY;
      }
      return penalty;
   }


}
