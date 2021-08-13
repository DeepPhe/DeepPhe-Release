package org.healthnlp.deepphe.summary.attribute.tnm;

import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;

import java.util.Map;


final public class TnmCodeInfoStore implements CodeInfoStore {

   public String _bestCode;


   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
      _bestCode = getBestTnmCode( uriInfoStore._bestUri );
   }

   public String getBestCode() {
      return _bestCode;
   }

   static private String getBestTnmCode( final String bestUri ) {
      if ( bestUri.isEmpty() || bestUri.equals( Neo4jConstants.MISSING_NODE_NAME ) ) {
         return "";
      }
      return bestUri.replace( "_Stage_Finding", "" )
                    .replace( "_Stage", "" )
                    .replace( "_minus", "-" )
                    .replace( "_i", "i" );
    }

}
