package org.healthnlp.deepphe.util;

import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;

public class Gold815HistologyCollector {
// record_id|histology|behavior|topography|laterality|grade|seer_site|reportable|processed_text|notes from Dave

   public static void main( String[] args ) {

      //                                                             LUNG ?         LUNG ?   NOT Prostate
      final Collection<String> NOS_NOTES_FROM_SEAN = Arrays.asList( "20110170", "20100012", "20100161" );
      final String GOLD_PATH
            = "C:\\Spiffy\\data\\dphe_data\\datasets\\KCR\\preexisting_gold_annotations\\preexisting_815\\previous_gold_annotations_dave.bsv";

      final String TRIMMED_GOLD = "C:\\Spiffy\\data\\dphe_data\\datasets\\KCR\\preexisting_gold_annotations\\gold_for_3_cancers.bsv";
      final String CORPUS_DIR = "C:\\Spiffy\\data\\dphe_data\\datasets\\KCR\\preexisting_gold_annotations\\corpus_for_3_cancers\\";

      final Collection<String> WANTED_SITES = Arrays.asList( "Breast",
                                                             "Prostate",
                                                             "Bronchus", "Esophagus", "Larynx", "Pharynx",
                                                             "Trachea", "Respiratory_System", "Hypopharynx" );


//      final Map<String,String> histologies = new HashMap<>();
//      final Map<String,String> topographies = new HashMap<>();

      int i = 0;
      try ( BufferedReader reader = new BufferedReader( new FileReader( GOLD_PATH ) ); FileWriter goldWriter = new FileWriter( TRIMMED_GOLD ) ) {
         goldWriter.write( "record_id|histology|behavior|topography|laterality|grade|seer_site|reportable|NOT_processed_text|notes from Dave\n" );
         String line = reader.readLine();
         while ( line != null ) {
            i++;
            line = line.trim();
            if ( i == 1 || line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length < 9 ) {
               line = reader.readLine();
               continue;
            }
            final String docId = splits[ 0 ].trim();
            final String siteClass = TopoMorphValidator.getInstance().getSiteClass( splits[ 3 ].trim().substring( 0, 3 ) );
            if ( !WANTED_SITES.contains( siteClass ) && !NOS_NOTES_FROM_SEAN.contains( docId ) ) {
               line = reader.readLine();
               continue;
            }
            final StringBuilder builder = new StringBuilder();
            builder.append( splits[ 8 ] ).append( "\n" );
            for (;;) {
               line = reader.readLine();
               if ( line == null ) {
                  break;
               }
               if ( line.endsWith( "|" ) ) {
                  builder.append( line, 0, line.length() - 1 ).append( "\n" );
                  break;
               }
               builder.append( line ).append( "\n" );
            }
            final String docText = builder.toString().replace( "\"", "" );
               // WRITE GOLD
            goldWriter.write( docId + "|"
                              + splits[ 1 ].trim() + "|"
                              + splits[ 2 ].trim() + "|"
                              + splits[ 3 ].trim() + "|"
                              + splits[ 4 ].trim() + "|"
                              + splits[ 5 ].trim() + "|"
                              + splits[ 6 ].trim() + "|"
                              + splits[ 7 ].trim() + "|||\n" );
            // WRITE DOCUMENT FILE
            final File docDir = new File( CORPUS_DIR, docId );
            docDir.mkdirs();
            Writer docWriter = new FileWriter( new File( docDir, docId ) );
            docWriter.write( docText );
            docWriter.close();
            if ( line == null ) {
               // Somehow reached a null within a document text
               break;
            }
//            histologies.put( splits[ 0 ].trim(), splits[ 1 ].trim() );
//            topographies.put( splits[ 0 ].trim(), splits[ 3 ].trim() );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.out.println( ioE.getMessage() );
      }


//      System.out.println( "Lines: " + i + " Records: " + histologies.size() );
//      System.out.println( "HISTOLOGIES: " );
//      histologies.values().stream().distinct().forEach( System.out::println );
//      System.out.println( "\nTOPOGRAPHIES: " );
//      topographies.values().stream().distinct().forEach( System.out::println );
//
//      final Function<String,String> mapSite = s -> s + " = " + TopoMorphValidator.getInstance().getSiteClass( s );
//
//      TopoMorphValidator.getInstance();
//      System.out.println( "\nSITES: " );
//      topographies.values().stream().map( s -> s.substring( 0, 3 ) ).distinct().map( mapSite ).forEach( System.out::println );

   }


}
