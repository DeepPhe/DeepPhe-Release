package org.healthnlp.deepphe.nlp.ae.neo4j;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
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

   static public final String _PARAM = "GraphDb";
   static public final String _DESC = "Directory containing Neo4j Ontology Graph.";
   @ConfigurationParameter(
         name = _PARAM,
         description = _DESC,
         mandatory = false
   )
   private String _graphdb;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      LOGGER.info( "Loading Graph ..." );
      try ( DotLogger dotLogger = new DotLogger() ) {
         if ( _graphdb != null && !_graphdb.isEmpty() ) {
            EmbeddedConnection.getInstance().connectToGraph( _graphdb );
         } else {
            EmbeddedConnection.getInstance()
                              .connectToGraph();
         }
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
