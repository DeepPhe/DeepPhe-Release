package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;

import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
public class AnnotationPenalties {

   private final Pair<Integer> _beginEndTokens;
   private final double _assertionPenalty;
   private final double _historyPenalty;
   private final double _sectionPenalty;

   public AnnotationPenalties( final IdentifiedAnnotation annotation,
                               final int beginTokenNum,
                               final int endTokenNum,
                               final String precedingText,
                        final String sentenceText,
                        final int docLength ) {
      _beginEndTokens = new Pair<>( beginTokenNum, endTokenNum );
      _assertionPenalty = AssertionPenalty.getPenalty( annotation );
      _historyPenalty = HistoryPenalty.getPenalty( precedingText, annotation );
      _sectionPenalty = getSectionPenalty( annotation );
   }


   public Pair<Integer> getBeginEndTokens() {
      return _beginEndTokens;
   }

   protected double getSectionPenalty( final IdentifiedAnnotation annotation ) {
      return SectionPenalty.getPenalty( annotation );
   }

   public double getTotalPenalty() {
      return _assertionPenalty + _historyPenalty + _sectionPenalty;
   }


   public String toString() {
      return " Assert " + _assertionPenalty + " Hist " + _historyPenalty + " Sect " + _sectionPenalty;
   }


   static public AnnotationPenalties createAnnotationPenalties( final boolean isLocation,
                                                                 final IdentifiedAnnotation annotation,
                                                                 final int beginTokenNum,
                                                                 final int endTokenNum,
                                                                 final String precedingText,
                                                                 final String sentenceText,
                                                                 final int docLength) {
      return isLocation ? new LocationPenalties( annotation,
            beginTokenNum,
            endTokenNum,
            precedingText,
            sentenceText,
            docLength )
                        : new AnnotationPenalties( annotation,
                              beginTokenNum,
                              endTokenNum,
                              precedingText,
                              sentenceText,
                              docLength );
   }




}
