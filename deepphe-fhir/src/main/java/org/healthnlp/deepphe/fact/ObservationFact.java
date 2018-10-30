package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.ArrayList;
import java.util.List;

import static org.healthnlp.deepphe.util.FHIRConstants.*;

public class ObservationFact extends Fact implements ValueOwner, MethodOwner, InterpretationOwner, BodySiteOwner {

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private Fact interpretation, method, value;
   private FactList bodySite;

   public ObservationFact( final ConceptInstance conceptInstance ) {
      super( conceptInstance, FHIRConstants.OBSERVATION );
   }

   public ObservationFact( final ConceptInstance conceptInstance, final String type ) {
      super( conceptInstance, type );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected long createUniqueIdNum() {
      synchronized ( ID_NUM_LOCK ) {
         _ID_NUM++;
         return _ID_NUM;
      }
   }

   @Override
   public FactList getBodySite() {
      if ( bodySite == null ) {
         bodySite = new DefaultFactList();
         bodySite.addType( FHIRConstants.BODY_SITE );
      }
      return bodySite;
   }

   @Override
   public void setBodySite( final FactList bodySite ) {
      this.bodySite = bodySite;
   }

   @Override
   public Fact getInterpretation() {
      return interpretation;
   }

   @Override
   public void setInterpretation( final Fact interpretation ) {
      this.interpretation = interpretation;
   }

   @Override
   public Fact getMethod() {
      return method;
   }

   @Override
   public void setMethod( Fact method ) {
      this.method = method;
   }

   @Override
   public Fact getValue() {
      return value;
   }

   @Override
   public void setValue( final Fact value ) {
      this.value = value;
   }

   public String getSummaryText() {
      return getLabel() + getValueSnippet() + getInterpretationSnippet() + getMethodSnippet();
   }

   public List<Fact> getContainedFacts() {
      final List<Fact> facts = new ArrayList<>();
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
    * @param property -
    * @return a value for this fact for a given property
    */
   public Fact getValue( final String property ) {
      switch ( property ) {
         case HAS_INTERPRETATION:
            return getInterpretation();
         case HAS_NUM_VALUE:
            return getValue();
         case HAS_METHOD:
            return getMethod();
      }
      return this;
   }

   public void append( final Fact fact ) {
      if ( !ObservationFact.class.isInstance( fact ) ) {
         return;
      }
      super.append( fact );
      final ObservationFact otherFact = (ObservationFact)fact;
      appendBodySites( (ObservationFact)fact );
      setOrAppendValue( otherFact.getValue() );
      setOrAppendMethod( otherFact.getMethod() );
      setOrAppendInterpretation( otherFact.getInterpretation() );
   }


   /**
    * @return "Observation"
    */
   protected String createDefaultType() {
      return FHIRConstants.OBSERVATION;
   }

}
