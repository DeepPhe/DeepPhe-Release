package org.apache.ctakes.cancer.phenotype.receptor;


import org.apache.ctakes.cancer.phenotype.property.SpannedValue;
import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/6/2015
 */
final class SpannedStatusValue extends SpannedValue<StatusValue> {

   static private final Logger LOGGER = Logger.getLogger( "SpannedStatusValue" );

   SpannedStatusValue( final StatusValue statusValue, final int startOffset, final int endOffset ) {
      super( statusValue, startOffset, endOffset );
   }

}
