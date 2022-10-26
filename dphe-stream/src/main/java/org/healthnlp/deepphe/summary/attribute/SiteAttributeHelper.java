package org.healthnlp.deepphe.summary.attribute;

import org.apache.ctakes.core.util.Pair;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.TopoMorphValidator;
import org.healthnlp.deepphe.util.UriScoreUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

public class SiteAttributeHelper extends AbstractAttributeHelper {

   static private final Logger LOGGER = Logger.getLogger( "SiteAttributeHelper" );

   static private final List<String> SITE_RELATIONS = Arrays.asList( DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                                                                     DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                                                                     Disease_Has_Associated_Region,
                                                                     Disease_Has_Associated_Cavity,
                                                                     DISEASE_HAS_METASTATIC_ANATOMIC_SITE );



   private final List<Integer> _sitesOrders = new ArrayList<>();
   private final Map<Integer, Collection<ConceptAggregate>> _orderedSiteMap = new HashMap<>();
   private final Map<Integer, Pair<KeyValue<String,Double>>> _orderedScoreMap = new HashMap<>();


         // v-- Should do by all URIs in winner concept.  So Left_Breast wins 1 and Breast wins 2, Left_Breast gets
   // score of Breast.
   // winner score order 1 // winner - runner-up score order 1 //   So, .4 from level 1.  At level 2 still .4, but
   // other has .5 == -0.1
   //  " order 2 // " order 2 //    ... through order 5.
//-->  If not in order/level/layer, use zero.  Runner-up can be greater or less than winner, or zero if none
   // # ln( mentions+1 ), max 10.  winner  //  winner - runner-up  ln( mentions+1 ), max 10 //
   //  winner mentions / total patient mentions
   // found in ontology
   // found in validator table
   // ontology matches validator (-1 == no, 0 == N/A (one or other didn't exist), 1 == match)



   /**
    *
    * @param neoplasm -
    * @param allConcepts -
    */
   public SiteAttributeHelper( final ConceptAggregate neoplasm, final Collection<ConceptAggregate> allConcepts ) {
      super( neoplasm, allConcepts );

      // Create Map of relation levels ( Site Relations ) and the site concepts at those levels.
      sortNeoplasmSites( neoplasm );
      setAllNeoplasmConceptsForType( collectNeoplasmSites() );
      // Collect all of the site concepts for the patient.
      setAllPatientConceptsForType( collectPatientSites( allConcepts ) );

      final Collection<Mention> allNeoplasmMentions = getAllNeoplasmMentionsForType();
//      final Map<Integer,KeyValue<String,Double>> orderedBestScores =
//            _orderedSiteMap.entrySet()
//                           .stream()
//                           .collect( Collectors.toMap( Map.Entry::getKey,
//                                                       e -> getBestUriScore( e.getValue(), allNeoplasmMentions ) ) );
      final Map<Integer,Pair<KeyValue<String,Double>>> orderedBestScores =
            _orderedSiteMap.entrySet()
                           .stream()
                           .collect( Collectors.toMap( Map.Entry::getKey,
                                                       e -> getTwoBestUriScores( e.getValue(), allNeoplasmMentions ) ) );
      _orderedScoreMap.putAll( orderedBestScores );
//      LOGGER.info( "!!!!!!!!!! Ordered Best Scores: " );
      String bestSiteUri = "";
      for ( Integer order : _sitesOrders ) {
//         LOGGER.info( order + " "
//                      + orderedBestScores.get( order ).getValue1().getKey()
//                      + " = " + orderedBestScores.get( order ).getValue1().getValue()
//                      + " , "
//                        + orderedBestScores.get( order ).getValue2().getKey()
//                      + " = " + orderedBestScores.get( order ).getValue2().getValue() );
         if ( bestSiteUri.isEmpty() ) {
            bestSiteUri = orderedBestScores.get( order ).getValue1().getKey();
         }
      }
      setBestUriForType( bestSiteUri );
      final Collection<String> uriRoots = getAllPatientUriRootsForType().get( bestSiteUri );
      uriRoots.add( bestSiteUri );
      final Predicate<ConceptAggregate> uriOverlap = c -> c.getAllUris().stream().anyMatch( uriRoots::contains );
      final Collection<ConceptAggregate> bestConcepts
            = getAllNeoplasmConceptsForType().stream()
                                             .filter( uriOverlap )
                                             .collect( Collectors.toSet() );
      setBestConceptsForType( bestConcepts );

      final Collection<String> ontoTopoCodes = getOntoTopoCodes( getAllBestUrisForType() );
      final Collection<String> tableTopoCodes = getTableTopoCodes( getAllBestUrisForType() );
      final Collection<String> allTopoCodes = new HashSet<>( ontoTopoCodes );
      allTopoCodes.addAll( tableTopoCodes );
      setPossibleIcdoCodes( allTopoCodes );
      setBestIcdoCode( getMajorTopoCode( allTopoCodes ) );
   }


