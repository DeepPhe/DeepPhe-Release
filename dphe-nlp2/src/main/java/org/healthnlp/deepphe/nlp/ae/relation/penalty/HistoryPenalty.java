package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final class HistoryPenalty {
   static private final double FAMILY_HISTORY_PENALTY = 50;
   static private final double PERSONAL_HISTORY_PENALTY = 5;

   static private final Collection<String> FAMILY_HISTORY_PRECEDENTS = new HashSet<>( Arrays.asList(
         "family history",
         "family hist",
         "family hx",
         "fam hx",
         "famhx",
         "fmhx",
         "fmh",
         "fh" ) );

   static private final Collection<String> PERSONAL_HISTORY_PRECEDENTS = new HashSet<>( Arrays.asList(
         "history of",
         "hist of",
         "hx" ) );

   private HistoryPenalty() {}


   static double getPenalty( final String precedingText, final IdentifiedAnnotation target ) {
      if ( isFamilyHistory( precedingText, target ) ) {
         return FAMILY_HISTORY_PENALTY;
      } else if ( isPersonalHistory( precedingText ) ) {
         return PERSONAL_HISTORY_PENALTY;
      }
      return 0;
   }

   static private boolean isFamilyHistory( final String precedingText, final IdentifiedAnnotation annotation ) {
      if ( !annotation.getSubject().equals( CONST.ATTR_SUBJECT_PATIENT ) ) {
         return true;
      }
      return FAMILY_HISTORY_PRECEDENTS.stream().anyMatch( precedingText::contains );
   }

   static private boolean isPersonalHistory( final String precedingText ) {
      return PERSONAL_HISTORY_PRECEDENTS.stream().anyMatch( precedingText::contains );
   }


}
