package org.apache.ctakes.ner.filter;

import org.apache.ctakes.ner.term.DetailedTerm;

import java.util.Collection;

/**
 * No Filtering.
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
final public class PassFilter implements TermFilter {

   public Collection<DetailedTerm> getFilteredTerms( final Collection<DetailedTerm> detailedTerms ) {
      return detailedTerms;
   }

}
