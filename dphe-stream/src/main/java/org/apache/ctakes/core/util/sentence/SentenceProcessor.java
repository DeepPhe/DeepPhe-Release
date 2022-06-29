package org.apache.ctakes.core.util.sentence;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.typesystem.type.textspan.Topic;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {12/9/2021}
 */
public interface SentenceProcessor {

   default void initializeForSentences( UimaContext context ) throws ResourceInitializationException {}

   default Collection<Pair<Integer>> processSentences( JCas jCas,
                                                   Segment section,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      return processSentences( jCas,
                           section,
                           null,
                           null,
                           noteDivisions.getSectionSentences( jCas ).get( section ),
                           noteDivisions );
   }

   default Collection<Pair<Integer>> processSentences( JCas jCas,
                                                   Segment section,
                                                   Topic topic,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      return processSentences( jCas,
                           section,
                           topic,
                           null,
                           noteDivisions.getTopicSentences( jCas ).get( topic ),
                           noteDivisions );
   }

   default Collection<Pair<Integer>> processSentences( JCas jCas,
                                                   Segment section,
                                                   Topic topic,
                                                   Paragraph paragraph,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      return processSentences( jCas,
                           section,
                           topic,
                           paragraph,
                           noteDivisions.getParagraphSentences( jCas ).get( paragraph ),
                           noteDivisions );
   }


   default Collection<Pair<Integer>> processSentences( JCas jCas,
                                                   Segment section,
                                                   Topic topic,
                                                   Paragraph paragraph,
                                                   Collection<Sentence> sentences,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      if ( sentences == null || sentences.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Pair<Integer>> processedSpans = new ArrayList<>();
      final Collection<Pair<Integer>> availableSpans = noteDivisions.getAvailableSpans( jCas );
      for ( Sentence sentence : sentences ) {
         if ( TextSpanUtil.isAnnotationCovered( availableSpans, sentence ) ) {
            // Only process a sentence that has not been processed by some previous processor.
            processedSpans.addAll( processSentence( jCas, section, topic, paragraph, sentence, noteDivisions ) );
         }
      }
      return processedSpans;
   }


   Collection<Pair<Integer>> processSentence( JCas jCas,
                                          Segment section,
                                          Topic topic,
                                          Paragraph paragraph,
                                              Sentence sentence,
                                          NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException;


}
