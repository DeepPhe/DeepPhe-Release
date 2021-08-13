package org.healthnlp.deepphe.nlp.reader.file;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.healthnlp.deepphe.nlp.reader.file.Doc.EMPTY_DOC;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2020
 */
public class MutableDocStore implements DocStore {

   final private List<Doc> _docs = new ArrayList<>();

   public void addDoc( final Doc doc ) {
      _docs.add( doc );
   }

   public void addDocs( final Collection<Doc> docs ) {
      _docs.addAll( docs );
   }


   /**
    * @param index -
    * @return the doc at index.
    */
   @Override
   public Doc getDoc( final int index ) {
      if ( index >= getDocCount() ) {
         return EMPTY_DOC;
      }
      return _docs.get( index );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getDocCount() {
      return _docs.size();
   }


}
