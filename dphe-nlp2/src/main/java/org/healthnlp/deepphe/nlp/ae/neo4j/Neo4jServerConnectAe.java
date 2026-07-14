package org.healthnlp.deepphe.nlp.ae.neo4j;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.neo4j.driver.DriverConnection;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/15/2020
 */
@PipeBitInfo(
      name = "Neo4jServerConnectAe",
      description = "Connects to an external neo4j Server.  Can also start neo4j Server.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class Neo4jServerConnectAe extends JCasAnnotator_ImplBase {

   public static final String PARAMETER_NEO4J_START = "StartNeo4j";
   public static final String PARAMETER_NEO4J_URI = "Neo4jUri";
   public static final String PARAMETER_NEO4J_USER = "Neo4jUser";
   public static final String PARAMETER_NEO4J_PASS = "Neo4jPass";
   @ConfigurationParameter(
         name = PARAMETER_NEO4J_START,
         description = "Start the neo4j server at the given location.",
         mandatory = false
   )
   private String _startNeo4j;

   @ConfigurationParameter(
         name = PARAMETER_NEO4J_URI,
         description = "The URI to the neo4j server.",
         mandatory = false
   )
   private String _neo4jUri;

   @ConfigurationParameter(
         name = PARAMETER_NEO4J_USER,
         description = "The User name for the neo4j server.",
         mandatory = false
   )
   private String _neo4jUser;

   @ConfigurationParameter(
         name = PARAMETER_NEO4J_PASS,
         description = "The User password for the neo4j server.",
         mandatory = false
   )
   private String _neo4jPass;


   static private final Logger LOGGER = Logger.getLogger( "Neo4jServerConnectAe" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );
      if ( _startNeo4j != null && !_startNeo4j.isEmpty() ) {
         LOGGER.info( "Initializing Neo4j Driver ..." );
         startNeo4j();
      }
      if ( DriverConnection.getInstance().getDriver() != null ) {
         return;
      }
      try {
         if ( _neo4jUri == null || _neo4jUri.isEmpty() ) {
            _neo4jUri = "Local";
         }
         if ( _neo4jUser == null || _neo4jUser.isEmpty() ) {
            _neo4jUser = "Me";
         }
         if ( _neo4jPass == null || _neo4jPass.isEmpty() ) {
            _neo4jPass = "None";
         }
         LOGGER.info( "Neo4j Driver Connecting to " + _neo4jUri + " as " + _neo4jUser );
         DriverConnection.getInstance().createDriver( _neo4jUri, _neo4jUser, _neo4jPass );
      } catch ( Exception e ) {
         throw new ResourceInitializationException( e );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      // Does nothing.
   }


   private void startNeo4j() throws ResourceInitializationException {
      //         http://localhost:7474
      //         user = neo4j  pass = neo4j
      //         bolt://127.0.0.1:7687
      final File neo4jHome = new File( _startNeo4j );
      if ( !neo4jHome.isDirectory() ) {
         throw new ResourceInitializationException(
               new FileNotFoundException( "Could not find Neo4j Home directory " + _startNeo4j ) );
      }
      final File logFile = new File( neo4jHome, "DeepPheClient.log" );
      final ProcessBuilder builder = new ProcessBuilder();
      builder.directory( neo4jHome )
             .redirectOutput( logFile)
             .redirectError( logFile );
      final boolean isWindows = System.getProperty( "os.name" )
                                      .toLowerCase()
                                      .startsWith( "windows" );
      if ( isWindows ) {
         builder.command( "cmd.exe", "/c", "bin\\neo4j console" );
      } else {
         builder.command( "sh", "-c", "bin/neo4j console" );
      }
      try {
         logFile.delete();
         logFile.createNewFile();
         builder.start();
         // Sleep for 30 seconds to allow the neo4j service to become available
         LOGGER.info( "Waiting 30 seconds for Neo4j service at " + _startNeo4j + " to initialize ..." );
         try {
            TimeUnit.SECONDS.sleep( 30 );
         } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
   }


}
