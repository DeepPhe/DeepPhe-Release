package org.apache.ctakes.core.util.treelist;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.typesystem.type.textspan.FormattedList;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
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
public interface ListProcessor {

   default void initializeForLists( UimaContext context ) throws ResourceInitializationException {}

   default Collection<Pair<Integer>> processLists( JCas jCas,
                                                   Segment section,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      return processLists( jCas,
                           section,
                           null,
                           null,
                           noteDivisions.getSectionLists( jCas ).get( section ),
                           noteDivisions );
   }

   default Collection<Pair<Integer>> processLists( JCas jCas,
                                                   Segment section,
                                                   Topic topic,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      return processLists( jCas,
                           section,
                           topic,
                           null,
                           noteDivisions.getTopicLists( jCas ).get( topic ),
                           noteDivisions );
   }

   default Collection<Pair<Integer>> processLists( JCas jCas,
                                                   Segment section,
                                                   Topic topic,
                                                   Paragraph paragraph,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      return processLists( jCas,
                           section,
                           topic,
                           paragraph,
                           noteDivisions.getParagraphLists( jCas ).get( paragraph ),
                           noteDivisions );
   }

   default Collection<Pair<Integer>> processLists( JCas jCas,
                                                   Segment section,
                                                   Topic topic,
                                                   Paragraph paragraph,
                                                   Collection<FormattedList> lists,
                                                   NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      if ( lists == null || lists.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Pair<Integer>> processedSpans = new ArrayList<>();
      final Collection<Pair<Integer>> availableSpans = noteDivisions.getAvailableSpans( jCas );
      for ( FormattedList list : lists ) {
         if ( TextSpanUtil.isAnnotationCovered( availableSpans, list ) ) {
            // Only process a list that has not been processed by some previous processor.
            processedSpans.addAll( processList( jCas, section, topic, paragraph, list, noteDivisions ) );
         }
      }
      return processedSpans;
   }

   /**
    *
    * @param jCas -
    * @param section -
    * @param topic -
    * @param paragraph -
    * @param list -
    * @param noteDivisions -
    * @return text span processed.  Usually the total text span of the list.
    * @throws AnalysisEngineProcessException -
    */
   Collection<Pair<Integer>> processList( JCas jCas,
                                  Segment section,
                                  Topic topic,
                                  Paragraph paragraph,
                                          FormattedList list,
                                  NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException;

}
