package org.apache.ctakes.cancer.ae;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
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
import org.cleartk.ml.feature.extractor.*;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

/**
 * An AE that extracts features for episode classification.
 * 
 * @author Chen Lin
 */
public class EpisodeFeatureExtractorAe extends CleartkAnnotator<String> {

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);    
	}

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EpisodeFeatureExtractorAe.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(File modelPath) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EpisodeFeatureExtractorAe.class,
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

		try(FileWriter fw = new FileWriter("Gold_episodes_inDev.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			DocumentID docId = JCasUtil.selectSingle(jCas, DocumentID.class);
			System.out.println("Doc ID: " + docId.getDocumentID());

			//modify the gold view labels:
			if(!this.isTraining()){
				List<String> epilabels = new ArrayList<>();
				for(Episode episode : JCasUtil.select(goldView, Episode.class)){
					String label = episode.getEpisodeType();
					if(label.startsWith("Treatment") || label.equals("Therapy-Incurable")){
						label = "Treatment";
						episode.setEpisodeType(label);
					}
					epilabels.add(label);
				}
				if(!epilabels.isEmpty()){
					out.println("Document "+ docId.getDocumentID() + ":");
					for(String lab: epilabels){
						out.println("\t"+lab);
					}
				}
			}

			for(Episode episode : JCasUtil.select(systemView, Episode.class)){

				List<Feature> features = new ArrayList<Feature>();

				String did=docId.getDocumentID();
				int index=did.lastIndexOf('_');
				String didFeat = did.substring(index+1);
				features.add(new Feature("document_type", didFeat));


				String segId = null;
				for(Segment segment : JCasUtil.selectCovering(systemView, Segment.class, episode)){
					segId = segment.getId();
					features.add(new Feature("segmentId", segId));
				}
				for(EventMention event: JCasUtil.selectCovered(systemView, EventMention.class, episode)){
					features.add(new Feature("event", event.getCoveredText().toLowerCase()));
					
					//get docTimeRel:
					String dtl =null;
					if(event.getEvent() != null){
						dtl = event.getEvent().getProperties().getDocTimeRel();
						features.add(new Feature("event_dtr", dtl));
					}
					features.add(new Feature("event_polarity", event.getPolarity()));
					features.add(new Feature("event_typeId", event.getTypeIndexID()));
					features.add(new Feature("event_uncertainty", event.getUncertainty()));
					
					if(!event.getClass().equals(EventMention.class)){
						String eventClass = event.getClass().getSimpleName();
						features.add(new Feature("event_class", eventClass));
					}
				}


				for(TimeMention time: JCasUtil.selectCovered(systemView, TimeMention.class, episode)){
					features.add(new Feature("time", time.getCoveredText().toLowerCase()));
					features.add(new Feature("time-class", time.getTimeClass()));
				}

				// retrieve the document label from the CAS
				String label = episode.getEpisodeType();
				if(label.startsWith("Treatment") || label.equals("Therapy-Incurable")){
					label = "Treatment";
				}

				// create a classification instance and write it to the training data
				if(this.isTraining()){
					this.dataWriter.write(new Instance<String>(label, features));
					System.out.println("Episode label: "+label);
				}else{
					String prelabel = this.classifier.classify(features);
					episode.setEpisodeType(prelabel);
					System.out.println("Gold label: "+label);
				}



			}

		} catch (IOException e) {
			//exception handling left as an exercise for the reader
		}

	}
}
