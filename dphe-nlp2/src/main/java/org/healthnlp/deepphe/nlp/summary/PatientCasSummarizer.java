package org.healthnlp.deepphe.nlp.summary;


import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.neo4j.node.xn.*;
import org.healthnlp.deepphe.nlp.ae.patient.PatientSummarizer;
import org.healthnlp.deepphe.nlp.attribute.xn.AttributeXnCreator;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptCreator;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.patient.PatientCasCreator;
import org.healthnlp.deepphe.nlp.patient.PatientCasUtil;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {9/20/2023}
 */
final public class PatientCasSummarizer {


   static private final Logger LOGGER = Logger.getLogger( "PatientCasSummarizer" );

   static private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern( "ddMMyyyykkmmss" );

//   static public PatientSummaryXn summarizePatient(final JCas patientCas, final PatientSummarizer.SummaryPrefs prefs ) {
//      final String patientTime = TIME_FORMATTER.format( OffsetDateTime.now() );
//      final String patientId = SourceMetadataUtil.getPatientIdentifier( patientCas );
//      final List<JCas> docCases = PatientCasUtil.getDocCases( patientId );
//      final Map<String,DocumentXn> docIdDocMap = new HashMap<>();
//      final Map<Mention, String> allMentionDocIds = new HashMap<>();
//      final Collection<MentionRelation> allMentionRelations = new ArrayList<>();
//      final Map<Mention,IdentifiedAnnotation> allMentionAnnotationMap = new HashMap<>();
//      DocumentXnCreator.resetCounter();
//      UriConceptCreator.resetCounter();
//      AttributeXnCreator.resetCounter();
//      MentionCreator.resetCounter();
//      NeoplasmAttributesCreator.resetCounter();
//      for ( JCas jCas : docCases ) {
//         final DocumentXn doc = DocumentXnCreator.createDocumentXn( jCas, patientId, patientTime );
//         final String docId = doc.getId();
//         docIdDocMap.put( docId, doc );
//         final Map<IdentifiedAnnotation, Mention> annotationMentionMap
//               = MentionCreator.createAnnotationMentionMap( jCas, patientId, docId, patientTime );
//         doc.setMentions( MentionCreator.sortMentions( annotationMentionMap.values() ) );
//         final List<MentionRelation> relationList
//               = MentionRelationCreator.createRelationList( annotationMentionMap, jCas );
//         doc.setMentionRelations( relationList );
//         annotationMentionMap.values().forEach( m -> allMentionDocIds.put( m, docId ) );
//         allMentionRelations.addAll( doc.getMentionRelations() );
//         annotationMentionMap.forEach( (k,v) -> allMentionAnnotationMap.put( v, k ) );
//      }
//      // Create the joined UriConcepts
//      final Collection<UriConcept> allUriConcepts
//            = UriConceptCreator.createAllUriConcepts( patientId, patientTime, allMentionDocIds, allMentionRelations );
//      final List<Concept> conceptList = ConceptCreator.createConcepts( allUriConcepts );
////      final List<ConceptRelation> conceptRelations = ConceptCreator.createConceptRelations( allUriConcepts );
//      final List<ConceptRelation> conceptRelations = ConceptCreator.createConceptRelations( allUriConcepts );
//      final Map<UriConcept,TumorSummaryXn> tumorSummaryXnMap
//              = createTumorSummaryMap( patientId, patientTime, allUriConcepts, prefs );
//      // Find Cancers
//      final Collection<UriConcept> cancers = prefs.trimCancers( getCancerConcepts( patientId, allUriConcepts ) );
//      final List<CancerSummaryXn> cancerSummaries = new ArrayList<>( cancers.size() );
//      for ( UriConcept cancer : cancers ) {
//         LogFileWriter.add( "Cancer " + cancer.getUri() + " " + cancer.getMentions().size()
//               +  " " + cancer.getConfidence() );
////         LOGGER.info( "Cancer " + cancer.getUri() + " " + cancer.getMentions().size()
////               +  " " + cancer.getConfidence() );
//         // Find Tumors for Cancer
//         final Collection<UriConcept> tumors = prefs.trimTumors( getTumorConcepts( patientId, cancer ) );
//         tumors.stream().map( t -> "   Tumor " + t.getUri() + " " + t.getMentions().size() + " " + t.getConfidence() )
//               .forEach( LogFileWriter::add );
////         tumors.stream().map( t -> "   Tumor " + t.getUri() + " " + t.getMentions().size() + " " + t.getConfidence() )
////               .forEach( LOGGER::info );
//         final List<TumorSummaryXn> tumorSummaries
//               = tumors.stream().map( tumorSummaryXnMap::get )
//                       .sorted( Comparator.comparing( TumorSummaryXn::getClassUri ) )
//                       .collect( Collectors.toList() );
//         cancerSummaries.add( createCancerSummary( cancer, tumorSummaries, patientId, patientTime, prefs ) );
//      }
//      // Fill in the Patient Cas so that it can be interrogated just like a Document Cas.
//      PatientCasCreator.fillPatientCas( patientCas, allUriConcepts, allMentionAnnotationMap );
//      // Trim mention relations within documents.  Do after concept relations are created to use all small relations.
//      for ( DocumentXn doc : docIdDocMap.values() ) {
//         doc.setMentionRelations( prefs.trimMentionRelations( doc.getMentionRelations() ) );
//      }
//      return createPatientSummaryXn( patientId,
//            docIdDocMap.values()
//                       .stream()
//                       .sorted( Comparator.comparing( DocumentXn::getName ) )
//                       .collect( Collectors.toList() ),
//            ConceptCreator.sortConcepts( conceptList ),
//            ConceptCreator.sortConceptRelations( conceptRelations ),
//            cancerSummaries.stream()
//                           .sorted( Comparator.comparing( CancerSummaryXn::getClassUri ) )
//                           .collect( Collectors.toList() ) );
//   }

