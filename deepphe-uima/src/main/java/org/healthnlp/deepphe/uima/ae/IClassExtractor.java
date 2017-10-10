package org.healthnlp.deepphe.uima.ae;

import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2015
 */
public class IClassExtractor extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "IClassExtractor" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Starting processing" );

      OwlOntologyConceptUtil.getUris( jcas ).stream().forEach( LOGGER::info );

      OwlOntologyConceptUtil.getIClasses( jcas ).stream().forEach( LOGGER::info );

      LOGGER.info( "Finished processing" );
   }


}
