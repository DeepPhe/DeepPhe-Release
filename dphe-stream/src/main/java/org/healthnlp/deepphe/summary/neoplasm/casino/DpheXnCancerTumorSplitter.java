package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.concept.ConceptAggregate.NULL_AGGREGATE;

/**
 * @author SPF , chip-nlp
 * @since {6/23/2022}
 */
public class DpheXnCancerTumorSplitter {


   /**
    *
    * @param siteNeoplasms -
    * @param siteHistologies -
    * @return Map of Cancer to Tumors.
    */
   static public Map<ConceptAggregate, Collection<ConceptAggregate>> splitCancerTumors(
         final Map<String,ConceptAggregate> siteNeoplasms,
         final Map<String,String> siteHistologies) {
      if ( siteNeoplasms.isEmpty() ) {
         return new HashMap<>();
      }
      final Map<ConceptAggregate,Collection<ConceptAggregate>> cancerTumors = new HashMap<>();
      if ( siteNeoplasms.size() == 1 ) {
         // create a single cancer with a single tumor.
         final String site = new ArrayList<>( siteNeoplasms.keySet() ).get( 0 );
         final ConceptAggregate neoplasm = siteNeoplasms.get( site );
         cancerTumors.put( neoplasm, Collections.singletonList( neoplasm ) );
         return cancerTumors;
      }

      // For each site, get histology.
      // For each histology, tie together neoplasms of same histology.
      final Map<String,Collection<ConceptAggregate>> histologyNeoplasmsMap = new HashMap<>();
      siteNeoplasms.forEach( (k,v) -> histologyNeoplasmsMap.computeIfAbsent( siteHistologies.get( k ),
                                                                          h -> new HashSet<>() )
                                                        .add( v ) );
      final ConceptAggregate lymphNodeNeoplasm
            = siteNeoplasms.getOrDefault( SiteTable.TOPOGRAPHY_LYMPH_NODE, NULL_AGGREGATE );

      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      final Collection<String> cancerUris = UriConstants.getNeoplasmUris( graphDb );
      final Collection<String> primaryUris = UriConstants.getPrimaryUris( graphDb );

      final Collection<String> secondaryUris = UriConstants.getMetastasisUris( graphDb );
      final Collection<String> massUris = UriConstants.getMassUris( graphDb );

      final Collection<Collection<String>> cancerSearchUris = Arrays.asList( cancerUris, primaryUris );
      final Collection<Collection<String>> tumorSearchUris = Arrays.asList( secondaryUris, massUris );

      for ( Map.Entry<String,Collection<ConceptAggregate>> histologyNeoplasms : histologyNeoplasmsMap.entrySet() ) {
//         final ConceptAggregate cancer = getBestPrimary( histologyNeoplasms.getValue(),
//         final ConceptAggregate cancer = getBestPrimaryBySite( histologyNeoplasms.getValue(),
         final ConceptAggregate cancer = getBestPrimaryBySitePlus( histologyNeoplasms.getValue(),
                                                          lymphNodeNeoplasm,
                                                          cancerSearchUris,
                                                          tumorSearchUris );
         cancerTumors.put( cancer, histologyNeoplasms.getValue() );
      }
      return cancerTumors;
   }

//   static private ConceptAggregate getBestPrimaryBySite( final Collection<ConceptAggregate> neoplasms,
//                                                   final ConceptAggregate lymphNodeNeoplasm,
//                                                   final Collection<Collection<String>> cancerSearchUris,
//                                                   Collection<Collection<String>> tumorSearchUris ) {
//      if ( neoplasms.size() == 1 ) {
//         // There is only one neoplasm.
//         return new ArrayList<>( neoplasms ).get( 0 );
//      }
//      final List<ConceptAggregate> nonLymphNodeNeoplasms = new ArrayList<>( neoplasms );
//      nonLymphNodeNeoplasms.remove( lymphNodeNeoplasm );
//      if ( nonLymphNodeNeoplasms.size() == 1 ) {
//         // There is only one neoplasm that is not on a lymph node.
//         return nonLymphNodeNeoplasms.get( 0 );
//      }
//      final Map<ConceptAggregate, Integer> siteCounts = new HashMap<>();
//      for ( ConceptAggregate neoplasm : nonLymphNodeNeoplasms ) {
//         final int siteCount = neoplasm.getRelatedSites()
//                                       .stream()
//                                       .map( ConceptAggregate::getMentions )
//                                       .mapToInt( Collection::size )
//                                       .sum();
//         siteCounts.put( neoplasm, siteCount );
//      }
//      final ConceptAggregate cancer = getBestCancer(  siteCounts );
//      if ( !cancer.equals( NULL_AGGREGATE ) ) {
//         return cancer;
//      }
//      return getBestPrimary( neoplasms, lymphNodeNeoplasm, cancerSearchUris, tumorSearchUris );
//   }

