package org.apache.ctakes.ner.tokenizer;


import org.apache.ctakes.core.util.StringUtil;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * @author SPF , chip-nlp
 * @since {1/31/2022}
 */
final public class CasedTokenizer {

   private CasedTokenizer() {
   }

   // These rules come from https://clear.colorado.edu/compsem/documents/treebank_guidelines.pdf
   // Bracketing Biomedical Text: An Addendum to Penn Treebank II Guidelines
   // as well as https://www.cis.upenn.edu/~bies/manuals/root.pdf
   // Bracketing Guidelines for the Treebank II-style Penn Treebank Project
   // Guidelines include person titles etc. mtht should not end sentences, "Gov.", "i.e.", "min." etc.
//  https://catalog.ldc.upenn.edu/docs/LDC2011T03/treebank/english-translation-treebank-guidelines.pdf
   // URLs, e-mail addresses, and file names are kept as single tokens.
   // http://seer.cancer.gov/csr/1975_2005/index.html
   // info@ndif.org

   // Token types are not POS tags, so they should be easy to figure out.

   static public final Collection<String> PREFIXES = new HashSet<>( Arrays.asList(
         "e-",
         "a-",
         "u-",
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
         // From email from Colin Warner <colinw@ldc.upenn.edu> on 7/25/2010.
         "electro-",
         "gastro-",
         "homo-",
         "hetero-",
         "ortho-",
         "phospho-",
         // From paper(s) There are repeats from copy and paste.
         "adeno-", "aorto-", "ambi-", "axio-", "be-", "bibio-", "broncho-",
         "centi-",
         "circum-", "cis-", "colo-", "contra-", "cortico-",
         "cran-", "crypto-", "deca-", "demi-", "dis-",
         "ennea-", "veno-", "ventriculo-", "ferro-", "giga-", "hepta-", "hemi-",
         "hypo-", "hexa-", "in-", "ideo-", "idio-", "infra-", "iso-", "judeo-",
         "mono-", "musculo-", "neuro-", "nitro-", "medi-", "milli-", "novem-",
         "octa-", "octo-", "paleo-", "pheno-", "penta-", "pica-", "poly-", "preter-",
         "pneumo-", "quadri-", "quinque-", "pelvi-", "recto-", "salpingo-", "sero-",
         "sept-", "soci-", "supra-", "sur-", "tele-", "tera-", "tetra-", "uber-"
   ) );

   static public final Collection<String> SUFFIXES = new HashSet<>( Arrays.asList(
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
         "-wise",
         // From paper(s) There are repeats from copy and paste.
         "-able", "-ahol", "-aholic", "-ation", "-centric", "-cracy", "-crat", "-dom",
         "-based", "-er", "-ery", "-ful", "-gon", "-hood", "-ian", "-ible", "-ing",
         "-isation", "-ise", "-ising", "-ism", "-ist", "-ization", "-ize", "-izing",
         "-logist", "-logy", "-ly"
   ) );
   // Should add "-cell" for things like "B-cell"

   static private final Collection<String> SLASH_ABBREVIATIONS = new HashSet<>( Arrays.asList(
         // because, discontinue, ultrasound, without
         "b/c", "d/c", "u/s", "w/o" ) );

   // Don't worry about not splitting dates, phone numbers, URLs, email addresses and file paths.
   // They shouldn't be in Dictionary synonyms.

