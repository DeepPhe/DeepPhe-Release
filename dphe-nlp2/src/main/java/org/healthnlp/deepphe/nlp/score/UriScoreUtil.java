package org.healthnlp.deepphe.nlp.score;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.core.util.KeyValue;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/22/2020
 */
final public class UriScoreUtil {

   static private final Logger LOGGER = Logger.getLogger( "UriScoreUtil" );

   private UriScoreUtil() {}


//   static public String getBestUri( final Collection<String> uris ) {
//      final Map<String,Collection<String>> uriRootsMap = UriUtil.mapUriRoots( uris );
//      return getBestUriScore( uris, uriRootsMap ).getKey();
//   }
//
//   static public String getBestUri( final Collection<String> uris,
//                                    final Map<String,Collection<String>> uriRootsMap ) {
//      return getBestUriScore( uris, uriRootsMap ).getKey();
//   }

//   static public String getBestUri( final Collection<String> uris,
//                                    final Map<String,Collection<String>> uriRootsMap ) {
//      return getBestUriScore( uris, uriRootsMap ).getKey();
//   }

//   static public Map<String,Integer> createClassLevelMap( final List<KeyValue<String,Double>> bestKeyValues ) {
////      if ( bestKeyValues.size() == 1 ) {
//////         LOGGER.info( "Only one best URI score for concept " + bestKeyValues.get( 0 ).getKey() + " score = " +  bestKeyValues.get( 0 ).getValue() );
////         return Collections.singletonMap( bestKeyValues.get( 0 ).getKey(), 1 );
////      }
//      return bestKeyValues.stream()
//                           .map( KeyValue::getKey )
//                           .collect( Collectors.toMap( Function.identity(),
//                                                       Neo4jOntologyConceptUtil::getClassLevel ) );
//   }

//   static public Map<String,Integer> createUriClassLevelMap( final Collection<String> uris ) {
//      return uris.stream()
//                 .collect( Collectors.toMap( Function.identity(),
//                                             Neo4jOntologyConceptUtil::getClassLevel ) );
//   }


   static public KeyValue<String,Double> getBestUriScore( final Collection<String> uris,
                                                          final Map<String,Collection<String>> uriRootsMap,
                                                          final Collection<Mention> mentions ) {
      final List<KeyValue<String, Double>> uriQuotients = mapUriQuotients( uris, uriRootsMap, mentions );
      return getBestUriScore( uriRootsMap, uriQuotients );
   }

      static public KeyValue<String,Double> getBestUriScore( final Map<String,Collection<String>> uriRootsMap,
                                                             final List<KeyValue<String, Double>> uriQuotients ) {

//      LOGGER.info( "!!!    UriScoreUtil.getBestUriScore");
//      uriQuotients.stream().map( kv -> "URI " + kv.getKey()
//                                       + "   quotient score " + kv.getValue()
//                                       + "   class level " + loggerClassLevelMap.get( kv.getKey() )
//                                       + "   root count " + uriRootsMap.get( kv.getKey() ).size()
//                                       + "   quotient level score " + (kv.getValue()*loggerClassLevelMap.get( kv.getKey() ))
//                                       + "   rooted " + (kv.getValue()*loggerClassLevelMap.get( kv.getKey() )*uriRootsMap.get( kv.getKey() ).size()) )
//                  .forEach( LOGGER::info );

      if ( uriQuotients.size() == 1 ) {
//         LOGGER.info( "Only one URI for concept " + uriQuotients.get( 0 ).getKey() + " score = 1.0"  );
         return uriQuotients.get( 0 );
      }
      final List<KeyValue<String,Double>> bestKeyValues = getBestUriScores( uriQuotients );
      if ( bestKeyValues.size() == 1 ) {
//         LOGGER.info( "Only one best URI score for concept " + bestKeyValues.get( 0 ).getKey() + " score = " +  bestKeyValues.get( 0 ).getValue() );
         return bestKeyValues.get( 0 );
      }
//      final Map<String,Integer> classLevelMap = createClassLevelMap( bestKeyValues );
         final Map<String,Integer> classLevelMap = Collections.emptyMap();

      return getBestUriScore( bestKeyValues, classLevelMap, uriRootsMap );
   }


