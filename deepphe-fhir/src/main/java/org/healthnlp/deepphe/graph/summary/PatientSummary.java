package org.healthnlp.deepphe.graph.summary;

import org.healthnlp.deepphe.graph.Report;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.net.URI;


public class PatientSummary extends Summary {
   private PatientPhenotype phenotype;
   private FactList name;
   private FactList gender;
   private FactList birthDate;
   private FactList deathDate;
   private FactList outcomes;
   private FactList sequenceVariant;
   private String displayText;

   public void setReport( Report r ) {
      super.setReport( r );
      getPhenotype().setReport( r );
   }

   public FactList getName() {
      return name;
   }

   public void setName( FactList name ) {
      this.name = name;
   }

   public String getResourceIdentifier() {
      return getClass().getSimpleName() + "_" + Math.abs( hashCode() );
   }

   public FactList getGender() {
      return gender;
   }

   public void setGender( FactList gender ) {
      this.gender = gender;

   }

   public FactList getBirthDate() {
      return birthDate;
   }

   public void setBirthDate( FactList birthDate ) {
      this.birthDate = birthDate;
   }

   public FactList getDeathDate() {
      return deathDate;
   }

   public void setDeathDate( FactList deathDate ) {
      this.deathDate = deathDate;
   }

   public PatientPhenotype getPhenotype() {
      if ( phenotype == null )
         phenotype = new PatientPhenotype();
      return phenotype;
   }

   public void setPhenotype( PatientPhenotype phenotype ) {
      this.phenotype = phenotype;
   }

   public String getSummaryText() {
      if ( super.getSummaryText() == null )
         return "";
      StringBuffer st = new StringBuffer( super.getSummaryText() );
      st.append( getPhenotype().getSummaryText() + "\n" );
      return st.toString();
   }

   public URI getConceptURI() {
      return FHIRConstants.PATIENT_SUMMARY_URI;
   }

   public FactList getOutcomes() {
      return outcomes;
   }

   public void setOutcomes( FactList outcomes ) {
      this.outcomes = outcomes;
   }

   public FactList getSequenceVariant() {
      return sequenceVariant;
   }

   public void setSequenceVariant( FactList sequenceVariant ) {
      this.sequenceVariant = sequenceVariant;
   }


   public String getDisplayText() {
      return displayText;
   }

   public void setDisplayText( String displayText ) {
      this.displayText = displayText;
   }
}
