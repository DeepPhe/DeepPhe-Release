package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.core.util.regex.TimeoutMatcher;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Topic;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @since {10/14/2021}
 */
@PipeBitInfo(
      name = "Regex Topic Finder (A)",
      description = "Annotates Document Topic topics by detecting Topic Headers using Regular Expressions.",
      products = { PipeBitInfo.TypeProduct.SECTION }
)
abstract public class RegexTopicFinder extends JCasAnnotator_ImplBase {

   // TODO Move to ctakes.
   // TODO Extract some kind of loader, parser, document divider from which this and sectionizer can inherit
   // TODO Extract some kind of regex loader and handler for document dividers?

   static private final Logger LOGGER = Logger.getLogger( "RegexTopicFinder" );

   static public final String PARAM_TAG_DIVIDERS = "TagDividers";
   @ConfigurationParameter(
         name = PARAM_TAG_DIVIDERS,
         description = "True if lines of divider characters ____ , ---- , === should divide topics",
         defaultValue = "true",
         mandatory = false
   )
   private boolean _tagDividers = true;

   /**
    * classic ctakes default topic id
    */
//   static private final String DEFAULT_TOPIC_ID = "SIMPLE_TOPIC";
   static private final String NO_TOPIC = "NO_TOPIC";
   static private final String TOPIC_NAME_ID = "TOPIC_NAME";
   static public final String DIVIDER_LINE_NAME = "DIVIDER_LINE";
//   static private final Pattern DIVIDER_LINE_PATTERN = Pattern.compile( "^[\\t ]*[_\\-=]{4,}[\\t ]*$" );
   static private final Pattern DIVIDER_LINE_PATTERN
         = Pattern.compile( "^[\\t ]*[_=-]{10,}[\\t ]*$", Pattern.MULTILINE );

   private enum TagType {
      HEADER, FOOTER, DIVIDER
   }

   private enum RegexGroupType {
      Index, Subject;
   }


   /**
    * Holder for topic type as defined in the user's specification bsv file
    */
   static protected final class TopicType {
      static private final TopicType
            DEFAULT_TYPE = new TopicType( NO_TOPIC, null );
      private final String __name;
      private final Pattern __fullPattern;

      public TopicType( final String name, final Pattern fullPattern ) {
         __name = name;
         __fullPattern = fullPattern;
      }
   }

   /**
    * Holder for information about a topic tag discovered in text
    */
   static final class TopicTag {
      private final String __typeName;
      private final String __index;
      private final String __subject;

      private TopicTag( final String typeName, final String index, final String subject ) {
         __typeName = typeName;
         __index = index;
         __subject = subject;
      }
   }

//   static protected final TopicTag LINE_DIVIDER_TAG
//         = new TopicTag( DIVIDER_LINE_NAME, DIVIDER_LINE_NAME, TagType.DIVIDER );

//   /**
//    * Normally I would put this in a singleton but I'm not sure that a singleton will work well with/as uima ae
//    *
//    * @param topicId id of a topic / topic
//    * @return false iff a topic by the given id is known and was assigned the "don't parse" flag
//    */
//   static public boolean shouldParseTopic( final String topicId ) {
//      final TopicType topicType = _topicTypes.getOrDefault( topicId, TopicType.DEFAULT_TYPE );
//      return topicType.__shouldParse;
//   }


   static private final Object TOPIC_TYPE_LOCK = new Object();
//   static private final Map<String, TopicType> _topicTypes = new HashMap<>();
   static private final List<TopicType> _topicTypes = new ArrayList<>();
   static private volatile boolean _topicsLoaded = false;

   static protected void addTopicType( final TopicType topicType ) {
      _topicTypes.add( topicType );
   }

