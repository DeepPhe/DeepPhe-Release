//package org.healthnlp.deepphe.summary.neoplasm;
//
//import org.apache.log4j.Logger;
//import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
//import org.healthnlp.deepphe.neo4j.constant.UriConstants;
//import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
//import org.healthnlp.deepphe.summary.attribute.histology.Histology;
//import org.healthnlp.deepphe.summary.attribute.histology.HistologyCodeInfoStore;
//import org.healthnlp.deepphe.summary.attribute.histology.HistologyUriInfoVisitor;
//import org.healthnlp.deepphe.summary.attribute.topography.Topography;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregateHandler;
//import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;
//import org.neo4j.graphdb.GraphDatabaseService;
//
//import java.util.*;
//import java.util.function.Predicate;
//import java.util.function.ToIntFunction;
//import java.util.stream.Collectors;
//
//import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.DISEASE_MAY_HAVE_FINDING;
//
//
///**
// * @author SPF , chip-nlp
// * @since {6/9/2022}
// */
//final public class ByAttributeMerger {
//
//   private ByAttributeMerger() {}
//
//   static private final Logger LOGGER = Logger.getLogger( "ByAttributeMerger" );
//
//
//   static private final double TOPOGRAPHY_CUTOFF = 0.20;
//   static private final String TOPOGRAPHY_UNDETERMINED = "C80";
//   static private final Collection<String> TOPOGRAPHY_UNDETERMINED_AND_LYMPH = Arrays.asList( "C80", "C77" );
//
//   static private final double LOW_HISTOLOGY_CUTOFF = 0.30;
//   static private final double HIGH_HISTOLOGY_CUTOFF = 0.50;
//   // 8010 is all carcinoma nos. /0 Benign (epithelioma), /2 In Situ and /3 Malignant (DNE)
//   // 800* might be unknown.
//   // We only want to use the first 3 digits of the histology.  Those are the "Major" Histologic Types.
//   static private final String CARCINOMA_NOS = "801";
//   static private final Collection<String> CARCINOMA_UNKNOWNS = Arrays.asList( "800", "801" );
//   static private final Collection<String> KEEP_CARCINOMA_NOS = Collections.emptyList();
//
//
//   static private final double LATERALITY_CUTOFF = 0.30;
//   static private final Collection<String> LATERALITY_NONE = Arrays.asList( "0", "9" );
//
//   static private final int BEST_MAX_CANCERS = 2;
//   static private final int BEST_MAX_TUMORS = 3;
//
//   // For "relocated" cancer attributes, change the relation type.
//   // We want to keep some indication of a relation to discovered site information,
//   // but we no longer want it to be used for site calculations.
//   static private final String ADJUSTED_SITE_RELATION = DISEASE_MAY_HAVE_FINDING;
//
//
//   static private final Map<String,Collection<String>> TOPOGRAPHY_GROUPS = new HashMap<>();
//   static {
//      // Mouth
//      TOPOGRAPHY_GROUPS.put( "C00", Arrays.asList( "C00", "C01", "C03", "C07" ) );
//      // Pharynx
//      TOPOGRAPHY_GROUPS.put( "C14", Arrays.asList( "C09", "C10", "C11", "C12", "C13", "C14" ) );
//      // Stomach and Small Intestine - 26 is digestive organ nos
//      TOPOGRAPHY_GROUPS.put( "C16", Arrays.asList( "C15", "C16", "C17", "C26" ) );
//      // Colon
//      TOPOGRAPHY_GROUPS.put( "C18", Arrays.asList( "C18", "C19", "C20", "C21" ) );
//      // Larynx, Trachea
//      TOPOGRAPHY_GROUPS.put( "C32", Arrays.asList( "C32", "C33" ) );
//      // Lung, Respiratory System
//      TOPOGRAPHY_GROUPS.put( "C34", Arrays.asList( "C34", "C39" ) );
////      // Female Reproduction, Pelvis Bladder and Genital NOS.  C56 is Ovary
////      TOPOGRAPHY_GROUPS.put( "C56", Arrays.asList( "C51", "C52", "C53", "C54", "C55", "C56", "C57",
////                                            "C65", "C66", "C67","C68" ) );
////      // Male Reproduction, Pelvis Bladder and Genital NOS.  C61 is Prostate
////      TOPOGRAPHY_GROUPS.put( "C61", Arrays.asList( "C60", "C61", "C62", "C63",
////                                            "C65", "C66", "C67","C68"  ) );
//      // Female, Male, and overall genitals.
//      TOPOGRAPHY_GROUPS.put( "C68", Arrays.asList( "C51", "C52", "C53", "C54", "C55", "C56", "C57",
//                                                   "C60", "C61", "C62", "C63",
//                                                   "C65", "C66", "C67","C68" ) );
//      // Undetermined.
//      TOPOGRAPHY_GROUPS.put( "C80", Arrays.asList( "C80", "C76" ) );
//   }
//
//   static private String getSiteGroup( final String bestCode ) {
//      final String code = bestCode.substring( 0, 3 );
//      return TOPOGRAPHY_GROUPS.entrySet().stream()
//                             .filter( e -> e.getValue().contains( code ) )
//                             .map( Map.Entry::getKey )
//                              .findFirst()
//                              .orElse( code );
//   }
//
//
//   static public Collection<ConceptAggregate> mergeCancers( final String patientId,
//                                                             final Collection<ConceptAggregate> neoplasms,
//                                                             final Collection<ConceptAggregate> allConcepts ) {
//      final NeoplasmCasino neoplasmArrangement = new NeoplasmCasino( neoplasms, allConcepts );
//      // Need to distribute relations from these neoplasms to neoplasms at best sites ...
//      relateUndeterminedSiteNos( patientId, neoplasmArrangement._unwantedNeoplasms,
//                                 neoplasmArrangement._bestSiteNeoplasmsMap, allConcepts );
//
//      // Get top histology per site.
//      final Map<String,String> allSiteBestHistologies
//            = collectSiteHistologies( neoplasmArrangement._allSiteNeoplasmsMap,
//                                      neoplasmArrangement._allHistologyNeoplasmsMap,
//                                      neoplasmArrangement._bestHistologyNeoplasmsMap.keySet(), allConcepts );
//
//
//      final Collection<ConceptAggregate> neoplasmsNotOnBestSites
//            = neoplasmArrangement._unwantedSites.stream()
//                                                .map( neoplasmArrangement._allSiteNeoplasmsMap::get )
//                                                .flatMap( Collection::stream )
//                                                .collect( Collectors.toSet() );
//
//      //  TODO Need to "get rid of" neoplasms on unwanted sites.
//      //  If they have one of the "best" histologies,
//      //  create a new merged neoplasm and copy it to all of the "best" sites that have that histology.
//      //  Relations for same histologies have already been copied.
//      // That way the Mentions will be preserved.
//      // If they have a crap histology OR carcinoma_NOS,
//      // put them in a merged "nos" neoplasm and copy it into a "best" site at "undetermined".
//      // Copy non-site relations into all neoplasm concepts?
//      // Hopefully undetermined never becomes something ridiculous.
//
//
//      final Map<String,Collection<ConceptAggregate>> bestHistologyUnwantedSites = new HashMap<>();
//      for ( Map.Entry<String,Collection<ConceptAggregate>> bestHistologyNeoplasms :
//            bestHistologyNeoplasmsMap.entrySet() ) {
//         final Collection<ConceptAggregate> histologyNeoplasms = new HashSet<>( bestHistologyNeoplasms.getValue() );
//         histologyNeoplasms.retainAll( neoplasmsNotOnBestSites );
//         if ( !histologyNeoplasms.isEmpty() ) {
//            bestHistologyUnwantedSites.put( bestHistologyNeoplasms.getKey(), histologyNeoplasms );
//         }
//      }
//
//      final Collection<ConceptAggregate> carcinomaNosNeoplasms
//            = CARCINOMA_UNKNOWNS.stream()
//                                .map( h -> bestHistologyNeoplasmsMap.getOrDefault( h, Collections.emptyList() ) )
//                                .flatMap( Collection::stream )
//                                .collect( Collectors.toSet() );
//
//      final Collection<ConceptAggregate> undeterminedNosSiteNeoplasms
//            = new HashSet<>( bestSiteNeoplasmsMap.getOrDefault( TOPOGRAPHY_UNDETERMINED,
//                                                                      Collections.emptyList() ) );
//      undeterminedNosSiteNeoplasms.retainAll( carcinomaNosNeoplasms );
//
////      final Map<String,ConceptAggregate> siteMergedNeoplasms = new HashMap<>( bestSiteNeoplasmsMap.size() );
////      final Collection<ConceptAggregate> usedNeoplasms = new HashSet<>();
//      for ( Map.Entry<String,Collection<ConceptAggregate>> bestSiteNeoplasms
//            : bestSiteNeoplasmsMap.entrySet() ) {
//         final String histology = allSiteBestHistologies.get( bestSiteNeoplasms.getKey() );
//         final Collection<ConceptAggregate> unwantedSiteNeoplasms
//               = bestHistologyUnwantedSites.getOrDefault( histology, Collections.emptyList() );
//         final Collection<ConceptAggregate> otherBestSiteNeoplasms =
//               new HashSet<>( bestHistologyNeoplasmsMap.get( histology ) );
//         otherBestSiteNeoplasms.removeAll( bestSiteNeoplasms.getValue() );
//         otherBestSiteNeoplasms.removeAll( unwantedSiteNeoplasms );
//
////         final ConceptAggregate newNeoplasm = createMergedNeoplasm( patientId,
////                                                                    bestSiteNeoplasms.getValue(),
////                                                                    unwantedSiteNeoplasms,
////                                                                    allConcepts );
////         siteMergedNeoplasms.put( bestSiteNeoplasms.getKey(), newNeoplasm );
////         usedNeoplasms.addAll( bestSiteNeoplasms.getValue() );
//         createMergedNeoplasm( patientId, bestSiteNeoplasms.getValue(),
//                               otherBestSiteNeoplasms,
//                               unwantedSiteNeoplasms,
//                               undeterminedNosSiteNeoplasms,
//                               allConcepts );
//      }
//
////      final Map<String,Collection<String>> histologyBestTopographiesMap = new HashMap<>();
////      allSiteBestHistologies.entrySet().stream()
////                                  .filter( e -> siteMergedNeoplasms.containsKey( e.getKey() ) )
////                                  .forEach( e -> histologyBestTopographiesMap.computeIfAbsent( e.getValue(),
////                                                                                               t -> new HashSet<>() )
////                                                                             .add( e.getKey() ) );
////
////      for ( Map.Entry<String,Collection<ConceptAggregate>> bestHistologyNeoplasms :
////            bestHistologyNeoplasmsMap.entrySet() ) {
////         final String histology = bestHistologyNeoplasms.getKey();
////         final Collection<histologyBestTopo>
////
////
////         final Collection<ConceptAggregate> otherBestSiteNeoplasms =
////               new HashSet<>( bestHistologyNeoplasmsMap.get( histology ) );
////         otherBestSiteNeoplasms.removeAll( bestSiteNeoplasms.getValue() );
////         otherBestSiteNeoplasms.removeAll( unwantedSiteNeoplasms );
////      }
////
////      for ( Map.Entry<String,ConceptAggregate> siteMergedNeoplasm : siteMergedNeoplasms.entrySet() ) {
////         final String histology = allSiteBestHistologies.get( siteMergedNeoplasm.getKey() );
////         final Collection<ConceptAggregate> unwantedSiteNeoplasms
////               = bestHistologyUnwantedSites.getOrDefault( histology, Collections.emptyList() );
////
////
////      }
//
//
//
//      removeUnwantedSiteNeoplasms( bestHistologyUnwantedSites, allConcepts );
//
//
//      // Thin by best site histologies?
//      // e.g. bestHistologyNeoplasms.keySet().retainAll( allSiteBestHistologies.getValue() );
//
//      // TODO --> best sites are: bestSiteNeoplasms.keySet().
//      // TODO --> best neoplasms are at bestSiteNeoplasms.[ values ]
//      // TODO --> best site histologies are those from allSiteBestHistologies.
//      // TODO --> Want to merge all neoplasms that aren't on the best sites with neoplasms that are on the best sites.
//      // TODO --> First merge all of the neoplasms at non-best topographies
//      //    with those on best site topographies with the same histology.
//      // TODO --> Next merge all of the neoplasms at non-best topographies
//      //    with ALL best site topographies.  This basically distributes the "unknown" histologies.
//
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      final Collection<String> massUris = UriConstants.getMassUris( graphDb );
//      final Predicate<ConceptAggregate> isMass = c -> c.getAllUris().stream().anyMatch( massUris::contains );
//      final Collection<ConceptAggregate> tumors = neoplasms.stream().filter( isMass ).collect( Collectors.toSet() );
//      final Collection<ConceptAggregate> cancers = new HashSet<>( neoplasms );
//      // Any Cancer sites that end up not having tumors will have tumors "created" at those sites.
//      // So, for any neoplasm that fits into cancer and tumor favor the cancer.
//      // This is better for histology and (primary)  topo.
//      tumors.removeAll( cancers );
//
//
//      return null;
//   }
//
//
//
//   static private Collection<ConceptAggregate> getNotBestSiteNeoplasms(
//         final Collection<String> unwantedSites,
//         final Map<String,Collection<ConceptAggregate>> allSiteNeoplasmsMap,
//         final Map<String,Collection<ConceptAggregate>> bestSiteNeoplasmsMap ) {
//      final Collection<ConceptAggregate> notBestSiteNeoplasms = new HashSet<>();
//      unwantedSites.stream()
//                   .map( allSiteNeoplasmsMap::get )
//                   .forEach( notBestSiteNeoplasms::addAll );
//      notBestSiteNeoplasms.addAll( bestSiteNeoplasmsMap.getOrDefault( TOPOGRAPHY_UNDETERMINED,
//                                                                      Collections.emptyList() ) );
//      return notBestSiteNeoplasms;
//   }
//
//   static private Collection<ConceptAggregate> getNotBestHistologyNeoplasms(
//         final Collection<String> unwantedHistologies,
//         final Map<String,Collection<ConceptAggregate>> allHistologyNeoplasmsMap,
//         final Map<String,Collection<ConceptAggregate>> bestHistologyNeoplasmsMap ) {
//      final Collection<ConceptAggregate> notBestHistologyNeoplasms = new HashSet<>();
//      unwantedHistologies.stream()
//                         .map( allHistologyNeoplasmsMap::get )
//                         .forEach( notBestHistologyNeoplasms::addAll );
//      CARCINOMA_UNKNOWNS.forEach( c -> notBestHistologyNeoplasms.addAll(
//            bestHistologyNeoplasmsMap.getOrDefault( c, Collections.emptyList() ) ) );
//      return notBestHistologyNeoplasms;
//   }
//
//
//
//
//   /////////////////////////////////////////////////////////////////////////
//   //
//   //         Neoplasm Sorting by histology.
//   //
//   /////////////////////////////////////////////////////////////////////////
//
//
//   static private Map<String,Collection<ConceptAggregate>> collectAllByHistology(
//         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
//      final Map<String, Collection<ConceptAggregate>> histologyNeoplasms = new HashMap<>();
//      for ( ConceptAggregate neoplasm : neoplasms ) {
//         final String bestCode = getHistologyCode( neoplasm, neoplasms, allConcepts );
//         final String code = bestCode.substring( 0, 3 );
//         histologyNeoplasms.computeIfAbsent( code, c -> new ArrayList<>() ).add( neoplasm );
//      }
//      return histologyNeoplasms;
//   }
//
//
//   static private Map<String,Collection<ConceptAggregate>> collectBestByHistology(
//         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
//         final Collection<String> keepCodes ) {
//      return collectBestByHistology( histologyNeoplasms, LOW_HISTOLOGY_CUTOFF, keepCodes );
//   }
//
//
//   static private Map<String,Collection<ConceptAggregate>> collectBestByHistology(
//         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
//         final double cutoff,
//         final Collection<String> keepCodes ) {
//      if ( histologyNeoplasms.size() <= 1 ) {
//         return new HashMap<>( histologyNeoplasms );
//      }
//      return getOnlyHighCountNeoplasms( histologyNeoplasms, cutoff, keepCodes );
//   }
//
//
//   /////////////////////////////////////////////////////////////////////////
//   //
//   //         Neoplasm Sorting by site.
//   //
//   /////////////////////////////////////////////////////////////////////////
//
//   /**
//    * ICDO code is a higher level than unique uri.  e.g. br, left br, nipple, etc.  This should allow better merges.
//    * @param neoplasms -
//    * @param allConcepts -
//    * @return map of major topo codes to list of concepts with those topo codes.
//    */
//   static private Map<String, Collection<ConceptAggregate>> collectAllBySite(
//         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
//      final Map<String, Collection<ConceptAggregate>> siteNeoplasms = new HashMap<>();
//      for ( ConceptAggregate neoplasm : neoplasms ) {
//         final Topography topography = new Topography( neoplasm, allConcepts );
//         final String bestCode = topography.getBestMajorTopoCode();
//         final String bestGroup = getSiteGroup( bestCode );
//         siteNeoplasms.computeIfAbsent( bestGroup, c -> new ArrayList<>() ).add( neoplasm );
//      }
//      return siteNeoplasms;
//   }
//
//   static private Map<String,Collection<ConceptAggregate>> collectBestBySite(
//         final Map<String,Collection<ConceptAggregate>> siteNeoplasms,
//         final Collection<String> keepCodes ) {
//      if ( siteNeoplasms.size() <= 1 ) {
//         return new HashMap<>( siteNeoplasms );
//      }
//      return getOnlyHighCountNeoplasms( siteNeoplasms, TOPOGRAPHY_CUTOFF, keepCodes );
//   }
//
//
//
//
//
//   /////////////////////////////////////////////////////////////////////////
//   //
//   //         Neoplasm Sorting by site histology.
//   //
//   /////////////////////////////////////////////////////////////////////////
//
//   // All 850 = Ductal Carcinoma.  8500 = BrCa in situ or invasive.  8501 = Comedocarcinoma.
//   // 8502 = Secretory_Breast_Carcinoma.  8503 = Intraductal_Papillary_Breast_Carcinoma.  etc.
//   // todo - should there be a more exact/narrow sort?
//   static private String getHistologyCode( final ConceptAggregate neoplasm,
//                                           final Collection<ConceptAggregate> allNeoplasms,
//                                           final Collection<ConceptAggregate> allConcepts ) {
//      final DefaultAttribute<HistologyUriInfoVisitor, HistologyCodeInfoStore> histology
//            = new Histology( neoplasm,
//                             allConcepts,
//                             allNeoplasms );
//      return histology.getBestCode();
//   }
//
//
//   static private Map<String,Collection<ConceptAggregate>> getBestHistoOneSite(
//         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
//         final Collection<String> keepCodes ) {
//      if ( histologyNeoplasms.size() <= 1 ) {
//         return new HashMap<>( histologyNeoplasms );
//      }
//      return getOnlyHighCountNeoplasms( histologyNeoplasms, HIGH_HISTOLOGY_CUTOFF, keepCodes );
//   }
//
//
//   static private Map<String,Collection<String>> collectAllSiteHistologies(
//         final Map<String,Collection<ConceptAggregate>> allSiteNeoplasms,
//         final Map<String,Collection<ConceptAggregate>> allHistologyNeoplasms ) {
//      final Map<ConceptAggregate,String> neoplasmHistologies = createNeoplasmCodeMap( allHistologyNeoplasms );
//      final Map<String,Collection<String>> allSiteHistologies = new HashMap<>( allSiteNeoplasms.size() );
//      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : allSiteNeoplasms.entrySet() ) {
//         final Collection<String> histologies = siteNeoplasms.getValue().stream()
//                                                                   .map( neoplasmHistologies::get )
//                                                                   .collect( Collectors.toSet() );
//         allSiteHistologies.put( siteNeoplasms.getKey(), histologies );
//      }
//      return allSiteHistologies;
//   }
//
//
//
//   // If we are lucky there will be only 1 histology at the site matching one of the best histologies of the patient.
//   static private String getLoneSiteHistology( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//                                               final Collection<String> bestHistologies ) {
//      final List<String> onlyBestHistologies = new ArrayList<>( siteHistologyNeoplasms.keySet() );
//      onlyBestHistologies.retainAll( bestHistologies );
//      if ( onlyBestHistologies.size() == 1 ) {
//         return onlyBestHistologies.get( 0 );
//      }
//      onlyBestHistologies.removeAll( CARCINOMA_UNKNOWNS );
//      if ( onlyBestHistologies.size() == 1 ) {
//         return onlyBestHistologies.get( 0 );
//      }
//      return "";
//   }
//
//
//   static private String getBestSiteHistologyLoneOrCalc( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//                                               final Collection<String> bestHistologies  ) {
//      // If we are lucky there will be only 1 histology at the site matching one of the best histologies of the patient.
//      String loneHistology = getLoneSiteHistology( siteHistologyNeoplasms, bestHistologies );
//      if ( !loneHistology.isEmpty() ) {
//         return loneHistology;
//      }
//      // Calculate the best histologies using all the existing histologies of neoplasms at the site
//      loneHistology = getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies );
//      if ( !loneHistology.isEmpty() ) {
//         return loneHistology;
//      }
//      return "";
//   }
//
//
//   // Calculate the best histologies using all the existing histologies of neoplasms at the site
//   static private String getBestSiteHistologyByCalc(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies ) {
//      final String bestSiteHistology
//            = getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies, LOW_HISTOLOGY_CUTOFF );
//      if ( !bestSiteHistology.isEmpty() ) {
//         return bestSiteHistology;
//      }
//      return getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies, HIGH_HISTOLOGY_CUTOFF );
//   }
//
//   // Calculate the best histologies using all the existing histologies of neoplasms at the site
//   static private String getBestSiteHistologyByCalc(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies, final double cutoff ) {
//      final Map<String,Collection<ConceptAggregate>> siteBestHistologyNeoplasms
//            = collectBestByHistology( siteHistologyNeoplasms, cutoff, CARCINOMA_UNKNOWNS );
//      return getLoneSiteHistology( siteBestHistologyNeoplasms, bestHistologies );
//   }
//
//   // Recalculate histologies using all the neoplasms at the site.
//   // THIS IS A PLACEHOLDER.
//   // The idea is that the neoplasms at the site are used as the "patientNeoplasms" in Attribute (Histology)
//   // Value Calculation.  At this time that is not done, so just return empty.
//   static private String getBestSiteHistologyByRecalc(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies,
//         final Collection<ConceptAggregate> allConcepts ) {
//      return "";
//      // Recalculate histologies using all the neoplasms at the site
////      final Collection<ConceptAggregate> siteNeoplasms = siteHistologyNeoplasms.values().stream()
////                                                                               .flatMap( Collection::stream )
////                                                                               .collect( Collectors.toSet() );
////      final Map<String,Collection<ConceptAggregate>> newSiteHistologyNeoplasms
////            = collectAllByHistology( siteNeoplasms, allConcepts );
////      if ( siteHistologyNeoplasms.keySet().equals( newSiteHistologyNeoplasms.keySet() ) ) {
////         // Don't bother to recalculate based upon "best" as that has already been done.
////         return "";
////      }
////      return getBestSiteHistologyLoneOrCalc( newSiteHistologyNeoplasms, bestHistologies );
//   }
//
//   static private Map<String,Collection<ConceptAggregate>> getBestSiteHistologyByMention(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies) {
//      final Map<String,Collection<ConceptAggregate>> maxMentionNeoplasms = new HashMap<>();
//      int max = 0;
//      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : siteHistologyNeoplasms.entrySet() ) {
//         final String code = entry.getKey();
//         if ( !bestHistologies.contains( code ) || code.equals( CARCINOMA_NOS ) ) {
//            continue;
//         }
//         int count = entry.getValue().stream()
//                          .map( ConceptAggregate::getMentions )
//                          .mapToInt( Collection::size )
//                          .sum();
//         if ( count > max ) {
//            max = count;
//            maxMentionNeoplasms.clear();
//         }
//         if ( count >= max ) {
//            maxMentionNeoplasms.computeIfAbsent( code, n -> new HashSet<>() ).addAll( entry.getValue() );
//         }
//      }
//      return maxMentionNeoplasms;
//   }
//
//   static private String getBestSiteHistologyByCount(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies ) {
//      final ToIntFunction<ConceptAggregate> countMentions = c -> c.getMentions().size();
//      final Map<String,Collection<ConceptAggregate>> maxMentionNeoplasms
//            = getBestSiteHistologyByCount( siteHistologyNeoplasms, bestHistologies, countMentions );
//      if ( maxMentionNeoplasms.size() == 1 ) {
//         return new ArrayList<>( maxMentionNeoplasms.keySet() ).get( 0 );
//      }
//      final ToIntFunction<ConceptAggregate> countRelations = c -> c.getRelatedConceptMap()
//                                                                   .values()
//                                                                   .stream()
//                                                                   .mapToInt( Collection::size )
//                                                                   .sum();
//      final Map<String,Collection<ConceptAggregate>> maxRelationNeoplasms
//            = getBestSiteHistologyByCount( maxMentionNeoplasms, bestHistologies, countRelations );
//      if ( maxRelationNeoplasms.size() == 1 ) {
//         return new ArrayList<>( maxRelationNeoplasms.keySet() ).get( 0 );
//      }
//      return "";
//   }
//
//   static private Map<String,Collection<ConceptAggregate>> getBestSiteHistologyByCount(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies,
//         final ToIntFunction<ConceptAggregate> countFunction ) {
//      final Map<String,Collection<ConceptAggregate>> maxCountNeoplasms = new HashMap<>();
//      int max = 0;
//      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : siteHistologyNeoplasms.entrySet() ) {
//         final String code = entry.getKey();
//         if ( !bestHistologies.contains( code ) || code.equals( CARCINOMA_NOS ) ) {
//            continue;
//         }
//         int count = entry.getValue().stream()
//                          .mapToInt( countFunction )
//                          .sum();
//         if ( count > max ) {
//            max = count;
//            maxCountNeoplasms.clear();
//         }
//         if ( count >= max ) {
//            maxCountNeoplasms.computeIfAbsent( code, n -> new HashSet<>() )
//                             .addAll( entry.getValue() );
//         }
//      }
//      return maxCountNeoplasms;
//   }
//
//
//   /**
//    * @param siteHistologyNeoplasms Neoplasms at the site major site.
//    * @param bestHistologies Best histologies for the patient.
//    * @param allConcepts all patient concepts.
//    * @return best histology at a site major that matches one of the best histologies
//    * as determined for the patient.
//    */
//   static private String getBestSiteHistology( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//                                           final Collection<String> bestHistologies,
//                                           final Collection<ConceptAggregate> allConcepts ) {
//      // If there are none of the patient's best histologies at this site, return CARCINOMA_NOS.
//      final List<String> onlyBestHistologies = new ArrayList<>( siteHistologyNeoplasms.keySet() );
//      onlyBestHistologies.retainAll( bestHistologies );
//      if ( onlyBestHistologies.isEmpty() ) {
//         return CARCINOMA_NOS;
//      }
//      // If there is a lone histology at the site, use that.
//      // If there is a histology that can be determined by (re)calculating the "best" histology
//      // at the site using the currently determined histologies, use that.
//      String bestHistology = getBestSiteHistologyLoneOrCalc( siteHistologyNeoplasms, bestHistologies );
//      if ( !bestHistology.isEmpty() ) {
//         return bestHistology;
//      }
//      // if there is a histology that can be determined by lone or (re)calculating the "best" histology
//      // at the site using recalculated histologies for the site, use that.
//      // Right now this always returns empty.
//      bestHistology = getBestSiteHistologyByRecalc( siteHistologyNeoplasms, bestHistologies, allConcepts );
//      if ( !bestHistology.isEmpty() ) {
//         return bestHistology;
//      }
//      // Get the histology that has either the max mention count or the max mention and max relation count.
//      bestHistology = getBestSiteHistologyByCount( siteHistologyNeoplasms, bestHistologies );
//      if ( !bestHistology.isEmpty() ) {
//         return bestHistology;
//      }
//      // If we get to this point we have low representations, e.g.:
//      //    BrCa has 1 Concept with 1 mention, 1 relation.
//      //    OvCa has 1 Concept with 1 mention, 1 relation.
//      // We can't really favor one over another.  Return NOS to distribute attributes across all merged neoplasms.
//      return CARCINOMA_NOS;
//   }
//
//
//
//
//   static private Map<String,String> collectSiteHistologies(
//         final Map<String,Collection<ConceptAggregate>> allSiteNeoplasms,
//         final Map<String,Collection<ConceptAggregate>> allHistologyNeoplasms,
//         final Collection<String> bestHistologies,
//         final Collection<ConceptAggregate> allConcepts ) {
//      final Map<String,String> bestSiteHistologies = new HashMap<>( allSiteNeoplasms.size() );
//      final Map<ConceptAggregate,String> neoplasmHistologies = createNeoplasmCodeMap( allHistologyNeoplasms );
//
//      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : allSiteNeoplasms.entrySet() ) {
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms = new HashMap<>();
//         for ( ConceptAggregate neoplasm : siteNeoplasms.getValue() ) {
//            siteHistologyNeoplasms.computeIfAbsent( neoplasmHistologies.get( neoplasm ), h -> new ArrayList<>() )
//                                  .add( neoplasm );
//         }
//         final String bestSiteHistology = getBestSiteHistology( siteHistologyNeoplasms, bestHistologies, allConcepts );
//         bestSiteHistologies.put( siteNeoplasms.getKey(), bestSiteHistology );
//      }
//      return bestSiteHistologies;
//   }
//
//
//   static private Map<ConceptAggregate,String> createNeoplasmCodeMap(
//         final Map<String,Collection<ConceptAggregate>> codeNeoplasmMap ) {
//      final Map<ConceptAggregate,String> neoplasmCodeMap = new HashMap<>();
//      for ( Map.Entry<String,Collection<ConceptAggregate>> codeNeoplasms : codeNeoplasmMap.entrySet() ) {
//         final String histology = codeNeoplasms.getKey();
//         codeNeoplasms.getValue().forEach( n -> neoplasmCodeMap.put( n, histology ) );
//      }
//      return neoplasmCodeMap;
//   }
//
//
//
//
//
//
//   /////////////////////////////////////////////////////////////////////////
//   //
//   //         Neoplasm Sorting Utilities.
//   //
//   /////////////////////////////////////////////////////////////////////////
//
//
//
//
//
//
//
//   static private Map<String,Collection<ConceptAggregate>> getOnlyHighCountNeoplasms(
//         final Map<String,Collection<ConceptAggregate>> codeNeoplasms,
//         final double cutoff_constant,
//         final Collection<String> keepCodes ) {
//      final Map<String,Collection<ConceptAggregate>> tempCodeNeoplasms = new HashMap<>( codeNeoplasms );
//      tempCodeNeoplasms.keySet().removeAll( keepCodes );
//      if ( tempCodeNeoplasms.size() <= 1 ) {
//         return new HashMap<>( codeNeoplasms );
//      }
//      final Map<String,Integer> codeMentionCounts = new HashMap<>();
//      int max = 0;
//      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : tempCodeNeoplasms.entrySet() ) {
//         final String code = entry.getKey();
//         int count = entry.getValue().stream()
//                          .map( ConceptAggregate::getMentions )
//                          .mapToInt( Collection::size )
//                          .sum();
//         codeMentionCounts.put( code, count );
//         max = Math.max( max, count );
//      }
//      double cutoff = max * cutoff_constant;
//      final Collection<String> highCountCodes = getOnlyHighCountCodes( codeMentionCounts, cutoff );
//      highCountCodes.addAll( keepCodes );
//      final Map<String,Collection<ConceptAggregate>> bestCodeNeoplasms = new HashMap<>( codeNeoplasms );
//      bestCodeNeoplasms.keySet().retainAll( highCountCodes );
//      return bestCodeNeoplasms;
//   }
//
//
//   static private Collection<String> getOnlyHighCountCodes( final Map<String,Integer> codeMentionCounts,
//                                                            final double cutoff ) {
//      final Collection<String> lowCountCodes = codeMentionCounts.entrySet().stream()
//                                                                .filter( e -> e.getValue() < cutoff )
//                                                                .map( Map.Entry::getKey )
//                                                                .collect( Collectors.toSet() );
//      if ( lowCountCodes.isEmpty() ) {
//         return codeMentionCounts.keySet();
//      }
//      final Collection<String> codes = new HashSet<>( codeMentionCounts.keySet() );
//      codes.removeAll( lowCountCodes );
//      return codes;
//   }
//
//
//   static private Collection<ConceptAggregate> getFlatBestSiteNeoplasms(
//         final Map<String,Collection<ConceptAggregate>> bestSiteNeoplasms ) {
//      return bestSiteNeoplasms.values().stream()
//                                    .flatMap( Collection::stream )
//                                    .collect( Collectors.toSet() );
//   }
//
//
//   static private Collection<ConceptAggregate> getUndeterminedHistologies(
//         final Collection<String> bestHistologies,
//         final Map<String,Collection<ConceptAggregate>> allHistologyNeoplasms,
//         final Collection<ConceptAggregate> flatBestSiteNeoplasms ) {
//      return CARCINOMA_UNKNOWNS.stream()
//                               .filter( u -> !bestHistologies.contains( u ) )
//                               .map( allHistologyNeoplasms::get )
//                               .flatMap( Collection::stream )
//                               .filter( n -> !flatBestSiteNeoplasms.contains( n ) )
//                               .collect( Collectors.toSet() );
//   }
//
//
//
////   static private void createMergedNeoplasms( final String histology,
////                                             final Collection<String> bestHistologies,
////                                             final Map<String,Collection<ConceptAggregate>> allHistologyNeoplasms,
////                                             final Map<String,Collection<ConceptAggregate>> allSiteNeoplasms,
////                                             final Map<String,Collection<ConceptAggregate>> bestSiteNeoplasms,
////                                             final Map<String,String> allSiteBestHistologies ) {
////      final Collection<String> sitesWithHistology
////            = allSiteBestHistologies.entrySet().stream()
////                                          .filter( e -> e.getValue().equals( histology ) )
////                                          .map( Map.Entry::getKey )
////                                          .collect( Collectors.toSet() );
////      final Collection<String> bestSitesWithHistology = new ArrayList<>( sitesWithHistology );
////      bestSitesWithHistology.retainAll( bestSiteNeoplasms.keySet() );
////
////      final Collection<ConceptAggregate> flatBestSiteNeoplasms
////            = getFlatBestSiteNeoplasms( bestSiteNeoplasms );
////      final Collection<ConceptAggregate> undeterminedHistologiesToDistribute
////            = getUndeterminedHistologies( bestHistologies, allHistologyNeoplasms, flatBestSiteNeoplasms );
////
////      final Map<String,Collection<ConceptAggregate>> bestSiteHistologyConcepts = new HashMap<>();
////      final Collection<ConceptAggregate> sameHistologyNeoplasmsToDistribute = new HashSet<>();
////      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : allSiteNeoplasms.entrySet() ) {
////         final String site = siteNeoplasms.getKey();
////         if ( !sitesWithHistology.contains( site ) ) {
////            continue;
////         }
////         if ( bestSitesWithHistology.contains( site ) ) {
////            bestSiteHistologyConcepts.put( site, siteNeoplasms.getValue() );
////            continue;
////         }
////         sameHistologyNeoplasmsToDistribute.addAll( siteNeoplasms.getValue() );
////      }
////      // Now have primary and secondary sites with exact histology and unwanted sites with exact histology
////
////
////   }
//
//
////   static private void updateNeoplasmMergeMap(
////         final ConceptAggregate newNeoplasm,
////                                          final Collection<ConceptAggregate> mergingSiteNeoplasms,
////                                          final Collection<ConceptAggregate> otherSiteNeoplasms,
////                                          final Collection<ConceptAggregate> unwantedSiteNeoplasms,
////                                          final Collection<ConceptAggregate> undeterminedSiteNosNeoplasms,
////         final Map<ConceptAggregate, Collection<ConceptAggregate>> mergeMap ) {
////      updateNeoplasmMergeMap( newNeoplasm, mergingSiteNeoplasms, mergeMap );
////      updateNeoplasmMergeMap( newNeoplasm, otherSiteNeoplasms, mergeMap );
////   }
////
////
////   static private void updateNeoplasmMergeMap( final ConceptAggregate newNeoplasm,
////                                          final Collection<ConceptAggregate> mergingNeoplasms,
////                                          final Map<ConceptAggregate,Collection<ConceptAggregate>> mergeMap ) {
////      mergingNeoplasms.forEach( n -> mergeMap.computeIfAbsent( n, m -> new HashSet<>() ).add( newNeoplasm ) );
////   }
////
////   static private void reassignRelations( final Map<ConceptAggregate, Collection<ConceptAggregate>> fullMergeMap,
////                                          final Map<ConceptAggregate, Collection<ConceptAggregate>> noSiteMergeMap,
////                                          final Map<ConceptAggregate, Collection<ConceptAggregate>> trackSiteMergeMap,
////                                          final Collection<ConceptAggregate> allConcepts ) {
////      for ( ConceptAggregate concept : allConcepts ) {
////         final Map<String, Collection<ConceptAggregate>> oldRelations = concept.getRelatedConceptMap();
////         if ( oldRelations.isEmpty() ) {
////            continue;
////         }
////         final Map<String,Collection<ConceptAggregate>> newRelations = new HashMap<>();
////         for ( Map.Entry<String,Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
////            final String relationName = oldRelation.getKey();
////            final Collection<ConceptAggregate> newTargets = new HashSet<>();
////            for ( ConceptAggregate oldTarget : oldRelation.getValue() ) {
////               final Collection<ConceptAggregate> fullMergedTargets
////                     = fullMergeMap.getOrDefault( oldTarget, Collections.emptyList() );
////               final Collection<ConceptAggregate> noSiteMergedTargets
////                     = noSiteMergeMap.getOrDefault( oldTarget, Collections.emptyList() );
////               final Collection<ConceptAggregate> trackSiteMergedTargets
////                     = trackSiteMergeMap.getOrDefault( oldTarget, Collections.emptyList() );
////               if ( RelationConstants.isHasSiteRelation( relationName ) ) {
////
////               } else {
////
////               }
////            }
////
////
////         }
////
////
////      }
////   }
//
//   static private Map<String,Collection<String>> collectMergingUriRoots(
//         final Collection<ConceptAggregate> mergingSiteNeoplasms,
//         final Collection<ConceptAggregate> unwantedSiteNeoplasms ) {
//      final Map<String,Collection<String>> newNeoplasmUriRoots = new HashMap<>();
//      mergingSiteNeoplasms.stream()
//                          .map( ConceptAggregate::getUriRootsMap )
//                          .map( Map::entrySet )
//                          .flatMap( Collection::stream )
//                          .filter( e -> !newNeoplasmUriRoots.containsKey( e.getKey() ) )
//                          .forEach( e -> newNeoplasmUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
//                                                            .addAll( e.getValue() ) );
//      unwantedSiteNeoplasms.stream()
//                           .map( ConceptAggregate::getUriRootsMap )
//                           .map( Map::entrySet )
//                           .flatMap( Collection::stream )
//                           .filter( e -> !newNeoplasmUriRoots.containsKey( e.getKey() ) )
//                           .forEach( e -> newNeoplasmUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
//                                                             .addAll( e.getValue() ) );
////      undeterminedSiteNosNeoplasms.stream()
////                           .map( ConceptAggregate::getUriRootsMap )
////                           .map( Map::entrySet )
////                           .flatMap( Collection::stream )
////                           .forEach( e -> siteUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
////                           .addAll( e.getValue() ) );
//      return newNeoplasmUriRoots;
//   }
//
//   static private ConceptAggregate createMergedNeoplasm( final String patientId,
//         final Collection<ConceptAggregate> mergingSiteNeoplasms,
//         final Collection<ConceptAggregate> unwantedSiteNeoplasms ) {
//      final Map<String,Collection<String>> newNeoplasmUriRoots = collectMergingUriRoots( mergingSiteNeoplasms,
//                                                                                         unwantedSiteNeoplasms );
//      final Map<String, Collection<Mention>> newNeoplasmDocMentions
//            = ConceptAggregateHandler.collectDocMentions( mergingSiteNeoplasms );
//      ConceptAggregateHandler.appendDocMentions( unwantedSiteNeoplasms, newNeoplasmDocMentions );
//      return new DefaultConceptAggregate( patientId, newNeoplasmUriRoots, newNeoplasmDocMentions );
//   }
//
//   /**
//    * Merge given neoplasms into a single neoplasm.
//    * !!! Note !!!   This undoes previous concept separations.
//    *
//    * @param patientId    -
//    * @param mergingSiteNeoplasms -
//    * @param allConcepts -
//    */
//   static public void createMergedNeoplasm( final String patientId,
//                                            final Collection<ConceptAggregate> mergingSiteNeoplasms,
//                                            final Collection<ConceptAggregate> otherSiteNeoplasms,
//                                            final Collection<ConceptAggregate> unwantedSiteNeoplasms,
//                                            final Collection<ConceptAggregate> undeterminedSiteNosNeoplasms,
//                                            final Collection<ConceptAggregate> allConcepts ) {
//      final ConceptAggregate newNeoplasm
//            = createMergedNeoplasm( patientId, unwantedSiteNeoplasms, unwantedSiteNeoplasms );
//
//      final Map<String, Collection<ConceptAggregate>> newNeoplasmRelations = new HashMap<>();
//
//      for ( ConceptAggregate concept : allConcepts ) {
//         final Map<String, Collection<ConceptAggregate>> oldRelations = concept.getRelatedConceptMap();
//         if ( mergingSiteNeoplasms.contains( concept ) ) {
//            // Replace references to concept with references to the new merged neoplasm.
//            appendNewNeoplasmRelations( newNeoplasm, mergingSiteNeoplasms, unwantedSiteNeoplasms,
//                                        newNeoplasmRelations, oldRelations );
//         } else if ( otherSiteNeoplasms.contains( concept ) ) {
//            // add relations from other sites with the same histology to the new Neoplasm.  Not including hasLocation.
//            appendOtherSiteRelations( newNeoplasm, mergingSiteNeoplasms, unwantedSiteNeoplasms,
//                                    newNeoplasmRelations, oldRelations );
//         } else if ( unwantedSiteNeoplasms.contains( concept ) ) {
//
//            appendNonSiteRelations( newNeoplasm, mergingSiteNeoplasms, unwantedSiteNeoplasms,
//                                    newNeoplasmRelations, oldRelations );
//         } else if ( undeterminedSiteNosNeoplasms.contains( concept ) ) {
//            appendNonSiteRelations( newNeoplasm, mergingSiteNeoplasms, unwantedSiteNeoplasms,
//                                    newNeoplasmRelations, oldRelations );
//         } else {
//            adjustOtherRelations( newNeoplasm, mergingSiteNeoplasms, concept, unwantedSiteNeoplasms, oldRelations );
//         }
//      }
//      newNeoplasm.setRelated( newNeoplasmRelations );
//      allConcepts.removeAll( mergingSiteNeoplasms );
//      allConcepts.add( newNeoplasm );
////      LOGGER.info( "mergeNeoplasms : New NEOPLASM\n" + newNeoplasm );
//   }
//
////   static public ConceptAggregate createMergedNeoplasm( final String patientId,
////                                            final Collection<ConceptAggregate> mergingSiteNeoplasms,
////                                            final Collection<ConceptAggregate> unwantedSiteNeoplasms,
////                                            final Collection<ConceptAggregate> allConcepts ) {
////      final ConceptAggregate newNeoplasm
////            = createMergedNeoplasm( patientId, unwantedSiteNeoplasms, unwantedSiteNeoplasms );
////
////      for ( ConceptAggregate concept : allConcepts ) {
////         final Map<String, Collection<ConceptAggregate>> relationsMap = concept.getRelatedConceptMap();
////         for ( Map.Entry<String, Collection<ConceptAggregate>> relations : relationsMap.entrySet() ) {
////            if ( relations.getValue().removeAll( mergingSiteNeoplasms ) ) {
////               relations.getValue().add( newNeoplasm );
////            }
////         }
////      }
//////      LOGGER.info( "mergeNeoplasms : New NEOPLASM\n" + newNeoplasm );
////      return newNeoplasm;
////   }
//
//   // Same site, same histology.  Copy all relations for the merge.
//   static private void appendNewNeoplasmRelations( final ConceptAggregate newNeoplasm,
//                                                   final Collection<ConceptAggregate> mergingSiteNeoplasms,
//                                                   final Collection<ConceptAggregate> unwantedSiteNeoplasms,
//                                                   final Map<String, Collection<ConceptAggregate>> newNeoplasmRelations,
//                                                   final Map<String, Collection<ConceptAggregate>> oldRelations ) {
//      for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
//         for ( ConceptAggregate oldTargetConcept : oldRelation.getValue() ) {
//            final Collection<ConceptAggregate> related
//                  = newNeoplasmRelations.computeIfAbsent( oldRelation.getKey(), r -> new HashSet<>() );
//            if ( mergingSiteNeoplasms.contains( oldTargetConcept ) ) {
//               related.add( newNeoplasm );
//            } else if ( unwantedSiteNeoplasms.contains( oldTargetConcept ) ) {
//               related.add( newNeoplasm );
//               // Keep oldTargetConcept.  Future merged site iterations should keep adding their own newNeoplasm.
//               related.add( oldTargetConcept );
//            } else {
//               related.add( oldTargetConcept );
//            }
//         }
//      }
//   }
//
//   // Same Histology, other site that is one of the best.  Copy all non-site relations
//   static private void appendOtherSiteRelations( final ConceptAggregate newNeoplasm,
//                                               final Collection<ConceptAggregate> mergingSiteNeoplasms,
//                                               final Collection<ConceptAggregate> unwantedSiteNeoplasms,
//                                               final Map<String, Collection<ConceptAggregate>> newNeoplasmRelations,
//                                               final Map<String, Collection<ConceptAggregate>> otherRelations ) {
//      for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : otherRelations.entrySet() ) {
//         if ( RelationConstants.isHasSiteRelation( oldRelation.getKey() ) ) {
//            // Neoplasm is on a different wanted site.  We don't want to copy site relations.
//            continue;
//         }
//         for ( ConceptAggregate oldTargetConcept : oldRelation.getValue() ) {
//            final Collection<ConceptAggregate> related
//                  = newNeoplasmRelations.computeIfAbsent( oldRelation.getKey(), r -> new HashSet<>() );
//            if ( mergingSiteNeoplasms.contains( oldTargetConcept ) ) {
//               related.add( newNeoplasm );
//            } else if ( unwantedSiteNeoplasms.contains( oldTargetConcept ) ) {
//               related.add( newNeoplasm );
//               // Keep oldTargetConcept.  Future merged site iterations should keep adding their own newNeoplasm.
//               related.add( oldTargetConcept );
//            } else {
//               related.add( oldTargetConcept );
//            }
//         }
//      }
//   }
//
//   // Same Histology, other site that is not one of the best.  Copy all non-site relations, "track" site relations.
//   static private void appendNonSiteRelations( final ConceptAggregate newNeoplasm,
//                                            final Collection<ConceptAggregate> mergingSiteNeoplasms,
//                                               final Collection<ConceptAggregate> unwantedSiteNeoplasms,
//                                               final Map<String, Collection<ConceptAggregate>> newNeoplasmRelations,
//                                            final Map<String, Collection<ConceptAggregate>> otherRelations ) {
//      for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : otherRelations.entrySet() ) {
//         // Neoplasm is on a different wanted site.  We don't want to copy site relations, but keep track of them.
//         String relationName = RelationConstants.isHasSiteRelation( oldRelation.getKey() )
//                               ? ADJUSTED_SITE_RELATION : oldRelation.getKey();
//         for ( ConceptAggregate oldTargetConcept : oldRelation.getValue() ) {
//            final Collection<ConceptAggregate> related
//                  = newNeoplasmRelations.computeIfAbsent( relationName, r -> new HashSet<>() );
//            if ( mergingSiteNeoplasms.contains( oldTargetConcept ) ) {
//               related.add( newNeoplasm );
//            } else if ( unwantedSiteNeoplasms.contains( oldTargetConcept ) ) {
//               related.add( newNeoplasm );
//               // Keep oldTargetConcept.  Future merged site iterations should keep adding their own newNeoplasm.
//               related.add( oldTargetConcept );
//            } else {
//               related.add( oldTargetConcept );
//            }
//         }
//      }
//   }
//
//   // Generally unrelated, but we need to swap out any reference to non-merged neoplasms with the new merged neoplasm.
//   static private void adjustOtherRelations( final ConceptAggregate newNeoplasm,
//                                             final Collection<ConceptAggregate> mergingSiteNeoplasms,
//                                             final ConceptAggregate conceptToKeep,
//                                             final Collection<ConceptAggregate> unwantedSiteNeoplasms,
//                                             final Map<String, Collection<ConceptAggregate>> oldRelations ) {
//      final Map<String, Collection<ConceptAggregate>> newRelations = new HashMap<>();
//      final Collection<ConceptAggregate> newRelated = new HashSet<>();
//      boolean adjusted = false;
//      for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
//         for ( ConceptAggregate oldTargetConcept : oldRelation.getValue() ) {
//            if ( mergingSiteNeoplasms.contains( oldTargetConcept ) ) {
//               newRelated.add( newNeoplasm );
//               adjusted = true;
//            } else if ( unwantedSiteNeoplasms.contains( oldTargetConcept ) ) {
//               newRelated.add( newNeoplasm );
//               // Keep oldTargetConcept.  Future merged site iterations should keep adding their own newNeoplasm.
//               newRelated.add( oldTargetConcept );
//               adjusted = true;
//            } else {
//               newRelated.add( oldTargetConcept );
//            }
//         }
//         newRelations.put( oldRelation.getKey(), newRelated );
//      }
//      if ( adjusted ) {
//         conceptToKeep.setRelated( newRelations );
//      }
//   }
//
//
//
//   static private void removeUnwantedSiteNeoplasms( final Map<String,Collection<ConceptAggregate>> bestHistologyNotBestSites,
//                                                    final Collection<ConceptAggregate> allConcepts ) {
//      final Collection<ConceptAggregate> bestHistologyNotBestSitesNeoplasms
//            = bestHistologyNotBestSites.values().stream().flatMap( Collection::stream ).collect( Collectors.toSet() );
//
//      for ( ConceptAggregate concept : allConcepts ) {
//         final Map<String, Collection<ConceptAggregate>> newRelations = new HashMap<>();
//         final Map<String, Collection<ConceptAggregate>> oldRelationsMap = concept.getRelatedConceptMap();
//         if ( oldRelationsMap.isEmpty() ) {
//            continue;
//         }
//         for ( Map.Entry<String,Collection<ConceptAggregate>> oldRelations : oldRelationsMap.entrySet() ) {
//            final Collection<ConceptAggregate> newTargets = new HashSet<>( oldRelations.getValue() );
//            newTargets.removeAll( bestHistologyNotBestSitesNeoplasms );
//            if ( !newTargets.isEmpty() ) {
//               newRelations.put( oldRelations.getKey(), newTargets );
//            }
//         }
//         concept.setRelated( newRelations );
//      }
//      allConcepts.removeAll( bestHistologyNotBestSitesNeoplasms );
//   }
//
//   /**
//    * For all the neoplasms with the same histology, distribute all the relations except for sites.
//    * @param sameSiteNeoplasms either neoplasms with the same histology or neoplasms with the same site.
//    */
//   static private void relateForSameSite( final Collection<ConceptAggregate> sameSiteNeoplasms ) {
//      final Map<String,Collection<ConceptAggregate>> allRelations = new HashMap<>();
//      for ( ConceptAggregate neoplasm : sameSiteNeoplasms ) {
//         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
//         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
//            allRelations.computeIfAbsent( relatedConcepts.getKey(), r -> new HashSet<>() )
//                        .addAll( relatedConcepts.getValue() );
//         }
//      }
//      for ( ConceptAggregate neoplasm : sameSiteNeoplasms ) {
//         allRelations.forEach( neoplasm::addRelated );
//      }
//   }
//
//
//   /**
//    * For all the neoplasms with the same histology, distribute all the relations except for sites.
//    * @param sameHistologyNeoplasms either neoplasms with the same histology or neoplasms with the same site.
//    */
//   static private void relateForSameHistologies( final Collection<ConceptAggregate> sameHistologyNeoplasms ) {
//      final Map<String,Collection<ConceptAggregate>> nonSiteRelations = new HashMap<>();
//      for ( ConceptAggregate neoplasm : sameHistologyNeoplasms ) {
//         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
//         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
//            if ( RelationConstants.isHasSiteRelation( relatedConcepts.getKey() ) ) {
//               continue;
//            }
//            nonSiteRelations.computeIfAbsent( relatedConcepts.getKey(), r -> new HashSet<>() )
//                            .addAll( relatedConcepts.getValue() );
//         }
//      }
//      for ( ConceptAggregate neoplasm : sameHistologyNeoplasms ) {
//         nonSiteRelations.forEach( neoplasm::addRelated );
//      }
//   }
//
//   /**
//    * For all the neoplasms with an undetermined (or minority) site and nos histology,
//    * distribute all the relations except for sites.
//    * @param patientId -
//    * @param undeterminedNosNeoplasms -
//    * @param bestSiteNeoplasmsMap -
//    */
//   static private void relateUndeterminedSiteNos( final String patientId,
//         final Collection<ConceptAggregate> undeterminedNosNeoplasms,
//         final Map<String,Collection<ConceptAggregate>> bestSiteNeoplasmsMap,
//                                                  final Collection<ConceptAggregate> allConcepts ) {
//      final Map<String,Collection<ConceptAggregate>> adjustedRelations = new HashMap<>();
//      for ( ConceptAggregate neoplasm : undeterminedNosNeoplasms ) {
//         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
//         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
//            final String name = RelationConstants.isHasSiteRelation( relatedConcepts.getKey() )
//                                ? ADJUSTED_SITE_RELATION : relatedConcepts.getKey();
//            adjustedRelations.computeIfAbsent( name, r -> new HashSet<>() )
//                            .addAll( relatedConcepts.getValue() );
//         }
//      }
//      for ( Map.Entry<String,Collection<ConceptAggregate>> relations : adjustedRelations.entrySet() ) {
//         bestSiteNeoplasmsMap.values().stream()
//                             .flatMap( Collection::stream )
//                             .forEach( n -> n.addRelated( relations.getKey(), relations.getValue() ) );
//      }
//      final ConceptAggregate undeterminedNosNeoplasm
//            = createMergedNeoplasm( patientId, undeterminedNosNeoplasms, Collections.emptyList() );
//      bestSiteNeoplasmsMap.values().forEach( c -> c.add( undeterminedNosNeoplasm ) );
//      replaceRelated( undeterminedNosNeoplasm, undeterminedNosNeoplasms, allConcepts );
//      allConcepts.removeAll( undeterminedNosNeoplasms );
//      allConcepts.add( undeterminedNosNeoplasm );
//   }
//
//
//   static private void replaceRelated( final ConceptAggregate replacement,
//                                       final Collection<ConceptAggregate> toReplace,
//                                       final Collection<ConceptAggregate> allConcepts ) {
//      for ( ConceptAggregate concept : allConcepts ) {
//         final Map<String, Collection<ConceptAggregate>> relationsMap = concept.getRelatedConceptMap();
//         for ( Map.Entry<String, Collection<ConceptAggregate>> relations : relationsMap.entrySet() ) {
//            if ( relations.getValue().removeAll( toReplace ) ) {
//               relations.getValue().add( replacement );
//            }
//         }
//      }
//   }
//
//
//
////   /**
////    * Merge given neoplasms into a single neoplasm.
////    * !!! Note !!!   This undoes previous concept separations.
////    *
////    * @param patientId    -
////    * @param neoplasms -
////    * @param allConcepts -
////    * @return -
////    */
////   static public ConceptAggregate createMergedNeoplasm( final String patientId,
////                                                         final Collection<ConceptAggregate> neoplasms,
////                                                         final Collection<ConceptAggregate> allConcepts ) {
////      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
////      neoplasms.stream()
////                     .map( ConceptAggregate::getUriRootsMap )
////                     .map( Map::entrySet )
////                     .flatMap( Collection::stream )
////                     .forEach( e -> allUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() ).addAll( e.getValue() ) );
////      final Map<String, Collection<Mention>> docMentions = ConceptAggregateHandler.collectDocMentions( neoplasms );
////      final ConceptAggregate newNeoplasm = new DefaultConceptAggregate( patientId, allUriRoots, docMentions );
////
////      final Map<String, Collection<ConceptAggregate>> neoplasmRelations = new HashMap<>();
////
////      for ( ConceptAggregate concept : allConcepts ) {
////         final boolean isNeoplasm = neoplasms.contains( concept );
////         final Map<String, Collection<ConceptAggregate>> oldRelations = concept.getRelatedConceptMap();
////         final Map<String, Collection<ConceptAggregate>> newRelations = new HashMap<>( oldRelations.size() );
////         for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
////            if ( isNeoplasm ) {
////               for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
////                  final Collection<ConceptAggregate> related
////                        = neoplasmRelations.computeIfAbsent( oldRelation.getKey(), r -> new HashSet<>() );
////                  if ( neoplasms.contains( oldConcept ) ) {
////                     related.add( newNeoplasm );
////                  } else {
////                     related.add( oldConcept );
////                  }
////               }
////               continue;
////            }
////            final Collection<ConceptAggregate> newRelated = new HashSet<>();
////            for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
////               if ( neoplasms.contains( oldConcept ) ) {
////                  newRelated.add( newNeoplasm );
////               } else {
////                  newRelated.add( oldConcept );
////               }
////            }
////            newRelations.put( oldRelation.getKey(), newRelated );
////         }
////         concept.setRelated( newRelations );
////      }
////      newNeoplasm.setRelated( neoplasmRelations );
////      allConcepts.removeAll( neoplasms );
////      allConcepts.add( newNeoplasm );
////
//////      LOGGER.info( "mergeNeoplasms : New NEOPLASM\n" + newNeoplasm );
////
////      return newNeoplasm;
////   }
////
////
////
////   /**
////    * Merge given neoplasms into a single neoplasm.
////    * !!! Note !!!   This undoes previous concept separations.
////    *
////    * @param patientId    -
////    * @param tempNeoplasms -
////    * @param tempAllConcepts -
////    * @return -
////    */
////   static public ConceptAggregate createTempNeoplasm( final String patientId,
////                                                        final Collection<ConceptAggregate> tempNeoplasms,
////                                                        final Collection<ConceptAggregate> tempAllConcepts ) {
////      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
////      tempNeoplasms.stream()
////               .map( ConceptAggregate::getUriRootsMap )
////               .map( Map::entrySet )
////               .flatMap( Collection::stream )
////               .forEach( e -> allUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() ).addAll( e.getValue() ) );
////      final Map<String, Collection<Mention>> docMentions = ConceptAggregateHandler.collectDocMentions( tempNeoplasms );
////      final ConceptAggregate newNeoplasm = new DefaultConceptAggregate( patientId, allUriRoots, docMentions );
////
////      final Map<String, Collection<ConceptAggregate>> neoplasmRelations = new HashMap<>();
////
////      for ( ConceptAggregate concept : tempAllConcepts ) {
////         final boolean isNeoplasm = tempNeoplasms.contains( concept );
////         final Map<String, Collection<ConceptAggregate>> oldRelations = concept.getRelatedConceptMap();
////         final Map<String, Collection<ConceptAggregate>> newRelations = new HashMap<>( oldRelations.size() );
////         for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
////            if ( isNeoplasm ) {
////               for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
////                  final Collection<ConceptAggregate> related
////                        = neoplasmRelations.computeIfAbsent( oldRelation.getKey(), r -> new HashSet<>() );
////                  if ( tempNeoplasms.contains( oldConcept ) ) {
////                     related.add( newNeoplasm );
////                  } else {
////                     related.add( oldConcept );
////                  }
////               }
////               continue;
////            }
////            final Collection<ConceptAggregate> newRelated = new HashSet<>();
////            for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
////               if ( tempNeoplasms.contains( oldConcept ) ) {
////                  newRelated.add( newNeoplasm );
////               } else {
////                  newRelated.add( oldConcept );
////               }
////            }
////            newRelations.put( oldRelation.getKey(), newRelated );
////         }
////         concept.setRelated( newRelations );
////      }
////      newNeoplasm.setRelated( neoplasmRelations );
////      tempAllConcepts.removeAll( tempNeoplasms );
////      tempAllConcepts.add( newNeoplasm );
////
////      LOGGER.info( "createTempNeoplasm : New TEMP NEOPLASM\n" + newNeoplasm );
////
////      return newNeoplasm;
////   }
//
//
//
//
////   /////////////////////////////////////////////////////////////////////////
////   //
////   //         Neoplasm Sorting by site.
////   //
////   /////////////////////////////////////////////////////////////////////////
////
////   /**
////    * ICDO code is a higher level than unique uri.  e.g. br, left br, nipple, etc.  This should allow better merges.
////    * @param neoplasms -
////    * @param allConcepts -
////    * @return map of major topo codes to list of concepts with those topo codes.
////    */
////   static private Map<String, Collection<ConceptAggregate>> collectAllBySite(
////         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
////      final Map<String, Collection<ConceptAggregate>> siteNeoplasms = new HashMap<>();
////      for ( ConceptAggregate neoplasm : neoplasms ) {
////         final Topography topography = new Topography( neoplasm, allConcepts );
////         final String bestCode = topography.getBestMajorTopoCode();
////         final String bestGroup = getSiteGroup( bestCode );
////         siteNeoplasms.computeIfAbsent( bestGroup, c -> new ArrayList<>() ).add( neoplasm );
////      }
////      return siteNeoplasms;
////   }
////
////   static private Map<String,Collection<ConceptAggregate>> collectBestBySite(
////         final Map<String,Collection<ConceptAggregate>> siteNeoplasms,
////         final Collection<String> keepCodes ) {
////      if ( siteNeoplasms.size() <= 1 ) {
////         return new HashMap<>( siteNeoplasms );
////      }
////      return getOnlyHighCountNeoplasms( siteNeoplasms, TOPOGRAPHY_CUTOFF, keepCodes );
////   }
////
////
////
////   /**
////    * For all the neoplasms with the same histology, distribute all the relations except for sites.
////    * @param sameSiteNeoplasms either neoplasms with the same histology or neoplasms with the same site.
////    */
////   static private void relateForSameSite( final Collection<ConceptAggregate> sameSiteNeoplasms ) {
////      final Map<String,Collection<ConceptAggregate>> allRelations = new HashMap<>();
////      for ( ConceptAggregate neoplasm : sameSiteNeoplasms ) {
////         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
////         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
////            allRelations.computeIfAbsent( relatedConcepts.getKey(), r -> new HashSet<>() )
////                        .addAll( relatedConcepts.getValue() );
////         }
////      }
////      for ( ConceptAggregate neoplasm : sameSiteNeoplasms ) {
////         allRelations.forEach( neoplasm::addRelated );
////      }
////   }
//
//
//
//
//
//
//
//
////
////   static private Map<String,Collection<ConceptAggregate>> collectBestByHistology(
////         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
////         final Collection<String> keepCodes ) {
////      return collectBestByHistology( histologyNeoplasms, LOW_HISTOLOGY_CUTOFF, keepCodes );
////   }
////
////
////   static private Map<String,Collection<ConceptAggregate>> collectBestByHistology(
////         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
////         final double cutoff,
////         final Collection<String> keepCodes ) {
////      if ( histologyNeoplasms.size() <= 1 ) {
////         return new HashMap<>( histologyNeoplasms );
////      }
////      return AttributeTable.getNeoplasmsAboveCutoff( histologyNeoplasms, cutoff, keepCodes );
////   }
//
//
//
//
//}
