package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class GuiPatientSummary {

        PatientInfo patientInfo;
        List<NewReport> reportData;


        public PatientInfo getPatientInfo() {
            return patientInfo;
        }

        public void setPatientInfo(PatientInfo patientInfo) {
            this.patientInfo = patientInfo;
        }

        public List<NewReport> getReportData() {
            return reportData;
        }

        public void setReportData(List<NewReport> reportData) {
            this.reportData = reportData;
        }



}

