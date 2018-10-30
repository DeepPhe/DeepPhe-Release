package org.healthnlp.deepphe.viz.neo4j;


import apoc.result.VirtualNode;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/6/2018
 */
public class DataUtil {

   static private final Logger LOGGER = Logger.getLogger( "DataUtil" );

   static private final boolean INCLUDE_EMPTY_PROPERTIES = false;
   static private final boolean LOWER_CASE_RELATIONS = false;


   // for now these constants are in FHIRConstants.
   public static final String HAS_PATHOLOGIC_T_CLASSIFICATION = "hasPathologicTClassification";
   public static final String HAS_PATHOLOGIC_N_CLASSIFICATION = "hasPathologicNClassification";
   public static final String HAS_PATHOLOGIC_M_CLASSIFICATION = "hasPathologicMClassification";
   public static final String HAS_CLINICAL_T_CLASSIFICATION = "hasClinicalTClassification";
   public static final String HAS_CLINICAL_N_CLASSIFICATION = "hasClinicalNClassification";
   public static final String HAS_CLINICAL_M_CLASSIFICATION = "hasClinicalMClassification";
   public static final String TNM_STAGE = "TNM_Staging_System";

   // For now in Neo4jSummaryWriter
   static public final String SUMMARY_FACT_RELATION_TYPE = "Summary_Fact_Relation_Type";

   /**
    * works with objects
    */
   static public boolean isSemanticType( final String labelName ) {
      return labelName.equals( OBJECT_LABEL.name() );
   }

   static Node getPatientRoot( final GraphDatabaseService graphDb ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientRoot = SearchUtil.getLabeledNode( graphDb, SUBJECT_LABEL, PATIENT_URI );
         tx.success();
         return patientRoot;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return null;
   }

   static Collection<Node> getAllPatientNodes( final GraphDatabaseService graphDb ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientRoot = getPatientRoot( graphDb );
         if ( patientRoot == null ) {
            tx.success();
            return Collections.emptyList();
         }
         final Collection<Node> patients = new ArrayList<>();
         for ( Relationship instanceOf : patientRoot.getRelationships( INSTANCE_OF_RELATION, Direction.INCOMING ) ) {
            final Node patient = instanceOf.getOtherNode( patientRoot );
            if ( patient != null ) {
               patients.add( patient );
            }
         }
         tx.success();
         return patients;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Collections.emptyList();
   }

   static Collection<Node> getAllLabeledPatientNodes( final GraphDatabaseService graphDb ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final NodeNameComparator comparator = new NodeNameComparator( graphDb );
         final List<Node> patients = graphDb.findNodes( SUBJECT_LABEL ).stream()
                                            .filter( n -> n.hasLabel( OBJECT_LABEL ) )
                                            .sorted( comparator )
                                            .collect( Collectors.toList() );
         tx.success();
         return patients;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Collections.emptyList();
   }

