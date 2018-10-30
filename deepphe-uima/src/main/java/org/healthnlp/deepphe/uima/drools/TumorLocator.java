package org.healthnlp.deepphe.uima.drools;

import org.healthnlp.deepphe.fact.Fact;

import java.util.Collection;
import java.util.LinkedHashSet;

public class TumorLocator {
	private String bodySite="", docTumorId, docType, recordId, bodySide="", quadrant="", clockFacePos="";
	private String histologicType = "", summaryId;
	private Fact tumorSiteFact = null, bodySideFact = null, quadrantFact = null, clockfacePosFact = null;
	private Collection<String> bodySiteAncestors = null;
	private Collection<String>rulesApplied = null; 
	
	private boolean readyForRetraction = false;
	
	public Collection<String> getBodySiteAncestors() {
		if(bodySiteAncestors == null)
			bodySiteAncestors = new LinkedHashSet<String>();
		return bodySiteAncestors;
	}
	
	public Collection<String> getRulesApplied() {
	      if ( rulesApplied == null ) {
         rulesApplied = new LinkedHashSet<>();
      }
      return rulesApplied;
   }

   public void addRulesApplied( final String ruleName ) {
      getRulesApplied().add( ruleName );
   }

	
	public void setBodySiteAncestors(Collection<String> collection){
		getBodySiteAncestors().addAll(collection);
	}

	public String getBodySite() {
		return bodySite;
	}
	
