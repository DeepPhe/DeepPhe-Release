/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.cancer.episode;

import com.google.common.collect.Lists;
import com.lexicalscope.jewel.cli.Option;

import org.apache.ctakes.cancer.ae.EpisodeFeaturesExtractor.XMIReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.component.ViewTextCopierAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import java.io.*;
import java.util.*;


public abstract class Evaluation_ImplBase<STATISTICS_TYPE> extends
org.cleartk.eval.Evaluation_ImplBase<Integer, STATISTICS_TYPE> {

	static Logger LOGGER = Logger.getLogger( Evaluation_ImplBase.class );

	private static boolean isTraining;

	public static HashSet<String> badNotes;

	public static final String GOLD_VIEW_NAME = "GoldView";

	public static final String PROB_VIEW_NAME = "ProbView";

	public static final int MAX_DOC_VIEWS = 3;

	public enum XMLFormat {Knowtator, Anafora, I2B2, AnaforaCoref}

	public enum Subcorpus {Colon, Brain, DeepPhe}


	public static interface Options {


		@Option( longName = "xmi" )
		public File getXMIDirectory();

		@Option
		public boolean getPrintErrors();

		@Option
		public boolean getTest();

		@Option( longName = "kernelParams", defaultToNull = true )
		public String getKernelParams();

		@Option( defaultToNull = true )
		public String getI2B2Output();

		@Option( defaultToNull = true )
		public String getAnaforaOutput();

		@Option
		public boolean getSkipTrain();

		@Option(longName = "skipWrite")
		public boolean getSkipDataWriting();
	}


	protected File xmiDirectory;

	protected File xmiDevDirectory;

	private boolean xmiExists;

	protected File treebankDirectory;

	protected boolean printErrors = false;

	protected boolean printOverlapping = true;

	protected String i2b2Output = null;

	protected String anaforaOutput = null; 

	protected String[] kernelParams;

	public Evaluation_ImplBase(File baseDirectory, File xmiDirectory) {
		super( baseDirectory );
		this.baseDirectory = baseDirectory;
		this.xmiDirectory = xmiDirectory;
	}

	public void setI2B2Output( String outDir ) {
		i2b2Output = outDir;
	}

	public void setXMIDir(File xmiDir){
		xmiDirectory = xmiDir;
	}

	public void prepareXMIsFor( List<Integer> patientSets ) throws Exception {
		boolean needsXMIs = false;
		for ( File textFile : this.getFilesFor( patientSets ) ) {
			if ( !getXMIFile( this.xmiDirectory, textFile ).exists() ) {
				needsXMIs = true;
				break;
			}
		}
		this.xmiExists = true;
	}

	private List<File> getFilesFor( List<Integer> patientSets ) throws FileNotFoundException {
		List<File> files = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for ( Integer set : patientSets ) {
			ids.add( String.format( "patient%02d", set ) );
		}
		for ( File dir : this.xmiDirectory.listFiles() ) {
			if ( dir.isFile() ) {
				if ( ids.contains( dir.getName().split("_")[0] ) ) {
					if ( dir.exists() ) {
						files.add( dir );
					} else {
						LOGGER.warn( "Missing note: " + dir );
					}
				}
			}
		}

		return files;
	}

	@Override
	protected CollectionReader getCollectionReader( List<Integer> patientSets ) throws Exception {
		List<File> collectedFiles = this.getFilesFor( patientSets );
		Collections.sort(collectedFiles);

		return UriCollectionReader.getCollectionReaderFromFiles( collectedFiles );
	}

	protected AggregateBuilder getPreprocessorAggregateBuilder() throws Exception {
		return this.getXMIReadingPreprocessorAggregateBuilder();
	}

	protected AggregateBuilder getXMIReadingPreprocessorAggregateBuilder() throws UIMAException {
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		// TODO: Is this necessary? Doesn't the default view have the text populated in the xmis?
		aggregateBuilder.add( UriToDocumentTextAnnotator.getDescription() );
		aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
				XMIReader.class,
				XMIReader.PARAM_XMI_DIRECTORY,
				this.xmiDirectory ) );
		aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
				ViewCreatorAnnotator.class,
				ViewCreatorAnnotator.PARAM_VIEW_NAME,
				GOLD_VIEW_NAME ) );
		aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
				ViewTextCopierAnnotator.class,
				ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
				CAS.NAME_DEFAULT_SOFA,
				ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
				GOLD_VIEW_NAME ) );
		return aggregateBuilder;
	}


	static File getXMIFile( File xmiDirectory, File textFile ) {
		String fileName = textFile.getName();
		if(!fileName.contains(".xmi")){
			fileName += ".xmi";
		}
		return new File( xmiDirectory, fileName);// + ".xmi" 
	}

	static File getXMIFile( File xmiDirectory, JCas jCas ) throws AnalysisEngineProcessException {
		return getXMIFile( xmiDirectory, new File( ViewUriUtil.getURI( jCas ).getPath() ) );
	}


	@PipeBitInfo(
			name = "System Annotation Copier",
			description = "Copies an annotation type from the System view to a Gold view.",
			role = PipeBitInfo.Role.SPECIAL
			)
	public static class CopyFromSystem extends JCasAnnotator_ImplBase {

		public static AnalysisEngineDescription getDescription( Class<?>... classes )
				throws ResourceInitializationException {
			return AnalysisEngineFactory.createEngineDescription(
					CopyFromSystem.class,
					CopyFromSystem.PARAM_ANNOTATION_CLASSES,
					classes );
		}

		public static final String PARAM_ANNOTATION_CLASSES = "AnnotationClasses";

		@ConfigurationParameter( name = PARAM_ANNOTATION_CLASSES, mandatory = true )
		private Class<? extends TOP>[] annotationClasses;

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			JCas goldView, systemView;
			try {
				goldView = jCas.getView( GOLD_VIEW_NAME );
				systemView = jCas.getView( CAS.NAME_DEFAULT_SOFA );
			} catch ( CASException e ) {
				throw new AnalysisEngineProcessException( e );
			}
			for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
				for ( TOP annotation : Lists.newArrayList( JCasUtil.select( goldView, annotationClass ) ) ) {
					if ( annotation.getClass().equals( annotationClass ) ) {
						annotation.removeFromIndexes();
					}
				}
			}
			CasCopier copier = new CasCopier( systemView.getCas(), goldView.getCas() );
			Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName( CAS.FEATURE_FULL_NAME_SOFA );
			for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
				for ( TOP annotation : JCasUtil.select( systemView, annotationClass ) ) {
					TOP copy = (TOP)copier.copyFs( annotation );
					if ( copy instanceof Annotation ) {
						copy.setFeatureValue( sofaFeature, goldView.getSofa() );
					}
					copy.addToIndexes( goldView );
				}
			}
		}
	}

}

