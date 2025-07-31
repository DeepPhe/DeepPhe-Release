package org.healthnlp.deepphe.nlp.util;

import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.Neo4jRelationUtil;
import org.healthnlp.deepphe.neo4j.util.RelatedUris;
import org.healthnlp.deepphe.nlp.uri.ShortUriUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {1/9/2024}
 */
public class RelationLister {

   static private final String DATABASE = "C:/Spiffy/output/mark_6_onto/test_runs/neo4j/DeepPhe_2023_v1.db";

   static  private final String ROOT_WORD = "Skin Squamous Cell Carcinoma In Situ";
   static private final String ROOT_URI  = "";


   public static void main( String... args ) {
      final GraphDatabaseService graphDb
            = EmbeddedConnection.getInstance().connectToGraph( DATABASE );

      String rootUri = ROOT_URI;
      if ( ROOT_WORD != null && !ROOT_WORD.isEmpty() ) {
         rootUri = ShortUriUtil.createShortUri( ROOT_WORD );
         System.out.println( "URI for " + ROOT_WORD + " is " + rootUri );
      }

      final RelatedUris relatedInGraph = Neo4jRelationUtil.getAllRelatedClassUris( graphDb, rootUri );
      System.out.println( "Relations for " + rootUri );
      for ( Map.Entry<String,Collection<String>> uriRelations : relatedInGraph.getRelationTargets().entrySet() ) {
         System.out.println( uriRelations.getKey() );
         for ( String target : uriRelations.getValue() ) {
            System.out.println( "   " + target + "  " + relatedInGraph.getTargetOwnerDistance( target ) );
         }
      }

//      final Map<String,Collection<String>> relatedInGraph = Neo4jRelationUtil.getAllRelatedClassUris( graphDb, rootUri );
//      System.out.println( "Relations for " + rootUri );
//      for ( Map.Entry<String,Collection<String>> uriRelations : relatedInGraph.entrySet() ) {
//         System.out.println( uriRelations.getKey() );
//         for ( String target : uriRelations.getValue() ) {
//            System.out.println( "   " + target );
//         }
//      }


   }
}
