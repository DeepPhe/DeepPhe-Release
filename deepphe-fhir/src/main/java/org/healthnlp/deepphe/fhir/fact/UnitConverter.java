package org.healthnlp.deepphe.fhir.fact;

public class UnitConverter {
   public static final String MM = "mm";
   public static final String CM = "cm";

   /**
    * normalize value fact to target units
    *
    * @param fact
    * @param targetUnits
    * @return
    */
   public static String getDefaultUnits( ObservationFact fact ) {
      //TODO: obviesly figure this out from ontology
      return MM;
   }

   /**
    * normalize value fact to target units
    *
    * @param fact
    * @param targetUnits
    * @return
    */
   public static ValueFact normalizeToUnits( ValueFact fact, ObservationFact ob ) {
      return normalizeToUnits( fact, getDefaultUnits( ob ) );
   }

   /**
    * normalize value fact to target units
    *
    * @param fact
    * @param targetUnits
    * @return
    */
   public static ValueFact normalizeToUnits( ValueFact fact, String targetUnits ) {
      double[] values = new double[ fact.getValues().length ];
      String units = fact.getUnit();
      // no need to convert if target is identical to what we have
      if ( targetUnits.equals( units ) )
         return fact;

      //TODO: implement a better conversion in the future
      for ( int i = 0; i < values.length; i++ ) {
         if ( MM.equals( targetUnits ) ) {
            if ( CM.equals( units ) ) {
               values[ i ] = fact.getValues()[ i ] * 10;
            }
         }
      }

      // constract value fact to return
      ValueFact valueFact = FactFactory.createValueFact( values, targetUnits );
      valueFact.setCategory( fact.getCategory() );
      return valueFact;
   }
}
