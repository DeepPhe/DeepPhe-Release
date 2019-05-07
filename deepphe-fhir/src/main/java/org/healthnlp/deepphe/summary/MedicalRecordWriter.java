package org.healthnlp.deepphe.summary;

import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/30/2018
 */
abstract public class MedicalRecordWriter extends AbstractFileWriter<MedicalRecord> implements NamedEngine {

   static private final Logger LOGGER = Logger.getLogger( "MedicalRecordWriter" );

   /**
    * The Medical Record contains patient, note, cancer and tumor summaries.
    * Those summaries contain Fact and other information that should be written to FHIR.
    */
   private MedicalRecord _medicalRecord;


   abstract protected void writeMedicalRecord( final MedicalRecord medicalRecord, final String outputDir )
         throws IOException;

   protected String getEmrOutputDirectory() {
      return getRootDirectory() + "/" + getSimpleSubDirectory();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );
      // MedicalRecordStore keeps a cache of Medical Records that are ready to be consumed.
      // In order to prevent a Medical Record from being disposed from the cache before it has been used,
      // register this class with the MedicalRecordStore.
      MedicalRecordStore.getInstance().registerEngine( getEngineName() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      _medicalRecord = MedicalRecordStore.getInstance().pop( getEngineName() );
      if ( _medicalRecord != null ) {
         try {
            writeMedicalRecord( _medicalRecord, getEmrOutputDirectory() );
         } catch ( IOException ioE ) {
            throw new AnalysisEngineProcessException( ioE );
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void createData( final JCas jCas ) {
      // popMedicalRecord can only be performed once per registered engine.  Do not use this method more than once.
      _medicalRecord = MedicalRecordStore.getInstance().pop( getEngineName() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected MedicalRecord getData() {
      return _medicalRecord;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void writeComplete( final MedicalRecord medicalRecord ) {
   }

   /**
    * {@inheritDoc}
    *
    * @return just the rootPath
    */
   @Override
   protected String getOutputDirectory( final JCas jcas, final String rootPath, final String documentId ) {
      return getEmrOutputDirectory();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final MedicalRecord medicalRecord,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      if ( medicalRecord == null ) {
         LOGGER.warn( "No Medical Record" );
         return;
      }
      writeMedicalRecord( medicalRecord, outputDir );
   }

}