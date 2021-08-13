package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class TumorSummary {
    String tumorId;
    String hasTumorType;
    List<TumorFact> tumorFacts;

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

    public List<TumorFact> getTumorFacts() {
        return tumorFacts;
    }

    public void setTumorFacts(List<TumorFact> tumorFacts) {
        this.tumorFacts = tumorFacts;
    }
}
