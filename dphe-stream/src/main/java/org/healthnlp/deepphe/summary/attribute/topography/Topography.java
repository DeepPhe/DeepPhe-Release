package org.healthnlp.deepphe.summary.attribute.topography;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.TopoMorphValidator;
import org.healthnlp.deepphe.util.UriScoreUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.EvidenceLevel.*;
import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.*;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.*;
import static org.healthnlp.deepphe.summary.attribute.util.UriMapUtil.*;



public class Topography implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "Topography" );

   private String _bestUri = "";
   private String _bestMajorTopoCode = "";
   private Collection<String> _firstSiteMainUris;
   private Collection<String> _firstSiteAllUris;
   private Collection<String> _topographyCodes;
   final private NeoplasmAttribute _majorTopography;
//   final private NeoplasmAttribute _minorTopography;


   public Topography( final ConceptAggregate neoplasm,
                      final Collection<ConceptAggregate> allConcepts ) {
      _majorTopography = createMajorTopoAttribute( neoplasm, allConcepts );
//      _minorTopography = createMinorTopoAttribute();
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _majorTopography;
   }

//   public NeoplasmAttribute getMinorTopography() {
//      return _minorTopography;
//   }

   public String getMajorSiteUri() {
      return _bestUri;
   }

//   public String getMinorSiteUri() {
//      String specificUri = "";
//      try {
//         // TODO try _firstSiteAllUris and see if there are better results
//         specificUri = UriUtil.getMostSpecificUri( _firstSiteMainUris );
//      } catch ( NullPointerException npE ) {
//         //
//      }
//      return specificUri;
//   }

   public Collection<String> getFirstSiteMainUris() {
      return _firstSiteMainUris;
   }

   public Collection<String> getFirstSiteAllUris() {
      return _firstSiteAllUris;
   }

   public Collection<String> getTopographyCodes() {
      return _topographyCodes;
   }

   public String getBestMajorTopoCode() {
      return _bestMajorTopoCode;
   }

   static private String getMajorTopoCode( final Collection<String> topographyCodes ) {
      if ( topographyCodes.isEmpty() ) {
//         LOGGER.info( "No sites, using C80." );
         return "C80";
      }
      final Function<String, String> getMajorCode = t -> {
         final int dot = t.indexOf( '.' );
         return dot > 0
                ? t.substring( 0, dot )
                : t;
      };
      return topographyCodes.stream()
                            .map( getMajorCode )
                            .distinct()
                            .sorted()
                            .collect( Collectors.joining( ";" ) );
   }

//   private NeoplasmAttribute createMinorTopoAttribute() {
//      final List<Integer> minorFeatures = new ArrayList<>( _majorTopography.getConfidenceFeatures() );
//      minorFeatures.addAll( createMinorFeatures( _topographyCodes ) );
//      return SpecificAttribute.createAttribute( "topography_minor",
//                                                getBestMinorTopoCode(),
//                                                _bestUri,
//                                                _majorTopography.getDirectEvidence(),
//                                                _majorTopography.getIndirectEvidence(),
//                                                _majorTopography.getNotEvidence(),
//                                                minorFeatures );
//   }

