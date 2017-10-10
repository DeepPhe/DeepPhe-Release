package org.apache.ctakes.core.ontology;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.dictionary.lookup2.concept.OwlConcept;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Utility used to obtain annotations in a cas using owl uris
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/24/2015
 */
final public class OwlOntologyConceptUtil {

   static private final Logger LOGGER = Logger.getLogger( "OwlOntologyConceptUtil" );

   private OwlOntologyConceptUtil() {
   }


   static private final BinaryOperator<Collection<IdentifiedAnnotation>> mergeSets
         = ( set1, set2 ) -> {
      set1.addAll( set2 );
      return set1;
   };


   /**
    * Convenience method that calls {@link OntologyConceptUtil#getCodes} with {@link OwlConcept#URI_CODING_SCHEME} as the scheme
    *
    * @param annotation -
    * @return all owl URIs that exist for the given annotation
    */
   static public Collection<String> getUris( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getCodes( annotation, OwlConcept.URI_CODING_SCHEME );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param <T>          type for lookup window
    * @return all owl URIs that exist for the given window
    */
   static public <T extends Annotation> Collection<String> getUris( final JCas jcas, final T lookupWindow ) {
      return OntologyConceptUtil.getCodes( jcas, lookupWindow, OwlConcept.URI_CODING_SCHEME );
   }

   /**
    * Convenience method that calls {@link OntologyConceptUtil#getCodes} with {@link OwlConcept#URI_CODING_SCHEME} as the scheme
    *
    * @param jcas -
    * @return all owl URIs that exist for the given annotation
    */
   static public Collection<String> getUris( final JCas jcas ) {
      return OntologyConceptUtil.getCodes( jcas, OwlConcept.URI_CODING_SCHEME );
   }


   /**
    * @param owlUri -
    * @return Owl Classes for the given URI
    */
   static public IClass getIClass( final String owlUri ) {
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getDefaultOntology();
         return ontology.getClass( owlUri );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         LOGGER.error( multE.getMessage(), multE );
      }
      return null;
   }

