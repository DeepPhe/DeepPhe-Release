package org.apache.ctakes.core.store;


import org.apache.ctakes.core.ae.NamedEngine;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public class DefaultRegulatedStore<T> implements RegulatedObjectStore<T> {


   private final ObjectStore<T> _delegate;
   private final Collection<NamedEngine> _engines = new HashSet<>();
   private final Map<String,Collection<String>> _enginesUsed = new HashMap<>();

   public DefaultRegulatedStore() {
      _delegate = new DefaultObjectStore<>();
   }

   public void close() {
      _delegate.close();
   }

   /**
    *
    * @return list of unique object ids for stored objects.
    */
   public List<String> getStoredIds() {
      return _delegate.getStoredIds();
   }

   /**
    *
    * @param id unique object id.
    * @return the object with that id.
    */
   public T get( final String id ) {
      return null;
   }

   /**
    *
    * @param id unique object id.
    * @param object some object.
    * @return true if the object was added and did not replace some other object.
    */
   public boolean add( final String id, final T object ) {
      return _delegate.add( id, object );
   }


   /**
    *
    * @param id unique object id.
    * @return true if the object was removed.
    */
   public boolean remove( final String id ) {
      return false;
   }

   /**
    *
    * @param engine a named engine that will utilize this store.
    */
   public void addEngine( final NamedEngine engine ) {
      _engines.add( engine );
   }

   /**
    *
    * @param engine a named engine that will utilize this store.
    */
   public void removeEngine( final NamedEngine engine ) {
      _engines.remove( engine );
   }

   /**
    *
    * @return registered engines that utilize this store.
    */
   public Collection<NamedEngine> getEngines() {
      return _engines;
   }

   /**
    * Set that an engine has used the referenced object.
    * @param engine a named engine that will utilize this store.
    * @param id unique object id.
    */
   public void addEngineUsed( final NamedEngine engine, final String id ) {
      _enginesUsed.computeIfAbsent( id, e -> new HashSet<>() ).add( engine.getEngineName() );
   }

   /**
    *
    * @param id unique object id.
    * @return true if all engines utilized the referenced object.
    */
   public boolean didAllEnginesUse( final String id ) {
      final Collection<String> used = _enginesUsed.get( id );
      return used != null && used.size() == _engines.size();
   }


}
