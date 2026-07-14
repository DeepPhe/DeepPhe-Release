package org.healthnlp.deepphe.nlp.concept;

import org.healthnlp.deepphe.neo4j.node.MentionRelation;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {9/9/2023}
 */
public class DefaultUriConceptRelation implements UriConceptRelation {

    private final String _type;
    private final UriConcept _target;
    private final Collection<MentionRelation> _mentionRelations;
    private final double _confidence;

    public DefaultUriConceptRelation( final String type,
                                     final UriConcept target,
                                     final Collection<MentionRelation> mentionRelations,
                                      final double confidence ) {
        _type = type;
        _target = target;
        _mentionRelations = mentionRelations;
        _confidence = confidence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return _type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UriConcept getTarget() {
        return _target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<MentionRelation> getMentionRelations() {
        return _mentionRelations;
    }

    /**
     * Use (c1 + c2 + c3 + cN) / (N + 2 * sqrt(N))
     * (1)100 ~ (2)90 ~ (3)80 ~ (4)70 ~ (7)60     (>=33.3)   --> To pass 100 requires 3, 4, 5, 8
     * (2)100 ~ (3)90 ~ (5)80 ~ (9)70 ~ (20)60    (>=41.41)  --> To pass 100 requires 4, 6, 10, 21
     * @return Adjusted average as 'confidence' between 0 and 1
     */
    @Override
    public double getConfidence() {
        return _confidence;
    }

}
