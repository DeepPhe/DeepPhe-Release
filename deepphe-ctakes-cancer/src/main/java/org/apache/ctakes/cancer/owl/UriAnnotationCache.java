package org.apache.ctakes.cancer.owl;

import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/17/2017
 */
public enum UriAnnotationCache {
   INSTANCE;

   static public UriAnnotationCache getInstance() {
      return INSTANCE;
   }

   private final Logger LOGGER = Logger.getLogger( "UriAnnotationCache" );

   static private final int MAX_CACHE_SIZE = 500;
   static private final int CLEANUP_SIZE = 499;
   static private final int CLEANUP_MAX = 449;

   static private final Map<String, Integer> SEMANTIC_ROOTS;
   static private final Collection<String> VALID_ROOTS;

   static {
      final Map<String, Integer> semanticRoots = new HashMap<>( 6 );
      semanticRoots.put( OwlConstants.BODY_SITE_URI, CONST.NE_TYPE_ID_ANATOMICAL_SITE );
      semanticRoots.put( OwlConstants.DISEASE_DISORDER_URI, CONST.NE_TYPE_ID_DISORDER );
      semanticRoots.put( OwlConstants.FINDING_URI, CONST.NE_TYPE_ID_FINDING );
      semanticRoots.put( OwlConstants.PROCEDURE_URI, CONST.NE_TYPE_ID_PROCEDURE );
      semanticRoots.put( OwlConstants.MEDICATION_URI, CONST.NE_TYPE_ID_DRUG );
      semanticRoots.put( OwlConstants.BODY_MODIFIER_URI, CONST.NE_TYPE_ID_ANATOMICAL_SITE );
      SEMANTIC_ROOTS = Collections.unmodifiableMap( semanticRoots );
      VALID_ROOTS = Collections.unmodifiableSet( SEMANTIC_ROOTS.keySet() );
   }

   private final Map<String, Map<String, Collection<IdentifiedAnnotation>>> _branchAnnotations = new ConcurrentHashMap<>();

   private final Map<String, Integer> _uriSemanticMap = new ConcurrentHashMap<>( MAX_CACHE_SIZE );
   private final Map<String, Integer> _semanticAccesses = new ConcurrentHashMap<>( MAX_CACHE_SIZE );

   private final Map<String, Collection<String>> _pathToRootMap = new ConcurrentHashMap<>( MAX_CACHE_SIZE );
   private final Map<String, Integer> _pathAccesses = new ConcurrentHashMap<>( MAX_CACHE_SIZE );

   private final Map<String, Map<String, Set<String>>> _uriRelationMap = new ConcurrentHashMap<>( MAX_CACHE_SIZE );
   private final Map<String, Integer> _relationAccesses = new ConcurrentHashMap<>( MAX_CACHE_SIZE );


   /**
    * @param jCas ye olde ...
    * @param uri  some uri that may or may not have related annotations in the cas
    * @return collection of annotations having the given uri
    */
   public Collection<IdentifiedAnnotation> getBranchAnnotations( final JCas jCas, final String uri ) {
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      Map<String, Collection<IdentifiedAnnotation>> map = _branchAnnotations.computeIfAbsent( documentId, m -> new HashMap<>() );
      return map.computeIfAbsent( uri, a -> OwlOntologyConceptUtil.getAnnotationsByUriBranch( jCas, uri ) );
   }

   /**
    * @param documentId id for a document that is no longer being processed
    */
   public void clearAnnotationCache( final String documentId ) {
      // Attempt a brute force clearing of elements / hashcodes for better garbage collection
      final Map<String, Collection<IdentifiedAnnotation>> map = _branchAnnotations.get( documentId );
      if ( map == null ) {
         return;
      }
      map.values().forEach( Collection::clear );
      map.clear();
      _branchAnnotations.remove( documentId );
   }

   /**
    * @param uri child uri
    * @return the ctakes semantic type id for the given uri
    */
   public Integer getUriSemanticRoot( final String uri ) {
      Integer semantic = _uriSemanticMap.get( uri );
      if ( semantic != null ) {
         _semanticAccesses.merge( uri, 1, Integer::sum );
         return semantic;
      }
      if ( _uriSemanticMap.size() >= CLEANUP_SIZE ) {
         cleanupUriMap( _uriSemanticMap, _semanticAccesses, 1 );
      }
      semantic = OwlOntologyConceptUtil.getUriRootsStream( uri )
            .filter( VALID_ROOTS::contains )
            .findFirst()
            .map( SEMANTIC_ROOTS::get )
            .orElse( CONST.NE_TYPE_ID_UNKNOWN );
      _uriSemanticMap.put( uri, semantic );
      _semanticAccesses.put( uri, 1 );
      return semantic;
   }

   /**
    * @param uri child uri
    * @return the ctakes semantic type id for the given uri
    */
   public Collection<String> getPathToRoot( final String uri ) {
      Collection<String> path = _pathToRootMap.get( uri );
      if ( path != null ) {
         _pathAccesses.merge( uri, 1, Integer::sum );
         return path;
      }
      if ( _pathToRootMap.size() >= CLEANUP_SIZE ) {
         cleanupUriMap( _pathToRootMap, _pathAccesses, 1 );
      }
      path = OwlOntologyConceptUtil.getUriRootsStream( uri ).collect( Collectors.toList() );
      _pathToRootMap.put( uri, path );
      _pathAccesses.put( uri, 1 );
      return path;
   }

   /**
    * @param uri some uri
    * @return a map of relation names to the relatable uri types for each relation
    */
   public Map<String, Set<String>> getUriRelations( final String uri ) {
      Map<String, Set<String>> uriRelations = _uriRelationMap.get( uri );
      if ( uriRelations != null ) {
         _relationAccesses.merge( uri, 1, Integer::sum );
         return uriRelations;
      }
      if ( _uriRelationMap.size() >= CLEANUP_SIZE ) {
         cleanupUriMap( _uriRelationMap, _relationAccesses, 1 );
      }
      uriRelations = OwlRelationUtil.getAllUriRelations( uri );
      _uriRelationMap.put( uri, uriRelations );
      _relationAccesses.put( uri, 1 );
      return uriRelations;
   }

   /**
    * prevents a uri map from becoming too large
    *
    * @param actualMap map of uri -to- uri property
    * @param accessMap map of uri to number of accesses
    * @param threshold the number of accesses to a uri, below which the uri should be removed from the cache
    */
   static private void cleanupUriMap( final Map<String, ?> actualMap, final Map<String, Integer> accessMap, final int threshold ) {
      final Collection<String> removals = accessMap.entrySet().stream()
            .filter( e -> e.getValue() <= threshold )
            .map( Map.Entry::getKey )
            .collect( Collectors.toSet() );
      accessMap.keySet().removeAll( removals );
      actualMap.keySet().removeAll( removals );
      if ( removals.size() > CLEANUP_MAX ) {
         cleanupUriMap( actualMap, accessMap, threshold + 1 );
      }
   }


}
