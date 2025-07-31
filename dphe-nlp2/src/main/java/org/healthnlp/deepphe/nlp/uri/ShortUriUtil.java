package org.healthnlp.deepphe.nlp.uri;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.ner.tokenizer.CasedTokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * TODO - implement uri-to-text.
 *
 * @author SPF , chip-nlp
 * @since {11/5/2023}
 */
final public class ShortUriUtil {

   private ShortUriUtil() {}

   static private final Collection<Pair<String>> CHAR_TO_CODE
         = Arrays.asList(
         new Pair<>( "~", "_tld_" ),
         new Pair<>( "!", "_bng_" ),
         new Pair<>( "?", "_qry_" ),
         new Pair<>( "@", "_at_" ),
         new Pair<>( "#", "_hsh_" ),
         new Pair<>( "$", "_dlr_" ),
         new Pair<>( "%", "_pct_" ),
         new Pair<>( "^", "_crt_" ),
         new Pair<>( "&", "_and_" ),
         new Pair<>( "*", "_str_" ),
         new Pair<>( "-", "_sub_" ),
         new Pair<>( "+", "_add_" ),
         new Pair<>( "=", "_eql_" ),
         new Pair<>( ":", "_cln_" ),
         new Pair<>( ";", "_sem_" ),
         new Pair<>( "'", "_qt_" ),
         new Pair<>( "\"", "_dqt_" ),
         new Pair<>( "`", "_bqt_" ),
         new Pair<>( ",", "_cma_" ),
         new Pair<>( ".", "_dot_" ),
         new Pair<>( "<", "_lt_" ),
         new Pair<>( ">", "_gt_" ),
         new Pair<>( "{", "_lbc_" ),
         new Pair<>( "}", "_rbc_" ),
         new Pair<>( "[", "_lbt_" ),
         new Pair<>( "]", "_rbt_" ),
         new Pair<>( "(", "_lpn_" ),
         new Pair<>( ")", "_rpn_" ),
         new Pair<>( "\\", "_bsl_" ),
         new Pair<>( "/", "_sl_" ) );

   static public String createShortUri( final String preferredText ) {
      final String keepPrefixSuffix = keepPrefixSuffix( preferredText );
      final String camelCase = camelCase( keepPrefixSuffix );
      String noSym = camelCase;
      for ( Pair<String> replacement : CHAR_TO_CODE ) {
         noSym = noSym.replace( replacement.getValue1(), replacement.getValue2() );
      }
      if ( Character.isDigit( noSym.charAt( 0 ) ) ) {
         return "n_" + noSym;
      }
      if ( noSym.charAt( 0 ) == '_' ) {
         return "s" + noSym;
      }
      return noSym;
   }

   static private String keepPrefixSuffix( final String pt ) {
      final String[] tokens = StringUtil.fastSplit( pt, ' ' );
      final List<String> words = new ArrayList<>( tokens.length );
      for ( String token : tokens ) {
         final String word = replaceDashSuffix( replacePrefixDash( token ) );
         words.add( word );
      }
      return String.join( " ", words );
   }


   static private String replacePrefixDash( final String text ) {
      final String lower = text.toLowerCase();
      for ( String prefix : CasedTokenizer.PREFIXES ) {
         if ( lower.equals( prefix ) ) {
            return text;
         }
         if ( lower.startsWith( prefix ) ) {
            return text.substring( 0, prefix.length() - 1 ) + "_" + text.substring( prefix.length() );
         }
      }
      return text;
   }

   static private String replaceDashSuffix( final String text ) {
      final String lower = text.toLowerCase();
      for ( String suffix : CasedTokenizer.SUFFIXES ) {
         if ( lower.equals( suffix ) ) {
            return text;
         }
         if ( lower.endsWith( suffix ) ) {
            final int minusSuffix = text.length() - suffix.length();
            return text.substring( 0, minusSuffix ) + "_" + text.substring( minusSuffix + 1 );
         }
      }
      return text;
   }

   static private String camelCase( final String pt ) {
      final StringBuilder sb = new StringBuilder( pt.length() );
      for ( String token : StringUtil.fastSplit( pt, ' ' ) ) {
         if ( token.isEmpty() ) {
            System.out.println( "---" + pt + "---" );
            continue;
         }
         sb.append( Character.toUpperCase( token.charAt( 0 ) ) );
         if ( token.length() > 1 ) {
            sb.append( token.substring( 1 ) );
         }
      }
      return sb.toString();
   }

   static public String uriToText( final String uri ) {
      String spaced = camelSpace( uri );
      for ( Pair<String> replacement : CHAR_TO_CODE ) {
         spaced = spaced.replace( replacement.getValue2(), " " + replacement.getValue1() + " " );
      }
      if ( spaced.startsWith( "n_" ) ) {
         spaced = spaced.substring( 2 );
      }
      return spaced.replace( '_', '-' );
   }

   // Terrible, but other than looking up the concept what can we do?
   static private String camelSpace( final String uri ) {
      final StringBuilder sb = new StringBuilder( uri.length() );
      final char[] chars = uri.toCharArray();
      sb.append( chars[ 0 ] );
      for ( int i=1; i<chars.length; i++ ) {
         if ( Character.isUpperCase( chars[ i ] ) ) {
            sb.append( ' ' );
         }
         sb.append( chars[ i ] );
      }
      return sb.toString();
   }

   public static void main( String[] args ) {
      final String text = "Chemo/immuno/hormone Therapy Regimen";
      System.out.println( createShortUri( text ) );
      // Going backwards doesn't work because of spaces.
      System.out.println( uriToText( createShortUri( text ) ) );
   }


}
