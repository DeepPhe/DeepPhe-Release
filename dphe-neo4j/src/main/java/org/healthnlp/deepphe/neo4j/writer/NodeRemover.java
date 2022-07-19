package org.healthnlp.deepphe.neo4j.writer;

import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.util.Collection;
import java.util.HashSet;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {7/13/2022}
 */
public class NodeRemover {

   /**
    * Clear all existing Notes and Summaries from the patient.
    * @param graphDb -
    * @param log -
    * @param patientNode -
    */
   static public void clearPatient(  final GraphDatabaseService graphDb,
                                     final Log log,
                                     final Node patientNode ) {
      log.info( "Clearing old Patient information." );
      final Collection<Node> clearableNodes = new HashSet<>();
      final Collection<Relationship> clearableRelationships = new HashSet<>();
      try ( Transaction tx = graphDb.beginTx() ) {
         collectClearables( graphDb, log, patientNode, new HashSet<>(), new HashSet<>(),
                            clearableNodes, clearableRelationships );
//         final Collection<Node> noteNodes = SearchUtil.getOutRelatedNodes( graphDb, patientNode,
//                                                                           SUBJECT_HAS_NOTE_RELATION );
//         for ( Node noteNode : noteNodes ) {
//            final Collection<Relationship> noteIsRelations = SearchUtil.getOutRelationships( graphDb, log,
//                                                                                             noteNode,
//                                                                                             INSTANCE_OF_RELATION );
//            clearableRelationships.addAll( noteIsRelations );
//         }
//         final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes( graphDb, patientNode,
//                                                                           SUBJECT_HAS_CANCER_RELATION );
//         for ( Node cancerNode : cancerNodes ) {
//            final Collection<Relationship> cancerIsRelations = SearchUtil.getOutRelationships( graphDb, log,
//                                                                                               cancerNode,
//                                                                                             INSTANCE_OF_RELATION );
//            clearableRelationships.addAll( cancerIsRelations );
//         }
//         clearableNodes.remove( patientNode );
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         log.error( mfE.getMessage(), mfE );
      }
      removeUnwanted( graphDb, log, clearableNodes, clearableRelationships );
   }


   static public void collectClearables( final GraphDatabaseService graphDb,
                                          final Log log,
                                          final Node node,
                                          final Collection<Relationship> knownRelationships,
                                          final Collection<Node> handledNodes,
                                          final Collection<Node> clearableNodes,
                                          final Collection<Relationship> clearableRelationships ) {
      if ( handledNodes.contains( node ) ) {
         return;
      }
      handledNodes.add( node );
//      log.info( "Collecting Clearables for: " + DataUtil.objectToString( node.getProperty( NAME_KEY ) ) );
      final Collection<Relationship> relationships = SearchUtil.getAllOutRelationships( graphDb, log, node,
                                                                                        knownRelationships );
      for ( Relationship relationship : relationships ) {
         if ( relationship.isType( INSTANCE_OF_RELATION ) ) {
            clearableRelationships.add( relationship );
            continue;
         }
         final Node startNode = relationship.getStartNode();
         final Node endNode = relationship.getEndNode();
         if ( startNode != null ) {
            clearableRelationships.add( relationship );
//            log.info( "  Relationship: " + relationship.getType().name() );
         }
         if ( endNode != null ) {
            clearableNodes.add( endNode );
            collectClearables( graphDb, log, endNode, knownRelationships,
                               handledNodes, clearableNodes, clearableRelationships );
         }
      }
   }

   static private void removeUnwanted( final GraphDatabaseService graphDb,
                                final Log log,
                                final Collection<Node> clearableNodes,
                                final Collection<Relationship> clearableRelationships ) {
      if ( !clearableRelationships.isEmpty() ) {
         try ( Transaction tx = graphDb.beginTx() ) {
            for ( Relationship relationship : clearableRelationships ) {
//               log.info( "Deleting Relationship " + relationship.getType()
//                                                                .name()
//                         + " " + relationship.getStartNode()
//                                             .getProperty( NAME_KEY )
//                         + " " + relationship.getStartNode().getId()
//                         + " " + relationship.getEndNode()
//                                             .getProperty( NAME_KEY )
//                         + " " + relationship.getEndNode().getId() );
               relationship.delete();
//               log.info( "Relation deletion done" );
            }
            tx.success();
         } catch ( MultipleFoundException mfE ) {
            log.error( mfE.getMessage(), mfE );
         }
      }
      if ( clearableNodes.isEmpty() ) {
         return;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         for ( Node node : clearableNodes ) {
//            log.info( "Deleting Node " + node.getProperty( NAME_KEY ) + " " + node.getId() );
            final Collection<Relationship> relationships = SearchUtil.getAllRelationships( graphDb, log, node );
            for ( Relationship relationship : relationships ) {
               log.info( relationship.getStartNode().getId()
                         + " " + relationship.getType().name()
                         + " " + relationship.getEndNode().getId() );
            }
            node.delete();
//            log.info( "Node deletion done" );
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         log.error( mfE.getMessage(), mfE );
      }
   }


}
