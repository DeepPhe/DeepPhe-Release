package org.healthnlp.deepphe.nlp.concept;

import org.apache.ctakes.core.util.IdCounter;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.ner.group.dphe.DpheGroupAccessor;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.nlp.uri.UriInfoUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.ctakes.ner.group.dphe.DpheGroup.*;

/**
 * @author SPF , chip-nlp
 * @since {9/21/2023}
 */
final public class UriConceptCreator {

   private UriConceptCreator() {
   }

   static private final IdCounter ID_COUNTER = new IdCounter();

   static public IdCounter getIdCounter() {
      return ID_COUNTER;
   }

   static public void resetCounter() {
      ID_COUNTER.reset();
   }

   /**
    * @param patientId             -
    * @param patientMentionNoteIds -
    * @param patientRelations      -
    * @return map of best uri to its concept instances
    */
   static public Collection<UriConcept> createAllUriConcepts(
         final String patientId,
         final String patientTime ,
         final Map<Mention, String> patientMentionNoteIds,
         final Collection<MentionRelation> patientRelations ) {
      final long mentionCount = patientMentionNoteIds.size();
      final Map<Boolean, Collection<Mention>> assertedMentions = sortAssertedMentions( patientMentionNoteIds.keySet() );
      final Collection<UriConcept> concepts = new HashSet<>();
//      LogFileWriter.add( "UriConceptCreator creatUriConcepts TRUE" );
      concepts.addAll( createUriConcepts( patientId,
            patientTime,
            patientMentionNoteIds,
            assertedMentions.getOrDefault( Boolean.TRUE, Collections.emptyList() ),
            patientRelations, mentionCount ) );
//      LogFileWriter.add( "UriConceptCreator creatUriConcepts FALSE" );
      concepts.addAll( createUriConcepts( patientId,
            patientTime,
            patientMentionNoteIds,
            assertedMentions.getOrDefault( Boolean.FALSE, Collections.emptyList() ),
            patientRelations, mentionCount ) );
      addRelations( concepts, patientRelations );
      return concepts;
   }


   static private Collection<UriConcept> createUriConcepts( final String patientId,
                                                            final String patientTime,
                                                            final Map<Mention, String> patientMentionNoteIds,
                                                            final Collection<Mention> assertTFMentions,
                                                            final Collection<MentionRelation> patientRelations,
                                                            final long mentionCount ) {
      if ( assertTFMentions.isEmpty() ) {
         return Collections.emptyList();
      }
      // Map of unique URIs to all Mentions with that URI.
      final Map<String, List<Mention>> uriMentionsMap
            = assertTFMentions.stream().collect( Collectors.groupingBy( Mention::getClassUri ) );
//      final Map<DpheGroup, Collection<String>> groupUrisMap = new EnumMap<>( DpheGroup.class );
//      final Map<DpheGroup,List<Double>> groupMentionConfidenceMap = new EnumMap<>( DpheGroup.class );
//      for ( Map.Entry<String, List<Mention>> uriMentions : uriMentionsMap.entrySet() ) {
//         final DpheGroup group = DpheGroup.getBestMentionGroup( uriMentions.getValue() );
//         groupUrisMap.computeIfAbsent( group, g -> new HashSet<>() ).add( uriMentions.getKey() );
//         final List<Double> mentionConfidences = ConfidenceCalculator.getMentionConfidenceList( uriMentions.getValue() );
//         groupMentionConfidenceMap.computeIfAbsent( group, g -> new ArrayList<>() ).addAll( mentionConfidences );
//      }
      final Map<DpheGroup, List<String>> groupUrisMap
            = uriMentionsMap.keySet()
                            .stream()
                            .collect( Collectors.groupingBy( UriInfoCache.getInstance()::getDpheGroup ) );
      final Map<String,Double> uriConfidenceMap = new HashMap<>();
      uriMentionsMap.forEach( (k,v) -> uriConfidenceMap.put( k, ConfidenceCalculator.getOwnersConfidence( v, mentionCount ) ) );

      final Collection<UriConcept> uriConcepts = new HashSet<>();
      // 3/21/2024 I know that we were going to use site concepts, but can it be moved?
//      final Collection<UriConcept> siteConcepts = new HashSet<>();
      for ( Map.Entry<DpheGroup,List<String>> groupUris : groupUrisMap.entrySet() ) {
//         final DpheGroup group = groupUris.getKey();
         // 3/21/2024 handle Cancer and Mass the same as other concept types.
//         if ( group == CANCER || group == MASS ) {
//            continue;
//         }
//         final List<Double> groupMentionConfidences = groupMentionConfidenceMap.get( groupUris.getKey() );
//         final double groupAve = ConfidenceCalculator.getAve( groupConfidences );
//         final double groupPowerSum = ConfidenceCalculator.getPowerSum( groupMentionConfidences );
         final Collection<String> uris = groupUris.getValue();
//         for ( String uri : uris ) {
//            final List<Double> mentionConfidences = ConfidenceCalculator.getMentionConfidenceList( uriMentionsMap.get( uri ) );
//            final double confidence = ConfidenceCalculator.calculateDefaultConfidence( mentionConfidences, groupPowerSum,
//                    groupMentionConfidences.size() );
//            uriConfidenceMap.put( uri, confidence );
//         }
         final Map<String,Collection<String>> uriUrisMap = UriInfoUtil.getAssociatedUriMap( uris, uriConfidenceMap );
         for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
            final UriConcept uriConcept = createUriConcept( patientId, patientTime,
                  patientMentionNoteIds,
                  uriUris.getKey(),
                  uriUris.getValue(),
                  uriMentionsMap,
                  mentionCount );
//                    groupMentionConfidences );
            uriConcepts.add( uriConcept );
//            if ( group == BODY_PART ) {
//               siteConcepts.add( uriConcept );
//            }
         }
      }
//      uriConcepts.addAll( CancerUriConceptCreator.createCancerConcepts( patientId, patientMentionNoteIds, patientRelations,
//              uriMentionsMap, groupUrisMap, siteConcepts, groupMentionConfidenceMap ) );
//      uriConcepts.addAll( CancerUriConceptCreator.createCancerConcepts( patientId, patientMentionNoteIds, patientRelations,
//            uriMentionsMap, groupUrisMap, siteConcepts ) );
      return uriConcepts;
   }

