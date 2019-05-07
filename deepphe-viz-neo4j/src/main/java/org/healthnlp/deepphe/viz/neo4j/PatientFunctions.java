package org.healthnlp.deepphe.viz.neo4j;

import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.healthnlp.deepphe.neo4j.UriConstants;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.lifecycle.LifecycleException;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/6/2018
 */
public class PatientFunctions {

    static private final String NOTE_DATE_VIZ = DataUtil.adjustPropertyName( NOTE_DATE );
    static private final String NOTE_TYPE_VIZ = DataUtil.adjustPropertyName( NOTE_TYPE );
    static private final String NOTE_NAME_VIZ = DataUtil.adjustPropertyName( NOTE_NAME );
    static private final String NOTE_EPISODE_VIZ = DataUtil.adjustPropertyName( NOTE_EPISODE );
    static private final String NOTE_TEXT_VIZ = DataUtil.adjustPropertyName( NOTE_TEXT );
    static private final String TEXT_SPAN_BEGIN_VIZ = DataUtil.adjustPropertyName( TEXT_SPAN_BEGIN );
    static private final String TEXT_SPAN_END_VIZ = DataUtil.adjustPropertyName( TEXT_SPAN_END );
    static private final String PATIENT_NAME_VIZ = DataUtil.adjustPropertyName( PATIENT_NAME );
    static private final String PATIENT_BIRTH_DATE_VIZ = DataUtil.adjustPropertyName( PATIENT_BIRTH_DATE );
    static private final String PATIENT_FIRST_ENCOUNTER_VIZ = DataUtil.adjustPropertyName( PATIENT_FIRST_ENCOUNTER );
    static private final String PATIENT_LAST_ENCOUNTER_VIZ = DataUtil.adjustPropertyName( PATIENT_LAST_ENCOUNTER );
    static private final String VALUE_TEXT_VIZ = DataUtil.adjustPropertyName( VALUE_TEXT );

    // TODO can now use SUBJECT_HAS_FACT_RELATION to get all concept instances related to patient, not just those in cancer / tumor

    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService graphDb;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            COHORT DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////
    @UserFunction("deepphe.getCohortData")
    @Description("Returns a list of all patients (patient properties and stages).")
    public List<Map<String, Object>> getCohortData() {
        List<Map<String, Object>> patients = new ArrayList<>();

        try (Transaction tx = graphDb.beginTx()) {
            // DataUtil.getAllPatientNodes() is supposed to return all unique patients
            final Collection<Node> patientNodes = DataUtil.getAllPatientNodes(graphDb);
            for (Node patientNode : patientNodes) {
                // get the major stage values for the patient
                final Collection<String> stages = new HashSet<>();

                final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION);
                for (Node cancerNode : cancerNodes) {
                    SearchUtil.getOutRelatedNodes(graphDb, cancerNode, HAS_STAGE).stream()
                              .map( n -> DataUtil.getUri( graphDb, n ) )
                            .map(PatientFunctions::getPrettyStage)
                            .forEach(stages::add);
                }

                final Map<String, Object> patientProperties = createSharedPatientProperties(patientNode);
                // Also add stages for cohort
                patientProperties.put("stages", stages);

                // Add to the set, this doesn't allow duplicates
                patients.add(patientProperties);
            }
            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getCohortData()");
        }

