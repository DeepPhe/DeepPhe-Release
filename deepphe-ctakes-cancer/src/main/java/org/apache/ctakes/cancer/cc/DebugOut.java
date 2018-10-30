package org.apache.ctakes.cancer.cc;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import java.util.function.Function;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/12/2017
 */
public class DebugOut extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DebugOut" );

   public void process( final JCas jCas ) throws AnalysisEngineProcessException {

      Function<ConceptInstance, ConceptInstance> toText = ci -> {
         LOGGER.info( "Concept:  " + ci.toString() );
         return ci;
      };

      LOGGER.info( "DOCUMENT : " + DocumentIDAnnotationUtil.getDocumentID( jCas ) );
   }


}
