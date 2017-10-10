package org.healthnlp.deepphe.graph.summary;

import org.healthnlp.deepphe.graph.Report;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.net.URI;
import java.util.List;


public class CancerSummary extends Summary {
   private List<CancerPhenotype> phenotypes;
   private List<TumorSummary> tumors;
   private FactList bodySite;
   private FactList treatments;
   private FactList outcomes;

   public CancerSummary() {

   }

   public List<CancerPhenotype> getPhenotypes() {
      return phenotypes;
   }

   public void setPhenotypes( List<CancerPhenotype> phenotypes ) {
      this.phenotypes = phenotypes;
   }

   public List<TumorSummary> getTumors() {
      return tumors;
   }

   public void setTumors( List<TumorSummary> tumors ) {
      this.tumors = tumors;
   }

   public FactList getBodySite() {
      return bodySite;
   }

   public void setBodySite( FactList bodySite ) {
      this.bodySite = bodySite;
   }

   public FactList getTreatments() {
      return treatments;
   }

   public void setTreatments( FactList treatments ) {
      this.treatments = treatments;
   }

   public FactList getOutcomes() {
      return outcomes;
   }

   public void setOutcomes( FactList outcomes ) {
      this.outcomes = outcomes;
   }

   public void setReport( Report r ) {
      if ( r == null )
         return;
      super.setReport( r );
      if ( getPhenotypes() != null )
         getPhenotypes().get( 0 ).setReport( r );
      if ( getTumors() != null ) {
         for ( TumorSummary ts : getTumors() ) {
            ts.setReport( r );
         }
      }
   }


   public String getDisplayText() {
      return getClass().getSimpleName();
   }

   public String getResourceIdentifier() {
      return getClass().getSimpleName() + "_" + Math.abs( hashCode() );
   }

   public String getSummaryText() {
      if ( super.getSummaryText() == null )
         return "";

      StringBuilder st = new StringBuilder( super.getSummaryText() );
      st.append( getPhenotypes().get( 0 ).getSummaryText() );
      st.append( "\n" );

      for ( TumorSummary ts : getTumors() ) {
         st.append( ts.getSummaryText() + "\n" );
      }
      return st.toString();
   }

   public URI getConceptURI() {
      return FHIRConstants.CANCER_SUMMARY_URI;
   }


}
