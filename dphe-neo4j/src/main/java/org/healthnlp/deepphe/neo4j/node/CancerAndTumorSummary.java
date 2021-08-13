package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class CancerAndTumorSummary {

    public List<CancerSummary> getCancers() {
        return cancers;
    }

    public void setCancers(List<CancerSummary> cancers) {
        this.cancers = cancers;
    }

    List<CancerSummary> cancers;
}