   public List<Integer>  createFeatures() {
      final List<Integer> features = new ArrayList<>();


      return features;
   }


   public List<Integer> createEmptyFeatures() {
      final Integer[] features = new Integer[ 55 ];
      Arrays.fill( features, -1 );
      return Arrays.asList( features );
   }


   protected Map<Integer, Pair<KeyValue<String,Double>>> getOrderedScoreMap() {
      return _orderedScoreMap;
   }


//   private KeyValue<String,Double> getBestUriScore( final Collection<ConceptAggregate> orderConcepts,
//                                                  final Collection<Mention> allNeoplasmMentions ) {
//      if ( orderConcepts.size() == 1 ) {
//         final ConceptAggregate concept = new ArrayList<>( orderConcepts ).get( 0 );
//         return new KeyValue<>( concept.getUri(), 1.0 );
//      }
//      final Collection<String> orderUris = getAllUris( orderConcepts );
//      final Map<String,Integer> uriCountsMap = UriScoreUtil.mapUriMentionCounts( orderUris, allNeoplasmMentions );
//      final int totalCounts = uriCountsMap.values()
//                                          .stream()
//                                          .mapToInt( i -> i )
//                                          .sum();
//      final Map<String,Collection<String>> uriRootsMap = mapAllUriRoots( orderUris );
//      final Map<String,Integer> uriSums = UriScoreUtil.mapUriSums( orderUris, uriRootsMap, uriCountsMap );
//      final Map<String,Double> uriQuotientMap = UriScoreUtil.mapUriQuotientsBB( uriSums, totalCounts );
//      final List<KeyValue<String,Double>> uriQuotientList = UriScoreUtil.listUriQuotients( uriQuotientMap );
//      final List<KeyValue<String,Double>> bestKeyValues = UriScoreUtil.getBestUriScores( uriQuotientList );
//      final Map<String,Integer> classLevelMap = UriScoreUtil.createClassLevelMap( bestKeyValues );
//
//      LOGGER.info( "!!!    SiteAttributeHelper.getBestUriScore");
//      uriQuotientList.stream().map( kv -> "URI " + kv.getKey()
//                                          + "   mention count " + uriCountsMap.get( kv.getKey() )
//                                          + "   quotient score " + kv.getValue()
//                                          + "   root count "
//                                          + ( uriRootsMap.get( kv.getKey() ) == null
//                                              ? "-1"
//                                              : uriRootsMap.get( kv.getKey() ).size() )
//                                          + "   uri Sum "
//                                          + ( uriSums.get( kv.getKey() ) == null
//                                              ? "-1"
//                                              : uriSums.get( kv.getKey() ) )
//                                          + "   quotient level score "
//                                          + ( classLevelMap.get( kv.getKey() ) == null
//                                              ? "-1"
//                                              : (kv.getValue()*classLevelMap.get( kv.getKey() )) )
//                                          + "   rooted "
//                                          + ( classLevelMap.get( kv.getKey() ) == null || uriRootsMap.get( kv.getKey() ) == null
//                                                ? "-1"
//                                                : (kv.getValue()
//                                                     *classLevelMap.get( kv.getKey() )
//                                                     *uriRootsMap.get( kv.getKey() ).size()) )
//                                          + "   class level " + classLevelMap.get( kv.getKey() )
//                                  )
//                     .forEach( LOGGER::info );
//
//      return UriScoreUtil.getBestUriScore( bestKeyValues, classLevelMap, uriRootsMap );
//   }

