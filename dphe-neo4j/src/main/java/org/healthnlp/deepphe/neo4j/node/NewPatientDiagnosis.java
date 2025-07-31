package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class NewPatientDiagnosis {
    List<String> diagnosisGroups;
    List<String> diagnosis;
    String patientId;

    public List<String> getDiagnosisGroups() {
        return diagnosisGroups;
    }

    public void setDiagnosisGroups(List<String> diagnosisGroups) {
        this.diagnosisGroups = diagnosisGroups;
    }

    public List<String> getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(List<String> diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPatientID() {
        return patientId;
    }

    public void setPatientId(String patientID) {
        this.patientId = patientID;
    }
}
