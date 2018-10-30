package org.healthnlp.deepphe.uima.drools;

import org.healthnlp.deepphe.fact.BodySiteFact;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactFactory;
import org.healthnlp.deepphe.fact.ObservationFact;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.*;

/**
 * Represents tumors, merged across all documents. Used in drools only
 * @author opm1
 *
 */
public class MergedTumor {
	private String bodySite="", mergedTumorId, bodySide="", quadrant="", clockFacePos = "", histologicType = "";
	private String tumorType = "";
	private Set<Fact> tumorSiteFactSet, bodySideFactSet, quadrantFactSet, clockfacePosFactSet;
	private Set<Fact> diagnosisFactSet, sizeFactSet;
	private Set<Receptor> prStatus, erStatus, her2Status;
	private Collection<String> quadrantNamesSet, clockFacePosNamesSet;
	private Set<String> tumorSummaryIdSet;
	private Map<String, List<String>> documentTypeMap = new HashMap<String, List<String>>(); //<docType, List of docID>
	private boolean readyForRetraction = false;
	private boolean fromPathologyReport = false;
	private boolean gotStatistics = false;
	private boolean checkedForPR = false;
	private boolean lymphNodeSite = false;
	private Collection<String>rulesApplied = null;
	
	
	
	
	public MergedTumor(){
		setMergedTumorId("MergedTumor-"+hashCode());
		tumorSiteFactSet = new HashSet<>();
		bodySideFactSet = new HashSet<>();
		quadrantFactSet = new HashSet<>();
		clockfacePosFactSet = new HashSet<>();
		diagnosisFactSet = new HashSet<>();
		sizeFactSet = new HashSet<>();
		setTumorSummaryIdSet( new HashSet<>() );
		quadrantNamesSet = new HashSet<>();
		clockFacePosNamesSet = new HashSet<>();

		prStatus = new HashSet<>();
		erStatus = new HashSet<>();
		her2Status = new HashSet<>();
	}
	
	public String getTumorSummaryID() {
		return FHIRConstants.TUMOR_SUMMARY +"-"+getMergedTumorId();
	}
	
	public Collection<String> getRulesApplied() {
		if ( rulesApplied == null ) 
		rulesApplied = new LinkedHashSet<>();
    return rulesApplied;
	}

	 public void addRulesApplied( final String ruleName ) {
	    getRulesApplied().add( ruleName );
	 }
	
	
	public String getReceptorName(Fact recepFact) {
		String toret = null;
		toret = recepFact.getName();
		toret = toret.substring(0, toret.lastIndexOf("_"));
		return toret;
	}
	
	
	/*
	 * Now each interpSet has <STATUS>_NALUE, i.e: Progesterone_Receptor_Negative
	 */
	public void addReceptorStatus(Fact recepFact, Set<Fact> methodSet){
		
		String receptorStatusName = getReceptorName(recepFact);
		Set<Receptor> curSet = getCurrentSet(receptorStatusName);

		if(curSet != null){
			Receptor receptor = new Receptor(recepFact, methodSet);
			curSet.add(receptor);
		}
	}
	
	public Set<Receptor> getErStatus(){
		return erStatus;
	}
	
	
	