   /**
    * Reset counters in the node object creators.
    */
   static private void resetCounters() {
      DocumentXnCreator.resetCounter();
      UriConceptCreator.resetCounter();
      AttributeXnCreator.resetCounter();
      MentionCreator.resetCounter();
      NeoplasmAttributesCreator.resetCounter();
   }

   static public PatientSummaryXn summarizePatient(final JCas patientCas, final PatientSummarizer.SummaryPrefs prefs ) {
      resetCounters();
      final String patientTime = TIME_FORMATTER.format( OffsetDateTime.now() );
      final String patientId = SourceMetadataUtil.getPatientIdentifier( patientCas );
      final List<JCas> docCases = PatientCasUtil.getDocCases( patientId );
      final Map<Mention,IdentifiedAnnotation> allMentionAnnotationMap = new HashMap<>();
      // Create all UriConcepts.  Not trimmed.  Also creates all Mentions and all Mention Relations.
      final Map<String,Collection<Mention>> docMentions = new HashMap<>();
      final Map<String,Collection<MentionRelation>> docRelations = new HashMap<>();
      final Collection<UriConcept> allUriConcepts = createAllUriConcepts( patientId, patientTime, docCases,
            docMentions, docRelations, allMentionAnnotationMap );
      final Collection<String> requiredConceptIDs = new HashSet<>();
      final long mentionCount = allUriConcepts.stream().map( UriConcept::getMentions )
                                               .mapToLong( Collection::size ).sum();
      // Find Cancers as UriConcepts
      // Create Map of wanted UriConcept Tumors to UriConcept Cancers.
      // Cancers and Tumors are trimmed and all cancer and tumor concept IDs are added as required.
      final Map<UriConcept,Collection<UriConcept>> cancerTumorsMap
            = createCancerTumorsMap( patientId, allUriConcepts, prefs, requiredConceptIDs );
      // Create CancerSummaries.  Each includes new TumorSummaries and Attributes.
      // Attributes are trimmed and all attribute value concept IDs are added as required.
      final List<CancerSummaryXn> cancerSummaries
            = createCancerSummaries( patientId, patientTime, prefs, cancerTumorsMap, requiredConceptIDs, mentionCount );

      final Collection<UriConcept> goodUriConcepts = prefs.trimAllUriConcepts( allUriConcepts, requiredConceptIDs );
      // Create Concepts from the wanted UriConcepts.
      final List<Concept> conceptList = ConceptCreator.createConcepts( goodUriConcepts );
      // Create Concept Relations based upon wanted UriConcepts.
      final List<ConceptRelation> conceptRelations
            = prefs.trimConceptRelations( ConceptCreator.createConceptRelations( goodUriConcepts ),
            conceptList.stream().map( Concept::getId ).collect( Collectors.toSet() ) );

      // Fill in the Patient Cas so that it can be interrogated just like a Document Cas.
      PatientCasCreator.fillPatientCas( patientCas, goodUriConcepts, conceptRelations, allMentionAnnotationMap );

      final List<DocumentXn> docs
            = createDocuments( patientId, patientTime, docCases, conceptList, docMentions, docRelations );
      return createPatientSummaryXn( patientId,
            docs,
            ConceptCreator.sortConcepts( conceptList ),
            ConceptCreator.sortConceptRelations( conceptRelations ),
            cancerSummaries.stream()
                           .sorted( Comparator.comparing( CancerSummaryXn::getClassUri ) )
                           .collect( Collectors.toList() ) );
   }


