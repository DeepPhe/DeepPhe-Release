package org.healthnlp.deepphe.neo4j.util;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants2;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.IS_A_PROP;
import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.NAME_KEY;

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
         for ( Node node : traverser.traverse( rootBase ).nodes() ) {
            for ( Relationship relation : node.getRelationships( Direction.OUTGOING ) ) {
               final String relationName = relation.getType().name();
               if ( relationName.equals( IS_A_PROP )
                     || !RelationConstants2.isRequiredRelation( relationName )
                     || finalRelations.containsKey( relationName )) {
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

//   /**
//    *
//    * @param uri -
//    * @return all relations possible for the given uri class and its ancestors, stopping at the most specific related node for each relation type
//    */
//   static public Map<String, Collection<String>> getAllRelatedClassUris( final GraphDatabaseService graphDb,
//                                                                      final String uri ) {
//      try ( Transaction tx = graphDb.beginTx() ) {
//         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
//         if ( rootBase == null ) {
//            LOGGER.error( "No Class exists for URI " + uri );
//            tx.success();
//            return Collections.emptyMap();
//         }
//         final Map<String, Collection<String>> relationTargets = new HashMap<>();
//         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
//                                                                     .getOrderedRootsTraverser( graphDb );
//         for ( Node node : traverser.traverse( rootBase )
//                                    .nodes() ) {
//            for ( Relationship relation : node.getRelationships( Direction.OUTGOING ) ) {
//               final String relationName = relation.getType()
//                                                   .name();
//               if ( relationName.equals( IS_A_PROP ) ) {
//                  continue;
//               }
//               if ( !RelationConstants.REQUIRED_RELATIONS.contains( relationName )
//                     && !RelationConstants2.REQUIRED_RELATIONS.contains( relationName ) ) {
//                  continue;
//               }
//               final String target = relation.getOtherNode( node ).getProperty( NAME_KEY ).toString();
//               relationTargets.computeIfAbsent( relationName, t -> new HashSet<>() ).add( target );
//            }
//         }
//         tx.success();
//         return relationTargets;
//      } catch ( MultipleFoundException mfE ) {
//         LOGGER.error( mfE.getMessage(), mfE );
//      }
//      return Collections.emptyMap();
//   }

   /**
    *
    * @param uri -
    * @return all relations possible for the given uri class and its ancestors, stopping at the most specific related node for each relation type
    */
   static public RelatedUris getAllRelatedClassUris( final GraphDatabaseService graphDb, final String uri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
         if ( rootBase == null ) {
            LOGGER.error( "No Class exists for URI " + uri );
            tx.success();
            return RelatedUris.EMPTY_URIS;
         }
         // Map of relation Names to Targets for those relations.  e.g. hasSite(Arm,Hand), hasSite(BodyPart)
         final Map<String,Collection<String>> relationTargets = new HashMap<>();
         // Map of relation target owners to their distance from the original class of interest.
         // e.g. CancerOfTheArm(1), neoplasm(3) if the original uri was CancerOfTheRightArm
         final Map<String,Integer> targetOwnerDistance = new HashMap<>();
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getOrderedRootsTraverser( graphDb );
         for ( Path path : traverser.traverse( rootBase ) ) {
            final Node relationOwnerNode = path.endNode();
            for ( Relationship relation : relationOwnerNode.getRelationships( Direction.OUTGOING ) ) {
               final String relationName = relation.getType().name();
               if ( relationName.equals( IS_A_PROP ) || !RelationConstants2.isRequiredRelation( relationName ) ) {
                  continue;
               }
               final String relationTarget = relation.getOtherNode( relationOwnerNode ).getProperty( NAME_KEY ).toString();
               final boolean added = relationTargets.computeIfAbsent( relationName, t -> new HashSet<>() ).add( relationTarget );
               final int ownerDistance = path.length();
               if ( added ) {
                  targetOwnerDistance.put( relationTarget, ownerDistance );
               } else {
                  final int otherDistance = targetOwnerDistance.get( relationTarget );
                  if ( ownerDistance < otherDistance ) {
                     targetOwnerDistance.put( relationTarget, ownerDistance );
                  }
               }
            }
         }
         tx.success();
         return new RelatedUris( relationTargets, targetOwnerDistance );
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return RelatedUris.EMPTY_URIS;
   }

//   /**
//    *
//    * @param uri -
//    * @return all relations possible for the given uri class and its ancestors, stopping at the most specific related node for each relation type
//    */
//   static public Map<String, Collection<Node>> getRelatedClassNodes( final GraphDatabaseService graphDb,
//                                                                     final String uri ) {
//      try ( Transaction tx = graphDb.beginTx() ) {
//         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
//         if ( rootBase == null ) {
//            LOGGER.error( "No Class exists for URI " + uri );
//            tx.success();
//            return Collections.emptyMap();
//         }
//         final Map<String, Collection<Node>> finalRelations = new HashMap<>();
//         final Map<String, Collection<Node>> currentRelations = new HashMap<>();
//         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance().getOrderedRootsTraverser( graphDb );
//         for ( Node node : traverser.traverse( rootBase ).nodes() ) {
//            for ( Relationship relation : node.getRelationships( Direction.OUTGOING ) ) {
//               final String relationName = relation.getType().name();
//               if ( finalRelations.containsKey( relationName ) ) {
//                  continue;
//               }
//               final Collection<Node> endNodes = currentRelations.computeIfAbsent( relationName,
//                                                                                   n -> new ArrayList<>() );
//               endNodes.add( relation.getOtherNode( node ) );
//            }
//            finalRelations.putAll( currentRelations );
//            currentRelations.clear();
//         }
//         tx.success();
//         return finalRelations;
//      } catch ( MultipleFoundException mfE ) {
//         LOGGER.error( mfE.getMessage(), mfE );
//      }
//      return Collections.emptyMap();
//   }


//   static public Collection<String> getRelatableUris( final GraphDatabaseService graphDb,
//                                                      final Collection<String> availableUris,
//                                                      final Collection<String> relationTargetUris ) {
//      final Collection<String> targetUris = new HashSet<>();
//      final Collection<String> tempList = new ArrayList<>();
//      for ( String relationTargetUri : relationTargetUris ) {
//         final Collection<String> targetableBranch = SearchUtil.getBranchUris( graphDb, relationTargetUri );
//         tempList.addAll( targetableBranch );
//         tempList.retainAll( availableUris );
//         targetUris.addAll( tempList );
//         tempList.clear();
//      }
//      return targetUris;
//   }

//   static public Collection<String> getRelatableUris( final GraphDatabaseService graphDb,
//                                                      final Collection<String> availableUris,
//                                                      final Collection<String> relationTargetUris ) {
//      final Collection<String> targetUris = new HashSet<>();
//      for ( String relationTargetUri : relationTargetUris ) {
//         if ( targetUris.contains( relationTargetUri ) ) {
//            // the relation target uri and its branch nodes are already in the collection of branch nodes.
//            continue;
//         }
//         final Collection<String> targetableBranch = SearchUtil.getBranchUris( graphDb, relationTargetUri );
//         targetUris.addAll( targetableBranch );
//      }
//      targetUris.retainAll( availableUris );
//      return targetUris;
//   }


}
