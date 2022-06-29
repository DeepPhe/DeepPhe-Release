package org.healthnlp.deepphe.summary.ae;

import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.summary.engine.DpheXnSummaryEngine;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/4/2020
 */
@PipeBitInfo(
      name = "PatientSummarizerXn",
      description = "DeepPhe Translational Patient Summarizer.", role = PipeBitInfo.Role.SPECIAL
)
final public class PatientSummarizerXn extends AbstractPatientConsumer {

   static private final Logger LOGGER = Logger.getLogger( "PatientSummarizerXn" );

   public PatientSummarizerXn() {
      super( "PatientSummarizerXn", "Summarizing Patient" );
   }

   /**
    * Call necessary processing for patient
    * <p>
    * {@inheritDoc}
    */
   @Override
   protected void processPatientCas( final JCas patientCas ) throws AnalysisEngineProcessException {

      final String patientId = SourceMetadataUtil.getPatientIdentifier( patientCas );
      LOGGER.info( "Summarizing patient " + patientId + " ..." );

      final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );

      final PatientSummary patientSummary =  DpheXnSummaryEngine.createPatientSummary( patient );

//      patientSummary.getNeoplasms().forEach( PatientSummarizerXn::logNeoplasm );

   }


   static private void logNeoplasm( final NeoplasmSummary neoplasm ) {
      LOGGER.info( "===================== Neoplasm " + neoplasm.getId() + " =====================" );
      neoplasm.getAttributes().forEach( PatientSummarizerXn::logAttribute );
      LOGGER.info( "====================================================" );
   }

   static private void logAttribute( final NeoplasmAttribute attribute ) {
      LOGGER.info( attribute.getName() + ": " + attribute.getClassUri() + " = " + attribute.getValue() );
   }


}
