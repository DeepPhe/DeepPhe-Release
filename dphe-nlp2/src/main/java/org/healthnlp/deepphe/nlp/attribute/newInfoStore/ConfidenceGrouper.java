package org.healthnlp.deepphe.nlp.attribute.newInfoStore;


import org.healthnlp.deepphe.nlp.confidence.ConfidenceGroup;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceOwner;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public interface ConfidenceGrouper {

   void init( final Collection<ConfidenceOwner> confidenceOwners );

   ConfidenceGroup<ConfidenceOwner> getConfidenceGroup();

}
