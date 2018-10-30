package org.healthnlp.deepphe.viz.neo4j;

import apoc.convert.Json;
import apoc.result.MapResult;
import apoc.result.NodeResult;
import apoc.result.StringResult;
import apoc.result.VirtualNode;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;
import static org.healthnlp.deepphe.viz.neo4j.DataUtil.*;



/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/22/2018
 */
public class PatientProcedures {

   // This field declares that we need a GraphDatabaseService
   // as context when any procedure in this class is invoked
   @Context
   public GraphDatabaseService graphDb;

   // This gives us a log instance that outputs messages to the
   // standard log, normally found under `data/log/console.log`
   @Context
   public Log log;


   static private final boolean INCLUDE_EMPTY_PROPERTIES = false;
   static private final boolean LOWER_CASE_RELATIONS = false;


   @Procedure( name = "deepphe.getAllPatientNames" )
   @Description( "return all non-null patient names as a string" )
   public Stream<StringResult> getAllPatientNames() {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Stream<StringResult> patients = DataUtil.getAllPatientNodes( graphDb ).stream()
                                                       .map( n -> n.getProperty( PATIENT_NAME ).toString() )
                                                       .sorted()
                                                       .map( StringResult::new );
         tx.success();
         return patients;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Stream.empty();
   }



   @Procedure( name = "deepphe.getAllPatientNodes" )
   @Description( "return all non-null patient information as node objects.  Similar to neo4jCypherQueries::getPatientInfo(patientName)" )
   public Stream<NodeResult> getAllPatientNodes() {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Stream<NodeResult> patients = DataUtil.getAllPatientNodes( graphDb )
                                                     .stream()
                                                     .sorted( new DataUtil.NodeNameComparator( graphDb ) )
                                                     .map( p -> createFullVirtual( graphDb, p ) )
                                                     .map( NodeResult::new );
         tx.success();
         return patients;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Stream.empty();
   }


   @Procedure( name = "deepphe.getPatientNode" )
   @Description( "return all non-null patient information as node objects.  Similar to neo4jCypherQueries::getPatientInfo(patientName)" )
   public Stream<NodeResult> getPatientNode( @Name( "patientId" ) String patientId ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientNode = SearchUtil.getObjectNode( graphDb, patientId );
         if ( patientNode == null ) {
            tx.success();
            return Stream.empty();
         }
         final VirtualNode virtual = createFullVirtual( graphDb, patientNode );
         tx.success();
         return Stream.of( new NodeResult( virtual ) );
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Stream.empty();
   }


   @Procedure( name = "deepphe.getAllCancerPatientNodes" )
   @Description( "return all non-null patient ids and the full fact information for cancers that they have.  Similar to neo4jCypherQueries::getCancerSummary" )
   public Stream<NodeResult> getAllCancerPatientNodes() {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Collection<NodeResult> cancerPatientVirtuals = new ArrayList<>();
         final List<Node> cancerPatients = DataUtil.getAllPatientNodes( graphDb ).stream()
                                                   .filter( n -> SearchUtil.hasOutRelation( graphDb, n, SUBJECT_HAS_CANCER_RELATION ) )
                                                   .sorted( new DataUtil.NodeNameComparator( graphDb ) )
                                                   .collect( Collectors.toList() );
         for ( Node patient : cancerPatients ) {
            final VirtualNode patientVirtual = createFullVirtual( graphDb, patient );
            SearchUtil.getOutRelatedNodes( graphDb, patient, SUBJECT_HAS_CANCER_RELATION )
                      .forEach( n -> patientVirtual.createRelationshipTo( n, SUBJECT_HAS_CANCER_RELATION ) );
            cancerPatientVirtuals.add( new NodeResult( patientVirtual ) );
         }
         tx.success();
         return cancerPatientVirtuals.stream();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Stream.empty();
   }


