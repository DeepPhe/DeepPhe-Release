package org.healthnlp.deepphe.fhir.fact;

import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConditionFact extends Fact {
   private FactList bodySite;
   private Set<String> relatedEvidenceIds;

   //private StageFact stage;
   public ConditionFact() {
      setType( FHIRConstants.CONDITION );
   }

   public FactList getBodySite() {
      if ( bodySite == null ) {
         bodySite = new DefaultFactList();
         bodySite.addType( FHIRConstants.BODY_SITE );
      }
      return bodySite;
   }

   public void setBodySite( FactList bodySite ) {
      this.bodySite = bodySite;
   }

   /*public StageFact getStage() {
      return stage;
   }
   public void setStage(StageFact stage) {
      this.stage = stage;
   }*/
   /*public List<Fact> getContainedFacts(){
      List<Fact> facts = new ArrayList<Fact>();
		if(stage != null){
			addContainedFact(facts,stage);
		}
		for(Fact fact: getBodySite()){
			addContainedFact(facts, fact);
		}
		return facts;
	}*/
   public String getSummaryText() {
      StringBuffer b = new StringBuffer( getLabel() );
      if ( !getBodySite().isEmpty() )
         b.append( " | location: " + getBodySite() );
      //if(getStage() != null)
      //	b.append(" | stage: "+getStage().getSummaryText());
      return b.toString();
   }

   public void append( Fact fact ) {
      if ( fact instanceof ConditionFact ) {
         super.append( fact );

         ConditionFact cfact = (ConditionFact) fact;
         //get body sites
         for ( Fact bf : cfact.getBodySite() ) {
            if ( !FactHelper.contains( getBodySite(), bf ) ) {
               getBodySite().add( bf );
            }
         }
         // set stage
			/*if(getStage() == null)
				setStage(cfact.getStage());
			else if(cfact.getStage() !=null)
				getStage().append(cfact.getStage());*/
      }
   }

   public void addRelatedEvidnceId( String id ) {
      getRelatedEvidenceIds().add( id );
   }

   public Set<String> getRelatedEvidenceIds() {
      if ( relatedEvidenceIds == null )
         relatedEvidenceIds = new LinkedHashSet<String>();
      return relatedEvidenceIds;
   }

   public void setRelatedEvidenceIds( Set<String> relatedEvidenceIds ) {
      this.relatedEvidenceIds = relatedEvidenceIds;
   }

   public Map<String, String> getProperties() {
      Map<String, String> map = super.getProperties();
      if ( !getRelatedEvidenceIds().isEmpty() ) {
         map.put( FHIRUtils.EVIDENCE, getRelatedEvidenceIds().toString() );
      }
      return map;
   }

   public void setProperties( Map<String, String> properties ) {
      super.setProperties( properties );
      if ( properties.containsKey( FHIRUtils.EVIDENCE ) ) {
         String str = properties.get( FHIRUtils.EVIDENCE );
         if ( str.startsWith( "[" ) && str.endsWith( "]" ) )
            str = str.substring( 1, str.length() - 1 );
         for ( String id : str.split( "," ) ) {
            addRelatedEvidnceId( id.trim() );
         }
      }
   }


}
