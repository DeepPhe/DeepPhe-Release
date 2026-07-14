package org.apache.ctakes.core.store;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/30/2020
 */
public class CreatingCleaningStore<T> extends SelfCleaningStore<T> implements CreatingObjectStore<T> {

   private final ObjectCreator<T> _creator;
   public CreatingCleaningStore( final ObjectStore<T> delegateStore, final ObjectCreator<T> creator ) {
      super( delegateStore );
      _creator = creator;
   }

   public T create( final String id ) {
      return _creator.create( id );
   }

}
