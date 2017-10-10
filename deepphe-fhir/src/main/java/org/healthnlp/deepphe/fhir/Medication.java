package org.healthnlp.deepphe.fhir;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Resource;


public class Medication extends org.hl7.fhir.instance.model.Medication implements Element {
   private Report report;
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;

   public String getDisplayText() {
      return getCode().getText();
   }

   public String getResourceIdentifier() {
      return FHIRUtils.createResourceIdentifier( this );
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer();
      st.append( "Medication:\t" + getDisplayText() );
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
      return report;
   }

   /**
    * assign report instance and add appropriate information from there
    */
   public void setComposition( Report r ) {
      report = r;
   }


   public void save( File dir ) throws Exception {
      FHIRUtils.saveFHIR( this, getResourceIdentifier(), dir );
   }

   public void copy( Resource r ) {
      org.hl7.fhir.instance.model.Medication p = (org.hl7.fhir.instance.model.Medication) r;
      code = p.getCode();
      isBrand = p.getIsBrandElement();
      manufacturer = p.getManufacturer();
      product = p.getProduct();
      package_ = p.getPackage();
      extension = new ArrayList<Extension>();
      for ( Extension e : p.getExtension() )
         extension.add( e );

   }

   public String toString() {
      return getDisplayText();
   }

   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( String annotationType ) {
      this.annotationType = annotationType;
   }

}
