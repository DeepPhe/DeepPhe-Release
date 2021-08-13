package org.healthnlp.deepphe;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.nlp.pipeline.DmsRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

final public class PatientSummarizer {

    static private final Logger LOGGER = Logger.getLogger( "PatientSummarizer" );

    public static void main( final String... args ) {
        LOGGER.info( "Initializing ..." );
        DmsRunner.getInstance();
        LOGGER.info( "Reading docs in " + args[1] + " for patient " + args[0]  );
        final String patientId = args[ 0 ];
        final String docDir = args[ 1 ];
        final File dir = new File( docDir );
        final File[] docs = dir.listFiles();
        for ( File doc : docs ) {
            final String docId = doc.getName();
            String text = "";
            try {
                text = new String(Files.readAllBytes(Paths.get(doc.getPath())));
            } catch (IOException ioE) {
                LOGGER.error("Processing Failed:\n" + ioE.getMessage());
                System.exit(-1);
            }
            LOGGER.info("Doc " + docId + " has size: " + text.length());
            // Process doc text
            final String json = DmsRunner.getInstance().summarizeAndStoreDoc( patientId, docId, text );
            LOGGER.info( "Doc " + docId + " JSON:\n" + json );
        }
        LOGGER.info( "Summarizing Patient " + args[ 0 ] );
        final String json = DmsRunner.getInstance().summarizePatient( patientId );
        LOGGER.info( "Patient " + patientId + " JSON:\n" + json );

        DmsRunner.getInstance().close();
        System.exit( 0 );
    }

}
