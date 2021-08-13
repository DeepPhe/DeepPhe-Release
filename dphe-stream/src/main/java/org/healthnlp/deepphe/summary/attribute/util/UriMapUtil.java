package org.healthnlp.deepphe.summary.attribute.util;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.*;
import java.util.stream.Collectors;

final public class UriMapUtil {

   private UriMapUtil() {}

   static public Map<String, List<Mention>> mapUriMentions( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .collect( Collectors.groupingBy( Mention::getClassUri ) );
   }

   static public Map<String, List<Mention>> mapUriConceptMentions( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.groupingBy( Mention::getClassUri ) );
   }

   static public Map<String,Integer> mapAllUriBranchMentionCounts( final Map<String, List<Mention>> uriMentionsMap,
                                                                    final Map<String,Collection<String>> uriRootsMap ) {
      final Map<String,Integer> uriMentionCountsMap
            = uriMentionsMap.entrySet()
                            .stream()
                            .collect( Collectors.toMap( Map.Entry::getKey,
                                                        e -> e.getValue().size() ) );
      return UriScoreUtil.mapBestUriSums( uriRootsMap.keySet(), uriRootsMap, uriMentionCountsMap );
   }

   static public Map<String,Integer> mapUriBranchConceptCounts( final Collection<ConceptAggregate> concepts,
                                                                 final Map<String, Collection<String>> uriRootsMap ) {
      final Map<String,Collection<ConceptAggregate>> uriConceptMap
            = mapUriBranchConcepts( concepts, uriRootsMap );
      return uriConceptMap.entrySet()
                          .stream()
                          .collect( Collectors.toMap( Map.Entry::getKey,
                                                      e -> e.getValue().size() ) );
   }

   static public Map<String,Collection<ConceptAggregate>> mapUriBranchConcepts(
         final Collection<ConceptAggregate> concepts,
         final Map<String, Collection<String>> allUriRootsMap ) {
      final Map<String,Collection<String>> uriRootsMap = mapAllUriRoots( concepts, allUriRootsMap );
      final Map<String,Collection<String>> uriSetsMap = UriScoreUtil.mapUriSetBranches( uriRootsMap.keySet(),
                                                                                        uriRootsMap );
      final Map<String,Collection<ConceptAggregate>> uriConceptMap = new HashMap<>();
      for ( ConceptAggregate concept : concepts ) {
         for ( Map.Entry<String,Collection<String>> uriSets : uriSetsMap.entrySet() ) {
            if ( uriSets.getValue()
                        .stream()
                        .anyMatch( concept.getAllUris()::contains ) ) {
               uriConceptMap.computeIfAbsent( uriSets.getKey(), u -> new HashSet<>() ).add( concept );
               break;
            }
         }
      }
      return uriConceptMap;
   }

   static public Map<String,Integer> mapUriBranchMentionCounts(
         final Collection<ConceptAggregate> concepts,
         final Map<String, Collection<String>> allUriRootsMap ) {
      final Map<String,Collection<String>> uriRootsMap = mapAllUriRoots( concepts, allUriRootsMap );
      final Map<String,List<Mention>> uriMentionsMap = mapUriConceptMentions( concepts );
      final Map<String,Integer> uriMentionCountsMap
            = uriMentionsMap.entrySet()
                            .stream()
                            .collect( Collectors.toMap( Map.Entry::getKey,
                                                        e -> e.getValue().size() ) );
      return UriScoreUtil.mapBestUriSums( uriRootsMap.keySet(), uriRootsMap, uriMentionCountsMap );
   }

   static public Map<String,Collection<String>> mapAllUriRoots( final Collection<ConceptAggregate> concepts,
                                                                 final Map<String, Collection<String>> allUriRootsMap ) {
      final Map<String,Collection<String>> rootsMap = new HashMap<>();
      concepts.stream()
              .map( ConceptAggregate::getAllUris )
              .flatMap( Collection::stream )
              .distinct()
              .forEach( u -> rootsMap.put( u, allUriRootsMap.getOrDefault( u, Collections.emptyList() ) ) );
      return rootsMap;
   }



}
