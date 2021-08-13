package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.uri.UriAnnotationFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/2/2021}
 */
@PipeBitInfo(
      name = "BiomarkerFinder",
      description = "Finds Biomarker values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR )
final public class BiomarkerFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "BiomarkerFinder" );





   static private final String REGEX_METHOD
         = "IHC|Immunohistochemistry|ISH|(?:IN SITU HYBRIDIZATION)|(?:DUAL ISH)"
           + "|FISH|(?:Fluorecent IN SITU HYBRIDIZATION)|(?:Nuclear Staining)";
   // for

   static private final String REGEX_TEST = "Test|Method";

   static private final String REGEX_LEVEL = "Level|status|expression|result|results|score";

//   static private final String REGEX_IS = "is|are|was";

   static private final String REGEX_STRONGLY = "weakly|strongly|greatly";
   static private final String REGEX_ELEVATED = "rising|increasing|elevated|elvtd|raised|increased|strong|amplified";
   static private final String REGEX_FALLING = "falling|decreasing|low|lowered|decreased|weak";
   static private final String REGEX_STABLE = "stable";


   static private final String REGEX_GT_LT = "(?:(?:Greater|>|Higher|Less|<|Lower)(?: than ?)?)?"
                                             + "(?: or )?(?:Greater|>|Higher|Less|<|Lower|Equal|=)(?: than|to "
                                             + "?)?";

//   static private final String REGEX_POSITIVE = "\\+?pos(?:itive|itivity)?|\\+(?:pos)?|overexpression";
//   static private final String REGEX_NEGATIVE = "\\-?neg(?:ative)?|\\-(?:neg)?|(?:not amplified)|(?:no [a-z] detected)";
static private final String REGEX_POSITIVE = "\\+?pos(?:itive|itivity)?|overexpression";
   static private final String REGEX_NEGATIVE = "-?neg(?:ative)?|(?:not amplified)|(?:no [a-z] detected)|(?:non-? "
                                                + "?detected)";
   static private final String REGEX_UNKNOWN
