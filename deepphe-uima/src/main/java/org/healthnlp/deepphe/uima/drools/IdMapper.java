package org.healthnlp.deepphe.uima.drools;

import java.util.Collection;
import java.util.LinkedHashSet;

public class IdMapper {
	private String oldId, newId, oldSummaryId, newSummaryId;
	private Collection<String> rulesApplied;
	
	public IdMapper(String oldId, String newId, String oldSummaryId, String newSummaryId){
		this.oldId = oldId;
		this.newId = newId;
		this.oldSummaryId = oldSummaryId;
		this.newSummaryId = newSummaryId;
	}

	public String getOldId() {
		return oldId;
	}

	public void setOldId(String oldId) {
		this.oldId = oldId;
	}

	public String getNewId() {
		return newId;
	}

	public void setNewId(String newId) {
		this.newId = newId;
	}

	public String getOldSummaryId() {
		return oldSummaryId;
	}

	public void setOldSummaryId(String oldSummaryId) {
		this.oldSummaryId = oldSummaryId;
	}

	public String getNewSummaryId() {
		return newSummaryId;
	}

	public void setNewSummaryId(String newSummaryId) {
		this.newSummaryId = newSummaryId;
	}
	
	final public Collection<String> getRulesApplied() {
	      if ( rulesApplied == null ) {
	         rulesApplied = new LinkedHashSet<>();
	      }
	      return rulesApplied;
	   }

	   final public void addRulesApplied( final String ruleName ) {
	      getRulesApplied().add( ruleName );
	   }
}
