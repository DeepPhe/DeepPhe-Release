package org.healthnlp.deepphe.nlp.phenotype.receptor;

import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.nlp.uri.UriAnnotationFactory;
import org.junit.Test;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.UriConstants.*;
import static org.junit.Assert.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/29/2015
 */
final public class StatusFinder {

   private StatusFinder() {
   }

   static private final Logger LOGGER = Logger.getLogger( "StatusFinder" );


   //   // TODO add negative lookahead for "antibody" and "immunostain"
   // TODO - - switch to (and/or(ER|PR|HER2)){0,2}
   // TODO add "validTest" relations in ontology for the "*_Receptor_Status" uri nodes
   static private final String TYPE_REGEX
         = "Triple|" +
           "(?:Estrogen(?: Receptor \\(ER\\))?(?:\\s+and\\s+Progesterone)?)|" +
           "(?:ER(?:sq)?(:(?:\\s*DCIS)?(?:\\s*IS)?)?(?:\\s*BY IHC)?(?:\\s*(?:\\/|and|or)\\s*(?:PR|(?:HER\\-? ?2(\\s*\\/?\\s*neu)?)))?)|" +
           "(?:(?:Progesterone(?: Receptor \\(Pg?R\\))?)|Pg?R(?:sq)?(:(?:\\s*DCIS)?(\\s*IS)?)?(?:\\s*BY IHC)?)|" +
           "(?:HER\\-? ?2(\\s*\\/?\\-?\\s*neu)?(?:\\s*ONCOGENE)?(?:\\s*ONCOPROTEIN)?\\(?(?:\\s*BY)?(?:\\s*IHC)?(?:\\s*IN SITU HYBRIDIZATION)?(?:\\s*DUAL ISH)?(?:\\s*FISH)?(?:\\s*IMMUNOHISTOCHEMISTRY)?(?:\\s*METHOD)?\\)?(?:\\s*\\(?ERBB2\\)?)?(?:\\s*\\(?CERB B-2\\)?)?(?:\\s*\\(?DCIS\\)?)?)";
   static private final String INTERIM_EX
         = "(?:" +
           "(?:-?\\s*?Receptors?\\s*-?)?" +
           "\\s*" +
           "(?:(?:status|expression|test|result|results|(?:\\([a-zA-Z 0-9]+\\)))\\s*)?" +
           "(?:\\s*-\\s*score(?:\\s*-\\s*[0-9])?)?"
           + "\\s*\\(?" +
           "(?:\\s*BY IHC METHOD\\s*)?" +
           "(?:(?:is|are|was|-)\\s*)?" +
           "\\s*:?)" +
           "|(?:\\s+PROTEIN\\s+EXPRESSION\\s+\\(0-3\\+\\):)";
   static private final String STRENGTH_EX = "(?:also )?(?:strong|weak|[0-9%]*)(?:ly)?(?:\\s*amplified)? ?;?";
   static private final String LONG_VALUE = "(?:\\+?pos(?:itive)?)|(?:\\-?neg(?:ative)?)"
                                            +
                                            "|(?:N\\/?A\\b)|unknown|indeterminate|borderline|equivocal|(?:not\\s+assessed)";
   static private final String SHORT_VALUE = "\\b[0-3]\\+|[0-3]{0}\\+(?:pos)?|\\-(?:neg)?";

   // Order is very important
   static private final String FULL_REGEX = "\\b(?<TYPE>" + TYPE_REGEX + ")\\s*"
         + "(?:" + INTERIM_EX + ")?\\s*"
         + "(?:" + STRENGTH_EX + ")?\\s*"
         + "(?<VALUE>(?:" + LONG_VALUE + ")|(?:" + SHORT_VALUE + "))";
   static private final Pattern FULL_PATTERN = Pattern.compile( FULL_REGEX, Pattern.CASE_INSENSITIVE );

   static private final String AND_OR = "\\s*,?\\s*(?:\\/|and|or)?\\s+";
   static private final String ER_PR_HER2 = "(?:" + SimpleStatusType.ER.getRegex()
                                            + ")|(?:" + SimpleStatusType.PR.getRegex() + ")|(?:" +
                                            SimpleStatusType.HER2.getRegex() + ")";