   // Units are dangerous.  Splitting some of these from numbers may mess up things like genes and chemicals.
   static private final Collection<String> PTB_UNITS = new HashSet<>( Arrays.asList(
         "am", "cc", "cm", "d", "days", "kg", "km", "l", "m", "mcg", "mg", "min", "minutes", "ml",
         "mm", "mos", "pm", "units", "y", "yr", "yrs" ) );
   static private final Collection<String> DPHE_UNITS = new HashSet<>( Arrays.asList(
         "g", "gm", "ug", "mcl", "cl", "dl", "ul", "ci", "mci", "mcci", "uci", "oz", "percent", "pt", "lb", "qt",
         "cm2", "sq", "tsp", "mm2", "um2",
         "mol", "mole", "mmol", "millimole", "umol", "micromole", "dl", "gal", "gram", "gsm", "iu",
         "j", "joule", "joules", "mj", "mjoule", "cgy", "centigray", "cge", "gy", "gray", "mgy", "milligray",
         "mrad", "mrd", "rad", "bq", "mbq", "kbq", "c", "r", "rem", "sv", "cps", "dpm", "dps",
         "aci", "cci", "dci", "fci", "nci", "pci", "gbq", "kbq", "mbq",
         "h", "hr", "hrs", "cal", "deg", "cel", "s", "sec", "us", "usec", "ms", "msec", "mins", "mo",
         "ns", "nsec", "ms2", "wk", "m3", "drp", "gtt"

   ) );
   // Fr is actually from PTB as their only case-sensitive unit.
   static private final Collection<String> DPHE_CAP_UNITS = new HashSet<>( Arrays.asList(
         "C", "F", "Fr"
   ) );

   // Multi-token numbers might also be dangerous for genes and chems.  e.g. "3 5/8"
   // Units abbreviations with period may be dangerous, e.g. lb.

   static private final Map<String, Integer> WORD_SPLITS = new HashMap<>();
   static private final Collection<String> ABBREVIATIONS = new HashSet<>();
   static private final Collection<String> SPOKEN_DASH = new HashSet<>();

   static {
      WORD_SPLITS.put( "cannot", 3 );
      WORD_SPLITS.put( "gonna", 3 );
      WORD_SPLITS.put( "gotta", 3 );
      WORD_SPLITS.put( "lemme", 3 );
      WORD_SPLITS.put( "more'n", 4 );
      WORD_SPLITS.put( "'tis", 2 );
      WORD_SPLITS.put( "'twas", 2 );
      WORD_SPLITS.put( "wanna", 3 );
//      WORD_SPLITS.put( "whaddya", Arrays.asList( "wha", "dd", "ya" ) );
//      WORD_SPLITS.put( "whatcha", Arrays.asList( "wha", "t", "cha" ) );
      WORD_SPLITS.put( "can't", 2 );
      WORD_SPLITS.put( "won't", 2 );
      WORD_SPLITS.put( "don't", 2 );
      WORD_SPLITS.put( "couldn't", 5 );
      WORD_SPLITS.put( "wouldn't", 5 );
      WORD_SPLITS.put( "shouldn't", 6 );
      WORD_SPLITS.put( "isn't", 2 );
      WORD_SPLITS.put( "wasn't", 3 );
      WORD_SPLITS.put( "weren't", 4 );
      WORD_SPLITS.put( "hadn't", 3 );
      WORD_SPLITS.put( "didn't", 3 );
      ABBREVIATIONS.add( "A.D." );
      ABBREVIATIONS.add( "Adm." );
      ABBREVIATIONS.add( "Ave." );
      ABBREVIATIONS.add( "Brig." );
      ABBREVIATIONS.add( "Capt." );
      ABBREVIATIONS.add( "Cmdr." );
      ABBREVIATIONS.add( "C.N.P." );
      ABBREVIATIONS.add( "Co." );
      ABBREVIATIONS.add( "Col." );
      ABBREVIATIONS.add( "Cpl." );
      ABBREVIATIONS.add( "cm." );
      ABBREVIATIONS.add( "D.O." );
      ABBREVIATIONS.add( "Dr." );
      ABBREVIATIONS.add( "Drs." );
      ABBREVIATIONS.add( "e.g." );
      ABBREVIATIONS.add( "etc." );
//      ABBREVIATIONS.add( "‘et al.’" );
      ABBREVIATIONS.add( "Fig." );
      ABBREVIATIONS.add( "Fr." );
      ABBREVIATIONS.add( "Ft." );
      ABBREVIATIONS.add( "Gen." );
      ABBREVIATIONS.add( "Gov." );
      ABBREVIATIONS.add( "i.e." );
      ABBREVIATIONS.add( "Lt." );
      ABBREVIATIONS.add( "lt." );
      ABBREVIATIONS.add( "Ltd." );
      ABBREVIATIONS.add( "Maj." );
      ABBREVIATIONS.add( "M.B.B.S." );
      ABBREVIATIONS.add( "min." );
      ABBREVIATIONS.add( "Mr." );
      ABBREVIATIONS.add( "Mrs." );
      ABBREVIATIONS.add( "Ms." );
      ABBREVIATIONS.add( "MI." );
      ABBREVIATIONS.add( "M.D." );
      ABBREVIATIONS.add( "No." );
      ABBREVIATIONS.add( "no." );
      ABBREVIATIONS.add( "N.P." );
      ABBREVIATIONS.add( "P.A.-C." );
      ABBREVIATIONS.add( "Ph.D." );
      ABBREVIATIONS.add( "P.O." );
      ABBREVIATIONS.add( "Prof." );
      ABBREVIATIONS.add( "Prop." );
      ABBREVIATIONS.add( "Pte." );
      ABBREVIATIONS.add( "Pvt." );
      ABBREVIATIONS.add( "Rep." );
      ABBREVIATIONS.add( "Reps." );
      ABBREVIATIONS.add( "Rev." );
      ABBREVIATIONS.add( "R.N." );
      ABBREVIATIONS.add( "Rt." );
      ABBREVIATIONS.add( "rt." );
      ABBREVIATIONS.add( "Sen." );
      ABBREVIATIONS.add( "Sens." );
      ABBREVIATIONS.add( "Sgt." );
      ABBREVIATIONS.add( "St." );
      ABBREVIATIONS.add( "Ste." );
      ABBREVIATIONS.add( "tr." );
      ABBREVIATIONS.add( "U.S." );
      ABBREVIATIONS.add( "vs." );
      // dphe
      SPOKEN_DASH.add( "x-ray" );
      SPOKEN_DASH.add( "x-rays" );
      SPOKEN_DASH.add( "x-rayed" );
      SPOKEN_DASH.add( "mm-hm" );
      SPOKEN_DASH.add( "mm-mm" );
      SPOKEN_DASH.add( "uh-huh" );
      SPOKEN_DASH.add( "uh-oh" );
      SPOKEN_DASH.add( "o-kay" );
   }

