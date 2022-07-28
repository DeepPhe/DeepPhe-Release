package org.healthnlp.deepphe.neo4j.reader;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.neo4j.util.DataUtil;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.healthnlp.deepphe.neo4j.util.StructuredPatientDataGenerator;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Name;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.neo4j.util.DataUtil.safeGetProperty;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/27/2020
 */


public enum NodeReader {
    INSTANCE;

    @SuppressWarnings("SameReturnValue")
    static public NodeReader getInstance() {
        return INSTANCE;
    }

    static public final Collection<String> BIOMARKERS = Arrays.asList(
            HAS_ER_STATUS,
            HAS_PR_STATUS,
            HAS_HER2_STATUS);

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            COHORT DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////

    //JDL 2022-07-25: compiler says this is unused?
    @SuppressWarnings("unused")
    public List<PatientDiagnosis> getPatientDiagnoses(final GraphDatabaseService graphDb,
                                                      final Log log) {
        return DataUtil.getAllPatientNodes(graphDb)
                .stream()
                .map(n -> createPatientDiagnoses(graphDb, log, n))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<PatientDiagnosis> createPatientDiagnoses(final GraphDatabaseService graphDb,
                                                          final Log log,
                                                          final Node patientNode) {
        final String patientId = DataUtil.getNodeName(graphDb, patientNode);
        List<NeoplasmSummary> cancers = getCancers(graphDb, log, patientId);
        if (cancers != null) {
            return cancers.stream()
                    .map(c -> createPatientDiagnosis(patientId, c))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    static private PatientDiagnosis createPatientDiagnosis(final String patientId, final NeoplasmSummary cancer) {
        final PatientDiagnosis diagnosis = new PatientDiagnosis();
        diagnosis.setPatientId(patientId);
        diagnosis.setClassUri(cancer.getClassUri());
        return diagnosis;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            PATIENT DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////

    public Patient getPatient(final GraphDatabaseService graphDb,
                              final Log log,
                              final String patientId) {

        try (Transaction tx = graphDb.beginTx()) {
            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
            if (patientNode == null) {
                return null;
            }
            Patient patient = getPatient(graphDb, log, patientNode, patientId);
            tx.success();
            return patient;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get patient " + patientId + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return null;
    }


    public Patient getPatient(final GraphDatabaseService graphDb,
                              final Log log,
                              final Node patientNode,
                              final String patientId) {
        if (patientNode == null) {
            log.error("No patient node for " + patientId);
            return null;
        }
        final Patient patient = new Patient();
        patient.setId(patientId);
        patient.setBirth("");
        patient.setDeath("");
        patient.setGender("");
        patient.setName(patientId);

        final List<Note> notes = getNotes(graphDb, log, patientNode);
        patient.setNotes(notes);

        final NewPatientDiagnosis diagnosis = getDiagnosis(graphDb, log, patientId);
        List<NewPatientDiagnosis> newPatientDiagnoses = new ArrayList<>();
        newPatientDiagnoses.add(diagnosis);
        patient.setDiagnoses(newPatientDiagnoses);

        final List<NewBiomarkerSummary> biomarkers = getBiomarkers(graphDb, log, patientId);
        patient.setBiomarkers(biomarkers);
        return patient;
    }

    //JDL 2022-07-25: compiler says this is unused?
    @SuppressWarnings("unused")
    public PatientSummary getPatientSummary(final GraphDatabaseService graphDb,
                                            final Log log,
                                            final Node patientNode) {
        log.debug("In getPatientSummary...");
        if (patientNode == null) {
            log.error("Null Patient Node to getPatientSummary");
            return null;
        }
        final String patientId = DataUtil.getNodeName(graphDb, patientNode);
        if (patientId.equals(MISSING_NODE_NAME)) {
            log.error("No patient Id for " + patientNode.getId());
            return null;
        }
        return getPatientSummary(graphDb, log, patientNode, patientId);
    }

    public PatientSummary getPatientSummary(final GraphDatabaseService graphDb,
                                            final Log log,
                                            final Node patientNode,
                                            final String patientId) {
        if (patientNode == null) {
            log.error("No patient node for " + patientId);
            return null;
        }
        final PatientSummary patientSummary = new PatientSummary();
        patientSummary.setId(patientId);
        final Patient patient = getPatient(graphDb, log, patientNode, patientId);
        if (patient == null) {
            return null;
        }
        patientSummary.setPatient(patient);
        // get cancer summaries
        final List<NeoplasmSummary> cancers = getCancers(graphDb, log, patientId);
        patientSummary.setNeoplasms(cancers);
        return patientSummary;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            NEOPLASM DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////

    private List<NeoplasmSummary> getCancers(final GraphDatabaseService graphDb,
                                             final Log log,
                                             final String patientId) {
        final List<NeoplasmSummary> cancers = new ArrayList<>();
        try (Transaction tx = graphDb.beginTx()) {

            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
            if (patientNode == null) {
                log.error("Error in getCancers().  Looking for node named " + patientId + " but it does not exist.");
                return null;
            }
            SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION)
                    .stream()
                    .map(n -> createCancer(graphDb, log, n))
                    .filter(Objects::nonNull)
                    .forEach(cancers::add);
            tx.success();
        } catch (TransactionFailureException txE) {
            log.error("Cannot get cancers for " + patientId + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return cancers;
    }

//    JDL 2022-07-25: Not used...remove?
//
//    private List<NeoplasmSummary> getTumors(final GraphDatabaseService graphDb,
//                                            final Log log,
//                                            final String patientId) {
//        log.debug("In getTumors...");
//        final List<NeoplasmSummary> cancers = new ArrayList<>();
//        try (Transaction tx = graphDb.beginTx()) {
//            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
//            if (patientNode == null) {
//                log.error("Error in getCancers().  Looking for node named " + patientId + " but it does not exist.");
//                tx.success();
//                return null;
//            }
//            SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_FACT_RELATION)
//                    .stream()
//                    .map(n -> createCancer(graphDb, log, n))
//                    .filter(Objects::nonNull)
//                    .forEach(cancers::add);
//            tx.success();
//        } catch (TransactionFailureException txE) {
//            log.error("Cannot get cancers for " + patientId + " from graph.");
//            log.error(txE.getMessage());
//        } catch (Exception e) {
//            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
//            log.error("Ignoring Exception " + e.getMessage());
//            // Attempt to continue.
//        }
//        return cancers;
//    }


    private NeoplasmSummary createCancer(final GraphDatabaseService graphDb,
                                         final Log log,
                                         final Node cancerNode) {
        final NeoplasmSummary cancer = new NeoplasmSummary();

        cancer.setHer2(SearchUtil.getAttributeByNameAsString(graphDb, log, cancerNode, HAS_HER2_STATUS));
        cancer.setEr(SearchUtil.getAttributeByNameAsString(graphDb, log, cancerNode, HAS_ER_STATUS));
        cancer.setPr(SearchUtil.getAttributeByNameAsString(graphDb, log, cancerNode, HAS_PR_STATUS));

        try (Transaction tx = graphDb.beginTx()) {
            populateNeoplasm(graphDb, log, cancer, cancerNode);
            final List<NeoplasmSummary> tumors = new ArrayList<>();
            SearchUtil.getOutRelatedNodes(graphDb, cancerNode, CANCER_HAS_TUMOR_RELATION)
                    .stream()
                    .map(t -> populateNeoplasm(graphDb, log, new NeoplasmSummary(), t))
                    .filter(Objects::nonNull)
                    .forEach(tumors::add);
            cancer.setSubSummaries(tumors);
            tx.success();
            return cancer;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get cancer " + cancerNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return null;
    }

    private NeoplasmSummary populateNeoplasm(final GraphDatabaseService graphDb,
                                             final Log log,
                                             final NeoplasmSummary neoplasm,
                                             final Node neoplasmNode) {
        try (Transaction tx = graphDb.beginTx()) {
            neoplasm.setId(DataUtil.objectToString(neoplasmNode.getProperty(NAME_KEY)));
            neoplasm.setClassUri(DataUtil.getUri(graphDb, neoplasmNode));
            final List<NeoplasmAttribute> attributes = getAttributes(graphDb, log, neoplasmNode);
            neoplasm.setAttributes(attributes);
            tx.success();
            return neoplasm;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get neoplasm " + neoplasmNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return null;
    }

    public List<NeoplasmAttribute> getAttributes(final GraphDatabaseService graphDb,
                                                 final Log log,
                                                 final Node neoplasmNode) {
        final List<NeoplasmAttribute> attributes = new ArrayList<>();
        try (Transaction tx = graphDb.beginTx()) {
            SearchUtil.getOutRelatedNodes(graphDb, neoplasmNode, NEOPLASM_HAS_ATTRIBUTE_RELATION)
                    .stream()
                    .map(a -> createAttribute(graphDb, log, a))
                    .filter(Objects::nonNull)
                    .forEach(attributes::add);
            tx.success();
        } catch (TransactionFailureException txE) {
            log.error("Cannot get attributes for " + neoplasmNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return attributes;
    }

    private NeoplasmAttribute createAttribute(final GraphDatabaseService graphDb,
                                              final Log log,
                                              final Node attributeNode) {
        try (Transaction tx = graphDb.beginTx()) {

            final NeoplasmAttribute attribute = new NeoplasmAttribute();

            attribute.setId(DataUtil.objectToString(attributeNode.getProperty(NAME_KEY)));
            if (attributeNode.hasProperty(ATTRIBUTE_URI))
                attribute.setClassUri(DataUtil.objectToString(attributeNode.getProperty(ATTRIBUTE_URI)));
            attribute.setName(DataUtil.objectToString(attributeNode.getProperty(ATTRIBUTE_NAME)));
            attribute.setValue(DataUtil.objectToString(attributeNode.getProperty(ATTRIBUTE_VALUE)));
            final List<Mention> directEvidence = new ArrayList<>();
            SearchUtil.getOutRelatedNodes(graphDb, attributeNode, ATTRIBUTE_DIRECT_MENTION_RELATION)
                    .stream()
                    .map(m -> createMention(graphDb, log, m))
                    .filter(Objects::nonNull)
                    .forEach(directEvidence::add);
            attribute.setDirectEvidence(directEvidence);
            final List<Mention> indirectEvidence = new ArrayList<>();
            SearchUtil.getOutRelatedNodes(graphDb, attributeNode, ATTRIBUTE_INDIRECT_MENTION_RELATION)
                    .stream()
                    .map(m -> createMention(graphDb, log, m))
                    .filter(Objects::nonNull)
                    .forEach(indirectEvidence::add);
            attribute.setIndirectEvidence(indirectEvidence);
            final List<Mention> notEvidence = new ArrayList<>();
            SearchUtil.getOutRelatedNodes(graphDb, attributeNode, ATTRIBUTE_NOT_MENTION_RELATION)
                    .stream()
                    .map(m -> createMention(graphDb, log, m))
                    .filter(Objects::nonNull)
                    .forEach(notEvidence::add);
            attribute.setNotEvidence(notEvidence);
            tx.success();

            return attribute;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get attribute " + attributeNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            NOTE DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public Note getNote(final GraphDatabaseService graphDb,
                        final Log log,
                        final String noteId) {
        try (Transaction tx = graphDb.beginTx()) {
            final Node noteNode = SearchUtil.getLabeledNode(graphDb, TEXT_DOCUMENT_LABEL, noteId);
            if (noteNode == null) {
                log.error("No note node for " + noteId);
                tx.success();
                return null;
            }
            final Note note = createNote(graphDb, log, noteNode);
            tx.success();
            return note;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get note " + noteId + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        final Note badNote = new Note();
        badNote.setId(noteId);
        return badNote;
    }

    private List<NewBiomarkerSummary> getBiomarkers(final GraphDatabaseService graphDb,
                                                    final Log log,
                                                    final String patientId) {
        final List<NewBiomarkerSummary> biomarkers = new ArrayList<>();

        try (Transaction tx = graphDb.beginTx()) {

            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);

            final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode,
                    SUBJECT_HAS_CANCER_RELATION);
            for (Node cancerNode : cancerNodes) {
                for (String biomarker : BIOMARKERS) {
                    String markerValues = SearchUtil.getAttributeByNameAsString(graphDb, log, cancerNode, biomarker);
                    if (markerValues != null) {
                        String[] values = markerValues.split(";");
                        for (String value : values) {
                            NewBiomarkerSummary biomarkerSummary = new NewBiomarkerSummary();
                            biomarkerSummary.setPatientId(patientId);
                            biomarkerSummary.setTumorFactRelation(biomarker);
                            biomarkerSummary.setValueText(value);
                            //debug here, need to do this once for positive once for negative
                            biomarkerSummary.setRelationPrettyName(DataUtil.getRelationPrettyName(biomarkerSummary.getTumorFactRelation()));
                            biomarkers.add(biomarkerSummary);
                        }
                    }
                }
            }

            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getBiomarkers() " + e.getMessage());
        }

        return biomarkers;
    }


    private NewPatientDiagnosis getDiagnosis(final GraphDatabaseService graphDb,
                                             final Log log,
                                             final String patientId) {
        log.debug("In getDiagnosis()...");
        final NewPatientDiagnosis patientDiagnosis = new NewPatientDiagnosis();
        final Map<String, String> diagnosisGroupNames = UriConstants.getDiagnosisGroupNames(graphDb);

        try (Transaction tx = graphDb.beginTx()) {
            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
            final List<String> diagnoses = new ArrayList<>();
            final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode,
                    SUBJECT_HAS_CANCER_RELATION);
            for (Node cancerNode : cancerNodes) {
                diagnoses.add(DataUtil.getPreferredText(graphDb, cancerNode));
            }
            Collections.sort(diagnoses);

            final Collection<String> diagnosisGroups
                    = diagnoses.stream()
                    .map(d -> diagnosisGroupNames.getOrDefault(d, "Unknown"))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            patientDiagnosis.setDiagnosis(diagnoses);
            patientDiagnosis.setDiagnosisGroups(new ArrayList<>(diagnosisGroups));
            patientDiagnosis.setPatientId(patientId);

            tx.success();
        }
        return patientDiagnosis;
    }

    private List<Note> getNotes(final GraphDatabaseService graphDb,
                                final Log log,
                                final Node patientNode) {
        final List<Note> notes = new ArrayList<>();
        try (Transaction tx = graphDb.beginTx()) {
            SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_NOTE_RELATION)
                    .stream()
                    .map(n -> createNote(graphDb, log, n))
                    .forEach(notes::add);
            tx.success();
        } catch (TransactionFailureException txE) {
            log.error("Cannot get notes for " + patientNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return notes;
    }


    private Note createNote(final GraphDatabaseService graphDb,
                            final Log log,
                            final Node noteNode) {
        final Note note = new Note();
        try (Transaction tx = graphDb.beginTx()) {
            note.setId(DataUtil.objectToString(noteNode.getProperty(NAME_KEY)));
            note.setType(DataUtil.objectToString(noteNode.getProperty(NOTE_TYPE)));
            note.setDate(DataUtil.objectToString(noteNode.getProperty(NOTE_DATE)));
            note.setEpisode(DataUtil.objectToString(noteNode.getProperty(NOTE_EPISODE)));
            note.setText(DataUtil.objectToString(noteNode.getProperty(NOTE_TEXT)));

            note.setSections(getSections(graphDb, log, noteNode));
            final Collection<FullMention> fullMentions = getFullMentions(graphDb, log, noteNode);
            note.setMentions(getMentions(fullMentions));
            note.setRelations(getRelations(fullMentions));
            note.setCorefs(getCorefs(fullMentions));

            tx.success();
        } catch (TransactionFailureException txE) {
            log.error("Cannot get Note " + noteNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return note;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            SECTION DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public List<Section> getSections(final GraphDatabaseService graphDb,
                                     final Log log,
                                     final Node noteNode) {
        final List<Section> sections = new ArrayList<>();
        try (Transaction tx = graphDb.beginTx()) {
            SearchUtil.getOutRelatedNodes(graphDb, noteNode, NOTE_HAS_SECTION_RELATION)
                    .stream()
                    .map(s -> createSection(graphDb, log, s))
                    .filter(Objects::nonNull)
                    .forEach(sections::add);
            tx.success();
        } catch (TransactionFailureException txE) {
            log.error("Cannot get sections for " + noteNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return sections;
    }


    private Section createSection(final GraphDatabaseService graphDb,
                                  final Log log,
                                  final Node sectionNode) {
        try (Transaction tx = graphDb.beginTx()) {
            final Section section = new Section();
            section.setId(DataUtil.objectToString(sectionNode.getProperty(NAME_KEY)));
            section.setType(DataUtil.objectToString(sectionNode.getProperty(SECTION_TYPE)));
            section.setBegin(DataUtil.objectToInt(sectionNode.getProperty(TEXT_SPAN_BEGIN)));
            section.setEnd(DataUtil.objectToInt(sectionNode.getProperty(TEXT_SPAN_END)));
            tx.success();
            return section;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get Section " + sectionNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            MENTION RELATION AND COREF DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    public Collection<FullMention> getFullMentions(final GraphDatabaseService graphDb,
                                                   final Log log,
                                                   final Node noteNode) {
        final Collection<FullMention> mentions = new ArrayList<>();
        try (Transaction tx = graphDb.beginTx()) {
            SearchUtil.getOutRelatedNodes(graphDb, noteNode, NOTE_HAS_TEXT_MENTION_RELATION)
                    .stream()
                    .map(m -> createFullMention(graphDb, log, m))
                    .filter(Objects::nonNull)
                    .forEach(mentions::add);
            tx.success();
        } catch (TransactionFailureException txE) {
            log.error("Cannot get mentions for " + noteNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return mentions;
    }

    private FullMention createFullMention(final GraphDatabaseService graphDb,
                                          final Log log,
                                          final Node mentionNode) {
        try (Transaction tx = graphDb.beginTx()) {
            final Mention mention = createMention(graphDb, log, mentionNode);
            if (mention == null) {
                tx.success();
                return null;
            }
            final FullMention fullMention = new FullMention(mention);
            for (Relationship relation : mentionNode.getRelationships(Direction.OUTGOING)) {
                final String relationName = relation.getType().name();
                if (relationName.equals(INSTANCE_OF)) {
                    continue;
                }
                if (relationName.equals(MENTION_COREF)) {
                    fullMention.addCoref(DataUtil.objectToString(relation.getProperty(COREF_ID)));
                    continue;
                }
                final Node targetNode = relation.getOtherNode(mentionNode);
                fullMention.addRelation(DataUtil.objectToString(targetNode.getProperty(NAME_KEY)), relationName);
            }

            tx.success();
            return fullMention;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get Mention " + mentionNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return null;
    }

    public Mention createMention(final GraphDatabaseService graphDb,
                                 final Log log,
                                 final Node mentionNode) {
        try (Transaction tx = graphDb.beginTx()) {
            final Mention mention = new Mention();
            mention.setId(DataUtil.objectToString(mentionNode.getProperty(NAME_KEY)));
            mention.setClassUri(DataUtil.getUri(graphDb, mentionNode));
            for (Relationship relation : mentionNode.getRelationships(Direction.INCOMING,
                    NOTE_HAS_TEXT_MENTION_RELATION)) {
                final Node noteNode = relation.getOtherNode(mentionNode);
                mention.setNoteId(DataUtil.objectToString(noteNode.getProperty(NAME_KEY)));
                mention.setNoteType(DataUtil.objectToString(noteNode.getProperty(NOTE_TYPE)));
            }
            mention.setBegin(DataUtil.objectToInt(mentionNode.getProperty(TEXT_SPAN_BEGIN)));
            mention.setEnd(DataUtil.objectToInt(mentionNode.getProperty(TEXT_SPAN_END)));
            mention.setNegated(DataUtil.objectToBoolean(mentionNode.getProperty(INSTANCE_NEGATED)));
            mention.setUncertain(DataUtil.objectToBoolean(mentionNode.getProperty(INSTANCE_UNCERTAIN)));
            mention.setGeneric(DataUtil.objectToBoolean(mentionNode.getProperty(INSTANCE_GENERIC)));
            mention.setConditional(DataUtil.objectToBoolean(mentionNode.getProperty(INSTANCE_CONDITIONAL)));
            mention.setHistoric(DataUtil.objectToBoolean(mentionNode.getProperty(INSTANCE_HISTORIC)));
            mention.setTemporality(DataUtil.objectToString(mentionNode.getProperty(INSTANCE_TEMPORALITY)));
            tx.success();
            return mention;
        } catch (TransactionFailureException txE) {
            log.error("Cannot get Mention " + mentionNode.getId() + " from graph.");
            log.error(txE.getMessage());
        } catch (Exception e) {
            // While it is bad practice to catch pure Exception, neo4j throws undeclared exceptions of all types.
            log.error("Ignoring Exception " + e.getMessage());
            // Attempt to continue.
        }
        return null;
    }

    private List<Mention> getMentions(final Collection<FullMention> fullMentions) {
        return fullMentions.stream()
                .map(FullMention::getMention)
                .collect(Collectors.toList());
    }


    private List<MentionRelation> getRelations(final Collection<FullMention> fullMentions) {
        return fullMentions.stream()
                .map(FullMention::getRelations)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    private List<MentionCoref> getCorefs(final Collection<FullMention> fullMentions) {
        final Map<String, Collection<String>> corefMap = new HashMap<>();
        for (FullMention mention : fullMentions) {
            mention.getCorefs().forEach(c -> corefMap.computeIfAbsent(c, d -> new HashSet<>()).add(mention.getId()));
        }
        final List<MentionCoref> corefs = new ArrayList<>(corefMap.size());
        for (Map.Entry<String, Collection<String>> entry : corefMap.entrySet()) {
            final MentionCoref coref = new MentionCoref();
            coref.setId(entry.getKey());
            coref.setIdChain(entry.getValue().toArray(new String[0]));
            corefs.add(coref);
        }
        return corefs;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            MENTION RELATION AND COREF HELPER CLASS
    //
    /////////////////////////////////////////////////////////////////////////////////////////

    static private PatientInfo createPatientInfo(final Node patientNode) throws RuntimeException, ParseException {
        String patientId = DataUtil.objectToString(patientNode.getProperty(NAME_KEY));
        if (patientId == null) {
            //unrecoverable error?
            throw new RuntimeException("Node supplied to createSharedPatientProperties does not contain required property: " + NAME_KEY);
        }
        NewStructuredPatientData structuredPatientData = getStructuredPatientDataForPatientId(patientId);

        if (structuredPatientData != null) {
            return populateNewRandomPatient(structuredPatientData);
        } else return null;
    }

    public static PatientInfo populateNewRandomPatient(NewStructuredPatientData structuredPatientData) throws ParseException {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String fed = structuredPatientData.getFirstEncounterDate();
        String led = structuredPatientData.getLastEncounterDate();
        String dob = structuredPatientData.getBirthdate();

        Date fedDate = simpleDateFormat.parse(fed);
        Date ledDate = simpleDateFormat.parse(led);
        Date dobDate = simpleDateFormat.parse(dob);

        @SuppressWarnings("IntegerDivisionInFloatingPointContext") int ageAtFirstEncounter = Math.round((fedDate.getTime() - dobDate.getTime()) / StructuredPatientDataGenerator.MILLS_IN_A_YEAR);
        @SuppressWarnings("IntegerDivisionInFloatingPointContext") int ageAtLastEncounter = Math.round((ledDate.getTime() - dobDate.getTime()) / StructuredPatientDataGenerator.MILLS_IN_A_YEAR);

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setPatientId(structuredPatientData.getPatientId());
        patientInfo.setPatientName(structuredPatientData.getFirstname() + " " + structuredPatientData.getLastname());
        patientInfo.setBirthDate(structuredPatientData.getBirthdate());
        patientInfo.setFirstEncounterAge(Integer.toString(ageAtFirstEncounter));
        patientInfo.setFirstEncounterDate(structuredPatientData.getFirstEncounterDate());
        patientInfo.setLastEncounterAge(Integer.toString(ageAtLastEncounter));
        patientInfo.setLastEncounterDate(structuredPatientData.getLastEncounterDate());
        patientInfo.setGender(structuredPatientData.getGender());
        return patientInfo;

    }

    public static NewStructuredPatientData getStructuredPatientDataForPatientId(String actualPatientId) {
        //the idea is to try to get the same random person values, given the same actualPatientId
        StructuredPatientDataGenerator structuredPatientDataGenerator = new StructuredPatientDataGenerator(actualPatientId);
        return structuredPatientDataGenerator.iterator().next();
    }

    //TODO: throwing generic exception, make it more specific
    static private PatientInfoAndStages createSharedPatientProperties(final Node patientNode) throws
            ParseException {
        String actualPatientId = DataUtil.objectToString(patientNode.getProperty(NAME_KEY));
        PatientInfo patientInfo = populateNewRandomPatient(Objects.requireNonNull(getStructuredPatientDataForPatientId(actualPatientId)));
        return new PatientInfoAndStages(patientInfo);
    }

    public PatientSummaryAndStagesList patientSummaryAndStagesList(GraphDatabaseService graphDb, Log log,
                                                                   boolean includeStages) {
        log.debug("In patientSummaryAndStagesList...");
        PatientSummaryAndStagesList patientSummaryAndStagesList = new PatientSummaryAndStagesList();
        try (Transaction tx = graphDb.beginTx()) {
            // DataUtil.getAllPatientNodes() is supposed to return all unique patients
            final Collection<Node> patientNodes = DataUtil.getAllPatientNodes(graphDb);
            for (Node patientNode : patientNodes) {
                PatientInfoAndStages patientSummaryAndStages = createSharedPatientProperties(patientNode);
                if (includeStages) {
                    // get the major stage values for the patient
                    final Set<String> stages = new HashSet<>();
                    final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode,
                            SUBJECT_HAS_CANCER_RELATION);
                    for (Node cancerNode : cancerNodes) {
                        // TODO : We are no longer using specific relation names for the various attributes.
                        //  Using a unique relation name per attribute type makes the graph too 'ugly', especially since we
                        //  are working up to 50+ attribute types.
                        // We are now using the Neo4jConstants NEOPLASM_HAS_ATTRIBUTE / NEOPLASM_HAS_ATTRIBUTE_RELATION to
                        // specify normalized NeoplasmAttribute for any attribute type.
                        // The attribute has a normalized ontology uri, "human readable" name and value.
                        // TODO once this is moved to ReadFunctions it should be more like:*/

                        SearchUtil.getOutRelatedNodes(graphDb, cancerNode, NEOPLASM_HAS_ATTRIBUTE_RELATION)
                                .stream()
                                .filter(n -> DataUtil.objectToString(n.getProperty(ATTRIBUTE_NAME)).equals("stage"))
                                .map(n -> DataUtil.objectToString(n.getProperty(ATTRIBUTE_URI)))
                                //.map( TextFormatter::toPrettyStage )
                                .forEach(stages::add);
                        // TODO - note that we should probably make an AttributeConstants class or something like that.
//                    //  SearchUtil.getOutRelatedNodes( graphDb, cancerNode, HAS_STAGE )
//                                .stream()
//                                .map( n -> DataUtil.getUri( graphDb, n ) )
//                                .map( TextFormatter::toPrettyStage )
//                                .forEach( stages::add );
                    }
                    // Also add stages for cohort
                    //patientProperties.put( "stages", stages );
                    patientSummaryAndStages.getStages().addAll(stages);
                }

                // Add to the set, this doesn't allow duplicates
                patientSummaryAndStagesList.getPatientSummaryAndStages().add(patientSummaryAndStages);
            }
            tx.success();

        } catch (Exception e) {
            throw new RuntimeException("Failed to call getCohortData():" + e.getMessage());
        }
        return patientSummaryAndStagesList;
    }

//JDL 2022-07-25: compiler says this is unused?
//    private static String convertToLogSyntax(NeoplasmAttribute neoplasmAttribute) {
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("id: ").append(neoplasmAttribute.getId()).append("\n");
//        stringBuilder.append(" |--classUri : ").append(neoplasmAttribute.getClassUri()).append("\n");
//        stringBuilder.append(" |--name: ").append(neoplasmAttribute.getName()).append("\n");
//        stringBuilder.append(" |--value: ").append(neoplasmAttribute.getValue()).append("\n");
//        stringBuilder.append(" |--confidence: ").append(neoplasmAttribute.getConfidence()).append("\n");
//        if (neoplasmAttribute.getConfidenceFeatures() != null)
//            stringBuilder.append(" +--confidenceFeatures size: ").append(neoplasmAttribute.getConfidenceFeatures().size()).append("\n");
//        if (neoplasmAttribute.getIndirectEvidence() != null)
//            stringBuilder.append(" +--indirectEvidence size: ").append(neoplasmAttribute.getIndirectEvidence().size()).append("\n");
//        if (neoplasmAttribute.getDirectEvidence() != null)
//            stringBuilder.append(" +--directEvidence size: ").append(neoplasmAttribute.getDirectEvidence().size()).append("\n");
//        if (neoplasmAttribute.getNotEvidence() != null)
//            stringBuilder.append(" +--notEvidence size: ").append(neoplasmAttribute.getNotEvidence().size()).append("\n");
//
//        return stringBuilder.toString();
//    }


//    JDL 2022-07-25: Not used...remove?
//    private static void logAttribute(String message, NeoplasmAttribute neoplasmAttribute, Log log) {
//        if (neoplasmAttribute != null) {
//            String logMessage = convertToLogSyntax(neoplasmAttribute);
//            log.info(message + logMessage);
//        }
//    }

    //should be using getAttributes()
    public NewCancerAndTumorSummary getCancerAndTumorSummary(GraphDatabaseService graphDb, Log log, String
            patientId) {

        NewCancerAndTumorSummary cancerAndTumorSummary = new NewCancerAndTumorSummary();
        List<NewCancerSummary> cancers = new ArrayList<>();
        cancerAndTumorSummary.setCancers(cancers);

        List<NeoplasmSummary> neoplasmSummaries = getCancers(graphDb, log, patientId);

        for (NeoplasmSummary cancer : Objects.requireNonNull(neoplasmSummaries)) {
            // Cancer summary
            //final Map<String, Object> cancer = new HashMap<>();
            final NewCancerSummary cancerSummary = new NewCancerSummary();

            final String summaryName = cancer.getId();
            // final String cancerId = DataUtil.objectToString( cancerNode.getProperty( NAME_KEY ) );
            cancerSummary.setCancerId(summaryName);

            List<NewTumorSummary> tumors = new ArrayList<>();
            cancerSummary.setTumors(tumors);

            List<NewCancerFact> cancerFacts = new ArrayList<>();
            cancerSummary.setCancerFacts(cancerFacts);

            for (NeoplasmAttribute neoplasmAttribute : cancer.getAttributes()) {
                //logAttribute("\ncancer attribute:\n", neoplasmAttribute, log);
                NewCancerFact cancerFact = new NewCancerFact();
                cancerFact.setRelation(neoplasmAttribute.getName());
                cancerFact.setRelationPrettyName(DataUtil.getRelationPrettyName(cancerFact.getRelation()));

                NewFactInfo newCancerFactInfo = new NewFactInfo();
                newCancerFactInfo.setId(neoplasmAttribute.getId());
                //7-15-2022
                //newCancerFactInfo.setName(neoplasmAttribute.getClassUri());
                newCancerFactInfo.setName(neoplasmAttribute.getName());
                newCancerFactInfo.setValue(neoplasmAttribute.getValue());

                //some of these ^^^ are null?
                newCancerFactInfo.setPrettyName(DataUtil.getRelationPrettyName(newCancerFactInfo.getName()));
                cancerFact.setCancerFactInfo(newCancerFactInfo);
                cancerFacts.add(cancerFact);

            }

            for (NeoplasmSummary tumor : cancer.getSubSummaries()) {
                NewTumorSummary tumorSummary = new NewTumorSummary();

                tumorSummary.setTumorId(tumor.getId());

                List<NewTumorFact> tumorFacts = new ArrayList<>();

                String behavior = "Generic";
                for (NeoplasmAttribute tumorAttribute : tumor.getAttributes()) {
                    if (tumorAttribute.getName().equalsIgnoreCase("behavior"))
                        behavior = tumorAttribute.getClassUri();

                    //logAttribute("\ntumor:\n", tumorAttribute, log);
                    NewTumorFact tumorFact = new NewTumorFact();

                    NewFactInfo newTumorFactInfo = new NewFactInfo();
                    newTumorFactInfo.setId(tumorAttribute.getId());
                    newTumorFactInfo.setName(tumorAttribute.getClassUri());
                    newTumorFactInfo.setValue(tumorAttribute.getValue());
                    newTumorFactInfo.setPrettyName(DataUtil.getRelationPrettyName(newTumorFactInfo.getName()));
                    tumorFact.setTumorFactInfo(newTumorFactInfo);
                    tumorFact.setRelationPrettyName(DataUtil.getRelationPrettyName(tumorAttribute.getName()));
                    tumorFact.setRelation(tumorAttribute.getName());
                    tumorFacts.add(tumorFact);
                }
                tumorSummary.setHasTumorType(behavior);
                tumorSummary.setTumorFacts(tumorFacts);
                tumors.add(tumorSummary);
            }

            cancers.add(cancerSummary);
        }
        return cancerAndTumorSummary;
    }

    //JDL 2022-07-25: compiler says this is unused?
    @SuppressWarnings("unused")
    public List<GuiPatientSummary> getPatientSummary(GraphDatabaseService graphDb, Log log, String patientId) {
        log.debug("In patientSummaryAndStagesList...");
        return DataUtil.getAllPatientNodes(graphDb)
                .stream()
                .map(n -> createPatientSummary(graphDb, n))
                .filter(n -> n.getPatientInfo().getPatientId().equals(patientId))
                .collect(Collectors.toList());
    }

    //JDL 2022-07-25: compiler says this is unused?
    @SuppressWarnings("unused")
    public List<GuiPatientSummary> getPatientSummaries(GraphDatabaseService graphDb, Log log) {
        log.debug("In getPatientSummaries...");
        return DataUtil.getAllPatientNodes(graphDb)
                .stream()
                .map(n -> createPatientSummary(graphDb, n))
                //.filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private GuiPatientSummary createPatientSummary(GraphDatabaseService graphDb, Node patientNode) {

        final Collection<Node> notes = SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_NOTE_RELATION);
        final List<NewReport> reportList = new ArrayList<>();
        // For each note, add a patient object
        for (Node note : notes) {
            NewReport report = new NewReport();

            // Report ID
            //report.setId(DataUtil.objectToString(note.getProperty(NAME_KEY)));
            report.setId(safeGetProperty(note, NAME_KEY, NAME_KEY + "_property_not_found"));
            // Report principal date
            report.setDate(DataUtil.getReportDate(safeGetProperty(note, NOTE_DATE, NOTE_DATE + "_property_not_found")));
            // Report title/name
            report.setReportName(safeGetProperty(note, NOTE_NAME, NOTE_NAME + "_property_not_found"));
            // Report type
            report.setType(safeGetProperty(note, NOTE_TYPE, NOTE_TYPE + "_property_not_found"));
            // Report episode
            report.setEpisode(safeGetProperty(note, NOTE_EPISODE, NOTE_EPISODE + "_property_not_found"));
            // Add to the reportList
            reportList.add(report);
        }
        GuiPatientSummary patientSummary = new GuiPatientSummary();
        try {
            patientSummary.setPatientInfo(createPatientInfo(patientNode));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        patientSummary.setReportData(reportList);
        return patientSummary;

    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            MENTION RELATION AND COREF HELPER CLASS
    //
    /////////////////////////////////////////////////////////////////////////////////////////


    static private class FullMention {
        private final Mention _mention;
        private Collection<HalfRelation> _relations;
        private Collection<String> _corefs;

        private FullMention(final Mention mention) {
            _mention = mention;
        }

        private void addRelation(final String targetId, final String relationType) {
            if (_relations == null) {
                _relations = new HashSet<>();
            }
            _relations.add(new HalfRelation(targetId, relationType));
        }

        private void addCoref(final String chainId) {
            if (_corefs == null) {
                _corefs = new HashSet<>();
            }
            _corefs.add(chainId);
        }

        public String getId() {
            return _mention.getId();
        }

        public Mention getMention() {
            return _mention;
        }

        public Collection<MentionRelation> getRelations() {
            if (_relations == null) {
                return Collections.emptyList();
            }
            return _relations.stream().map(this::createRelation).collect(Collectors.toList());
        }

        private MentionRelation createRelation(final HalfRelation vector) {
            final MentionRelation relation = new MentionRelation();
            relation.setSourceId(getId());
            relation.setTargetId(vector.getTargetId());
            relation.setType(vector.getRelationType());
            return relation;
        }

        public Collection<String> getCorefs() {
            if (_corefs == null) {
                return Collections.emptyList();
            }
            return _corefs;
        }

        static private class HalfRelation {
            final private String _targetId;
            final private String _relationType;

            public HalfRelation(final String targetId, final String relationType) {
                _targetId = targetId;
                _relationType = relationType;
            }

            public String getTargetId() {
                return _targetId;
            }

            public String getRelationType() {
                return _relationType;
            }
        }
    }


    private void populateTextProvenancesAndMentionedTerms(List<NeoplasmSummary> cancers,
                                                          String factId,
                                                          FactInfoAndGroupedTextProvenances factInfoAndGroupedTextProvenances,
                                                          List<NewMentionedTerm> mentionedTerms) {
        for (NeoplasmSummary cancer : Objects.requireNonNull(cancers)) {
            populateTextProvenancesAndMentionedTermsFromAttributes(cancer.getAttributes(), factId, factInfoAndGroupedTextProvenances, mentionedTerms);
            populateTextProvenancesAndMentionedTermsFromSubSummaries(cancer.getSubSummaries(), factId, factInfoAndGroupedTextProvenances, mentionedTerms);
        }
    }


    private void populateTextProvenancesAndMentionedTermsFromAttributes(List<NeoplasmAttribute> facts,
                                                                        String factId,
                                                                        FactInfoAndGroupedTextProvenances factInfoAndGroupedTextProvenances,
                                                                        List<NewMentionedTerm> mentionedTerms) {
        for (NeoplasmAttribute fact : facts) {
            if (fact.getId().equalsIgnoreCase(factId)) {
                //use direct evidence to build the "mention" data structure
                NewFactInfo factInfo = new NewFactInfo();
                factInfo.setName(fact.getName());
                factInfo.setId(fact.getId());
                factInfo.setValue(fact.getValue());
                factInfo.setPrettyName(DataUtil.getRelationPrettyName(fact.getClassUri()));
                factInfoAndGroupedTextProvenances.setSourceFact(factInfo);
                for (Mention mention : fact.getDirectEvidence()) {
                    NewMentionedTerm mentionedTerm = new NewMentionedTerm();
                    mentionedTerm.setTerm(mention.getClassUri());
                    mentionedTerm.setReportId(mention.getNoteId());
                    mentionedTerm.setReportType(mention.getNoteType());
                    mentionedTerm.setReportName(mention.getNoteId());
                    mentionedTerm.setBegin(mention.getBegin());
                    mentionedTerm.setEnd(mention.getEnd());
                    mentionedTerms.add(mentionedTerm);
                }
            }
        }
    }

    private void populateTextProvenancesAndMentionedTermsFromSubSummaries(List<NeoplasmSummary> subSummaries,
                                                                          String factId,
                                                                          FactInfoAndGroupedTextProvenances factInfoAndGroupedTextProvenances,
                                                                          List<NewMentionedTerm> mentionedTerms) {
        if (subSummaries != null) {
            for (NeoplasmSummary summary : Objects.requireNonNull(subSummaries)) {
                populateTextProvenancesAndMentionedTermsFromAttributes(summary.getAttributes(), factId, factInfoAndGroupedTextProvenances, mentionedTerms);
                //recursive call because subSummaries can have subSummaries
                populateTextProvenancesAndMentionedTermsFromSubSummaries(summary.getSubSummaries(), factId, factInfoAndGroupedTextProvenances, mentionedTerms);
            }
        }
    }

    // TODO - Create a bean in dphe-neo4j...node package that contains "Fact" summary information.
    // TODO - There is a node there  ... "Concept" or something.
    // TODO -  A Fact is a ConceptAggregate node from the graph.  It is not the same as something custom like Stage -
    //  even though Stage is a "fact"
    // TODO - Modify this method to populate the bean and gson it up as the return.
    // TODO - move to ReadFunctions class.

    public FactInfoAndGroupedTextProvenances getFact(GraphDatabaseService graphDb, Log
            log, @Name("patientId") String patientId, @Name("factId") String factId) {
        FactInfoAndGroupedTextProvenances factInfoAndGroupedTextProvenances = new FactInfoAndGroupedTextProvenances();
        List<NewMentionedTerm> mentionedTerms = new ArrayList<>();
        factInfoAndGroupedTextProvenances.setMentionedTerms(mentionedTerms);
        populateTextProvenancesAndMentionedTerms(getCancers(graphDb, log, patientId), factId, factInfoAndGroupedTextProvenances, mentionedTerms);
        return factInfoAndGroupedTextProvenances;
    }

    public static void main(String[] args) {
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("test"));
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("test"));
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("test"));
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("23"));
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("Patient 8"));
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("Patient 28"));
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("Patient 8"));
        System.out.println(NodeReader.getStructuredPatientDataForPatientId("fake_patient7"));

    }
}
