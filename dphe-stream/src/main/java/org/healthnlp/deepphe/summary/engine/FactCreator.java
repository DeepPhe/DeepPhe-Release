package org.healthnlp.deepphe.summary.engine;

import org.healthnlp.deepphe.neo4j.node.Fact;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {5/6/2021}
 */
final public class FactCreator {

   private FactCreator() {}


   static public Map<ConceptAggregate,Fact> createFactMap( final Collection<ConceptAggregate> concepts ) {
      final Map<ConceptAggregate,Fact> factMap = concepts.stream()
                     .collect( Collectors.toMap( Function.identity(), FactCreator::initFact ) );
      factMap.keySet().forEach( c -> addFactRelations( factMap, c ) );
      return factMap;
   }

   static private Fact initFact( final ConceptAggregate concept ) {
      final Fact fact = new Fact();
      fact.setId( concept.getId() );
      fact.setClassUri( concept.getUri() );
      fact.setName( concept.getPreferredText() );
      fact.setValue( concept.getCoveredText() );
      final List<String> mentionIds
            = concept.getMentions()
                     .stream()
                     .sorted( Comparator.comparingInt( Mention::getBegin ).thenComparing( Mention::getEnd ) )
                     .map( Mention::getId )
                     .collect( Collectors.toList() );
            fact.setDirectEvidenceIds( mentionIds );
      return fact;
   }

   static private List<Fact> finalizeFacts( final Map<ConceptAggregate,Fact> factMap ) {
      return factMap.keySet()
                    .stream()
                    .map( c -> addFactRelations( factMap, c ) )
                    .collect( Collectors.toList() );
   }

   static private Fact addFactRelations( final Map<ConceptAggregate,Fact> factMap,
                                         final ConceptAggregate concept ) {
      final Fact fact = factMap.get( concept );
      if ( fact == null ) {
         return null;
      }
      final Map<String,List<String>> factRelations = getFactRelations( factMap, concept );
      fact.setRelatedFactIds( factRelations );
      return fact;
   }

   static public Map<String,List<String>> getFactRelations( final Map<ConceptAggregate,Fact> factMap,
                                                         final ConceptAggregate concept ) {
      Map<String,List<String>> factRelations = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : concept.getRelatedConceptMap().entrySet() ) {
         final List<String> facts = entry.getValue()
                                       .stream()
                                       .map( factMap::get )
                                       .filter( Objects::nonNull )
                                       .map( Fact::getId )
                                       .collect( Collectors.toList() );
         factRelations.put( entry.getKey(), facts );
      }
      return factRelations;
   }


}
