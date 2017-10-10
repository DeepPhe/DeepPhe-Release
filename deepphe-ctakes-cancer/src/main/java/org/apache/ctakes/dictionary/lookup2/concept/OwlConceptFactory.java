package org.apache.ctakes.dictionary.lookup2.concept;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import org.apache.ctakes.core.util.DotLogger;
import org.apache.ctakes.dictionary.lookup2.bsv.BsvParserUtil;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.ValidTextUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2015
 */
final public class OwlConceptFactory implements ConceptFactory {

   static private final Logger LOGGER = Logger.getLogger( "OwlConceptFactory" );

   static private final String OWL_FILE_PATH = "owlPath";
   static private final String OWL_ROOT_URIS = "owlRootURIs";

   private final ConceptFactory _delegateFactory;

   public OwlConceptFactory( final String name, final UimaContext uimaContext, final Properties properties ) {
      this( name, properties.getProperty( OWL_FILE_PATH ),
            properties.getProperty( OWL_ROOT_URIS ) == null
                  ? new String[ 0 ]
                  : properties.getProperty( OWL_ROOT_URIS ).split( "," ) );
   }

   public OwlConceptFactory( final String name, final String owlFilePath, final String... rootUris ) {
      try {
         OwlConnectionFactory.getInstance().getOntology( owlFilePath );
      } catch ( IOntologyException | FileNotFoundException ontE ) {
         LOGGER.error( ontE.getMessage() );
      }
      // Get the bsv terms first just in case there are crazy overlaps
      final Map<Long, Concept> conceptMap = BsvParserUtil.parseConceptFiles( owlFilePath );
      conceptMap.putAll( parseOwlFile( owlFilePath, rootUris ) );
      _delegateFactory = new MemConceptFactory( name, conceptMap );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateFactory.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      return _delegateFactory.createConcept( cuiCode );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes ) {
      return _delegateFactory.createConcepts( cuiCodes );
   }


   static private Map<Long, Concept> createOwlConcepts( final IClass iClass ) {
      final String uri = OwlParserUtil.getUriString( iClass );
      if ( OwlParserUtil.getInstance().isUnwantedUri( uri ) ) {
         return Collections.emptyMap();
      }
      final edu.pitt.dbmi.nlp.noble.terminology.Concept concept = iClass.getConcept();
      final String[] synonyms = concept.getSynonyms();
      if ( synonyms == null ) {
         return Collections.emptyMap();
      }
      if ( !Arrays.stream( concept.getSynonyms() )
            .map( String::toLowerCase )
            .filter( ValidTextUtil::isValidText )
            .findFirst().isPresent() ) {
         return Collections.emptyMap();
      }
      final String cui = OwlParserUtil.getCui( concept );
      final Long cuiCode = CuiCodeUtil.getInstance().getCuiCode( cui );
      final String tui = OwlParserUtil.getTui( iClass );
      final String preferredText = OwlParserUtil.getPreferredText( iClass );
      final Concept ontologyConcept = new OwlConcept( cui, tui, uri, preferredText );
      return Collections.singletonMap( cuiCode, ontologyConcept );
   }

   /**
    * Create a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects
    * by parsing a owl file.
    *
    * @param owlFilePath path to file containing ontology owl
    * @return collection of all valid terms read from the bsv file
    */
   static private Map<Long, Concept> parseOwlFile( final String owlFilePath, final String... rootUris ) {
      if ( rootUris.length == 0 ) {
         LOGGER.warn( "No root URIs provided, no owl concepts created" );
         return Collections.emptyMap();
      }
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getOntology( owlFilePath );
         LOGGER.info( "Creating Concepts from Ontology Owl:" );
         try ( DotLogger dotter = new DotLogger() ) {
            final Map<Long, Concept> conceptMap = new HashMap<>();
            for ( String rootUri : rootUris ) {
               final IClass root = ontology.getClass( rootUri.trim() );
               if ( root == null ) {
                  LOGGER.error( "No class exists for root " + rootUri );
                  continue;
               }
               for ( IClass childClass : root.getSubClasses() ) {
                  conceptMap.putAll( createOwlConcepts( childClass ) );
               }
            }
            return conceptMap;
         } catch ( IOException ioE ) {
            LOGGER.error( "Could not create concepts from Ontology Owl" );
         }
      } catch ( IOntologyException | FileNotFoundException ontE ) {
         LOGGER.error( ontE.getMessage() );
      }
      return Collections.emptyMap();
   }


}
