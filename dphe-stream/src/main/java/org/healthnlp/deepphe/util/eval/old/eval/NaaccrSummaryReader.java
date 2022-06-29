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
final class NaaccrSummaryReader {

   static private final Logger LOGGER = Logger.getLogger( "NaaccrSummaryReader" );

   private NaaccrSummaryReader() {
   }

   /**
    * @param file -
    * @return map of patient id to summaries
    */
   static Map<String, Collection<NeoplasmSummary>> readSummaries( final File file,
                                                                  final Collection<String> requiredNames,
                                                                  final Collection<String> otherNames,
                                                                  final Map<String, Integer> indices ) {
      return readSummaries( file, requiredNames, otherNames, indices, Collections.emptyList() );
   }

   /**
    * @param file -
    * @return map of patient id to summaries
    */
   static Map<String, Collection<NeoplasmSummary>> readSummaries( final File file,
                                                                  final Collection<String> requiredColumnNames,
                                                                  final Collection<String> otherColumnNames,
                                                                  final Map<String, Integer> columnIndices,
                                                                  final Collection<String> patientNames ) {
      final Map<String, Collection<NeoplasmSummary>> summaryMap = new HashMap<>();
//      final int patientIndex = indices.get( "patient ID" );
//      final int summaryIndex = indices.get( "histology: ICD-O code" );
//      final int patientIndex = indices.get( "record_id" );
      Integer patientIndex = columnIndices.get( PATIENT_ID );
      if ( patientIndex == null ) {
         patientIndex = columnIndices.get( "record_id" );
      }
// id number is a kludge for gold annotations without unique id.
      int id = 1;
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         // skip header
         reader.readLine();
         String line = reader.readLine();
         while ( line != null ) {
            if ( line.startsWith( "//" ) || line.startsWith( "#" ) || line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
//            final List<String> values = Arrays.asList( StringUtil.fastSplit( line, '|' ) );
            final List<String> values = Arrays.asList( line.split( "\\|" ) );
//            if ( patientIndex >= values.size() || summaryIndex >= values.size() ) {
            if ( patientIndex >= values.size()  ) {
               LOGGER.error(
                     file.getPath() + " is missing either patient ID or histology: ICD-O code on " + line );
               System.exit( -1 );
            }
            final String patientId = values.get( patientIndex ).trim();
            if ( patientNames.isEmpty() || !patientNames.contains( patientId ) ) {
               line = reader.readLine();
               continue;
            }
//            final String summaryId = values.get( summaryIndex ) + "_" + id;
            final String summaryId = values.get( patientIndex ) + "_" + id;
            id++;

            final Map<String, String> required = new HashMap<>( requiredColumnNames.size() );
            for ( String name : requiredColumnNames ) {
               final Integer index = columnIndices.get( name );
               if ( index == null || index >= values.size() ) {
                  // Kludge for gold sets that do not contain values for all attributes.  See MatchUtil.isSpecialMatch
                  required.put( name, "[]" );
               } else {
                  required.put( name, values.get( index ) );
               }
            }

            final Map<String, String> other = new HashMap<>( otherColumnNames.size() );
            for ( String name : otherColumnNames ) {
               final Integer index = columnIndices.get( name );
               if ( index == null || index >= values.size() ) {
                  // Kludge for gold sets that do not contain values for all attributes.  See MatchUtil.isSpecialMatch
                  other.put( name, "[]" );
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

   static List<String> readColumnHeader( final File file ) {
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         final String header = reader.readLine();
         return Arrays.asList( StringUtil.fastSplit( header, '|' ) );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      return Collections.emptyList();
   }

   static Collection<String> getRequiredColumnNames( final List<String> names ) {
      return names.stream()
                  .filter( p -> p.startsWith( "*" ) )
                  .map( p -> p.substring( 1 ) )
                  .collect( Collectors.toList() );
   }

   static Collection<String> getScoringColumnNames( final List<String> names ) {
      return names.stream()
                  .filter( p -> !p.startsWith( "*" ) )
                  .filter( p -> !p.startsWith( "-" ) )
                  .collect( Collectors.toList() );
   }

   static Map<String, Integer> mapColumnNameIndices( final List<String> names ) {
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

   static Collection<String> readPatientNames( final File goldFile, final int patientIndex ) {
      final Collection<String> patientNames = new HashSet<>();
//      final int patientIndex = indices.get( "patient ID" );
//      final int summaryIndex = indices.get( "histology: ICD-O code" );
//      final int patientIndex = indices.get( "record_id" );
// id number is a kludge for gold annotations without unique id.
      int id = 1;
      try ( BufferedReader reader = new BufferedReader( new FileReader( goldFile ) ) ) {
         // skip header
         reader.readLine();
         String line = reader.readLine();
         while ( line != null ) {
            if ( line.startsWith( "//" ) || line.startsWith( "#" ) || line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
//            final List<String> values = Arrays.asList( StringUtil.fastSplit( line, '|' ) );
            final List<String> values = Arrays.asList( line.split( "\\|" ) );
//            if ( patientIndex >= values.size() || summaryIndex >= values.size() ) {
            if ( patientIndex >= values.size()  ) {
               LOGGER.error(
                     goldFile.getPath() + " is missing either patient ID or histology: ICD-O code on " + line );
               System.exit( -1 );
            }
            final String patientId = values.get( patientIndex ).trim();
            patientNames.add( patientId.trim() );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      return patientNames;
   }


}