   static private final String VALUE_TYPE_REGEX = "\\b(?:" + STRENGTH_EX + ")?\\s*"
                                                  + "(?:" + LONG_VALUE + ")" +
                                                  "\\s+(?:nuclear )?(?:staining )?for\\s+" +
                                                  "(?:" + ER_PR_HER2 + ")" + "(?:" + AND_OR + "(?:" + ER_PR_HER2 + ")){0,2}";

   static private final Pattern VALUE_TYPE_PATTERN = Pattern.compile( VALUE_TYPE_REGEX, Pattern.CASE_INSENSITIVE );

   private enum SimpleStatusType {
      ER( ER_STATUS, "(?:Estrogen|ER(?!B)|ER:(\\s*DCIS)?(\\s*IS)?)" ),
      PR( PR_STATUS, "(?:Progesterone|Pg?R|PR:(\\s*DCIS)?(\\s*IS)?)" ),
      HER2( HER2_STATUS, "(?:HER\\-? ?2(?: ?\\/\\-? ?neu)?(?:\\s*ONCOGENE)?(?:\\s*\\(?ERBB2\\)?)?)" ),
      NEG_3( TRIPLE_NEGATIVE, "Triple" );

      static private final String RECEPTOR_EX = "(?:\\s*\\-?\\s*?Receptors?\\s*\\-?)?\\s*(?:status|expression)?";
      final private String _uri;
      final private String _regex;
      final private Pattern _pattern;

      SimpleStatusType( final String uri, final String regex ) {
         _uri = uri;
         _regex = regex + RECEPTOR_EX;
         _pattern = Pattern.compile( "\\b" + regex + RECEPTOR_EX, Pattern.CASE_INSENSITIVE );
      }

      public String getUri() {
         return _uri;
      }

      public String getRegex() {
         return _regex;
      }

      public Matcher getMatcher( final CharSequence lookupWindow ) {
         return _pattern.matcher( lookupWindow );
      }

      public String getUriForValue() {
         return getUri().replace( "_Status", "" );
      }
   }

   enum SimpleStatusValue {
      POSITIVE( "_Positive", "3\\+|\\+?pos(?:itive)?|\\+(?:pos)?" ),
      NEGATIVE( "_Negative", "0\\+|1\\+|\\-?neg(?:ative)?|\\-(?:neg)?" ),
      UNKNOWN( "_Status_Unknown", "(?:2\\+|unknown|indeterminate|equivocal|borderline|(?:not assessed)|(?:not applicable)|\\bN\\/?A\\b)" );
      final private String _uriSuffix;
      final private Pattern _pattern;

      SimpleStatusValue( final String uriSuffix, final String regex ) {
         _uriSuffix = uriSuffix;
         _pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
      }

      public String getUriSuffix() {
         return _uriSuffix;
      }

      public Matcher getMatcher( final CharSequence lookupWindow ) {
         return _pattern.matcher( lookupWindow );
      }

   }

   static private final class SimpleStatus {
      private final int _begin;
      private final int _end;
      private final String _uri;
      private SimpleStatus( final int begin, final int end, final String uri ) {
         _begin = begin;
         _end = end;
         if ( uri.startsWith( SimpleStatusType.NEG_3.getUri() ) ) {
            _uri = SimpleStatusType.NEG_3.getUri();
         } else {
            _uri = uri;
         }
      }
   }



   static public List<IdentifiedAnnotation> addReceptorStatuses( final JCas jcas, final AnnotationFS lookupWindow ) {
      final String windowText = lookupWindow.getCoveredText();
      final List<SimpleStatus> statuses = getReceptorStatuses( windowText );
      final Collection<SimpleStatus> statuses2 = getReceptorStatuses2( windowText );
      statuses.addAll( statuses2 );
      if ( statuses.isEmpty() ) {
         return Collections.emptyList();
      }
      final int windowStartOffset = lookupWindow.getBegin();
      return statuses.stream()
                     .map( s -> UriAnnotationFactory.createIdentifiedAnnotations( jcas,
                                                                                    windowStartOffset + s._begin,
                                                                                    windowStartOffset + s._end,
                                                                                    s._uri, SemanticGroup.LAB,
                                                                                    "T034" ) )
              .flatMap( Collection::stream )
              .sorted( Comparator.comparingInt( Annotation::getBegin ).thenComparing( Annotation::getEnd ) )
              .collect( Collectors.toList() );
   }


