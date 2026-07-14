package org.healthnlp.deepphe.nlp.refactor.node;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public enum StorageChoices {
   INSTANCE;

   static public StorageChoices getInstance() {
      return INSTANCE;
   }

   static private final Object LOCK = new Object();
   // 1 Hour
   static private final long TIMEOUT = 1000 * 60 * 60;

   static private final long PERIOD = TIMEOUT / 4;
   static private final long START = TIMEOUT + PERIOD;

   private final ScheduledExecutorService _cacheCleaner;


   private final Map<String,Long> _storeNotes = new HashMap<>();
   private final Map<String,Long> _storePatients = new HashMap<>();


   StorageChoices() {
      _cacheCleaner = Executors.newScheduledThreadPool( 1 );
      // Won't start without this
//      _cacheCleaner.scheduleAtFixedRate( new StorageChoices.CacheCleaner(), START, PERIOD, TimeUnit.MILLISECONDS );
   }

   public void shutdown() {
      _cacheCleaner.shutdown();
   }

   public boolean getStoreNote( final String noteId ) {
      return true;
//      synchronized ( LOCK ) {
//         return _storeNotes.containsKey( noteId );
//      }
   }

   public void setStoreNote( final String noteId ) {
      synchronized ( LOCK ) {
         _storeNotes.put( noteId, System.currentTimeMillis() );
      }
   }

   public boolean getStorePatient( final String patientId ) {
      return true;
//      synchronized ( LOCK ) {
//         return _storePatients.containsKey( patientId );
//      }
   }

   public void setStorePatient( final String patientId ) {
      synchronized ( LOCK ) {
         _storePatients.put( patientId, System.currentTimeMillis() );
      }
   }


   private final class CacheCleaner implements Runnable {
      public void run() {
         final long old = System.currentTimeMillis() - TIMEOUT;
         synchronized ( LOCK ) {
            final Collection<String> removals = new HashSet<>();
            _storeNotes.entrySet()
                          .stream()
                          .filter( e -> e.getValue() < old )
                          .map( Map.Entry::getKey )
                          .forEach( removals::add );
            _storeNotes.keySet().removeAll( removals );
            removals.clear();
            _storePatients.entrySet()
                          .stream()
                          .filter( e -> e.getValue() < old )
                          .map( Map.Entry::getKey )
                          .forEach( removals::add );
            _storePatients.keySet().removeAll( removals );
         }
      }
   }


}
