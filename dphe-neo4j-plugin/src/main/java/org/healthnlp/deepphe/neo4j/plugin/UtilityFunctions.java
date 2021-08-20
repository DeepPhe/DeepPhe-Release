package org.healthnlp.deepphe.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author SPF , chip-nlp
 * @since {8/20/2021}
 */
final public class UtilityFunctions {

   // This field declares that we need a GraphDatabaseService
   // as context when any procedure in this class is invoked
   @Context
   public GraphDatabaseService graphDb;

   // This gives us a log instance that outputs messages to the
   // standard log, normally found under `data/log/console.log`
   @Context
   public Log log;


   @Procedure( name="deepphe.shutdownServer", mode= Mode.DBMS )
   @Description( "Shuts down the Neo4j Service." )
   public Stream<ProcedureString> shutdownServer() {
      log.info( "Shutting down neo4j in 5 seconds ..." );
      final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
      executorService.schedule( new Stopper(), 5, TimeUnit.SECONDS );
      final ProcedureString joke = new ProcedureString( "Shutting down neo4j in 5 seconds ..." );
      return Stream.of( joke );
   }

   private final class Stopper implements Runnable {
      public void run() {
         log.info( "Shutting down neo4j ..." );
         try {
            graphDb.shutdown();
         } catch ( Exception e ) {
            log.warn( "Could not shutdown service using graphDb.shutdown().  Forcing exit ..." );
         }
         log.info( "Exiting neo4j ..." );
         System.exit( 0 );
      }
   }


}
