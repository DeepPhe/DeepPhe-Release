package org.healthnlp.deepphe.nlp.confidence;

/**
 * @author SPF , chip-nlp
 * @since {3/19/2023}
 */
public interface ConfidenceOwner {

   default double getConfidence() {
      return 0;
   }

}
