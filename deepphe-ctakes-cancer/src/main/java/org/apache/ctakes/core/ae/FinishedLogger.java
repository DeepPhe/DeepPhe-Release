package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2018
 */
@PipeBitInfo(
      name = "FinishedLogger",
      description = "For deepphe.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class FinishedLogger extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "FinishedLogger" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
   }

   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      final String FINISHED =
            "\n\n" +
            " ######    ######   ##     ##  #######   ##       #######  #######  #######\n" +
            "##    ##  ##    ##  ###   ###  ##    ##  ##       ##         ##     ##     \n" +
            "##        ##    ##  #### ####  ##    ##  ##       ##         ##     ##     \n" +
            "##        ##    ##  ## ### ##  #######   ##       #####      ##     #####  \n" +
            "##        ##    ##  ##     ##  ##        ##       ##         ##     ##     \n" +
            "##    ##  ##    ##  ##     ##  ##        ##       ##         ##     ##     \n" +
            " ######    ######   ##     ##  ##        #######  #######    ##     #######\n\n";
      LOGGER.info( FINISHED );
   }


}
