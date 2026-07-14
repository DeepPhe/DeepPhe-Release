package org.apache.ctakes.ner.creator;

import org.apache.ctakes.ner.term.DetailedTerm;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/26/2020
 */
public interface AnnotationCreator {

   void createAnnotations( final JCas jCas,
                           final Collection<DetailedTerm> terms );

}
