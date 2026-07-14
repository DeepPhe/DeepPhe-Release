package org.apache.ctakes.ner.term;


import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
final public class DiscoveredTerm {

   private final TermCandidate _termCandidate;
   private final List<LookupToken> _sentenceTokens;
   private final int _sentenceTokenIndex;
   private final int _consecutiveSkips;
   private final int _totalSkips;

   public DiscoveredTerm( final TermCandidate termCandidate,
                          final List<LookupToken> sentenceTokens,
                          final int sentenceTokenIndex,
                          final int consecutiveSkips,
                          final int totalSkips ) {
      _termCandidate = termCandidate;
      _sentenceTokens = sentenceTokens;
      _sentenceTokenIndex = sentenceTokenIndex;
      _consecutiveSkips = consecutiveSkips;
      _totalSkips = totalSkips;
   }

   public String getUri() {
      return _termCandidate.getUri();
   }

   public double calculateScore() {
      final String[] termTokens = _termCandidate.getTokens();
      final String[] sentenceTokens = getSentenceTokens();
      final long tokenLengthDeficit = getTokenLengthDeficit( termTokens );
      final long tokenCaseDeficit = getCaseDeficit( termTokens, sentenceTokens );
      // 100 -   3=(50-65)  4=(20-40)  5=(10-40)  6=(0-25)  7+=(0-25)
      // 100 -   3=(60-75)  4=(40-)  5=(30-)  6=(20-)  7+=(0-25)
      final long posDeficit = getPosDeficit();
      return Math.max( 1, 100 - tokenLengthDeficit - tokenCaseDeficit - posDeficit );
   }

   private String[] getSentenceTokens() {
      final String[] sentenceTokens = new String[ _termCandidate.getTokenCount() ];
      final int sentenceOffset = _sentenceTokenIndex - _termCandidate.getPrefixLength();
      for ( int i=0; i< _termCandidate.getTokenCount(); i++ ) {
         sentenceTokens[i] = _sentenceTokens.get( sentenceOffset+i ).getText();
      }
      return sentenceTokens;
   }

   /**
    *
    * @return 20 if the term is only one word and is of a part of speech that shouldn't represent a term.
    */
   private long getPosDeficit() {
      if ( _termCandidate.getTokenCount() > 1 ) {
         return 0;
      }
      final String pos = _sentenceTokens.get( _sentenceTokenIndex ).getPOS();
      return BAD_POS.contains( pos ) ? 20 : 0;
   }

   // See RareWordUtil for examples of these parts of speech.  Maybe less with JJ, JJR, JJS ?
   static private final Collection<String> BAD_POS = new HashSet<>( Arrays.asList(
         "CC", "DT", "EX", "IN", "MD", "PDT", "PP$", "PPZ", "PRP", "PRP$", "TO", "WDT", "WP", "WP$", "WRB"
   ) );

