package org.healthnlp.deepphe.summary.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.summary.engine.SummaryEngine;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/4/2020
 */
@PipeBitInfo(
      name = "NoteSummarizer",
      description = "For dphe-stream.", role = PipeBitInfo.Role.SPECIAL
)
final public class NoteSummarizer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "NoteSummarizer" );

   /**
    * Call necessary processing for note
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {

      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      LOGGER.info( "Summarizing patient " + patientId + " ..." );

      final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );

      final PatientSummary patientSummary = SummaryEngine.createPatientSummary( patient );

      patientSummary.getNeoplasms().forEach( NoteSummarizer::logNeoplasm );

   }


   static private void logNeoplasm( final NeoplasmSummary neoplasm ) {
      LOGGER.info( "===================== Neoplasm " + neoplasm.getId() + " =====================" );
      neoplasm.getAttributes().forEach( NoteSummarizer::logAttribute );
      LOGGER.info( "====================================================" );
   }

   static private void logAttribute( final NeoplasmAttribute attribute ) {
      LOGGER.info( attribute.getName() + ": " + attribute.getClassUri() + " = " + attribute.getValue() );
   }

}
