package org.apache.ctakes.cancer.phenotype.property;

import org.apache.ctakes.cancer.util.SpannedEntity;
import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
public class SpannedTest<T extends Test> implements SpannedEntity {

   static private final Logger LOGGER = Logger.getLogger( "SpannedTest" );

   private final T _test;
   private final int _startOffset;
   private final int _endOffset;


   public SpannedTest( final T test, final int startOffset, final int endOffset ) {
      _test = test;
      _startOffset = startOffset;
      _endOffset = endOffset;
   }

   final public T getTest() {
      return _test;
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
      return _test.getTitle() + " at " + getStartOffset() + "-" + getEndOffset();
   }


}
