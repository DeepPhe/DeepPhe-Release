package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.json.JsonNoteWriter;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.node.PatientCreator;
import org.healthnlp.deepphe.node.PatientNodeStore;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/17/2020
 */
@PipeBitInfo(
      name = "JsonNodeCreator",
      description = "Creates a note node from the cas and stores it.  The patient is also stored.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class JsonNodeCreator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "JsonNodeCreator" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adding Information to NodeStores ..." );
      final Note note = JsonNoteWriter.createNote( jCas );
      NoteNodeStore.getInstance().add( note.getId(), note );
      final Patient patient
            = PatientNodeStore.getInstance().getOrCreate( SourceMetadataUtil.getPatientIdentifier( jCas ) );
      PatientCreator.addNote( patient, note );
      PatientNodeStore.getInstance().add( patient.getId(), patient );
   }


}
