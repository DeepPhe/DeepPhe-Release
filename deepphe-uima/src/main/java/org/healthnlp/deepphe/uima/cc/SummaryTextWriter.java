package org.healthnlp.deepphe.uima.cc;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.summary.CancerSummary;
import org.healthnlp.deepphe.summary.MedicalRecord;
import org.healthnlp.deepphe.summary.MedicalRecordWriter;
import org.healthnlp.deepphe.summary.PatientSummary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SummaryTextWriter extends MedicalRecordWriter {

   static private final Logger LOGGER = Logger.getLogger( "SummaryTextWriter" );

   static private final String BREAK = "\n-------------\n";

   /**
    * {@inheritDoc}
    */
   @Override
   protected void writeMedicalRecord( final MedicalRecord record, final String outputDir ) throws IOException {
      final String patientName = record.getSimplePatientName();
      LOGGER.info( "Writing Summary Files for " + patientName + " ..." );


      final StringBuilder summary = new StringBuilder();
      final PatientSummary patientSummary = record.getPatientSummary();
      if ( patientSummary != null ) {
         summary.append( patientSummary.getSummaryText() ).append( BREAK );
      }
      final CancerSummary cancerSummary = record.getCancerSummary();
      if ( cancerSummary != null ) {
         summary.append( cancerSummary.getSummaryText() ).append( BREAK );
      }
      // save all summaries
      saveText( summary.toString(), new File( outputDir, patientName + File.separator + "MEDICAL_RECORD_SUMMARY.txt" ) );

      // save all facts
      final StringBuilder sb = new StringBuilder();
      for ( Fact f : record.getReportLevelFacts() ) {
         sb.append( f.getInfo() ).append( "\n" );
      }
      saveText( sb.toString(), new File( outputDir, patientName + File.separator + "ALL_REPORT_FACTS.txt" ) );

      LOGGER.info( "Finished Writing" );
   }

   /**
    * save generic text file
    *
    * @param text -
    * @param file -
    * @throws IOException -
    */
   private void saveText( String text, File file ) throws IOException {
      if ( !file.getParentFile().exists() ) {
         file.getParentFile().mkdirs();
      }
      try ( BufferedWriter w = new BufferedWriter( new FileWriter( file ) ) ) {
         LOGGER.info( "Writing Summary data data to " + file.getAbsolutePath() + " ..." );
         w.write( text + "\n" );
         LOGGER.info( "Finished." );
      }
   }

}
