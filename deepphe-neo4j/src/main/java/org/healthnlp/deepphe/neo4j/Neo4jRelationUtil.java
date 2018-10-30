package org.healthnlp.deepphe.neo4j;


import org.apache.log4j.Logger;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.IS_A_PROP;
import static org.healthnlp.deepphe.neo4j.Neo4jConstants.NAME_KEY;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/27/2017
 */
final public class Neo4jRelationUtil {

   static private final Logger LOGGER = Logger.getLogger( "Neo4jRelationUtil" );

   private Neo4jRelationUtil() {}

   /**
    *
    * @param uri -
    * @return all relations possible for the given uri class and its ancestors, stopping at the most specific related node for each relation type
    */
   static public Map<String, Collection<String>> getRelatedClassUris( final GraphDatabaseService graphDb,
                                                                      final String uri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
         if ( rootBase == null ) {
            LOGGER.error( "No Class exists for URI " + uri );
            tx.success();
            return Collections.emptyMap();
         }
         final Map<String, Collection<String>> finalRelations = new HashMap<>();
         final Map<String, Collection<String>> currentRelations = new HashMap<>();
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getOrderedRootsTraverser( graphDb );
         for ( Node node : traverser.traverse( rootBase )
                                    .nodes() ) {
            for ( Relationship relation : node.getRelationships( Direction.OUTGOING ) ) {
               final String relationName = relation.getType()
                                                   .name();
               if ( relationName.equals( IS_A_PROP ) || finalRelations.containsKey( relationName )
                    || !RelationConstants.REQUIRED_RELATIONS.contains( relationName ) ) {
                  // Added new "required relations" so that Dphe doesn't waste time trying to find relations that don't (yet) interest us.
                  continue;
               }
               final Collection<String> endUris = currentRelations.computeIfAbsent( relationName,
                                                                                    n -> new ArrayList<>() );
               endUris.add( relation.getOtherNode( node )
                                    .getProperty( NAME_KEY )
                                    .toString() );
            }
            finalRelations.putAll( currentRelations );
            currentRelations.clear();
         }
         tx.success();
         return finalRelations;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyMap();
   }


   /**
    *
    * @param uri -
    * @return all relations possible for the given uri class and its ancestors, stopping at the most specific related node for each relation type
    */
   static public Map<String, Collection<Node>> getRelatedClassNodes( final GraphDatabaseService graphDb,
                                                                     final String uri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
         if ( rootBase == null ) {
            LOGGER.error( "No Class exists for URI " + uri );
            tx.success();
            return Collections.emptyMap();
         }
         final Map<String, Collection<Node>> finalRelations = new HashMap<>();
         final Map<String, Collection<Node>> currentRelations = new HashMap<>();
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance().getOrderedRootsTraverser( graphDb );
         for ( Node node : traverser.traverse( rootBase ).nodes() ) {
            for ( Relationship relation : node.getRelationships( Direction.OUTGOING ) ) {
               final String relationName = relation.getType().name();
               if ( finalRelations.containsKey( relationName ) ) {
                  continue;
               }
               final Collection<Node> endNodes = currentRelations.computeIfAbsent( relationName,
                                                                                   n -> new ArrayList<>() );
               endNodes.add( relation.getOtherNode( node ) );
            }
            finalRelations.putAll( currentRelations );
            currentRelations.clear();
         }
         tx.success();
         return finalRelations;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyMap();
   }


   static public Collection<String> getRelatableUris( final GraphDatabaseService graphDb,
                                                      final Collection<String> availableUris,
                                                      final Collection<String> relationTargetUris ) {
      final Collection<String> targetUris = new HashSet<>();
      final Collection<String> tempList = new ArrayList<>();
      for ( String relationTargetUri : relationTargetUris ) {
         final Collection<String> targetableBranch = SearchUtil.getBranchUris( graphDb, relationTargetUri );
         tempList.addAll( targetableBranch );
         tempList.retainAll( availableUris );
         targetUris.addAll( tempList );
         tempList.clear();
      }
      return targetUris;
   }


}
