package org.healthnlp.deepphe.neo4j.node;

public class PatientInfo {
    private String patientId;
    private String patientName;
    private String birthDate;
    private String lastEncounterAge;
    private String firstEncounterAge;
    private String lastEncounterDate;
    private String firstEncounterDate;

    public PatientInfo() {

    }
    public PatientInfo(String patientId, String patientName, String birthDate, String lastEncounterAge, String firstEncounterAge, String lastEncounterDate, String firstEncounterDate, String gender) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.birthDate = birthDate;
        this.lastEncounterAge = lastEncounterAge;
        this.firstEncounterAge = firstEncounterAge;
        this.lastEncounterDate = lastEncounterDate;
        this.firstEncounterDate = firstEncounterDate;
        this.gender = gender;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    private String gender;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getLastEncounterAge() {
        return lastEncounterAge;
    }

    public void setLastEncounterAge(String lastEncounterAge) {
        this.lastEncounterAge = lastEncounterAge;
    }

    public String getFirstEncounterAge() {
        return firstEncounterAge;
    }

    public void setFirstEncounterAge(String firstEncounterAge) {
        this.firstEncounterAge = firstEncounterAge;
    }

    public String getLastEncounterDate() {
        return lastEncounterDate;
    }

    public void setLastEncounterDate(String lastEncounterDate) {
        this.lastEncounterDate = lastEncounterDate;
    }

    public String getFirstEncounterDate() {
        return firstEncounterDate;
    }

    public void setFirstEncounterDate(String firstEncounterDate) {
        this.firstEncounterDate = firstEncounterDate;
    }
}
