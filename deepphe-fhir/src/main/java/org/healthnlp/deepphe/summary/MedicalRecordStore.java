package org.healthnlp.deepphe.summary;

import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.uima.analysis_component.AnalysisComponent;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/30/2018
 */
public enum MedicalRecordStore {
   INSTANCE;

   static public MedicalRecordStore getInstance() {
      return INSTANCE;
   }

   private final Collection<String> _registeredEngines = new HashSet<>();

   private final Map<MedicalRecord, Collection<String>> _records = new HashMap<>();

   public synchronized void registerEngine( final NamedEngine namedEngine ) {
      registerEngine( namedEngine.getEngineName() );
   }

   /**
    * @param engineName -
    */
   public synchronized void registerEngine( final String engineName ) {
      if ( !_registeredEngines.add( engineName ) ) {
         throw new IllegalArgumentException( engineName + " already Registered!  To add an engine twice, please use the parameter " + "EngineName" + " to specify unique names OR if you are developing the engine override getEngineName() method.");
      }
   }

   public synchronized void storeMedicalRecord( final MedicalRecord medicalRecord ) {
      _records.put( medicalRecord, new ArrayList<>() );
   }

   /**
    * Obtain a medical record not yet processed by the given engine.
    * If all registered engines have popped the record then it is removed from the store.
    * @param namedEngine engine requesting the medical record
    * @return a medical record that has not yet run on the given engine
    */
   public synchronized MedicalRecord popMedicalRecord( final NamedEngine namedEngine ) {
      return popMedicalRecord( namedEngine.getEngineName() );
   }

   /**
    * Obtain a medical record not yet processed by the given engine.
    * If all registered engines have popped the record then it is removed from the store.
    * @param engineName engine requesting the medical record
    * @return a medical record that has not yet run on the given engine
    */
   public synchronized MedicalRecord popMedicalRecord( final String engineName ) {
      if ( _records.isEmpty() ) {
         return null;
      }
      MedicalRecord record = null;
      boolean remove = false;
      for ( Map.Entry<MedicalRecord,Collection<String>> recordEntry : _records.entrySet() ) {
         final Collection<String> enginesRun = recordEntry.getValue();
         if ( !enginesRun.contains( engineName ) ) {
            record = recordEntry.getKey();
            enginesRun.add( engineName );
            remove = enginesRun.size() == _registeredEngines.size();
            break;
         }
      }
      if ( remove ) {
         _records.remove( record );
      }
      return record;
   }

}