   /**
    * @param owlUris -
    * @return Owl Classes for the given URIs
    */
   static private Collection<IClass> getIClasses( final Collection<String> owlUris ) {
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getDefaultOntology();
         return owlUris.stream()
               .map( ontology::getClass )
               .collect( Collectors.toSet() );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         LOGGER.error( multE.getMessage(), multE );
      }
      return Collections.emptyList();
   }

   /**
    * @param annotation -
    * @return all iClasses for the given annotation
    */
   static public Collection<IClass> getIClasses( final IdentifiedAnnotation annotation ) {
      return getIClasses( getUris( annotation ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param <T>          type for lookup window
    * @return all iClasses within the lookup window
    */
   static public <T extends Annotation> Collection<IClass> getIClasses( final JCas jcas, final T lookupWindow ) {
      return getIClasses( getUris( jcas, lookupWindow ) );
   }

   /**
    * @param jcas -
    * @return all iClasses in the jcas
    */
   static public Collection<IClass> getIClasses( final JCas jcas ) {
      return getIClasses( getUris( jcas ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param uri          uri of interest
    * @param <T>          type for lookup window
    * @return all IdentifiedAnnotations within the given window that have the given uri
    */
   static public <T extends Annotation> Collection<IdentifiedAnnotation> getAnnotationsByUri( final JCas jcas,
                                                                                              final T lookupWindow,
                                                                                              final String uri ) {
      return OntologyConceptUtil.getAnnotationsByCode( jcas, lookupWindow, uri );
   }

   /**
    * Convenience method that calls {@link OntologyConceptUtil#getAnnotationsByCode}
    *
    * @param jcas -
    * @param uri  uri of interest
    * @return all IdentifiedAnnotations that have the given uri
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByUri( final JCas jcas,
                                                                       final String uri ) {
      return OntologyConceptUtil.getAnnotationsByCode( jcas, uri );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param rootUri      uri of interest
    * @param <T>          type for lookup window
    * @return all IdentifiedAnnotations within the given window for the given uri and its children
    */
   static public <T extends Annotation> Collection<IdentifiedAnnotation> getAnnotationsByUriBranch( final JCas jcas,
                                                                                                    final T lookupWindow,
                                                                                                    final String rootUri ) {
      return getUriBranchStream( rootUri )
            .map( uri -> getAnnotationsByUri( jcas, lookupWindow, uri ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   /**
    * @param jcas    -
    * @param rootUri uri of interest
    * @return all IdentifiedAnnotations for the given uri and its children
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByUriBranch( final JCas jcas,
                                                                             final String rootUri ) {
      return getAnnotationStreamByUriBranch( jcas, rootUri ).collect( Collectors.toSet() );
   }

   /**
    * @param jcas    -
    * @param rootUri uri of interest
    * @return all IdentifiedAnnotations for the given uri and its children
    */
   static public Stream<IdentifiedAnnotation> getAnnotationStreamByUriBranch( final JCas jcas,
                                                                              final String rootUri ) {
      return getUriBranchStream( rootUri )
            .map( uri -> getAnnotationsByUri( jcas, uri ) )
            .flatMap( Collection::stream );
   }

   /**
    * @param jcas    -
    * @param rootUri uri of interest
    * @return map of uris and IdentifiedAnnotations for the given uri and its children
    */
   static public Map<String, Collection<IdentifiedAnnotation>> getUriAnnotationsByUriBranch( final JCas jcas,
                                                                                             final String rootUri ) {
      final Map<String, Collection<IdentifiedAnnotation>> uriAnnotations = new HashMap<>();
      for ( String uri : getUriBranchStream( rootUri ).collect( Collectors.toList() ) ) {
         final Collection<IdentifiedAnnotation> annotations = getAnnotationsByUri( jcas, uri );
         if ( !annotations.isEmpty() ) {
            uriAnnotations.put( uri, annotations );
         }
      }
      return uriAnnotations;
//      return getUriBranchStream( rootUri )
//            .collect( Collectors
//                  .toMap( Function.identity(), uri -> getAnnotationsByUri( jcas, uri ), mergeSets ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param rootUri      uri of interest
    * @param <T>          type for lookup window
    * @return map of uris and IdentifiedAnnotations for the given uri and its children
    */
   static public <T extends Annotation> Map<String, Collection<IdentifiedAnnotation>> getUriAnnotationsByUriBranch( final JCas jcas,
                                                                                                                    final T lookupWindow,
                                                                                                                    final String rootUri ) {
      final Map<String, Collection<IdentifiedAnnotation>> uriAnnotations = new HashMap<>();
      for ( String uri : getUriBranchStream( rootUri ).collect( Collectors.toList() ) ) {
         final Collection<IdentifiedAnnotation> annotations = getAnnotationsByUri( jcas, lookupWindow, uri );
         if ( !annotations.isEmpty() ) {
            uriAnnotations.put( uri, annotations );
         }
      }
      return uriAnnotations;
//      return getUriBranchStream( rootUri )
//            .collect( Collectors
//                  .toMap( Function.identity(), uri -> getAnnotationsByUri( jcas, lookupWindow, uri ), mergeSets ) );
   }

   /**
    * @param rootUri uri for the root of a branch
    * @return all of the iClasses in the branch, starting with the branch root
    */
   static public Collection<IClass> getUriBranchClasses( final String rootUri ) {
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getDefaultOntology();
         final IClass branchRoot = ontology.getClass( rootUri );
         if ( branchRoot == null ) {
            LOGGER.error( "No Class exists for URI " + rootUri );
            return Collections.emptyList();
         }
         final IClass[] branches = branchRoot.getSubClasses();
         final IClass[] branch = new IClass[ branches.length + 1 ];
         System.arraycopy( branches, 0, branch, 1, branches.length );
         branch[ 0 ] = branchRoot;
         return Arrays.asList( branch );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         LOGGER.error( multE.getMessage(), multE );
      }
      return Collections.emptyList();
   }

   /**
    * @param rootUri uri for the root of a branch
    * @return a stream with all of the uris in the branch, starting with the branch root
    */
   static public Stream<String> getUriBranchStream( final String rootUri ) {
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getDefaultOntology();
         final IClass branchRoot = ontology.getClass( rootUri );
         if ( branchRoot == null ) {
            LOGGER.error( "No Class exists for URI " + rootUri );
            return Stream.empty();
         }
         final IClass[] branches = branchRoot.getSubClasses();
         final IClass[] branch = new IClass[ branches.length + 1 ];
         System.arraycopy( branches, 0, branch, 1, branches.length );
         branch[ 0 ] = branchRoot;
         return Arrays.stream( branch ).map( OwlParserUtil::getUriString );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         LOGGER.error( multE.getMessage(), multE );
      }
      return Stream.empty();
   }

   /**
    * @param baseUri uri for the base of a branch
    * @return a stream with all of the root uris above the base, starting with the base
    */
   static public Stream<String> getUriRootsStream( final String baseUri ) {
      try {
         final IOntology ontology = OwlConnectionFactory.getInstance().getDefaultOntology();
         final IClass rootBase = ontology.getClass( baseUri );
         if ( rootBase == null ) {
            LOGGER.error( "No Class exists for URI " + baseUri );
            return Stream.empty();
         }
         final IClass[] roots = rootBase.getSuperClasses();
         final IClass[] root = new IClass[ roots.length + 1 ];
         System.arraycopy( roots, 0, root, 1, roots.length );
         root[ 0 ] = rootBase;
         return Arrays.stream( root ).map( OwlParserUtil::getUriString );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         LOGGER.error( multE.getMessage(), multE );
      }
      return Stream.empty();
   }


}