   static private List<DocumentXn> createDocuments( final String patientId, final String patientTime,
                                                          final Collection<JCas> docCases,
                                                          final List<Concept> conceptList,
                                                          final Map<String,Collection<Mention>> docMentions,
                                                          final Map<String,Collection<MentionRelation>> docRelations ) {
      final List<DocumentXn> docs = new ArrayList<>( docCases.size() );
      final Collection<String> requiredMentionIDs = conceptList.stream()
                                                     .map( Concept::getMentionIds )
                                                     .flatMap( Collection::stream )
                                                     .collect( Collectors.toSet() );
      for ( JCas jCas : docCases ) {
         final DocumentXn doc = DocumentXnCreator.createDocumentXn( jCas, patientId, patientTime );
         docs.add( doc );
         final String docName = doc.getName();
         doc.setMentions( PatientSummarizer.trimMentions( docMentions.get( docName ), requiredMentionIDs ) );
         doc.setMentionRelations( PatientSummarizer.trimMentionRelations( docRelations.get( docName ), requiredMentionIDs ) );
      }
      docs.sort( Comparator.comparing( DocumentXn::getName ) );
      return docs;
   }


   static private Collection<UriConcept> createAllUriConcepts( final String patientId,
                                                               final String patientTime,
                                                               final Collection<JCas> docCases,
                                                               final Map<String,Collection<Mention>> docMentions,
                                                               final Map<String,Collection<MentionRelation>> docRelations,
                                                               final Map<Mention,IdentifiedAnnotation> allMentionAnnotationMap ) {
      final Map<Mention, String> allMentionDocIds = new HashMap<>();
      final Collection<MentionRelation> allMentionRelations = new ArrayList<>();
      for ( JCas jCas : docCases ) {
         final String docId = DocIdUtil.getDocumentID( jCas );
         final Map<IdentifiedAnnotation, Mention> annotationMentionMap
               = MentionCreator.createAnnotationMentionMap( jCas, patientId, docId, patientTime );
         final List<MentionRelation> relationList
               = MentionRelationCreator.createRelationList( annotationMentionMap, jCas );
         docMentions.put( docId, annotationMentionMap.values() );
         docRelations.put( docId, relationList );
         annotationMentionMap.values().forEach( m -> allMentionDocIds.put( m, docId ) );
         allMentionRelations.addAll( relationList);
         annotationMentionMap.forEach( (k,v) -> allMentionAnnotationMap.put( v, k ) );
      }
      return UriConceptCreator.createAllUriConcepts( patientId, patientTime, allMentionDocIds, allMentionRelations );
   }

//   static public Collection<UriConcept> createAllUriConcepts(
//         final String patientId,
//         final String patientTime ,
//         final Map<Mention, String> patientMentionNoteIds,
//         final Collection<MentionRelation> patientRelations ) {


   static private PatientSummaryXn createPatientSummaryXn( final String patientId,
                                                         final List<DocumentXn> documents,
                                                           final List<Concept> concepts,
                                                           final List<ConceptRelation> conceptRelations,
                                                           final List<CancerSummaryXn> cancers ) {
      LOGGER.info( "PatientCasSummarizer creating PatientSummaryXn: " + patientId );
      final PatientSummaryXn patientSummary = new PatientSummaryXn();
      patientSummary.setId( patientId );
      patientSummary.setName( patientId );
      patientSummary.setDocuments( documents );
      patientSummary.setConcepts( concepts );
      patientSummary.setConceptRelations( conceptRelations );
      patientSummary.setCancers( cancers );
      return patientSummary;
   }


