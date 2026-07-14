package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {3/8/2021}
 */
public class CancerSummary extends NeoplasmSummary {

    private List<NeoplasmSummary> tumors;

    public List<NeoplasmSummary> getTumors() {
        return tumors;
    }

    public void setTumors( final List<NeoplasmSummary> tumors ) {
        this.tumors = tumors;
    }

}