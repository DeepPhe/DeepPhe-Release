package org.healthnlp.deepphe.nlp.uri;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.healthnlp.deepphe.neo4j.constant.UriConstants2;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.Neo4jRelationUtil;
import org.healthnlp.deepphe.neo4j.util.RelatedUris;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.ctakes.ner.group.dphe.DpheGroup.CANCER;
import static org.apache.ctakes.ner.group.dphe.DpheGroup.MASS;


/**
 * @author SPF , chip-nlp
 * @since {3/9/2023}
 */
public enum UriInfoCache {
   INSTANCE;

   static public UriInfoCache getInstance() {
      return INSTANCE;
   }


   static private final Object LOCK = new Object();
   // Every PERIOD it will check for information over TIMEOUT old and remove them from the cache.
   // 1 minute - If there are ~4 notes/second, ~250 notes/minute, ~50 unique per note (gene, etc.)
   // = ~12500 per minute, * 7 maps = a lot.
   // So make 10 seconds old.   Even at 2 notes/second a lot of garbage uris are generated.
//   static private final long TIMEOUT = 1000 * 10;
   // So make 2 seconds old.   Even at 2 notes/second a lot of garbage uris are generated.
   static private final long TIMEOUT = 1000 * 2;
   static private final long PERIOD = TIMEOUT / 2;
   static private final long START = TIMEOUT + PERIOD;


//   private final Map<String, Long> _timeMap = new ConcurrentHashMap<>();
   private final Map<String, DpheGroup> _uriGroupMap = new ConcurrentHashMap<>();
   private final Map<String, String> _uriPrefTextMap = new ConcurrentHashMap<>();
   private final Map<String, Collection<String>> _uriBranchMap = new ConcurrentHashMap<>();
   private final Map<String, Collection<String>> _uriRootMap = new ConcurrentHashMap<>();
//   private final Map<String, UriRelations> _uriNodeMap = new ConcurrentHashMap<>();
//   private final ScheduledExecutorService _cacheCleaner;

   private final Map<String,Map<String,Integer>> _uriUriDistances = new HashMap<>();
//   private final Map<String,Map<String,Collection<String>>> _uriRelatedUris = new HashMap<>();
   private final Map<String, RelatedUris> _uriRelatedUris = new HashMap<>();

   private final Map<String,String> _uriCuiMap = new HashMap<>();

   UriInfoCache() {
//      _cacheCleaner = Executors.newScheduledThreadPool( 1 );
//      _cacheCleaner.scheduleAtFixedRate( new CacheCleaner(), START, PERIOD, TimeUnit.MILLISECONDS );
   }

   public void close() {
//      _cacheCleaner.shutdown();
   }


   /**
    *
    * @param uri1 -
    * @param uri2 -
    * @return the vertical distance between the two uris.
    * If uri2 is higher than uri1 then the number is negative. MAX_VALUE if they are not in the same branch.
    */
   public int getUriDistance( final String uri1, final String uri2 ) {
      final Map<String,Integer> distances1 = _uriUriDistances.getOrDefault( uri1, Collections.emptyMap() );
      if ( !distances1.isEmpty() ) {
         final Integer distance = distances1.get( uri2 );
         if ( distance != null ) {
//            LogFileWriter.add( "UriInfoCache getUriDistance 1-2 " + uri1 + " -> " + uri2 + " = " + distance );
            return distance;
         }
      }
      final Map<String,Integer> distances2 = _uriUriDistances.getOrDefault( uri2, Collections.emptyMap() );
      if ( !distances2.isEmpty() ) {
         final Integer distance = distances2.get( uri1 );
         if ( distance != null ) {
//            LogFileWriter.add( "UriInfoCache getUriDistance 2-1 " + uri2 + " -> " + uri1 + " = " + distance );
            return distance;
         }
      }
      final int distance = UriInfoUtil.getUriDistance( uri1, uri2 );
      if ( distance != Integer.MAX_VALUE ) {
//         LogFileWriter.add( "UriInfoCache getUriDistance calc " + uri1 + " -> " + uri2 + " = " + distance );
         _uriUriDistances.computeIfAbsent( uri1, u -> new HashMap<>() ).put( uri2, distance );
      }
      return distance;
   }

//   private Map<String,Collection<String>> getRelatedGraphUris( final String uri ) {
//      final Map<String,Collection<String>> relatedUris = _uriRelatedUris.get( uri );
//      if ( relatedUris != null ) {
//         return relatedUris;
//      }
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
//      final Map<String, Collection<String>> relatedInGraph = Neo4jRelationUtil.getRelatedClassUris( graphDb, uri );
//      _uriRelatedUris.put( uri, relatedInGraph );
//      return relatedInGraph;
//   }