   static private Map<UriConcept,Collection<UriConcept>> createCancerTumorsMap( final String patientId,
                                                                                final Collection<UriConcept> allUriConcepts,
                                                                                final PatientSummarizer.SummaryPrefs prefs,
                                                                                final Collection<String> requiredConceptIDs ) {
      final Collection<UriConcept> cancers = prefs.trimCancers( getCancerConcepts( patientId, allUriConcepts ) );
      final Map<UriConcept,Collection<UriConcept>> cancerTumorsMap = new HashMap<>();
      for ( UriConcept cancer : cancers ) {
         LogFileWriter.add( "Cancer " + cancer.getUri() + " " + cancer.getMentions().size()
               +  " " + cancer.getConfidence() );
               //+ " " + cancer.getGroupedConfidence() );
         requiredConceptIDs.add( cancer.getId() );
         // Find Tumors for Cancer
         final Collection<UriConcept> tumors = prefs.trimTumors( getTumorConcepts( patientId, cancer, prefs ) );
         cancerTumorsMap.put( cancer, tumors );
         tumors.forEach( t -> requiredConceptIDs.add( t.getId() ) );
         tumors.stream().map( t -> "   Tumor " + t.getUri() + " " + t.getMentions().size()
                     + " " + t.getConfidence()
//                     + " " + t.getGroupedConfidence()
               )
               .forEach( LogFileWriter::add );
      }
      return cancerTumorsMap;
   }


   static private List<CancerSummaryXn> createCancerSummaries( final String patientId, final String patientTime,
                                                               final PatientSummarizer.SummaryPrefs prefs,
                                                               final Map<UriConcept,Collection<UriConcept>> cancerTumorsMap,
                                                               final Collection<String> requiredConceptIDs,
                                                               final long mentionCount ) {
      final List<CancerSummaryXn> cancerSummaries = new ArrayList<>( cancerTumorsMap.size() );
      for ( Map.Entry<UriConcept,Collection<UriConcept>> cancerTumors : cancerTumorsMap.entrySet() ) {
         final UriConcept cancer = cancerTumors.getKey();
         final Collection<UriConcept> tumors = cancerTumors.getValue();
         final List<TumorSummaryXn> tumorSummaries
               = tumors.stream()
                       .map( t -> createTumorSummary( t, patientId, patientTime, prefs, mentionCount ) )
                       .sorted( Comparator.comparing( TumorSummaryXn::getClassUri ) )
                       .collect( Collectors.toList() );
         final CancerSummaryXn cancerSummary = createCancerSummary( cancer, tumorSummaries,
               patientId, patientTime, prefs, mentionCount );
         cancerSummaries.add( cancerSummary );
         // The *SummaryXn creator methods have already trimmed the attributes to those fitting min, max, confidence.
         // So we just need to add the UriConcept IDs of the values.
         cancerSummary.getAttributes()
                      .stream()
                      .map( AttributeXn::getValues )
                      .flatMap( Collection::stream )
                      .map( AttributeValue::getConceptIds )
                      .forEach( requiredConceptIDs::addAll );
         tumorSummaries.stream()
                       .map( TumorSummaryXn::getAttributes )
                       .flatMap( Collection::stream )
                       .map( AttributeXn::getValues )
                       .flatMap( Collection::stream )
                       .map( AttributeValue::getConceptIds )
                       .forEach( requiredConceptIDs::addAll );
      }
      cancerSummaries.sort( Comparator.comparing( CancerSummaryXn::getClassUri ) );
      return cancerSummaries;
   }



   static private CancerSummaryXn createCancerSummary( final UriConcept cancer,
                                                       final List<TumorSummaryXn> tumorSummaries,
                                                       final String patientId,
                                                       final String patientTime,
                                                       final PatientSummarizer.SummaryPrefs prefs,
                                                       final long mentionCount ) {
      LOGGER.info( "PatientCasSummarizer creating CancerSummary: " + cancer.getUri() + " " + cancer.getConfidence() );
//            + " " + cancer.getGroupedConfidence() );
      final CancerSummaryXn cancerSummary = new CancerSummaryXn();
//      cancerSummary.setDpheGroup( cancer.getDpheGroup().getName() );
//      cancerSummary.setPreferredText( UriInfoCache.getInstance().getPrefText( cancer.getUri() ) );
      cancerSummary.setId( cancer.getId() + "_C" );
      cancerSummary.setClassUri( cancer.getUri() );
      cancerSummary.setdConfidence( cancer.getConfidence() );
      cancerSummary.setNegated( cancer.isNegated() );
      cancerSummary.setUncertain( cancer.isUncertain() );
      cancerSummary.setHistoric( cancer.inPatientHistory() );
      cancerSummary.setConceptIds( Collections.singletonList( cancer.getId() ) );
      final List<AttributeXn> attributes
              = NeoplasmAttributesCreator.createCancerAttributes( cancer, patientId, patientTime, prefs, mentionCount );
      cancerSummary.setAttributes( prefs.trimAttributes( attributes ) );
      cancerSummary.setTumors( tumorSummaries );
//      cancerSummary.setGroupedConfidence( cancer.getGroupedConfidence() );
      return cancerSummary;
   }

