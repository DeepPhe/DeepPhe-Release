package org.apache.ctakes.ner.filter;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.ner.group.Group;
import org.apache.ctakes.ner.group.GroupHierarchy;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
public abstract class HierarchyItemFilter<G extends Group<G>,I> {

   private final GroupHierarchy<G> _hierarchy;


   public HierarchyItemFilter( final GroupHierarchy<G> hierarchy ) {
      _hierarchy = hierarchy;
   }

   // 
   public Collection<I> getFilteredTerms( final Collection<I> items ) {
      final Map<G,Collection<G>> subsumerGroupMap = createSubsumerGroupMap();

      final Map<I,G> itemToGroupMap = createItemToGroupMap( items );

      final Map<Pair<Integer>, List<I>> spanItemsMap = null;
            // TODO TextSpanOwner
//            = items.stream().collect( Collectors.groupingBy( I::getTextSpan ) );

      final Map<Pair<Integer>,Map<G,List<I>>> spanToGroupItems
            = mapSpanGroupItems( spanItemsMap, itemToGroupMap, subsumerGroupMap );

      final Collection<I> subsumedItems = getSubsumedItems( spanToGroupItems, subsumerGroupMap );

      items.removeAll( subsumedItems );
      return items;
   }

   private Map<G,Collection<G>> createSubsumerGroupMap() {
      return _hierarchy.getGroups()
                      .stream()
                      .collect( Collectors.toMap( Function.identity(), _hierarchy::getSubsumed ) );
   }

   private Map<I,G> createItemToGroupMap( final Collection<I> items ) {
      final Map<I,G> map = new HashMap<>( items.size() );
      for ( I item : items ) {
         // TODO GroupNameOwner / GroupOwner
         final Collection<G> groups = null;
//               = item.getGroupNames()
//                                          .stream()
//                                          .map( _hierarchy::getByName )
//                                          .filter( Objects::nonNull )
//                                          .collect( Collectors.toList() );
         map.put( item, _hierarchy.getBestGroup( groups ) );
      }
      return map;
   }

   private Map<Pair<Integer>,Map<G,List<I>>> mapSpanGroupItems(
         final Map<Pair<Integer>, List<I>> spanItemsMap,
         final Map<I,G> itemToGroupMap,
         final Map<G,Collection<G>> subsumerMap ) {
      final Map<Pair<Integer>,Map<G,List<I>>> spanGroupItems = new HashMap<>( spanItemsMap.size() );
      for ( Map.Entry<Pair<Integer>,List<I>> spanItems : spanItemsMap.entrySet() ) {
         final Map<G,List<I>> groupItems
               = spanItems.getValue()
                          .stream()
                          .collect( Collectors.groupingBy( itemToGroupMap::get ) );
         // Subsume terms with equal span.
         final Collection<G> subsumedGroups
               = groupItems.keySet()
                           .stream()
                           .map( g -> subsumerMap.getOrDefault( g, Collections.emptyList() ) )
                           .flatMap( Collection::stream )
                           .collect( Collectors.toList() );
         if ( subsumedGroups.size() == groupItems.size() ) {
            logAllSubsumed( groupItems );
         }
         subsumedGroups.forEach( groupItems.keySet()::remove );
         if ( !groupItems.isEmpty() ) {
            spanGroupItems.put( spanItems.getKey(), groupItems );
         }
      }
      return spanGroupItems;
   }


   private Collection<I> getSubsumedItems(
         final Map<Pair<Integer>,Map<G,List<I>>> spanToGroupItems,
         final Map<G,Collection<G>> subsumerGroupMap ) {
      final List<Pair<Integer>> spans = spanToGroupItems.keySet()
                                                        .stream()
                                                        .sorted( Comparator.comparingInt( Pair<Integer>::getValue1 )
                                                                           .thenComparingInt( Pair::getValue2 ) )
                                                        .collect( Collectors.toList() );
      final Collection<I> subsumedItems = new HashSet<>();
      for ( int i=0; i<spans.size()-1; i++ ) {
         final Pair<Integer> iSpan = spans.get( i );
         final Map<G,List<I>> iGroupTerms = spanToGroupItems.get( iSpan );
         for ( int j=i+1; j<spans.size(); j++ ) {
            final Pair<Integer> jSpan = spans.get( j );
            final Map<G,List<I>> jGroupItems = spanToGroupItems.get( jSpan );
            Collection<G> subsumerGroups;
            Map<G,List<I>> subsumedGroupItems;
            if ( spanContainsSpan( iSpan, jSpan ) ) {
               subsumerGroups = iGroupTerms.keySet();
               subsumedGroupItems = jGroupItems;
            } else if ( spanContainsSpan( jSpan, iSpan ) ) {
               subsumerGroups = jGroupItems.keySet();
               subsumedGroupItems = iGroupTerms;
            } else {
               continue;
            }
            subsumerGroups.stream()
                          .map( g -> subsumerGroupMap.getOrDefault( g, Collections.emptyList() ) )
                          .flatMap( Collection::stream )
                          .map( g -> subsumedGroupItems.getOrDefault( g, Collections.emptyList() ) )
                          .forEach( subsumedItems::addAll );
         }
      }
      return subsumedItems;
   }


   static protected boolean spanContainsSpan( final Pair<Integer> span1, Pair<Integer> span2 ) {
      return span1.getValue1() <= span2.getValue1() && span2.getValue2() <= span1.getValue2();
   }

   private void logAllSubsumed( final Map<G,List<I>> groupTerms ) {
//      Logger.getLogger( "SemanticSubsumer" )
//            .warning( "All groups subsumed for "
//                  + groupTerms.values()
//                              .stream()
//                              .flatMap( Collection::stream )
//                              .map( I::getUri )
//                              .collect( Collectors.joining( " " ) ) );
   }

}
