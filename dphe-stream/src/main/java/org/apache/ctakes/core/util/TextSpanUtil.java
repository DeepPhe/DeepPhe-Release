package org.apache.ctakes.core.util;

import org.apache.log4j.Logger;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {11/19/2021}
 */
final public class TextSpanUtil {

   static private final Logger LOGGER = Logger.getLogger( "TextSpanUtil" );

   static private final Comparator<Annotation> ANNOTATION_BY_SHORT_SORTER = new AnnotationByShortSorter();

   static private final Comparator<Annotation> ANNOTATION_BEGIN_LONG_SORTER = new AnnotationByBegin_LongSorter();
   static public final Comparator<Pair<Integer>> SPAN_BEGIN_LONG_SORTER = new SpanByBegin_LongSorter();

   static private final Pair<Integer> BAD_SPAN = new Pair<>( -1, -1 );

   private TextSpanUtil() {}


   static public Pair<Integer> getOverlap( final Pair<Integer> span1, final Pair<Integer> span2 ) {
      Pair<Integer> overlap = new Pair<>( Math.max( span1.getValue1(), span2.getValue1() ),
                                          Math.min( span1.getValue2(), span2.getValue2() ) );
      if ( isValidSpan( overlap ) ) {
         return overlap;
      }
      return BAD_SPAN;
   }

   static public boolean hasOverlap( final Pair<Integer> span1, final Pair<Integer> span2 ) {
      Pair<Integer> overlap = new Pair<>( Math.max( span1.getValue1(), span2.getValue1() ),
                                          Math.min( span1.getValue2(), span2.getValue2() ) );
      return isValidSpan( overlap );
   }

   static public Collection<Pair<Integer>> getAvailableSpans( final int windowOffset,
                                                               final int windowLength,
                                                               final Collection<? extends Annotation> unavailableCoverings ) {
      final int begin = 0;
      if ( unavailableCoverings.isEmpty() ) {
         return Collections.singletonList( new Pair<>( begin, windowLength ) );
      }
      final List<Pair<Integer>> unavailableSpans
            = unavailableCoverings.stream()
                       .filter( t -> t.getEnd() > windowOffset + begin && t.getBegin() < windowOffset + windowLength )
                       .sorted( Comparator.comparingInt( Annotation::getBegin )
                                          .thenComparing( Annotation::getEnd ) )
                       .map( t -> new Pair<>( t.getBegin()-windowOffset, t.getEnd()-windowOffset ) )
                       .collect( Collectors.toList() );
      return getAvailableSpans( windowLength, unavailableSpans );
   }


   static public Collection<Pair<Integer>> getAvailableSpans( final int windowLength,
                                                              final Collection<Pair<Integer>> unavailableSpans ) {
      if ( unavailableSpans.isEmpty() ) {
         LOGGER.info( "No Unavailable Spans.  Whole window is available.  0," + windowLength );
         return Collections.singletonList( new Pair<>( 0, windowLength ) );
      }
      final List<Pair<Integer>> usedSpans = new ArrayList<>( unavailableSpans );
      usedSpans.sort( Comparator.comparingInt( Pair::getValue1 ) );
      final Collection<Pair<Integer>> availableSpans = new ArrayList<>();
      int previousEnd = 0;
      for ( final Pair<Integer> span : usedSpans ) {
         LOGGER.info( "Unavailable span " + span.getValue1() + "," + span.getValue2()
                      + "  PreviousEnd " + previousEnd );
         if ( previousEnd < span.getValue1() ) {
            // previous end is more than one line previous to the new span's begin.  Add span between as available.
            availableSpans.add( new Pair<>( previousEnd, span.getValue1() ) );
            LOGGER.info( "Added available span " + previousEnd + "," + span.getValue1() );
         }
         previousEnd = span.getValue2();
      }
      if ( previousEnd < windowLength ) {
         // previous end is more than one line previous to the new span's begin.  Add span between as available.
         availableSpans.add( new Pair<>( previousEnd, windowLength ) );
         LOGGER.info( "End of loop, added available span " + previousEnd + "," + windowLength );
      }
      LOGGER.info( "Spans available: " + availableSpans.size() + " "
                   + availableSpans.stream()
                                   .map( p -> p.getValue1() +"," + p.getValue2() )
                                   .collect( Collectors.joining( "  " ) ) );
      return availableSpans;
   }


