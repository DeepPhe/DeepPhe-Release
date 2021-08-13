package org.healthnlp.deepphe;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.nlp.pipeline.DmsRunner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static junit.framework.Assert.assertEquals;

public class DocSummarizerTest {

    static private final Logger LOGGER = Logger.getLogger( "DocSummarizerTest" );


    private PatientSummary docTest(String docId, String pathToFile, Integer expectedNeoplasms) {
        LOGGER.debug( "Getting DmsRunner instance." );
        DmsRunner.getInstance();
        LOGGER.debug( "Reading doc " + docId + " from " + pathToFile );

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(pathToFile).getFile());

        String text = "";
        try {
            text = new String ( Files.readAllBytes( file.toPath() ) );
        } catch ( IOException ioE ) {
            LOGGER.error( "Processing Failed:\n" + ioE.getMessage() );
            System.exit( -1 );
        }
        // Process doc text
        final String patientSummaryJson = DmsRunner.getInstance().summarizeDoc( docId, text );
        LOGGER.debug( "Patient Summary JSON: " + patientSummaryJson);

        LOGGER.debug( "Deserializing Patient Summary");


        final PatientSummary patientSummary = (new Gson()).fromJson(patientSummaryJson, PatientSummary.class);

        assertEquals("PatientID and DocID are expected to be equal.", patientSummary.getId(), docId);

        assertEquals("Document " + docId+" is expected to have one neoplasm.", (int) expectedNeoplasms, (int) patientSummary.getNeoplasms().size());
        LOGGER.debug("Document" + docId + " has " + patientSummary.getNeoplasms().size() + " neoplasms.");
        DmsRunner.getInstance().close();

        return patientSummary;
    }

    @Test
    public void Doc1Test() {
        PatientSummary patientSummary = docTest("1", "patientX_doc1_RAD.txt", 1);
        assertEquals("Document 1 is expected to have one neoplasm.", 1, patientSummary.getNeoplasms().size());
    }

    @Test
    public void Doc2Test() {
        PatientSummary patientSummary = docTest("2", "patientX_doc2_SP.txt", 1);

    }

    @Test
    public void Doc3Test() {
        PatientSummary patientSummary = docTest("3", "patientX_doc3_NOTE.txt", 1);

    }



}
