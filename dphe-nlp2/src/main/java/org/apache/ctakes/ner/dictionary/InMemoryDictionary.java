package org.apache.ctakes.ner.dictionary;

import org.apache.ctakes.ner.term.TermCandidate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
final public class InMemoryDictionary implements Dictionary {

   private final String _name;

   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   private final Map<String, Collection<TermCandidate>> _rareCandidateMap;

   /**
    * @param name         unique name for dictionary
    * @param rareCandidateMap Map with a lower-case Rare Word (tokens) as key, and CandidateTerm Collection as value
    */
   public InMemoryDictionary( final String name,
                              final Map<String, Collection<TermCandidate>> rareCandidateMap ) {
      _name = name;
      _rareCandidateMap = rareCandidateMap;
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
   public Collection<TermCandidate> getTermCandidates( final String lookupText,
                                                       final int indexInSentence,
                                                       final int sentenceLength ) {
      return _rareCandidateMap.getOrDefault( lookupText, Collections.emptyList() )
                              .stream()
                              .filter( t -> t.fitsInSentence( indexInSentence, sentenceLength ) )
                              .collect( Collectors.toSet());
   }


}