   static public Collection<Pair<Integer>> getAvailableSpans( final Annotation window,
                                                              final List<Pair<Integer>> unavailableSpans ) {
      if ( unavailableSpans.isEmpty() ) {
         return Collections.singletonList( new Pair<>( window.getBegin(), window.getEnd() ) );
      }
      final Collection<Pair<Integer>> windowUnavailables
            = unavailableSpans.stream()
                            .filter( t -> t.getValue1() >= window.getBegin() && t.getValue2() <= window.getEnd() )
                            .collect( Collectors.toList() );
      if ( windowUnavailables.isEmpty() ) {
         return Collections.singletonList( new Pair<>( window.getBegin(), window.getEnd() ) );
      }
      final Collection<Pair<Integer>> availableSpans = new ArrayList<>();
      int previousEnd = window.getBegin();
      for ( final Pair<Integer> span : windowUnavailables ) {
         if ( previousEnd < span.getValue1() ) {
            // previous end is more than one line previous to the new span's begin.  Add span between as available.
            availableSpans.add( new Pair<>( previousEnd, span.getValue1() ) );
         }
         previousEnd = span.getValue2();
      }
      if ( previousEnd < window.getEnd() ) {
         // previous end is more than one line previous to the new span's begin.  Add span between as available.
         availableSpans.add( new Pair<>( previousEnd, window.getEnd() ) );
      }
      return availableSpans;
   }



   static public boolean isAnnotationCovered( final Collection<Pair<Integer>> spans,
                                              final Annotation annotation ) {
//      Logger.getLogger( "TextSpanUtil" ).info( "isAnnotationCovered "
//                                               + annotation.getBegin() + "," + annotation.getEnd() + " "
//                                               + spans.stream()
//                                                      .map( s -> s.getValue1() + "," + s.getValue2() )
//                                                     .collect( Collectors.joining(" ; ")) );
      if ( spans.isEmpty() ) {
         return false;
      }
      final int begin = annotation.getBegin();
      final int end = annotation.getEnd();
      return spans.stream()
                  .anyMatch( s -> s.getValue1() <= begin && end <= s.getValue2() );
   }

   static public boolean isAnnotationCovered( final Pair<Integer> span, final Annotation annotation ) {
      return span.getValue1() <= annotation.getBegin()
             && annotation.getEnd() <= span.getValue2();
   }

   static public boolean isAnnotationCovered( final Annotation covering, final Annotation annotation ) {
      return covering.getBegin() <= annotation.getBegin()
             && annotation.getEnd() <= covering.getEnd();
   }

   /**
    * Use this instead of making overlapping calls to JCasUtil.indexCovered(..).
    * Because of the definition of covered (no overlap), it is best used for types like sentences within paragraphs,
    * paragraphs within sections, etc.
    * @param coverings Larger covering annotations
    * @param covereds Smaller coveredAnnotations
    * @param <C> -
    * @param <A> -
    * @return Map of covering over completely covered.  All covering will exist in the keyset, but if a covereds
    * is not covered at all or completely (partial overlap coverage is no good) then it will not be in the map.
    */
   static public <C extends Annotation,A extends Annotation> Map<C,Collection<A>> indexCovered(
         final Collection<C> coverings, final Collection<A> covereds ) {
      final Map<C,Collection<A>> map = new HashMap<>( coverings.size() );
      for ( C covering : coverings ) {
         final Collection<A> covered
               = covereds.stream()
                         .filter( a -> TextSpanUtil.isAnnotationCovered( covering, a ) )
                         .collect( Collectors.toList() );
         map.put( covering, covered );
      }
      return map;
   }

