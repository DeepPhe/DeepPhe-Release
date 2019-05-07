package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/17/2018
 */
@PipeBitInfo(
      name = "CancerAttributeAdjuster",
      description = "Negate expressions like -Free of Cancer-.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class CancerAttributeAdjuster extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "CancerAttributeAdjuster" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjust Neoplasm mention attributes ..." );

      final Collection<String> tumorCancerUris = new ArrayList<>( UriConstants.getTumorUris() );
//      tumorCancerUris.addAll( UriConstants.getCancerUris() );
      final Collection<IdentifiedAnnotation> allTumors
            = tumorCancerUris.stream()
                             .map( u -> Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, u ) )
                             .flatMap( Collection::stream )
                             .filter( a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT )
                             .collect( Collectors.toList() );
      final String docText = jCas.getDocumentText();
      for ( IdentifiedAnnotation tumor : allTumors ) {
         final int begin = tumor.getBegin();
         if ( begin > 30 ) {
            final String window = docText.substring( begin - 30, begin ).toLowerCase().replaceAll("\\s+", " " );
            if ( (window.contains( "free of" ) && !window.contains( " not " )) || window.contains( "negative for " ) ) {
               tumor.setPolarity( CONST.NE_POLARITY_NEGATION_PRESENT );
            } else if ( window.contains( "check next " ) ) {
               tumor.setGeneric( true );
            } else if ( window.contains( "possible" )
                        || window.contains( "possibility of" )
                        || window.contains( "suggestive for" ) ) {
               tumor.setUncertainty( CONST.NE_UNCERTAINTY_PRESENT );
            } else if ( window.contains( "history" ) && !window.contains( "no history of" )) {
               tumor.setUncertainty( CONST.NE_UNCERTAINTY_ABSENT );
            }
         }
         final int end = tumor.getEnd();
         if ( end + 10 < docText.length() ) {
            final String window = docText.substring( end, end + 10 ).toLowerCase();
            if ( window.contains( ": n/a" ) || window.contains( ": n / a" ) || window.contains( ": not i" ) ) {
               tumor.setGeneric( true );
            }
         }
      }

      LOGGER.info( "Finished." );
   }


}
