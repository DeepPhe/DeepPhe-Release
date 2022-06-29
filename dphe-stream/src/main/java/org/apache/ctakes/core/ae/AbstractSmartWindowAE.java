//package org.apache.ctakes.core.ae;
//
//import org.apache.ctakes.core.util.Pair;
//import org.apache.ctakes.core.util.TextSpanUtil;
//import org.apache.ctakes.core.util.paragraph.ParagraphProcessor;
//import org.apache.ctakes.core.util.prose.ProseProcessor;
//import org.apache.ctakes.core.util.section.SectionProcessor;
//import org.apache.ctakes.core.util.sentence.SentenceProcessor;
//import org.apache.ctakes.core.util.topic.TopicProcessor;
//import org.apache.ctakes.core.util.treelist.ListProcessor;
//import org.apache.ctakes.core.util.window.InWindowFinder;
//import org.apache.ctakes.typesystem.type.textspan.NormalizableAnnotation;
//import org.apache.ctakes.typesystem.type.textspan.Paragraph;
//import org.apache.ctakes.typesystem.type.textspan.Segment;
//import org.apache.ctakes.typesystem.type.textspan.Topic;
//import org.apache.log4j.Logger;
//import org.apache.uima.UimaContext;
//import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
//import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
//import org.apache.uima.fit.util.JCasUtil;
//import org.apache.uima.jcas.JCas;
//import org.apache.uima.jcas.tcas.Annotation;
//import org.apache.uima.resource.ResourceInitializationException;
//
//import java.util.*;
//
///**
// * @author SPF , chip-nlp
// * @since {12/9/2021}
// */
//abstract public class AbstractSmartWindowAE extends JCasAnnotator_ImplBase {
//
////   static public final String PARAM_LIST_FIRST = "ListFirst";
////   @ConfigurationParameter(
////         name = PARAM_LIST_FIRST,
////         description = "True if Lists should be processed before prose.",
////         defaultValue = "true",
////         mandatory = false
////   )
////   private boolean _listFirst = true;
//
//   abstract TopicProcessor getTopicProcessor();
//
//   abstract ListProcessor getListProcessor();
//
//   abstract ProseProcessor getProseProcessor();
//
//   abstract SectionProcessor getSectionProcessor();
//
//   abstract ParagraphProcessor getParagraphProcessor();
//
//   abstract SentenceProcessor getSentenceProcessor();
//
//   protected Logger get4jLogger() {
//      return Logger.getLogger( "AbstractSmartWindowAE" );
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void initialize( final UimaContext context ) throws ResourceInitializationException {
//      super.initialize( context );
//      if ( getTopicProcessor() != null ) {
//         getTopicProcessor().initialize( context );
//      }
//      if ( getSectionProcessor() != null ) {
//         getSectionProcessor().initialize( context );
//      }
//      if ( getParagraphProcessor() != null ) {
//         getParagraphProcessor().initialize( context );
//      }
//      if ( getSentenceProcessor() != null ) {
//         getSentenceProcessor().initialize( context );
//      }
//      if ( getListProcessor() != null ) {
//         getListProcessor().initialize( context );
//      }
////      if ( getProseProcessor() != null ) {
////         getProseProcessor().initialize( context );
////      }
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
////      if ( _listFirst ) {
////         getListProcessor().process( jcas );
////         getProseProcessor().process( jcas );
////      } else {
////         getProseProcessor().process( jcas );
////         getListProcessor().process( jcas );
////      }
//   }
//
//   static public <A extends Annotation> List<A> processInWindows( final JCas jCas ) {
//      final Map<Segment,Collection<Topic>> sectionTopics = JCasUtil.indexCovered( jCas, Segment.class, Topic.class );
//
//
//      final Collection<Topic> topics = JCasUtil.select( jCas, Topic.class );
//      if ( topics != null && !topics.isEmpty() ) {
//         if ( logger != null && !processName.isEmpty() ) {
//            logger.info( processName + " in Topics ..." );
//         }
//         for ( Topic topic : topics ) {
//            int topicOffset = topic.getBegin();
//            inWindowFinder.addFound( jCas, topicOffset, topic.getCoveredText(), foundItems );
//            final NormalizableAnnotation subject = topic.getSubject();
//            if ( subject != null ) {
//               usedTopicSpans.add( new Pair<>( Math.min( subject.getBegin(), topicOffset ),
//                                               Math.max( subject.getEnd(), topic.getEnd() ) ) );
//            } else {
//               usedTopicSpans.add( new Pair<>( topicOffset, topic.getEnd() ) );
//            }
//         }
//         usedTopicSpans.sort( Comparator.comparingInt( Pair::getValue1 ) );
//      }
//
//
//
//
//
//      final List<A> foundItems = new ArrayList<>();
//      final List<Pair<Integer>> usedTopicSpans = new ArrayList<>();
//      final Collection<Topic> topics = JCasUtil.select( jCas, Topic.class );
//      if ( topics != null && !topics.isEmpty() ) {
//         if ( logger != null && !processName.isEmpty() ) {
//            logger.info( processName + " in Topics ..." );
//         }
//         for ( Topic topic : topics ) {
//            int topicOffset = topic.getBegin();
//            inWindowFinder.addFound( jCas, topicOffset, topic.getCoveredText(), foundItems );
//            final NormalizableAnnotation subject = topic.getSubject();
//            if ( subject != null ) {
//               usedTopicSpans.add( new Pair<>( Math.min( subject.getBegin(), topicOffset ),
//                                               Math.max( subject.getEnd(), topic.getEnd() ) ) );
//            } else {
//               usedTopicSpans.add( new Pair<>( topicOffset, topic.getEnd() ) );
//            }
//         }
//         usedTopicSpans.sort( Comparator.comparingInt( Pair::getValue1 ) );
//      }
//      final Collection<Paragraph> paragraphs = JCasUtil.select( jCas, Paragraph.class );
//      if ( paragraphs != null && !paragraphs.isEmpty() ) {
//         if ( logger != null && !processName.isEmpty() ) {
//            logger.info( processName + " in Paragraphs ..." );
//         }
//         for ( Paragraph paragraph : paragraphs ) {
//            if ( TextSpanUtil.isAnnotationCovered( usedTopicSpans, paragraph ) ) {
//               continue;
//            }
//            inWindowFinder.addFound( jCas, paragraph.getBegin(), paragraph.getCoveredText(), foundItems );
//         }
//      } else {
//         if ( logger != null && !processName.isEmpty() ) {
//            logger.info( processName + " in Sections ..." );
//         }
//         for ( Segment section : JCasUtil.select( jCas, Segment.class ) ) {
//            final Collection<Pair<Integer>> availableSpans = TextSpanUtil.getAvailableSpans( section, usedTopicSpans );
//            for ( Pair<Integer> span : availableSpans ) {
//               final String spanText = jCas.getDocumentText()
//                                           .substring( span.getValue1(), span.getValue2() );
//               inWindowFinder.addFound( jCas, span.getValue1(), spanText, foundItems );
//            }
//         }
//      }
//      return foundItems;
//   }
//
//
//
//
////   static public <A extends Annotation> List<A> findInWindows( final JCas jCas,
////                                                               final Logger logger,
////                                                               final String processName,
////                                                               final InWindowFinder<A> inWindowFinder ) {
////
////      final List<A> foundItems = new ArrayList<>();
////      final List<Pair<Integer>> usedTopicSpans = new ArrayList<>();
////      final Collection<Topic> topics = JCasUtil.select( jCas, Topic.class );
////      if ( topics != null && !topics.isEmpty() ) {
////         if ( logger != null && !processName.isEmpty() ) {
////            logger.info( processName + " in Topics ..." );
////         }
////         for ( Topic topic : topics ) {
////            int topicOffset = topic.getBegin();
////            inWindowFinder.addFound( jCas, topicOffset, topic.getCoveredText(), foundItems );
////            final NormalizableAnnotation subject = topic.getSubject();
////            if ( subject != null ) {
////               usedTopicSpans.add( new Pair<>( Math.min( subject.getBegin(), topicOffset ),
////                                               Math.max( subject.getEnd(), topic.getEnd() ) ) );
////            } else {
////               usedTopicSpans.add( new Pair<>( topicOffset, topic.getEnd() ) );
////            }
////         }
////         usedTopicSpans.sort( Comparator.comparingInt( Pair::getValue1 ) );
////      }
////      final Collection<Paragraph> paragraphs = JCasUtil.select( jCas, Paragraph.class );
////      if ( paragraphs != null && !paragraphs.isEmpty() ) {
////         if ( logger != null && !processName.isEmpty() ) {
////            logger.info( processName + " in Paragraphs ..." );
////         }
////         for ( Paragraph paragraph : paragraphs ) {
////            if ( TextSpanUtil.isAnnotationCovered( usedTopicSpans, paragraph ) ) {
////               continue;
////            }
////            inWindowFinder.addFound( jCas, paragraph.getBegin(), paragraph.getCoveredText(), foundItems );
////         }
////      } else {
////         if ( logger != null && !processName.isEmpty() ) {
////            logger.info( processName + " in Sections ..." );
////         }
////         for ( Segment section : JCasUtil.select( jCas, Segment.class ) ) {
////            final Collection<Pair<Integer>> availableSpans = TextSpanUtil.getAvailableSpans( section, usedTopicSpans );
////            for ( Pair<Integer> span : availableSpans ) {
////               final String spanText = jCas.getDocumentText()
////                                           .substring( span.getValue1(), span.getValue2() );
////               inWindowFinder.addFound( jCas, span.getValue1(), spanText, foundItems );
////            }
////         }
////      }
////      return foundItems;
////   }
//
//
//
//}
