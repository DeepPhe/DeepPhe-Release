package org.apache.ctakes.cancer.ae;


import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.cancer.owl.OwlUriResolver;
import org.apache.ctakes.cancer.phenotype.receptor.StatusFinder;
import org.apache.ctakes.cancer.phenotype.size.SizeFinder;
import org.apache.ctakes.cancer.phenotype.stage.StageFinder;
import org.apache.ctakes.cancer.phenotype.tnm.TnmFinder;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * Finds and Adds phenotypes size, tnm, stage, receptor status to cas
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/7/2017
 */
final public class PhenotypeFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PhenotypeFinder" );

   /**
    * Finds and Adds phenotypes size, tnm, stage, receptor status to cas
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Phenotype Mentions ..." );
      final Collection<Sentence> sentences = JCasUtil.select( jCas, Sentence.class );
      for ( Sentence sentence : sentences ) {
         // Size
         SizeFinder.addSentenceSizes( jCas, sentence );
         // TNM
         TnmFinder.getInstance().findTnms( jCas, sentence );
         // Stage
         StageFinder.getInstance().findStages( jCas, sentence );
         // Receptor Status.  Only for brca
         if ( OwlUriResolver.getBaseUri().equals( OwlConstants.BREAST_CANCER_OWL ) ) {
            StatusFinder.addReceptorStatuses( jCas, sentence );
         }
      }
      LOGGER.info( "Finished Processing" );
   }


}
