package org.apache.ctakes.dictionary.lookup.cased.lookup;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.dictionary.lookup.cased.dictionary.CasedDictionary;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class ContiguousLookupEngine implements LookupEngine {

   static private final Logger LOGGER = Logger.getLogger( "ContiguousLookupEngine" );


   /**
    * Given a dictionary, tokens, and lookup token indices, populate a terms collection with discovered terms
    *
    * @param dictionary   -
    * @param windowTokens -
    * @return map of text spans to terms discovered at those text spans.
    */
   public Map<Pair<Integer>, Collection<DiscoveredTerm>> findTerms( final CasedDictionary dictionary,
                                                                    final List<LookupToken> windowTokens,
                                                                    final int consecutiveSkipMax,
                                                                    final int totalSkipMax ) {
      final Map<Pair<Integer>, Collection<DiscoveredTerm>> discoveredTermMap = new HashMap<>();
      int windowTokenIndex = -1;
      final int windowLength = windowTokens.size();
      Collection<CandidateTerm> candidateTerms;
      for ( LookupToken windowToken : windowTokens ) {
         windowTokenIndex++;
         if ( !windowToken.isValidIndexToken() ) {
            continue;
         }
         candidateTerms = dictionary.getCandidateTerms( windowToken );
         if ( candidateTerms == null || candidateTerms.isEmpty() ) {
            continue;
         }
         for ( CandidateTerm candidateTerm : candidateTerms ) {
            if ( candidateTerm.getTokens().length == 1 ) {
               // Single word term, add and move on
               discoveredTermMap.computeIfAbsent( windowToken.getTextSpan(), s -> new HashSet<>() )
                                .add( new DiscoveredTerm( candidateTerm,
                                                          Collections.singletonList( windowToken ),
                                                          windowTokenIndex == 0 ) );
               continue;
            }
            if ( candidateTerm.isOutsideWindow( windowLength, windowTokenIndex ) ) {
               continue;
            }
            if ( candidateTerm.isMatch( windowTokens, windowTokenIndex ) ) {
               final int spanBegin = windowTokens.get( windowTokenIndex - candidateTerm.getLookupIndex() )
                                                 .getBegin();
               final int spanEnd = windowTokens.get( windowTokenIndex + candidateTerm.getSuffixLength() )
                                               .getEnd();
               discoveredTermMap.computeIfAbsent( new Pair<>( spanBegin, spanEnd ), s -> new HashSet<>() )
                                .add( new DiscoveredTerm( candidateTerm, windowTokens, windowTokenIndex ) );
            }
         }
      }
      return discoveredTermMap;
   }

}
