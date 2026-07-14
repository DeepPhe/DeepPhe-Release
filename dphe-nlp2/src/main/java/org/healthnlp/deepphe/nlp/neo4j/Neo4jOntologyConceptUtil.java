package org.healthnlp.deepphe.nlp.neo4j;


import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.Neo4jTraverserFactory;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.*;

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

//   /**
//    * @param annotation -
//    * @return most specific dphe URI that exists for the given annotation
//    */
//   static public String getUri( final IdentifiedAnnotation annotation ) {
//      return UriUtil.getMostSpecificUri( getUris( annotation ) );
//   }


//   /**
//    * @param jcas         -
//    * @param lookupWindow -
//    * @param <T>          type for lookup window
//    * @return all dphe URIs that exist for the given window
//    */
//   static public <T extends Annotation> Collection<String> getUris( final JCas jcas, final T lookupWindow ) {
//      return OntologyConceptUtil.getCodes( jcas, lookupWindow, DPHE_CODING_SCHEME );
//   }

//   /**
//    *
//    * @param jcas -
//    * @return all dphe URIs that exist for the given annotation
//    */
//   static public Collection<String> getUris( final JCas jcas ) {
//      return OntologyConceptUtil.getCodes( jcas, DPHE_CODING_SCHEME );
//   }
//
//   static public String getCui( final String uri ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      try ( Transaction tx = graphDb.beginTx() ) {
//         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
//         if ( graphNode == null ) {
//            LOGGER.debug( "getCui(..) : No Class exists for URI " + uri );
//            return "";
//         }
//         final Object property = graphNode.getProperty( CUI_KEY );
//         if ( property == null ) {
//            return "";
//         }
//         tx.success();
//         return property.toString();
//      } catch ( MultipleFoundException mfE ) {
//         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
//      }
//      return "";
//   }


   static public Collection<String> getIcdoCodes( final String uri ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                  .getGraph();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
         if ( rootBase == null ) {
            LOGGER.error( "No Class exists for URI " + uri );
            tx.success();
            return Collections.emptyList();
         }
         try {
            final Object rootIcdo = rootBase.getProperty( ICDO_KEY );
            if ( rootIcdo != null ) {
               tx.success();
               return Collections.singletonList( rootIcdo.toString() );
            }
         } catch ( NotFoundException ignored ) {
         }
         final Collection<String> icdoCodes = new HashSet<>();
         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
                                                                     .getOrderedRootsTraverser( graphDb );
         for ( Node node : traverser.traverse( rootBase )
                                    .nodes() ) {
            try {
               final Object icdo = node.getProperty( ICDO_KEY );
               if ( icdo != null ) {
                  icdoCodes.add( icdo.toString() );
               }
            } catch ( NotFoundException ignored ) {
            }
         }
         tx.success();
         return icdoCodes;
      } catch ( MultipleFoundException mfE ) {
         LOGGER.error( mfE.getMessage(), mfE );
      }
      return Collections.emptyList();
   }

   static public String getIcdoTopoCode( final String uri ) {
      final List<String> icdos
            = getIcdoCodes( uri ).stream()
                                 .filter( i -> i.startsWith( "C" ) )
                                 .filter( i -> !i.startsWith( "C80" ) )  // Unknown
                                 .filter( i -> !i.startsWith( "C76" ) )  // body region
                                 .filter( i -> !i.startsWith( "C44" ) ) // skin
                                 .sorted()
                                 .collect( Collectors.toList() );
      if ( icdos.isEmpty() ) {
         return "";
      }
      if ( icdos.size() == 1 ) {
         return icdos.get( 0 );
      }
      final String firstMajor = icdos.get( 0 );
      if ( firstMajor.contains( "." ) ) {
         // Contains major site and minor site
         return firstMajor;
      }
      final String withMinor = icdos.get( 1 );
      if ( withMinor.startsWith( firstMajor ) ) {
         return withMinor;
      }
      return firstMajor;
   }


   static public String getIcdoMorphCode( final String uri ) {
      final List<String> icdos
            = getIcdoCodes( uri ).stream()
                                 .filter( i -> !i.startsWith( "C" ) )
                                 .filter( i -> !i.contains( "-" ) )
                                 .sorted()
                                 .collect( Collectors.toList() );
      if ( icdos.isEmpty() ) {
         return "";
      }
      return icdos.get( icdos.size() - 1 );
//      if ( icdos.size() == 1 ) {
//         return icdos.get( 0 );
//      }
//      final String firstHisto = icdos.get( 0 );
//      if ( firstHisto.contains( "/" ) ) {
//         // Contains histology and behavior
//         return firstHisto;
//      }
//      final String withBehave = icdos.get( 1 );
//      if ( withBehave.startsWith( firstHisto ) ) {
//         return withBehave;
//      }
//      return firstHisto;
   }


