package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.util.FHIRConstants.HAS_METHOD;

public class ValueFact extends Fact implements MethodOwner {

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   static public final double NO_VALUE = Double.NaN;

   private String unit;
   private double[] values;
   private Fact unitFact;
   private ValueFact _normalizedFact;
   private Fact method;


   public ValueFact( final ConceptInstance conceptInstance ) {
      super( conceptInstance, FHIRConstants.QUANTITY );
   }

   public ValueFact( final ConceptInstance conceptInstance, final String type ) {
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
   public Fact getMethod() {
      return method;
   }

   @Override
   public void setMethod( Fact method ) {
      this.method = method;
   }

   /**
    * @return a url from the ontology representing the unit
    */
   public String getUnit() {
      return unit;
   }

   /**
    * @param unit a url from the ontology representing the unit
    */
   public void setUnit( final String unit ) {
      this.unit = unit;
   }

   public double getValue() {
      return values != null && values.length > 0 ? values[ 0 ] : NO_VALUE;
   }

   public double[] getValues() {
      return values;
   }

   public String getValueString() {
      return Arrays.stream( values )
                   .mapToObj( Double::toString )
                   .collect( Collectors.joining( "x" ) );
   }

   public void setUnitFact( final Fact fact ) {
      unitFact = fact;
   }

   public Fact getUnitFact() {
      if ( unitFact == null && unit != null ) {
         unitFact = new Fact( getConceptInstance() );
         unitFact.setName( unit );
         unitFact.setLabel( getConceptInstance().getUri() );
         unitFact.setIdentifier( unit );
      }
      return unitFact;
   }


   public ValueFact getNormalizedFact() {
      return _normalizedFact;
   }

   public void setNormalizedFact( final ValueFact normalizedFact ) {
      _normalizedFact = normalizedFact;
   }

   private static String toString( final double number ) {
      if ( number == (long)number ) {
         return String.format( "%d", (long)number );
      } else {
         return String.format( "%s", number );
      }
   }

   public void setValue( final double value ) {
      this.values = new double[]{ value };
   }

   public void setValues( final double[] value ) {
      this.values = value;
   }

   /**
    *
    * @param property -
    * @return a value for this fact for a given property
    */
   public Fact getValue( final String property ) {
      switch ( property ) {
         case HAS_METHOD:
            return getMethod();
      }
      return this;
   }

   public void append( final Fact fact ) {
      if ( !ValueFact.class.isInstance( fact ) ) {
         return;
      }
      super.append( fact );
      final ValueFact otherFact = (ValueFact)fact;
      setOrAppendMethod( otherFact.getMethod() );
   }


   public String getSummaryText() {
      return getValue() + " " + getUnit() + " " + getMethodSnippet();
   }

   public List<Fact> getContainedFacts() {
      final List<Fact> facts = new ArrayList<>();
      if ( method != null ) {
         addContainedFact( facts, method );
      }
      return facts;
   }

   public String getInfo() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "name: " )
        .append( getName() )
        .append( "|" );
      sb.append( "value: " )
        .append( getValueString() )
        .append( "|" );
      sb.append( "unit: " )
        .append( getUnit() )
        .append( "|" );
      sb.append( "uri: " )
        .append( getUri() )
        .append( "|" );
      sb.append( "category: " )
        .append( getCategory() )
        .append( "|" );
      sb.append( "type: " )
        .append( getType() )
        .append( "|" );
      sb.append( "id: " )
        .append( getId() )
        .append( "|" );
      sb.append( "patient id: " )
        .append( getPatientIdentifier() )
        .append( "|" );
      sb.append( "document id: " )
        .append( getDocumentIdentifier() )
        .append( "|" );
      sb.append( "document type: " )
        .append( getDocumentType() )
        .append( "|" );
      sb.append( "container ids: " )
        .append( getContainerIdentifier() )
        .append( "|" );
      sb.append( "ancestors: " )
        .append( getAncestors() )
        .append( "\n" );
      return sb.toString();
   }

   /**
    * @return "Quantity"
    */
   protected String createDefaultType() {
      return FHIRConstants.QUANTITY;
   }

   protected String createDefaultLabel() {
		try {
	      final String unit = getUnit();
	      return getValueString() + ( unit == null ? "" : " " + unit );
		} catch (Exception e){
			return super.createDefaultLabel();
		}
   }

   protected String createDefaultCategory() {
      return FHIRConstants.HAS_NUM_VALUE;
   }

   protected String createDefaultId() {
      if ( getType() == null ) {
         createDefaultType();
      }
      final String docId = getDocumentIdentifier();
      final String unit = getUnit();
      return ( docId == null ? "" : docId + '_' )
            + getType().toUpperCase() + '_'
            + getValueString() + ( unit == null ? "" : unit ) + '_'
            + hashCode();
   }

}