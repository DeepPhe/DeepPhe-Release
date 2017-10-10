package org.apache.ctakes.dictionary.lookup2.ontology;

import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.DotLogger;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2015
 */
public enum OwlConnectionFactory {
   INSTANCE;

   public static OwlConnectionFactory getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "OwlConnectionFactory" );


   private final Map<String, String> ONTOLOGY_PATHS = Collections.synchronizedMap( new HashMap<>() );
   private final Map<String, IOntology> ONTOLOGIES = Collections.synchronizedMap( new HashMap<>() );
   private String _defaultOntologyPath;

   synchronized public Collection<String> listOntologyPaths() {
      return Collections.unmodifiableSet( ONTOLOGIES.keySet() );
   }

   synchronized public IOntology getOntology( final String owlPath ) throws IOntologyException, FileNotFoundException {
      // FileLocator can throw declared exception fnfE - no need for try catch (descriptive FileLocator fnfE message)
      String fullOwlPath = ONTOLOGY_PATHS.get( owlPath );
      if ( fullOwlPath == null ) {
         final File file = FileLocator.getFile( owlPath );
         fullOwlPath = file.getPath();
         ONTOLOGY_PATHS.put( owlPath, fullOwlPath );
      }
      IOntology ontology = ONTOLOGIES.get( fullOwlPath );
      if ( ontology != null ) {
         return ontology;
      }
      LOGGER.info( "Loading Ontology at " + fullOwlPath + ":" );
      try ( DotLogger dotter = new DotLogger() ) {
         ontology = OOntology.loadOntology( fullOwlPath );
      } catch ( IOntologyException | IOException multE ) {
         multE.printStackTrace();
         LOGGER.error( "Could not load Ontology at " + fullOwlPath );
         throw new IOntologyException( multE.getMessage(), multE );
      }
      LOGGER.info( "Ontology loaded" );
      ONTOLOGIES.put( fullOwlPath, ontology );
      if ( ONTOLOGIES.size() == 1 ) {
         _defaultOntologyPath = fullOwlPath;
         OwlParserUtil.getInstance().updateUnwantedUris();
      }
      return ontology;
   }

   synchronized public IOntology getDefaultOntology() throws IOntologyException, FileNotFoundException {
      if ( _defaultOntologyPath == null ) {
         throw new IOntologyException( "No Default Ontology" );
      }
      return getOntology( _defaultOntologyPath );
   }

}
