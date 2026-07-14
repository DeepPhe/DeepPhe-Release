package org.apache.ctakes.core.store;


import org.apache.ctakes.core.ae.NamedEngine;

import java.util.Collection;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public interface RegulatedObjectStore<T> extends ObjectStore<T> {

   /**
    *
    * @param engine a named engine that will utilize this store.
    */
   void addEngine( NamedEngine engine );

   /**
    *
    * @param engine a named engine that will utilize this store.
    */
   void removeEngine( NamedEngine engine );

   /**
    *
    * @return registered engines that utilize this store.
    */
   Collection<NamedEngine> getEngines();

   /**
    * Set that an engine has used the referenced object.
    * @param engine a named engine that will utilize this store.
    * @param id unique object id.
    */
   void addEngineUsed( NamedEngine engine, String id );

   /**
    *
    * @param id unique object id.
    * @return true if all engines utilized the referenced object.
    */
   boolean didAllEnginesUse( String id );

   /**
    * get the object.  If all named engines have gotten the object, remove the object.
    * @param engine a named engine that will utilize this store.
    * @param id unique object id.
    */
   default T getAndRemoveFor( NamedEngine engine, String id ) {
      final T object = get( id );
      addEngineUsed( engine, id );
      if ( didAllEnginesUse( id ) ) {
         remove( id );
      }
      return object;
   }

   /**
    * get the objects.  If all named engines have gotten an object, remove that object.
    *
    * @param engine a named engine that will utilize this store.
    */
   default Collection<T> getAndRemoveAllFor( NamedEngine engine ) {
      return getStoredIds().stream()
                           .map( id -> getAndRemoveFor( engine, id ) )
                           .collect( Collectors.toSet() );
   }

}
