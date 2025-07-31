package org.healthnlp.deepphe.nlp.ae.annotation;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
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
      name = "SubjectAdjuster",
      description = "Adjusts Subject for Annotations.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class SubjectAdjuster extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SubjectAdjuster" );

   static private final String[] FAMILY = { " mother", " father", " sister", " brother", " cousin", " aunt", " uncle",
                                            " grandfather", " grandmother",
                                            "mother ", "father ", "sister ", "brother ", "cousin ", "aunt ", "uncle ",
                                            "grandfather ", "grandmother ",
                                            "(mother)", "(father)", "(sister)", "(brother)", "(cousin)", "(aunt)",
                                            "(uncle)",
                                            "(grandfather)", "(grandmother)" };

   // TODO merge with setSegmentId for speed
   // TODO add section check - family history, patient history for subject and/or historic

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjusting Subjects ..." );

      final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations
            = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );

      for ( Map.Entry<Sentence, Collection<IdentifiedAnnotation>> entry : sentenceAnnotations.entrySet() ) {
         final String text = entry.getKey().getCoveredText().toLowerCase();
         if ( Arrays.stream( FAMILY ).anyMatch( text::contains ) ) {
            entry.getValue().forEach( a -> a.setSubject( CONST.ATTR_SUBJECT_FAMILY_MEMBER ) );
         } else {
            entry.getValue().stream()
                 .filter( a -> a.getSubject() == null || a.getSubject().isEmpty() )
                 .forEach( a -> a.setSubject( CONST.ATTR_SUBJECT_PATIENT ) );
         }
      }
   }


}
