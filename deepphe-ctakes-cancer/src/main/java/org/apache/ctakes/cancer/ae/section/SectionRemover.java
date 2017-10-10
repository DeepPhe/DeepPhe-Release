package org.apache.ctakes.cancer.ae.section;


import org.apache.ctakes.cancer.util.SectionHolder;
import org.apache.ctakes.core.ae.RegexSectionizer;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2016
 */
final public class SectionRemover extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SectionRemover" );


   /**
    * Where Sentences are in certain unwanted sections, they are removed.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Temporarily removing unwanted Sections ..." );
      final Collection<Segment> sections = JCasUtil.select( jcas, Segment.class );
      if ( sections == null || sections.isEmpty() ) {
         LOGGER.info( "Finished Processing" );
         return;
      }
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jcas );
      final Collection<Annotation> removalAnnotations = new HashSet<>();
      for ( Segment section : sections ) {
         final String title = section.getPreferredText();
         if ( title.equals( "Pittsburgh Header" ) || title.equals( "HistologySummary_Section" ) ) {
            SectionHolder.getInstance().addHiddenSection( documentId, section );
            removalAnnotations.add( section );
         } else if ( title.equals( RegexSectionizer.DIVIDER_LINE_NAME ) ) {
            removalAnnotations.add( section );
         }
      }
      removalAnnotations.forEach( Annotation::removeFromIndexes );
      LOGGER.info( "Finished Processing" );
   }


}
