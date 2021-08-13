package org.healthnlp.deepphe.summary.attribute.stage;

import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;

import java.util.Map;


final public class StageCodeInfoStore implements CodeInfoStore {

   public String _bestCode;


   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
      _bestCode = getBestStageCode( uriInfoStore._bestUri );
   }

   public String getBestCode() {
      return _bestCode;
   }

   static private String getBestStageCode( final String bestUri ) {
      if ( bestUri.isEmpty() || bestUri.equals( Neo4jConstants.MISSING_NODE_NAME ) ) {
         return "";
      }
      if ( bestUri.length() == 7 ) {
         switch ( bestUri ) {
            case "Stage_0":
               return "0";
            case "Stage_1":
               return "I";
            case "Stage_2":
               return "II";
            case "Stage_3":
               return "III";
            case "Stage_4":
               return "IV";
            case "Stage_5":
               return "V";
         }
      }
      final String uri = bestUri.substring( 0, 8 );
      switch ( uri ) {
         case "Stage_Un":
            return "Not Found";
         case "Stage_0_":
            return "0";
         case "Stage_0i":
            return "0";
         case "Stage_0a":
            return "0";
         case "Stage_Is":
            return "0";
         case "Stage_1_":
            return "I";
         case "Stage_1m":
            return "I";
         case "Stage_1A":
            return "IA";
         case "Stage_1B":
            return "IB";
         case "Stage_1C":
            return "IC";
         case "Stage_2_":
            return "II";
         case "Stage_2A":
            return "IIA";
         case "Stage_2B":
            return "IIB";
         case "Stage_2C":
            return "IIC";
         case "Stage_3_":
            return "III";
         case "Stage_3A":
            return "IIIA";
         case "Stage_3B":
            return "IIIB";
         case "Stage_3C":
            return "IIIC";
         case "Stage_4_":
            return "IV";
         case "Stage_4A":
            return "IVA";
         case "Stage_4B":
            return "IVB";
         case "Stage_4C":
            return "IVC";
         case "Stage_5_":
            return "V";
      }
      return "";
   }

}
