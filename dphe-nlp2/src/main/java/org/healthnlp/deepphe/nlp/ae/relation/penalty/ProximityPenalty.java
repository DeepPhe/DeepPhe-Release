package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final class ProximityPenalty {
   static private final double NEARBY_PENALTY = 30;

   static private final Collection<String> UNWANTED_SITE_PRECEDENTS = new HashSet<>( Arrays.asList(
         "near ",
         "over ",
         "under ",
         "above ",
         "below ",
         "between ",
//         "in "
         // Often "metastasis to the"
         "from ",
         "to the ",
         "adjacent to ",
         "adj to ",
         "anterior to ",
         "superior to " ) );

   private ProximityPenalty() {}

   static double getPenalty( final String precedingText ) {
      if ( isIndirectSite( precedingText ) ) {
         return NEARBY_PENALTY;
      }
      return 0;
   }

   static private boolean isIndirectSite( final String precedingText ) {
      for ( String word : UNWANTED_SITE_PRECEDENTS ) {
         if ( precedingText.endsWith( word ) || precedingText.endsWith( word + "the " ) ) {
            return true;
         }
      }
      return false;
   }

}