	public Set<Receptor> getCurrentSet(String switchStr){
		switch (switchStr) {
			case FHIRConstants.ER_STATUS:
				return erStatus;
			case FHIRConstants.PR_STATUS:
				return prStatus;
			case FHIRConstants.HER2_STATUS:
				return her2Status;
		}
		return new HashSet<>();
	}
	
/**
 * Al Receptor rules are here
 * @param switchStr - receptor status name
 * @return
 */
	public Receptor getBestReceptor(String switchStr){
		Set<Receptor> curSet = getCurrentSet(switchStr);
		// get most recent FISH from Pathology report
		Receptor curReceptor = getMostRecentPathologyRS_FISH(curSet);
		// get most recent pathology RS
		if(curReceptor == null)
			curReceptor = getMostRecentPathologyRS(curSet);
		// get most recent from NOT PR with FISH
		if(curReceptor == null)
			curReceptor = getMostRecentNotPathologyRS_FISH(curSet);
		// just most recent not from PR
		if(curReceptor == null)
			curReceptor = getMostRecentNotPathologyRS(curSet);
		return curReceptor;
		
	}
	
	
	public Fact getTumorSummaryReceptor(String statusName, String receptorName, Fact cancerFact){
		receptorName = receptorName.substring(0, receptorName.lastIndexOf("_"));
		Receptor receptor = getBestReceptor(receptorName);
		Fact tf = null;
		if(receptor != null){
			Fact recFact = receptor.getNameFact();			
			String newId = FHIRConstants.TUMOR_SUMMARY +"-"+getMergedTumorId();
			tf = FactFactory.createFact(recFact, FHIRConstants.OBSERVATION, recFact.getUri(), FHIRConstants.RECORD_SUMMARY);
			tf.addContainerIdentifier(FHIRConstants.TUMOR_SUMMARY +"-"+getMergedTumorId());
			tf.setIdentifier(tf.getName()+"-"+newId);
			tf.setSummaryId(newId);
			tf.setSummaryType(FHIRConstants.TUMOR_SUMMARY);
			tf.setCategory(statusName);
			tf.addContainerIdentifier(cancerFact.getSummaryId());
			tf.addProvenanceFact(recFact);
					
			//method
			Set<Fact> methods = receptor.getMethods();
			if(methods.size() > 0){
				Fact mFact = FactFactory.createTumorFactModifier(methods.iterator().next().getUri(), tf, 
																cancerFact, FHIRConstants.TUMOR_SUMMARY, FHIRConstants.HAS_METHOD, 
																FHIRConstants.RECORD_SUMMARY, FHIRConstants.DIAGNOSTIC_PROCEDURE);
				mFact.addContainerIdentifier(tf.getSummaryId());
				mFact.setSummaryId(tf.getSummaryId());
	
				((ObservationFact)tf).setMethod(mFact);
				
				for(Fact f : methods)
					mFact.addProvenanceFact(f);
			}
		}
		return tf;
		
	}
	
	public Receptor getMostRecentPathologyRS(Set<Receptor> curSet){
		Receptor toret = null;
		int pos = 0;
		for(Receptor r: curSet){
			if(pos == 0 && r.isFromPathologyReport()){
				toret = r;
				pos ++;
			}
			else if(pos >0 && toret != null && r.getMentionDate() != null){
				if(r.isFromPathologyReport() && toret.getMentionDate().before(r.getMentionDate()))
					toret = r;
			}
		}
		return toret;
	}
	
	public Receptor getMostRecentPathologyRS_FISH(Set<Receptor> curSet){
		Receptor toret = null;
		int pos = 0;
		for(Receptor r: curSet){
			if(pos == 0 && r.isFromPathologyReport() && r.isFromFISH()){
				toret = r;
				pos ++;
			}
			else if(pos >0 && toret != null && r.getMentionDate() != null){
				if(r.isFromPathologyReport() && r.isFromFISH() && toret.getMentionDate().before(r.getMentionDate()))
					toret = r;
			}
		}
		return toret;
	}
	
	public Receptor getMostRecentNotPathologyRS_FISH(Set<Receptor> curSet){
		Receptor toret = null;
		int pos = 0;
		for(Receptor r: curSet){
			if(pos == 0 && !r.isFromPathologyReport() && r.isFromFISH()){
				toret = r;
				pos ++;
			}
			else if(pos >0 && toret != null && r.getMentionDate() != null){
				if(!r.isFromPathologyReport() && r.isFromFISH() && toret.getMentionDate().before(r.getMentionDate()))
					toret = r;
			}
		}
		return toret;
	}
	
	public Receptor getMostRecentNotPathologyRS(Set<Receptor> curSet){
		Receptor toret = null;
		int pos = 0;
		for(Receptor r: curSet){
			if(pos == 0 && !r.isFromPathologyReport()){
				toret = r;
				pos ++;
			}
			else if(pos >0 && toret != null && r.getMentionDate() != null){
				if(!r.isFromPathologyReport() && toret.getMentionDate().before(r.getMentionDate())) {
					toret = r;
				}
			}
		}
		return toret;
	}
	
	public void addToDocumentTypeMap(Set<Fact> facts){
		for(Fact f:facts){
			addToDocumentTypeMap(f.getDocumentType(), f.getDocumentIdentifier());
		}
	}
		
	public void addToDocumentTypeMap(String docType, String docId){
		List<String> docList = documentTypeMap.get(docType);
		if(docList == null){
			docList = new ArrayList<String>();
			documentTypeMap.put(docType, docList);
			if(docType.equals(FHIRConstants.PATHOLOGY_REPORT) || docType.equals(FHIRConstants.SURGICAL_PATHOLOGY_REPORT))
				setFromPathologyReport(true);
		}
		if(!docList.contains(docId))
			docList.add(docId);	
	}
	
	public Map<String, List<String>> getDocumentTypeMap(){
		return documentTypeMap;
	}
	