   static public KeyValue<String,Double> getBestUriScore( final List<KeyValue<String,Double>> bestUriScores,
                                                          final Map<String,Integer> classLevelMap,
                                                          final Map<String,Collection<String>> uriRootsMap ) {
//      LOGGER.info( "The best URI is the one with the highest quotient score and the highest class level " +
//                   "(furthest from root by the shortest path) with ties broken by total number of nodes (all routes) to root.\n" +
//                   "This is all about high representation and high precision.\n" +
//                   "The highest quotient is a measure of fully and exactly representing the most mentions.\n" +
//                   "The class level is a measure of specificity - the furthest the shortest path is from root the more specific the concept.\n" +
//                   "Breaking a tie with the most nodes between a concept and root is sort of a measure of both\n" +
//                   "specificity and high representation, but a much less exact measure of each." );
//      final ToIntFunction<KeyValue<String,Double>> getClassLevel = kv -> classLevelMap.get( kv.getKey() );
      final ToIntFunction<KeyValue<String,Double>> getRootCount = kv -> uriRootsMap.get( kv.getKey() ).size();
//      return bestUriScores.stream()
//                          .max( Comparator.comparingInt( getClassLevel ).thenComparingInt( getRootCount ) )
//                          .orElse( bestUriScores.get( bestUriScores.size()-1 ) );
      return bestUriScores.stream()
                          .max( Comparator.comparingInt( getRootCount ) )
                          .orElse( bestUriScores.get( bestUriScores.size()-1 ) );
   }

   static public List<KeyValue<String,Double>> getBestUriScores( final List<KeyValue<String,Double>> uriQuotients ) {
      if ( uriQuotients.isEmpty() ) {
         return Collections.singletonList( new KeyValue<>( "DeepPhe", 1d ) );
      }
      final Double bestQuotient = uriQuotients.get( uriQuotients.size()-1 ).getValue();
      return uriQuotients.stream()
                         .filter( q -> bestQuotient.compareTo( q.getValue() ) == 0 )
                         .sorted( Comparator.comparing( KeyValue::getKey ) )
                         .collect( Collectors.toList() );
   }

   static public List<KeyValue<String,Double>> getBestUriScores( final List<KeyValue<String,Double>> uriQuotients,
                                                                 final Map<String,Integer> classLevelMap,
                                                                 final Map<String,Collection<String>> uriRootsMap ) {
      // uriQuotients is list of pairs, URI to quotient value, sorted lowest to highest quotient value
      if ( uriQuotients.isEmpty() ) {
         return Collections.singletonList( new KeyValue<>( "DeepPhe", 1d ) );
      }
      final ToDoubleFunction<KeyValue<String,Double>> getQuotient = KeyValue::getValue;
      final ToIntFunction<KeyValue<String,Double>> getClassLevel = kv -> classLevelMap.get( kv.getKey() );
      final ToIntFunction<KeyValue<String,Double>> getRootCount = kv -> uriRootsMap.get( kv.getKey() ).size();
      return uriQuotients.stream()
                         .sorted( Comparator.comparingDouble( getQuotient )
                                            .thenComparingInt( getClassLevel )
                                            .thenComparingInt( getRootCount ) )
                         .collect( Collectors.toList() );
   }


   static public List<KeyValue<String,Double>> mapUriQuotients( final Collection<String> uris,
                                                                final Map<String, Collection<String>> uriRootsMap,
                                                                final Collection<Mention> mentions ) {
      final Map<String,Integer> uriCountsMap = mapUriMentionCounts( mentions );
      return mapUriQuotientsA( uris, uriRootsMap, uriCountsMap );
   }

   static public List<KeyValue<String,Double>> mapUriQuotients( final Collection<String> uris,
                                                                final Map<String, Collection<String>> uriRootsMap,
                                                                final Map<String,Collection<Mention>> uriMentions ) {
      final Collection<Mention> mentions = uris.stream()
                                               .map( uriMentions::get )
                                               .filter( Objects::nonNull )
                                               .flatMap( Collection::stream )
                                               .collect( Collectors.toSet() );
      return mapUriQuotients( uris, uriRootsMap, mentions );
   }


