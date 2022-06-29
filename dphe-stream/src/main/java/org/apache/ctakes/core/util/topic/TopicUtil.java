package org.apache.ctakes.core.util.topic;

import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.typesystem.type.textspan.Topic;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {11/19/2021}
 */
final public class TopicUtil {


   static public boolean isValidTopic( final Topic topic ) {
      return topic != null && TextSpanUtil.isValidSpan( topic );
   }


   /**
    *
    * @param jCas -
    * @param annotation -
    * @return covering topics, smallest to largest.
    */
   static public List<Topic> getTopics( final JCas jCas, final Annotation annotation ) {
      final List<Topic> topics = JCasUtil.selectCovering( jCas, Topic.class, annotation );
      if ( topics == null || topics.isEmpty() ) {
         return Collections.emptyList();
      }
      topics.sort( new TextSpanUtil.AnnotationByShortSorter().reversed() );
      return topics;
   }


   static public Topic getLargestTopic( final JCas jCas, final Annotation annotation ) {
      final List<Topic> topics = JCasUtil.selectCovering( jCas, Topic.class, annotation );
      if ( topics == null || topics.isEmpty() ) {
         return new Topic( jCas, -1, -1 );
      }
      topics.sort( new TextSpanUtil.AnnotationByShortSorter().reversed() );
      return topics.get( 0 );
   }


   static public Topic getSmallestTopic( final JCas jCas, final Annotation annotation ) {
      final List<Topic> topics = JCasUtil.selectCovering( jCas, Topic.class, annotation );
      if ( topics == null || topics.isEmpty() ) {
         return new Topic( jCas, -1, -1 );
      }
      topics.sort( new TextSpanUtil.AnnotationByShortSorter() );
      return topics.get( 0 );
   }


}
