package org.healthnlp.deepphe.neo4j.node;

public class BiomarkerSummary {
    String relationPrettyName;
    String tumorFactRelation;
    String patientId;
    String valueText;

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    public String getRelationPrettyName() {
        return relationPrettyName;
    }

    public void setRelationPrettyName(String relationPrettyName) {
        this.relationPrettyName = relationPrettyName;
    }

    public String getTumorFactRelation() {
        return tumorFactRelation;
    }

    public void setTumorFactRelation(String tumorFactRelation) {
        this.tumorFactRelation = tumorFactRelation;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
}