//package org.healthnlp.deepphe.nlp.summary;
//
//import org.apache.ctakes.core.util.log.LogFileWriter;
//import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.neo4j.node.MentionRelation;
//import org.healthnlp.deepphe.nlp.concept.UriConcept;
//import org.healthnlp.deepphe.nlp.concept.UriConceptCreator;
//import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;
//import org.healthnlp.deepphe.nlp.uri.UriInfoUtil;
//
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
///**
// * @author SPF , chip-nlp
// * @since {12/17/2023}
// */
//final public class SiteSorter {
//
////   static public Collection<UriConcept> getSitedCancerMentions( final String patientId,
////                                                                final Map<Mention, String> patientMentionNoteIds,
////                                                               final Collection<String> cancerUris,
////                                                               final Collection<UriConcept> siteConcepts,
////                                                               final Map<String, List<Mention>> uriMentionsMap,
////                                                               final Collection<MentionRelation> mentionRelations,
////                                                                final List<Double> allMentionConfidences ) {
////      final Map<Mention, Collection<UriConcept>> cancerMentionSiteConcepts
////            = getCancerMentionSiteConcepts( cancerUris, siteConcepts, uriMentionsMap, mentionRelations );
////      final Map<UriConcept, Collection<Mention>> siteCancerMentionsMap
////            = getSiteCancerMentions( cancerMentionSiteConcepts );
////      final Collection<UriConcept> uriConcepts = new HashSet<>();
////      for ( Collection<Mention> sitedMentions : siteCancerMentionsMap.values() ) {
////         final Map<String,List<Mention>> sitedUriMentions = sitedMentions.stream()
////                                   .collect( Collectors.groupingBy( Mention::getClassUri ) );
////         final Map<String,Collection<String>> uriUrisMap = getSiteUriUrisMap( sitedUriMentions );
////         for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
////            final UriConcept uriConcept = UriConceptCreator.createUriConcept( patientId,
////                  patientMentionNoteIds,
////                  uriUris.getKey(),
////                  uriUris.getValue(),
////                  sitedUriMentions,
////                    allMentionConfidences );
////            uriConcepts.add( uriConcept );
////         }
////      }
////      return uriConcepts;
////   }
//
//   static public Collection<UriConcept> getSitedCancerMentions( final String patientId,
//                                                                final Map<Mention, String> patientMentionNoteIds,
//                                                                final Collection<String> cancerUris,
//                                                                final Collection<UriConcept> siteConcepts,
//                                                                final Map<String, List<Mention>> uriMentionsMap,
//                                                                final Collection<MentionRelation> mentionRelations,
//                                                                final List<Double> groupConfidences ) {
//      final Map<Mention, Collection<UriConcept>> cancerMentionSiteConcepts
//              = getCancerMentionSiteConcepts( cancerUris, siteConcepts, uriMentionsMap, mentionRelations );
//      final Map<UriConcept, Collection<Mention>> siteCancerMentionsMap
//              = getSiteCancerMentions( cancerMentionSiteConcepts );
//      final Collection<UriConcept> uriConcepts = new HashSet<>();
//      for ( Collection<Mention> siteCancerMentions : siteCancerMentionsMap.values() ) {
//         final Map<String,List<Mention>> siteUriCancerMentions = siteCancerMentions.stream()
//                 .collect( Collectors.groupingBy( Mention::getClassUri ) );
//         final Map<String,Collection<String>> uriUrisMap = joinSiteUriUrisMap( siteUriCancerMentions, groupConfidences );
//         for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
//            final UriConcept uriConcept = UriConceptCreator.createUriConcept( patientId,
//                    patientMentionNoteIds,
//                    uriUris.getKey(),
//                    uriUris.getValue(),
//                    siteUriCancerMentions,
//                    groupConfidences );
//            uriConcepts.add( uriConcept );
//         }
//      }
//      return uriConcepts;
//   }
//
//
//   static private Map<Mention, Collection<UriConcept>> getCancerMentionSiteConcepts(
//         final Collection<String> cancerUris,
//         final Collection<UriConcept> siteConcepts,
//         final Map<String, List<Mention>> uriMentionsMap,
//         final Collection<MentionRelation> mentionRelations ) {
////      final Collection<Mention> cancerMentions = cancerUris.stream()
////                                                           .map( uriMentionsMap::get )
////                                                           .flatMap( Collection::stream )
////                                                           .collect( Collectors.toList() );
////      final Map<String, Mention> idCancerMentions = cancerMentions.stream()
////                                                           .collect( Collectors.toMap( Mention::getId,
////                                                                 Function.identity() ) );
//      final Map<String, Mention> idCancerMentions = cancerUris.stream()
//              .map( uriMentionsMap::get )
//              .flatMap( Collection::stream )
//              .collect( Collectors.toMap( Mention::getId, Function.identity() ) );
//      final Map<String, UriConcept> mentionIdConceptMap = new HashMap<>();
//      for ( UriConcept concept : siteConcepts ) {
//         concept.getMentions().forEach( m -> mentionIdConceptMap.put( m.getId(), concept ) );
//      }
//      final Map<Mention, Collection<UriConcept>> cancerMentionSiteConcepts = new HashMap<>();
//      for ( MentionRelation mentionRelation : mentionRelations ) {
//         if ( !mentionRelation.getType().equals( RelationConstants.HAS_SITE )
//               && !mentionRelation.getType().equals( RelationConstants.HAS_ASSOCIATED_SITE ) ) {
//            continue;
//         }
//         final String sourceId = mentionRelation.getSourceId();
//         final Mention cancerMention = idCancerMentions.get( sourceId );
//         if ( cancerMention == null ) {
//            // This is probably a mass site instead of a cancer site or vice-versa
////            LogFileWriter.add( "SiteSorter No Cancer Mention for " + sourceId );
//            continue;
//         }
//         final String targetId = mentionRelation.getTargetId();
//         final UriConcept siteConcept = mentionIdConceptMap.get( targetId );
//         if ( siteConcept == null ) {
//            LogFileWriter.add( "SiteSorter No Target Site Concept for " + targetId );
//            continue;
//         }
//         cancerMentionSiteConcepts.computeIfAbsent( cancerMention, c -> new HashSet<>() ).add( siteConcept );
//      }
//      return cancerMentionSiteConcepts;
//   }
//
//
//
//   static private Map<UriConcept,Collection<Mention>> getSiteCancerMentions(
//         final Map<Mention,Collection<UriConcept>> cancerMentionSiteConcepts ) {
//      final Map<UriConcept,Collection<Mention>> siteCancerMentions = new HashMap<>();
//      for ( Map.Entry<Mention,Collection<UriConcept>> cancerSites : cancerMentionSiteConcepts.entrySet() ) {
//         final List<UriConcept> sites = new ArrayList<>( cancerSites.getValue() );
//         if ( sites.isEmpty() ) {
//            continue;
//         }
//         final Mention cancer = cancerSites.getKey();
//         if ( sites.size() == 1 ) {
//            siteCancerMentions.computeIfAbsent( sites.get( 0 ), s -> new HashSet<>() ).add( cancer );
//            continue;
//         }
//         final Map<Double, List<UriConcept>> confidentSites
//               = sites.stream().collect( Collectors.groupingBy( UriConcept::getConfidence ) );
//         final Double max = confidentSites.keySet().stream().mapToDouble( d -> d ).max().orElse( 0 );
//         confidentSites.get( max ).forEach( u -> siteCancerMentions.computeIfAbsent( u, s -> new HashSet<>() )
//                                                                   .add( cancer ) );
//      }
//      return siteCancerMentions;
//   }
//
//
////   static private Map<String, Collection<String>> getSiteUriUrisMap(
////         final Map<String,List<Mention>> sitedUriMentions ) {
////      // Only want the confidences of the cancer uri [mentions] attached to this site.
////      final Map<String,Double> uriConfidencesMap = new HashMap<>();
////      for ( Map.Entry<String, List<Mention>> uriMentions : sitedUriMentions.entrySet() ) {
////         uriConfidencesMap.put( uriMentions.getKey(),
////               ConfidenceCalculator.getMentionsConfidence( uriMentions.getValue() ) );
////      }
////      return UriInfoUtil.getAssociatedUriMap(
////            uriConfidencesMap.keySet(),
////            uriConfidencesMap );
////   }
//
//   static private Map<String, Collection<String>> joinSiteUriUrisMap(
//           final Map<String,List<Mention>> sitedUriMentions,
//           final List<Double> groupConfidences ) {
//      // Only want the confidences of the cancer uri [mentions] attached to this site.
//      final Map<String,List<Double>> uriConfidencesMap = new HashMap<>();
//      final Map<String,Double> uriConfidenceMap = new HashMap<>();
//      for ( Map.Entry<String, List<Mention>> uriMentions : sitedUriMentions.entrySet() ) {
//         final List<Double> confidences = ConfidenceCalculator.getMentionConfidenceList( uriMentions.getValue() );
//         uriConfidencesMap.put( uriMentions.getKey(), confidences );
//         uriConfidenceMap.put( uriMentions.getKey(),
//                 ConfidenceCalculator.calculateConfidence( confidences, 0.1, groupConfidences ) );
//      }
//      final Map<String,Collection<String>> uriUrisMap = UriInfoUtil.getAssociatedUriMap(
//              uriConfidenceMap.keySet(),
//              uriConfidenceMap );
//      final double groupAve = ConfidenceCalculator.getAve( groupConfidences );
//      String topUri = "";
//      double topConfidence = 0;
//      for ( Map.Entry<String,Collection<String>> uriUris : uriUrisMap.entrySet() ) {
//         final List<Double> confidences = uriUris.getValue()
//                 .stream()
//                 .map( uriConfidencesMap::get )
//                 .flatMap( Collection::stream )
//                 .collect(Collectors.toList());
//         final double confidence = ConfidenceCalculator.calculateDefaultConfidence( confidences, groupAve,
//                 groupConfidences.size() );
//         if ( confidence > topConfidence ) {
//            topConfidence = confidence;
//            topUri = uriUris.getKey();
//         }
//      }
//      return Collections.singletonMap( topUri, sitedUriMentions.keySet() );
//   }
//
//
//
//   static public Map<String,Map<String,List<Double>>> getUriSideConfidences( final String patientId,
//                                                                final Map<Mention, String> patientMentionNoteIds,
//                                                                final Collection<String> cancerUris,
//                                                                final Collection<UriConcept> siteConcepts,
//                                                                final Map<String, List<Mention>> uriMentionsMap,
//                                                                final Collection<MentionRelation> mentionRelations,
//                                                                final List<Double> groupConfidences ) {
//      final Map<String,Mention> idMentions = uriMentionsMap.values()
//              .stream()
//              .flatMap( Collection::stream )
//              .collect( Collectors.toMap( Mention::getId, Function.identity() ) );
//      final Map<String,Map<String,List<Double>>> cancerUriSideConfidences = new HashMap<>();
//      for ( MentionRelation relation : mentionRelations ) {
//         if ( !relation.getType().equals( RelationConstants.HAS_LATERALITY ) ) {
//            continue;
//         }
//         final Mention source = idMentions.get( relation.getSourceId() );
//         if ( source == null ) {
//            LogFileWriter.add( "SideSort no source for laterality " + relation.getSourceId() );
//            continue;
//         }
//         final Mention target = idMentions.get( relation.getTargetId() );
//         if ( target == null ) {
//            LogFileWriter.add( "SideSort no target for laterality " + relation.getTargetId() );
//            continue;
//         }
//         cancerUriSideConfidences.computeIfAbsent( source.getClassUri(), s -> new HashMap<>() )
//                 .computeIfAbsent( target.getClassUri(), u -> new ArrayList<>() ).add( relation.getConfidence() );
//      }
//      return cancerUriSideConfidences;
//   }
//
//
//
//}
