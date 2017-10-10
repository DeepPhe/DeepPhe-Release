package org.apache.ctakes.cancer.phenotype.property;

import org.apache.ctakes.cancer.util.SpannedEntity;
import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
public class SpannedType<T extends Type> implements SpannedEntity {

   static private final Logger LOGGER = Logger.getLogger( "SpannedType" );

   private final T _type;
   private final int _startOffset;
   private final int _endOffset;


   public SpannedType( final T type, final int startOffset, final int endOffset ) {
      _type = type;
      _startOffset = startOffset;
      _endOffset = endOffset;
   }

   final public T getType() {
      return _type;
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
      return _type.getTitle() + " at " + getStartOffset() + "-" + getEndOffset();
   }


}
