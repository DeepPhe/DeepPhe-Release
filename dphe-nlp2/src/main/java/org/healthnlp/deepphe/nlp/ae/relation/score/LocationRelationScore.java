package org.healthnlp.deepphe.nlp.ae.relation.score;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.healthnlp.deepphe.nlp.ae.relation.penalty.AnnotationPenalties;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
public class LocationRelationScore extends RelationScore {
   static private final double MIN_LOCATION_CONFIDENCE = 0;

   LocationRelationScore( final IdentifiedAnnotation targetAnnotation,
                          final double uriRelationScore,
                          final double distanceScore,
                          final AnnotationPenalties sourcePenalties,
                          final AnnotationPenalties targetPenalties ) {
      super( targetAnnotation, uriRelationScore, distanceScore, sourcePenalties, targetPenalties );
   }

   protected double getMinimumScore() {
      return MIN_LOCATION_CONFIDENCE;
   }
}
