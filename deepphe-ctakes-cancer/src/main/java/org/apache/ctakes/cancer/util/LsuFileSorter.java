package org.apache.ctakes.cancer.util;

import org.apache.ctakes.cancer.cr.Hl7Reader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/15/2017
 */
public class LsuFileSorter {

   static private final Logger LOGGER = Logger.getLogger( "LsuFileSorter" );

   static private final String ALL_NOTES_DIR = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\cases_100\\all_notes";
   static private final String[] HL7_RAD_PREFIX = { "OHS", "OLOL" };
   static private final String[] HL7_PATH_PREFIX = { "", "TMG" };

   static private final String ROOT_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\cases_100";
   static private final String BR_CA_OUT = "brca";
   static private final String SKIN_OUT = "skin";
   static private final String UNKNOWN_OUT = "unknown";

   static private final String ABSTRACT_FILE = "C:\\Spiffy\\prj_darth_phenome\\data\\lsu\\cases_100\\abstract\\All_Abstracts.txt";

   static private final int TUMOR_INDEX = 886;
   static private final int TUMOR_LENGTH = 2;



   static private final int CANCER_INDEX = 540 - 1;
   static private final int CANCER_LENGTH = 3;
   static private final String BRCA_CODE = "C50";
   static private final String SKIN_CODE = "C44";

   static private final int PATIENT_INDEX = 42 - 1;
   static private final int PATIENT_LENGTH = 8;


   static private String createDpheFileName( final String hl7FileName, final String pid, final int index ) {
      String type = "SP";
      if ( hl7FileName.startsWith( "OHS" ) || hl7FileName.startsWith( "OLOL" ) ) {
         type = "RAD";
      }
      return "patient" + pid + "_report" + String.format( "%04d",index ) + "_" + type + ".txt";
   }

   private LsuFileSorter() {
      // file to patient name.   PID has to be parsed from each hl7 file.
      final Map<String,String> filePatient = new HashMap<>();
      // file to extracted text.
      final Map<String,String> fileText = new HashMap<>();
      // patient to cancer type.   PID (PATIENT_INDEX) and Cancer type (CANCER_INDEX) have to be parsed from abstract.
      final Map<String,String> patientCaType = new HashMap<>();
      // original hl7 filenames to new patient and note-based text filenames.
      final Map<String,String> hl7toTxt = new HashMap<>();

      final File rootDir = new File( ALL_NOTES_DIR );
      final File[] files = rootDir.listFiles();
      if ( files == null ) {
         System.err.println( "No files" );
         System.exit( 1 );
      }
      try {
         final Path path = new File( ABSTRACT_FILE ).toPath();
         final Collection<String> abstractLines = Files.readAllLines( path );
         for ( String line : abstractLines ) {
            final String cancerCode = line.substring( CANCER_INDEX, CANCER_INDEX + CANCER_LENGTH );
            final String pid = line.substring( PATIENT_INDEX, PATIENT_INDEX + PATIENT_LENGTH ).trim();

            String outDir = UNKNOWN_OUT;
            if ( cancerCode.equals( BRCA_CODE ) ) {
               outDir = BR_CA_OUT;
            } else if ( cancerCode.equals( SKIN_CODE ) ) {
               outDir = SKIN_OUT;
            }
            patientCaType.put( pid, outDir );
         }

         int fileIndex = 1;
         for ( File file : files ) {
            final String hl7Name = file.getName();
            final String pid = getPid( file );
            filePatient.put( hl7Name, pid );

            final String text = Hl7Reader.readFileText( file );
            fileText.put( hl7Name, text );

            final String txtName = createDpheFileName( hl7Name, pid, fileIndex );
            hl7toTxt.put( hl7Name, txtName );
            fileIndex++;
         }
         // write the text files
         final List<String> fileMapping = new ArrayList<>();
         for ( Map.Entry<String,String> entry : hl7toTxt.entrySet() ) {
            final String hl7 = entry.getKey();
            final String txtName = entry.getValue();
            final String text = fileText.get( hl7 );
            final String pid  = filePatient.get( hl7 );
            final String childType = patientCaType.get( pid );
            final String typeDir = ROOT_OUT + "/" + childType;
            final File outDir = new File( typeDir, pid );
            outDir.mkdirs();
            final Path outPath = new File( outDir, txtName ).toPath();
            Files.write( outPath, Collections.singletonList( text ), StandardOpenOption.CREATE );
            fileMapping.add( txtName + "|" + hl7 );
         }
         // write the file map
         fileMapping.sort( String::compareToIgnoreCase );
         final Path outPath = new File( ROOT_OUT, "FileMap.bsv" ).toPath();
         Files.write( outPath, fileMapping, StandardOpenOption.CREATE );
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }

   }

   static private String getPid( final File file ) throws IOException {
      final Path path = file.toPath();
      final Collection<String> lines = Files.readAllLines( path );
      for ( String line : lines ) {
         if ( line.startsWith( "PID|" ) ) {
            return line.substring( 4 ).trim();
         }
      }
      throw new IOException( "No PID in " + file.getName() );
   }


   public static void main( final String... args ) {
      new LsuFileSorter();
   }

}
