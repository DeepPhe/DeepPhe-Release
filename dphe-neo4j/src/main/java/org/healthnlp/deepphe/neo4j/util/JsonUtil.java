package org.healthnlp.deepphe.neo4j.util;


import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/5/2020
 */
final public class JsonUtil {

   private JsonUtil() {}

   static private final String DOUBLE_QUOTE = "_dQ_Qd_";
   static private final String SINGLE_QUOTE = "_sQs_";
   static private final String BACK_QUOTE = "_bQb_";
   static private final String BRACE_1 = "_b1c_";
   static private final String BRACE_2 = "_b2c_";
   static private final String BRACKET_1 = "_b1t_";
   static private final String BRACKET_2 = "_b2t_";
   static private final String PARENTH_1 = "_p1p_";
   static private final String PARENTH_2 = "_p2p_";
   static private final String COMMA = "_cMa_";
   static private final String SEMICOLON = "_sCn_";
   static private final String COLON = "_cLn_";
   static private final String SLASH = "_sLs_";

   static private final Map<Character,String> SUBSTITUTES = new HashMap<>( 12 );
   static {
      SUBSTITUTES.put( '\"', DOUBLE_QUOTE );
      SUBSTITUTES.put( '\'', SINGLE_QUOTE );
      SUBSTITUTES.put( '`', BACK_QUOTE );
      SUBSTITUTES.put( '{', BRACE_1 );
      SUBSTITUTES.put( '}', BRACE_2 );
      SUBSTITUTES.put( '[', BRACKET_1 );
      SUBSTITUTES.put( ']', BRACKET_2 );
      SUBSTITUTES.put( '(', PARENTH_1 );
      SUBSTITUTES.put( ')', PARENTH_2 );
      SUBSTITUTES.put( ',', COMMA );
      SUBSTITUTES.put( ';', SEMICOLON );
      SUBSTITUTES.put( ':', COLON );
      SUBSTITUTES.put( '\\', SLASH );
   }

   static public String packForNeo4j( final String json ) {
      String neo4jText = json;
      for ( Map.Entry<Character,String> sub : SUBSTITUTES.entrySet() ) {
         neo4jText = neo4jText.replace( ""+sub.getKey(), sub.getValue() );
      }
      return neo4jText;
   }

   static public String unpackFromNeo4j( final String text ) {
      String json = text;
      for ( Map.Entry<Character,String> sub : SUBSTITUTES.entrySet() ) {
         json = json.replace( sub.getValue(), ""+sub.getKey() );
      }
      return json;
   }


}
