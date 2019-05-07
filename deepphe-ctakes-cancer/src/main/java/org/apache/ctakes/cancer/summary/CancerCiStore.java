package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.store.DataStore;
import org.apache.ctakes.core.store.DefaultDataStore;

import java.util.Collection;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/1/2019
 */
public enum CancerCiStore implements DataStore<Collection<CancerCiContainer>> {
   INSTANCE;

   static public CancerCiStore getInstance() {
      return INSTANCE;
   }

   // delegate
   private final DefaultDataStore<Collection<CancerCiContainer>> _dataStore = new DefaultDataStore<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void registerEngine( final NamedEngine namedEngine ) {
      _dataStore.registerEngine( namedEngine );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void registerEngine( final String engineName ) {
      _dataStore.registerEngine( engineName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearEngines() {
      _dataStore.clearEngines();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void store( final Collection<CancerCiContainer> data ) {
      _dataStore.store( data );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<CancerCiContainer> pop( final NamedEngine namedEngine ) {
      return _dataStore.pop( namedEngine );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<CancerCiContainer> pop( final String engineName ) {
      return _dataStore.pop( engineName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isDataEmpty() {
      return _dataStore.isDataEmpty();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearData() {
      _dataStore.clearData();
   }

}
