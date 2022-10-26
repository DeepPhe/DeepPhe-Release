package org.healthnlp.deepphe.nlp.ae;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @since {6/24/2021}
 */
final public class PipelineStopper extends JCasAnnotator_ImplBase {

   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
   }

   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      Logger.getLogger( "PipelineStopper" ).info( "Processing Complete." );
      final Frame[] frames = Frame.getFrames();
      if ( frames != null && frames.length > 0 ) {
         JOptionPane.showMessageDialog( null, "Processing Complete.  \n"
                                              + "Please exit the Patient Phenotype Summarizer." );
      } else {
         System.exit( 0 );
      }
   }

}
