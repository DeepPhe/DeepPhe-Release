package org.healthnlp.deepphe.nlp.attribute.histology;


import org.healthnlp.deepphe.nlp.attribute.newInfoStore.DefaultInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class HistologyInfoCollector extends DefaultInfoCollector {

   public Collection<UriConcept> getUriConcepts() {
      return Collections.singletonList( getNeoplasm() );
   }

}
