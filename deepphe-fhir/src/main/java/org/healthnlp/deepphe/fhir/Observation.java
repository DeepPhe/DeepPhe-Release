package org.healthnlp.deepphe.fhir;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.healthnlp.deepphe.util.TextUtils;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.DecimalType;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Quantity;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.StringType;
import org.hl7.fhir.instance.model.Type;


/**
 * Observation object
 *
 * @author tseytlin
 */
public class Observation extends org.hl7.fhir.instance.model.Observation implements Element {
   private Report report;
   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;

   public Observation() {
      setStatus( ObservationStatus.FINAL );
      //setLanguage(FHIRUtils.DEFAULT_LANGUAGE); // we only care about English
   }

   public String getDisplayText() {
      return getCode().getText();
   }

   public String getResourceIdentifier() {
      return FHIRUtils.getIdentifier( getIdentifier() );
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer();
      st.append( getClass().getSimpleName() + ":\t" + getDisplayText() );
      st.append( " | value: " + getObservationValue() );
      if ( method != null )
         st.append( " | method: " + method.getText() );

      // add text provenance
      st.append( " | text: " + FHIRUtils.getMentions( this ) );
      // add extendsions
      st.append( FHIRUtils.getExtensionsAsString( this ) );
      return st.toString();

   }

   public Resource getResource() {
      return this;
   }

   public Report getComposition() {
      return report;
   }

   public void setComposition( Report r ) {
      report = r;
      // set patient
      Patient p = r.getPatient();
      if ( p != null ) {
         setSubject( FHIRUtils.getResourceReference( p ) );
         setSubjectTarget( p );
      }
      // set date
      Date d = r.getDate();
      if ( d != null ) {
         setIssued( d );
      }
   }

   public void setValue( String value, String unit ) {
      setValues( TextUtils.parseValues( value ), unit );
   }

   public void setValue( double value, String unit ) {
      Quantity q = new Quantity();
      q.setValue( new BigDecimal( value, MathContext.DECIMAL32 ) );
      q.setUnit( unit );
      setValue( q );
   }

   public void setValues( List<Double> values, String unit ) {
      // pick the largest value
      double value = -1;
      for ( double v : values ) {
         if ( v > value )
            value = v;
      }
      // setup quantity with the largest values
      Quantity q = new Quantity();
      q.setValue( new BigDecimal( value, MathContext.DECIMAL32 ) );
      q.setUnit( unit );

      // add all dimensions
      if ( values.size() > 1 ) {
         int i = 1;
         for ( double v : values ) {
            q.addExtension( FHIRUtils.createExtension( FHIRUtils.DIMENSION_URL + (i++), "" + v ) );
         }
      }


      setValue( q );
   }


   public String getObservationValue() {
      Type t = getValue();
      if ( t != null ) {
         if ( t instanceof StringType )
            return ((StringType) t).getValue();
         else if ( t instanceof DecimalType )
            return "" + ((DecimalType) t).getValue();
         else if ( t instanceof Quantity )
            return ((Quantity) t).getValue().doubleValue() + " " + ((Quantity) t).getUnit();
         else
            return t.toString();
      }
      if ( getInterpretation() != null ) {
         return getInterpretation().getText();
      }

      return null;
   }


   public void save( File dir ) throws Exception {
      FHIRUtils.saveFHIR( this, getResourceIdentifier(), dir );
   }

   public void copy( Resource r ) {
      //TODO:
      org.hl7.fhir.instance.model.Observation o = (org.hl7.fhir.instance.model.Observation) r;
      code = o.getCode();
      value = o.getValue();
      interpretation = o.getInterpretation();
      comments = o.getCommentsElement();
      //applies = o.getApplies();
      issued = o.getIssuedElement();
      status = o.getStatusElement();
      //reliability = o.getReliability();
      bodySite = o.getBodySite();
      method = o.getMethod();
      identifier = o.getIdentifier();
      subject = o.getSubject();
      specimen = o.getSpecimen();
      performer = o.getPerformer();
      //for (ResourceReference i : o.getPerformer())
      //	performer.add(i.copy());
      referenceRange = new ArrayList();
      for ( ObservationReferenceRangeComponent i : o.getReferenceRange() )
         referenceRange.add( i.copy() );
      related = new ArrayList();
      for ( ObservationRelatedComponent i : o.getRelated() )
         related.add( i.copy() );
      extension = new ArrayList<Extension>();
      for ( Extension e : o.getExtension() )
         extension.add( e );

   }

   public String toString() {
      return getDisplayText();
   }

   public URI getConceptURI() {
      return FHIRUtils.getConceptURI( getCode() );
   }

   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( String annotationType ) {
      this.annotationType = annotationType;
   }

}