   static private List<SimpleStatus> getReceptorStatuses( final String lookupWindow ) {
      if ( lookupWindow.length() < 3 ) {
         return new ArrayList<>();
      }
      final List<SimpleStatus> statuses = new ArrayList<>();
      final Matcher fullMatcher = FULL_PATTERN.matcher( lookupWindow );
      while ( fullMatcher.find() ) {
         final String typeWindow = fullMatcher.group( "TYPE" );
         final int typeWindowStart = fullMatcher.start( "TYPE" );
         final String valueWindow = fullMatcher.group( "VALUE" );
         final int valueWindowStart = fullMatcher.start( "VALUE" );
         for ( SimpleStatusType type : SimpleStatusType.values() ) {
            final Matcher typeMatcher = type.getMatcher( typeWindow );
            while ( typeMatcher.find() ) {
               final int typeStart = typeWindowStart + typeMatcher.start();
               final int typeEnd = typeWindowStart + typeMatcher.end();
               for ( SimpleStatusValue value : SimpleStatusValue.values() ) {
                  final Matcher valueMatcher = value.getMatcher( valueWindow );
                  if ( valueMatcher.matches() ) {
                     if ( valueWindowStart + valueMatcher.end() < lookupWindow.length()
                          && lookupWindow.charAt( valueWindowStart + valueMatcher.end() ) == ':' ) {
                        // Kludge because negative lookahead doesn't appear to be working.
                        // In list entry with name that has status and value.
                        continue;
                     }
                     if ( valueWindowStart + valueMatcher.end() + 3 < lookupWindow.length()
                          && lookupWindow.substring( valueWindowStart + valueMatcher.end(),
                           valueWindowStart + valueMatcher.end() + 3 ).equalsIgnoreCase( "neu" ) ) {
                        // Kludge because negative lookahead doesn't appear to be working.
                        // match is "her2-neu"
                        continue;
                     }
                     final int valueStart = valueWindowStart + valueMatcher.start();
                     final int valueEnd = valueWindowStart + valueMatcher.end();
                     statuses.add( new SimpleStatus( Math.min( typeStart, valueStart ),
                           Math.max( typeEnd, valueEnd ),
                           type.getUriForValue() + value.getUriSuffix() ) );
                     break;
                  }
               }
            }
         }
      }
      return statuses;
   }

   static private List<SimpleStatus> getReceptorStatuses2( final String lookupWindow ) {
      if ( lookupWindow.length() < 3 ) {
         return Collections.emptyList();
      }
      final List<SimpleStatus> statuses = new ArrayList<>();
      final Matcher fullMatcher = VALUE_TYPE_PATTERN.matcher( lookupWindow );
      while ( fullMatcher.find() ) {
         final String matchWindow = lookupWindow.substring( fullMatcher.start(), fullMatcher.end() );
         for ( SimpleStatusValue value : SimpleStatusValue.values() ) {
            final Matcher valueMatcher = value.getMatcher( matchWindow );
            if ( valueMatcher.find() ) {
               final int valueStart = fullMatcher.start() + valueMatcher.start();
               final int valueEnd = fullMatcher.start() + valueMatcher.end();
               final String typeLookupWindow = matchWindow.substring( valueMatcher.end() );
               for ( SimpleStatusType type : SimpleStatusType.values() ) {
                  final Matcher typeMatcher = type.getMatcher( typeLookupWindow );
                  if ( typeMatcher.find() ) {
                     final int typeEnd = valueEnd + typeMatcher.end();
                     statuses.add( new SimpleStatus( valueStart, typeEnd, type.getUriForValue() + value.getUriSuffix() ) );
                  }
               }
            }
         }
      }
      return statuses;
   }



   static final public class ReceptorStatusFinderTester {

   private final Logger LOGGER = Logger.getLogger( "ReceptorStatusTester" );

   public ReceptorStatusFinderTester() {}

   private final String[] MULTIPLE_MENTION_SENTENCES = {
         "invasive ductal\n" +
               "carcinoma ER -/PR -/Her 2+.  "
   };

