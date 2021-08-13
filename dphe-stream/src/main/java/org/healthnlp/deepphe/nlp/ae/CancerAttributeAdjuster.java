package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.ArrayList;
import java.util.Collection;

import static org.apache.ctakes.typesystem.type.constants.CONST.*;

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
      LOGGER.info( "Adjusting Neoplasm mention attributes ..." );

      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();

      final Collection<String> massNeoplasmUris = new ArrayList<>( UriConstants.getMassNeoplasmUris( graphDb ) );
      final Collection<IdentifiedAnnotation> allTumors = Neo4jOntologyConceptUtil.getAnnotationsByUris( jCas, massNeoplasmUris );
      final String docText = jCas.getDocumentText();
      for ( IdentifiedAnnotation tumor : allTumors ) {
         final int begin = tumor.getBegin();
         final int end = tumor.getEnd();
         if ( end + 10 < docText.length() ) {
            final String following = docText.substring( end, end + 10 ).toLowerCase();
            if ( following.contains( ":" )
                 && ( following.contains( "n/a" )
                      || following.contains( "n / a" )
                      || following.contains( "not i" )
                      || following.contains( "not appl" ) ) ) {
               tumor.setGeneric( true );
            }
            if ( following.contains( " nos " ) || following.contains( "(nos)" ) || following.contains( " nos." ) ) {
               tumor.setPolarity( NE_POLARITY_NEGATION_ABSENT );
            }
         }

         if ( begin > 40 ) {
            final String preceding = docText.substring( begin - 40, begin ).toLowerCase().replaceAll( "\\s+", " " );
            if ( tumor.getPolarity() != NE_POLARITY_NEGATION_PRESENT ) {
               if ( (preceding.contains( "free of" ) && !preceding.contains( " not " ))
                    || preceding.contains( "negative for " )
                    || preceding.contains( "uninvolved" )
                    || preceding.contains( "no lymph nodes submitted or found" ) ) {
                  tumor.setPolarity( NE_POLARITY_NEGATION_PRESENT );
               }
            }
            if ( preceding.contains( "check next " ) ) {
               tumor.setGeneric( true );
            } else if ( preceding.contains( "possible" )
                        || preceding.contains( "possibility of" )
                        || preceding.contains( "suggestive for" ) ) {
               tumor.setUncertainty( NE_UNCERTAINTY_PRESENT );
            } else if ( preceding.contains( "history" ) && !preceding.contains( "no history of" ) ) {
               tumor.setUncertainty( NE_UNCERTAINTY_ABSENT );
            }
         }
         if ( end + 35 < docText.length() ) {
            final String following = docText.substring( end, end + 35 ).toLowerCase();
            if ( following.contains( "not otherwise specified" ) ) {
               tumor.setPolarity( NE_POLARITY_NEGATION_ABSENT );
            } else if ( following.contains( "not identified" ) || following.contains( "none identified" ) ) {
               tumor.setPolarity( NE_POLARITY_NEGATION_PRESENT );
            }
         }
      }

   }


}
