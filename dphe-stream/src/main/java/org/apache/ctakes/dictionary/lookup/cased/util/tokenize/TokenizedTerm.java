package org.apache.ctakes.dictionary.lookup.cased.util.tokenize;

import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.dictionary.lookup.cased.lookup.CapsType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
@Immutable
final public class TokenizedTerm {

   static private final Collection<String> PREFIXES = new HashSet<>( Arrays.asList(
         "e-",
         "a-",
         "u-",
         "x-",
         "agro-",
         "ante-",
         "anti-",
         "arch-",
         "be-",
         "bi-",
         "bio-",
         "co-",
         "counter-",
         "cross-",
         "cyber-",
         "de-",
         "eco-",
         "ex-",
         "extra-",
         "inter-",
         "intra-",
         "macro-",
         "mega-",
         "micro-",
         "mid-",
         "mini-",
         "multi-",
         "neo-",
         "non-",
         "over-",
         "pan-",
         "para-",
         "peri-",
         "post-",
         "pre-",
         "pro-",
         "pseudo-",
         "quasi-",
         "re-",
         "semi-",
         "sub-",
         "super-",
         "tri-",
         "ultra-",
         "un-",
         "uni-",
         "vice-",
         // From email from Colin Warner <colinw@ldc.upenn.edu> on 7/25/2010
         "electro-",
         "gasto-",
         "homo-",
         "hetero-",
         "ortho-",
         "phospho-" ) );

   static private final Collection<String> SUFFIXES = new HashSet<>( Arrays.asList(
         "-esque",
         "-ette",
         "-fest",
         "-fold",
         "-gate",
         "-itis",
         "-less",
         "-most",
         "-o-torium",
         "-rama",
         "-wise" ) );

   static private final Collection<String> UPPER_PREFIXES = PREFIXES.stream()
                                                                    .map( String::toUpperCase )
                                                                    .collect( Collectors.toSet() );

   static private final Collection<String> UPPER_SUFFIXES = SUFFIXES.stream()
                                                                    .map( String::toUpperCase )
                                                                    .collect( Collectors.toSet() );


   final private String[] _tokens;
   final private String _cui;
   final private int _hashcode;

   public TokenizedTerm( final String cui, final String text ) {
      _cui = cui;
      _tokens = getTermTokens( text );
      _hashcode = (cui + "_" + text).hashCode();
   }

   public String getCui() {
      return _cui;
   }

   public String[] getTokens() {
      return _tokens;
   }

   public CapsType[] getCapsTypes() {
      final CapsType[] capsTypes = new CapsType[ _tokens.length ];
      for ( int i=0; i< _tokens.length; i++ ) {
         capsTypes[ i ] = CapsType.getCapsType( _tokens[ i ] );
      }
      return capsTypes;
   }

   static private String[] getTermTokens( final String text ) {
      if ( text.isEmpty() ) {
         return new String[ 0 ];
      }
      return Arrays.stream( text.split( "\\s+" ) )
                   .map( TokenizedTerm::getTokens )
                   .flatMap( Collection::stream )
                   .toArray( String[]::new );
   }

   // TODO should this be exactly the same as getTokens in TextTokenizer (dictionary gui code)  ? probably ...
   static private List<String> getTokens( final String word ) {
      final List<String> tokens = new ArrayList<>();
      final StringBuilder sb = new StringBuilder();
      final int count = word.length();
      for ( int i = 0; i < count; i++ ) {
         final char c = word.charAt( i );
         if ( Character.isLetterOrDigit( c ) ) {
            sb.append( c );
            continue;
         }
         if ( c == '-' && (isPrefix( sb.toString() ) || isSuffix( word, i + 1 )) ) {
            // what precedes is a prefix or what follows is a suffix so append the dash to the current word and move on
            sb.append( c );
            continue;
         }
         if ( (c == '\'' && isOwnerApostrophe( word, i + 1 ))
              || (c == '.' && isNumberDecimal( word, i + 1 )) ) {
            // what follows is an 's or .# so add the preceding and move on
            if ( sb.length() != 0 ) {
               tokens.add( createToken( sb ) );
               sb.setLength( 0 );
            }
            sb.append( c );
            continue;
         }
         // Wasn't a special symbol for consideration, so add the previous and symbol separately
         if ( sb.length() != 0 ) {
            tokens.add( createToken( sb ) );
            sb.setLength( 0 );
         }
         tokens.add( "" + c );
      }
      if ( sb.length() != 0 ) {
         tokens.add( createToken( sb ) );
      }
      return tokens;
   }

   static private String createToken( final StringBuilder sb ) {
      return sb.toString();
   }

   static private boolean isPrefix( final String word ) {
      return PREFIXES.contains( word + "-" ) || UPPER_PREFIXES.contains( word + "-" );
   }

   static private boolean isSuffix( final String word, final int startIndex ) {
      if ( word.length() <= startIndex ) {
         return false;
      }
      final String nextCharTerm = getNextCharTerm( word.substring( startIndex ) );
      if ( nextCharTerm.isEmpty() ) {
         return false;
      }
      return SUFFIXES.contains( "-" + nextCharTerm ) || UPPER_SUFFIXES.contains( "-" + nextCharTerm );
   }

   static private boolean isOwnerApostrophe( final CharSequence word, final int startIndex ) {
      return word.length() == startIndex + 1 && word.charAt( startIndex ) == 's';
   }

   static private boolean isNumberDecimal( final CharSequence word, final int startIndex ) {
      // Bizarre scenario in which ctakes tokenizes ".2" as a fraction, but not ".22"
      return word.length() == startIndex + 1 && Character.isDigit( word.charAt( startIndex ) );
   }

   static private String getNextCharTerm( final String word ) {
      final int count = word.length();
      for ( int i = 0; i < count; i++ ) {
         final char c = word.charAt( i );
         if ( !Character.isLetterOrDigit( c ) ) {
            return word.substring( 0, i );
         }
      }
      return word;
   }

   public boolean equals( final Object value ) {
      return value instanceof TokenizedTerm
             && Arrays.equals( _tokens, ((TokenizedTerm)value)._tokens )
             && _cui.equals( ((TokenizedTerm)value)._cui );
   }

   public int hashCode() {
      return _hashcode;
   }


}
