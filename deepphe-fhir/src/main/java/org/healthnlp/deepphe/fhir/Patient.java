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
import org.hl7.fhir.instance.model.Address;
import org.hl7.fhir.instance.model.Attachment;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.ContactPoint;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.HumanName;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.model.Resource;


public class Patient extends org.hl7.fhir.instance.model.Patient implements Element {
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;
   //	private int yearsOld;
   private Report doc;

   public Patient() {
      setActive( true );
      //FHIRUtils.createIdentifier(addIdentifier(),this);
   }

   /**
    * set patient name
    *
    * @param name
    */
   public void setPatientName( String name ) {
      HumanName hn = addName();
      if ( name.contains( "," ) ) {
         String[] p = name.split( "," );
         hn.addFamily( p[ 0 ].trim() );
         if ( p.length > 1 ) {
            hn.addGiven( p[ p.length - 1 ].trim() );
         }
      } else if ( name.contains( " " ) ) {
         String[] p = name.split( "\\s+" );
         hn.addGiven( p[ 0 ].trim() );
         if ( p.length > 1 ) {
            hn.addFamily( p[ p.length - 1 ].trim() );
         }
      } else {
         hn.addFamily( name );
      }
      //String id = getClass().getSimpleName().toUpperCase()+"_"+name.replaceAll("\\W+","_");
      FHIRUtils.createIdentifier( addIdentifier(), this );
   }

   /**
    * get a simple name for a patient
    *
    * @return
    */
   public String getPatientName() {
      for ( HumanName n : getName() ) {
         String f = (!n.getGiven().isEmpty()) ? n.getGiven().get( 0 ).getValue() : "";
         String l = (!n.getFamily().isEmpty()) ? n.getFamily().get( 0 ).getValue() : "";
         return (f + " " + l).trim();
      }
      return null;
   }

   public String getGivenName() {
      for ( HumanName n : getName() ) {
         return (!n.getGiven().isEmpty()) ? n.getGiven().get( 0 ).getValue() : "";
      }
      return "";
   }

   public String getFamilyName() {
      for ( HumanName n : getName() ) {
         return (!n.getFamily().isEmpty()) ? n.getFamily().get( 0 ).getValue() : "";
      }
      return "";
   }


   public Reference getReference() {
      return getReference( new Reference() );
   }

   public Reference getReference( Reference r ) {
      r.setDisplay( getPatientName() );
      r.setReference( getResourceIdentifier() );
      return r;
   }

   public void setCurrentDate( Date currentDate ) {
      // derive birhdate if available
//		if(yearsOld > 0){
//			Date bday = new Date(currentDate.getTime()-FHIRUtils.MILLISECONDS_IN_YEAR* yearsOld);
//			setBirthDate(bday);
//		}
   }


   public String getDisplayText() {
      return getPatientName();
   }

   public String getResourceIdentifier() {
      return FHIRUtils.getIdentifier( getIdentifier() );
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer();
      st.append( "Patient:\t" + getDisplayText() );
      if ( getGender() != null )
         st.append( " | gender: " + getGender().getDisplay() );

      return st.toString();
   }

   public Resource getResource() {
      return this;
   }

   /**
    * copy data from an existing patient object
    *
    * @param r
    */
   public void copy( Resource r ) {
      org.hl7.fhir.instance.model.Patient target = (org.hl7.fhir.instance.model.Patient) r;
      Patient dst = this;
      dst.identifier = new ArrayList();
      for ( Identifier i : target.getIdentifier() )
         dst.identifier.add( i.copy() );
      dst.name = new ArrayList();
      for ( HumanName i : target.getName() )
         dst.name.add( i.copy() );
      dst.telecom = new ArrayList();
      for ( ContactPoint i : target.getTelecom() )
         dst.telecom.add( i.copy() );
      dst.gender = ((target.getGender() == null) ? null : target.getGenderElement().copy());
      dst.birthDate = ((target.getBirthDate() == null) ? null : target.getBirthDateElement().copy());
      dst.deceased = ((target.getDeceased() == null) ? null : target.getDeceased().copy());
      dst.address = new ArrayList();
      for ( Address i : target.getAddress() )
         dst.address.add( i.copy() );
      dst.maritalStatus = ((target.getMaritalStatus() == null) ? null : target.getMaritalStatus().copy());
      dst.multipleBirth = ((target.getMultipleBirth() == null) ? null : target.getMultipleBirth().copy());
      dst.photo = new ArrayList();
      for ( Attachment i : target.getPhoto() )
         dst.photo.add( i.copy() );
      dst.contact = new ArrayList();
      for ( ContactComponent i : target.getContact() )
         dst.contact.add( i.copy() );
      dst.animal = ((target.getAnimal() == null) ? null : target.getAnimal().copy());
      dst.communication = new ArrayList();
      for ( PatientCommunicationComponent i : target.getCommunication() )
         dst.communication.add( i.copy() );
      dst.careProvider = new ArrayList();
      for ( Reference i : target.getCareProvider() )
         dst.careProvider.add( i.copy() );
      dst.managingOrganization = ((target.getManagingOrganization() == null) ? null : target.getManagingOrganization().copy());
      dst.link = new ArrayList();
      for ( PatientLinkComponent i : target.getLink() )
         dst.link.add( i.copy() );
      dst.active = target.getActiveElement();
      dst.extension = new ArrayList<Extension>();
      for ( Extension e : target.getExtension() )
         dst.extension.add( e );
   }

   public void save( File dir ) throws Exception {
      FHIRUtils.saveFHIR( this, getResourceIdentifier(), dir );
   }

   public Report getComposition() {
      return doc;
   }

   public void setComposition( Report r ) {
      doc = r;
   }

   public String toString() {
      return getDisplayText();
   }

//	public int getAge(){
//		return yearsOld;
//	}


   public URI getConceptURI() {
      return URI.create( FHIRUtils.CANCER_URL + "#" + FHIRUtils.PATIENT );
   }

   private void writeObject( ObjectOutputStream stream ) throws IOException {
      stream.defaultWriteObject();
   }

   private void readObject( ObjectInputStream stream ) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
   }

   public CodeableConcept getCode() {
      return FHIRUtils.getCodeableConcept( getConceptURI() );
   }

   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( String annotationType ) {
      this.annotationType = annotationType;
   }

}
