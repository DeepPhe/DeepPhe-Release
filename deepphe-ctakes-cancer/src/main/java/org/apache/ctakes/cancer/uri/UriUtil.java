package org.apache.ctakes.cancer.uri;


import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.Neo4jTraverserFactory;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2018
 */
final public class UriUtil {

   private UriUtil() {
   }

   static public String getExtension( final String url ) {
      final int hashIndex = url.indexOf( '#' );
      if ( hashIndex >= 0 && hashIndex < url.length() - 1 ) {
         return url.substring( hashIndex + 1 );
      }
      return url;
   }

   public static void main( final String... args ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      UriConstants.getCancerUris().stream()
                  .map( u -> SearchUtil.getBranchUris( graphDb, u ) )
                  .flatMap( Collection::stream )
                  .forEach( System.out::println );


      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Biopsy_Site" ) ) + "\n" );
      // Other_Anatomic_Concept

      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Body" ) ) + "\n" );
      // Other_Anatomic_Concept

      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Lower_Eyelid" ) ) + "\n" );
      // Eye_Appendage, Head_and_Neck_Part, Body_Part
      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Conjunctiva" ) ) + "\n" );
      // Eye_Appendage Head_and_Neck_Part Body_Part ----> is also a tissue
      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Fluid" ) ) + "\n" );

      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Branch" ) ) + "\n" );
      // Other_Anatomic_Concept

      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Node" ) ) + "\n" );
      // Other_Anatomic_Concept

      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "L3_Vertebra" ) ) + "\n" );
      // Skeletal_System_Part Musculoskeletal_System_Part Body_Part ----> is also a tissue
      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Vertebral_Body" ) ) + "\n" );
      // Skeletal_System_Part Musculoskeletal_System_Part Body_Part

      System.out.println(
            String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Other_Anatomic_Concept" ) ) + "\n" );

      System.out.println(
            String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Body_Fluid_or_Substance" ) ) + "\n" );

      System.out.println(
            String.join( "\n", SearchUtil.getBranchUris( graphDb, "Body_Fluid_or_Substance" ) ) + "\n" );


      System.out.println();

   }


   static private final RelationshipType IS_A_RELATION = RelationshipType.withName( IS_A_PROP );

   static private Node createAllPatientsNode() {
      final Node thingNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.THING );
      if ( thingNode == null ) {
         System.err.println( "No " + UriConstants.THING + " node!  Cannot create put Subject in graph." );
         return null;
      }
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();

      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );
      final Node subjectNode;
      try ( Transaction tx = graphDb.beginTx() ) {
         subjectNode = graphDb.createNode();
         subjectNode.setProperty( NAME_KEY, "Subject" );
         subjectNode.addLabel( CLASS_LABEL );
         subjectNode.createRelationshipTo( thingNode, IS_A_RELATION );
         tx.success();
      }
      System.out.println( "Subjects count : " + getClassNodes( "Subject" ).size() );
      getClassNodes( "Subject" ).forEach( n -> printNodeInfo( graphDb, "Subject", n ) );

      final Node allPatientsNode;
      try ( Transaction tx2 = graphDb.beginTx() ) {
         allPatientsNode = graphDb.createNode();
         allPatientsNode.setProperty( NAME_KEY, "Patient" );
         allPatientsNode.addLabel( CLASS_LABEL );
         allPatientsNode.createRelationshipTo( subjectNode, IS_A_RELATION );
         tx2.success();
      }
      System.out.println( "Patients count : " + getClassNodes( "Patient" ).size() );
      getClassNodes( "Patient" ).forEach( n -> printNodeInfo( graphDb, "Patient", n ) );

      return allPatientsNode;
   }

   static private void printNodeInfo( GraphDatabaseService graphDb, final String uri, final Node node ) {
      if ( node == null ) {
         System.err.println( "Null Node for " + uri + " !" );
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         System.out.println( "\n" + uri + " ..." );
         node.getLabels().iterator().forEachRemaining( l -> System.out.println( "Label : " + l.name() ) );
         node.getRelationships()
             .iterator()
             .forEachRemaining( r -> System.out.println( "Relation : " + r.getType().name() ) );
         node.getPropertyKeys().iterator().forEachRemaining( p -> System.out.println( "Property : " + p ) );
         node.getPropertyKeys()
             .iterator()
             .forEachRemaining( p -> System.out.println( "Value : " + node.getProperty( p ).toString() ) );
         tx.success();
      }
   }

   static private UniqueFactory.UniqueNodeFactory createNodeFactory( final GraphDatabaseService graphDb ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         Arrays.stream( graphDb.index().nodeIndexNames() ).forEach( i -> System.out.println( "Index : " + i ) );
         final UniqueFactory.UniqueNodeFactory factory = new UniqueFactory.UniqueNodeFactory( graphDb, NAME_INDEX ) {
            @Override
            protected void initialize( final Node created, final Map<String, Object> properties ) {
               // set a name for the node
               created.setProperty( NAME_KEY, properties.get( NAME_KEY ) );
            }
         };
         tx.success();
         return factory;
      }
   }

   /**
    * @param dPheUri -
    * @return neo4j Object for the given URI.  Objects are actual mentions discovered in text.
    */
   static public List<Node> getObjectNodes( final String dPheUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final ResourceIterator<Node> nodes = graphDb.findNodes( OBJECT_LABEL, NAME_KEY, dPheUri );
         tx.success();
         return nodes.stream().collect( Collectors.toList() );
      } catch ( MultipleFoundException mfE ) {
         System.err.println( mfE.getMessage() );
      }
      return Collections.emptyList();
   }

   /**
    * @param dPheUri -
    * @return neo4j Object for the given URI.  Objects are actual mentions discovered in text.
    */
   static public List<Node> getClassNodes( final String dPheUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final ResourceIterator<Node> nodes = graphDb.findNodes( CLASS_LABEL, NAME_KEY, dPheUri );
         tx.success();
         return nodes.stream().collect( Collectors.toList() );
      } catch ( MultipleFoundException mfE ) {
         System.err.println( mfE.getMessage() );
      }
      return Collections.emptyList();
   }


   static private void printMultipleRoots( final GraphDatabaseService graphDb, final String leafUri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final ResourceIterator<Node> rootLeaves = SearchUtil.getClassNodes( graphDb, leafUri );
         if ( rootLeaves == null ) {
            System.err.println( "printMultipleRoots(..) : No Class exists for URI " + leafUri );
            tx.success();
            return;
         }

         Node node;
         try {
            while ( rootLeaves.hasNext() ) {
               node = rootLeaves.next();
               System.out.println( node.getProperty( NAME_KEY ) );
            }
         } catch ( Throwable t ) {
            System.err.println( t.getMessage() );
         }


         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getRootsTraverser( graphDb, IS_A_PROP );
         for ( Node rootLeaf : new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
               return rootLeaves;
            }
         } ) {
            try {
               System.out.println( rootLeaf.getProperty( NAME_KEY ) );
               System.out.println( traverser.traverse( rootLeaf )
                                            .nodes()
                                            .stream()
                                            .map( n -> n.getProperty( NAME_KEY ) )
                                            .map( Object::toString )
                                            .distinct()
                                            .collect( Collectors.joining( "\n" ) ) );
            } catch ( NotFoundException nfE ) {
               System.err.println( nfE.getMessage() );
            }
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         System.err.println( mfE.getMessage() );
      }
   }


   static private Collection<String> getTargets( final Iterable<Relationship> relationships ) {
      if ( relationships == null ) {
         return Collections.emptyList();
      }
      return StreamSupport.stream( relationships.spliterator(), false )
                          .map( Relationship::getEndNode )
                          .map( n -> n.getProperty( NAME_KEY ) )
                          .map( Object::toString )
                          .collect( Collectors.toList() );
   }

   static private void printBranchUrisWithRelation( final String rootUri, final String relationName ) {
      // See https://neo4j.com/docs/java-reference/current/#tutorial-traversal
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = SearchUtil.getClassNode( graphDb, rootUri );
         if ( branchRoot == null ) {
            System.err.println( "printBranchUrisWithRelation(..) : No Class exists for URI " + rootUri );
            tx.success();
            return;
         }
         final RelationshipType relation = RelationshipType.withName( relationName );
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, IS_A_PROP );

         for ( Node node : traverser.traverse( branchRoot ).nodes() ) {
            final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING, relation );
            if ( relationships == null ) {
               continue;
            }
            final Collection<String> targets = getTargets( relationships );
            if ( targets.isEmpty() ) {
               continue;
            }
            final String name = node.getProperty( NAME_KEY ).toString();
            for ( String child : SearchUtil.getBranchUris( graphDb, name ) ) {
               System.out.println( child + " : " + String.join( " ", targets ) );
            }
         }
      } catch ( MultipleFoundException mfE ) {
         System.err.println( mfE.getMessage() );
      }
   }


}
