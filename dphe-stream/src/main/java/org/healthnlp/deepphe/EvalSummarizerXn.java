package org.healthnlp.deepphe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CASRuntimeException;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.node.PatientCreator;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.summary.DpheXnRunner;
import org.healthnlp.deepphe.summary.engine.DpheXnSummaryEngine;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.eval.ForEvalLineCreator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * dphe-cr
 * Run multiple docs and write output for scoring with the eval tool and output for features.
 */
final public class EvalSummarizerXn {

   static private final Logger LOGGER = Logger.getLogger( "EvalSummarizerXn" );

   static public final String PATIENT_ID = "Patient_ID";

   static private final List<String> ID_NAMES = Arrays.asList(  "*Patient_ID", "-record_id" );

//*record_id|Patient_ID|location|laterality|historic|stage|t|n|m|cli_prefix|cli_suffix|path_prefix|path_suffix
   static private final List<String> CANCER_ATTRIBUTE_NAMES
         = Arrays.asList( "location",
                          "-topography_major",
                          "laterality",
                         "-laterality_code",

                         "-grade",
                          "stage",
                          "t", "n", "m",

                          "-historic" );
   //  Gold uses "cancer_type" and "histologic_type" to denote something -like- histology.
   //  "tumor_type" is primary, metastatic, etc.
   //  "extent" is behavior.
   static private final List<String> TUMOR_ATTRIBUTE_NAMES
         = Arrays.asList( "location",
                          "-topography_major",
                          "-topography_minor",
                          "clockface",
                          "quadrant",
                          "laterality",
                          "-laterality_code",

                          "diagnosis",
                          "histology",
                          "-histologic_type",
                          "cancer_type",
                          "extent",
                          "-behavior",
                          "tumor_type",

                          "-tumor_size",
                          "-tumor_size_procedure",

                          "-calcifications",

                          "ER_",
                          "-ER_amount",
                          "-ER_procedure",

                          "PR_",
                          "-PR_amount",
                          "-PR_procedure",

                          "HER2",
                          "-HER2_amount",
                          "-HER2_procedure",

                          "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
                          "PDL1", "MSI", "KRAS", "PSA", "PSA_EL",

                          "-treatment" );