//   static public SemanticGroup getBestSemanticGroup( final String uri ) {
//      return SemanticGroup.getBestGroup( getSemanticGroups( uri ) );
//   }
//
//   static public Collection<SemanticGroup> getSemanticGroups( final String uri ) {
//      return getSemanticTuis( uri ).stream()
//                                   .map( SemanticTui::getGroup )
//                                   .collect( Collectors.toSet() );
//   }
//
//   static public Collection<SemanticTui> getSemanticTuis( final String uri ) {
//      return getTuis( uri ).stream().map( SemanticTui::getTuiFromCode ).collect( Collectors.toSet() );
//   }
//
//   static public Collection<String> getTuis( final String uri ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                                  .getGraph();
//      try ( Transaction tx = graphDb.beginTx() ) {
//         final Node rootBase = SearchUtil.getClassNode( graphDb, uri );
//         if ( rootBase == null ) {
//            LOGGER.error( "No Class exists for URI " + uri );
//            tx.success();
//            return Collections.emptyList();
//         }
//         try {
//            final Object tuiObject = rootBase.getProperty( TUI_KEY );
//            if ( tuiObject != null ) {
//               final Collection<String> tuiSet = getObjectTuis( tuiObject );
//               if ( !tuiSet.isEmpty() ) {
//                  tx.success();
//                  return tuiSet;
//               }
//            }
//         } catch ( NotFoundException ignored ) {
//         }
//         final Collection<String> tuiSet = new HashSet<>();
//         final TraversalDescription traverser = Neo4jTraverserFactory.getInstance()
//                                                                     .getOrderedRootsTraverser( graphDb );
//         for ( Node node : traverser.traverse( rootBase )
//                                    .nodes() ) {
//            try {
//               final Object tuiObject = node.getProperty( TUI_KEY );
//               if ( tuiObject != null ) {
//                  tuiSet.addAll( getObjectTuis( tuiObject ) );
//               }
//            } catch ( NotFoundException ignored ) {
//            }
//         }
//         tx.success();
//         return tuiSet;
//      } catch ( MultipleFoundException mfE ) {
//         LOGGER.error( mfE.getMessage(), mfE );
//      }
//      return Collections.emptyList();
//   }
//
//   static private Collection<String> getObjectTuis( final Object tuiObject ) {
//      final String tuiSet = DataUtil.objectToString( tuiObject );
//      if ( tuiSet.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      return Arrays.asList( StringUtil.fastSplit( tuiSet, '|' ) );
//   }

//   static public int getClassLevel( final String uri ) {
//      if ( uri.equals( UriConstants.EVENT ) ) {
//         return -1;
//      }
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      int classLevel = -1;
//      try ( Transaction tx = graphDb.beginTx() ) {
//         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
//         if ( graphNode == null ) {
//            LOGGER.debug( "getClassLevel(..) : No Class exists for URI " + uri );
//            tx.success();
//            return -1;
//         }
//         final Object property = graphNode.getProperty( LEVEL_KEY );
//         if ( property == null ) {
//            LOGGER.debug( "No Class Level for URI " + uri );
//         } else {
//            classLevel = getLevel( property );
//         }
//         tx.success();
//      } catch ( MultipleFoundException mfE ) {
//         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
//      }
//      return classLevel;
//   }
//
//   static private int getLevel( final Object value ) {
//      if ( value instanceof Number ) {
//         return ((Number)value).intValue();
//      }
//      try {
//         return Integer.parseInt( value.toString() );
//      } catch ( NumberFormatException nfE ) {
//         return -1;
//      }
//   }

