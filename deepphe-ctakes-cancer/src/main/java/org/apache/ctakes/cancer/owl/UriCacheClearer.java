package org.apache.ctakes.cancer.owl;

import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/17/2017
 */
public class UriCacheClearer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "UriCacheClearer" );


   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Starting URI cache clearance" );
      // clear markables from cas
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      UriAnnotationCache.getInstance().clearAnnotationCache( documentId );
      LOGGER.info( "Finished URI cache clearance" );
   }

}
