package org.healthnlp.deepphe.neo4j.node;

public class TumorFact extends NewFact {
    FactInfo tumorFactInfo;

    public FactInfo getTumorFactInfo() {
        return tumorFactInfo;
    }

    public void setTumorFactInfo(FactInfo tumorFactInfo) {
        this.tumorFactInfo = tumorFactInfo;
    }
}
