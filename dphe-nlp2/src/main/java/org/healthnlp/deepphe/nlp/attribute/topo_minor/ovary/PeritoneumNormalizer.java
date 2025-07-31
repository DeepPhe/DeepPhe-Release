package org.healthnlp.deepphe.nlp.attribute.topo_minor.ovary;


import org.healthnlp.deepphe.nlp.attribute.topo_minor.TopoMinorNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

/**
 * https://training.seer.cancer.gov/ovarian/abstract-code-stage/codes.html
 *
 * C48.1	Specified Parts of Peritoneum
 * C48.2	Peritoneum NOS
 * C48.8	Overlapping lesion of Retroperitoneum and Peritoneum
 *
 * @author SPF , chip-nlp
 * @since {4/14/2023}
 */
final public class PeritoneumNormalizer extends TopoMinorNormalizer {


   public String getNormalNoValue() {
      return "2";
   }


   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();

      if ( OvaryUriCollection.getInstance().getPeritoneumPartUris().contains( uri ) ) {
         return "1";
      }
      if ( OvaryUriCollection.getInstance().getPeritoneumUri().equals( uri ) ) {
         return "2";
      }
//      if ( OvaryUriCollection.getInstance().getOverlappingRpUris().contains( uri ) ) {
//         return 8;
//      }
      return getNormalNoValue();
   }


}
