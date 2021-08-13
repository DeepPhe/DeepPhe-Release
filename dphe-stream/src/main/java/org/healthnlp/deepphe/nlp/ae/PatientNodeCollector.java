package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.node.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
@PipeBitInfo(
      name = "PatientNodeCollector",
      description = "For dphe-stream.", role = PipeBitInfo.Role.SPECIAL
)
final public class PatientNodeCollector extends JCasAnnotator_ImplBase {

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
      final Note note = NoteNodeCreator.createNote( jCas );
      if ( storeNote ) {
         NoteNodeStore.getInstance().add( note.getId(), note );
         if ( !storePatient ) {
            return;
         }
      }
      final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      PatientCreator.addNote( patient, note );
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
