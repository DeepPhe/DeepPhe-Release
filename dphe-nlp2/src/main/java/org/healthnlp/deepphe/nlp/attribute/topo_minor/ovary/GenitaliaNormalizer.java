package org.healthnlp.deepphe.nlp.attribute.topo_minor.ovary;


import org.apache.ctakes.core.util.log.LogFileWriter;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.TopoMinorNormalizer;

import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * https://training.seer.cancer.gov/ovarian/abstract-code-stage/codes.html
 *
 * C57.0	Fallopian tube
 * C57.1	Broad Ligament
 * C57.2	Round ligament
 * C57.3	Parametrium
 * C57.4	Uterine adnexa
 * C57.7	Other specified parts of female genital organs
 * C57.8	Overlapping lesion of female genital organs
 * C57.9	Female genital tract, NOS
 *
 * @author SPF , chip-nlp
 * @since {4/14/2023}
 */
final public class GenitaliaNormalizer extends TopoMinorNormalizer {


   public String getNormalNoValue() {
      return "9";
   }

   /**
    *  * C57.0	Fallopian tube
    *  * C57.1	Broad Ligament
    *  * C57.2	Round ligament
    *  * C57.3	Parametrium
    *  * C57.4	Uterine adnexa
    *  * C57.7	Other specified parts of female genital organs
    *  * C57.8	Overlapping lesion of female genital organs
    *  * C57.9	Female genital tract, NOS
    * @param concept -
    * @return -
    */
   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();

      if ( OvaryUriCollection.getInstance().getFallopianTubeUri().equals( uri ) ) {
         return "0";
      }
      if ( OvaryUriCollection.getInstance().getBroadLigamentUri().equals( uri ) ) {
         return "1";
      }
      if ( OvaryUriCollection.getInstance().getRoundLigamentUris().contains( uri ) ) {
         return "2";
      }
      if ( OvaryUriCollection.getInstance().getParametriumUri().equals( uri ) ) {
         return "3";
      }
      if ( OvaryUriCollection.getInstance().getUterineAdnexaUri().equals( uri ) ) {
         return "4";
      }
      if ( OvaryUriCollection.getInstance().getOtherGenitalUris().contains( uri ) ) {
         return "7";
      }
//      if ( OvaryUriCollection.getInstance().getOverlappingGenitalUris().contains( uri ) ) {
//         return 8;
//      }
      if ( OvaryUriCollection.getInstance().getGenitalTractUri().equals( uri ) ) {
         return "9";
      }
      return getNormalNoValue();
   }

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
            boolean tract = false;
            for ( UriConceptRelation relation : normalRelations.getValue() ) {
               final String uri = relation.getTarget().getUri();
               if ( OvaryUriCollection.getInstance().getGenitalTractUri().equals( uri ) ) {
                  tract = true;
                  break;
               }
            }
            if ( !tract ) {
               continue;
            }
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
         LogFileWriter.add( "DefaultXnAttributeNormalizer.createAttributeValues "
               + normalRelations.getKey() + " " + confidence + "\n   " + ranked.get( 0 ).toLongText() + "\n      "
               + ranked.stream().map( UriConcept::toLongText ).collect( Collectors.joining("\n     ") ) );
      }
      return attributeValues;
   }


}
