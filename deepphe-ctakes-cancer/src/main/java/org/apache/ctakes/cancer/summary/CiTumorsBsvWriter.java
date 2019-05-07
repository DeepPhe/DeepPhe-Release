package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.cancer.summary.writer.TumorBsvWriter;

import java.io.*;
import java.util.Collection;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/3/2019
 */
final public class CiTumorsBsvWriter extends AbstractPatientCiWriter {

   private TumorBsvWriter _delegate;


   /**
    * {@inheritDoc}
    */
   @Override
   protected void createFile() {
      if ( _delegate == null ) {
         _delegate = new TumorBsvWriter( getOutputDirectory() );
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
         _delegate.setCancerId( cancer.getId() );
         for ( NeoplasmCiContainer tumor : cancer.getTumors() ) {
            _delegate.writeNeoplasm( patientId, tumor );
         }
      }
   }


}
