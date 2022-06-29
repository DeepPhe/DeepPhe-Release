package org.apache.ctakes.core.util.section;

import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * The AbstractSectionProcessor can delegate to a TopicProcessor, ParagraphProcessor, ListProcessor and ProseProcessor.
 * Delegation depends upon what processors are specified.
 *
 * @author SPF , chip-nlp
 * @since {12/9/2021}
 */
abstract public class AbstractSectionProcessor extends JCasAnnotator_ImplBase implements SectionProcessor {


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      initializeForSections( context );
      if ( getTopicProcessor() != null ) {
         getTopicProcessor().initializeForTopics( context );
      }
      if ( getParagraphProcessor() != null ) {
         getParagraphProcessor().initializeForParagraphs( context );
      }
      if ( getListProcessor() != null ) {
         getListProcessor().initializeForLists( context );
      }
      if ( getSentenceProcessor() != null ) {
         getSentenceProcessor().initializeForSentences( context );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final NoteDivisions noteDivisions = new NoteDivisions();
      processSections( jCas, noteDivisions );
   }


}
