package org.healthnlp.deepphe.neo4j.node.xn;

/**
 * @author SPF , chip-nlp
 * @since {12/29/2023}
 */
public class InfoNode extends ConfidenceOwner {

    private String id;
    private String classUri;
    private boolean negated;
    private boolean uncertain;
    private boolean historic;
    private Integer confidence;

    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    public String getClassUri() {
        return classUri;
    }

    public void setClassUri( final String classUri ) {
        this.classUri = classUri;
    }


    public boolean isNegated() {
        return negated;
    }

    public void setNegated( final boolean negated ) {
        this.negated = negated;
    }

    public boolean isUncertain() {
        return uncertain;
    }

    public void setUncertain( final boolean uncertain ) {
        this.uncertain = uncertain;
    }

    public boolean isHistoric() {
        return historic;
    }

    public void setHistoric( final boolean historic ) {
        this.historic = historic;
    }

    public void setdConfidence( final double dConfidence ) {
        super.setdConfidence( dConfidence );
        setConfidence( (int)Math.round( dConfidence ) );
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence( final Integer confidence ) {
        this.confidence = confidence;
    }

}
