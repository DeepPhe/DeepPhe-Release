package org.apache.ctakes.ner.term;

import org.apache.ctakes.core.util.StringUtil;

import java.util.Arrays;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
final public class TermCandidate {

   private final String _uri;
   private final int _rareIndex;
   private final String[] _tokens;
   final private int _hashCode;

   public TermCandidate( final String uri,
                         final int rareIndex,
                         final String fullText ) {
      this( uri, rareIndex, StringUtil.fastSplit( fullText, ' ' ) );
   }

   public TermCandidate( final String uri,
                         final int rareIndex,
                         final String[] tokens ) {
      _uri = uri;
      _rareIndex = rareIndex;
      _tokens = tokens;
      _hashCode = (uri + Arrays.hashCode( tokens )).hashCode();
   }

   public String getUri() {
      return _uri;
   }

   public String[] getTokens() {
      return _tokens;
   }

   public int getTokenCount() {
      return _tokens.length;
   }

   public int getRareIndex() {
      return _rareIndex;
   }

   public int getPrefixLength() {
      return _rareIndex;
   }

   public int getSuffixLength() {
      return _tokens.length - _rareIndex - 1;
   }

   public boolean fitsInSentence( final int indexInSentence, final int sentenceLength ) {
      return prefixFitsInSentence( indexInSentence ) && suffixFitsInSentence( indexInSentence, sentenceLength );
   }

   private boolean prefixFitsInSentence( final int indexInSentence ) {
      return indexInSentence >= _rareIndex;
   }

   private boolean suffixFitsInSentence( final int indexInSentence, final int sentenceLength ) {
      return indexInSentence + getSuffixLength() < sentenceLength;
   }

//   public String[] getPrefixes() {
//      if ( _rareIndex == 0 ) {
//         return new String[0];
//      }
//      final String[] prefixes = new String[_rareIndex];
//      System.arraycopy( _tokens, 0, prefixes, 0, _rareIndex );
//      return prefixes;
//   }
//
//   public String[] getSuffixes() {
//      int suffixLength = getSuffixLength();
//      if ( suffixLength == 0 ) {
//         return new String[0];
//      }
//      final String[] suffixes = new String[suffixLength];
//      System.arraycopy( _tokens, _rareIndex+1, suffixes, 0, suffixLength );
//      return suffixes;
//   }
//
//   /**
//    * Preferred method
//    * @param tokenInSentence -
//    * @param indexInSentence -
//    * @return t/f
//    */
//   public boolean isLookupTokenMatch( final String tokenInSentence, final int indexInSentence ) {
//      if ( indexInSentence < _rareIndex ) {
//         // shortcut
//         return false;
//      }
//      return _tokens[ _rareIndex ].equalsIgnoreCase( tokenInSentence );
//   }

   public boolean isPrefixMatch( final List<LookupToken> sentenceTokens, final int indexInSentence ) {
      if ( _rareIndex == 0 ) {
         return true;
      }
      if ( !prefixFitsInSentence( indexInSentence) ) {
         return false;
      }
      final int sentenceTokenOffset = indexInSentence - _rareIndex;
      for ( int i=0; i<_rareIndex; i++ ) {
         if ( !_tokens[i].equalsIgnoreCase( sentenceTokens.get( sentenceTokenOffset+i ).getText() ) ) {
            return false;
         }
      }
      return true;
   }

   public boolean isSuffixMatch( final List<LookupToken> sentenceTokens, final int indexInSentence ) {
      final int suffixLength = getSuffixLength();
      if ( suffixLength == 0 ) {
         return true;
      }
      if ( !suffixFitsInSentence( indexInSentence, sentenceTokens.size() ) ) {
         return false;
      }
      for ( int i=0; i<suffixLength; i++ ) {
         if ( !_tokens[_rareIndex+i+1].equalsIgnoreCase( sentenceTokens.get( indexInSentence+i+1 ).getText() ) ) {
            return false;
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return value instanceof TermCandidate
            && value.hashCode() == hashCode()
            && _uri.equals( ((TermCandidate) value)._uri );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _hashCode;
   }

}
