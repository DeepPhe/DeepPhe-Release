package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class NewCancerAndTumorSummary {

    public List<NewCancerSummary> getCancers() {
        return cancers;
    }

    public void setCancers(List<NewCancerSummary> cancers) {
        this.cancers = cancers;
    }

    List<NewCancerSummary> cancers;
}
