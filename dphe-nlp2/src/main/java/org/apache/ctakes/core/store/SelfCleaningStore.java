package org.apache.ctakes.core.store;


import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The object cache is cleaned up every 15 minutes,
 * with all object not accessed within the last hour removed.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/30/2020
 */
public class SelfCleaningStore<T> implements ObjectStore<T> {

   static private final Object LOCK = new Object();
   // 1 Hour
//   static private final long TIMEOUT = 1000 * 60 * 60;
   // 5 minutes
   static private final long TIMEOUT = 1000 * 60 * 5;


//   static private final long PERIOD = TIMEOUT / 4;
   static private final long PERIOD = TIMEOUT / 5;
   static private final long START = TIMEOUT + PERIOD;

   private final ObjectStore<T> _delegate;

   private final ScheduledExecutorService _cacheCleaner;
   private final Map<String,Long> _accesses = new HashMap<>();

   public SelfCleaningStore( final ObjectStore<T> delegate ) {
      _delegate = delegate;
      _cacheCleaner = Executors.newScheduledThreadPool( 1 );
      _cacheCleaner.scheduleAtFixedRate( new CacheCleaner(), START, PERIOD, TimeUnit.MILLISECONDS );
   }

   public void close() {
      _cacheCleaner.shutdown();
      _delegate.close();
   }

   public List<String> getStoredIds() {
      return _delegate.getStoredIds();
   }

   public T get( final String objectId ) {
      final T object = _delegate.get( objectId );
      if ( object != null ) {
         updateAccess( objectId );
      }
      return object;
   }

   public boolean add( final String objectId, final T object ) {
      updateAccess( objectId );
      return _delegate.add( objectId, object );
   }

   /**
    *
    * @param id unique object id.
    * @return true if the object was removed.
    */
   public boolean remove( final String id ) {
      _accesses.remove( id );
      return _delegate.remove( id );
   }

   private void updateAccess( final String objectId ) {
      synchronized ( LOCK ) {
         _accesses.put( objectId, System.currentTimeMillis() );
      }
   }


   private final class CacheCleaner implements Runnable {
      public void run() {
         final long old = System.currentTimeMillis() - TIMEOUT;
         synchronized ( LOCK ) {
            final Collection<String> removals = new HashSet<>();
            _accesses.entrySet()
                            .stream()
                            .filter( e -> e.getValue() < old )
                            .map( Map.Entry::getKey )
                            .forEach( removals::add );
            _accesses.keySet().removeAll( removals );
            for ( final String removal : removals ) {
               _delegate.remove( removal );
            }
         }
      }
   }


}