    private final String[] MULTIPLE_MENTION_SENTENCES_2 = {
         "Negative for Er, Pr, and Her-2/neu"
   };

    private final String[] NO_RECEPTOR_SENTENCES = {
         "Patient stated her position.", "Patient sex is neuter.", "Patient is positive her pulse is rapid.",
         "Patient was rushed to the ER", "The patient rapped PR to the ER for HER2 the neu!",
         "xy complement, progesterone absorption negative",
         "ER Positive: 1+" };

   //  THIS SENTENCE IS EXPECTED TO CREATE A FALSE POSITIVE
    private final String[] FAIL_RECEPTOR_SENTENCES = { "The ER is positive that the patient is stable." };

    private final String[] ER_POS_SENTENCES = {
          "ER by IHC\nPositive",
          "POSITIVE nuclear staining for Estrogen\nReceptor",
          "Patient tested ER+ last week.", "Patient tested ER + last week.",
          "Patient tested ER+pos last week.", "Patient tested ER+positive last week.",
          "Patient tested ER pos last week.", "Patient tested ER positive last week.",
          "Patient tested ER status pos last week.", "Patient tested ER status positive last week.",
          "Patient ER status pos.", "Patient ER status is positive.",
          "ER      POS", "ER Status           POSITIVE",
          "ER: POS", "Estrogen Receptors: Positive",
          "Estrogen      POS", "Estrogen           POSITIVE",
          "Estrogen Receptor POS", "Estrogen-Receptor POSITIVE", "Estrogen Receptor-positive",
          "Estrogen Receptor status POS", "Estrogen-Receptor status is positive",
          "Patient is positive for Estrogen Receptor",
          "ER:  Positive           80              (0 50%;  1+ 10#%;  2+ 40%;  3+ 0%)",
          "The estrogen receptor was positive H.", "ER TEST: WEAKLY POSITIVE", "ER TEST: WEAK POSITIVE",
          "Estrogen Receptor:    POSITIVE.", "some: Estrogen Receptor:   POSITIVE.",
          "POSITIVE nuclear staining for Estrogen Receptor", "Estrogen receptor ( clone 1D5 ) : Positive",
          "ERsq : Positive", "ERsq : 100% Positive",
          "Estrogen Receptor (ER) Status\n" +
          "      Positive",
          "Estrogen 90% positive 3+" };

    private final String[] ER_NEG_SENTENCES = {
          "ER NEGATIVE",
          "ER: NEGATIVE",
          "ER IS NEGATIVE",
          "ER:  DCIS IS NEGATIVE (0%), SEE COMMENT.",
          "Patient tested ER- last week.", "Patient tested ER - last week.",
          "Patient tested ER-neg last week.", "Patient tested ER-negative last week.",
          "Patient tested ER neg last week.", "Patient tested ER negative last week.",
          "Patient tested ER status neg last week.", "Patient tested ER status negative last week.",
          "Patient ER status neg.", "Patient ER status is negative.",
          "ER      NEG", "ER Status           NEGATIVE",
          "ER: NEG", "Estrogen Receptors : Negative",
          "Estrogen      NEG", "Estrogen           NEGATIVE",
          "Estrogen Receptor NEG", "Estrogen-Receptor NEGATIVE", "Estrogen Receptor-negative",
          "Estrogen Receptor status NEG", "Estrogen-Receptor status is negative",
          "ER:  Negative           0              (0 100%;  1+ 0%;  2+ 0%;  3+ 0%)",
          "Patient is negative for Estrogen Receptor",
          "ESTROGEN RECEPTOR STATUS BY IHC METHOD : Negative",
          "- ESTROGEN RECEPTOR STATUS\n" +
          "- NEGATIVE ( UNFAVORABLE )",
          "ESTROGEN RECEPTOR STATUS\n" +
          "    - NEGATIVE (UNFAVORABLE)" };

