package org.healthnlp.deepphe.util;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {1/19/2022}
 */
final public class DictionaryCollector {

   private DictionaryCollector() {}

   static private final String SOURCE_DICTIONARY
         = "org/apache/ctakes/dictionary/lookup/fast/ncit_plus_16ab/ncit_plus_16ab.script";
   static private final String TARGET_DIR = "C:/Spiffy/ontology/dphe_onto_source/custom_v6_xn/BadSynonyms/";

   static private final String WORD = "Disorder";
   // Done: Stage, Grade, Differentiated, Primary, Metastatic,
   // second, invasive, recurrent, anaplast, benign, adult, child/pedi/infant, - category
   // local, distal, regional, acute, chronic
   // Done: ==cancer ==tumor ==neoplasm  ==carcinoma ==sarcoma ==adenoma, ==adenocarcinoma, ==melanoma
   //  ==lymphoma,  ==carcinosarcoma ==fibrosarcoma, ==malignancy, ==epithelioma ==syndrome
   //  ==disease ==in situ, ==disorder


   // todo - tag a node as benign, malignant, metastatic, etc.?

   public static void main( String[] args ) {
      final String word = WORD.toLowerCase();
      try ( BufferedWriter synonymsWriter = new BufferedWriter(
            new FileWriter( TARGET_DIR + WORD + "Synonyms.bsv" ) );
            BufferedWriter conceptsWriter = new BufferedWriter(
                  new FileWriter( TARGET_DIR + WORD + "AllCui.bsv" ) )
      ) {
         final Map<String, Collection<String>> cuiSynonyms
               = parseBsvFile( SOURCE_DICTIONARY );
         // Contains
//         for ( Map.Entry<String, Collection<String>> entry : cuiSynonyms.entrySet() ) {
//            final Collection<String> synonyms = entry.getValue();
//            if ( synonyms.stream()
//                         .noneMatch( s -> s.contains( word ) ) ) {
//               continue;
//            }
//            if ( synonyms.stream()
//                         .allMatch( s -> s.contains( word ) ) ) {
//               for ( String synonym : synonyms ) {
//                  conceptsWriter.write( entry.getKey() + "|" + synonym + "\n" );
//               }
//            } else {
//               for ( String synonym : synonyms ) {
//                  if ( synonym.contains( word ) ) {
//                     synonymsWriter.write( entry.getKey() + "|" + synonym + "\n" );
//                  } else {
//                     synonymsWriter.write( "//" + entry.getKey() + "|" + synonym + "\n" );
//                  }
//               }
//            }
//         }
         // Equal
         for ( Map.Entry<String, Collection<String>> entry : cuiSynonyms.entrySet() ) {
            final Collection<String> synonyms = entry.getValue();
            if ( synonyms.stream()
                         .noneMatch( s -> s.equals( word ) ) ) {
               continue;
            }
            if ( synonyms.stream()
                         .allMatch( s -> s.equals( word ) ) ) {
               for ( String synonym : synonyms ) {
                  conceptsWriter.write( entry.getKey() + "|" + synonym + "\n" );
               }
            } else {
               for ( String synonym : synonyms ) {
                  if ( synonym.equals( word ) ) {
                     synonymsWriter.write( entry.getKey() + "|" + synonym + "\n" );
                  } else {
                     synonymsWriter.write( "//" + entry.getKey() + "|" + synonym + "\n" );
                  }
               }
            }
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
   }

   static private Map<String,Collection<String>> parseBsvFile( final String filePath ) throws IOException {
      if ( filePath == null || filePath.isEmpty() ) {
         throw new IOException( "No File Path to Regex BSV." );
      }
      final Map<String,Collection<String>> cuiSynonyms = new HashMap<>();
      try ( BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( filePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final Pair<String> cuiSynonym = parseBsvLine( line );
            if ( cuiSynonym.equals( EMPTY_BSV_LINE ) ) {
               line = reader.readLine();
               continue;
            }
            cuiSynonyms.computeIfAbsent( cuiSynonym.getValue1(), s -> new HashSet<>() )
                       .add( cuiSynonym.getValue2() );
            line = reader.readLine();
         }
      }
      return cuiSynonyms;
   }

   static private final String INSERT_COMMAND = "INSERT INTO CUI_TERMS VALUES(";
   static private final Pair<String>  EMPTY_BSV_LINE = new Pair<>( "", "" );
   static private Pair<String> parseBsvLine( final String line ) {
      if ( !line.startsWith( INSERT_COMMAND ) ) {
         // comment
         return EMPTY_BSV_LINE;
      }
      final String inserts = line.substring( INSERT_COMMAND.length(), line.length()-1 );
      if ( inserts.isEmpty() ) {
         return EMPTY_BSV_LINE;
      }
      final String[] splits = StringUtil.fastSplit( inserts, ',' );
      if ( splits.length < 5 ) {
         System.err.println( "bad line " + line );
         return EMPTY_BSV_LINE;
      }
      final String cui = splits[ 0 ];
      final String synonym = splits[ 3 ].substring( 1, splits[ 3 ].length()-1 );
      return new Pair<>( cui, synonym );
   }



}