   /**
    * Use this instead of making overlapping calls to JCasUtil.indexCovered(..).
    * Because of the definition of covered (no overlap), it is best used for types like sentences within paragraphs,
    * paragraphs within sections, etc.
    * @param coverings Larger covering annotations
    * @param covereds Smaller coveredAnnotations
    * @param <C> -
    * @param <A> -
    * @return Map of covering over completely covered.  All covering will exist in the keyset, but if a covereds
    * is not covered at all or completely (partial overlap coverage is no good) then it will not be in the map.
    */
   static public <C extends Annotation,A extends Annotation> Map<C,Collection<A>> indexCoveredOnce(
         final Collection<C> coverings, final Collection<A> covereds ) {
      final Map<C,Collection<A>> map = new HashMap<>( coverings.size() );
      final Collection<A> used = new HashSet<>( covereds.size() );
      for ( C covering : coverings ) {
         final Collection<A> covered
               = covereds.stream()
                         .filter( a -> !used.contains( a ) )
                         .filter( a -> TextSpanUtil.isAnnotationCovered( covering, a ) )
                         .collect( Collectors.toList() );
         map.put( covering, covered );
         used.addAll( covered );
      }
      return map;
   }



   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   // There are a lot of ways to sort annotations.
   // Sometimes using .reversed() on a sort is tempting, but it doesn't always work.
   // The most commonly used method is to sort primarily by the begin offset and secondarily by the end offset.
   // This is also a sort by offset and length, primarily by offset and secondarily by length (short to long).
   //  |==1==|
   //  |===2===|
   //  |====3====|
   //    |==4==|
   //    |===5===|
   //      |=6=|
   //
   // Should you want to sort primarily by end and secondarily by begin, you cannot simply reverse the above sort.
   // This is also a sort by offset and length, primarily by end offset and secondarily by length (long to short).
   //  |====3====|
   //    |===5===|
   //  |===2===|
   //    |==4==|
   //      |=6=|
   //  |==1==|
   //
   // However, you may want to sort primarily by length, the longest first, and secondarily by the begin offset.
   // This cannot be achieved by simply reversing the offset sorts (above), and so requires a special sorter.
   //  |====3====|
   //  |===2===|
   //    |===5===|
   //  |==1==|
   //    |==4==|
   //      |=6=|
   //
   // You may want to sort primarily by length, the shortest first, and secondarily by the begin offset.
   // This cannot be achieved by simply reversing the length sort above, and so requires a special sorter.
   //      |=6=|
   //  |==1==|
   //    |==4==|
   //  |===2===|
   //    |===5===|
   //  |====3====|
   //
   // You get the point.  Naming the possible sorts becomes difficult.
   // By numbering the above we could just name them like boxing combinations, but most would not understand.
   // 1 = begin, 2 = end, 3 = short, 4 = long.   1,2 (1,3)   2,1   4,1   3,1   etc.
   // Instead, Begin_End (Begin_Short)   End_Begin   Long_Begin   Short_Begin   etc.
   //
   // Now imagine subsumption made simple:
   // Sort Begin_Long  - ignore span 3, and new span 7
   //  |===2===|
   //  |==1==|
   //    |===5===|
   //    |==4==|
   //      |=6=|
   //       |=7=|
   // Then iterate i,j removing shorter spans:
   // for i=begin, j=i+1
   //  if end_j <= end_i, remove j          [i=s2, j=s1], [i=s5, j=s4,s6,s7]
   //  else (end_j > end_i), i=j, continue  [i=s2, j=s5]
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////

   static public <A extends Annotation> List<A> getCovering( final Collection<A> coverCandidates,
                                                             final Annotation annotation ) {
      if ( coverCandidates.isEmpty() ) {
         return Collections.emptyList();
      }
      final int begin = annotation.getBegin();
      final int end = annotation.getEnd();
      return coverCandidates.stream()
                      .filter( c -> c.getBegin() <= begin && end <= c.getEnd() )
                      .collect( Collectors.toList() );
   }

