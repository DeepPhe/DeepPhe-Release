package org.apache.ctakes.cancer.owl;

import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/28/2016
 */
final public class OwlUriUtil {

   static private final Logger LOGGER = Logger.getLogger( "OwlUriUtil" );

   private OwlUriUtil() {
   }


   /**
    * @param annotations -
    * @return a Collection of the most specific uris for all the given annotations
    */
   static public String getSpecificUri( final IdentifiedAnnotation... annotations ) {
      return getSpecificUri( Arrays.asList( annotations ) );
   }

   /**
    * @param annotations -
    * @return a Collection of the most specific uris for all the given annotations
    */
   static public String getSpecificUri( final Collection<IdentifiedAnnotation> annotations ) {
      return getLeafUris( annotations ).stream().findFirst().orElse( "" );
   }

   /**
    * Gets a stem uri.  For example, when given "left breast", "upper right breast"
    * it will return the common parent "breast".
    *
    * @param annotations -
    * @return a uri that covers all the leaves for all the given annotations
    */
   static public String getStemUri( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return null;
      }
      long leastLength = Integer.MAX_VALUE;
      String bestUri = "";
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Collection<String> uris = OwlOntologyConceptUtil.getUris( annotation );
         for ( String uri : uris ) {
            long length = OwlOntologyConceptUtil.getUriRootsStream( uri ).count();
            if ( length < leastLength ) {
               leastLength = length;
               bestUri = uri;
            }
         }
      }
      return bestUri;
   }

   /**
    * Gets the most descriptive leaf uris.
    * For example, when given "breast", "left breast", "right breast", "upper right breast"
    * it will return the most descriptive "left breast" and "right upper breast"
    *
    * @param annotation -
    * @return the most specific uris representing the annotations.
    */
   static public Collection<String> getLeafUris( final IdentifiedAnnotation annotation ) {
      return getLeafUris( Collections.singletonList( annotation ) );
   }


   /**
    * Gets the most descriptive leaf uris.
    * For example, when given "breast", "left breast", "right breast", "upper right breast"
    * it will return the most descriptive "left breast" and "right upper breast"
    *
    * @param annotations -
    * @return the most specific uris representing the annotations.
    */
   static public Collection<String> getLeafUris( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return Collections.emptyList();
      }
      // create map of all leaf uris to a collection of their roots
      final Map<String, Collection<String>> uriSetMap = annotations.stream()
            .map( OwlOntologyConceptUtil::getUris )
            .flatMap( Collection::stream )
            .collect( Collectors.toMap( Function.identity(),
                  u -> OwlOntologyConceptUtil.getUriRootsStream( u )
                        .collect( Collectors.toSet() ) ) );
      // collect all non leaf uris from each root
      final Collection<String> nonLeafUris = new HashSet<>();
      for ( Map.Entry<String, Collection<String>> entry : uriSetMap.entrySet() ) {
         final String leaf = entry.getKey();
         entry.getValue().stream()
               .filter( u -> !leaf.equals( u ) )
               .forEach( nonLeafUris::add );
      }
      // remove all non-(other)-leaf root uris from each root
      uriSetMap.values().forEach( set -> set.removeAll( nonLeafUris ) );
      // if the root is not empty then the associated leaf is not in any other root
      return uriSetMap.entrySet().stream()
            .filter( e -> !e.getValue().isEmpty() )
            .map( Map.Entry::getKey )
            .collect( Collectors.toSet() );
   }


   static public Collection<String> getUriLeafUris( final Collection<String> uris ) {
      // create map of all leaf uris to a collection of their roots
      final Map<String, Collection<String>> uriSetMap = uris.stream()
            .collect( Collectors.toMap( Function.identity(),
                  u -> OwlOntologyConceptUtil.getUriRootsStream( u )
                        .collect( Collectors.toSet() ) ) );
      // collect all non leaf uris from each root
      final Collection<String> nonLeafUris = new HashSet<>();
      for ( Map.Entry<String, Collection<String>> entry : uriSetMap.entrySet() ) {
         final String leaf = entry.getKey();
         entry.getValue().stream()
               .filter( u -> !leaf.equals( u ) )
               .forEach( nonLeafUris::add );
      }
      // remove all non-(other)-leaf root uris from each root
      uriSetMap.values().forEach( set -> set.removeAll( nonLeafUris ) );
      // if the root is not empty then the associated leaf is not in any other root
      return uriSetMap.entrySet().stream()
            .filter( e -> !e.getValue().isEmpty() )
            .map( Map.Entry::getKey )
            .collect( Collectors.toSet() );
   }


}
