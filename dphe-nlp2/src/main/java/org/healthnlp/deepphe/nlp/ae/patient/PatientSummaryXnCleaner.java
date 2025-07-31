package org.healthnlp.deepphe.nlp.ae.patient;

import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.patient.PatientSummaryXnStore;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;


/**
 * @author SPF , chip-nlp
 * @since {3/9/2021}
 */
@PipeBitInfo(
      name = "PatientSummaryXnCleaner",
      description = "Clear a cached patient summary.  Only use at the end of a pipeline.", role =
            PipeBitInfo.Role.SPECIAL
)
public class PatientSummaryXnCleaner extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PatientSummaryXnCleaner" );

   // todo - keep track of currentPatientID.
   //  If patient changes (new patient) and (previous) currentPatientID doc count == wanted,
   //  remove previous patient from cache, and reset the currentPatient.

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
       if (PatientDocCounter.getInstance().isPatientFull(patientId)) {
           PatientSummaryXnStore.getInstance().remove(patientId);
           UriInfoCache.getInstance().clear();
       }
   }

   /**
    * Close the PatientSummaryNodeStore.
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      PatientSummaryXnStore.getInstance().close();
   }

}
