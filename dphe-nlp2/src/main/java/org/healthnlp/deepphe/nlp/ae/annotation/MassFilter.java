package org.healthnlp.deepphe.nlp.ae.annotation;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;


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
      LOGGER.info( "Removing misidentified Annotations ..." );
      // TODO should be able to handle this with pos, but for now brute force

      final String docText = jCas.getDocumentText().toLowerCase();
      final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );
      final Collection<IdentifiedAnnotation> removals = new ArrayList<>();

      for ( IdentifiedAnnotation annotation : annotations ) {
         final int begin = annotation.getBegin();
         final int end = annotation.getEnd();
         final String text = annotation.getCoveredText().toLowerCase();
         switch ( text ) {
            case "ca": {
               // Calcium 125 is a biomarker.
               if ( end < docText.length() - 10 && docText.substring( end + 1, end + 9 ).contains( "125" ) ) {
                  removals.add( annotation );
               }
            }
            case "mass": {
               if ( begin > 5 && docText.substring( begin - 5, begin - 1 ).contains( "body" ) ) {
                  removals.add( annotation );
               } else if ( end < docText.length() - 7 && docText.substring( end + 2, end + 7 ).contains( "effect" ) ) {
                  removals.add( annotation );
               }
            }
            case "t1": {
               if ( begin > 20 && (docText.substring( begin - 20, begin - 1 ).contains( "axial" )
                     || docText.substring( begin - 20, begin - 1 ).contains( "postcontrast" )  ) ) {
                  removals.add( annotation );
               } else if ( end < docText.length() - 20 &&
                     (docText.substring( end + 2, end + 20 ).contains( "axial" )
                           || docText.substring( end + 2, end + 20 ).contains( "postcontrast" )
                           || docText.substring( end + 2, end + 20 ).contains( "prolongation" )
                           || docText.substring( end + 2, end + 20 ).contains( "signal" ) ) ) {
                  removals.add( annotation );
               }
            }
            case "t2": {
               if ( begin > 20 && (docText.substring( begin - 20, begin - 1 ).contains( "axial" )
                     || docText.substring( begin - 20, begin - 1 ).contains( "postcontrast" )  ) ) {
                  removals.add( annotation );
               } else if ( end < docText.length() - 20 &&
                     (docText.substring( end + 2, end + 20 ).contains( "axial" )
                           || docText.substring( end + 2, end + 20 ).contains( "postcontrast" )
                           || docText.substring( end + 2, end + 20 ).contains( "prolongation" )
                           || docText.substring( end + 2, end + 20 ).contains( "signal" ) ) ) {
                  removals.add( annotation );
               }
            }
            case "cancer": {
               if ( end < docText.length() - 20 && docText.substring( end + 1, end + 20 ).contains( "protocol" ) ) {
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
//            case "duct": {
//               if ( end < docText.length() - 15 && docText.substring( end + 1, end + 15 ).contains( "carcinoma" ) ) {
//                  removals.add( annotation );
//               }
//            }
            case "mouth": {
               if ( begin > 3 && docText.substring( begin - 3, begin - 1 ).contains( "by" ) ) {
                  removals.add( annotation );
               }
            }
         }
         if ( text.startsWith( "grade" ) ) {
            if ( begin > 10 && docText.substring( begin - 10, begin - 1 ).contains( "nuclear" ) ) {
               removals.add( annotation );
            }
         }
      }
      removals.forEach( IdentifiedAnnotation::removeFromIndexes );

//      LOGGER.info( "Removing unwanted anatomic sites ..." );
//      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
//                                                                 .getGraph();
//      Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, UriConstants.getUnwantedAnatomyUris( graphDb ) )
//                              .values()
//                              .stream()
//                              .flatMap( Collection::stream )
//                              .filter( a -> a.getTypeID() == CONST.NE_TYPE_ID_ANATOMICAL_SITE )
//                              .forEach( Annotation::removeFromIndexes );
//
//
//      LOGGER.info( "Removing unwanted regions ..." );
//      Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, Collections.singletonList( "Local_Recurrence" ) )
//                              .values()
//                              .stream()
//                              .flatMap( Collection::stream )
//                              .filter( a -> a.getCoveredText().equalsIgnoreCase( "region" )
//                                            || a.getCoveredText().equalsIgnoreCase( "regional" ) )
//                              .forEach( Annotation::removeFromIndexes );

      // Remove "histology" annotation
//      final Collection<IdentifiedAnnotation> histologies
//            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.HISTOLOGY );
//      histologies.forEach( IdentifiedAnnotation::removeFromIndexes );

//      final Collection<IdentifiedAnnotation> adh
//            = JCasUtil.select( jCas, IdentifiedAnnotation.class ).stream()
//                      .filter( a -> a.getCoveredText().equalsIgnoreCase( "ADH" ) )
//                      .collect( Collectors.toSet() );
//      adh.forEach( IdentifiedAnnotation::removeFromIndexes );

   }


}
