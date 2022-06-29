package org.apache.ctakes.dictionary.lookup.cased.lookup;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.Pair;

import java.util.List;

import static org.apache.ctakes.dictionary.lookup.cased.lookup.CapsType.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
@Immutable
final public class DiscoveredTerm {
   private final CandidateTerm _candidateTerm;
   private final List<LookupToken> _docTextTokens;
   private final int _consecutiveSkips;
   private final int _totalSkips;
   private final boolean _startsSentence;

   public DiscoveredTerm( final CandidateTerm candidateTerm,
                          final List<LookupToken> windowTokens,
                          final int windowTokenIndex ) {
      this( candidateTerm,
            windowTokens.subList( windowTokenIndex - candidateTerm.getLookupIndex(),
                                  windowTokenIndex + candidateTerm.getSuffixLength() + 1 ),
            windowTokenIndex - candidateTerm.getLookupIndex() == 0 );
   }

   public DiscoveredTerm( final CandidateTerm candidateTerm,
                          final List<LookupToken> docTextTokens,
                          final boolean startsSentence ) {
      this( candidateTerm, docTextTokens, startsSentence, 0, 0 );
   }

   public DiscoveredTerm( final CandidateTerm candidateTerm,
                          final List<LookupToken> docTextTokens,
                          final boolean startsSentence,
                          final int consecutiveSkips,
                          final int totalSkips ) {
      _candidateTerm = candidateTerm;
      _docTextTokens = docTextTokens;
      _startsSentence = startsSentence;
      _consecutiveSkips = consecutiveSkips;
      _totalSkips = totalSkips;
   }

   public String getCui() {
      return _candidateTerm.getCui();
   }

   public Pair<Integer> getTextSpan() {
      return new Pair<>( _docTextTokens.get( 0 ).getBegin(),
                         _docTextTokens.get( _docTextTokens.size() - 1 ).getEnd() );
   }

   public short getTopRank() {
      return _candidateTerm.getTopRank();
   }

   public short getEntryCounts() {
      return _candidateTerm.getEntryCounts();
   }

   public short getSecondRank() {
      return _candidateTerm.getSecondRank();
   }

   public short getSecondCounts() {
      return _candidateTerm.getSecondCounts();
   }

   public short getVariantTopRank() {
      return _candidateTerm.getVariantTopRank();
   }

   public short getVariantEntryCounts() {
      return _candidateTerm.getVariantEntryCounts();
   }

   public short getOtherCuisCount() {
      return _candidateTerm.getOtherCuisCount();
   }

   public String[] getVocabCodes() {
      return _candidateTerm.getVocabCodes();
   }


   public int getCapsTypeMatchCount() {
      int count = 0;
      for ( int i=0; i<_candidateTerm.getTokens().length; i++ ) {
         if ( _candidateTerm.getCapsType( i ) == _docTextTokens.get( i ).getCapsType() ) {
            count++;
         }
      }
      return count;
   }



   public int getTotalSkips() {
      return _totalSkips;
   }

   public int getConsecutiveSkips() {
      return _consecutiveSkips;
   }

   /**
    *
    * @return admittedly questionable value of confidence between 0.0 and 1.0
    */
   public float getConfidence() {
      return (confidenceByTopRank()
              + confidenceByEntryCounts()
              + confidenceByVariantRank()
              + confidenceByVariantCounts()
              + confidenceByOtherCuis()
              + confidenceByCapsTypes() ) / 6;
   }

   private float confidenceByTopRank() {
      // Umls has 446 ranks, ncim has 423 ... use 500.
      return Math.min( 1f, (float)getTopRank()/500 );
   }

   private float confidenceByEntryCounts() {
      // Tough to quantify.  More entries is better, but how to really use this value? 0.75 + 0.5 * entry count
      return Math.max( 1f, 0.75f + 0.5f*getEntryCounts() );
   }

   private float confidenceByVariantRank() {
      // Kludgy.  If there was no variation used, 1.  If it is equal to the top rank, 1.
      if ( getVariantTopRank() == 0 || getVariantTopRank() == getTopRank() ) {
         return 1f;
      }
      return Math.min( 1f, (float)getVariantTopRank()/getTopRank() );
   }

   private float confidenceByVariantCounts() {
      // Zero variants is good, but many variants are also good.  Variants should roughly scale with token count.
      return Math.max( 0,
                       (_candidateTerm.getTokens().length - getVariantEntryCounts())
                       / _candidateTerm.getTokens().length );
   }

   private float confidenceByOtherCuis() {
      // Discount by 25% every conflicting cui with the same text.
      return Math.max( 0f, 1f - getOtherCuisCount()*.25f );
   }

   private float confidenceByCapsTypes() {
      int max = 0;
      int score = _candidateTerm.getTokens().length;
      for ( int i=0; i<_candidateTerm.getTokens().length; i++ ) {
         final CapsType candidate = _candidateTerm.getCapsType( i );
         final CapsType docText = _docTextTokens.get( i )
                                                .getCapsType();
         switch ( candidate ) {
            case CAPITALIZED: {
               max += 10;
               if ( docText == CAPITALIZED ) {
                  score += 9;
               } else if ( i == 0 && docText == LOWER && !_startsSentence ) {
                  score += 4;
               }
            }
            case UPPER: {
               max += 15;
               if ( docText == UPPER ) {
                  score += 14;
               }
            }
            case MIXED: {
               max += 20;
               if ( docText == MIXED ) {
                  score += getMixMatch( _docTextTokens.get( i ).getText(), _candidateTerm.getTokens()[ i ] );
               }
            }
            case LOWER: {
               max += 5;
               if ( docText == LOWER ) {
                  score += 4;
               } else if ( i == 0 && docText == CAPITALIZED && _startsSentence ) {
                  score += 2;
               }
            } case NO_CHARACTER: {
               max += 1;
            }
         }
      }
      return (float)score / (float)max;
   }


   static private int getMixMatch( final String text1, final String text2 ) {
      final char[] chars1 = text1.toCharArray();
      final char[] chars2 = text2.toCharArray();
      double match = 0;
      for ( int i=0; i<chars1.length; i++ ) {
         match += chars1[ i ] == chars2[ i ] ? 1 : 0;
      }
      return (int)Math.ceil( 19 * match / chars1.length );
   }

}
