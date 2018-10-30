package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.DotLogger;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/16/2018
 */
@PipeBitInfo(
      name = "Neo4jConnector",
      description = "Connects to neo4j session on initialization.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class Neo4jConnector extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "Neo4jConnector" );

   static private final String OUTPUT_GRAPH_DB = "output_graph/deepphe.db";

   /**
    * Name of configuration parameter that must be set to the path of a directory into which the
    * output files will be written.
    */
   @ConfigurationParameter(
         name = ConfigParameterConstants.PARAM_OUTPUTDIR,
         description = ConfigParameterConstants.DESC_OUTPUTDIR
   )
   private File _outputRootDir;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      LOGGER.info( "Loading Graph ..." );
      try ( DotLogger dotLogger = new DotLogger() ) {
         if ( _outputRootDir == null ) {
            Neo4jConnectionFactory.getInstance()
                                  .connectToGraph();
         } else {
            Neo4jConnectionFactory.getInstance()
                                  .connectToGraph( _outputRootDir.getAbsolutePath() + "/" + OUTPUT_GRAPH_DB );
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