   private Pair<KeyValue<String,Double>> getTwoBestUriScores( final Collection<ConceptAggregate> orderConcepts,
                                                              final Collection<Mention> allNeoplasmMentions ) {
      if ( orderConcepts.size() == 1 ) {
         final ConceptAggregate concept = new ArrayList<>( orderConcepts ).get( 0 );
         return new Pair<>( new KeyValue<>( concept.getUri(), 1.0 ), new KeyValue<>( "DeepPhe", 0d ) );
      }
      final Collection<String> orderUris = getAllUris( orderConcepts );
      final Map<String,Integer> uriCountsMap = UriScoreUtil.mapUriMentionCounts( allNeoplasmMentions );
      final int totalCounts = uriCountsMap.values()
                                          .stream()
                                          .mapToInt( i -> i )
                                          .sum();
      final Map<String,Collection<String>> uriRootsMap = mapAllUriRoots( orderUris );
      final Map<String,Integer> uriSums = UriScoreUtil.mapUriSums( orderUris, uriRootsMap, uriCountsMap );
      final Map<String,Double> uriQuotientMap = UriScoreUtil.mapUriQuotientsBB( uriSums, totalCounts );
      final List<KeyValue<String,Double>> uriQuotientList = UriScoreUtil.listUriQuotients( uriQuotientMap );
      final List<KeyValue<String,Double>> bestKeyValues = UriScoreUtil.getBestUriScores( uriQuotientList );
      final Map<String,Integer> classLevelMap = UriScoreUtil.createClassLevelMap( bestKeyValues );

//      LOGGER.info( "!!!    SiteAttributeHelper.getTwoBestUriScores");
//      uriQuotientList.stream().map( kv -> "URI " + kv.getKey()
//                                          + "   mention count " + uriCountsMap.get( kv.getKey() )
//                                          + "   quotient score " + kv.getValue()
//                                          + "   root count "
//                                          + ( uriRootsMap.get( kv.getKey() ) == null
//                                              ? "-1"
//                                              : uriRootsMap.get( kv.getKey() ).size() )
//                                          + "   uri Sum "
//                                          + ( uriSums.get( kv.getKey() ) == null
//                                              ? "-1"
//                                              : uriSums.get( kv.getKey() ) )
//                                          + "   quotient level score "
//                                          + ( classLevelMap.get( kv.getKey() ) == null
//                                              ? "-1"
//                                              : (kv.getValue()*classLevelMap.get( kv.getKey() )) )
//                                          + "   rooted "
//                                          + ( classLevelMap.get( kv.getKey() ) == null || uriRootsMap.get( kv.getKey() ) == null
//                                              ? "-1"
//                                              : (kv.getValue()
//                                                 *classLevelMap.get( kv.getKey() )
//                                                 *uriRootsMap.get( kv.getKey() ).size()) )
//                                          + "   class level " + classLevelMap.get( kv.getKey() )
//                                  )
//                     .forEach( LOGGER::info );

      return getTwoBestUriScores( bestKeyValues, classLevelMap, uriRootsMap );
   }


