package org.apache.ctakes.cancer.episode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.apache.ctakes.cancer.ae.EpisodeFeatureExtractorAe;
import org.apache.ctakes.cancer.episode.Evaluation_ImplBase.CopyFromSystem;
import org.apache.ctakes.cancer.type.textspan.Episode;
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
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.lexicalscope.jewel.cli.CliFactory;

public class EvaluationOfEpisode extends EvaluationOfAnnotationSpans_ImplBase  {
	
	public static void main(String[] args) throws Exception {
		Options options = CliFactory.parseArguments(Options.class, args);
		List<Integer> trainItems = null;
		List<Integer> devItems = null;
		List<Integer> testItems = null;

		trainItems = Arrays.asList(3, 5, 6, 11, 18, 19, 25, 28, 30, 33, 34, 42, 92, 93);
		devItems = Arrays.asList(2, 7, 21, 32);//,43
		testItems = Arrays.asList(1, 16);

		List<Integer> allTraining = new ArrayList<>(trainItems);
		List<Integer> allTest = null;
		if (options.getTest()) {
			allTraining.addAll(devItems);
			allTest = new ArrayList<>(testItems);
		} else {
			allTest = new ArrayList<>(devItems);
		}
		EvaluationOfEpisode evaluation = new EvaluationOfEpisode(
				new File("target/eval/episode-spans"),
				options.getXMIDirectory(), 
				Episode.class);
		evaluation.skipTrain = options.getSkipTrain();
		evaluation.setLogging(Level.FINE, new File("target/eval/ctakes-episode-errors.log"));
		AnnotationStatistics<String> stats = evaluation.trainAndTest(allTraining, allTest);
		System.err.println(stats);
	}

	public EvaluationOfEpisode(File baseDirectory, File xmiDirectory, 
			Class<? extends Annotation> annotationClass) {
		super(baseDirectory, xmiDirectory, annotationClass);
	}
	
	protected boolean skipTrain=false;
	
	@Override
    protected void train(CollectionReader collectionReader, File directory) throws Exception {
            if(this.skipTrain) return;
            AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
            aggregateBuilder.add(EpisodeFeatureExtractorAe.createDataWriterDescription(
                    LibLinearStringOutcomeDataWriter.class,        
                    directory));
            SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
            
            //calculate class-wise weights:
            String[] weightArray=new String[2];
            weightArray[0] = "-c";
            weightArray[1] = "0.1";
            JarClassifierBuilder.trainAndPackage(directory,weightArray);
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
