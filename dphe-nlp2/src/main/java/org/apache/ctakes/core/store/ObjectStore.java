package org.apache.ctakes.core.store;

import java.io.Closeable;
import java.util.List;

/**
 * Store and retrieve objects outside the standard UIMA context.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public interface ObjectStore<T> extends Closeable {

   /**
    * Some ObjectStores may need to perform some shutdown procedure
    */
   default void close() {
   }

   /**
    *
    * @return list of unique object ids for stored objects.
    */
   List<String> getStoredIds();

   /**
    *
    * @param id unique object id.
    * @return the object with that id.
    */
   T get( String id );

   /**
    *
    * @param id unique object id.
    * @param object some object.
    * @return true if the object was added and did not replace some other object.
    */
   boolean add( String id, T object );

   /**
    * Performs a get and remove.  Similar to a pop.
    * @param id unique object id.
    * @return the object with that id.
    */
   default T getAndRemove( String id ) {
      final T object = get( id );
      remove( id );
      return object;
   }

   /**
    *
    * @param id unique object id.
    * @return true if the object was removed.
    */
   default boolean remove( String id ) {
      return false;
   }


}
