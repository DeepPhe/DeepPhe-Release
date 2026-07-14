package org.healthnlp.deepphe.nlp.ae.relation.score;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.healthnlp.deepphe.nlp.ae.relation.penalty.AnnotationPenalties;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
public class RelationScore {
   static private final double MIN_RELATION_CONFIDENCE = 1;

   private final IdentifiedAnnotation _targetAnnotation;
   private final double _uriRelationScore;
   private final double _distanceScore;
   private final AnnotationPenalties _sourcePenalties;
   private final AnnotationPenalties _targetPenalties;
   private double _totalScore = Double.MIN_VALUE;

   RelationScore( final IdentifiedAnnotation targetAnnotation,
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

   public IdentifiedAnnotation getTargetAnnotation() {
      return _targetAnnotation;
   }

   protected double getMinimumScore() {
      return MIN_RELATION_CONFIDENCE;
   }

   /**
    * @return between 1 and 100.  100 is max because the uri relation score and distance score have a max of 100.
    */
   public double getTotalScore() {
      if ( _totalScore != Double.MIN_VALUE ) {
         return _totalScore;
      }
      _totalScore = Math.max( getMinimumScore(), (_uriRelationScore + _distanceScore) / 2
            - _sourcePenalties.getTotalPenalty()
            - _targetPenalties.getTotalPenalty() );
//         LogFileWriter.add( "RelationScore.getTotalScore: (rType+placement)"
//                                          + "/2- ( " +
//                                          _targetAnnotation.getCoveredText() + "(" +
//                                          _uriRelationScore + "+" + _distanceScore + ")/2 - "
//                                          + _sourcePenalties  + " " + _targetPenalties
//                                          + " ) = " + _totalScore );
      return _totalScore;
   }


   static public RelationScore createRelationScore( final boolean isLocation,
                                                     final IdentifiedAnnotation targetAnnotation,
                                                     final double uriRelationScore,
                                                     final double placementScore,
                                                     final AnnotationPenalties sourcePenalties,
                                                     final AnnotationPenalties targetPenalties ) {
      return isLocation ? new LocationRelationScore( targetAnnotation,
            uriRelationScore,
            placementScore,
            sourcePenalties,
            targetPenalties )
                        : new RelationScore( targetAnnotation,
                              uriRelationScore,
                              placementScore,
                              sourcePenalties,
                              targetPenalties );
   }

}
