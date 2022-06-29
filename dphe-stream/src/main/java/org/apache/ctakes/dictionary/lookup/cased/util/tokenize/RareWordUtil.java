package org.apache.ctakes.dictionary.lookup.cased.util.tokenize;


import org.apache.ctakes.dictionary.lookup.cased.lookup.CandidateTerm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/17/14
 */
final public class RareWordUtil {

   private RareWordUtil() {
   }

   // LookupDesc for the standard excluded pos tags are
   //   VB,VBD,VBG,VBN,VBP,VBZ,CC,CD,DT,EX,LS,MD,PDT,POS,PP,PP$,PRP,PRP$,RP,TO,WDT,WP,WPS,WRB
   // Listing every verb in the language seems a pain, but listing the others is possible.
   // Verbs should be rare in the dictionaries, excepting perhaps the activity and concept dictionaries
   // CD, CC, DT, EX, MD, PDT, PP, PP$, PRP, PRP$, RP, TO, WDT, WP, WPS, WRB
   // why not WP$ (possessive wh- pronoun "whose")
   // PP$ is a Brown POS tag, not Penn Treebank (as are the rest)

   static private final Set<String> BAD_POS_TERM_SET;

   static {
      final String[] BAD_POS_TERMS = {
            // VB  verb
            "be", "has", "have", "had", "do", "does", "did", "is", "isn", "am", "are", "was", "were",
            // CD  cardinal number
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            // CC  coordinating conjunction
            "and", "or", "but", "for", "nor", "so", "yet", "both", "while", "because",
            // DT  determiner
            "this", "that", "these", "those", "the", "all", "an", "another", "any", "each",
            "either", "many", "much", "neither", "no", "some", "such", "them",
            // EX  existential there
            "there",
            // IN
            "among", "upon", "in", "into", "below", "atop", "until", "over", "under", "towards", "to",
            "whether", "despite", "if",
            // MD  modal
            "can", "should", "will", "may", "shall", "might", "must", "could", "would", "need", "ought",
            "cannot", "shouldn",
            // PDT  predeterminer
            "some", "many", "any", "each", "all", "few", "most", "both", "half", "none", "twice",
            // PP  prepositional phrase (preposition)
            "at", "before", "after", "behind", "beneath", "beside", "between", "into", "through", "across", "of",
            "concerning", "like", "unlike", "except", "with", "within", "without", "toward", "to", "past", "against",
            "during", "until", "throughout", "below", "besides", "beyond", "from", "inside", "near", "outside", "since",
            "upon",
            // PP$  possessive personal pronoun - Brown POS tag, not Penn TreeBank
            "my", "our", "your", "her", "their", "whose",
            // PRP  personal pronoun, plurals added
            "i", "you", "he", "she", "it", "them", "they", "him", "himself", "we", "us",
            // PRP$  possesive pronoun
            "mine", "yours", "his", "hers", "its", "our", "ours", "theirs",
            // RP  particle  - this contains some prepositions
            "about", "off", "up", "along", "away", "back", "by", "down", "forward", "in", "on", "out",
            "over", "around", "under",
            // TO  to  - also a preposition
            "to",
            // WDT  wh- determiner
            "what", "whatever", "which", "whichever", "that",
            // WP, WPS  wh- pronoun, nominative wh- pronoun
            "who", "whom", "which", "that", "whoever", "whomever", "whose",
            // WRB
            "how", "where", "when", "however", "wherever", "whenever", "wherein", "why",
            // Mine ... some correlative conjunctions, etc.
            "no", "not", "oh", "mr", "mrs", "miss", "dr", "as", "only", "also", "either", "neither", "whether", "per",
            // additional numbers
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
            "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
            "hundred", "thousand", "million", "billion", "trillion",
            };
      BAD_POS_TERM_SET = new HashSet<>( Arrays.asList( BAD_POS_TERMS ) );
   }

   static public Collection<String> getUnwantedPosTexts() {
      return Collections.unmodifiableCollection( BAD_POS_TERM_SET );
   }

   static public Map<String, Collection<CandidateTerm>> createTermMap(
         final Collection<TokenizedTerm> tokenizedTerms ) {
      final Map<String, Long> tokenCountMap = createTokenCountMap( tokenizedTerms );
      final Map<String, Collection<CandidateTerm>> candidateTermMap = new HashMap<>();
      for ( TokenizedTerm tokenizedTerm : tokenizedTerms ) {
         final String[] tokens = tokenizedTerm.getTokens();
         final int rareWordIndex = getRareTokenIndex( tokens, tokenCountMap );
         if ( rareWordIndex < 0 ) {
//            LOGGER.warn( "Bad Rare Word Index for " + String.join( " ", tokens ) );
            continue;
         }
         candidateTermMap.computeIfAbsent( tokenizedTerm.getTokens()[ rareWordIndex ].toLowerCase(),
                                           t -> new HashSet<>() )
                         .add( new CandidateTerm( tokenizedTerm.getCui(),
                                                  tokens,
                                                  (short)rareWordIndex,
                                                  (short)500, (short)1 ) );
      }
      return candidateTermMap;
   }