   static private final Collection<String> CONTRACTED_ENDS
         = new HashSet<>( Arrays.asList( "s", "ve", "re", "ll", "d", "S", "VE", "RE", "LL", "D" ) );

   static public String[] getTokens( final String text ) {
      if ( text.isEmpty() ) {
         return new String[ 0 ];
      }
      final char[] chars = text.toCharArray();
      final List<String> tokens = new ArrayList<>();
      final StringBuilder sb = new StringBuilder();
      for ( final char c : chars ) {
         if ( Character.isWhitespace( c ) ) {
            if ( sb.length() != 0 ) {
               tokens.addAll( getWordTokens( sb.toString() ) );
               sb.setLength( 0 );
            }
         } else {
            sb.append( c );
         }
      }
      if ( sb.length() != 0 ) {
         tokens.addAll( getWordTokens( sb.toString() ) );
      }
      return tokens.toArray( new String[ 0 ] );
   }



   static private List<String> splitAllTokens( final List<String> tokens, final String wordOnly,
                                              final boolean endPunctuation, final char endChar ) {
      final char[] chars = wordOnly.toCharArray();
      final StringBuilder sb = new StringBuilder();
      for ( final char c : chars ) {
         if ( Character.isLetterOrDigit( c ) ) {
            sb.append( c );
            continue;
         }
         if ( sb.length() > 0 ) {
            tokens.add( sb.toString() );
            sb.setLength( 0 );
         }
         tokens.add( String.valueOf( c ) );
      }
      if ( sb.length() > 0 ) {
         tokens.add( sb.toString() );
      }
      if ( endPunctuation ) {
         tokens.add( String.valueOf( endChar ) );
      }
      return tokens;
   }

