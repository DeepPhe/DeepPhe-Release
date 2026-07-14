package org.healthnlp.deepphe.nlp.attribute.topo_minor;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/25/2024}
 */
public class TopoMinorNormalizer extends DefaultXnAttributeNormalizer {

   protected List<XnAttributeValue> createAttributeValues( final Collection<UriConceptRelation> confidenceRelations,
                                                           final long mentionCount ) {
      final Map<String,List<UriConceptRelation>> normalRelationsMap = new HashMap<>();
      for ( UriConceptRelation relation : confidenceRelations ) {
         final UriConcept target = relation.getTarget();
         if ( target == null ) {
            continue;
         }
         final String normal = getNormalValue( target );
         normalRelationsMap.computeIfAbsent( normal, n -> new ArrayList<>() ).add( relation );
      }
      final List<XnAttributeValue> attributeValues = new ArrayList<>();
      final boolean options = normalRelationsMap.size() > 1;
      for ( Map.Entry<String,List<UriConceptRelation>> normalRelations : normalRelationsMap.entrySet() ) {
         final String normal = normalRelations.getKey();
         if ( options && (normal.isEmpty() || normal.equals( "9" )) ) {
//            LogFileWriter.add( "TopoMinorNormalizer skipping " + normalRelations.getValue().stream()
//                                                                                .map( UriConceptRelation::getTarget )
//                                                                                .map( UriConcept::getUri ).collect(
//                        Collectors.joining(" " ) ) );
            continue;
         }
         final double confidence = ConfidenceCalculator.getAttributeConfidence( normalRelations.getValue(), mentionCount );
         final List<UriConcept> ranked
               = ConfidenceCalculator.rankConfidentConcepts( normalRelations.getValue()
                                                                            .stream()
                                                                            .map( UriConceptRelation::getTarget )
                                                                            .collect( Collectors.toSet() ), mentionCount );
         if ( ranked.isEmpty() ) {
            continue;
         }
         attributeValues.add( new XnAttributeValue( ranked.get( 0 ), normalRelations.getKey(), confidence, ranked ) );
         LogFileWriter.add( "TopoMinorNormalizer.createAttributeValues "
               + normalRelations.getKey() + " " + confidence + "\n   " + ranked.get( 0 ).toLongText() + "\n      "
               + ranked.stream().map( UriConcept::toLongText ).collect( Collectors.joining("\n     ") ) );
      }
      return attributeValues;
   }


}