   public RelatedUris getRelatedGraphUris( final String uri ) {
      final RelatedUris relatedUris = _uriRelatedUris.get( uri );
      if ( relatedUris != null ) {
         return relatedUris;
      }
      final DpheGroup group = getDpheGroup( uri );
      if ( group == DpheGroup.UNKNOWN ) {
         LogFileWriter.add( "UriInfoCache no group for " + uri );
      }
      if ( group != CANCER && group != MASS ) {
         return RelatedUris.EMPTY_URIS;
      }
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      final RelatedUris relatedInGraph = Neo4jRelationUtil.getAllRelatedClassUris( graphDb, uri );
      _uriRelatedUris.put( uri, relatedInGraph );
      return relatedInGraph;
   }

//   public boolean isLaterality( final String uri ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
//      return getDpheGroup( uri ).equals( DpheGroup.LATERALITY )
//            || UriConstants2.getLeftLungLobes( graphDb ).contains( uri )
//            || UriConstants2.getRightLungLobes( graphDb ).contains( uri );
//   }

   /**
    *
    * @param uris all unique URIs for all annotations in the document.
    */
//   public Map<String, UriRelations> createDocUriNodeMap( final Collection<String> uris ) {
   public void initGraphPlacement( final Collection<String> uris ) {
//      final Map<String, UriRelations> uriNodes = new HashMap<>( uris.size() );
      for ( String uri : uris ) {
         getUriBranch( uri );
         getUriRoots( uri );
//         uriNodes.put( uri, getUriNode( uri ) );
      }
//      return uriNodes;
   }


//   /**
//    *
//    * @param uri -
//    * @param group -
//    */
//   public void initDpheGroup( final String uri, final DpheGroup group ) {
//      final long millis = System.currentTimeMillis();
//      synchronized ( LOCK ) {
//         _uriGroupMap.putIfAbsent( uri, group );
//         _timeMap.put( uri, millis );
//      }
//   }
//
//   /**
//    *
//    * @param uri -
//    * @param prefText -
//    */
//   public void initPrefText( final String uri, final String prefText ) {
//      final long millis = System.currentTimeMillis();
//      synchronized ( LOCK ) {
//         _uriPrefTextMap.put( uri, prefText );
//         _timeMap.put( uri, millis );
//      }
//   }

