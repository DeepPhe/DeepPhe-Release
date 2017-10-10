package org.apache.ctakes.cancer.ae.section;


import org.apache.ctakes.core.ae.OwlRegexSectionizer;
import org.apache.ctakes.core.ae.RegexSectionizer;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/5/2016
 */
final public class PittSectionizer extends OwlRegexSectionizer {

   static private final Logger LOGGER = Logger.getLogger( "PittSectionizer" );

   static public final String PITTSBURGH_HEADER = "Pittsburgh Header";
   static private final String HEADER_START_REGEX = "^(?=Report ID\\.{5,})";
   static private final String HEADER_END_REGEX = "^\\[Report de\\-identified [^\\]]+\\]$";


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Sectionizing " + DocumentIDAnnotationUtil.getDocumentID( jcas ) );
      super.process( jcas );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected void loadSections() throws ResourceInitializationException {
      super.loadSections();
      final RegexSectionizer.SectionType pittHeader
            = new RegexSectionizer.SectionType( PITTSBURGH_HEADER, HEADER_START_REGEX, HEADER_END_REGEX, false );
      addSectionType( pittHeader );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String createFullRegex( final String[] regexes ) {
      final String joined = Arrays.stream( regexes )
            .map( String::trim )
            .filter( r -> r.length() > 2 )
            .map( String::toLowerCase )
            .distinct()
            .map( r -> r.replace( "-", "\\-" ) )
            .collect( Collectors.joining( "|" ) );
      return "(?:^\\s*\\r?\\n\\s*(?:" + joined + ")[^A-Za-z0-9])|(?:^(?:" + joined + "):?\\r?\\n)";
   }

   static public AnalysisEngineDescription createAnnotatorDescription( final String owlFilePath, final String owlRootUris )
         throws ResourceInitializationException {
      return createEngineDescription( owlFilePath, owlRootUris );
   }

   static public AnalysisEngineDescription createEngineDescription( final String owlFilePath, final String owlRootUris )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( PittSectionizer.class,
            OWL_FILE_PATH, owlFilePath, SECTION_ROOT_URI, owlRootUris );
   }


}
