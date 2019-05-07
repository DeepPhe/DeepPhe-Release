package org.apache.ctakes.neo4j;


import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.semantic.SemanticGroup;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.Neo4jTraverserFactory;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2017
 */
final public class Neo4jOntologyConceptUtil {

   static private final Logger LOGGER = Logger.getLogger( "Neo4jOntologyConceptUtil" );

   private Neo4jOntologyConceptUtil() {}

   static private final BinaryOperator<Collection<IdentifiedAnnotation>> mergeSets
         = ( set1, set2 ) -> {
      set1.addAll( set2 );
      return set1;
   };


   /**
    *
    * @param annotation -
    * @return all dphe URIs that exist for the given annotation
    */
   static public Collection<String> getUris( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getCodes( annotation, DPHE_CODING_SCHEME );
   }

   /**
    * @param annotation -
    * @return most specific dphe URI that exists for the given annotation
    */
   static public String getUri( final IdentifiedAnnotation annotation ) {
      return UriUtil.getMostSpecificUri( getUris( annotation ) );
   }


   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param <T>          type for lookup window
    * @return all dphe URIs that exist for the given window
    */
   static public <T extends Annotation> Collection<String> getUris( final JCas jcas, final T lookupWindow ) {
      return OntologyConceptUtil.getCodes( jcas, lookupWindow, DPHE_CODING_SCHEME );
   }

   /**
    *
    * @param jcas -
    * @return all dphe URIs that exist for the given annotation
    */
   static public Collection<String> getUris( final JCas jcas ) {
      return OntologyConceptUtil.getCodes( jcas, DPHE_CODING_SCHEME );
   }

