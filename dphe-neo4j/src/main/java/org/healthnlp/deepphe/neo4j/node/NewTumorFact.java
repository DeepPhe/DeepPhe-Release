package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class NewTumorFact extends NewFact {
    NewFactInfo tumorFactInfo;

    public NewFactInfo getTumorFactInfo() {
        return tumorFactInfo;
    }

    public void setTumorFactInfo(NewFactInfo tumorFactInfo) {
        this.tumorFactInfo = tumorFactInfo;
    }
}
