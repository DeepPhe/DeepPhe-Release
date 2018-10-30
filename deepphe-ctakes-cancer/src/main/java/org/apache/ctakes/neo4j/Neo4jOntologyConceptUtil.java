package org.apache.ctakes.neo4j;


import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
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
      return ConceptInstanceFactory.getMostSpecificUri( getUris( annotation ) );
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
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = SearchUtil.getClassNode( graphDb, rootUri );
         if ( branchRoot == null ) {
            LOGGER.debug( "getBranchUris(..) : No Class exists for URI " + rootUri );
            tx.success();
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, IS_A_PROP );
         final Collection<String> branch = traverser.traverse( branchRoot )
                                                    .nodes()
                                                    .stream()
                                                    .map( n -> n.getProperty( NAME_KEY ) )
                                                    .map( Object::toString )
                                                    .distinct()
                                                    .collect( Collectors.toList() );
         tx.success();
         return branch;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public Collection<String> getRootUris( final String leafUri ) {
      if ( leafUri == null || leafUri.isEmpty() ) {
         new Exception( "getRootUris(..) : Empty or null URI" ).printStackTrace();
      }
      if ( leafUri.equals( UriConstants.EVENT ) ) {
         return Arrays.asList( UriConstants.THING, UriConstants.EVENT );
      }
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootLeaf = SearchUtil.getClassNode( graphDb, leafUri );
         if ( rootLeaf == null ) {
            LOGGER.debug( "getRootUris(..) : No Class exists for URI " + leafUri );
            tx.success();
            return Collections.emptyList();
         }
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getRootsTraverser( graphDb, IS_A_PROP );
         final Collection<String> root = traverser.traverse( rootLeaf )
                                                  .nodes()
                                                  .stream()
                                                  .map( n -> n.getProperty( NAME_KEY ) )
                                                  .map( Object::toString )
                                                  .distinct()
                                                  .collect( Collectors.toList() );
         tx.success();
         return root;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( leafUri + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }


   static private boolean hasTarget( final Iterable<Relationship> relationships, final Node target ) {
      return relationships != null && StreamSupport.stream( relationships.spliterator(), false ).map( Relationship::getEndNode ).anyMatch( target::equals );
   }

   static public Collection<String> getBranchUrisWithRelation( final String rootUri,
                                                               final String relationName,
                                                               final String targetUri ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node branchRoot = SearchUtil.getClassNode( graphDb, rootUri );
         if ( branchRoot == null ) {
            LOGGER.debug( "getBranchUrisWithRelation(..) : No Class exists for URI " + rootUri );
            tx.success();
            return Collections.emptyList();
         }
         final Node targetNode = SearchUtil.getClassNode( graphDb, targetUri );
         if ( targetNode == null ) {
            LOGGER.debug( "getBranchUrisWithRelation(..) : No Class exists for URI " + targetUri );
            tx.success();
            return Collections.emptyList();
         }
         final RelationshipType relation = RelationshipType.withName( relationName );
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getBranchTraverser( graphDb, IS_A_PROP );
         final Collection<String> targetRoots
               = traverser.traverse( branchRoot )
                          .nodes()
                          .stream()
                          .filter( n -> hasTarget( n.getRelationships( Direction.OUTGOING, relation ), targetNode ) )
                          .map( n -> n.getProperty( NAME_KEY ) )
                          .map( Object::toString )
                          .distinct()
                          .collect( Collectors.toList() );
         tx.success();
         return targetRoots.stream()
                           .map( u -> SearchUtil.getBranchUris( graphDb, u ) )
                           .flatMap( Collection::stream )
                           .distinct()
                           .collect( Collectors.toList() );
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( rootUri + " , " + targetUri + " " + relationName + " : " + mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }


}
