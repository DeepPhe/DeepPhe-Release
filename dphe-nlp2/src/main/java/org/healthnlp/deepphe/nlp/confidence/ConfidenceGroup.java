package org.healthnlp.deepphe.nlp.confidence;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.nlp.confidence.ConfidencePlacement.*;


/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
final public class ConfidenceGroup<T extends ConfidenceOwner> {

   final private EnumMap<ConfidencePlacement, Collection<T>> _placementMap;

   public ConfidenceGroup( final Collection<T> confidenceOwners ) {
      _placementMap = new EnumMap<>( ConfidencePlacement.class );
      if ( confidenceOwners.size() <= 1 ) {
         _placementMap.put( BEST, confidenceOwners );
         return;
      }
      final Map<Double, List<T>> scoreMap = createConfidenceMap( confidenceOwners );
      final List<Double> rankList = new ArrayList<>( scoreMap.keySet() );
      Collections.sort( rankList );
      final Collection<T> best = scoreMap.get( rankList.get( rankList.size() - 1 ) );
      _placementMap.put( BEST, best );
      if ( rankList.size() == 1 ) {
         return;
      }
      final Collection<T> next = scoreMap.get( rankList.get( rankList.size() - 2 ) );
      _placementMap.put( NEXT, next );
      if ( rankList.size() == 2 ) {
         return;
      }
      for ( int i=0; i<rankList.size()-2; i++ ) {
         _placementMap.put( REST, scoreMap.get( rankList.get( i ) ) );
      }
   }

   private Collection<T> get( final ConfidencePlacement placement ) {
      return _placementMap.getOrDefault( placement, Collections.emptyList() );
   }

   public Collection<T> getBest() {
      return get( BEST );
   }

   public Collection<T> getNext() {
      return get( NEXT );
   }

   public Collection<T> getOther() {
      return get( REST );
   }

   private double getConfidence( final ConfidencePlacement placement ) {
      return get( placement ).stream()
                      .map( ConfidenceOwner::getConfidence )
                      .findFirst()
                      .orElse( 0.0 );
   }

   public double getBestConfidence() {
      return getConfidence( BEST );
   }

   public double getNextConfidence() {
      return getConfidence( NEXT );
   }

   public Collection<T> getBestAndNext() {
      final Collection<T> bestAndNext = new HashSet<>( getBest() );
      bestAndNext.addAll( getNext() );
      return bestAndNext;
   }

   public Collection<T> getAll() {
      final Collection<T> all = new HashSet<>( getBest() );
      all.addAll( getNext() );
      all.addAll( getOther() );
      return all;
   }

   /**
    *
    * @return At least 2 (if available) topmost confidence owners, which includes the BEST and,
    * if there is only 1 BEST then all the NEXT.
    */
   public Collection<T> getTopmost() {
      final Collection<T> topmost = new HashSet<>( getBest() );
      if ( topmost.size() != 1 ) {
         return topmost;
      }
      topmost.addAll( getNext() );
      return topmost;
   }



   static private <T extends ConfidenceOwner> Map<Double,List<T>> createConfidenceMap(
         final Collection<T> confidenceOwners ) {
      return confidenceOwners.stream()
                             .collect( Collectors.groupingBy( ConfidenceOwner::getConfidence ) );
   }


}
