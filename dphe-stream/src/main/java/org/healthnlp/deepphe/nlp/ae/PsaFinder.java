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

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/28/2019
 */
@PipeBitInfo(
      name = "PsaFinder",
      description = "Finds Prostate Specific Antigen values.",
      dependencies = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION,
      products = PipeBitInfo.TypeProduct.GENERIC_RELATION,
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class PsaFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PsaFinder" );

   static private final String PSA_URI = "Prostate_Specific_Antigen";

   static private final Pattern SKIP_PATTERN = Pattern.compile( "[0-9]{3}\\.[0-9]{2}" );
   static private final Pattern VALUE_PATTERN
         = Pattern.compile( "[0-9]{1,2}(?:\\.[0-9]{1,4})?(?: ?ng\\/mL)?", Pattern.CASE_INSENSITIVE );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Prostate Specific Antigen Values ..." );

      findPsa( jCas );

   }

   static public void findPsa( final JCas jCas ) {
//      final Collection<IdentifiedAnnotation> psas
//            = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, PSA_URI );
      final Collection<IdentifiedAnnotation> psas
            = Neo4jOntologyConceptUtil.getAnnotationsByUris( jCas, Arrays.asList( PSA_URI, "PSA_Progression" ) );
      if ( psas.isEmpty() ) {
         return;
      }
      final String docText = jCas.getDocumentText();
      for ( IdentifiedAnnotation psa : psas ) {
         final String nextText = docText.substring( psa.getEnd(), Math.min( docText.length(), psa.getEnd() + 20 ) );
//         LOGGER.info( "Testing PSA " + nextText );
         final Matcher noMatcher = SKIP_PATTERN.matcher( nextText );
         if ( noMatcher.find() ) {
//            LOGGER.info( "Skipping non-PSA " + docText.substring( psa.getEnd() + noMatcher.start(), psa.getEnd() + noMatcher.end() ) );
            continue;
         }
         final Matcher matcher = VALUE_PATTERN.matcher( nextText );
         if ( matcher.find() ) {
            final Collection<IdentifiedAnnotation> values = UriAnnotationFactory.createIdentifiedAnnotations( jCas,
                  psa.getEnd() + matcher.start(),
                  psa.getEnd() + matcher.end(), "PSA_Level_Finding", SemanticGroup.FINDING, "T184" );
//            LOGGER.info( "PSA_Level_Finding " + docText.substring( psa.getEnd() + matcher.start(), psa.getEnd() + matcher.end() ) );
         }
      }
   }

}
