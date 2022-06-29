package org.apache.ctakes.dictionary.lookup.cased.lookup;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.dictionary.lookup.cased.dictionary.CasedDictionary;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public interface LookupEngine {



   /**
    * Given a dictionary, tokens, and lookup token indices, populate a terms collection with discovered terms
    *
    * @param dictionary   -
    * @param windowTokens -
    * @return map of text spans to terms discovered at those text spans.
    */
   Map<Pair<Integer>, Collection<DiscoveredTerm>> findTerms( final CasedDictionary dictionary,
                                                                             final List<LookupToken> windowTokens,
                                                                             final int consecutiveSkipMax,
                                                                             final int totalSkipMax );


}
