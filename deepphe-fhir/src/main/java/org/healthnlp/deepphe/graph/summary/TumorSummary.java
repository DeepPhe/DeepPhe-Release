package org.healthnlp.deepphe.graph.summary;

import org.healthnlp.deepphe.graph.Report;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.net.URI;


public class TumorSummary extends Summary {
   private TumorPhenotype phenotype;

   private FactList bodySite;
   private FactList treatment;
   private FactList outcome;
   private FactList sequenceVariants;
   private FactList tumorType;

   public TumorSummary() {
      phenotype = new TumorPhenotype();
   }

   public FactList getTumorType() {
      return tumorType;
   }

   public void setTumorType( FactList tumorType ) {
      this.tumorType = tumorType;
   }

   public void setReport( Report r ) {
      super.setReport( r );
      getPhenotype().setReport( r );
   }

   public TumorPhenotype getPhenotype() {
      return phenotype;
   }

   public void setPhenotype( TumorPhenotype phenotype ) {
      this.phenotype = phenotype;
   }

   public FactList getBodySite() {
      return bodySite;
   }

   public void setBodySite( FactList bodySite ) {
      this.bodySite = bodySite;
   }

   public FactList getTreatment() {
      return treatment;
   }

   public void setTreatment( FactList treatment ) {
      this.treatment = treatment;
   }

   public FactList getOutcome() {
      return outcome;
   }

   public void setOutcome( FactList outcome ) {
      this.outcome = outcome;
   }

   public FactList getSequenceVariants() {
      return sequenceVariants;
   }

   public void setSequenceVariants( FactList sequenceVariants ) {
      this.sequenceVariants = sequenceVariants;
   }

   public String getDisplayText() {
      return getClass().getSimpleName();
   }

   public String getResourceIdentifier() {
      return getClass().getSimpleName() + "_" + Math.abs( hashCode() );
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer( super.getSummaryText() );
      // add phenotype
      if ( getPhenotype() != null ) {
         st.append( getPhenotype().getSummaryText() + "\n" );
      }
      return st.toString();
   }

   public URI getConceptURI() {
      return FHIRConstants.TUMOR_SUMMARY_URI;
   }


}
