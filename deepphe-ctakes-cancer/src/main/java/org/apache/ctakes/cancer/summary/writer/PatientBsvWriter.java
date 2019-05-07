package org.apache.ctakes.cancer.summary.writer;


import org.apache.ctakes.cancer.summary.CancerCiContainer;
import org.apache.ctakes.cancer.summary.NeoplasmCiContainer;
import org.apache.ctakes.cancer.summary.PatientCiContainerStore;
import org.apache.ctakes.cancer.summary.PatientCiContainer;
import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/9/2019
 */
final public class PatientBsvWriter extends AbstractFileWriter<PatientCiContainer> implements NamedEngine {

   static private final Logger LOGGER = Logger.getLogger( "PatientCiSummaryWriter" );

   public static final String PARAMETER_CLEAR_STORE = "ClearStore";
   public static final String PARAMETER_ENGINE_NAME = "EngineName";
   @ConfigurationParameter(
         name = PARAMETER_CLEAR_STORE,
         description = "The Writer should clear the Concept Instance data store when the pipeline is finished.",
         mandatory = false,
         defaultValue = {"true"}
   )
   private boolean _clear;

   @ConfigurationParameter(
         name = PARAMETER_ENGINE_NAME,
         description = "The Name to use for this File Writer.  Must be unique in the pipeline.",
         mandatory = false
   )
   private String _engineName;

   private PatientCiContainer _patient;

   private CancerBsvWriter _cancerWriter;
   private TumorBsvWriter _tumorWriter;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );
      PatientCiContainerStore.getInstance().registerEngine( getEngineName() );
      _cancerWriter = new CancerBsvWriter( getOutputDirectory() );
      _tumorWriter = new TumorBsvWriter( getOutputDirectory() );
   }

   private void writePatient( final PatientCiContainer patient )
         throws IOException {
      for ( CancerCiContainer cancer : patient.getCancers() ) {
         _cancerWriter.writeNeoplasm( patient.getWorldId(), cancer.getCancer() );
         for ( NeoplasmCiContainer tumor : cancer.getTumors() ) {
            _tumorWriter.writeNeoplasm( patient.getWorldId(), tumor );
         }
      }
   }

   private String getOutputDirectory() {
      return getRootDirectory() + "/" + getSimpleSubDirectory();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getEngineName() {
      return _engineName != null && !_engineName.isEmpty() ? _engineName : getClass().getSimpleName();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      _patient = PatientCiContainerStore.getInstance().pop( getEngineName() );
      while ( _patient != null ) {
         try {
            writePatient( _patient );
         } catch ( IOException ioE ) {
            throw new AnalysisEngineProcessException( ioE );
         }
         _patient = PatientCiContainerStore.getInstance().pop( getEngineName() );
      }
      if ( _clear && PatientCiContainerStore.getInstance().isDataEmpty() ) {
         PatientCiContainerStore.getInstance().clearData();
         PatientCiContainerStore.getInstance().clearEngines();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void createData( final JCas jCas ) {
      _patient = PatientCiContainerStore.getInstance().pop( getEngineName() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PatientCiContainer getData() {
      return _patient;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void writeComplete( final PatientCiContainer summary ) {
   }

   /**
    * {@inheritDoc}
    *
    * @return just the rootPath and subdirectory
    */
   @Override
   protected String getOutputDirectory( final JCas jcas, final String rootPath, final String documentId ) {
      return getOutputDirectory();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final PatientCiContainer patient,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      if ( patient == null ) {
         LOGGER.error( "No Patient Summary available" );
         return;
      }
      writePatient( patient );
   }

}
