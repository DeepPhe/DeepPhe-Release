package org.apache.ctakes.cancer.ae.coref;

import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/10/2017
 */
public final class MarkableRemover extends JCasAnnotator_ImplBase {
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      // remove markables from cas
      final Collection<Markable> markables = new HashSet<>( org.apache.uima.fit.util.JCasUtil.select( jCas, Markable.class ) );
      markables.forEach( Markable::removeFromIndexes );
   }
}
