package org.healthnlp.deepphe.nlp.attribute.newInfoStore;


import org.healthnlp.deepphe.nlp.confidence.ConfidenceGroup;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceOwner;

import java.util.Collection;

/**
 *
 * This takes the place of [Cr]UriInfoStore.
 *
 *
 * @author SPF , chip-nlp
 * @since {3/22/2023}
 */
public class DefaultConfidenceGrouper implements ConfidenceGrouper {

   private ConfidenceGroup<ConfidenceOwner> _confidenceGroup;

   public void init( final Collection<ConfidenceOwner> confidenceOwners ) {
      _confidenceGroup = new ConfidenceGroup<>( confidenceOwners );
   }

   public ConfidenceGroup<ConfidenceOwner> getConfidenceGroup() {
      return _confidenceGroup;
   }


}
