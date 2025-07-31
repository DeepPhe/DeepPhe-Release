package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final class TestPenalty {
   static private final double TEST_PENALTY = 40;

   static private final Collection<String> TEST_PRECEDENTS = new HashSet<>( Arrays.asList(
         "specimen",
         "tissue",
         "biopsy",
         "biopsies",
         "bx",
         "fna",
         " ct",
         " cxr",
         "ray",
         " mri",
         " pet ",
         " pet:",
         "radio",
         "imaging",
         "ultrasound",
         "scan",
         " pe ",
         " pe:",
         "radiation",
         "view",
         "procedure",
         "frozen",
         "slide",
         "block",
         "cytology",
         "stain",
         "wash",
         "fluid",
         "exam",
         "screen",
         "mast",
         "ectomy",
         "pe:",
         "resect",
         "complaint",
         "excis",
         "incis",
         "source",
         "submit",
         "reveal",
         "seen",
         "presents",
         "evaluat",
         "perform",
         "thin" ));

   private TestPenalty() {}

   static double getPenalty( final String precedingText, final String sentenceText ) {
//      if ( isTestSentence( precedingText ) ) {
      if ( isTestSentence( sentenceText ) ) {
         return TEST_PENALTY;
      }
      return 0;
   }

   // TODO When tests/procedures are added to the ontology use them here.
   static private boolean isTestSentence( final String precedingText ) {
      return TEST_PRECEDENTS.stream().anyMatch( precedingText::contains );
   }


}