   /**
    *
    * @param patientId -
    * @param patientTime -
    * @param allUriConcepts -
    * @return TumorSummaryXn map for masses AND cancers.  IF a cancer has no tumors then it can be used as its own tumor.
    */
   static private Map<UriConcept,TumorSummaryXn> createTumorSummaryMap( final String patientId,
                                                                        final String patientTime,
                                                                        final Collection<UriConcept> allUriConcepts,
                                                                        final PatientSummarizer.SummaryPrefs prefs,
                                                                        final long mentionCount ) {
      return allUriConcepts.stream()
                     .filter( a -> !a.isNegated() )
                     .filter( c -> c.getDpheGroup() == DpheGroup.MASS || c.getDpheGroup() == DpheGroup.CANCER )
                   .collect( Collectors.toMap( Function.identity(),
                         t -> createTumorSummary( t, patientId, patientTime, prefs, mentionCount ) ) );
   }

   static private TumorSummaryXn createTumorSummary( final UriConcept tumor, final String patientId,
                                                     final String patientTime,
                                                     final PatientSummarizer.SummaryPrefs prefs,
                                                     final long mentionCount ) {
      LOGGER.info( "PatientCasSummarizer creating TumorSummary: " + tumor.getUri() + " " + tumor.getConfidence() );
//            + " " + tumor.getGroupedConfidence() );
      final TumorSummaryXn tumorSummary = new TumorSummaryXn();
//      tumorSummary.setDpheGroup( tumor.getDpheGroup().getName() );
//      tumorSummary.setPreferredText( UriInfoCache.getInstance().getPrefText( tumor.getUri() ) );
      tumorSummary.setId( tumor.getId() + "_T" );
      tumorSummary.setClassUri( tumor.getUri() );
      tumorSummary.setdConfidence( tumor.getConfidence() );
      tumorSummary.setNegated( tumor.isNegated() );
      tumorSummary.setUncertain( tumor.isUncertain() );
      tumorSummary.setHistoric( tumor.inPatientHistory() );
      tumorSummary.setConceptIds( Collections.singletonList( tumor.getId() ) );
      final List<AttributeXn> attributes
              = NeoplasmAttributesCreator.createTumorAttributes( tumor, patientId, patientTime, prefs, mentionCount );
      tumorSummary.setAttributes( prefs.trimAttributes( attributes ) );
//      tumorSummary.setGroupedConfidence( tumor.getGroupedConfidence() );
      return tumorSummary;
   }


   static private Collection<UriConcept> getCancerConcepts( final String patientId, final Collection<UriConcept> concepts ) {
      Collection<UriConcept> cancers = concepts.stream()
                                               .filter( a -> !a.isNegated() )
                                               .filter( c -> c.getDpheGroup() == DpheGroup.CANCER )
                                               .collect( Collectors.toSet() );
      if ( cancers.isEmpty() ) {
         LOGGER.warn( "No Asserted Cancer found for Patient " + patientId );
      }
      if ( cancers.isEmpty() ) {
         cancers = concepts.stream()
                           .filter( a -> !a.isNegated() )
                           .filter( c -> c.getDpheGroup() == DpheGroup.MASS )
                           .collect( Collectors.toSet() );
      }
      if ( cancers.isEmpty() ) {
         LOGGER.warn( "No Asserted Mass/Tumor found for Patient " + patientId );
      }
      return cancers;
   }


   // Use hasMass relation
   static private Collection<UriConcept> getTumorConcepts( final String patientId,
                                                           final UriConcept cancer,
                                                           PatientSummarizer.SummaryPrefs prefs ) {
      final Collection<UriConceptRelation> massRelations
            = prefs.trimTumorRelations( cancer.getRelations( "hasMass" ) );
      if ( massRelations.isEmpty() ) {
         LOGGER.warn( "No Mass associated with " + patientId + " " + cancer.getUri() );
         return Collections.singletonList( cancer );
      }
      Collection<UriConcept> tumors
            = massRelations.stream()
                              .map( UriConceptRelation::getTarget )
                              .filter( c -> !c.isNegated() )
                              .filter( c -> DpheGroup.MASS == c.getDpheGroup() )
                              .collect( Collectors.toList() );
      if ( !tumors.isEmpty() ) {
         return tumors;
      }
      LOGGER.warn( "No Asserted Tumor found for Cancer " + patientId + " " + cancer.getUri() );
      return Collections.singletonList( cancer );
   }



}