//   static private final Collection<DpheGroup> COMPETITIVE = new HashSet<>( Arrays.asList(
//         LATERALITY, SEVERITY, STATUS, BODY_PART, ORGAN_SYSTEM, BODY_CAVITY, BODY_REGION, ORGAN, TISSUE,
//         BODY_FLUID_OR_SUBSTANCE, DISEASE_STAGE_QUALIFIER, DISEASE_GRADE_QUALIFIER, BEHAVIOR
//   ));

//   static private Collection<UriConcept> createUriConcepts( final String patientId, final String patientTime,
//                                                            final Map<Mention, String> patientMentionNoteIds,
//                                                            final Collection<Mention> assertTFMentions,
//                                                            final Collection<MentionRelation> patientRelations ) {
//      if ( assertTFMentions.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      // Map of unique URIs to all Mentions with that URI.
//      final Map<String, List<Mention>> uriMentionsMap
//            = assertTFMentions.stream().collect( Collectors.groupingBy( Mention::getClassUri ) );
//      final Map<DpheGroup, Collection<String>> groupUrisMap = new EnumMap<>( DpheGroup.class );
//      final Map<DpheGroup,List<Double>> groupMentionConfidenceMap = new EnumMap<>( DpheGroup.class );
//      for ( Map.Entry<String, List<Mention>> uriMentions : uriMentionsMap.entrySet() ) {
//         final DpheGroup group = DpheGroup.getBestMentionGroup( uriMentions.getValue() );
//         groupUrisMap.computeIfAbsent( group, g -> new HashSet<>() ).add( uriMentions.getKey() );
//         final List<Double> mentionConfidences = ConfidenceCalculator.getMentionScores( uriMentions.getValue() );
//         groupMentionConfidenceMap.computeIfAbsent( group, g -> new ArrayList<>() ).addAll( mentionConfidences );
//      }
//      final Collection<UriConcept> uriConcepts = new HashSet<>();
//      for ( Map.Entry<DpheGroup,Collection<String>> groupUris : groupUrisMap.entrySet() ) {
//         final DpheGroup group = groupUris.getKey();
//         if ( group == CANCER || group == MASS ) {
//            continue;
//         }
////         LogFileWriter.setWriteLogs( group == BODY_PART );
//         uriConcepts.addAll( createConcepts( patientId, patientTime, groupUris, patientMentionNoteIds, uriMentionsMap,
//               groupMentionConfidenceMap.get( group ), COMPETITIVE.contains( group ) ) );
//      }
//      LogFileWriter.setWriteLogs( true );
//      uriConcepts.addAll( CancerUriConceptCreator.createCancerConcepts( patientId, patientTime, patientMentionNoteIds, patientRelations,
//            uriMentionsMap, groupUrisMap, groupMentionConfidenceMap ) );
//      return uriConcepts;
//   }

