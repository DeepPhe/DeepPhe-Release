package org.healthnlp.deepphe.util;

import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.nlp.pipeline.DmsRunner;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.summary.engine.SummaryEngine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EvalPipelineRunner {


   public static void main( String[] args ) {

      final Map<String, Collection<String>> patientNotesMap = new HashMap<>();  // readCorpusDir

      for ( Map.Entry<String,Collection<String>> patientNotes : patientNotesMap.entrySet() ) {
         final String patientId = patientNotes.getKey();
         for ( String noteId : patientNotes.getValue() ) {
            final String text = ""; // readNote
            DmsRunner.getInstance().summarizeAndStoreDoc( patientId, noteId, text );
         }

         final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );
         PatientNodeStore.getInstance().remove( patientId );
         final PatientSummary patientSummary = SummaryEngine.createPatientSummary( patient );
         // Write to eval file
      }

      PatientNodeStore.getInstance().close();
   }

   static private String createPatientSummaryLines( final PatientSummary patientSummary ) {
      final StringBuilder sb = new StringBuilder();
      final String patientId = patientSummary.getPatient() != null
                               ? patientSummary.getPatient().getId()
                               : patientSummary.getId();
      patientSummary.getNeoplasms()
                    .stream()
                    .map( n -> createNeoplasmSummaryLine( patientId, n ) )
                    .forEach( sb::append );
      return sb.toString();
   }

   static private String createNeoplasmSummaryLine( final String patientId, final NeoplasmSummary neoplasm ) {
      return patientId + "|"
            + neoplasm.getId() + "|"
            + neoplasm.getPathologic_t() + "|"
            + neoplasm.getPathologic_t() + "|"
            + neoplasm.getPathologic_n() + "|"
            + neoplasm.getPathologic_m() + "|"
            + neoplasm.getEr() + "|"
            + neoplasm.getPr() + "|"
            + neoplasm.getHer2() + "\n";
   }

}
