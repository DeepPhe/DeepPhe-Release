package org.healthnlp.deepphe.neo4j.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.healthnlp.deepphe.neo4j.embedded.TestServiceFactory;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.node.PatientSummaryAndStagesList;
import org.healthnlp.deepphe.neo4j.util.DataUtil;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.embedded.TestServiceFactory.PATIENT_ID_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * @author zhy19
 */
public class ReadFunctionsTest {

   private final String FAKE_PATIENT_ID = "fake_patient1";
   private final String[] FAKE_PATIENT_IDS = new String[] {"fake_patient1", "fake_patient2", "fake_patient3", "fake_patient4", "fake_patient5", "fake_patient6", "fake_patient7"};
   private static ReadFunctions _readFunctions;
   private static final Gson g = new Gson();

   @BeforeClass
   public static void init() {
      final GraphDatabaseService graphDb = TestServiceFactory.getInstance().getGraphDb();

      _readFunctions = new ReadFunctions();
      _readFunctions.graphDb = graphDb;
      //DriverConnection.getInstance().createDriver( "bolt://localhost:7687", "neo4j", "1234j" );
   }

   @Test
   public void getPatientSummary() {
//      try {
//         String patientSummaryJson = _readFunctions.getPatientSummary(FAKE_PATIENT_ID);
//         PatientSummary patientSummary = g.fromJson(patientSummaryJson, PatientSummary.class);
//         assertEquals(FAKE_PATIENT_ID, patientSummary.getId());
//      } catch (Exception e) {
//         fail("Test failed: " + e.getMessage());
//      }
   }

   @Test
   public void testGetTimelineData() {
     // Map<String, Object> result = _readFunctions.getTimelineData(FAKE_PATIENT_ID); //maybe?!
    //  System.out.println(result);
   }


   @Test
   public void testGetPatientData() {
      try {
         String patientJson = _readFunctions.getPatientData(FAKE_PATIENT_ID);
         Patient p = g.fromJson(patientJson, Patient.class);
         assertEquals(p.getId(), FAKE_PATIENT_ID);
      } catch (Exception e) {
         fail("Test failed: " + e.getMessage());
      }
   }
   @Test
   public void testGetCohortData() {
      try {
         boolean includeStages = true;
         String patientSummaryAndStagesListJson = _readFunctions.getNewPatientList(includeStages);
         PatientSummaryAndStagesList patientSummaryAndStagesList = g.fromJson(patientSummaryAndStagesListJson, PatientSummaryAndStagesList.class);
         //here are the old results
         assertEquals(true, patientSummaryAndStagesList.getPatientSummaryAndStages().size() > 0);
      } catch (Exception e) {
         fail("Test failed: " + e.getMessage());
      }
   }

   //jdl need some tumors to show up before I can test
   @Test
   public void testGetBiomarkers() {
      List<String> biomarkers = new ArrayList<>();
      for (String id : FAKE_PATIENT_IDS)
         biomarkers.add(id);
     String result = _readFunctions.getBiomarkers(biomarkers);
      System.out.println(result);

   }

   @Test
   public void checkPatients() {
      System.out.println( "Checking Patients" );
      try ( Transaction tx = _readFunctions.graphDb.beginTx() ) {
         final Node allPatientsNode = SearchUtil.getClassNode( _readFunctions.graphDb, PATIENT_URI );
         if ( allPatientsNode == null ) {
            System.out.println( "No class node for uri " + PATIENT_URI );
            tx.success();
         }
         System.out.println( SearchUtil.getInRelatedNodes( _readFunctions.graphDb, allPatientsNode, INSTANCE_OF_RELATION )
                                       .stream()
                                       .map( n -> n.getProperty( NAME_KEY ) )
                                       .map( DataUtil::objectToString )
                                       .collect( Collectors.joining( "\n" ) ) );
         tx.success();
      } catch ( TransactionFailureException txE ) {
         System.out.println( txE.getMessage() );
      }
   }

   //This method is currently eliminated from the ReadFunctions class.
//
//   @Test
//   public void getDocumentsTest() {
//      System.out.println("getDocuments():");
//      final Gson gson = new GsonBuilder().create();
//      final List<String> docIds = Arrays.asList( NOTE_ID_1, NOTE_ID_2 );
//      final List<String> notes = _readFunctions.getDocuments( docIds );
//      notes.stream()
//           .map( gson::toJson )
////           .collect( Collectors.joining( "," ) )
//      .forEach( System.out::println );
//   }

   @Test
   public void getPatientDataTest() {
      System.out.println("getPatientData():");
      final Gson gson = new GsonBuilder().create();
      final String patient = _readFunctions.getPatientData( PATIENT_ID_1 );
      System.out.println( gson.toJson( patient ) );
   }


}