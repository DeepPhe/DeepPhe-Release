package org.healthnlp.deepphe.neo4j.util;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

import java.util.HashMap;
import java.util.Map;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.IS_A_PROP;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/27/2017
 */
public enum Neo4jTraverserFactory {
   INSTANCE;

   static public Neo4jTraverserFactory getInstance() {
      return INSTANCE;
   }

   private final Map<String, TraversalDescription> _branchTraversers = new HashMap<>();
   private final Map<String, TraversalDescription> _rootTraversers = new HashMap<>();

   private final Map<String, TraversalDescription> _childTraversers = new HashMap<>();
   private final Map<String, TraversalDescription> _parentTraversers = new HashMap<>();

   static private final Object ORDERED_ROOTS_LOCK = new Object();
   private TraversalDescription _orderedRootsTraverser;

   /**
    * @param graphDb      -
    * @param relationName -
    * @return A traverser that can walk a path down through all descendants.
    */
   public TraversalDescription getBranchTraverser( final GraphDatabaseService graphDb, final String relationName ) {
      synchronized ( _branchTraversers ) {
         final TraversalDescription traverser = _branchTraversers.get( relationName );
         if ( traverser != null ) {
            return traverser;
         }
         final RelationshipType relation = RelationshipType.withName( relationName );
         final TraversalDescription newTraverser = graphDb.traversalDescription()
                                                          .depthFirst()
                                                          .relationships( relation, Direction.INCOMING )
                                                          .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
         _branchTraversers.put( relationName, newTraverser );
         return newTraverser;
      }
   }

   /**
    * @param graphDb      -
    * @param relationName -
    * @param depth        distance from the rootUri to traverse
    * @return A traverser that can walk a path down through all descendants.
    */
   public TraversalDescription getBranchTraverser( final GraphDatabaseService graphDb, final String relationName,
                                                   final int depth ) {
      synchronized ( _branchTraversers ) {
         final TraversalDescription traverser = _branchTraversers.get( relationName + "_depth_" + depth );
         if ( traverser != null ) {
            return traverser;
         }
         final TraversalDescription noDepth = getBranchTraverser( graphDb, relationName );
         final TraversalDescription newTraverser = noDepth.evaluator( Evaluators.toDepth( depth ) );
         _branchTraversers.put( relationName + "_depth_" + depth, newTraverser );
         return newTraverser;
      }
   }

   /**
    * @param graphDb      -
    * @param relationName -
    * @return a traverser that can walk a path up through all ancestors.
    */
   public TraversalDescription getRootsTraverser( final GraphDatabaseService graphDb, final String relationName ) {
      synchronized ( _rootTraversers ) {
         final TraversalDescription traverser = _rootTraversers.get( relationName );
         if ( traverser != null ) {
            return traverser;
         }
         final RelationshipType relation = RelationshipType.withName( relationName );
         final TraversalDescription newTraverser = graphDb.traversalDescription()
                                                          .depthFirst()
                                                          .relationships( relation, Direction.OUTGOING )
                                                          .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
         _rootTraversers.put( relationName, newTraverser );
         return newTraverser;
      }
   }

   /**
    * @param graphDb      -
    * @param relationName -
    * @param depth        distance from the rootUri to traverse
    * @return a traverser that can walk a path up through all ancestors.
    */
   public TraversalDescription getRootsTraverser( final GraphDatabaseService graphDb,
                                                  final String relationName,
                                                  final int depth ) {
      synchronized ( _rootTraversers ) {
         final TraversalDescription traverser = _rootTraversers.get( relationName + "_depth_" + depth );
         if ( traverser != null ) {
            return traverser;
         }
         final TraversalDescription noDepth = getRootsTraverser( graphDb, relationName );
         final TraversalDescription newTraverser = noDepth.evaluator( Evaluators.toDepth( depth ) );
         _rootTraversers.put( relationName + "_depth_" + depth, newTraverser );
         return newTraverser;
      }
   }

   /**
    * @param graphDb      -
    * @param relationName -
    * @return A traverser that can get all immediate children.
    */
   public TraversalDescription getChildTraverser( final GraphDatabaseService graphDb, final String relationName ) {
      synchronized ( _childTraversers ) {
         final TraversalDescription traverser = _childTraversers.get( relationName );
         if ( traverser != null ) {
            return traverser;
         }
         final RelationshipType relation = RelationshipType.withName( relationName );
         final TraversalDescription newTraverser = graphDb.traversalDescription()
                                                          .depthFirst()
                                                          .relationships( relation, Direction.INCOMING )
                                                          .evaluator( Evaluators.toDepth( 1 ) )
                                                          .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
         _childTraversers.put( relationName, newTraverser );
         return newTraverser;
      }
   }

   /**
    * @param graphDb      -
    * @param relationName -
    * @return a traverser that can get all immediate parents.
    */
   public TraversalDescription getParentTraverser( final GraphDatabaseService graphDb, final String relationName ) {
      synchronized ( _parentTraversers ) {
         final TraversalDescription traverser = _parentTraversers.get( relationName );
         if ( traverser != null ) {
            return traverser;
         }
         final RelationshipType relation = RelationshipType.withName( relationName );
         final TraversalDescription newTraverser = graphDb.traversalDescription()
                                                          .depthFirst()
                                                          .relationships( relation, Direction.OUTGOING )
                                                          .evaluator( Evaluators.toDepth( 1 ) )
                                                          .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
         _parentTraversers.put( relationName, newTraverser );
         return newTraverser;
      }
   }

   /**
    * @param graphDb -
    * @return -
    */
   public TraversalDescription getOrderedRootsTraverser( final GraphDatabaseService graphDb ) {
      synchronized ( ORDERED_ROOTS_LOCK ) {
         if ( _orderedRootsTraverser == null ) {
            final RelationshipType relation = RelationshipType.withName( IS_A_PROP );
            _orderedRootsTraverser = graphDb.traversalDescription()
                                            .breadthFirst()
                                            .relationships( relation, Direction.OUTGOING )
                                            .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
         }
         return _orderedRootsTraverser;
      }
   }

}
