package org.healthnlp.deepphe.fhir;

import java.io.File;
import java.net.URI;

import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.BodySite;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Resource;


public class AnatomicalSite extends BodySite implements Element {
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;
   private Report doc;

   public String getDisplayText() {
      return getCode().getText();
   }

   public String getResourceIdentifier() {
      return FHIRUtils.getIdentifier( getIdentifier() );
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer();
      st.append( getClass().getSimpleName() + ":\t" + getDisplayText() );
      for ( CodeableConcept l : getModifier() ) {
         st.append( " | modifier: " + l.getText() );
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

   public URI getConceptURI() {
      return FHIRUtils.getConceptURI( getCode() );
   }

   public Report getComposition() {
      return doc;
   }

   public void setComposition( Report r ) {
      doc = r;
      Patient p = r.getPatient();
      if ( p != null ) {
         setPatient( FHIRUtils.getResourceReference( p ) );
         setPatientTarget( p );
      }
   }

   public void save( File dir ) throws Exception {
      FHIRUtils.saveFHIR( this, getResourceIdentifier(), dir );
   }

   public void copy( Resource r ) {
      BodySite bs = (BodySite) r;
      setCode( bs.getCode() );
      setDescription( bs.getDescription() );
      setLanguage( bs.getLanguage() );
      setPatient( bs.getPatient() );
      setPatientTarget( bs.getPatientTarget() );
      for ( CodeableConcept c : bs.getModifier() ) {
         addModifier( c );
      }
      for ( Extension ex : bs.getExtension() ) {
         addExtension( ex );
      }
      for ( Identifier id : bs.getIdentifier() ) {
         addIdentifier( id );
      }

   }

   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( String annotationType ) {
      this.annotationType = annotationType;
   }

   public String toString() {
      return getSummaryText();
   }
}
