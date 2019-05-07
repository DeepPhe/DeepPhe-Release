package org.apache.ctakes.core.store;

import org.apache.ctakes.core.ae.NamedEngine;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/1/2019
 */
public interface DataStore<T> {

   /**
    * @param namedEngine -
    */
   default void registerEngine( final NamedEngine namedEngine ) {
      registerEngine( namedEngine.getEngineName() );
   }

   /**
    * @param engineName -
    */
   void registerEngine( final String engineName );

   /**
    * remove all registered engines.  Necessary if the user creates a new pipeline with the same engine.
    */
   void clearEngines();

   /**
    *
    * @param data -
    */
   void store( final T data );

   /**
    * Obtain a medical record not yet processed by the given engine.
    * If all registered engines have popped the record then it is removed from the store.
    * @param namedEngine engine requesting the medical record
    * @return a medical record that has not yet run on the given engine
    */
   default T pop( final NamedEngine namedEngine ) {
      return pop( namedEngine.getEngineName() );
   }

   /**
    * Obtain a medical record not yet processed by the given engine.
    * If all registered engines have popped the record then it is removed from the store.
    * @param engineName engine requesting the medical record
    * @return a medical record that has not yet run on the given engine
    */
   T pop( final String engineName );

   /**
    *
    * @return true if there is no unpopped data
    */
   boolean isDataEmpty();

   /**
    * remove all data.  Just in case something wasn't popped.
    */
   void clearData();

}
