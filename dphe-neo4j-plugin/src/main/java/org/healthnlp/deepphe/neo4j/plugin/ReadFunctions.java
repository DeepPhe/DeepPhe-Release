package org.healthnlp.deepphe.neo4j.plugin;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.neo4j.reader.NodeReader;
import org.healthnlp.deepphe.neo4j.util.DataUtil;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;

/**
 * All functions return JSON
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/10/2020
 */
final public class ReadFunctions {
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService graphDb;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;


    // TODO make sure that the Patient bean has this information.
    // TODO get rid of this function.

    /**
     * Shared patient properties used in cohort and individual patient view
     *
     * @param patientNode -
     * @return -
     */
    static private PatientInfoAndStages createSharedPatientProperties(Log log, final Node patientNode) {
        String actualPatientId = DataUtil.objectToString(patientNode.getProperty(NAME_KEY));
        NewStructuredPatientData structuredPatientData = NodeReader.getInstance().getStructuredPatientDataForPatientId(actualPatientId);
        if (structuredPatientData != null) {
            try {
                PatientInfo patientInfo = NodeReader.populateNewRandomPatient(structuredPatientData);
                PatientInfoAndStages patientInfoAndStages = new PatientInfoAndStages(patientInfo);
               return patientInfoAndStages;

            } catch (ParseException e) {
                log.error("Error reading from structured patient data.  \n\tError: " + e.getMessage());
            }
        } else {
            log.error("Couldn't find patient ID " + actualPatientId + " in structured data.");
        }
        return null;
    }

    @UserFunction(name = "deepphe.getCohortData")
    @Description("Returns a list of all patients (patient properties and stages).")
    public String getCohortData() {
        List<PatientInfoAndStages> patientList = getPatientList();
        final Gson gson = new GsonBuilder().create();
        return gson.toJson(patientList);
    }

    @UserFunction("deepphe.getCancerAndTumorSummary")
    @Description("Returns patient cancer and tumor information for a given patient ID.")
    public String getCancerAndTumorSummary(@Name("patientId") String patientId) {
        NewCancerAndTumorSummary cancerAndTumorSummary = NodeReader.getInstance().getCancerAndTumorSummary(graphDb, log, patientId);
        final Gson gson = new GsonBuilder().create();
        return gson.toJson(cancerAndTumorSummary);
    }

    @UserFunction("deepphe.getFact")
    @Description("Returns patient cancer and tumor information for a given patient ID.")
    public String getFact(@Name("patientId") String patientId, @Name("factId") String factId) {
        FactInfoAndGroupedTextProvenances factInfoAndGroupedTextProvenances = NodeReader.getInstance().getFact(graphDb, log, patientId, factId);
        final Gson gson = new GsonBuilder().create();
        return gson.toJson(factInfoAndGroupedTextProvenances);
    }


    @UserFunction(name = "deepphe.getReport")
    @Description("Returns report text and all text mentions for a given report ID.")
    public Map<String, Object> getReport(@Name("reportId") String reportId) {
        final Map<String, Object> reportData = new HashMap<>();
        try (Transaction tx = graphDb.beginTx()) {
            final Node noteNode = SearchUtil.getLabeledNode(graphDb, TEXT_DOCUMENT_LABEL, reportId);
            if (noteNode == null) {
                tx.success();
                return reportData;
            }
            final String noteText = DataUtil.objectToString(noteNode.getProperty(NOTE_TEXT));
            // Add to map
            reportData.put("reportText", noteText);
            final Collection<Node> mentionNodes = SearchUtil.getOutRelatedNodes(graphDb, noteNode, NOTE_HAS_TEXT_MENTION_RELATION);
            final List<Map<String, String>> mentionedTerms = new ArrayList<>();
            for (Node mentionNode : mentionNodes) {
                final int begin = DataUtil.objectToInt(mentionNode.getProperty(TEXT_SPAN_BEGIN));
                final int end = DataUtil.objectToInt(mentionNode.getProperty(TEXT_SPAN_END));
                if (begin >= 0 && end > begin && end <= noteText.length()) {
                    final Map<String, String> mentionedTerm = new HashMap<>();
                    mentionedTerm.put("term", noteText.substring(begin, end));
                    // Convert the int to String value to avoid the {"low": n, "high": 0} issue probably due to
                    // the javascript neo4j driver doesn't handle integers in neo4j type system correctly - Joe
                    mentionedTerm.put("begin", String.valueOf(begin));
                    mentionedTerm.put("end", String.valueOf(end));
                    // Add to the mentionedTerms list
                    mentionedTerms.add(mentionedTerm);
                }
            }
            // Add to the map
            reportData.put("mentionedTerms", mentionedTerms);
            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getReport()");
        }
        return reportData;
    }

