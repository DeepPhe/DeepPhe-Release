package org.healthnlp.deepphe.nlp.patient;

import org.apache.ctakes.core.store.CreatingCleaningStore;
import org.apache.ctakes.core.store.CreatingObjectStore;
import org.apache.ctakes.core.store.DefaultObjectStore;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {9/11/2023}
 */
public enum PatientCasStore implements CreatingObjectStore<JCas> {
   INSTANCE;

   public static PatientCasStore getInstance() {
      return INSTANCE;
   }

   private final CreatingCleaningStore<JCas> _delegate;


   PatientCasStore() {
      _delegate = new CreatingCleaningStore<>( new DefaultObjectStore<>(), new PatientCasCreator() );
   }

   public void close() {
      _delegate.close();
   }

   public List<String> getStoredIds() {
      return _delegate.getStoredIds();
   }

   public JCas get( final String patientId ) {
      return _delegate.get( patientId );
   }

   public boolean add( final String patientId, final JCas patient ) {
      return _delegate.add( patientId, patient );
   }

   public JCas create( final String patientId ) {
      return _delegate.create( patientId );
   }

}
