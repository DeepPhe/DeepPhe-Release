package org.healthnlp.deepphe.nlp.attribute.xn;

import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {1/2/2024}
 */
public class XnAttributeValue {

   private final UriConcept _concept;
   private final String _value;
   private final double _confidence;
   private final List<UriConcept> _directEvidence;

   public XnAttributeValue( final UriConcept concept, final String value, final double confidence ) {
      this( concept, value, confidence, Collections.singletonList( concept ) );
   }

   public XnAttributeValue( final UriConcept concept, final String value, final double confidence,
                            final List<UriConcept> directEvidence ) {
      _concept = concept;
      _value = value;
      _confidence = confidence;
      _directEvidence = directEvidence;
   }

   public UriConcept getConcept() {
      return _concept;
   }

   public String getValue() {
      return _value;
   }

   public double getConfidence() {
      return _confidence;
   }

   public List<UriConcept> getDirectEvidence() {
      return _directEvidence;
   }

   public int getMentionCount() {
      return (int)getDirectEvidence().stream().map( UriConcept::getMentions ).count();
   }


}