   static public List<KeyValue<String,Double>> mapUriQuotientsA( final Collection<String> uris,
                                                                final Map<String, Collection<String>> uriRootsMap,
                                                                final Map<String,Integer> uriCountsMap ) {

//      LOGGER.info( "!!!    UriScoreUtil.mapUriQuotientsA" );
//      uriCountsMap.keySet().stream().filter( k -> !uriRootsMap.containsKey( k ) ).forEach( k -> LOGGER.info( "No uriRoots entry for " + k ) );
//      uriCountsMap.forEach( (k,v) -> LOGGER.info( "Uri Mentions: " + k + " : " + v ) );

      final Map<String,Double> uriQuotientsB = mapUriQuotientsB( uris, uriRootsMap, uriCountsMap );
      return listUriQuotients( uriQuotientsB );
   }

//   public List<KeyValue<String,Double>> getUriDistanceScores() {
//      final List<String> uris = createUriList();
//      final Map<String,Long> uriCountsMap = mapUriCounts( uris );
//      return mapUriDistanceQuotients( uris, uriCountsMap, getUriRootsMap() )
//            .entrySet()
//            .stream()
//            .map( e -> new KeyValue<>( e.getKey(), e.getValue() ) )
//            .sorted( Comparator.comparingDouble( KeyValue::getValue ) )
//            .collect( Collectors.toList() );
//   }

//   static public Map<String,Long> mapUriMentionCounts( final Collection<String> uris ) {
//      if ( uris.size() == 1 ) {
//         final String uri = new ArrayList<>( uris ).get( 0 );
////         LOGGER.info( "Only one URI for concept " + uri + " quotient score = 1.0"  );
//         return Collections.singletonMap( uri, 1L );
//      }
//      uris.remove( UriConstants.NEOPLASM );
//      if ( uris.size() == 1 ) {
//         final String uri = new ArrayList<>( uris ).get( 0 );
////         LOGGER.info( "Only one URI under root for concept " + uri + " quotient score = 1.0"  );
//         return Collections.singletonMap( uri, 1L );
//      }
//      return uris.stream()
//                 .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
//   }

   static public Map<String,Integer> mapUriMentionCounts( final Collection<Mention> mentions ) {
      final Map<String,Integer> uriMentionCounts = new HashMap<>();
      for ( Mention mention : mentions ) {
         final String uri = mention.getClassUri();
         uriMentionCounts.put( uri, uriMentionCounts.getOrDefault( uri, 0 ) + 1 );
      }
      return uriMentionCounts;
   }

   /**
    * Does not remove the uris that are subsumed.
    * e.g. Upper_Limb (2), Arm (3) == {[Arm,(5)],[Upper_Limb,(2)]}   <-- Upper Limb count in map even though subsumed.
    * @param uris -
    * @param uriRootsMap -
    * @param uriCountsMap -
    * @return -
    */
   static public Map<String,Integer> mapUriSums( final Collection<String> uris,
                                              final Map<String,Collection<String>> uriRootsMap,
                                              final Map<String,Integer> uriCountsMap ) {
      final Map<String,Integer> uriSumMap = new HashMap<>();
      final Collection<String> uriSet = new HashSet<>( uris );
      for ( String uri : uriSet ) {
         final Collection<String> uriRoots = new HashSet<>( uriRootsMap.getOrDefault( uri, new HashSet<>() ) );
         uriRoots.add( uri );
         // How many of the roots of this uri are in the collection of uris for this concept (and how many times), multiplied
         final int sum = uriRoots.stream()
                                  .filter( uriSet::contains )
                                  .mapToInt( u -> uriCountsMap.getOrDefault( u, 0 ) )
                                  .sum();
         uriSumMap.put( uri, sum );
//         LOGGER.info( "URI " + uri + " has " + sum
//                      + " nodes in its path to root that are also distinct URIs for mentions in this ConceptAggregate." );
      }
      return uriSumMap;
   }

