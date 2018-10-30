package org.healthnlp.deepphe.uima.drools;

import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author opm1
 *
 */
public class Summarizer {
	private Pattern tnmPattern = Pattern.compile("(T[0-4])|(M[0-4])|(N[0-4])");
	
	private int numDocuments = 0;
	private Map<String, List<Fact>> tClassificationMap = new HashMap<String, List<Fact>>();
	private Map<String, List<Fact>> mClassificationMap = new HashMap<String, List<Fact>>();
	private Map<String, List<Fact>> nClassificationMap = new HashMap<String, List<Fact>>();
	private Map<String, List<Fact>> canserTypeMap = new HashMap<String, List<Fact>>();
	public int getNumDocuments() {
		return numDocuments;
	}
	public void setNumDocuments(int numDocuments) {
		this.numDocuments = numDocuments;
	}
	public Map<String, List<Fact>> getTClassificationMap() {
		return tClassificationMap;
	}
	public void setTClassificationMap(Map<String, List<Fact>> tClassificationMap) {
		this.tClassificationMap.clear();
		this.tClassificationMap = tClassificationMap;
	}
	public Map<String, List<Fact>> getMClassificationMap() {
		return mClassificationMap;
	}
	public void setMClassificationMap(Map<String, List<Fact>> mClassificationMap) {
		this.mClassificationMap.clear();
		this.mClassificationMap = mClassificationMap;
	}
	public Map<String, List<Fact>> getNClassificationMap() {
		return nClassificationMap;
	}
	public void setNClassificationMap(Map<String, List<Fact>> nClassificationMap) {
		this.nClassificationMap.clear();
		this.nClassificationMap = nClassificationMap;
	}
	public Map<String, List<Fact>> getCanserTypeMap() {
		return canserTypeMap;
	}
	public void setCanserTypeMap(Map<String, List<Fact>> canserTypeMap) {
		this.canserTypeMap = canserTypeMap;
	}
	

	/**
	 * Return type from any Fact in the map
	 * @param map <uri, List<Fact>>
	 * @return
	 */
	public String getType(Map<String,List<Fact>> map){
		List<List<Fact>> facts = new ArrayList<List<Fact>>(map.values());

		if(facts.isEmpty())
			return null;

		String toret = facts.get(0).get(0).getType();

		facts.clear();
		facts = null;
		return toret;
	}
	
	/**
	 * Return category from any Fact in the map
	 * @param map <uri, List<Fact>>
	 * @return
	 */
	public String getCategory(Map<String,List<Fact>> map){
		List<List<Fact>> facts = new ArrayList<List<Fact>>(map.values());

		String toret = facts.get(0).get(0).getCategory();

		facts.clear();
		facts = null;
		return toret;
	}
	
	/**
	 * return keySet as list
	 * @param map <uri, List<Fact>>
	 * @return
	 */
	public List<String> getURIs(Map<String,List<Fact>> map){
		return new ArrayList<String>(map.keySet());
	}
	
	/**
	 * Creates summary fact with the same: name,uri,label,category,type,
	 *                                    patientIdentifier
	 * Considering that all Facts in List<Fact> have the SAME  parameters
	 * @param uri
	 * @param map
	 * @return
	 */
	public Fact createSummaryFact(String uri, Map<String,List<Fact>> map){
		List<List<Fact>> facts = new ArrayList<List<Fact>>(map.values());
		Fact anyFact = facts.get(0).get(0);
		
		Fact newFact = createSummaryFact(anyFact);
		newFact.addProvenanceFacts(map.get(uri));
		
		facts .clear();
		facts = null;
		return newFact;
	}
	
	public Fact createSummaryFact(Fact oldFact){
      return FactFactory.createFact( oldFact );
	}
	
	
	public String regexStatus(String longName){
		Matcher m = tnmPattern.matcher(longName);
		String toret = m.find()?m.group(1):null;
		m.reset();
		return toret;
	}

	
}
