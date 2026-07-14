package org.apache.ctakes.ner.tokenizer;


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
            "cannot", "couldn", "shouldn",
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



   ////////////////////////////   Gets a map of lower-case tokens to their count in the synonym set.
   ////////////////////////////   Includes both unwanted POS tokens and tokens with digits.
   ////////////////////////////   The unwanted POS tokens and tokens with digits will score lower when rare is chosen.

   static public Map<String, Long> getTokenCounts( final Collection<String[]> tokensCollection ) {
      return tokensCollection.stream()
                             .flatMap( Arrays::stream )
                             .filter( RareWordUtil::isOkToken )
                             .map( String::toLowerCase )
                             .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   /**
    *
    * @param token -
    * @return result of testing against token size, all letters, and unwanted POS token and invalid characters.
    */
   static private boolean isBestToken( final String token ) {
      return token.length() >= 5 && token.length() <= 24 && isBetterToken( token );
   }

   /**
    *
    * @param token -
    * @return result of testing against all letters, and unwanted POS token.
    */
   static private boolean isBetterToken( final String token ) {
      return token.length() > 2 && token.chars().allMatch( Character::isLetter )
            && !BAD_POS_TERM_SET.contains( token );
   }


   /**
    *
    * @param token -
    * @return result of testing against unwanted POS token and invalid characters.
    */
   static private boolean isGoodToken( final String token ) {
      return isOkToken( token ) && !BAD_POS_TERM_SET.contains( token );
   }

   /**
    *
    * @param token -
    * @return true if longer than 1 character and contains no invalid characters.
    */
   static private boolean isOkToken( final String token ) {
      return token.length() > 1
            && token.chars().allMatch( c -> Character.isLetterOrDigit( c ) || c == '-' );
   }

   /**
    *
    * @param tokens lower-case tokens
    * @param tokenCounts lower case token counts
    * @return lower case token with the fewest counts
    */
   static public int getRareTokenIndex( final String[] tokens, final Map<String, Long> tokenCounts ) {
      final List<String> lowers = new ArrayList<>();
      for ( String token : tokens ) {
         lowers.add( token.toLowerCase() );
      }
      int bestIndex = getRareTokenIndex( lowers, tokenCounts, RareWordUtil::isBestToken );
      if ( bestIndex < 0 ) {
         bestIndex = getRareTokenIndex( lowers, tokenCounts, RareWordUtil::isBetterToken );
      }
      if ( bestIndex < 0 ) {
         bestIndex = getRareTokenIndex( lowers, tokenCounts, RareWordUtil::isGoodToken );
      }
      if ( bestIndex < 0 ) {
         bestIndex = getRareTokenIndex( lowers, tokenCounts, RareWordUtil::isOkToken );
      }
      return bestIndex;
   }

   static private int getRareTokenIndex( final String[] tokens, final Map<String,Long> tokenCounts,
                                           final Function<String,Boolean> tester ) {
      int bestIndex = -1;
      long bestCount = Long.MAX_VALUE;
      for ( int i = 0; i < tokens.length; i++ ) {
         final String token = tokens[ i ];
         if ( !tester.apply( token ) ) {
            continue;
         }
         Long count = tokenCounts.get( tokens[i] );
         if ( count != null && count < bestCount ) {
            bestIndex = i;
            bestCount = count;
         }
      }
      return bestIndex;
   }

   static private int getRareTokenIndex( final List<String> tokens, final Map<String,Long> tokenCounts,
                                         final Function<String,Boolean> tester ) {
      int bestIndex = -1;
      long bestCount = Long.MAX_VALUE;
      for ( int i = 0; i < tokens.size(); i++ ) {
         final String token = tokens.get( i );
         if ( !tester.apply( token ) ) {
            continue;
         }
         Long count = tokenCounts.get( token );
         if ( count != null && count < bestCount ) {
            bestIndex = i;
            bestCount = count;
         }
      }
      return bestIndex;
   }


}