   /**
    * Removes the uris that are subsumed.
    * e.g. Upper_Limb (2), Arm (3) == {[Arm,(5)]}  <-- Upper_Limb was subsumed.
    * @param uris -
    * @param uriRootsMap -
    * @param uriCountsMap -
    * @return -
    */
   static public Map<String,Integer> mapBestUriSums( final Collection<String> uris,
                                              final Map<String,Collection<String>> uriRootsMap,
                                              final Map<String,Integer> uriCountsMap ) {
      final Map<String,Integer> uriSumMap = new HashMap<>();
      final Collection<String> uriSet = new HashSet<>( uris );
      final Collection<String> removalUris = new HashSet<>();
      for ( String uri : uriSet ) {
         final Collection<String> uriRoots = new HashSet<>( uriRootsMap.getOrDefault( uri, new HashSet<>() ) );
         final Collection<String> subsumedUris = uriRoots.stream()
                                                         .filter( u -> !uri.equals( u ) )
                                                         .filter( uriSet::contains )
                                                         .collect( Collectors.toSet() );
         removalUris.addAll( subsumedUris );
         subsumedUris.add( uri );
         // How many of the roots of this uri are in the collection of uris for this concept (and how many times), multiplied
         final int sum = subsumedUris.stream()
                                      .mapToInt( uriCountsMap::get )
                                       .sum();
         uriSumMap.put( uri, sum );
//         LOGGER.info( "URI " + uri + " has " + sum
//                      + " nodes in its path to root that are also distinct URIs for mentions in this ConceptAggregate." );
      }
      uriSumMap.keySet().removeAll( removalUris );
      return uriSumMap;
   }


   /**
    * Removes the uris that are subsumed.
    * e.g. Upper_Limb (2), Arm (3) == {[Arm,(5)]}  <-- Upper_Limb was subsumed.
    * @param uris -
    * @param uriRootsMap -
    * @return -
    */
   static public Map<String,Collection<String>> mapUriSetBranches( final Collection<String> uris,
                                                     final Map<String,Collection<String>> uriRootsMap ) {
      final Map<String,Collection<String>> uriSetBranchMap = new HashMap<>();
      final Collection<String> uriSet = new HashSet<>( uris );
      final Collection<String> removalUris = new HashSet<>();
      for ( String uri : uriSet ) {
         final Collection<String> uriRoots = new HashSet<>( uriRootsMap.getOrDefault( uri, new HashSet<>() ) );
         final Collection<String> subsumedUris = uriRoots.stream()
                                                         .filter( u -> !uri.equals( u ) )
                                                         .filter( uriSet::contains )
                                                         .collect( Collectors.toSet() );
         removalUris.addAll( subsumedUris );
         subsumedUris.add( uri );
         uriSetBranchMap.put( uri, subsumedUris );
//         LOGGER.info( "URI " + uri + " has " + sum
//                      + " nodes in its path to root that are also distinct URIs for mentions in this ConceptAggregate." );
      }
      uriSetBranchMap.keySet().removeAll( removalUris );
      return uriSetBranchMap;
   }


   /**
    *
    * @param uris list of uris associated with some thing.  Order does not matter.
    * @return Map of Uris and their scores.
    */
   static public Map<String,Double> mapUriQuotientsB( final Collection<String> uris,
                                                       final Map<String,Collection<String>> uriRootsMap,
                                                       final Map<String,Integer> uriCountsMap ) {
      final Map<String,Integer> uriSumMap = mapUriSums( uris, uriRootsMap, uriCountsMap );
      final int totalSum = uriCountsMap.values().stream().mapToInt( i -> i ).sum();
      return mapUriQuotientsBB( uriSumMap, totalSum );
   }