   @Procedure( name = "deepphe.getAllTumorPatientNodes" )
   @Description( "return all non-null patient ids, cancer ids and the full fact information for tumors that they have.  Similar to neo4jCypherQueries::getTumorSummary" )
   public Stream<NodeResult> getAllTumorPatientNodes() {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Collection<Node> tumorPatientVirtuals = new ArrayList<>();
         final Collection<Node> cancerPatients = DataUtil.getAllPatientNodes( graphDb );
         for ( Node patient : cancerPatients ) {
            VirtualNode patientVirtual = null;
            final Collection<Node> cancers
                  = SearchUtil.getOutRelatedNodes( graphDb, patient, SUBJECT_HAS_CANCER_RELATION );
            for ( Node cancer : cancers ) {
               VirtualNode cancerVirtual = null;
               final Collection<Node> tumors
                     = SearchUtil.getOutRelatedNodes( graphDb, cancer, CANCER_HAS_TUMOR_RELATION );
               if ( !tumors.isEmpty() ) {
                  cancerVirtual = createSimpleVirtual( graphDb, cancer );
                  if ( patientVirtual == null ) {
                     patientVirtual = createSimpleVirtual( graphDb, patient );
                     patientVirtual.setProperty( PATIENT_NAME, objectToString( patient.getProperty( PATIENT_NAME ) ) );
                     tumorPatientVirtuals.add( patientVirtual );
                  }
                  patientVirtual.createRelationshipTo( cancerVirtual, SUBJECT_HAS_CANCER_RELATION );
                  for ( Node tumor : tumors ) {
                     addAllRelatedVirtuals( graphDb, cancerVirtual, tumor, CANCER_HAS_TUMOR_RELATION );
                  }
               }
            }
         }
         tx.success();
         return tumorPatientVirtuals.stream().map( NodeResult::new );
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Stream.empty();
   }





   // Returns what looks like a correct tree.  9-5-11:am
   @Procedure( name = "deepphe.getAllPatientTree" )
   @Description( "Creates a stream of nested nodes representing all patients and their information" )
   public Stream<MapResult> getAllPatientTree() {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientRoot = getPatientRoot( graphDb );
         if ( patientRoot == null ) {
            tx.success();
            return Stream.empty();
         }
         final TraversalDescription traverser = graphDb.traversalDescription()
                                                       .depthFirst()
                                                       .relationships( INSTANCE_OF_RELATION, Direction.INCOMING )
                                                       .relationships( SUBJECT_HAS_NOTE_RELATION, Direction.OUTGOING )
                                                       .relationships( SUBJECT_HAS_CANCER_RELATION, Direction.OUTGOING )
                                                       .relationships( CANCER_HAS_TUMOR_RELATION, Direction.OUTGOING )
                                                       .relationships( CANCER_HAS_FACT_RELATION, Direction.OUTGOING )
                                                       .relationships( TUMOR_HAS_FACT_RELATION, Direction.OUTGOING )
                                                       .relationships( FACT_HAS_RELATED_FACT_RELATION, Direction.OUTGOING )
                                                       .relationships( NOTE_HAS_TEXT_MENTION_RELATION, Direction.OUTGOING )
                                                       .relationships( FACT_HAS_TEXT_MENTION_RELATION, Direction.OUTGOING )
                                                       .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL )
                                                       .evaluator( Evaluators.excludeStartPosition() );
         final List<Path> branch = traverser.traverse( patientRoot ).stream().collect( Collectors.toList() );
         tx.success();
         return new Json().toTree( branch, LOWER_CASE_RELATIONS );
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Stream.empty();
   }

   @Procedure( name = "deepphe.getPatientTree" )
   @Description( "creates a stream of nested documents representing one patient and their information" )
   public Stream<MapResult> getPatientTree( @Name( "patientId" ) String patientId ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patient = SearchUtil.getObjectNode( graphDb, patientId );
         if ( patient == null ) {
            tx.success();
            return Stream.empty();
         }
         final TraversalDescription traverser = graphDb.traversalDescription()
                                                       .depthFirst()
                                                       .relationships( INSTANCE_OF_RELATION, Direction.INCOMING )
                                                       .relationships( SUBJECT_HAS_NOTE_RELATION, Direction.OUTGOING )
                                                       .relationships( SUBJECT_HAS_CANCER_RELATION, Direction.OUTGOING )
                                                       .relationships( CANCER_HAS_TUMOR_RELATION, Direction.OUTGOING )
                                                       .relationships( CANCER_HAS_FACT_RELATION, Direction.OUTGOING )
                                                       .relationships( TUMOR_HAS_FACT_RELATION, Direction.OUTGOING )
                                                       .relationships( FACT_HAS_RELATED_FACT_RELATION, Direction.OUTGOING )
                                                       .relationships( NOTE_HAS_TEXT_MENTION_RELATION, Direction.OUTGOING )
                                                       .relationships( FACT_HAS_TEXT_MENTION_RELATION, Direction.OUTGOING )
                                                       .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
         final List<Path> branch = traverser.traverse( patient ).stream().collect( Collectors.toList() );
         tx.success();
         return new Json().toTree( branch, LOWER_CASE_RELATIONS );
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return Stream.empty();
   }


//////////////////////////////////////////////////////////////////////////////////////////
//
//                                     PRIVATE
//
//////////////////////////////////////////////////////////////////////////////////////////






}
