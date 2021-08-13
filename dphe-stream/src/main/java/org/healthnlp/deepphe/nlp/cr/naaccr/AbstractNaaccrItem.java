package org.healthnlp.deepphe.nlp.cr.naaccr;


import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
abstract public class AbstractNaaccrItem<T extends NaaccrItem>
      implements NaaccrItem, NaaccrItemGetter<T>,NaaccrSectionGetter {

   private String _id;
   private int _index = 0;
   private final List<T> _subItems = new ArrayList<>();

   public String getId() {
      return _id;
   }

   public void setId( final String id ) {
      _id = id;
   }

   public void add( final T item ) {
      _subItems.add( item );
   }

   public T get() {
      return _subItems.get( _index );
   }

   public boolean hasNext() {
      return _index < _subItems.size() - 1;
   }

   public T next() {
      _index++;
      return get();
   }

   public JCasBuilder addToBuilder( final JCasBuilder builder ) {
      return get().addToBuilder( builder );
   }

   public void populateJCas( final JCas jCas ) {
      get().populateJCas( jCas );
   }

   public int getSectionCount() {
      int total = 0;
      for ( T subItem : _subItems ) {
         if ( subItem instanceof NaaccrSectionGetter ) {
            total += ((NaaccrSectionGetter)subItem).getSectionCount();
         } else {
            total++;
         }
      }
      return total;
   }

}
