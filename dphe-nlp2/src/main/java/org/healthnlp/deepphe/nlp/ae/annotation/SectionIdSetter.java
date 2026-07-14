package org.healthnlp.deepphe.nlp.ae.annotation;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.section.DefinedSectionType;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Sets segmentID and sentenceId attribute of all IdentifiedAnnotation that are in a section other than SIMPLE_SEGMENT
 * Makes it possible to determine if a singleton mention is in a final diagnosis section
 * where singletons often are more important than in other sections
 *
 * @author JJM , chip-nlp
 * @version %I%
 * @since 9/11/2018
 */
final public class SectionIdSetter extends JCasAnnotator_ImplBase {


    private static final String CLASS_NAME = MethodHandles.lookup().lookupClass().getSimpleName();

    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    @Override
    public void process( JCas jCas ) throws AnalysisEngineProcessException {
       LOGGER.info("Setting segmentID and sentenceId attributes ....");

       final Map<Segment, Collection<IdentifiedAnnotation>> sectionAnnotationMap
             = JCasUtil.indexCovered( jCas, Segment.class, IdentifiedAnnotation.class );
       int i = 1;
       for ( Map.Entry<Segment, Collection<IdentifiedAnnotation>> sectionAnnotations : sectionAnnotationMap.entrySet() ) {
          final Segment section = sectionAnnotations.getKey();
          final String sectionId = DefinedSectionType.getStandardizedSectionName( section ) + "_" + i;
          section.setId( sectionId );
          sectionAnnotations.getValue().forEach( a -> a.setSegmentID( sectionId ) );
          i++;
       }

       i = 1;
       final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceMap
             = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );
       for ( Map.Entry<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations : sentenceMap.entrySet() ) {
          final Sentence sentence = sentenceAnnotations.getKey();
          sentence.setSentenceNumber( i );
          final String sentenceId = "" + i;
          sentenceAnnotations.getValue().forEach( a -> a.setSentenceID( sentenceId ) );
          i++;
       }
    }

   static public void setContainerIds( final JCas jCas, final Collection<IdentifiedAnnotation> annotations ) {
      final Map<Segment, Collection<IdentifiedAnnotation>> sectionAnnotationMap
            = JCasUtil.indexCovered( jCas, Segment.class, IdentifiedAnnotation.class );
      for ( Map.Entry<Segment, Collection<IdentifiedAnnotation>> sectionAnnotations : sectionAnnotationMap.entrySet() ) {
         final Segment section = sectionAnnotations.getKey();
         final String sectionId = section.getId();
         sectionAnnotations.getValue().stream()
                           .filter( annotations::contains )
                           .forEach( a -> a.setSegmentID( sectionId ) );
      }

      final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceMap
            = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );
      for ( Map.Entry<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations : sentenceMap.entrySet() ) {
         final Sentence sentence = sentenceAnnotations.getKey();
         final int sentenceNumber = sentence.getSentenceNumber();
         final String sentenceId = "" + sentenceNumber;
         sentenceAnnotations.getValue().stream()
                            .filter( annotations::contains )
                            .forEach( a -> a.setSentenceID( sentenceId ) );
      }
    }

}

