package org.healthnlp.deepphe.nlp.attribute.topo_minor.brain;


import org.healthnlp.deepphe.nlp.attribute.topo_minor.TopoMinorNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

/**
 * @author SPF , chip-nlp
 * @since {4/5/2023}
 */
public class MeningesNormalizer extends TopoMinorNormalizer {

//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // The Cancer Registry default is 9.
//         return "9";
//      }
//      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
//      final List<Integer> bestCodes = getBestIntCodes( intCountMap );
//      bestCodes.sort( Comparator.reverseOrder() );
//      final int bestIntCode = bestCodes.get( 0 );
//      long bestCount = intCountMap.get( bestIntCode );
//      setBestCodesCount( (int)bestCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( intCountMap.size() );
//      NeoplasmSummaryCreator.addDebug( "MeningesNormalizer "
//                                       + intCountMap.entrySet().stream()
//                                                    .map( e -> e.getKey() + ":" + e.getValue() )
//                                                    .collect( Collectors.joining(",") ) + " = "
//                                       + bestIntCode +"\n");
//      return bestIntCode < 0 ? "9" : bestIntCode+"";
//   }

   public String getNormalNoValue() {
      return "9";
   }

   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      if ( BrainUriCollection.getInstance().getCerebralMeninges().contains( uri ) ) {
         return "0";
      }
      if ( BrainUriCollection.getInstance().getSpinalMeninges().contains( uri ) ) {
         return "1";
      }
//      if ( BrainUriCollection.getInstance().getMeningesNOS().contains( uri ) ) {
//         return 9;
//      }
      return getNormalNoValue();
   }

}
