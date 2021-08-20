package org.healthnlp.deepphe.neo4j.writer;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/4/2020
 */
public enum NodeWriter {
    INSTANCE;

    static public NodeWriter getInstance() {
        return INSTANCE;
    }

    private final Map<String, RelationshipType> _relationshipTypes = new HashMap<>();

    private final ClearableNote _clearableNote = new ClearableNote();

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            COHORT INITIALIZATION
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public void initializeDphe( final GraphDatabaseService graphDb,
                                final Log log ) {
        final Node thingNode = SearchUtil.getClassNode( graphDb, UriConstants.THING );
        createAllPatientsNode( graphDb, log, thingNode );
        createAllDocumentsNode( graphDb, log, thingNode );
        createUnknownStageNode( graphDb, log, thingNode );
        // Writing is only done on the neo4j server.  Because the server generates its own graphDb,
        // we can't explicitly add a specific shutdown hook.  Try to do it here.
        // --> This didn't work, so don't bother.
//      ShutdownHook.registerShutdownHook( graphDb );
    }

    static private void createAllPatientsNode( final GraphDatabaseService graphDb,
                                               final Log log,
                                               final Node thingNode ) {
        if ( thingNode == null ) {
            log.error( "No " + UriConstants.THING + " node!  Cannot create put " + SUBJECT_URI + " in graph." );
            return;
        }
        final Node extantNode = SearchUtil.getClassNode( graphDb, SUBJECT_URI );
        if ( extantNode != null ) {
            return;
        }
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node subjectNode = graphDb.createNode( CLASS_LABEL );
            subjectNode.setProperty( NAME_KEY, SUBJECT_URI );
            subjectNode.createRelationshipTo( thingNode, IS_A_RELATION );
            final Node allPatientsNode = graphDb.createNode( CLASS_LABEL );
            allPatientsNode.setProperty( NAME_KEY, PATIENT_URI );
            allPatientsNode.createRelationshipTo( subjectNode, IS_A_RELATION );
            tx.success();
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while creating all Patients node\n"
                    + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }

    static private void createAllDocumentsNode( final GraphDatabaseService graphDb,
                                                final Log log,
                                                final Node thingNode ) {
        if ( thingNode == null ) {
            log.error( "No Thing node!  Cannot create put " + EMR_NOTE_URI + " in graph." );
            return;
        }
        final Node extantNode = SearchUtil.getClassNode( graphDb, EMR_NOTE_URI );
        if ( extantNode != null ) {
            return;
        }
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node allDocumentsNode = graphDb.createNode( CLASS_LABEL );
            allDocumentsNode.setProperty( NAME_KEY, EMR_NOTE_URI );
            allDocumentsNode.createRelationshipTo( thingNode, IS_A_RELATION );
            tx.success();
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while creating all Documents node\n"
                    + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }

    static private void createUnknownStageNode( final GraphDatabaseService graphDb,
                                                final Log log,
                                                final Node thingNode ) {
        if ( thingNode == null ) {
            log.error( "No " + UriConstants.THING + " node!  Cannot create " + UriConstants.STAGE_UNKNOWN + " in graph." );
            return;
        }
        final Node extantNode = SearchUtil.getClassNode( graphDb, UriConstants.STAGE_UNKNOWN );
        if ( extantNode != null ) {
            return;
        }
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node unknownStageNode = graphDb.createNode( CLASS_LABEL );
            unknownStageNode.setProperty( NAME_KEY, UriConstants.STAGE_UNKNOWN );
            unknownStageNode.createRelationshipTo( thingNode, IS_A_RELATION );
            tx.success();
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while creating unknown Stage node\n"
                    + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }


