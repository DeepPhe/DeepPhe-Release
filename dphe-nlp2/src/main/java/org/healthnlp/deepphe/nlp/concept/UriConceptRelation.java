package org.healthnlp.deepphe.nlp.concept;

import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceOwner;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {9/9/2023}
 */
public interface UriConceptRelation extends ConfidenceOwner {

    /**
     *
     * @return name of relation type
     */
    String getType();

    /**
     *
     * @return target concept
     */
    UriConcept getTarget();

    /**
     *
     * @return constituent mention relations
     */
    Collection<MentionRelation> getMentionRelations();

}
