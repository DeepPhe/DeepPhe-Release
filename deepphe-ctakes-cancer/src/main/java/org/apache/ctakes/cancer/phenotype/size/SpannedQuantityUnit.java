package org.apache.ctakes.cancer.phenotype.size;

import org.apache.ctakes.cancer.phenotype.property.SpannedType;
import org.apache.log4j.Logger;

import javax.annotation.concurrent.Immutable;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/8/2016
 */
@Immutable
final class SpannedQuantityUnit extends SpannedType<QuantityUnit> {

   static private final Logger LOGGER = Logger.getLogger( "SpannedQuantityUnit" );

   SpannedQuantityUnit( final QuantityUnit type, final int startOffset, final int endOffset ) {
      super( type, startOffset, endOffset );
   }

}