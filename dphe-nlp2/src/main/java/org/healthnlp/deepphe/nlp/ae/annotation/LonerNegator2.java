package org.healthnlp.deepphe.nlp.ae.annotation;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/21/2020
 */
@PipeBitInfo(
      name = "LonerNegator",
      description = "For dphe-stream.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class LonerNegator2 extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "LonerNegator2" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Checking Paragraph Lone Mentions for Negation ..." );
      final Map<Paragraph, Collection<IdentifiedAnnotation>> mentionMap
            = JCasUtil.indexCovered( jCas, Paragraph.class, IdentifiedAnnotation.class );
      for ( Collection<IdentifiedAnnotation> mentions : mentionMap.values() ) {
         final Collection<IdentifiedAnnotation> masses
               = mentions.stream()
                         .filter( LonerNegator2::isMassOrCancer )
                         .collect( Collectors.toList());
         if ( masses.size() <= 1 || masses.size() > 4 ) {
            continue;
         }
         final Map<String,Collection<IdentifiedAnnotation>> uriMasses = new HashMap<>();
         for ( IdentifiedAnnotation mass : masses ) {
            final String uri = Neo4jOntologyConceptUtil.getUris( mass ).stream().findFirst().orElse( "" );
            if ( !uri.isEmpty() ) {
               uriMasses.computeIfAbsent( uri, u -> new ArrayList<>() ).add( mass );
            }
         }
         if ( uriMasses.size() != 1 ) {
            continue;
         }
         final boolean negated = uriMasses.values().stream()
                                            .flatMap( Collection::stream )
                                            .anyMatch( IdentifiedAnnotationUtil::isNegated );
         if ( negated ) {
            uriMasses.values().stream()
                       .flatMap( Collection::stream )
                       .forEach( m -> m.setPolarity( CONST.NE_POLARITY_NEGATION_PRESENT ) );
         }
      }
   }

   static private boolean isMassOrCancer( final IdentifiedAnnotation annotation ) {
      final DpheGroup group = DpheGroup.getBestAnnotationGroup( annotation );
      return group == DpheGroup.MASS || group == DpheGroup.CANCER;
   }


}
