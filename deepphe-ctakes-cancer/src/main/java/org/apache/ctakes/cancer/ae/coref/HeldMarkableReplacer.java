package org.apache.ctakes.cancer.ae.coref;

import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import static org.apache.ctakes.cancer.util.MarkableHolder.getMarkables;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/10/2017
 */
public final class HeldMarkableReplacer extends JCasAnnotator_ImplBase {
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      // add markables from cas
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      getMarkables( documentId ).forEach( Markable::addToIndexes );
   }
}
