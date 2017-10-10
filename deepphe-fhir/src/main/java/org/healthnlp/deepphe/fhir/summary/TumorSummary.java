package org.healthnlp.deepphe.fhir.summary;

import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.*;

public class TumorSummary extends Summary {
   private CancerSummary cancerSummary;
   private TumorPhenotype phenotype;
   private FactList tumorType;
   private Map<String, Episode> episodes;

   public TumorSummary( String id ) {
      setResourceIdentifier( id );
      phenotype = new TumorPhenotype();
      phenotype.setResourceIdentifier( id );
   }

   /**
    * add episode to the tummor summary
    *
    * @param episode
    */
   public void addEpisode( Episode ep ) {
      Episode episode = getEpisodeMap().get( ep.getType() );
      if ( episode == null ) {
         episode = new Episode();
         episode.setType( ep.getType() );
         episode.setResourceIdentifier( getResourceIdentifier() + "_" + ep.getType() );
         episode.setTumorSummary( this );
         getEpisodeMap().put( episode.getType(), episode );
      }
      episode.append( ep );
   }

   private Map<String, Episode> getEpisodeMap() {
      if ( episodes == null )
         episodes = new LinkedHashMap<String, Episode>();
      return episodes;
   }

   /**
    * get all episodes associated witht his tumor
    *
    * @return
    */
   public Collection<Episode> getEpisodes() {
      List<Episode> list = new ArrayList<Episode>();
      for ( String key : getEpisodeMap().keySet() ) {
         list.add( getEpisode( key ) );
      }
      return list;
   }

   /**
    * get episode by type:
    * FHIRConstants.EPISODE_PREDIAGNOSTIC
    * FHIRConstants.EPISODE_DIAGNOSTIC
    * FHIRConstants.EPISODE_TREATMENT
    * FHIRConstants.EPISODE_FOLLOW_UP
    *
    * @param type
    * @return
    */
   public Episode getEpisode( String type ) {
      return getEpisodeMap().get( type );
   }


   public void setComposition( Report r ) {
      super.setComposition( r );
      getPhenotype().setComposition( r );
   }

   public void setPatient( Patient r ) {
      super.setPatient( r );
      getPhenotype().setPatient( r );
   }


   public CancerSummary getCancerSummary() {
      return cancerSummary;
   }


   public void setCancerSummary( CancerSummary cancerSummary ) {
      this.cancerSummary = cancerSummary;
   }


   /**
    * return all facts that are contained within this fact
    *
    * @return
    */
   public List<Fact> getContainedFacts() {
      List<Fact> list = super.getContainedFacts();
      //list.addAll(getPhenotype().getContainedFacts());
      List<Fact> phFacts = getPhenotype().getContainedFacts();
      for ( Fact f : phFacts ) {
         f.addContainerIdentifier( getResourceIdentifier() );
         list.add( f );
      }

      return list;
   }

   public FactList getTumorType() {
      if ( tumorType == null )
         tumorType = getFacts( FHIRConstants.HAS_TUMOR_TYPE );
      return tumorType;
   }

   public void setTumorType( FactList tumorType ) {
      this.tumorType = tumorType;
   }

   public TumorPhenotype getPhenotype() {
      return phenotype;
   }

   public void setPhenotype( TumorPhenotype phenotype ) {
      this.phenotype = phenotype;
   }

   public FactList getTreatment() {
      return getFactsOrInsert( FHIRConstants.HAS_TREATMENT );
   }

   public FactList getSequenceVariants() {
      return getFactsOrInsert( FHIRConstants.HAS_SEQUENCE_VARIENT );
   }

   public FactList getOutcome() {
      return getFactsOrInsert( FHIRConstants.HAS_OUTCOME );
   }

   public FactList getBodySite() {
      return getFactsOrInsert( FHIRConstants.HAS_BODY_SITE );
   }

   public FactList getDiagnosis() {
      return getFactsOrInsert( FHIRConstants.HAS_DIAGNOSIS );
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer( super.getSummaryText() );
      // add phenotype
      if ( getPhenotype() != null ) {
         st.append( getPhenotype().getSummaryText() );
         st.append( "\n" );
      }
      // append episode info
      if ( !getEpisodeMap().isEmpty() ) {
         st.append( "Episodes:\n" );
         for ( Episode ep : getEpisodes() ) {
            st.append( "\t" + ep.getSummaryText() );
         }
      }
      return st.toString();
   }

   public URI getConceptURI() {
      return conceptURI != null ? conceptURI : FHIRConstants.TUMOR_SUMMARY_URI;
   }


   public boolean isAppendable( Summary s ) {
      if ( s instanceof TumorSummary ) {
         // if no body site defined, assume they are the same
         if ( getBodySite().isEmpty() )
            return true;
         // else see if the body sites intersect
         TumorSummary ts = (TumorSummary) s;
         for ( Fact c : ts.getBodySite() ) {
            if ( FHIRUtils.contains( getBodySite(), c ) )
               return true;
         }

      }
      return false;
   }

   public void append( Summary s ) {
      TumorSummary summary = (TumorSummary) s;

      // add body site
      for ( String cat : summary.getFactCategories() ) {
         for ( Fact c : summary.getFacts( cat ) ) {
            if ( !FHIRUtils.contains( getFacts( cat ), c ) ) {
               addFact( cat, c );
            }
         }
      }

      // add phenotypes (worry about 1 for now)
      getPhenotype().append( summary.getPhenotype() );

   }

   public boolean isPrimary() {
      for ( Fact f : getFacts( FHIRConstants.HAS_TUMOR_TYPE ) ) {
         if ( FHIRConstants.PRIMARY_TUMOR.equals( f.getName() ) ) {
            return true;
         }
      }
      return false;
   }

}
