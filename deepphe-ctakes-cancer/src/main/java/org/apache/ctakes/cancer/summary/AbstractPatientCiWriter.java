package org.apache.ctakes.cancer.summary;

import org.apache.ctakes.core.cc.AbstractDataStoreFileWriter;
import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;


/**
 * Replaces the V1 evaluation writer
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/31/2018
 */
abstract public class AbstractPatientCiWriter extends AbstractDataStoreFileWriter<PatientCiContainerStore, PatientCiContainer> {


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );
      createFile();
   }

   abstract protected void createFile();

   abstract protected void writePatient( final PatientCiContainer patient ) throws IOException;

   /**
    * {@inheritDoc}
    */
   @Override
   final protected PatientCiContainerStore getDataStore() {
      return PatientCiContainerStore.getInstance();
   }

   final protected String getOutputDirectory() {
      return getRootDirectory() + "/" + getSimpleSubDirectory();
   }


   /**
    * {@inheritDoc}
    *
    * @return just the rootPath and subdirectory
    */
   @Override
   final protected String getOutputDirectory( final JCas jcas, final String rootPath, final String documentId ) {
      return getOutputDirectory();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public void writeFile( final PatientCiContainer patient,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      if ( patient == null ) {
         return;
      }
      writePatient( patient );
   }


}