   static private Map<ConceptAggregate,Integer> getSiteCounts( final Collection<ConceptAggregate> cancers ) {
      final Map<ConceptAggregate, Integer> siteCounts = new HashMap<>();
      for ( ConceptAggregate cancer : cancers ) {
         final int siteCount = cancer.getRelatedSites()
                                       .stream()
                                       .map( ConceptAggregate::getMentions )
                                       .mapToInt( Collection::size )
                                       .sum();
         siteCounts.put( cancer, siteCount );
      }
      return siteCounts;
   }

   static private ConceptAggregate getBestPrimaryBySitePlus( final Collection<ConceptAggregate> neoplasms,
                                                   final ConceptAggregate lymphNodeNeoplasm,
                                                   final Collection<Collection<String>> cancerSearchUris,
                                                   Collection<Collection<String>> tumorSearchUris ) {
      if ( neoplasms.size() == 1 ) {
         // There is only one neoplasm.
         return new ArrayList<>( neoplasms ).get( 0 );
      }
      final List<ConceptAggregate> nonLymphNodeNeoplasms = new ArrayList<>( neoplasms );
      nonLymphNodeNeoplasms.remove( lymphNodeNeoplasm );
      if ( nonLymphNodeNeoplasms.size() == 1 ) {
         // There is only one neoplasm that is not on a lymph node.
         return nonLymphNodeNeoplasms.get( 0 );
      }
      final Map<ConceptAggregate,Integer> cancerCounts = new HashMap<>();
      final Map<ConceptAggregate,Integer> siteCounts = new HashMap<>();
      final Map<String,List<ConceptAggregate>> uriNeoplasms
            = nonLymphNodeNeoplasms.stream().collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
      for ( Collection<String> cancerUris : cancerSearchUris ) {
         final Collection<ConceptAggregate> cancers = getCancers( uriNeoplasms, cancerUris );
         addSiteCounts( cancers, siteCounts, getSiteCounts( cancers ) );
         final ConceptAggregate siteCancer = getBestCancer(  siteCounts );
         if ( !siteCancer.equals( NULL_AGGREGATE ) ) {
            return siteCancer;
         }
         addCancerCounts( cancers, cancerCounts );
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      final List<ConceptAggregate> allNeoplasms = uriNeoplasms.values().stream()
                                                              .flatMap( Collection::stream )
                                                              .collect( Collectors.toList() );
      for ( Collection<String> tumorUris : tumorSearchUris ) {
         final Collection<ConceptAggregate> cancers = getNotTumors( uriNeoplasms, tumorUris, allNeoplasms );
         addCancerCounts( cancers, cancerCounts );
         addSiteCounts( cancers, siteCounts, getSiteCounts( cancers ) );
         final ConceptAggregate siteCancer = getBestCancer(  siteCounts );
         if ( !siteCancer.equals( NULL_AGGREGATE ) ) {
            return siteCancer;
         }
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      // Go through the uri quotients.  Grab the neoplasm that has a cancer uri with the highest quotient.
      for ( Collection<String> cancerUris : cancerSearchUris ) {
         final Collection<ConceptAggregate> cancers = getByQuotientCount( allNeoplasms, cancerUris );
         addSiteCounts( cancers, siteCounts, getSiteCounts( cancers ) );
         final ConceptAggregate siteCancer = getBestCancer(  siteCounts );
         if ( !siteCancer.equals( NULL_AGGREGATE ) ) {
            return siteCancer;
         }
         addCancerCounts( cancers, cancerCounts );
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      // Go through all uris and return the one with the most mention cancer uris.
      for ( Collection<String> cancerUris : cancerSearchUris ) {
         final Collection<ConceptAggregate> cancers = getByMentionCount( allNeoplasms, cancerUris );
         addSiteCounts( cancers, siteCounts, getSiteCounts( cancers ) );
         final ConceptAggregate siteCancer = getBestCancer(  siteCounts );
         if ( !siteCancer.equals( NULL_AGGREGATE ) ) {
            return siteCancer;
         }
         addCancerCounts( cancers, cancerCounts );
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      // Use the site/neoplasm with the most mentions.
      final Collection<ConceptAggregate> cancers = getByAllMentionCount( allNeoplasms );
      addSiteCounts( cancers, siteCounts, getSiteCounts( cancers ) );
      final ConceptAggregate siteCancer = getBestCancer(  siteCounts );
      if ( !siteCancer.equals( NULL_AGGREGATE ) ) {
         return siteCancer;
      }
      addCancerCounts( cancers, cancerCounts );
      final ConceptAggregate cancer = getBestCancer(  cancerCounts );
      if ( !cancer.equals( NULL_AGGREGATE ) ) {
         return cancer;
      }
      return getLastDitchBestCancer( cancerCounts );
   }

   static private ConceptAggregate getBestPrimary( final Collection<ConceptAggregate> neoplasms,
                                          final ConceptAggregate lymphNodeNeoplasm,
                                          final Collection<Collection<String>> cancerSearchUris,
                                          Collection<Collection<String>> tumorSearchUris ) {
      if ( neoplasms.size() == 1 ) {
         // There is only one neoplasm.
         return new ArrayList<>( neoplasms ).get( 0 );
      }
      final List<ConceptAggregate> nonLymphNodeNeoplasms = new ArrayList<>( neoplasms );
      nonLymphNodeNeoplasms.remove( lymphNodeNeoplasm );
      if ( nonLymphNodeNeoplasms.size() == 1 ) {
         // There is only one neoplasm that is not on a lymph node.
         return nonLymphNodeNeoplasms.get( 0 );
      }
      final Map<ConceptAggregate,Integer> cancerCounts = new HashMap<>();
      final Map<String,List<ConceptAggregate>> uriNeoplasms
            = nonLymphNodeNeoplasms.stream().collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
      for ( Collection<String> cancerUris : cancerSearchUris ) {
         final Collection<ConceptAggregate> cancers = getCancers( uriNeoplasms, cancerUris );
         addCancerCounts( cancers, cancerCounts );
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      final List<ConceptAggregate> allNeoplasms = uriNeoplasms.values().stream()
                                                              .flatMap( Collection::stream )
                                                              .collect( Collectors.toList() );
      for ( Collection<String> tumorUris : tumorSearchUris ) {
         final Collection<ConceptAggregate> cancers = getNotTumors( uriNeoplasms, tumorUris, allNeoplasms );
         addCancerCounts( cancers, cancerCounts );
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      // Go through the uri quotients.  Grab the neoplasm that has a cancer uri with the highest quotient.
      for ( Collection<String> cancerUris : cancerSearchUris ) {
         final Collection<ConceptAggregate> cancers = getByQuotientCount( allNeoplasms, cancerUris );
         addCancerCounts( cancers, cancerCounts );
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      // Go through all uris and return the one with the most mention cancer uris.
      for ( Collection<String> cancerUris : cancerSearchUris ) {
         final Collection<ConceptAggregate> cancers = getByMentionCount( allNeoplasms, cancerUris );
         addCancerCounts( cancers, cancerCounts );
         final ConceptAggregate cancer = getBestCancer(  cancerCounts );
         if ( !cancer.equals( NULL_AGGREGATE ) ) {
            return cancer;
         }
      }
      // Use the site/neoplasm with the most mentions.
      final Collection<ConceptAggregate> cancers = getByAllMentionCount( allNeoplasms );
      addCancerCounts( cancers, cancerCounts );
      final ConceptAggregate cancer = getBestCancer(  cancerCounts );
      if ( !cancer.equals( NULL_AGGREGATE ) ) {
         return cancer;
      }
      return getLastDitchBestCancer( cancerCounts );
   }


   static private ConceptAggregate getBestCancer( final Map<ConceptAggregate,Integer> cancerCounts ) {
      if ( cancerCounts.isEmpty() ) {
         return NULL_AGGREGATE;
      }
      if ( cancerCounts.size() == 1 ) {
         return new ArrayList<>( cancerCounts.keySet() ).get( 0 );
      }
      int max = 0;
      final List<ConceptAggregate> maxCancers = new ArrayList<>();
      for ( Map.Entry<ConceptAggregate,Integer> cancerCount : cancerCounts.entrySet() ) {
         final int count = cancerCount.getValue();
         if ( count < max ) {
            continue;
         }
         if ( cancerCount.getValue() == max ) {
            maxCancers.add( cancerCount.getKey() );
            continue;
         }
         max = count;
         maxCancers.clear();
         maxCancers.add( cancerCount.getKey() );
      }
      return maxCancers.size() == 1 ? maxCancers.get( 0 ) : NULL_AGGREGATE;
   }

   static private void addCancerCounts( final Collection<ConceptAggregate> cancers,
                                        final Map<ConceptAggregate,Integer> cancerCounts ) {
      for ( ConceptAggregate cancer : cancers ) {
         final int count = cancerCounts.getOrDefault( cancer, 0 ) + 1;
         cancerCounts.put( cancer, count );
      }
   }

   static private void addSiteCounts( final Collection<ConceptAggregate> cancers,
                                        final Map<ConceptAggregate,Integer> siteCounts,
                                      final Map<ConceptAggregate,Integer> newSiteCounts ) {
      for ( ConceptAggregate cancer : cancers ) {
         final int count = siteCounts.getOrDefault( cancer, 0 )
                           + newSiteCounts.getOrDefault( cancer, 0 );
         siteCounts.put( cancer, count );
      }
   }


   static private Collection<ConceptAggregate> getCancers( final Map<String,List<ConceptAggregate>> uriNeoplasms,
                                             final Collection<String> cancerUris ) {
      return uriNeoplasms.entrySet().stream()
                                                         .filter( e -> cancerUris.contains( e.getKey() ) )
                                                         .map( Map.Entry::getValue )
                                                         .flatMap( Collection::stream )
                                                         .collect( Collectors.toSet() );
   }

   static private Collection<ConceptAggregate> getNotTumors( final Map<String,List<ConceptAggregate>> uriNeoplasms,
                                             final Collection<String> tumorUris,
                                                   final Collection<ConceptAggregate> allNeoplasms ) {
      final Collection<ConceptAggregate> tumors = uriNeoplasms.entrySet().stream()
                                                         .filter( e -> tumorUris.contains( e.getKey() ) )
                                                         .map( Map.Entry::getValue )
                                                         .flatMap( Collection::stream )
                                                         .collect( Collectors.toSet() );
      allNeoplasms.removeAll( tumors );
      return allNeoplasms;
   }

   static private Collection<ConceptAggregate> getByMentionCount( final List<ConceptAggregate> allNeoplasms,
                                                      final Collection<String> cancerUris ) {
      final Map<Integer,List<ConceptAggregate>> countedCancers = new HashMap<>();
      for ( ConceptAggregate neoplasm : allNeoplasms ) {
         final Map<String,List<Mention>> uriMentions = neoplasm.getUriMentions();
         final int cancerCount = uriMentions.entrySet().stream()
                                            .filter( e -> cancerUris.contains( e.getKey() ) )
                                            .mapToInt( e -> e.getValue().size() )
                                            .sum();
         countedCancers.computeIfAbsent( cancerCount, n -> new ArrayList<>() ).add( neoplasm );
      }
      if ( countedCancers.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Integer> counts = new ArrayList<>( countedCancers.keySet() );
      Collections.sort( counts );
      return countedCancers.get( counts.get( counts.size()-1 ) );
   }

   static private Collection<ConceptAggregate> getByQuotientCount( final List<ConceptAggregate> allNeoplasms,
                                                      final Collection<String> cancerUris ) {
      final Map<Double,List<ConceptAggregate>> countedCancers = new HashMap<>();
      for ( ConceptAggregate neoplasm : allNeoplasms ) {
         final List<KeyValue<String, Double>> uriQuotients = neoplasm.getUriQuotients();
         final double cancerCount = uriQuotients.stream()
                                                .filter( kv -> cancerUris.contains( kv.getKey() ) )
                                                .mapToDouble( KeyValue::getValue )
                                                .sum();
         countedCancers.computeIfAbsent( cancerCount, n -> new ArrayList<>() ).add( neoplasm );
      }
      if ( countedCancers.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Double> counts = new ArrayList<>( countedCancers.keySet() );
      Collections.sort( counts );
      return countedCancers.get( counts.get( counts.size()-1 ) );
   }

   /**
    * This is a last ditch effort.  At this point we need to return something.
    * @param allNeoplasms -
    * @return -
    */
   static private Collection<ConceptAggregate> getByAllMentionCount( final List<ConceptAggregate> allNeoplasms ) {
      final Map<Integer,List<ConceptAggregate>> mentionCountNeoplasms
            = allNeoplasms.stream().collect( Collectors.groupingBy( n -> n.getMentions().size() ) );
      if ( mentionCountNeoplasms.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Integer> counts = new ArrayList<>( mentionCountNeoplasms.keySet() );
      Collections.sort( counts );
      return mentionCountNeoplasms.get( counts.get( counts.size()-1 ) );
   }

   static private ConceptAggregate getLastDitchBestCancer( final Map<ConceptAggregate,Integer> cancerCounts ) {
      if ( cancerCounts.isEmpty() ) {
         // By the time we get here this should never happen.  There should at least be cancers with count == 0;
         return NULL_AGGREGATE;
      }
      if ( cancerCounts.size() == 1 ) {
         return new ArrayList<>( cancerCounts.keySet() ).get( 0 );
      }
      int max = 0;
      final List<ConceptAggregate> maxCancers = new ArrayList<>();
      for ( Map.Entry<ConceptAggregate,Integer> cancerCount : cancerCounts.entrySet() ) {
         final int count = cancerCount.getValue();
         if ( count < max ) {
            continue;
         }
         if ( cancerCount.getValue() == max ) {
            maxCancers.add( cancerCount.getKey() );
            continue;
         }
         max = count;
         maxCancers.clear();
         maxCancers.add( cancerCount.getKey() );
      }
      return maxCancers.get( 0 );
   }


}
