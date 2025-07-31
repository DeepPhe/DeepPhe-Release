package org.healthnlp.deepphe.neo4j.node.xn;


import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {10/20/2023}
 */
public class PatientSummaryXn {

    private String id;
    private String name;
    private String gender;
    private String birth;
    private String death;
    private List<DocumentXn> documents;
    private List<Concept> concepts;
    private List<ConceptRelation> conceptRelations;
    private List<CancerSummaryXn> cancers;

    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( final String name ) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender( final String gender ) {
        this.gender = gender;
    }

    public String getBirth() {
        return birth;
    }

    public void setBirth( final String birth ) {
        this.birth = birth;
    }

    public String getDeath() {
        return death;
    }

    public void setDeath( final String death ) {
        this.death = death;
    }

    public List<DocumentXn> getDocuments() {
        return documents;
    }

    public void setDocuments( final List<DocumentXn> documents ) {
        this.documents = documents;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public void setConcepts( final List<Concept> concepts ) {
        this.concepts = concepts;
    }

    public List<ConceptRelation> getConceptRelations() {
        return conceptRelations;
    }

    public void setConceptRelations( final List<ConceptRelation> conceptRelations ) {
        this.conceptRelations = conceptRelations;
    }

    public List<CancerSummaryXn> getCancers() {
        return cancers;
    }

    public void setCancers( final List<CancerSummaryXn> cancers ) {
        this.cancers = cancers;
    }

}
