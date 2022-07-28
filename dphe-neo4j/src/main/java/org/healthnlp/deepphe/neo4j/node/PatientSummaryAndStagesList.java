package org.healthnlp.deepphe.neo4j.node;

import java.util.ArrayList;
import java.util.List;

public class PatientSummaryAndStagesList {
    public List<PatientInfoAndStages> getPatientSummaryAndStages() {
        if (patientSummaryAndStages == null) {
            patientSummaryAndStages = new ArrayList<PatientInfoAndStages>();
        }
        return patientSummaryAndStages;
    }

    public void setPatientSummaryAndStages(List<PatientInfoAndStages> patientSummaryAndStages) {
        this.patientSummaryAndStages = patientSummaryAndStages;
    }

    List<PatientInfoAndStages> patientSummaryAndStages;
}
