package org.healthnlp.deepphe.nlp.attribute.xn;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

/**
 * @author SPF , chip-nlp
 * @since {3/21/2024}
 */
public class PrefTextNormalizer extends DefaultXnAttributeNormalizer {

   public String getNormalValue( final UriConcept concept ) {
      if ( UriInfoCache.getInstance().getPrefText( concept.getUri() ).isEmpty() ) {
         LogFileWriter.add( "PrefTextNormalizer empty prefText for " + concept.getUri() );
      }
      return UriInfoCache.getInstance().getPrefText( concept.getUri() );
   }

}
