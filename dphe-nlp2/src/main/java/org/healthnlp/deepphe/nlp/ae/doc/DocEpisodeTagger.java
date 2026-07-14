package org.healthnlp.deepphe.nlp.ae.doc;

import org.apache.ctakes.core.util.MutableUimaContext;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Episode;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.healthnlp.deepphe.nlp.ae.section.Sectionizer;

import java.util.*;
import java.util.Map.Entry;

/**
 * An AE that extracts features for episode classification.
 *
 * @author Chen Lin
 */
final public class DocEpisodeTagger extends CleartkAnnotator<String> {

   public static final String NO_CATEGORY = "unknown";

   static private final String CLASSIFIER_JAR_PATH = "classifierJarPath";

   static private final String BREAST_MODEL = "/org/healthnlp/deepphe/episode/breast/model.jar";

   static private final String SIMPLE_SEGMENT = Sectionizer.SIMPLE_SEGMENT.toLowerCase();

   /*
    * Implement the standard UIMA initialize method.
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      Object model = context.getConfigParameterValue( CLASSIFIER_JAR_PATH );
      if ( model != null ) {
         super.initialize( context );
         return;
      }
      final MutableUimaContext mutableContext = new MutableUimaContext( context );
      mutableContext.setConfigParameter( CLASSIFIER_JAR_PATH, BREAST_MODEL );
      super.initialize( mutableContext );
   }

   /*
    * Implement the standard UIMA process method.
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      if ( MelanomaDocEpisodeTagger.hasDomainUris( jCas ) ) {
         return;
      }

      final CoveredTextExtractor<WordToken> extractor = new CoveredTextExtractor<>();

      //extract features:
      final List<Feature> features = new ArrayList<>();

      //get the document type feature
      final SourceData sourceData = SourceMetadataUtil.getSourceData( jCas );
      final String docType = createDocumentType( sourceData );
      features.add( new Feature( "document_type", docType ) );

      boolean ifAPSectionFuturistic = false;
      boolean ifAPSectionAboutTreatment = false;
      boolean ifContainsIL2 = false;

      //iterate through all segments
      for ( Segment segment : JCasUtil.select( jCas, Segment.class ) ) {
         //get segment feature:
         final String segId = segment.getId().toLowerCase();
         if ( segId.equals( SIMPLE_SEGMENT ) ) {
            continue;
         }

         final Collection<EventMention> eventMentions = JCasUtil.selectCovered( jCas, EventMention.class, segment );
         if ( segId.contains( "plan" ) ) {
            features.add( new Feature( "contains_AssessmentAndPlan_Section" ) );
            for ( EventMention event : eventMentions ) {
               if ( !isEventAffirmed( event ) || event.getEvent() == null ) {
                  continue;
               }
               if ( event.getClass().getSimpleName().contains( "Procedure" ) ) {
                  features.add( new Feature( "PlanSectionContainsProcedure" ) );
               }
               String dtl = event.getEvent().getProperties().getDocTimeRel();
               if ( dtl.contains( "AFTER" ) ) {
                  ifAPSectionFuturistic = true;
                  features.add( new Feature( "PlanAboutNextStep" ) );
               } else if ( dtl.equals( "BEFORE/OVERLAP" ) ) {
                  ifAPSectionAboutTreatment = true;
                  features.add( new Feature( "PlanIncludingPastOrPresentTreatment" ) );
               }
            }
         }

         if ( !segId.contains( "history" ) ) {
            final int featureCount = features.size();
            eventMentions.stream()
                         .filter( this::isEventAffirmed )
                         .map( OntologyConceptUtil::getCuis )
                         .flatMap( Collection::stream )
                         .filter( "C0021756"::equals )
                         .map( c -> new Feature( "IL-2_section", segId ) )
                         .forEach( features::add );
            if ( features.size() > featureCount ) {
               ifContainsIL2 = true;
            }
         }

         features.addAll( extractEventFeatures( jCas, docType, segment, segId, extractor ) );

         features.add( new Feature( "segmentId", segId ) );
         String docType_segId_pNum = docType + "_" + segId + "_" +
                                     JCasUtil.selectCovered( jCas, Paragraph.class, segment ).size();
         features.add( new Feature( "docType_segment_paragNum", docType_segId_pNum ) );
         String docType_segIdeNum = docType + "_" + segId + "_" + eventMentions.size();
         features.add( new Feature( "docType_segment_eventNum", docType_segIdeNum ) );
      }

      String systemLabel;
      if ( ifContainsIL2 ) {
         systemLabel = "Treatment";
      } else {
         systemLabel = this.classifier.classify( features );
      }
      if ( systemLabel.equals( "Treatment" ) && ifAPSectionAboutTreatment && ifAPSectionFuturistic ) {//
         systemLabel = "Follow-up";
      }

      //remove episode from system view:
      JCasUtil.select( jCas, Episode.class ).forEach( Episode::removeFromIndexes );
      //create a final episode for the system view:
      Episode sysEpi = new Episode( jCas, 0, jCas.getDocumentText().length() );
      sysEpi.setEpisodeType( systemLabel );
      sysEpi.addToIndexes();
   }

   private Collection<? extends Feature> extractEventFeatures( JCas systemView, String docType, Segment segment,
                                                               String segId,
                                                               CoveredTextExtractor<WordToken> extractor ) {
      List<Feature> features = new ArrayList<>();
      List<String> beforeEvents = new ArrayList<>();
      List<String> afterEvents = new ArrayList<>();
      List<String> overlapEvents = new ArrayList<>();

      int procedureMentionCount = 0;
      int eventCount = 0;
      int dtrBeforeCount = 0;
      int dtrAfterCount = 0;
      int dtrOverlapCount = 0;

      for ( WordToken word : JCasUtil.selectCovered( systemView, WordToken.class, segment ) ) {
         features.addAll( extractor.extract( systemView, word ) );
      }

      for ( EventMention event : JCasUtil.selectCovered( systemView, EventMention.class, segment ) ) {
         if ( isEventAffirmed( event ) && event.getEvent() != null ) {
            String dtl = event.getEvent().getProperties().getDocTimeRel();
            String eventClass = event.getClass().getSimpleName();
            if ( dtl.equals( "BEFORE" ) ) {
               beforeEvents.add( eventClass );
               dtrBeforeCount++;
            } else if ( dtl.contains( "OVERLAP" ) ) {
               overlapEvents.add( eventClass );
               dtrOverlapCount++;
            } else if ( dtl.contains( "AFTER" ) ) {
               afterEvents.add( eventClass );
               dtrAfterCount++;
            }

            if ( eventClass.contains( "Procedure" ) ) {
               procedureMentionCount++;
            }

         }
      }


      features.add( new Feature( "docType_segment_eventBeforeCount", docType + "_" + segId + "_" + dtrBeforeCount ) );
      features.add( new Feature( "docType_segment_eventOverlapCount", docType + "_" + segId + "_" + dtrOverlapCount ) );
      features.add( new Feature( "docType_segment_eventAfterCount", docType + "_" + segId + "_" + dtrAfterCount ) );
      if ( dtrAfterCount > 0 ) {
         features.add( new Feature( "docType_segment_ExistAfterEvent" ) );
      }

      features.add( new Feature( "docType_segment_eventCount", docType + "_" + segId + "_" + eventCount ) );
      features.add( new Feature( "docType_segment_procedureCount",
            docType + "_" + segId + "_" + procedureMentionCount ) );
      if ( procedureMentionCount > 0 ) {
         features.add( new Feature( "docType_segment_ExistProcedureEvent" ) );
      }
      if ( !beforeEvents.isEmpty() ) {
         String mostFEClass = docType + "_" + segId + "_" + mostCommon( beforeEvents );
         features.add( new Feature( "docType_segment_MostFrequentBeforeEventClass", mostFEClass ) );
      }
      if ( !overlapEvents.isEmpty() ) {
         String mostFOClass = docType + "_" + segId + "_" + mostCommon( overlapEvents );
         features.add( new Feature( "docType_segment_MostFrequentOverlapEventClass", mostFOClass ) );
      }
      if ( !afterEvents.isEmpty() ) {
         String mostFAClass = docType + "_" + segId + "_" + mostCommon( afterEvents );
         features.add( new Feature( "docType_segment_MostFrequentAfterEventClass", mostFAClass ) );
      }

      return features;
   }

   private boolean isEventAffirmed( EventMention event ) {
      if ( event.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT ) {
         return true;
      }
      return false;
   }

   public static <T> T mostCommon( List<T> list ) {
      Map<T, Integer> map = new HashMap<>();

      for ( T t : list ) {
         Integer val = map.get( t );
         map.put( t, val == null ? 1 : val + 1 );
      }

      Entry<T, Integer> max = null;

      for ( Entry<T, Integer> e : map.entrySet() ) {
         if ( max == null || e.getValue() > max.getValue() ) {
            max = e;
         }
      }

      return max.getKey();
   }

   private List<Feature> extractFeatures( JCas systemView, String docType, Segment segment, Paragraph parag,
                                          CoveredTextExtractor<WordToken> extractor ) {
      List<Feature> features = new ArrayList<Feature>();


      for ( WordToken word : JCasUtil.selectCovered( systemView, WordToken.class, parag ) ) {
         features.addAll( extractor.extract( systemView, word ) );
      }

      int sentNum = JCasUtil.selectCovered( systemView, Sentence.class, parag ).size();
      features.add( new Feature( "Sent_num", sentNum ) );

      String segId = segment.getId();

      for ( EventMention event : JCasUtil.selectCovered( systemView, EventMention.class, parag ) ) {

         //get docTimeRel:
         String dtl = null;
         if ( event.getEvent() != null ) {
            dtl = event.getEvent().getProperties().getDocTimeRel();
         }
         //get CUIs:
         final FSArray fsArray = event.getOntologyConceptArr();
         if ( fsArray != null ) {
            final FeatureStructure[] featureStructures = fsArray.toArray();
            Set<String> CUIs = new HashSet<String>();
            for ( FeatureStructure featureStructure : featureStructures ) {
               if ( featureStructure instanceof UmlsConcept ) {
                  final UmlsConcept umlsConcept = (UmlsConcept)featureStructure;
                  final String cui = umlsConcept.getCui();
                  CUIs.add( cui );
               }
            }
            for ( String cui : CUIs ) {
               features.add( new Feature( "event_cui", cui ) );
               features.add( new Feature( "cui_dtl", cui + "_" + dtl ) );
               features.add( new Feature( "docType_segment_cui_dtl", docType + "_" + segId + "_" + cui + "_" + dtl ) );
               features.add( new Feature( "docType_segment_cui_polarity",
                     docType + "_" + segId + "_" + cui + "_" + event.getPolarity() ) );
               features.add( new Feature( "docType_segment_typeId",
                     docType + "_" + segId + "_" + event.getTypeIndexID() ) );
               features.add( new Feature( "docType_segment_cui_uncertainty",
                     docType + "_" + segId + "_" + cui + "_" + event.getUncertainty() ) );
            }
         }
      }

      for ( TimeMention time : JCasUtil.selectCovered( systemView, TimeMention.class, parag ) ) {
         features.add( new Feature( "docType_segment_time",
               docType + "_" + segId + "_" + time.getCoveredText().toLowerCase() ) );
         features.add( new Feature( "docType_segment_time-class", docType + "_" + segId + "_" + time.getTimeClass() ) );
      }
      return features;
   }


   private static String createDocumentType( final SourceData sourceData ) {
      if ( sourceData == null ) {
         return NoteSpecs.ID_NAME_CLINICAL_NOTE;
      } else {
         String sourceType = sourceData.getNoteTypeCode();
         return sourceType == null ? NoteSpecs.ID_NAME_CLINICAL_NOTE : sourceType;
      }
   }

}
