package org.healthnlp.deepphe.uima.cc;

import org.apache.ctakes.core.cc.AbstractOutputFileWriter;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.summary.CancerSummary;
import org.healthnlp.deepphe.fhir.summary.MedicalRecord;
import org.healthnlp.deepphe.fhir.summary.PatientSummary;
import org.healthnlp.deepphe.uima.fhir.PhenotypeResourceFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SummaryTextWriter extends AbstractOutputFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "SummaryTextWriter" );

   static private final String BREAK = "\n-------------\n";

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas, final String outputDir,
                          final String documentId, final String fileName ) throws IOException {
      LOGGER.info( "Writing Summary Files for " + documentId + " ..." );

      final Patient patient = PhenotypeResourceFactory.loadPatient( jCas );
      final String patientName = patient != null ? patient.getPatientName() : "unknown";

      for ( Report report : PhenotypeResourceFactory.loadReports( jCas ) ) {
         saveText( report.getSummaryText(), new File( outputDir, patientName + File.separator + report.getTitle() + ".txt" ) );
      }

      final StringBuilder summary = new StringBuilder();

      final MedicalRecord record = PhenotypeResourceFactory.loadMedicalRecord( jCas );

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
      BufferedWriter w = new BufferedWriter( new FileWriter( file ) );
      LOGGER.info( "Writing Summary data data to " + file.getAbsolutePath() + " ..." );
      w.write( text + "\n" );
      w.close();
      LOGGER.info( "Finished." );
   }

}
