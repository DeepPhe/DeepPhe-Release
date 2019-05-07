package org.apache.ctakes.cancer.summary;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/19/2019
 */
@Immutable
final public class CiSummaryUtil {

   static private final Logger LOGGER = Logger.getLogger( "CiSummaryUtil" );

   private CiSummaryUtil() {}


   /**
    * Histology most likely does NOT have explicit representation in the document.
    * However, we have a {@link RelationConstants#HAS_HISTOLOGY} relation / attribute.
    *
    * @return String for the histology or an empty string.
    */
   static public String getHistology( final String uri ) {
      for ( Map.Entry<String, Collection<String>> histologies : UriConstants.getHistologyMap().entrySet() ) {
         if ( histologies.getValue().contains( uri ) ) {
            return histologies.getKey();
         }
      }
      return "";
   }

   /**
    * Cancer type most likely does NOT have explicit representation in the document.
    *
    * @return String for the histology or an empty string.
    */
   static public String getCancerType( final String uri ) {
      for ( Map.Entry<String, Collection<String>> cancerTypes : UriConstants.getCancerTypeMap().entrySet() ) {
         if ( cancerTypes.getValue().contains( uri ) ) {
            return cancerTypes.getKey();
         }
      }
      // Default to carcinoma ?
      return "Carcinoma";
   }


}
