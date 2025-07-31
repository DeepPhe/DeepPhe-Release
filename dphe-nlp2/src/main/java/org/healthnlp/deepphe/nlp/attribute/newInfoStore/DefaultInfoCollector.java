package org.healthnlp.deepphe.nlp.attribute.newInfoStore;

import org.healthnlp.deepphe.nlp.concept.UriConcept;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class DefaultInfoCollector implements AttributeInfoCollector {

   private UriConcept _neoplasm;
   private String[] _relationTypes;

   public void init( final UriConcept neoplasm, final String... relationTypes ) {
      _neoplasm = neoplasm;
      _relationTypes = relationTypes;
   }

   public UriConcept getNeoplasm() {
      return _neoplasm;
   }

   public String[] getRelationTypes() {
      return _relationTypes;
   }

}