    private final String[] ER_NA_SENTENCES = {
         "Patient ER status unknown.", "Patient ER status is unknown.",
         "Patient ER status indeterminate.", "Patient ER status is indeterminate.",
         "Patient ER status equivocal.", "Patient ER status is equivocal.",
         "Patient ER status not assessed.", "Patient ER status is not assessed.",
         "Patient ER status NA.", "Patient ER status is N/A.",
         "ER      unknown", "ER Status           unknown",
         "ER      indeterminate", "ER Status           indeterminate",
         "ER      equivocal", "ER Status           equivocal",
         "ER      not assessed", "ER Status           not assessed",
         "ER      NA", "ER Status           NA",
         "ER      N/A", "ER Status           N/A",
         "Estrogen      unknown", "Estrogen      indeterminate",
         "Estrogen      equivocal", "Estrogen      not assessed",
         "Estrogen      NA", "Estrogen      N/A" };


    private final String[] PR_POS_SENTENCES = {
          "PR:  DCIS IS POSITIVE",
          "Patient tested PR+ last week.", "Patient tested PR + last week.",
          "Patient tested PR+pos last week.", "Patient tested PR+positive last week.",
          "Patient tested PR pos last week.", "Patient tested PR positive last week.",
          "Patient tested PR status pos last week.", "Patient tested PR status positive last week.",
          "Patient PR status pos.", "Patient PR status is positive.",
          "PR      POS", "PR Status           POSITIVE",
          "PR: POS", "Progesterone Receptors : Positive",
          "Progesterone      POS", "Progesterone           POSITIVE",
          "Progesterone Receptor POS", "Progesterone-Receptor POSITIVE", "Progesterone Receptor-positive",
          "Progesterone Receptor status POS", "Progesterone-Receptor status is positive",
          "Patient is positive for Progesterone Receptor", "POSITIVE staining for Progesterone",
          "Progesterone receptor (clone PgR 636) : Positive",
          "PRsq : Positive", "PRsq : 100% Positive" };

    private final String[] PR_NEG_SENTENCES = {
         "PR by IHC\nNegative",
         "Patient tested PR- last week.", "Patient tested PR - last week.",
         "Patient tested PR-neg last week.", "Patient tested PR-negative last week.",
         "Patient tested PR neg last week.", "Patient tested PR negative last week.",
         "Patient tested PR status neg last week.", "Patient tested PR status negative last week.",
         "Patient PR status neg.", "Patient PR status is negative.",
         "PR      NEG", "PR Status           NEGATIVE",
         "PR: NEG", "Progesterone Receptors : Negative",
         "Progesterone      NEG", "Progesterone           NEGATIVE             0",
         "Progesterone Receptor NEG", "Progesterone-Receptor NEGATIVE", "Progesterone Receptor-negative",
         "Progesterone Receptor status NEG", "Progesterone-Receptor status is negative",
         "Patient is negative for Progesterone Receptor" };

    private final String[] PR_NA_SENTENCES = {
         "Patient PR status unknown.", "Patient PR status is unknown.",
         "Patient PR status indeterminate.", "Patient PR status is indeterminate.",
         "Patient PR status equivocal.", "Patient PR status is equivocal.",
         "Patient PR status not assessed.", "Patient PR status is not assessed.",
         "Patient PR status NA.", "Patient PR status is N/A.",
         "PR      unknown", "PR Status           unknown",
         "PR      indeterminate", "PR Status           indeterminate",
         "PR      equivocal", "PR Status           equivocal",
         "PR      not assessed", "PR Status           not assessed",
         "PR      NA", "PR Status           NA",
         "PR      N/A", "PR Status           N/A",
         "Progesterone      unknown", "Progesterone      indeterminate",
         "Progesterone      equivocal", "Progesterone      not assessed",
         "Progesterone      NA", "Progesterone      N/A" };


