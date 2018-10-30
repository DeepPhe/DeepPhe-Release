package org.healthnlp.deepphe.uima.drools;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.util.FHIRConstants;

public class Receptor {
	private Fact nameFact;
	private Set<Fact> methodFacts;
	
	private boolean fromPathologyReport = false;
	private boolean fromFISH = false;
	
	Date mentionDate;
	
	public Receptor(Fact nameFact, Set<Fact> methodFacts){
		this.nameFact = nameFact;
		addCorrectMethods(nameFact.getSummaryId(), methodFacts);
		setParams();
	}
	
	/**
	 * Extract only methods which have the same summary Id as  the receptor
	 * @param oldTumorSummaryId
	 * @param allMethods - methods for all old summaryId 
	 * @return
	 */
	private void addCorrectMethods(String oldTumorSummaryId, Set<Fact> allMethods){
		if(allMethods.size() > 0) {
			for(Fact mFact : allMethods) {
				if(mFact.getSummaryId().equals(oldTumorSummaryId))
					getMethods().add(mFact);
			}
		}
	}
	
	private void setParams(){
		mentionDate = (Date) nameFact.getRecordedDate();
		
		String docType = nameFact.getDocumentType();
		if(docType.equals(FHIRConstants.PATHOLOGY_REPORT) || docType.equals(FHIRConstants.SURGICAL_PATHOLOGY_REPORT))
			setFromPathologyReport(true);
		
		String method = getValue(FHIRConstants.HAS_METHOD);
		if(method != null && !method.startsWith("Immunohistochemical"))
			setFromFISH(true);
	}
	
	public Fact getNameFact(){
		return nameFact;
	}
	

	public Set<Fact> getMethods(){
		if(methodFacts == null)
			methodFacts = new HashSet<Fact>();
		return methodFacts;
	}
	

	public Date getMentionDate(){
		return mentionDate;
	}
	
	public void setFromPathologyReport(boolean b){
		fromPathologyReport = b;
	}
	
	public boolean isFromPathologyReport(){
		return fromPathologyReport;
	}
	
	public void setFromFISH(boolean b){
		fromFISH = b;	
	}
	
	public boolean isFromFISH(){
		return fromFISH;
	}

	
	public String getName(){
		if(nameFact != null)
			return nameFact.getName();
		else return null;
	}
	
	public String getValue(String switchStr){
		String toret = null;
		Set<Fact> curSet = null;
		switch(switchStr){
			case FHIRConstants.HAS_METHOD:
				curSet = methodFacts;
				break;		
		}
		if(curSet != null){
			boolean hasError = false;
			int pos = 0;
			for(Fact f: curSet){
				if(pos == 0)
					toret = f.getName();
				else{
					if(!hasError && !toret.equals(f.getName()))
						hasError = true;
				}
			}
			
			if(hasError){
				System.err.println("RECEPTOR FACT: "+ nameFact.getInfo()+" has different "+switchStr+":");
				for(Fact f: curSet){
					System.err.println(switchStr+" FACT: "+ f.getInfo());
				}
			}
		}
			
		return toret;
	}
	
}