   /**
    *
    * @param uri -
    * @param group -
    */
   public void initBasics( final String uri, final String cui, final DpheGroup group, final String prefText ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         _uriCuiMap.put( uri, cui );
         _uriGroupMap.put( uri, group );
         _uriPrefTextMap.put( uri, prefText );
//         _timeMap.put( uri, millis );
      }
//      LogFileWriter.add( "initBasics " + uri + " " + group.getName() + " " + prefText );
   }

   public String getCui( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final String cachedCui = _uriCuiMap.get( uri );
         if ( cachedCui != null ) {
//            _timeMap.put( uri, millis );
            return cachedCui;
         }
         LogFileWriter.add( "No CUI stored for " + uri );
         return "Unknown";
      }
   }

   /**
    *
    * @param uri -
    * @return -
    */
   public DpheGroup getDpheGroup( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final DpheGroup cachedGroup = _uriGroupMap.get( uri );
         if ( cachedGroup != null ) {
//            _timeMap.put( uri, millis );
            return cachedGroup;
         }
         LogFileWriter.add( "No Dphe Group stored for " + uri );
         return DpheGroup.UNKNOWN;
      }
   }

   /**
    *
    * @param uri -
    * @return -
    */
   public String getPrefText( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final String cachedText = _uriPrefTextMap.get( uri );
         if ( cachedText != null ) {
//            _timeMap.put( uri, millis );
            return cachedText;
         }
         LogFileWriter.add( "No Pref Text stored for " + uri );
         return "";
      }
   }

   /**
    *
    * @param uri -
    * @return All child URIs.  Does NOT include URI itself.
    */
   public Collection<String> getUriBranch( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final Collection<String> cachedBranch = _uriBranchMap.get( uri );
         if ( cachedBranch != null ) {
//            _timeMap.put( uri, millis );
            return cachedBranch;
         }
         final Collection<String> branch =  Neo4jOntologyConceptUtil.getBranchUris( uri );
         branch.remove( uri );
//         _timeMap.put( uri, millis );
         _uriBranchMap.put( uri, branch );
         return branch;
      }
   }

   /**
    *
    * @param uri -
    * @return All ancestor URIs.  DOES include URI itself.
    */
   public Collection<String> getUriRoots( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final Collection<String> cachedRoots = _uriRootMap.get( uri );
         if ( cachedRoots != null ) {
//            _timeMap.put( uri, millis );
            return cachedRoots;
         }
         final Collection<String> roots = Neo4jOntologyConceptUtil.getRootUris( uri );
         roots.remove( "Thing" );
         roots.remove( "DeepPhe" );
//         _timeMap.put( uri, millis );
         _uriRootMap.put( uri, roots );
         return roots;
      }
   }


//   public UriRelations getUriNode( final String uri ) {
//      final long millis = System.currentTimeMillis();
//      synchronized ( LOCK ) {
//         final UriRelations cachedNode = _uriNodeMap.get( uri );
//         if ( cachedNode != null ) {
//            _timeMap.put( uri, millis );
//            return cachedNode;
//         }
//         final DpheGroup grouping = UriInfoCache.getInstance().getDpheGroup( uri );
//         if ( grouping != DpheGroup.CANCER && grouping != DpheGroup.MASS ) {
//            final UriRelations uriRelations = new UriRelations( uri, Collections.emptyMap(), Collections.emptyMap() );
////            final UriRelations uriRelations = new UriRelations( uri, Collections.emptyMap() );
//            _timeMap.put( uri, millis );
//            _uriNodeMap.put( uri, uriRelations );
//            return uriRelations;
//         }
////         final Map<String,Collection<String>> relatedGraphUris = getRelatedGraphUris( uri );
////         final Map<String,Collection<String>> siteRelations = new HashMap<>();
////         final Map<String,Collection<String>> nonSiteRelations = new HashMap<>();
////         for ( Map.Entry<String,Collection<String>> relation : relatedGraphUris.entrySet() ) {
////            if ( RelationConstants.isHasSiteRelation( relation.getKey() ) ) {
//////               LogFileWriter.add( "Possible uri sites: " + uri + " " + relation.getKey() + " : "
//////                                             + String.join( ",", relation.getValue() ) );
//////               siteRelations.computeIfAbsent( relation.getKey(), s -> new HashSet<>() )
//////                            .addAll( relation.getValue() );
////               siteRelations.computeIfAbsent( RelationConstants.HAS_ASSOCIATED_SITE, s -> new HashSet<>() )
////                            .addAll( relation.getValue() );
////            } else {
////               nonSiteRelations.computeIfAbsent( relation.getKey(), s -> new HashSet<>() )
////                               .addAll( relation.getValue() );
////            }
////         }
////         nonSiteRelations.remove( RelationConstants.IS_PART_OF_SITE );
////         final UriRelations node = new UriRelations( uri, nonSiteRelations, siteRelations );
//         final Neo4jRelationUtil.RelatedUris relatedGraphUris = getRelatedGraphUris( uri );
//         final UriRelations node = new UriRelations( uri, relatedGraphUris );
//         _timeMap.put( uri, millis );
//         _uriNodeMap.put( uri, node );
//         return node;
//      }
//   }

