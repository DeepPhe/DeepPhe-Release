package org.healthnlp.deepphe.summary.engine;

import org.healthnlp.deepphe.neo4j.node.Fact;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {5/7/2021}
 */
final public class FactRelationUtil {

   private FactRelationUtil() {}

   static public Map<String, List<Fact>> getRelatedFactsMap( final Map<String,List<String>> relatedIdMap,
                                                            final Map<String,Fact> factIdMap ) {
      final Map<String,List<Fact>> relatedFactsMap = new HashMap<>( relatedIdMap.size() );
      for ( Map.Entry<String,List<String>> idRelated : relatedIdMap.entrySet() ) {
         final List<Fact> facts = idRelated.getValue()
                                           .stream()
                                           .map( factIdMap::get )
                                           .collect( Collectors.toList() );
         relatedFactsMap.put( idRelated.getKey(), facts );
      }
      return relatedFactsMap;
   }


}
