package org.apache.ctakes.cancer.ae;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import com.google.common.collect.Lists;

import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

/**
 * An AE that extracts features for episode classification.
 * 
 * @author Chen Lin
 */
public class EpisodicDocumentAeMela extends CleartkAnnotator<String> {

	public static final String NO_CATEGORY = "unknown";

	public static Map<String, Integer> category_frequency = new LinkedHashMap<>();

	private WordEmbeddings word_embed = null;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);    

		try {
			word_embed = WordVectorReader.getEmbeddings(FileLocator.getAsStream("breastMelanoma_w2v.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EpisodicDocumentAeMela.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(File modelPath) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EpisodicDocumentAeMela.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelPath, "model.jar"));
	}
	/*
	 * Implement the standard UIMA process method.
	 */
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		JCas systemView;
		try {
			systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA); 
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		JCas goldView;
		try {
			goldView = jCas.getView("GoldView"); 
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		List<Episode> episodes = new ArrayList<>();
		for(Episode episode : JCasUtil.select(goldView, Episode.class)){
			String label = episode.getEpisodeType();
			//merge label for treatment:
			if(label.startsWith("Treatment") || label.equals("Therapy-Incurable")){
				label = "Treatment";
				episode.setEpisodeType(label);
			}
			episodes.add(episode);
		}


		CoveredTextExtractor<WordToken> extractor = new CoveredTextExtractor<WordToken>();
		DocumentID docId = JCasUtil.selectSingle(jCas, DocumentID.class);
		System.out.println("Doc ID: " + docId.getDocumentID());

		//extract features:
		List<Feature> features = new ArrayList<>();

		//get the document type feature
		String dId=docId.getDocumentID();
		int index=dId.lastIndexOf('_');
		String docType = dId.substring(index+1);
		features.add(new Feature("document_type", docType));

		boolean ifAPSectionFuturistic = false;
		boolean ifAPSectionAboutTreatment = false;
		boolean ifContainsIL2 = false;

		//iterate through all segments
		for(Segment segment : JCasUtil.select(systemView, Segment.class)){
			//get segment feature:
			String segId = segment.getId();
			if(!segId.equals("SIMPLE_SEGMENT")){
				segId = segId.toLowerCase();
				
				if(segId.contains("plan")){
					features.add(new Feature("contains_AssessmentAndPlan_Section"));
					for(EventMention event: JCasUtil.selectCovered(systemView, EventMention.class, segment)){
						if(isEventAffirmed(event) && event.getEvent() != null){
							if(event.getClass().getSimpleName().contains("Procedure")){
								features.add(new Feature("PlanSectionContainsProcedure"));
							}
							String dtl = event.getEvent().getProperties().getDocTimeRel();
							if(dtl.contains("AFTER")){
								ifAPSectionFuturistic=true;
								features.add(new Feature("PlanAboutNextStep"));
							}else if(dtl.equals("BEFORE/OVERLAP")){
								ifAPSectionAboutTreatment = true;
								features.add(new Feature("PlanIncludingPastOrPresentTreatment"));
							}
						}
					}
				}
				
				if(!segId.contains("history") ){
					for(MedicationMention event: JCasUtil.selectCovered(systemView, MedicationMention.class, segment)){
						if(isEventAffirmed(event)&&event.getEvent() != null){
							//get CUIs:
							final FSArray fsArray = event.getOntologyConceptArr();
							if(fsArray != null){
								final FeatureStructure[] featureStructures = fsArray.toArray();
								for ( FeatureStructure featureStructure : featureStructures ) {
									if ( featureStructure instanceof UmlsConcept ) {
										final UmlsConcept umlsConcept = (UmlsConcept)featureStructure;
										final String cui = umlsConcept.getCui();
										if(cui.equals("C0021756")){ //if the IL2 cui
											ifContainsIL2 = true;
											features.add(new Feature("IL-2_section", segId));
										}
									}
								}
							}
						}
					}
				}

				features.addAll(extractEventFeatures(systemView, docType, segment, segId, extractor));

				features.add(new Feature("segmentId", segId));
				String docType_segId_pNum = docType+"_"+segId+"_"+JCasUtil.selectCovered(systemView, Paragraph.class, segment).size();
				features.add(new Feature("docType_segment_paragNum", docType_segId_pNum));
				String docType_segIdeNum = docType+"_"+segId+"_"+JCasUtil.selectCovered(systemView, EventMention.class, segment).size();
				features.add(new Feature("docType_segment_eventNum", docType_segIdeNum));
			}
		}


		if(this.isTraining()){
			//get labels
			String label = null;
			if(episodes.size()==0){//if this document contains no episode annotation

				label = NO_CATEGORY;

			}else{//if there is episode annotatioin:

				//determine label based on majority vote:
				List<String> episodeTypes = new ArrayList<>();
				for(Episode episode : episodes){
					episodeTypes.add(episode.getEpisodeType());
				}
				label=mostCommon(episodeTypes);
			}

			this.dataWriter.write(new Instance<String>(label, features));
			if(category_frequency.containsKey(label)){
				category_frequency.put(label, category_frequency.get(label)+1);
			}else{
				category_frequency.put(label, 1);
			}

		}else{//if testing:
			String systemLabel = this.classifier.classify(features);

			if(ifContainsIL2){
				systemLabel = "Treatment";
			}
			//
			if(systemLabel.equals("Treatment") && ifAPSectionAboutTreatment && ifAPSectionFuturistic ){//
				systemLabel = "Follow-up";
			}

			//remove episode from system view:
			for ( Episode epi : Lists.newArrayList( JCasUtil.select( systemView, Episode.class ) ) ) {
				epi.removeFromIndexes();
			}
			//create an final episode for the system view:
			Episode sysEpi = new Episode(systemView);
			sysEpi.setBegin(0);
			sysEpi.setEnd(systemView.getDocumentText().length());
			sysEpi.setEpisodeType(systemLabel);
			sysEpi.addToIndexes();

			//remove episode from gold view
			List<String> epiTypes = new ArrayList<>();
			for(Episode epi : Lists.newArrayList(JCasUtil.select(goldView, Episode.class))){
				epiTypes.add(epi.getEpisodeType());
				epi.removeFromIndexes();
			}
			String goldLabel=null;
			if(!epiTypes.isEmpty()){
				goldLabel=mostCommon(epiTypes);
				if(!goldLabel.equals(systemLabel)){
					System.out.print("Test doc " + docId+ " has episode(s): ");
					for(String epitype : epiTypes){
						System.out.print(epitype+", ");
					}
					System.out.print("BUT SYSTEM LABEL IS: "+ systemLabel);
					System.out.println();
				}
			}else{
				goldLabel=NO_CATEGORY;
				if(!goldLabel.equals(systemLabel)){
					System.out.println("Test doc " + docId+ " has no episodes, but system Label is "+ systemLabel);
				}
			}
			//create an final episode for the gold view:
			Episode goldEpi = new Episode(goldView);
			goldEpi.setBegin(0);
			goldEpi.setEnd(systemView.getDocumentText().length());
			goldEpi.setEpisodeType(goldLabel);
			goldEpi.addToIndexes();

		}

	}

	private Collection<? extends Feature> extractEventFeatures(JCas systemView, String docType, Segment segment, String segId, CoveredTextExtractor<WordToken> extractor) {
		List<Feature> features = new ArrayList<>();
		List<String> beforeEvents = new ArrayList<>();
		List<String> afterEvents = new ArrayList<>();
		List<String> overlapEvents = new ArrayList<>();

		int procedureMentionCount = 0;
		int eventCount = 0;
		int dtrBeforeCount = 0;
		int dtrAfterCount = 0;
		int dtrOverlapCount = 0;

		for(WordToken word : JCasUtil.selectCovered(systemView, WordToken.class, segment)){
			features.addAll(extractor.extract(systemView, word));
		}

		for(EventMention event: JCasUtil.selectCovered(systemView, EventMention.class, segment)){
			if(isEventAffirmed(event)&&event.getEvent() != null){
				String dtl = event.getEvent().getProperties().getDocTimeRel();
				String eventClass = event.getClass().getSimpleName();
				if(dtl.equals("BEFORE")){
					beforeEvents.add(eventClass);
					dtrBeforeCount++;
				}else if(dtl.contains("OVERLAP")){
					overlapEvents.add(eventClass);
					dtrOverlapCount++;
				}else if(dtl.contains("AFTER")){
					afterEvents.add(eventClass);
					dtrAfterCount++;
				}

				if(eventClass.contains("Procedure")){
					procedureMentionCount++;
				}

			}
		}


		features.add(new Feature("docType_segment_eventBeforeCount", docType+"_"+segId+"_"+dtrBeforeCount));
		features.add(new Feature("docType_segment_eventOverlapCount", docType+"_"+segId+"_"+dtrOverlapCount));
		features.add(new Feature("docType_segment_eventAfterCount", docType+"_"+segId+"_"+dtrAfterCount));
		if(dtrAfterCount>0){
			features.add(new Feature("docType_segment_ExistAfterEvent"));
		}

		features.add(new Feature("docType_segment_eventCount", docType+"_"+segId+"_"+eventCount));
		features.add(new Feature("docType_segment_procedureCount", docType+"_"+segId+"_"+procedureMentionCount));
		if(procedureMentionCount>0){
			features.add(new Feature("docType_segment_ExistProcedureEvent"));
		}
		if(!beforeEvents.isEmpty()){
			String mostFEClass = docType+"_"+segId+"_"+mostCommon(beforeEvents);
			features.add(new Feature("docType_segment_MostFrequentBeforeEventClass", mostFEClass));
		}
		if(!overlapEvents.isEmpty()){
			String mostFOClass = docType+"_"+segId+"_"+mostCommon(overlapEvents);
			features.add(new Feature("docType_segment_MostFrequentOverlapEventClass", mostFOClass));
		}
		if(!afterEvents.isEmpty()){
			String mostFAClass = docType+"_"+segId+"_"+mostCommon(afterEvents);
			features.add(new Feature("docType_segment_MostFrequentAfterEventClass", mostFAClass));
		}
		
		return features;
	}

	private boolean isEventAffirmed(EventMention event) {
		if(event.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT){
			return true;
		}
		return false;
	}

	public static <T> T mostCommon(List<T> list) {
		Map<T, Integer> map = new HashMap<>();

		for (T t : list) {
			Integer val = map.get(t);
			map.put(t, val == null ? 1 : val + 1);
		}

		Entry<T, Integer> max = null;

		for (Entry<T, Integer> e : map.entrySet()) {
			if (max == null || e.getValue() > max.getValue())
				max = e;
		}

		return max.getKey();
	}

	private List<Feature> extractFeatures(JCas systemView, String docType, Segment segment, Paragraph parag, CoveredTextExtractor<WordToken> extractor) {
		List<Feature> features = new ArrayList<Feature>();



		for(WordToken word : JCasUtil.selectCovered(systemView, WordToken.class, parag)){
			features.addAll(extractor.extract(systemView, word));
		}

		int sentNum = JCasUtil.selectCovered(systemView, Sentence.class, parag).size();
		features.add(new Feature("Sent_num", sentNum));

		String segId = segment.getId();

		for(EventMention event: JCasUtil.selectCovered(systemView, EventMention.class, parag)){

			//get docTimeRel:
			String dtl =null;
			if(event.getEvent() != null){
				dtl = event.getEvent().getProperties().getDocTimeRel();
			}
			//get CUIs:
			final FSArray fsArray = event.getOntologyConceptArr();
			if(fsArray != null){
				final FeatureStructure[] featureStructures = fsArray.toArray();
				Set<String> CUIs = new HashSet<String>();
				for ( FeatureStructure featureStructure : featureStructures ) {
					if ( featureStructure instanceof UmlsConcept ) {
						final UmlsConcept umlsConcept = (UmlsConcept)featureStructure;
						final String cui = umlsConcept.getCui();
						CUIs.add(cui);
					}
				}
				for(String cui: CUIs){
					features.add(new Feature("event_cui", cui));
					features.add(new Feature("cui_dtl", cui+"_"+dtl));
					features.add(new Feature("docType_segment_cui_dtl", docType+"_"+segId+"_"+cui+"_"+dtl));
					features.add(new Feature("docType_segment_cui_polarity", docType+"_"+segId+"_"+cui+"_"+event.getPolarity()));
					features.add(new Feature("docType_segment_typeId", docType+"_"+segId+"_"+event.getTypeIndexID()));
					features.add(new Feature("docType_segment_cui_uncertainty", docType+"_"+segId+"_"+cui+"_"+event.getUncertainty()));
				}
			}


		}

		for(TimeMention time: JCasUtil.selectCovered(systemView, TimeMention.class, parag)){
			features.add(new Feature("docType_segment_time", docType+"_"+segId+"_"+time.getCoveredText().toLowerCase()));
			features.add(new Feature("docType_segment_time-class", docType+"_"+segId+"_"+time.getTimeClass()));
		}
		return features;
	}
}
