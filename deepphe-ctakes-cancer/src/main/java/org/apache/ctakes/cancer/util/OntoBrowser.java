package org.apache.ctakes.cancer.util;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.Neo4jConstants;
import org.healthnlp.deepphe.neo4j.Neo4jTraverserFactory;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.ctakes.cancer.uri.UriConstants.MALIGNANT_NEOPLASM;
import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.Neo4jConstants.IS_A_PROP;
import static org.healthnlp.deepphe.neo4j.Neo4jConstants.NAME_KEY;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/6/2019
 */
final public class OntoBrowser {

   private OntoBrowser() {}


   static private Collection<String> getChildren( final String uri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {

         Collection<String> children = SearchUtil.getChildClassNodes( graphDb, uri )
                   .stream()
                   .map( n -> n.getProperty( NAME_KEY ) )
                   .filter( Objects::nonNull )
                   .map( Object::toString )
                   .collect( Collectors.toList() );

         tx.success();
         return children;
      } catch ( MultipleFoundException mfE ) {
         System.out.println( mfE.getMessage() );
      }
      return Collections.emptyList();
   }

   public static void main( final String... args ) {

      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Invasive_Ductal_Carcinoma_Not_Otherwise_Specified" ) ) + "\n" );

//      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
//                                                                 .getGraph();
//
//      //            DIAGNOSIS_GROUP_NAMES
//      final Map<String, String> DIAGNOSIS_GROUP_NAMES = new HashMap<>();
//      final Collection<String> sites = getChildren(  "Neoplasm_by_Site" );
//      final Collection<String> morphs = getChildren( "Neoplasm_by_Morphology" );
//      final Collection<String> specials = getChildren( "Neoplasm_by_Morphology" );
//      final Collection<String> masses = getChildren(  "Mass" );
//
//      for ( String mass : masses ) {
//         final String groupText = SearchUtil.getPreferredText( graphDb, mass );
//         final Collection<String> branch = SearchUtil.getBranchUris( graphDb, mass );
//         for ( String node : branch ) {
//            final String prefText = SearchUtil.getPreferredText( graphDb, node );
//            DIAGNOSIS_GROUP_NAMES.put( prefText, groupText );
//         }
//         DIAGNOSIS_GROUP_NAMES.put( "Mass", "Mass" );
//      }
//      for ( String special : specials ) {
//         final String groupText = SearchUtil.getPreferredText( graphDb, special );
//         final Collection<String> branch = SearchUtil.getBranchUris( graphDb, special );
//         for ( String node : branch ) {
//            final String prefText = SearchUtil.getPreferredText( graphDb, node );
//            DIAGNOSIS_GROUP_NAMES.put( prefText, groupText );
//         }
//      }
//      for ( String morph : morphs ) {
//         final String groupText = SearchUtil.getPreferredText( graphDb, morph );
//         final Collection<String> branch = SearchUtil.getBranchUris( graphDb, morph );
//         for ( String node : branch ) {
//            final String prefText = SearchUtil.getPreferredText( graphDb, node );
//            DIAGNOSIS_GROUP_NAMES.put( prefText, groupText );
//         }
//      }
//      for ( String site : sites ) {
//         final String groupText = SearchUtil.getPreferredText( graphDb, site );
//         final Collection<String> branch = SearchUtil.getBranchUris( graphDb, site );
//         for ( String node : branch ) {
//            final String prefText = SearchUtil.getPreferredText( graphDb, node );
//            DIAGNOSIS_GROUP_NAMES.put( prefText, groupText );
//         }
//      }
//
//      final Collection<String> primaries = org.healthnlp.deepphe.neo4j.UriConstants.getPrimaryUris( graphDb );
//      for ( String primary : primaries ) {
//         final String prefText = SearchUtil.getPreferredText( graphDb, primary );
//         final String diagnosis = DIAGNOSIS_GROUP_NAMES.get( prefText );
//         if ( diagnosis != null ) {
//            System.out.println( "Primary " + prefText + " = " + diagnosis );
//         } else {
//            System.out.println( "Primary " + prefText + " has no group" );
//         }
//      }
//      final Collection<String> generics = org.healthnlp.deepphe.neo4j.UriConstants.getGenericTumorUris( graphDb );
//      for ( String generic : generics ) {
//         final String prefText = SearchUtil.getPreferredText( graphDb, generic );
//         final String diagnosis = DIAGNOSIS_GROUP_NAMES.get( prefText );
//         if ( diagnosis != null ) {
//            System.out.println( "Generic " + prefText + " = " + diagnosis );
//         } else {
//            System.out.println( "Generic " + prefText + " has no group" );
//         }
//      }
//      final Collection<String> metastases = org.healthnlp.deepphe.neo4j.UriConstants.getMetastasisUris( graphDb );
//      for ( String metastasis : metastases ) {
//         final String prefText = SearchUtil.getPreferredText( graphDb, metastasis );
//         final String diagnosis = DIAGNOSIS_GROUP_NAMES.get( prefText );
//         if ( diagnosis != null ) {
//            System.out.println( "Metastasis " + prefText + " = " + diagnosis );
//         } else {
//            System.out.println( "Metastasis " + prefText + " has no group" );
//         }
//      }
//      final Collection<String> benigns = org.healthnlp.deepphe.neo4j.UriConstants.getBenignTumorUris( graphDb );
//      for ( String benign : benigns ) {
//         final String prefText = SearchUtil.getPreferredText( graphDb, benign );
//         final String diagnosis = DIAGNOSIS_GROUP_NAMES.get( prefText );
//         if ( diagnosis != null ) {
//            System.out.println( "Benign " + prefText + " = " + diagnosis );
//         } else {
//            System.out.println( "Benign " + prefText + " has no group" );
//         }
//      }

//      getChildren( "Neoplasm_by_Site" ).forEach( System.out::println );
//      final Collection<String> specials = getChildren( "Neoplasm_by_Morphology" );
//      final Collection<String> morphs = getChildren( "Neoplasm_by_Morphology" );
//      final Collection<String> sites = getChildren( "Neoplasm_by_Site" );
//      final Collection<String> mass = getChildren( "Mass" );
//      final Collection<String> all = new HashSet<>( specials );
//      all.addAll( morphs );
//      all.addAll( sites );
//      all.addAll( mass );
//
//      System.out.println( all.size() );
//      all.forEach( System.out::println );


//      final Collection<String> missing = new ArrayList<>( getChildren( "Secondary_Neoplasm" ) );         // All secondary children are covered by specials, morphs and sites
//      final Collection<String> missing = new ArrayList<>( mass );         // All mass children are covered by specials, morphs and sites
//      missing.removeAll( morphs );
//      missing.removeAll( sites );
//      missing.removeAll( specials );
////      missing.removeAll( mass );
//      missing.forEach( System.out::println );         // Tons are missing.

//      Neoplasm_by_Special_Category   NO        Lots of descendants that are missing from by_site
//      Neoplasm_by_Morphology         NO        Lots of descendants that are missing from by_site
//      Neoplasm_by_Site
//          Connective_and_Soft_Tissue_Neoplasm
//                Sarcoma
//                Mesenchymal_Cell_Neoplasm
//                Bone_Neoplasm
//                Benign_Connective_and_Soft_Tissue_Neoplasm
//                Soft_Tissue_Neoplasm
//                Giant_Cell_Tumor
//          Thoracic_Neoplasm
//                  Malignant_Thoracic_Neoplasm
//                  Lung_Neoplasm
//                  Axillary_Neoplasm
//                  Sternal_Neoplasm
//                  Mediastinal_Neoplasm
//                  Chest_Wall_Neoplasm
//                  Intrathoracic_Paravertebral_Paraganglioma
//                  Benign_Thoracic_Neoplasm
//                  Pleural_Neoplasm
//                  Cardiac_Neoplasm
//          Respiratory_Tract_Neoplasm
//          Skin_Neoplasm
//          Cardiovascular_Neoplasm
//          Eye_Neoplasm
//          Reproductive_System_Neoplasm
//          Urinary_System_Neoplasm
//          Hematopoietic_and_Lymphoid_System_Neoplasm
//          Peritoneal_and_Retroperitoneal_Neoplasms
//          Digestive_System_Neoplasm
//          Head_and_Neck_Neoplasm
//          Endocrine_Neoplasm
//          Breast_Neoplasm
//          Nervous_System_Neoplasm


// Malignant_Neoplasm , Benign_Neoplasm

//      UriConstants.getCancerUris().stream()
//                  .map( Neo4jOntologyConceptUtil::getBranchUris )
//                  .flatMap( Collection::stream )
//                  .forEach( System.out::println );

//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getBranchUris( "Mass" ) ) + "\n" );
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Mass" ) ) + "\n" );


//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Cutaneous_Melanoma" ) ) + "\n" );

//      Neo4jOntologyConceptUtil.getBranchUris( MALIGNANT_NEOPLASM ).forEach( System.out::println );
//      UriConstants.getTumorUris().stream()
//                  .map( Neo4jOntologyConceptUtil::getBranchUris )
//                  .flatMap( Collection::stream )
//                  .forEach( System.out::println );

//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Biopsy_Site" ) ) + "\n" );
//      // Other_Anatomic_Concept
//
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Body" ) ) + "\n" );
//      // Other_Anatomic_Concept
//
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Lower_Eyelid" ) ) + "\n" );
//      // Eye_Appendage, Head_and_Neck_Part, Body_Part
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Conjunctiva" ) ) + "\n" );
//      // Eye_Appendage Head_and_Neck_Part Body_Part ----> is also a tissue
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Fluid" ) ) + "\n" );
//
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Branch" ) ) + "\n" );
//      // Other_Anatomic_Concept
//
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Node" ) ) + "\n" );
//      // Other_Anatomic_Concept
//
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "L3_Vertebra" ) ) + "\n" );
//      // Skeletal_System_Part Musculoskeletal_System_Part Body_Part ----> is also a tissue
//      System.out.println( String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Vertebral_Body" ) ) + "\n" );
//      // Skeletal_System_Part Musculoskeletal_System_Part Body_Part
//
//      System.out.println(
//            String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Other_Anatomic_Concept" ) ) + "\n" );
//
//      System.out.println(
//            String.join( "\n", Neo4jOntologyConceptUtil.getRootUris( "Body_Fluid_or_Substance" ) ) + "\n" );
//
//      System.out.println(
//            String.join( "\n", SearchUtil.getBranchUris( graphDb, "Body_Fluid_or_Substance" ) ) + "\n" );
//

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