//   static private Collection<UriConcept> createUriConcepts( final String patientId, final String patientTime,
//                                                            final Map<Mention, String> patientMentionNoteIds,
//                                                            final Collection<Mention> assertTFMentions,
//                                                            final Collection<MentionRelation> patientRelations ) {
//      if ( assertTFMentions.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      // Map of unique URIs to all Mentions with that URI.
//      final Map<String, List<Mention>> uriMentionsMap
//            = assertTFMentions.stream().collect( Collectors.groupingBy( Mention::getClassUri ) );
//      final Map<DpheGroup, Collection<String>> groupUrisMap = new EnumMap<>( DpheGroup.class );
//      final Map<DpheGroup,List<Double>> groupMentionConfidenceMap = new EnumMap<>( DpheGroup.class );
//      for ( Map.Entry<String, List<Mention>> uriMentions : uriMentionsMap.entrySet() ) {
//         final DpheGroup group = DpheGroup.getBestMentionGroup( uriMentions.getValue() );
//         groupUrisMap.computeIfAbsent( group, g -> new HashSet<>() ).add( uriMentions.getKey() );
//         final List<Double> mentionConfidences = ConfidenceCalculator.getScores( uriMentions.getValue() );
//         groupMentionConfidenceMap.computeIfAbsent( group, g -> new ArrayList<>() ).addAll( mentionConfidences );
//      }
//      final Collection<UriConcept> uriConcepts = new HashSet<>();
//      for ( Map.Entry<DpheGroup,Collection<String>> groupUris : groupUrisMap.entrySet() ) {
//         final DpheGroup group = groupUris.getKey();
//         if ( group == CANCER || group == MASS ) {
//            continue;
//         }
////         LogFileWriter.setWriteLogs( group == BODY_PART );
//         uriConcepts.addAll( createConcepts( patientId, patientTime, groupUris, patientMentionNoteIds, uriMentionsMap,
//               groupMentionConfidenceMap.get( group ), COMPETITIVE.contains( group ) ) );
//      }
//      LogFileWriter.setWriteLogs( true );
//      uriConcepts.addAll( CancerUriConceptCreator.createCancerConcepts( patientId, patientTime, patientMentionNoteIds, patientRelations,
//            uriMentionsMap, groupUrisMap, groupMentionConfidenceMap ) );
//      return uriConcepts;
//   }

//   static private Collection<UriConcept> createConcepts( final String patientId, final String patientTime,
//                                                         final Map.Entry<DpheGroup,Collection<String>> groupUris,
//                                                         final Map<Mention, String> patientMentionNoteIds,
//                                                         final Map<String, List<Mention>> uriMentionsMap,
//                                                         final List<Double> groupMentionConfidences,
//                                                         final boolean competitive ) {
//      final Collection<UriConcept> uriConcepts = new HashSet<>();
//      final Map<String,Collection<String>> uriUrisMap = UriInfoUtil.getAssociatedUriMap( groupUris.getValue() );
//      for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
//         final UriConcept uriConcept = createConcept( patientId, patientTime,
//               patientMentionNoteIds,
//               uriUris.getKey(),
//               uriUris.getValue(),
//               uriMentionsMap,
//               groupMentionConfidences,
//               competitive );
//         uriConcepts.add( uriConcept );
//      }
//      return uriConcepts;
//   }