    @UserFunction(name = "deepphe.getTimelineData")
    @Description("Returns the patient information and all the reports/notes for a given patient ID.")
    public String getTimelineData(@Name("patientId") String patientId) {
        List<GuiPatientSummary> patientSummaries = NodeReader.getInstance().getPatientSummary(graphDb, log, patientId);
        if (patientSummaries == null) {
            return null;
        }
        for (GuiPatientSummary patientSummary : patientSummaries) {
            if (patientSummary.getPatientInfo().getPatientId().equals(patientId)) {
                final Gson gson = new GsonBuilder().create();
                return gson.toJson(patientSummary);
            }
        }
        return null;
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            PATIENT DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////

    protected String getNewPatientList(boolean includeStages) {
        final PatientSummaryAndStagesList patientSummaryAndStagesList = NodeReader.getInstance().patientSummaryAndStagesList(graphDb, log, includeStages);
        final Gson gson = new GsonBuilder().create();
        return gson.toJson(patientSummaryAndStagesList);
    }

//JDL 2021-07-28: IntelliJ reporting as unused.  If you see this it's probably safe to delete it.
//    public List<String> getAllPatientSummaries() {
//        final Gson gson = new GsonBuilder().create();
//        return NodeReader.getInstance().getPatientSummaries(graphDb, log)
//                .stream()
//                .map(gson::toJson)
//                .collect(Collectors.toList());
//    }

    private List<String> getStages(GraphDatabaseService graphDb, Node node) {
        List<String> stages = new ArrayList<>();
        Collection<Node> outRelatedNodes = SearchUtil.getOutRelatedNodes(graphDb, node, NEOPLASM_HAS_ATTRIBUTE);
        outRelatedNodes.addAll(SearchUtil.getOutRelatedNodes(graphDb, node, NEOPLASM_HAS_ATTRIBUTE_RELATION));

        final Object[] objects = outRelatedNodes.toArray();
        for (Object object : objects) {
            Node relatedNode = ((Node) object);

            if (relatedNode.hasProperty(ATTRIBUTE_NAME) && ((String) relatedNode.getProperty(ATTRIBUTE_NAME)).equalsIgnoreCase("stage")) {
                stages.add((String) relatedNode.getProperty(ATTRIBUTE_URI));
            }
        }
        return stages;
    }

    protected List<PatientInfoAndStages> getPatientList() {
        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());

        final List<PatientInfoAndStages> patients = new ArrayList<>();
        try (Transaction tx = graphDb.beginTx()) {
            // DataUtil.getAllPatientNodes() is supposed to return all unique patients
            final Collection<Node> patientNodes = DataUtil.getAllPatientNodes(graphDb);

            for (Node patientNode : patientNodes) {
                final PatientInfoAndStages patientProperties = createSharedPatientProperties(log, patientNode);

                    // get the major stage values for the patient
                    final Collection<String> stages = new HashSet<>();
                    final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode,
                            SUBJECT_HAS_CANCER_RELATION);
                    for (Node cancerNode : cancerNodes) {
                        // TODO: is this the right way to get stages?
                        stages.addAll(getStages(graphDb, cancerNode));

                    }
                    // Also add stages for cohort
                    patientProperties.setStages(new ArrayList<>(stages));

                // Add to the set, this doesn't allow duplicates
                patients.add(patientProperties);
            }
            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getPatientList()" + e.getMessage());
        }
        return patients;
    }

    /**
     * Fetches everything under a Patient node in the graph.
     *
     * @param patientId ID previously used to run the patient.
     * @return JSON containing all NLP-extracted and summarized information on a patient.
     */

    @UserFunction("deepphe.getPatientData")
    @Description("Fetches everything under a Patient node.")
    public String getPatientData(@Name("patientId") String patientId) {
        final Patient patient = NodeReader.getInstance().getPatient(graphDb, log, patientId);
        if (patient == null) {
            return "";
        }
        final Gson gson = new GsonBuilder().create();
        return gson.toJson(patient);
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            NOTE DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Fetches everything under a Note node in the graph.
     *
     * @param noteId ID previously used to run the note.
     * @return JSON containing all NLP-extracted information on a note.
     */
    @UserFunction("deepphe.getNoteData")
    @Description("Fetches everything under a Note node.")
    public String getNoteData(@Name("noteId") String noteId) {
        final Note note = NodeReader.getInstance().getNote(graphDb, log, noteId);
        if (note == null) {
            return "";
        }
        final Gson gson = new GsonBuilder().create();
        return gson.toJson(note);
    }

//JDL 2021-07-28: getDocuments might not be called by the visualizer
//   @UserFunction(name = "deepphe.getDocuments")
    @Description("Returns a list of documents for a given list of document Ids.")
    public List<String> getDocuments(@Name("documentIds") List<String> documentIds) {
        final Gson gson = new GsonBuilder().create();
        return documentIds.stream()
                .map(id -> NodeReader.getInstance().getNote(graphDb, log, id))
                .map(gson::toJson)
                .collect(Collectors.toList());
    }

    /**
     * Fetches everything under all Note nodes for a Patient in the graph.
     * This is similar to {@link #getPatientData(String)} but no patient summary data is included.
     *
     * @param patientId ID previously used to run the patient.
     * @return JSON containing all NLP-extracted information on a collection of notes.
     */
//   @UserFunction(name = "deepphe.getPatientDocuments")
    @Description("Returns all the documents for a given patient ID.")
    public List<String> getPatientDocuments(@Name("patientId") String patientId) {
        final Gson gson = new GsonBuilder().create();
        return NodeReader.getInstance().getPatient(graphDb, log, patientId)
                .getNotes()
                .stream()
                .map(gson::toJson)
                .collect(Collectors.toList());
    }

    @UserFunction("deepphe.getBiomarkers")
    @Description("Returns biomarkers information for a given list of patient IDs.")
    public String getBiomarkers(@Name("patientIds") List<String> patientIds) {
        final List<NewBiomarkerSummary> biomarkerSummaries = new ArrayList<>();
        //jdl important
        patientIds.stream()
                .map(id -> NodeReader.getInstance().getPatient(graphDb, log, id).getBiomarkers()).forEach(biomarkerSummaries::addAll);
        final Gson gson = new GsonBuilder().create();
        return gson.toJson(biomarkerSummaries);
    }

    @UserFunction("deepphe.getDiagnosis")
    @Description("Returns a list of diagnoses per patient for a given list of patient IDs.")
    public String getDiagnosis(@Name("patientIds") List<String> patientIds) {
        final List<NewPatientDiagnosis> patientDiagnoses = new ArrayList<>();
        patientIds.stream()
                .map(id -> NodeReader.getInstance().getPatient(graphDb, log, id).getDiagnoses()).forEach(patientDiagnoses::addAll);
        Gson gson = new GsonBuilder().create();
        return gson.toJson(patientDiagnoses);
    }

}
