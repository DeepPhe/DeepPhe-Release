package org.healthnlp.deepphe.graph.summary;

import org.healthnlp.deepphe.graph.Patient;
import org.healthnlp.deepphe.graph.Report;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.neo4j.ogm.annotation.Relationship;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * medical record copy for graph
 *
 * @author Girish Chavan
 */
public class MedicalRecord {
   private Long id;

   private String patientIdentifier;

   @Relationship( direction = Relationship.OUTGOING )
   private Patient patient;

   @Relationship( direction = Relationship.OUTGOING )
   private PatientSummary patientSummary;

   @Relationship( direction = Relationship.OUTGOING )
   private CancerSummary cancerSummary;

   @Relationship( direction = Relationship.OUTGOING )
   private List<Report> reports;

   public String getDisplayText() {
      return (patient != null ? patient.getPatientName() : "Generic") + " Medical Record";
   }

   public String getSummaryText() {
      StringBuffer b = new StringBuffer( getDisplayText() );
      b.append( "\n==========================\n" );
      if ( patientSummary != null ) {
         b.append( patientSummary.getSummaryText() + "\n" );
      }
      if ( cancerSummary != null ) {
         b.append( cancerSummary.getSummaryText() );
      }
      return b.toString();
   }

   public String getPatientIdentifier() {
      return patientIdentifier;
   }

   public void setPatientIdentifier( String patientIdentifier ) {
      this.patientIdentifier = patientIdentifier;
   }

   public CodeableConcept getCode() {
      return FHIRUtils.getCodeableConcept( getConceptURI() );
   }

   public URI getConceptURI() {
      return FHIRConstants.MEDICAL_RECORD_URI;
   }

   public String getAnnotationType() {
      return FHIRConstants.ANNOTATION_TYPE_RECORD;
   }

   public Patient getPatient() {
      return patient;
   }

   public void setPatient( Patient patient ) {
      this.patient = patient;
   }

   @Relationship( direction = Relationship.OUTGOING )
   public PatientSummary getPatientSummary() {
      return patientSummary;
   }

   public void setPatientSummary( PatientSummary patientSummary ) {
      this.patientSummary = patientSummary;
   }

   public CancerSummary getCancerSummary() {
      return cancerSummary;
   }

   public void setCancerSummary( CancerSummary cancerSummary ) {
      this.cancerSummary = cancerSummary;
   }

   public List<Report> getReports() {
      if ( reports == null )
         reports = new ArrayList<Report>();
      return reports;
   }

   public void setReports( List<Report> reports ) {
      this.reports = reports;
   }

   public Long getId() {
      return id;
   }

   public void setId( Long id ) {
      this.id = id;
   }
}