   static public <A extends Annotation> List<A> getCovering( final Collection<A> coverCandidates,
                                                             final Annotation annotation,
                                                             final Comparator<Annotation> comparator ) {
      if ( coverCandidates.isEmpty() ) {
         return Collections.emptyList();
      }
      final int begin = annotation.getBegin();
      final int end = annotation.getEnd();
      return coverCandidates.stream()
                      .filter( c -> c.getBegin() <= begin && end <= c.getEnd() )
                      .sorted( comparator )
                      .collect( Collectors.toList() );
   }


   static public <A extends Annotation> List<A> getCoveringFromBegin_Shortest( final Collection<A> coverCandidates,
                                                                               final Annotation annotation ) {
      return getCovering( coverCandidates, annotation, Comparator.comparingInt( Annotation::getBegin )
                                                           .thenComparing( Annotation::getEnd ) );
   }

   static public <A extends Annotation> List<A> getCoveringFromBegin_Longest( final Collection<A> coverCandidates,
                                                                              final Annotation annotation ) {
      return getCovering( coverCandidates, annotation, ANNOTATION_BEGIN_LONG_SORTER );
   }

   static public <A extends Annotation> List<A> getCoveringFromShortest( final Collection<A> coverCandidates,
                                                                         final Annotation annotation ) {
      return getCovering( coverCandidates, annotation, ANNOTATION_BY_SHORT_SORTER );
   }


   static public boolean isValidSpan( final Pair<Integer> span ) {
      return span.getValue1() >= 0 && span.getValue2() >= span.getValue1();
   }

   static public boolean isValidSpan( final Annotation annotation ) {
      return annotation.getBegin() >= 0 && annotation.getEnd() >= annotation.getBegin();
   }

   static public boolean isValidSpan( final Pair<Integer> span, final int docLength ) {
      return span.getValue1() >= 0
             && span.getValue2() >= span.getValue1()
             && span.getValue2() <= docLength;
   }

   static public boolean isValidSpan( final Annotation annotation, final int docLength ) {
      return annotation.getBegin() >= 0
             && annotation.getEnd() >= annotation.getBegin()
             && annotation.getEnd() <= docLength;
   }


   /**
    * Sorts by first offset, LONGER bounds first:
    *   |=============|
    *      |========|
    *      |====|
    *        |==============|
    *                |==============|
    *                     |========|
    */
   static public final class SpanByBegin_LongSorter implements Comparator<Pair<Integer>> {
      public int compare( final Pair<Integer> p1, final Pair<Integer> p2 ) {
         final int start = p1.getValue1() - p2.getValue1();
         if ( start != 0 ) {
            return start;
         }
         return p2.getValue2() - p1.getValue2();
      }
   }


   /**
    * Sorts by first offset, LONGER bounds first:
    *   |=============|
    *      |========|
    *      |====|
    *        |==============|
    *                |==============|
    *                     |========|
    */
   static public final class AnnotationByBegin_LongSorter implements Comparator<Annotation> {
      public int compare( final Annotation a1, final Annotation a2 ) {
         final int start = a1.getBegin() - a2.getBegin();
         if ( start != 0 ) {
            return start;
         }
         return a2.getEnd() - a1.getEnd();
      }
   }


   /**
    * Sorts by span size, smallest to longest
    */
   static public final class AnnotationByShortSorter implements Comparator<Annotation> {
      public int compare( final Annotation a1, final Annotation a2 ) {
         return (a1.getEnd()-a1.getBegin()) - (a2.getEnd()-a2.getBegin());
      }
   }

   static public List<Pair<Integer>> subsumeSpans( final Collection<Pair<Integer>> spans ) {
      final List<Pair<Integer>> sorted = spans.stream()
                                              .distinct()
                                              .sorted( SPAN_BEGIN_LONG_SORTER )
                                              .collect( Collectors.toList() );
      final Collection<Pair<Integer>> removals = new HashSet<>();
      int i=0;
      while ( i<sorted.size()-1 ) {
         final int iEnd = sorted.get( i ).getValue2();
         for ( int j=i+1; j<sorted.size(); j++ ) {
            if ( sorted.get( j ).getValue2() > iEnd ) {
               i = j;
               break;
            }
            removals.add( sorted.get( j ) );
         }
      }
      sorted.removeAll( removals );
      return sorted;
   }