   static public Pair<KeyValue<String,Double>> getTwoBestUriScores( final List<KeyValue<String,Double>> bestKeyValues,
                                                                      final Map<String,Integer> classLevelMap,
                                                                      final Map<String,Collection<String>> uriRootsMap ) {
//      LOGGER.info( "The best URI is the one with the highest quotient score and the highest class level " +
//                   "(furthest from root by the shortest path) with ties broken by total number of nodes (all routes) to root.\n" +
//                   "This is all about high representation and high precision.\n" +
//                   "The highest quotient is a measure of fully and exactly representing the most mentions.\n" +
//                   "The class level is a measure of specificity - the furthest the shortest path is from root the more specific the concept.\n" +
//                   "Breaking a tie with the most nodes between a concept and root is sort of a measure of both\n" +
//                   "specificity and high representation, but a much less exact measure of each." );
      final ToIntFunction<KeyValue<String,Double>> getClassLevel = kv -> classLevelMap.get( kv.getKey() );
      final ToIntFunction<KeyValue<String,Double>> getRootCount = kv -> uriRootsMap.get( kv.getKey() ).size();
      final KeyValue<String,Double> topScore =
            bestKeyValues.stream()
                          .max( Comparator.comparingInt( getClassLevel ).thenComparingInt( getRootCount ) )
                          .orElse( bestKeyValues.get( bestKeyValues.size()-1 ) );
      final String topUri = topScore.getKey();
      final Collection<String> topRoots = uriRootsMap.get( topUri );
      KeyValue<String,Double> runnerUp = new KeyValue<>( "DeepPhe", 0d );
      for ( KeyValue<String,Double> keyValue : bestKeyValues ) {
         final String uri = keyValue.getKey();
         if ( uri.equals( topUri ) || topRoots.contains( uri ) ) {
            continue;
         }
         if ( keyValue.getValue() > runnerUp.getValue() ) {
            runnerUp = keyValue;
         }
      }
      return new Pair<>( topScore, runnerUp );
   }



   static private Collection<String> getOntoTopoCodes( final Collection<String> uris ) {
      return uris.stream()
                 .map( Neo4jOntologyConceptUtil::getIcdoTopoCode )
                 .filter( t -> !t.isEmpty() )
                 .collect( Collectors.toSet() );
   }

   static private Collection<String> getTableTopoCodes( final Collection<String> uris ) {
      return uris.stream()
                 .map( TopoMorphValidator.getInstance()::getSiteCode )
                 .filter( t -> !t.isEmpty() )
                 .collect( Collectors.toSet() );
   }

   static private String getMajorTopoCode( final Collection<String> topographyCodes ) {
      if ( topographyCodes.isEmpty() ) {
         return "C80";
      }
      final Function<String, String> getMajorCode = t -> {
         final int dot = t.indexOf( '.' );
         return dot > 0 ? t.substring( 0, dot ) : t;
      };
      final List<String> codes = topographyCodes.stream()
                                                .map( getMajorCode )
                                                .distinct()
                                                .sorted()
                                                .collect( Collectors.toList() );
      return codes.get( codes.size()-1 );
   }



//   public Collection<String> getFirstOrderUris() {
//      return getAllUris( getFirstOrderSites() );
//   }
//
//   public Collection<ConceptAggregate> getFirstOrderSites() {
//      if ( _sitesOrders.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      return _orderedSiteMap.get( _sitesOrders.get( 0 ) );
//   }



   private void sortNeoplasmSites( final ConceptAggregate neoplasm ) {
      int order = 1;
      for ( String relation : SITE_RELATIONS ) {
         final Collection<ConceptAggregate> sites = neoplasm.getRelated( relation );
         if ( !sites.isEmpty() ) {
            _sitesOrders.add( order );
            _orderedSiteMap.put( order, sites );
         }
         order++;
      }
   }

   private Collection<ConceptAggregate> collectNeoplasmSites() {
      return _orderedSiteMap.values()
                            .stream()
                            .flatMap( Collection::stream )
                            .collect( Collectors.toSet() );
   }

   private Collection<ConceptAggregate> collectPatientSites( final Collection<ConceptAggregate> allConcepts ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> siteUris = UriConstants.getLocationUris( graphDb );
      return allConcepts.stream()
                         .filter( c -> siteUris.contains( c.getUri() ) )
                         .collect( Collectors.toSet() );
   }




}
