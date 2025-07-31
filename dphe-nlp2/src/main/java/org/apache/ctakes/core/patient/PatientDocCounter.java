package org.apache.ctakes.core.patient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {6/7/2024}
 */
public enum PatientDocCounter {
   INSTANCE;

   public static PatientDocCounter getInstance() {
      return INSTANCE;
   }


   // Map of Patient Name (id) to document count for that patient.  Required to remove cached patient after last pop()
   private final Map<String, Integer> _processedDocCounts;

   private long _totalProcessedCount = 0;

   PatientDocCounter() {
      _processedDocCounts = new HashMap<>();
   }

   synchronized public int incrementProcessedDocCount(final String patientId) {
      final int count = _processedDocCounts.computeIfAbsent(patientId, p -> 0) + 1;
      _processedDocCounts.put(patientId, count);
      _totalProcessedCount++;
      return count;
   }

   synchronized public int getProcessedDocCount(final String patientId) {
      return _processedDocCounts.getOrDefault(patientId, 0);
   }

   public boolean isPatientFull(final String patientId) {
      // Even though the ctakes PatientNoteStore isn't being used to store any patient jcas,
      // it still has note counts as they should be set by a collection reader.
      final int patientDocCount = PatientNoteStore.getInstance()
              .getWantedDocCount(patientId);
      return getProcessedDocCount(patientId) >= patientDocCount;
   }

   synchronized public void removePatient(final String patientId) {
      _processedDocCounts.remove(patientId);
   }

   synchronized public long getTotalProcessedCount() {
      return _totalProcessedCount;
   }

}