	public void setBodySiteFromDrools(String bodySite) {		
		if(bodySite.toLowerCase().indexOf("left_") != -1) {
			setBodySide("Left");
			this.bodySite = bodySite.replace("Left_", "");
		}else if(bodySite.toLowerCase().indexOf("right_") != -1) {
			setBodySide("Right");
			this.bodySite = bodySite.replace("Right_", "");
		} else
			this.bodySite = bodySite;
	}
	public String getDocTumorId() {
		return docTumorId;
	}
	public void setDocTumorId(String docTumorId) {
		this.docTumorId = docTumorId;
	}
	public String getDocType() {
		return docType;
	}
	public void setDocType(String docType) {
		this.docType = docType;
	}	
	public String getRecordId() {
		return recordId;
	}
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}
	public String getBodySide() {
		return bodySide;
	}
	public void setBodySide(String bodySide) {
		this.bodySide = bodySide;
	}
	public String getQuadrant() {
		return quadrant;
	}
	public void setQuadrant(String quadrant) {
		this.quadrant = quadrant;
	}
	public String getClockFacePos() {
		return clockFacePos;
	}
	public void setClockFacePos(String clockFacePos) {
		this.clockFacePos = clockFacePos;
	}
	
	public String getHistologicType() {
		return histologicType;
	}
	public void setHistologicType(String histologicType) {
		this.histologicType = histologicType;
	}
	
	public String getInfo(){
		StringBuffer b = new StringBuffer("\n");	
		b.append("bodySite: "+getBodySite()+"|");
		b.append("Laterality: "+getBodySide()+"|");
		b.append("Quadrant: "+getQuadrant()+"|");
		b.append("clockFacePos: "+getClockFacePos()+"|");
		b.append("histologicType: "+getHistologicType()+"|");
		b.append("docType: "+getDocType()+"|");
		b.append("docTumorId: "+getDocTumorId()+"|");
		b.append("hashcode: "+hashCode()+"|");
		b.append("readyForRetraction: "+readyForRetraction+"\n");
		b.append("tumorSiTTFact: "+tumorSiteFact.getInfo()+"\n");
		if(bodySideFact != null)
			b.append("bodySideFact: "+bodySideFact.getInfo()+"\n");
		if(quadrantFact != null)
			b.append("quadrantFact: "+quadrantFact.getInfo()+"\n");
		if(clockfacePosFact != null)
			b.append("clockfacePosFact: "+clockfacePosFact.getInfo()+"\n");
		return b.toString();
	}
	
	
	public Fact getTumorSiteFact() {
		return tumorSiteFact;
	}
	public void setTumorSiteFact(Fact tumorSiteFact) {
		this.tumorSiteFact = tumorSiteFact;
		setSummaryId(tumorSiteFact.getSummaryId());
		setBodySiteAncestors(tumorSiteFact.getAncestors());
	}
	
	
	public String getSummaryId() {
		return summaryId;
	}
	public void setSummaryId(String summaryId) {
		this.summaryId = summaryId;
	}
	
	public Fact getBodySideFact() {
		return bodySideFact;
	}
	public void setBodySideFact(Fact bodySideFact) {
		this.bodySideFact = bodySideFact;
	}
	public Fact getQuadrantFact() {
		return quadrantFact;
	}
	public void setQuadrantFact(Fact quadrantFact) {
		this.quadrantFact = quadrantFact;
	}
	public Fact getClockfacePosFact() {
		return clockfacePosFact;
	}
	public void setClockfacePosFact(Fact clockfacePosFact) {
		this.clockfacePosFact = clockfacePosFact;
	}
	
	public boolean compareTo(TumorLocator otherTL){
		if(bodySite.equals(otherTL.getBodySite()) && bodySide.equals(otherTL.getBodySide()) && 
				quadrant.equals(otherTL.getQuadrant()) && clockFacePos.equals(otherTL.getClockFacePos()) &&
				bodySideFact == otherTL.getBodySideFact() && quadrantFact == otherTL.getQuadrantFact() && clockfacePosFact == otherTL.getClockfacePosFact())
		return true;
		return false;
			
			
	}
	public boolean isReadyForRetraction() {
		return readyForRetraction;
	}
	public void setReadyForRetraction(boolean readyForRetraction) {
		this.readyForRetraction = readyForRetraction;
	}
	
	/**
	 * Check and assigns ONE quadrant for any clockface position, including 12, 3, 6, 9 o'clock.
	 * @param bodySide
	 * @param clockfacePos
	 * @return
	 */
	public static String normalizeQuadrant(String bodySide, String clockfacePos){
		String toret = "";
		float clockfacePosNum = 0;
		try{
			clockfacePosNum = Float.valueOf(clockfacePos.substring(0, clockfacePos.indexOf("_")));
			
		} catch (NumberFormatException e){
			return toret;
		}
		if(bodySide.equalsIgnoreCase("LEFT")){
			if(clockfacePosNum < 3)
				toret = "Upper_Outer_Quadrant_of_the_Breast";
			else if (clockfacePosNum == 3)
				toret = "Lower_Outer_Quadrant_of_the_Breast";
			else if(clockfacePosNum > 3 && clockfacePosNum < 6)
				toret = "Lower_Outer_Quadrant_of_the_Breast";
			else if (clockfacePosNum == 6)
				toret = "Lower_Inner_Quadrant_of_the_Breast";
			else if(clockfacePosNum > 6 && clockfacePosNum < 9)
				toret = "Lower_Inner_Quadrant_of_the_Breast";
			else if (clockfacePosNum == 9)
				toret = "Upper_Inner_Quadrant_of_the_Breast";
			else if(clockfacePosNum > 9 && clockfacePosNum < 12)
				toret = "Upper_Inner_Quadrant_of_the_Breast";
			else if(clockfacePosNum == 12)
				toret = "Upper_Outer_Quadrant_of_the_Breast";
		}
		else if(bodySide.equalsIgnoreCase("RIGHT")){
			if(clockfacePosNum < 3)
				toret = "Upper_Inner_Quadrant_of_the_Breast";
			else if (clockfacePosNum == 3)
				toret = "Lower_Inner_Quadrant_of_the_Breast";
			else if(clockfacePosNum > 3 && clockfacePosNum < 6)
				toret = "Lower_Inner_Quadrant_of_the_Breast";
			else if (clockfacePosNum == 6)
				toret = "Lower_Outer_Quadrant_of_the_Breast";
			else if(clockfacePosNum > 6 && clockfacePosNum < 9)
				toret = "Lower_Outer_Quadrant_of_the_Breast";
			else if (clockfacePosNum == 9)
				toret = "Upper_Outer_Quadrant_of_the_Breast";
			else if(clockfacePosNum > 9 && clockfacePosNum <= 12)
				toret = "Upper_Outer_Quadrant_of_the_Breast";
			else if(clockfacePosNum == 12)
				toret = "Upper_Inner_Quadrant_of_the_Breast";
		}
		return toret;
	}
	
	public static boolean clockFactInQuadrant(String bodySide, String clockfacePos, String quadrant){
		float clockfacePosNum = 0;
		try{
			clockfacePosNum = Float.valueOf(clockfacePos.substring(0, clockfacePos.indexOf("_")));
			
		} catch (NumberFormatException e){
			return false;
		}
		
		if(bodySide.equalsIgnoreCase("RIGHT")){
			if(clockfacePosNum < 3 && quadrant.equals("Upper_Inner_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 3 && (quadrant.equals("Upper_Inner_Quadrant_of_the_Breast") || quadrant.equals("Lower_Inner_Quadrant_of_the_Breast")))
				return true;
			else if(clockfacePosNum > 3 && clockfacePosNum < 6 && quadrant.equals("Lower_Inner_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 6 && (quadrant.equals("Lower_Inner_Quadrant_of_the_Breast") || quadrant.equals("Lower_Outer_Quadrant_of_the_Breast")))
				return true;
			else if(clockfacePosNum > 6 && clockfacePosNum < 9 && quadrant.equals("Lower_Outer_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 9 && (quadrant.equals("Lower_Outer_Quadrant_of_the_Breast") || quadrant.equals("Upper_Outer_Quadrant_of_the_Breast")))
				return true;
			else if(clockfacePosNum > 9 && clockfacePosNum < 12 && quadrant.equals("Upper_Outer_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 12 && (quadrant.equals("Upper_Outer_Quadrant_of_the_Breast") || quadrant.equals("Upper_Inner_Quadrant_of_the_Breast")))
				return true;
		}
		else if(bodySide.equalsIgnoreCase("LEFT")){
			if(clockfacePosNum < 3 && quadrant.equals("Upper_Outer_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 3 && (quadrant.equals("Upper_Outer_Quadrant_of_the_Breast") || quadrant.equals("Lower_Outer_Quadrant_of_the_Breast")))
				return true;
			else if(clockfacePosNum > 3 && clockfacePosNum < 6 && quadrant.equals("Lower_Outer_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 6 && (quadrant.equals("Lower_Outer_Quadrant_of_the_Breast") || quadrant.equals("Lower_Inner_Quadrant_of_the_Breast")))
				return true;
			else if(clockfacePosNum > 6 && clockfacePosNum < 9 && quadrant.equals("Lower_Inner_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 9 && (quadrant.equals("Lower_Inner_Quadrant_of_the_Breast") || quadrant.equals("Upper_Inner_Quadrant_of_the_Breast")))
				return true;
			else if(clockfacePosNum > 9 && clockfacePosNum < 12 && quadrant.equals("Upper_Inner_Quadrant_of_the_Breast"))
				return true;
			else if (clockfacePosNum == 12 && (quadrant.equals("Upper_Inner_Quadrant_of_the_Breast") || quadrant.equals("Upper_Outer_Quadrant_of_the_Breast")))
				return true;
		}
		
		return false;
		
	}

}