//   static private Collection<UriConcept> createConcepts( final String patientId,
//                                                         final Map<Mention, String> patientMentionNoteIds,
//                                                         final Map<String, List<Mention>> uriMentionsMap ) {
//      final Map<DpheGroup, Collection<String>> groupUrisMap = new HashMap<>();
//      final Map<String,Double> uriConfidencesMap = new HashMap<>();
//      for ( Map.Entry<String, List<Mention>> uriMentions : uriMentionsMap.entrySet() ) {
//         groupUrisMap.computeIfAbsent( DpheGroup.getBestMentionGroup( uriMentions.getValue() ), t -> new HashSet<>() )
//                 .add( uriMentions.getKey() );
//         uriConfidencesMap.put( uriMentions.getKey(),
//                 ConfidenceCalculator.getMentionsConfidence( uriMentions.getValue() ) );
//      }
//      final Collection<UriConcept> concepts = new HashSet<>();
//      for ( Map.Entry<DpheGroup, Collection<String>> groupUris : groupUrisMap.entrySet() ) {
//         final Collection<String> uris = groupUris.getValue();
////         final Map<String, Collection<String>> uriUrisMap = UriInfoUtil.getAssociatedUriMap( uris );
//         final Map<String,Collection<String>> uriUrisMap = UriInfoUtil.getAssociatedUriMap( uris, uriConfidencesMap );
//         for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
//            final UriConcept otherConcept = createUriConcept( patientId,
//                    patientMentionNoteIds,
//                    uriUris.getKey(),
//                    uriUris.getValue(),
//                    uriMentionsMap );
//            concepts.add( otherConcept );
//         }
//      }
////      final Map<UriConcept,Collection<Mention>> siteCancerMentions = SiteSorter.getSitedCancerMentions( groupUrisMap.get( DpheGroup.CANCER ), uriMentionsMap, mentionRelations, mentionIdConceptMap );
//      return concepts;
//   }


//   static public UriConcept createConcept( final String patientId, final String patientTime,
//                                           final Map<Mention, String> patientMentionNoteIds,
//                                           final String uri,
//                                           final Collection<String> otherUris,
//                                           final Map<String, List<Mention>> uriMentionsMap,
//                                           final List<Double> groupMentionConfidences,
//                                           final boolean competitive) {
//      final Collection<String> allOtherUris = new HashSet<>( otherUris );
//      allOtherUris.add( uri );
//      return createConcept( patientId, patientTime, patientMentionNoteIds, allOtherUris, uriMentionsMap,
//            groupMentionConfidences, competitive );
//   }
//
//   static private UriConcept createConcept( final String patientId, final String patientTime,
//                                            final Map<Mention, String> patientMentionNoteIds,
//                                            final Collection<String> uris,
//                                            final Map<String, List<Mention>> uriMentionsMap,
//                                            final List<Double> groupMentionConfidences,
//                                            final boolean competitive ) {
//      // smaller map of uris to roots that only contains pertinent uris.
//      final Map<String, Collection<String>> uriRoots = new HashMap<>();
//      final Collection<Mention> mentions = new HashSet<>();
//      for ( String uri : uris ) {
//         uriRoots.put( uri, UriInfoCache.getInstance().getUriRoots( uri ) );
//         mentions.addAll( uriMentionsMap.get( uri ) );
//      }
//      final Map<String, Collection<Mention>> noteIdMentionsMap = new HashMap<>();
//      for ( Mention mention : mentions ) {
//         noteIdMentionsMap.computeIfAbsent( patientMentionNoteIds.get( mention ), d -> new HashSet<>() )
//                          .add( mention );
//      }
//      final List<Double> mentionConfidences = ConfidenceCalculator.getMentionScores( mentions );
//      final double confidence = ConfidenceCalculator.calculateDefaultConfidence( mentionConfidences );
//      double groupedConfidence;
//      if ( competitive ) {
//         groupedConfidence = ConfidenceCalculator.calculateDefaultConfidence( mentionConfidences, groupMentionConfidences );
//      } else {
//         groupedConfidence = confidence/100;
//      }
//      LogFileWriter.add( "UriConceptCreator Confidence: " + confidence + " mentionConfidences: "
//            + mentionConfidences.stream().map( Object::toString ).collect(
//            Collectors.joining( " " )) );
//      return new DefaultUriConcept( patientId, patientTime, uriRoots, noteIdMentionsMap, confidence, groupedConfidence );
//   }

