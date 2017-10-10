package org.healthnlp.deepphe.fhir.summary;

import java.net.URI;

import org.healthnlp.deepphe.util.FHIRConstants;

public class PatientPhenotype extends Summary {
   private String summaryType = getClass().getSimpleName();
   private String uuid = String.valueOf( Math.abs( hashCode() ) );

   public String getDisplayText() {
      return summaryType;
   }

   public String getResourceIdentifier() {
      return summaryType + "_" + uuid;
   }

   public URI getConceptURI() {
      return conceptURI != null ? conceptURI : FHIRConstants.PATIENT_PHENOTYPE_SUMMARY_URI;
   }

   public boolean isAppendable( Summary s ) {
      return s instanceof PatientPhenotype;
   }

   public String getSummaryType() {
      return summaryType;
   }

   public void setSummaryType( String summaryType ) {
      this.summaryType = summaryType;
   }

   public String getUuid() {
      return uuid;
   }

   public void setUuid( String uuid ) {
      this.uuid = uuid;
   }
}