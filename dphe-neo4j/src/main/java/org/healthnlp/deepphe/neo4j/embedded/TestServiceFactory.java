package org.healthnlp.deepphe.neo4j.embedded;


import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.writer.NodeWriter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.logging.BufferingLog;
import org.neo4j.logging.Log;

import java.io.File;
import java.util.Collections;
import java.util.Date;

/**
 * TODO Move this to a parallel source tree under "test".  I must have really rushed this.
 * Performs Setup for Neo4j Testing.  Adds two patients and a note to a Neo4j database.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/27/2020
 */
public enum TestServiceFactory {
   INSTANCE;

   static public TestServiceFactory getInstance() {
      return INSTANCE;
   }


   static public final String PATIENT_ID_1 = "fake_patient1";
   static public final String PATIENT_ID_2 = "fake_patient2";

   static public final String NOTE_ID_1 = PATIENT_ID_1 + "_doc1_RAD";
   static public final String NOTE_ID_2 = PATIENT_ID_2 + "_doc1_NOTE";

  // static private final String LOCAL_GRAPH_DB = "resources/graph/neo4j/ontology.db";
   static private final String LOCAL_GRAPH_DB = "/Users/johnlevander/dev/neo4j-community-3.5.12/data/databases/ontology.db";


   final private GraphDatabaseService _graphDb;

   TestServiceFactory() {
      System.out.println( "Neo4j DB File: " + LOCAL_GRAPH_DB );
      _graphDb = createService( LOCAL_GRAPH_DB );
      final Log log = new BufferingLog();

      NodeWriter.getInstance().initializeDphe( _graphDb, log );

//      addPatient( _graphDb, log, createPatient( PATIENT_ID_1 ) );
//      addPatient( _graphDb, log, createPatient( PATIENT_ID_2 ) );
//
//      addNote( _graphDb, log, PATIENT_ID_1, createNote( NOTE_ID_1, "Some fake RAD text.", "RAD" ) );
//      addNote( _graphDb, log, PATIENT_ID_2, createNote( NOTE_ID_2, "Some fake NOTE text.", "NOTE" ) );
   }

   static public String getGraphDir() {
      return LOCAL_GRAPH_DB;
   }

   public GraphDatabaseService getGraphDb() {
      return _graphDb;
   }

   static public GraphDatabaseService createService( final String graphDbPath ) {
      final File graphDbFile = new File( graphDbPath );
      if ( !graphDbFile.isDirectory() ) {
         System.out.println( "No Database exists at: " + graphDbPath );
         System.exit( -1 );
      }


      final GraphDatabaseService graphDb = new GraphDatabaseFactory().
            newEmbeddedDatabaseBuilder( graphDbFile )
            .setConfig( GraphDatabaseSettings.read_only, "true" ).
             newGraphDatabase();
      if ( !graphDb.isAvailable( 500 ) ) {
         System.out.println( "Could not initialize neo4j connection for: " + graphDbPath );
         System.exit( -1 );
      }
      ShutdownHook.registerShutdownHook( graphDb, graphDbPath );
      return graphDb;
   }


//   TODO The following do not test the DriverFactory.
//    They test the writer and should be in a different test class that is in the .writer package.
//    A service can be created with @BeforeClass and failure reported there.
   static private Patient createPatient( final String id ) {
      final Patient patient = new Patient();
      patient.setId( id );
      patient.setNotes( Collections.emptyList() );
      //patient.setDiagnoses( Collections.emptyList());
      return patient;
   }

   static private void addPatient( final GraphDatabaseService graphDb, final Log log, final Patient patient ) {
      NodeWriter.getInstance().addPatientInfo( graphDb, log, patient );
   }


   static private Note createNote( final String id,
                                   final String text,
                                   final String type ) {
      final Note note = new Note();
      note.setId( id );
      note.setText( text );
      note.setType( type );
      note.setEpisode( "" );
      note.setDate( new Date().toString() );
      note.setSections( Collections.emptyList() );
      note.setMentions( Collections.emptyList() );
      note.setRelations( Collections.emptyList() );
      note.setCorefs( Collections.emptyList() );
      return note;
   }

   static private void addNote( final GraphDatabaseService graphDb,
                                final Log log,
                                final String patientId,
                                final Note note ) {
      NodeWriter.getInstance().addNoteInfo( graphDb, log, patientId, note );
   }


}
