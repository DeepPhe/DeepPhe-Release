package org.apache.ctakes.core.util.section;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.core.util.paragraph.ParagraphProcessor;
import org.apache.ctakes.core.util.sentence.SentenceProcessor;
import org.apache.ctakes.core.util.topic.TopicProcessor;
import org.apache.ctakes.core.util.treelist.ListProcessor;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Collection;
import java.util.HashSet;


/**
 * @author SPF , chip-nlp
 * @since {12/9/2021}
 */
public interface SectionProcessor {

   TopicProcessor getTopicProcessor();
   ParagraphProcessor getParagraphProcessor();
   ListProcessor getListProcessor();
   SentenceProcessor getSentenceProcessor();

   default void initializeForSections( UimaContext context ) throws ResourceInitializationException {}


   default Collection<Pair<Integer>> processSections( JCas jCas, NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      final Collection<Pair<Integer>> processedSpans = new HashSet<>();
      for ( Segment section : noteDivisions.getSections( jCas ) ) {
         processedSpans.addAll( processSection( jCas, section, noteDivisions ) );
      }
      return processedSpans;
   }

   default Collection<Pair<Integer>> processSection( JCas jCas,
                                             Segment section,
                                             NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      final Collection<Pair<Integer>> allProcessedSpans = new HashSet<>();
      final TopicProcessor topicProcessor = getTopicProcessor();
      if ( topicProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = topicProcessor.processTopics( jCas, section, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      final ParagraphProcessor paragraphProcessor = getParagraphProcessor();
      if ( paragraphProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = paragraphProcessor.processParagraphs( jCas, section, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      final ListProcessor listProcessor = getListProcessor();
      if ( listProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = listProcessor.processLists( jCas, section, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      final SentenceProcessor sentenceProcessor = getSentenceProcessor();
      if ( sentenceProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = sentenceProcessor.processSentences( jCas, section, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      return allProcessedSpans;
   }


}
