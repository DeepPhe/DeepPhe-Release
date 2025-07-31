package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class FactInfoAndGroupedTextProvenances {
    public NewFactInfo getSourceFact() {
        return sourceFact;
    }

    public void setSourceFact(NewFactInfo sourceFact) {
        this.sourceFact = sourceFact;
    }

    NewFactInfo sourceFact;
    List<NewMentionedTerm> mentionedTerms;

    public List<NewMentionedTerm> getMentionedTerms() {
        return mentionedTerms;
    }

    public void setMentionedTerms(List<NewMentionedTerm> mentionedTerms) {
        this.mentionedTerms = mentionedTerms;
    }
}
