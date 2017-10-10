package org.healthnlp.deepphe.uima.ae;


import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.uima.fhir.DocumentResourceFactory;

import java.io.File;

/**
 * create FHIR represetnation of document mention level data in
 * document centric cTAKES pipeline
 *
 * @author tseytlin
 */
// TODO extend AbstractOutputFileWriter
public class DocumentSummarizerAE extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DocumentSummarizerAE" );

   public static final String FHIR_TYPE = "FHIR";
   private String _outputDirPath;


   /**
    * Name of configuration parameter that must be set to the path of a directory into which the
    * output files will be written.
    * This can be set with cli -o
    */
   @ConfigurationParameter( name = ConfigParameterConstants.PARAM_OUTPUTDIR,
         description = ConfigParameterConstants.DESC_OUTPUTDIR + "  Added is the subdirectory " + FHIR_TYPE )
   private File _outputRootDir;

   /**
    * sets the fhir output directory to be the standard output directory plus a fhir subdirectory
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _outputDirPath = _outputRootDir.getAbsolutePath() + "/" + FHIR_TYPE;
   }


   public void process( JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Creating Document Summary ..." );
      try {

         final Patient patient = DocumentResourceFactory.getPatient( jcas );

         final String namedID = patient != null ? patient.getPatientName() : "unknown";
         final Report report = DocumentResourceFactory.getReport( jcas );

         LOGGER.info( "PROCESSING patient: " + namedID + " document: " + report.getTitle() + " .." );

         // TODO make this a fhir report saver ae/cc
         // save FHIR related data
         File patientDir = new File( _outputDirPath, namedID );
         if ( !patientDir.exists() ) {
            patientDir.mkdirs();
         }
         report.save( patientDir );

      } catch ( Exception e ) {
         e.printStackTrace();
      }
      LOGGER.info( "Summary Complete for " + DocumentIDAnnotationUtil.getDocumentID( jcas ) );
   }

}