   static Node getInstanceClass( final GraphDatabaseService graphDb, final Node instance ) {
      if ( instance == null ) {
         return null;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Relationship instanceOf = instance.getSingleRelationship( INSTANCE_OF_RELATION, Direction.OUTGOING );
         if ( instanceOf == null ) {
            tx.success();
            return null;
         }
         final Node classNode = instanceOf.getOtherNode( instance );
         tx.success();
         return classNode;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return null;
   }


   static String getNodeName( final GraphDatabaseService graphDb, final Node node ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Object property = node.getProperty( NAME_KEY );
         tx.success();
         final String name = objectToString( property );
         if ( !name.isEmpty() ) {
            return name;
         }
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return MISSING_NODE_NAME;
   }

   static String getPreferredText( final GraphDatabaseService graphDb, final Node node ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node typeClass = getInstanceClass( graphDb, node );
         final String prefText = objectToString( typeClass.getProperty( PREF_TEXT_KEY ) );
         tx.success();
         return prefText;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return "";
   }

   static String getUri( final GraphDatabaseService graphDb, final Node node ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node typeClass = getInstanceClass( graphDb, node );
         final String uri = getNodeName( graphDb, typeClass );
         tx.success();
         return uri;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return "";
   }

   static String objectToString( final Object stringObject ) {
      if ( stringObject == null ) {
         return "";
      }
      return stringObject.toString();
   }

   static int objectToInt( final Object intObject ) {
      if ( intObject == null ) {
         return Integer.MIN_VALUE;
      }
      if ( intObject instanceof Integer ) {
         return (Integer)intObject;
      }
      try {
         return Integer.parseInt( intObject.toString() );
      } catch ( NumberFormatException nfE ) {
         return Integer.MIN_VALUE;
      }
   }

   static VirtualNode createSimpleVirtual( final GraphDatabaseService graphDb,
                                           final Node node,
                                           final String... wantedProperties ) {
      return createVirtualNode( graphDb, node, INCLUDE_EMPTY_PROPERTIES, wantedProperties );
   }

   static VirtualNode createVirtualNode( final GraphDatabaseService graphDb,
                                         final Node node,
                                         final boolean includeEmpty,
                                         final String... wantedProperties ) {
      final Collection<String> wantedNames = Arrays.asList( wantedProperties );
      try ( Transaction tx = graphDb.beginTx() ) {
         final VirtualNode virtual = new VirtualNode( node.hashCode(), graphDb );
         node.getLabels().forEach( virtual::addLabel );
         if ( wantedNames.isEmpty() ) {
            virtual.setProperty( NAME_KEY, getNodeName( graphDb, node ) );
         } else {
            for ( Map.Entry<String, Object> property : node.getAllProperties().entrySet() ) {
               if ( property.getKey().equals( NAME_KEY )
                    || (wantedNames.contains( property.getKey() )
                        && (includeEmpty || property.getValue() != null)) ) {
                  virtual.setProperty( property.getKey(), objectToString( property.getValue() ) );
               }
            }
         }
         tx.success();
         return virtual;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return null;
   }


   static VirtualNode createFullVirtual( final GraphDatabaseService graphDb, final Node node ) {
      return createFullVirtual( graphDb, node, INCLUDE_EMPTY_PROPERTIES );
   }

   static VirtualNode createFullVirtual( final GraphDatabaseService graphDb, final Node node,
                                         final boolean includeEmpty ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final VirtualNode virtual = new VirtualNode( node.hashCode(), graphDb );
         node.getLabels().forEach( virtual::addLabel );
         final Map<String, Object> properties = node.getAllProperties();
         if ( properties.isEmpty() ) {
            virtual.setProperty( NAME_KEY, getNodeName( graphDb, node ) );
         } else {
            for ( Map.Entry<String, Object> property : node.getAllProperties().entrySet() ) {
               if ( property.getKey().equals( NAME_KEY ) || (includeEmpty || property.getValue() != null) ) {
                  virtual.setProperty( property.getKey(), objectToString( property.getValue() ) );
               }
            }
         }
         tx.success();
         return virtual;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return null;
   }

   static VirtualNode addAllRelatedVirtuals( final GraphDatabaseService graphDb,
                                             final VirtualNode parentVirtual,
                                             final Node parentNode,
                                             final RelationshipType... relationTypes ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         for ( RelationshipType relationType : relationTypes ) {
            SearchUtil.getOutRelatedNodes( graphDb, parentNode, relationType ).stream()
                      .map( n -> createFullVirtual( graphDb, n ) )
                      .forEach( v -> parentVirtual.createRelationshipTo( v, relationType ) );
         }
         tx.success();
         return parentVirtual;

      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return parentVirtual;
   }


   static class NodeNameComparator implements Comparator<Node> {
      private final GraphDatabaseService _graphDb;

      public NodeNameComparator( final GraphDatabaseService graphDb ) {
         _graphDb = graphDb;
      }

      public int compare( final Node node1, final Node node2 ) {
         return String.CASE_INSENSITIVE_ORDER.compare(
               getNodeName( _graphDb, node1 ),
               getNodeName( _graphDb, node2 ) );
      }
   }

   // note date / time in format yyyyMMddhhmm
   // viz wants 12/27/2013 01:00 AM AST
   static String unpackDateTime( final String compactDate ) {
      if ( compactDate.length() != 12 ) {
         return "01/01/1999 01:00 AM AST";
      }
      final char[] chars = compactDate.toCharArray();
      final StringBuilder sb = new StringBuilder();
      sb.append( chars[ 4 ] ).append( chars[ 5 ] ).append( '/' )
        .append( chars[ 6 ] ).append( chars[ 7 ] ).append( '/' )
        .append( chars[ 0 ] ).append( chars[ 1 ] ).append( chars[ 2 ] ).append( chars[ 3 ] );
      sb.append( ' ' );
      final int military = Integer.parseInt( compactDate.substring( 8, 10 ) );
      String meridian = "AM";
      String hour = Integer.toString( military );
      if ( military > 12 ) {
         meridian = "PM";
         hour = Integer.toString( military - 12 );
      }
      if ( hour.length() == 1 ) {
         sb.append( '0' );
      }
      sb.append( hour ).append( ':' ).append( chars[ 10 ] ).append( chars[ 11 ] );
      sb.append( ' ' ).append( meridian ).append( " AST" );
      return sb.toString();
   }

}