        return patients;
    }

    static private final Collection<String> BIOMARKERS = Arrays.asList(
          HAS_ER_STATUS,
          HAS_PR_STATUS,
          HAS_HER2_STATUS );

    @UserFunction("deepphe.getBiomarkers")
    @Description("Returns biomarkers information for a given list of patient IDs.")
    public List<Map<String, Object>> getBiomarkers(@Name("patientIds") List<String> patientIds) {
        List<Map<String, Object>> patientsTumorInfo = new ArrayList<>();

        try (Transaction tx = graphDb.beginTx()) {
            for (String patientId : patientIds) {
                final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
                if (patientNode == null) {
                    continue;
                }
                final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION);
                for (Node cancerNode : cancerNodes) {
//                    final Collection<Node> tumorNodes = SearchUtil.getAllOutRelatedNodes(graphDb, cancerNode);
                    final Collection<Node> tumorNodes = SearchUtil.getOutRelatedNodes(graphDb, cancerNode, CANCER_HAS_TUMOR_RELATION);
                    for (Node tumorNode : tumorNodes) {
                        final String tumorId = DataUtil.objectToString(tumorNode.getProperty(NAME_KEY));
                        for ( String biomarker : BIOMARKERS ) {
                            for ( Relationship relation : tumorNode.getRelationships( RelationshipType.withName( biomarker ), Direction.OUTGOING ) ) {
                                final Map<String, Object> patientTumorInfo = new HashMap<>();

                                // Filter biomarkers with the "hasReceptorStatus" relation
                                // Relation removed, maybe a "has_Biomarker_Status" in the future, but until now use a list.
                                final Node targetNode = relation.getOtherNode( tumorNode );
                                final Node classNode = DataUtil.getInstanceClass( graphDb, targetNode );
                                final Map<String,Object> properties = targetNode.getAllProperties();
                                final Map<String, String> targetFact = new HashMap<>();
                                final String nameId = DataUtil.objectToString( properties.get( NAME_KEY ) );
                                targetFact.put( "id", nameId );
                                targetFact.put( "name", nameId );
                                targetFact.put( "prettyName", DataUtil.objectToString( classNode.getProperty( PREF_TEXT_KEY ) ) );
                                final String valueText = DataUtil.objectToString( properties.get( VALUE_TEXT_VIZ ) );
                                if ( !valueText.isEmpty() ) {
                                    targetFact.put( VALUE_TEXT_VIZ, valueText );
                                }
                                // Add patientId to the map
                                patientTumorInfo.put( "patientId", patientId );
                                patientTumorInfo.put( "tumorId", tumorId );
                                patientTumorInfo.put( "tumorFactRelation", biomarker );
                                patientTumorInfo.put( "relationPrettyName", DataUtil.getRelationPrettyName( biomarker ) );
                                patientTumorInfo.put( "tumorFact", targetFact );

                                // Add to the list
                                patientsTumorInfo.add( patientTumorInfo );
                            }
                        }
                    }
                }
            }
            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getBiomarkers() " + e.getMessage() );
        }

        return patientsTumorInfo;
    }

    @UserFunction("deepphe.getDiagnosis")
    @Description("Returns a list of diagnoses per patient for a given list of patient IDs.")
    public List<Map<String, Object>> getDiagnosis(@Name("patientIds") List<String> patientIds) {
        List<Map<String, Object>> patientDiagnosis = new ArrayList<>();
       final Map<String,String> diagnosisGroupNames = UriConstants.getDiagnosisGroupNames( graphDb );

        try (Transaction tx = graphDb.beginTx()) {
            for (String patientId : patientIds) {
                final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
                if (patientNode == null) {
                    continue;
                }
                final Set<String> diagnoses = new HashSet<>();

                final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION);
                for (Node cancerNode : cancerNodes) {
                    // Cancers no longer have diagnoses, they ARE the diagnoses
//                    SearchUtil.getOutRelatedNodes(graphDb, cancerNode, HAS_DIAGNOSIS).stream()
//                            .map(n -> DataUtil.getPreferredText(graphDb, n))
//                            .forEach(diagnoses::add);
//
//                    SearchUtil.getOutRelatedNodes(graphDb, cancerNode, CANCER_HAS_TUMOR_RELATION).stream()
//                            .map(t -> SearchUtil.getOutRelatedNodes(graphDb, t, HAS_DIAGNOSIS))
//                            .flatMap(Collection::stream)
//                            .map(n -> DataUtil.getPreferredText(graphDb, n))
//                            .forEach(diagnoses::add);
                    diagnoses.add( DataUtil.getPreferredText( graphDb, cancerNode ) );
                }

                final Map<String, Object> map = new HashMap<>();
                map.put("patientId", patientId);
                map.put("diagnosis", diagnoses);
                final Collection<String> diagnosisGroups
                      = diagnoses.stream()
                                 .map( d -> diagnosisGroupNames.getOrDefault( d, "Unknown" ) )
                                 .collect( Collectors.toSet() );
                map.put( "diagnosisGroups", diagnosisGroups );

                // Add to the list
                patientDiagnosis.add(map);
            }
            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getDiagnosis()");
        }

        return patientDiagnosis;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            INDIVIDUAL PATIENT DATA
    //
    /////////////////////////////////////////////////////////////////////////////////////////
    @UserFunction("deepphe.getPatientInfo")
    @Description("Returns patient properties as a map for a given pateint ID.")
    public Map<String, Object> getPatientInfo(@Name("patientId") String patientId) {
        Map<String, Object> patientInfoMap = new HashMap<>();

        try (Transaction tx = graphDb.beginTx()) {
            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
            if (patientNode == null) {
                tx.success();
                return patientInfoMap;
            }

            patientInfoMap = createSharedPatientProperties(patientNode);

            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getPatientInfo()");
        }

        return patientInfoMap;
    }

    @UserFunction("deepphe.getCancerAndTumorSummary")
    @Description("Returns patient cancer and tumor information for a given patient ID.")
    public List<Map<String, Object>> getCancerAndTumorSummary(@Name("patientId") String patientId) {
        List<Map<String, Object>> cancers = new ArrayList<>();

        try (Transaction tx = graphDb.beginTx()) {
            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);

            if (patientNode == null) {
                tx.success();
                return cancers;
            }

            final Collection<Node> cancerNodes = SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION);
            for (Node cancerNode : cancerNodes) {
                // Cancer summary
                Map<String, Object> cancer = new HashMap<>();

                final String cancerId = DataUtil.objectToString(cancerNode.getProperty(NAME_KEY));

                // Add to cancer map
                cancer.put("cancerId", cancerId);

                List<Map> cancerFacts = new ArrayList<>();

                for (Relationship relation : cancerNode.getRelationships( Direction.OUTGOING)) {
                    Map<String, Object> cancerFact = new HashMap<>();
                    Map<String, Object> cancerFactInfo = new HashMap<>();

                    final String cancerFactRelationName = relation.getType().name();
                    if ( cancerFactRelationName.equals( INSTANCE_OF )
                         || cancerFactRelationName.equals( CANCER_HAS_TUMOR )
                         || cancerFactRelationName.equals( FACT_HAS_TEXT_MENTION ) ) {
                        continue;
                    }

                    cancerFact.put( "relation", cancerFactRelationName );
                    cancerFact.put( "relationPrettyName", DataUtil.getRelationPrettyName( cancerFactRelationName ) );

                    final Node targetNode = relation.getOtherNode(cancerNode);

                    final Node classNode = DataUtil.getInstanceClass(graphDb, targetNode);
                    final String classId = DataUtil.objectToString(classNode.getProperty(NAME_KEY));

                    cancerFactInfo.put("id", DataUtil.objectToString(targetNode.getProperty(NAME_KEY)));
                    cancerFactInfo.put("name", classId);

                    if (HAS_STAGE.equals(cancerFactRelationName)) {
                        cancerFactInfo.put("prettyName", getPrettyStage(classId));
                    } else {
                        cancerFactInfo.put("prettyName", DataUtil.objectToString(classNode.getProperty( PREF_TEXT_KEY )));
                    }

                    // Add fact to cancerFact map
                    cancerFact.put("cancerFactInfo", cancerFactInfo);

                    // Add to list
                    cancerFacts.add(cancerFact);
                }

                // Add to cancer map
                cancer.put("cancerFacts", cancerFacts);

                // Tumor summary
                List<Map<String, Object>> tumors = new ArrayList<>();
                final Collection<Node> tumorNodes = SearchUtil.getOutRelatedNodes(graphDb, cancerNode, CANCER_HAS_TUMOR_RELATION);
                for (Node tumorNode : tumorNodes) {
                    Map<String, Object> tumor = new HashMap<>();

                    final String tumorId = DataUtil.objectToString(tumorNode.getProperty(NAME_KEY));

                    // Add tumorId
                    tumor.put("tumorId", tumorId);
                    tumor.put( HAS_TUMOR_TYPE, DataUtil.objectToString(tumorNode.getProperty( HAS_TUMOR_TYPE ) ) );


                    List<Map<String, Object>> tumorFacts = new ArrayList<>();

                    for (Relationship relation : tumorNode.getRelationships( Direction.OUTGOING)) {
                        Map<String, Object> tumorFact = new HashMap<>();
                        Map<String, Object> tumorFactInfo = new HashMap<>();

                        final String tumorFactRelationName = relation.getType().name();
                        if ( tumorFactRelationName.equals( INSTANCE_OF )
                             || tumorFactRelationName.equals( FACT_HAS_TEXT_MENTION ) ) {
                            continue;
                        }

                        tumorFact.put( "relation", tumorFactRelationName );
                        tumorFact.put( "relationPrettyName", DataUtil.getRelationPrettyName( tumorFactRelationName ) );

                        final Node targetNode = relation.getOtherNode(tumorNode);

                        final Node classNode = DataUtil.getInstanceClass(graphDb, targetNode);
                        final String classId = DataUtil.objectToString(classNode.getProperty(NAME_KEY));
                        tumorFactInfo.put("id", DataUtil.objectToString(targetNode.getProperty(NAME_KEY)));
                        tumorFactInfo.put("name", classId);
                        tumorFactInfo.put("prettyName", DataUtil.objectToString(classNode.getProperty( PREF_TEXT_KEY )));
                        // Add fact to tumorFact map
                        tumorFact.put("tumorFactInfo", tumorFactInfo);

                        // Add to list
                        tumorFacts.add(tumorFact);
                    }

                    // Add tumorId
                    tumor.put("tumorFacts", tumorFacts);

                    // Add to tumors list
                    tumors.add(tumor);
                }

                // Add to cancer map
                cancer.put("tumors", tumors);

                // Finally add to the cancers list
                cancers.add(cancer);
            }

            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getCancerAndTumorSummary() " + e.getMessage());
        }

        return cancers;
    }

    @UserFunction(name = "deepphe.getTimelineData")
    @Description("Returns the patient information and all the reports/notes for a given patient ID.")
    public Map<String, Object> getTimelineData(@Name("patientId") String patientId) {
        Map<String, Object> timelineData = new HashMap<>();

        try (Transaction tx = graphDb.beginTx()) {
            final Node patientNode = SearchUtil.getLabeledNode(graphDb, PATIENT_LABEL, patientId);
            if (patientNode == null) {
                tx.success();
                return timelineData;
            }

            final Map<String, Object> patientProperties = createSharedPatientProperties(patientNode);

            // Add to the timelineData map
            timelineData.put("patientInfo", patientProperties);

            // get the notes for the patient
            final Collection<Node> notes = SearchUtil.getOutRelatedNodes(graphDb, patientNode, SUBJECT_HAS_NOTE_RELATION);

            List<Map<String, String>> reportList = new ArrayList<>();

            // For each note, add a patient object
            for (Node note : notes) {
                final Map<String, String> report = new HashMap<>();

                // Report ID
                report.put("reportId", DataUtil.objectToString(note.getProperty(NAME_KEY)));
                // Report principal date
                report.put("reportDate", DataUtil.getReportDate(DataUtil.objectToString(note.getProperty( NOTE_DATE_VIZ ))));
                // Report title/name
                report.put("reportName", DataUtil.objectToString(note.getProperty( NOTE_NAME_VIZ )));

                // Report type
               report.put("reportType", DataUtil.objectToString(note.getProperty( NOTE_TYPE_VIZ )));
               // Report episode
                report.put("reportEpisode", DataUtil.objectToString(note.getProperty( NOTE_EPISODE_VIZ )));

                // Add to the reportList
                reportList.add(report);
            }

            // Add to the timelineData map
            timelineData.put("reports", reportList);

            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getTimelineData()");
        }

        return timelineData;
    }

    @UserFunction(name = "deepphe.getReport")
    @Description("Returns report text and all text mentions for a given report ID.")
    public Map<String, Object> getReport(@Name("reportId") String reportId) {
        Map<String, Object> reportData = new HashMap<>();

        try (Transaction tx = graphDb.beginTx()) {

            final Node noteNode = SearchUtil.getLabeledNode( graphDb, TEXT_DOCUMENT_LABEL, reportId );

            if (noteNode == null) {
                tx.success();
                return reportData;
            }

            final String noteText = DataUtil.objectToString(noteNode.getProperty( NOTE_TEXT_VIZ ));

            // Add to map
            reportData.put("reportText", noteText);

            final Collection<Node> mentionNodes = SearchUtil.getOutRelatedNodes(graphDb, noteNode, NOTE_HAS_TEXT_MENTION_RELATION);

            List<Map> mentionedTerms = new ArrayList<>();

            for (Node mentionNode : mentionNodes) {
                final int begin = DataUtil.objectToInt(mentionNode.getProperty( TEXT_SPAN_BEGIN_VIZ ));
                final int end = DataUtil.objectToInt(mentionNode.getProperty( TEXT_SPAN_END_VIZ ));

                if (begin >= 0 && end > begin && end <= noteText.length()) {
                    final Map<String, String> mentionedTerm = new HashMap<>();

                    mentionedTerm.put("term", noteText.substring(begin, end));
                    // Convert the int to String value to avoid the {"low": n, "high": 0} issue probably due to
                    // the javascript neo4j driver doesn't handle intergers in neo4j type system correctly - Joe
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

    @UserFunction("deepphe.getFact")
    @Description("Returns fact information as json for a given fact ID.")
    public Map<String, Object> getFact(@Name("patientId") String patientId, @Name("factId") String factId) {
        Map<String, Object> factData = new HashMap<>();

        try (Transaction tx = graphDb.beginTx()) {
            final Node factNode = SearchUtil.getObjectNode(graphDb, factId);
            if (factNode == null) {
                tx.success();
                return factData;
            }

            Map<String, String> sourceFact = new HashMap<>();

            final Node classNode = DataUtil.getInstanceClass(graphDb, factNode);
            final String factPrefText = DataUtil.objectToString(classNode.getProperty( PREF_TEXT_KEY ));
            final String factUri = DataUtil.objectToString(classNode.getProperty(NAME_KEY));

            sourceFact.put("id", factId);
            sourceFact.put("name", factUri);
            sourceFact.put("prettyName", factPrefText);

            // Add the source fact to the map
            factData.put("sourceFact", sourceFact);

            // A list of text mentions
            List<Map<String, String>> mentionedTerms = new ArrayList<>();

            // All text mention nodes from this fact node
            final Collection<Node> mentionNodes = SearchUtil.getOutRelatedNodes(graphDb, factNode, FACT_HAS_TEXT_MENTION_RELATION);
            for (Node mentionNode : mentionNodes) {

                // Each text mention node can only have one source report (note that mentions this term in a specific position)
                final Collection<Node> noteNodes = SearchUtil.getInRelatedNodes(graphDb, mentionNode, NOTE_HAS_TEXT_MENTION_RELATION);
                if (noteNodes.size() != 1) {
                    continue;
                }
                final Node noteNode = new ArrayList<>(noteNodes).get(0);
                final String noteText = DataUtil.objectToString(noteNode.getProperty( NOTE_TEXT_VIZ ));
                final int noteLength = noteText.length();
                final String noteType = DataUtil.objectToString(noteNode.getProperty( NOTE_TYPE_VIZ ));
                final String noteId = DataUtil.objectToString(noteNode.getProperty(NAME_KEY));
                final String noteName = DataUtil.objectToString(noteNode.getProperty( NOTE_NAME_VIZ ));

                String sourcePatientId = "";

                // Find the source patient node
                final Collection<Node> patientNodes = SearchUtil.getInRelatedNodes(graphDb, noteNode, SUBJECT_HAS_NOTE_RELATION);
                if (patientNodes.size() == 1) {
                    sourcePatientId = DataUtil.objectToString(new ArrayList<>(patientNodes).get(0).getProperty(NAME_KEY));
                }

                // Only care about text mentions for this patient
                // because a fact related text mention can belong to a different patient
                if (sourcePatientId.equals(patientId)) {
                    final int begin = DataUtil.objectToInt(mentionNode.getProperty( TEXT_SPAN_BEGIN_VIZ ));
                    final int end = DataUtil.objectToInt(mentionNode.getProperty( TEXT_SPAN_END_VIZ ));
                    if (begin >= 0 && end > begin && end <= noteLength) {
                        Map<String, String> mentionedTerm = new HashMap<>();
                        mentionedTerm.put("reportId", noteId);
                        mentionedTerm.put("reportName", noteName);
                        mentionedTerm.put("reportType", noteType);
                        mentionedTerm.put("term", noteText.substring(begin, end));
                        // Convert the int to String value to avoid the {"low": n, "high": 0} issue probably due to
                        // the javascript neo4j driver doesn't handle intergers in neo4j type system correctly - Joe
                        mentionedTerm.put("begin", String.valueOf(begin));
                        mentionedTerm.put("end", String.valueOf(end));

                        // Add to list
                        mentionedTerms.add(mentionedTerm);
                    }
                }
            }

            // Add the text mentions to the map
            factData.put("mentionedTerms", mentionedTerms);

            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getFact()");
        }

        return factData;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            API ONLY
    //
    /////////////////////////////////////////////////////////////////////////////////////////
    @UserFunction("deepphe.getAllPatients")
    @Description("Returns all the patient nodes in a list.")
    public List<Map<String, Object>> getAllPatients() {
        List<Map<String, Object>> patients = new LinkedList<>();

        try (Transaction tx = graphDb.beginTx()) {
            final Collection<Node> patientNodes = DataUtil.getAllPatientNodes(graphDb);
            for (Node patientNode : patientNodes) {
                Map<String, Object> patientInfo = createSharedPatientProperties(patientNode);
                patients.add(patientInfo);
            }
            tx.success();
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to call getAllPatients()");
        }

        return patients;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    //                            HELPER METHODS
    //
    /////////////////////////////////////////////////////////////////////////////////////////
    static private String getPatientEncounterAge(final String birthDate, final String encounterDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        int age = 0;

        if ((birthDate != null) && (encounterDate != null)) {
            age = Period.between(LocalDate.parse(birthDate, formatter), LocalDate.parse(encounterDate, formatter)).getYears();
        }

        // Convert the int to String value to avoid the {"low": n, "high": 0} issue probably due to
        // the javascript neo4j driver doesn't handle intergers in neo4j type system correctly - Joe
        return String.valueOf(age);
    }

    /**
     * Shared patient properties used in cohort and individual patient view
     *
     * @param patientNode -
     * @return -
     */
    static private Map<String, Object> createSharedPatientProperties(Node patientNode) {
        final Map<String, Object> sharedPatientProperties = new HashMap<>();
        // Currently the NAME_KEY works as patient ID - Joe
        sharedPatientProperties.put("patientId", DataUtil.objectToString(patientNode.getProperty(NAME_KEY)));
        sharedPatientProperties.put("patientName", DataUtil.objectToString(patientNode.getProperty( PATIENT_NAME_VIZ )));
        sharedPatientProperties.put("birthDate", DataUtil.objectToString(patientNode.getProperty( PATIENT_BIRTH_DATE_VIZ )));
        sharedPatientProperties.put("firstEncounterDate", DataUtil.objectToString(patientNode.getProperty( PATIENT_FIRST_ENCOUNTER_VIZ )));
        sharedPatientProperties.put("lastEncounterDate", DataUtil.objectToString(patientNode.getProperty( PATIENT_LAST_ENCOUNTER_VIZ )));
        sharedPatientProperties.put("firstEncounterAge", getPatientEncounterAge(DataUtil.objectToString(patientNode.getProperty( PATIENT_BIRTH_DATE_VIZ )), DataUtil.objectToString(patientNode.getProperty( PATIENT_FIRST_ENCOUNTER_VIZ ))));
        sharedPatientProperties.put("lastEncounterAge", getPatientEncounterAge(DataUtil.objectToString(patientNode.getProperty( PATIENT_BIRTH_DATE_VIZ )), DataUtil.objectToString(patientNode.getProperty( PATIENT_LAST_ENCOUNTER_VIZ ))));

        return sharedPatientProperties;
    }

    static private String getPrettyStage(final String name) {
        if ( name.isEmpty() || name.equals( MISSING_NODE_NAME ) ) {
            return "Stage Unknown";
        }
        if (name.length() == 7) {
            switch (name) {
                case "Stage_0":
                    return "Stage 0";
                case "Stage_1":
                    return "Stage I";
                case "Stage_2":
                    return "Stage II";
                case "Stage_3":
                    return "Stage III";
                case "Stage_4":
                    return "Stage IV";
                case "Stage_5":
                    return "Stage V";
            }
        }
        final String uri = name.substring(0, 8);
        switch (uri) {
            case "Stage_Un":
                return "Stage Unknown";
            case "Stage_0_":
                return "Stage 0";
            case "Stage_0i":
                return "Stage 0";
            case "Stage_0a":
                return "Stage 0";
            case "Stage_Is":
                return "Stage 0";
            case "Stage_1_":
                return "Stage I";
            case "Stage_1m":
                return "Stage I";
            case "Stage_1A":
                return "Stage IA";
            case "Stage_1B":
                return "Stage IB";
            case "Stage_1C":
                return "Stage IC";
            case "Stage_2_":
                return "Stage II";
            case "Stage_2A":
                return "Stage IIA";
            case "Stage_2B":
                return "Stage IIB";
            case "Stage_2C":
                return "Stage IIC";
            case "Stage_3_":
                return "Stage III";
            case "Stage_3A":
                return "Stage IIIA";
            case "Stage_3B":
                return "Stage IIIB";
            case "Stage_3C":
                return "Stage IIIC";
            case "Stage_4_":
                return "Stage IV";
            case "Stage_4A":
                return "Stage IVA";
            case "Stage_4B":
                return "Stage IVB";
            case "Stage_4C":
                return "Stage IVC";
            case "Stage_5_":
                return "Stage V";
        }
        return name;
    }

    public static void main(final String... args) {
        System.out.println("DB File: " + args[0]);
        final File graphDbFile = new File(args[0]);
        if (!graphDbFile.isDirectory()) {
            System.err.println("No Database exists at: " + graphDbFile.getAbsolutePath());
            System.exit(-1);
        }
        final PatientFunctions functions = new PatientFunctions();
        functions.graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabase(graphDbFile);
        if (!functions.graphDb.isAvailable(500)) {
            System.err.println("Could not initialize neo4j connection for: " + graphDbFile.getAbsolutePath());
            System.exit(-1);
        }
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                functions.graphDb.shutdown();
            } catch (LifecycleException | RotationTimeoutException multE) {
                // ignore
            }
        }));

        System.out.println("COHORT:");
        System.out.println(functions.getCohortData());
        final List<String> patientIds = new ArrayList<>();
        try (Transaction tx = functions.graphDb.beginTx()) {
            final Collection<Node> patientNodes = DataUtil.getAllPatientNodes(functions.graphDb);
            for (Node patientNode : patientNodes) {
                final String patientId = DataUtil.objectToString(patientNode.getProperty(NAME_KEY));

                patientIds.add(patientId);

                System.out.println("\nPATIENT_TIMELINE: " + patientId);
                System.out.println(functions.getTimelineData(patientId));
                System.out.println("\nPATIENT_INFO: " + patientId);
                System.out.println(functions.getPatientInfo(patientId));
                System.out.println("\nCANCER AND TUMOR SUMMARY: " + patientId);
                System.out.println(functions.getCancerAndTumorSummary(patientId));
            }

            System.out.println("\nPATIENTS BIOMARKERS:");
            System.out.println(functions.getBiomarkers(patientIds));
            System.out.println("\nPATIENTS DIAGNOSES:");
            System.out.println(functions.getDiagnosis(patientIds));

            tx.success();
        } catch (MultipleFoundException mfE) {
//         throw new RuntimeException( mfE );
        }
    }

}
