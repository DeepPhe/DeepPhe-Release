package org.healthnlp.deepphe.neo4j.plugin;

import com.google.gson.Gson;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.neo4j.util.JsonUtil;
import org.healthnlp.deepphe.neo4j.writer.NodeWriter;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/4/2020
 */
public class WriteFunctions {


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



   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MENTION COREF DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////



}
