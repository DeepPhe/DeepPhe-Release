package org.healthnlp.deepphe.fhir.summary;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.TextMention;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Resource;

/**
 * medical record for a given patient
 *
 * @author tseytlin
 */
public class MedicalRecord implements Element {
   private String patientIdentifier;
   private Patient patient;
   private PatientSummary patientSummary;
   private CancerSummary cancerSummary;
   private List<Report> reports;

   public String getDisplayText() {
      return (patient != null ? patient.getPatientName() : "Generic") + " Medical Record";
   }

   public String getResourceIdentifier() {
      return getDisplayText();
   }

   public String toString() {
      return getDisplayText();
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

   public Report getComposition() {
      return null;
   }

   public void setComposition( Report r ) {

   }

   public String getPatientIdentifier() {
      if ( patientIdentifier == null && patient != null )
         patientIdentifier = getPatient().getResourceIdentifier();
      return patientIdentifier;
   }

   public void setPatientIdentifier( String patientIdentifier ) {
      this.patientIdentifier = patientIdentifier;
   }

   public Resource getResource() {
      return null;
   }

   public CodeableConcept getCode() {
      return FHIRUtils.getCodeableConcept( getConceptURI() );
   }

   public URI getConceptURI() {
      return FHIRConstants.MEDICAL_RECORD_URI;
   }

   public void save( File e ) throws Exception {
   }

   public void copy( Resource r ) {
   }

   public String getAnnotationType() {
      return FHIRConstants.ANNOTATION_TYPE_RECORD;
   }

   public Patient getPatient() {
      return patient;
   }

   public void setPatient( Patient patient ) {
      this.patient = patient;
      if ( getPatientSummary() != null )
         getPatientSummary().setPatient( patient );
      if ( getCancerSummary() != null )
         getCancerSummary().setPatient( patient );

   }

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

   public void addReport( Report r ) {
      getReports().add( r );
   }

   /**
    * return all facts that are contained within report level summaries
    *
    * @return
    */
   public List<Fact> getReportLevelFacts() {
      List<Fact> list = new ArrayList<Fact>();
      for ( Report r : getReports() ) {
         for ( Summary s : r.getCompositionSummaries() ) {
            list.addAll( s.getContainedFacts() );
         }
      }
      return list;
   }

   /**
    * return all facts that are contained within phenotype level summaries
    *
    * @return
    */
   public List<Fact> getRecordLevelFacts() {
      List<Fact> list = new ArrayList<Fact>();
      if ( cancerSummary != null )
         list.addAll( cancerSummary.getContainedFacts() );
      if ( patientSummary != null )
         list.addAll( patientSummary.getContainedFacts() );
      return list;
   }

}
