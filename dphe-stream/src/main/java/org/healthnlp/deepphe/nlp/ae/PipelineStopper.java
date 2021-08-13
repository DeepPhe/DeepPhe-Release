package org.healthnlp.deepphe.nlp.ae;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * @author SPF , chip-nlp
 * @since {6/24/2021}
 */
final public class PipelineStopper extends JCasAnnotator_ImplBase {

   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
   }

   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      System.exit( 0 );
   }

}
