package org.apache.ctakes.core.ae;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/5/2016
 */
public class OwlRegexSectionizer extends RegexSectionizer {

   static private final Logger LOGGER = Logger.getLogger( "OwlRegexSectionizer" );

   static public final String OWL_FILE_PATH = "sectionsOwl";
   static public final String OWL_FILE_DESC = "path to a file containing a list of regular expressions and corresponding section types.";

   static public final String SECTION_ROOT_URI = "sectionRootURIs";
   static private final String OWL_ROOT_URI_DESC = "comma-separated list of section regex root uri.";


   @ConfigurationParameter(
         name = OWL_FILE_PATH,
         description = OWL_FILE_DESC
   )
   private String _owlFilePath;

   @ConfigurationParameter(
         name = SECTION_ROOT_URI,
         description = OWL_ROOT_URI_DESC
   )
   private String _owlRootUris;


   /**
    * {@inheritDoc}
    */
   @Override
   protected void loadSections() throws ResourceInitializationException {
      LOGGER.info( "Parsing " + _owlFilePath );
      final String[] rootUris = _owlRootUris.split( "," );
      if ( rootUris.length == 0 ) {
         LOGGER.warn( "No root URIs provided, no sections created" );
         return;
      }
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getOntology( _owlFilePath );
         for ( String rootUri : rootUris ) {
            final IClass root = ontology.getClass( rootUri.trim() );
            if ( root == null ) {
               LOGGER.error( "No class exists for root " + rootUri );
               continue;
            }
            for ( IClass childClass : root.getSubClasses() ) {
               createRegexSections( childClass );
            }
         }
      } catch ( IOntologyException | FileNotFoundException ontE ) {
         LOGGER.error( ontE.getMessage() );
      }
      LOGGER.info( "Finished Parsing" );
   }

   /**
    * @param iClass -
    */
   private void createRegexSections( final IClass iClass ) {
      final String sectionName = iClass.getName();
      final Concept concept = iClass.getConcept();
      final String[] regexes = concept.getSynonyms();
      if ( regexes == null || regexes.length == 0 ) {
         LOGGER.warn( "No Regex for " + sectionName );
         return;
      }
      final String fullRegex = createFullRegex( regexes );
      final RegexSectionizer.SectionType sectionType = new RegexSectionizer.SectionType( sectionName, fullRegex, null, true );
      addSectionType( sectionType );
   }

   protected String createFullRegex( final String[] regexes ) {
      return Arrays.stream( regexes )
            .filter( r -> r.length() > 1 )
            // TODO Should have .tolowercase here as the regex is case insensitive
            .distinct()
            .collect( Collectors.joining( "|" ) );
   }


   static public AnalysisEngineDescription createEngineDescription( final String owlFilePath, final String owlRootUris )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( OwlRegexSectionizer.class,
            OWL_FILE_PATH, owlFilePath, SECTION_ROOT_URI, owlRootUris );
   }

}
