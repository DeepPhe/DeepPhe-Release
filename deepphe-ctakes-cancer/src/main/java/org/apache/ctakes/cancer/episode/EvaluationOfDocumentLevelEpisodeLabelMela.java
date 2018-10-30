package org.apache.ctakes.cancer.episode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.ctakes.cancer.ae.EpisodicDocumentAeMela;
//import org.apache.ctakes.cancer.ae.EpisodicParagraphAe;
import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;

import com.google.common.base.Function;
import com.lexicalscope.jewel.cli.CliFactory;

public class EvaluationOfDocumentLevelEpisodeLabelMela extends EvaluationOfAnnotationSpans_ImplBase  {
	
	public static void main(String[] args) throws Exception {
		Options options = CliFactory.parseArguments(Options.class, args);
		List<Integer> trainItems = null;
		List<Integer> devItems = null;
		List<Integer> testItems = null;

		trainItems = Arrays.asList(5, 6, 18, 19, 25, 28, 30, 33, 34, 42);//train on melanoma
		devItems = Arrays.asList(7, 32, 43); //test on melanoma dev
		testItems = Arrays.asList(2, 3, 11, 12, 14, 16, 24, 27, 41, 44);//test on melanoma test set

		List<Integer> allTraining = new ArrayList<>(trainItems);
		List<Integer> allTest = null;
		if (options.getTest()) {
			allTraining.addAll(devItems);
			allTest = new ArrayList<>(testItems);
		} else {
			allTest = new ArrayList<>(devItems);
		}
		EvaluationOfDocumentLevelEpisodeLabelMela evaluation = new EvaluationOfDocumentLevelEpisodeLabelMela(
				new File("target/eval/episode-spans/mela"),
				options.getXMIDirectory(), 
				Episode.class);
		evaluation.skipTrain = options.getSkipTrain();
		evaluation.setLogging(Level.FINE, new File("target/eval/ctakes-episode-errors.log"));
		AnnotationStatistics<String> stats = evaluation.trainAndTest(allTraining, allTest);
		System.err.println(stats);
		System.err.println(stats.confusions().toString());
	}

	public EvaluationOfDocumentLevelEpisodeLabelMela(File baseDirectory, File xmiDirectory, 
			Class<? extends Annotation> annotationClass) {
		super(baseDirectory, xmiDirectory, annotationClass);
	}
	
	protected boolean skipTrain=false;
	
	@Override
    protected void train(CollectionReader collectionReader, File directory) throws Exception {
            if(this.skipTrain) return;
            AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
            aggregateBuilder.add(CopyFromSystem.getDescription(Episode.class));
            aggregateBuilder.add(EpisodicDocumentAeMela.createDataWriterDescription(
                    LibLinearStringOutcomeDataWriter.class,        
                    directory));
            SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());

            //calculate class-wise weights:
          //calculate class-wise weights:
            String[] weightArray=new String[EpisodicDocumentAeMela.category_frequency.size()*2+4];
            int weight_idx = 0;
            float baseFreq = EpisodicDocumentAeMela.category_frequency.get(EpisodicDocumentAeMela.NO_CATEGORY);
            for( Map.Entry<String, Integer> entry: EpisodicDocumentAeMela.category_frequency.entrySet()){
                    weightArray[weight_idx*2] = "-w"+Integer.toString(weight_idx + 1);
                    float weight = baseFreq/entry.getValue();
                    weightArray[weight_idx*2+1] = Float.toString(weight);
                    weight_idx ++;
                    System.err.println("Category:"+entry.getKey()+"  freq:"+entry.getValue() + "   weight:"+weight);
            }

            weightArray[weight_idx*2] = "-c";
            weightArray[weight_idx*2+1] = "0.001";//0.005
            weight_idx ++;
            weightArray[weight_idx*2] = "-s";
            weightArray[weight_idx*2+1] = "1";//best 1
            JarClassifierBuilder.trainAndPackage(directory,weightArray);
    }
	
	@Override
	protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
			throws Exception {
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
		aggregateBuilder.add(CopyFromSystem.getDescription(Episode.class));
		aggregateBuilder.add(EpisodicDocumentAeMela.createAnnotatorDescription(directory));
		AnnotationStatistics<String> stats = new AnnotationStatistics<>();
		
		Function<Annotation, List<Integer>> getSpan = new Function<Annotation, List<Integer>>() {
			public List<Integer> apply(Annotation episode) {
				return Arrays.asList(episode.getBegin(), episode.getEnd());
			}
		};
		Function<Annotation, String> getOutcome = AnnotationStatistics.annotationToFeatureValue("EpisodeType");
		for (Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
			JCas jCas = casIter.next();
			DocumentID docId = JCasUtil.selectSingle(jCas, DocumentID.class);
			this.logger.fine("Processed Document: " + docId.getDocumentID());
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			Collection<? extends Annotation> goldAnnotations = JCasUtil.select(goldView, Episode.class);
			Collection<? extends Annotation> systemAnnotations = JCasUtil.select(systemView, Episode.class);
			stats.add(goldAnnotations, systemAnnotations, getSpan, getOutcome);
		}
		return stats;
	}
	

	@Override
	protected AnalysisEngineDescription getAnnotatorDescription(File arg0) throws ResourceInitializationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AnalysisEngineDescription getDataWriterDescription(File arg0) throws ResourceInitializationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<? extends Annotation> getGoldAnnotations(JCas jCas, Segment segment) {
		return JCasUtil.selectCovered(jCas, Episode.class, segment);
	}

	@Override
	protected Collection<? extends Annotation> getSystemAnnotations(JCas jCas, Segment segment) {
		return JCasUtil.selectCovered(jCas, Episode.class, segment);
	}

	@Override
	protected void trainAndPackage(File arg0) throws Exception {
		// TODO Auto-generated method stub

	}

}
