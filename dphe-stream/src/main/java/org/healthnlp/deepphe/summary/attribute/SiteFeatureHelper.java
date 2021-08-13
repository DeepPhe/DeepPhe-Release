//package org.healthnlp.deepphe.summary.attribute;
//
//import org.healthnlp.deepphe.core.uri.UriUtil;
//import org.healthnlp.deepphe.neo4j.constant.UriConstants;
//import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.healthnlp.deepphe.util.KeyValue;
//import org.healthnlp.deepphe.util.UriScoreUtil;
//import org.neo4j.graphdb.GraphDatabaseService;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
//
//public class SiteFeatureHelper implements FeatureHelper {
//
//   static private final List<String> SITE_RELATIONS = Arrays.asList( DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
//                                                                     DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
//                                                                     Disease_Has_Associated_Region,
//                                                                     Disease_Has_Associated_Cavity,
//                                                                     DISEASE_HAS_METASTATIC_ANATOMIC_SITE );
//
//   private final ConceptAggregate _neoplasm;
//   private final Collection<ConceptAggregate> _allConcepts;
//   private final String _bestSiteUri;
//   private final Collection<String> _allBestSiteUris;
//   private final List<Integer> _sitesOrders = new ArrayList<>();
//   private final Map<Integer, Collection<ConceptAggregate>> _orderedSiteMap = new HashMap<>();
//   private final Collection<ConceptAggregate> _patientSites = new HashSet<>();
//   final Map<String,Collection<String>> _patientSitesRootsMap;
//
//   public SiteFeatureHelper( final ConceptAggregate neoplasm, final Collection<ConceptAggregate> allConcepts ) {
//      _neoplasm = neoplasm;
//      _allConcepts = allConcepts;
//      sortNeoplasmSites( neoplasm );
//      collectPatientSites( allConcepts );
//      _patientSitesRootsMap = UriUtil.mapUriRoots( getPatientUris() );
//
//      final Collection<String> firstSiteUris = getFirstUris();
//      final Map<String,Collection<String>> firstSitesRootsMap = mapAllUriRoots( getFirstSites() );
//
//      final Map<Boolean,Collection<Mention>> evidence = FeatureHelper.mapEvidence( firstSiteUris, getNeoplasmSites() );
//      final List<Mention> directEvidence
//            = new ArrayList<>( evidence.get( SpecificAttribute.DIRECT_EVIDENCE ) );
//      final Map<String,Integer> uriCountsMap = UriScoreUtil.mapUriMentionCounts( firstSiteUris, directEvidence );
//      final Map<String,Integer> uriSums = UriScoreUtil.mapUriSums( firstSiteUris, firstSitesRootsMap, uriCountsMap );
//      final int totalCounts = uriCountsMap.values().stream().mapToInt( i -> i ).sum();
//      final Map<String,Double> uriQuotientMap = UriScoreUtil.mapUriQuotientsBB( uriSums, totalCounts );
//      final List<KeyValue<String,Double>> uriQuotientList = UriScoreUtil.listUriQuotients( uriQuotientMap );
//      final List<KeyValue<String,Double>> bestKeyValues = UriScoreUtil.getBestUriScores( uriQuotientList );
//      final Map<String,Integer> classLevelMap = UriScoreUtil.createClassLevelMap( bestKeyValues );
////
//      _bestSiteUri = UriScoreUtil.getBestUriScore( bestKeyValues, classLevelMap, firstSitesRootsMap ).getKey();
//      _allBestSiteUris = getHasBestUriSites( getFirstSites() ).stream()
//                                                              .map( ConceptAggregate::getAllUris )
//                                                              .flatMap( Collection::stream )
//                                                              .collect( Collectors.toSet() );
//   }
//
//   public String getBestUri() {
//      return _bestSiteUri;
//   }
//
//   public Collection<String> getAllBestAssociatedUris() {
//      return _allBestSiteUris;
//   }
//
//   public Map<String,Collection<String>> getPatientUriRootsMap() {
//      return _patientSitesRootsMap;
//   }
//
//   public ConceptAggregate getNeoplasm() {
//      return _neoplasm;
//   }
//
//   public Collection<ConceptAggregate> getAllPatientConcepts() {
//      return _allConcepts;
//   }
//
//   private void sortNeoplasmSites( final ConceptAggregate neoplasm ) {
//      int order = 1;
//      for ( String relation : SITE_RELATIONS ) {
//         final Collection<ConceptAggregate> sites = neoplasm.getRelated( relation );
//         if ( !sites.isEmpty() ) {
//            _sitesOrders.add( order );
//            _orderedSiteMap.put( order, sites );
//         }
//         order++;
//      }
//   }
//
//   private void collectPatientSites( final Collection<ConceptAggregate> allConcepts ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      final Collection<String> siteUris = UriConstants.getLocationUris( graphDb );
//      _patientSites.addAll( allConcepts.stream()
//                                       .filter( c -> siteUris.contains( c.getUri() ) )
//                                       .collect( Collectors.toSet() ) );
//   }
//
//   public List<Integer> getSiteRelationOrders() {
//      return _sitesOrders;
//   }
//
//   public Collection<ConceptAggregate> getFirstSites() {
//      if ( _sitesOrders.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      return _orderedSiteMap.get( _sitesOrders.get( 0 ) );
//   }
//
//   public Collection<ConceptAggregate> getNeoplasmSites() {
//      return _orderedSiteMap.values()
//                            .stream()
//                            .flatMap( Collection::stream )
//                            .collect( Collectors.toSet() );
//   }
//
//   public Collection<Integer> getOrdersOccupied( final Collection<String> uris ) {
//      if ( _orderedSiteMap.size() == 1 ) {
//         return _orderedSiteMap.keySet();
//      }
//      final Collection<Integer> orders = new HashSet<>();
//      for ( Map.Entry<Integer,Collection<ConceptAggregate>> orderedSites : _orderedSiteMap.entrySet() ) {
//         if ( orderedSites.getValue()
//                          .stream()
//                          .map( ConceptAggregate::getAllUris )
//                          .flatMap( Collection::stream )
//                          .anyMatch( uris::contains ) ) {
//            orders.add( orderedSites.getKey() );
//            if ( orders.size() == _orderedSiteMap.size() ) {
//               return orders;
//            }
//         }
//      }
//      return orders;
//   }
//
//   public Collection<ConceptAggregate> getPatientSites() {
//      return _patientSites;
//   }
//
//   public Collection<String> getFirstUris() {
//      return getAllUris( getFirstSites() );
//   }
//   public Collection<String> getNeoplasmUris() {
//      return getAllUris( getNeoplasmSites() );
//   }
//   public Collection<String> getPatientUris() {
//      return getAllUris( getPatientSites() );
//   }
//   public Collection<Mention> getFirstMentions() {
//      return getAllMentions( getFirstSites() );
//   }
//   public Collection<Mention> getNeoplasmMentions() {
//      return getAllMentions( getNeoplasmSites() );
//   }
//   public Collection<Mention> getPatientMentions() {
//      return getAllMentions( getPatientSites() );
//   }
//
//   public Collection<ConceptAggregate> getHasBestUriSites( final Collection<ConceptAggregate> concepts ) {
//      return getHasBestUriConcepts( concepts );
//   }
//   public Collection<ConceptAggregate> getExactBestUriSites( final Collection<ConceptAggregate> concepts ) {
//      return getExactBestUriConcepts( concepts );
//   }
//
////      private Map<String,Collection<ConceptAggregate>> mapUriConcepts( final Collection<ConceptAggregate> concepts ) {
////         final Map<String,Collection<ConceptAggregate>> uriConceptMap = new HashMap<>();
////         for ( ConceptAggregate concept : concepts ) {
////            concept.getAllUris()
////                   .forEach( u -> uriConceptMap.computeIfAbsent( u, c -> new HashSet<>() )
////                                               .add( concept ) );
////         }
////         return uriConceptMap;
////      }
//
//
//
//
//
//}
