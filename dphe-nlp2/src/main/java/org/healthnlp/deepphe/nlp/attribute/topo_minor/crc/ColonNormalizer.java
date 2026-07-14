package org.healthnlp.deepphe.nlp.attribute.topo_minor.crc;


import org.healthnlp.deepphe.nlp.attribute.topo_minor.TopoMinorNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;
import org.healthnlp.deepphe.nlp.concept.UriConcept;


/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class ColonNormalizer extends TopoMinorNormalizer {


//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // The Cancer Registry default is 9.
//         return "9";
//      }
//      int bestIntCode = -1;
//      long bestCount = 0;
//      int uniqueCount = 0;
//      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( aggregates );
//      final Map<Integer,Long> bestCountMap = confidenceGroup.getBest()
//                            .stream()
//                           .map( CrConceptAggregate::getUri )
//                            .map( ColonNormalizer::getUriColonNumber )
//                              .filter( c -> c >= 0 )
//                            .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
//      if ( !bestCountMap.isEmpty() ) {
//         final List<Integer> bestCodes = getBestIntCodes( bestCountMap );
//         bestCodes.sort( Comparator.reverseOrder() );
//         bestIntCode = bestCodes.get( 0 );
//         bestCount = bestCountMap.get( bestIntCode );
//         uniqueCount += bestCountMap.size();
//      }
//      final Map<Integer,Long> otherCountMap = confidenceGroup.getBest()
//                                                            .stream()
//                                                            .map( CrConceptAggregate::getUri )
//                                                            .map( ColonNormalizer::getOtherUriColonNumber )
//                                                            .filter( c -> c >= 0 )
//                                                            .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
//      if ( !otherCountMap.isEmpty() ) {
//         if ( bestIntCode < 0 ) {
//            final List<Integer> bestCodes = getBestIntCodes( otherCountMap );
//            bestCodes.sort( Comparator.reverseOrder() );
//            bestIntCode = bestCodes.get( 0 );
//            bestCount = otherCountMap.get( bestIntCode );
//         }
//         uniqueCount += otherCountMap.size();
//      }
//      final Map<Integer,Long> finalCountMap = confidenceGroup.getBest()
//                                                             .stream()
//                                                             .map( CrConceptAggregate::getUri )
//                                                             .map( ColonNormalizer::getFinalUriColonNumber )
//                                                             .filter( c -> c >= 0 )
//                                                             .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
//      if ( !finalCountMap.isEmpty() ) {
//         if ( bestIntCode < 0 ) {
//            final List<Integer> bestCodes = getBestIntCodes( finalCountMap );
//            bestCodes.sort( Comparator.reverseOrder() );
//            bestIntCode = bestCodes.get( 0 );
//            bestCount = finalCountMap.get( bestIntCode );
//         }
//         uniqueCount += finalCountMap.size();
//      }
//      setBestCodesCount( (int)bestCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( uniqueCount );
//      NeoplasmSummaryCreator.addDebug( "ColonNormalizer "
//                                       + bestCountMap.entrySet().stream()
//                                                    .map( e -> e.getKey() + ":" + e.getValue() )
//                                                    .collect( Collectors.joining(",") ) + " ; "
//                                       + otherCountMap.entrySet().stream()
//                                                     .map( e -> e.getKey() + ":" + e.getValue() )
//                                                     .collect( Collectors.joining(",") ) + " ; "
//                                       + finalCountMap.entrySet().stream()
//                                                     .map( e -> e.getKey() + ":" + e.getValue() )
//                                                     .collect( Collectors.joining(",") ) + " ; "
//                                       + bestIntCode +"\n" );
//      return bestIntCode < 0 ? "9" : bestIntCode+"";
//   }

   public String getNormalNoValue() {
      return "9";
   }


   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      int colonNumber = getUriColonNumber( uri );
      if ( colonNumber >= 0 ) {
         return String.valueOf( colonNumber );
      }
      int otherNumber = getOtherUriColonNumber( uri );
      if ( otherNumber >= 0 ) {
         return String.valueOf( otherNumber );
      }
      int finalNumber = getFinalUriColonNumber( uri );
      if ( finalNumber >= 0 ) {
         return String.valueOf( finalNumber );
      }
      return getNormalNoValue();
   }

   static private int getUriColonNumber( final String uri ) {
      if ( CrcUriCollection.getInstance().getHepaticUri().equals( uri ) ) {
         return 3;
      } else if ( CrcUriCollection.getInstance().getTransverseUri().equals( uri ) ) {
         return 4;
      } else if ( CrcUriCollection.getInstance().getSplenicUri().equals( uri ) ) {
         return 5;
      } else if ( CrcUriCollection.getInstance().getSigmoidUri().equals( uri ) ) {
         return 7;
      }
      return -1;
   }

   static private int getOtherUriColonNumber( final String uri ) {
      if ( CrcUriCollection.getInstance().getAscendingUris().contains( uri ) ) {
         return 2;
      } else if ( CrcUriCollection.getInstance().getDescendingUris().contains( uri ) ) {
         return 6;
      }
      return -1;
   }

   static private int getFinalUriColonNumber( final String uri ) {
      if ( CrcUriCollection.getInstance().getCecumUri().equals( uri ) ) {
         return 0;
      } else if ( CrcUriCollection.getInstance().getAppendixUri().equals( uri ) ) {
         return 1;
      }
      return -1;
   }


   //   C18.0	Cecum
//C18.1	Appendix
//C18.2	Ascending colon; Right colon
//C18.3	Hepatic flexure of colon
//C18.4	Transverse colon
//C18.5	Splenic flexure of colon
//C18.6	Descending colon; Left colon
//C18.7	Sigmoid colon
//C18.8	Overlapping lesion of colon
//C18.9	Colon, NOS
//ICD-O-2/3	Term
//Rectosigmoid junction
//C19.9	Rectosigmoid junction
//ICD-O-2/3	Term
//Rectum
//C20.9	Rectum, NOS
//ICD-O-2/3	Term

}
