package org.apache.ctakes.cancer.phenotype.property;

import org.apache.ctakes.cancer.util.SpannedEntity;
import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
public class SpannedValue<T extends Value> implements SpannedEntity {

   static private final Logger LOGGER = Logger.getLogger( "SpannedValue" );

   private final T _value;
   private final int _startOffset;
   private final int _endOffset;


   public SpannedValue( final T value, final int startOffset, final int endOffset ) {
      _value = value;
      _startOffset = startOffset;
      _endOffset = endOffset;
   }

   final public T getValue() {
      return _value;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public int getStartOffset() {
      return _startOffset;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public int getEndOffset() {
      return _endOffset;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public String toString() {
      return _value.getTitle() + " at " + getStartOffset() + "-" + getEndOffset();
   }

}
