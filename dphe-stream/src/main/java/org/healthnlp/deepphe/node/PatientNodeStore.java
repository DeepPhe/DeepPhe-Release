package org.healthnlp.deepphe.node;


import org.apache.ctakes.core.store.CreatingCleaningStore;
import org.apache.ctakes.core.store.CreatingObjectStore;
import org.apache.ctakes.core.store.DefaultObjectStore;
import org.healthnlp.deepphe.neo4j.node.Patient;

import java.util.List;

/**
 * Stores Patient Nodes.
 * The Patient node cache is cleaned up every 15 minutes,
 * with all patients not accessed within the last hour removed.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/4/2020
 */
public enum PatientNodeStore implements CreatingObjectStore<Patient> {
   INSTANCE;

   public static PatientNodeStore getInstance() {
      return INSTANCE;
   }


   private final CreatingCleaningStore<Patient> _delegate;


   PatientNodeStore() {
      _delegate = new CreatingCleaningStore<>( new DefaultObjectStore<>(), new PatientCreator() );
   }

   public void close() {
      _delegate.close();
   }

   public List<String> getStoredIds() {
      return _delegate.getStoredIds();
   }

   public Patient get( final String patientId ) {
      return _delegate.get( patientId );
   }

   public boolean add( final String patientId, final Patient patient ) {
      return _delegate.add( patientId, patient );
   }

   public Patient create( final String patientId ) {
      return _delegate.create( patientId );
   }


}