   /**
    *
    * @param annotations -
    * @param <A> -
    * @return subsuming annotations, sorted by begin offset.  Subsuming annotations with identical spans remain.
    */
   static public <A extends Annotation> List<A> subsumeAnnotations( final Collection<A> annotations ) {
      final List<A> sorted = annotations.stream()
                                        .sorted( ANNOTATION_BEGIN_LONG_SORTER )
                                        .collect( Collectors.toList() );
      final Collection<A> removals = new HashSet<>();
      int i = 0;
      int j = 1;
      int iEnd;
      while ( i < sorted.size()-1 && j < sorted.size() ) {
         iEnd = sorted.get( i ).getEnd();
         i++;
         j = i;
         while ( j < sorted.size() ) {
            if ( sorted.get( j ).getEnd() > iEnd ) {
               i = j;
               break;
            }
            removals.add( sorted.get( j ) );
            j++;
         }
      }
      sorted.removeAll( removals );
      return sorted;
   }


   static public List<Pair<Integer>> mergeSpans( final Collection<Pair<Integer>> spans ) {
      if ( spans.size() <= 1 ) {
         LOGGER.info( spans.size() + " spans to merge.  Returning as singleton." );
         return new ArrayList<>( spans );
      }
      final List<Pair<Integer>> sorted = spans.stream()
                                              .distinct()
                                              .sorted( SPAN_BEGIN_LONG_SORTER )
                                              .collect( Collectors.toList() );
      final List<Pair<Integer>> merged = new ArrayList<>();
      int beginIndex = 0;
      int endIndex = 0;
      int mergeBegin = sorted.get( beginIndex ).getValue1();
      int mergedEnd = sorted.get( beginIndex ).getValue2();
      while ( beginIndex < sorted.size() && endIndex < sorted.size() ) {
         mergeBegin = sorted.get( beginIndex ).getValue1();
         mergedEnd = sorted.get( beginIndex ).getValue2();
         LOGGER.info( "Merge Loop beginIndex:  beginIndex,endIndex " + beginIndex + "," + endIndex
                      + " mergeBegin,mergeEnd " + mergeBegin + "," + mergedEnd );
         beginIndex++;
         endIndex = beginIndex;
         while ( endIndex < sorted.size() ) {
            LOGGER.info( "Merge Loop endIndex:  beginIndex,endIndex " + beginIndex + "," + endIndex
                         + " mergeBegin,mergeEnd " + mergeBegin + "," + mergedEnd );
            if ( sorted.get( endIndex ).getValue1() > mergedEnd ) {
               LOGGER.info( "Adding Merged span " + mergeBegin + "," + mergedEnd
                            + " based upon sorted(j) begin > mergeEnd "
                            + sorted.get( endIndex ).getValue1() + " > " + mergedEnd
                            + " then setting i to j " + beginIndex + " to " + endIndex );
               merged.add( new Pair<>( mergeBegin, mergedEnd ) );
               beginIndex = endIndex;
               break;
            }
            LOGGER.info( "Setting mergeEnd to max of current mergeEnd or end of sorted(endIndex) "
                         + sorted.get( endIndex ).getValue2() + " or " + mergedEnd + " and incrementing endIndex "
                         + (endIndex+1) );
            mergedEnd = Math.max( sorted.get( endIndex ).getValue2(), mergedEnd );
            endIndex++;
         }
      }
      LOGGER.info( "Adding Merged span " + mergeBegin + "," + mergedEnd
                   + " because we are at the end. " );
      merged.add( new Pair<>( mergeBegin, mergedEnd ) );
      return merged;
   }


}
