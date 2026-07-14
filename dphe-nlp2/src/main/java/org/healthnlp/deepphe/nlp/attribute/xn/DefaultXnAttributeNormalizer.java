package org.healthnlp.deepphe.nlp.attribute.xn;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {1/2/2024}
 */
public class DefaultXnAttributeNormalizer implements XnAttributeNormalizer {

   private List<XnAttributeValue> _attributeValues;

   public String getNormalNoValue() {
      return "";
   }

   public String getNormalValue( final UriConcept concept ) {
      final String value = concept.getValue();
      if ( !value.isEmpty() ) {
         return value;
      }
//      if ( UriInfoCache.getInstance().getPrefText( concept.getUri() ).isEmpty() ) {
//         LogFileWriter.add( "DefaultXnAttributeNormalizer empty prefText for " + concept.getUri() );
//      }
      return UriInfoCache.getInstance().getPrefText( concept.getUri() );
   }

//   public String getNormalValue( final String uri ) {
//      return UriInfoCache.INSTANCE.getPrefText( uri );
//   }

   public void init( final AttributeInfoCollector infoCollector, final Map<String,List<XnAttributeValue>> dependencies,
                     final long mentionCount ) {
//      final Map<Double,List<UriConcept>> confidenceConceptsMap
//            = createConfidenceConceptsMap( infoCollector.getRelations() );
//      _attributeValues = createAttributeValues( confidenceConceptsMap );
//      final Map<Double,List<UriConceptRelation>> confidenceRelationsMap
//            = createConfidenceRelationsMap( infoCollector.getRelations() );
//      _attributeValues = createRelAttributeValues( confidenceRelationsMap );
      _attributeValues = createAttributeValues( infoCollector.getRelations(), mentionCount );
   }

