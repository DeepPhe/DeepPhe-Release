package org.healthnlp.deepphe.neo4j.node;

/**
 * @author SPF , chip-nlp
 * @since {4/27/2021}
 */
public class PatientDiagnosis {

   private String patientId;
   private String classUri;

   public String getPatientId() {
      return patientId;
   }

   public void setPatientId( final String patientId ) {
      this.patientId = patientId;
   }

   public String getClassUri() {
      return classUri;
   }

   public void setClassUri( final String classUri ) {
      this.classUri = classUri;
   }


}