//         = "unknown|indeterminate|equivocal|borderline|(?:not assessed|requested|applicable)|\\sN\\/?A\\s";
         = "unknown|indeterminate|equivocal|borderline";
   static private final String REGEX_NOT_ASSESSED
         = "(?:not assessed|requested|applicable)|insufficient|pending|\\sN\\/?A";


   static private final String REGEX_POS_NEG = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")";

   static private final String REGEX_POS_NEG_UNK
         = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")|(?:" + REGEX_UNKNOWN + ")";

   static private final String REGEX_POS_NEG_UNK_NA
         = "(?:" + REGEX_POSITIVE
         + ")|(?:" + REGEX_NEGATIVE
         + ")|(?:" + REGEX_UNKNOWN
         + ")|(?:" + REGEX_NOT_ASSESSED + ")";

   static private final String REGEX_0_9
         = "[0-9]|zero|one|two|three|four|five|six|seven|eight|nine";

   static private final String REGEX_NUMTEEN
         = "(?:1[0-9])|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen";
   static private final String REGEX_0_19 = REGEX_0_9 + "|" + REGEX_NUMTEEN;

   static private final String REGEX_NUMTY
         = "(?:[2-9]0)|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety";
   static private final String REGEX_0_99
         = "(?:" + REGEX_0_19 + ")|(?:(?:" + REGEX_NUMTY + ")(?: ?-? ?" + REGEX_0_9 + ")?)";

   static private final String REGEX_HUNDREDS
         = "(?:[1-9]00)|(?:(?:" + REGEX_0_9 + " ?-? )?hundred)";
   static private final String REGEX_0_999
         = "(?:" + REGEX_0_99 + ")|(?:" + REGEX_HUNDREDS + ")(: ?-? ?" + REGEX_0_99 + ")?)";

   static private final String REGEX_DECIMAL = "\\.[0-9]{1,4}";




   private enum Biomarker {
      ER_( "(?:Estrogen|ER(?!B)\\+?-?|ER:(\\s*DCIS)?(\\s*IS)?)",
           "",
           REGEX_POS_NEG_UNK_NA,
           true ),

      PR_( "(?:Progesterone|Pg?R\\+?-?|PR:(\\s*DCIS)?(\\s*IS)?)",
           "",
           REGEX_POS_NEG_UNK_NA,
           true ),

      HER2( "(?:HER-? ?2(?: ?\\/?-? ?neu)?\\+?-?(?:\\s*ONCOGENE)?(?:\\s*\\(?ERBB2\\)?)?)",
            "",
            REGEX_POS_NEG_UNK_NA ),

      KI67( "M?KI ?-? ?67(?: Antigen)?",
            "",
            "(?:>|< ?)?[0-9]{1,2}(?:\\.[0-9]{1,2} ?)? ?%(?: positive)?",
            true ),

      BRCA1( "(?:BRCA1|BROVCA1|(?:Breast Cancer Type 1))"
             + "(?: Susceptibility)?(?: Gene)?(?: Polymorphism)?",
             "",
             "" ),

      BRCA2( "(?:BRCA2|BROVCA2|FANCD1|(?:Breast Cancer Type 2))"
             + "(?: Susceptibility)?(?: Gene)?(?: Polymorphism)?",
             "",
             "" ),

      ALK( "(?:ALK\\+?-?|CD246\\+?-?|(?:Anaplastic Lymphoma (?:Receptor Tyrosine )?Kinase))"
           + "(?: Fusion)?(?: Gene|Oncogene)?(?: Alteration)?",
           "",
           "(?:" + REGEX_POS_NEG_UNK_NA +")|(?:no rearrangement)",

           true ),

      EGFR( "EGFR\\+?-?|HER1\\+?-?|ERBB\\+?-?|C-ERBB1\\+?-?|(?:Epidermal Growth Factor)"
            + "(?: Receptor)?",
            "",
            "(?:" + REGEX_POS_NEG_UNK_NA +")|(?:not mutant)|(?:no mutations?)|",
            true ),

      BRAF( "(?:Serine\\/Threonine-Protein Kinase )?B-?RAF1?"
            + "(?: Fusion)?",
            "",
            "" ),

      ROS1( "(?:Proto-Oncogene )?(?:ROS1\\+?-?|MCF3\\+?-?|C-ROS-1\\+?-?"
            + "|(?:ROS Proto-Oncogene 1)"
            + "|(?:Tyrosine-Protein Kinase ROS)"
            + "|(?:Receptor Tyrosine Kinase c-ROS Oncogene 1))"
            + "(?: Gene)?(?: Fusion|Alteration|Rearrangement)?",
            "",
            REGEX_POS_NEG_UNK_NA,
            true ),

//      PD1,
      PDL1( "(?:PDL1|PD-L1|CD247|B7|B7-H|B7H1|PDCD1L1|PDCD1LG1|(?:Programmed Cell Death 1 Ligand 1))"
            + "(?: Antigen)?(?: Molecule)?",
            "",
            "[0-9]{1,2} ?%(?: high expression)?" ),

      MSI( "MSI|MSS|Microsatellite",
           "",
           "stable" ),

      KRAS( "(?:KRAS\\+?-?|C-K-RAS\\+?-?|KRAS2\\+?-?|KRAS-2\\+?-?|V-KI-RAS2\\+?-?|(?:Kirsten Rat Sarcoma Viral Oncogene Homolog))"
            + "(?: Wild ?-?type|wt)?(?: Gene Mutation)?",
            "",
            REGEX_POS_NEG_UNK_NA,
            true ),

      PSA( "PSA(?: Prostate Specific Antigen)?|(?:Prostate Specific Antigen(?: [PSA])?)",
           "",
           "[0-9]{1,2}\\.[0-9]{1,4}" ),

     PSA_EL( "PSA(?: Prostate Specific Antigen)?|(?:Prostate Specific Antigen(?: [PSA])?)",
          "",
             "(?:" + REGEX_ELEVATED + ")|(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")",
             true );

      final Pattern _typePattern;
      final int _windowSize;
      final boolean _checkSkip;
      final Pattern _skipPattern;
      final Pattern _valuePattern;
      final boolean _canPrecede;
      final boolean _plusMinus;
      Biomarker( final String typeRegex, final String skipRegex, final String valueRegex ) {
         this( typeRegex, 20, skipRegex, valueRegex, false );
      }
      Biomarker( final String typeRegex, final String skipRegex, final String valueRegex,
                 final boolean canPrecede ) {
         this( typeRegex, 20, skipRegex, valueRegex, canPrecede );
      }
      Biomarker( final String typeRegex, final int windowSize, final String skipRegex,
                 final String valueRegex, final boolean canPrecede ) {
         _typePattern = Pattern.compile( typeRegex, Pattern.CASE_INSENSITIVE );
         _windowSize = windowSize;
         if ( skipRegex.isEmpty() ) {
            _checkSkip = false;
            _skipPattern = null;
         } else {
            _checkSkip = true;
            _skipPattern = Pattern.compile( skipRegex, Pattern.CASE_INSENSITIVE );
         }
         _valuePattern = Pattern.compile( valueRegex, Pattern.CASE_INSENSITIVE );
         _canPrecede = canPrecede;
         _plusMinus = REGEX_POS_NEG_UNK.equals( valueRegex );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Biomarkers and Values ..." );

      findBiomarkers( jCas );

   }


   static public void findBiomarkers( final JCas jCas ) {
      final String docText = jCas.getDocumentText();
      final Collection<Integer> annotationBegins = JCasUtil.select( jCas, IdentifiedAnnotation.class )
                                                           .stream()
                                                           .filter( a -> ( a instanceof EventMention
                                                                           || a instanceof AnatomicalSiteMention ) )
                                                           .map( IdentifiedAnnotation::getBegin )
                                                           .collect( Collectors.toList() );
      final Collection<Pair<Integer>> sentenceSpans
            = JCasUtil.select( jCas, Sentence.class )
                      .stream()
                      .map( s -> new Pair<>( s.getBegin(), s.getEnd() ) )
                      .collect( Collectors.toList() );
      for ( Biomarker biomarker : Biomarker.values() ) {
         final List<Pair<Integer>> biomarkerSpans = findBiomarkerSpans( biomarker, docText );
         addBiomarkers( jCas, biomarker, docText, biomarkerSpans, sentenceSpans, annotationBegins );
      }
   }


   static private List<Pair<Integer>> findBiomarkerSpans( final Biomarker biomarker, final String text ) {
      try ( RegexSpanFinder finder = new RegexSpanFinder( biomarker._typePattern ) ) {
         return finder.findSpans( text )
                      .stream()
                      .filter( s -> isWholeWord( text, s ) )
                      .collect( Collectors.toList() );
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.warn( iaE.getMessage() );
         return Collections.emptyList();
      }
   }

   // Why the heck don't word boundaries ever work in java?!
   static private boolean isWholeWord( final String text, final Pair<Integer> span ) {
      return isWholeWord( text, span.getValue1(), span.getValue2() );
   }

   // Why the heck don't word boundaries ever work in java?!
   static private boolean isWholeWord( final String text, final int begin, final int end ) {
      if ( begin > 0 ) {
         if ( Character.isLetterOrDigit( text.charAt( begin-1 ) ) ) {
            return false;
         }
      }
      if ( end == text.length() ) {
         return true;
      }
      return !Character.isLetterOrDigit( text.charAt( end ) );
   }


   static private void addBiomarkers( final JCas jCas,
                                      final Biomarker biomarker,
                                       final String text,
                                       final List<Pair<Integer>> biomarkerSpans,
                                      final Collection<Pair<Integer>> sentenceSpans,
                                      final Collection<Integer> annotationBegins ) {
      if ( biomarkerSpans.isEmpty() ) {
         return;
      }
      for ( Pair<Integer> biomarkerSpan : biomarkerSpans ) {
         addBiomarker( jCas, biomarker, text, biomarkerSpan, sentenceSpans, annotationBegins );
      }
   }


   static private void addBiomarker( final JCas jCas,
                                      final Biomarker biomarker,
                                      final String text,
                                      final Pair<Integer> biomarkerSpan,
                                     final Collection<Pair<Integer>> sentenceSpans,
                                     final Collection<Integer> annotationBegins ) {
      final Pair<Integer> sentenceSpan = getSentenceSpan( biomarkerSpan, sentenceSpans );
      final int followingAnnotation = getFollowingAnnotation( biomarkerSpan, text.length(), annotationBegins );
      if ( addBioMarkerFollowed( jCas, biomarker, text, biomarkerSpan, sentenceSpan, followingAnnotation ) ) {
         return;
      }
      if ( biomarker._canPrecede ) {
         final int precedingAnnotation = getPrecedingAnnotation( biomarkerSpan, annotationBegins );
         addBioMarkerPreceded( jCas, biomarker, text, biomarkerSpan, sentenceSpan, precedingAnnotation );
      }
   }

   static private boolean addBioMarkerFollowed( final JCas jCas,
                                                final Biomarker biomarker,
                                                final String text,
                                                final Pair<Integer> biomarkerSpan,
                                                final Pair<Integer> sentenceSpan,
                                                final int followingAnnotation ) {
      if ( biomarker._plusMinus ) {
         final char c = text.charAt( biomarkerSpan.getValue2()-1 );
         if ( (c == '+' || c == '-') && isWholeWord( text, biomarkerSpan ) ) {
            addBiomarker( jCas, biomarker, biomarkerSpan.getValue1(), biomarkerSpan.getValue2() );
            return true;
         }
      }

      final String nextText = getFollowingText( biomarker, biomarkerSpan, text, sentenceSpan, followingAnnotation );
      if ( nextText.isEmpty() ) {
         return false;
      }
      if ( biomarker._checkSkip ) {
         final Matcher skipMatcher = biomarker._skipPattern.matcher( nextText );
         if ( skipMatcher.find() ) {
            return false;
         }
      }
      final Matcher matcher = biomarker._valuePattern.matcher( nextText );
      if ( matcher.find() ) {
         final int matchBegin = biomarkerSpan.getValue2() + matcher.start();
         final int matchEnd = biomarkerSpan.getValue2() + matcher.end();
         if ( isWholeWord( text, matchBegin, matchEnd ) ) {
            addBiomarker( jCas, biomarker, matchBegin, matchEnd );
            return true;
         }
      }
      return false;
   }

   static private boolean addBioMarkerPreceded( final JCas jCas,
                                                final Biomarker biomarker,
                                                final String text,
                                                final Pair<Integer> biomarkerSpan,
                                                final Pair<Integer> sentenceSpan,
                                                final int precedingAnnotation ) {
      if ( !biomarker._canPrecede ) {
         return false;
      }
      final String prevText = getPrecedingText( biomarker, biomarkerSpan, text, sentenceSpan, precedingAnnotation );
      if ( prevText.isEmpty() ) {
         return false;
      }
      final Matcher matcher = biomarker._valuePattern.matcher( prevText );
      Pair<Integer> lastMatch = null;
      while ( matcher.find() ) {
         lastMatch = new Pair<>( matcher.start(), matcher.end() );
      }
      if ( lastMatch == null ) {
         return false;
      }
      final int matchBegin = biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue1();
      final int matchEnd = biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue2();
      if ( isWholeWord( text, matchBegin, matchEnd ) ) {
         addBiomarker( jCas, biomarker, matchBegin, matchEnd );
         return true;
      }
      return false;
   }

   static private void addBiomarker( final JCas jCas,
                                      final Biomarker biomarker,
                                        final int valueSpanBegin, final int valueSpanEnd ) {
      UriAnnotationFactory.createIdentifiedAnnotations( jCas,
                                                        valueSpanBegin,
                                                        valueSpanEnd,
                                                        biomarker.name(),
                                                        SemanticGroup.FINDING,
                                                        "T184" );
   }


   static private Pair<Integer> getSentenceSpan( final Pair<Integer> biomarkerSpan,
                                                 final Collection<Pair<Integer>> sentenceSpans ) {
      return sentenceSpans.stream()
                         .filter( s -> s.getValue1() <= biomarkerSpan.getValue1()
                                       && biomarkerSpan.getValue2() <= s.getValue2() )
                         .findFirst()
                         .orElse( biomarkerSpan );
   }

   static private int getPrecedingAnnotation( final Pair<Integer> biomarkerSpan,
                                               final Collection<Integer> annotationBegins ) {
      return annotationBegins.stream()
                          .filter( b -> b < biomarkerSpan.getValue1() )
                             .mapToInt( b -> b )
                          .max()
                          .orElse( 0 );
   }

   static private int getFollowingAnnotation( final Pair<Integer> biomarkerSpan,
                                              final int textLength,
                                              final Collection<Integer> annotationBegins ) {
      return annotationBegins.stream()
                             .filter( b -> b >= biomarkerSpan.getValue2() )
                             .mapToInt( b -> b )
                             .min()
                             .orElse( textLength );
   }

   static private String getPrecedingText( final Biomarker biomarker,
                                           final Pair<Integer> biomarkerSpan,
                                           final String text,
                                           final Pair<Integer> sentenceSpan,
                                           final int precedingAnnotation ) {
      final int sentenceOrAnnotation = Math.max( precedingAnnotation, sentenceSpan.getValue1() );
//      final int windowSize = Math.max( 0, biomarkerSpan.getValue1() - biomarker._windowSize );
      final String prevText = text.substring( sentenceOrAnnotation, biomarkerSpan.getValue1() );
      // Check for end of paragraph
      final int pIndex = prevText.lastIndexOf( "\n\n" );
      if ( pIndex >= 0 ) {
         return prevText.substring( pIndex+2 );
      }
      return prevText;
   }


   static private String getFollowingText( final Biomarker biomarker,
                                            final Pair<Integer> biomarkerSpan,
                                            final String text,
                                           final Pair<Integer> sentenceSpan,
                                           final int followingAnnotation ) {
      final int sentenceOrAnnotation = Math.min( followingAnnotation, sentenceSpan.getValue2() );
//      final int windowSize = Math.min( text.length(), biomarkerSpan.getValue2() + biomarker._windowSize );
      String nextText = text.substring( biomarkerSpan.getValue2(), sentenceOrAnnotation );
      // Check for end of paragraph
      final int pIndex = nextText.indexOf( "\n\n" );
      if ( pIndex == 0 ) {
         return "";
      }
      // Sometimes value sets are in brackets.  e.g.  "ER: [pos;neg;unk] = neg"
      final int startBracket = nextText.indexOf( '[' );
      if ( startBracket >= 0 ) {
         final int endBracket = nextText.indexOf( ']', startBracket );
         if ( endBracket > 0 ) {
            final char[] chars = nextText.toCharArray();
            for ( int i=startBracket+1; i<endBracket; i++ ) {
               chars[ i ] = 'V';
            }
            nextText = new String( chars );
         }
      }

      if ( pIndex > 0 ) {
         return nextText.substring( 0, pIndex );
      }
      return nextText;
   }



}
