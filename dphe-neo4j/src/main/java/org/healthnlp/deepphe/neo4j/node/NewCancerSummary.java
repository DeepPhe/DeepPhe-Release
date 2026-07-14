package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class NewCancerSummary {
    String cancerId;
    List<NewCancerFact> cancerFacts;
    List<NewTumorSummary> tumors;


    public String getCancerId() {
        return cancerId;
    }

    public void setCancerId(String cancerId) {
        this.cancerId = cancerId;
    }

    public List<NewCancerFact> getCancerFacts() {
        return cancerFacts;
    }

    public void setCancerFacts(List<NewCancerFact> cancerFacts) {
        this.cancerFacts = cancerFacts;
    }

    public List<NewTumorSummary> getTumors() {
        return tumors;
    }

    public void setTumors(List<NewTumorSummary> tumors) {
        this.tumors = tumors;
    }
}
