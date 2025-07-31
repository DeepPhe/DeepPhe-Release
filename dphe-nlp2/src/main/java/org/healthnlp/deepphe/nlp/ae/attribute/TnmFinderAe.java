package org.healthnlp.deepphe.nlp.ae.attribute;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2023}
 */
@PipeBitInfo(
      name = "TnmFinderAe",
      description = "Finds TNM.",
      role = PipeBitInfo.Role.ANNOTATOR
)
public class TnmFinderAe extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "TnmFinderAe" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding TNM ..." );
      TnmFinder.addTnms( jcas );
   }

}
