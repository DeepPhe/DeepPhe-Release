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
import java.util.regex.Pattern;
import java.util.stream.Collectors;



/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/28/2019
 */
@PipeBitInfo(
      name = "GradeFinder",
      description = "Finds Complex Grade values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class GradeFinderNew extends AbstractSectionProcessor implements SectionProcessor,
                                                                              ListProcessor,
                                                                              SentenceProcessor {
   // Location of a value term in relation to the location of the name/trigger term
   private enum RelativeValueLocation {
      BEFORE,
      AFTER,
      CONTAINS,
      BEFORE_AFTER,
      NONE;
   }


   private interface RelativeLocationOwner {
      RelativeValueLocation getLocation();
   }

   static private class FoundLocatedName extends SpanScoreOwner implements RelativeLocationOwner {
      private final RelativeValueLocation _location;
      private FoundLocatedName( final Pair<Integer> span, final int score, final RelativeValueLocation location ) {
         super( span, score );
         _location = location;
      }
      public RelativeValueLocation getLocation() {
         return _location;
      }
   }


   static private final FoundLocatedName NO_FOUND_LOCATED_NAME
         = new FoundLocatedName( SpanOwner.NO_SPAN, 0, RelativeValueLocation.NONE );

   static private final Logger LOGGER = Logger.getLogger( "GradeFinder" );

   static private final String GRADE_URI = "Histologic_Grade";
   static private final String GRADE_CUI = "C0919553";
   static private final String GRADE_PREF_TEXT = "Histologic Grade";

   static private final Pattern VALUE_PATTERN
         = Pattern.compile( "\\s+\\(?(?:BLOOM\\-RICHARDSON )?(?:score)?\\)?:?\\s*[1-4]\\r?\\n?", Pattern.CASE_INSENSITIVE );


   private enum ListGradeName implements ScoreOwner, TextsOwner {
      HISTOLOGIC( 100, "nottingham combined histologic grade", "nottingham combined grade",
                  "combined histologic grade", "combined grade",
                  "histologic grade", "histologic differentiation" ),
      BY_GRADE( 0, "by grade", "by ajcc grade", "by nci grade", "by who grade" ),
      NUCLEAR( 0, "nuclear grade", "fuhrman grade" ),
      GLEASON( 0, "gleason grade" ),
      TOXICITY( 0, "toxicity grade", "ctcae grade", "ctcae v4 grade" ),
      AJCC( 90, "ajcc grade", "ajcc tumor grade",
            "ajcc gx", "ajcc g1", "ajcc g2", "ajcc g3", "ajcc g4" ),
      NCI( 90, "nci grade", "nci tumor grade" ),
      WHO( 90, "who grade", "who tumor grade" ),
      TUMOR( 90, "tumor grade", "tumour grade" ),
      SIMPLE( 70, "grade" );
      final private int _score;
      final private String[] _texts;
      ListGradeName( final int score, final String... texts ) {
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

   private enum SentenceGradeName implements ScoreOwner, TextsOwner {
      COMBINED( 100, RelativeValueLocation.AFTER, "nottingham combined histologic grade",
                "nottingham combined grade", "combined histologic grade", "combined grade" ),
      HISTOLOGIC( 100, RelativeValueLocation.AFTER, "histologic grade" ),
      HISTOLOGIC_DIFF( 100, RelativeValueLocation.BEFORE_AFTER, "histologic differentiation" ),
      BY_GRADE( 0, RelativeValueLocation.NONE, "by grade", "by ajcc grade", "by nci grade", "by who grade" ),
      NUCLEAR( 0, RelativeValueLocation.NONE, "nuclear grade", "fuhrman grade" ),
      GLEASON( 0, RelativeValueLocation.NONE, "gleason grade" ),
      TOXICITY( 0, RelativeValueLocation.NONE, "toxicity grade", "ctcae grade", "ctcae v4 grade" ),
      NOT_ASSESSED( 90, RelativeValueLocation.CONTAINS, "grade cannot be assessed" ),
      AJCC( 90, RelativeValueLocation.AFTER, "ajcc grade", "ajcc tumor grade",
            "ajcc gx", "ajcc g1", "ajcc g2", "ajcc g3", "ajcc g4" ),
      DIFFERENTIATED( 70, RelativeValueLocation.CONTAINS,
                      "well differentiated", "moderately differentiated", "poorly differentiated",
                      "well-differentiated", "moderately-differentiated", "poorly-differentiated",
                      "undifferentiated" ),
      PLASTIC( 50, RelativeValueLocation.CONTAINS, "anaplastic", "metaplastic" ),
      NCI( 90, RelativeValueLocation.AFTER, "nci grade", "nci tumor grade" ),
      WHO( 90, RelativeValueLocation.AFTER, "who grade", "who tumor grade" ),
      TUMOR( 90, RelativeValueLocation.AFTER, "tumor grade", "tumour grade" ),
      LEVEL( 80, RelativeValueLocation.CONTAINS, "high grade", "high-grade",
             "intermediate grade", "intermediate-grade",
             "low grade", "low-grade" ),
      SIMPLE( 60, RelativeValueLocation.BEFORE_AFTER, "grade" );
      final private int _score;
      final private RelativeValueLocation _location;
      final private String[] _texts;
      SentenceGradeName( final int score, final RelativeValueLocation location, final String... texts ) {
         _score = score;
         _location = location;
         _texts = texts;
      }
      public int getScore() {
         return _score;
      }
      public String[] getTexts() {
         return _texts;
      }
      RelativeValueLocation getValueLocation() {
         return _location;
      }
      static FoundLocatedName findGradeName( final int offset, final String text ) {
         final String lower = text.toLowerCase();
         for ( SentenceGradeName name : values() ) {
            for ( String nameText : name._texts ) {
               final int index = lower.indexOf( nameText );
               if ( index >= 0 ) {
                  return new FoundLocatedName( new Pair<>( offset + index, offset + index + nameText.length() ),
                                               name._score,
                                               name._location );
               }
            }
         }
         return NO_FOUND_LOCATED_NAME;
      }
   }


   private enum GradeValue implements ScoreOwner, CodeOwner, TextsOwner {
      NOT_ASSESSED( 90, "C0439673", "Unknown", "cannot be assessed", "unclassified" ),
      LOW_GRADE( 90, "C1282907", "Low_Grade", "low grade",
                 "well differentiated", "well-differentiated", "grade 1" ),
      INTERMEDIATE_GRADE( 90, "C1512863", "Intermediate_Grade", "intermediate grade",
                          "moderately differentiated", "moderately-differentiated", "grade 2" ),
      HIGH_GRADE( 90, "CL378245", "High_Grade", "high grade",
                  "poorly differentiated", "poorly-differentiated", "grade 3" ),
      UNDIFFERENTIATED_GRADE( 90, "C0205618", "Undifferentiated", "undifferentiated", "grade 4" ),
      UNDETERMINED( 70, "C0439673", "Unknown", "undetermined" ),
      LOW(  60, "C1282907", "Low_Grade", "low", "well" ),
      INTERMEDIATE( 60, "C1512863", "Intermediate_Grade", "intermediate", "moderately" ),
      HIGH( 60, "CL378245", "High_Grade", "high", "poorly" ),
      NOT_ASSESSED_G( 50, "C0439673", "Unknown", "gx" ),
      LOW_G( 50, "C1282907", "Low_Grade", "g1" ),
      INTERMEDIATE_G( 50, "C1512863", "Intermediate_Grade", "g2" ),
      HIGH_G( 50, "CL378245", "High_Grade", "g3" ),
      UNDIFFERENTIATED_G( 50, "C0205618", "Undifferentiated", "g4" ),
      ANAPLASTIC( 40, "C0205618", "Undifferentiated", "anaplastic" ),
      METAPLASTIC( 40, "CL378245", "High_Grade",  "metaplastic" ),
      ONE(  40, "C1282907", "Low_Grade", "1", "i" ),
      TWO( 40, "C1512863", "Intermediate_Grade", "2", "ii" ),
      THREE( 40, "CL378245", "High_Grade", "3", "iii" ),
      FOUR( 40, "C0205618", "Undifferentiated", "4", "iv" ),;

      // There is a grade 5, but we are not handling right now.
      final private int _score;
      final private String _cui;
      final private String _uri;
      final private String[] _texts;
      GradeValue(  final int score, final String cui, final String uri, final String... texts ) {
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
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Grade Values ..." );
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

   @Override
   public Collection<Pair<Integer>> processSentence( final JCas jCas, final Segment section, final Topic topic,
                                                     final Paragraph paragraph,
                                                     final Sentence sentence, final NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      LOGGER.info( "Processed Spans: " + jCas.getDocumentText().length() + "\n"
                   + noteDivisions.getProcessedSpans()
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() + " " )
                                  .collect(Collectors.joining( " ; " ) ) );
      LOGGER.info( "Available Spans: " + jCas.getDocumentText().length() + "\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect(Collectors.joining( " ; " ) ) );
      final String text = sentence.getCoveredText();
//      LOGGER.info( "Processing Sentence " + text );
      final FoundLocatedName foundName = SentenceGradeName.findGradeName( sentence.getBegin(), text );
      final int score = foundName.getScore();
      if ( score == 0 ) {
         return Collections.singletonList( new Pair<>( sentence.getBegin(), sentence.getEnd() ) );
      }
      final Pair<Integer> candidateSpan = getSentenceValueSpan( foundName.getLocation(),
                                                                 foundName.getSpan(),
                                                                 sentence.getBegin(),
                                                                 text.length() );
      final SpanScoreCodeOwner foundValue
            = SpanScoreCodeOwner.findSpanScoreCodeOwner( GradeValue.values(),
                                                         candidateSpan.getValue1(),
                                                         text.substring( candidateSpan.getValue1()-sentence.getBegin(),
                                                                         candidateSpan.getValue2()-sentence.getBegin() ) );
      if ( foundValue.getScore() == 0 ) {
         return Collections.singletonList( new Pair<>( sentence.getBegin(), sentence.getEnd() ) );
      }
      boolean madeGrade = false;
      if ( TextSpanUtil.hasOverlap( foundName.getSpan(), foundValue.getSpan() ) ) {
         // name and value overlap.  Try to resolve this.  e.g. name "grade cannot be assessed" and "high-grade"
         madeGrade = createOverlappingGrade( jCas, foundName, foundValue );
      }
      if ( !madeGrade ) {
         createGrade( jCas, foundName, foundValue );
      }
//      LOGGER.info( "Processed Sentence " + sentence.getBegin() + "," + sentence.getEnd() );
      return Collections.singletonList( new Pair<>( sentence.getBegin(), sentence.getEnd() ) );
   }

   static private boolean createOverlappingGrade( final JCas jCas,
                                                  final SpanScoreOwner foundName,
                                                  final SpanScoreCodeOwner foundValue ) {
      // name and value overlap.  Try to resolve this.  e.g. name "grade cannot be assessed" and "high-grade"
      final String valueText = jCas.getDocumentText()
                                  .substring( foundValue.getSpan().getValue1(),
                                              foundValue.getSpan().getValue2() )
                                  .toLowerCase();
      final int gradeIndex = valueText.indexOf( "grade" );
      if ( gradeIndex < 0 ) {
         return false;
      }
      Pair<Integer> nameSpan;
      Pair<Integer> valueSpan;
      if ( gradeIndex == 0 ) {
         LOGGER.info( "Adjusting spans for grade in beginning of " + valueText );
         nameSpan = new Pair<>( foundValue.getSpan().getValue1(),
                                foundValue.getSpan().getValue1() + 5 );
         valueSpan = new Pair<>( foundValue.getSpan().getValue1() + 6,
                                 foundValue.getSpan().getValue2() );
      } else if ( gradeIndex == valueText.length() - 5 ) {
         LOGGER.info( "Adjusting spans for grade in end of " + valueText );
         nameSpan = new Pair<>( foundValue.getSpan().getValue2() - 5,
                                foundValue.getSpan().getValue2() );
         valueSpan = new Pair<>( foundValue.getSpan().getValue1(),
                                 foundValue.getSpan().getValue2() - 6 );
      } else {
         return false;
      }
      final SpanScoreOwner adjustName = new SpanScoreOwner( nameSpan, foundName.getScore() );
      final SpanScoreCodeOwner adjustValue = new SpanScoreCodeOwner( valueSpan,
                                                     foundValue.getScore(),
                                                     foundValue.getCui(),
                                                     foundValue.getUri() );
      createGrade( jCas, adjustName, adjustValue );
      return true;
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
         final SpanScoreOwner foundName = SpanScoreOwner.findSpanScoreOwner( ListGradeName.values(),
                                                                                   heading.getBegin(),
                                                                                   heading.getCoveredText() );
         final int score = foundName.getScore();
         LOGGER.info( "Heading " + heading.getCoveredText() + " score " + score );
         if ( score == 0 ) {
            return Collections.singletonList( new Pair<>( list.getBegin(), list.getEnd() ) );
         }
      }
      final Map<SpanScoreOwner,FormattedListEntry> gradeEntries = new HashMap<>();
      for ( FormattedListEntry entry : InTreeListFinderUtil.getListEntries( list ) ) {
         final IdentifiedAnnotation name = entry.getName();
         if ( name == null ) {
//            LOGGER.warn( "No name in " + entry.getCoveredText() );
            continue;
         }
         final SpanScoreOwner foundName = SpanScoreOwner.findSpanScoreOwner( ListGradeName.values(),
                                                                                   name.getBegin(),
                                                                                   name.getCoveredText() );
         if ( foundName.getScore() == 0 ) {
//            LOGGER.info( "No Grade: " + entry.getCoveredText() );
            continue;
         }
//         LOGGER.info( "Grade Entry Name: " + foundName.getScore() + " " + name.getCoveredText() );
         gradeEntries.put( foundName, entry );
      }
      if ( !gradeEntries.isEmpty() ) {
         processCandidateEntries( jCas, gradeEntries );
      }
      LOGGER.info( "Processed List " + list.getBegin() + "," + list.getEnd() );
      return Collections.singletonList( new Pair<>( list.getBegin(), list.getEnd() ) );
   }

   static private void processCandidateEntries( final JCas jCas,
                                                final Map<SpanScoreOwner,FormattedListEntry> gradeEntries ) {
      final Map<Integer,List<SpanScoreOwner>> scoredNames
            = gradeEntries.keySet()
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
                  = processCandidateEntry( jCas, topScoreName, gradeEntries.get( topScoreName ) );
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
      final String valueText = value.getCoveredText();
      final SpanScoreCodeOwner foundValue
            = SpanScoreCodeOwner.findSpanScoreCodeOwner( GradeValue.values(), value.getBegin(), valueText );
      if ( foundValue.getScore() > 0 ) {
//         LOGGER.info( "Entry " + listEntry.getCoveredText() + " value scored "
//                      + foundName.getScore() + "," + foundValue.getScore() );
         createGrade( jCas, foundName, foundValue );
         return true;
      }
      return false;
   }


   static private void createGrade( final JCas jCas,
                                                final SpanScoreOwner foundName,
                                                final SpanScoreCodeOwner foundValue ) {
      final IdentifiedAnnotation grade = foundName.createAnnotation( jCas, GRADE_CUI, GRADE_URI,
                                                                     GRADE_PREF_TEXT );
      final IdentifiedAnnotation value = foundValue.createAnnotation( jCas );
      new RelationBuilder<DegreeOfTextRelation>().creator( DegreeOfTextRelation::new )
                                                 .name( "hasGrade" )
                                                 .annotation( grade )
                                                 .hasRelated( value )
                                                 .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                                 .confidence( 90 )
                                                 .build( jCas );
      LOGGER.info( "Created " + grade.getCoveredText() + " hasGrade " + value.getCoveredText() );
   }


   static private Pair<Integer> getSentenceValueSpan( final RelativeValueLocation location,
                                                       final Pair<Integer> nameSpan,
                                                       final int sentenceOffset,
                                                       final int sentenceLength ) {
      switch ( location ) {
         case BEFORE: {
            return new Pair<>( Math.max( sentenceOffset, nameSpan.getValue1() - 15 ),
                               nameSpan.getValue2() );
         }
         case BEFORE_AFTER: {
           return new Pair<>( Math.max( sentenceOffset, nameSpan.getValue1() - 15 ),
                              Math.min( sentenceOffset + sentenceLength, nameSpan.getValue2()+5 ) );
         }
         case AFTER: {
            return new Pair<>( nameSpan.getValue1(),
                               Math.min( sentenceOffset + sentenceLength, nameSpan.getValue2()+5 ) );
         }
      }
      return nameSpan;
   }

}

