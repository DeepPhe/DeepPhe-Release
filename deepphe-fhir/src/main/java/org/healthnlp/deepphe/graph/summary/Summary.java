package org.healthnlp.deepphe.graph.summary;

import org.healthnlp.deepphe.graph.Report;
import org.healthnlp.deepphe.graph.Patient;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.net.URI;


public abstract class Summary {

   private Long id;

   protected Report report;
   protected Patient patient;
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;
   private String summaryText;

   public abstract String getDisplayText();

   public abstract String getResourceIdentifier();

   public String getSummaryText() {
      return summaryText;
   }

   public void setSummaryText( String summaryText ) {
      this.summaryText = summaryText;
   }

   public abstract URI getConceptURI();

   public Report getReport() {
      return report;
   }

   public void setReport( Report r ) {
      if ( r == null )
         return;

      report = r;
      if ( r.getPatient() != null )
         setPatient( r.getPatient() );
   }

   public Long getId() {
      return id;
   }

   public void setId( Long id ) {
      this.id = id;
   }

   public Patient getPatient() {
      return patient;
   }

   public void setPatient( Patient patient ) {
      this.patient = patient;
   }


   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( String annotationType ) {
      this.annotationType = annotationType;
   }


}
