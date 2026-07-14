package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final public class PlacementPenalty {
   static private final double MIN_PLACEMENT_SCORE = 1;
   static private final double SENTENCE_OFFSET = 0;
   static private final double LIST_ENTRY_OFFSET = 1;
   static private final double PARAGRAPH_OFFSET = 5;
   static private final double SECTION_OFFSET = 10;
   // 4/24/2023  DOCUMENT_OFFSET from 15 to 20 since the sentence splitter can be overly enthusiastic.
   static private final double DOCUMENT_OFFSET = 20;
   static private final double SENTENCE_MULTIPLIER = 0.1;
   static private final double LIST_ENTRY_MULTIPLIER = 0.1;
   static private final double PARAGRAPH_MULTIPLIER = 0.2;
   static private final double SECTION_MULTIPLIER = 0.3;
   static private final double DOCUMENT_MULTIPLIER = .5;
   static private final double PRECEDE_MULTIPLIER = 0.1;
   static private final double FOLLOW_MULTIPLIER = 0.2;

   private PlacementPenalty() {}

   static public Pair<Double> getPenalty( final IdentifiedAnnotation source,
                                                    final IdentifiedAnnotation target,
                                                    final Collection<Pair<Integer>> paragraphBounds,
                                                    final Collection<Pair<Integer>> listEntryBounds ) {
      if ( !source.getSegmentID().equals( target.getSegmentID() ) ) {
         // Not in the same section, so nothing else can be in common.
         return new Pair<>( DOCUMENT_OFFSET, DOCUMENT_MULTIPLIER );
      }
      if ( source.getSentenceID().equals( target.getSentenceID() ) ) {
         return new Pair<>( SENTENCE_OFFSET, SENTENCE_MULTIPLIER );
      }
      if ( inSameListEntry( source, target, listEntryBounds ) ) {
         return new Pair<>( LIST_ENTRY_OFFSET, LIST_ENTRY_MULTIPLIER );
      }
      if ( inSameParagraph( source, target, paragraphBounds ) ) {
         return new Pair<>( PARAGRAPH_OFFSET, PARAGRAPH_MULTIPLIER );
      }
      // In the same Section, but that is the best of it.
      return new Pair<>( SECTION_OFFSET, SECTION_MULTIPLIER );
   }

   static private boolean inSameParagraph( final IdentifiedAnnotation source,
                                           final IdentifiedAnnotation target,
                                           final Collection<Pair<Integer>> paragraphBounds ) {
      for ( Pair<Integer> paragraph : paragraphBounds ) {
         final boolean foundSource = paragraph.getValue1() <= source.getBegin()
               && source.getEnd() <= paragraph.getValue2();
         final boolean foundTarget = paragraph.getValue1() <= target.getBegin()
               && target.getEnd() <= paragraph.getValue2();
         if ( foundSource != foundTarget ) {
            return false;
         } else if ( foundSource ) {
            return true;
         }
      }
      return false;
   }

   static private boolean inSameListEntry( final IdentifiedAnnotation source,
                                           final IdentifiedAnnotation target,
                                           final Collection<Pair<Integer>> listEntryBounds ) {
      int sourceIndex = -1;
      int targetIndex = -2;
      int index = 1;
      for ( Pair<Integer> listEntry : listEntryBounds ) {
         if ( listEntry.getValue1() <= source.getBegin()
               && source.getEnd() <= listEntry.getValue2() ) {
            sourceIndex = index;
         }
         if ( listEntry.getValue1() <= target.getBegin()
               && target.getEnd() <= listEntry.getValue2() ) {
            targetIndex = index;
         }
         if ( sourceIndex > 0 && targetIndex > 0 ) {
            break;
         }
         index++;
      }
      return sourceIndex == targetIndex;
   }

   /**
    *
    * @param source -
    * @param target -
    * @param placementPenalty -
    * @return score between 3 and 100.  Distances and penalties > 155 are 3.
    */
   static public double getPlacementScore(  final Pair<Integer> source, final Pair<Integer> target,
                                             final Pair<Double> placementPenalty) {
      final double tokenDistance = getDistance( source, target);
      if ( tokenDistance == 0 ) {
         return 100;
      }
//      LogFileWriter.add( "PlacementPenalty.getPlacementScore: 100 - ( v1 + v2 * mentionDistance ) or 1 ) == 100 - ("
//                                       + placementPenalty.getValue1() + " + " + placementPenalty.getValue2()
//                                       + " * " + tokenDistance + ") = " + MIN_PLACEMENT_SCORE + " or "
//                                       + (100 - (placementPenalty.getValue1()
//                                                 + placementPenalty.getValue2() * tokenDistance)) );
      return Math.max( MIN_PLACEMENT_SCORE, 100 - (placementPenalty.getValue1() + placementPenalty.getValue2() * tokenDistance) );
   }


   static private int getDistance( final int begin1, final int end1, final int begin2, final int end2 ) {
      if ( end1 < begin2 ) {
         return begin2 - end1;
      } else if ( end2 < begin1 ) {
         return begin1 - end2;
      }
      return 0;
   }

   static private int getDistance( final Pair<Integer> source, final Pair<Integer> target ) {
      return getDistance( source.getValue1(), source.getValue2(), target.getValue1(), target.getValue2() );
   }

}
