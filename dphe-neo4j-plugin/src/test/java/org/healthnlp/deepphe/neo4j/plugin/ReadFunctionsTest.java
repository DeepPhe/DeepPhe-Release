package org.healthnlp.deepphe.neo4j.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.healthnlp.deepphe.neo4j.embedded.TestServiceFactory;
import org.healthnlp.deepphe.neo4j.util.DataUtil;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.embedded.TestServiceFactory.*;

/**
 *
 * @author zhy19
 */
public class ReadFunctionsTest {

   private static ReadFunctions _readFunctions;

   @BeforeClass
   public static void init() {
      final GraphDatabaseService graphDb = TestServiceFactory.getInstance().getGraphDb();

      _readFunctions = new ReadFunctions();
      _readFunctions.graphDb = graphDb;
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

   @Test
   public void getDocumentsTest() {
      System.out.println("getDocuments():");
      final Gson gson = new GsonBuilder().create();
      final List<String> docIds = Arrays.asList( NOTE_ID_1, NOTE_ID_2 );
      final List<String> notes = _readFunctions.getDocuments( docIds );
      notes.stream()
           .map( gson::toJson )
//           .collect( Collectors.joining( "," ) )
      .forEach( System.out::println );
   }

   @Test
   public void getPatientDataTest() {
      System.out.println("getPatientData():");
      final Gson gson = new GsonBuilder().create();
      final String patient = _readFunctions.getPatientData( PATIENT_ID_1 );
      System.out.println( gson.toJson( patient ) );
   }


}