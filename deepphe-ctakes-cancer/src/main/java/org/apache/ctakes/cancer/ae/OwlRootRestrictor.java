package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2016
 */
final public class OwlRootRestrictor extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "OwlRootRestrictor" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      OwlParserUtil.getInstance().addUnwantedUriRoot( OwlConstants.CANCER_STAGE_URI );
      OwlParserUtil.getInstance().addUnwantedUriRoot( OwlConstants.TNM_STAGING_URI );
      OwlParserUtil.getInstance().addUnwantedUriRoot( OwlConstants.RECEPTOR_STATUS_URI );
      OwlParserUtil.getInstance().addUnwantedUriRoot( OwlConstants.SECTIONS_URI );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      // Does nothing.  This class exists for initialization only.
   }

}