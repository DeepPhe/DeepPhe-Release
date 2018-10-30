package org.apache.ctakes.cancer.ae.section;


import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2016
 */
final public class SectionReAdder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SectionReAdder" );


   /**
    * Where Sentences were in certain unwanted sections, they are re-added to the cas.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Replacing temporarily removed Sections ..." );
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jcas );
      final Collection<Segment> sections = SectionHolder.getInstance().getHiddenSections( documentId );
      sections.forEach( Annotation::addToIndexes );
      SectionHolder.getInstance().clear( documentId );
      LOGGER.info( "Finished Processing" );
   }

}
