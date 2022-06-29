package org.apache.ctakes.dictionary.lookup.cased.dictionary;


import org.apache.ctakes.dictionary.lookup.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.lookup.cased.lookup.LookupToken;

import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
final public class InMemoryDictionary implements CasedDictionary {

   private final String _name;

   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   private final Map<String, Collection<CandidateTerm>> _candidateTermMap;

   /**
    * @param name         unique name for dictionary
    * @param candidateTermMap Map with a case-sensitive Rare Word (tokens) as key, and RareWordTerm Collection as value
    */
   public InMemoryDictionary( final String name,
                              final Map<String, Collection<CandidateTerm>> candidateTermMap ) {
      _name = name;
      _candidateTermMap = candidateTermMap;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<CandidateTerm> getCandidateTerms( final LookupToken lookupToken ) {
      return _candidateTermMap.get( lookupToken.getLowerText() );
   }


}