    private final String[] HER2_POS_SENTENCES = {
         "HER2 BY IHC POSITIVE",
         "Patient tested HER2+ last week.", "Patient tested HER2 + last week.",
         "Patient tested HER2+pos last week.", "Patient tested HER2+positive last week.",
         "Patient tested HER2 pos last week.", "Patient tested HER2 positive last week.",
         "Patient tested HER2 status pos last week.", "Patient tested HER2 status positive last week.",
         "Patient HER2 status pos.", "Patient HER2 status is positive.",
         "HER-2      POS", "HER2 Status           POSITIVE",
         "Patient tested HER2/neu+ last week.", "Patient tested HER2/neu + last week.",
         "Patient tested HER2/neu+pos last week.", "Patient tested HER2/neu+positive last week.",
         "Patient tested HER2/neu pos last week.", "Patient tested HER2/neu positive last week.",
         "Patient tested HER2/neu status pos last week.", "Patient tested HER2/neu status positive last week.",
         "Patient HER2/neu status pos.", "Patient HER2/neu status is positive.",
         "HER-2/neu      POS", "HER2/neu Status           POSITIVE",
         "HER2: POS", "HER2/neu Receptors : Positive",
         "HER2/neu Receptor POS", "HER2/neu-Receptor POSITIVE",
         "HER2/neu Receptor status POS", "HER2/neu-Receptor status is positive",
         "HER2 PROTEIN EXPRESSION (0-3+):    3+",
         "Patient is positive for Her-2/neu",
         "HER-2/NEU          Positive                        3+",
         " IMMUNOHISTOCHEMISTRY [NEGATIVE:0,1+; EQUIVOCAL 2+; POSITIVE 3+]\n" +
         "RESULT                       SCORE\n" +
         "HER-2/NEU           Positive                         3+\n" +
         "TUMOR CELL PROLIFERATION INDEX (Ki-67)\n",
         "HER-2/neu expression is positive",
         "Her2/Neu oncoprotein       positive",
         "Her2/Neu oncoprotein       RESULT:  positive",
         "HER-2-NEU (CERB B-2): POSITIVE"
   };

    private final String[] HER2_NEG_SENTENCES = {
          "Patient tested HER2- last week.", "Patient tested HER2 - last week.",
          "Patient tested HER2-neg last week.", "Patient tested HER2-negative last week.",
          "Patient tested HER2 neg last week.", "Patient tested HER2 negative last week.",
          "Patient tested HER2 status neg last week.", "Patient tested HER2 status negative last week.",
          "Patient HER2 status neg.", "Patient HER2 status is negative.",
          "HER-2      NEG", "HER2 Status           NEGATIVE",
          "Patient tested HER2/neu- last week.", "Patient tested HER2/neu - last week.",
          "Patient tested HER2/neu-neg last week.", "Patient tested HER2/neu-negative last week.",
          "Patient tested HER2/neu neg last week.", "Patient tested HER2/neu negative last week.",
          "Patient tested HER2/neu status neg last week.", "Patient tested HER2/neu status negative last week.",
          "Patient HER2/neu status neg.", "Patient HER2/neu status is negative.",
          "HER2: NEG", "HER2/neu Receptors : Negative",
          "HER-2/neu      NEG", "HER2/neu Status           NEGATIVE",
          "HER2/neu Receptor NEG", "HER2/neu-Receptor NEGATIVE",
          "HER2/neu Receptor status NEG", "HER2/neu-Receptor status is negative",
          "HER2 PROTEIN EXPRESSION (0-3+):    0+", "HER2 PROTEIN EXPRESSION (0-3+):    1+",
          "Patient is negative for Her-2/neu", "Patient is negative for Her-2", "Her-2/neu -",
          "HER2/NEU:      negative, 0", "HER-2/NEU NEGATIVE WITHA KI-67 INDEX OF 45%.",
          "HER-2/neu (HercepTest) : Negative for overexpression",
          "HER2 / neu STATUS - SCORE - 0 ( NEGATIVE FOR OVEREXPRESSION )",
          "HER2/neu STATUS\n" +
          "    - SCORE - 0 (NEGATIVE FOR OVEREXPRESSION)" };

