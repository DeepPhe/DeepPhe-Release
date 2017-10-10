package org.apache.ctakes.dictionary.lookup2.dictionary;


import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.bsv.BsvParserUtil;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.ctakes.dictionary.lookup2.util.ValidTextUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2015
 */
final public class OwlDictionary implements RareWordDictionary {

   static private final Logger LOGGER = Logger.getLogger( "OwlDictionary" );

   static private final String OWL_FILE_PATH = "owlPath";
   static private final String OWL_ROOT_URIS = "owlRootURIs";

   private RareWordDictionary _delegateDictionary;

   public OwlDictionary( final String name, final UimaContext uimaContext, final Properties properties ) {
      this( name, properties.getProperty( OWL_FILE_PATH ),
            properties.getProperty( OWL_ROOT_URIS ) == null
                  ? new String[ 0 ]
                  : properties.getProperty( OWL_ROOT_URIS ).split( "," ) );
   }


   public OwlDictionary( final String name, final String owlFilePath, final String... rootUris ) {
      try {
         OwlConnectionFactory.getInstance().getOntology( owlFilePath );
      } catch ( IOntologyException | FileNotFoundException ontE ) {
         LOGGER.error( ontE.getMessage() );
      }
      // Get the bsv terms first just in case there are crazy overlaps
      final Collection<CuiTerm> cuiTerms = BsvParserUtil.parseCuiTermFiles( owlFilePath );
      cuiTerms.addAll( parseOwlFile( owlFilePath, rootUris ) );
      final CollectionMap<String, RareWordTerm, ? extends Collection<RareWordTerm>> rareWordTermMap
            = RareWordTermMapCreator.createRareWordTermMap( cuiTerms );
      _delegateDictionary = new MemRareWordDictionary( name, rareWordTermMap );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateDictionary.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken ) {
      return _delegateDictionary.getRareWordHits( fastLookupToken );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
      return _delegateDictionary.getRareWordHits( rareWordText );
   }


   static private Collection<CuiTerm> createCuiTerms( final IClass iClass ) {
      if ( OwlParserUtil.getInstance().isUnwantedUri( OwlParserUtil.getUriString( iClass ) ) ) {
         return Collections.emptyList();
      }
      final Concept concept = iClass.getConcept();
      final String[] synonyms = concept.getSynonyms();
      if ( synonyms == null ) {
         return Collections.emptyList();
      }
      final String cui = OwlParserUtil.getCui( concept );
      return Arrays.stream( synonyms )
            .map( String::toLowerCase )
            .filter( ValidTextUtil::isValidText )
            .distinct()
            .map( synonym -> new CuiTerm( cui, synonym ) )
            .collect( Collectors.toSet() );
   }


   /**
    * Create a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects
    * by parsing a owl file.
    *
    * @param owlFilePath path to file containing ontology owl
    * @return collection of all valid terms read from the owl file
    */
   static private Collection<CuiTerm> parseOwlFile( final String owlFilePath, final String... rootUris ) {
      if ( rootUris.length == 0 ) {
         LOGGER.warn( "No root URIs provided, no owl terms created" );
         return Collections.emptyList();
      }
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getOntology( owlFilePath );
         final Collection<CuiTerm> cuiTerms = new ArrayList<>();
         for ( String rootUri : rootUris ) {
            final IClass root = ontology.getClass( rootUri.trim() );
            if ( root == null ) {
               LOGGER.error( "No class exists for root " + rootUri );
               continue;
            }
            for ( IClass childClass : root.getSubClasses() ) {
               cuiTerms.addAll( createCuiTerms( childClass ) );
            }
         }
         return cuiTerms;
      } catch ( IOntologyException | FileNotFoundException ontE ) {
         LOGGER.error( ontE.getMessage() );
      }
      return Collections.emptyList();
   }


}
