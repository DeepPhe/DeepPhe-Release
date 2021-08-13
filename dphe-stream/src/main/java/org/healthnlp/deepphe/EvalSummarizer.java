package org.healthnlp.deepphe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.nlp.pipeline.DmsRunner;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.eval.FeatureFilesAppender;
import org.healthnlp.deepphe.util.eval.ForEvalLineCreator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Run multiple docs and write output for scoring with the eval tool and output for features.
 */
final public class EvalSummarizer {

   static private final Logger LOGGER = Logger.getLogger( "EvalSummarizer" );

   static public final String PATIENT_ID = "Patient_ID";

   static private final List<String> ATTRIBUTE_NAMES
         = Arrays.asList( "topography_major",
                         "topography_minor",
                         "histology",
                         "behavior",
                         "laterality",
                         "grade",
                          "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
                          "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" );
   static private final List<String> EVAL_ATTRIBUTE_NAMES
         = Arrays.asList( "*record_id",
                          "-Summary_ID",
                          "topography_major",
                          "topography_minor",
                          "histology",
                          "behavior",
                          "laterality",
                          "grade",
                          "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
                          "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" );

   public static void main( final String... args ) {
      LOGGER.info( "Initializing ..." );
      DmsRunner.getInstance();
      LOGGER.info( "Reading docs in " + args[ 0 ] + " writing Output for Eval to " + args[ 1 ] );
      final File evalFile = new File( args[ 1 ] );
      final File featuresDir = new File( evalFile.getParentFile(), "features" );
      final File jsonDir = new File( evalFile.getParentFile(), "json" );
      final File debugDir = evalFile.getParentFile();
      featuresDir.mkdirs();
      jsonDir.mkdirs();
      try {
         FeatureFilesAppender.initFeatureFiles( featuresDir, ATTRIBUTE_NAMES );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE );
      }
      LOGGER.info( "Writing Header in " + evalFile.getPath() );
      try ( Writer evalWriter = new FileWriter( evalFile ) ) {
         // Our gold standard files use horrifically long "human-readable" names.
         for ( String attribute : EVAL_ATTRIBUTE_NAMES ) {
            evalWriter.write( attribute + "|" );
         }
         evalWriter.write( "\n" );
         evalWriter.flush();
         processDir( new File( args[ 0 ] ), jsonDir, evalWriter, featuresDir );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      DmsRunner.getInstance()
               .close();

      try ( Writer writer = new FileWriter( new File( debugDir, "EvalDebug.txt" ) ) ) {
         writer.write( NeoplasmSummaryCreator.DEBUG_SB.toString() );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }

      System.exit( 0 );
   }

   static private void processDir( final File dir,
                                   final File jsonDir,
                                   final Writer evalWriter,
                                   final File featureDir )
         throws IOException {
      LOGGER.info( "Processing directory " + dir.getPath() );
      final File[] files = dir.listFiles();
      if ( files == null ) {
         return;
      }
      for ( File file : files ) {
         if ( file.isDirectory() ) {
            processDir( file, jsonDir, evalWriter, featureDir );
         } else {
            processDoc( file, jsonDir, evalWriter, featureDir );
         }
      }
   }

   static private void processDoc( final File file,
                                   final File jsonDir,
                                   final Writer evalWriter,
                                   final File featureDir )
         throws IOException {
      String docId = file.getName();
      final int dotIndex = docId.lastIndexOf( '.' );
      if ( dotIndex > 0 ) {
         docId = docId.substring( 0, dotIndex );
      }
      String text = "";
      try {
         text = new String( Files.readAllBytes( Paths.get( file.getPath() ) ) );
      } catch ( IOException ioE ) {
         LOGGER.error( "Processing Failed:\n" + ioE.getMessage() );
         System.exit( -1 );
      }
      // Process doc text
      final PatientSummary summary = DmsRunner.getInstance()
                                      .createPatientSummary( docId, text );
      if ( summary == null ) {
         LOGGER.error( "Could not process doc " + docId );
         return;
      }
      final String patientId = summary.getPatient()
                                      .getId();
      for ( NeoplasmSummary neoplasm : summary.getNeoplasms() ) {
         writeEval( patientId, neoplasm, evalWriter );
         writeFeatures( patientId, neoplasm, featureDir );
      }
      writeJson( summary, jsonDir );
   }

   static private void writeEval( final String patientId,
                                  final NeoplasmSummary summary,
                                  final Writer writer ) throws IOException {
      writer.write( ForEvalLineCreator.createBsv( patientId, summary, ATTRIBUTE_NAMES ) );
   }

   static private void writeFeatures( final String patientId,
                                      final NeoplasmSummary summary,
                                      final File featureDir ) {
      FeatureFilesAppender.appendFeatureFiles( patientId, summary, featureDir, ATTRIBUTE_NAMES );
   }

   static private void writeJson( final PatientSummary summary, final File jsonDir ) {
      final List<NeoplasmSummary> neoplasms = summary.getNeoplasms();
      for ( NeoplasmSummary neoplasm : neoplasms ) {
         final List<NeoplasmAttribute> attributes = neoplasm.getAttributes();
         attributes.forEach( a -> a.setConfidenceFeatures( Collections.emptyList() ) );
         neoplasm.setAttributes( attributes );
      }
      summary.setNeoplasms( neoplasms );
      final String patientId = summary.getPatient()
                                      .getId();
      try ( Writer writer = new FileWriter( new File( jsonDir, patientId+".json" ) ) ) {
         final Gson gson = new GsonBuilder().setPrettyPrinting().create();
         final String json = gson.toJson( summary );
         writer.write( json + "\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

}
