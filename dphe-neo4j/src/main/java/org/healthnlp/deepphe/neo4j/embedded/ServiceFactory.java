package org.healthnlp.deepphe.neo4j.embedded;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/23/2020
 */
final public class ServiceFactory {

   static private final Logger LOGGER = Logger.getLogger( "ServiceFactory" );

   private ServiceFactory() {}

   static public GraphDatabaseService createService( final String graphDbPath ) {
      final File graphDbFile = new File( graphDbPath );
      if ( !graphDbFile.isDirectory() ) {
         LOGGER.error( "No Database exists at: " + graphDbPath );
         System.exit( -1 );
      }

      GraphDatabaseService graphDb = null;
      try {
         graphDb = createService( graphDbFile );
         LOGGER.info( "Graph Connected." );
      } catch ( RuntimeException rtE ) {
         LOGGER.warn( "Could not immediately initialize neo4j connection for: " + graphDbPath );
         LOGGER.warn( rtE.getMessage() );
         LOGGER.warn( "Deleting old logs for a second attempt ..."  );
         // Errors in previous runs can lead to "No CheckPoint" errors.  Delete the logs before starting.
         final Predicate<File> isLogFile
               = f -> f.getName().startsWith( "neostore.counts.db" )
                      || f.getName().startsWith( "neostore.transaction.db" );
         if ( graphDbFile.isDirectory() ) {
            final File[] files = graphDbFile.listFiles();
            if ( files != null ) {
               Arrays.stream( files )
                     .filter( isLogFile )
                     .forEach( File::delete );
            }
         }
      }
      if ( graphDb == null ) {
         try {
            graphDb = createService( graphDbFile );
            LOGGER.info( "Graph Connected." );
         } catch ( RuntimeException rtE ) {
            LOGGER.error( "Could not initialize neo4j connection for: " + graphDbPath );
            LOGGER.error( rtE.getMessage() );
            System.exit( -1 );
         }
      }
      if ( !graphDb.isAvailable( 500 ) ) {
         LOGGER.error( "Graph is not available for: " + graphDbPath );
         System.exit( -1 );
      }
      ShutdownHook.registerShutdownHook( graphDb, graphDbPath );
      return graphDb;
   }


   static private GraphDatabaseService createService( final File graphDbFile ) throws RuntimeException {
      return new GraphDatabaseFactory()
            .newEmbeddedDatabaseBuilder( graphDbFile )
            // setting access to read only is persisting across use.  This breaks up other modules that need to write.
//            .setConfig( GraphDatabaseSettings.read_only, "true" )
//            .setConfig( GraphDatabaseSettings.keep_logical_logs, "1M size" )
            .setConfig( GraphDatabaseSettings.keep_logical_logs, "false" )
            .setConfig( GraphDatabaseSettings.logical_log_rotation_threshold, "1M" )
            .setConfig( GraphDatabaseSettings.check_point_interval_tx, "100" )
            .setConfig( GraphDatabaseSettings.check_point_interval_time, "1s" )
            // fail_on_corrupted_log_files appears to not be used.  Oh Well.
            .setConfig( GraphDatabaseSettings.fail_on_corrupted_log_files, "false" )
            .newGraphDatabase();
   }

}
