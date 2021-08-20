package org.healthnlp.deepphe.neo4j.util;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.logging.Log;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/22/2018
 */
final public class SearchUtil {

   static private final Logger LOGGER = Logger.getLogger( "SearchUtil" );

   private SearchUtil() {
   }


    static public String getAttributeByNameAsString(final GraphDatabaseService graphDb,
                                            final Log log,
                                            final Node cancerNode,
                                            String attribute) {

      List<String> attributeList = new ArrayList<>();
      SearchUtil.getOutRelatedNodes(graphDb, cancerNode, NEOPLASM_HAS_ATTRIBUTE_RELATION)
              .stream()
              .filter(n -> DataUtil.objectToString(n.getProperty(ATTRIBUTE_NAME)).equals(attribute))
              .map(n -> DataUtil.objectToString(n.getProperty(ATTRIBUTE_VALUE)))
              .forEach(attributeList::add);
      if (attributeList.size() > 0) {
         return String.join(" --- ", attributeList);
      } else {
         return null;
      }


   }
   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri uri for the root of a branch
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriBranchClasses( final GraphDatabaseService graphDb, final String rootUri ) {
      return getUriBranchClasses( graphDb, rootUri, IS_A_PROP );
   }

   /**
    * @param graphDb      graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri      uri for the root of a branch
    * @param relationName name of the relation type
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriBranchClasses( final GraphDatabaseService graphDb, final String rootUri,
                                                       final String relationName ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = getClassNode( graphDb, rootUri );
         if ( branchRoot == null ) {
            tx.success();
            LOGGER.debug( "getUriBranchClasses(..) : No Class exists for URI " + rootUri );
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, relationName );
         final Collection<Node> branch = traverser.traverse( branchRoot ).nodes().stream()
                                                  .collect( Collectors.toSet() );
         tx.success();
         return branch;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri uri for the root of a branch
    * @param depth   distance from the rootUri to traverse
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriBranchClasses( final GraphDatabaseService graphDb, final String rootUri,
                                                       final int depth ) {
      return getUriBranchClasses( graphDb, rootUri, IS_A_PROP, depth );
   }

   /**
    * @param graphDb      graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri      uri for the root of a branch
    * @param relationName name of the relation type
    * @param depth        distance from the rootUri to traverse
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriBranchClasses( final GraphDatabaseService graphDb, final String rootUri,
                                                       final String relationName, final int depth ) {
      return getUriBranchClasses( graphDb, rootUri, CLASS_LABEL, relationName, depth );
   }

   /**
    * @param graphDb      graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri      uri for the root of a branch
    * @param label        -
    * @param relationName name of the relation type
    * @param depth        distance from the rootUri to traverse
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriBranchClasses( final GraphDatabaseService graphDb,
                                                       final String rootUri,
                                                       final Label label,
                                                       final String relationName,
                                                       final int depth ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = getLabeledNode( graphDb, label, rootUri );
         if ( branchRoot == null ) {
            tx.success();
            LOGGER.debug( "getUriBranchClasses(..) : No Class exists for URI " + rootUri );
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, relationName, depth );
         final Collection<Node> branch = traverser.traverse( branchRoot ).nodes().stream()
                                                  .collect( Collectors.toSet() );
         tx.success();
         return branch;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri uri for the root of a branch
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriRootClasses( final GraphDatabaseService graphDb, final String rootUri ) {
      return getUriRootClasses( graphDb, rootUri, IS_A_PROP );
   }

   /**
    * @param graphDb      graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri      uri for the root of a branch
    * @param relationName name of the relation type
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriRootClasses( final GraphDatabaseService graphDb, final String rootUri,
                                                     final String relationName ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootBase = getClassNode( graphDb, rootUri );
         if ( rootBase == null ) {
            tx.success();
            LOGGER.debug( "getUriRootClassesS(..) : No Class exists for URI " + rootUri );
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getRootsTraverser( graphDb, relationName );
         final Collection<Node> roots = traverser.traverse( rootBase ).nodes().stream()
                                                 .collect( Collectors.toSet() );
         tx.success();
         return roots;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   /**
    * @param graphDb      graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri      uri for the root of a branch
    * @param label        -
    * @param relationName name of the relation type
    * @param depth        distance from the rootUri to traverse
    * @return all of the neo4j nodes in the branch, starting with the branch root
    */
   static public Collection<Node> getUriRootClasses( final GraphDatabaseService graphDb,
                                                     final String rootUri,
                                                     final Label label,
                                                     final String relationName,
                                                     final int depth ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootBase = getLabeledNode( graphDb, label, rootUri );
         if ( rootBase == null ) {
            tx.success();
            LOGGER.debug( "getUriRootClassesS(..) : No Class exists for URI " + rootUri );
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getRootsTraverser( graphDb, relationName, depth );
         final Collection<Node> roots = traverser.traverse( rootBase ).nodes().stream()
                                                 .collect( Collectors.toSet() );
         tx.success();
         return roots;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<Node> getUriChildNodes( final GraphDatabaseService graphDb,
                                                    final String parentUri,
                                                    final Label label,
                                                    final String relationName ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node parentNode = getLabeledNode( graphDb, label, parentUri );
         if ( parentNode == null ) {
            tx.success();
            LOGGER.debug( "getUriChildNodes(..) : No Node exists for URI " + parentUri );
            return Collections.emptyList();
         }
         final Collection<Node> childNodes = new ArrayList<>();
         for ( Relationship relation : parentNode.getRelationships( RelationshipType.withName( relationName ) ) ) {
            childNodes.add( relation.getOtherNode( parentNode ) );
         }
         tx.success();
         return childNodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( parentUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<Node> getChildClassNodes( final GraphDatabaseService graphDb,
                                                      final String parentUri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node parentNode = getLabeledNode( graphDb, Neo4jConstants.CLASS_LABEL, parentUri );
         if ( parentNode == null ) {
            tx.success();
            LOGGER.debug( "getChildClassNodes(..) : No Node exists for URI " + parentUri );
            return Collections.emptyList();
         }
         final Collection<Node> childNodes = new ArrayList<>();
         for ( Relationship relation : parentNode.getRelationships( Direction.INCOMING, IS_A_RELATION ) ) {
            childNodes.add( relation.getOtherNode( parentNode ) );
         }
         tx.success();
         return childNodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( parentUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<Node> getParentClassNodes( final GraphDatabaseService graphDb,
                                                      final String childUri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node parentNode = getLabeledNode( graphDb, Neo4jConstants.CLASS_LABEL, childUri );
         if ( parentNode == null ) {
            tx.success();
            LOGGER.debug( "getParentClassNodes(..) : No Node exists for URI " + childUri );
            return Collections.emptyList();
         }
         final Collection<Node> childNodes = new ArrayList<>();
         for ( Relationship relation : parentNode.getRelationships( Direction.OUTGOING, IS_A_RELATION ) ) {
            childNodes.add( relation.getOtherNode( parentNode ) );
         }
         tx.success();
         return childNodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( childUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }


   /**
    * \
    *
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri -
    * @return all uris down to the final leaves
    */
   static public Collection<String> getBranchUris( final GraphDatabaseService graphDb, final String rootUri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = getClassNode( graphDb, rootUri );
         if ( branchRoot == null ) {
            tx.success();
            LOGGER.debug( "getBranchUris(..) : No Class exists for URI " + rootUri );
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, IS_A_PROP );
         final Collection<String> branch = traverser.traverse( branchRoot )
                                                    .nodes()
                                                    .stream()
                                                    .map( n -> n.getProperty( NAME_KEY ) )
                                                    .filter( Objects::nonNull )
                                                    .map( Object::toString )
                                                    .collect( Collectors.toSet() );
         tx.success();
         return branch;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param leafUri -
    * @return all uris back to the root (Thing)
    */
   static public Collection<String> getRootUris( final GraphDatabaseService graphDb, final String leafUri ) {
      if ( leafUri == null || leafUri.isEmpty() ) {
         new Exception( "getRootUris(..) : Empty or null URI" ).printStackTrace();
      }
      if ( leafUri.equals( UriConstants.EVENT ) ) {
         return Arrays.asList( UriConstants.THING, UriConstants.EVENT );
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootLeaf = getClassNode( graphDb, leafUri );
         if ( rootLeaf == null ) {
            LOGGER.debug( "getRootUris(..) : No Class exists for URI " + leafUri );
            tx.success();
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getRootsTraverser( graphDb, IS_A_PROP );
         final Collection<String> root = traverser.traverse( rootLeaf )
                                                  .nodes()
                                                  .stream()
                                                  .map( n -> n.getProperty( NAME_KEY ) )
                                                  .filter( Objects::nonNull )
                                                  .map( Object::toString )
                                                  .collect( Collectors.toSet() );
         tx.success();
         return root;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( leafUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static private boolean hasTarget( final Iterable<Relationship> relationships, final Node target ) {
      return relationships != null && StreamSupport.stream( relationships.spliterator(), false )
                                                   .map( Relationship::getEndNode )
                                                   .anyMatch( target::equals );
   }

   /**
    * @param graphDb      graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param rootUri      -
    * @param relationName -
    * @param targetUri    -
    * @return -
    */
   static public Collection<String> getBranchUrisWithRelation( final GraphDatabaseService graphDb,
                                                               final String rootUri,
                                                               final String relationName,
                                                               final String targetUri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = getClassNode( graphDb, rootUri );
         if ( branchRoot == null ) {
            tx.success();
            LOGGER.debug( "getBranchUrisWithRelation(..) : No Class exists for URI " + rootUri );
            return Collections.emptyList();
         }
         final Node targetNode = getClassNode( graphDb, targetUri );
         if ( targetNode == null ) {
            tx.success();
            LOGGER.debug( "getBranchUrisWithRelation(..) : No Class exists for URI " + targetUri );
            return Collections.emptyList();
         }
         final RelationshipType relation = RelationshipType.withName( relationName );
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, IS_A_PROP );
         final Collection<String> targetRoots
               = traverser.traverse( branchRoot )
                          .nodes()
                          .stream()
                          .filter( n -> hasTarget( n.getRelationships( Direction.OUTGOING, relation ), targetNode ) )
                          .map( n -> n.getProperty( NAME_KEY ) )
                          .filter( Objects::nonNull )
                          .map( Object::toString )
                          .collect( Collectors.toSet() );
         tx.success();
         return targetRoots.stream()
                           .map( u -> getBranchUris( graphDb, u ) )
                           .flatMap( Collection::stream )
                           .collect( Collectors.toSet() );
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " , " + targetUri + " " + relationName + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<String> getBranchUrisWithAttribute( final GraphDatabaseService graphDb,
                                                                final String rootUri,
                                                                final String attributeName,
                                                                final String value ) {
      // See https://neo4j.com/docs/java-reference/current/#tutorial-traversal
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = SearchUtil.getClassNode( graphDb, rootUri );
         if ( branchRoot == null ) {
            LOGGER.debug( "getBranchUrisWithAttribute(..) : No Class exists for URI " + rootUri );
            tx.success();
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, IS_A_PROP );
         final Collection<String> targetRoots
               = traverser.traverse( branchRoot )
                          .nodes()
                          .stream()
                          .filter( n -> n.getProperty( attributeName, "" ).equals( value ) )
                          .map( n -> n.getProperty( NAME_KEY ) )
                          .filter( Objects::nonNull )
                          .map( Object::toString )
                          .collect( Collectors.toSet() );
         tx.success();
         return targetRoots.stream()
                           .map( u -> SearchUtil.getBranchUris( graphDb, u ) )
                           .flatMap( Collection::stream )
                           .collect( Collectors.toSet() );
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " , " + attributeName + " " + value + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param uri     -
    * @return -
    */
   static public String getCui( final GraphDatabaseService graphDb, final String uri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node graphNode = getClassNode( graphDb, uri );
         if ( graphNode == null ) {
            tx.success();
            LOGGER.debug( "getCui(..) : No Class exists for URI " + uri );
            return "";
         }
         final Object property = graphNode.getProperty( CUI_KEY );
         if ( property == null ) {
            tx.success();
            return "";
         }
         tx.success();
         return property.toString();
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
      }
      return "";
   }



   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param uri     -
    * @return -
    */
   static public String getPreferredText( final GraphDatabaseService graphDb, final String uri ) {
      if ( uri.equals( UriConstants.EVENT ) ) {
         return "Event";
      }
      String prefText = toPreferredText( uri );
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node graphNode = getClassNode( graphDb, uri );
         if ( graphNode == null ) {
            tx.success();
            LOGGER.debug( "getPreferredText(..) : No Class exists for URI " + uri );
            return prefText;
         }
         final Object property = graphNode.getProperty( PREF_TEXT_KEY );
         if ( property == null ) {
            LOGGER.debug( "No Preferred Text for URI " + uri );
         } else {
            prefText = property.toString();
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
      }
      return prefText;
   }

   static private String toPreferredText( final String uri ) {
      final int hashIndex = uri.lastIndexOf( '#' );
      if ( hashIndex < 0 ) {
         return uri.replace( '_', ' ' );
      }
      if ( hashIndex == uri.length() - 1 ) {
         LOGGER.debug( "Cannot get extension, URI ends with hash " + uri );
         return uri.replace( '_', ' ' );
      }
      return uri.substring( hashIndex + 1 )
                .replace( '_', ' ' );
   }

   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param uri -
    * @return neo4j Classes for the given URI.  Classes are mention types, not mentions discovered in text.
    */
   static public Node getClassNode( final GraphDatabaseService graphDb, final String uri ) {
      return getLabeledNode( graphDb, CLASS_LABEL, uri );
   }


   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param uri -
    * @return neo4j Objects for the given URI.  Classes are mention types, not mentions discovered in text.
    */
   static public Node getObjectNode( final GraphDatabaseService graphDb, final String uri ) {
      return getLabeledNode( graphDb, OBJECT_LABEL, uri );
   }

   static public ResourceIterator<Node> getClassNodes( final GraphDatabaseService graphDb, final String uri ) {
      return getLabeledNodes( graphDb, CLASS_LABEL, uri );
   }

   /**
    * @param graphDb graph database service.  Passed manually so that it can be injected with procedure calls.
    * @param label   neo4j label for the node
    * @param id      dPhe name for the node
    * @return neo4j Object for the given id.
    */
   static public Node getLabeledNode( final GraphDatabaseService graphDb, final Label label, final String id ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node node = graphDb.findNode( label, NAME_KEY, id );
         tx.success();
         return node;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return null;
   }

   static public ResourceIterator<Node> getLabeledNodes( final GraphDatabaseService graphDb, final Label label,
                                                         final String uri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final ResourceIterator<Node> nodes = graphDb.findNodes( label, NAME_KEY, uri );
         tx.success();
         return nodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return null;
   }

   static public Node getOrCreateLabeledNode( final GraphDatabaseService graphDb,
                                              final Label label,
                                              final String id ) {
      final Node extantNode = getLabeledNode( graphDb, label, id );
      if ( extantNode != null ) {
         return extantNode;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node newNode = graphDb.createNode( label );
         newNode.setProperty( NAME_KEY, id );
         tx.success();
         return newNode;
      } catch ( TransactionFailureException txE ) {
         LOGGER.error( "Cannot create create Node " + label.name() + " " + id );
         LOGGER.error( txE.getMessage() );
      } catch ( Exception e ) {
         // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
         LOGGER.error( "Ignoring Exception while creating Node "
                    + label.name() + " " + id + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
         // Attempt to continue.
      }
      return null;
   }

   static public Collection<Node> getOutRelatedNodes( final GraphDatabaseService graphDb,
                                                      final Node node,
                                                      final String relationName ) {
      return getOutRelatedNodes( graphDb, node, RelationshipType.withName( relationName ) );
   }


   static public Collection<Node> getOutRelatedNodes( final GraphDatabaseService graphDb,
                                                      final Node node,
                                                      final RelationshipType relationType ) {
      if ( node == null ) {
         return Collections.emptyList();
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Collection<Node> relatedNodes = new ArrayList<>();
         for ( Relationship relation : node.getRelationships( Direction.OUTGOING, relationType ) ) {
            relatedNodes.add( relation.getOtherNode( node ) );
         }
         tx.success();
         return relatedNodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<Node> getAllOutRelatedNodes( final GraphDatabaseService graphDb,
                                                         final Node node ) {
      if ( node == null ) {
         return Collections.emptyList();
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Collection<Node> relatedNodes = new ArrayList<>();
         for ( Relationship relation : node.getRelationships( Direction.OUTGOING ) ) {
            relatedNodes.add( relation.getOtherNode( node ) );
         }
         tx.success();
         return relatedNodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<Relationship> getAllOutRelationships( final GraphDatabaseService graphDb,
                                                                  final Log log,
                                                                  final Node node,
                                                                  final Collection<Relationship> knownRelationships ) {
      if ( node == null ) {
         return Collections.emptyList();
      }
      try ( Transaction tx = graphDb.beginTx() ) {
//         log.info( "Getting Out Relations for " + node.getProperty( NAME_KEY ) );
         final Collection<Relationship> relationships = new HashSet<>();
         for ( Relationship relation : node.getRelationships( Direction.OUTGOING ) ) {
            if ( !knownRelationships.contains( relation ) ) {
//               log.info( "   Relation " + relation.getType().name()
//                         + " to " + relation.getEndNode().getProperty( NAME_KEY ) );
               relationships.add( relation );
            }
         }
         tx.success();
         return relationships;
      } catch ( TransactionFailureException txE ) {
         log.error( txE.getMessage() );
      } catch ( MultipleFoundException mfE ) {
         log.error( mfE.getMessage() );
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<Relationship> getBranchAllOutRelationships( final GraphDatabaseService graphDb,
                                                                        final Log log,
                                                                        final Node node,
                                                                        final Collection<Relationship> branchRelationships,
                                                                        final Collection<Node> handledNodes ) {
      if ( handledNodes.contains( node ) ) {
         return Collections.emptyList();
      }
      handledNodes.add( node );
      final Collection<Relationship> relationships = getAllOutRelationships( graphDb, log, node, branchRelationships );
      for ( Relationship relationship : relationships ) {
         if ( relationship.isType( INSTANCE_OF_RELATION ) ) {
            branchRelationships.add( relationship );
            continue;
         }
         final Node endNode = relationship.getEndNode();
         if ( endNode != null ) {
            branchRelationships.addAll( getBranchAllOutRelationships( graphDb, log, endNode, branchRelationships,
                                                                      handledNodes ) );
         }
      }
      return branchRelationships;
   }

   static public boolean hasOutRelation( final GraphDatabaseService graphDb,
                                         final Node node,
                                         final RelationshipType relationType ) {
      if ( node == null ) {
         return false;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         boolean hasRelation = node.hasRelationship( relationType, relationType );
         tx.success();
         return hasRelation;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return false;
   }


   static public Collection<Node> getInRelatedNodes( final GraphDatabaseService graphDb,
                                                     final Node node,
                                                     final RelationshipType relationType ) {
      if ( node == null ) {
         return Collections.emptyList();
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Collection<Node> relatedNodes = new ArrayList<>();
         for ( Relationship relation : node.getRelationships( Direction.INCOMING, relationType ) ) {
            relatedNodes.add( relation.getOtherNode( node ) );
         }
         tx.success();
         return relatedNodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }


   static public Collection<String> getObjectUris( final GraphDatabaseService graphDb,
                                                   final Node node ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Collection<String> uris
               = getOutRelatedNodes( graphDb, node, INSTANCE_OF_RELATION ).stream()
                                                                          .map( n -> n.getProperty( NAME_KEY ) )
                                                                          .filter( Objects::nonNull )
                                                                          .map( Object::toString )
                                                                          .filter( u -> !u.isEmpty() )
                                                                          .distinct()
                                                                          .sorted()
                                                                          .collect( Collectors.toList() );
         tx.success();
         return uris;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

}
