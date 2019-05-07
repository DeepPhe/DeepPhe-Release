package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.store.DataStore;
import org.apache.ctakes.core.store.DefaultDataStore;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/1/2019
 */
public enum PatientCiContainerStore implements DataStore<PatientCiContainer> {
   INSTANCE;

   static public PatientCiContainerStore getInstance() {
      return INSTANCE;
   }

   // delegate
   private final DefaultDataStore<PatientCiContainer> _dataStore = new DefaultDataStore<>();

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
   public void store( final PatientCiContainer data ) {
      _dataStore.store( data );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PatientCiContainer pop( final NamedEngine namedEngine ) {
      return _dataStore.pop( namedEngine );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PatientCiContainer pop( final String engineName ) {
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