    private final String[] HER2_NA_SENTENCES = {
         "Patient HER2 status unknown.", "Patient HER2 status is unknown.",
         "Patient HER2 status indeterminate.", "Patient HER2 status is indeterminate.",
         "Patient HER2 status equivocal.", "Patient HER2 status is equivocal.",
         "Patient HER2 status not assessed.", "Patient HER2 status is not assessed.",
         "Patient HER2 status NA.", "Patient HER2 status is N/A.",
         "HER2      unknown", "HER2 Status           unknown",
         "HER2      indeterminate", "HER2 Status           indeterminate",
         "HER2      equivocal", "HER2 Status           equivocal",
         "HER2      not assessed", "HER2 Status           not assessed",
         "HER2      NA", "HER2 Status           NA",
         "HER-2      N/A", "HER2 Status           N/A",
         "Patient HER2/neu status unknown.", "Patient HER2/neu status is unknown.",
         "Patient HER2/neu status indeterminate.", "Patient HER2/neu status is indeterminate.",
         "Patient HER2/neu status equivocal.", "Patient HER2/neu status is equivocal.",
         "Patient HER2/neu status not assessed.", "Patient HER2/neu status is not assessed.",
         "Patient HER2/neu status NA.", "Patient HER2/neu status is N/A.",
         "HER2/neu      unknown", "HER2/neu Status           unknown",
         "HER2/neu      indeterminate", "HER2/neu Status           indeterminate",
         "HER2/neu      equivocal", "HER2/neu Status           equivocal",
         "HER2/neu      not assessed", "HER2/neu Status           not assessed",
         "HER-2/neu      NA", "HER2/neu Status           NA",
         "HER2/neu      N/A", "HER2/neu Status           N/A",
         "HER2 PROTEIN EXPRESSION (0-3+):    2+" };


    private final String[] TRIPLE_NEG_SENTENCES = {
         "Patient tested Triple- last week.", "Patient tested Triple - last week.",
         "Patient tested Triple-neg last week.", "Patient tested Triple-negative last week.",
         "Patient tested Triple neg last week.", "Patient tested Triple negative last week."
   };

   @Test
   public void testMultipleMention() {
      for ( String sentence : MULTIPLE_MENTION_SENTENCES ) {
         testMultiples( sentence );
      }
   }

   private void testMultiples( final String sentence ) {
      final List<SimpleStatus> statuses
            = StatusFinder.getReceptorStatuses( sentence );
      assertEquals( "Expect three Hormone Receptors in " + sentence, 3, statuses.size() );
      for ( SimpleStatus status : statuses ) {
         System.out.println( status._uri + "   " + sentence.substring( status._begin, status._end ) );
      }
   }

   @Test
   public void testMultipleMention2() {
      for ( String sentence : MULTIPLE_MENTION_SENTENCES_2 ) {
         testMultiples2( sentence );
      }
   }

   private void testMultiples2( final String sentence ) {
      final List<SimpleStatus> statuses
            = StatusFinder.getReceptorStatuses2( sentence );
      assertEquals( "Expect three Hormone Receptors in " + sentence, 3, statuses.size() );
      for ( SimpleStatus status : statuses ) {
         System.out.println( status._uri + "   " + sentence.substring( status._begin, status._end ) );
      }
   }

    private void testReceptorStatus( final String[] sentences,
                                     final SimpleStatusType expectedType,
                                     final StatusValue expectedValue ) {
      final List<SimpleStatus> statuses = new ArrayList<>( 1 );
      for ( String sentence : sentences ) {
         statuses.addAll( StatusFinder.getReceptorStatuses( sentence ) );
         statuses.addAll( StatusFinder.getReceptorStatuses2( sentence ) );
         assertEquals( "Expect one Hormone Receptor in " + sentence, 1, statuses.size() );
         for ( SimpleStatus status : statuses ) {
            System.out.println( status._uri + "   " + sentence.substring( status._begin, status._end ) );
         }
         statuses.clear();
      }
   }

   private void testTripleStatus( final String[] sentences,
                                         final StatusValue expectedValue ) {
      final List<SimpleStatus> statuses = new ArrayList<>( 3 );
      for ( String sentence : sentences ) {
         statuses.addAll( StatusFinder.getReceptorStatuses( sentence ) );
         assertEquals( "Expect one Triple Negative in " + sentence, 1, statuses.size() );
         for ( SimpleStatus status : statuses ) {
            System.out.println( status._uri + "   " + sentence.substring( status._begin, status._end ) );
         }
         statuses.clear();
      }
   }

   @Test
   public void testErPos() {
      testReceptorStatus( ER_POS_SENTENCES, SimpleStatusType.ER, StatusValue.POSITIVE );
   }

   @Test
   public void testErNeg() {
      testReceptorStatus( ER_NEG_SENTENCES, SimpleStatusType.ER, StatusValue.NEGATIVE );
   }