//   public UriRelations getUriNode( final String uri ) {
//      final long millis = System.currentTimeMillis();
//      synchronized ( LOCK ) {
//         final UriRelations cachedNode = _uriNodeMap.get( uri );
//         if ( cachedNode != null ) {
//            _timeMap.put( uri, millis );
//            return cachedNode;
//         }
//         final DpheGroup grouping = UriInfoCache.getInstance().getDpheGroup( uri );
//         if ( grouping != DpheGroup.CANCER && grouping != DpheGroup.MASS ) {
//            final UriRelations uriRelations = new UriRelations( uri, Collections.emptyMap(), Collections.emptyMap() );
//            _timeMap.put( uri, millis );
//            _uriNodeMap.put( uri, uriRelations );
//            return uriRelations;
//         }
//         final Neo4jRelationUtil.RelatedUris relatedGraphUris = getRelatedGraphUris( uri );
//         final Map<String,Collection<String>> siteRelations = new HashMap<>();
//         final Map<String,Collection<String>> nonSiteRelations = new HashMap<>();
//         for ( Map.Entry<String,Collection<String>> relation : relatedGraphUris.getRelationTargets().entrySet() ) {
//            if ( RelationConstants.isHasSiteRelation( relation.getKey() ) ) {
////               LogFileWriter.add( "Possible uri sites: " + uri + " " + relation.getKey() + " : "
////                                             + String.join( ",", relation.getValue() ) );
////               siteRelations.computeIfAbsent( relation.getKey(), s -> new HashSet<>() )
////                            .addAll( relation.getValue() );
//               siteRelations.computeIfAbsent( RelationConstants.HAS_ASSOCIATED_SITE, s -> new HashSet<>() )
//                            .addAll( relation.getValue() );
//            } else {
//               nonSiteRelations.computeIfAbsent( relation.getKey(), s -> new HashSet<>() )
//                               .addAll( relation.getValue() );
//            }
//         }
////         nonSiteRelations.remove( RelationConstants.IS_PART_OF_SITE );
//         final UriRelations node = new UriRelations( uri, nonSiteRelations, siteRelations );
//         _timeMap.put( uri, millis );
//         _uriNodeMap.put( uri, node );
//         return node;
//      }
//   }

   public void clear() {
//      _timeMap.clear();
      _uriBranchMap.clear();
      _uriRootMap.clear();
      _uriUriDistances.clear();
      _uriCuiMap.clear();
      _uriGroupMap.clear();
      _uriPrefTextMap.clear();
      _uriRelatedUris.clear();
   }

//   private final class CacheCleaner implements Runnable {
//      public void run() {
//         final long old = System.currentTimeMillis() - TIMEOUT;
//         synchronized ( LOCK ) {
//            final Collection<String> removals = new ArrayList<>();
//            for ( Map.Entry<String, Long> timeEntry : _timeMap.entrySet() ) {
//               if ( timeEntry.getValue() < old ) {
//                  removals.add( timeEntry.getKey() );
//               }
//            }
//            LogFileWriter.add( "Cache Cleaning " + removals.size() + " : " + _timeMap.size() + "    " + old );
//            for ( String removal : removals ) {
//               _timeMap.remove( removal );
//               _uriBranchMap.remove( removal );
//               _uriRootMap.remove( removal );
////               _uriNodeMap.remove( removal );
//               _uriUriDistances.remove( removal );
//               _uriUriDistances.values().forEach( d -> d.remove( removal ) );
//               _uriCuiMap.remove( removal );
//               _uriGroupMap.remove( removal );
//               _uriPrefTextMap.remove( removal );
//               _uriRelatedUris.remove( removal );
//            }
//         }
//      }
//   }

}
