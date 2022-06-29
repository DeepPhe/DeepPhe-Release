package org.apache.ctakes.core.util.topic;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.core.util.paragraph.ParagraphProcessor;
import org.apache.ctakes.core.util.sentence.SentenceProcessor;
import org.apache.ctakes.core.util.treelist.ListProcessor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
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
public interface TopicProcessor {

   ParagraphProcessor getParagraphProcessor();
   ListProcessor getListProcessor();
   SentenceProcessor getSentenceProcessor();

   default void initializeForTopics( UimaContext context )
         throws ResourceInitializationException {}

   /**
    * Process all Topics within a document Section.
    * @param jCas ye olde ...
    * @param noteDivisions Container for divisions of a note.
    * @return Collection of text spans that have been covered / processed.
    * @throws AnalysisEngineProcessException -
    */
   default Collection<Pair<Integer>> processTopics( JCas jCas,
                                                    final Segment section,
                                                    NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      final Collection<Topic> topics = noteDivisions.getSectionTopics( jCas ).get( section );
      if ( topics == null || topics.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Pair<Integer>> processedSpans = new ArrayList<>();
      final Collection<Pair<Integer>> availableSpans = noteDivisions.getAvailableSpans( jCas );
      for ( Topic topic : topics ) {
         if ( TextSpanUtil.isAnnotationCovered( availableSpans, topic ) ) {
            // Only process a topic that has been processed by some previous processor.
            processedSpans.addAll( processTopic( jCas, section, topic, noteDivisions ) );
         }
      }
      return processedSpans;
   }

   /**
    * Process a single Topic.
    * @param jCas ye olde ...
    * @param section covering Section.
    * @param topic -
    * @param noteDivisions Container for divisions of a note.
    * @return Collection of text spans that have been covered / processed.
    * @throws AnalysisEngineProcessException -
    */
   default Collection<Pair<Integer>> processTopic( JCas jCas,
                                           Segment section,
                                           Topic topic,
                                           NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      final Collection<Pair<Integer>> allProcessedSpans = new HashSet<>();
      final ParagraphProcessor paragraphProcessor = getParagraphProcessor();
      if ( paragraphProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = paragraphProcessor.processParagraphs( jCas, section, topic, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      final ListProcessor listProcessor = getListProcessor();
      if ( listProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = listProcessor.processLists( jCas, section, topic, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      final SentenceProcessor sentenceProcessor = getSentenceProcessor();
      if ( sentenceProcessor != null ) {
         final Collection<Pair<Integer>> processedSpans
               = sentenceProcessor.processSentences( jCas, section, topic, noteDivisions );
         allProcessedSpans.addAll( processedSpans );
         noteDivisions.addProcessedSpans( processedSpans );
      }
      return allProcessedSpans;
   }



   /**
    *
    * @param topic -
    * @return true if the subject is not null.
    */
   default boolean isSubjectKnown( Topic topic) {
      return isSubjectKnown( topic.getSubject() );
   }

   /**
    *
    * @param topicSubject Subject of the Topic, e.g. "Skin Biopsy 1: Left arm." or null.
    * @return true if the subject is not null.
    */
   default boolean isSubjectKnown( IdentifiedAnnotation topicSubject ) {
      return topicSubject != null;
   }

   /**
    * A topic span may or may not include the span of the topic's subject.
    * By default, a topic span will contain the span of a non-null subject.
    * @param topic -
    * @return The full text coverage span of a topic.
    */
   default Pair<Integer> getTopicSpan( final Topic topic ) {
      final IdentifiedAnnotation subject = topic.getSubject();
      if ( subject != null ) {
         return new Pair<>( Math.min( subject.getBegin(), topic.getBegin() ),
                            Math.max( subject.getEnd(), topic.getEnd() ) );
      }
      return new Pair<>( topic.getBegin(), topic.getEnd() );
   }

}
