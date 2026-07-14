package org.healthnlp.deepphe.nlp.attribute.topo_minor.crc;


import org.healthnlp.deepphe.nlp.attribute.topo_minor.TopoMinorNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class AnusNormalizer extends TopoMinorNormalizer {

   public String getNormalNoValue() {
      return "0";
   }

   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      if ( CrcUriCollection.getInstance().getAnusUri().equals( uri ) ) {
         return "0";
      }
      if ( CrcUriCollection.getInstance().getAnalCanalUris().contains( uri ) ) {
         return "1";
      }
      if ( CrcUriCollection.getInstance().getCloacogenicZoneUris().contains( uri ) ) {
         return "2";
      }
      if ( CrcUriCollection.getInstance().getAnorectalUri().equals( uri ) ) {
         return "8";
      }
      return getNormalNoValue();
   }

   //Anus and Anal canal
//C21.0	Anus, NOS (excludes Skin of anus and Perianal skin C44.5)
//C21.1	Anal canal
//C21.2	Cloacogenic zone
//C21.8	Overlapping lesion of rectum, anus and anal canal


}
