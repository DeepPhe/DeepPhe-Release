package org.apache.ctakes.ner.filter;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.Group;
import org.apache.ctakes.ner.group.GroupHierarchy;
import org.apache.ctakes.ner.term.DetailedTerm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
public class HierarchyTermFilter<G extends Group<G>> implements TermFilter {

   private final GroupHierarchy<G> _hierarchy;


   public HierarchyTermFilter( final GroupHierarchy<G> hierarchy ) {
      _hierarchy = hierarchy;
   }

   public Collection<DetailedTerm> getFilteredTerms( final Collection<DetailedTerm> terms ) {
      final Map<G,Collection<G>> subsumerGroupMap = createSubsumerGroupMap();

      final Map<DetailedTerm,G> termToGroupMap = createTermToGroup( terms );

      final Map<Pair<Integer>, List<DetailedTerm>> spanTermsMap
            = terms.stream().collect( Collectors.groupingBy( DetailedTerm::getTextSpan ) );

      final Map<Pair<Integer>,Map<G,List<DetailedTerm>>> spanToGroupTerms
            = mapSpanGroupTerms( spanTermsMap, termToGroupMap, subsumerGroupMap );

      final Collection<DetailedTerm> subsumedTerms = getSubsumedTerms( spanToGroupTerms, subsumerGroupMap );

      terms.removeAll( subsumedTerms );
      return terms;
   }

   private Map<G,Collection<G>> createSubsumerGroupMap() {
      return _hierarchy.getGroups()
                      .stream()
                      .collect( Collectors.toMap( Function.identity(), _hierarchy::getSubsumed ) );
   }

   private Map<DetailedTerm,G> createTermToGroup( final Collection<DetailedTerm> terms ) {
      final Map<DetailedTerm,G> map = new HashMap<>( terms.size() );
      for ( DetailedTerm term : terms ) {
         final Collection<G> groups = term.getGroupNames()
                                          .stream()
                                          .map( _hierarchy::getByName )
                                          .filter( Objects::nonNull )
                                          .collect( Collectors.toList() );
         map.put( term, _hierarchy.getBestGroup( groups ) );
      }
      return map;
   }

   private Map<Pair<Integer>,Map<G,List<DetailedTerm>>> mapSpanGroupTerms(
         final Map<Pair<Integer>, List<DetailedTerm>> spanTermsMap,
         final Map<DetailedTerm,G> termToGroupMap,
         final Map<G,Collection<G>> subsumerMap ) {
      final Map<Pair<Integer>,Map<G,List<DetailedTerm>>> spanGroupTerms = new HashMap<>( spanTermsMap.size() );
      for ( Map.Entry<Pair<Integer>,List<DetailedTerm>> spanTerms : spanTermsMap.entrySet() ) {
         final Map<G,List<DetailedTerm>> groupTerms
               = spanTerms.getValue()
                          .stream()
                          .collect( Collectors.groupingBy( termToGroupMap::get ) );
         // Subsume terms of subsuming groups with equal span.
         final Collection<G> subsumedGroups
               = groupTerms.keySet()
                           .stream()
                           .map( subsumerMap::get )
               .filter( Objects::nonNull )
                           .flatMap( Collection::stream )
                           .collect( Collectors.toList() );
         subsumedGroups.forEach( groupTerms.keySet()::remove );
         if ( !groupTerms.isEmpty() ) {
            spanGroupTerms.put( spanTerms.getKey(), groupTerms );
         }
      }
      return spanGroupTerms;
   }


   private Collection<DetailedTerm> getSubsumedTerms(
         final Map<Pair<Integer>,Map<G,List<DetailedTerm>>> spanToGroupTerms,
         final Map<G,Collection<G>> subsumerGroupMap ) {
      // Sorting is not necessary.
      final List<Pair<Integer>> spans = new ArrayList<>( spanToGroupTerms.keySet() );
      final Collection<DetailedTerm> subsumedTerms = new HashSet<>();
      for ( int i=0; i<spans.size()-1; i++ ) {
         final Pair<Integer> iSpan = spans.get( i );
         final Map<G,List<DetailedTerm>> iGroupTerms = spanToGroupTerms.get( iSpan );
         for ( int j=i+1; j<spans.size(); j++ ) {
            final Pair<Integer> jSpan = spans.get( j );
            final Map<G,List<DetailedTerm>> jGroupTerms = spanToGroupTerms.get( jSpan );
            Collection<G> subsumerGroups;
            Map<G,List<DetailedTerm>> subsumedGroupTerms;
            if ( spanContainsSpan( iSpan, jSpan ) ) {
               subsumerGroups = iGroupTerms.keySet();
               subsumedGroupTerms = jGroupTerms;
            } else if ( spanContainsSpan( jSpan, iSpan ) ) {
               subsumerGroups = jGroupTerms.keySet();
               subsumedGroupTerms = iGroupTerms;
            } else {
               continue;
            }
            // Filter out smaller spans of a specially subsumed group
            subsumerGroups.stream()
                          .map( subsumerGroupMap::get )
                          .filter( Objects::nonNull )
                          .flatMap( Collection::stream )
                          .map( subsumedGroupTerms::get )
                          .filter( Objects::nonNull )
                          .forEach( subsumedTerms::addAll );
            // Filter out smaller spans of the same group.
            subsumerGroups.stream()
                          .map( subsumedGroupTerms::get )
                          .filter( Objects::nonNull )
                          .forEach( subsumedTerms::addAll );
         }
      }
      return subsumedTerms;
   }


   static protected boolean spanContainsSpan( final Pair<Integer> span1, Pair<Integer> span2 ) {
      return (span1.getValue1() <= span2.getValue1() && span2.getValue2() < span1.getValue2())
            || (span1.getValue1() < span2.getValue1() && span2.getValue2() <= span1.getValue2());
   }


}