    // DATES AS yyyy/MM/dd
    // {
    //    {
    //       "id" : "patient01",
    //       "name" : "John Smith",
    //       "gender" : "Male",
    //       "birth" : "2000/01/01",
    //       "death" : "",
    //       "notes" : [
    //          {
    //             "id" : "note_A",
    //             "type" : "RAD",
    //             "date" : "2020/01/01",
    //             "episode" : "DIAGNOSIS",
    //             "text" : "Lots of text",
    //             "mentions" : [
    //                {
    //                   "id" : "note_A_01",
    //                   "class" : "breast_cancer",
    //                   "begin" : 10,
    //                   "end" : 20
    //                }
    //             ],
    //          }
    //       ],
    //    }
    // }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            PATIENT DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public void addPatientInfo( final GraphDatabaseService graphDb,
                                final Log log,
                                final Patient patient ) {
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node patientNode = createPatientNode( graphDb, log, patient.getId() );
            if ( patientNode == null ) {
                return;
            }
//         node.setProperty( adjustPropertyName( PATIENT_NAME ), patient.getName() );
//         node.setProperty( adjustPropertyName( PATIENT_GENDER ), patient.getGender() );
//         node.setProperty( adjustPropertyName( PATIENT_BIRTH_DATE ), patient.getBirth() );
//         node.setProperty( adjustPropertyName( PATIENT_DEATH_DATE ), patient.getDeath() );
            // TODO : Dynamic
//         node.setProperty( adjustPropertyName( PATIENT_FIRST_ENCOUNTER ), patient.getFirstDateSlashText() );
//         node.setProperty( adjustPropertyName( PATIENT_LAST_ENCOUNTER ), patient.getLastDateSlashText() );
            patient.getNotes().forEach( n -> addNoteInfo( graphDb, log, patientNode, n ) );
            tx.success();
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Patient information for "
                    + patient.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }


    private Node createPatientNode(  final GraphDatabaseService graphDb,
                                     final Log log,
                                     final String patientId ) {
        try ( Transaction tx = graphDb.beginTx() ) {
            Node allPatientsNode = SearchUtil.getClassNode( graphDb, PATIENT_URI );
            if ( allPatientsNode == null ) {
                initializeDphe( graphDb, log );
                allPatientsNode = SearchUtil.getClassNode( graphDb, PATIENT_URI );
            }
            if ( allPatientsNode == null ) {
                log.error(
                        "No class node for uri " + PATIENT_URI
                                + ".  Cannot create put patient " + patientId + " in graph." );
                tx.success();
                return null;
            }
            Node patientNode = SearchUtil.getLabeledNode( graphDb, PATIENT_LABEL, patientId );
            if ( patientNode == null ) {
                patientNode = graphDb.createNode( PATIENT_LABEL );
                patientNode.setProperty( NAME_KEY, patientId );
            }
            setInstanceOf( graphDb, log, patientNode, allPatientsNode );
            tx.success();
            return patientNode;
        } catch ( TransactionFailureException txE ) {
            log.error( "Cannot create put patient " + patientId + " in graph." );
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while creating Patient "
                    + patientId + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
        return null;
    }


    private Node getOrCreatePatientNode(  final GraphDatabaseService graphDb,
                                          final Log log,
                                          final String patientId ) {
        final Node patientNode = SearchUtil.getObjectNode( graphDb, patientId );
        if ( patientNode != null ) {
            return patientNode;
        }
        return createPatientNode( graphDb, log, patientId );
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            CANCER DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public void addCancerInfo( final GraphDatabaseService graphDb,
                               final Log log,
                               final String patientId,
                               final NeoplasmSummary cancer ) {
        final Node patientNode = getOrCreatePatientNode( graphDb, log, patientId );
        if ( patientNode == null ) {
            log.error( "No Patient Node for " + patientId );
            return;
        }
        addCancerInfo( graphDb, log, patientNode, cancer );
    }

    private void addCancerInfo(  final GraphDatabaseService graphDb,
                                 final Log log,
                                 final Node patientNode,
                                 final NeoplasmSummary cancer ) {
//      log.info( "Creating Cancer node " + cancer.getId() );
        final Node cancerNode = createNeoplasmNode( graphDb, log, CANCER_LABEL, cancer );
        if ( cancerNode == null ) {
            return;
        }
//      log.info( "Created Cancer node " + cancer.getId() );
        try ( Transaction tx = graphDb.beginTx() ) {
//         log.info( "Creating Subject Has Cancer for " + cancer.getId() );
            createRelation( graphDb, log, patientNode, cancerNode, SUBJECT_HAS_CANCER_RELATION );
//         log.info( "created" );
            final Collection<NeoplasmSummary> tumors = cancer.getSubSummaries();
            if ( tumors != null && !tumors.isEmpty() ) {
                for ( NeoplasmSummary tumor : tumors ) {
//               log.info( "Creating Tumor node for " + tumor.getId() );
                    final Node tumorNode = createNeoplasmNode( graphDb, log, TUMOR_LABEL, tumor );
//               log.info( "created" );
                    if ( tumorNode != null ) {
//                  log.info( "Creating Cancer has Tumor for " + tumor.getId() );
                        createRelation( graphDb, log, cancerNode, tumorNode, CANCER_HAS_TUMOR_RELATION );
//                  log.info( "created" );
                    }
                }
            }
            tx.success();
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Cancer information "
                    + cancer.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }

    public void addNeoplasmInfo( final GraphDatabaseService graphDb,
                                 final Log log,
                                 final String patientId,
                                 final NeoplasmSummary neoplasm ) {
        final Node patientNode = getOrCreatePatientNode( graphDb, log, patientId );
        if ( patientNode == null ) {
            log.error( "No Patient Node for " + patientId );
            return;
        }
        addNeoplasmInfo( graphDb, log, patientNode, neoplasm );
    }

    private void addNeoplasmInfo(  final GraphDatabaseService graphDb,
                                   final Log log,
                                   final Node patientNode,
                                   final NeoplasmSummary neoplasm ) {
        final Node neoplasmNode = createNeoplasmNode( graphDb, log, CANCER_LABEL, neoplasm );
        if ( neoplasmNode == null ) {
            return;
        }
        try ( Transaction tx = graphDb.beginTx() ) {
            createRelation( graphDb, log, patientNode, neoplasmNode, SUBJECT_HAS_CANCER_RELATION );
//         final Collection<NeoplasmSummary> tumors = neoplasm.getTumors();
//         for ( NeoplasmSummary tumor : tumors ) {
//            final Node tumorNode = createNeoplasmNode( graphDb, log, TUMOR_LABEL, tumor );
//            if ( tumorNode != null ) {
//               createRelation( graphDb, log, cancerNode, tumorNode, CANCER_HAS_TUMOR_RELATION );
//            }
//         }
            tx.success();
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Neoplasm information "
                    + neoplasm.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }

    private Node createNeoplasmNode(  final GraphDatabaseService graphDb,
                                      final Log log,
                                      final Label neoplasmLabel,
                                      final NeoplasmSummary neoplasm ) {
        final String id = neoplasm.getId();
        if ( id == null || id.isEmpty() ) {
            log.error( "No ID for Neoplasm" );
            return null;
        }
        final String uri = neoplasm.getClassUri();
        if ( uri == null || uri.isEmpty() ) {
            log.error( "No Class URI for Neoplasm " + id );
            return null;
        }
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node classNode = SearchUtil.getClassNode( graphDb, uri );
            if ( classNode == null ) {
                log.error( "Illegal Class URI for Neoplasm " + id + " " + uri );
                tx.success();
                return null;
            }
            final Node neoplasmNode = graphDb.createNode( neoplasmLabel );
            neoplasmNode.setProperty( NAME_KEY, id );
            setInstanceOf( graphDb, log, neoplasmNode, classNode );
            final Collection<NeoplasmAttribute> attributes = neoplasm.getAttributes();
            for ( NeoplasmAttribute attribute : attributes ) {
                addAttributeNode( graphDb, log, id, neoplasmNode, attribute );
            }
            tx.success();
            return neoplasmNode;
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while creating Neoplasm "
                    + neoplasm.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
        return null;
    }

    private void addAttributeNode( final GraphDatabaseService graphDb,
                                   final Log log,
                                   final String neoplasmId,
                                   final Node neoplasmNode,
                                   final NeoplasmAttribute attribute ) {
        final String id = attribute.getId();
        if ( id == null || id.isEmpty() ) {
            log.error( "No ID for Attribute " + attribute.getName() + " " + neoplasmId );
            return;
        }
        final Node attributeNode = graphDb.createNode( ATTRIBUTE_LABEL );
        attributeNode.setProperty( NAME_KEY, id );
        createRelation( graphDb, log, neoplasmNode, attributeNode, NEOPLASM_HAS_ATTRIBUTE_RELATION );
        final String uri = attribute.getClassUri();
        if ( uri != null && !uri.isEmpty() ) {
            // Setting the uri as a property vs. using an instance_of relation may speed up neo4j 'read' processes
            attributeNode.setProperty( ATTRIBUTE_URI, uri );
        }
        final String name = attribute.getName();
        if ( name != null && !name.isEmpty() ) {
            attributeNode.setProperty( ATTRIBUTE_NAME, name );
        }
        final String value = attribute.getValue();
        if ( value != null && !value.isEmpty() ) {
            attributeNode.setProperty( ATTRIBUTE_VALUE, value );
        }
        addAttributeMentions( graphDb, log, attributeNode,
                ATTRIBUTE_DIRECT_MENTION_RELATION,
                attribute.getDirectEvidence() );
        addAttributeMentions( graphDb, log, attributeNode,
                ATTRIBUTE_INDIRECT_MENTION_RELATION,
                attribute.getIndirectEvidence() );
        addAttributeMentions( graphDb, log, attributeNode,
                ATTRIBUTE_NOT_MENTION_RELATION,
                attribute.getNotEvidence() );
    }

    private void addAttributeMentions( final GraphDatabaseService graphDb,
                                       final Log log,
                                       final Node attributeNode,
                                       final RelationshipType relation,
                                       final Collection<Mention> mentions ) {
        for ( Mention mention : mentions ) {
            final String mentionId = mention.getId();
            if ( mentionId == null || mentionId.isEmpty() ) {
                continue;
            }
            final Node mentionNode = SearchUtil.getLabeledNode( graphDb, TEXT_MENTION_LABEL, mentionId );
            if ( mentionNode == null ) {
                continue;
            }
            createRelation( graphDb, log, attributeNode, mentionNode, relation );
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            NOTE DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public void addNoteInfo(  final GraphDatabaseService graphDb,
                              final Log log,
                              final String patientId, final Note note ) {
        final Node patientNode = getOrCreatePatientNode( graphDb, log, patientId );
        if ( patientNode == null ) {
            log.error( "No Patient Node for " + patientId );
            return;
        }
        addNoteInfo( graphDb, log, patientNode, note );
    }


//    private void addNoteInfo(  final GraphDatabaseService graphDb,
//                               final Log log,
//                               final Node patientNode, final Note note ) {
//        Node node = null;
//        try ( Transaction tx = graphDb.beginTx() ) {
//            final Node allDocumentsNode = SearchUtil.getClassNode( graphDb, EMR_NOTE_URI );
//            if ( allDocumentsNode == null ) {
//                log.error( "No class for uri " + EMR_NOTE_URI + ".  Cannot create put note in graph." );
//                tx.success();
//                return;
//            }
////         log.info( "Checking note node " + note.getId() );
//            node = SearchUtil.getLabeledNode( graphDb, TEXT_DOCUMENT_LABEL, note.getId() );
//            if ( node != null ) {
//                // clear everything below the note so that we can rewrite it.
////            log.info( "Have some not-null Note node:\n"
////                       + node.getAllProperties().entrySet().stream()
////                             .map( e -> e.getKey() + " = " + e.getValue() )
////                             .collect( Collectors.joining("\n") ) );
//                clearNode( graphDb, log, node );
////            log.info( "Done clearing note.  Removing Properties." );
//                node.removeProperty( NOTE_TYPE );
////         log.info( "Setting note date " + note.getDate() );
//                node.removeProperty( NOTE_DATE );
////         log.info( "Setting note episode " + note.getEpisode() );
//                node.removeProperty( NOTE_EPISODE );
////         log.info( "Setting note text" );
//                node.removeProperty( NOTE_TEXT );
////            log.info( "Done removing properties." );
//            } else {
////            log.info( "Creating note node " + note.getId() );
//                node = graphDb.createNode( TEXT_DOCUMENT_LABEL );
////            log.info( "Setting note name " + note.getId() );
//                node.setProperty( NAME_KEY, note.getId() );
////            log.info( "Setting instance of " + note.getId() );
//                setInstanceOf( graphDb, log, node, allDocumentsNode );
////            log.info( "Creating relation subject has note" );
//                createRelation( graphDb, log, patientNode, node, SUBJECT_HAS_NOTE_RELATION );
//            }
//            // Writes note date / time in format yyyyMMddhhmm
////         log.info( "Setting note type " + note.getType() );
//            node.setProperty( NOTE_TYPE, note.getType() );
////         log.info( "Setting note date " + note.getDate() );
//            node.setProperty( NOTE_DATE, note.getDate() );
////         log.info( "Setting note episode " + note.getEpisode() );
//            node.setProperty( NOTE_EPISODE, note.getEpisode() );
////         log.info( "Setting note text" );
//            node.setProperty( NOTE_TEXT, note.getText() );
////         node.setProperty( NOTE_NAME, note.getId() );
//            tx.success();
//        } catch ( TransactionFailureException txE ) {
//            log.error( txE.getMessage() );
//        } catch ( Exception e ) {
//            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
//            log.error( "Ignoring Exception while adding Note information "
//                    + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
//            // Attempt to continue.
//        }
//
//        final Node noteNode = node;
//        try ( Transaction tx = graphDb.beginTx() ) {
////         log.info( "Setting note sections" );
//            note.getSections().forEach( s -> addSectionInfo( graphDb, log, noteNode, s ) );
//            tx.success();
//        } catch ( TransactionFailureException txE ) {
//            log.error( txE.getMessage() );
//        } catch ( Exception e ) {
//            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
//            log.error( "Ignoring Exception while adding Note sections "
//                    + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
//            // Attempt to continue.
//        }
//
//        try ( Transaction tx = graphDb.beginTx() ) {
////         log.info( "Setting note mentions" );
//            note.getMentions().forEach( m -> addMentionInfo( graphDb, log, noteNode, m ) );
//            tx.success();
//        } catch ( TransactionFailureException txE ) {
//            log.error( txE.getMessage() );
//        } catch ( Exception e ) {
//            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
//            log.error( "Ignoring Exception while adding Note mentions "
//                    + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
//            // Attempt to continue.
//        }
//
//        try ( Transaction tx = graphDb.beginTx() ) {
//            //         log.info( "Setting note relations" );
//            note.getRelations().forEach( r -> addMentionRelation( graphDb, log, r ) );
////         log.error( "Setting note corefs" );
////         note.getCorefs().forEach( c -> addMentionCoref( graphDb, log, c ) );
////         log.info( "Done" );
//            tx.success();
//        } catch ( TransactionFailureException txE ) {
//            log.error( txE.getMessage() );
//        } catch ( Exception e ) {
//            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
//            log.error( "Ignoring Exception while adding Note relations "
//                    + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
//            // Attempt to continue.
//        }
//    }


    private void addNoteInfo(  final GraphDatabaseService graphDb,
                               final Log log,
                               final Node patientNode, final Note note ) {
        _clearableNote.clear();
        Node noteNode = null;
        try ( Transaction tx = graphDb.beginTx() ) {
            final Node allDocumentsNode = SearchUtil.getClassNode( graphDb, EMR_NOTE_URI );
            if ( allDocumentsNode == null ) {
                log.error( "No class for uri " + EMR_NOTE_URI + ".  Cannot create put note in graph." );
                tx.success();
                return;
            }
         log.info( "Checking note node " + note.getId() );
            noteNode = SearchUtil.getLabeledNode( graphDb, TEXT_DOCUMENT_LABEL, note.getId() );
            if ( noteNode != null ) {
                _clearableNote.collectClearables( graphDb, log, noteNode );
             } else {
            log.info( "Creating note node " + note.getId() );
                noteNode = graphDb.createNode( TEXT_DOCUMENT_LABEL );
            log.info( "Setting note name " + note.getId() );
                noteNode.setProperty( NAME_KEY, note.getId() );
            log.info( "Setting instance of " + note.getId() );
                setInstanceOf( graphDb, log, noteNode, allDocumentsNode );
            log.info( "Creating relation subject has note" );
                createRelation( graphDb, log, patientNode, noteNode, SUBJECT_HAS_NOTE_RELATION );
            }
            // Writes note date / time in format yyyyMMddhhmm
         log.info( "Setting note type " + note.getType() );
            noteNode.setProperty( NOTE_TYPE, note.getType() );
         log.info( "Setting note date " + note.getDate() );
            noteNode.setProperty( NOTE_DATE, note.getDate() );
         log.info( "Setting note episode " + note.getEpisode() );
            noteNode.setProperty( NOTE_EPISODE, note.getEpisode() );
         log.info( "Setting note text" );
            noteNode.setProperty( NOTE_TEXT, note.getText() );
//         noteNode.setProperty( NOTE_NAME, note.getId() );
            tx.success();
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Note information "
                       + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }

        final Node noteNode1 = noteNode;
        try ( Transaction tx = graphDb.beginTx() ) {
         log.info( "Setting note sections" );
            note.getSections().forEach( s -> addSectionInfo( graphDb, log, noteNode1, s ) );
            tx.success();
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Note sections "
                       + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }

        try ( Transaction tx = graphDb.beginTx() ) {
         log.info( "Setting note mentions" );
            note.getMentions().forEach( m -> addMentionInfo( graphDb, log, noteNode1, m ) );
            tx.success();
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Note mentions "
                       + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }

        try ( Transaction tx = graphDb.beginTx() ) {
                     log.info( "Setting note relations" );
            note.getRelations().forEach( r -> addMentionRelation( graphDb, log, r ) );
//         log.error( "Setting note corefs" );
//         note.getCorefs().forEach( c -> addMentionCoref( graphDb, log, c ) );
//         log.info( "Done" );
            tx.success();
        } catch ( TransactionFailureException txE ) {
            log.error( txE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Note relations "
                       + note.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
        _clearableNote.removeUnwanted( graphDb, log );
    }






    private void clearNode( final GraphDatabaseService graphDb,
                            final Log log, final Node node ) {
        if ( node == null ) {
            return;
        }
        final Collection<Relationship> noteRelationships
                = SearchUtil.getBranchAllOutRelationships( graphDb, log, node, new HashSet<>(), new HashSet<>() );
        final Collection<Node> endNodes = new HashSet<>( noteRelationships.size() );
        try ( Transaction tx = graphDb.beginTx() ) {
            for ( Relationship relationship : noteRelationships ) {
                final Node startNode = relationship.getStartNode();
                final Node endNode = relationship.getEndNode();
                if ( !relationship.isType( INSTANCE_OF_RELATION ) ) {
                    if ( endNode != null ) {
                        endNodes.add( endNode );
                    }
                    if ( startNode != null && !startNode.equals( node ) ) {
//                  log.info( "Deleting Relationship " + relationship.getType().name()
//                            + " " + relationship.getStartNode().getProperty( NAME_KEY )
//                            + " " + relationship.getEndNode().getProperty( NAME_KEY ));
                        final Collection<String> propertyKeys = new HashSet<>();
                        relationship.getPropertyKeys().forEach( propertyKeys::add );
                        for ( String key : propertyKeys ) {
                            relationship.removeProperty( key );
                        }
                        relationship.delete();
//                  log.info( "deletion done" );
                    }
                }
            }
            tx.success();
        } catch ( MultipleFoundException mfE ) {
            log.error( mfE.getMessage(), mfE );
        }
        try ( Transaction tx = graphDb.beginTx() ) {
            for ( Node endNode : endNodes ) {
//            log.info( "Deleting Node " + endNode.getProperty( NAME_KEY ) + " " + endNode.getId() );
                final Collection<String> propertyKeys = new HashSet<>();
                endNode.getPropertyKeys().forEach( propertyKeys::add );
                for ( String key : propertyKeys ) {
                    endNode.removeProperty( key );
                }
                endNode.delete();
//            log.info( "deletion done" );
            }
            tx.success();
        } catch ( MultipleFoundException mfE ) {
            log.error( mfE.getMessage(), mfE );
        }
    }


    static private class ClearableNote {
        private final Collection<Node> _endNodes = new HashSet<>();
        private final Collection<Relationship> _relationships = new HashSet<>();

        private void clear() {
            _endNodes.clear();
            _relationships.clear();
        }

        private void collectClearables( final GraphDatabaseService graphDb,
                                        final Log log,
                                        final Node noteNode ) {
            if ( noteNode == null ) {
                return;
            }
            final Collection<Relationship> noteRelationships
                  = SearchUtil.getBranchAllOutRelationships( graphDb, log, noteNode, new HashSet<>(), new HashSet<>() );
            try ( Transaction tx = graphDb.beginTx() ) {
                for ( Relationship relationship : noteRelationships ) {
                    final Node startNode = relationship.getStartNode();
                    final Node endNode = relationship.getEndNode();
                    if ( !relationship.isType( INSTANCE_OF_RELATION ) ) {
                        if ( endNode != null ) {
                            _endNodes.add( endNode );
                        }
                        if ( startNode != null && !startNode.equals( noteNode ) ) {
                  log.info( "Have existing Relationship " + relationship.getType().name()
                            + " " + relationship.getStartNode().getProperty( NAME_KEY )
                            + " " + relationship.getEndNode().getProperty( NAME_KEY ));
                            _relationships.add( relationship );
                        }
                    }
                }
                tx.success();
            } catch ( MultipleFoundException mfE ) {
                log.error( mfE.getMessage(), mfE );
            }
        }

        private void keepNode( final Node node ) {
            _endNodes.remove( node );
        }

        private void keepRelationship( final Relationship relationship ) {
            _relationships.remove( relationship );
        }

        private void removeUnwanted( final GraphDatabaseService graphDb, final Log log ) {
            try ( Transaction tx = graphDb.beginTx() ) {
                for ( Relationship relationship : _relationships ) {
                  log.info( "Deleting Relationship " + relationship.getType().name()
                            + " " + relationship.getStartNode().getProperty( NAME_KEY )
                            + " " + relationship.getEndNode().getProperty( NAME_KEY ));
                    relationship.delete();
                  log.info( "Relation deletion done" );
                }
                tx.success();
            } catch( MultipleFoundException mfE ) {
                log.error( mfE.getMessage(), mfE );
            }
            try ( Transaction tx = graphDb.beginTx() ) {
                for ( Node endNode : _endNodes ) {
            log.info( "Deleting Node " + endNode.getProperty( NAME_KEY ) + " " + endNode.getId() );
                    endNode.delete();
            log.info( "Node deletion done" );
                }
                tx.success();
            } catch ( MultipleFoundException mfE ) {
                log.error( mfE.getMessage(), mfE );
            }
        }

    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            SECTION DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    //            node.setProperty( SECTION_TYPE, mention.getSectionType() );
    public void addSectionInfo(  final GraphDatabaseService graphDb,
                                 final Log log,
                                 final Node noteNode,
                                 final Section section ) {
        try ( Transaction tx = graphDb.beginTx() ) {
            Node node = SearchUtil.getLabeledNode( graphDb, DOCUMENT_SECTION_LABEL,  section.getId() );
            if ( node == null ) {
                log.info( "Creating Section node " + section.getId() );
                node = graphDb.createNode( DOCUMENT_SECTION_LABEL );
                node.setProperty( NAME_KEY, section.getId() );
            } else {
                log.info( "Keeping Section node " + section.getId() );
                _clearableNote.keepNode( node );
            }
//            final Node node = graphDb.createNode( DOCUMENT_SECTION_LABEL );
//            node.setProperty( NAME_KEY, section.getId() );
            node.setProperty( SECTION_TYPE, section.getType() );
            node.setProperty( TEXT_SPAN_BEGIN, section.getBegin() );
            node.setProperty( TEXT_SPAN_END, section.getEnd() );

            if ( noteNode != null ) {
                final Relationship relationship
                      = createRelation( graphDb, log, noteNode, node, NOTE_HAS_SECTION_RELATION );
                _clearableNote.keepRelationship( relationship );
            }

            tx.success();
        } catch ( MultipleFoundException mfE ) {
            log.error( mfE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Section information "
                    + section.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            MENTION DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public void addMentionInfo(  final GraphDatabaseService graphDb,
                                 final Log log,
                                 final String noteId,
                                 final Mention mention ) {
        final Node noteNode = SearchUtil.getObjectNode( graphDb, noteId );
        if ( noteNode == null ) {
            log.error( "No Document Node for " + noteId );
            return;
        }
        addMentionInfo( graphDb, log, noteNode, mention );
    }


    private void addMentionInfo(  final GraphDatabaseService graphDb,
                                         final Log log,
                                         final Node noteNode,
                                         final Mention mention ) {
        try ( Transaction tx = graphDb.beginTx() ) {
//         final Node sameNode = graphDb.findNode( TEXT_MENTION_LABEL, NAME_KEY, mention._id );
//         if ( sameNode != null ) {
//            annotationNodes.put( annotation, sameNode );
//            tx.success();
//            return sameNode;
//         }
//         log.info( "Creating Mention node " + mention.getId() );
            Node node = SearchUtil.getLabeledNode( graphDb, TEXT_MENTION_LABEL,  mention.getId() );
            if ( node == null ) {
                node = graphDb.createNode( TEXT_MENTION_LABEL );
                node.setProperty( NAME_KEY, mention.getId() );
            } else {
                _clearableNote.keepNode( node );
            }
//            final Node node = graphDb.createNode( TEXT_MENTION_LABEL );
//            node.setProperty( NAME_KEY, mention.getId() );
//         annotationNodes.put( annotation, node );

            if ( !mention.getClassUri().isEmpty() ) {
                final Node classNode = SearchUtil.getClassNode( graphDb, mention.getClassUri() );
                if ( classNode != null ) {
                    final Relationship instanceOf = setInstanceOf( graphDb, log, node, classNode );
                    _clearableNote.keepRelationship( instanceOf );
                }
            }
            node.setProperty( TEXT_SPAN_BEGIN, mention.getBegin() );
            node.setProperty( TEXT_SPAN_END, mention.getEnd() );
            node.setProperty( INSTANCE_NEGATED, mention.isNegated() );
            node.setProperty( INSTANCE_UNCERTAIN, mention.isUncertain() );
            node.setProperty( INSTANCE_GENERIC, mention.isGeneric() );
            node.setProperty( INSTANCE_CONDITIONAL, mention.isConditional() );
            node.setProperty( INSTANCE_HISTORIC, mention.isHistoric() );
            node.setProperty( INSTANCE_TEMPORALITY, mention.getTemporality() );

            if ( noteNode != null ) {
                final Relationship relationship
                      = createRelation( graphDb, log, noteNode, node, NOTE_HAS_TEXT_MENTION_RELATION );
                _clearableNote.keepRelationship( relationship );
            }
//         log.info( "Created mention node " + mention.getId() + " " + node.getId() );

            final Node sourceNode = SearchUtil.getLabeledNode( graphDb, TEXT_MENTION_LABEL, mention.getId() );
            if ( sourceNode != null ) {
//            log.info( "Have Mention node " + mention.getId() + " " + sourceNode.getId() );
            } else {
                log.error( "Do not have mention node " + mention.getId() );
            }

            tx.success();
        } catch ( MultipleFoundException mfE ) {
            log.error( mfE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while adding Mention information "
                    + mention.getId() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            MENTION RELATION DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public Relationship addMentionRelation(  final GraphDatabaseService graphDb,
                                     final Log log,
                                     final MentionRelation relation ) {
        final Node sourceNode = SearchUtil.getLabeledNode( graphDb, TEXT_MENTION_LABEL, relation.getSourceId() );
        if ( sourceNode == null ) {
            log.error( "No Source Node " + relation.getSourceId() + " for relation " + relation.getType() );
            return null;
        }
        final Node targetNode = SearchUtil.getLabeledNode( graphDb, TEXT_MENTION_LABEL, relation.getTargetId() );
        if ( targetNode == null ) {
            log.error( "No Target Node " + relation.getTargetId() + " for relation " + relation.getType() );
            return null;
        }
        return addMentionRelation( graphDb, log, sourceNode, targetNode, relation.getType() );
    }


    private Relationship addMentionRelation( final GraphDatabaseService graphDb,
                                         final Log log,
                                         final Node sourceNode,
                                         final Node targetNode,
                                         final String relationType ) {
        final Relationship relationship = createRelation( graphDb, log, sourceNode, targetNode, relationType );
        _clearableNote.keepRelationship( relationship );
        return relationship;
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            MENTION COREF DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


//    public void addMentionCoref(  final GraphDatabaseService graphDb,
//                                  final Log log,
//                                  final MentionCoref coref ) {
//        final String[] idChain = coref.getIdChain();
//        if ( idChain.length < 2 ) {
//            return;
//        }
//        final String corefId = coref.getId();
//        final Node firstNode = SearchUtil.getLabeledNode( graphDb, TEXT_MENTION_LABEL, idChain[ 0 ] );
//        if ( firstNode == null ) {
//            log.error( "No First Node " + idChain[ 0 ] + " for coreference chain " + corefId );
//            return;
//        }
//        Node previousNode = firstNode;
//        for ( int i = 1; i < idChain.length; i++ ) {
//            final Node node = SearchUtil.getLabeledNode( graphDb, TEXT_MENTION_LABEL, idChain[ i ] );
//            if ( node == null ) {
//                log.error( "No Node " + idChain[ i ] + " for coreference chain " + corefId );
//                continue;
//            }
//            addMentionCoref( graphDb, log, previousNode, node, corefId );
//            previousNode = node;
//        }
//    }
//
//
//    static private void addMentionCoref(  final GraphDatabaseService graphDb,
//                                          final Log log,
//                                          final Node firstNode,
//                                          final Node nextNode,
//                                          final String id ) {
//        try ( Transaction tx = graphDb.beginTx() ) {
//            for ( Relationship existing : firstNode.getRelationships( MENTION_COREF_RELATION, Direction.BOTH ) ) {
//                if ( existing.getOtherNode( firstNode ).equals( nextNode )
//                        && id.equals( existing.getProperty( COREF_ID ) ) ) {
//                    // Coref Relation already exists
//                    tx.success();
//                    return;
//                }
//            }
//            final Relationship coref = firstNode.createRelationshipTo( nextNode, MENTION_COREF_RELATION );
//            coref.setProperty( COREF_ID, id );
//            tx.success();
//        } catch ( MultipleFoundException mfE ) {
//            log.error( mfE.getMessage() );
//        } catch ( Exception e ) {
//            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
//            log.error( "Ignoring Exception while adding Coreference\n"
//                    + e.getClass().getSimpleName() + "\n" + e.getMessage() );
//            // Attempt to continue.
//        }
//    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            UTILITIES
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    static private Relationship setInstanceOf(  final GraphDatabaseService graphDb,
                                        final Log log,
                                        final Node instanceNode,
                                        final Node classNode ) {
        return createRelation( graphDb, log, instanceNode, classNode, INSTANCE_OF_RELATION );
    }


    private Relationship createRelation(  final GraphDatabaseService graphDb,
                                  final Log log,
                                  final Node node,
                                  final Node relatedNode,
                                  final String relationName ) {
        final RelationshipType relationshipType
                = _relationshipTypes.computeIfAbsent( relationName, RelationshipType::withName );
        return createRelation( graphDb, log, node, relatedNode, relationshipType );
    }


    static private Relationship createRelation( final GraphDatabaseService graphDb,
                                        final Log log,
                                        final Node node,
                                        final Node relatedNode,
                                        final RelationshipType relationshipType ) {
        try ( Transaction tx = graphDb.beginTx() ) {
            for ( Relationship existing : node.getRelationships( relationshipType, Direction.OUTGOING ) ) {
                if ( existing.getOtherNode( node ).equals( relatedNode ) ) {
                    // Relation already exists
                    log.info( "Relationship " + relationshipType.name() + " already exists" );
                    tx.success();
                    return existing;
                }
            }
            final Relationship relationship = node.createRelationshipTo( relatedNode, relationshipType );
            tx.success();
            return relationship;

        } catch ( MultipleFoundException mfE ) {
            log.error( mfE.getMessage() );
        } catch ( Exception e ) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error( "Ignoring Exception while creating relation "
                    + relationshipType.name() + "\n" + e.getClass().getSimpleName() + "\n" + e.getMessage() );
            // Attempt to continue.
        }
        return null;
    }


}