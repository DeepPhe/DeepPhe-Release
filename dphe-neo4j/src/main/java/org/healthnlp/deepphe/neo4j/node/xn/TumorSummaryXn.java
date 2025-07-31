package org.healthnlp.deepphe.neo4j.node.xn;


import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {10/20/2023}
 */
public class TumorSummaryXn extends InfoNode {
//    implements
// GroupConfidenceOwner {

    private List<String> conceptIds;
    private List<AttributeXn> attributes;
//    transient private double _groupedConfidence;

    public List<String> getConceptIds() {
        return conceptIds;
    }

    public void setConceptIds(final List<String> conceptIds) {
        this.conceptIds = conceptIds;
    }

    public List<AttributeXn> getAttributes() {
        return attributes;
    }

    public void setAttributes( final List<AttributeXn> attributes ) {
        this.attributes = attributes;
    }

//    public double getGroupedConfidence() {
//        return _groupedConfidence;
//    }
//
//    public void setGroupedConfidence( final double groupedConfidence ) {
//        _groupedConfidence = groupedConfidence;
//    }

}
