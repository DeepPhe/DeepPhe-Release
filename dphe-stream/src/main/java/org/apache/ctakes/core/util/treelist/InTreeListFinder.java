package org.apache.ctakes.core.util.treelist;

import org.apache.ctakes.typesystem.type.textspan.FormattedList;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {12/3/2021}
 */
@FunctionalInterface
public interface InTreeListFinder<A extends Annotation> {

   /**
    *
    * @param jcas ye olde ...
    * @param list -
    * @param foundItems list of items already found by any other means.
    * @return the foundItems list with any newly found items added.
    */
   List<A> addFound( JCas jcas, FormattedList list, List<A> foundItems );

   default List<A> findInTreeList( JCas jcas, FormattedList list ) {
      return addFound( jcas, list,  new ArrayList<>() );
   }

}
