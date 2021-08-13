package org.healthnlp.deepphe.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/16/2018
 */
@PipeBitInfo(
      name = "Neo4jEmbeddedConnectAe",
      description = "Connects to neo4j session on initialization.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class Neo4jEmbeddedConnectAe extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "Neo4jEmbeddedConnectAe" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      LOGGER.info( "Loading Graph ..." );
      try ( DotLogger dotLogger = new DotLogger() ) {
            EmbeddedConnection.getInstance()
                              .connectToGraph();
      } catch ( IOException ioE ) {
         // Do nothing
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      // Do nothing
   }


}
