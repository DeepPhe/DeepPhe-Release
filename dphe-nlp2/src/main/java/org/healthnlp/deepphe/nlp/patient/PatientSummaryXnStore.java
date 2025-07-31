package org.healthnlp.deepphe.nlp.patient;


import org.apache.ctakes.core.store.DefaultObjectStore;
import org.apache.ctakes.core.store.ObjectStore;
import org.apache.ctakes.core.store.SelfCleaningStore;
import org.healthnlp.deepphe.neo4j.node.xn.PatientSummaryXn;

import java.util.List;

/**
 * Stores Patient Summary Nodes.
 * The Patient summary node cache is cleaned up every 15 minutes,
 * with all patient summaries not accessed within the last hour removed.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/4/2020
 */
public enum PatientSummaryXnStore implements ObjectStore<PatientSummaryXn> {
   INSTANCE;

   public static PatientSummaryXnStore getInstance() {
      return INSTANCE;
   }


   private final ObjectStore<PatientSummaryXn> _delegate;


   PatientSummaryXnStore() {
      _delegate = new SelfCleaningStore<>( new DefaultObjectStore<>() );
   }

   public void close() {
      _delegate.close();
   }

   public List<String> getStoredIds() {
      return _delegate.getStoredIds();
   }

   public PatientSummaryXn get( final String patientId ) {
      return _delegate.get( patientId );
   }

   public boolean add( final String patientId, final PatientSummaryXn patientSummary ) {
      return _delegate.add( patientId, patientSummary );
   }

}
