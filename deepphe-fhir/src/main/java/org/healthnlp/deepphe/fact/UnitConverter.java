package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.healthnlp.deepphe.util.FHIRConstants;

import static org.apache.ctakes.cancer.uri.UriConstants.CENTIMETER;
import static org.apache.ctakes.cancer.uri.UriConstants.MILLIMETER;

final public class UnitConverter {


   static public final String MM = "mm";
   static public final String CM = "cm";

   private UnitConverter() {
   }


   @Deprecated
   static public String getDefaultUnits( final ObservationFact fact ) {
      return MM;
   }

   @Deprecated
   public static ValueFact normalizeToUnits( final ValueFact valueFact, final ObservationFact observationFact ) {
      return normalizeToUnits( valueFact, getDefaultUnits( observationFact ) );
   }

   @Deprecated
   public static ValueFact normalizeToUnits( final ValueFact valueFact, final String targetUnits ) {
      double[] values = new double[ valueFact.getValues().length ];
      final String units = valueFact.getUnit();
      // no need to convert if target is identical to what we have
      if ( targetUnits.equals( units ) )
         return valueFact;

      for ( int i = 0; i < values.length; i++ ) {
         if ( MM.equals( targetUnits ) ) {
            if ( CM.equals( units ) ) {
               values[ i ] = valueFact.getValues()[ i ] * 10;
            }
         }
      }

      // construct value fact to return
      final ValueFact newValueFact = FactFactory.createValueFact( valueFact, values, targetUnits );
      newValueFact.setCategory( valueFact.getCategory() );
      return newValueFact;
   }

   /**
    * @param valueFact fact with some values and a unit type
    * @return a new fact with the default unit type and the values of the original fact converted appropriately
    */
   static public ValueFact createNormalizedFact( final ValueFact valueFact ) {
      final String units = valueFact.getUnit();
      if ( units == null || units.isEmpty() ) {
         return valueFact;
      }
      final UnitInfo unitInfo = UnitInfo.getByText( units );
      if ( unitInfo == UnitInfo.UNKNOWN ) {
         return valueFact;
      }
      final Converter converter = unitInfo.getConverter();
      if ( converter == NullConverter.NULL_CONVERTER ) {
         return valueFact;
      }
      final UnitInfo newUnitInfo = UnitInfo.getByUrl( converter.getTargetUrl() );
      double[] newNumbers = new double[ valueFact.getValues().length ];
      for ( int i = 0; i < newNumbers.length; i++ ) {
         newNumbers[ i ] = converter.convert( valueFact.getValues()[ i ] );
      }
      final ValueFact newValueFact = new ValueFact( valueFact.getConceptInstance(), valueFact.getType() );
      newValueFact.setUri( valueFact.getUri() );
      newValueFact.setValues( newNumbers );
      newValueFact.setUnit( newUnitInfo.getPrefText() );
      newValueFact.setUnitFact( createUnitFact( newUnitInfo, valueFact ) );
      newValueFact.setIdentifier( valueFact.getId() + "_normalized" );
      newValueFact.autoFillDefaults();
      newValueFact.copyDocInfo( valueFact );
      return newValueFact;
   }

   static public Fact createUnitFact( final String unitText, final Fact valueFact ) {
      final UnitInfo unitInfo = UnitInfo.getByText( unitText );
      return createUnitFact( unitInfo, valueFact );
   }

   static public Fact createUnitFact( final UnitInfo unitInfo, final Fact valueFact ) {
      final Fact fact = new Fact( valueFact.getConceptInstance(), FHIRConstants.UNIT );
      fact.setUri( unitInfo.getUrl() );
      fact.setLabel( unitInfo.getUrl() );
      fact.autoFillDefaults();
      fact.copyDocInfo( valueFact );
      return fact;
   }


   /**
    * @param value         some value to be converted to a new unit type
    * @param sourceUnitUrl the current unit type for the given value
    * @param targetUnitUrl the wanted unit type
    * @return a new value based upon unit -required conversion
    */
   static private double convertNumber( final double value, final String sourceUnitUrl, final String targetUnitUrl ) {
      if ( Double.compare( value, ValueFact.NO_VALUE ) == 0 ) {
         return value;
      }
      if ( sourceUnitUrl.equals( targetUnitUrl ) ) {
         return value;
      }
      return getConverter( sourceUnitUrl, targetUnitUrl ).convert( value );
   }

   static private Converter getConverterByText( final String unitText ) {
      final UnitInfo unitInfo = UnitInfo.getByText( unitText );
      return unitInfo.getConverter();
   }


   static private Converter getConverter( final String sourceUnit ) {
      Converter converter = MultiplyConverter.getConverter( sourceUnit );
      if ( converter == null ) {
         converter = DivideConverter.getConverter( sourceUnit );
      }
      if ( converter == null ) {
         converter = NullConverter.NULL_CONVERTER;
      }
      return converter;
   }

