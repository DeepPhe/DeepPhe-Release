package org.apache.ctakes.core.store;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/1/2019
 */
final public class DefaultDataStore<T> implements DataStore<T> {

   private final Collection<String> _engines = ConcurrentHashMap.newKeySet();

   private final Map<T, Collection<String>> _dataMap = new ConcurrentHashMap<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void registerEngine( final String engineName ) {
      if ( !_engines.add( engineName ) ) {
         throw new IllegalArgumentException(
               engineName + " already Registered!  To add an engine twice, please use the parameter " +
               "EngineName" +
               " to specify unique names OR if you are developing the engine override getEngineName() method." );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearEngines() {
      _engines.clear();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void store( final T data ) {
      _dataMap.put( data, new ArrayList<>() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public synchronized T pop( final String engineName ) {
      if ( _dataMap.isEmpty() ) {
         return null;
      }
      T data = null;
      boolean removeDataEntry = false;
      for ( Map.Entry<T,Collection<String>> recordEntry : _dataMap.entrySet() ) {
         final Collection<String> enginesRun = recordEntry.getValue();
         if ( !enginesRun.contains( engineName ) ) {
            data = recordEntry.getKey();
            enginesRun.add( engineName );
            removeDataEntry = enginesRun.size() == _engines.size();
            break;
         }
      }
      if ( removeDataEntry ) {
         _dataMap.remove( data );
      }
      return data;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isDataEmpty() {
      return _dataMap.isEmpty();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearData() {
      _dataMap.clear();
   }

}