//   public String getBestMinorTopoCode() {
//      return getMinorTopoCode( _topographyCodes );
//   }
//
//   static private String getMinorTopoCode( final Collection<String> topographyCodes ) {
//      if ( topographyCodes.isEmpty() ) {
//         return "9";
//      }
//      final Function<String, String> getMinor = t -> {
//         final int dot = t.indexOf( '.' );
//         return dot > 0 ? t.substring( dot + 1 ) : "";
//      };
//      final Collection<String> allMinors = topographyCodes.stream()
//                                                          .map( getMinor )
//                                                          .filter( t -> !t.isEmpty() )
//                                                          .distinct()
//                                                          .sorted()
//                                                          .collect( Collectors.toList() );
//      if ( allMinors.size() > 1 ) {
//         allMinors.remove( "9" );
//      }
//      String minors = String.join( ";", allMinors );
//      if ( minors.isEmpty() ) {
////         LOGGER.info( "No specific site codes, using 9." );
//         return "9";
//      }
//      return minors;
//   }
//
//   static private List<Integer> createMinorFeatures( final Collection<String> topographyCodes ) {
//      final List<Integer> features = new ArrayList<>( 2 );
//      final Function<String, String> getMinor = t -> {
//         final int dot = t.indexOf( '.' );
//         return dot > 0 ? t.substring( dot + 1 ) : "";
//      };
//      final Collection<String> allMinors = topographyCodes.stream()
//                                                          .map( getMinor )
//                                                          .filter( t -> !t.isEmpty() )
//                                                          .distinct()
//                                                          .sorted()
//                                                          .collect( Collectors.toList() );
//      if ( allMinors.size() > 1 ) {
//         allMinors.remove( "9" );
//      }
//      final boolean default9 = allMinors.isEmpty();
//      addCollectionFeatures( features, topographyCodes );
//      addCollectionFeatures( features, allMinors );
//      addBooleanFeatures( features, default9 );
//      return features;
//   }
static private String toConceptText( final ConceptAggregate concept ) {
   final StringBuilder sb = new StringBuilder();
   sb.append( "  " ).append( concept.getUri() );
   final Map<String,Double> uriQuotients
         = concept.getUriQuotients()
                  .stream()
                  .collect( Collectors.toMap( KeyValue::getKey, KeyValue::getValue ) );
   final Map<String,List<Mention>> uriMentionsMap = concept.getUriMentions();
   for ( Map.Entry<String,List<Mention>> uriMentions : uriMentionsMap.entrySet() ) {
      sb.append( "\n    " )
        .append( uriMentions.getKey() )
        .append( " = " )
        .append( uriQuotients.get( uriMentions.getKey() ) )
        .append( " : " );
      for( Mention mention : uriMentions.getValue() ) {
         sb.append( "[" ).append( concept.getCoveredText( mention ) ).append( "]" );
      }
   }
   return sb.append( "\n" ).toString();
}

   private NeoplasmAttribute createMajorTopoAttribute( final ConceptAggregate neoplasm,
                                                       final Collection<ConceptAggregate> allConcepts ) {
      final Collection<ConceptAggregate> firstSiteConcepts = getFirstSiteConcepts( neoplasm );
      NeoplasmSummaryCreator.DEBUG_SB.append( "TopoMajor First Sites\n" );
      firstSiteConcepts.stream()
                       .map( Topography::toConceptText )
                       .forEach( NeoplasmSummaryCreator.DEBUG_SB::append );
      _firstSiteMainUris = firstSiteConcepts.stream()
                                        .map( ConceptAggregate::getUri )
                                        .collect( Collectors.toSet() );
      _firstSiteAllUris = firstSiteConcepts.stream()
                                       .map( ConceptAggregate::getAllUris )
                                       .flatMap( Collection::stream )
                                       .collect( Collectors.toSet() );
//      _topographyCodes = _firstSiteMainUris.stream()
//                                           .map( Neo4jOntologyConceptUtil::getIcdoTopoCode )
//                                           .filter( t -> !t.isEmpty() )
//                                           .collect( Collectors.toSet() );

      final Collection<ConceptAggregate> neoplasmSiteConcepts = neoplasm.getRelatedSites();
      final Collection<ConceptAggregate> patientSiteConcepts = getPatientSiteConcepts( allConcepts );
      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( getFirstSiteConcepts( neoplasm ), neoplasmSiteConcepts, patientSiteConcepts );
      final List<Mention> directEvidence = new ArrayList<>( evidence.getOrDefault( DIRECT_EVIDENCE,
                                                                                   Collections.emptyList() ) );

      final List<Integer> features = createFeatures( neoplasm,
                                                     firstSiteConcepts,
                                                     neoplasmSiteConcepts,
                                                     _firstSiteMainUris,
                                                     _firstSiteAllUris,
                                                     directEvidence,
                                                     allConcepts );

      return SpecificAttribute.createAttribute( "topography_major",
                                                _bestMajorTopoCode,
                                                _bestUri,
                                                directEvidence,
                                                new ArrayList<>( evidence.getOrDefault( INDIRECT_EVIDENCE,
                                                                                        Collections.emptyList() ) ),
                                                new ArrayList<>( evidence.getOrDefault( NOT_EVIDENCE,
                                                                                        Collections.emptyList() ) ),
                                                features );
   }


   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Collection<ConceptAggregate> firstSiteConcepts,
                                         final Collection<ConceptAggregate> neoplasmSiteConcepts,
                                         final Collection<String> firstSiteMainUris,
                                         final Collection<String> firstSiteAllUris,
                                         final Collection<Mention> directEvidence,
                                         final Collection<ConceptAggregate> allConcepts ) {
      final List<Integer> features = new ArrayList<>( 5 );

      final Collection<ConceptAggregate> patientSiteConcepts = getPatientSiteConcepts( allConcepts );
      final Collection<Mention> allPatientSiteMentions = getPatientSiteMentions( allConcepts );
      final Collection<String> patientSiteMainUris = getMainUris( getPatientSiteConcepts( allConcepts ) );
      final Map<String, Collection<String>> allPatientSitesRootsMap = UriUtil.mapUriRoots( patientSiteMainUris );

      final Map<String, Collection<String>> firstSitesRootsMap = new HashMap<>( firstSiteMainUris.size() );
      firstSiteMainUris.forEach(
            u -> firstSitesRootsMap.put( u, allPatientSitesRootsMap.getOrDefault( u, Collections.emptyList() ) ) );

      final Map<String, Integer> uriCountsMap = UriScoreUtil.mapUriMentionCounts( directEvidence );
      final Map<String, Integer> uriSums = UriScoreUtil.mapUriSums( firstSiteMainUris, firstSitesRootsMap,
                                                                    uriCountsMap );
      final int totalCounts = uriCountsMap.values()
                                          .stream()
                                          .mapToInt( i -> i )
                                          .sum();
      final Map<String, Double> uriQuotientMap = UriScoreUtil.mapUriQuotientsBB( uriSums, totalCounts );
      final List<KeyValue<String, Double>> uriQuotientList = UriScoreUtil.listUriQuotients( uriQuotientMap );
//      final List<KeyValue<String, Double>> bestUriScores = UriScoreUtil.getBestUriScores( uriQuotientList );
//      final Map<String, Integer> classLevelMap = UriScoreUtil.createClassLevelMap( bestUriScores );
      final Map<String, Integer> classLevelMap = UriScoreUtil.createUriClassLevelMap( firstSiteMainUris );
      final List<KeyValue<String, Double>> bestUriScores = UriScoreUtil.getBestUriScores( uriQuotientList,
                                                                                          classLevelMap,
                                                                                          firstSitesRootsMap );

//      _bestSiteUri = UriScoreUtil.getBestUriScore( bestUriScores, classLevelMap, firstSitesRootsMap )
//                                 .getKey();
      _bestUri = bestUriScores.get( bestUriScores.size() - 1 )
                              .getKey();


      //1. Number of exact site class mentions. Normalized over total site class mentions.  Rounded to 0-10.
      final int exactSiteMentionCount = uriCountsMap.getOrDefault( _bestUri, 0 );
//      LOGGER.info( "1. " + _bestUri + "=" + exactSiteMentionCount + " "
//                   + allPatientSiteMentions.stream()
//                                           .map( Mention::getClassUri )
//                                           .collect( Collectors.joining( "," ) ) );
      features.add( getPrimaryToPatientMentions( exactSiteMentionCount, allPatientSiteMentions.size() ) );

      final Map<String, Integer> allSitesUriCountsMap = UriScoreUtil.mapUriMentionCounts( allPatientSiteMentions );
      final Map<String, Integer> bestSitesUriSums = UriScoreUtil.mapBestUriSums( patientSiteMainUris,
                                                                                 allPatientSitesRootsMap,
                                                                                 allSitesUriCountsMap );
      final int totalSitesUriSums = bestSitesUriSums.values()
                                                    .stream()
                                                    .mapToInt( i -> i )
                                                    .sum();
//      LOGGER.info( "2. " + _bestUri + "=" + uriSums.getOrDefault( _bestUri, 0 ) + " "
//                   + bestSitesUriSums.entrySet()
//                                     .stream()
//                                     .map( e -> e.getKey() + "=" + e.getValue() )
//                                     .collect( Collectors.joining( "," ) ) );
      features.add( createFeature2( uriSums.getOrDefault( _bestUri, 0 ), totalSitesUriSums ) );

//      LOGGER.info( "3. " + _bestUri + "=" + classLevelMap.getOrDefault( _bestUri, 0 ) + " "
//                   + classLevelMap.entrySet()
//                                  .stream()
//                                  .map( e -> e.getKey() + "=" + e.getValue() )
//                                  .collect( Collectors.joining( "," ) ) );
      features.add( createFeature3( _bestUri, classLevelMap ) );

      final Map<String, Integer> uriRelationCounts = mapSiteUriCounts( neoplasm );
      final int bestUriRelationCount = uriRelationCounts.getOrDefault( _bestUri, 0 );
//      LOGGER.info( "4. " + _bestUri + "=" + bestUriRelationCount + " sum " + uriRelationCounts.values()
//                                                                                              .stream()
//                                                                                              .mapToInt( l -> l )
//                                                                                              .sum() + " "
//                   + uriRelationCounts.entrySet()
//                                      .stream()
//                                      .map( e -> e.getKey() + "=" + e.getValue() )
//                                      .collect( Collectors.joining( "," ) ) );
      features.add( createFeature4( bestUriRelationCount, uriRelationCounts ) );

//      LOGGER.info( "5. " + _bestUri + "=" + bestUriRelationCount + " max " + uriRelationCounts.values()
//                                                                                              .stream()
//                                                                                              .max( Integer::compareTo )
//                                                                                              .orElse(
//                                                                                                        Integer.MAX_VALUE ) + " "
//                   + uriRelationCounts.entrySet()
//                                      .stream()
//                                      .map( e -> e.getKey() + "=" + e.getValue() )
//                                      .collect( Collectors.joining( "," ) ) );
      features.add( createFeature5( bestUriRelationCount, uriRelationCounts ) );

      addIntFeature( features, bestUriRelationCount );

      //1.  !!!!!  Best URI  !!!!!
      //    ======  CONCEPT  =====

      final Collection<ConceptAggregate> bestInFirstMainConcepts = getIfUriIsMain( _bestUri,
                                                                                   firstSiteConcepts );
      final Collection<ConceptAggregate> bestInFirstAllConcepts = getIfUriIsAny( _bestUri,
                                                                                 firstSiteConcepts );
      final Collection<ConceptAggregate> bestInNeoplasmMainConcepts = getIfUriIsMain( _bestUri,
                                                                                      neoplasmSiteConcepts );
      final Collection<ConceptAggregate> bestInNeoplasmAllConcepts = getIfUriIsAny( _bestUri,
                                                                                    neoplasmSiteConcepts );
      final Collection<ConceptAggregate> bestInPatientMainConcepts = getIfUriIsMain( _bestUri,
                                                                                     patientSiteConcepts );
      final Collection<ConceptAggregate> bestInPatientAllConcepts = getIfUriIsAny( _bestUri,
                                                                                   patientSiteConcepts );

      addCollectionFeatures( features, firstSiteConcepts, neoplasmSiteConcepts, patientSiteConcepts );

      addStandardFeatures( features,
                           bestInFirstMainConcepts,
                           firstSiteConcepts,
                           neoplasmSiteConcepts,
                           patientSiteConcepts );
      addStandardFeatures( features,
                           bestInFirstAllConcepts,
                           firstSiteConcepts,
                           neoplasmSiteConcepts,
                           patientSiteConcepts );
      addStandardFeatures( features, bestInNeoplasmMainConcepts, neoplasmSiteConcepts, patientSiteConcepts );
      addStandardFeatures( features, bestInNeoplasmAllConcepts, neoplasmSiteConcepts, patientSiteConcepts );
      addStandardFeatures( features, bestInPatientMainConcepts, patientSiteConcepts );
      addStandardFeatures( features, bestInPatientAllConcepts, patientSiteConcepts );

      //    ======  MENTION  =====
      final Collection<Mention> firstSiteMentions = getMentions( firstSiteConcepts );
      final Collection<Mention> neoplasmSiteMentions = getMentions( neoplasmSiteConcepts );
      final Collection<Mention> patientSiteMentions = getMentions( patientSiteConcepts );

      final Collection<Mention> bestInFirstMentions
            = firstSiteMentions.stream()
                               .filter( m -> m.getClassUri()
                                              .equals( _bestUri ) )
                               .collect( Collectors.toSet() );
      final Collection<Mention> bestInNeoplasmMentions
            = neoplasmSiteMentions.stream()
                                  .filter( m -> m.getClassUri()
                                                 .equals( _bestUri ) )
                                  .collect( Collectors.toSet() );
      final Collection<Mention> bestInPatientMentions
            = patientSiteMentions.stream()
                                 .filter( m -> m.getClassUri()
                                                .equals( _bestUri ) )
                                 .collect( Collectors.toSet() );

      addCollectionFeatures( features, firstSiteMentions, neoplasmSiteMentions, patientSiteMentions );

      addStandardFeatures( features,
                           bestInFirstMentions,
                           firstSiteMentions,
                           neoplasmSiteMentions,
                           patientSiteMentions );
      addStandardFeatures( features, bestInNeoplasmMentions, neoplasmSiteMentions, patientSiteMentions );
      addStandardFeatures( features, bestInPatientMentions, patientSiteMentions );


      //    ======  URI  =====
      final Collection<String> neoplasmSiteMainUris = neoplasm.getRelatedSiteMainUris();
      final Collection<String> neoplasmSiteAllUris = neoplasm.getRelatedSiteAllUris();
      final Collection<String> patientSiteAllUris = getAllUris( getPatientSiteConcepts( allConcepts ) );

      final Collection<String> bestAllUris = firstSiteConcepts.stream()
                                                              .map( ConceptAggregate::getAllUris )
                                                              .filter( s -> s.contains( _bestUri ) )
                                                              .flatMap( Collection::stream )
                                                              .collect( Collectors.toSet() );

      addCollectionFeatures( features, _firstSiteMainUris, neoplasmSiteMainUris, patientSiteMainUris );
      addCollectionFeatures( features, _firstSiteAllUris, neoplasmSiteAllUris, patientSiteAllUris );
      addStandardFeatures( features,
                           bestAllUris,
                           _firstSiteAllUris,
                           neoplasmSiteAllUris,
                           patientSiteAllUris );

      final Map<String, List<Mention>> firstSiteUriMentions = mapUriMentions( firstSiteMentions );
      final Map<String, List<Mention>> neoplasmSiteUriMentions = mapUriMentions( neoplasmSiteMentions );
      final Map<String, List<Mention>> patientSiteUriMentions = mapUriMentions( patientSiteMentions );
      addStandardFeatures( features, firstSiteUriMentions.keySet(), firstSiteMentions );
      addStandardFeatures( features, neoplasmSiteUriMentions.keySet(), neoplasmSiteMentions );
      addStandardFeatures( features, patientSiteUriMentions.keySet(), patientSiteMentions );


      //2.  !!!!!  URI Branch  !!!!!
      //    ======  URI  =====
      final Map<String, Collection<String>> patientSiteAllUriRootsMap = UriUtil.mapUriRoots( patientSiteAllUris );
//      final Map<String, Collection<String>> patientSiteMainUriRootsMap = new HashMap<>( patientSiteAllUriRootsMap );
//      patientSiteMainUriRootsMap.keySet()
//                                .retainAll( patientSiteMainUris );

      final Map<String, Collection<String>> neoplasmSiteAllUriRootsMap = new HashMap<>( patientSiteAllUriRootsMap );
      neoplasmSiteAllUriRootsMap.keySet()
                                .retainAll( neoplasmSiteAllUris );
//      final Map<String, Collection<String>> neoplasmSiteMainUriRootsMap = new HashMap<>( neoplasmSiteAllUriRootsMap );
//      neoplasmSiteMainUriRootsMap.keySet()
//                                 .retainAll( neoplasmSiteMainUris );

      final Map<String, Collection<String>> firstSiteAllUriRootsMap = new HashMap<>( neoplasmSiteAllUriRootsMap );
      firstSiteAllUriRootsMap.keySet()
                             .retainAll( _firstSiteAllUris );
//      final Map<String, Collection<String>> firstSiteMainUriRootsMap = new HashMap<>( firstSiteAllUriRootsMap );
//      firstSiteMainUriRootsMap.keySet()
//                              .retainAll( _firstSiteMainUris );

      final Map<String, Integer> firstMentionBranchCounts = mapAllUriBranchMentionCounts( firstSiteUriMentions,
                                                                                          firstSiteAllUriRootsMap );
      final Map<String, Integer> neoplasmMentionBranchCounts = mapAllUriBranchMentionCounts( neoplasmSiteUriMentions,
                                                                                             neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> patientMentionBranchCounts = mapAllUriBranchMentionCounts( patientSiteUriMentions,
                                                                                            patientSiteAllUriRootsMap );
      addCollectionFeatures( features, firstMentionBranchCounts.keySet(),
                             neoplasmMentionBranchCounts.keySet(),
                             patientMentionBranchCounts.keySet() );


      //    ======  CONCEPT  =====
      final Map<String, Integer> bestFirstMainConceptBranchCounts
            = mapUriBranchConceptCounts( bestInFirstMainConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> bestNeoplasmMainConceptBranchCounts
            = mapUriBranchConceptCounts( bestInNeoplasmMainConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> bestPatientMainConceptBranchCounts
            = mapUriBranchConceptCounts( bestInPatientMainConcepts, patientSiteAllUriRootsMap );

      final Map<String, Integer> bestFirstAllConceptBranchCounts
            = mapUriBranchConceptCounts( bestInFirstAllConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> bestNeoplasmAllConceptBranchCounts
            = mapUriBranchConceptCounts( bestInNeoplasmAllConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> bestPatientAllConceptBranchCounts
            = mapUriBranchConceptCounts( bestInPatientAllConcepts, patientSiteAllUriRootsMap );

      final Map<String, Integer> firstConceptBranchCounts
            = mapUriBranchConceptCounts( firstSiteConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> neoplasmConceptBranchCounts
            = mapUriBranchConceptCounts( neoplasmSiteConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> patientConceptBranchCounts
            = mapUriBranchConceptCounts( patientSiteConcepts, patientSiteAllUriRootsMap );

      final int bestFirstMainConceptBranchCount = getBranchCountsSum( bestFirstMainConceptBranchCounts );
      final int bestNeoplasmMainConceptBranchCount = getBranchCountsSum( bestNeoplasmMainConceptBranchCounts );
      final int bestPatientMainConceptBranchCount = getBranchCountsSum( bestPatientMainConceptBranchCounts );
      final int bestFirstAllConceptBranchCount = getBranchCountsSum( bestFirstAllConceptBranchCounts );
      final int bestNeoplasmAllConceptBranchCount = getBranchCountsSum( bestNeoplasmAllConceptBranchCounts );
      final int bestPatientAllConceptBranchCount = getBranchCountsSum( bestPatientAllConceptBranchCounts );
      final int firstConceptBranchCount = getBranchCountsSum( firstConceptBranchCounts );
      final int neoplasmConceptBranchCount = getBranchCountsSum( neoplasmConceptBranchCounts );
      final int patientConceptBranchCount = getBranchCountsSum( patientConceptBranchCounts );

      addStandardFeatures( features,
                           bestFirstMainConceptBranchCount,
                           firstConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount );
      addStandardFeatures( features,
                           bestFirstAllConceptBranchCount,
                           firstConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount );
      addStandardFeatures( features,
                           bestNeoplasmMainConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount );
      addStandardFeatures( features,
                           bestNeoplasmAllConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount );
      addStandardFeatures( features, bestPatientMainConceptBranchCount, patientConceptBranchCount );
      addStandardFeatures( features, bestPatientAllConceptBranchCount, patientConceptBranchCount );


      //    ======  MENTION  =====
      final Map<String, Integer> bestFirstMainMentionBranchCounts
            = mapUriBranchMentionCounts( bestInFirstMainConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> bestNeoplasmMainMentionBranchCounts
            = mapUriBranchMentionCounts( bestInNeoplasmMainConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> bestPatientMainMentionBranchCounts
            = mapUriBranchMentionCounts( bestInPatientMainConcepts, patientSiteAllUriRootsMap );

      final Map<String, Integer> bestFirstAllMentionBranchCounts
            = mapUriBranchMentionCounts( bestInFirstAllConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> bestNeoplasmAllMentionBranchCounts
            = mapUriBranchMentionCounts( bestInNeoplasmAllConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> bestPatientAllMentionBranchCounts
            = mapUriBranchMentionCounts( bestInPatientAllConcepts, patientSiteAllUriRootsMap );

      final int bestFirstMainMentionBranchCount = getBranchCountsSum( bestFirstMainMentionBranchCounts );
      final int bestNeoplasmMainMentionBranchCount = getBranchCountsSum( bestNeoplasmMainMentionBranchCounts );
      final int bestPatientMainMentionBranchCount = getBranchCountsSum( bestPatientMainMentionBranchCounts );
      final int bestFirstAllMentionBranchCount = getBranchCountsSum( bestFirstAllMentionBranchCounts );
      final int bestNeoplasmAllMentionBranchCount = getBranchCountsSum( bestNeoplasmAllMentionBranchCounts );
      final int bestPatientAllMentionBranchCount = getBranchCountsSum( bestPatientAllMentionBranchCounts );
      final int firstMentionBranchCount = getBranchCountsSum( firstMentionBranchCounts );
      final int neoplasmMentionBranchCount = getBranchCountsSum( neoplasmMentionBranchCounts );
      final int patientMentionBranchCount = getBranchCountsSum( patientMentionBranchCounts );

      addLargeIntFeatures( features, firstMentionBranchCount, neoplasmMentionBranchCount, patientMentionBranchCount );

      addStandardFeatures( features,
                           bestFirstMainMentionBranchCount,
                           firstMentionBranchCount,
                           neoplasmMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestFirstAllMentionBranchCount,
                           firstMentionBranchCount,
                           neoplasmMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestNeoplasmMainMentionBranchCount,
                           neoplasmMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestNeoplasmAllMentionBranchCount,
                           neoplasmMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestPatientMainMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestPatientAllMentionBranchCount,
                           patientMentionBranchCount );


      //3.  !!!!!  URI Depth  !!!!!
      //    ======  URI  =====
      final int bestDepth = classLevelMap.getOrDefault( _bestUri, 0 );

      final int bestMaxDepth = bestAllUris.stream()
                                          .mapToInt( u -> classLevelMap.getOrDefault( u, 0 ) )
                                          .max()
                                          .orElse( 0 );
      final int firstMainMaxDepth = firstSiteMainUris.stream()
                                                     .mapToInt( u -> classLevelMap.getOrDefault( u, 0 ) )
                                                     .max()
                                                     .orElse( 0 );
      final int firstAllMaxDepth = firstSiteAllUris.stream()
                                                   .mapToInt( u -> classLevelMap.getOrDefault( u, 0 ) )
                                                   .max()
                                                   .orElse( 0 );
      final int neoplasmMainMaxDepth = neoplasmSiteMainUris.stream()
                                                           .mapToInt( u -> classLevelMap.getOrDefault( u, 0 ) )
                                                           .max()
                                                           .orElse( 0 );
      final int neoplasmAllMaxDepth = neoplasmSiteAllUris.stream()
                                                         .mapToInt( u -> classLevelMap.getOrDefault( u, 0 ) )
                                                         .max()
                                                         .orElse( 0 );
      final int patientMainMaxDepth = patientSiteMainUris.stream()
                                                         .mapToInt( u -> classLevelMap.getOrDefault( u, 0 ) )
                                                         .max()
                                                         .orElse( 0 );
      final int patientAllMaxDepth = classLevelMap.values()
                                                  .stream()
                                                  .mapToInt( i -> i )
                                                  .max()
                                                  .orElse( 0 );

      addLargeIntFeatures( features,
                     bestDepth * 2, bestMaxDepth * 2,firstMainMaxDepth * 2,firstAllMaxDepth * 2,
                     neoplasmMainMaxDepth * 2,neoplasmAllMaxDepth * 2,patientMainMaxDepth * 2,patientAllMaxDepth * 2 );

      addStandardFeatures( features, bestDepth,
                           firstMainMaxDepth,
                           firstAllMaxDepth,
                           neoplasmMainMaxDepth,
                           neoplasmAllMaxDepth,
                           patientMainMaxDepth,
                           patientAllMaxDepth );

      addStandardFeatures( features, bestMaxDepth,
                           firstMainMaxDepth,
                           firstAllMaxDepth,
                           neoplasmMainMaxDepth,
                           neoplasmAllMaxDepth,
                           patientMainMaxDepth,
                           patientAllMaxDepth );

      //4.  !!!!!  Relation Count  !!!!!
      final Collection<Collection<ConceptAggregate>> relations =
            neoplasm.getRelatedConceptMap()
                    .entrySet()
                    .stream()
                    .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
                    .map( Map.Entry::getValue )
                    .collect( Collectors.toList() );
      final Predicate<ConceptAggregate> hasBestUri = c -> c.getAllUris()
                                                           .stream()
                                                           .anyMatch( bestAllUris::contains );
      final Predicate<Collection<ConceptAggregate>> setHasBestUri = c -> c.stream()
                                                                          .anyMatch( hasBestUri );
      final int bestRelationCount = (int) relations.stream()
                                                   .filter( setHasBestUri )
                                                   .count();
      final int allSiteRelationCount = relations.size();
      final int patientRelationCount = (int) allConcepts
            .stream()
            .map( ConceptAggregate::getRelatedConceptMap )
            .map( Map::entrySet )
            .flatMap( Collection::stream )
            .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
            .map( Map.Entry::getValue )
            .count();

      addLargeIntFeatures( features, allSiteRelationCount, patientRelationCount );
      addStandardFeatures( features, bestRelationCount, allSiteRelationCount, patientRelationCount );


      //5.  !!!!!  Runner-Up  !!!!!
      //    ======  CONCEPT  =====
      final double bestUriScore = bestUriScores.get( bestUriScores.size() - 1 )
                                               .getValue();
      addDoubleDivisionFeature( features, 1, bestUriScore );
      final boolean haveRunnerUp = bestUriScores.size() > 1;
      double runnerUpScore = haveRunnerUp
                             ? bestUriScores.get( bestUriScores.size() - 2 )
                                            .getValue()
                             : 0;
      addDoubleDivisionFeature( features, 1, runnerUpScore );
      addDoubleDivisionFeature( features, runnerUpScore, bestUriScore );

      final String runnerUp = haveRunnerUp ? bestUriScores.get( bestUriScores.size()-2 )
                                                          .getKey() : "";
      final Collection<ConceptAggregate> runnerUpInFirstMainConcepts
            = haveRunnerUp ? getIfUriIsMain( runnerUp, firstSiteConcepts ) : Collections.emptyList();
      final Collection<ConceptAggregate> runnerUpNeoplasmMainConcepts
            = haveRunnerUp ? getIfUriIsMain( runnerUp, neoplasmSiteConcepts ) : Collections.emptyList();
      final Collection<ConceptAggregate> runnerUpPatientMainConcepts
            = haveRunnerUp ? getIfUriIsMain( runnerUp, patientSiteConcepts ) : Collections.emptyList();

      final Collection<ConceptAggregate> runnerUpInFirstAllConcepts
            = haveRunnerUp ? getIfUriIsAny( runnerUp, firstSiteConcepts ) : Collections.emptyList();
      final Collection<ConceptAggregate> runnerUpNeoplasmAllConcepts
            = haveRunnerUp ? getIfUriIsAny( runnerUp, neoplasmSiteConcepts ) : Collections.emptyList();
      final Collection<ConceptAggregate> runnerUpPatientAllConcepts
            = haveRunnerUp ? getIfUriIsAny( runnerUp, patientSiteConcepts ) : Collections.emptyList();

      addStandardFeatures( features, runnerUpInFirstMainConcepts,
                           firstSiteConcepts,
                           neoplasmSiteConcepts,
                           patientSiteConcepts );
      addStandardFeatures( features, runnerUpInFirstAllConcepts,
                           firstSiteConcepts,
                           neoplasmSiteConcepts,
                           patientSiteConcepts );
      addStandardFeatures( features, runnerUpNeoplasmMainConcepts,
                           neoplasmSiteConcepts,
                           patientSiteConcepts );
      addStandardFeatures( features, runnerUpNeoplasmAllConcepts,
                           neoplasmSiteConcepts,
                           patientSiteConcepts );
      addStandardFeatures( features, runnerUpPatientMainConcepts, patientSiteConcepts );
      addStandardFeatures( features, runnerUpPatientAllConcepts, patientSiteConcepts );


      final Map<String, Integer> runnerUpFirstMainConceptBranchCounts
            = mapUriBranchConceptCounts( runnerUpInFirstMainConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpNeoplasmMainConceptBranchCounts
            = mapUriBranchConceptCounts( runnerUpNeoplasmMainConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpPatientMainConceptBranchCounts
            = mapUriBranchConceptCounts( runnerUpPatientMainConcepts, patientSiteAllUriRootsMap );

      final Map<String, Integer> runnerUpFirstAllConceptBranchCounts
            = mapUriBranchConceptCounts( runnerUpInFirstAllConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpNeoplasmAllConceptBranchCounts
            = mapUriBranchConceptCounts( runnerUpNeoplasmAllConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpPatientAllConceptBranchCounts
            = mapUriBranchConceptCounts( runnerUpPatientAllConcepts, patientSiteAllUriRootsMap );

      final int runnerUpFirstMainConceptBranchCount = getBranchCountsSum( runnerUpFirstMainConceptBranchCounts );
      final int runnerUpNeoplasmMainConceptBranchCount = getBranchCountsSum( runnerUpNeoplasmMainConceptBranchCounts );
      final int runnerUpPatientMainConceptBranchCount = getBranchCountsSum( runnerUpPatientMainConceptBranchCounts );

      final int runnerUpFirstAllConceptBranchCount = getBranchCountsSum( runnerUpFirstAllConceptBranchCounts );
      final int runnerUpNeoplasmAllConceptBranchCount = getBranchCountsSum( runnerUpNeoplasmAllConceptBranchCounts );
      final int runnerUpPatientAllConceptBranchCount = getBranchCountsSum( runnerUpPatientAllConceptBranchCounts );

      addStandardFeatures( features, runnerUpFirstMainConceptBranchCount,
                           firstConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount,
                           bestFirstMainConceptBranchCount,
                           bestNeoplasmMainConceptBranchCount,
                           bestPatientMainConceptBranchCount );
      addStandardFeatures( features, runnerUpFirstAllConceptBranchCount,
                           firstConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount,
                           bestFirstMainConceptBranchCount,
                           bestNeoplasmMainConceptBranchCount,
                           bestPatientMainConceptBranchCount );
      addStandardFeatures( features, runnerUpNeoplasmMainConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount,
                           bestNeoplasmMainConceptBranchCount,
                           bestPatientMainConceptBranchCount );
      addStandardFeatures( features, runnerUpNeoplasmAllConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount,
                           bestNeoplasmMainConceptBranchCount,
                           bestPatientMainConceptBranchCount );
      addStandardFeatures( features, runnerUpPatientMainConceptBranchCount,
                           patientConceptBranchCount,
                           bestPatientMainConceptBranchCount );
      addStandardFeatures( features, runnerUpPatientAllConceptBranchCount,
                           patientConceptBranchCount,
                           bestPatientMainConceptBranchCount );


      //    ======  MENTION  =====
      final Collection<Mention> runnerUpFirstMentions
            = firstSiteMentions.stream()
                               .filter( m -> m.getClassUri()
                                              .equals( runnerUp ) )
                               .collect( Collectors.toSet() );
      final Collection<Mention> runnerUpNeoplasmMentions
            = neoplasmSiteMentions.stream()
                                  .filter( m -> m.getClassUri()
                                                 .equals( runnerUp ) )
                                  .collect( Collectors.toSet() );
      final Collection<Mention> runnerUpPatientMentions
            = patientSiteMentions.stream()
                                 .filter( m -> m.getClassUri()
                                                .equals( runnerUp ) )
                                 .collect( Collectors.toSet() );

      addStandardFeatures( features, runnerUpFirstMentions,
                           firstSiteMentions,
                           neoplasmSiteMentions,
                           patientSiteMentions,
                           bestInFirstMentions,
                           bestInNeoplasmMentions,
                           bestInPatientMentions );
      addStandardFeatures( features, runnerUpNeoplasmMentions,
                           neoplasmSiteMentions,
                           patientSiteMentions,
                           bestInNeoplasmMentions,
                           bestInPatientMentions );
      addStandardFeatures( features, runnerUpPatientMentions,
                           patientSiteMentions,
                           bestInPatientMentions );


      final Map<String, Integer> runnerUpFirstMainMentionBranchCounts
            = mapUriBranchMentionCounts( runnerUpInFirstMainConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpNeoplasmMainMentionBranchCounts
            = mapUriBranchMentionCounts( runnerUpNeoplasmMainConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpPatientMainMentionBranchCounts
            = mapUriBranchMentionCounts( runnerUpPatientMainConcepts, patientSiteAllUriRootsMap );

      final Map<String, Integer> runnerUpFirstAllMentionBranchCounts
            = mapUriBranchMentionCounts( runnerUpInFirstAllConcepts, firstSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpNeoplasmAllMentionBranchCounts
            = mapUriBranchMentionCounts( runnerUpNeoplasmAllConcepts, neoplasmSiteAllUriRootsMap );
      final Map<String, Integer> runnerUpPatientAllMentionBranchCounts
            = mapUriBranchMentionCounts( runnerUpPatientAllConcepts, patientSiteAllUriRootsMap );

      final int runnerUpFirstMainMentionBranchCount = getBranchCountsSum( runnerUpFirstMainMentionBranchCounts );
      final int runnerUpNeoplasmMainMentionBranchCount = getBranchCountsSum( runnerUpNeoplasmMainMentionBranchCounts );
      final int runnerUpPatientMainMentionBranchCount = getBranchCountsSum( runnerUpPatientMainMentionBranchCounts );
      final int runnerUpFirstAllMentionBranchCount = getBranchCountsSum( runnerUpFirstAllMentionBranchCounts );
      final int runnerUpNeoplasmAllMentionBranchCount = getBranchCountsSum( runnerUpNeoplasmAllMentionBranchCounts );
      final int runnerUpPatientAllMentionBranchCount = getBranchCountsSum( runnerUpPatientAllMentionBranchCounts );

      addStandardFeatures( features, runnerUpFirstMainMentionBranchCount,
                           firstMentionBranchCount, neoplasmMentionBranchCount, patientMentionBranchCount );
      addStandardFeatures( features, runnerUpFirstAllMentionBranchCount,
                           firstMentionBranchCount, neoplasmMentionBranchCount, patientMentionBranchCount );
      addStandardFeatures( features, runnerUpNeoplasmMainMentionBranchCount,
                           neoplasmMentionBranchCount, patientMentionBranchCount );
      addStandardFeatures( features, runnerUpNeoplasmAllMentionBranchCount,
                           neoplasmMentionBranchCount, patientMentionBranchCount );
      addStandardFeatures( features, runnerUpPatientMainMentionBranchCount, patientMentionBranchCount );
      addStandardFeatures( features, runnerUpPatientAllMentionBranchCount, patientMentionBranchCount );


      //    ======  URI  =====
      final Collection<String> runnerUpAllUris
            = haveRunnerUp ? firstSiteConcepts.stream()
                                               .map( ConceptAggregate::getAllUris )
                                               .filter( s -> s.contains( runnerUp ) )
                                               .flatMap( Collection::stream )
                                               .collect( Collectors.toSet() )
                           : Collections.emptyList();
      addStandardFeatures( features, runnerUpAllUris, _firstSiteAllUris, neoplasmSiteAllUris, patientSiteAllUris );

      //3.  !!!!!  URI Depth  !!!!!
      //    ======  URI  =====
      final int runnerUpDepth = haveRunnerUp ? classLevelMap.getOrDefault( runnerUp, 0 ) : 0;
      final int runnerUpMaxDepth = runnerUpAllUris.stream()
                                          .mapToInt( u -> classLevelMap.getOrDefault( u, 0 ) )
                                          .max()
                                          .orElse( 0 );

      addLargeIntFeatures( features, runnerUpDepth * 2, runnerUpMaxDepth * 2 );
      addStandardFeatures( features, runnerUpDepth, firstMainMaxDepth, firstAllMaxDepth, neoplasmMainMaxDepth,
                           neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );
      addStandardFeatures( features, runnerUpMaxDepth, firstMainMaxDepth, firstAllMaxDepth, neoplasmMainMaxDepth,
                           neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );


      //6.  !!!!!  Relation Depth  !!!!!
      //    =====  Relation Depth  =====
      final List<Integer> orderDepths = new ArrayList<>();
      final Map<Integer,Collection<ConceptAggregate>> orderSiteMap = new HashMap<>();
      int order = 1;
      for ( String relation : SITE_RELATIONS ) {
         final Collection<ConceptAggregate> sites = neoplasm.getRelated( relation );
         if ( !sites.isEmpty() ) {
            orderDepths.add( order );
            orderSiteMap.put( order, sites );
         }
         order++;
      }
      addIntFeature( features, orderDepths.isEmpty() ? 0 : orderDepths.get( 0 ) * 2 );
      addIntFeature( features, orderDepths.size() <= 1
                            ? 0
                            : orderDepths.get( 1 ) * 2 );
      addIntFeature( features, orderDepths.size() * 2 );
      addIntFeature( features, getOrdersOccupied( orderSiteMap,
                                                  Collections.singletonList( _bestUri ) ).size() * 2 );
      addIntFeature( features, getOrdersOccupied( orderSiteMap, bestAllUris ).size() * 2 );

//      final int feature45 = 2 *
//                            featureHelper.getOrdersOccupied( featureHelper.mapAllUriRoots( site._bestInFirstSites )
//                                                                          .values()
//                                                                          .stream()
//                                                                          .flatMap( Collection::stream )
//                                                                          .collect( Collectors.toList() ) )
//                                         .size();

      //7.  !!!!!  Topography Codes  !!!!!
      final Collection<String> ontoTopoCodes = getOntoTopoCodes( Collections.singletonList( _bestUri ) );
      final Collection<String> tableTopoCodes = getTableTopoCodes( Collections.singletonList( _bestUri ) );
      final Collection<String> firstMainTopoCodes = getOntoTopoCodes( _firstSiteMainUris );
      final Collection<String> firstMainTableCodes = getTableTopoCodes( _firstSiteMainUris );

      addStandardFeatures( features, ontoTopoCodes, firstMainTopoCodes );
      addStandardFeatures( features, tableTopoCodes, firstMainTableCodes );

      final Collection<String> ontoAllTopoCodes = getOntoTopoCodes( bestAllUris );
      final Collection<String> tableAllTopoCodes = getTableTopoCodes( bestAllUris );
      final Collection<String> firstAllTopoCodes = getOntoTopoCodes( _firstSiteAllUris );
      final Collection<String> firstAllTableCodes = getTableTopoCodes( _firstSiteAllUris );

      addStandardFeatures( features, ontoAllTopoCodes, firstAllTopoCodes );
      addStandardFeatures( features, tableAllTopoCodes, firstAllTableCodes );

      if ( !ontoTopoCodes.isEmpty() ) {
         _topographyCodes = ontoTopoCodes;
      } else if ( !tableTopoCodes.isEmpty() ) {
         _topographyCodes = tableTopoCodes;
      } else if ( !ontoAllTopoCodes.isEmpty() ) {
         _topographyCodes = ontoAllTopoCodes;
      } else if ( !tableAllTopoCodes.isEmpty() ) {
         _topographyCodes = tableAllTopoCodes;
      } else if ( !firstMainTopoCodes.isEmpty() ) {
         _topographyCodes = firstMainTopoCodes;
      } else if ( !firstMainTableCodes.isEmpty() ) {
         _topographyCodes = firstMainTableCodes;
      } else if ( !firstAllTopoCodes.isEmpty() ) {
         _topographyCodes = firstAllTopoCodes;
      } else if ( !firstAllTableCodes.isEmpty() ) {
         _topographyCodes = firstAllTableCodes;
      } else {
         _topographyCodes = Collections.emptyList();
      }
      _bestMajorTopoCode = getMajorTopoCode( _topographyCodes );
      if ( _bestMajorTopoCode.equals( "C60-C63" ) ) {
         // Hard swap for now.
         _bestMajorTopoCode = "C61";
      }

      addCollectionFeatures( features, _topographyCodes );
//      features.add( Math.min( 10, site._bestNeoplasmMentionBranchCount ) );
      features.add( _bestMajorTopoCode.equals( "C80" ) ? 0 : 10 );
      addBooleanFeatures( features, neoplasm.isNegated(), neoplasm.isUncertain(), neoplasm.isGeneric(),
                          neoplasm.isConditional() );
      addLargeIntFeatures( features, neoplasm.getMentions().size() );

//      final Collection<String> morphs = NeoplasmSummaryCreator.getMorphology( featureHelper.getNeoplasm(),
//                                                                              _majorTopoCode + "0" );
//      features.add( NeoplasmSummaryCreator.getBestHistology( morphs ).equals( "8000" ) ? 0 : 10 );
//      features.add( Math.min( 10, (site._bestNeoplasmMentionBranchCount+1) * site._bestNeoplasmMentionBranchCount ) );

//      LOGGER.info( "Features: " + features.size() );
      return features;
   }



//   static private int getBranchCountsSum( final Map<String, Integer> conceptBranchCounts ) {
//      return conceptBranchCounts.values()
//                                         .stream()
//                                         .mapToInt( i -> i )
//                                         .sum();
//   }

   static private Collection<ConceptAggregate> getPatientSiteConcepts( final Collection<ConceptAggregate> allConcepts ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      return allConcepts.stream()
                        .filter( c -> UriConstants.getLocationUris( graphDb )
                                                  .contains( c.getUri() ) )
                        .collect( Collectors.toSet() );
   }

   static private Collection<Mention> getPatientSiteMentions( final Collection<ConceptAggregate> allConcepts ) {
      return getMentions( getPatientSiteConcepts( allConcepts ) );
   }







   //                The following features are neoplasm-independent.
   //                The following features involve site class and branch prevalence.

   /**
    * Number of exact site class mentions. Normalized over total site class mentions.
    * For instance, forearm (2) vs. abdomen (3) vs. finger (3) ... vs. upper_limb (4) vs. trunk (1).
    * Here the top exact class is upper_limb (4/13).
    *
    * @param primaryMentionCount number of site mentions for the neoplasm that have exactly the best site uri.
    * @param patientMentionCount number of site mentions for the entire patient.
    * @return integer 1-10 representing ratio of neoplasm exact sites to all patient sites.
    */
   static private int getPrimaryToPatientMentions( final int primaryMentionCount,
                                                   final int patientMentionCount ) {
      // Chen, 1/19/2021  Correlation ranked #4 of 5
      return divisionInt0to10( primaryMentionCount, patientMentionCount );
   }

   /**
    * Number of site class branch mentions.  Normalized over total site class mentions for patient.
    * For instance, [forearm (2), upper_limb (4)]=(6) vs. [abdomen (3), trunk (1)]=(4) vs. [finger (3)]=(3).
    * Here the top class by branch is forearm (6/13).
    * Another reason to normalize over the total site mentions:
    * Consider 1 note with 20 site mentions, most populous = (17) vs. 5 notes (or 1 long note)
    * with 100 total site mentions, most populous = (37).
    * The ratios 17/20 vs. 37/100 are very different from the absolute 17 vs. 37.
    * Keep in mind that branches can overlap.
    * For instance, [forearm, upper_limb] (6) vs. [arm, upper_limb] (5) where upper_limb is common with 4 mentions.
    *
    * @param exactSiteBranchMentionCount   -
    * @param patientSiteBranchMentionCount -
    * @return -
    */
   static private int createFeature2( final int exactSiteBranchMentionCount,
                                      final int patientSiteBranchMentionCount ) {
      // Chen, 1/19/2021  Correlation ranked #5 of 5
      return divisionInt0to10( exactSiteBranchMentionCount, patientSiteBranchMentionCount );
   }


   //                The following feature involves class precision.
   //                - A precise class is more likely to be correct than an imprecise class.

   /**
    * Distance of exact site class from root.  Normalize over the furthest distance class.
    * For instance forearm {3} vs. abdomen {3} vs. finger {3}.
    * - Keep in mind that because of speed constraints we are
    * not walking the ontology through PART_OF (etc.) relations.  (finger P_O hand P_O upper_limb).
    *
    * @return -
    */
   static private int createFeature3( final String bestUri, final Map<String, Integer> classLevelMap ) {
      return absolute0to10( classLevelMap.getOrDefault( bestUri, 0 ) );
      // Chen, 1/19/2021  Correlation ranked #2 of 5  - which is hilarious since it is a constant.
//      final int max = classLevelMap.values()
//                                   .stream()
//                                   .max( Integer::compareTo )
//                                   .orElse( Integer.MAX_VALUE );
//      return scoreInt0to10( classLevelMap.getOrDefault( bestUri, 0 ), max );
   }


   //                The following are neoplasm-dependent.  They account for relations.

   /**
    * Number of "HAS_SITE" relations between best site and neoplasm.
    * Normalized over the total "HAS_SITE" relation count for the neoplasm.
    * Consider that multiple neoplasm mentions can have the same site mention.
    * forearm <2>, abdomen <0>, finger <5>.  So, the top score would be finger <5/7>
    *
    * @param bestUriRelationCounts -
    * @param uriRelationCounts     -
    * @return -
    */
   static private int createFeature4( final int bestUriRelationCounts, final Map<String, Integer> uriRelationCounts ) {
      // Chen, 1/19/2021  Correlation ranked #1 of 5
      final int sum = uriRelationCounts.values()
                                       .stream()
                                       .mapToInt( l -> l )
                                       .sum();
      return divisionInt0to10( bestUriRelationCounts, sum );
   }


   /**
    * We now have most mentioned class upper_limb (4/13), most mentioned branch class forearm (6/13),
    * most 'precise' class tied at {3} and most related class branch finger <5>.
    * Notice that finger <5> relations outnumber branch mentions of finger (3).
    * The site of the neoplasm is (largely) determined by #5:
    * the number of times neoplasm branch mentions are related to site branch mentions.
    * This takes into account the different "HAS_SITE" type relations.
    * <p>
    * Next nearest site related to neoplasm, normalized by winning site.  In this case forearm <2/5>.
    * We could normalize to the total relations <2/7>, but consider relations <3>,<2>,<2>.
    * I think that the score <2/3> is better than <2/7>.
    *
    * @param bestUriRelationCounts -
    * @param uriRelationCounts     -
    * @return -
    */
   static private int createFeature5( final int bestUriRelationCounts, final Map<String, Integer> uriRelationCounts ) {
      // Chen, 1/19/2021  Correlation ranked #3 of 5
      final int max = uriRelationCounts.values()
                                       .stream()
                                       .max( Integer::compareTo )
                                       .orElse( Integer.MAX_VALUE );
      return divisionInt0to10( bestUriRelationCounts, max );
   }


   static private Collection<ConceptAggregate> getFirstSiteConcepts( final ConceptAggregate neoplasm ) {
      final Collection<ConceptAggregate> firstConcepts = getFirstRelatedConcepts( neoplasm,
                                      DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                                      DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                                      DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
                                      Disease_Has_Associated_Region,
                                      Disease_Has_Associated_Cavity );
      if ( firstConcepts.size() <= 1 ) {
         return firstConcepts;
      }

      //  Added 3-22-2021
      final Collection<String> allUris = firstConcepts.stream()
                                                   .map( ConceptAggregate::getAllUris )
                                                   .flatMap( Collection::stream )
                                                   .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allUriRoots = firstConcepts.stream()
                                                                   .map( ConceptAggregate::getUriRootsMap )
                                                                   .map( Map::entrySet )
                                                                   .flatMap( Collection::stream )
                                                                   .distinct()
                                                                   .collect( Collectors.toMap( Map.Entry::getKey,
                                                                                               Map.Entry::getValue ) );
      final Collection<Mention> allMentions = firstConcepts.stream()
                                                        .map( ConceptAggregate::getMentions )
                                                        .flatMap( Collection::stream )
                                                        .collect( Collectors.toSet() );
      final List<KeyValue<String, Double>> uriQuotients = UriScoreUtil.mapUriQuotients( allUris,
                                                                                        allUriRoots,
                                                                                        allMentions );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         final int strength = (int)Math.ceil( quotients.getValue() * 100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      UriInfoVisitor.applySectionAttributeUriStrengths( firstConcepts, uriStrengths );
      UriInfoVisitor.applyHistoryAttributeUriStrengths( firstConcepts, uriStrengths );
      final String topUri = uriStrengths.entrySet()
                           .stream()
                           .max( Comparator.comparingInt( Map.Entry::getValue ) )
                           .map( Map.Entry::getKey )
                                        .orElse( "" );
      return firstConcepts.stream()
                   .filter( c -> c.getAllUris().contains( topUri ) )
                          .collect( Collectors.toSet() );
   }


   static private Collection<ConceptAggregate> getFirstRelatedConcepts( final ConceptAggregate conceptAggregate,
                                                          final String... relationTypes ) {
      for ( String type : relationTypes ) {
         final Collection<ConceptAggregate> relatedConcepts = conceptAggregate.getRelated( type );
         if ( relatedConcepts != null && !relatedConcepts.isEmpty() ) {
            return relatedConcepts;
         }
      }
      return Collections.emptyList();
   }


   static private Map<String, Integer> mapSiteUriCounts( final ConceptAggregate neoplasm ) {
      return mapSiteUriCounts( neoplasm,
                               DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                               DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                               DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
                               Disease_Has_Associated_Region,
                               Disease_Has_Associated_Cavity );
   }

   static private Map<String, Integer> mapSiteUriCounts( final ConceptAggregate conceptAggregate,
                                                         final String... relations ) {
      final Map<String, Integer> uriCounts = new HashMap<>();
      for ( String relation : relations ) {
         final Map<String, List<ConceptAggregate>> uriConceptMap = conceptAggregate
               .getRelated( relation )
               .stream()
               .collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
         uriConceptMap.forEach( ( k, v ) -> uriCounts.put( k, uriCounts.getOrDefault( k, 0 ) + v.size() ) );
      }
      return uriCounts;
   }


   static private final List<String> SITE_RELATIONS = Arrays.asList( DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                                                                     DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                                                                     Disease_Has_Associated_Region,
                                                                     Disease_Has_Associated_Cavity,
                                                                     DISEASE_HAS_METASTATIC_ANATOMIC_SITE );

   public Collection<Integer> getOrdersOccupied( final Map<Integer,Collection<ConceptAggregate>> orderedSiteMap,
                                                 final Collection<String> uris ) {
      if ( orderedSiteMap.size() == 1 ) {
         return orderedSiteMap.keySet();
      }
      final Collection<Integer> orders = new HashSet<>();
      for ( Map.Entry<Integer,Collection<ConceptAggregate>> orderedSites : orderedSiteMap.entrySet() ) {
         if ( orderedSites.getValue()
                          .stream()
                          .map( ConceptAggregate::getAllUris )
                          .flatMap( Collection::stream )
                          .anyMatch( uris::contains ) ) {
            orders.add( orderedSites.getKey() );
            if ( orders.size() == orderedSiteMap.size() ) {
               return orders;
            }
         }
      }
      return orders;
   }



   static private Collection<String> getOntoTopoCodes( final Collection<String> uris ) {
      return uris.stream()
                 .map( Neo4jOntologyConceptUtil::getIcdoTopoCode )
                 .filter( t -> !t.isEmpty() )
                 .filter( t -> !t.contains( "-" ) )
                 .collect( Collectors.toSet() );
   }

   static private Collection<String> getTableTopoCodes( final Collection<String> uris ) {
      return uris.stream()
                 .map( TopoMorphValidator.getInstance()::getSiteCode )
                 .filter( t -> !t.isEmpty() )
                 .collect( Collectors.toSet() );
   }

}