   static private Converter getConverter( final String sourceUnit, final String targetUnit ) {
      Converter converter = MultiplyConverter.getConverter( sourceUnit, targetUnit );
      if ( converter == null ) {
         converter = DivideConverter.getConverter( sourceUnit, targetUnit );
      }
      if ( converter == null ) {
         converter = NullConverter.NULL_CONVERTER;
      }
      return converter;
   }

   public enum UnitInfo {
      CM( CENTIMETER, MultiplyConverter.CM_MM, "cm" ),
      MM( MILLIMETER, NullConverter.NULL_CONVERTER, "mm" ),
      UNKNOWN( UriConstants.UNKNOWN, NullConverter.NULL_CONVERTER, "unknown" );
      private final String _url;
      private final Converter _converter;
      private final String[] _texts;

      UnitInfo( final String url, final Converter converter, final String... texts ) {
         _url = url;
         _converter = converter;
         _texts = texts;
      }

      static public UnitInfo getByUrl( final String url ) {
         for ( UnitInfo info : values() ) {
            if ( info._url.equals( url ) ) {
               return info;
            }
         }
         return UNKNOWN;
      }

      static public UnitInfo getByText( final String unitText ) {
         for ( UnitInfo info : values() ) {
            for ( String text : info._texts ) {
               if ( unitText.equalsIgnoreCase( text ) ) {
                  return info;
               }
            }
         }
         return UNKNOWN;
      }

      public String getPrefText() {
         return _texts[ 0 ];
      }

      public String getUrl() {
         return _url;
      }

      public Converter getConverter() {
         return _converter;
      }
   }

   public interface Converter {
      double convert( final double source );

      String getSourceUrl();

      String getTargetUrl();
   }

   private enum NullConverter implements Converter {
      NULL_CONVERTER;

      public double convert( final double source ) {
         return source;
      }

      @Override
      public String getSourceUrl() {
         return null;
      }

      @Override
      public String getTargetUrl() {
         return null;
      }
   }

   private enum MultiplyConverter implements Converter {
      CM_MM( CENTIMETER, MILLIMETER, 10d, true );

      private final String _sourceUnit;
      private final String _targetUnit;
      private final double _multiplier;
      private final boolean _isPreferred;
      private final String[] _text;

      MultiplyConverter( final String sourceUnit, final String targetUnit,
                         final double multiplier, final boolean isPreferred, final String... text ) {
         _sourceUnit = sourceUnit;
         _targetUnit = targetUnit;
         _multiplier = multiplier;
         _isPreferred = isPreferred;
         _text = text;
      }

      public double convert( final double source ) {
         return source * _multiplier;
      }

      static Converter getConverter( final String sourceUnit ) {
         for ( MultiplyConverter converter : values() ) {
            if ( converter._sourceUnit.equals( sourceUnit ) && converter._isPreferred ) {
               return converter;
            }
         }
         return null;
      }

      static Converter getConverter( final String sourceUnit, final String targetUnit ) {
         for ( MultiplyConverter converter : values() ) {
            if ( converter._sourceUnit.equals( sourceUnit ) && converter._targetUnit.equals( targetUnit ) ) {
               return converter;
            }
         }
         return null;
      }

      @Override
      public String getSourceUrl() {
         return _sourceUnit;
      }

      @Override
      public String getTargetUrl() {
         return _targetUnit;
      }
   }

   private enum DivideConverter implements Converter {
      MM_CM( MILLIMETER, CENTIMETER, 10d, false );

      private final String _sourceUnit;
      private final String _targetUnit;
      private final double _denominator;
      private final boolean _isPreferred;

      DivideConverter( final String sourceUnit, final String targetUnit,
                       final double denominator, final boolean isPreferred, final String... text ) {
         _sourceUnit = sourceUnit;
         _targetUnit = targetUnit;
         _denominator = denominator;
         _isPreferred = isPreferred;
      }

      public double convert( final double source ) {
         return source / _denominator;
      }

      static Converter getConverter( final String sourceUnit ) {
         for ( DivideConverter converter : values() ) {
            if ( converter._sourceUnit.equals( sourceUnit ) && converter._isPreferred ) {
               return converter;
            }
         }
         return null;
      }

      static Converter getConverter( final String sourceUnit, final String targetUnit ) {
         for ( DivideConverter converter : values() ) {
            if ( converter._sourceUnit.equals( sourceUnit ) && converter._targetUnit.equals( targetUnit ) ) {
               return converter;
            }
         }
         return null;
      }

      @Override
      public String getSourceUrl() {
         return _sourceUnit;
      }

      @Override
      public String getTargetUrl() {
         return _targetUnit;
      }
   }


}