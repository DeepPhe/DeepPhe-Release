package org.apache.ctakes.dictionary.lookup.cased.detailer;

import org.apache.ctakes.dictionary.lookup.cased.lookup.DiscoveredTerm;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public interface TermDetailer {

   /**
    * The Type identifier and Name are used to maintain a collection of term encoders,
    * so the combination of Type and Name should be unique for each encoder if possible.
    *
    * @return simple name for the encoder
    */
   String getName();


   default Details getDetails( final DiscoveredTerm discoveredTerm ) {
      return getDetails( discoveredTerm.getCui() );
   }


   Details getDetails( final String cui );


}
