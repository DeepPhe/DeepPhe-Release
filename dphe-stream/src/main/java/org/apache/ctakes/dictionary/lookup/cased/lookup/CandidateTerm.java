package org.apache.ctakes.dictionary.lookup.cased.lookup;


import jdk.nashorn.internal.ir.annotations.Immutable;

import java.util.Arrays;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
@Immutable
final public class CandidateTerm {
   private final String _cui;
   private final String[] _tokens;
   private final int _lookupIndex;

   private final short _topRank;
   private final short _entryCounts;
   private final short _secondRank;
   private final short _secondCounts;
   private final short _variantTopRank;
   private final short _variantEntryCounts;
   private final short _otherCuisCount;
   private final String[] _vocabCodes;

   final private int _hashCode;


   public CandidateTerm(
         final String cui,
         final String[] tokens,
         final short lookupIndex,
         final short topRank,
         final short entryCounts,
         final short secondRank,
         final short secondCounts,
         final short variantTopRank,
         final short variantEntryCounts,
         final short otherCuisCount,
         final String[] vocabCodes ) {
      _cui = cui;
      _tokens = tokens;
      _lookupIndex = lookupIndex;
      _topRank = topRank > 0 ? topRank : variantTopRank;
      _entryCounts = entryCounts > 0 ? entryCounts : 1;
      _secondRank = secondRank;
      _secondCounts = secondCounts;
      _variantTopRank = variantTopRank;
      _variantEntryCounts = variantEntryCounts;
      _otherCuisCount = otherCuisCount;
      _vocabCodes = vocabCodes;
      _hashCode = cui.hashCode() + Arrays.hashCode( tokens );
   }

   public CandidateTerm(
         final String cui,
         final String[] tokens,
         final short lookupIndex,
         final short topRank,
         final short entryCounts ) {
      _cui = cui;
      _tokens = tokens;
      _lookupIndex = lookupIndex;
      _topRank = topRank;
      _entryCounts = entryCounts;
      _secondRank = 0;
      _secondCounts = 0;
      _variantTopRank = 0;
      _variantEntryCounts = 0;
      _otherCuisCount = 0;
      _vocabCodes = new String[0];
      _hashCode = cui.hashCode() + Arrays.hashCode( tokens );
   }


   /**
    * @return umls cui for the term
    */
   public String getCui() {
      return _cui;
   }

   /**
    * @return each token in the term as a separate String
    */
   public String[] getTokens() {
      return _tokens;
   }

   public CapsType getCapsType( final int index ) {
      return CapsType.getCapsType( _tokens[ index ] );
   }

   public int getLookupIndex() {
      return _lookupIndex;
   }

   public int getSuffixLength() {
      return _tokens.length - _lookupIndex - 1;
   }

   public short getTopRank() {
      return _topRank;
   }

   public short getEntryCounts() {
      return _entryCounts;
   }

   public short getSecondRank() {
      return _secondRank;
   }

   public short getSecondCounts() {
      return _secondCounts;
   }

   public short getVariantTopRank() {
      return _variantTopRank;
   }

   public short getVariantEntryCounts() {
      return _variantEntryCounts;
   }

   public short getOtherCuisCount() {
      return _otherCuisCount;
   }

   public String[] getVocabCodes() {
      return _vocabCodes;
   }

   public boolean isOutsideWindow( final int windowLength, final int windowLookupIndex ) {
      if ( _tokens.length == 1 ) {
         return false;
      }
      if ( _lookupIndex > windowLookupIndex ) {
         // prefix is longer than window lookup index
         return true;
      }
      // check if suffix is longer than window allows
      return ( windowLength - windowLookupIndex ) < ( _tokens.length - _lookupIndex );
   }

   public boolean isMatch( final List<LookupToken> windowTokens, final int windowLookupIndex ) {
      int windowTokenIndex = windowLookupIndex - _lookupIndex;
      for ( final String token : _tokens ) {
         if ( !windowTokens.get( windowTokenIndex )
                           .equalsIgnoreCase( token ) ) {
            return false;
         }
         windowTokenIndex++;
      }
      // the token normal matched
      return true;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return value instanceof CandidateTerm
             && value.hashCode() == hashCode()
             && ( (CandidateTerm) value ).getCui().equals( getCui() )
             && Arrays.equals( ((CandidateTerm) value ).getTokens(), getTokens() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _hashCode;
   }

}
