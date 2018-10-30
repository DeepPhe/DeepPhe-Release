package org.apache.ctakes.neo4j;


import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.lifecycle.LifecycleException;

import java.io.File;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2017
 */
public enum Neo4jConnectionFactory {
   INSTANCE;

   public static Neo4jConnectionFactory getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "Neo4jConnectionFactory" );

   static private final String STATIC_GRAPH_DB = "resources/org/healthnlp/deepphe/graph/neo4j/test.db";
   static private final String OUTPUT_GRAPH_DB = "output_graph/deepphe.db";

   private GraphDatabaseService _graphDb;

   private void createOutputGraph( final String outputDbPath ) {
      final File outputGraph = new File( outputDbPath );
      final String path = outputGraph.getAbsolutePath();
      if ( !outputGraph.exists() ) {
         LOGGER.info( "Creating output Graph " + path );
         outputGraph.mkdirs();
      } else {
         LOGGER.info( "Replacing existing output Graph " + path );
         deleteDirectory( outputGraph );
      }
      final File defaultGraph = new File( getStaticGraphDb() );
      try {
         FileUtils.copyDirectory( defaultGraph, outputGraph, false );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage(), ioE );
         System.exit( 1 );
      }
   }

   static private boolean deleteDirectory( final File directory ) {
      final File[] files = directory.listFiles();
      if ( files == null ) {
         return true;
      }
      boolean ok = true;
      for ( File file : files ) {
         if ( file.isDirectory() ) {
            ok &= deleteDirectory( file );
         }
         ok &= file.delete();
      }
      return ok;
   }

   private String getStaticGraphDb() {
      return STATIC_GRAPH_DB;
   }

   public String getOutputGraphDb() {
      return OUTPUT_GRAPH_DB;
   }

   public GraphDatabaseService connectToGraph() {
      return connectToGraph( getOutputGraphDb() );
   }

   synchronized public GraphDatabaseService connectToGraph( final String graphDbPath ) {
      if ( _graphDb != null ) {
         return _graphDb;
      }
      createOutputGraph( graphDbPath );
      final File graphDbFile = new File( graphDbPath );
      if ( !graphDbFile.isDirectory() ) {
         LOGGER.error( "No Database exists at: " + graphDbPath );
         System.exit( -1 );
      }
      _graphDb = new GraphDatabaseFactory()
      .newEmbeddedDatabase( graphDbFile );
      if ( !_graphDb.isAvailable( 500 ) ) {
         LOGGER.error( "Could not initialize neo4j connection for: " + graphDbPath );
         System.exit( -1 );
      }
      registerShutdownHook( _graphDb );
      return _graphDb;
   }

   public GraphDatabaseService getGraph() {
      if ( _graphDb == null ) {
         return connectToGraph();
      }
      return _graphDb;
   }

   static private void registerShutdownHook( final GraphDatabaseService graphDb ) {
      // Registers a shutdown hook for the Neo4j instance so that it
      // shuts down nicely when the VM exits (even if you "Ctrl-C" the
      // running application).
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         try {
            graphDb.shutdown();
         } catch ( LifecycleException | RotationTimeoutException multE ) {
            // ignore
         }
      } ) );
   }


}
