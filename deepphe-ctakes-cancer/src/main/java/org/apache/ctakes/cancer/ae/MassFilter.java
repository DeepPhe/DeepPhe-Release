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
      LOGGER.info( "Removing misidentified Masses ..." );
      // TODO should be able to handle this with pos, but for now brute force

      final String docText = jCas.getDocumentText();
      final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );
      final Collection<IdentifiedAnnotation> removals = new ArrayList<>();

      for ( IdentifiedAnnotation annotation : annotations ) {
         final int begin = annotation.getBegin();
         final int end = annotation.getEnd();
         final String text = annotation.getCoveredText().toLowerCase();
         switch ( text ) {
            case "mass": {
               if ( begin > 5 && docText.substring( begin - 5, begin - 1 ).contains( "body" ) ) {
                  removals.add( annotation );
               } else if ( end < docText.length() - 7 && docText.substring( end + 2, end + 7 ).contains( "effect" ) ) {
                  removals.add( annotation );
               }
            }
            case "local": {
               if ( end < docText.length() - 15 && docText.substring( end + 1, end + 15 ).contains( "esthesia" ) ) {
                  removals.add( annotation );
               }
            }
            case "back": {
               if ( begin > 5
                    && (docText.substring( begin - 5, begin - 1 ).contains( "came" )
                        || docText.substring( begin - 5, begin - 1 ).contains( "went" )) ) {
                  removals.add( annotation );
               }
            }
            case "duct": {
               if ( end < docText.length() - 15 && docText.substring( end + 1, end + 15 ).contains( "carcinoma" ) ) {
                  removals.add( annotation );
               }
            }
         }
      }

      LOGGER.info( "Removing unwanted anatomic sites ..." );
      Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, UriConstants.getUnwantedAnatomyUris() )
                              .values()
                              .stream()
                              .flatMap( Collection::stream )
                              .forEach( Annotation::removeFromIndexes );


      LOGGER.info( "Removing unwanted regions ..." );
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
