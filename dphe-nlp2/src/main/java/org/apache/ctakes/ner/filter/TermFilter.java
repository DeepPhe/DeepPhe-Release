package org.apache.ctakes.ner.filter;

import org.apache.ctakes.ner.term.DetailedTerm;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
public interface TermFilter {

   Collection<DetailedTerm> getFilteredTerms( Collection<DetailedTerm> detailedTerms );

}
