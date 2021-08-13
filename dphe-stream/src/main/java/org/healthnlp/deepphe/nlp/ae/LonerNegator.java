package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/21/2020
 */
@PipeBitInfo(
      name = "LonerNegator",
      description = "For dphe-stream.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class LonerNegator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "LonerNegator" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Checking Paragraph Lone Mentions for Negation ..." );

      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massNeoplasmUris = new ArrayList<>( UriConstants.getMassNeoplasmUris( graphDb ) );


      final Map<Paragraph, Collection<IdentifiedAnnotation>> mentionMap
            = JCasUtil.indexCovered( jCas, Paragraph.class, IdentifiedAnnotation.class );

      for ( Collection<IdentifiedAnnotation> mentions : mentionMap.values() ) {
         if ( mentions.size() <= 1 || mentions.size() > 4 ) {
            continue;
         }
         final Map<String,Collection<IdentifiedAnnotation>> uriMentions = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( mentions, massNeoplasmUris );
         if ( uriMentions.size() != 1 ) {
            continue;
         }
         final boolean negated = uriMentions.values().stream().flatMap( Collection::stream ).anyMatch( IdentifiedAnnotationUtil::isNegated );
         if ( negated ) {
            uriMentions.values().stream().flatMap( Collection::stream ).forEach( m -> m.setPolarity( CONST.NE_POLARITY_NEGATION_PRESENT ) );
         }
      }
   }


}
