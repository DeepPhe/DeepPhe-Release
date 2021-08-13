package org.healthnlp.deepphe.neo4j.node;

import java.util.ArrayList;
import java.util.List;

public class PatientSummaryAndStagesList {
    public List getPatientSummaryAndStages() {
        if (patientSummaryAndStages == null) {
            patientSummaryAndStages = new ArrayList();
        }
        return patientSummaryAndStages;
    }

    public void setPatientSummaryAndStages(List patientSummaryAndStages) {
        this.patientSummaryAndStages = patientSummaryAndStages;
    }

    List<PatientInfoAndStages> patientSummaryAndStages;
}
