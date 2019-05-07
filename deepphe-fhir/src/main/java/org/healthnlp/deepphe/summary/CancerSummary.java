package org.healthnlp.deepphe.summary;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.summary.CiContainer;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.healthnlp.deepphe.fact.BodySiteFact;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactList;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.*;
import java.util.stream.Collectors;


public class CancerSummary extends Summary {

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private List<TumorSummary> tumors;

   public CancerSummary( final ConceptInstance conceptInstance ) {
      this(conceptInstance, "PT_NOT_SET"); // makes easy to find in the code
   }

   public CancerSummary( final ConceptInstance conceptInstance, String patient ) {
      this( UriUtil.getExtension( conceptInstance.getUri() ), patient );
      setConceptURI( conceptInstance.getUri() );
   }

   public CancerSummary( final String id ) {
      this(id, id);
   }

   public CancerSummary( final String id, String patient ) {
      super( id );
      setPatientIdentifier(patient);
   }

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
      return FHIRConstants.CANCER_SUMMARY_URI;
   }

   @Override
   public void setPatientIdentifier( final String patientId ) {
      super.setPatientIdentifier( patientId );
      getTumors().forEach( ts -> ts.setPatientIdentifier( patientId ) );
   }

   /**
    * @return all facts that are contained within this summary
    */
   public List<Fact> getContainedFacts() {
      final List<Fact> list = super.getContainedFacts();
      getTumors().stream()
                 .map( Summary::getContainedFacts )
                 .forEach( list::addAll );
      return list;
   }


   public FactList getBodySite() {
      return getOrCreateFacts( FHIRConstants.HAS_BODY_SITE );
   }

   public FactList getDiagnosis() {
      return getOrCreateFacts( FHIRConstants.HAS_DIAGNOSIS );
   }


   public FactList getTreatments() {
      return getOrCreateFacts( FHIRConstants.HAS_TREATMENT );
   }

   public FactList getOutcomes() {
      return getFacts( FHIRConstants.HAS_OUTCOME );
   }

   public List<TumorSummary> getTumors() {
      if ( tumors == null ) {
         tumors = new ArrayList<>();
      }
      return tumors;
   }

   public void addTumor( final TumorSummary tumor ) {
      tumor.setAnnotationType( getAnnotationType() );
      tumor.setCancerSummary( this );
      getTumors().add( tumor );
      
   }

   public void addTumorWithoutAddingFacts( final TumorSummary tumor ) {
      tumor.setAnnotationType( getAnnotationType() );
      if (!getTumors().contains(tumor)) getTumors().add( tumor );

   }

   // removeTumor method is used by Drools
   public void removeTumor( final TumorSummary tumor ) {

      if ( tumor != null && getTumors().size() > 0 ) {
         tumors.remove( tumor );
         getBodySite().removeAll( tumor.getBodySite() );
      }
   }

   // TODO SPF required ?  should have a getOrCreate name
   public TumorSummary getTumorSummaryByIdentifier( final String id ) {
      final TumorSummary tumorSummary = getTumors().stream()
                                                   .filter( ts -> ts.getId()
                                                                    .equals( id ) )
                                                   .findAny()
                                                   .orElse( null );
      if ( tumorSummary != null ) {
         return tumorSummary;
      }
      final TumorSummary newTumor = new TumorSummary( id );
      addTumor( newTumor );
      return newTumor;
   }


   // like the part of getSummaryText that is just for the cancer, not the tumor.
   // But just the facts, not the text
   @Override
   public Map<String, FactList> getSummaryFacts() {
      Map<String, FactList> m = super.getSummaryFacts();
      return m;
   }

   public String getSummaryText() {
      final StringBuilder sb = new StringBuilder( super.getSummaryText() );
      for ( TumorSummary tumorSummary : getTumors() ) {
         sb.append( FHIRConstants.LINE )
           .append( "\n" )
           .append( tumorSummary.getSummaryText() )
           .append( "\n" );
      }
      return sb.toString();
   }

   @Override
   public void append( final Summary summary ) {
      super.append( summary );
      CancerSummary cancerSummary = (CancerSummary)summary;
      cancerSummary.getTumors().forEach( this::appendTumorSummary );
   }

   /**
    * append tumor summary if possible
    *
    * @param tumorSummary -
    */
   public void appendTumorSummary( final TumorSummary tumorSummary ) {
      String id = resourceIdentifier
            + '-'
            + tumorSummary.getBodySite()
                          .stream()
                          .map( Fact::getName )
                          .collect( Collectors.joining( "-" ) );
      // add tumors if none exist
      if ( getTumors().isEmpty() ) {
         addTumor( new TumorSummary( id ) );
      }
      // go over existing tumors and append if possible
      boolean appended = false;
      for ( TumorSummary myTumorSummary : getTumors() ) {
         if ( myTumorSummary.isAppendable( tumorSummary ) ) {
            myTumorSummary.append( tumorSummary );
            appended = true;
         }
      }
      // if this tumor was not appended to existing tumors
      // add a new one
      if ( !appended ) {
         final TumorSummary newTumorSummary = new TumorSummary( id );
         newTumorSummary.append( tumorSummary );
         addTumor( newTumorSummary );
      }
   }

   public String getFullCancerIdentifier() {
      final StringBuilder sb = new StringBuilder( "cancer_" );
      sb.append( getPatientIdentifier() ).append( "_" )
        .append( getCancerBodySite() ).append( "_" );
      sb.append( getTemporality() );
      return sb.toString();
	}

   private String getCancerBodySite() {
      final StringBuilder siteSb = new StringBuilder();
      for ( Fact site : getBodySite() ) {
         // select only breasts
         if ( site instanceof BodySiteFact ) {
            final BodySiteFact location = (BodySiteFact)site;
            String side = "";
            if ( location.getBodySide() != null ) {
               side = location.getBodySide().getName();
            }
            siteSb.append( "_" ).append( side ).append( "_" ).append( location.getName() );
         }
      }
      if ( siteSb.length() == 0 ) {
         return "";
      }
      // select best site (since everything has an _ prefix, remove 1st character
      return siteSb.substring( 1, siteSb.length() );
   }

   public String getBodySide() {
//      final CiContainer ciContainer = getCiContainer();
//      if ( ciContainer != null ) {
//         final Collection<ConceptInstance> lateralities
//               = ciContainer.getRelations().get( RelationConstants.HAS_LATERALITY );
//         if ( lateralities != null ) {
//            return lateralities.stream()
//                               .map( ConceptInstance::getUri )
//                               .collect( Collectors.joining( ";" ) );
//         }
//      }
      for ( Fact site : getBodySite() ) {
         if ( site instanceof BodySiteFact ) {
            final BodySiteFact location = (BodySiteFact)site;
            if ( location.getBodySide() != null ) {
               return location.getBodySide().getName();
            }
         }
      }
      return getFacts( RelationConstants.HAS_LATERALITY ).stream()
                                                         .map( Fact::getUri )
                                                         .collect( Collectors.joining( ";" ) );
//      return "";
   }

   /**
    * @return true iff there are no facts in the content of this CancerSummary
    */
   public boolean isEmpty() {
      return getContent().values().stream().allMatch( FactList::isEmpty );
   }
   
   @Override
   public void cleanSummary() {
	      final FactList dfl = getDiagnosis();
	      // remove TumorSummaries without Diagnosis  or BodySite
	      Iterator<TumorSummary> it = tumors.iterator();
	      while (it.hasNext()) {
	    	  TumorSummary ts = it.next();
	          if (ts.getDiagnosis().size() == 0 || ts.getBodySite().size() == 0) {
	              it.remove();
	          }
	      }
	      
	      // remove the CancerSummary content if Cancer has no bodySites,
	      // BUT leave stage since can have a stage for an empty cancer container
	      if(getBodySite().size() == 0) {
	    	  FactList cStage = getFacts(FHIRConstants.HAS_CANCER_STAGE);
	    	  getContent().clear();
	    	  for(Fact f : cStage) {
                 addFact(FHIRConstants.HAS_CANCER_STAGE, f);
              }
	      }
	}


}
