package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.node.PatientSummaryNodeStore;


/**
 * @author SPF , chip-nlp
 * @since {3/9/2021}
 */
@PipeBitInfo(
      name = "PatientSummaryCleaner",
      description = "Clear a cached patient summary.  Only use at the end of a pipeline.", role =
            PipeBitInfo.Role.SPECIAL
)
public class PatientSummaryCleaner extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PatientSummaryCleaner" );

   // todo - keep track of currentPatientID.
   //  If patient changes (new patient) and (previous) currentPatientID doc count == wanted,
   //  remove previous patient from cache, and reset the currentPatient.

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      // Even though the ctakes PatientNoteStore isn't being used to store any patient jcas,
      // it still has note counts as they should be set by a collection reader.
      final int patientDocCount = PatientNoteStore.getInstance()
                                                  .getWantedDocCount( patientId );
      final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      if ( patient.getNotes().size() < patientDocCount ) {
         return;
      }
      PatientSummaryNodeStore.getInstance().remove( patientId );
   }

   /**
    * Close the PatientSummaryNodeStore.
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      PatientSummaryNodeStore.getInstance().close();
   }

}
