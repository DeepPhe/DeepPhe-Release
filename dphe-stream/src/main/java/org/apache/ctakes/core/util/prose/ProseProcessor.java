package org.apache.ctakes.core.util.prose;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Topic;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {12/9/2021}
 */
public interface ProseProcessor {

   // TODO - may need? processProse( jcas, section, topic, annotation )  processProse( jcas, section, annotation )

   void initializeForProse( UimaContext context ) throws ResourceInitializationException;


   default Collection<Pair<Integer>> processProse( JCas jCas,
                                                   Annotation proseAnnotation ) throws AnalysisEngineProcessException {
      return processProse( jCas, proseAnnotation.getCoveredText() );
   }

   default Collection<Pair<Integer>> processProse( JCas jCas,
                                                   Pair<Integer> proseSpan ) throws AnalysisEngineProcessException {
      return processProse( jCas, jCas.getDocumentText()
                              .substring( proseSpan.getValue1(),
                                          proseSpan.getValue2() ) );
   }

   default Collection<Pair<Integer>> processProse( JCas jCas,
                                                   String text ) throws AnalysisEngineProcessException {
      return processProse( jCas, "", "", null, text );
   }

   default Collection<Pair<Integer>> processProse( JCas jCas,
                              Segment section,
                              String text )
         throws AnalysisEngineProcessException {
      return processProse( jCas,
                    section.getPreferredText(),
                    "",
                    null,
                    text );
   }

   default Collection<Pair<Integer>> processProse( JCas jCas,
                             Segment section,
                             Topic topic,
                             String text )
         throws AnalysisEngineProcessException {
      return processProse( jCas,
                   section.getPreferredText(),
                   topic.getTopicType(),
                   topic.getSubject(),
                   text );
   }

   Collection<Pair<Integer>> processProse( JCas jCas,
                      String sectionName,
                      String topicType,
                      IdentifiedAnnotation topicSubject,
                      String text )
         throws AnalysisEngineProcessException;

}
