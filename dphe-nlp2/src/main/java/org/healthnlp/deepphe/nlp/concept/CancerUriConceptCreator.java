package org.healthnlp.deepphe.nlp.concept;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.nlp.uri.UriInfoUtil;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.ner.group.dphe.DpheGroup.CANCER;
import static org.apache.ctakes.ner.group.dphe.DpheGroup.MASS;

/**
 * @author SPF , chip-nlp
 * @since {9/21/2023}
 */
final public class CancerUriConceptCreator {

   private CancerUriConceptCreator() {
   }


////   static public Collection<UriConcept> createCancerConcepts( final String patientId,
////                                                               final String patientTime,
////                                                               final Map<Mention, String> patientMentionNoteIds,
////                                                               final Collection<MentionRelation> patientRelations,
////                                                               final Map<String, List<Mention>> uriMentionsMap,
////                                                               final Map<DpheGroup, Collection<String>> groupUrisMap,
////                                                               final Map<DpheGroup,List<Double>> groupMentionConfidenceMap ) {
////      final Collection<UriConcept> concepts = new HashSet<>();
////      final Collection<String> cancerUris = groupUrisMap.get( CANCER );
////      if ( cancerUris != null ) {
////         final List<Double> cancerMentionConfidences = groupMentionConfidenceMap.get( CANCER );
////         LogFileWriter.add( "CancerUriConceptCreator createCancerConcepts CANCER" );
////         concepts.addAll( createCancerConcepts( patientId, patientTime, patientMentionNoteIds, patientRelations,
////               uriMentionsMap, cancerUris, cancerMentionConfidences ) );
////      }
////      final Collection<String> massUris = groupUrisMap.get( MASS );
////      if ( massUris != null ) {
////         final List<Double> massMentionConfidences = groupMentionConfidenceMap.get( MASS );
////         LogFileWriter.add( "CancerUriConceptCreator createCancerConcepts MASS" );
////         concepts.addAll( createCancerConcepts( patientId, patientTime, patientMentionNoteIds, patientRelations,
////               uriMentionsMap, massUris, massMentionConfidences ) );
////      }
////      return concepts;
////   }
//
//   static public Collection<UriConcept> createCancerConcepts( final String patientId,
//                                                              final String patientTime,
//                                                              final Map<Mention, String> patientMentionNoteIds,
//                                                              final Collection<MentionRelation> patientRelations,
//                                                              final Map<String, List<Mention>> uriMentionsMap,
//                                                              final Map<DpheGroup, Collection<String>> groupUrisMap ) {
//      final Collection<UriConcept> concepts = new HashSet<>();
//      final Collection<String> cancerUris = groupUrisMap.get( CANCER );
//      if ( cancerUris != null ) {
//
//         uriMentionsMap.forEach( (k,v) -> uriConfidenceMap.put( k, ConfidenceCalculator.getOwnersConfidence( v ) ) );
//
//
//
//
//         final List<Double> cancerMentionConfidences = groupMentionConfidenceMap.get( CANCER );
//         LogFileWriter.add( "CancerUriConceptCreator createCancerConcepts CANCER" );
//         concepts.addAll( createCancerConcepts( patientId, patientTime, patientMentionNoteIds, patientRelations,
//               uriMentionsMap, cancerUris, cancerMentionConfidences ) );
//      }
//      final Collection<String> massUris = groupUrisMap.get( MASS );
//      if ( massUris != null ) {
//         final List<Double> massMentionConfidences = groupMentionConfidenceMap.get( MASS );
//         LogFileWriter.add( "CancerUriConceptCreator createCancerConcepts MASS" );
//         concepts.addAll( createCancerConcepts( patientId, patientTime, patientMentionNoteIds, patientRelations,
//               uriMentionsMap, massUris, massMentionConfidences ) );
//      }
//      return concepts;
//   }
//
//
//
//   static public Collection<UriConcept> createCancerConcepts( final String patientId,
//                                                              final String patientTime,
//                                                              final Map<Mention, String> patientMentionNoteIds,
//                                                              final Collection<MentionRelation> patientRelations,
//                                                              final Map<String, List<Mention>> uriMentionsMap,
//                                                              final Collection<String> cancerUris,
//                                                              final List<Double> cancerMentionConfidences ) {
//      final Collection<Mention> cancerMentions = cancerUris.stream()
//                                                           .map( uriMentionsMap::get )
//                                                           .filter( Objects::nonNull )
//                                                           .flatMap( Collection::stream )
//                                                           .collect( Collectors.toSet() );
//      final Collection<MentionRelation> cancerMentionRelations = getMentionRelations( cancerMentions, patientRelations );
////      final double cancerMentionPowerSum = ConfidenceCalculator.getPowerSum( cancerMentionConfidences );
////      final List<Double> cancerRelationConfidences = ConfidenceCalculator.getMentionRelationScores( cancerMentionRelations );
////      final double cancerRelationPowerSum = ConfidenceCalculator.getPowerSum( cancerRelationConfidences );
//      return createCancerConcepts( patientId, patientTime, cancerUris,
//            cancerMentionRelations, patientMentionNoteIds, uriMentionsMap,
//            cancerMentionConfidences );
////            cancerRelationConfidences );
//   }
//
//
//   static private Collection<UriConcept> createCancerConcepts( final String patientId, final String patientTime,
//                                                         final Collection<String> cancerUris,
//                                                         final Collection<MentionRelation> cancerMentionRelations,
//                                                         final Map<Mention, String> patientMentionNoteIds,
//                                                               final Map<String, List<Mention>> uriMentionsMap,
//                                                         final List<Double> cancerMentionConfidences ) {
////                                                               final List<Double> cancerRelationConfidences ) {
//      final Collection<UriConcept> uriConcepts = new HashSet<>();
//      final Map<String,Collection<String>> uriUrisMap = UriInfoUtil.getAssociatedUriMap( cancerUris );
//      for ( Map.Entry<String,Collection<String>> uriUris : uriUrisMap.entrySet() ) {
//         LogFileWriter.add( "CancerUriConceptCreator uriUris " + uriUris.getKey() + " : "
//               + String.join( " , ", uriUris.getValue() ) );
//      }
//      for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
//         final UriConcept uriConcept = createCancerConcept( patientId, patientTime,
//               patientMentionNoteIds,
//               uriUris.getKey(),
//               uriUris.getValue(),
//               cancerMentionRelations,
//               uriMentionsMap,
//               cancerMentionConfidences );
////               cancerRelationConfidences );
//         uriConcepts.add( uriConcept );
//      }
//      return uriConcepts;
//   }
//
//
//   static public UriConcept createCancerConcept( final String patientId, final String patientTime,
//                                           final Map<Mention, String> patientMentionNoteIds,
//                                           final String uri,
//                                           final Collection<String> uriChain,
//                                                 final Collection<MentionRelation> cancerRelations,
//                                                 final Map<String, List<Mention>> uriMentionsMap,
//                                                 final List<Double> cancerMentionConfidences ) {
////                                                 final List<Double> cancerRelationConfidences ) {
//      final Collection<String> allChainUris = new HashSet<>( uriChain );
//      allChainUris.add( uri );
//      return createCancerConcept( patientId, patientTime, patientMentionNoteIds, allChainUris, cancerRelations,
//            uriMentionsMap, cancerMentionConfidences );
////            , cancerRelationConfidences );
//   }
//
//
//   static private UriConcept createCancerConcept(final String patientId, final String patientTime,
//                                              final Map<Mention, String> patientMentionNoteIds,
//                                                 final Collection<String> uriChain,
//                                                 final Collection<MentionRelation> allCancerMentionRelations,
//                                              final Map<String, List<Mention>> uriMentionsMap,
//                                              final List<Double> allCancerMentionConfidences ) {
////                                                 final List<Double> allCancerRelConfidences ) {
//      // smaller map of uris to roots that only contains pertinent uris.
//      final Map<String, Collection<String>> thisUriRoots = new HashMap<>();
//      final Collection<Mention> thisMentions = new HashSet<>();
//      for ( String uri : uriChain ) {
//         thisUriRoots.put( uri, UriInfoCache.getInstance().getUriRoots( uri ) );
//         thisMentions.addAll( uriMentionsMap.get( uri ) );
//      }
//      final Map<String, Collection<Mention>> thisNoteIdMentionsMap = new HashMap<>();
//      for ( Mention mention : thisMentions ) {
//         thisNoteIdMentionsMap.computeIfAbsent( patientMentionNoteIds.get( mention ), d -> new HashSet<>() )
//                          .add( mention );
//      }
//      final List<Double> thisMentionConfidences = ConfidenceCalculator.getMentionScores( thisMentions );
//      final double confidence = ConfidenceCalculator.calculateDefaultConfidence( thisMentionConfidences );
////      final double groupedConfidence
////            = ConfidenceCalculator.calculateDefaultConfidence( thisMentionConfidences, allCancerMentionConfidences );
////      final Collection<MentionRelation> thisMentionRels = getMentionRelations( thisMentions, allCancerMentionRelations );
////      final List<Double> thisRelConfidences = ConfidenceCalculator.getMentionRelationConfidenceList( thisMentionRels );
////      final double thisRelConfidence
////            = ConfidenceCalculator.calculateDefaultConfidence( thisRelConfidences, allCancerRelConfidences );
////      final double confidence = ConfidenceCalculator.calculateGoldenMean( thisMentionConfidence, thisRelConfidence );
////      final double confidence = thisMentionConfidence;
////      LogFileWriter.add( "CancerUriConceptCreator uris " + String.join( " , ", thisUris ) );
////      LogFileWriter.add( "CancerUriConceptCreator thisMentionRels " + thisRelConfidences.stream().map( String::valueOf ).collect(
////            Collectors.joining(" , " ) ) + " allMentionRels " + allCancerRelConfidences.stream().map( String::valueOf ).collect(
////            Collectors.joining(" , " ) ) );
////      LogFileWriter.add( "CancerUriConceptCreator mention : " + thisMentionConfidence + " relation " + thisRelConfidence
////            + " = " + ConfidenceCalculator.calculateGoldenMean( thisMentionConfidence, thisRelConfidence ) );
//      return new DefaultUriConcept( patientId, patientTime, thisUriRoots, thisNoteIdMentionsMap, confidence );
////            , groupedConfidence );
//   }
//
//
//
//
//   static private UriConcept createCancerConcept(final String patientId, final String patientTime,
//                                                 final Map<Mention, String> patientMentionNoteIds,
//                                                 final Collection<String> thisUris,
//                                                 final Map<String, List<Mention>> uriMentionsMap ) {
//      // smaller map of uris to roots that only contains pertinent uris.
//      final Map<String, Collection<String>> thisUriRoots = new HashMap<>();
//      final Collection<Mention> thisMentions = new HashSet<>();
//      for ( String uri : thisUris ) {
//         thisUriRoots.put( uri, UriInfoCache.getInstance().getUriRoots( uri ) );
//         thisMentions.addAll( uriMentionsMap.get( uri ) );
//      }
//      final Map<String, Collection<Mention>> thisNoteIdMentionsMap = new HashMap<>();
//      for ( Mention mention : thisMentions ) {
//         thisNoteIdMentionsMap.computeIfAbsent( patientMentionNoteIds.get( mention ), d -> new HashSet<>() )
//                              .add( mention );
//      }
//      final double confidence = ConfidenceCalculator.getOwnersConfidence( thisMentions );
////      final Collection<MentionRelation> thisMentionRels = getMentionRelations( thisMentions, allCancerMentionRelations );
////      final List<Double> thisRelConfidences = ConfidenceCalculator.getMentionRelationConfidenceList( thisMentionRels );
////      final double thisRelConfidence
////            = ConfidenceCalculator.calculateDefaultConfidence( thisRelConfidences, allCancerRelConfidences );
////      final double confidence = ConfidenceCalculator.calculateGoldenMean( thisMentionConfidence, thisRelConfidence );
////      final double confidence = thisMentionConfidence;
////      LogFileWriter.add( "CancerUriConceptCreator uris " + String.join( " , ", thisUris ) );
////      LogFileWriter.add( "CancerUriConceptCreator thisMentionRels " + thisRelConfidences.stream().map( String::valueOf ).collect(
////            Collectors.joining(" , " ) ) + " allMentionRels " + allCancerRelConfidences.stream().map( String::valueOf ).collect(
////            Collectors.joining(" , " ) ) );
////      LogFileWriter.add( "CancerUriConceptCreator mention : " + thisMentionConfidence + " relation " + thisRelConfidence
////            + " = " + ConfidenceCalculator.calculateGoldenMean( thisMentionConfidence, thisRelConfidence ) );
//      return new DefaultUriConcept( patientId, patientTime, thisUriRoots, thisNoteIdMentionsMap, confidence );
//   }
//
//
//
//
//
//
//
//
//
//
//   static private Collection<MentionRelation> getMentionRelations( final Collection<Mention> sourceMentions,
//                                               final Collection<MentionRelation> mentionRelations ) {
//      if ( sourceMentions.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final Collection<String> sourceIds = sourceMentions.stream().map( Mention::getId ).collect( Collectors.toSet() );
//      return mentionRelations.stream().filter( r -> sourceIds.contains( r.getSourceId() ) ).collect( Collectors.toList() );
//   }



}
