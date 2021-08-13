package org.healthnlp.deepphe.neo4j.plugin;

import com.google.gson.Gson;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.neo4j.util.JsonUtil;
import org.healthnlp.deepphe.neo4j.writer.NodeWriter;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/4/2020
 */
final public class WriteFunctions {


   // This field declares that we need a GraphDatabaseService
   // as context when any procedure in this class is invoked
   @Context
   public GraphDatabaseService graphDb;

   // This gives us a log instance that outputs messages to the
   // standard log, normally found under `data/log/console.log`
   @Context
   public Log log;


   static private final Map<String, RelationshipType> _relationshipTypes = new HashMap<>();


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            COHORT INITIALIZATION
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @Procedure( name="deepphe.initializeDphe", mode=Mode.WRITE )
   @Description( "Creates root nodes for Patient, Document, Stage." )
   public Stream<ProcedureString> initializeDphe() {
      NodeWriter.getInstance().initializeDphe( graphDb, log );
      final ProcedureString joke = new ProcedureString( "DeepPhe Graph Initialized." );
      return Stream.of( joke );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            PATIENT DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @Procedure( name="deepphe.addPatientInfo", mode=Mode.WRITE )
   @Description( "Creates or appends to a Patient node." )
   public Stream<ProcedureString> addPatientInfo( @Name( "patientJson" ) String patientJson ) {
      final String realJson = JsonUtil.unpackFromNeo4j( patientJson );
      final Gson gson = new Gson();
//      final Patient patient = gson.fromJson( patientJson, Patient.class );
      final Patient patient = gson.fromJson( realJson, Patient.class );
      NodeWriter.getInstance().addPatientInfo( graphDb, log, patient );
      final ProcedureString joke
            = new ProcedureString( "Patient " + patient.getId() + " added to DeepPhe graph." );
      return Stream.of( joke );
   }


   @Procedure( name="deepphe.addPatientSummary", mode=Mode.WRITE )
   @Description( "Creates or appends to a Patient node and cancer summary." )
   public Stream<ProcedureString> addPatientSummary( @Name( "patientSummaryJson" ) String patientSummaryJson ) {
      final String realJson = JsonUtil.unpackFromNeo4j( patientSummaryJson );
      final Gson gson = new Gson();
      final PatientSummary patientSummary = gson.fromJson( realJson, PatientSummary.class );
      final Patient patient = patientSummary.getPatient();
      System.out.println("writing " + patient.getId());
       NodeWriter.getInstance().addPatientInfo( graphDb, log, patient );
      System.out.println("wrote " + patient.getId());
      System.out.println("getting neoplasms for " + patient.getId());
      final List<NeoplasmSummary> neoplasms = patientSummary.getNeoplasms();
      System.out.println("got neoplasms for " + patient.getId());

      for ( NeoplasmSummary neoplasm : neoplasms ) {

         //jdl: talking with sean, we probably want to always just call addCancerInfo
         //if (neoplasm instanceof CancerSummary) {
         System.out.println("adding cancer info for " + patient.getId());
         NodeWriter.getInstance().addCancerInfo(graphDb, log, patient.getId(), neoplasm);
         //} else {
         //   NodeWriter.getInstance().addNeoplasmInfo(graphDb, log, patient.getId(), neoplasm);
        // }
      }
      final ProcedureString joke
            = new ProcedureString( "Patient Summary " + patientSummary.getId() + " added to DeepPhe graph." );
      return Stream.of( joke );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            CANCER DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @Procedure( name="deepphe.addCancerInfo", mode=Mode.WRITE )
   @Description( "Appends Cancer Summary information to a Patient node." )
   public Stream<ProcedureString> addCancerInfo( @Name( "patientId" ) String patientId,
                                                 @Name( "cancerJson" ) String cancerJson ) {
      final String realJson = JsonUtil.unpackFromNeo4j( cancerJson );
      final Gson gson = new Gson();
//      final Patient patient = gson.fromJson( patientJson, Patient.class );
      final NeoplasmSummary cancer = gson.fromJson( realJson, NeoplasmSummary.class );
      NodeWriter.getInstance().addCancerInfo( graphDb, log, patientId, cancer );
      final ProcedureString joke
            = new ProcedureString( "Cancer " + cancer.getId() + " added to DeepPhe graph." );
      return Stream.of( joke );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NOTE DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @Procedure( name="deepphe.addNoteInfo", mode=Mode.WRITE )
   @Description( "Creates or appends to a Note node." )
   public Stream<ProcedureString> addNoteInfo( @Name( "patientId" ) String patientId, @Name( "noteJson" ) String noteJson ) {
      final String realJson = JsonUtil.unpackFromNeo4j( noteJson );
      final Gson gson = new Gson();
//      final Note note = gson.fromJson( noteJson, Note.class );
      final Note note = gson.fromJson( realJson, Note.class );
      NodeWriter.getInstance().addNoteInfo( graphDb, log, patientId, note );
      final ProcedureString joke = new ProcedureString( "Note " + note.getId() + " added to DeepPhe graph." );
      return Stream.of( joke );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SECTION DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////

   // Handled through NodeWriter.addNoteInfo -> NodeWriter.addSectionInfo


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MENTION DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @Procedure( name="deepphe.addMentionInfo", mode=Mode.WRITE )
   @Description( "Creates or appends to a Mention node." )
   public Stream<ProcedureString> addMentionInfo( @Name( "noteId" ) String noteId, @Name( "mentionJson" ) String mentionJson ) {
      final String realJson = JsonUtil.unpackFromNeo4j( mentionJson );
      final Gson gson = new Gson();
//      final Mention mention = gson.fromJson( mentionJson, Mention.class );
      final Mention mention = gson.fromJson( realJson, Mention.class );
      NodeWriter.getInstance().addMentionInfo( graphDb, log, noteId, mention );
      final ProcedureString joke = new ProcedureString( "Mention " + mention.getId() + " added to DeepPhe graph." );
      return Stream.of( joke );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MENTION RELATION DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////

   // Handled through NodeWriter.addNoteInfo -> NodeWriter.addMentionRelation


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MENTION COREF DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////

   // Handled through NodeWriter.addNoteInfo -> NodeWriter.addMentionCoref


}