   public List<XnAttributeValue> getValues() {
      return _attributeValues;
   }


//   protected Map<Double,List<UriConcept>> createConfidenceConceptsMap( final Collection<UriConceptRelation> relations ) {
//      if ( relations.isEmpty() ) {
//         return Collections.emptyMap();
//      }
//      // 2 different sites can have a mass or lesion with the same uri.  (For example)
//      // We still only want the one with the highest confidence.
//      final Map<Double,List<UriConcept>> confidenceConcepts = new HashMap<>();
//      for ( UriConceptRelation relation : relations ) {
//         final UriConcept target = relation.getTarget();
//         if ( target == null ) {
//            continue;
//         }
//         final double relationConfidence = relation.getConfidence();
////         final double targetConfidence = target.getConfidence();
//         final double targetConfidence = target.getGroupedConfidence();
////         final double confidence = ConfidenceCalculator.calculateHarmonicMean( relationConfidence, targetConfidence );
//         final double confidence = ConfidenceCalculator.calculateArithmeticMean( relationConfidence, targetConfidence );
//         LogFileWriter.add( "DefaultXnAttributeNormalizer.createConfidenceMap "
//               + relation.getType() + " " + target.getUri() + " relationConfidence " + relationConfidence
//               + " , targetConfidence " + targetConfidence + " confidence " + confidence );
//         confidenceConcepts.computeIfAbsent( confidence, c -> new ArrayList<>() ).add( target );
//      }
//      return confidenceConcepts;
//   }

//   protected Map<Double,List<UriConceptRelation>> createConfidenceRelationsMap( final Collection<UriConceptRelation> relations ) {
//      if ( relations.isEmpty() ) {
//         return Collections.emptyMap();
//      }
//      // 2 different sites can have a mass or lesion with the same uri.  (For example)
//      // We still only want the one with the highest confidence.
//      final Map<Double,List<UriConceptRelation>> confidenceRelations = new HashMap<>();
//      for ( UriConceptRelation relation : relations ) {
//         final UriConcept target = relation.getTarget();
//         if ( target == null ) {
//            continue;
//         }
////         final double relationConfidence = relation.getConfidence();
////         final double targetConfidence = target.getGroupedConfidence();
////         final double confidence = ConfidenceCalculator.calculateArithmeticMean( relationConfidence, targetConfidence );
//         final double confidence = ConfidenceCalculator.getRelatedConceptConfidence( relation, target );
//         LogFileWriter.add( "DefaultXnAttributeNormalizer.createConfidenceRelationsMap "
//               + relation.getType() + " " + target.getUri()
//               + " relation Confidence " + ConfidenceCalculator.getConceptRelationConfidence( relation )
//               + " , target Grouped confidence " + ConfidenceCalculator.getConceptConfidence( target )
//               + " attribute value rank confidence " + confidence );
//         confidenceRelations.computeIfAbsent( confidence, c -> new ArrayList<>() ).add( relation );
//      }
//      return confidenceRelations;
//   }


//   protected List<XnAttributeValue> createAttributeValues( final Map<Double,List<UriConcept>> confidenceConcepts ) {
//      if ( confidenceConcepts.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final List<Double> rankedConfidences = confidenceConcepts.keySet()
//                                                               .stream()
//                                                               .sorted()
//                                                               .collect( Collectors.toList() );
//      Collections.reverse( rankedConfidences );
//      final List<XnAttributeValue> attributeValues = new ArrayList<>();
//      for ( double confidence : rankedConfidences ) {
//         final List<UriConcept> concepts = confidenceConcepts.get( confidence );
//         for ( UriConcept concept : concepts ) {
////            attributeValues.add( new XnAttributeValue( concept, getNormalValue( concept ), confidence ) );
//            attributeValues.add( new XnAttributeValue( concept, getNormalValue( concept ), concept.getConfidence() ) );
//         }
//      }
//      return attributeValues;
//   }

//   protected List<XnAttributeValue> createRelAttributeValues( final Map<Double,List<UriConceptRelation>> confidenceRelations ) {
//      if ( confidenceRelations.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final List<Double> rankedConfidences = confidenceRelations.keySet()
//                                                               .stream()
//                                                               .sorted()
//                                                               .collect( Collectors.toList() );
//      Collections.reverse( rankedConfidences );
//      final List<XnAttributeValue> attributeValues = new ArrayList<>();
//      for ( double confidence : rankedConfidences ) {
//         final List<UriConceptRelation> relations = confidenceRelations.get( confidence );
//         for ( UriConceptRelation relation : relations ) {
////            attributeValues.add( new XnAttributeValue( concept, getNormalValue( concept ), confidence ) );
//            final UriConcept target = relation.getTarget();
//            final double relationConfidence = relation.getConfidence();
//            final double targetConfidence = target.getConfidence();
//            final double attributeConfidence = ConfidenceCalculator.calculateArithmeticMean( relationConfidence, targetConfidence );
//            attributeValues.add( new XnAttributeValue( target, getNormalValue( target ), attributeConfidence ) );
//         }
//      }
//      return attributeValues;
//   }



//   protected List<XnAttributeValue> createRelAttributeValues( final Map<Double,List<UriConceptRelation>> confidenceRelations ) {
//      if ( confidenceRelations.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final List<Double> rankedConfidences = confidenceRelations.keySet()
//                                                                .stream()
//                                                                .sorted()
//                                                                .collect( Collectors.toList() );
//      Collections.reverse( rankedConfidences );

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
      for ( Map.Entry<String,List<UriConceptRelation>> normalRelations : normalRelationsMap.entrySet() ) {
         LogFileWriter.add( "DefaultXnAttributeNormalizer "
               + normalRelations.getValue().stream().map( UriConceptRelation::getTarget ).map( UriConcept::getUri ).collect(
               Collectors.joining(" ")) );
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
         LogFileWriter.add( "DefaultXnAttributeNormalizer.createAttributeValues "
               + normalRelations.getKey() + " " + confidence + "\n   " + ranked.get( 0 ).toLongText() + "\n      "
               + ranked.stream().map( UriConcept::toLongText ).collect( Collectors.joining("\n     ") ) );
      }
      return attributeValues;
   }





}
