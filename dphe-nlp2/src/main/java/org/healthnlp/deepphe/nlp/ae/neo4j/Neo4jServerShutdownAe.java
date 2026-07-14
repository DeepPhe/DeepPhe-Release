package org.healthnlp.deepphe.nlp.ae.neo4j;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.driver.DriverConnection;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/15/2020
 */
@PipeBitInfo(
      name = "Neo4jServerShutdownAe",
      description = "Shuts down an external neo4j Server.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class Neo4jServerShutdownAe extends JCasAnnotator_ImplBase {

   public static final String PARAMETER_NEO4J_START = "StartNeo4j";
   public static final String PARAMETER_NEO4J_STOP = "StopNeo4j";
   @ConfigurationParameter(
         name = PARAMETER_NEO4J_START,
         description = "Start the neo4j server at the given location.",
         mandatory = false
   )
   private String _startNeo4j;

   @ConfigurationParameter(
         name = PARAMETER_NEO4J_STOP,
         description = "Stop the neo4j server at the location indicated by " + PARAMETER_NEO4J_START + ".  Yes/No",
         mandatory = false
   )
   private String _stopNeo4j;


   static private final Logger LOGGER = Logger.getLogger( "Neo4jServerShutdownAe" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      // Does nothing.
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      if ( _startNeo4j != null && !_startNeo4j.isEmpty()
           && _stopNeo4j != null && _stopNeo4j.equalsIgnoreCase( "yes" ) ) {
         stopNeo4j();
      }
   }


   private void stopNeo4j() throws AnalysisEngineProcessException {
      LOGGER.info( "Shutting down external Neo4j Service ..." );
      final Driver driver = DriverConnection.getInstance().getDriver();
      if ( driver == null ) {
         LOGGER.info( "Empty Driver.  Stopping Neo4j will be skipped." );
         return;
      }
      try ( Session session = driver.session() ) {
         try ( Transaction tx = session.beginTransaction() ) {
            tx.run( "CALL deepphe.shutdownServer()" );
            tx.commit();
         }
//      } catch ( ServiceUnavailableException suE ) {
         // This is probably fine as the server may shutdown before sending a return.  Skip.
      } catch ( Exception e ) {
         LOGGER.error( e.getMessage() );
         throw new AnalysisEngineProcessException( e );
      }
   }


}
