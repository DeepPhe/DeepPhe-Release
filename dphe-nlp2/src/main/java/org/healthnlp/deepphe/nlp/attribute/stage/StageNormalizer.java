package org.healthnlp.deepphe.nlp.attribute.stage;

import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class StageNormalizer extends DefaultXnAttributeNormalizer {


//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // No Default stage.
//         return "";
//      }
//      final Map<String,Long> countMap = createCodeCountMap( aggregates );
//      final List<String> codeList = new ArrayList<>( countMap.keySet() );
//      codeList.sort( Comparator.comparingInt( SORT_LIST::indexOf ).reversed() );
//      final String bestCode = codeList.get( 0 );
//      if ( bestCode.isEmpty() ) {
//         return "";
//      }
//      long bestCount = countMap.get( bestCode );
//      setBestCodesCount( (int)bestCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( countMap.size() );
//      NeoplasmSummaryCreator.addDebug( "StageNormalizer "
//                                       + countMap.entrySet().stream()
//                                                 .map( e -> e.getKey() + ":" + e.getValue() )
//                                                 .collect( Collectors.joining(",") ) + " = "
//                                       + bestCode +"\n");
//      return bestCode;
//   }

   public List<XnAttributeValue> getValues() {
      final List<XnAttributeValue> superList = super.getValues();
      return superList.stream().sorted( STAGE_COMPARATOR ).collect( Collectors.toList() );
   }

   static private final Map<String,String> EXACT_URIS = new HashMap<>();
   static {
      EXACT_URIS.put( "Stage0", "0" );
      EXACT_URIS.put( "Stage0a", "0" );
      EXACT_URIS.put( "Stage0is", "0" );
      EXACT_URIS.put( "InSitu", "0" );
      EXACT_URIS.put( "StageIs", "0" );

      EXACT_URIS.put( "StageI", "I" );
      EXACT_URIS.put( "StageIA", "IA" );
      EXACT_URIS.put( "StageIA1", "IA" );
      EXACT_URIS.put( "StageIA2", "IA" );
      EXACT_URIS.put( "StageIA3", "IA" );
      EXACT_URIS.put( "StageIB", "IB" );
      EXACT_URIS.put( "StageIB1", "IB" );
      EXACT_URIS.put( "StageIB2", "IB" );
      EXACT_URIS.put( "StageIB3", "IB" );
      EXACT_URIS.put( "StageIC", "IC" );

      EXACT_URIS.put( "StageII", "II" );
      EXACT_URIS.put( "StageIIA", "IIA" );
      EXACT_URIS.put( "StageIIA1", "IIA" );
      EXACT_URIS.put( "StageIIA2", "IIA" );
      EXACT_URIS.put( "StageIIB", "IIB" );
      EXACT_URIS.put( "StageIIC", "IIC" );
      // Stage 2E is special for Non-Hodgkin Lymphoma
      EXACT_URIS.put( "StageIIE", "IIE" );

      EXACT_URIS.put( "StageIII", "III" );
      EXACT_URIS.put( "LocallyMetastatic", "III" );
      EXACT_URIS.put( "StageIIIA", "IIIA" );
      EXACT_URIS.put( "StageIIIA1", "IIIA" );
      EXACT_URIS.put( "StageIIIA2", "IIIA" );
      EXACT_URIS.put( "StageIIIB", "IIIB" );
      EXACT_URIS.put( "StageIIIC", "IIIC" );
      EXACT_URIS.put( "StageIIIC1", "IIIC" );
      EXACT_URIS.put( "StageIIIC2", "IIIC" );

      EXACT_URIS.put( "StageIV", "IV" );
      EXACT_URIS.put( "Metastatic", "IV" );
      EXACT_URIS.put( "DistantlyMetastatic", "IV" );
      EXACT_URIS.put( "AdvancedStage", "IV" );
      EXACT_URIS.put( "StageIVA", "IVA" );
      EXACT_URIS.put( "StageIVA1", "IVA" );
      EXACT_URIS.put( "StageIVA2", "IVA" );
      EXACT_URIS.put( "StageIVB", "IVB" );
      EXACT_URIS.put( "StageIVC", "IVC" );

      EXACT_URIS.put( "StageX", "Unknown" );
      EXACT_URIS.put( "StageUnknown", "Unknown" );
      EXACT_URIS.put( "StageUnspecified", "Unknown" );
      EXACT_URIS.put( "StagingIncomplete", "Unknown" );
   }

   static private final Map<String,String> ROUGH_URIS = new HashMap<>();
   static {
      ROUGH_URIS.put( "Stage0", "0" );
      ROUGH_URIS.put( "Stage0a", "0" );
      ROUGH_URIS.put( "Stage0is", "0" );
      ROUGH_URIS.put( "InSitu", "0" );
      ROUGH_URIS.put( "StageIs", "0" );

      ROUGH_URIS.put( "StageI", "I" );
      ROUGH_URIS.put( "StageIA", "I" );
      ROUGH_URIS.put( "StageIA1", "I" );
      ROUGH_URIS.put( "StageIA2", "I" );
      ROUGH_URIS.put( "StageIA3", "I" );
      ROUGH_URIS.put( "StageIB", "I" );
      ROUGH_URIS.put( "StageIB1", "I" );
      ROUGH_URIS.put( "StageIB2", "I" );
      ROUGH_URIS.put( "StageIB3", "I" );
      ROUGH_URIS.put( "StageIC", "I" );

      ROUGH_URIS.put( "StageII", "II" );
      ROUGH_URIS.put( "StageIIA", "II" );
      ROUGH_URIS.put( "StageIIA1", "II" );
      ROUGH_URIS.put( "StageIIA2", "II" );
      ROUGH_URIS.put( "StageIIB", "II" );
      ROUGH_URIS.put( "StageIIC", "II" );
      // Stage 2E is special for Non-Hodgkin Lymphoma
      ROUGH_URIS.put( "StageIIE", "II" );

      ROUGH_URIS.put( "StageIII", "III" );
      ROUGH_URIS.put( "LocallyMetastatic", "III" );
      ROUGH_URIS.put( "StageIIIA", "III" );
      ROUGH_URIS.put( "StageIIIA1", "III" );
      ROUGH_URIS.put( "StageIIIA2", "III" );
      ROUGH_URIS.put( "StageIIIB", "III" );
      ROUGH_URIS.put( "StageIIIC", "III" );
      ROUGH_URIS.put( "StageIIIC1", "III" );
      ROUGH_URIS.put( "StageIIIC2", "III" );

      ROUGH_URIS.put( "StageIV", "IV" );
      ROUGH_URIS.put( "Metastatic", "IV" );
      ROUGH_URIS.put( "DistantlyMetastatic", "IV" );
      ROUGH_URIS.put( "AdvancedStage", "IV" );
      ROUGH_URIS.put( "StageIVA", "IV" );
      ROUGH_URIS.put( "StageIVA1", "IV" );
      ROUGH_URIS.put( "StageIVA2", "IV" );
      ROUGH_URIS.put( "StageIVB", "IV" );
      ROUGH_URIS.put( "StageIVC", "IV" );

      ROUGH_URIS.put( "StageX", "Unknown" );
      ROUGH_URIS.put( "StageUnknown", "Unknown" );
      ROUGH_URIS.put( "StageUnspecified", "Unknown" );
      ROUGH_URIS.put( "StagingIncomplete", "Unknown" );
   }


   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
//      return EXACT_URIS.getOrDefault( uri, "" );
      return ROUGH_URIS.getOrDefault( uri, "" );
   }

   static private final List<String> SORT_LIST = Arrays.asList(
         "", "Unknown", "0", "I", "IA", "IB", "IC",
         "II", "IIA", "IIB", "IIC",
         "III", "IIIA", "IIIB", "IIIC",
         "IV", "IVA", "IVB", "IVC" );

   static private final Comparator<XnAttributeValue> STAGE_COMPARATOR
         = ( v1, v2 ) -> SORT_LIST.indexOf( v2.getValue() ) - SORT_LIST.indexOf( v1.getValue() );

}
