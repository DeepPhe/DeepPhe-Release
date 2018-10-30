package org.apache.ctakes.cancer.ae;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionReader;
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.xml.sax.SAXException;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * 
 * Read XMI files and apply a consumer that extracts Episode features for classification.
 * 
 * @author Chen Lin
 *
 */
public class EpisodeFeaturesExtractor {

	public static interface Options {

		@Option
		public String getInputDir();

		@Option
		public File getOutputDir();
	}

	public static void main(String[] args) throws Exception {

		Options options = CliFactory.parseArguments(Options.class, args);

		//find files with gold annotations:
		CollectionReader collectionReader = UriCollectionReader.getCollectionReaderFromDirectory(new File(options.getInputDir()));

        AggregateBuilder aggregateBuilder = new AggregateBuilder();
        aggregateBuilder.add(UriToDocumentTextAnnotator.getDescription());
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
                        XMIReader.class,
                        XMIReader.PARAM_XMI_DIRECTORY,
                        new File(options.getInputDir())));
		
		aggregateBuilder.add(EpisodeFeatureExtractorAe.createDataWriterDescription(
				LibSvmStringOutcomeDataWriter.class,        
                options.getOutputDir()));
				

		SimplePipeline.runPipeline(
                collectionReader,
                aggregateBuilder.createAggregate());
		System.out.println("pipeline finished!");
	}

	public static class XMIReader extends JCasAnnotator_ImplBase {

		public static final String PARAM_XMI_DIRECTORY = "XMIDirectory";

		@ConfigurationParameter( name = PARAM_XMI_DIRECTORY, mandatory = true )
		private File xmiDirectory;

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			File xmiFile = getXMIFile( this.xmiDirectory, jCas );
			try {
				FileInputStream inputStream = new FileInputStream( xmiFile );
				try {
					XmiCasDeserializer.deserialize( inputStream, jCas.getCas() );
				} finally {
					inputStream.close();
				}
			} catch ( SAXException e ) {
				throw new AnalysisEngineProcessException( e );
			} catch ( IOException e ) {
				throw new AnalysisEngineProcessException( e );
			}
		}
	}
	static File getXMIFile( File xmiDirectory, JCas jCas ) throws AnalysisEngineProcessException {
		return getXMIFile( xmiDirectory, new File( ViewUriUtil.getURI( jCas ).getPath() ) );
	}

	static File getXMIFile( File xmiDirectory, File textFile ) {
		String fileName = textFile.getName();
		if(!fileName.contains(".xmi")){
			fileName += ".xmi";
		}
		return new File( xmiDirectory, fileName);
	}
}
