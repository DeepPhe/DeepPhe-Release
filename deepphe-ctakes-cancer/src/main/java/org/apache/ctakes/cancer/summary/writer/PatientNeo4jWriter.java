package org.apache.ctakes.cancer.summary.writer;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceUtil;
import org.apache.ctakes.cancer.summary.*;
import org.apache.ctakes.core.cc.AbstractStoreDataWriter;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.healthnlp.deepphe.neo4j.UriConstants;
import org.neo4j.graphdb.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.HAS_STAGE;
import static org.healthnlp.deepphe.neo4j.RelationConstants.HAS_TUMOR_TYPE;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/10/2019
 */
@PipeBitInfo(
      name = "PatientNeo4jWriter",
      description = "Write dPhe patient to neo4j.",
      role = PipeBitInfo.Role.WRITER
)
public class PatientNeo4jWriter extends AbstractStoreDataWriter<PatientCiContainerStore, PatientCiContainer> {

   static private final Logger LOGGER = Logger.getLogger( "PatientNeo4jWriter" );


   static private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddhhmm");

   static private final Map<String, RelationshipType> _relationshipTypes = new HashMap<>();



   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Neo4j Summary Writer ..." );
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );

      final Node thingNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.THING );
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      createAllPatientsNode( graphDb, thingNode );
      createAllDocumentsNode( graphDb, thingNode );
      createUnknownStageNode( graphDb, thingNode );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PatientCiContainerStore getDataStore() {
      return PatientCiContainerStore.getInstance();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void write( final PatientCiContainer patient ) {
      writePatient( patient );
   }

   private void writePatient( final PatientCiContainer patient ) {
      if ( patient == null ) {
         return;
      }
      LOGGER.info( "Writing Patient " + patient.getId() + " to Neo4j ..." );

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();

      // Document Nodes
      final Node allDocumentsNode = Neo4jOntologyConceptUtil.getClassNode( EMR_NOTE_URI );
      final Map<String,Node> documentNodes = createDocumentNodes( graphDb, allDocumentsNode, patient.getNotes() );
      final Map<String,String> docIdToNoteId = createDocIdMap( patient.getNotes() );

      // InstanceNodes, annotation nodes
      final Map<String,Node> instanceNodes = createInstanceNodes( graphDb, documentNodes, docIdToNoteId, patient.getAllInstances() );

      final Node patientNode = createPatientNode( graphDb, documentNodes, instanceNodes, patient );

      final Collection<CancerCiContainer> cancers = patient.getCancers();
      for ( CancerCiContainer cancerContainer : cancers ) {
         final NeoplasmCiContainer cancer = cancerContainer.getCancer();
         // writeCancerNode just gets a ConceptInstance in the container and adds labels and properties.
         final Node cancerNode = createCancerNode( graphDb, patientNode, instanceNodes, cancer );
         final Collection<NeoplasmCiContainer> tumors = cancerContainer.getTumors();
         for ( NeoplasmCiContainer tumor : tumors ) {
            // writeTumorNode just gets or creates ConceptInstance in the container and adds labels and properties.
            createTumorNode( graphDb, cancerNode, instanceNodes, tumor );
         }
      }

   }

   static private Map<String,String> createDocIdMap( final Collection<NoteCiContainer> notes ) {
      final Map<String,String> docIdMap = new HashMap<>( notes.size() );
      for ( NoteCiContainer note : notes ) {
         final String docId = note.getNoteSpecs().getDocumentId();
         docIdMap.put( docId, note.getId() );
      }
      return docIdMap;
   }

   static private Map<String,Node> createDocumentNodes( final GraphDatabaseService graphDb,
                                                        final Node allDocumentsNode,
                                                        final Collection<NoteCiContainer> notes ) {
      final Map<String,Node> docNodes = new HashMap<>( notes.size() );
      try ( Transaction tx = graphDb.beginTx() ) {
         for ( NoteCiContainer note : notes ) {
            final NoteSpecs noteSpecs = note.getNoteSpecs();
            final String noteId = note.getId();
            final Node node = graphDb.createNode( TEXT_DOCUMENT_LABEL );
            node.setProperty( NAME_KEY, noteId );
            setInstanceOf( graphDb, node, allDocumentsNode );

            // Writes note date / time in format yyyyMMddhhmm
            node.setProperty( adjustPropertyName( NOTE_DATE ), noteSpecs.getNoteTime() );
            final String docText = noteSpecs.getDocumentText();
            if ( docText != null ) {
               node.setProperty( adjustPropertyName( NOTE_TEXT ), docText );
            } else {
               LOGGER.error( "No document text for " + noteId );
            }
            node.setProperty( adjustPropertyName( NOTE_NAME ), noteSpecs.getDocumentId() );
            node.setProperty( adjustPropertyName( NOTE_TYPE ), getDocType( noteSpecs.getDocumentType() ) );
            node.setProperty( adjustPropertyName( NOTE_EPISODE ), note.getEpisodeType() );
            docNodes.put( noteId, node );
         }
         tx.success();
         return docNodes;
      }
   }

   static private String getDocType( final String docType ) {
      switch ( docType ) {
         case "RAD":
            return "Radiology Report";
         case "PATH":
            return "Pathology Report";
         case "SP":
            return "Surgical Pathology Report";
         case "DS":
            return "Discharge Summary";
         case "PGN":
            return "Progress Note";
         case "NOTE":
            return "Clinical Note";
         case NoteSpecs.ID_NAME_CLINICAL_NOTE:
            return "Clinical Note";
      }
      return docType.replace( '_', ' ' );
   }


   static private Node createPatientNode( final GraphDatabaseService graphDb,
                                          final Map<String,Node> documentNodes,
                                          final Map<String,Node> instanceNodes,
                                          final PatientCiContainer patient ) {
      final Node allPatientsNode = Neo4jOntologyConceptUtil.getClassNode( PATIENT_URI );
      if ( allPatientsNode == null ) {
         LOGGER.error( "No class for uri " + PATIENT_URI + ".  Cannot create put patient in graph." );
         return null;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node node = graphDb.createNode( PATIENT_LABEL );
         node.setProperty( NAME_KEY, patient.getId() );
         setInstanceOf( graphDb, node, allPatientsNode );
         node.setProperty( adjustPropertyName( PATIENT_NAME ), patient.getPatientName() );
         node.setProperty( adjustPropertyName( PATIENT_GENDER ), patient.getFilledGender() );
         node.setProperty( adjustPropertyName( PATIENT_BIRTH_DATE ), patient.getBirthday() );
         node.setProperty( adjustPropertyName( PATIENT_DEATH_DATE ), patient.getDeathday() );
         node.setProperty( adjustPropertyName( PATIENT_FIRST_ENCOUNTER ), patient.getFirstDateSlashText() );
         node.setProperty( adjustPropertyName( PATIENT_LAST_ENCOUNTER ), patient.getLastDateSlashText() );

         for ( Node noteNode : documentNodes.values() ) {
            createRelation( graphDb, node, noteNode, SUBJECT_HAS_NOTE_RELATION );
         }

         for ( Node instanceNode : instanceNodes.values() ) {
            createRelation( graphDb, node, instanceNode, SUBJECT_HAS_FACT_RELATION );
         }
         tx.success();
         return node;
      }
   }

   static private Node createCancerNode( final GraphDatabaseService graphDb,
                                         final Node patientNode,
                                         final Map<String,Node> instanceNodes,
                                         final NeoplasmCiContainer cancer ) {
      final ConceptInstance cancerInstance = cancer.getConceptInstance();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node cancerNode = instanceNodes.get( cancerInstance.getId() );
         cancerNode.addLabel( CANCER_LABEL );
         // Add container properties
         cancer.getProperties().forEach( (k,v) -> cancerNode.setProperty( adjustPropertyName( k ), v ) );
         final Collection<Node> stages = SearchUtil.getOutRelatedNodes( graphDb, cancerNode, HAS_STAGE );
         if ( stages == null || stages.isEmpty() ) {
            // Viz tool requires a stage for cohort display.  Add an explicit fact for unknown stage.
            final Node stageNode = createStageUnknownNode( graphDb );
            createRelation( graphDb, cancerNode, stageNode, HAS_STAGE );
         }
         createRelation( graphDb, patientNode, cancerNode, SUBJECT_HAS_CANCER_RELATION );
         tx.success();
         return cancerNode;
      }
   }

   static private Node createTumorNode( final GraphDatabaseService graphDb,
                                         final Node cancerNode,
                                         final Map<String,Node> instanceNodes,
                                         final NeoplasmCiContainer tumor ) {
      final ConceptInstance tumorInstance = tumor.getConceptInstance();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node tumorNode = instanceNodes.get( tumorInstance.getId() );
         if ( tumorNode == null ) {
            LOGGER.error( "Null Tumor Node " + tumorInstance.getId() );
            instanceNodes.forEach( (k,v) -> LOGGER.error( k + " , " + v.getId() ) );
            System.exit( -1 );
         }
         tumorNode.addLabel( TUMOR_LABEL );
         // Add container properties
         tumorNode.setProperty( HAS_TUMOR_TYPE, tumor.getType() );
         tumor.getProperties().forEach( (k,v) -> tumorNode.setProperty( adjustPropertyName( k ), v ) );
         createRelation( graphDb, cancerNode, tumorNode, CANCER_HAS_TUMOR_RELATION );
         tx.success();
         return tumorNode;
      }
   }

   static private void setInstanceOf( final GraphDatabaseService graphDb,
                                      final Node instanceNode,
                                      final Node classNode ) {
      createRelation( graphDb, instanceNode, classNode, INSTANCE_OF_RELATION );
   }


   static private void setTextMentionOf( final GraphDatabaseService graphDb,
                                         final Node annotationNode,
                                         final Node classNode ) {
      createRelation( graphDb, annotationNode, classNode, TEXT_MENTION_OF_RELATION );
    }


   static private void createRelation( final GraphDatabaseService graphDb,
                                        final Node node,
                                         final Node relatedNode,
                                         final String relationName ) {
      final RelationshipType relationshipType
            = _relationshipTypes.computeIfAbsent( relationName, RelationshipType::withName );
      createRelation( graphDb, node, relatedNode, relationshipType );
   }

   static private void createRelation( final GraphDatabaseService graphDb,
                                       final Node node,
                                       final Node relatedNode,
                                       final RelationshipType relationshipType ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         for ( Relationship existing : node.getRelationships( relationshipType, Direction.OUTGOING ) ) {
            if ( existing.getOtherNode( node ).equals( relatedNode ) ) {
               // Relation already exists
               tx.success();
               return;
            }
         }
         node.createRelationshipTo( relatedNode, relationshipType );
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
   }


   static private Node createAllDocumentsNode( final GraphDatabaseService graphDb, final Node thingNode ) {
      if ( thingNode == null ) {
         LOGGER.error( "No Thing node!  Cannot create put " + EMR_NOTE_URI + " in graph." );
         return null;
      }
      final Node extantNode = Neo4jOntologyConceptUtil.getClassNode( EMR_NOTE_URI );
      if ( extantNode != null ) {
         return extantNode;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node allDocumentsNode = graphDb.createNode( CLASS_LABEL );
         allDocumentsNode.setProperty( NAME_KEY, EMR_NOTE_URI );
         allDocumentsNode.createRelationshipTo( thingNode, IS_A_RELATION );
         tx.success();
         return allDocumentsNode;
      }
   }

   static private Node createAllCancersNode( final GraphDatabaseService graphDb, final Node thingNode ) {
      if ( thingNode == null ) {
         LOGGER.error( "No Thing node!  Cannot create put " + CANCER_SUMMARY_URI + " in graph." );
         return null;
      }
      final Node extantNode = Neo4jOntologyConceptUtil.getClassNode( CANCER_SUMMARY_URI );
      if ( extantNode != null ) {
         return extantNode;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node allCancers = graphDb.createNode( CLASS_LABEL );
         allCancers.setProperty( NAME_KEY, CANCER_SUMMARY_URI );
         allCancers.createRelationshipTo( thingNode, IS_A_RELATION );
         tx.success();
         return allCancers;
      }
   }

   static private Node createAllTumorsNode( final GraphDatabaseService graphDb, final Node thingNode ) {
      if ( thingNode == null ) {
         LOGGER.error( "No Thing node!  Cannot create put " + CANCER_SUMMARY_URI + " in graph." );
         return null;
      }
      final Node extantNode = Neo4jOntologyConceptUtil.getClassNode( TUMOR_SUMMARY_URI );
      if ( extantNode != null ) {
         return extantNode;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node allTumors = graphDb.createNode( CLASS_LABEL );
         allTumors.setProperty( NAME_KEY, TUMOR_SUMMARY_URI );
         allTumors.createRelationshipTo( thingNode, IS_A_RELATION );
         tx.success();
         return allTumors;
      }
   }

   static private Node createAllPatientsNode( final GraphDatabaseService graphDb, final Node thingNode ) {
      if ( thingNode == null ) {
         LOGGER.error( "No " + UriConstants.THING + " node!  Cannot create put " + SUBJECT_URI + " in graph." );
         return null;
      }
      final Node extantNode = Neo4jOntologyConceptUtil.getClassNode( SUBJECT_URI );
      if ( extantNode != null ) {
         return extantNode;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node subjectNode = graphDb.createNode( CLASS_LABEL );
         subjectNode.setProperty( NAME_KEY, SUBJECT_URI );
         subjectNode.createRelationshipTo( thingNode, IS_A_RELATION );
         final Node allPatientsNode = graphDb.createNode( CLASS_LABEL );
         allPatientsNode.setProperty( NAME_KEY, PATIENT_URI );
         allPatientsNode.createRelationshipTo( subjectNode, IS_A_RELATION );
         tx.success();
         return allPatientsNode;
      }
   }

   static private Node createUnknownStageNode( final GraphDatabaseService graphDb, final Node thingNode ) {
      if ( thingNode == null ) {
         LOGGER.error( "No " + UriConstants.THING + " node!  Cannot create " + UriConstants.STAGE_UNKNOWN + " in graph." );
         return null;
      }
      final Node extantNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.STAGE_UNKNOWN );
      if ( extantNode != null ) {
         return extantNode;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node unknownStageNode = graphDb.createNode( CLASS_LABEL );
         unknownStageNode.setProperty( NAME_KEY, UriConstants.STAGE_UNKNOWN );
         unknownStageNode.createRelationshipTo( thingNode, IS_A_RELATION );
         tx.success();
         return unknownStageNode;
      }
   }



   static private Map<String,Node> createInstanceNodes( final GraphDatabaseService graphDb,
                                                        final Map<String,Node> documentNodes,
                                                        final Map<String,String> docIdToNoteId,
                                                        final Collection<ConceptInstance> instances ) {
      final Map<String,Node> instanceNodes = new HashMap<>( instances.size() );
      final Map<IdentifiedAnnotation,Node> annotationNodes = new HashMap<>();
      for ( ConceptInstance instance : instances ) {
         getOrCreateInstanceNode( graphDb, documentNodes, docIdToNoteId, instanceNodes, annotationNodes, instance );
      }
      return instanceNodes;
   }


   /**
    * Should create a node for all concept instances, regardless of assertion
    * @param graphDb -
    * @param documentNodes -
    * @param instanceNodes -
    * @param annotationNodes -
    * @param instance -
    * @return -
    */
   static private Node getOrCreateInstanceNode( final GraphDatabaseService graphDb,
                                                final Map<String,Node> documentNodes,
                                                final Map<String,String> docIdToNoteId,
                                                final Map<String,Node> instanceNodes,
                                 final Map<IdentifiedAnnotation,Node> annotationNodes,
                                 final ConceptInstance instance ) {
      final String instanceId = instance.getId();
      final Node extantNode = instanceNodes.get( instanceId );
      if ( extantNode != null ) {
         return extantNode;
      }

      try ( Transaction tx = graphDb.beginTx() ) {
         // Create node, instance_of
         final Node node = graphDb.createNode( OBJECT_LABEL );
         node.setProperty( NAME_KEY, instanceId );
         // Stop recursion with cyclic graph
         instanceNodes.put( instanceId, node );
         final Node classNode = Neo4jOntologyConceptUtil.getClassNode( instance.getUri() );
         if ( classNode != null ) {
            setInstanceOf( graphDb, node, classNode );
         } else {
            LOGGER.error( "No Class Node for " + instance.getUri() );
         }
         // Add instance properties
         ConceptInstanceUtil.getProperties( instance )
                            .forEach( (k,v) -> node.setProperty( adjustPropertyName( k ), v ) );
         // Add instance relations
         final Collection<String> writtenIds = new ArrayList<>();
         for ( Map.Entry<String, Collection<ConceptInstance>> related : instance.getRelated().entrySet() ) {
            final String relationName = related.getKey();
            for ( ConceptInstance relatedInstance : related.getValue() ) {
               // Do some instances have the same relation twice?
               if ( !writtenIds.contains( relatedInstance.getId() ) ) {
                  final Node relatedNode
                        = getOrCreateInstanceNode( graphDb, documentNodes, docIdToNoteId, instanceNodes, annotationNodes, relatedInstance );
                  createRelation( graphDb, node, relatedNode, relationName );
                  writtenIds.add( relatedInstance.getId() );
               }
            }
            writtenIds.clear();
         }

         final String patientId = instance.getPatientId();
         final String uri = instance.getUri();
         final Collection<IdentifiedAnnotation> annotations = instance.getAnnotations();
//         LOGGER.info( "getOrCreateInstanceNode setting annotations for " + instanceId + " " + annotations.size() );
         for ( IdentifiedAnnotation annotation : annotations ) {
//            LOGGER.info( "getOrCreateInstanceNode setting annotations for " + instanceId + " with " +
//                         annotation.getCoveredText() );
            final String docId = instance.getDocumentId( annotation );
            final String noteId = docIdToNoteId.get( docId );
//            LOGGER.info(
//                  "getOrCreateInstanceNode setting annotations for " + instanceId + " doc " + docId + " " + noteId );
            final Node annotationNode
                  = getOrCreateAnnotationNode( graphDb, classNode, patientId, uri, documentNodes, noteId, annotationNodes, annotation );
            if ( annotationNode != null ) {
//               LOGGER.info( "getOrCreateInstanceNode creating annotation relation " + instanceId );
               createRelation( graphDb, node, annotationNode, FACT_HAS_TEXT_MENTION_RELATION );
//               LOGGER.info( "getOrCreateInstanceNode created annotation relation " + instanceId );
            }
         }
//         LOGGER.info(
//               "getOrCreateInstanceNode finished setting annotations for " + instanceId + " " + annotations.size() );

         tx.success();
//         LOGGER.info( "getOrCreateInstanceNode success" );
         return node;
      } catch ( TransactionFailureException tfE ) {
         // haven't a clue what to do here.
         LOGGER.error( tfE.getMessage() );
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return null;
   }


   /**
    * Create a node for all annotations tied to a concept instance.
    * @param graphDb -
    * @param uri -
    * @param documentNodes -
    * @param noteId -
    * @param annotationNodes -
    * @param annotation -
    * @return -
    */
   static private Node getOrCreateAnnotationNode( final GraphDatabaseService graphDb,
                                                  final Node classNode,
                                                  final String patientId,
                                                  final String uri,
                                                final Map<String,Node> documentNodes,
                                                  final String noteId,
                                                  final Map<IdentifiedAnnotation,Node> annotationNodes,
                                                final IdentifiedAnnotation annotation ) {
      final Node extantNode = annotationNodes.get( annotation );
      if ( extantNode != null ) {
         return extantNode;
      }
      final Node docNode = documentNodes.get( noteId );
      if ( docNode == null ) {
         LOGGER.error( "No Document Node for " + annotation.getCoveredText() );
      }
      final int spanBegin = annotation.getBegin();
      final int spanEnd = annotation.getEnd();
      final String annotationId = patientId + '_' + noteId + '_' + uri + '_' + spanBegin + '_' + spanEnd;
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node sameNode = graphDb.findNode( TEXT_MENTION_LABEL, NAME_KEY, annotationId );
         if ( sameNode != null ) {
            annotationNodes.put( annotation, sameNode );
            tx.success();
            return sameNode;
         }
         final Node node = graphDb.createNode( TEXT_MENTION_LABEL );
         node.setProperty( NAME_KEY, annotationId );
         annotationNodes.put( annotation, node );

//         if ( !uri.isEmpty() ) {
//            if ( classNode != null ) {
//               setTextMentionOf( graphDb, node, classNode );
//            }
//         }
         node.setProperty( adjustPropertyName( TEXT_SPAN_BEGIN ), spanBegin );
         node.setProperty( adjustPropertyName( TEXT_SPAN_END ), spanEnd );

         if ( docNode != null ) {
            createRelation( graphDb, docNode, node, NOTE_HAS_TEXT_MENTION_RELATION );
         }

         tx.success();
//         LOGGER.info( "getOrCreateInstanceNode created annotation " + annotationId );

         return node;
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return null;
   }

   static private Node createStageUnknownNode(  final GraphDatabaseService graphDb ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node node = graphDb.createNode( OBJECT_LABEL );
         node.setProperty( NAME_KEY, UriConstants.STAGE_UNKNOWN );
         final Node classNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.STAGE_UNKNOWN );
         if ( classNode != null ) {
            setInstanceOf( graphDb, node, classNode );
         }
         node.setProperty( adjustPropertyName( PREF_TEXT_KEY ), "Stage Unknown" );
         node.setProperty( adjustPropertyName( VALUE_TEXT ), "Unknown" );
         tx.success();
         return node;
      } catch ( MultipleFoundException mfE ) {
         //         throw new RuntimeException( mfE );
      }
      return null;
   }

   static private final String adjustPropertyName( final String propertyName ) {
      final char[] original = propertyName.toCharArray();
      final char[] adjusted = new char[ propertyName.length() ];
      adjusted[0] = Character.toLowerCase( original[0] );
      boolean wasScore = false;
      int adjustedLength = 1;
      for ( int i=1; i<original.length; i++ ) {
         if ( wasScore ) {
            adjusted[ adjustedLength ] = Character.toUpperCase( original[ i ] );
            wasScore = false;
         } else if ( original[ i ] == '_' || original[ i ] == ' ' ) {
            wasScore = true;
            adjustedLength--;
         } else {
            adjusted[ adjustedLength ] = original[ i ];
         }
         adjustedLength++;
      }
      return new String( Arrays.copyOf( adjusted, adjustedLength ) );
   }



}
