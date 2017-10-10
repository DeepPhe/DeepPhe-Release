package org.healthnlp.deepphe.fhir.summary;

import java.net.URI;
import java.util.List;

import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;


public class PatientSummary extends Summary {
   private PatientPhenotype phenotype;
   private String summaryType = getClass().getSimpleName();
   private String uuid = String.valueOf( Math.abs( hashCode() ) );

   public void setComposition( Report r ) {
      super.setComposition( r );
      getPhenotype().setComposition( r );
   }

   /**
    * return all facts that are contained within this fact
    *
    * @return
    */
   public List<Fact> getContainedFacts() {
      List<Fact> list = super.getContainedFacts();
      list.addAll( getPhenotype().getContainedFacts() );
      return list;
   }


   public FactList getName() {
      return getFactsOrInsert( FHIRConstants.HAS_NAME );
   }

   public FactList getGender() {
      return getFactsOrInsert( FHIRConstants.HAS_GENDER );
   }

   public FactList getBirthDate() {
      return getFactsOrInsert( FHIRConstants.HAS_BIRTH_DATE );
   }

   public FactList getDeathDate() {
      return getFactsOrInsert( FHIRConstants.HAS_DEATH_DATE );
   }


   public PatientPhenotype getPhenotype() {
      if ( phenotype == null )
         phenotype = new PatientPhenotype();
      return phenotype;
   }


   public void setPhenotype( PatientPhenotype phenotype ) {
      this.phenotype = phenotype;
   }


   public FactList getOutcomes() {
      return getFacts( FHIRConstants.HAS_OUTCOME );
   }

   public FactList getSequenceVariant() {
      return getFacts( FHIRConstants.HAS_SEQUENCE_VARIENT );
   }

   public String getDisplayText() {
      return summaryType;
   }

   public String getResourceIdentifier() {
      return summaryType + "_" + uuid;
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer( super.getSummaryText() );
      st.append( getPhenotype().getSummaryText() + "\n" );
      return st.toString();
   }

   public URI getConceptURI() {
      return conceptURI != null ? conceptURI : FHIRConstants.PATIENT_SUMMARY_URI;
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

   public boolean isAppendable( Summary s ) {
      return s instanceof PatientSummary;
   }

   public void append( Summary s ) {
      super.append( s );
      PatientSummary summary = (PatientSummary) s;
      getPhenotype().append( summary.getPhenotype() );

   }
}
