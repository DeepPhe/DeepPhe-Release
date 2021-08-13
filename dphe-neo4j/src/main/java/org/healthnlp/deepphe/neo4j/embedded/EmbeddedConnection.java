package org.healthnlp.deepphe.neo4j.embedded;


import org.neo4j.graphdb.GraphDatabaseService;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2017
 */
public enum EmbeddedConnection {
   INSTANCE;

   public static EmbeddedConnection getInstance() {
      return INSTANCE;
   }

   static private final String LOCAL_GRAPH_DB = "resources/graph/neo4j/ontology.db";

   private GraphDatabaseService _graphDb;


   private String getEmbeddedGraphDb() {
      return LOCAL_GRAPH_DB;
   }

   public GraphDatabaseService connectToGraph() {
      return connectToGraph( getEmbeddedGraphDb() );
   }

   synchronized public GraphDatabaseService connectToGraph( final String graphDbPath ) {
      if ( _graphDb != null ) {
         return _graphDb;
      }
      _graphDb = ServiceFactory.createService( graphDbPath );
      return _graphDb;
   }

   public GraphDatabaseService getGraph() {
      if ( _graphDb == null ) {
         return connectToGraph();
      }
      return _graphDb;
   }


}
