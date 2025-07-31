package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final class MassPenalty {
   static private final double MASS_BUMP = -40;
   static private final double AFFIRM_BUMP = -20;
   static private final double MASS_PENALTY = 20;
   static private final double NODE_PENALTY = 25;

   // This is probably important for location and laterality confidence.
   // TODO - Don't change confidence for source cancer ?
   static private final Collection<String> MASS_WORDS = new HashSet<>( Arrays.asList(
         " mass ",
         " mass.",
         " mass,",
         "masses",
         "metastas",
         "mets",
         "infiltrate",
         "infiltration",
         "implant",
         "cyst ",
         "polyp",
         "adnexal",
         "nodule",
         "node",
         "nodal",
         " ln",
         "sln",
         "lesion",
         "ascites",
         "density",
         "body",
         "bodies",
         "tiss",
         "cm ",
         "cm,",
         "cm.",
         "mm ",
         "mm,",
         "mm.",
         "largest",
         "not involved",
         "benign",
         "unremarkable",
         "margin"
   ));

   static private final Collection<String> MASS_BUMPS = Arrays.asList(
         "tumor site", "primary tumor", "histologic type" );

   private MassPenalty() {}


   static double getPenalty( final String precedingText, final String sentenceText ) {
      if ( MASS_BUMPS.stream().anyMatch( precedingText::contains ) ) {
         // boost
         return MASS_BUMP;
      }
      if ( sentenceText.contains( "node" ) || sentenceText.contains( "nodul" ) ) {
         return NODE_PENALTY;
      }
      if ( isMassSentence( precedingText, sentenceText ) ) {
         return MASS_PENALTY;
      }
      return 0;
   }

   static private boolean isMassSentence( final String precedingText, final String sentenceText ) {
      return MASS_WORDS.stream().anyMatch( sentenceText::contains );
   }


}
