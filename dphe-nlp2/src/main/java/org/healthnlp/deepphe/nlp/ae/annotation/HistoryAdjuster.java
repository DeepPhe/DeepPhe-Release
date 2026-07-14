package org.healthnlp.deepphe.nlp.ae.annotation;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/17/2018
 */
@PipeBitInfo(
      name = "HistoryAdjuster",
      description = "Adjusts History for Annotations.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class HistoryAdjuster extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "HistoryAdjuster" );

   static private final List<String> HISTORY = Arrays.asList( "Clinical History",
                                                              "Family Medical History",
                                                              "History",
                                                              "History of Present Illness",
                                                              "Interim History",
                                                              "Past Medical History",
                                                              "Past Surgical History",
                                                              "Patient History",
                                                              "Prior Malignancy",
                                                              "Prior Therapy" );


   static public boolean isHistoric( final IdentifiedAnnotation annotation ) {
      final String sectionId = annotation.getSegmentID();
      if ( sectionId == null || sectionId.isEmpty() ) {
         // false by default.
         return false;
      }
      final int scoreIndex = sectionId.lastIndexOf( '_' );
      if ( scoreIndex > 5 && scoreIndex < sectionId.length()-2
           && Character.isDigit( sectionId.charAt( scoreIndex+1 ) ) ) {
         // The sectionId may be a section type followed by an underscore and a number.
         return isHistoric( sectionId.substring( 0, scoreIndex ) );
      }
      return isHistoric( annotation.getSegmentID() );
   }

   static public boolean isHistoric( final Segment section ) {
      return isHistoric( section.getPreferredText() );
   }

   static public boolean isHistoric( final String sectionName ) {
      if ( sectionName == null || sectionName.isEmpty() ) {
         // false by default.
         return false;
      }
      return HISTORY.contains( sectionName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjusting History ..." );

      final Map<Segment, Collection<IdentifiedAnnotation>> sectionAnnotations
            = JCasUtil.indexCovered( jCas, Segment.class, IdentifiedAnnotation.class );

      for ( Map.Entry<Segment, Collection<IdentifiedAnnotation>> entry : sectionAnnotations.entrySet() ) {
         final boolean isHistoric = isHistoric( entry.getKey() );
         if ( !isHistoric ) {
            continue;
         }
         entry.getValue().forEach( a -> a.setHistoryOf( CONST.NE_HISTORY_OF_PRESENT ) );
      }
   }


}
