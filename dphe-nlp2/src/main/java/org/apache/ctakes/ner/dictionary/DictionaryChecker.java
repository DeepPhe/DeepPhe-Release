package org.apache.ctakes.ner.dictionary;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.term.DiscoveredTerm;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.ner.term.LookupToken;
import org.apache.ctakes.ner.term.TermCandidate;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public class DictionaryChecker {

   static private final Logger LOGGER = Logger.getLogger( "DictionaryChecker" );

   /**
    * Given a dictionary, tokens, and lookup token indices, populate a terms collection with discovered terms
    *
    * @param dictionary   -
    * @param sentenceTokens -
    * @return map of text spans to terms discovered at those text spans.
    */
   public Map<Pair<Integer>, Collection<DiscoveredTerm>> findTerms( final Dictionary dictionary,
                                                                          final List<LookupToken> sentenceTokens,
                                                                          final int consecutiveSkipMax,
                                                                          final int totalSkipMax ) {
      final Map<Pair<Integer>, Collection<DiscoveredTerm>> discoveredTermMap = new HashMap<>();
      int sentenceTokenIndex = -1;
      final int sentenceLength = sentenceTokens.size();
      Collection<TermCandidate> termCandidates;
      for ( LookupToken sentenceToken : sentenceTokens ) {
         sentenceTokenIndex++;
         if ( !sentenceToken.isValidIndexToken() ) {
            continue;
         }
         final String lowerLookup = sentenceToken.getText().toLowerCase();
         termCandidates = dictionary.getTermCandidates( lowerLookup, sentenceTokenIndex, sentenceLength );
         if ( termCandidates == null || termCandidates.isEmpty() ) {
            continue;
         }
         for ( TermCandidate termCandidate : termCandidates ) {
            if ( termCandidate.getTokenCount() == 1 ) {
               // Single word term, add and move on
               discoveredTermMap.computeIfAbsent( sentenceToken.getTextSpan(), s -> new HashSet<>() )
                                .add( new DiscoveredTerm( termCandidate,
                                      sentenceTokens,
                                      sentenceTokenIndex,
                                      consecutiveSkipMax,
                                      totalSkipMax ) );
               continue;
            }
            if ( !termCandidate.isPrefixMatch( sentenceTokens, sentenceTokenIndex ) ) {
//               LogFileWriter.add( termCandidate.getUri() + " " + sentenceTokenIndex
//                     + " Prefix Mismatch for "
//                     + String.join( " ", termCandidate.getPrefixes() )
//                     + " -" + termCandidate.getTokens()[ termCandidate.getRareIndex() ] + "- "
//                     + String.join( " ", termCandidate.getSuffixes() ) + " : "
//                     + sentenceTokens.stream().map( LookupToken::getText ).collect( Collectors.joining( " " )) );
               continue;
            }
            if ( !termCandidate.isSuffixMatch( sentenceTokens, sentenceTokenIndex ) ) {
//               LogFileWriter.add( termCandidate.getUri() + " " + sentenceTokenIndex
//                     + " Suffix Mismatch for "
//                     + String.join( " ", termCandidate.getPrefixes() )
//                     + " -" + termCandidate.getTokens()[ termCandidate.getRareIndex() ] + "- "
//                     + String.join( " ", termCandidate.getSuffixes() ) + " : "
//                     + sentenceTokens.stream().map( LookupToken::getText ).collect( Collectors.joining( " " )) );
               continue;
            }
//            LogFileWriter.add( "Found: " + String.join( " ", termCandidate.getPrefixes() )
//                  + " -" + termCandidate.getTokens()[ termCandidate.getRareIndex() ] + "- "
//                  + String.join( " ", termCandidate.getSuffixes() ) );
            final int spanBegin = sentenceTokens.get( sentenceTokenIndex - termCandidate.getPrefixLength() ).getBegin();
            final int spanEnd = sentenceTokens.get( sentenceTokenIndex + termCandidate.getSuffixLength() ).getEnd();
            discoveredTermMap.computeIfAbsent( new Pair<>( spanBegin, spanEnd ), s -> new HashSet<>() )
                             .add( new DiscoveredTerm( termCandidate,
                                   sentenceTokens,
                                   sentenceTokenIndex,
                                   consecutiveSkipMax,
                                   totalSkipMax ) );
         }
      }
      return discoveredTermMap;
   }

}
