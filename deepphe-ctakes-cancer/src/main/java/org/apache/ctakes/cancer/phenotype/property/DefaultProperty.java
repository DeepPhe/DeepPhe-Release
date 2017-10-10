package org.apache.ctakes.cancer.phenotype.property;

import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
public class DefaultProperty<T extends Type, V extends Value> implements SpannedProperty<T, V> {

   static private final Logger LOGGER = Logger.getLogger( "DefaultProperty" );

   private final SpannedType<T> _spannedType;
   private final SpannedValue<V> _spannedValue;

   public DefaultProperty( final SpannedType<T> spannedType, final SpannedValue<V> spannedValue ) {
      _spannedType = spannedType;
      _spannedValue = spannedValue;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public SpannedType<T> getSpannedType() {
      return _spannedType;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public SpannedValue<V> getSpannedValue() {
      return _spannedValue;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public int getStartOffset() {
      return Math.min( _spannedType.getStartOffset(), _spannedValue.getStartOffset() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public int getEndOffset() {
      return Math.max( _spannedType.getEndOffset(), _spannedValue.getEndOffset() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return _spannedType.toString() + " is " + _spannedValue.toString();
   }

}
