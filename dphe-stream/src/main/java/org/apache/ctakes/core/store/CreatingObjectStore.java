package org.apache.ctakes.core.store;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public interface CreatingObjectStore<T> extends ObjectStore<T>, ObjectCreator<T> {

   /**
    *
    * @param id unique object id.
    * @return a stored object with the id or a new object created with the id.
    */
   default T getOrCreate( String id ) {
      final T stored = get( id );
      if ( stored != null ) {
         return stored;
      }
      final T newObject = create( id );
      if ( newObject == null ) {
         return null;
      }
      add( id, newObject );
      return newObject;
   }

}
