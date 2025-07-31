package org.apache.ctakes.ner.dictionary;


import org.apache.ctakes.ner.term.LookupToken;
import org.apache.ctakes.ner.term.TermCandidate;

import java.util.Collection;

/**
 * Dictionary used to lookup terms by the most rare word within them.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
public interface Dictionary {


   /**
    * The Type identifier and Name are used to maintain a collection of dictionaries,
    * so the combination of Type and Name should be unique for each dictionary if possible.
    *
    * @return simple name for the dictionary
    */
   String getName();

   /**
    * Any single token can exist in zero or more terms in the dictionary.  It may exist as its -own- form or as an
    * alternate canonical variant.  This method will check the dictionary for both.
    *
    * @param lookupToken a single-word token
    * @return zero or more terms that contain the lookup token
    */
   default Collection<TermCandidate> getTermCandidates( final LookupToken lookupToken,
                                                        final int indexInSentence,
                                                        final int sentenceLength  ) {
      return getTermCandidates( lookupToken.getText(), indexInSentence, sentenceLength );
   }

   /**
    * Any single token can exist in zero or more terms in the dictionary.  It may exist as its -own- form or as an
    * alternate canonical variant.  This method will check the dictionary for both.
    *
    * @param lookupText a single-word token
    * @return zero or more terms that contain the lookup token
    */
   Collection<TermCandidate> getTermCandidates( final String lookupText,
                                                final int indexInSentence,
                                                final int sentenceLength );

}
