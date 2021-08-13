//package org.healthnlp.deepphe.summary.concept;
//
//import org.apache.log4j.Logger;
//import org.healthnlp.deepphe.core.uri.UriUtil;
//import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
//import org.healthnlp.deepphe.neo4j.constant.UriConstants;
//import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.neo4j.node.MentionRelation;
//import org.healthnlp.deepphe.neo4j.util.SearchUtil;
//import org.healthnlp.deepphe.summary.concept.bin.LateralityBinOld;
//import org.healthnlp.deepphe.util.MentionUtil;
//import org.neo4j.graphdb.GraphDatabaseService;
//
//import java.util.*;
//import java.util.function.Function;
//import java.util.function.Supplier;
//import java.util.stream.Collectors;
//
///**
// * @author SPF , chip-nlp
// * @since {5/19/2021}
// */
//final public class ConceptAggregateCreator {
//
//   static private final Logger LOGGER = Logger.getLogger( "ConceptAggregateCreator" );
//
//   private ConceptAggregateCreator() {
//   }
//
//
////   Step 1:
////> Create and merge Site concepts.
////> Sort and Create Concept Aggregates based upon merged sites.
////
////Step 2:
////> Sort by Laterality.  For any with no laterality assign to same-site type the dominant (most Mentions) existing
//// with laterality.
////> For any same-cancer/mass concepts with 2 lateralities, if one laterality is dominant add cancer attributes to the
//// concept with dominant laterality.
////
////Step 3:
////> for every concept that is in a hasDiagnosis or hasTumorExtent relationship, add cancer attributes to those
//// concepts.  e.g. T2_Stage in Hepatic_Cyst to Ductal_Breast_Carcinoma_In_Situ of hasDiagnosis.
////-- Be careful not to get in a loop where the hasDiagnosis is the same concept.  e.g. "cancerA" hasDiagnosis "cancerA".
////
////Step 4:
////> Sort by affirmed vs. negated cancer/mass.
////> For any same-cancer/mass concepts with 2 affirm/negated, if one affirmation is dominant add cancer attributes to
//// the concept with affirmed.
////> Remove any non-dominant negated cancer/mass.
////
////Step 5:
////> Remove cancer/mass with non-dominant laterality.
////> Remove non-dominant cancer/mass.
////
////Step 6:
////> Merge back any dominant negated and affirmed cancer/mass.
////
////Step 7:
////> Merge Concepts again?  See if this is necessary.\
//
//   static private final Collection<String> LATERALITY_URIS
//         = Arrays.asList( UriConstants.LEFT, UriConstants.RIGHT, UriConstants.BILATERAL );
//
//   /**
//    *
//    * @param patientMentionNoteIds -
//    * @return Map of unique URIs to all Mentions with that exact URI.
//    */
//   static private Map<String, List<Mention>> createUriToMentionsMap(
//         final Map<Mention, String> patientMentionNoteIds ) {
//      return patientMentionNoteIds.keySet()
//                                   .stream()
//                                   .collect( Collectors.groupingBy( Mention::getClassUri ) );
//   }
//
//   /**
//    * This essentially defines ConceptAggregates.  A ConceptAggregate URI and all of its mentions.
//    * @param bestUriToUrisMap Map of each "best" uri and the existing other uris that are its roots.
//    * @param uriMentionsMap Map of unique URIs to all Mentions with that exact URI.
//    * @return Map of all best Uris to all mentions with that best uri
//    */
//   static private Map<String,Collection<Mention>> createBestUriMentionsMap(
//         final Map<String, Collection<String>> bestUriToUrisMap,
//         final Map<String, List<Mention>> uriMentionsMap ) {
//      // Map of all best Uris to all mentions with that best uri
//      // A single Mention may exist in multiple best Uri collections.  See comment on bestUriToUrisMap.
//      final Map<String,Collection<Mention>> bestUriMentionsMap = new HashMap<>();
//      for ( Map.Entry<String,Collection<String>> bestUriToUris : bestUriToUrisMap.entrySet() ) {
//         final String bestUri = bestUriToUris.getKey();
//         for ( String uri : bestUriToUris.getValue() ) {
//            bestUriMentionsMap.computeIfAbsent( bestUri, m -> new HashSet<>() )
//                              .addAll( uriMentionsMap.get( uri ) );
//         }
//      }
//      return bestUriMentionsMap;
//   }
//
//   static private Map<String, Collection<Mention>> createDocIdMentionMap(
//         final Map<Mention, String> patientMentionNoteIds,
//         final Collection<Mention> mentions ) {
//      return mentions.stream()
//                     .collect( Collectors.groupingBy( patientMentionNoteIds::get,
//                                                      Collectors.toCollection(
//                                                            (Supplier<Collection<Mention>>) HashSet::new ) ) );
//   }
//
//   static private Collection<String> getSiteUris( final Collection<String> uris ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      final Collection<String> locationUris = UriConstants.getLocationUris( graphDb );
//      final Collection<String> siteUris = new HashSet<>( uris );
//      siteUris.retainAll( locationUris );
//      return siteUris;
//   }
//
//   static private Map<String,Collection<String>> createSiteBestUriToUrisMap(
//         final Map<String, Collection<String>> bestUriToUrisMap ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      final Collection<String> siteUris = UriConstants.getLocationUris( graphDb );
//      final Map<String,Collection<String>> siteBestUriToUrisMap = new HashMap<>( bestUriToUrisMap );
//      siteBestUriToUrisMap.keySet().retainAll( siteUris );
//      return siteBestUriToUrisMap;
//   }
//
//
//   static private Collection<ConceptAggregate> createSiteConceptAggregates(
//         final String patientId,
//         final Map<Mention, String> patientMentionNoteIds,
//         final Map<String, Collection<String>> bestUriToUrisMap,
//         final Map<String,Collection<Mention>> bestUriMentionsMap ) {
//      // Get roots of all uris here to prevent repeated lookup for mentions in different chains.
//      final Map<String, Collection<String>> allUriRoots = UriUtil.mapUriRoots( bestUriToUrisMap.keySet() );
//
//      final Collection<ConceptAggregate> conceptAggregates = new HashSet<>( bestUriToUrisMap.size() );
//      for ( Map.Entry<String,Collection<String>> bestUriToUris : bestUriToUrisMap.entrySet() ) {
//         final Map<String,Collection<String>> uriRootsMap = new HashMap<>( allUriRoots );
//         uriRootsMap.keySet()
//                    .retainAll( bestUriToUris.getValue() );
//         final Map<String, Collection<Mention>> docIdMentionMap = createDocIdMentionMap(
//               patientMentionNoteIds,
//               bestUriMentionsMap.get( bestUriToUris.getKey() ) );
//         conceptAggregates.add( new DefaultConceptAggregate( patientId, uriRootsMap, docIdMentionMap ) );
//      }
//      return conceptAggregates;
//   }
//
//
//   static private Collection<ConceptAggregate> createConceptAggregates(
//         final String patientId,
//         final Map<Mention, String> patientMentionNoteIds,
//         final Map<String, Collection<String>> bestUriToUrisMap,
//         final Map<String,Collection<Mention>> bestUriMentionsMap ) {
//      // Get roots of all uris here to prevent repeated lookup for mentions in different chains.
//      final Map<String, Collection<String>> allUriRoots = UriUtil.mapUriRoots( bestUriToUrisMap.keySet() );
//
//      final Collection<ConceptAggregate> conceptAggregates = new HashSet<>( bestUriToUrisMap.size() );
//      for ( Map.Entry<String,Collection<String>> bestUriToUris : bestUriToUrisMap.entrySet() ) {
//         final Map<String,Collection<String>> uriRootsMap = new HashMap<>( allUriRoots );
//         uriRootsMap.keySet()
//                    .retainAll( bestUriToUris.getValue() );
//         final Map<String, Collection<Mention>> docIdMentionMap = createDocIdMentionMap(
//               patientMentionNoteIds,
//               bestUriMentionsMap.get( bestUriToUris.getKey() ) );
//         conceptAggregates.add( new DefaultConceptAggregate( patientId, uriRootsMap, docIdMentionMap ) );
//      }
//      return conceptAggregates;
//   }
//
//   static private Map<String,Collection<ConceptAggregate>> createMentionIdConceptsMap(
//         final Collection<ConceptAggregate> conceptAggregates ) {
//      final Map<String,Collection<ConceptAggregate>> mentionIdConceptsMap = new HashMap<>();
//      for ( ConceptAggregate conceptAggregate : conceptAggregates ) {
//         for ( Mention mention : conceptAggregate.getMentions() ) {
//            mentionIdConceptsMap.computeIfAbsent( mention.getId(), c -> new HashSet<>() )
//                                .add( conceptAggregate );
//         }
//      }
//      return mentionIdConceptsMap;
//   }
//
//
//   static private Map<ConceptAggregate,Collection<Mention>> createConceptSitedMentionsMap(
//         final Collection<MentionRelation> mentionRelations,
//         final Map<String, Mention> idToMentionMap,
//         final Map<String,Collection<ConceptAggregate>> mentionIdSiteConceptsMap ) {
//      final Map<ConceptAggregate,Collection<Mention>> conceptSitedMentionsMap = new HashMap<>();
//      for ( MentionRelation relation : mentionRelations ) {
//         if ( RelationConstants.isHasSiteRelation( relation.getType() ) ) {
//            final Mention located = idToMentionMap.get( relation.getSourceId() );
//            final Collection<ConceptAggregate> sites = mentionIdSiteConceptsMap.get( relation.getTargetId() );
//            sites.forEach( s -> conceptSitedMentionsMap.computeIfAbsent( s, c -> new HashSet<>() )
//                                                       .add( located ) );
//         }
//      }
//      return conceptSitedMentionsMap;
//   }
//
//   static private Map<ConceptAggregate,Collection<Mention>> createConceptLateraledMentionsMap(
//         final Collection<MentionRelation> mentionRelations,
//         final Map<String, Mention> idToMentionMap,
//         final Map<String,Collection<ConceptAggregate>> mentionIdLateralConceptsMap ) {
//      final Map<ConceptAggregate,Collection<Mention>> conceptLateraledMentionsMap = new HashMap<>();
//      for ( MentionRelation relation : mentionRelations ) {
//         if ( RelationConstants.HAS_LATERALITY.equals( relation.getType() ) ) {
//            final Mention lateraled = idToMentionMap.get( relation.getSourceId() );
//            final Collection<ConceptAggregate> laterality = mentionIdLateralConceptsMap.get( relation.getTargetId() );
//            laterality.forEach( l -> conceptLateraledMentionsMap.computeIfAbsent( l, c -> new HashSet<>() )
//                                                                .add( lateraled ) );
//         }
//      }
//      return conceptLateraledMentionsMap;
//   }
//
//
//   /**
//    * @param patientId             -
//    * @param patientMentionNoteIds -
//    * @param mentionRelations      -
//    * @return map of best uri to its concept instances
//    */
//   static public Map<String, Collection<ConceptAggregate>> createUriConceptAggregateMap(
//         final String patientId,
//         final Map<Mention, String> patientMentionNoteIds,
//         final Collection<MentionRelation> mentionRelations ) {
//
//      // Map of unique URIs to all Mentions with that exact URI.
//      final Map<String, List<Mention>> uriMentionsMap
//            = createUriToMentionsMap( patientMentionNoteIds );
//
//      final Map<String, Mention> idToMentionMap
//            = patientMentionNoteIds.keySet()
//                                   .stream()
//                                   .collect( Collectors.toMap( Mention::getId, Function.identity() ) );
//
//      //   Step 1:
//      //> Create and merge Site concepts.
//      //> Sort non-site uris based upon merged sites.
//
//      final Collection<String> siteUris = getSiteUris( uriMentionsMap.keySet() );
//
//      final Map<String,Collection<String>> siteBestUriToUrisMap
//            = UriUtil.getAssociatedUriMap( siteUris );
//
//      // Map of all best Uris to all mentions with that best uri
//      final Map<String,Collection<Mention>> siteBestUriMentionsMap
//            = createBestUriMentionsMap( siteBestUriToUrisMap, uriMentionsMap );
//
//      // Create Site ConceptAggregates.
//      final Collection<ConceptAggregate> siteConcepts
//            = createConceptAggregates( patientId, patientMentionNoteIds, siteBestUriToUrisMap, siteBestUriMentionsMap );
//
//      final Map<String,Collection<ConceptAggregate>> mentionIdSiteConceptsMap
//            = createMentionIdConceptsMap( siteConcepts );
//
//      final Map<ConceptAggregate,Collection<Mention>> conceptSitedMentionsMap
//            = createConceptSitedMentionsMap( mentionRelations, idToMentionMap, mentionIdSiteConceptsMap );
//
//
//      //   Step 2:
//      //> Sort by Laterality.  For any with no laterality assign to same-site type the dominant (most Mentions) existing
//      // with laterality.
//      //> For any same-cancer/mass concepts with 2 lateralities, if one laterality is dominant add cancer attributes to the
//      // concept with dominant laterality.
//      // Create Laterality ConceptAggregates.
//      final Map<String,Collection<String>> lateralityBestUriToUrisMap
//            = UriUtil.getAssociatedUriMap( LATERALITY_URIS );
//
//      // Map of all best Uris to all mentions with that best uri
//      final Map<String,Collection<Mention>> lateralityBestUriMentionsMap
//            = createBestUriMentionsMap( lateralityBestUriToUrisMap, uriMentionsMap );
//
//      // Create ConceptAggregates.
//      final Collection<ConceptAggregate> lateralityConcepts
//            = createConceptAggregates( patientId,
//                                       patientMentionNoteIds,
//                                       lateralityBestUriToUrisMap,
//                                       lateralityBestUriMentionsMap );
//
//      final Map<String,Collection<ConceptAggregate>> mentionIdLateralityConceptsMap
//            = createMentionIdConceptsMap( lateralityConcepts );
//
//      final Map<ConceptAggregate,Collection<Mention>> conceptLateraledMentionsMap
//            = createConceptLateraledMentionsMap( mentionRelations, idToMentionMap, mentionIdLateralityConceptsMap );
//
//
//      final Map<String, List<Mention>> nonSiteUriMentionsMap = new HashMap<>( uriMentionsMap );
//      nonSiteUriMentionsMap.keySet().removeAll( siteBestUriToUrisMap.keySet() );
//      nonSiteUriMentionsMap.keySet().removeAll( LATERALITY_URIS );
//
//      // Have Mentions by site in Map<ConceptAggregate,Collection<Mention>> conceptSitedMentionsMap
//      // Have Mentions by laterality in Map<ConceptAggregate,Collection<Mention>> conceptLateraledMentionsMap
//      // Have Mentions by uri that are not site or laterality in Map<String, List<Mention>> nonSiteUriMentionsMap
//
//      // Build list of sited mentions, put all non-sited mentions in NO_SITE_CONCEPT
//      // Build list of lateraled mentions, put all non-lateraled in NO_LATERALITY_CONCEPT
//
//      // Create map of <LateralityConcept,Map<SiteConcept,Collection<Mentions>>
//      // for Mention in conceptSitedMentionsMap.values, get all lateralities.
//      // For each laterality computeIfAbsent( site, new set() ).add( mention )
//
//      // Now have map of Laterality and Site to mentions with both that laterality and site.
//
//      // for those divided mentions, divvy the mentions in non-sited and non-lateraled ...
//      // First Go through non-lateral, non-sited.  For each mention, check the uri.
//      // Go through all the non-lateral sited mention lists and if any are the same uri then move the mention into that
//      // site list.
//      // Next go through the non-lateral, all sited mentions.  If there is only one lateral version of the same site,
//      // move all of the non-lateral sited mentions there.  If there are multiple lateral versions of the same site,
//      // count mentions for each laterality & site list for each mention uri.  Copy the uri mention into the larger
//      // list.  If
//
//      // For each non-site mention, place related site mentions in one or more laterality bins.
//      // -- in separate laterality bins put the mention.
//      // -- in separate
//
//      // -- in each laterality bin, do a bestUri:Uris and make separate ConceptAggregates for each laterality bin.
//
//      // Copy the site uris from the no laterality bin into all of the laterality bins.
//      // For each laterality bin create a map of bestUri:Uris, then conceptAggregates for each laterality bin.
//
//
//      //-------------------------------------------
//      //      This will be slow, but it might be best overall.   Possibly use primary/associated site first, then
//      //      add region and then cavity.
//      // 1. Go through each mention.  If a mention has a laterality,
//      // put a map of <mention,Collection<Site>> for that laterality.   If it has multiple lateralities, fine.
//      // If it has no laterality, use a "no laterality" bin.
//      // --> 1.5 in all laterality bins create a mention bestUri:Uris map.
//      // --> 1.6 in all laterality bins for each mention bestUri collection of sites make a bestUri:uris
//      // map.
//      // --> 1.8  Make an all-bin, all-site bestUri:bestUriRoots map.  make an all-bin, all-mention bestUri:roots map.
//
//      // 2. For the no laterality bin, create a map of
//      // bestUri:collection<bestSiteUris>:collection<laterality:Map<bestUri>:Collection<bestSiteUris>>.
////      // - also create a map of exact match -^
////      //     -- Check bestMentionUri : lateral bestMentionUri , bestSiteUri : lateral bestSiteUri
////      //     --> for each laterality match, add bestUri and lateralities
////      //     to exact match map bestUri:collection<bestSiteUris>:collection<laterality:Map<bestUri>:Collection
////      //     <bestSiteUris>>.
//      //     -- Check bestMentionUri : lateral bestMentionRoots , bestSiteUri : lateral bestSiteRoots
//      //     --> for each laterality match, add bestUri and lateralities
//      //     to bestUri:collection<bestSiteUris>:collection<laterality:Map<bestUri>:Collection<bestSiteUris>>.
//      //     -- Check bestMentionUri : lateral bestMentionRoots , bestSiteRoots : lateral bestSiteUri
//      //     --> for each laterality match, add bestUri and lateralities
//      //     to bestUri:collection<bestSiteUris>:collection<laterality:Map<bestUri>:Collection<bestSiteUris>>.
//      //     -- Check bestMentionRoots : lateral bestMentionUri , bestSiteUri : lateral bestSiteRoots
//      //     --> for each laterality match, add bestUri and lateralities
//      //     to bestUri:collection<bestSiteUris>:collection<laterality:Map<bestUri>:Collection<bestSiteUris>>.
//      //     -- Check bestMentionRoots : lateral bestMentionUri , bestSiteRoots : lateral bestSiteUri
//      //     --> for each laterality match, add bestUri and lateralities
//      //     to bestUri:collection<bestSiteUris>:collection<laterality:Map<bestUri>:Collection<bestSiteUris>>.
//      // --> 2.5 Go through bestUri:collection<bestSiteUris>:collection<laterality:Map<bestUri>:Collection<bestSiteUris>>.
//      // For each bestUri:bestSiteUri with a single laterality, computeIfAbsent copy the bestUri into that laterality.
//      // Note lateralities that have changed.
//      // On each changed laterality, go through another round of mention bestUri: and mention siteBestUri (1.6)
//
//      // If there are laterality conflicts (e.g. left brca breast, right brca breast) then try to resolve them now.
//      // Go through 2., but only account for left vs. right.
//      // For each conflicting mention:site combo, compare mention counts left v. right.
//      // Copy lesser (threshold) mention:site combo information into greater mention:site combo.
//      // and remove from lesser laterality.
//      // ?? Do the same with bilateral vs. left and right?  Would need to do that first.  No, could be required.
//      // !! Do this previous to #2 !!!  That should be ok and save time.
//      // !! For this, only use exact mention uri that is a bestUri match and full site bestUri,roots match.
//      // !! - We don't want 10 left cancer trunk and 1 left brca breast to compete with 1 right cancer trunk, 5
//      // right brca breast.  brca breast would be moved to left instead of right.
//      // Done correctly, afterward we would have 10 left cancer trunk and 6 right brca breast and 1 right cancer trunk.
//      // --> because Cancer is not a bestUri in either laterality it would not be moved.
//      // If we also have 3 left lungCa lung then the 10 left cancer trunks would be subsumed there.
//      // While the 1 right cancer trunk would be subsumed by the right brca breast.
//
//      // ?? What about 10 left Ca Breast vs 1 right BrCa Breast ??
//      // Do another match of mention bestUri,to bestUriRoots and exact siteUri ?
//      // ?? What about 10 left Ca Nipple vs 1 right BrCa Breast ??
//      // Do one with left bestMention is a root of right bestMention and right site bestMention is a root of left
//      // site bestMention? In this case 10 left Ca Nipple < 1 right BrCa Breast and would NOT be moved.
//      // ? What if there was 3 left BrCa Breast 10 left Ca Nipple, 10 right BrCa Breast ?
//      // - 3 left BrCa Breast would be moved to right.  Even though left really has 13 left BrCa Nipple/Breast.
//
//      // !! Should also be checking site count
//      // ::  Left = 10 Ca 3 Br, 5 BrCa 3 Br, 1 DCIS 3 Br   ;  best = 16 DCIS 3 Br [1 DCIS, 5 BrCa, 10 Ca] 3 Br
//      // ::  Right = 1 Ca 3 Br, 5 BrCa 3 Br, 10 DCIS 3 Br  ;  best = 16 DCIS 3 Br [10 DCIS, 5 BrCa, 1 Ca] 3 Br
//      // ~~ Want No Change.
//      // ::  Right = 1 Ca 3 Br, 5 BrCa 3 Br, 5 DCIS 3 Br  ;  best = 11 DCIS 3 Br [5 DCIS, 5 BrCa, 1 Ca] 3 Br
//      // ~~ Want No Change.
//      // ::  Right = 1 Ca 3 Br, 1 BrCa 3 Br, 5 DCIS 3 Br  ;  best = 7 DCIS 3 Br [5 DCIS, 1 BrCa, 1 Ca] 3 Br
//      // ~~ Want No Change.  Still below some threshold ( 16 v 7 ).  Maybe threshold of 1:4 ?
//
//      // !! What about removing any rootUri in a bestUri:Uris chain that is in 2+ bestUri:Uris chains?
//      // BrCa:[BrCa,Ca] , CaP:[CaP,Ca] ==>  BrCa:[BrCa] , CaP:[CaP] , Ca:[Ca]
//      // That should easily change the bestUri:Uris counts.
//      // It can be done with both sides.
//      // ::  Left = 10 Ca 3 Br, 5 BrCa 3 Br, 1 DCIS 3 Br, 5 CaP 3 Br
//      // ;  best = 6 DCIS 3 Br [1 DCIS, 5 BrCa] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  10 Ca 3 Br [10 Ca] 3 Br
//      // ::  Right = 1 Ca 3 Br, 5 BrCa 3 Br, 10 DCIS 3 Br, 5 CaP 3 Br
//      // ;  best = 15 DCIS 3 Br [10 DCIS, 5 BrCa] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  1 Ca 3 Br [1 Ca] 3 Br
//      // ~~ In this case we may want to put Left 6 DCIS into Right 15 DCIS.
//      // If we also removed mentions common to both sides:
//      // ;  best = 1 DCIS 3 Br [1 DCIS] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  10 Ca 3 Br [10 Ca] 3 Br,  5 BrCa[]..
//      // ;  best = 10 DCIS 3 Br [10 DCIS] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  1 Ca 3 Br [1 Ca] 3 Br,  5 BrCa[]..
//      // ~~ In this case we may want to move Left 1 DCIS into Right 10 DCIS.
//      // ~~ What happens to the 5 BrCa in each side?  In this case they would stay on each side, no movement.
//      // ^^ For above matches, can use site bestUri:UriRoots to match.
//
//      // !! Should create bestUri:Uris for sites first.  Then assign those to the mention Uris, then do mention map.
//      // So, Left [Br,Trunk]:[DCIS,BrCa,Ca] , [IDC,BrCa,Ca]    Right [Nipple,Br]:[DCIS,BrCa,Ca] , [IDC,BrCa,Ca]
//      // Site counts for each laterality should not matter.  Only Neoplasm counts per site match should matter.
//      // ?? Should it be a combination of exactUri and bestUri counts?
//      // e.g. Left #DCIS v Right #DCIS   -and-  Left #[DCIS,BrCa,Ca] v Right #[DCIS,BrCa,Ca]  ??
//      // If both are dominantly Left, move [DCIS,BrCa,Ca] to Left.  Otherwise leave alone.
//
//      // Map<Laterality, Map< Map<bestSiteUri,Collection<bestSites>>, Map<bestNeoplasmUri,Collection<bestNeoplasms>>>
//      // - To Simplify use a lateralityStore class.  Put maps and methods in there.
//      // 1. Split neoplasms into left, right, bilateral, noLaterality bins.
//      // 1.5 Create map of neoplasms to sites.
//      // 1.6 Distribute sites into laterality bins based upon related neoplasms.
//      // 2. In each laterality, Map bestSites.   bestSiteUri:[siteMentions]
//      // 3. In each laterality bestSite, Map neoplasms in the matching laterality bin related to the sites.
//      //      bestSiteUri:[siteMentions]::[neoplasmMentions]
//      // 4. For each collection of neoplasmMentions, map the bestUris.
//      //      bestSiteUri:[siteMentions]::[bestNeoUri:::[neoplasmMentions]]
//      // - To Simplify, map of bestSiteUri:[siteMentions], map of bestSiteUri::[bestNeoUri:[neoplasmMentions]]
//      // - make a set-aside map of common bestSiteUri:NeoUriMentions and remove them from the chains in multiple sites
//      // ^ That shouldn't happen too much except that we can get organ vs. region vs. cavity.
//      // Maybe do all this mapping by organ?  Put in "Placed List"  Then anything remaining do by region and cavity?
//      // If close tie between left and right, check for match in bilateral.  If there is a dominant match then move.
//      // After all left/right checks, DO NOT perform bilateral vs. left , bilateral vs. right.
//
//      // 5. For no laterality bin, try to match with left, right, bilateral.
//      // - check for bestSiteUri matches.
//      //
//
//
//
//      // Do another Mention bestUri:Uris :: bestSiteUri:siteUris.
//
//
//
//
//
//      // If there were multiple laterality matches, maybe this is a good time to cull/move the weaker laterality.
//      // Check each laterality
//      // bestUri:bestSiteUri for a max count, then "remove" the laterality combo with fewer mentions.
//      // --> Need to account for counts of mention and site mentions of the laterality.
//      // --> Need to account for non-exact bestUris in comparisons.  e.g. left brca breast, right cancer breast.
//      // 3. For each mention in the no laterality bin, get the roots.  Go through each laterality bin and check uris
//      // vs. roots and sites just like #2.  Move when matching root and any site.
//      // 4. For each mention in each laterality bin, get the roots.  Now go through the no laterality bin and check
//      // each mention uri for fit into a laterality bin mention's roots.  Move when appropriate.
//      // 5. Repeat #3, this time using exact mention uri and roots for sites.
//      // 6. Repeat #4, this time using exact mention uri and roots for sites.
//      // 7. Repeat a combo of #3, #4 where any mention roots match any site roots.
//      // 8. Repeat a combo of #5, #6 where any mention roots match any site roots.
//      // 9. For each laterality bin, collect all site uris and create site concept aggregates.
//      // --> for each laterality bin, re-sort mentions, assigning them to site concept aggregates.
//      // ! We now have mentions sorted by laterality and site.
//      // Hopefully enough matching has been done to get the best concept aggregates per laterality.
//
//
//
//      // Map of each "best" uri and the existing other uris that are its roots.
//      // A uri may have more than one "best" uri, e.g. Brain in:
//      //             * Occipital_Lobe : [Occipital_Lobe, Brain, Nervous_System]
//      //             * Parietal_Lobe : [Parietal_Lobe, Brain, Nervous_System]
//      final Map<String, Collection<String>> bestUriToUrisMap
//            = UriUtil.getAssociatedUriMap( uriMentionsMap.keySet() );
//
//      // Map of all best Uris to all mentions with that best uri
//      final Map<String,Collection<Mention>> bestUriMentionsMap
//            = createBestUriMentionsMap( bestUriToUrisMap, uriMentionsMap );
//
//
//      //   Step 1:
//      //> Create and merge Site concepts.
//      //> Sort non-site uris based upon merged sites.
//
////      final Map<ConceptAggregate,Collection<Mention>> conceptSitedMentionsMap
////            = createConceptSitedMentionsMap( mentionRelations, idToMentionMap, mentionIdSiteConceptsMap );
//
//      //   Step 2:
//      //> Sort by Laterality.  For any with no laterality assign to same-site type the dominant (most Mentions) existing
//      // with laterality.
//      //> For any same-cancer/mass concepts with 2 lateralities, if one laterality is dominant add cancer attributes to the
//      // concept with dominant laterality.
//      final Collection<String> lateralityUris= Arrays.asList( UriConstants.LEFT,
//                                                             UriConstants.RIGHT,
//                                                             UriConstants.BILATERAL );
//      // Create Laterality ConceptAggregates.
//      final Map<String,Collection<String>> lateralityBestUriToUrisMap = new HashMap<>( bestUriToUrisMap );
//      lateralityBestUriToUrisMap.keySet().retainAll( lateralityUris );
//
//      // Create ConceptAggregates.
//      final Collection<ConceptAggregate> lateralityConcepts
//            = createConceptAggregates( patientId,
//                                       patientMentionNoteIds,
//                                       lateralityBestUriToUrisMap,
//                                       bestUriMentionsMap );
//
//      final Map<String,Collection<ConceptAggregate>> mentionIdLateralityConceptsMap
//            = createMentionIdConceptsMap( lateralityConcepts );
//
//      final Map<ConceptAggregate,Collection<Mention>> conceptLateraledMentionsMap
//            = createConceptLateraledMentionsMap( mentionRelations, idToMentionMap, mentionIdLateralityConceptsMap );
//
//      final Collection<String> nonLocationBestUris = new HashSet<>( bestUriToUrisMap.keySet() );
//      nonLocationBestUris.removeAll( siteBestUriToUrisMap.keySet() );
//      nonLocationBestUris.removeAll( lateralityUris );
//
//
//
//
//
//
//
//
//
////      final Map<String, Mention> idToMentionMap
////            = patientMentionNoteIds.keySet()
////                                   .stream()
////                                   .collect( Collectors.toMap( Mention::getId, Function.identity() ) );
//
//
//     // Create ConceptAggregates.
//      // Create map of MentionID to Collection<ConceptAggregate>
//
//      // Iterate over MentionRelations, for each mentionID get all the ConceptAggregates for that MentionID,
//      // Add Relation of ConceptAggregate to ConceptAggregate.  addRelated( String type, ConceptAggregate related )
//
//
//      // Create map of Non-Site Mentions to multiple site mentions.
//      // Iterate through relations, if relation is a hasSite type and source is not a site,
//      // target ID to
//
//
//
//
//
//
////      LOGGER.info( "\nMap of unique xDoc URIs to URIs that are associated (e.g. same branch)." );
////      associatedUrisMap.forEach( (k,v) -> LOGGER.info( k + ": (" + String.join( ",", v ) + ")" ) );
//
//
////      final Map<Mention, Collection<String>> locationUris = new HashMap<>();
////      "Disease_Has_Primary_Anatomic_Site", "Disease_Has_Associated_Anatomic_Site", "Disease_Has_Metastatic_Anatomic_Site",
////      "Disease_Has_Associated_Region", "Disease_Has_Associated_Cavity",
////      "Finding_Has_Associated_Site", "Finding_Has_Associated_Region", "Finding_Has_Associated_Cavity"
//      final Map<String, Map<Mention, Collection<String>>> typeLocationUris = new HashMap<>();
//      final Map<Mention, Collection<String>> lateralityUris = new HashMap<>();
////      buildPlacements( patientRelations, mentionIdMap, locationUris, lateralityUris );
//      buildPlacements( patientRelations, idToMentionMap, typeLocationUris, lateralityUris );
//
//
////      LOGGER.info( "!!!    Determined Locations for all Mentions." );
////      locationUris.forEach( (k,v) -> LOGGER.info( "Mention " + k.getClassUri() +" "+ k.getId() + " at (" + String.join( ",", v ) + ")" ) );
////      lateralityUris.forEach( (k,v) -> LOGGER.info( "Mention " + k.getClassUri() +" "+ k.getId() + " on (" + String.join( ",", v ) + ")" ) );
//
//
//      // Get roots of all uris here to prevent repeated lookup for mentions in different chains.
//      final Map<String, Collection<String>> allUriRoots = UriUtil.mapUriRoots( uriMentionsMap.keySet() );
//
//      final Map<String, Collection<ConceptAggregate>> conceptAggregates = new HashMap<>();
//      final Collection<Mention> usedMentions = new ArrayList<>();
//
////      for ( Collection<String> finalBranch : finalBranches ) {
//      for ( Collection<String> finalBranch : bestUriToUrisMap.values() ) {
//         final Collection<Mention> mentionGroup = finalBranch.stream()
//                                                             .map( uriMentionsMap::get )
//                                                             .flatMap( Collection::stream )
//                                                             .collect( Collectors.toSet() );
//         final List<List<Mention>> chains = new ArrayList<>();
////         MentionUtil.collateCoref( chains, mentionGroup, locationUris, lateralityUris );
//         MentionUtil.collateCoref( chains, mentionGroup, typeLocationUris, lateralityUris );
//         for ( List<Mention> chain : chains ) {
//            if ( chain.size() > 1 ) {
//               final Map<String, Collection<Mention>> noteIdMentionsMap = new HashMap<>();
//               for ( Mention mention : chain ) {
//                  noteIdMentionsMap.computeIfAbsent( patientMentionNoteIds.get( mention ), d -> new HashSet<>() )
//                                   .add( mention );
//               }
//               // Create a concept aggregate with each annotation assigned to the appropriate docId.
//               final Collection<String> uris = chain.stream()
//                                                    .map( Mention::getClassUri )
//                                                    .collect( Collectors.toSet() );
//               // smaller map of uris to roots that only contains pertinent uris.
//               final Map<String, Collection<String>> uriRoots = new HashMap<>( allUriRoots );
//               uriRoots.keySet()
//                       .retainAll( uris );
//               final ConceptAggregate concept
//                     = new DefaultConceptAggregate( patientId, uriRoots, noteIdMentionsMap );
//
//
//               LOGGER.info(
//                     "Created " + chain.size() + "mention ConceptAggregate " + concept.getUri() + " " + concept.getId() + " "
//                     + "scored:"
//                     + " " + concept.getUriScore() );
////               uriRoots.forEach( (k,v) -> LOGGER.info( "URI " + k + " with -unordered- root URIs (" + String.join( ",", v ) + ")" ) );
//
//
//               conceptAggregates.computeIfAbsent( concept.getUri(), ci -> new ArrayList<>() )
//                                .add( concept );
//               usedMentions.addAll( chain );
//            }
//         }
//      }
//      patientMentionNoteIds.keySet()
//                           .removeAll( usedMentions );
//      for ( Map.Entry<Mention, String> mentionNoteId : patientMentionNoteIds.entrySet() ) {
//         final String bestUri = mentionNoteId.getKey()
//                                             .getClassUri();
//         if ( bestUri.isEmpty() ) {
//            continue;
//         }
////         final Map<String,Collection<String>> bestUriMap = new HashMap<>( 1 );
////          bestUriMap.put( bestUri, Collections.singletonList( bestUri ) );
//         final Map<String, Collection<String>> uriRoots = new HashMap<>( 1 );
//         uriRoots.put( bestUri, allUriRoots.get( bestUri ) );
//
//         conceptAggregates
//               .computeIfAbsent( bestUri, ci -> new HashSet<>() )
//               .add( new DefaultConceptAggregate( patientId,
//                                                  uriRoots,
//                                                  Collections.singletonMap( mentionNoteId.getValue(),
//                                                                            Collections.singletonList(
//                                                                                  mentionNoteId.getKey() ) ) ) );
//
//
//         LOGGER.info( "Created Simple ConceptAggregate of " + bestUri );
//
//
//      }
//
//   }
//
//
//
//
//   // ?? What about 10 left Ca Breast vs 1 right BrCa Breast ??
//   // ?? What about 10 left Ca Nipple vs 1 right BrCa Breast ??
//   // Do one with left bestMention is a root of right bestMention and right site bestMention is a root of left
//   // site bestMention? In this case 10 left Ca Nipple < 1 right BrCa Breast and would NOT be moved.
//   // ? What if there was 3 left BrCa Breast 10 left Ca Nipple, 10 right BrCa Breast ?
//   // - 3 left BrCa Breast would be moved to right.  Even though left really has 13 left BrCa Nipple/Breast.
//
//   // !! Should also be checking site count
//   // ::  Left = 10 Ca 3 Br, 5 BrCa 3 Br, 1 DCIS 3 Br   ;  best = 16 DCIS 3 Br [1 DCIS, 5 BrCa, 10 Ca] 3 Br
//   // ::  Right = 1 Ca 3 Br, 5 BrCa 3 Br, 10 DCIS 3 Br  ;  best = 16 DCIS 3 Br [10 DCIS, 5 BrCa, 1 Ca] 3 Br
//   // ~~ Want No Change.
//   // ::  Right = 1 Ca 3 Br, 5 BrCa 3 Br, 5 DCIS 3 Br  ;  best = 11 DCIS 3 Br [5 DCIS, 5 BrCa, 1 Ca] 3 Br
//   // ~~ Want No Change.
//   // ::  Right = 1 Ca 3 Br, 1 BrCa 3 Br, 5 DCIS 3 Br  ;  best = 7 DCIS 3 Br [5 DCIS, 1 BrCa, 1 Ca] 3 Br
//   // ~~ Want No Change.  Still below some threshold ( 16 v 7 ).  Maybe threshold of 1:4 ?
//
//   // !! What about removing any rootUri in a bestUri:Uris chain that is in 2+ bestUri:Uris chains?
//   // BrCa:[BrCa,Ca] , CaP:[CaP,Ca] ==>  BrCa:[BrCa] , CaP:[CaP] , Ca:[Ca]
//   // That should easily change the bestUri:Uris counts.
//   // It can be done with both sides.
//   // ::  Left = 10 Ca 3 Br, 5 BrCa 3 Br, 1 DCIS 3 Br, 5 CaP 3 Br
//   // ;  best = 6 DCIS 3 Br [1 DCIS, 5 BrCa] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  10 Ca 3 Br [10 Ca] 3 Br
//   // ::  Right = 1 Ca 3 Br, 5 BrCa 3 Br, 10 DCIS 3 Br, 5 CaP 3 Br
//   // ;  best = 15 DCIS 3 Br [10 DCIS, 5 BrCa] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  1 Ca 3 Br [1 Ca] 3 Br
//   // ~~ In this case we may want to put Left 6 DCIS into Right 15 DCIS.
//   // If we also removed mentions common to both sides:
//   // ;  best = 1 DCIS 3 Br [1 DCIS] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  10 Ca 3 Br [10 Ca] 3 Br,  5 BrCa[]..
//   // ;  best = 10 DCIS 3 Br [10 DCIS] 3 Br  ,  5 CaP 3 Br [5 CaP] 3 Br  ,  1 Ca 3 Br [1 Ca] 3 Br,  5 BrCa[]..
//   // ~~ In this case we may want to move Left 1 DCIS into Right 10 DCIS.
//   // ~~ What happens to the 5 BrCa in each side?  In this case they would stay on each side, no movement.
//   // ^^ For above matches, can use site bestUri:UriRoots to match.
//
//   // !! Should create bestUri:Uris for sites first.  Then assign those to the mention Uris, then do mention map.
//   // So, Left [Br,Trunk]:[DCIS,BrCa,Ca] , [IDC,BrCa,Ca]    Right [Nipple,Br]:[DCIS,BrCa,Ca] , [IDC,BrCa,Ca]
//   // Site counts for each laterality should not matter.  Only Neoplasm counts per site match should matter.
//   // ?? Should it be a combination of exactUri and bestUri counts?
//   // e.g. Left #DCIS v Right #DCIS   -and-  Left #[DCIS,BrCa,Ca] v Right #[DCIS,BrCa,Ca]  ??
//   // If both are dominantly Left, move [DCIS,BrCa,Ca] to Left.  Otherwise leave alone.
//
//
//   //  0. Negation bin (neg)
//   //  1. Laterality bin (lat)
//   //  2. Site bins, by organ vs. region.
//   //  !! Procedures and non-mass findings might be both left and right.
//   //  That is ok, can use one pass as bestUri chains should take care of the difference.
//   //  3. Site (Organ) BestUri Map
//   //  4. Mass, Cancer (N,neoplasm) bestUri Map.
//   //  5. For each N bestUri in a lat, get site.
//   //  6. get N site bestUri.
//   //  7. Get count of N bestUri exact with site bestUri chain.
//   //  8. Move lesser count N bestUri exact mentions to higher count lat with matching site chain.
//   //  9. Move any bestUri not moved to a "handle later" bin.  Otherwise repeat in #10 will infinite loop.
//   // 10. REPEAT #3 - #9.  This -should- move mentions for each N to the appropriate lat.
//
//   // 11. Move mentions from "handle later" back into the candidate bins.
//   // 12. Repeat #3 - #9 with regions.
//
//   // 13. Make N bestUri chains.
//   // 14. Attempt to match bestUri chains by site (organ) chains.
//   // 15. Move lesser chains to other lat.
//   // 16. Move any bestUri not moved to a "handle later" bin.  Otherwise repeat in #13 will infinite loop.
//   // 17. Move mentions from "handle later" back into the candidate bins.
//   // 18. Repeat #13 - #16
//
//   // 17. Move mentions from "handle later" back into the candidate/primary bins.
//
//   // !!. Should now have resolved best left,right N chains.  Any remaining conflicts ... oh well.
//
//   // 18. Perform modified #3 - 17 with no-laterality bin vs. left, right, bilateral bins.
//   // !!. Note that left,right were not distributed into bilateral bin.
//   // Since bilateral is rare the FN caused by more left, right than bilat. should be acceptable.
//
//   // !!. Should now have resolved best left, right, bilat, no lat N chains.  Any remaining conflicts ... oh well.
//
//   // 19. Create new bestChain N, site, * in all lat bins.
//   // 20. Create ConceptAggregates per bestChain.
//   // 21. For any "hasDiagnosis", copy attributes from the diagnosis concept to hasDiagnosis concept and vice-versa.
//   // !!. Also hasTumorExtent and any other N to N relations.
//   // 22. Across all lat bins, "remove" any lesser C, M ConceptAggregate, (send to "unwanted" bin).
//   // ^^. Copying any attributes to ConceptAggregates with common mentions in their chain.
//   // !!. Do for cancer and mass separately.
//   // 24. Gather everything from affirmed laterality bins.  Those are the final ConceptAggregates.
//
//
//
//
//
//   static public final Collection<String> ORGAN_URIS = new HashSet<>();
//   static {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      ORGAN_URIS.addAll( SearchUtil.getBranchUris( graphDb, UriConstants.ORGAN ) );
//      ORGAN_URIS.addAll( SearchUtil.getBranchUris( graphDb, UriConstants.ORGAN_PART ) );
//   }
//
//   private Map<String, LateralityBinOld> _lateralityBinMap = new HashMap<>( 4 );
//
//
////   @Immutable
////   ConceptAggregate
////         NO_SITE_CONCEPT = new ConceptAggregate() {
////      @Override
////      public String getUri() {
////         return UriConstants.UNKNOWN;
////      }
////
////      @Override
////      public Map<String,Collection<String>> getUriRootsMap() {
////         return Collections.emptyMap();
////      }
////
////      @Override
////      public double getUriScore() {
////         return 0.0;
////      }
////      @Override
////      public List<KeyValue<String, Double>> getUriQuotients() {
////         return Collections.emptyList();
////      }
////      @Override
////      public Map<Mention, String> getNoteIdMap() {
////         return Collections.emptyMap();
////      }
////
////      @Override
////      public String getPreferredText() {
////         return "";
////      }
////
////      @Override
////      public String getPatientId() {
////         return "";
////      }
////
////      @Override
////      public String getJoinedNoteId() {
////         return "";
////      }
////
////      @Override
////      public Date getNoteDate( final Mention mention ) {
////         return null;
////      }
////
////      @Override
////      public String getCoveredText() {
////         return "";
////      }
////
////      @Override
////      public Map<String, Collection<ConceptAggregate>> getRelatedConceptMap() {
////         return null;
////      }
////
////      @Override
////      public void clearRelations() {
////      }
////
////      @Override
////      public void addRelated( final String type, final ConceptAggregate related ) {
////      }
////
////      @Override
////      public String getId() {
////         return "NO_SITE_CONCEPT";
////      }
////   };
////
////
////   @Immutable
////   ConceptAggregate
////         NO_LATERALITY_CONCEPT = new ConceptAggregate() {
////      @Override
////      public String getUri() {
////         return UriConstants.UNKNOWN;
////      }
////
////      @Override
////      public Map<String,Collection<String>> getUriRootsMap() {
////         return Collections.emptyMap();
////      }
////
////      @Override
////      public double getUriScore() {
////         return 0.0;
////      }
////      @Override
////      public List<KeyValue<String, Double>> getUriQuotients() {
////         return Collections.emptyList();
////      }
////      @Override
////      public Map<Mention, String> getNoteIdMap() {
////         return Collections.emptyMap();
////      }
////
////      @Override
////      public String getPreferredText() {
////         return "";
////      }
////
////      @Override
////      public String getPatientId() {
////         return "";
////      }
////
////      @Override
////      public String getJoinedNoteId() {
////         return "";
////      }
////
////      @Override
////      public Date getNoteDate( final Mention mention ) {
////         return null;
////      }
////
////      @Override
////      public String getCoveredText() {
////         return "";
////      }
////
////      @Override
////      public Map<String, Collection<ConceptAggregate>> getRelatedConceptMap() {
////         return null;
////      }
////
////      @Override
////      public void clearRelations() {
////      }
////
////      @Override
////      public void addRelated( final String type, final ConceptAggregate related ) {
////      }
////
////      @Override
////      public String getId() {
////         return "NO_LATERALITY_CONCEPT";
////      }
////   };
//
//
//
//
//}