package org.healthnlp.deepphe.nlp.uri;


import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/7/2019
 */
final public class UriAnnotationUtil {

   static private final Logger LOGGER = Logger.getLogger( "UriAnnotationUtil" );

   private UriAnnotationUtil() {}


   static public Map<String,List<IdentifiedAnnotation>> mapUriAnnotations(
         final Collection<IdentifiedAnnotation> annotations ) {
      final Map<String,List<IdentifiedAnnotation>> uriAnnotations = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final String uri = Neo4jOntologyConceptUtil.getUri( annotation );
         if ( uri != null && !uri.isEmpty() ) {
            uriAnnotations.computeIfAbsent( uri, a -> new ArrayList<>() ).add( annotation );
         }
      }
      return uriAnnotations;
   }


   /**
    * Gets a stem uri.  For example, when given "left breast", "upper right breast"
    * it will return the common parent "breast".
    *
    * @param annotations -
    * @return a uri that covers all the leaves for all the given annotations
    */
   public static String getMostSpecificUri( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return null;
      }
      final Collection<String> allUris = annotations.stream()
                                                    .map( Neo4jOntologyConceptUtil::getUris )
                                                    .flatMap( Collection::stream )
                                                    .collect( Collectors.toSet() );
      final String bestUri = UriUtil.getMostSpecificUri( allUris );
      if ( bestUri != null ) {
         return bestUri;
      }
      return UriUtil.getShortestRootUri( allUris );
   }

   static public Map<String,List<IdentifiedAnnotation>> getAssociatedUriAnnotations(
         final Collection<IdentifiedAnnotation> annotations ) {
      return getAssociatedUriAnnotations( mapUriAnnotations( annotations ) );
   }

   static public Map<String,List<IdentifiedAnnotation>> getAssociatedUriAnnotations(
         final Map<String,List<IdentifiedAnnotation>> uriAnnotations ) {
      final Map<String,Collection<String>> associatedUris = UriUtil.getAssociatedUriMap( uriAnnotations.keySet() );
      final Map<String,List<IdentifiedAnnotation>> associatedAnnotations = new HashMap<>( associatedUris.size() );
      for ( Map.Entry<String,Collection<String>> associated : associatedUris.entrySet() ) {
         final List<IdentifiedAnnotation> assocAnnotations
               = associated.getValue().stream()
                           .map( uriAnnotations::get )
                           .flatMap( Collection::stream )
                           .collect( Collectors.toList() );
         associatedAnnotations.put( associated.getKey(), assocAnnotations );
      }
      return associatedAnnotations;
   }


//   static public Collection<Collection<String>> getAssociatedUris( final Collection<IdentifiedAnnotation> annotations ) {
//      return getAssociatedUris( mapUriAnnotations( annotations ) );
//   }
//
//   static public Collection<Collection<String>> getAssociatedUris( final Map<String,List<IdentifiedAnnotation>> uriAnnotations ) {
//      return UriUtil.getAssociatedUris( uriAnnotations.keySet() );
//   }
//
//   static public Map<String,Collection<String>> getAssociatedUriMap( final Collection<IdentifiedAnnotation> annotations ) {
//      return getAssociatedUriMap( mapUriAnnotations( annotations ) );
//   }

   static public Map<String,Collection<String>> getAssociatedUriMap( final Map<String, List<IdentifiedAnnotation>> uriAnnotations ) {
      return UriUtil.getAssociatedUriMap( uriAnnotations.keySet() );
   }

   static public Map<String,Collection<IdentifiedAnnotation>> getAssociatedAnnotations( final Map<String, List<IdentifiedAnnotation>> uriAnnotations ) {
      final Map<String,Collection<String>> associatedUris = UriUtil.getAssociatedUriMap( uriAnnotations.keySet() );
      final Map<String,Collection<IdentifiedAnnotation>> associatedAnnotations = new HashMap<>( associatedUris.size() );
      for ( Map.Entry<String,Collection<String>> associated : associatedUris.entrySet() ) {
         final Collection<IdentifiedAnnotation> annotations = mapAnnotations( associated.getValue(), uriAnnotations );
         associatedAnnotations.put( associated.getKey(), annotations );
      }
      return associatedAnnotations;
   }


   static public Map<String,String> getBestRoots( final Collection<IdentifiedAnnotation> annotations ) {
      return getBestRoots( mapUriAnnotations( annotations ) );
   }

   static public Map<String,String> getBestRoots( final Map<String,List<IdentifiedAnnotation>> uriAnnotations ) {
      return UriUtil.getBestRoots( uriAnnotations.keySet() );
   }

   static public Map<String,Collection<String>> getUniqueChildren( final Collection<IdentifiedAnnotation> annotations ) {
      return getUniqueChildren( mapUriAnnotations( annotations ) );
   }

   static public Map<String,Collection<String>> getUniqueChildren( final Map<String,List<IdentifiedAnnotation>> uriAnnotations ) {
      return UriUtil.getUniqueChildren( uriAnnotations.keySet() );
   }


   static private Collection<IdentifiedAnnotation> mapAnnotations( final Collection<String> uris,
                                                          final Map<String, List<IdentifiedAnnotation>> uriAnnotations ) {
      return uris.stream()
                 .map( uriAnnotations::get )
                 .flatMap( Collection::stream )
                 .collect( Collectors.toSet() );
   }

}
