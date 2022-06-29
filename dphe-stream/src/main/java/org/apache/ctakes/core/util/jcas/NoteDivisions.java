package org.apache.ctakes.core.util.jcas;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.typesystem.type.textspan.*;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repeated fetching of note divisions from the cas is a major performance hit.
 * This container class can facilitate division reuse between within-ae processes.
 * Note that any collection changes to divisions in the cas will not be reflected here.
 * Divisions are loaded from the cas in a lazy manner.
 * @author SPF , chip-nlp
 * @since {12/9/2021}
 */
final public class NoteDivisions {

   private Collection<Segment> _sections;
   private Collection<Topic> _topics;
   private Collection<Paragraph> _paragraphs;
   private Collection<FormattedList> _lists;
   private Collection<Sentence> _sentences;
   private Map<Segment,Collection<Topic>> _sectionTopicMap;
   private Map<Segment,Collection<Paragraph>> _sectionParagraphMap;
   private Map<Segment,Collection<FormattedList>> _sectionListMap;
   private Map<Segment,Collection<Sentence>> _sectionSentenceMap;
   private Map<Topic,Collection<Paragraph>> _topicParagraphMap;
   private Map<Topic,Collection<Sentence>> _topicSentenceMap;
   private Map<Topic,Collection<FormattedList>> _topicListMap;
   private Map<Paragraph,Collection<FormattedList>> _paragraphListMap;
   private Map<Paragraph,Collection<Sentence>> _paragraphSentenceMap;

   private Collection<Pair<Integer>> _processedSpans = new HashSet<>();
   final private Collection<Pair<Integer>> _availableSpans = new HashSet<>();

   public Collection<Segment> getSections( final JCas jCas ) {
      if ( _sections == null ) {
         _sections = JCasUtil.select( jCas, Segment.class );
      }
      return _sections;
   }

   public Collection<Topic> getTopics( final JCas jCas ) {
      if ( _topics == null ) {
         _topics = JCasUtil.select( jCas, Topic.class );
      }
      return _topics;
   }

   public Collection<Paragraph> getParagraphs( final JCas jCas ) {
      if ( _paragraphs == null ) {
         _paragraphs = JCasUtil.select( jCas, Paragraph.class );
      }
      return _paragraphs;
   }

   public Collection<FormattedList> getLists( final JCas jCas ) {
      if ( _lists == null ) {
         _lists = JCasUtil.select( jCas, FormattedList.class );
      }
      return _lists;
   }
   
   public Collection<Sentence> getSentences( final JCas jCas ) {
      if ( _sentences == null ) {
         _sentences = JCasUtil.select( jCas, Sentence.class );
      }
      return _sentences;
   }

   public Map<Segment,Collection<Topic>> getSectionTopics( final JCas jCas ) {
      if ( _sectionTopicMap == null ) {
         _sectionTopicMap = TextSpanUtil.indexCoveredOnce( getSections( jCas ), getTopics( jCas ) );
      }
      return _sectionTopicMap;
   }

   public Map<Segment,Collection<Paragraph>> getSectionParagraphs( final JCas jCas ) {
      if ( _sectionParagraphMap == null ) {
         _sectionParagraphMap = TextSpanUtil.indexCoveredOnce( getSections( jCas ), getParagraphs( jCas ) );
      }
      return _sectionParagraphMap;
   }

   public Map<Segment,Collection<FormattedList>> getSectionLists( final JCas jCas ) {
      if ( _sectionListMap == null ) {
         _sectionListMap = TextSpanUtil.indexCoveredOnce( getSections( jCas ), getLists( jCas ) );
      }
      return _sectionListMap;
   }

   public Map<Segment,Collection<Sentence>> getSectionSentences( final JCas jCas ) {
      if ( _sectionSentenceMap == null ) {
         _sectionSentenceMap = TextSpanUtil.indexCoveredOnce( getSections( jCas ), getSentences( jCas ) );
      }
      return _sectionSentenceMap;
   }

   public Map<Topic,Collection<Paragraph>> getTopicParagraphs( final JCas jCas ) {
      if ( _topicParagraphMap == null ) {
         _topicParagraphMap = TextSpanUtil.indexCoveredOnce( getTopics( jCas ), getParagraphs( jCas ) );
      }
      return _topicParagraphMap;
   }

   public Map<Topic,Collection<FormattedList>> getTopicLists( final JCas jCas ) {
      if ( _topicListMap == null ) {
         _topicListMap = TextSpanUtil.indexCoveredOnce( getTopics( jCas ), getLists( jCas ) );
      }
      return _topicListMap;
   }

   public Map<Topic,Collection<Sentence>> getTopicSentences( final JCas jCas ) {
      if ( _topicSentenceMap == null ) {
         _topicSentenceMap = TextSpanUtil.indexCoveredOnce( getTopics( jCas ), getSentences( jCas ) );
      }
      return _topicSentenceMap;
   }

   public Map<Paragraph,Collection<FormattedList>> getParagraphLists( final JCas jCas ) {
      if ( _paragraphListMap == null ) {
         _paragraphListMap = TextSpanUtil.indexCoveredOnce( getParagraphs( jCas ), getLists( jCas ) );
      }
      return _paragraphListMap;
   }

   public Map<Paragraph,Collection<Sentence>> getParagraphSentences( final JCas jCas ) {
      if ( _paragraphSentenceMap == null ) {
         _paragraphSentenceMap = TextSpanUtil.indexCoveredOnce( getParagraphs( jCas ), getSentences( jCas ) );
      }
      return _paragraphSentenceMap;
   }

   public Collection<Pair<Integer>> getProcessedSpans() {
      return _processedSpans;
   }

   public void addProcessedSpans( final Collection<Pair<Integer>> spans ) {
      if ( spans.isEmpty() ) {
         return;
      }
      Logger.getLogger( "NoteDivisions" )
            .info( "AddProcessedSpans " + spans.stream()
                                               .map( s -> s.getValue1() + "," + s.getValue2() )
                                               .collect( Collectors.joining(" ; ") ) );
      final boolean modified = _processedSpans.addAll( spans );
      if ( modified ) {
         _processedSpans = TextSpanUtil.mergeSpans( _processedSpans );
         _availableSpans.clear();
      }
   }

   public Collection<Pair<Integer>>  getAvailableSpans( final JCas jCas ) {
      if ( _availableSpans.isEmpty() ) {
         _availableSpans.addAll( TextSpanUtil.getAvailableSpans( jCas.getDocumentText().length(), _processedSpans ) );
      }
      return _availableSpans;
   }

   public void resetProcessedSpans() {
      _processedSpans = new HashSet<>();
      _availableSpans.clear();
   }

}
