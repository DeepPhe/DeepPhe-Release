package org.apache.ctakes.core.util.window;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {12/1/2021}
 */
@FunctionalInterface
public interface InWindowFinder<A extends Annotation> {

   /**
    *
    * @param jcas ye olde ...
    * @param windowOffset character offset of the window within the document text.
    * @param windowText text within the window.
    * @param foundItems list of items already found by any other means.
    * @return the foundItems list with any newly found items added.
    */
   List<A> addFound( JCas jcas, Integer windowOffset, String windowText, List<A> foundItems );


   default List<A> findInAnnotation( Annotation annotation ) {
      try {
         return findInWindow( annotation.getCAS().getJCas(),
                              annotation.getBegin(),
                              annotation.getCoveredText() );
      } catch ( CASException casE ) {
         return Collections.emptyList();
      }
   }

   default List<A> findInWindow( JCas jcas, Integer windowOffset, String windowText ) {
      return addFound( jcas, windowOffset, windowText, new ArrayList<>() );
   }

}