   static private long getTokenLengthDeficit( final String[] tokens ) {
      if ( tokens.length > 3 ) {
         return 0;
      }
      final IntSummaryStatistics stats = Arrays.stream( tokens )
                                               .mapToInt( String::length )
                                               .summaryStatistics();
      // term length deficit: 1  = 30, 2 = 25, 3 = 20, ELSE 0 ... old 4 = 10, 5 = 5   = 5 * (6-length)
      final long termLengthDeficit = Math.max( 0, 5*(7-stats.getSum()) );
      // token length deficit: 1 = 20, 2 = 15, 3 = 10, ELSE 0  ... old 4 = 5   = 5 * (5-length)
      final long tokenLengthDeficit = Math.max( 0, 5*(5-stats.getMax()) );
      // count deficit: 1 = 2, 2 = 1 , 3 = 0   = 3-count
      final long tokenCountDeficit = Math.max( 0, 3-tokens.length );
         // term 5*6-length , token 5*5-length
      // 2=(20+20)*4
      // 3=(15+10)*2 =50
      // 4(1 token)=(10+5)*2 =30 || 4(2 tokens)=(10+10)*1=20
      // 5(1 tokens)=(5+0)*2 =10 || 5(2 tokens 4,1)=(5+5)*1=10  || 5(2 tokens 3,2)=(5+10)*1 =15
      // 6(1 token)=(0+0)*2 =0 || 6(2 tokens 5,1)=(0+0)*1 =0  || 6(2 tokens 4,2)=(0+5)*1=5  || 6(2 3,3)=(0+10)*1 = 10
      // 6(3 tokens 3,2,1)=(0+10)*0  || 6(3 tokens 4,1,1) =(0+5)*0
         // term 5*6-length , token 5*6-length
      // 3=(15+15)*2 =60
      // 4(1 token)=(10+10)*2 =40 || 4(2 tokens)=(10+15)*1=25
      // 5(1 tokens)=(5+5)*2 =20 || 5(2 tokens 4,1)=(5+10)*1=15  || 5(2 tokens 3,2)=(5+15)*1 =20
      // 6(1 token)=(0+0)*2 =0 || 6(2 tokens 5,1)=(0+5)*1 =5  || 6(2 tokens 4,2)=(0+10)*1=10  || 6(2 3,3)=(0+15)*1 = 15
      // 6(3 tokens 3,2,1)=(0+15)*0  || 6(3 tokens 4,1,1) =(0+10)*0
         // term 5*7-length , token 5*6-length
      // 3=(20+15)*2 =60
      // 4(1 token)=(15+10)*2 =50 || 4(2 tokens)=(15+15)*1=30
      // 5(1 tokens)=(10+5)*2 =30 || 5(2 tokens 4,1)=(10+10)*1=20  || 5(2 tokens 3,2)=(10+15)*1 =25
      // 6(1 token)=(5+0)*2 =10 || 6(2 tokens 5,1)=(5+5)*1 =10  || 6(2 tokens 4,2)=(5+10)*1=15  || 6(2 3,3)=(5+15)*1 = 20
      // 6(3 tokens 3,2,1)=(5+15)*0  || 6(3 tokens 4,1,1) =(5+10)*0
      return (termLengthDeficit + tokenLengthDeficit) * tokenCountDeficit;
   }


   static private long getCaseDeficit( final String[] termTokens, final String[] sentenceTokens ) {
      long deficit = 0;
      for ( int i=0; i<termTokens.length; i++ ) {
          deficit += getCaseDeficit( termTokens[i], sentenceTokens[i] );
          if ( deficit >= 25 ) {
             return 25;
          }
      }
      return deficit;
   }

   static private long getCaseDeficit( final String token, final String sentenceToken ) {
      // 1/1 * 25 =25                                 1*5 =5
      // 1/2*20=10 2/2*20=20                          2*5 =10   1/2=5
      // 1/3*15=5 2/3*15=10 3/3*15=15                 3*5 =15   1/3=5  2/3=10
      // 1/4*10=2.5 2/4*10=5 3/4*10=7.5 4/4*10=10     4*5 =20   1/4=5  2/4=10  3/4=15
      // 1/5*5=1 2/5*5=2 ...                          5*5 =25   1/5=5  2/5=10  3/5=15  4/5=20
      final char[] tokenChars = token.toCharArray();
      final char[] sentenceTchars = sentenceToken.toCharArray();
      // If a token length > 3 start with 2nd char, force match the first char in case of capitalization.
      int start = token.length() > 3 ? 1 : 0;
      // 2 = -5, 3 = -4, 4 = -3, 5 = -2, 6 = -1     7-Min( 6, length )
      long penalty = 7 - Math.min( 6, token.length() );
      long deficit = 0;
      for ( int i=start; i<token.length(); i++ ) {
         // ascii codes are off by 32 if the case is different
         deficit += ( tokenChars[i] - sentenceTchars[i] == 0 ) ? 0 : penalty;
         if ( deficit >= 25 ) {
            // break.  Bad enough.
            break;
         }
      }
      return deficit;
   }



   public int getTotalSkips() {
      return _totalSkips;
   }

   public int getConsecutiveSkips() {
      return _consecutiveSkips;
   }

}
