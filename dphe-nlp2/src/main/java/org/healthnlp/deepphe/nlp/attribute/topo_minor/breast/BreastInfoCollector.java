package org.healthnlp.deepphe.nlp.attribute.topo_minor.breast;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;


/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
public class BreastInfoCollector extends AbstractTopoMinorInfoCollector {

   public void init( final UriConcept neoplasm, final String... relationTypes ) {
      super.init( neoplasm, RelationConstants.HAS_CLOCKFACE, RelationConstants.HAS_QUADRANT,
            RelationConstants.HAS_SITE, RelationConstants.HAS_ASSOCIATED_SITE );
   }

}
