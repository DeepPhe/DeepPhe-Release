package org.healthnlp.deepphe.nlp.ae.division;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.core.util.normal.Polarity;
import org.apache.ctakes.core.util.owner.*;
import org.apache.ctakes.core.util.paragraph.ParagraphProcessor;
import org.apache.ctakes.core.util.relation.RelationBuilder;
import org.apache.ctakes.core.util.section.AbstractSectionProcessor;
import org.apache.ctakes.core.util.section.SectionProcessor;
import org.apache.ctakes.core.util.sentence.SentenceProcessor;
import org.apache.ctakes.core.util.topic.TopicProcessor;
import org.apache.ctakes.core.util.treelist.ListProcessor;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {3/10/2022}
 */
@PipeBitInfo(
      name = "BiomarkerFinder",
      description = "Finds Biomarker values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR
)
public class BiomarkerFinderNew extends AbstractSectionProcessor implements SectionProcessor,
                                                                            ListProcessor,
                                                                            SentenceProcessor {
   static private final Logger LOGGER = Logger.getLogger( "BiomarkerFinder" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Biomarker Values ..." );
      super.process( jCas );
   }

   @Override
   public TopicProcessor getTopicProcessor() {
      return null;
   }

   @Override
   public ParagraphProcessor getParagraphProcessor() {
      return null;
   }

   @Override
   public ListProcessor getListProcessor() {
      return this;
   }

   @Override
   public SentenceProcessor getSentenceProcessor() {
      return this;
   }








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
   static private final String REGEX_NEGATIVE
         = "-?neg(?:ative)?|(?:not amplified)|(?:no [a-z] detected)|(?:non-? ?detected)";
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


   // TODO: In lists there can be preceding texts like "Result: "
//   private enum BiomarkerValue implements ScoreOwner, CodeOwner, PatternOwner {
//      ER_VALUE( REGEX_POS_NEG_UNK_NA, true ),
//      PR_VALUE( REGEX_POS_NEG_UNK_NA, true ),
//      HER2_VALUE( REGEX_POS_NEG_UNK_NA ),
//      KI67_VALUE( "(?:>|< ?)?[0-9]{1,2}(?:\\.[0-9]{1,2} ?)? ?%(?: positive)?", true ),
//      BRCA1_VALUE( "" ),
//      BRCA2_VALUE( "" ),
//      ALK_VALUE( "(?:" + REGEX_POS_NEG_UNK_NA +")|(?:no rearrangement)", true ),
//      EGFR_VALUE( "(?:" + REGEX_POS_NEG_UNK_NA +")|(?:not mutant)|(?:no mutations?)", true ),
//      BRAF_VALUE( "" ),
//      ROS1_VALUE( REGEX_POS_NEG_UNK_NA, true ),
//      PDL1_VALUE( "[0-9]{1,2} ?%(?: high expression)?" ),
//      MSI_VALUE( "stable" ),
//      KRAS_VALUE( REGEX_POS_NEG_UNK_NA, true ),
//      PSA_VALUE( "[0-9]{1,2}\\.[0-9]{1,4}" ),
//      PSA_EL_VALUE( "(?:" + REGEX_ELEVATED + ")|(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")", true );
//
//      final Pattern _pattern;
//      final boolean _canPrecede;
//      final boolean _plusMinus;
//      BiomarkerValue( final String regex ) {
//         this( regex, false );
//      }
////      <T extends CodeOwner & PatternOwner> BiomarkerValue( final T CodePatternOwner ) {
////         this( regex, false );
////      }
//      BiomarkerValue( final String regex, final boolean canPrecede ) {
//         _pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
//         _canPrecede = canPrecede;
//         _plusMinus = REGEX_POS_NEG_UNK.equals( regex );
//      }
//      public int getScore() {
//         return 70;
//      }
//      public String getCui() {
//         return null;
//      }
//      public String getUri() {
//         return null;
//      }
//      public Pattern getPattern() {
//         return _pattern;
//      }
//   }

//   static private final ScoreCodePatternOwner NO_REARRANGEMENT
//         = new ScoreCodePatternOwner( 80, "", "", "no rearrangement" );
//   static private final ScoreCodePatternOwner NO_MUTATION
//         = new ScoreCodePatternOwner( 80, "", "", "(?:not mutant)|(?:no mutations?)" );
   static private final ScoreCodePatternOwner GT_LT_DECIMAL_POSITIVE
         = new ScoreCodePatternOwner( 70, "", "", "(?:>|< ?)?[0-9]{1,2}(?:\\.[0-9]{1,2} ?)? ?%(?: positive)?" );
   static private final ScoreCodePatternOwner NUMBER_EXPRESSION
         = new ScoreCodePatternOwner( 80, "", "", "[0-9]{1,2} ?%(?: high expression)?" );
   static private final ScoreCodePatternOwner DECIMAL
         = new ScoreCodePatternOwner( 70, "", "", "[0-9]{1,2}\\.[0-9]{1,4}" );
   static private final ScoreCodePatternOwner ELEVATED
         = new ScoreCodePatternOwner( 70, "", "", "rising|increasing|elevated|elvtd|raised|increased|strong|amplified" );
   static private final ScoreCodePatternOwner STABLE
         = new ScoreCodePatternOwner( 60, "", "", "stable" );

   static private final ScoreCodePatternOwner EMPTY
         = new ScoreCodePatternOwner( 50, "", "", "" );


   static private <T extends ScoreOwner & CodeOwner & PatternOwner> ScoreCodePatternOwner[] getOwnerArray(
         final T[] owners ) {
      return getOwnerArray( owners, new ScoreCodePatternOwner[0] );
   }

   @SafeVarargs
   static private <T extends ScoreOwner & CodeOwner & PatternOwner> ScoreCodePatternOwner[] getOwnerArray(
         final T[] owners, final T... owner ) {
      final ScoreCodePatternOwner[] scoreCodePatternOwners = new ScoreCodePatternOwner[ owners.length + owner.length ];
      for ( int i=0; i<owners.length; i++ ) {
         if ( owners[ i ] instanceof ScoreCodePatternOwner ) {
            scoreCodePatternOwners[ i ] = (ScoreCodePatternOwner)owners[ i ];
         } else {
            scoreCodePatternOwners[ i ] = new ScoreCodePatternOwner( owners[ i ] );
         }
      }
      for ( int i=0; i<owner.length; i++ ) {
         if ( owner[ i ] instanceof ScoreCodePatternOwner ) {
            scoreCodePatternOwners[ owners.length+i ] = (ScoreCodePatternOwner)owner[ i ];
         } else {
            scoreCodePatternOwners[ owners.length + i ] = new ScoreCodePatternOwner( owner[ i ] );
         }
      }
      return scoreCodePatternOwners;
   }


   private enum BiomarkerValue {
      ER_VALUE( Polarity.values(), true ),
      PR_VALUE( Polarity.values(), true ),
      HER2_VALUE( Polarity.values() ),
      KI67_VALUE( new ScoreCodePatternOwner[]{ GT_LT_DECIMAL_POSITIVE }, true ),
      PDL1_VALUE( NUMBER_EXPRESSION ),
      MSI_VALUE( STABLE ),
      PSA_VALUE( DECIMAL ),
      PSA_EL_VALUE( getOwnerArray( Polarity.values(), ELEVATED ), true );

      final private ScoreCodePatternOwner[] _delegates;
      final private boolean _canPrecede;
      <T extends ScoreOwner & CodeOwner & PatternOwner> BiomarkerValue( final ScoreCodePatternOwner delegate ) {
         _delegates = new ScoreCodePatternOwner[]{ delegate };
         _canPrecede = false;
      }
      <T extends ScoreOwner & CodeOwner & PatternOwner> BiomarkerValue( final T[] delegates ) {
         this( delegates, false );
      }
      <T extends ScoreOwner & CodeOwner & PatternOwner> BiomarkerValue( final T[] delegates,
                                                                        final boolean canPrecede ) {
         _delegates
               = Arrays.stream( delegates )
                       .map( ScoreCodePatternOwner::new )
                       .toArray( ScoreCodePatternOwner[]::new );
         _canPrecede = canPrecede;
      }
      public boolean canPrecede() {
         return _canPrecede;
      }
      public ScoreCodePatternOwner[] getDelegates() {
         return _delegates;
      }
      public boolean canPlusMinus( final PatternOwner patternOwner ) {
         return Polarity.NEGATIVE.equals( patternOwner ) || Polarity.POSITIVE.equals( patternOwner );
      }
   }


//   TODO  - These suddenly appeared for our dphe-cr UG3 progress report
//   CA-125, CEA, NRAS.

   // Can we add everything under Protein_or_Enzyme_Type_Measurement?  That has HER2, ER, PSA ...
   // Microsatellite_Instability_Analysis is under Molecular_Analysis ... We don't want the rest of that branch
   // so add it as special.  May end up doing that with other classes, but it seems special.
   // The problem then becomes normalizing the value.  Medium difficult in list, really tough in unstructured text.
   // Maybe create a relation "hasNormalizedValue" and point known biomarkers to known normal values.
   // Protein_or_Enzyme... can be tied to a parent class "Normalized_Values".
   // Would need to treat "NUMBER_EXPRESSION", "GT_LT_DECIMAL_POSITIVE" and "DECIMAL" specially ...
   // So ... On loading this class, check everything under Protein_or... for targets of hasNormalizedValue.
   // Dictionary lookup discovers biomarker mentions by synonym.
   // Dictionary lookup also discovers normalization terms by synonym.
   // This class attempts to match discovered biomarkers to discovered normalize terms in list or within window.
   // If the normalization type class is a number then it goes through its number detection routine.

   // Need to add "Confidence".  Add to ontology?

   // Maybe add "biomarker values" to ontology, add everything from the "Normal" implementations.
   // Then look in list value, look in window of text.  Could catch equivalent of "ER, PR Negative"
   // Speaking of text like "ER, PR Negative" - the use of "following annotation" as a stop prevents detection.

   private enum BiomarkerType implements ScoreOwner, CodeOwner, PatternOwner {
      ER_( "C3811131", "Estrogen_Receptor_Measurement", "hasER",
           "(?:Estrogen|ER(?!B)\\+?-?|ER:(\\s*DCIS)?(\\s*IS)?)",
           "", BiomarkerValue.ER_VALUE ),
      PR_( "C11846", "Progesterone_Receptor_Measurement", "hasPR",
           "(?:Progesterone|Pg?R\\+?-?|PR:(\\s*DCIS)?(\\s*IS)?)",
           "", BiomarkerValue.PR_VALUE ),
      HER2( "C3810543", "Human_Epidermal_Growth_Factor_Receptor_2_Measurement", "hasHER2",
            "(?:HER-? ?2(?: ?\\/?-? ?neu)?\\+?-?(?:\\s*ONCOGENE)?(?:\\s*\\(?ERBB2\\)?)?)",
            "", BiomarkerValue.HER2_VALUE ),
      KI67( "C4049944", "Ki67_Measurement", "hasKI67",
            "p?M?KI ?-? ?67(?: Antigen)?",
            "", BiomarkerValue.KI67_VALUE ),
      PDL1( "CL555536", "Programmed_Death_Ligand_1_Measurement", "hasPDL1",
            "(?:PDL1|PD-L1|CD247|B7|B7-H|B7H1|PDCD1L1|PDCD1LG1|(?:Programmed Cell Death 1 Ligand 1))"
            + "(?: Antigen)?(?: Molecule)?",
            "", BiomarkerValue.PDL1_VALUE ),
      MSI( "C1881824", "Microsatellite_Instability_Analysis", "hasMSI",
           "MSI|MSS|Microsatellite",
           "", BiomarkerValue.MSI_VALUE ),
      PSA( "C0201544", "Prostate_Specific_Antigen_Measurement", "hasPSA",
            "PSA(?: Prostate Specific Antigen)?|(?:Prostate Specific Antigen(?: [PSA])?)",
           "", BiomarkerValue.PSA_VALUE ),
      PSA_EL( "C1510830", "Age_Adjusted_PSA", "hasPSA_El",
            "PSA(?: Prostate Specific Antigen)?|(?:Prostate Specific Antigen(?: [PSA])?)",
              "", BiomarkerValue.PSA_EL_VALUE );

      final private String _cui;
      final private String _uri;
      final private String _relationName;
      final private Pattern _pattern;
      final private boolean _checkSkip;
      final private Pattern _skipPattern;
      final private BiomarkerValue _biomarkerValue;
      BiomarkerType( final String cui, final String uri, final String relationName,
                     final String typeRegex, final String skipRegex,
                     final BiomarkerValue biomarkerValue ) {
         _cui = cui;
         _uri = uri;
         _relationName = relationName;
         _pattern = Pattern.compile( typeRegex, Pattern.CASE_INSENSITIVE );
         if ( skipRegex.isEmpty() ) {
            _checkSkip = false;
            _skipPattern = null;
         } else {
            _checkSkip = true;
            _skipPattern = Pattern.compile( skipRegex, Pattern.CASE_INSENSITIVE );
         }
         _biomarkerValue = biomarkerValue;
      }
      public int getScore() {
         return 70;
      }
      public String getCui() {
         return _cui;
      }
      public String getUri() {
         return _uri;
      }
      public Pattern getPattern() {
         return _pattern;
      }
      public String getRelationName() {
         return _relationName;
      }
      public BiomarkerValue getValueType() {
         return _biomarkerValue;
      }
   }





   // TODO Add -normal- texts to Normal enums from BiomarkerFinderOld.  Then implement this.

   @Override
   public Collection<Pair<Integer>> processSentence( final JCas jCas, final Segment section, final Topic topic,
                                                     final Paragraph paragraph,
                                                     final Sentence sentence, final NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      LOGGER.info( "Processed Spans: " + jCas.getDocumentText()
                                             .length() + "\n"
                   + noteDivisions.getProcessedSpans()
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() + " " )
                                  .collect( Collectors.joining( " ; " ) ) );
      LOGGER.info( "Available Spans: " + jCas.getDocumentText()
                                             .length() + "\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );
      final String text = sentence.getCoveredText();
      LOGGER.info( "Processing Sentence " + text );

      final Collection<Integer> annotationBegins
            = JCasUtil.selectCovered( jCas, IdentifiedAnnotation.class, sentence )
                      .stream()
                       .filter( a -> ( a instanceof EventMention
                                       || a instanceof AnatomicalSiteMention ) )
                       .map( IdentifiedAnnotation::getBegin )
                       .collect( Collectors.toList() );
      // Go through once to find following annotations.
      // This will use up values that we do not want to find later as preceding values.
      final Map<BiomarkerType,List<SpanScoreCodeOwner>> precedingValuesToFind = new HashMap<>();
      for ( BiomarkerType biomarkerType : BiomarkerType.values() ) {
         final List<SpanScoreCodeOwner> biomarkerFinds
               = SpanScoreCodeOwner.findSpanScoreCodeOwners( biomarkerType, text );
         for ( SpanScoreCodeOwner biomarkerFound : biomarkerFinds ) {
            final int valueSearchEnd = getValueSearchEnd( biomarkerFound.getSpan(), text.length(), annotationBegins );
            final boolean valueFound = attemptValueFollows( jCas, biomarkerType, biomarkerFound, text, valueSearchEnd );
            if ( !valueFound && biomarkerType.getValueType()._canPrecede) {
               precedingValuesToFind.computeIfAbsent( biomarkerType, l -> new ArrayList<>() ).add( biomarkerFound );
            }
         }
      }
      if ( precedingValuesToFind.isEmpty() ) {
         return Collections.singletonList( new Pair<>( sentence.getBegin(), sentence.getEnd() ) );
      }
      final Collection<Integer> annotationEnds
            = JCasUtil.selectCovered( jCas, IdentifiedAnnotation.class, sentence )
                      .stream()
                      .filter( a -> ( a instanceof EventMention
                                      || a instanceof AnatomicalSiteMention ) )
                      .map( IdentifiedAnnotation::getEnd )
                      .collect( Collectors.toList() );
      for ( Map.Entry<BiomarkerType,List<SpanScoreCodeOwner>> biomarkerEntry : precedingValuesToFind.entrySet() ) {
         for ( SpanScoreCodeOwner biomarkerFound : biomarkerEntry.getValue() ) {
               final int valueSearchBegin = getValueSearchBegin( biomarkerFound.getSpan(), annotationEnds );
               attemptValuePrecedes( jCas, biomarkerEntry.getKey(), biomarkerFound, text, valueSearchBegin );

         }
      }
      return Collections.singletonList( new Pair<>( sentence.getBegin(), sentence.getEnd() ) );
   }

   static private int getValueSearchEnd( final Pair<Integer> biomarkerSpan,
                                        final int textLength,
                                        final Collection<Integer> annotationBegins ) {
      return annotationBegins.stream()
                             .mapToInt( b -> b )
                             .filter( b -> b >= biomarkerSpan.getValue2() && b < textLength )
                             .min()
                             .orElse( textLength );
   }

   static private int getValueSearchBegin( final Pair<Integer> biomarkerSpan,
                                         final Collection<Integer> annotationEnds ) {
      return annotationEnds.stream()
                             .mapToInt( b -> b )
                             .filter( b -> b <= biomarkerSpan.getValue1() )
                             .max()
                             .orElse( 0 );
   }


   static private boolean attemptValueFollows( final JCas jCas,
                                               final BiomarkerType biomarkerType,
                                               final SpanScoreCodeOwner biomarkerFound,
                                               final String text,
                                               final int valueSearchEnd ) {
      if ( text.isEmpty() ) {
         return false;
      }
      final BiomarkerValue biomarkerValue = biomarkerType.getValueType();
//      LOGGER.info( "Biomarker Span: " + biomarkerFound.getSpan().getValue1() + "," + biomarkerFound.getSpan().getValue2()
//                 + " text length " + text.length() + " end " + valueSearchEnd );
      final String fixedText = fixText( text.substring( biomarkerFound.getSpan().getValue2(),
                                                        valueSearchEnd ) );
      final SpanScoreCodeOwner foundValue
            = SpanScoreCodeOwner.findSpanScoreCodeOwner( biomarkerValue.getDelegates(),
                                                         biomarkerFound.getSpan().getValue2(),
                                                         fixedText );
      if ( foundValue.getScore() == 0 ) {
         return false;
      }
      createBiomarker( jCas, biomarkerType.getRelationName(), biomarkerFound, foundValue );
      return true;
   }

   static private boolean attemptValuePrecedes( final JCas jCas,
                                               final BiomarkerType biomarkerType,
                                               final SpanScoreCodeOwner biomarkerFound,
                                               final String text,
                                               final int valueSearchBegin ) {
      if ( biomarkerFound.getSpan().getValue1() - valueSearchBegin < 2 ) {
         return false;
      }
      final BiomarkerValue biomarkerValue = biomarkerType.getValueType();
      final String fixedText = fixText( text.substring( valueSearchBegin, biomarkerFound.getSpan().getValue1() ) );
      final SpanScoreCodeOwner foundValue
            = SpanScoreCodeOwner.findSpanScoreCodeOwner( biomarkerValue.getDelegates(),
                                                         valueSearchBegin,
                                                         fixedText );
      if ( foundValue.getScore() == 0 ) {
         return false;
      }
      createBiomarker( jCas, biomarkerType.getRelationName(), biomarkerFound, foundValue );
      return true;
   }


   static private void createBiomarker( final JCas jCas,
                                       final String relationName,
                                       final SpanScoreCodeOwner foundName,
                                       final SpanScoreCodeOwner foundValue ) {
      final IdentifiedAnnotation biomarker = foundName.createAnnotation( jCas );
      final IdentifiedAnnotation value = foundValue.createAnnotation( jCas );
      new RelationBuilder<DegreeOfTextRelation>().creator( DegreeOfTextRelation::new )
                                                 .name( relationName )
                                                 .annotation( biomarker )
                                                 .hasRelated( value )
                                                 .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                                 .confidence( 90 )
                                                 .build( jCas );
      LOGGER.info( "Created " + biomarker.getCoveredText() + " " + relationName + " " + value.getCoveredText() );
   }


   static private String fixText( final String text ) {
      // Sometimes value sets are in brackets.  e.g.  "ER: [pos;neg;unk] = neg"
      final int startBracket = text.indexOf( '[' );
      if ( startBracket < 0 ) {
         return text;
      }
      final int endBracket = text.indexOf( ']', startBracket );
      if ( endBracket < 0 ) {
         return text;
      }
      final char[] chars = text.toCharArray();
      for ( int i=startBracket+1; i<endBracket; i++ ) {
         chars[ i ] = 'V';
      }
      return new String( chars );
   }



//Estrogen Receptor:                   Performed on another specimen: SXX-XXXXX
//                                     Positive
//                                     % of Tumor Cells with Nuclear Positivity: 90
//                                     Average Intensity of Tumor Cell Nuclei Staining - Strong (3+)
//Progesterone Receptor:               Performed on another specimen: SXX-XXXXX
//                                     % of Tumor Cells with Nuclear Positivity: 5
//                                     Average Intensity of Tumor Cell Nuclei Staining - Moderate (2+)
//HER2 Immunohistochemistry:           Performed on another specimen: SXX-XXXXX
//                                     Equivocal (Score 2+)
//HER2 In-Situ Hybridization:          Performed on another specimen: SXX-XXXXX
//                                     Result: Equivocal
//                                     Average Number of HER2 Gene Copies per Cell: 4.6
//                                     Average Number of Chromosome 17 per Cell: 3.4
//                                     Ratio: 1.4
//                                     Number of Cells Counted: 60
//                                     Using HER2 / Chr17 ratio (dual probe assay)
//Estrogen Receptor:               Performed on another specimen: SXX-XXXXX
//                                 Positive
//                                 % of Tumor Cells with Nuclear Positivity: 90
//                                 Average Intensity of Tumor Cell Nuclei Staining - Strong (3+)
//Progesterone Receptor:           Performed on another specimen: SXX-XXXXX
//                                 Positive
//                                 % of Tumor Cells with Nuclear Positivity: 98
//                                 Average Intensity of Tumor Cell Nuclei Staining - Strong (3+)
//HER2 Immunohistochemistry:       Not performed
//HER2 In-Situ Hybridization:      Performed on another specimen: SXX-XXXXX
//                                 Result: Not Amplified
//                                 Average Number of HER2 Gene Copies per Cell: 1.9
//                                 Average Number of Chromosome 17 per Cell: 1.6
//                                 Ratio: 1.2

//         Immunohistochemistry performed at BWH demonstrates the following
//      staining profile in glomus tumor cells:
//
//        Positive - SMA
//        Negative - ERG, S100


//   NOTE: Immunohistochemistry provided for review shows the lesional cells stain
//positively for SOX-10 and MART-1.


   @Override
   public Collection<Pair<Integer>> processList( final JCas jCas, final Segment section, final Topic topic,
                                                 final Paragraph paragraph,
                                                 final FormattedList list, final NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      LOGGER.info( "List Type " + list.getListType() );
      LOGGER.info( "DocText length: " + jCas.getDocumentText().length() + " Processed spans:\n"
                   + noteDivisions.getProcessedSpans()
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );

      LOGGER.info( "DocText length: " + jCas.getDocumentText().length() + " Available spans:\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );
      final IdentifiedAnnotation heading = list.getHeading();
//      if ( heading != null && !heading.getCoveredText().isEmpty() ) {
//         final GleasonFinder.FoundName foundName = GleasonFinder.ListGleasonName.findName( heading.getBegin(), heading.getCoveredText() );
//         final int score = foundName.getScore();
//         LOGGER.info( "Heading " + heading.getCoveredText() + " score " + score );
//         if ( score == 0 ) {
//            return Collections.singletonList( new Pair<>( list.getBegin(), list.getEnd() ) );
//         }
//      }
//      final Map<GleasonFinder.FoundName,FormattedListEntry> gleasonEntries = new HashMap<>();
//      for ( FormattedListEntry entry : InTreeListFinderUtil.getListEntries( list ) ) {
//         final IdentifiedAnnotation name = entry.getName();
//         if ( name == null ) {
////            LOGGER.warn( "No name in " + entry.getCoveredText() );
//            continue;
//         }
//         final GleasonFinder.FoundName foundName = GleasonFinder.ListGleasonName.findName( name.getBegin(), name.getCoveredText() );
//         if ( foundName.getScore() == 0 ) {
////            LOGGER.info( "No Grade: " + entry.getCoveredText() );
//            continue;
//         }
////         LOGGER.info( "Grade Entry Name: " + foundName.getScore() + " " + name.getCoveredText() );
//         gleasonEntries.put( foundName, entry );
//      }
//      if ( !gleasonEntries.isEmpty() ) {
//         processCandidateEntries( jCas, gleasonEntries );
//      }
      LOGGER.info( "Processed List " + list.getBegin() + "," + list.getEnd() );
      return Collections.singletonList( new Pair<>( list.getBegin(), list.getEnd() ) );
   }



}
