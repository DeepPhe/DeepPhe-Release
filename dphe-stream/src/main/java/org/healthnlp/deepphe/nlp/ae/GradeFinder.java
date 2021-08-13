package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriAnnotationFactory;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/28/2019
 */
@PipeBitInfo(
      name = "GradeFinder",
      description = "Finds Complex Grade values.",
      dependencies = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION,
      products = PipeBitInfo.TypeProduct.GENERIC_RELATION,
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class GradeFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "GradeFinder" );

   static private final String GRADE_URI = "Histological_Grade";

   static private final Pattern VALUE_PATTERN
         = Pattern.compile( "\\s+\\(?(?:BLOOM\\-RICHARDSON )?(?:score)?\\)?:?\\s*[1-4]\\r?\\n?", Pattern.CASE_INSENSITIVE );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Grade Values ..." );

      findGrade( jCas );

   }

   static public void findGrade( final JCas jCas ) {
      final Collection<IdentifiedAnnotation> grades
            = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, GRADE_URI );

      if ( grades.isEmpty() ) {
         return;
      }
      final String docText = jCas.getDocumentText();
      for ( IdentifiedAnnotation grade : grades ) {
         final String nextText = docText.substring( grade.getEnd(), Math.min( docText.length(), grade.getEnd() + 25 ) );
//         LOGGER.info( "Testing Grade " + nextText );
         final Matcher matcher = VALUE_PATTERN.matcher( nextText );
         if ( matcher.find() ) {
            String gradeUri = "Undifferentiated";
            final String matchText = nextText.substring( matcher.start(), matcher.end() ).trim();
            if ( matchText.endsWith( "1" ) ) {
               gradeUri = "Low_Grade";
            } else if ( matchText.endsWith( "2" ) ) {
               gradeUri = "Intermediate_Grade";
            } else if ( matchText.endsWith( "3" ) ) {
               gradeUri = "High_Grade";
            }
            final Collection<IdentifiedAnnotation> values = UriAnnotationFactory.createIdentifiedAnnotations( jCas,
                  grade.getBegin(), grade.getEnd() + matcher.end(), gradeUri, SemanticGroup.FINDING, "T184" );
            grade.removeFromIndexes();
//            LOGGER.info( gradeUri + " " + docText.substring( grade.getBegin(), grade.getEnd() + matcher.end() ) );
         }
      }
   }

}
