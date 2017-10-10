package org.healthnlp.deepphe.graph;

import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.CodeableConcept;

import java.net.URI;


public class Patient {

   private Long id;
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;
   private String patientName;
   private String givenName;
   private String familyName;

   public Long getId() {
      return id;
   }

   public void setId( Long id ) {
      this.id = id;
   }

   /**
    * set patient name
    *
    * @param name
    */
   public void setPatientName( String name ) {
      this.patientName = name;
   }


   public String getPatientName() {
      return patientName;
   }

   public String getGivenName() {
      return givenName;
   }

   public void setGivenName( String givenName ) {
      this.givenName = givenName;
   }

   public String getFamilyName() {
      return familyName;
   }

   public void setFamilyName( String familyName ) {
      this.familyName = familyName;
   }


   public String getDisplayText() {
      return getPatientName();
   }


   public URI getConceptURI() {
      return URI.create( FHIRUtils.CANCER_URL + "#" + FHIRUtils.PATIENT );
   }


   public CodeableConcept getCode() {
      return FHIRUtils.getCodeableConcept( getConceptURI() );
   }

//	public String getAnnotationType() {
//		return annotationType;
//	}
//
//	public void setAnnotationType(String annotationType) {
//		this.annotationType = annotationType;
//	}

}