   @Test
   public void testErNA() {
      testReceptorStatus( ER_NA_SENTENCES, SimpleStatusType.ER, StatusValue.UNKNOWN );
   }

   @Test
   public void testPrPos() {
      testReceptorStatus( PR_POS_SENTENCES, SimpleStatusType.PR, StatusValue.POSITIVE );
   }

   @Test
   public void testPrNeg() {
      testReceptorStatus( PR_NEG_SENTENCES, SimpleStatusType.PR, StatusValue.NEGATIVE );
   }

   @Test
   public void testPrNA() {
      testReceptorStatus( PR_NA_SENTENCES, SimpleStatusType.PR, StatusValue.UNKNOWN );
   }


   @Test
   public void testHer2Pos() {
      testReceptorStatus( HER2_POS_SENTENCES, SimpleStatusType.HER2, StatusValue.POSITIVE );
   }

   @Test
   public void testHer2Neg() {
      testReceptorStatus( HER2_NEG_SENTENCES, SimpleStatusType.HER2, StatusValue.NEGATIVE );
   }

   @Test
   public void testHer2NA() {
      testReceptorStatus( HER2_NA_SENTENCES, SimpleStatusType.HER2, StatusValue.UNKNOWN );
   }

   @Test
   public void testTripleNeg() {
      testTripleStatus( TRIPLE_NEG_SENTENCES, StatusValue.NEGATIVE );
   }

   private String [] her2Examples = new String [] {
           "HER-2/NEU STATUS: negative",
           "HER-2 STATUS: negative",
           "HER-2 STATUS: negative".toLowerCase(),
           "HER-2 STATUS: neg",
           "HER-2/NEU ONCOGENE STATUS: negative",  // appears in Train
           "HER-2/NEU ONCOGENE: negative",         // variation
           "HER-2 ERBB2: negative",
           "HER 2   ERBB2: negative",
   };

      private String [] her2ExamplesFromTrain = new String [] {
              "HER-2/NEU ONCOGENE STATUS: negative",  // appears in Train

   };
   @Test
   public void testHer2ExamplesFromTrain() {
      testReceptorStatus( her2ExamplesFromTrain, SimpleStatusType.HER2, StatusValue.NEGATIVE );
   }

   private String [] prExamplesFromTrain = new String [] {
   };
   @Test
   public void testPrExamplesFromTrain() {
      testReceptorStatus( prExamplesFromTrain, SimpleStatusType.PR, StatusValue.NEGATIVE );
   }

   @Test
   public void testOneRegEx() { // for testing a regex before incorporating into one that's used by DeepPhe
      String REGEX = "\\n\\s*ER:(\\s*DCIS)?(\\s*IS)?";
      Pattern p = Pattern.compile( REGEX, Pattern.CASE_INSENSITIVE );
      Matcher m;

      m = p.matcher("\n  ER: DCIS POSITIVE"); // OK to have whitespace between ER and preceding newline
      assertTrue(m.find());
      m = p.matcher("ER: DCIS POSITIVE");  // not OK to be missing a newline (requires not to be on first line, which should be OK
      assertFalse(m.find());

      m = p.matcher("\nER: DCIS POSITIVE END");
      assertTrue(m.find());

      // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
      String LONGER = REGEX + " POSITIVE";
      Pattern longer = Pattern.compile( LONGER, Pattern.CASE_INSENSITIVE );

      m = longer.matcher("\nsomething ER: POSITIVE"); // not OK for non white space between newline and ER
      assertFalse(m.find());

      m = longer.matcher("\nER: POSITIVE"); // OK for DCIS and IS to be missing
      assertTrue(m.find());

      m = longer.matcher("\nER:  DCIS POSITIVE"); // OK for IS to be missing
      assertTrue(m.find());

      m = longer.matcher("\r\nER:  DCIS POSITIVE"); // OK for IS to be missing
      assertTrue(m.find());

      m = longer.matcher("\n  ER:  DCIS  IS POSITIVE");
      assertTrue(m.find());

      m = longer.matcher("\nER:  DCIS  IS"); // not OK for POSITIVE to be missing here
      assertFalse(m.find());

   }

   }

}
