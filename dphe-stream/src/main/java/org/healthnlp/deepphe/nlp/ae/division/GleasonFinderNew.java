package org.healthnlp.deepphe.nlp.ae.division;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.core.util.owner.*;
import org.apache.ctakes.core.util.paragraph.ParagraphProcessor;
import org.apache.ctakes.core.util.relation.RelationBuilder;
import org.apache.ctakes.core.util.section.AbstractSectionProcessor;
import org.apache.ctakes.core.util.section.SectionProcessor;
import org.apache.ctakes.core.util.sentence.SentenceProcessor;
import org.apache.ctakes.core.util.topic.TopicProcessor;
import org.apache.ctakes.core.util.treelist.InTreeListFinderUtil;
import org.apache.ctakes.core.util.treelist.ListProcessor;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.util.owner.SpanScoreCodeOwner.NO_SPAN_SCORE_CODE;

/**
 * @author SPF , chip-nlp
 * @since {3/9/2022}
 */
@PipeBitInfo(
      name = "GleasonFinder",
      description = "Finds Gleason Grade values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR
)
public class GleasonFinderNew extends AbstractSectionProcessor implements SectionProcessor,
                                                                          ListProcessor,
                                                                          SentenceProcessor {

   static private final Logger LOGGER = Logger.getLogger( "GleasonFinder" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Gleason Values ..." );
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


   private enum ListGleasonName implements ScoreOwner, TextsOwner {
      GRADE( 100, "gleason grade", "gleasons grade", "gleason's grade" ),
      PATTERN( 100, "gleason pattern", "gleasons pattern", "gleason's pattern" ),
      SCORE( 100, "gleason score", "gleasons score", "gleason's score" ),
      SIMPLE( 70, "gleason", "gleasons", "gleason's" );
      final private int _score;
      final private String[] _texts;
      ListGleasonName( final int score, final String... texts ) {
         _score = score;
         _texts = texts;
      }
      public int getScore() {
         return _score;
      }
      public String[] getTexts() {
         return _texts;
      }
   }


   private enum GleasonGroup implements ScoreOwner, CodeOwner, TextsOwner {
//         implements Normal {
      GROUP_1( 90, "CL525030", "Gleason_Grade_Group_1" ),
      GROUP_2( 80, "CL525029", "Gleason_Grade_Group_2" ),
      GROUP_3( 80, "CL525028", "Gleason_Grade_Group_3" ),
      GROUP_3_MAYBE( 50, "CL525028", "Gleason_Grade_Group_3" ),
      GROUP_4( 90, "CL525035", "Gleason_Grade_Group_4" ),
      GROUP_5( 90, "CL525034", "Gleason_Grade_Group_5" ),
      UNKNOWN( 20, "C0439673", "Unknown" );
      final private int _score;
      final private String _cui;
      final private String _uri;
      GleasonGroup(  final int score, final String cui, final String uri ) {
         _score = score;
         _cui = cui;
         _uri = uri;
      }
      public int getScore() {
         return _score;
      }
      public String getUri() {
         return _uri;
      }
      public String getCui() {
         return _cui;
      }
      public String getNormalName() {
         return name();
      }
      public String[] getTexts() {
         return new String[0];
      }
      // Gleason group is 1-5: 1 = score <=6; 2 = score 7, 3+4; 3 = score 7, 4+3; 4 = score 8; 5 = score 9 and 10
      static private GleasonGroup getGroup( final GleasonPattern primary, final GleasonPattern secondary,
                                           final GleasonScore score ) {
         if ( score.ordinal() <= GleasonScore.SCORE_6.ordinal() ) {
            return GleasonGroup.GROUP_1;
         }
         if ( primary == GleasonPattern.PATTERN_3 && secondary == GleasonPattern.PATTERN_4 ) {
            return GROUP_2;
         }
         if ( primary == GleasonPattern.PATTERN_4 && secondary == GleasonPattern.PATTERN_3 ) {
            return GROUP_3;
         }
         if ( score == GleasonScore.SCORE_7 ) {
            return GROUP_3_MAYBE;
         }
         if ( score == GleasonScore.SCORE_8 ) {
            return GROUP_4;
         }
         if ( score == GleasonScore.SCORE_9 || score == GleasonScore.SCORE_10 ) {
            return GROUP_5;
         }
         return UNKNOWN;
      }
      static private SpanScoreCodeOwner findValue( final SpanScoreCodeOwner primary,
                                                  final SpanScoreCodeOwner secondary,
                                                  final SpanScoreCodeOwner score ) {
         final GleasonPattern primaryPattern = Arrays.stream( GleasonPattern.values() )
                                                     .filter( g -> primary.getUri()
                                                                          .equals( g.getUri() ) )
                                                     .findAny()
                                                     .orElse( GleasonPattern.UNKNOWN );
         final GleasonPattern secondaryPattern = Arrays.stream( GleasonPattern.values() )
                                                       .filter( g -> secondary.getUri()
                                                                              .equals( g.getUri() ) )
                                                       .findAny()
                                                       .orElse( GleasonPattern.UNKNOWN );
         final GleasonScore gleasonScore = Arrays.stream( GleasonScore.values() )
                                                 .filter( g -> score.getUri()
                                                                    .equals( g.getUri() ) )
                                                 .findAny()
                                                 .orElse( GleasonScore.UNKNOWN );
         final GleasonGroup group = getGroup( primaryPattern, secondaryPattern, gleasonScore );
         Pair<Integer> groupSpan = NO_SPAN;
         int start = -1;
         int end = -1;
         if ( TextSpanUtil.isValidSpan( score.getSpan() ) ) {
            end = score.getSpan()
                       .getValue2();
         } else if ( TextSpanUtil.isValidSpan( secondary.getSpan() ) ) {
            end = secondary.getSpan()
                           .getValue2();
         }
         if ( end > -1 ) {
            if ( TextSpanUtil.isValidSpan( primary.getSpan() ) ) {
               start = primary.getSpan()
                              .getValue1();
            } else if ( TextSpanUtil.isValidSpan( score.getSpan() ) ) {
               start = score.getSpan()
                            .getValue1();
            }
         }
         if ( start > -1 && end > start ) {
            groupSpan = new Pair<>( start, end );
         }
         return new SpanScoreCodeOwner( groupSpan, group.getScore(), group.getCui(), group.getUri() );
      }
  }

   // Gleason Score is the number 1-10.  It is made from the Primary and Secondary Gleason Grades.
//   static private final String PRIMARY_GLEASON_URI = "Primary_Gleason_Pattern";
//   static private final String PRIMARY_GLEASON_CUI = "C1273604";
//   static private final String SECONDARY_GLEASON_URI = "Secondary_Gleason_Pattern";
//   static private final String SECONDARY_GLEASON_CUI = "C1273605";
   private enum GleasonPattern implements TextsOwner, ScoreOwner, CodeOwner {
      PATTERN_1( 60, "C1276439", "Gleason_Pattern_1", "1"  ),
      PATTERN_2( 60, "C1276440", "Gleason_Pattern_2", "2" ),
      PATTERN_3( 60, "C1276441", "Gleason_Pattern_3", "3" ),
      PATTERN_4( 60, "C0332329", "Gleason_Pattern_4", "4" ),
      PATTERN_5( 60, "C1276443", "Gleason_Pattern_5", "5" ),
      UNKNOWN( 20, "C0439673", "Unknown" );
      final private int _score;
      final private String _cui;
      final private String _uri;
      final private String[] _texts;
      GleasonPattern(  final int score, final String cui, final String uri, final String... texts ) {
         _score = score;
         _cui = cui;
         _uri = uri;
         _texts = texts;
      }
      public int getScore() {
         return _score;
      }
      public String getCui() {
         return _cui;
      }
      public String getUri() {
         return _uri;
      }
      public String[] getTexts() {
         return _texts;
      }
      private boolean matchesText( final String text ) {
         return Arrays.asList( _texts )
                      .contains( text );
      }
      static public GleasonPattern getPattern( final String text ) {
         if ( text == null ) {
            return UNKNOWN;
         }
         return Arrays.stream( values() ).filter( p -> p.matchesText( text ) ).findAny().orElse( UNKNOWN );
      }
      static SpanScoreCodeOwner findValue( final String text, final Pair<Integer> span ) {
         if ( !TextSpanUtil.isValidSpan( span ) ) {
            return NO_SPAN_SCORE_CODE;
         }
         final GleasonPattern pattern = GleasonPattern.getPattern( text );
         return new SpanScoreCodeOwner( span, pattern.getScore(), pattern.getCui(), pattern.getUri() );
      }
   }

//      private enum GleasonScore implements TextListNormal, ScoreOwner, CodeOwner {
   private enum GleasonScore implements TextsOwner, ScoreOwner, CodeOwner {
      SCORE_2( 30, "C0332327", "Gleason_Score_2", "2" ),
      SCORE_3( 30, "C0332328", "Gleason_Score_3", "3" ),
      SCORE_4( 30, "C1276442", "Gleason_Score_4", "4" ),
      SCORE_5( 30, "C0332330", "Gleason_Score_5", "5" ),
      SCORE_6( 60, "C0332331", "Gleason_Score_6", "6" ),
      SCORE_7( 60, "C0332332", "Gleason_Score_7", "7" ),
      SCORE_8( 60, "C0332333", "Gleason_Score_8", "8" ),
      SCORE_9( 60, "C0332334", "Gleason_Score_9", "9" ),
      SCORE_10( 60, "C0332335", "Gleason_Score_10", "10" ),
      UNKNOWN( 20, "C0439673", "Unknown" );

      final private int _score;
      final private String _cui;
      final private String _uri;
      final private String[] _texts;
      GleasonScore(  final int score, final String cui, final String uri, final String... texts ) {
         _score = score;
         _cui = cui;
         _uri = uri;
         _texts = texts;
      }
      public int getScore() {
         return _score;
      }
      public String getUri() {
         return _uri;
      }
      public String getCui() {
         return _cui;
      }
      public String[] getTexts() {
         return _texts;
      }
      private boolean matchesText( final String text ) {
         return Arrays.asList( _texts )
                      .contains( text );
      }
      static public GleasonScore getScore( final String text ) {
         if ( text == null || text.isEmpty() ) {
            return UNKNOWN;
         }
         return Arrays.stream( values() ).filter( p -> p.matchesText( text ) ).findAny().orElse( UNKNOWN );
      }
      static public GleasonScore getScore( final String primaryText, final String secondaryText ) {
         if ( primaryText == null || secondaryText == null ) {
            return UNKNOWN;
         }
         final String score = computeScore( primaryText, secondaryText );
         return getScore( score );
      }

      static private String computeScore( final String primary, final String secondary ) {
         try {
            final int prime = Integer.parseInt( primary );
            final int second = Integer.parseInt( secondary );
            return "" + (prime+second);
         } catch ( NumberFormatException nfE ) {
            return "";
         }
      }
      static SpanScoreCodeOwner findValue( final String text, final Pair<Integer> span ) {
         if ( !TextSpanUtil.isValidSpan( span ) ) {
            return NO_SPAN_SCORE_CODE;
         }
         final GleasonPattern pattern = GleasonPattern.getPattern( text );
         return new SpanScoreCodeOwner( span, pattern.getScore(), pattern.getCui(), pattern.getUri() );
      }
      static SpanScoreCodeOwner findValue( final String primary, final String secondary,
                                   final Pair<Integer> primarySpan, final Pair<Integer> secondarySpan ) {
         if ( !TextSpanUtil.isValidSpan( primarySpan ) || !TextSpanUtil.isValidSpan( secondarySpan ) ) {
            return NO_SPAN_SCORE_CODE;
         }
         return findValue( computeScore( primary, secondary ), new Pair<>( primarySpan.getValue1(),
                                                                           secondarySpan.getValue2() ) );
      }
   }


   static private final String NAME_REGEX = "(?<NAME>Gleason'?s? ?(?:Grade|Score|Pattern)?)";
   static private final String PATTERN_REGEX = "(?:(?<PRIMARY>[1-5]) ?\\+ ?(?<SECONDARY>[1-5]))?";
   static private final String TOTAL_REGEX
         = "\\s*:?=?\\-?,?(?:\\s*total)?(?:\\s*combined)?(?:\\s*gleason)?(?:\\s*score)?(?:\\s*of)?\\s*";
   static private final String SCORE_REGEX = "(?<SCORE>[2-9]|10)(?:\\s*\\/\\s*10)?";

   static private final String FULL_VALUE_REGEX = PATTERN_REGEX
                                                  + TOTAL_REGEX
                                                  + SCORE_REGEX;
   static private final Pattern FULL_VALUE_PATTERN
         = Pattern.compile( FULL_VALUE_REGEX, Pattern.CASE_INSENSITIVE );

   static private final Pattern FULL_GLEASON_PATTERN
         = Pattern.compile( NAME_REGEX + "\\s*:?=?\\s*" + FULL_VALUE_REGEX, Pattern.CASE_INSENSITIVE );

   static private void createSentenceGleasons( final JCas jCas, final String windowText ) {
      if ( windowText.length() < 3 ) {
         return;
      }
      int comments = windowText.lastIndexOf( "COMMENTS" );
      if ( comments < 0 ) {
         // There may be a Comments section with grade groups listed, which creates FPs.
         comments = Integer.MAX_VALUE;
      }
      final Matcher fullMatcher = FULL_GLEASON_PATTERN.matcher( windowText );
      while ( fullMatcher.find() ) {
         if ( fullMatcher.start() <= comments ) {
            parseFullMatcher( jCas, fullMatcher );
         }
      }
   }


   static private void parseFullMatcher( final JCas jCas, final Matcher fullMatcher ) {
      final Pair<Integer> nameSpan = new Pair<>( fullMatcher.start( "NAME" ),
                                                 fullMatcher.end( "NAME" ) );
      if ( nameSpan.getValue1() == -1 ) {
         LOGGER.warn( "No Name for Gleason Score " + jCas.getDocumentText().substring( fullMatcher.start(),
                                                                                       fullMatcher.end() ) );
         return;
      }
      final SpanScoreOwner foundName  = SpanScoreOwner.findSpanScoreOwner( ListGleasonName.values(),
                                                             fullMatcher.group( "NAME" ),
                                                             nameSpan );
      final String primaryText = fullMatcher.group( "PRIMARY" );
      final String secondaryText = fullMatcher.group( "SECONDARY" );
      final String scoreText = fullMatcher.group( "SCORE" );
      if ( primaryText == null && secondaryText == null && scoreText == null ) {
         LOGGER.warn( "No Value for Gleason Score " + nameSpan.getValue1() + "," + nameSpan.getValue2()
                      + " " + fullMatcher.group( "NAME" ) );
         return;
      }
      final Pair<Integer> primarySpan = new Pair<>( fullMatcher.start( "PRIMARY" ),
                                                    fullMatcher.end( "PRIMARY" ) );
      final Pair<Integer> secondarySpan = new Pair<>( fullMatcher.start( "SECONDARY" ),
                                                      fullMatcher.end( "SECONDARY" ) );
      final Pair<Integer> scoreSpan =  new Pair<>( fullMatcher.start( "SCORE" ),
                                                   fullMatcher.end( "SCORE" ) );
      final SpanScoreCodeOwner primary = GleasonPattern.findValue( primaryText, primarySpan );
      final SpanScoreCodeOwner secondary = GleasonPattern.findValue( secondaryText, secondarySpan );
      final SpanScoreCodeOwner score = scoreText != null
                               ? GleasonScore.findValue( scoreText, scoreSpan )
                               : GleasonScore.findValue( primaryText, secondaryText, primarySpan, secondarySpan );
      final SpanScoreCodeOwner group = GleasonGroup.findValue( primary, secondary, score );
      createAnnotations( jCas, foundName, primary, secondary, score, group );
   }


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
      String lookupText = text;
      final int grouping = lookupText.indexOf( "Prognostic Grade Group" );
      if ( grouping >= 0 ) {
         lookupText = lookupText.substring( 0, grouping );
      }
      createSentenceGleasons( jCas, lookupText );
      return Collections.singletonList( new Pair<>( sentence.getBegin(), sentence.getEnd() ) );
   }


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
                                  .collect(Collectors.joining( " ; " ) ) );

      LOGGER.info( "DocText length: " + jCas.getDocumentText().length() + " Available spans:\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect(Collectors.joining( " ; " ) ) );
      final IdentifiedAnnotation heading = list.getHeading();
      if ( heading != null && !heading.getCoveredText().isEmpty() ) {
         final SpanScoreOwner foundName = SpanScoreOwner.findSpanScoreOwner( ListGleasonName.values(),
                                                                                   heading.getBegin(),
                                                                                   heading.getCoveredText() );
         final int score = foundName.getScore();
         LOGGER.info( "Heading " + heading.getCoveredText() + " score " + score );
         if ( score == 0 ) {
            return Collections.singletonList( new Pair<>( list.getBegin(), list.getEnd() ) );
         }
      }
      final Map<SpanScoreOwner,FormattedListEntry> gleasonEntries = new HashMap<>();
      for ( FormattedListEntry entry : InTreeListFinderUtil.getListEntries( list ) ) {
         final IdentifiedAnnotation name = entry.getName();
         if ( name == null ) {
//            LOGGER.warn( "No name in " + entry.getCoveredText() );
            continue;
         }
         final SpanScoreOwner foundName = SpanScoreOwner.findSpanScoreOwner( ListGleasonName.values(),
                                                                                   name.getBegin(),
                                                                                   name.getCoveredText() );
         if ( foundName.getScore() == 0 ) {
//            LOGGER.info( "No Grade: " + entry.getCoveredText() );
            continue;
         }
//         LOGGER.info( "Grade Entry Name: " + foundName.getScore() + " " + name.getCoveredText() );
         gleasonEntries.put( foundName, entry );
      }
      if ( !gleasonEntries.isEmpty() ) {
         processCandidateEntries( jCas, gleasonEntries );
      }
      LOGGER.info( "Processed List " + list.getBegin() + "," + list.getEnd() );
      return Collections.singletonList( new Pair<>( list.getBegin(), list.getEnd() ) );
   }

   static private void processCandidateEntries( final JCas jCas,
                                                final Map<SpanScoreOwner,FormattedListEntry> gleasonEntries ) {
      final Map<Integer, List<SpanScoreOwner>> scoredNames
            = gleasonEntries.keySet()
                          .stream()
                          .collect( Collectors.groupingBy( SpanScoreOwner::getScore ) );
      final List<Integer> reverseScores = scoredNames.keySet()
                                                     .stream()
                                                     .sorted( Comparator.comparingInt( Integer::intValue ).reversed() )
                                                     .collect( Collectors.toList() );
      for ( Integer score : reverseScores ) {
         final List<SpanScoreOwner> topScoreNames = scoredNames.get( score );
         for ( SpanScoreOwner topScoreName : topScoreNames ) {
            final boolean found
                  = processCandidateEntry( jCas, topScoreName, gleasonEntries.get( topScoreName ) );
            if ( found ) {
               break;
            }
         }
      }
   }


   static private boolean processCandidateEntry( final JCas jCas,
                                                 final SpanScoreOwner foundName, final FormattedListEntry listEntry ) {
      final IdentifiedAnnotation value = listEntry.getValue();
      if ( value == null ) {
//         LOGGER.warn( "No value in " + listEntry.getCoveredText() );
         return false;
      }
      final Matcher valueMatcher = FULL_VALUE_PATTERN.matcher( value.getCoveredText() );
      boolean found = false;
      while ( valueMatcher.find() ) {
         found |= parseValueMatcher( jCas, foundName, valueMatcher );
      }
      return found;
   }


   static private boolean parseValueMatcher( final JCas jCas, final SpanScoreOwner foundName, final Matcher valueMatcher ) {
      final String primaryText = valueMatcher.group( "PRIMARY" );
      final String secondaryText = valueMatcher.group( "SECONDARY" );
      final String scoreText = valueMatcher.group( "SCORE" );
      if ( primaryText == null && secondaryText == null && scoreText == null ) {
         LOGGER.warn( "No Value for Gleason Score " + foundName.getSpan().getValue1()
                      + "," + foundName.getSpan().getValue2() );
         return false;
      }
      final Pair<Integer> primarySpan = new Pair<>( valueMatcher.start( "PRIMARY" ),
                                                    valueMatcher.end( "PRIMARY" ) );
      final Pair<Integer> secondarySpan = new Pair<>( valueMatcher.start( "SECONDARY" ),
                                                      valueMatcher.end( "SECONDARY" ) );
      final Pair<Integer> scoreSpan =  new Pair<>( valueMatcher.start( "SCORE" ),
                                                   valueMatcher.end( "SCORE" ) );
      final SpanScoreCodeOwner primary = GleasonPattern.findValue( primaryText, primarySpan );
      final SpanScoreCodeOwner secondary = GleasonPattern.findValue( secondaryText, secondarySpan );
      final SpanScoreCodeOwner score = scoreText != null
                               ? GleasonScore.findValue( scoreText, scoreSpan )
                               : GleasonScore.findValue( primaryText, secondaryText, primarySpan, secondarySpan );
      final SpanScoreCodeOwner group = GleasonGroup.findValue( primary, secondary, score );
      return createAnnotations( jCas, foundName, primary, secondary, score, group );
   }






   static private final String GLEASON_GRADE_URI = "Gleason_Grading_System";
   static private final String GLEASON_GRADE_CUI = "C0332326";
   static private final String GLEASON_GRADE_PREFTEXT = "Gleason Grade";


   // TODO - throw the spans in here, maybe create a group, then create annotations and relations
   // name hasGleasonPrimary, hasGleasonSecondary, hasGleasonScore, hasGleasonGroup.
   // Set the group span to cover primary through score as applicable.
   static private boolean createAnnotations( final JCas jCas,
                                          final SpanScoreOwner name,
                                          final SpanScoreCodeOwner primary,
                                          final SpanScoreCodeOwner secondary,
                                          final SpanScoreCodeOwner score,
                                          final SpanScoreCodeOwner group ) {
      boolean created = false;
      final IdentifiedAnnotation gleason = name.createAnnotation( jCas, GLEASON_GRADE_CUI,
                                                                  GLEASON_GRADE_URI, GLEASON_GRADE_PREFTEXT );
      if ( TextSpanUtil.isValidSpan( primary.getSpan() ) ) {
         created = true;
         final IdentifiedAnnotation gPrimary = primary.createAnnotation( jCas );
         new RelationBuilder<DegreeOfTextRelation>().creator( DegreeOfTextRelation::new )
                                                    .name( "hasGleasonPrimary" )
                                                    .annotation( gleason )
                                                    .hasRelated( gPrimary )
                                                    .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                                    .confidence( 90 )
                                                    .build( jCas );
         LOGGER.info( "Created " + gleason.getCoveredText() + " hasGleasonPrimary " + gPrimary.getCoveredText() );
      }
      if ( TextSpanUtil.isValidSpan( secondary.getSpan() ) ) {
         created = true;
         final IdentifiedAnnotation gSecondary = secondary.createAnnotation( jCas );
         new RelationBuilder<DegreeOfTextRelation>().creator( DegreeOfTextRelation::new )
                                                    .name( "hasGleasonSecondary" )
                                                    .annotation( gleason )
                                                    .hasRelated( gSecondary )
                                                    .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                                    .confidence( 90 )
                                                    .build( jCas );
         LOGGER.info( "Created " + gleason.getCoveredText() + " hasGleasonSecondary " + gSecondary.getCoveredText() );
      }
      if ( TextSpanUtil.isValidSpan( score.getSpan() ) ) {
         created = true;
         final IdentifiedAnnotation gScore = score.createAnnotation( jCas );
         new RelationBuilder<DegreeOfTextRelation>().creator( DegreeOfTextRelation::new )
                                                    .name( "hasGleasonScore" )
                                                    .annotation( gleason )
                                                    .hasRelated( gScore )
                                                    .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                                    .confidence( 90 )
                                                    .build( jCas );
         LOGGER.info( "Created " + gleason.getCoveredText() + " hasGleasonScore " + gScore.getCoveredText() );
      }
      if ( TextSpanUtil.isValidSpan( group.getSpan() ) ) {
         created = true;
         final IdentifiedAnnotation gGroup = group.createAnnotation( jCas );
         new RelationBuilder<DegreeOfTextRelation>().creator( DegreeOfTextRelation::new )
                                                    .name( "hasGleasonGroup" )
                                                    .annotation( gleason )
                                                    .hasRelated( gGroup )
                                                    .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                                    .confidence( 90 )
                                                    .build( jCas );
         LOGGER.info( "Created " + gleason.getCoveredText() + " hasGleasonGroup " + gGroup.getCoveredText() );
      }
      return created;
   }




}
