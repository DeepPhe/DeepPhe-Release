package org.healthnlp.deepphe.fhir.summary;

import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CancerSummary extends Summary {
   private CancerPhenotype phenotype;
   private List<TumorSummary> tumors;

   public CancerSummary( String id ) {
      setResourceIdentifier( id );
      phenotype = new CancerPhenotype();
      phenotype.setResourceIdentifier( id );
   }

   public void setComposition( Report r ) {
      super.setComposition( r );
      getPhenotype().setComposition( r );
      for ( TumorSummary ts : getTumors() ) {
         ts.setComposition( r );
      }
   }

   public void setPatient( Patient r ) {
      super.setPatient( r );
      getPhenotype().setPatient( r );
      for ( TumorSummary ts : getTumors() ) {
         ts.setPatient( r );
      }
   }

   /**
    * return all facts that are contained within this fact
    *
    * @return
    */
   public List<Fact> getContainedFacts() {
      List<Fact> list = super.getContainedFacts();
      List<Fact> phFacts = getPhenotype().getContainedFacts();
      for ( Fact f : phFacts ) {
         f.addContainerIdentifier( getResourceIdentifier() );
         list.add( f );
      }
      //list.addAll(getPhenotype().getContainedFacts());
      for ( TumorSummary ts : getTumors() ) {
         List<Fact> tsFacts = ts.getContainedFacts();
         for ( Fact f : tsFacts ) {
            f.addContainerIdentifier( getResourceIdentifier() );
            list.add( f );
         }
         //list.addAll(ts.getContainedFacts());
      }
      return list;
   }


   public FactList getBodySite() {
      return getFactsOrInsert( FHIRConstants.HAS_BODY_SITE );
   }

   public FactList getDiagnosis() {
      return getFactsOrInsert( FHIRConstants.HAS_DIAGNOSIS );
   }

   public List<CancerPhenotype> getPhenotypes() {
      return Arrays.asList( getPhenotype() );
   }

   public FactList getTreatments() {
      return getFactsOrInsert( FHIRConstants.HAS_TREATMENT );
   }

   public CancerPhenotype getPhenotype() {
      return phenotype;
   }

   public void setPhenotype( CancerPhenotype phenotype ) {
      this.phenotype = phenotype;
   }

   public FactList getOutcomes() {
      return getFacts( FHIRConstants.HAS_OUTCOME );
   }

   public List<TumorSummary> getTumors() {
      if ( tumors == null )
         tumors = new ArrayList<TumorSummary>();
      return tumors;
   }

   public void addTumor( TumorSummary tumor ) {
      tumor.setAnnotationType( getAnnotationType() );
      tumor.setCancerSummary( this );
      getTumors().add( tumor );
   }

   public void removeTumor( TumorSummary tumor ) {
      if ( tumor != null && getTumors().size() > 0 ) {
         tumors.remove( tumor );
         getBodySite().removeAll( tumor.getBodySite() );
      }
   }

   public TumorSummary getTumorSummaryByIdentifier( String uuid ) {
      TumorSummary toret = null;
      for ( TumorSummary ts : getTumors() ) {
         if ( ts.getResourceIdentifier().equals( uuid ) )
            toret = ts;
      }
      if ( toret == null ) {
         toret = new TumorSummary( uuid );
         addTumor( toret );
      }
      return toret;
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer( super.getSummaryText() );
      st.append( getPhenotype().getSummaryText() + "\n" );
      for ( TumorSummary ts : getTumors() ) {
         st.append( FHIRConstants.LINE + "\n" + ts.getSummaryText() + "\n" );
      }
      return st.toString();
   }

   public URI getConceptURI() {
      return conceptURI != null ? conceptURI : FHIRConstants.CANCER_SUMMARY_URI;
   }

   public boolean isAppendable( Summary s ) {
      // maybe if it happens at the same bodySite?
      // will just append it for now
      return s instanceof CancerSummary;
   }

   public void append( Summary s ) {
      super.append( s );
      CancerSummary summary = (CancerSummary) s;

      CancerPhenotype phenotype = getPhenotype();
      phenotype.append( summary.getPhenotype() );

      // add tumors if none exist
      for ( TumorSummary t : summary.getTumors() ) {
         append( t );
      }
   }

   private String getTumorBodySite( TumorSummary ts ) {
      StringBuffer b = new StringBuffer();
      for ( Fact f : ts.getBodySite() ) {
         b.append( "-" + f.getName() );
      }
      return b.toString();
   }

   /**
    * append tumor summary if possible
    *
    * @param s
    */
   public void append( TumorSummary ts ) {
      String id = resourceIdentifier + getTumorBodySite( ts );
      // add tumors if none exist
      if ( getTumors().isEmpty() ) {
         addTumor( new TumorSummary( id ) );
      }
      // go over existing tumors and append if possible
      boolean appended = false;
      for ( TumorSummary tumor : getTumors() ) {
         if ( tumor.isAppendable( ts ) ) {
            tumor.append( ts );
            appended = true;
         }
      }
      // if this tumor was not appened to existing tumors
      // add a new one
      if ( !appended ) {
         TumorSummary tumor = new TumorSummary( id );
         tumor.append( ts );
         addTumor( tumor );
      }

   }

}
