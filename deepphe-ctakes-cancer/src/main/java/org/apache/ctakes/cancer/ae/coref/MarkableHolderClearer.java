package org.apache.ctakes.cancer.ae.coref;

import org.apache.ctakes.core.coref.MarkableHolder;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/10/2017
 */
public final class MarkableHolderClearer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "MarkableHolderClearer" );

   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Clearing Markable Cache ..." );
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      MarkableHolder.clearMarkables( documentId );
      LOGGER.info( "Finished Markable Clearance" );
   }

}