   static private final Collection<String> WHOLE_BEGINS = Arrays.asList(
         "httpd://", "https://", "http://", "ftp://"
   );

   static private final Collection<String> WHOLE_ENDS = Arrays.asList(
         ".com", ".org", ".edu", ".txt", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
         ".jpg", ".png", ".gif", ".xml", ".json", ".java", ".py"
   );


   static private List<String> getWordTokens( final List<String> tokens, final String casedRemainder,
                                              final String wordOnly, final String lowerOnly,
                                              final boolean endPunctuation, final char endChar ) {
      if ( casedRemainder.isEmpty() ) {
         return tokens;
      }
      if ( casedRemainder.length() == 1 ) {
         tokens.add( casedRemainder );
         return tokens;
      }
      if ( wordOnly.length() == 1 ) {
         tokens.add( wordOnly );
         tokens.add( String.valueOf( endChar ) );
         return tokens;
      }
      // WORD_SPLITS do not contain any end punctuation.
      if ( WORD_SPLITS.containsKey( lowerOnly ) ) {
         final int splitIndex = WORD_SPLITS.get( lowerOnly );
         return createTwoTokens( tokens, wordOnly, splitIndex, endPunctuation, endChar );
      }
      if ( lowerOnly.length() > 8 ) {
         for ( String end : WHOLE_ENDS ) {
            if ( lowerOnly.endsWith( end ) ) {
               return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
            }
         }
         if ( lowerOnly.length() > 12 ) {
            for ( String begin : WHOLE_BEGINS ) {
               if ( lowerOnly.startsWith( begin ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               }
            }
         }
      }
      final char[] chars = wordOnly.toCharArray();
//      final StringBuilder sb = new StringBuilder();
      boolean haveDigit = false;
      boolean haveLetter = false;
      for ( int i = 0; i < chars.length; i++ ) {
         final char c = chars[ i ];
         if ( Character.isDigit( c ) ) {
//            sb.append( c );
            haveDigit = true;
            continue;
         }
         if ( Character.isLetter( c ) ) {
            if ( haveDigit && !haveLetter ) {
               if ( satisfiesUpLow( wordOnly, lowerOnly, i, CasedTokenizer::isUnit ) ) {
                  return createTwoTokens( tokens, wordOnly, i, endPunctuation, endChar );
               }
            }
            haveLetter = true;
//            sb.append( c );
            continue;
         }
         if ( i == 0 ) {
            if ( c == '.' ) {
               if ( casedRemainder.equals( "..." ) ) {
                  tokens.add( casedRemainder );
                  return tokens;
               }
               return splitAllTokens( tokens, wordOnly, endPunctuation, endChar );
            } else if ( !endPunctuation && c == '(' && casedRemainder.equals( "(...)" ) ) {
               tokens.add( casedRemainder );
               return tokens;
            } else if ( endPunctuation && c == '(' && wordOnly.equals( "(...)" ) ) {
               tokens.add( wordOnly );
               tokens.add( String.valueOf( endChar ) );
               return tokens;
            }
            // Wasn't a special symbol for consideration, so add the previous and symbol separately
            return createTokens( tokens, casedRemainder, wordOnly, lowerOnly, 0, endPunctuation, endChar );
         }
         // At this point start checking
         if ( c == '-' ) {
            final String[] splits = StringUtil.fastSplit( lowerOnly, '-' );
            if ( haveDigit && !haveLetter ) {
               if ( satisfiesSplits( splits, CasedTokenizer::isIdentifier )
                     || satisfiesSplits( splits, CasedTokenizer::isDate ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               }
            } else if ( !haveDigit ) {
               if ( SPOKEN_DASH.contains( lowerOnly ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               }
               if ( satisfiesSplits( splits, CasedTokenizer::isPrefix )
                     || satisfiesSplits( splits, CasedTokenizer::isSuffix ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               }
            }
            return splitAllTokens( tokens, wordOnly, endPunctuation, endChar );
         } else if ( c == '/' ) {
            if ( i == 1 && haveLetter && SLASH_ABBREVIATIONS.contains( lowerOnly ) ) {
               return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
            }
            final String[] splits = StringUtil.fastSplit( lowerOnly, '/' );
            if ( haveDigit && !haveLetter ) {
               if ( satisfiesSplits( splits, CasedTokenizer::isDate ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               }
            }
            return splitAllTokens( tokens, wordOnly, endPunctuation, endChar );
         } else if ( c == '\'' ) {
            if ( haveLetter && !haveDigit ) {
               final String[] splits = StringUtil.fastSplit( wordOnly, '\'' );
               if ( satisfiesSplits( splits, CasedTokenizer::isApostropheOwner ) ) {
                  return createApostropheOwner( tokens, splits, endPunctuation, endChar );
               }
            }
            return splitAllTokens( tokens, wordOnly, endPunctuation, endChar );
         } else if ( c == '.' ) {
            if ( haveLetter && !haveDigit ) {
               if ( satisfiesWhole( wordOnly, CasedTokenizer::isAbbreviation ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               } else if ( satisfiesWhole( casedRemainder, CasedTokenizer::isAbbreviation ) ) {
                  return addSingleToken( tokens, casedRemainder );
               }
            }
            if ( casedRemainder.substring( i ).equals( "..." ) ) {
               return createTwoTokens( tokens, casedRemainder, i, false, endChar );
            }
            if ( !haveLetter ) {
               final String[] splits = StringUtil.fastSplit( wordOnly, '.' );
               if ( satisfiesSplits( splits, CasedTokenizer::isDecimalNumber ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               }
            }
            return splitAllTokens( tokens, wordOnly, endPunctuation, endChar );
         } else if ( c == ',' ) {
            if ( haveDigit && !haveLetter ) {
               final String[] splits = StringUtil.fastSplit( wordOnly, ',' );
               if ( satisfiesSplits( splits, CasedTokenizer::isCommaNumber ) ) {
                  return addSingleTokenEnded( tokens, wordOnly, endPunctuation, endChar );
               }
            }
            return splitAllTokens( tokens, wordOnly, endPunctuation, endChar );
         } else if ( c == '(' ) {
            if ( casedRemainder.substring( i ).equals( "(...)" ) ) {
               return createTwoTokens( tokens, casedRemainder, i, false, endChar );
            }
         }
         // Wasn't a special symbol for consideration, so add the previous and symbol separately
         return createTokens( tokens, casedRemainder, wordOnly, lowerOnly, i, endPunctuation, endChar );
      }
      if ( endPunctuation ) {
         if ( satisfiesWhole( casedRemainder, CasedTokenizer::isAbbreviation ) ) {
            return addSingleToken( tokens, casedRemainder );
         }
         tokens.add( wordOnly );
         tokens.add( String.valueOf( endChar ) );
      } else {
         tokens.add( casedRemainder );
      }
      return tokens;
   }

//   static private List<String> getSplitTokens( final List<String>tokens, final String[] splits, final char splitChar,
//                                               final boolean endPunctuation, final char endChar ) {
//      for ( int i=0; i<splits.length-1; i++  ) {
//         final String split = splits[i];
//         tokens.addAll( getWordTokens( tokens, split, split, split.toLowerCase(), false, ' ' ) );
//         tokens.add( String.valueOf( splitChar ) );
//      }
//      final String lastSplit = splits[ splits.length-1 ];
//      if ( endPunctuation ) {
//         tokens.addAll( getWordTokens( tokens, lastSplit+endChar,
//               lastSplit, lastSplit.toLowerCase(), true, endChar ) );
//      } else {
//         tokens.addAll( getWordTokens( tokens, lastSplit,
//               lastSplit, lastSplit.toLowerCase(), false, ' ' ) );
//      }
//      return tokens;
//   }


   static private List<String> getWordTokens( final String wholeWord ) {
      if ( wholeWord.length() == 1 ) {
         return Collections.singletonList( wholeWord );
      }
      if ( probableDivider( wholeWord ) ) {
         return Collections.singletonList( wholeWord );
      }
      final List<String> tokens = new ArrayList<>();
      final char[] chars = wholeWord.toCharArray();
      final char endChar = chars[ chars.length-1 ];
      final char beginChar = chars[ 0 ];
      final boolean bracketed = ( beginChar == '[' && endChar == ']' )
            || ( beginChar == '{' && endChar == '}' )
            || ( beginChar == '(' &&endChar == ')' ) || (beginChar == '"' && endChar == '"' )
            || ( beginChar == '\'' && endChar == '\'' );
      if ( bracketed && chars.length > 2 ) {
         tokens.add( String.valueOf( beginChar ) );
         tokens.addAll( getWordTokens( wholeWord.substring( 1,wholeWord.length()-1 ) ) );
         tokens.add( String.valueOf( endChar ) );
         return tokens;
      }
      final boolean endPunctuation = endChar == '.' || endChar == '?' || endChar == '!' || endChar == ','
            || endChar == ';' || endChar == ':';
      String wordOnly = endPunctuation ? wholeWord.substring( 0, wholeWord.length()-1 ) : wholeWord;
      String lowerOnly = wordOnly.toLowerCase();
      return getWordTokens( tokens, wholeWord, wordOnly, lowerOnly, endPunctuation, endChar );
   }


   static private boolean satisfiesWhole( final String word,
                                           final Predicate<String> decider ) {
      return decider.test( word );
   }

  static private boolean satisfiesUpLow( final String wordOnly,
                                     final String lowerOnly,
                                     final int index,
                                     final BiPredicate<String,String> decider ) {
      return decider.test( wordOnly.substring( index ), lowerOnly.substring( index ) );
   }


   static private boolean satisfiesSplits( final String[] splits,
                                           final Predicate<String[]> decider ) {
      return decider.test( splits );
   }


   static private List<String> createTokens( final List<String> tokens,
                                             final String casedWord,
                                             final String wordOnly,
                                             final String lowerOnly,
                                             final int splitIndex,
                                             final boolean endPunctuation,
                                             final char endChar ) {
      if ( splitIndex > 0 ) {
         tokens.add( casedWord.substring( 0, splitIndex ) );
      }
      tokens.add( String.valueOf( casedWord.charAt( splitIndex ) ) );
      final int nextIndex = splitIndex + 1;
      if ( endPunctuation ) {
         if ( nextIndex == wordOnly.length() ) {
            tokens.add( String.valueOf( endChar ) );
            return tokens;
         } else if ( nextIndex == wordOnly.length() - 1 ) {
            tokens.add( String.valueOf( wordOnly.charAt( nextIndex ) ) );
            tokens.add( String.valueOf( endChar ) );
            return tokens;
         }
      } else {
         if ( nextIndex == casedWord.length() ) {
            return tokens;
         } else if ( splitIndex == casedWord.length() - 1 ) {
            tokens.add( casedWord.substring( nextIndex ) );
            return tokens;
         }
      }
      return getWordTokens( tokens,
            casedWord.substring( nextIndex ),
            wordOnly.substring( nextIndex ),
            lowerOnly.substring( nextIndex ),
            endPunctuation,
            endChar );
   }

   static private List<String> createTwoTokens( final List<String> tokens,
                                             final String wordOnly,
                                             final int splitIndex,
                                             final boolean endPunctuation,
                                             final char endChar ) {
      tokens.add( wordOnly.substring( 0, splitIndex ) );
      tokens.add( wordOnly.substring( splitIndex ) );
      if ( endPunctuation ) {
         tokens.add( String.valueOf( endChar ) );
      }
      return tokens;
   }

   static private List<String> createApostropheOwner( final List<String> tokens,
                                                final String[] splits,
                                                final boolean endPunctuation,
                                                final char endChar ) {
      tokens.add( splits[0] );
      tokens.add( '\'' + splits[1] );
      if ( endPunctuation ) {
         tokens.add( String.valueOf( endChar ) );
      }
      return tokens;
   }


   static private List<String> addSingleTokenEnded( final List<String> tokens, final String wordOnly,
                                                    final boolean endPunctuation, final char endChar ) {
      tokens.add( wordOnly );
      if ( endPunctuation ) {
         tokens.add( String.valueOf( endChar ) );
      }
      return tokens;
   }

   static private List<String> addSingleToken( final List<String> tokens, final String casedRemainder ) {
      tokens.add( casedRemainder );
      return tokens;
   }


   static private boolean isUnit( final String word, final String lower ) {
      return PTB_UNITS.contains( lower )
            || DPHE_UNITS.contains( lower )
            || DPHE_CAP_UNITS.contains( word );
      }

   static private boolean isPrefix( final String[] splits ) {
      return splits.length == 2 && PREFIXES.contains( splits[0].toLowerCase() + "-" );
   }

   static private boolean isSuffix( final String[] splits ) {
      return splits.length == 2 && SUFFIXES.contains( "-" + splits[1].toLowerCase() );
   }

   static private boolean isApostropheOwner( final String[] splits ) {
      return splits.length == 2 && CONTRACTED_ENDS.contains( splits[1] );
   }

   static private boolean isAbbreviation( final String word ) {
      return ABBREVIATIONS.contains( word );
   }

   static private boolean isCommaNumber( final String[] splits ) {
      if ( splits[0].length() > 3 ) {
         return false;
      }
      for ( int i=1; i< splits.length-1; i++ ) {
         if ( splits.length != 3 || !allDigits( splits[i] ) ) {
            return false;
         }
      }
      if ( splits[splits.length-1].length() == 3 && allDigits( splits[splits.length-1] ) ) {
         return true;
      }
      final String[] digiSplits = StringUtil.fastSplit( splits[splits.length-1], '.' );
      return digiSplits.length <= 2 && digiSplits[ 0 ].length() == 3 && allDigits( digiSplits );
   }


   static private boolean isDecimalNumber( final String[] splits ) {
      return splits.length == 2 && allDigits( splits[1] );
   }



   // Kept two numbers like 9/16 and 9-16 as non-dates because in a properly-written clinical note
   // they are more likely to represent ranges (70-99 percent) and ratios (grade 4/4).
   static private boolean isDate( final String[] splits ) {
      if ( splits.length != 3 || !allDigits( splits ) ) {
         return false;
      }
      // d/m/yy d/mm/yy dd/m/yy dd/mm/yy   d/m/yyyy d/mm/yyyy dd/mm/yyyy dd/m/yyyy
      if ( isDayMonth( splits[ 0 ], splits[ 1 ] ) && isYear( splits[ 2 ] ) ) {
         return true;
      }
      // yy/m/d yy/m/dd yy/mm/d yy/mm/dd   yyyy/m/d yyyy/m/dd yyyy/mm/d yyyy/mm/dd
      return isYear( splits[ 0 ] ) && isDayMonth( splits[ 1 ], splits[ 2 ] );
   }

   static private boolean isDayMonth( final String split1, final String split2 ) {
      return isOneOrTwo( split1 ) && isOneOrTwo( split2 );
   }

   static private boolean isOneOrTwo( final CharSequence digits ) {
      return digits.length() == 1 || digits.length() == 2;
   }

   static private boolean isYear( final CharSequence digits ) {
      return digits.length() == 2 || digits.length() == 4;
   }

   /**
    *
    * @param splits -
    * @return true if it can be grant or patient identifier 9646-66 9-2282 968-9060
    */
   static private boolean isIdentifier( final String[] splits ) {
      if ( splits.length != 2 ) {
         return false;
      }
      // assume ID needs at least 5 digits
      if ( (splits[ 0 ].length() + splits[ 1 ].length()) < 5 ) {
         return false;
      }
      return allDigits( splits );
   }

   static private boolean allDigits( final String... splits ) {
      for ( String split : splits ) {
         for ( char c : split.toCharArray() ) {
            if ( !Character.isDigit( c ) ) {
               return false;
            }
         }
      }
      return true;
   }

   static private boolean probableDivider( final String word ) {
      if ( word.length() < 4 ) {
         return false;
      }
      // check for === ___ --- ***  ### @@@ !!! ... etc.
      final char[] chars = word.toCharArray();
      final char firstChar = chars[ 0 ];
      if ( Character.isLetterOrDigit( firstChar ) ) {
         return false;
      }
      for ( char c : chars ) {
         if ( c != firstChar ) {
            return false;
         }
      }
      return true;
   }



   public static void main( String[] args ) {
      System.out.println( "I have a C.N.P. in charge of a P.A.-C. for the U.S. vs. my Ph.D. professor in T.W.");
      System.out.println( String.join( " ",
                  getTokens( "I have a C.N.P. in charge of a P.A.-C. for the U.S. vs. my Ph.D. professor in T.W." ) ) );
      System.out.println( "We have a non-pharmacologic version of and anti-inflammatory being re-evaluated for anti-anxiety and bee-bop." );
      System.out.println( String.join( " ",
            getTokens( "We have a non-pharmacologic version of and anti-inflammatory being re-evaluated for anti-anxiety and bee-bop." ) ) );
      System.out.println( "we are doing this b/c we might d/c its use w/o first performing u/s on a patient's t/w.");
      System.out.println( String.join( " ",
            getTokens( "we are doing this b/c we might d/c its use w/o first performing u/s on a patient's t/w." ) ) );
      System.out.println( "Done 12-5-95 or 1995-3-1 by 5/4/98 and 6/15 of 1998 just in case of B48-3 or  4/56/78/910.");
      System.out.println( String.join( " ",
            getTokens( "Done 12-5-95 or 1995-3-1 by 5/4/98 and 6/15 of 1998 just in case of B48-3 or 4/56/78/910." ) ) );
      System.out.println( "For I.D. numbers 9646-66, 9-2282 and 968-9060, but not 45-67-890." );
      System.out.println( String.join( " ",
            getTokens( "For I.D. numbers 9646-66, 9-2282 and 968-9060, but not 45-67-890." ) ) );
      System.out.println( "How about web https://seer.cancer.gov/archive/csr/1975_2005/index.html or email info@ndif.org?" );
      System.out.println( String.join( " ",
            getTokens( "How about web https://seer.cancer.gov/archive/csr/1975_2005/index.html or email info@ndif.org?" ) ) );
      System.out.println( "We measured 9mm, 16g, 45kg and 7mu (made up) over 6yrs." );
      System.out.println( String.join( " ",
            getTokens( "We measured 9mm, 16g, 45kg and 7mu (made up) over 6yrs." ) ) );
      System.out.println( "Don't use 79% or >14% @temp for <5min." );
      System.out.println( String.join( " ",
            getTokens( "Don't use 79% or >14% @temp for <5min." ) ) );
      System.out.println( "I would like to end this... sentence with parenthetic(...) ellipses (...)." );
      System.out.println( String.join( " ",
            getTokens( "I would like to end this... sentence with parenthetic(...) ellipses (...)." ) ) );
      System.out.println( "He's got what shouldn't be gotten, like this,for instance! 20,00" );
      System.out.println( String.join( " ",
            getTokens( "He's got what shouldn't be gotten, like this,for instance! 20,00" ) ) );
      System.out.println( "Let's see if .we can mess.up some .100 numbers unlike (45.5) or 10,000,000 or 1,000.50." );
      System.out.println( String.join( " ",
            getTokens( "Let's see if .we can mess.up some .100 numbers unlike (45.5) or 10,000,000 or 1,000.50." ) ) );
      System.out.println( String.join( " ",
            getTokens( "What happens at 1:30 o'clock?" ) ) );

   }


}
