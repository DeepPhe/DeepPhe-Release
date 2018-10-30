package org.healthnlp.deepphe.summary;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.fact.BodySiteOwner;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.util.FHIRConstants.CLINICAL_NOTE;

public class TumorSummary extends MultiNoteSummary implements EpisodeOwner {

   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private CancerSummary cancerSummary;

   private FactList tumorType;
   private Map<String, EpisodeSummary> episodes;

   /**
    * Use the mainFact in creating the Id of the TumorSummary
    * @param conceptInstance
    * @param mainFact
    */
   public TumorSummary(final ConceptInstance conceptInstance, Fact mainFact) {
      this(mainFact.getDocumentIdentifier() + "_" +
              mainFact.getUri().replace(' ', '_') +
              (mainFact instanceof BodySiteOwner ? ("_" + (((BodySiteOwner) mainFact).getBodySite()).stream().map(Fact::toString).collect(Collectors.joining("_")).replace(' ', '_')) : "")
      );
      setConceptURI( conceptInstance.getUri() );
   }


   public TumorSummary( final String id ) { super( id ); }


   /**
    * {@inheritDoc}
    */
   @Override
   protected long createUniqueIdNum() {
      synchronized ( ID_NUM_LOCK ) {
         _ID_NUM++;
         return _ID_NUM;
      }
   }

   @Override
   protected String getDefaultUri() {
      return FHIRConstants.TUMOR_SUMMARY_URI;
   }

   public boolean isPrimary() {
      for ( Fact f : getFacts( FHIRConstants.HAS_TUMOR_TYPE ) ) {
         if ( FHIRConstants.PRIMARY_TUMOR.equals( f.getName() ) ) {
            return true;
         }
      }
      return false;
   }

   @Override
   public Map<String, EpisodeSummary> getEpisodeMap() {
      if ( episodes == null ) {
         episodes = new HashMap<>();
      }
      return episodes;
   }

   /**
    * add episode to the tumor summary
    *
    * @param episode -
    */
   @Override
   public void addEpisode( final EpisodeSummary episode ) {
      EpisodeSummary myEpisode = getEpisodeMap().get( episode.getType() );
      if ( myEpisode == null ) {
         myEpisode = new EpisodeSummary( getObjectId() + "_" + episode.getType() );
         myEpisode.setType( episode.getType() );
         myEpisode.setTumorSummary( this );
         getEpisodeMap().put( myEpisode.getType(), myEpisode );
      }
      myEpisode.append( episode );
   }

   public CancerSummary getCancerSummary() {
      return cancerSummary;
   }


   public void setCancerSummary( final CancerSummary cancerSummary ) {

      this.cancerSummary = cancerSummary;

      // For each FactList (getTreatment  getSequenceVariants getOutcome getBodySite getDiagnosis)
      // add the CancerSummary's container to each fact
      for (Fact f: getContainedFacts()) {
         f.addContainerIdentifier(cancerSummary.getId());
      }

   }


   public FactList getTumorType() {
      if ( tumorType == null ) {
         tumorType = getFacts( FHIRConstants.HAS_TUMOR_TYPE );
      }
      return tumorType;
   }

   public void setTumorType( final FactList tumorType ) {
      this.tumorType = tumorType;
   }

   public FactList getTreatment() {
      return getOrCreateFacts( FHIRConstants.HAS_TREATMENT );
   }

   public FactList getSequenceVariants() {
      return getOrCreateFacts( FHIRConstants.HAS_SEQUENCE_VARIENT );
   }

   public FactList getOutcome() {
      return getOrCreateFacts( FHIRConstants.HAS_OUTCOME );
   }

   public FactList getBodySite() {
      return getOrCreateFacts( FHIRConstants.HAS_BODY_SITE );
   }

   public FactList getDiagnosis() {
      return getOrCreateFacts( FHIRConstants.HAS_DIAGNOSIS );
   }

   @Override
   public Map<String, FactList> getSummaryFacts() {
      Map<String, FactList> indexedFacts = super.getSummaryFacts();
      // append facts from episode(s)
      if ( !getEpisodeMap().isEmpty() ) {
         for ( EpisodeSummary ep : getEpisodes() ) {
            for (Map.Entry<String, FactList> episodeEntry: ep.getSummaryFacts().entrySet()) {
               String category = episodeEntry.getKey();
               FactList fl = indexedFacts.get(category);
               for (Fact f: episodeEntry.getValue()) { // for each fact in this category
                  if (!fl.contains(f)) {
                     fl.add(f);
                  }
               }
            }
         }
      }
      return indexedFacts;
   }

   public String getSummaryText() {
      final StringBuilder sb = new StringBuilder( super.getSummaryText() );
      // append episode info
      if ( !getEpisodeMap().isEmpty() ) {
         sb.append( "Episodes:\n" );
         for ( EpisodeSummary ep : getEpisodes() ) {
            sb.append( "\t" )
              .append( ep.getSummaryText() );
         }
      }
      return sb.toString();
   }

   @Override
   public boolean isAppendable( final Summary summary ) {
      if ( !TumorSummary.class.isInstance( summary ) ) {
         return false;
      }
      // if no body site defined, assume they are the same
      final FactList bodySites = getBodySite();
      if ( bodySites.isEmpty() || ((TumorSummary)summary).getBodySite().isEmpty() ) {
         return true;
      }
      // else see if the body sites intersect
      return bodySites.intersects( ((TumorSummary)summary).getBodySite() );
   }

   public void tagAsTumorSummaryFact(Fact fact) {
      fact.setDocumentType(CLINICAL_NOTE);

      // If it's already tagged as a tumor summary, don't change which tumor summary it is tagged as part of
      if (TumorSummary.class.getSimpleName().equals(fact.getSummaryType())) return;
      fact.setSummaryId(this.getId());
      fact.setSummaryType(this.getSummaryType());
   }

   // return something such as cancer_patientx_Left_Breast_Current
   public String getFullTumorIdentifier() {
      String patient = getPatientIdentifier();
      if (patient==null) {
         CancerSummary cs = this.getCancerSummary();
         if (cs!=null && cs.getPatientIdentifier()!=null) {
            patient = cs.getPatientIdentifier();
         } else {
            patient = "patientABCXYZ"; // set a value that's easily searchable and that won't cause NPE so can produce output to help debug
         }
      }
      String result = "cancer_" + patient ;

      result = result + "_" + getBodySite().stream().map(Fact::toString).collect(Collectors.joining("_"));
      result = result + "_" + getTemporality();
      return result;
   }

	@Override
	public void cleanSummary() {
       // Nothing to clean for Tumors
    }

}