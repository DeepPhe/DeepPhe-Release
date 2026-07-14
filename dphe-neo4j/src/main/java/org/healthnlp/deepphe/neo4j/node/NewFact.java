package org.healthnlp.deepphe.neo4j.node;

public class NewFact {
    String relation;
    String relationPrettyName;


    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getRelationPrettyName() {
        return relationPrettyName;
    }

    public void setRelationPrettyName(String relationPrettyName) {
        this.relationPrettyName = relationPrettyName;
    }
}
