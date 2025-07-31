package org.healthnlp.deepphe.nlp.attribute.xn;

import org.healthnlp.deepphe.neo4j.node.xn.AttributeXn;
import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author SPF , chip-nlp
 * @since {12/29/2023}
 */
public class DefaultXnAttribute<C extends AttributeInfoCollector, N extends XnAttributeNormalizer> {

   private final String _attributeName;
   private final String _patientId;
   private final String _patientTime;

   private final List<XnAttributeValue> _xnValues;

   public DefaultXnAttribute( final String attributeName,
                              final UriConcept neoplasm,
                              final String patientId,
                              final String patientTime,
                              final Supplier<C> infoCollector,
                              final Supplier<N> normalizer,
                              final Map<String,List<XnAttributeValue>> dependencies,
                              final long mentionCount, final String... relationTypes ) {
      this( attributeName, neoplasm, patientId, patientTime, infoCollector.get(), normalizer.get(), dependencies,
            mentionCount,
            relationTypes );
   }

   public DefaultXnAttribute( final String attributeName,
                              final UriConcept neoplasm,
                              final String patientId,
                              final String patientTime,
                              final C infoCollector,
                              final N normalizer,
                              final Map<String,List<XnAttributeValue>> dependencies,
                              final long mentionCount, final String... relationTypes ) {
      _attributeName = attributeName;
      _patientId = patientId;
      _patientTime = patientTime;
      infoCollector.init( neoplasm, relationTypes );
      normalizer.init( infoCollector, dependencies, mentionCount );
      _xnValues = normalizer.getValues();
   }

   public List<XnAttributeValue> getXnValues() {
      return _xnValues;
   }

   public AttributeXn toAttributeXn() {
      return AttributeXnCreator.createConceptsAttribute( _patientId, _patientTime, _attributeName, getXnValues() );
   }


}
