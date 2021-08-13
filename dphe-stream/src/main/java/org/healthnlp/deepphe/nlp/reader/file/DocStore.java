package org.healthnlp.deepphe.nlp.reader.file;


//import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2020
 */
public interface DocStore extends Iterable<Doc> {

   /**
    * Exactly what it sounds like.
    */
   default void initialize() throws IOException {
   }

   /**
    * @param index -
    * @return the doc at index.
    */
   Doc getDoc( final int index );

   /**
    * @return the number of docs in the collection.
    */
   int getDocCount();

   /**
    * @return true if the document count is not fixed and more may be added later.
    */
   default boolean isDynamic() {
      return false;
   }


   /**
    * {@inheritDoc}
    */
   @Override
//   @Nonnull
   default Iterator<Doc> iterator() {
      return new DocIterator( this );
   }

   /**
    * This is backed directly by the given DocStore and is not thread safe.
    */
   class DocIterator implements Iterator<Doc> {
      private final DocStore _docStore;
      private int _index = 0;

      private DocIterator( final DocStore docStore ) {
         _docStore = docStore;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean hasNext() {
         return _index < _docStore.getDocCount();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Doc next() {
         final Doc doc = _docStore.getDoc( _index );
         _index++;
         return doc;
      }
   }

}
