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
      name = "Ki67Finder",
      description = "Finds Ki - 67 values.",
      dependencies = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION,
      products = PipeBitInfo.TypeProduct.GENERIC_RELATION,
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class Ki67Finder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "Ki67Finder" );

   static private final String KI67_URI = "MKI67_Gene";

   //   static private final Pattern SKIP_PATTERN = Pattern.compile( "(?:)" );
   static private final Pattern VALUE_PATTERN
         = Pattern.compile( "(?:>|< ?)?[0-9]{1,2}(?:\\.[0-9]{1,2} ?)? ?%(?: positive)?", Pattern.CASE_INSENSITIVE );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Ki - 67 Values ..." );

      findKi67( jCas );

   }

   static public void findKi67( final JCas jCas ) {
      final Collection<IdentifiedAnnotation> ki67s
            = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, KI67_URI );
      if ( ki67s.isEmpty() ) {
         return;
      }
      final String docText = jCas.getDocumentText();
      for ( IdentifiedAnnotation ki67 : ki67s ) {
//         final String nextText = docText.substring( ki67.getEnd(), Math.min( docText.length(), ki67.getEnd() + 20 ) );
         final String nextText = docText.substring( ki67.getEnd(), Math.min( docText.length(), ki67.getEnd() + 50 ) );
//         LOGGER.info( "Testing Ki-67 " + nextText );
//         final Matcher noMatcher = SKIP_PATTERN.matcher( nextText );
//         if ( noMatcher.find() ) {
//            LOGGER.info( "Skipping non-PSA " + docText.substring( psa.getEnd() + noMatcher.start(), psa.getEnd() + noMatcher.end() ) );
//            continue;
//         }
         final Matcher matcher = VALUE_PATTERN.matcher( nextText );
         if ( matcher.find() ) {
            final Collection<IdentifiedAnnotation> values = UriAnnotationFactory.createIdentifiedAnnotations( jCas,
                  ki67.getEnd() + matcher.start(),
                  ki67.getEnd() + matcher.end(), "MKI67_Positive", SemanticGroup.FINDING, "T184" );
//            LOGGER.info( "MKI67_Positive " + docText.substring( ki67.getEnd() + matcher.start(), ki67.getEnd() + matcher.end() ) );
         }
      }
   }

}
