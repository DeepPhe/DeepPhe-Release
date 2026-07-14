package org.healthnlp.deepphe.nlp.attribute.grade;


import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;
import org.healthnlp.deepphe.nlp.concept.UriConcept;


/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class GradeNormalizer extends DefaultXnAttributeNormalizer {

   // TODO ---> Some KCR Lung notes use "MD" to indicate "Moderately Differentiated"

//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // The Cancer Registry default is 9.
//         return "9";
//      }
//      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
//      int bestCode = -1;
//      long bestCodesCount = 0;
//      for ( Map.Entry<Integer,Long> codeCount : intCountMap.entrySet() ) {
//         final long count = codeCount.getValue();
//         if ( codeCount.getKey() > bestCode ) {
//            bestCode = codeCount.getKey();
//            bestCodesCount = count;
//         }
//      }
//      setBestCodesCount( (int)bestCodesCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( intCountMap.size() );
//      NeoplasmSummaryCreator.addDebug( "GradeNormalizer "
//                                       + intCountMap.entrySet().stream()
//                                                 .map( e -> e.getKey() + ":" + e.getValue() )
//                                                 .collect( Collectors.joining(",") ) + " = "
//                                       + bestCode +"\n");
//      return bestCode <= 0 ? "9" : bestCode+"";
//   }

   static private final String NORMAL_NO_VALUE = "9";

   public String getNormalNoValue() {
      return NORMAL_NO_VALUE;
   }

   public String getNormalValue( final UriConcept concept ) {
      return getUriNormalValue( concept.getUri() );
   }


   static public String getUriNormalValue( final String uri ) {
      if ( uri.startsWith( "Gleason_Score_" ) ) {
         if ( uri.endsWith( "6" ) ) {
            // well differentiated
            return "1";
         }
         else if ( uri.endsWith( "7" ) ) {
            // moderately differentiated
            return "2";
         }
         else if ( uri.endsWith( "8" )
               || uri.endsWith( "9" )
               || uri.endsWith( "10" ) ) {
            // poorly differentiated
            return "3";
         }
         else {
            return NORMAL_NO_VALUE;
         }
         // There is a Tumor_Grade_G0
      } else if ( uri.equals( "Grade1" )
            || uri.equals( "LowGrade" )
            || uri.equals( "LowGradeMalignantNeoplasm" )
            || uri.equals( "WellDifferentiated" ) ) {
         return "1";
      }
      else if ( uri.equals( "Grade2" )
            || uri.equals( "IntermediateGrade" )
            || uri.equals( "IntermediateGradeMalignantNeoplasm" )
            || uri.equals( "ModeratelyDifferentiated" ) ) {
         return "2";
      }
      else if ( uri.equals( "Grade3" )
            || uri.equals( "Grade3a" )
            || uri.equals( "Grade3b" )
            || uri.equals( "HighGrade" )
            || uri.equals( "HighGradeMalignantNeoplasm" )
            || uri.equals( "PoorlyDifferentiated" ) ) {
         return "3";
      }
      else if ( uri.equals( "Grade4" )
            || uri.equals( "Undifferentiated" ) ) {
//                  || uri.equals( "Anaplastic" ))
         return "4";
//      } else if ( uri.equals( "Grade5" ) ) {
//         return 5;
      }
      return NORMAL_NO_VALUE;
   }


//   protected void fillEvidenceMap( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies) {
//      useAllEvidenceMap( infoCollector, dependencies );
//   }

}
