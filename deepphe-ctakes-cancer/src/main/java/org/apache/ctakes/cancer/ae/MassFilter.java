package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
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
      LOGGER.info( "Removing misidentified Lesions ..." );
      final Collection<IdentifiedAnnotation> lesions = OwlOntologyConceptUtil.getAnnotationsByUriBranch( jCas, OwlConstants.LESION_URI );
      if ( lesions != null && !lesions.isEmpty() ) {
         final String text = jCas.getDocumentText();
         final Collection<IdentifiedAnnotation> removals = new ArrayList<>();
         for ( IdentifiedAnnotation lesion : lesions ) {
            if ( lesion.getCoveredText().equalsIgnoreCase( "mass" ) ) {
               final int begin = lesion.getBegin();
               final int end = lesion.getEnd();
               if ( begin > 5 && text.substring( begin - 5, begin - 1 ).equalsIgnoreCase( "body" ) ) {
                  removals.add( lesion );
               } else if ( end < text.length() - 7 && text.substring( end + 2, end + 7 ).equalsIgnoreCase( "effect" ) ) {
                  removals.add( lesion );
               }
            }
         }
         removals.forEach( IdentifiedAnnotation::removeFromIndexes );
      }
      LOGGER.info( "Finished Processing" );
   }


}
