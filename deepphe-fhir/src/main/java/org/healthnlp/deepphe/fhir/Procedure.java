package org.healthnlp.deepphe.fhir;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;

import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.DateType;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Period;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.model.Resource;

public class Procedure extends org.hl7.fhir.instance.model.Procedure implements Element {
   private Report composition;
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;

   public String getDisplayText() {
      return getCode().getText();
   }

   public String getResourceIdentifier() {
      return FHIRUtils.getIdentifier( getIdentifier() );
   }

   public String toString() {
      return getDisplayText();
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer();
      st.append( getClass().getSimpleName() + ":\t" + getDisplayText() );
      for ( CodeableConcept l : getBodySite() ) {
         st.append( " | location: " + l.getText() );
      }
      // add text provenance
      st.append( " | text: " + FHIRUtils.getMentions( this ) );
      // add extendsions
      st.append( FHIRUtils.getExtensionsAsString( this ) );

      return st.toString();
   }

   public Resource getResource() {
      return this;
   }

   public void save( File dir ) throws Exception {
      FHIRUtils.saveFHIR( this, getResourceIdentifier(), dir );

   }


   public Report getComposition() {
      return composition;
   }

   /**
    * assign report instance and add appropriate information from there
    */
   public void setComposition( Report r ) {
      composition = r;
      Patient p = r.getPatient();
      if ( p != null ) {
         setSubject( FHIRUtils.getResourceReference( p ) );
         setSubjectTarget( p );
      }
      // set date
      Date d = r.getDate();
      if ( d != null ) {
         setPerformed( new DateType( d ) );
      }
   }

   public URI getConceptURI() {
      return FHIRUtils.getConceptURI( getCode() );
   }

   public void copy( Resource r ) {
      org.hl7.fhir.instance.model.Procedure p = (org.hl7.fhir.instance.model.Procedure) r;
      identifier = new ArrayList();
      for ( Identifier i : p.getIdentifier() )
         identifier.add( i.copy() );
      subject = p.getSubject();
      code = p.getCode();
      bodySite = new ArrayList();
      for ( CodeableConcept i : p.getBodySite() )
         bodySite.add( i.copy() );
      performer = new ArrayList();
      for ( ProcedurePerformerComponent i : p.getPerformer() )
         performer.add( i.copy() );
      performed = p.getPerformed();
      encounter = p.getEncounter();
      outcome = p.getOutcome();
      report = new ArrayList();
      for ( Reference i : p.getReport() )
         report.add( i.copy() );
      complication = new ArrayList();
      for ( CodeableConcept i : p.getComplication() )
         complication.add( i.copy() );
      followUp = p.getFollowUp();
      notes = p.getNotes();
      extension = new ArrayList<Extension>();
      for ( Extension e : p.getExtension() )
         extension.add( e );
   }

   private void writeObject( ObjectOutputStream stream ) throws IOException {
      System.out.println( "WTF: " + getClass().getName() );
      stream.defaultWriteObject();
   }

   private void readObject( ObjectInputStream stream ) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
   }

   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( String annotationType ) {
      this.annotationType = annotationType;
   }

}
