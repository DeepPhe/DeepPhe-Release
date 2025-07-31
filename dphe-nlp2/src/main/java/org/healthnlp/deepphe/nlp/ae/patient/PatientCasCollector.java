package org.healthnlp.deepphe.nlp.ae.patient;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.patient.PatientCasCreator;
import org.healthnlp.deepphe.nlp.patient.PatientCasStore;
import org.healthnlp.deepphe.nlp.refactor.node.StorageChoices;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
@PipeBitInfo(
      name = "PatientCasCollector",
      description = "Places document JCas in Patient JCas", role = PipeBitInfo.Role.SPECIAL
)
final public class PatientCasCollector extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PatientNodeCollector" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      final boolean storePatient = StorageChoices.getInstance().getStorePatient( patientId );
      final String noteId = DocIdUtil.getDocumentID( jCas );
      final boolean storeNote = StorageChoices.getInstance().getStoreNote( noteId );
      if ( !storePatient && !storeNote ) {
         LOGGER.warn( "Not storing " + patientId + " " + noteId + " " + storePatient + " " + storeNote );
         return;
      }
      final String message = "Caching"
                             + ( storePatient ? " patient " + patientId : "" )
                             + ( storeNote ? " note " + noteId : "" );
      LOGGER.info( message );
      final JCas patient = PatientCasStore.getInstance().getOrCreate( patientId );
      PatientCasCreator.addDocCas( patient, jCas );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      StorageChoices.getInstance().shutdown();
   }


}
