package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
public class LocationPenalties extends AnnotationPenalties {

   private final double _procedurePenalty;
   private final double _massPenalty;
   private final double _nearbyPenalty;
   private final double _lengthPenalty;

   public LocationPenalties( final IdentifiedAnnotation annotation,
                             final int beginTokenNum,
                             final int endTokenNum,
                             final String precedingText,
                             final String sentenceText,
                             final int docLength ) {
      super( annotation, beginTokenNum, endTokenNum, precedingText, sentenceText, docLength );
      _procedurePenalty = TestPenalty.getPenalty( precedingText, sentenceText );
      _massPenalty = MassPenalty.getPenalty( precedingText, sentenceText );
      _nearbyPenalty = ProximityPenalty.getPenalty( precedingText );
      _lengthPenalty = LengthPenalty.getPenalty( annotation );
   }

   protected double getSectionPenalty( final IdentifiedAnnotation annotation ) {
      return SectionPenalty.getLocationSectionPenalty( annotation );
   }

   public double getTotalPenalty() {
      return super.getTotalPenalty() + _procedurePenalty + _massPenalty + _nearbyPenalty + _lengthPenalty;
   }

   public String toString() {
      return super.toString() + " Proc " + _procedurePenalty + " Mass "
            + _massPenalty + " Near " + _nearbyPenalty
            + " Length " + _lengthPenalty;
   }

}
