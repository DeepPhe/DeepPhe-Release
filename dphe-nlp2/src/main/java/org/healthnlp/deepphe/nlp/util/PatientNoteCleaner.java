package org.healthnlp.deepphe.nlp.util;

import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * @author SPF , chip-nlp
 * @since {5/23/2024}
 */
@PipeBitInfo (
      name = "PatientNoteCleaner",
      description = "Cleans patient notes from the PatientNoteStore.",
      role = PipeBitInfo.Role.SPECIAL
)
public class PatientNoteCleaner extends JCasAnnotator_ImplBase {
   static private final Logger LOGGER = Logger.getLogger( "PatientNoteCleaner" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
       if (PatientDocCounter.getInstance().isPatientFull(patientId)) {
           PatientNoteStore.getInstance().removePatient(patientId);
           PatientDocCounter.getInstance().removePatient(patientId);
       }
   }


}
