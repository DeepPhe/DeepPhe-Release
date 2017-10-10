package org.healthnlp.deepphe.graph;


import org.healthnlp.deepphe.graph.summary.CancerSummary;
import org.healthnlp.deepphe.graph.summary.PatientSummary;
import org.healthnlp.deepphe.graph.summary.Summary;
import org.healthnlp.deepphe.graph.summary.TumorSummary;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.Collection;
import java.util.List;


/**
 * Simplified OGM POJO for fhir.Report
 *
 * @author Girish Chavan
 */
public class Report {

   private Long id;
   String reportText;
   String summaryText;
   Collection<Summary> compositionSummaries;
   List<CancerSummary> cancerSummaries;
   List<TumorSummary> tumorSummaries;
   PatientSummary patientSummary;
   Patient patient;


   public Long getId() {
      return id;
   }

   public void setId( Long id ) {
      this.id = id;
   }

   public Patient getPatient() {
      return patient;
   }

   public void setPatient( Patient patient ) {
      this.patient = patient;
   }

   public String getReportText() {
      return reportText;
   }

   public void setReportText( String reportText ) {
      this.reportText = reportText;
   }

   public String getSummaryText() {
      return summaryText;
   }

   public void setSummaryText( String summaryText ) {
      this.summaryText = summaryText;
   }

   public Collection<Summary> getCompositionSummaries() {
      return compositionSummaries;
   }

   public void setCompositionSummaries( Collection<Summary> compositionSummaries ) {
      this.compositionSummaries = compositionSummaries;
   }

   public List<CancerSummary> getCancerSummaries() {
      return cancerSummaries;
   }

   public void setCancerSummaries( List<CancerSummary> cancerSummaries ) {
      this.cancerSummaries = cancerSummaries;
   }

   public List<TumorSummary> getTumorSummaries() {
      return tumorSummaries;
   }

   public void setTumorSummaries( List<TumorSummary> tumorSummaries ) {
      this.tumorSummaries = tumorSummaries;
   }

   public PatientSummary getPatientSummary() {
      return patientSummary;
   }

   public void setPatientSummary( PatientSummary patientSummary ) {
      this.patientSummary = patientSummary;
   }

   public String getAnnotationType() {
      return FHIRConstants.ANNOTATION_TYPE_DOCUMENT;
   }


}
