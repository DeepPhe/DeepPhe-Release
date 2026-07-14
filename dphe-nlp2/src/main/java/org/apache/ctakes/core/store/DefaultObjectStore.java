package org.apache.ctakes.core.store;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public class DefaultObjectStore<T> implements ObjectStore<T> {


   private final Map<String,T> _objects = new HashMap<>();

   /**
    *
    * @return list of unique object ids for stored objects.
    */
   public List<String> getStoredIds() {
      return Collections.unmodifiableList( new ArrayList<>( _objects.keySet() ) );
   }

   /**
    *
    * @param id unique object id.
    * @return the object with that id.
    */
   public T get( final String id ) {
      return _objects.get( id );
   }

   /**
    *
    * @param id unique object id.
    * @param object some object.
    * @return true if the object was added and did not replace some other object.
    */
   public boolean add( final String id, final T object ) {
      final T previous = _objects.put( id, object );
      return previous == null;
   }

   /**
    *
    * @param id unique object id.
    * @return true if the object was removed.
    */
   public boolean remove( final String id ) {
      final T previous = _objects.remove( id );
      return previous != null;
   }


}
