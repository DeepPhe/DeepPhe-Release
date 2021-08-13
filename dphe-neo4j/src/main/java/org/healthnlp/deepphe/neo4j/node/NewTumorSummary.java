package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class NewTumorSummary {
    String tumorId;
    String hasTumorType;
    List<NewTumorFact> tumorFacts;

    public String getTumorId() {
        return tumorId;
    }

    public void setTumorId(String tumorId) {
        this.tumorId = tumorId;
    }

    public String getHasTumorType() {
        return hasTumorType;
    }

    public void setHasTumorType(String hasTumorType) {
        this.hasTumorType = hasTumorType;
    }

    public List<NewTumorFact> getTumorFacts() {
        return tumorFacts;
    }

    public void setTumorFacts(List<NewTumorFact> tumorFacts) {
        this.tumorFacts = tumorFacts;
    }
}
