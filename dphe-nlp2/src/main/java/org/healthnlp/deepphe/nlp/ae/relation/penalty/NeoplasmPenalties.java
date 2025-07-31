package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
public class NeoplasmPenalties extends AnnotationPenalties {

   public NeoplasmPenalties( final IdentifiedAnnotation annotation,
                              final int beginTokenNum,
                              final int endTokenNum,
                              final String precedingText,
                              final String sentenceText,
                              final int docLength ) {
      super( annotation, beginTokenNum, endTokenNum, precedingText, sentenceText, docLength );
   }

   protected double getSectionPenalty( final IdentifiedAnnotation annotation ) {
      return SectionPenalty.getNeoplasmSectionPenalty( annotation );
   }

}
