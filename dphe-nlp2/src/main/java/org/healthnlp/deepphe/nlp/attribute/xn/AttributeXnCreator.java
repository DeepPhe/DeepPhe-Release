package org.healthnlp.deepphe.nlp.attribute.xn;

import org.apache.ctakes.core.util.IdCounter;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.xn.AttributeValue;
import org.healthnlp.deepphe.neo4j.node.xn.AttributeXn;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptCreator;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.nlp.util.IdCreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {1/2/2024}
 */
final public class AttributeXnCreator {

   private AttributeXnCreator() {}

   static private final IdCounter ID_COUNTER = new IdCounter();
   static private final IdCounter VALUE_COUNTER = new IdCounter();

   static public void resetCounter() {
      ID_COUNTER.reset();
      VALUE_COUNTER.reset();
   }

   // TODO    Get rid of CrSpecificAttribute and CrDefaultAttributeNew
   //  Make a simple DefaultAttribute
   //  Alternative:  MentionsAttribute, ConceptsAttribute  --> one uses mentions as determinants, the other concepts
   //      ^-- Maybe a Text version???

   // TODO Regardless, they are use InfoCollector and Normalizer.
   //  Create Mention and Concept versions for each of those?

   static public AttributeXn createConceptsAttribute( final String patientId, final String patientTime,
                                                      final String attributeName,
                                                      final List<XnAttributeValue> attributeValues ) {
      final AttributeXn attribute = new AttributeXn();
      attribute.setName( attributeName );
      attribute.setId( IdCreator.createId( patientId, patientTime, "A", ID_COUNTER ) );
      attribute.setValues( createAttributeValues( patientId, patientTime, attributeValues ) );
      return attribute;
   }

   static private List<AttributeValue> createAttributeValues( final String patientId, final String patientTime,
                                                              final List<XnAttributeValue> attributeValues ) {
//      final Collection<Mention> groupMentions = attributeValues.stream()
//                                                          .map( XnAttributeValue::getDirectEvidence )
//                                                          .flatMap( Collection::stream )
//                                                          .map( UriConcept::getMentions )
//                                                          .flatMap( Collection::stream )
//                                                          .collect( Collectors.toSet() );
//      final List<Double> groupScores = ConfidenceCalculator.getMentionScores( groupMentions );
      final List<AttributeValue> nodeValues = new ArrayList<>( attributeValues.size() );
      for ( XnAttributeValue value : attributeValues ) {
//         final Collection<Mention> mentions = value.getDirectEvidence()
//                                                   .stream()
//                                                   .map( UriConcept::getMentions )
//                                                   .flatMap( Collection::stream )
//                                                   .collect( Collectors.toSet() );
//         final List<Double> scores = ConfidenceCalculator.getMentionScores( mentions );
//         final double groupedConfidence = ConfidenceCalculator.calculateDefaultConfidence( scores, groupScores );
         final AttributeValue nodeValue = createAttributeValue( patientId, patientTime, value );
//         nodeValue.setGroupedConfidence( groupedConfidence );
         nodeValues.add( nodeValue );
      }
      return nodeValues;
   }

   static private AttributeValue createAttributeValue( final String patientId, final String patientTime,
                                                       final XnAttributeValue attributeValue ) {
      return createAttributeValue( patientId, patientTime, attributeValue.getValue(), attributeValue.getConcept(),
            attributeValue.getConfidence(), attributeValue.getDirectEvidence() );
   }

   static private AttributeValue createAttributeValue( final String patientId, final String patientTime,
                                                       final String value, final UriConcept concept,
                                                       final double confidence,
                                                       final List<UriConcept> directEvidence
   ) {
      final String uri = concept.getUri();
      final String prefText = UriInfoCache.getInstance().getPrefText( uri );
      if ( prefText.isEmpty() ) {
         LogFileWriter.add( "AttributeXnCreator empty prefText for " + uri );
      }
      return createAttributeValue( patientId, patientTime, value, concept.getDpheGroup().getName(),
            prefText, uri, confidence, concept.isNegated(), concept.isUncertain(),
            concept.inPatientHistory(), directEvidence );
   }

   static private AttributeValue createAttributeValue( final String patientId, final String patientTime,
                                                       final String value,
                                                       final String group, final String preferredText,
                                                       final String uri, final double confidence,
                                                       final boolean negated, final boolean uncertain,
                                                       final boolean historic,
                                                       final List<UriConcept> directEvidence ) {
      final AttributeValue attributeValue = new AttributeValue();
//      attributeValue.setDpheGroup( group );
//      attributeValue.setPreferredText( preferredText );
      attributeValue.setValue( value );
      attributeValue.setClassUri( uri );
      attributeValue.setId( IdCreator.createId( patientId, patientTime, "AV", VALUE_COUNTER ) );
      attributeValue.setdConfidence( confidence );
      attributeValue.setNegated( negated );
      attributeValue.setUncertain( uncertain );
      attributeValue.setHistoric( historic );
      attributeValue.setConceptIds( UriConceptCreator.sortConceptIds( directEvidence ) );
      return attributeValue;
   }

   static private List<String> getConceptIds( final List<UriConcept> concepts ) {
      return concepts.stream().map( UriConcept::getId ).collect( Collectors.toList() );
   }


}
