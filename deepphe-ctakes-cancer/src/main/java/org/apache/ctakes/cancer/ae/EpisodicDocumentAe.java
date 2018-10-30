package org.apache.ctakes.cancer.ae;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
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
public class EpisodicDocumentAe extends CleartkAnnotator<String> {

	public static final String NO_CATEGORY = "unknown";

	public static Map<String, Integer> category_frequency = new LinkedHashMap<>();

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);    
	}

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EpisodicDocumentAe.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(File modelPath) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EpisodicDocumentAe.class,
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


		try(FileWriter fw = new FileWriter("Gold_episodes_inDev.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
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
			
			//iterate through all segments
			for(Segment segment : JCasUtil.select(systemView, Segment.class)){
				//get segment feature:
				String segId = segment.getId();
				features.add(new Feature("segmentId", segId));
				
				int paragNum = 0;
				for(Paragraph parag : JCasUtil.selectCovered(systemView, Paragraph.class, segment)){//iterate through all paragraphs within a segment
					features.addAll(extractFeatures(systemView, docType, segment, parag, extractor));
					paragNum++;
				}
				features.add(new Feature("docType_segment_paragNum", docType+"_"+segId+"_"+paragNum));
				features.add(new Feature("docType_segment_eventNum", docType+"_"+segId+"_"+JCasUtil.selectCovered(systemView, EventMention.class, segment).size()));
			}

			if(this.isTraining()){
				
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
					if(epiTypes.size()>1){
						System.out.print("Test doc has multiple episodes: ");
						for(String epitype : epiTypes){
							System.out.print(epitype+", ");
						}
						System.out.println();
					}
				}else{
					goldLabel=NO_CATEGORY;
				}
				//create an final episode for the gold view:
				Episode goldEpi = new Episode(goldView);
				goldEpi.setBegin(0);
				goldEpi.setEnd(systemView.getDocumentText().length());
				goldEpi.setEpisodeType(goldLabel);
				goldEpi.addToIndexes();

			}

		} catch (IOException e) {
			//exception handling left as an exercise for the reader
		}

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
