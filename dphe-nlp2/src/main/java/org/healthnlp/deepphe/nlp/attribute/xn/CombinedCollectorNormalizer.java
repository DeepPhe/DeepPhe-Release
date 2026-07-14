package org.healthnlp.deepphe.nlp.attribute.xn;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {3/25/2024}
 */
public class CombinedCollectorNormalizer implements AttributeInfoCollector, XnAttributeNormalizer {

   private final AttributeInfoCollector _infoCollector;
   private final XnAttributeNormalizer _normalizer;

   public CombinedCollectorNormalizer( final AttributeInfoCollector infoCollector,
                                       final XnAttributeNormalizer normalizer ) {
      _infoCollector = infoCollector;
      _normalizer = normalizer;
   }

   public void init( final UriConcept neoplasm, final String... relationType ) {
      _infoCollector.init( neoplasm, relationType );
   }

   public UriConcept getNeoplasm() {
      return _infoCollector.getNeoplasm();
   }

   public String[] getRelationTypes() {
      return _infoCollector.getRelationTypes();
   }

   public Collection<UriConceptRelation> getRelations() {
      return _infoCollector.getRelations();
   }

   public Collection<UriConcept> getUriConcepts() {
      return _infoCollector.getUriConcepts();
   }

   public void init( final AttributeInfoCollector infoCollector,
                     final Map<String, List<XnAttributeValue>> dependencies, final long mentionCount ) {
      _normalizer.init( _infoCollector, dependencies, mentionCount );
   }

   public List<XnAttributeValue> getValues() {
      return _normalizer.getValues();
   }



}
