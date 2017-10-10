package org.healthnlp.deepphe.fhir.fact;

import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.ArrayList;
import java.util.List;

public class ObservationFact extends Fact {
   private Fact interpretation, method, value;
   private FactList bodySite;

   public ObservationFact() {
      setType( FHIRConstants.OBSERVATION );
   }

   public FactList getBodySite() {
      if ( bodySite == null ) {
         bodySite = new DefaultFactList();
         bodySite.addType( FHIRConstants.BODY_SITE );
      }
      return bodySite;
   }

   public void setBodySite( FactList bodySite ) {
      this.bodySite = bodySite;
   }

   public Fact getInterpretation() {
      return interpretation;
   }

   public void setInterpretation( Fact interpretation ) {
      this.interpretation = interpretation;
   }

   public Fact getMethod() {
      return method;
   }

   public void setMethod( Fact method ) {
      this.method = method;
   }

   public Fact getValue() {
      return value;
   }

   public void setValue( Fact value ) {
      this.value = value;
   }

   public String getSummaryText() {
      StringBuffer b = new StringBuffer( getLabel() );
      if ( value != null )
         b.append( " | value: " + value.getSummaryText() );
      if ( interpretation != null )
         b.append( " | interpretation: " + interpretation.getSummaryText() );
      if ( method != null )
         b.append( " | method: " + method.getSummaryText() );
      return b.toString();
   }

   public List<Fact> getContainedFacts() {
      List<Fact> facts = new ArrayList<Fact>();
      if ( method != null ) {
         addContainedFact( facts, method );
      }
      if ( interpretation != null ) {
         addContainedFact( facts, interpretation );
      }
      if ( value != null ) {
         addContainedFact( facts, value );
      }
      for ( Fact fact : getBodySite() ) {
         addContainedFact( facts, fact );
      }
      return facts;
   }

   /**
    * get a value for this fact for a given property
    *
    * @param property
    * @return
    */
   public Fact getValue( String property ) {
      if ( FHIRConstants.HAS_INTERPRETATION.equals( property ) )
         return getInterpretation();
      if ( FHIRConstants.HAS_NUM_VALUE.equals( property ) )
         return getValue();
      if ( FHIRConstants.HAS_METHOD.equals( property ) )
         return getMethod();
      return this;
   }

   public void append( Fact fact ) {
      if ( fact instanceof ObservationFact ) {
         super.append( fact );

         ObservationFact ofact = (ObservationFact) fact;
         //get body sites
         for ( Fact bf : ofact.getBodySite() ) {
            if ( !FactHelper.contains( getBodySite(), bf ) ) {
               getBodySite().add( bf );
            }
         }
         // set value
         if ( getValue() == null )
            setValue( ofact.getValue() );
         else if ( ofact.getValue() != null )
            getValue().append( ofact.getValue() );

         // set method
         if ( getMethod() == null )
            setMethod( ofact.getMethod() );
         else if ( ofact.getMethod() != null )
            getMethod().append( ofact.getMethod() );

         // set interpretation
         if ( getInterpretation() == null )
            setInterpretation( ofact.getInterpretation() );
         else if ( ofact.getInterpretation() != null )
            getInterpretation().append( ofact.getInterpretation() );
      }
   }
}
