package org.healthnlp.deepphe.util.eval.old.eval;


import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.EvalSummarizer.PATIENT_ID;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
final class NeoplasmSummaryReader {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmSummaryReader" );

   static private final String SUMMARY_ID = "Summary_ID";

   private NeoplasmSummaryReader() {
   }

   /**
    * @param file -
    * @return map of patient id to summaries
    */
   static Map<String, Collection<NeoplasmSummary>> readSummaries( final File file,
                                                                  final Collection<String> requiredNames,
                                                                  final Collection<String> otherNames,
                                                                  final Map<String, Integer> indices ) {
      final Map<String, Collection<NeoplasmSummary>> summaryMap = new HashMap<>();
      final int patientIndex = indices.get( PATIENT_ID );
      final int summaryIndex = indices.get( SUMMARY_ID );
      // id number is a kludge for gold annotations without unique id.
      int id = 1;
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         // skip header
         reader.readLine();
         String line = reader.readLine();
         while ( line != null ) {
            if ( line.startsWith( "//" ) || line.startsWith( "#" ) ) {
               line = reader.readLine();
               continue;
            }
            final List<String> values = Arrays.asList( StringUtil.fastSplit( line, '|' ) );
            if ( patientIndex >= values.size() || summaryIndex >= values.size() ) {
               LOGGER.error(
                     file.getPath() + " is missing either " + PATIENT_ID + " or " + SUMMARY_ID + " on " + line );
               System.exit( -1 );
            }
            final String patientId = values.get( patientIndex );
            final String summaryId = values.get( summaryIndex ) + "_" + id;
            id++;

            final Map<String, String> required = new HashMap<>( requiredNames.size() );
            for ( String name : requiredNames ) {
               final Integer index = indices.get( name );
               if ( index == null || index >= values.size() ) {
                  required.put( name, "" );
               } else {
                  required.put( name, values.get( index ) );
               }
            }

            final Map<String, String> other = new HashMap<>( otherNames.size() );
            for ( String name : otherNames ) {
               final Integer index = indices.get( name );
               if ( index == null || index >= values.size() ) {
                  other.put( name, "" );
               } else {
                  other.put( name, values.get( index ) );
               }
            }

            final NeoplasmSummary summary = new NeoplasmSummary( summaryId, required, other );
            summaryMap.computeIfAbsent( patientId, p -> new ArrayList<>() ).add( summary );

            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      return summaryMap;
   }

   static List<String> readHeader( final File file ) {
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         final String header = reader.readLine();
         return Arrays.asList( StringUtil.fastSplit( header, '|' ) );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      return Collections.emptyList();
   }

   static Collection<String> getRequiredNames( final List<String> names ) {
      return names.stream()
                  .filter( p -> p.startsWith( "*" ) )
                  .map( p -> p.substring( 1 ) )
                  .collect( Collectors.toList() );
   }

   static Collection<String> getScoringNames( final List<String> names ) {
      return names.stream()
                  .filter( p -> !p.startsWith( "*" ) )
                  .filter( p -> !p.startsWith( "-" ) )
                  .collect( Collectors.toList() );
   }

   static Map<String, Integer> mapNameIndices( final List<String> names ) {
      final Map<String, Integer> map = new HashMap<>( names.size() );
      int index = 0;
      for ( String propertyName : names ) {
         String name = propertyName;
         if ( propertyName.startsWith( "*" ) || propertyName.startsWith( "-" ) ) {
            name = propertyName.substring( 1 );
         }
         map.put( name, index );
         index++;
      }
      return map;
   }


}