	public String getBodySite() {
		return bodySite;
	}
	public void setBodySite(String bodySite) {
		this.bodySite = bodySite;
	}
	public String getMergedTumorId() {
		return mergedTumorId;
	}
	public void setMergedTumorId(String mergedTumorId) {
		this.mergedTumorId = mergedTumorId;
	}
	
	
	
	public String getTumorType() {
		return tumorType;
	}
	public void setTumorType(String bodytumorTypeSide) {
		this.tumorType = tumorType;
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
	
	public Collection<String> getClockFacePosNamesSet() {
		return clockFacePosNamesSet;
	}
	public void addClockFacePos(String cFacePos) {
		clockFacePosNamesSet.add(cFacePos);
	}
	
	public Set<Fact> getSizeFactSet() {
		return sizeFactSet;
	}
	
	public void setSizeFactSet(Set<Fact> sizeFactSet) {
		this.sizeFactSet = sizeFactSet;
	}
	
	public Set<Fact> getDiagnosisFactSet() {
		return diagnosisFactSet;
	}
	
	public void setDiagnosisFactSet(Set<Fact> diagnosisFactSet) {
		this.diagnosisFactSet = diagnosisFactSet;
	}

	public Set<Fact> getTumorSiteFactSet() {
		return tumorSiteFactSet;
	}
	
	public void setTumorSiteFactSet(Set<Fact> tumorSiteFact) {
		this.tumorSiteFactSet = tumorSiteFact;
		addToDocumentTypeMap(tumorSiteFact);
	}
	
	public Set<Fact> getBodySideFactSet() {
		return bodySideFactSet;
	}
	
	public void setBodySideFactSet(Set<Fact> bodySideFact) {
		this.bodySideFactSet = bodySideFact;
		addToDocumentTypeMap(bodySideFact);
	}
	
	public Set<Fact> getQuadrantFactSet() {
		return quadrantFactSet;
	}
	
	public void addQuadrantFactSet(Fact f) {
		quadrantFactSet.add(f);
		quadrantNamesSet.add(f.getName());
	}
	
	public void setQuadrantFactSet(Set<Fact> quadrantFact) {
		this.quadrantFactSet = quadrantFact;
		for(Fact f: quadrantFactSet) 
			quadrantNamesSet.add(f.getName());
		addToDocumentTypeMap(quadrantFact);
	}
	
	public Collection<String>getQuadrantNamesSet(){
		return quadrantNamesSet;
	}
	
	public Set<Fact> getClockfacePosFactSet() {
		return clockfacePosFactSet;
	}
	
	public void addClockFacePosNamesSet(Fact f) {
		clockfacePosFactSet.add(f);
		setClockFacePos(f.getName()); 
		clockFacePosNamesSet.add(f.getName());
	}
	
	public void setClockfacePosFactSet(Set<Fact> clockfacePosFact) {
		this.clockfacePosFactSet = clockfacePosFact;

		for(Fact f: clockfacePosFact) {
			setClockFacePos(f.getName()); 
			clockFacePosNamesSet.add(f.getName());
			 
		}
		addToDocumentTypeMap(clockfacePosFact);
	}
	
	public String getClockFacePos() {
		return clockFacePos;
	}
	
	public void setClockFacePos(String s) {
		clockFacePos = s;
	}
	

	public String getHistologicType() {
		return histologicType;
	}

	public void setHistologicType(String histologicType) {
		this.histologicType = histologicType;
	}
	
	public void addTumorFact(String setId, Fact f) {
		addToDocumentTypeMap(f.getDocumentType(), f.getDocumentIdentifier());
		
		tumorSummaryIdSet.add(f.getSummaryId());
		switch (setId){
		case FHIRConstants.BODY_SITE:
			tumorSiteFactSet.add(f);
			checkIsLymphNode(f);
			break;
		case FHIRConstants.LATERALITY:
			bodySideFactSet.add(f);
			break;
		case FHIRConstants.QUADRANT:
			addQuadrantFactSet(f);
			break;
		case FHIRConstants.CLOCKFACE_POSITION:
			addClockFacePosNamesSet(f);
			break;
		}
	}
	
	public String getInfo(){
		StringBuffer b = new StringBuffer();
		b.append("mergedTumorId: "+mergedTumorId+"|");
		b.append("histologicType: "+getHistologicType()+"|");
		b.append("bodySite: "+getBodySite()+"|");
		b.append("Laterality: "+getBodySide()+"|");
		b.append("isLymphNode: "+isLymphNodeSite()+"|");	
		b.append("Quadrant: "+getQuadrantNamesSet().toString()+"|");
		b.append("clockFacePos: "+getClockFacePosNamesSet().toString()+"\n");
		
		b.append("Doc TumorSummaryIds: ");
		for(String s : tumorSummaryIdSet){
			b.append(s+", ");
		}
		b.append("\n");
		if(bodySideFactSet != null)
			b.append("bodySideFacts: "+bodySideFactSet.size()+"\n");
		if(quadrantFactSet != null)
			b.append("quadrantFacts: "+quadrantFactSet.size()+"\n");
		if(clockfacePosFactSet != null)
			b.append("clockfacePosFacts: "+clockfacePosFactSet.size()+"\n");
		return b.toString();
	}
	
	public String toString(){
		return getInfo();
	}

	public boolean isReadyForRetraction() {
		return readyForRetraction;
	}

	public void setReadyForRetraction(boolean readyForRetraction) {
		this.readyForRetraction = readyForRetraction;
	}

	public Set<String> getTumorSummaryIdSet() {
		return tumorSummaryIdSet;
	}

	public void setTumorSummaryIdSet(Set<String> tumorSummaryIdSet) {
		this.tumorSummaryIdSet = tumorSummaryIdSet;
	}

	public boolean isFromPathologyReport() {
		return fromPathologyReport;
	}

	public void setFromPathologyReport(boolean fromPathologyReport) {
		this.fromPathologyReport = fromPathologyReport;
	}
	
	public String getDocStatistics(){
		StringBuffer b = new StringBuffer();
		b.append("\n=====================\n");
		b.append("Tumor ID: "+mergedTumorId+"\n");
		b.append("To be removed?: "+!fromPathologyReport+"\n");
		b.append("BodySite: "+bodySite+"|");
		b.append("Laterality: "+bodySide+"\n");
		String[] docTypes = {FHIRConstants.PATHOLOGY_REPORT, FHIRConstants.SURGICAL_PATHOLOGY_REPORT, FHIRConstants.RADIOLOGY_REPORT, FHIRConstants.PROGRESS_NOTE_REPORT};
		int count = 0;
		for(String dt: docTypes){
			try{
				count = documentTypeMap.get(dt).size();
			} catch (NullPointerException e){
				count = 0;
			}
			b.append(dt+": "+count+"|");
		}
		b.append("\n=====================\n");
		return b.toString();
		
	}

	public boolean isGotStatistics() {
		return gotStatistics;
	}

	public void setGotStatistics(boolean gotStatistics) {
		this.gotStatistics = gotStatistics;
	}
	
	public void checkIsLymphNode(Fact siteF) {
		if(!lymphNodeSite && (siteF.getName().equalsIgnoreCase("Lymph_Node") || siteF.getAncestors().contains("Lymph_Node")))
			lymphNodeSite = true;
	}
	
	public boolean isLymphNodeSite() {
		return lymphNodeSite;
	}

	public void setLymphNodeSite(boolean lymphNodeSite) {
		this.lymphNodeSite = lymphNodeSite;
	}

	public boolean isCheckedForPR() {
		return checkedForPR;
	}

	public void setCheckedForPR(boolean checkedForPR) {
		this.checkedForPR = checkedForPR;
	}

	public void addBodyModifiersToRecordFact(BodySiteFact tumorF, Fact cancerF, String ontURI, String newId) {
		if(bodySide != null && !"".equals(bodySide)) {
         Fact tumorsideF = FactFactory.createTumorFactModifier( ontURI + "#" + bodySide, tumorF,
               cancerF, FHIRConstants.TUMOR_SUMMARY, FHIRConstants.HAS_BODY_MODIFIER,
					FHIRConstants.RECORD_SUMMARY, FHIRConstants.LATERALITY);
			tumorF.addModifier(tumorsideF);
		}
		
		for(Fact f : quadrantFactSet){
         Fact qf = FactFactory.createTumorFactModifier( ontURI + "#" + f.getName(), tumorF,
               cancerF, FHIRConstants.TUMOR_SUMMARY, FHIRConstants.HAS_BODY_MODIFIER, FHIRConstants.RECORD_SUMMARY,
														FHIRConstants.BODY_MODIFIER);
			qf.setAncestors(f.getAncestors());
			tumorF.addModifier(qf);
		}
		
		for(Fact f : clockfacePosFactSet){
         Fact qf = FactFactory.createTumorFactModifier( ontURI + "#" + f.getName(), tumorF,
               cancerF, FHIRConstants.TUMOR_SUMMARY, FHIRConstants.HAS_BODY_MODIFIER, FHIRConstants.RECORD_SUMMARY,
														FHIRConstants.BODY_MODIFIER);
			qf.setAncestors(f.getAncestors());
			tumorF.addModifier(qf);
		}	
		
	}
	
}