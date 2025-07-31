package org.healthnlp.deepphe.nlp.attribute.topo_minor;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.nlp.attribute.newInfoStore.DefaultInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;


/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
abstract public class AbstractTopoMinorInfoCollector extends DefaultInfoCollector {

   public void init( final UriConcept neoplasm, final String... relationTypes ) {
      super.init( neoplasm, RelationConstants.HAS_SITE, RelationConstants.HAS_ASSOCIATED_SITE );
   }


}
