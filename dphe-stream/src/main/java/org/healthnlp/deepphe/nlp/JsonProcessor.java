package org.healthnlp.deepphe.nlp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.nlp.pipeline.NoteTextRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
final public class JsonProcessor {

   static private final Logger LOGGER = Logger.getLogger( "JsonProcessor" );

   public static void main( final String... args ) {
      final String patientId = args[ 0 ];
      final String docId = args[ 1 ];
      String text = "";
      try {
         text = new String ( Files.readAllBytes( Paths.get( args[ 2 ] ) ) );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      // Process note text
      final String json = NoteTextRunner.getInstance().processText( patientId, docId, text );

      final Gson gson = new GsonBuilder().create();
      final Note note = gson.fromJson( json, Note.class );

      final Gson gson2 = new GsonBuilder().setPrettyPrinting().create();
      final String json2 = gson2.toJson( note );

      LOGGER.info( "\n" + json2 );
      NoteTextRunner.getInstance().close();
      System.exit( 0 );
   }

}
