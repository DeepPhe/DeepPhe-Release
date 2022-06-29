package org.apache.ctakes.core.util.paragraph;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.core.util.sentence.SentenceProcessor;
import org.apache.ctakes.core.util.treelist.ListProcessor;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Topic;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;


/**
 * @author SPF , chip-nlp
 * @since {12/9/2021}
 */
public interface ParagraphProcessor {

   ListProcessor getListProcessor();
   SentenceProcessor getSentenceProcessor();

   default void initializeForParagraphs( UimaContext context ) throws ResourceInitializationException {}


   default Collection<Pair<Integer>> processParagraphs( JCas jCas,
                                                        Segment section,
                                                        NoteDivisions noteDivisions ) throws AnalysisEngineProcessException {
      return processParagraphs( jCas,
                                section,
                                null,
                                noteDivisions.getSectionParagraphs( jCas ).get( section ),
                                noteDivisions );
   }

   default Collection<Pair<Integer>> processParagraphs( JCas jCas,
                                  Segment section,
                                  Topic topic,
                                  NoteDivisions noteDivisions ) throws AnalysisEngineProcessException {
      return processParagraphs( jCas,
                                section,
                                topic,
                                noteDivisions.getTopicParagraphs( jCas ).get( topic ),
                         noteDivisions );
   }

   default Collection<Pair<Integer>> processParagraphs( JCas jCas,
                                                        Segment section,
                                                        Topic topic,
                                                        Collection<Paragraph> paragraphs,
                                                        NoteDivisions noteDivisions ) throws AnalysisEngineProcessException {
      if ( paragraphs == null || paragraphs.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Pair<Integer>> processedSpans = new ArrayList<>();
      final Collection<Pair<Integer>> availableSpans = noteDivisions.getAvailableSpans( jCas );
      for ( Paragraph paragraph : paragraphs ) {
         if ( TextSpanUtil.isAnnotationCovered( availableSpans, paragraph ) ) {
            // Only process a paragraph that has been processed by some previous processor.
            processedSpans.addAll( processParagraph( jCas, section, topic, paragraph, noteDivisions ) );
         }
      }
      return processedSpans;
   }

   default Collection<Pair<Integer>> processParagraph( JCas jCas,
                                                       Segment section,
                                                       Paragraph paragraph,
                                                       NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      return processParagraph( jCas,
                               section,
                               null,
                               paragraph,
                               noteDivisions );
   }

   default Collection<Pair<Integer>> processParagraph( JCas jCas,
                              Segment section,
                              Topic topic,
                              Paragraph paragraph,
                              NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      final Collection<Pair<Integer>> allProcessedSpans = new HashSet<>();
      final ListProcessor listProcessor = getListProcessor();
      if ( listProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = listProcessor.processLists( jCas, section, topic, paragraph, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      final SentenceProcessor sentenceProcessor = getSentenceProcessor();
      if ( sentenceProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = sentenceProcessor.processSentences( jCas, section, topic, paragraph, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      return allProcessedSpans;
   }


}
