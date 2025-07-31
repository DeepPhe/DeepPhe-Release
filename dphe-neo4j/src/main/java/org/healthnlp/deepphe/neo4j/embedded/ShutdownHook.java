package org.healthnlp.deepphe.neo4j.embedded;


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.impl.transaction.log.files.LogFiles;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.kernel.lifecycle.LifecycleException;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * A shutdown hook for the Neo4j instance so that it
 * shuts down nicely when the VM exits (even if you "Ctrl-C" the
 * running application).
 *
 * There are several levels of protection to protect spawning multiple shutdown hooks for a graphDb,
 * plus some protection against shutting down a graphDb more than once.
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/3/2020
 */
final public class ShutdownHook extends Thread {

   static private final String UNKNOWN_DIR = "UNKNOWN_DIR";

   static private final Collection<Integer> GRAPH_HASHES = new HashSet<>();

   static public void registerShutdownHook( final GraphDatabaseService graphDb ) {
      registerShutdownHook( graphDb, UNKNOWN_DIR );
   }

   static public void registerShutdownHook( final GraphDatabaseService graphDb, final String directory ) {
      synchronized ( UNKNOWN_DIR ) {
         if ( GRAPH_HASHES.contains( graphDb.hashCode() ) ) {
            return;
         }
         GRAPH_HASHES.add( graphDb.hashCode() );
         // Registers a shutdown hook for the Neo4j instance so that it
         // shuts down nicely when the VM exits (even if you "Ctrl-C" the
         // running application).
         Runtime.getRuntime().addShutdownHook( new ShutdownHook( graphDb, directory ) );
         System.out.println( "Registered Shutdown Hook for " + graphDb.hashCode() + " in " + directory );
      }
   }

   /**
    * Only use this for testing!!!
    * @param graphDb -
    * @param directory -
    * @return -
    */
   static public ShutdownHook createTestHook( final GraphDatabaseService graphDb, final String directory ) {
      return new ShutdownHook( graphDb, directory );
   }


   final GraphDatabaseService _graphDb;
   final String _directory;

   private ShutdownHook( final GraphDatabaseService graphDb ) {
      this( graphDb, UNKNOWN_DIR );
   }

   private ShutdownHook( final GraphDatabaseService graphDb, final String directory ) {
      _graphDb = graphDb;
      _directory = directory;
   }

   public void run() {
      // class-level lock, only one shutdown hook can execute this at a time.
      synchronized ( UNKNOWN_DIR ) {
         try {
            if ( !_graphDb.isAvailable( 10000 ) ) {
               // Wait a maximum of 10 seconds and see if the graph is available.
               // If not, assume that it has already been shut down.
               System.out.println( "GraphDb " + _graphDb.hashCode() + " is not available for shutdown.");
               return;
            }
            final LogFiles logFiles = ((GraphDatabaseAPI)_graphDb).getDependencyResolver()
                                                                  .resolveDependency( LogFiles.class );
            final File[] txLogFiles = logFiles.logFiles();
            _graphDb.shutdown();
            String _parentDir = UNKNOWN_DIR;
            // Delete the transaction logs.  This should actually help restarts.
            for ( File txLogFile : txLogFiles ) {
               _parentDir = txLogFile.getParent();
               FileUtils.deleteFile( txLogFile );
            }
            // Delete the counts logs.  This is necessary when different hosts restart a graph.
            String directory = _directory.equals( UNKNOWN_DIR ) ? _parentDir : _directory;
            if ( directory.equals( UNKNOWN_DIR ) ) {
               return;
            }
            final File graphDir = new File( directory );
            if ( graphDir.isDirectory() ) {
               final File[] files = graphDir.listFiles();
               if ( files != null ) {
                  Arrays.stream( files )
                        .filter( f -> f.getName().startsWith( "neostore.counts.db" ) )
                        .forEach( FileUtils::deleteFile );
               }
            }
            System.out.println( "GraphDb " + _graphDb.hashCode() + " has been shutdown.");
         } catch ( LifecycleException | RotationTimeoutException |
               ClassCastException multE ) {
            System.err.println( multE.getMessage() );
            multE.printStackTrace();
            // ignore
         }
      }
   }


}