   static public List<TopicType> getTopicTypes() {
      return Collections.unmodifiableList( _topicTypes );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      synchronized ( TOPIC_TYPE_LOCK ) {
         if ( !_topicsLoaded ) {
            loadTopics();
            LOGGER.info( "Use Tag Dividers " + _tagDividers );
            if ( _tagDividers ) {
               LOGGER.info( "Using Dividers " + DIVIDER_LINE_PATTERN.pattern() );
               addTopicType( new TopicType( TagType.DIVIDER.name(), DIVIDER_LINE_PATTERN ) );
            }
            _topicsLoaded = true;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Annotating Topics ..." );
      if ( _topicTypes.isEmpty() ) {
         LOGGER.info( "Finished processing, no topic types defined" );
         return;
      }
      final Collection<Segment> sections = JCasUtil.select( jcas, Segment.class );
      for ( Segment section : sections ) {
         LOGGER.info( "Checking for topics in " + section.getPreferredText() );
         final String sectionText = jcas.getDocumentText().substring( section.getBegin(), section.getEnd() );
         final Map<Pair<Integer>, TopicTag> topicTags = findTopicTags( sectionText );
         if ( topicTags.isEmpty() ) {
            LOGGER.info( "No topic tags found in " + section.getPreferredText() );
            continue;
         }
         createTopics( jcas, section.getBegin(), section.getEnd(), topicTags );
      }
   }

//   private List<Topic> findTopics( final JCas jCas, final int windowOffset, final String windowText ) {
//      if ( windowText.length() < 10 ) {
//         return Collections.emptyList();
//      }
//      int count = 0;
//      int lineLength = 0;
//      for ( char c : windowText.toCharArray() ) {
//         lineLength++;
//         if ( c == '\n' ) {
//            if ( lineLength > 4 ) {
//               count++;
//               if ( count > 2 ) {
//                  break;
//               }
//            }
//            lineLength = 0;
//         }
//      }
//      if ( count < 3 ) {
//         return Collections.emptyList();
//      }
//      final List<Topic> topics = new ArrayList<>();
//      for ( TopicType topicType : _topicTypes ) {
//         topics.addAll( findTopics( jCas, windowOffset, windowText, topicType ) );
//      }
//      return topics;
//   }

//   static private List<TopicTag> findTopics( final JCas jCas,
//                                                final int windowOffset,
//                                                final String windowText,
//                                                final TopicType topicType ) {
//      LOGGER.info( "Finding Topic Tags for " + topicType.__name );
//      final List<TopicTag> topicTags = new ArrayList<>();
//      final Matcher matcher = topicType.__fullPattern.matcher( windowText );
//      while ( matcher.find() ) {
//         LOGGER.info( "Matched " + matcher.start() + "," + matcher.end() );
//         final String index = RegexUtil.getGroupText( matcher, RegexGroupType.Index.name() );
//         final String subject = RegexUtil.getGroupText( matcher, RegexGroupType.Subject.name() );
//         final TopicTag topicTag = new TopicTag( topicType.__name, index, subject );
//         LOGGER.info( "TOPIC: " + topicTag.__typeName + " : " + topicTag.__index + " " + topicTag.__subject );
//      }
//      return topicTags;
//   }


   /**
    * Load Topics in a manner appropriate for the Regex Topicizer
    *
    * @throws ResourceInitializationException -
    */
   abstract protected void loadTopics() throws ResourceInitializationException;



   /**
    * find all topic separator header tags
    *
    * @param windowText -
    * @return topic tags mapped to index pairs
    */
   static private Map<Pair<Integer>, TopicTag> findTopicTags( final String windowText ) {
      if ( windowText.length() < 10 ) {
         return Collections.emptyMap();
      }
      int count = 0;
      int lineLength = 0;
      for ( char c : windowText.toCharArray() ) {
         lineLength++;
         if ( c == '\n' ) {
            if ( lineLength > 4 ) {
               count++;
               if ( count > 2 ) {
                  break;
               }
            }
            lineLength = 0;
         }
      }
      if ( count < 3 ) {
         return Collections.emptyMap();
      }
      final Map<Pair<Integer>, TopicTag> topicTagSpans = new HashMap<>();
      for ( TopicType topicType : _topicTypes ) {
         if ( topicType.__fullPattern == null ) {
            continue;
         }
         final Map<Pair<Integer>,TopicTag> typeTagSpans
               = findTopicTags( windowText, topicType.__name, topicType.__fullPattern );
         for ( Map.Entry<Pair<Integer>,TopicTag> typeTagSpan : typeTagSpans.entrySet() ) {
            // Only add tags for spans that do not already exist in the map.
            topicTagSpans.putIfAbsent( typeTagSpan.getKey(), typeTagSpan.getValue() );
         }
      }
      return topicTagSpans;
   }
//
//   /**
//    * find all topic separator footer tags
//    *
//    * @param docText -
//    * @return topic tags mapped to index pairs
//    */
//   static private Map<Pair<Integer>, TopicTag> findFooterTags( final String docText ) {
//      final Map<Pair<Integer>, TopicTag> footerTags = new HashMap<>();
//      for ( TopicType topicType : _topicTypes ) {
//         if ( topicType.__footerPattern == null ) {
//            continue;
//         }
//         footerTags
//               .putAll( findTopicTags( docText, topicType.__name, topicType.__footerPattern, TagType.FOOTER ) );
//      }
//      return footerTags;
//   }

   /**
    * @param docText    -
    * @param typeName   topic type name
    * @param tagPattern regex pattern for topic type
    * @return topic tags mapped to index pairs
    */
   static Map<Pair<Integer>, TopicTag> findTopicTags( final String docText,
                                                        final String typeName,
                                                        final Pattern tagPattern ) {
//      LOGGER.info( "Finding Topic Tags for " + typeName );
      final Map<Pair<Integer>, TopicTag> topicTagSpans = new HashMap<>();
      try ( TimeoutMatcher finder = new TimeoutMatcher( tagPattern, docText ) ) {
         Matcher matcher = finder.nextMatch();
         while ( matcher != null ) {
            // the start tag of this tag is the start of the current match
            // the end tag of this tag is the end of the current match, exclusive
            final Pair<Integer> tagBounds = new Pair<>( matcher.start(), matcher.end() );
            final String index = RegexUtil.getGroupText( matcher, RegexGroupType.Index.name() );
            final String subject = RegexUtil.getGroupText( matcher, RegexGroupType.Subject.name() );
            final TopicTag topicTag = new TopicTag( typeName, index, subject );
            LOGGER.info( "Created Topic Tag: " + topicTag.__typeName + " , Index: " + topicTag.__index
                         + " , Subject: " + topicTag.__subject );
            topicTagSpans.put( tagBounds, topicTag );
            matcher = finder.nextMatch();
         }
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.error( iaE.getMessage() );
      }
      return topicTagSpans;
   }

   /**
    * All tags are treated equally as topic bounds, whether header or footer
    *
    * @param jcas       -
    * @param sectionBegin the begin offset for the section containing the topics
    * @param sectionEnd the end offset for the section containing the topics
    * @param topicTags topic names are assigned based upon preceding headers
    */
   static private void createTopics( final JCas jcas,
                                       final int sectionBegin,
                                       final int sectionEnd,
                                       final Map<Pair<Integer>, TopicTag> topicTags ) {
//      LOGGER.info( "Topic Tags: " + topicTags.values().stream()
//                                             .map( t -> t.__typeName ).collect( Collectors.joining( "," ) ) );
      if ( topicTags.isEmpty() ) {
         return;
      }
      final String sectionText = jcas.getDocumentText().substring( sectionBegin, sectionEnd );
      final List<Pair<Integer>> boundsList = TextSpanUtil.subsumeSpans( topicTags.keySet() );

      Pair<Integer> leftBounds = boundsList.get( 0 );
      int topicEnd;
      final int length = boundsList.size();
      // add topics 1 -> n
      for ( int i = 0; i < length; i++ ) {
         leftBounds = boundsList.get( i );
         int topicBegin = leftBounds.getValue2();
         if ( i + 1 < length ) {
            topicEnd = boundsList.get( i + 1 ).getValue1();
         } else {
            // the last topic
            topicEnd = sectionText.length();
         }
         final TopicTag leftTag = topicTags.get( leftBounds );
         final IdentifiedAnnotation subject = createSubject( jcas,
                                                               sectionBegin,
                                                               leftBounds );
         subject.addToIndexes();
         final Topic topic = new Topic( jcas, sectionBegin + topicBegin, sectionBegin + topicEnd );
         // this tag is for a header, so the following topic has defined information
         topic.setTopicType( leftTag.__typeName );
         topic.setIndex( leftTag.__index );
         topic.setSubject( subject );
         topic.addToIndexes();
         LOGGER.info( "TOPIC " + topic.getTopicType() + " : " + subject.getCoveredText() + " \n" + topic.getCoveredText() );
      }
   }

   static private IdentifiedAnnotation createSubject( final JCas jCas,
                                                      final int spanOffset,
                                                      final Pair<Integer> span ) {
      final IdentifiedAnnotation subject = new IdentifiedAnnotation( jCas,
                                                                        spanOffset + span.getValue1(),
                                                                        spanOffset + span.getValue2() );
      subject.addToIndexes();
      return subject;
   }


}
