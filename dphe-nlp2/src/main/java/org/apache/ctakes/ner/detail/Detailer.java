package org.apache.ctakes.ner.detail;


import org.apache.ctakes.ner.term.DiscoveredTerm;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public interface Detailer {

   /**
    * The Type identifier and Name are used to maintain a collection of term encoders,
    * so the combination of Type and Name should be unique for each encoder if possible.
    *
    * @return simple name for the encoder
    */
   String getName();


   default Collection<Details> getDetails( final DiscoveredTerm discoveredTerm ) {
      return getDetails( discoveredTerm.getUri() );
   }

   Collection<Details> getDetails( final String uri );


}