   /**
    *
    * @param uriSumMap -
    * @param totalSum -
    * @return Map of Uris and their scores.
    */
   static public Map<String,Double> mapUriQuotientsBB( final Map<String,Integer> uriSumMap,
                                                       final int totalSum ) {
//      LOGGER.info( "Why we are using this number:  Each node is scored +1 for each Mention that it represents.\n" +
//                   "For instance, assume BrCa has the path to root [root,Ca,BrCa].\n" +
//                   "If we have mentions BrCa and Ca, node BrCa represents both of them with but with high specificity.\n" +
//                   "Ca, while applicable to both mentions, is not as specific." );
      return uriSumMap.keySet()
                      .stream()
                      .collect( Collectors.toMap( Function.identity(),
                                                  u -> computeQuotient( u, uriSumMap, totalSum ) ) );
   }

   static public List<KeyValue<String,Double>> listUriQuotients( final Map<String,Double> uriQuotientMap ) {

//      LOGGER.info( "!!!    UriScoreUtil.mapUriQuotients" );
//      uriCountsMap.keySet().stream().filter( k -> !uriRootsMap.containsKey( k ) ).forEach( k -> LOGGER.info( "No uriRoots entry for " + k ) );
//      uriCountsMap.forEach( (k,v) -> LOGGER.info( "Uri Mentions: " + k + " : " + v ) );

      return uriQuotientMap
            .entrySet()
            .stream()
            .map( e -> new KeyValue<>( e.getKey(), e.getValue() ) )
            .sorted( Comparator.comparingDouble( KeyValue::getValue ) )
            .collect( Collectors.toList() );
   }



//   /**
//    *
//    * @param uris list of uris associated with some thing.  Order does not matter.
//    * @return Map of Uris and their scores.
//    */
//   static private Map<String,Double> mapUriDistanceQuotients( final List<String> uris,
//                                                              final Map<String,Long> uriCounts,
//                                                              final Map<String,Collection<String>> uriRootsMap ) {
//      final Map<String,Long> uriDistanceProducts = new HashMap<>();
//      for ( Map.Entry<String,Collection<String>> uriRoots : uriRootsMap.entrySet() ) {
//         final String uri = uriRoots.getKey();
//         final Collection<String> allUris = new HashSet<>( uriRoots.getValue() );
//         allUris.add( uri );
//         final long product = allUris.stream()
//                                     .filter( uris::contains )
//                                     .mapToLong( uriCounts::get )
//                                     .reduce( 1, (a, b) -> a * b );
//         final long distance = uriRoots.getValue().size();
//         uriDistanceProducts.put( uri, distance + product );
//      }
//      return uriDistanceProducts.keySet().stream()
//                                .collect( Collectors.toMap( Function.identity(),
//                                      u -> computeQuotient( u, uriDistanceProducts ) ) );
//   }

   static private double computeQuotient( final String uri,
                                          final Map<String,Integer> uriProducts,
                                          final int productsSum ) {
      if ( uriProducts.size() == 1 ) {
         return 1d;
      }
//      LOGGER.info( "Representation Quotient for " + uri + " : " + uriProducts.get( uri ) + " / " + productsSum + " = " + Double.valueOf( uriProducts.get( uri ) ) / productsSum );
      return Double.valueOf( uriProducts.get( uri ) ) / productsSum;
   }


   static private double computeQuotient( final String uri, final Map<String,Integer> uriProducts ) {
      if ( uriProducts.size() == 1 ) {
         return 1d;
      }

//      LOGGER.info( "Quotient for " + uri + " : " + uriProducts.get( uri ) + " / " + sumAllExcept( uri, uriProducts ) + " = " + Double.valueOf( uriProducts.get( uri ) ) / sumAllExcept( uri, uriProducts ) );

      return Double.valueOf( uriProducts.get( uri ) ) / sumAllExcept( uri, uriProducts );
   }


   static private long sumAllExcept( final String exceptUri, final Map<String,Integer> uriProducts ) {
      return uriProducts.entrySet().stream()
                        .filter( e -> !e.getKey().equals( exceptUri ) )
                        .mapToInt( Map.Entry::getValue )
                        .sum();
   }

   static private long sumAll( final Map<String,Long> uriProducts ) {
      return uriProducts.values().stream()
                        .mapToLong( l -> l )
                        .sum();
   }





}
