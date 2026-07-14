package org.healthnlp.deepphe.nlp.ae.relation.score;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.healthnlp.deepphe.nlp.ae.relation.penalty.AnnotationPenalties;

/**
 * @author SPF , chip-nlp
 * @since {1/7/2024}
 */
public class RelationScore2 {

   static private final double MIN_RELATION_CONFIDENCE = 1;

   private final IdentifiedAnnotation _targetAnnotation;
   private final double _uriRelationScore;
   private final double _distanceScore;
   private final AnnotationPenalties _sourcePenalties;
   private final AnnotationPenalties _targetPenalties;
   private double _totalScore = Double.MIN_VALUE;

   RelationScore2( final IdentifiedAnnotation targetAnnotation,
                  final double uriRelationScore,
                  final double distanceScore,
                  final AnnotationPenalties sourcePenalties,
                  final AnnotationPenalties targetPenalties ) {
      _targetAnnotation = targetAnnotation;
      _uriRelationScore = uriRelationScore;
      _distanceScore = distanceScore;
      _sourcePenalties = sourcePenalties;
      _targetPenalties = targetPenalties;
   }



}
