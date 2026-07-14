package org.healthnlp.deepphe.nlp.util;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.log.DotLogger;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VumcUnzipper {

    private static final Pattern CR_LF = Pattern.compile("\\r\\n");


    //  PatientID-YYYYMMDD-Category-Doc_Type-Index-EncounterNum??.txt
    //  !!!!  Sometimes Date also has time?:  "20160121_101600"

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
            for ( int i=1; i<1000; i++ ) {
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

    static private String getNewFilepath( final String filename ) {
        final String[] splits = splitFilename( filename );
        final String patientId = getPatientId( splits );
        final String newFilename = getNewFilename( splits );
        return patientId + "/" + newFilename;
    }


    static private boolean isWantedFile( final String filename ) {
        final String lower = filename.toLowerCase();
        // 99% of the time the above are useless, discarding to prevent false positives.
        // True Positives should be picked up in other notes.  Though some info is valid, no cancer mentions.
        return !lower.contains("myhealthteam@vanderbilt_plan_of_care")
                && !lower.contains("fuel_reimbursement_form")
                && !lower.contains("-social_history-")
                && !lower.contains("-intake_assessment-")
                && !lower.contains("-braden_assessment-")
                && !lower.contains("-port_draw-")
                && !lower.contains("-research_lab_draw-")
                && !lower.contains("-core_measures-")
                && !lower.contains("-nurse")
                && !lower.contains("-parish_nurse_note")
                && !lower.contains("-phone_call-")
                && !lower.contains("-prescription")
                && !lower.contains("-immunizations-")
                && !lower.contains("-anesthetic_care_record-")
                // It is rare, but sometimes a clinical communication does contain info on mass or procedure.
//                  || lower.contains( "-clinical_communication" )
                && !lower.contains("-clinical_communication_(")
                && !lower.contains("-provider_communication")
                && !lower.contains("-clinic_summary-")
                && !lower.contains("-surgical_teaching")
                && !lower.contains("-dentistry_");
    }

    static private String readFile( final ZipFile zipFile, final ZipEntry entry ) throws IOException {
        // Use 8KB as the default buffer size
        byte[] buffer = new byte[ 8192 ];
        final StringBuilder sb = new StringBuilder();
        try (final InputStream inputStream = new BufferedInputStream( zipFile.getInputStream( entry ), buffer.length );
             DotLogger dotter = new DotLogger() ) {
            while ( true ) {
                final int length = inputStream.read( buffer );
                if ( length < 0 ) {
                    break;
                }
                sb.append( new String( buffer, 0, length ) );
            }
        }
        return sb.toString();
    }

    static private void unzipFile( final File file, final String outputRoot ) {
        final Enumeration<? extends ZipEntry> entries;
        System.out.println( file.getName() );
        try ( DotLogger dotter = new DotLogger() ) {
            ZipFile zipFile = new ZipFile( file );
            entries = zipFile.entries();
//            int entryCount = 0;
            while ( entries.hasMoreElements() ) {
                final ZipEntry zipEntry = entries.nextElement();
                final String entryPath = zipEntry.getName();
                if ( !isWantedFile( entryPath ) ) {
                    continue;
                }
                final String entryText = readFile( zipFile, zipEntry );
                final String text = VumcDocFixer.fixText( entryText );
                if ( text.isEmpty() ) {
                    continue;
                }
//                entryCount++;
//                if ( entryCount % 100 == 0 ) {
//                    System.out.print( "." );
//                }
//                final String newFilePath = outputRoot + getNewFilepath( entryPath );
                final String newFilePath = outputRoot + entryPath;
                final File newFile = new File( newFilePath );
                newFile.getParentFile().mkdirs();
                try ( Writer writer = new BufferedWriter( new FileWriter( newFile ) ) ) {
                    writer.write( text );
                } catch ( IOException ioE ) {
                    ioE.printStackTrace();
                    System.exit( 1 );
                }
            }
            zipFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println();
    }


    public static void main(String[] args) {
        final File rootDir = new File ( "C:\\spiffy\\corpus\\notes_in_10_zips" );
        final String outputRoot = "C:/spiffy/corpus/notes_in_dirs/";
        final File[] files = Objects.requireNonNull( rootDir.listFiles() );
        for ( File file : files ) {
            unzipFile( file, outputRoot );
        }
    }

}
