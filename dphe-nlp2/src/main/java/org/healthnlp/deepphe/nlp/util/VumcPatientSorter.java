package org.healthnlp.deepphe.nlp.util;

import org.apache.ctakes.core.util.StringUtil;
import org.eclipse.collections.impl.bag.mutable.HashBag;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author SPF , chip-nlp
 * @since {4/11/2024}
 */
final public class VumcPatientSorter {

   //  PatientID-YYYYMMDD-Category-Doc_Type-Index-EncounterNum??.txt
   //  !!!!  Sometimes Date also has time?:  "20160121_101600"

   static private int _fileCount = 0;
   static private int _badSplitCount = 0;

   static private String[] splitFilename( final String oldFilename ) {
      final String[] splits = StringUtil.fastSplit( oldFilename, '-' );
      final int splitCount = splits.length;
      if ( splitCount != 6 ) {
         // Inappropriate number of splits.
         _badSplitCount++;
      }

      // Only return filled splits 0, 1, 4, 5
      final String[] wantedSplits = new String[ 5 ];
      wantedSplits[ 0 ] = splits[ 0 ];
      wantedSplits[ 1 ] = splits[ 1 ];
      wantedSplits[ 2 ] = splits[ 2 ];
      wantedSplits[ 3 ] = splits[ splitCount - 2 ];
      wantedSplits[ 4 ] = splits[ splitCount - 1 ];
      return wantedSplits;
   }

   static private String getPatientId( final String[] splits ) {
      return splits[ 0 ];
   }

   static private String getDate( final String[] splits ) {
      final String dateTime = splits[ 1 ];
      final int timeSplit = dateTime.indexOf( '_' );
      if ( timeSplit < 6 ) {
         // Normally MMDDyyyy, but use 6 just in case it is MMDDyy
         return dateTime;
      }
      return dateTime.substring( 0, timeSplit );
   }


   static private final Collection<String> _usedFilenames = new HashSet<>( 300000 );

   static private String getCategory( final String[] splits ) {
      final String source = splits[ 2 ];
      if ( source.isEmpty() ) {
         return "Unknown";
      }
      return source.replace( ' ', '_' );
   }

   static private final char[] BAD_CHARS = { ' ', '&', '{', '}', '[', ']', '(', ')', '@' };

   static private String getNewFilename( final String[] splits ) {
      // Sometimes no index is given.
      final String index = splits[ 3 ].isEmpty() ? "0" : splits[ 3 ];
      // Remove the .txt filename extension;
      final String encounter = splits[ 4 ].substring( 0, splits[ 4 ].length() - 4 );
      final String date = getDate( splits );
      final String category = getCategory( splits );
      String join = encounter + "-" + index + "-" + category + "-" + date + ".txt";
      for ( char c : BAD_CHARS ) {
         join = join.replace( c, '_' );
      }
      if ( _usedFilenames.contains( join ) ) {
         for ( int i=1; i<100; i++ ) {
            join = encounter + "-" + i + "-" + category + "-" + date + ".txt";
            for ( char c : BAD_CHARS ) {
               join = join.replace( c, '_' );
            }
            if ( !_usedFilenames.contains( join ) ) {
               break;
            }
         }
      }
      _usedFilenames.add( join );
      return join;
   }

   static private String getZipEntryname( final String filename ) {
      final String[] splits = splitFilename( filename );
      final String patientId = getPatientId( splits );
      final String newFilename = getNewFilename( splits );
      return patientId + "/" + newFilename;
   }

   static private void zipFiles( final File inputDir, final List<String> filenames,
                                 final String outputZipPath ) {
      try ( FileOutputStream outputStream = new FileOutputStream( outputZipPath );
            ZipOutputStream zipStream = new ZipOutputStream( outputStream ) ) {
         for ( String filename : filenames ) {
            final String lower = filename.toLowerCase();
            if ( lower.contains( "myhealthteam@vanderbilt_plan_of_care" )
                  || lower.contains( "fuel_reimbursement_form" )
                  || lower.contains( "-social_history-" )
                  || lower.contains( "-intake_assessment-" )
                  || lower.contains( "-braden_assessment-" )
                  || lower.contains( "-port_draw-" )
                  || lower.contains( "-research_lab_draw-" )
                  || lower.contains( "-core_measures-" )
                  || lower.contains( "-nurse" )
                  || lower.contains( "-parish_nurse_note" )
                  || lower.contains( "-phone_call-" )
                  || lower.contains( "-prescription" )
                  || lower.contains( "-immunizations-" )
                  || lower.contains( "-anesthetic_care_record-" )
                  // It is rare, but sometimes a clinical communication does contain info on mass or procedure.
//                  || lower.contains( "-clinical_communication" )
                  || lower.contains( "-clinical_communication_(" )
                  || lower.contains( "-provider_communication" )
                  || lower.contains( "-clinic_summary-" )
                  || lower.contains( "-surgical_teaching" )
                  || lower.contains( "-dentistry_" ) ) {
               // 99% of the time the above are useless, discarding to prevent false positives.
               // True Positives should be picked up in other notes.  Though some info is valid, no cancer mentions.
               continue;
            }
            final File fileToZip = new File( inputDir, filename );
//            final FileInputStream inputStream = new FileInputStream( fileToZip );
            final String text = VumcDocFixer.fixDoc( fileToZip );
            if ( text.isEmpty() ) {
               continue;
            }
            final InputStream inputStream = new ByteArrayInputStream( text.getBytes() );
            final String zipEntryname = getZipEntryname( filename );
            final ZipEntry zipEntry = new ZipEntry( zipEntryname );
            zipStream.putNextEntry( zipEntry );
            byte[] bytes = new byte[ 1024 ];
            int length;
            while ( (length = inputStream.read( bytes )) >= 0 ) {
               zipStream.write( bytes, 0, length );
            }
            inputStream.close();
            _fileCount++;
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
   }

   public static void main( final String... args ) {
      if ( args.length < 3 ) {
         System.out.println( "Please enter inputPath , outputPath and zip filename as arguments." );
         System.exit( 0 );
      }
      final String inputPath = args[ 0 ];
      final String outputPath = args[ 1 ];
      final String zipFileName = args[ 2 ];
      final File inputDir = new File( inputPath );
      if ( !inputDir.isDirectory() ) {
         System.out.println( "Input Dir " + inputPath + " does not exist." );
         System.exit( 0 );
      }
      final File outputDir = new File( outputPath );
      outputDir.mkdirs();
      final List<String> filenames = Arrays.stream( Objects.requireNonNull( inputDir.listFiles() ) )
                                           .map( File::getName )
                                           .collect( Collectors.toList() );
      if ( filenames.isEmpty() ) {
         System.out.println( "No files in " + outputPath );
      }
      final String outputZipPath = new File( outputDir, zipFileName ).getAbsolutePath();
      zipFiles( inputDir, filenames, outputZipPath );
      System.out.println( "Files zipped: " + _fileCount );
      System.out.println( "Bad Filename Splits: " + _badSplitCount );
   }

}
