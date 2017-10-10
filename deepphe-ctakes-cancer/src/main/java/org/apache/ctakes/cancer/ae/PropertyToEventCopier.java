package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;


/**
 *
 */
final public class PropertyToEventCopier extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PropertyToEventCopier" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Copying Mention properties to Events ..." );
      for ( EventMention mention : JCasUtil.select( jcas, EventMention.class ) ) {
         // get temporal event mentions and not dictinoary-derived subclasses
         // find either an exact matching span, or an end-matching span with the smallest overlap
         if ( mention.getClass().equals( EventMention.class ) ) {
            EventMention bestCovering = null;
            int smallestSpan = Integer.MAX_VALUE;
            for ( EventMention covering : JCasUtil.selectCovering( EventMention.class, mention ) ) {
               if ( covering.getClass().equals( EventMention.class ) ) {
                  continue;
               }
               if ( covering.getBegin() == mention.getBegin() && covering.getEnd() == mention.getEnd() ) {
                  bestCovering = covering;
                  break;
               } else if ( covering.getEnd() == mention.getEnd() ) {
                  int span = covering.getEnd() - covering.getBegin();
                  if ( span < smallestSpan ) {
                     span = smallestSpan;
                     bestCovering = covering;
                  }
               }
            }
            if ( bestCovering != null ) {
               mention.setPolarity( bestCovering.getPolarity() );
               //            mention.getEvent().getProperties().setPolarity(bestCovering.getPolarity());
               mention.setUncertainty( bestCovering.getUncertainty() );
            }
         }
      }
      LOGGER.info( "Finished processing" );
   }

}
