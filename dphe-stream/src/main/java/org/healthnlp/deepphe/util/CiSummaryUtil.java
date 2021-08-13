package org.healthnlp.deepphe.util;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.neo4j.graphdb.GraphDatabaseService;

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
    * 
    *
    * @return String for the histology or an empty string.
    */
   static public String getHistology( final String uri ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      for ( Map.Entry<String, Collection<String>> histologies : UriConstants.getHistologyMap( graphDb ).entrySet() ) {
         if ( histologies.getValue().contains( uri ) ) {
            return histologies.getKey();
         }
      }
      return "";
      // Default to carcinoma ?
//      return "Carcinoma";
   }

   /**
    * Cancer type most likely does NOT have explicit representation in the document.
    *
    * @return String for the histology or an empty string.
    */
   static public String getCancerType( final String uri ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                 .getGraph();
      for ( Map.Entry<String, Collection<String>> cancerTypes : UriConstants.getCancerTypeMap( graphDb ).entrySet() ) {
         if ( cancerTypes.getValue().contains( uri ) ) {
            return cancerTypes.getKey();
         }
      }
      return "";
//      // Default to carcinoma ?
//      return "Carcinoma";
      // Default to Epithelial ?
//      return "Epithelial_Cell";
   }

//   /**
//    * Cancer type most likely does NOT have explicit representation in the document.
//    *
//    * @return String for the histology or an empty string.
//    */
//   static public String getMetastaticGroup( final String uri ) {
//      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
//                                                                 .getGraph();
//      final String group = UriConstants.getMetastaticGroupMap( graphDb ).get( uri );
//      if ( group != null && !group.isEmpty() ) {
//         return group;
//      }
//      return "Unknown";
//   }

}
