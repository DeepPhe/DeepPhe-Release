//package org.healthnlp.deepphe.summary.attribute;
//
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.healthnlp.deepphe.util.KeyValue;
//import org.healthnlp.deepphe.util.UriScoreUtil;
//
//import java.util.*;
//import java.util.function.Consumer;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//
//public interface FeatureHelper {
//
//   String getBestUri();
//
//   Collection<String> getAllBestAssociatedUris();
//
//   Map<String,Collection<String>> getPatientUriRootsMap();
//
//   ConceptAggregate getNeoplasm();
//
//   default Collection<ConceptAggregate> getHasBestUriConcepts( final Collection<ConceptAggregate> concepts ) {
//      return concepts.stream()
//                     .filter( c -> c.getAllUris().contains( getBestUri() ) )
//                     .collect( Collectors.toSet() );
//   }
//   default Collection<ConceptAggregate> getExactBestUriConcepts( final Collection<ConceptAggregate> concepts ) {
//      return concepts.stream()
//                     .filter( c -> c.getUri().equals( getBestUri() ) )
//                     .collect( Collectors.toSet() );
//   }
//
//   default Collection<String> getAllUris( final Collection<ConceptAggregate> concepts ) {
//      return concepts.stream()
//                     .map( ConceptAggregate::getAllUris )
//                     .flatMap( Collection::stream )
//                     .collect( Collectors.toSet() );
//   }
//   default Collection<Mention> getAllMentions( final Collection<ConceptAggregate> concepts ) {
//      return concepts.stream()
//                     .map( ConceptAggregate::getMentions )
//                     .flatMap( Collection::stream )
//                     .collect( Collectors.toSet() );
//   }
//
//   default Collection<Mention> getHasBestUriMentions( final Collection<ConceptAggregate> concepts ) {
//      return getHasBestUriConcepts( concepts ).stream()
//                                           .map( ConceptAggregate::getMentions )
//                                           .flatMap( Collection::stream )
//                                           .collect( Collectors.toSet() );
//   }
//   default Collection<Mention> getExactBestUriMentions( final Collection<ConceptAggregate> concepts ) {
//      return concepts.stream()
//                     .map( ConceptAggregate::getMentions )
//                     .flatMap( Collection::stream )
//                     .filter( m -> m.getClassUri().equals( getBestUri() ) )
//                     .collect( Collectors.toSet() );
//   }
//
//   default Map<String, List<Mention>> mapUriMentions( final Collection<ConceptAggregate> concepts ) {
//      return concepts.stream()
//                     .map( ConceptAggregate::getMentions )
//                     .flatMap( Collection::stream )
//                     .collect( Collectors.groupingBy( Mention::getClassUri ) );
//   }
//
//
//   default Map<String,Collection<String>> mapAllUriRoots( final Collection<ConceptAggregate> concepts ) {
//      final Map<String,Collection<String>> rootsMap = new HashMap<>();
//      concepts.stream()
//              .map( ConceptAggregate::getAllUris )
//              .flatMap( Collection::stream )
//              .distinct()
//              .forEach( u -> rootsMap.put( u, getPatientUriRootsMap().getOrDefault( u, Collections.emptyList() ) ) );
//      return rootsMap;
//   }
//
//
//   default Map<String,Integer> mapUriBranchConceptCounts( final Collection<ConceptAggregate> concepts ) {
//      final Map<String,Collection<ConceptAggregate>> uriConceptMap = mapUriBranchConcepts( concepts );
//      return uriConceptMap.entrySet()
//                          .stream()
//                          .collect( Collectors.toMap( Map.Entry::getKey,
//                                                      e -> e.getValue().size() ) );
//   }
//
//   default Map<String,Integer> mapUriBranchMentionCounts( final Collection<ConceptAggregate> concepts ) {
//      final Map<String,Collection<String>> uriRootsMap = mapAllUriRoots( concepts );
//      final Map<String,List<Mention>> uriMentionsMap = mapUriMentions( concepts );
//      final Map<String,Integer> uriMentionCountsMap
//            = uriMentionsMap.entrySet()
//                            .stream()
//                            .collect( Collectors.toMap( Map.Entry::getKey,
//                                                        e -> e.getValue().size() ) );
//      return mapUriBranchObjectCounts( uriRootsMap.keySet(), uriRootsMap, uriMentionCountsMap );
//   }
//   default Map<String,Integer> mapUriBranchMentionCounts( final Collection<String> uris,
//                                                          final Collection<ConceptAggregate> concepts ) {
//      final Map<String,Collection<String>> uriRootsMap = mapAllUriRoots( concepts );
//      final Map<String,List<Mention>> uriMentionsMap = mapUriMentions( concepts );
//      final Map<String,Integer> uriMentionCountsMap
//            = uriMentionsMap.entrySet()
//                            .stream()
//                            .collect( Collectors.toMap( Map.Entry::getKey,
//                                                        e -> e.getValue().size() ) );
//      return mapUriBranchObjectCounts( uris, uriRootsMap, uriMentionCountsMap );
//   }
//
//   default Map<String,Integer> mapUriBranchObjectCounts( final Collection<String> uris,
//                                                         final Map<String,Collection<String>> uriRootsMap,
//                                                         final Map<String,Integer> uriObjectCountsMap ) {
//      return UriScoreUtil.mapBestUriSums( uris, uriRootsMap, uriObjectCountsMap );
//   }
//
//   default Map<String,Collection<String>> mapUriSetBranches( final Collection<String> uris,
//                                                             final Map<String,Collection<String>> uriRootsMap ) {
//      return UriScoreUtil.mapUriSetBranches( uris, uriRootsMap );
//   }
//
//   default Map<String,Collection<ConceptAggregate>> mapUriBranchConcepts( final Collection<ConceptAggregate> concepts ) {
//      final Map<String,Collection<String>> uriRootsMap = mapAllUriRoots( concepts );
//      final Map<String,Collection<String>> uriSetsMap = UriScoreUtil.mapUriSetBranches( uriRootsMap.keySet(),
//                                                                                        uriRootsMap );
//      final Map<String,Collection<ConceptAggregate>> uriConceptMap = new HashMap<>();
//      for ( ConceptAggregate concept : concepts ) {
//         for ( Map.Entry<String,Collection<String>> uriSets : uriSetsMap.entrySet() ) {
//            if ( uriSets.getValue()
//                        .stream()
//                        .anyMatch( concept.getAllUris()::contains ) ) {
//               uriConceptMap.computeIfAbsent( uriSets.getKey(), u -> new HashSet<>() ).add( concept );
//               break;
//            }
//         }
//      }
//      return uriConceptMap;
//   }
//
//
//   static Map<Boolean,Collection<Mention>> mapEvidence( final Collection<String> directEvidenceUris,
//                                                        final Collection<ConceptAggregate> concepts ) {
//      final Map<Boolean,Collection<Mention>> evidenceMap = new HashMap<>();
//      evidenceMap.put( DIRECT_EVIDENCE, new HashSet<>() );
//      evidenceMap.put( INDIRECT_EVIDENCE, new HashSet<>() );
//      final Function<ConceptAggregate, KeyValue<Boolean,Collection<Mention>>> splitMentions = c ->
//            directEvidenceUris.contains( c.getUri() )
//            ? new KeyValue<>( DIRECT_EVIDENCE, c.getMentions() )
//            : new KeyValue<>( INDIRECT_EVIDENCE, c.getMentions() );
//      final Consumer<KeyValue<Boolean,Collection<Mention>>> placeMentions = kv ->
//            evidenceMap.computeIfAbsent( kv.getKey(), v -> new HashSet<>() ).addAll( kv.getValue() );
//      concepts.stream()
//                  .map( splitMentions )
//                  .forEach( placeMentions );
//      return evidenceMap;
//   }
//
//}
