package org.healthnlp.deepphe.nlp.attribute.xn;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;

import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {1/2/2024}
 */
public interface XnAttributeNormalizer {

   void init( AttributeInfoCollector infoCollector, Map<String,List<XnAttributeValue>> dependencies,
              final long mentionCount );

   List<XnAttributeValue> getValues();

}
