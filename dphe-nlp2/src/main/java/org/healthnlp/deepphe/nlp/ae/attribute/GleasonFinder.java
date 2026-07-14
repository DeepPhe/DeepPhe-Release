package org.healthnlp.deepphe.nlp.ae.attribute;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/30/2019
 */
@PipeBitInfo (
      name = "GleasonFinder",
      description = "For deepphe.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class GleasonFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "GleasonFinder" );

   static private final String GLEASON_URI = "GleasonGradingSystem";
   // "GleasonScoreForProstateCancer" and "GleasonGradingSystem"


   // TODO also need pattern for "Gleason's Score:  6 (3+3)" and simpler "Gleason score 3+3"

   static private final String NAME_REGEX = "Gleason'?s? ?(?:Grade|Score|Pattern)?";
   static private final String VALUE_REGEX = "[3-5] ?\\+ ?[3-5]";
   static private final String EQUALS_REGEX
         = "\\s*:?=?\\-?,?(?:\\s*total)?(?:\\s*combined)?(?:\\s*gleason)?(?:\\s*score)?(?:\\s*of)?\\s*";
   static private final String GRADE_REGEX = "(?<GRADE>[6-9]|10)(?:\\s*\\/\\s*10)?";

   static private final String FULL_VALUE_REGEX = "(?:" + VALUE_REGEX + ")?"
                                                  + EQUALS_REGEX
                                                  + GRADE_REGEX;

   static private final Pattern VALUE_PATTERN = Pattern.compile( FULL_VALUE_REGEX );

   static private final Pattern FULL_PATTERN
         = Pattern.compile( NAME_REGEX + "\\s*:?=?\\s*" + FULL_VALUE_REGEX, Pattern.CASE_INSENSITIVE );


   static private final class SimpleGrade {
      private final int _matchBegin;
      private final int _begin;
      private final int _end;
      private final String _uri;

      private SimpleGrade( final int matchBegin, final int begin, final int end, final String uri ) {
         _matchBegin = matchBegin;
         _begin = begin;
         _end = end;
         _uri = uri;
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Gleason Score Values ..." );

      // Remove gleasons found by dictionary lookup
      final Collection<IdentifiedAnnotation> gleasons
            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, "GleasonGradingSystem" );
      gleasons.forEach( IdentifiedAnnotation::removeFromIndexes );

      for ( Segment section : JCasUtil.select( jCas, Segment.class ) ) {
         findGleasonGrades( jCas, section );
      }

   }

   static public List<IdentifiedAnnotation> findGleasonGrades( final JCas jcas, final Annotation lookupWindow ) {
      String lookupText = lookupWindow.getCoveredText();
      final int grouping = lookupText.indexOf( "Prognostic Grade Group" );
      if ( grouping >= 0 ) {
         lookupText = lookupText.substring( 0, grouping );
      }
      final List<SimpleGrade> grades = getGleasonGrades( lookupText );
      if ( grades.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<IdentifiedAnnotation> plainGrades
            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jcas, lookupWindow, "CTCAE_Grade_Finding" );
      final int windowStartOffset = lookupWindow.getBegin();
      final List<IdentifiedAnnotation> annotations = new ArrayList<>( grades.size() );
      for ( SimpleGrade grade : grades ) {
         final IdentifiedAnnotation annotation = AnnotationFactory.createAnnotation( jcas,
               windowStartOffset + grade._begin, windowStartOffset + grade._end,
               DpheGroup.DISEASE_GRADE_QUALIFIER, grade._uri,
               "", grade._uri );
         annotations.add( annotation );
//         annotations.addAll(
//               UriAnnotationFactory.createIdentifiedAnnotations( jcas,
//                     windowStartOffset + grade._begin,
////                     windowStartOffset + grade._end, grade._uri, SemanticGroup.FINDING, "T184" ) );
//                     windowStartOffset + grade._end, grade._uri, SemanticGroup.FINDING, "T201" ) );

         plainGrades.stream()
                     .filter( a -> a.getBegin() >= windowStartOffset + grade._matchBegin )
                     .filter( a -> a.getEnd() <= windowStartOffset + grade._end )
                     .forEach( IdentifiedAnnotation::removeFromIndexes );
      }
      return annotations;
   }

   static private List<SimpleGrade> getGleasonGrades( final String lookupWindow ) {
      if ( lookupWindow.length() < 3 ) {
         return new ArrayList<>();
      }
      int comments = lookupWindow.lastIndexOf( "COMMENTS" );
      if ( comments < 0 ) {
         // There may be a Comments section with grade groups listed, which creates FPs.
         comments = Integer.MAX_VALUE;
      }
      final List<SimpleGrade> grades = new ArrayList<>();
      final Matcher fullMatcher = FULL_PATTERN.matcher( lookupWindow );
      while ( fullMatcher.find() ) {
         if ( fullMatcher.start() > comments ) {
            continue;
         }
         final String gradeText = fullMatcher.group( "GRADE" );
         final int gradeStart = fullMatcher.start( "GRADE" );
         final int gradeEnd = fullMatcher.end( "GRADE" );
         grades.add( new SimpleGrade( fullMatcher.start(), gradeStart, fullMatcher.end(), getGradeUri( gradeText ) ) );
      }
      return grades;
   }

   // Malignant_Neoplasm_by_Grade
   // Low_Grade_Malignant_Neoplasm Intermediate_Grade_Malignant_Neoplasm High_Grade_Malignant_Neoplasm
   //   The lowest Gleason score is 6, which is a low-grade cancer.
   //   A Gleason score of 7 is a medium-grade cancer, and a score of 8, 9, or 10 is a high-grade cancer.
   static private String getGradeUri( final String gradeText ) {
      // Children of Gleason_Grade_Finding_For_Prostatic_Cancer
      return "Gleason_Score_" + gradeText;
//      if ( gradeText.equalsIgnoreCase( "6" ) ) {
//         return "Low_Grade_Malignant_Neoplasm";
//      }
//      if ( gradeText.equals( "7" ) ) {
//         return "Intermediate_Grade_Malignant_Neoplasm";
//      }
//      return "High_Grade_Malignant_Neoplasm";
   }

}