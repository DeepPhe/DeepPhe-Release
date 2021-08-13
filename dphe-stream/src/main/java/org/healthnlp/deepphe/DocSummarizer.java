package org.healthnlp.deepphe;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.nlp.pipeline.DmsRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

final public class DocSummarizer {

    static private final Logger LOGGER = Logger.getLogger( "DocSummarizer" );

    public static void main( final String... args ) {
        LOGGER.info( "Initializing ..." );
        DmsRunner.getInstance();
        LOGGER.info( "Reading doc " + args[0] + " from " + args[1] );
        final String docId = args[ 0 ];
        String text = "";
        try {
            text = new String ( Files.readAllBytes( Paths.get( args[ 1 ] ) ) );
        } catch ( IOException ioE ) {
            LOGGER.error( "Processing Failed:\n" + ioE.getMessage() );
            System.exit( -1 );
        }
        // Process doc text
        final String json = DmsRunner.getInstance().summarizeDoc( docId, text );

        LOGGER.info( "Result JSON:\n" + json );
        DmsRunner.getInstance().close();
        System.exit( 0 );
    }

}
