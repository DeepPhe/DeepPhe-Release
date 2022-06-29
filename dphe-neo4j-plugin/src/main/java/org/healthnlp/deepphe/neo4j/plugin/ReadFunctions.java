package org.healthnlp.deepphe.neo4j.plugin;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.neo4j.reader.NodeReader;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/10/2020
 */
public class ReadFunctions {

   // This field declares that we need a GraphDatabaseService
   // as context when any procedure in this class is invoked
   @Context
   public GraphDatabaseService graphDb;

   // This gives us a log instance that outputs messages to the
   // standard log, normally found under `data/log/console.log`
   @Context
   public Log log;


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            PATIENT DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @UserFunction( "deepphe.getPatientData" )
   @Description( "Fetches everything under a Patient node." )
   public String getPatientData( @Name( "patientId" ) String patientId ) {
      final Patient patient = NodeReader.getInstance().getPatient( graphDb, log, patientId );
      if ( patient == null ) {
         return "";
      }
      final Gson gson = new GsonBuilder().create();
      return gson.toJson( patient );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NOTE DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @UserFunction( "deepphe.getNoteData" )
   @Description( "Fetches everything under a Note node." )
   public String getNoteData( @Name( "noteId" ) String noteId ) {
      final Note note = NodeReader.getInstance().getNote( graphDb, log, noteId );
      if ( note == null ) {
         return "";
      }
      final Gson gson = new GsonBuilder().create();
      return gson.toJson( note );
   }


   @UserFunction(name = "deepphe.getDocuments")
   @Description("Returns a list of documents for a given list of document Ids.")
   public List<String> getDocuments( @Name("documentIds") List<String> documentIds) {
      final Gson gson = new GsonBuilder().create();
      return documentIds.stream()
                        .map( id -> NodeReader.getInstance().getNote( graphDb, log, id ) )
                        .map( gson::toJson )
                        .collect( Collectors.toList() );
   }


   @UserFunction(name = "deepphe.getPatientDocuments")
   @Description("Returns all the documents for a given patient ID.")
   public List<String> getPatientDocuments(@Name("patientId") String patientId) {
      final Gson gson = new GsonBuilder().create();
      return NodeReader.getInstance().getPatient( graphDb, log, patientId )
                       .getNotes()
                       .stream()
                       .map( gson::toJson )
                       .collect( Collectors.toList() );
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SECTION DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////



   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MENTION RELATION AND COREF DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////



}
