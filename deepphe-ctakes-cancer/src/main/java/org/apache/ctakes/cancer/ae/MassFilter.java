package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;


/**
 * Removes Lesion annotations that are actually text "mass" within "body mass" or "mass effect".
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/11/2017
 */
final public class MassFilter extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "MassFilter" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final Collection<IdentifiedAnnotation> masses
            = JCasUtil.select( jCas, IdentifiedAnnotation.class ).stream()
            .filter( a -> a.getCoveredText().equalsIgnoreCase( "mass" ) )
            .collect( Collectors.toList() );
      if ( masses != null && !masses.isEmpty() ) {
         final String text = jCas.getDocumentText();
         final Collection<IdentifiedAnnotation> removals = new ArrayList<>();
         for ( IdentifiedAnnotation mass : masses ) {
            final int begin = mass.getBegin();
            final int end = mass.getEnd();
            if ( begin > 5 && text.substring( begin - 5, begin - 1 ).equalsIgnoreCase( "body" ) ) {
               removals.add( mass );
            } else if ( end < text.length() - 7 && text.substring( end + 2, end + 7 ).equalsIgnoreCase( "effect" ) ) {
               removals.add( mass );
            }
         }
         removals.forEach( IdentifiedAnnotation::removeFromIndexes );
      }
      // Should be fixed in ontology

      Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, UriConstants.getUnwantedAnatomyUris() )
                              .values()
                              .stream()
                              .flatMap( Collection::stream )
                              .forEach( Annotation::removeFromIndexes );

      Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, Collections.singletonList( "Hepatic_Tissue" ) )
                              .values()
                              .stream()
                              .flatMap( Collection::stream )
                              .filter( a -> a.getCoveredText().equalsIgnoreCase( "liver" ) )
                              .forEach( Annotation::removeFromIndexes );

      Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, Collections.singletonList( "Local_Recurrence" ) )
                              .values()
                              .stream()
                              .flatMap( Collection::stream )
                              .filter( a -> a.getCoveredText().equalsIgnoreCase( "region" )
                                            || a.getCoveredText().equalsIgnoreCase( "regional" ) )
                              .forEach( Annotation::removeFromIndexes );

      // Remove "histology" annotation
      final Collection<IdentifiedAnnotation> histologies
            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.HISTOLOGY );
      histologies.forEach( IdentifiedAnnotation::removeFromIndexes );

      final Collection<IdentifiedAnnotation> adh
            = JCasUtil.select( jCas, IdentifiedAnnotation.class ).stream()
                      .filter( a -> a.getCoveredText().equalsIgnoreCase( "ADH" ) )
                      .collect( Collectors.toList() );
      adh.forEach( IdentifiedAnnotation::removeFromIndexes );

      LOGGER.info( "Finished Processing" );
   }


}