   public static void main( final String... args ) {
      LOGGER.info( "Initializing ..." );
      DpheXnRunner.getInstance();
      LOGGER.info( "Reading docs in " + args[ 0 ] + " writing Output for Eval to " + args[ 1 ] );
      final File cancerFile = new File( args[ 1 ] + "_cancer.bsv" );
      final File tumorFile = new File( args[ 1 ] + "_tumor.bsv" );
      final File featuresDir = new File( cancerFile.getParentFile(), "features" );
      final File jsonDir = new File( cancerFile.getParentFile(), "json" );
      final File debugDir = cancerFile.getParentFile();
      featuresDir.mkdirs();
      jsonDir.mkdirs();
//      try {
//         FeatureFilesAppender.initFeatureFiles( featuresDir, ATTRIBUTE_NAMES );
//      } catch ( IOException ioE ) {
//         LOGGER.error( ioE );
//      }
      LOGGER.info( "Writing Headers in eval output files ..." );
      try ( Writer cancerWriter = new FileWriter( cancerFile );
            Writer tumorWriter = new FileWriter( tumorFile ) ) {
         // Our gold standard files use horrifically long "human-readable" names.
         for ( String id : ID_NAMES ) {
            cancerWriter.write( id + "|" );
            tumorWriter.write( id + "|" );
         }
         for ( String attribute : CANCER_ATTRIBUTE_NAMES ) {
            cancerWriter.write( attribute + "|" );
         }
         for ( String attribute : TUMOR_ATTRIBUTE_NAMES ) {
            tumorWriter.write( attribute + "|" );
         }
         cancerWriter.write( "\n" );
         cancerWriter.flush();
         tumorWriter.write( "\n" );
         tumorWriter.flush();
         processDir( new File( args[ 0 ] ), jsonDir, cancerWriter, tumorWriter, featuresDir, debugDir );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      DpheXnRunner.getInstance()
               .close();

      System.exit( 0 );
   }

   static private void processDir( final File dir,
                                   final File jsonDir,
                                   final Writer cancerWriter,
                                   final Writer tumorWriter,
                                   final File featureDir, final File debugDir )
         throws IOException {
      LOGGER.info( "Processing directory " + dir.getPath() );
      final File[] files = dir.listFiles();
      if ( files == null ) {
         LOGGER.error( "No files in " + dir );
         return;
      }
      for ( File file : files ) {
         if ( file.isDirectory() ) {
            processPatientDir( file, jsonDir, cancerWriter, tumorWriter, featureDir, debugDir );
         } else {
            LOGGER.error( dir.getPath() + " contains a file " + file.getName() );
            LOGGER.error( "Ignoring " + file.getName() );
         }
      }
   }

   static private void processPatientDir( final File dir,
                                   final File jsonDir,
                                   final Writer cancerWriter,
                                   final Writer tumorWriter,
                                   final File featureDir, final File debugDir )
         throws IOException {
      LOGGER.info( "Processing patient " + dir.getPath() );
      final File[] files = dir.listFiles();
      if ( files == null ) {
         return;
      }
      final String patientId = dir.getName();
      final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      for ( File file : files ) {
         if ( file.isDirectory() ) {
            LOGGER.error( dir.getPath() + " contains a directory " + file.getName() );
            LOGGER.error( "Ignoring " + file.getName() );
         } else {
            if ( file.getName().equals( ".DS_Store" ) ) {
               continue;  // because apple just insists upon breaking everything.
            }
            processDoc( patient, file );
         }
      }
      processPatient( patient, jsonDir, cancerWriter, tumorWriter );
      PatientNodeStore.getInstance().remove( patientId );
      patient.getNotes().forEach( n -> NoteNodeStore.getInstance().remove( n.getId() ) );
      try ( Writer writer = new FileWriter( new File( debugDir, patientId + "_EvalDebug.txt" ) ) ) {
         writer.write( NeoplasmSummaryCreator.getDebug() );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      NeoplasmSummaryCreator.resetDebug();
   }

   static private void processDoc( final Patient patient,
                                   final File file ) {
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
      try {
         final Note note = DpheXnRunner.getInstance().runNlp( patient.getId(), docId, text );
         if ( note == null ) {
            LOGGER.error( "Processing Failed for file " + file.getPath() );
            return;
         }
         NoteNodeStore.getInstance().add( note.getId(), note );
         PatientCreator.addNote( patient, note );
      } catch ( CASRuntimeException multE ) {
         LOGGER.error( multE.getMessage() );
      }
   }

   static private void processPatient( final Patient patient,
                                       final File jsonDir,
                                       final Writer cancerWriter,
                                       final Writer tumorWriter )
         throws IOException {
      final PatientSummary summary =  DpheXnSummaryEngine.createPatientSummary( patient );
//      if ( summary == null ) {
//         LOGGER.error( "Could not process doc " + docId );
//         return;
//      }
      for ( NeoplasmSummary neoplasm : summary.getNeoplasms() ) {
         writeCancerEval( patient.getId(), neoplasm, cancerWriter );
         writeTumorEval( patient.getId(), neoplasm, tumorWriter );
//         writeFeatures( patientId, neoplasm, featureDir );
      }
      writeJson( summary, jsonDir );
   }

   static private void writeCancerEval( final String patientId,
                                  final NeoplasmSummary summary,
                                  final Writer writer ) throws IOException {
      writer.write( ForEvalLineCreator.createBsv( patientId, summary, CANCER_ATTRIBUTE_NAMES ) );
   }

   static private void writeTumorEval( final String patientId,
                                        final NeoplasmSummary summary,
                                        final Writer writer ) throws IOException {
      final Collection<NeoplasmSummary> tumorSummaries = summary.getSubSummaries();
      for ( NeoplasmSummary tumorSummary : tumorSummaries ) {
         writer.write( ForEvalLineCreator.createBsv( patientId, tumorSummary, TUMOR_ATTRIBUTE_NAMES ) );
      }
   }

//   static private void writeFeatures( final String patientId,
//                                      final NeoplasmSummary summary,
//                                      final File featureDir ) {
//      FeatureFilesAppender.appendFeatureFiles( patientId, summary, featureDir, ATTRIBUTE_NAMES );
//   }

   static private void writeJson( final PatientSummary summary, final File jsonDir ) {
//      final List<NeoplasmSummary> neoplasms = summary.getNeoplasms();
//      for ( NeoplasmSummary neoplasm : neoplasms ) {
//         final List<NeoplasmAttribute> attributes = neoplasm.getAttributes();
//         attributes.forEach( a -> a.setConfidenceFeatures( Collections.emptyList() ) );
//         neoplasm.setAttributes( attributes );
//      }
//      summary.setNeoplasms( neoplasms );
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
