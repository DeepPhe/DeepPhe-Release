package org.healthnlp.deepphe.nlp.ae.section;

import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {6/23/2021}
 */
final public class WtfSectionFixer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "WtfSectionFixer" );

   @Override
   public void process( JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Checking Out of Bounds Sections ..." );
      final Collection<Segment> removals = new HashSet<>();
      final int length = jCas.getDocumentText().length();
      for ( Segment section : JCasUtil.select( jCas, Segment.class ) ) {
         final int begin = section.getBegin();
         final int end = section.getEnd();
         if ( begin < 0 || end <= 0 || end > length
              || begin >= end ) {
            LOGGER.error( "Out of Bounds Section " + section.getPreferredText()
                          + " " + begin + "," + end );
            removals.add( section );
         } else {
            try {
               section.getCoveredText();
            } catch ( StringIndexOutOfBoundsException oobE ) {
               LOGGER.error( "Exception Out of Bounds Section " + section.getPreferredText()
                             + " " + begin + "," + end );
               removals.add( section );
            }
         }
      }
      removals.forEach( Segment::removeFromIndexes );
      final Collection<Paragraph> paragraphRemovals = new HashSet<>();
      for ( Paragraph paragraph : JCasUtil.select( jCas, Paragraph.class ) ) {
         final int begin = paragraph.getBegin();
         final int end = paragraph.getEnd();
         if ( begin< 0 || begin >= length
              || end <= 0 || end > length
              || begin == end ) {
            LOGGER.error( "Out of Bounds Paragraph " + begin + "," + end );
            paragraphRemovals.add( paragraph );
         } else if ( begin > end ) {
            LOGGER.warn( "Reversed Paragraph Bounds " + begin + "," + end );
            paragraph.setBegin( end );
            paragraph.setEnd( begin );
         } else {
            try {
               paragraph.getCoveredText();
            } catch ( StringIndexOutOfBoundsException oobE ) {
               LOGGER.error( "Exception Out of Bounds Paragraph " + begin + "," + end );
               paragraphRemovals.add( paragraph );
            }
         }
      }
      paragraphRemovals.forEach( Paragraph::removeFromIndexes );
   }


}
