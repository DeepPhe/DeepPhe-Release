package org.healthnlp.deepphe.nlp.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.node.NoteNodeCreator;
import org.healthnlp.deepphe.node.PatientCreator;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.node.PatientSummaryNodeStore;
import org.healthnlp.deepphe.summary.engine.SummaryEngine;

import java.io.*;
import java.util.Collections;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @since {4/26/2021}
 */
public class PatientJsonFileWriter extends AbstractFileWriter<Patient> {

   private Patient _patient;

   /**
    * Sets data to be written to the jcas.
    *
    * @param jCas ye olde
    */
   @Override
   protected void createData( final JCas jCas ) {
      final Note note = NoteNodeCreator.createNote( jCas );
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      _patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      PatientCreator.addNote( _patient, note );
   }

   /**
    * @return the JCas.
    */
   @Override
   protected Patient getData() {
      return _patient;
   }

   /**
    * called after writing is complete
    *
    * @param data -
    */
   @Override
   protected void writeComplete( final Patient data ) {
   }

   /**
    * Write information into a file named based upon the document id and located based upon the document id prefix.
    *
    * This will write one file per patient, named after the patient, with each row containing columns of cuis.
    *
    * @param patient       data to be written
    * @param outputDir  output directory
    * @param documentId -- not used --
    * @param fileName   -- not used --
    * @throws IOException if anything goes wrong
    */
   public void writeFile( final Patient patient,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final String patientId = patient.getId();
      // Even though the ctakes PatientNoteStore isn't being used to store any patient jcas, it still has note counts.
      final int patientDocCount = PatientNoteStore.getInstance()
                                                  .getWantedDocCount( patientId );
      if ( patient.getNotes().size() < patientDocCount ) {
         return;
      }
      // Somebody else may have already created the patient summary.
      PatientSummary patientSummary = PatientSummaryNodeStore.getInstance().get( patientId );
      if ( patientSummary == null ) {
         // Create PatientSummary
         patientSummary = SummaryEngine.createPatientSummary( patient );
         // Add the summary just in case some other consumer can utilize it.  e.g. eval file writer.
         PatientSummaryNodeStore.getInstance().add( patientId, patientSummary );
      }
      final List<NeoplasmSummary> neoplasms = patientSummary.getNeoplasms();
      for ( NeoplasmSummary neoplasm : neoplasms ) {
         final List<NeoplasmAttribute> attributes = neoplasm.getAttributes();
         attributes.forEach( a -> a.setConfidenceFeatures( Collections.emptyList() ) );
         neoplasm.setAttributes( attributes );
      }
      patientSummary.setNeoplasms( neoplasms );
      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      final String summaryJson = gson.toJson( patientSummary );
      final File file = new File( outputDir, patientId + ".json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( summaryJson );
      }
   }



}
