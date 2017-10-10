package org.healthnlp.deepphe.fhir.fact;

import org.healthnlp.deepphe.util.FHIRConstants;

public class ValueFact extends Fact {
   public final static double NO_VALUE = -1;
   private String unit;
   private double[] values;
   private Fact unitFact;

   public ValueFact() {
      setType( FHIRConstants.QUANTITY );
   }

   public Fact getUnitFact() {
      if ( unitFact == null && unit != null ) {
         unitFact = new Fact();
         unitFact.setName( unit );
         unitFact.setLabel( unit );
         unitFact.setIdentifier( unit );
      }
      return unitFact;
   }

   public String getUnit() {
      return unit;
   }

   public void setUnit( String unit ) {
      this.unit = unit;
   }

   public double getValue() {
      return values != null && values.length > 0 ? values[ 0 ] : NO_VALUE;
   }

   public double[] getValues() {
      return values;
   }

   public String getValueString() {
      if ( values.length > 0 ) {
         StringBuffer sb = new StringBuffer( toString( values[ 0 ] ) );
         for ( int i = 1; i < values.length; i++ ) {
            sb.append( "x" + toString( values[ i ] ) );
         }
         return sb.toString();
      }
      return "";
   }

   private static String toString( double d ) {
      if ( d == (long) d )
         return String.format( "%d", (long) d );
      else
         return String.format( "%s", d );
   }

   public void setValue( double value ) {
      this.values = new double[]{ value };
   }

   public void setValues( double[] value ) {
      this.values = value;
   }


   public String getSummaryText() {
      StringBuffer b = new StringBuffer( getValueString() );
      if ( unit != null )
         b.append( " " + unit );
      return b.toString();
   }

   public String getInfo() {
      StringBuffer b = new StringBuffer();
      b.append( "name: " + getName() + "|" );
      b.append( "value: " + getValueString() + "|" );
      b.append( "unit: " + getUnit() + "|" );
      b.append( "uri: " + getUri() + "|" );
      b.append( "category: " + getCategory() + "|" );
      b.append( "type: " + getType() + "|" );
      b.append( "id: " + getIdentifier() + "|" );
      b.append( "patient id: " + getPatientIdentifier() + "|" );
      b.append( "document id: " + getDocumentIdentifier() + "|" );
      b.append( "document tyoe: " + getDocumentType() + "|" );
      b.append( "container ids: " + getContainerIdentifier() + "|" );
      b.append( "ancestors: " + getAncestors() + "\n" );
      return b.toString();
   }

}
