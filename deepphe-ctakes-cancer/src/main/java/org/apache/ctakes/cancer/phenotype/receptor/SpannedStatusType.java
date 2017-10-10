package org.apache.ctakes.cancer.phenotype.receptor;

import org.apache.ctakes.cancer.phenotype.property.SpannedType;
import org.apache.log4j.Logger;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/6/2015
 */
final class SpannedStatusType extends SpannedType<StatusType> {

   static private final Logger LOGGER = Logger.getLogger( "SpannedStatusType" );


   SpannedStatusType( final StatusType type, final int startOffset, final int endOffset ) {
      super( type, startOffset, endOffset );
   }

}
