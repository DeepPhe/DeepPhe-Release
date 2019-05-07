package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.cancer.summary.writer.CancerBsvWriter;

import java.io.*;
import java.util.Collection;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/3/2019
 */
final public class CiCancersBsvWriter extends AbstractPatientCiWriter {

   private CancerBsvWriter _delegate;

   /**
    * {@inheritDoc}
    */
   @Override
   protected void createFile() {
      if ( _delegate == null ) {
         _delegate = new CancerBsvWriter( getOutputDirectory() );
      }
      _delegate.createFile( getOutputDirectory() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void writePatient( final PatientCiContainer patient ) throws IOException {
      final String patientId = patient.getWorldId();
      final Collection<CancerCiContainer> cancers = patient.getCancers();
      for ( CancerCiContainer cancer : cancers ) {
         _delegate.writeNeoplasm( patientId, cancer.getCancer() );
      }
   }


}
