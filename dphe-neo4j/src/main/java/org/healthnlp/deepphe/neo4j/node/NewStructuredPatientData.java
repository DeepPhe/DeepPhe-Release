package org.healthnlp.deepphe.neo4j.node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NewStructuredPatientData {
    String patientId;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    String firstname;
    String lastname;
    String birthdate;
    String gender;
    String firstEncounterDate;
    String lastEncounterDate;
    String[] ICD9;
    String[] ICD10;

    @Override
    public String toString() {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson(this);
    }

    public String[] getICD9() {
        return ICD9;
    }

    public void setICD9(String[] ICD9) {
        this.ICD9 = ICD9;
    }

    public String[] getICD10() {
        return ICD10;
    }

    public void setICD10(String[] ICD10) {
        this.ICD10 = ICD10;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getFirstEncounterDate() {
        return firstEncounterDate;
    }

    public void setFirstEncounterDate(String firstEncounterDate) {
        this.firstEncounterDate = firstEncounterDate;
    }

    public String getLastEncounterDate() {
        return lastEncounterDate;
    }

    public void setLastEncounterDate(String lastEncounterDate) {
        this.lastEncounterDate = lastEncounterDate;
    }


}