   static public String getCui( final String uri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
         if ( graphNode == null ) {
            LOGGER.debug( "getCui(..) : No Class exists for URI " + uri );
            return "";
         }
         final Object property = graphNode.getProperty( CUI_KEY );
         if ( property == null ) {
            return "";
         }
         tx.success();
         return property.toString();
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
      }
      return "";
   }

   static public SemanticGroup getSemanticGroup( final String uri ) {
      if ( uri.equals( UriConstants.EVENT ) ) {
         return SemanticGroup.EVENT;
      }
      Iterable<Label> labels = null;
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
         if ( graphNode == null ) {
            LOGGER.debug( "getSemanticGroup(..) : No Class exists for URI " + uri );
            return SemanticGroup.UNKNOWN;
         }
         labels = graphNode.getLabels();
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
      }
      if ( labels == null ) {
         return SemanticGroup.UNKNOWN;
      }
      for ( Label label : labels ) {
         if ( label.equals( CLASS_LABEL ) ) {
            continue;
         }
         // Any non-class label should be a semantic group
         return SemanticGroup.getGroup( label.name() );
      }
      return SemanticGroup.UNKNOWN;
   }

   static public String getPreferredText( final String uri ) {
      if ( uri.equals( UriConstants.EVENT ) ) {
         return "Event";
      }
      String prefText = toPreferredText( uri );
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
         if ( graphNode == null ) {
            LOGGER.debug( "getPreferredText(..) : No Class exists for URI " + uri );
            return prefText;
         }
         final Object property = graphNode.getProperty( PREF_TEXT_KEY );
         if ( property == null ) {
            LOGGER.debug( "No Preferred Text for URI " + uri );
         } else {
            prefText = property.toString();
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
      }
      return prefText;
   }

   static private String toPreferredText( final String uri ) {
      final int hashIndex = uri.lastIndexOf( '#' );
      if ( hashIndex < 0 ) {
         return uri.replace( '_', ' ' );
      }
      if ( hashIndex == uri.length() - 1 ) {
         LOGGER.debug( "Cannot get extension, URI ends with hash " + uri );
         return uri.replace( '_', ' ' );
      }
      return uri.substring( hashIndex + 1 )
                .replace( '_', ' ' );
   }

   /**
    * @param dPheUri -
    * @return neo4j Classes for the given URI.  Classes are mention types, not mentions discovered in text.
    */
   static public Node getClassNode( final String dPheUri ) {
      return SearchUtil.getClassNode( Neo4jConnectionFactory.getInstance().getGraph(), dPheUri );
   }

   static public Map<String, Collection<IdentifiedAnnotation>> mapUriAnnotations( final Collection<IdentifiedAnnotation> annotations ) {
      final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         for ( String uri : getUris( annotation ) ) {
            final Collection<IdentifiedAnnotation> uriAnnotations = uriAnnotationMap.computeIfAbsent( uri,
                                                                                                      a -> new ArrayList<>() );
            uriAnnotations.add( annotation );
         }
      }
      return uriAnnotationMap;
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
      final Map<String,Collection<IdentifiedAnnotation>> uriMap = getUriAnnotationsByUriBranch( jcas, lookupWindow,
                                                                                                rootUri );
      return uriMap.values()
                   .stream()
                   .flatMap( Collection::stream )
                   .collect( Collectors.toList() );
   }

   /**
    * @param jcas    -
    * @param rootUri uri of interest
    * @return all IdentifiedAnnotations for the given uri and its children
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByUriBranch( final JCas jcas,
                                                                             final String rootUri ) {
      final Map<String,Collection<IdentifiedAnnotation>> uriMap = getUriAnnotationsByUriBranch( jcas, rootUri );
      return uriMap.values()
                   .stream()
                   .flatMap( Collection::stream )
                   .collect( Collectors.toList() );
   }

   /**
    * @param jcas    -
    * @param rootUri uri of interest
    * @return map of uris and IdentifiedAnnotations for the given uri and its children
    */
   static public Map<String, Collection<IdentifiedAnnotation>> getUriAnnotationsByUriBranch( final JCas jcas,
                                                                                             final String rootUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Collection<String> branchUris = SearchUtil.getBranchUris( graphDb, rootUri );
      final Collection<IdentifiedAnnotation> allAnnotations = JCasUtil.select( jcas, IdentifiedAnnotation.class );
      return getUriAnnotationsByUris( allAnnotations, branchUris );
   }

   /**
    * @param jcas    -
    * @param lookupWindow -
    * @param rootUri uri of interest
    * @param <T>          type for lookup window
    * @return map of uris and IdentifiedAnnotations for the given uri and its children
    */
   static public <T extends Annotation> Map<String, Collection<IdentifiedAnnotation>> getUriAnnotationsByUriBranch( final JCas jcas,
                                                                                                                    final T lookupWindow,
                                                                                                                    final String rootUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Collection<String> branchUris = SearchUtil.getBranchUris( graphDb, rootUri );
      final Collection<IdentifiedAnnotation> windowAnnotations = JCasUtil.selectCovered( jcas,
                                                                                         IdentifiedAnnotation.class,
                                                                                         lookupWindow );
      return getUriAnnotationsByUris( windowAnnotations, branchUris );
   }

   /**
    * @param jcas    -
    * @param uris        uris of interest
    * @return map of uris and IdentifiedAnnotations for the given uris and their children
    */
   static public Map<String, Collection<IdentifiedAnnotation>> getUriAnnotationsByUris( final JCas jcas,
                                                                                        final Collection<String> uris ) {
      final Collection<IdentifiedAnnotation> allAnnotations = JCasUtil.select( jcas, IdentifiedAnnotation.class );
      final Map<String, Collection<IdentifiedAnnotation>> uriMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : allAnnotations ) {
         for ( String uri : getUris( annotation ) ) {
            if ( uris.contains( uri ) ) {
               final Collection<IdentifiedAnnotation> uriAnnotations = uriMap.computeIfAbsent( uri,
                     ia -> new ArrayList<>() );
               uriAnnotations.add( annotation );
               break;
            }
         }
      }
      return uriMap;
   }

   /**
    * @param annotations -
    * @param uris        uris of interest
    * @return map of uris and IdentifiedAnnotations for the given uri and its children
    */
   static public Map<String, Collection<IdentifiedAnnotation>> getUriAnnotationsByUris(
         final Collection<IdentifiedAnnotation> annotations,
         final Collection<String> uris ) {
      final Map<String, Collection<IdentifiedAnnotation>> uriMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         for ( String uri : getUris( annotation ) ) {
            if ( uris.contains( uri ) ) {
               final Collection<IdentifiedAnnotation> uriAnnotations = uriMap.computeIfAbsent( uri,
                     ia -> new ArrayList<>() );
               uriAnnotations.add( annotation );
               break;
            }
         }
      }
      return uriMap;
   }


   static public Collection<String> getBranchUris( final String rootUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      return SearchUtil.getBranchUris( graphDb, rootUri );
   }

   static public Collection<String> getRootUris( final String leafUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      return SearchUtil.getRootUris( graphDb, leafUri );
   }


   static public Collection<String> getBranchUrisWithRelation( final String rootUri,
                                                               final String relationName,
                                                               final String targetUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      return SearchUtil.getBranchUrisWithRelation( graphDb, rootUri, relationName, targetUri );
   }

   static public Collection<String> getBranchUrisWithAttribute( final String rootUri,
                                                                final String attributeName,
                                                                final String value ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      return SearchUtil.getBranchUrisWithAttribute( graphDb, rootUri, attributeName, value );
   }

}