//   /**
//    *
//    * @param mentions the mentions in text, any and all document(s)
//    * @return map of URI to mentions having that exact uri
//    */
//   static private Map<String,List<Mention>> mapUriMentions( final Collection<Mention> mentions ) {
//      return mentions.stream().collect( Collectors.groupingBy( Mention::getClassUri ) );
//   }

   static private Map<Boolean, Collection<Mention>> sortAssertedMentions( final Collection<Mention> mentions ) {
      final Map<Boolean, Collection<Mention>> sortedMentions = new HashMap<>( 2 );
      for ( Mention mention : mentions ) {
         if ( mention.isNegated() ) {
            sortedMentions.computeIfAbsent( Boolean.FALSE, m -> new HashSet<>() ).add( mention );
         } else {
            sortedMentions.computeIfAbsent( Boolean.TRUE, m -> new HashSet<>() ).add( mention );
         }
      }
      return sortedMentions;
   }

   static private void addRelations( final Collection<UriConcept> concepts,
                                     final Collection<MentionRelation> mentionRelations ) {
      final Map<String, UriConcept> mentionIdConceptMap = new HashMap<>();
      for ( UriConcept concept : concepts ) {
//         if ( concept.isNegated() ) {
//            continue;
//         }
         concept.getMentions().forEach( m -> mentionIdConceptMap.put( m.getId(), concept ) );
      }
      final Map<UriConcept, Collection<MentionRelation>> sourceMentionRelationsMap = new HashMap<>();
      for ( MentionRelation mentionRelation : mentionRelations ) {
         final String sourceId = mentionRelation.getSourceId();
         final UriConcept sourceConcept = mentionIdConceptMap.get( sourceId );
         if ( sourceConcept == null ) {
            LogFileWriter.add( "UriConceptCreator No Source Concept for " + sourceId + " " + mentionRelation.getType() );
            continue;
         }
         sourceMentionRelationsMap.computeIfAbsent( sourceConcept, a -> new HashSet<>() )
                                  .add( mentionRelation );
      }
      for ( Map.Entry<UriConcept, Collection<MentionRelation>> sourceMentionRelations :
            sourceMentionRelationsMap.entrySet() ) {
         addRelations( sourceMentionRelations.getKey(), mentionIdConceptMap, sourceMentionRelations.getValue() );
      }
   }

   static private void addRelations( final UriConcept sourceConcept,
                                     final Map<String, UriConcept> mentionIdConceptMap,
                                     final Collection<MentionRelation> mentionRelations ) {
      final long mentionCount = mentionIdConceptMap.size();
      final Map<String, List<MentionRelation>> typeMentionRelationsMap
            = mentionRelations.stream()
                              .collect( Collectors.groupingBy( getRelationType ) );
      for ( Map.Entry<String, List<MentionRelation>> typeMentionRelations : typeMentionRelationsMap.entrySet() ) {
         addRelations( typeMentionRelations.getKey(), sourceConcept, mentionIdConceptMap,
               typeMentionRelations.getValue(), mentionCount );
      }
   }

   /**
    * We want to combine primary site and associated site.  Anything in both counts as 2.
    */
   static private final Function<MentionRelation, String> getRelationType
         = mentionRelation -> mentionRelation.getType().equals( RelationConstants.HAS_SITE )
                              ? RelationConstants.HAS_ASSOCIATED_SITE : mentionRelation.getType();


   static private void addRelations( final String type,
                                     final UriConcept sourceConcept,
                                     final Map<String, UriConcept> mentionIdConceptMap,
                                     final Collection<MentionRelation> mentionRelations,
                                     final long mentionCount ) {
      final Map<UriConcept, Collection<MentionRelation>> targetMentionRelationsMap = new HashMap<>();
      final List<Double> typeRelationConfidences = new ArrayList<>( mentionRelations.size() );
      for ( MentionRelation mentionRelation : mentionRelations ) {
         final UriConcept targetConcept = mentionIdConceptMap.get( mentionRelation.getTargetId() );
         if ( targetConcept == null ) {
            LogFileWriter.add( "UriConceptCreator No Target Concept for " + mentionRelation.getTargetId() );
            continue;
         }
         targetMentionRelationsMap.computeIfAbsent( targetConcept, a -> new HashSet<>() ).add( mentionRelation );
         typeRelationConfidences.add( mentionRelation.getdConfidence() );
      }
//      final double aveConfidence = ConfidenceCalculator.getAve( allConfidences );
//      final double typeSum = ConfidenceCalculator.getPowerSum( typeRelationConfidences );
      for ( Map.Entry<UriConcept, Collection<MentionRelation>> targetMentionRelations
            : targetMentionRelationsMap.entrySet() ) {
//         final double confidence
//                 = ConfidenceCalculator.calculateDefaultConfidence(
//                         ConfidenceCalculator.getMentionRelationScores( targetMentionRelations.getValue() ),
//                 typeSum, typeRelationConfidences.size() );
         final double confidence = ConfidenceCalculator.getOwnersConfidence( targetMentionRelations.getValue(), mentionCount );
         final UriConceptRelation relation = new DefaultUriConceptRelation( type,
               targetMentionRelations.getKey(),
               targetMentionRelations.getValue(),
                 confidence );
//         LogFileWriter.add( "UriConceptCreator Adding ConceptRelation " + type + " to "
//               + sourceConcept + " : " + relation.getTarget().getUri() );
         sourceConcept.addRelation( relation );
      }
   }





   // 3/20/2024
   static private Collection<UriConcept> createUriConcepts( final String patientId, final String patientTime,
                                                         final Map.Entry<DpheGroup,Collection<String>> groupUris,
                                                         final Map<Mention, String> patientMentionNoteIds,
                                                         final Map<String, List<Mention>> uriMentionsMap,
                                                            final long mentionCount ) {
      final Collection<UriConcept> uriConcepts = new HashSet<>();
      final Map<String,Collection<String>> uriUrisMap = UriInfoUtil.getAssociatedUriMap( groupUris.getValue() );
      for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
         final UriConcept uriConcept = createUriConcept( patientId, patientTime,
               patientMentionNoteIds,
               uriUris.getKey(),
               uriUris.getValue(),
               uriMentionsMap, mentionCount );
         uriConcepts.add( uriConcept );
      }
      return uriConcepts;
   }

   static public UriConcept createUriConcept( final String patientId, final String patientTime,
                                           final Map<Mention, String> patientMentionNoteIds,
                                           final String uri,
                                           final Collection<String> otherUris,
                                           final Map<String, List<Mention>> uriMentionsMap,
                                              final long mentionCount ) {
      final Collection<String> allOtherUris = new HashSet<>( otherUris );
      allOtherUris.add( uri );
      return createUriConcept( patientId, patientTime, patientMentionNoteIds, allOtherUris, uriMentionsMap, mentionCount );
   }

   static private UriConcept createUriConcept( final String patientId, final String patientTime,
                                            final Map<Mention, String> patientMentionNoteIds,
                                            final Collection<String> uris,
                                            final Map<String, List<Mention>> uriMentionsMap,
                                               final long mentionCount ) {
      // smaller map of uris to roots that only contains pertinent uris.
      final Map<String, Collection<String>> uriRoots = new HashMap<>();
      final Collection<Mention> mentions = new HashSet<>();
      for ( String uri : uris ) {
         uriRoots.put( uri, UriInfoCache.getInstance().getUriRoots( uri ) );
         mentions.addAll( uriMentionsMap.get( uri ) );
      }
      final Map<String, Collection<Mention>> noteIdMentionsMap = new HashMap<>();
      for ( Mention mention : mentions ) {
         noteIdMentionsMap.computeIfAbsent( patientMentionNoteIds.get( mention ), d -> new HashSet<>() )
                          .add( mention );
      }
      final double confidence = ConfidenceCalculator.getOwnersConfidence( mentions, mentionCount );
      LogFileWriter.add( "UriConceptCreator Confidence: " + confidence + " " + mentions.size() + " " + String.join( " ", uris ) );
      final UriConcept concept = new DefaultUriConcept( patientId, patientTime, uriRoots, noteIdMentionsMap );
      concept.setConfidence( ConfidenceCalculator.getConceptConfidence( concept, mentionCount ) );
      return concept;
   }







   static public List<UriConcept> sortConcepts( final Collection<UriConcept> concepts ) {
      return concepts.stream().sorted( CONCEPT_COMPARATOR ).collect( Collectors.toList() );
   }

   static public List<String> sortConceptIds( final Collection<UriConcept> concepts ) {
      return concepts.stream().sorted( CONCEPT_COMPARATOR ).map( UriConcept::getId ).collect(Collectors.toList());
   }

   static private final Comparator<UriConcept> CONCEPT_COMPARATOR = (c1, c2) -> {
      final int typeCompare = String.CASE_INSENSITIVE_ORDER.compare( c1.getDpheGroup().getName(),
              c2.getDpheGroup().getName() );
      if ( typeCompare != 0 ) {
         return typeCompare;
      }
      final int confidenceCompare = Double.compare( c1.getConfidence(), c2.getConfidence() );
      if ( confidenceCompare != 0 ) {
         return -1 * confidenceCompare;
      }
      return String.CASE_INSENSITIVE_ORDER.compare( c1.getUri(), c2.getUri() );
   };

}