   static private Map<String, Long> createTokenCountMap( final Collection<TokenizedTerm> tokenizedTerms ) {
      return tokenizedTerms.parallelStream()
                           .map( TokenizedTerm::getTokens )
                           .flatMap( Arrays::stream )
                           .filter( RareWordUtil::isRarableToken )
                           .map( String::toLowerCase )
                           .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }


   static public int getRareTokenCaps( final String[] tokens, final Map<String, Long> tokenCounts ) {
      int bestIndex = -1;
      long bestCount = Long.MAX_VALUE;
      for ( int i = 0; i < tokens.length; i++ ) {
         if ( tokens[ i ].length() >= 48 ) {
            continue;
         }
         Long count = tokenCounts.get( tokens[ i ] );
         if ( count != null && count < bestCount ) {
            bestIndex = i;
            bestCount = count;
         }
      }
      return bestIndex;
   }

   static public int getForcedRareTokenCaps( final String[] tokens ) {
      if ( tokens.length == 1 ) {
         return ( tokens[ 0 ].length() > 48) ? -1 : 0;
      }
      int bestIndex = -1;
      int bestScore = 0;
      for ( int i = 0; i < tokens.length; i++ ) {
         if ( tokens[ i ].length() >= 48 ) {
            continue;
         }
         final int score = scoreRarableTokenCaps( tokens[ i ] );
         if ( score > bestScore ) {
            bestIndex = i;
            bestScore = score;
         }
      }
      return bestIndex;
   }


   static public int scoreRarableTokenCaps( final String token ) {
      int letterCount = 0;
      int capsCount = 0;
      int numberCount = 0;
      for ( int i = 0; i < token.length(); i++ ) {
         if ( Character.isLetter( token.charAt( i ) ) ) {
            letterCount++;
            if ( Character.isUpperCase( token.charAt( i ) ) ) {
               capsCount++;
            }
         } else if ( Character.isDigit( token.charAt( i ) ) ) {
            numberCount++;
         }
      }
      final int score = capsCount * 10 + letterCount * 2 + numberCount;
      if ( BAD_POS_TERM_SET.contains( token.toLowerCase() ) ) {
         return Math.max( 2, score / 2 );
      }
      return score;
   }

   static public int getRareTokenIndex( final String[] tokens, final Map<String, Long> tokenCounts ) {
      final int rareWordIndex = getRareByCounts( tokens, tokenCounts );
      if ( rareWordIndex >= 0 ) {
         return rareWordIndex;
      }
      return getRareByForce( tokens );
   }

   static public int getRareByForce( final String[] tokens ) {
      if ( tokens.length == 1 ) {
         return ( tokens[ 0 ].length() > 48) ? -1 : 0;
      }
      int bestIndex = -1;
      int bestScore = 0;
      for ( int i = 0; i < tokens.length; i++ ) {
         if ( tokens[ i ].length() >= 48 ) {
            continue;
         }
         final int score = scoreRarableToken( tokens[ i ] );
         if ( score > bestScore ) {
            bestIndex = i;
            bestScore = score;
         }
      }
      return bestIndex;
   }

   static public int scoreRarableToken( final String token ) {
      int letterCount = 0;
      int numberCount = 0;
      for ( int i = 0; i < token.length(); i++ ) {
         if ( Character.isLetter( token.charAt( i ) ) ) {
            letterCount++;
         } else if ( Character.isDigit( token.charAt( i ) ) ) {
            numberCount++;
         }
      }
      final int score = 10 + letterCount - numberCount;
      if ( BAD_POS_TERM_SET.contains( token.toLowerCase() ) ) {
         return Math.max( 2, score / 2 );
      }
      return score;
   }

   static public boolean isRarableToken( final String token ) {
      if ( token.length() >= 48 || token.length() < 3 ) {
         return false;
      }
      if ( token.chars().anyMatch( c -> (!Character.isLetter( c ) && c != '-' ) ) ) {
         return false;
      }
      return !BAD_POS_TERM_SET.contains( token.toLowerCase() );
   }

   static private int getRareByCounts( final String[] tokens, final Map<String, Long> tokenCounts ) {
      int bestIndex = -1;
      long bestCount = Long.MAX_VALUE;
      for ( int i = 0; i < tokens.length; i++ ) {
         if ( !isRarableToken( tokens[i] ) ) {
            continue;
         }
         final String lower = tokens[ i ].toLowerCase();
         Long count = tokenCounts.get( lower );
         if ( count != null && count < bestCount ) {
            bestIndex = i;
            bestCount = count;
         }
      }
      return bestIndex;
   }


}
