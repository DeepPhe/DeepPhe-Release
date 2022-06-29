package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2022}
 */
abstract public class AttributeTable {



   abstract Map<String,Collection<ConceptAggregate>> getAllNeoplasmsMap();
   abstract Map<String,Collection<ConceptAggregate>> getBestNeoplasmsMap();

   abstract Collection<String> getUnwantedCodes();

   Collection<ConceptAggregate> getUnwantedNeoplasms() {
      return getUnwantedCodes().stream()
                               .map( v -> getAllNeoplasmsMap().getOrDefault( v, Collections.emptyList() ) )
                               .flatMap( Collection::stream )
                               .collect( Collectors.toSet() );
   }

   Map<String,Collection<ConceptAggregate>> getUnwantedNeoplasmsMap() {
      final Map<String,Collection<ConceptAggregate>> unwanted = new HashMap<>( getAllNeoplasmsMap() );
      unwanted.keySet().removeAll( getBestNeoplasmsMap().keySet() );
      return unwanted;
   }



      abstract Collection<String> getGenericCodes();

   Collection<ConceptAggregate> getGenericNeoplasms() {
      return getGenericCodes().stream()
                              .map( v -> getAllNeoplasmsMap().getOrDefault( v, Collections.emptyList() ) )
                              .flatMap( Collection::stream )
                              .collect( Collectors.toSet() );
   }

   abstract Collection<ConceptAggregate> getNotBestNeoplasms();

   abstract String getCode( final ConceptAggregate neoplasm,
                            final Collection<ConceptAggregate> neoplasms,
                            final Collection<ConceptAggregate> allConcepts );

   abstract Map<String,Collection<ConceptAggregate>> collectBest();


   abstract void addToBest( final String code, final ConceptAggregate neoplasm );

   boolean isWanted( final String code ) {
      return getUnwantedCodes().contains( code );
   }

   boolean areAllWanted() {
      return getUnwantedCodes().isEmpty();
   }



   protected Map<String, Collection<ConceptAggregate>> collectAll(
         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> codedNeoplasms = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final String code = getCode( neoplasm, neoplasms, allConcepts );
         codedNeoplasms.computeIfAbsent( code, c -> new ArrayList<>() ).add( neoplasm );
      }
      return codedNeoplasms;
   }



   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting Utilities.
   //
   /////////////////////////////////////////////////////////////////////////

   static Map<ConceptAggregate,String> createNeoplasmCodeMap(
         final Map<String,Collection<ConceptAggregate>> codeNeoplasmMap ) {
      final Map<ConceptAggregate,String> neoplasmCodeMap = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptAggregate>> codeNeoplasms : codeNeoplasmMap.entrySet() ) {
         final String histology = codeNeoplasms.getKey();
         codeNeoplasms.getValue().forEach( n -> neoplasmCodeMap.put( n, histology ) );
      }
      return neoplasmCodeMap;
   }


   static Map<String, Collection<ConceptAggregate>> getNeoplasmsAboveCutoff(
         final Map<String,Collection<ConceptAggregate>> codeNeoplasms,
         final double cutoff_constant,
         final Collection<String> keepCodes,
         final Collection<String> invalidCodes ) {
      final Map<String,Collection<ConceptAggregate>> tempCodeNeoplasms = new HashMap<>( codeNeoplasms );
      tempCodeNeoplasms.keySet().removeAll( keepCodes );
      if ( tempCodeNeoplasms.size() <= 1 ) {
         return new HashMap<>( codeNeoplasms );
      }
      final Map<String,Integer> codeMentionCounts = new HashMap<>();
      int max = 0;
      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : tempCodeNeoplasms.entrySet() ) {
         final String code = entry.getKey();
         int count = entry.getValue().stream()
                          .map( ConceptAggregate::getMentions )
                          .mapToInt( Collection::size )
                          .sum();
         codeMentionCounts.put( code, count );
         if ( !invalidCodes.contains( code ) ) {
            max = Math.max( max, count );
         }
      }
      double cutoff = max * cutoff_constant;
      final Collection<String> highCountCodes = getCodesAboveCutoff( codeMentionCounts, cutoff );
      highCountCodes.addAll( keepCodes );
      final Map<String,Collection<ConceptAggregate>> bestCodeNeoplasms = new HashMap<>( codeNeoplasms );
      bestCodeNeoplasms.keySet().retainAll( highCountCodes );
      return bestCodeNeoplasms;
   }


   static private Collection<String> getCodesAboveCutoff( final Map<String,Integer> codeMentionCounts,
                                                          final double cutoff ) {
      final Collection<String> lowCountCodes = codeMentionCounts.entrySet().stream()
                                                                .filter( e -> e.getValue() < cutoff )
                                                                .map( Map.Entry::getKey )
                                                                .collect( Collectors.toSet() );
      if ( lowCountCodes.isEmpty() ) {
         return new HashSet<>( codeMentionCounts.keySet() );
      }
      final Collection<String> codes = new HashSet<>( codeMentionCounts.keySet() );
      codes.removeAll( lowCountCodes );
      return codes;
   }



}