//   static public String getPreferredText( final String uri ) {
//      if ( uri.equals( UriConstants.EVENT ) ) {
//         return "Event";
//      }
//      String prefText = toPreferredText( uri );
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                                  .getGraph();
//      try ( Transaction tx = graphDb.beginTx() ) {
//         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
//         if ( graphNode == null ) {
//            LOGGER.debug( "getPreferredText(..) : No Class exists for URI " + uri );
//            tx.success();
//            return prefText;
//         }
//         final Object property = graphNode.getProperty( PREF_TEXT_KEY );
//         if ( property == null ) {
//            LOGGER.debug( "No Preferred Text for URI " + uri );
//         } else {
//            prefText = property.toString();
//         }
//         tx.success();
//      } catch ( MultipleFoundException mfE ) {
//         LOGGER.error( uri + " : " + mfE.getMessage(), mfE );
//      }
//      return prefText;
//   }
//
//   static private String toPreferredText( final String uri ) {
//      final int hashIndex = uri.lastIndexOf( '#' );
//      if ( hashIndex < 0 ) {
//         return uri.replace( '_', ' ' );
//      }
//      if ( hashIndex == uri.length() - 1 ) {
//         LOGGER.debug( "Cannot get extension, URI ends with hash " + uri );
//         return uri.replace( '_', ' ' );
//      }
//      return uri.substring( hashIndex + 1 )
//                .replace( '_', ' ' );
//   }

   /**
    * @param dPheUri -
    * @return neo4j Classes for the given URI.  Classes are mention types, not mentions discovered in text.
    */
   static public Node getClassNode( final String dPheUri ) {
      return SearchUtil.getClassNode( EmbeddedConnection.getInstance().getGraph(), dPheUri );
   }

   static public Map<String, Collection<IdentifiedAnnotation>> mapUriAnnotations( final Collection<IdentifiedAnnotation> annotations ) {
      final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         for ( String uri : getUris( annotation ) ) {
            uriAnnotationMap.computeIfAbsent( uri, a -> new HashSet<>() ).add( annotation );
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
                   .collect( Collectors.toSet() );
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
                   .collect( Collectors.toSet() );
   }

   /**
    * @param jcas -
    * @param uris uris of interest
    * @return IdentifiedAnnotations for the given uris
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByUris( final JCas jcas,
                                                                        final Collection<String> uris ) {
      final Predicate<IdentifiedAnnotation> hasUri = a -> getUris( a ).stream().anyMatch( uris::contains );
      return JCasUtil.select( jcas, IdentifiedAnnotation.class )
                     .stream()
                     .filter( hasUri )
                     .collect( Collectors.toSet() );
   }


   /**
    * @param jcas    -
    * @param rootUri uri of interest
    * @return map of uris and IdentifiedAnnotations for the given uri and its children
    */
   static public Map<String, Collection<IdentifiedAnnotation>> getUriAnnotationsByUriBranch( final JCas jcas,
                                                                                             final String rootUri ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
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
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
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
               uriMap.computeIfAbsent( uri, ia -> new HashSet<>() ).add( annotation );
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
               uriMap.computeIfAbsent( uri, ia -> new HashSet<>() ).add( annotation );
               break;
            }
         }
      }
      return uriMap;
   }


   static public Collection<String> getBranchUris( final String rootUri ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                  .getGraph();
      return SearchUtil.getBranchUris( graphDb, rootUri );
   }

   static public Collection<String> getRootUris( final String leafUri ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                  .getGraph();
      return SearchUtil.getRootUris( graphDb, leafUri );
   }


   static public Collection<String> getBranchUrisWithRelation( final String rootUri,
                                                               final String relationName,
                                                               final String targetUri ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                  .getGraph();
      return SearchUtil.getBranchUrisWithRelation( graphDb, rootUri, relationName, targetUri );
   }

   static public Collection<String> getBranchUrisWithAttribute( final String rootUri,
                                                                final String attributeName,
                                                                final String value ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                  .getGraph();
      return SearchUtil.getBranchUrisWithAttribute( graphDb, rootUri, attributeName, value );
   }

}
