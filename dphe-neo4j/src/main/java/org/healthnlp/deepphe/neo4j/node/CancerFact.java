package org.healthnlp.deepphe.neo4j.node;

public class CancerFact extends NewFact {

   FactInfo cancerFactInfo;

    public FactInfo getCancerFactInfo() {
        return cancerFactInfo;
    }

    public void setCancerFactInfo(FactInfo cancerFactInfo) {
        this.cancerFactInfo = cancerFactInfo;
    }
}
