package org.healthnlp.deepphe.core.uri;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.UriConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2018
 */
//@Immutable
final public class UriUtil {

   static private final Logger LOGGER = Logger.getLogger( "UriUtil" );

   private UriUtil() {
   }

   static public Map<String, Collection<String>> mapUriRoots( final Collection<String> uris ) {
      final Map<String,Collection<String>> uriRootsMap = new HashMap<>();
      for ( String uri : new HashSet<>( uris ) ) {
         uriRootsMap.put( uri, Neo4jOntologyConceptUtil.getRootUris( uri ) );
      }
      return uriRootsMap;
   }


   static public boolean containsUri( final String uri, Collection<String> toMatch ) {
      return toMatch.contains( uri );
   }

   static public boolean containsUri( final String uri, final String... toMatch ) {
      return Arrays.asList( toMatch ).contains( uri );
   }


   static public boolean isUriBranchMatch( final String uri1, final String uri2 ) {
      return uri1.equals( uri2 ) || isUriBranchMatch( Collections.singletonList( uri1 ), Collections.singletonList( uri2 ) );
   }

   static public boolean isUriBranchMatch( final Collection<String> uriSet1, final Collection<String> uriSet2 ) {
      final Map<String,Collection<String>> uri2BranchMap = new HashMap<>( uriSet2.size() );
      for ( String uri1 : uriSet1 ) {
         final Collection<String> branch1 = Neo4jOntologyConceptUtil.getBranchUris( uri1 );
         for ( String uri2 : uriSet2 ) {
            if ( isUriBranchMatch( uri1, branch1, uri2, uri2BranchMap ) ) {
               return true;
            }
         }
      }
      return false;
   }

   static private boolean isUriBranchMatch( final String uri1, final Collection<String> uriBranch1,
                                           final String uri2, final Map<String,Collection<String>> uri2BranchMap ) {
      if ( uriBranch1.contains( uri2 ) ) {
         return true;
      }
      final Collection<String> branch2
            = uri2BranchMap.computeIfAbsent( uri2, u -> Neo4jOntologyConceptUtil.getBranchUris( uri2 ) );
      return branch2.contains( uri1 );
   }

   static public boolean isUriInBranch( final String uri, final String rootUri ) {
      return uri.equals( rootUri ) || isUriInBranch( uri, Neo4jOntologyConceptUtil.getBranchUris( rootUri ) );
   }

   static public boolean isUriInBranch( final String uri, final Collection<String> branch ) {
      return branch.contains( uri );
   }



   static public Collection<Collection<String>> getAssociatedUris( final Collection<String> uris ) {
      return getAssociatedUriMap( uris ).values();
   }

   /**
    *
    * @param uris -
    * @return Map of each "best" uri and the existing other uris that are its roots
    */
   static public Map<String,Collection<String>> getAssociatedUriMap( final Collection<String> uris ) {
      if ( uris.size() == 1 ) {
         final Map<String,Collection<String>> map = new HashMap<>( 1 );
         uris.forEach( u -> map.put( u, new HashSet<>( uris ) ) );
         return map;
      }
      // Join all uris that fall within a root tree
      final Map<String,String> bestRoots = getBestRoots( uris );
      final Map<String,Collection<String>> rootChildrenMap = new HashMap<>();
      for ( Map.Entry<String,String> bestRoot : bestRoots.entrySet() ) {
         // fill the map of each "best" root uri to a list of all leaf uris for which it is best.
         rootChildrenMap.computeIfAbsent( bestRoot.getValue(), u -> new ArrayList<>() ).add( bestRoot.getKey() );
      }

      final Map<String,Collection<String>> bestAssociations = new HashMap<>();
      for ( Map.Entry<String,Collection<String>> rootLeafs : rootChildrenMap.entrySet() ) {
         final Map<String, Collection<String>> uniqueChildren = getUniqueChildren( rootLeafs.getValue() );
//             * Occipital_Lobe : [Occipital_Lobe]
//             * Parietal_Lobe : [Parietal_Lobe]
//             * Brain : [Occipital_Lobe, Parietal_Lobe]
//             * Nervous_System : [Occipital_Lobe, Parietal_Lobe]
         for ( Map.Entry<String,Collection<String>> rootBest : uniqueChildren.entrySet() ) {
            rootBest.getValue().forEach( u -> bestAssociations.computeIfAbsent( u, l -> new HashSet<>() )
                                                              .add( rootBest.getKey() ) );
         }
      }
//             * Occipital_Lobe : [Occipital_Lobe, Brain, Nervous_System]
//             * Parietal_Lobe : [Parietal_Lobe, Brain, Nervous_System]
      return bestAssociations;
   }

   /**
    *
    * @param uris -
    * @return Map of each "best" uri and the existing other uris that are its roots
    */
   static public Map<String,Collection<String>> getAllAssociatedUriMap( final Collection<String> uris ) {
      if ( uris.size() == 1 ) {
         final Map<String,Collection<String>> map = new HashMap<>( 1 );
         uris.forEach( u -> map.put( u, new HashSet<>( uris ) ) );
         return map;
      }
      // Join all uris that fall within a root tree
      final Map<String,String> bestRoots = getAllBestRoots( uris );
      final Map<String,Collection<String>> rootChildrenMap = new HashMap<>();
      for ( Map.Entry<String,String> bestRoot : bestRoots.entrySet() ) {
         // fill the map of each "best" root uri to a list of all leaf uris for which it is best.
         rootChildrenMap.computeIfAbsent( bestRoot.getValue(), u -> new HashSet<>() )
                        .add( bestRoot.getKey() );
      }

      final Map<String,Collection<String>> bestAssociations = new HashMap<>();
      for ( Map.Entry<String,Collection<String>> rootLeafs : rootChildrenMap.entrySet() ) {
         final Map<String, Collection<String>> uniqueChildren = getAllUniqueChildren( rootLeafs.getValue() );
//             * Occipital_Lobe : [Occipital_Lobe]
//             * Parietal_Lobe : [Parietal_Lobe]
//             * Brain : [Occipital_Lobe, Parietal_Lobe]
//             * Nervous_System : [Occipital_Lobe, Parietal_Lobe]
         for ( Map.Entry<String,Collection<String>> rootBest : uniqueChildren.entrySet() ) {
            rootBest.getValue().forEach( u -> bestAssociations.computeIfAbsent( u, l -> new HashSet<>() )
                                                              .add( rootBest.getKey() ) );
         }
      }
//             * Occipital_Lobe : [Occipital_Lobe, Brain, Nervous_System]
//             * Parietal_Lobe : [Parietal_Lobe, Brain, Nervous_System]
      return bestAssociations;
   }


   static public Map<String,String> getBestRoots( final Collection<String> uris ) {
      final Map<String, Collection<String>> uriBranches
            = uris.stream()
                  .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getBranchUris ) );
      return getBestRoots( uriBranches );
   }

   static public Map<String,String> getAllBestRoots( final Collection<String> uris ) {
      final Map<String, Collection<String>> uriBranches
            = uris.stream()
                  .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getBranchUris ) );
      final Map<String,Integer> uriLevelMap = uris.stream()
                                                  .collect(
                                                        Collectors.toMap( Function.identity(),
                                                                          Neo4jOntologyConceptUtil::getClassLevel ) );
      return getAllBestRoots( uriBranches, uriLevelMap );
   }


   static private Map<String,String> getBestRoots( final Map<String, Collection<String>> uriBranches ) {
      final List<String> uris = new ArrayList<>( uriBranches.keySet() );
      // We don't want to mess up modifiers with things like "Left" -> "Left_Arm"
      // TODO : BODY_MODIFIER does not exist
//      final Collection<String> modifiers = new ArrayList<>( Neo4jOntologyConceptUtil.getBranchUris( BODY_MODIFIER ) );
      final Collection<String> modifiers = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( LATERALITY ) );
      modifiers.add( MASS );
      modifiers.add( DISEASE );
      modifiers.add( NEOPLASM );
//      modifiers.add( NEOPLASTIC_DISEASE );
      modifiers.add( MALIGNANT_NEOPLASM );
      final List<String> modifierUris = uris.stream().filter( modifiers::contains ).collect( Collectors.toList() );
      uris.removeAll( modifierUris );
      uris.sort( String.CASE_INSENSITIVE_ORDER );
      // Create map seeded with each uri as best of itself
      final Map<String, String> uriBestRootMap
            = uriBranches.keySet().stream()
                         .collect( Collectors.toMap( Function.identity(), Function.identity() ) );
      for ( int i=0; i<uris.size()-1; i++ ) {
         final String iUri = uris.get( i );
         // A better iUri may have already been set when it was a j
         String bestIuri = uriBestRootMap.get( iUri );
         Collection<String> iBranch = uriBranches.get( bestIuri );
         for ( int j = i + 1; j < uris.size(); j++ ) {
            final String jUri = uris.get( j );
            // A better jUri may have already been set when previously compared to an i
            final String bestJuri = uriBestRootMap.get( jUri );
            Collection<String> jBranch = uriBranches.get( bestJuri );
            if ( jBranch.size() > iBranch.size() && jBranch.contains( bestIuri ) ) {
               replaceWithBest( bestIuri, bestJuri, uris, 0, i, uriBestRootMap );
               bestIuri = bestJuri;
               iBranch = jBranch;
            } else if ( iBranch.size() > jBranch.size() && iBranch.contains( bestJuri ) ) {
               replaceWithBest( bestJuri, bestIuri, uris, j, uris.size(), uriBestRootMap );
            } else {
               final String closeEnoughUri = getCloseUriRoot( bestIuri, bestJuri );
               if ( closeEnoughUri != null ) {
                  if ( bestJuri.equals( closeEnoughUri ) ) {
                     replaceWithBest( bestIuri, bestJuri, uris, 0, i, uriBestRootMap );
                     bestIuri = bestJuri;
                     iBranch = jBranch;
                  } else {
                     replaceWithBest( bestJuri, bestIuri, uris, j, uris.size(), uriBestRootMap );
                  }
               }
            }
         }
      }
      // put modifiers back in
      modifierUris.forEach( u -> uriBestRootMap.put( u, u ) );

//      LOGGER.info( "Best root for each uri: " );
//      uriBestRootMap.forEach( (k,v) -> LOGGER.info( k + " : " + v ) );

      return uriBestRootMap;
   }


   static private final class UriBestRootStore {
      private final String _uri;
      private final Collection<String> _uriBranch;
      private final int _uriLevel;
      private String _bestRoot;
      private Collection<String> _bestRootBranch;
      private int _bestRootLevel;
      private UriBestRootStore( final String uri,
                                final Map<String, Collection<String>> uriBranches,
                                final Map<String,Integer> uriLevels ) {
         _uri = uri;
         _uriBranch = uriBranches.get( uri );
         _uriLevel = uriLevels.get( uri );
         _bestRoot = uri;
         _bestRootBranch = _uriBranch;
         _bestRootLevel = _uriLevel;
      }
      String getUri() {
         return _uri;
      }
      String getBestRoot() {
         return _bestRoot;
      }
      private void checkAsBestRoot( final UriBestRootStore otherBest ) {
//         LOGGER.info( "Checking " + toString() + "\nvs. " + otherBest.toString() );
         if ( _uri.equals( otherBest._uri )
              || _bestRoot.equals( otherBest._uri )
              || !otherBest._uriBranch.contains( _uri ) ) {
            // Equal, already the root, or not a possible root.
            return;
         }
         if ( otherBest._uriBranch.contains( _bestRoot ) ) {
            // The other branch contains this current best root.
            setBest( otherBest );
         } else if ( otherBest._uriLevel < _bestRootLevel ) {
            // The other is higher in the overall ontology hierarchy.
            setBest( otherBest );
         } else if (otherBest._uriBranch.size() > _bestRootBranch.size() ) {
            // The other has a bigger branch.
            setBest( otherBest );
         }

      }
      private void setBest( final UriBestRootStore otherBest ) {
//         LOGGER.info( "Setting Best " + otherBest );
         _bestRoot = otherBest._uri;
         _bestRootBranch = otherBest._uriBranch;
         _bestRootLevel = otherBest._uriLevel;
      }
      public String toString() {
         return "URI  " + _uri + " " + _uriLevel + " : "
//                + _uriBranch.stream().sorted().collect( Collectors.joining( " " ) ) + "\n"
                + "BEST " + _bestRoot + " " + _bestRootLevel + " : ";
//                + _bestRootBranch.stream().sorted().collect( Collectors.joining( " " ) );
      }
   }

   static private Map<String,String> getAllBestRoots( final Map<String, Collection<String>> uriBranches,
                                                      final Map<String,Integer> uriLevels ) {
      // Create map seeded with each uri as best of itself
      final List<UriBestRootStore> uriBestRootStores
            = uriBranches.keySet()
                        .stream()
                        .map( u -> new UriBestRootStore( u, uriBranches,  uriLevels ) )
                         .sorted( Comparator.comparing( UriBestRootStore::getUri ) )
                         .collect( Collectors.toList() );
      for ( int i=0; i<uriBestRootStores.size()-1; i++ ) {
         // A better iUri may have already been set when it was a j
         final UriBestRootStore iBest = uriBestRootStores.get( i );
         for ( int j = i + 1; j < uriBestRootStores.size(); j++ ) {
            // A better jUri may have already been set when previously compared to an i.  Otherwise it is jUri.
            final UriBestRootStore jBest = uriBestRootStores.get( j );
            iBest.checkAsBestRoot( jBest );
            jBest.checkAsBestRoot( iBest );
         }
      }
//      LOGGER.info( "All Best root for each uri: " );
//      uriBestRootStores.forEach( LOGGER::info );
      return uriBestRootStores.stream()
                              .collect( Collectors.toMap( UriBestRootStore::getUri,
                                                          UriBestRootStore::getBestRoot ) );
   }

//   static private String getBestRoot( final String currentBest, final String candidateBest,
//                                     final Map<String,Collection<String>> uriBranches,
//                                     final Map<String,Integer> uriLevels ) {
//      final Collection<String> currentBestBranch = uriBranches.get( currentBest );
//      final Collection<String> candidateBestBranch = uriBranches.get( candidateBest );
//      if ( currentBestBranch.size() == candidateBestBranch.size() ) {
//         // any best root's branch must contain the other uri and its branch, so it must be larger.
//         return "";
//      }
//      // See if one is the root of the other.
//      final boolean currentInCandidate = candidateBestBranch.contains( currentBest );
//      final boolean candidateInCurrent = currentBestBranch.contains( candidateBest );
//      if ( currentInCandidate && !candidateInCurrent ) {
//         return candidateBest;
//      } else if ( !currentInCandidate && candidateInCurrent ) {
//         return currentBest;
//      }
//      // Both are roots of uri, and within each other's branch.  Use the root with a higher level.
//      final int currentLevel = uriLevels.get( currentBest );
//      final int candidateLevel = uriLevels.get( candidateBest );
//      if ( currentLevel < candidateLevel ) {
//         return currentBest;
//      } else if ( currentLevel > candidateLevel ) {
//         return candidateBest;
//      }
//      // As a last resort, use URI length;
//      return currentBest.length() < candidateBest.length() ? currentBest : candidateBest;
//   }
//
//   static private void setBestRoots( final String bestRoot,
//                                     final String toReplace,
//                                     final Map<String,String> uriBestRoots ) {
//      final Collection<String> toReplaceSet = new HashSet<>();
//      for ( Map.Entry<String,String> uriBestRoot : uriBestRoots.entrySet() ) {
//         if ( uriBestRoot.getValue().equals( toReplace ) ) {
//            toReplaceSet.add( uriBestRoot.getKey() );
//         }
//      }
//      toReplaceSet.forEach( u -> LOGGER.info( "Replacing " + u + " with " + bestRoot ) );
//      toReplaceSet.forEach( u -> uriBestRoots.put( u, bestRoot ) );
//   }
//
//   static private void setBestRoots( final String bestRoot,
//                                     final String toReplace1,
//                                     final String toReplace2,
//                                     final Map<String,String> uriBestRoots ) {
//      final Collection<String> toReplace = new HashSet<>();
//      for ( Map.Entry<String,String> uriBestRoot : uriBestRoots.entrySet() ) {
//         if ( uriBestRoot.getValue().equals( toReplace1 ) || uriBestRoot.getValue().equals( toReplace2 ) ) {
//            toReplace.add( uriBestRoot.getKey() );
//         }
//      }
//      toReplace.forEach( u -> uriBestRoots.put( u, bestRoot ) );
//   }

   static private void replaceWithBest( final String previousBest,
                                        final String newBest,
                                        final List<String> uris,
                                        final int startIndex,
                                        final int stopIndex,
                                        final Map<String, String> uriBestRootMap ) {
      final int max = Math.min( stopIndex, uris.size()-1 );
      for ( int k = startIndex; k<=max; k++ ) {
         final String kUri = uris.get( k );
         final String bestKuri = uriBestRootMap.get( kUri );
         if ( bestKuri.equals( previousBest ) ) {
            uriBestRootMap.put( kUri, newBest );
         }
      }
   }

   /**
    *
    * @param uris -
    * @return Map of each uri and the best existing parent leafs in its ancestry.
    * Occipital_Lobe : [Occipital_Lobe]
    * Parietal_Lobe : [Parietal_Lobe]
    * Brain : [Occipital_Lobe, Parietal_Lobe] --> Brain has 2 equal children that cannot subsume each other
    * Nervous_System : [Occipital_Lobe, Parietal_Lobe]
    */
   static public Map<String,Collection<String>> getUniqueChildren( final Collection<String> uris ) {
      if ( uris.size() == 1 ) {
         final String uri = new ArrayList<>( uris ).get( 0 );
         final Map<String,Collection<String>> singleMap = new HashMap<>( 1 );
         singleMap.put( uri, Collections.singletonList( uri ) );
         return singleMap;
      }
      final Map<String, Collection<String>> uriRoots
            = uris.stream()
                  .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getRootUris ) );
      return getUniqueChildren( uriRoots );
   }


   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
   static private Map<String,Collection<String>> getUniqueChildren( final Map<String, Collection<String>> uriRoots ) {
      final List<String> uris = new ArrayList<>( uriRoots.keySet() );
      // We don't want to mess up modifiers with things like "Left" -> "Left_Arm"
//      final Collection<String> modifiers = new ArrayList<>( Neo4jOntologyConceptUtil.getBranchUris( BODY_MODIFIER ) );
      final Collection<String> modifiers = new ArrayList<>();
      modifiers.addAll( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.LATERALITY ) );
      modifiers.add( UriConstants.MASS );
      modifiers.add( UriConstants.DISEASE );
      modifiers.add( UriConstants.NEOPLASM );
//      modifiers.add( UriConstants.NEOPLASTIC_DISEASE );
      modifiers.add( UriConstants.MALIGNANT_NEOPLASM );
      final List<String> modifierUris = uris.stream().filter( modifiers::contains ).collect( Collectors.toList() );
      uris.removeAll( modifierUris );
      // Create map seeded with each uri as best of itself
      final Map<String, Collection<String>> uniqueChildren = getUniqueChildren( uris, uriRoots );
      // put modifiers back in
      modifierUris.forEach( u -> uniqueChildren.computeIfAbsent( u, Collections::singletonList ) );
      return uniqueChildren;
   }

   /**
    *
    * @param uris -
    * @return Map of each uri and the best existing parent leafs in its ancestry.
    * Occipital_Lobe : [Occipital_Lobe]
    * Parietal_Lobe : [Parietal_Lobe]
    * Brain : [Occipital_Lobe, Parietal_Lobe] --> Brain has 2 equal children that cannot subsume each other
    * Nervous_System : [Occipital_Lobe, Parietal_Lobe]
    */
   static public Map<String,Collection<String>> getAllUniqueChildren( final Collection<String> uris ) {
      if ( uris.size() == 1 ) {
         final String uri = new ArrayList<>( uris ).get( 0 );
         final Map<String,Collection<String>> singleMap = new HashMap<>( 1 );
         singleMap.put( uri, Collections.singletonList( uri ) );
         return singleMap;
      }
      final Map<String, Collection<String>> uriRoots
            = uris.stream()
                  .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getRootUris ) );
      return getAllUniqueChildren( uriRoots );
   }


   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
   static private Map<String,Collection<String>> getAllUniqueChildren( final Map<String, Collection<String>> uriRoots ) {
      final List<String> uris = new ArrayList<>( uriRoots.keySet() );
      uris.sort( String.CASE_INSENSITIVE_ORDER );
      // Create map seeded with each uri as best of itself
      return getUniqueChildren( uris, uriRoots );
   }


   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
   static private Map<String,Collection<String>> getUniqueChildren( final List<String> uris, final Map<String, Collection<String>> uriRoots ) {
      // Create map seeded with each uri as best of itself
      final Map<String, Collection<String>> uniqueChildren = new HashMap<>( uris.size() );
      uris.forEach( u -> uniqueChildren.computeIfAbsent( u, l -> new ArrayList<>() ).add( u ) );

      for ( int i=0; i<uris.size()-1; i++ ) {
         final String iUri = uris.get( i );     // e.g. Brain
         final Collection<String> iLeafs = uniqueChildren.computeIfAbsent( iUri, u -> new HashSet<>() );  // e.g. Parietal_Lobe
         Collection<String> iRoots = uriRoots.get( iUri );    //  e.g. Body_Part > Anatomy
         for ( int j = i + 1; j < uris.size(); j++ ) {
            final String jUri = uris.get( j );  // e.g. Occipital_Lobe
            final Collection<String> jLeafs = uniqueChildren.computeIfAbsent( jUri, u -> new HashSet<>() );  // e.g. empty
            // check for new j subsumes older i
            final boolean jSubsumesI = subsumeParents( jUri, iLeafs, uriRoots );
            if ( !jSubsumesI ) {
               // check for older i subsumes newer j
               subsumeParents( iUri, iRoots, jLeafs, uriRoots );
            }
         }
      }
      return uniqueChildren;
   }


   static private String getBetterChild( final String uri1,
                                         final String uri2,
                                         final Map<String,Collection<String>> uriRoots ) {
      if ( uri1.equals( uri2 ) ) {
         return null;
      }
      return getBetterChild( uri1, uri2, uriRoots.get( uri1 ), uriRoots.get( uri2 ) );
   }

   static private String getBetterChild( final String uri1,
                                         final String uri2,
                                         final Collection<String> uri1Roots,
                                         final Collection<String> uri2Roots ) {
      if ( uri1.equals( uri2 ) ) {
         return null;
      }
      if ( uri1Roots.contains( uri2 ) ) {
         // The uri2 is a parent of the uri1.  Uri1 is the leaf.
         return uri1;
      } else if ( uri2Roots.contains( uri1 ) ) {
         // The uri1 is a parent of the uri2.  Uri2 is the leaf.
         return uri2;
      }
      return getCloseUriLeaf( uri1, uri2 );
   }


   static private boolean subsumeParents( final String subsumer,
                                          final Collection<String> leafs,
                                          final Map<String,Collection<String>> uriRoots ) {
      final Collection<String> subsumerRoots = uriRoots.get( subsumer );
      return subsumeParents( subsumer, subsumerRoots, leafs, uriRoots );
   }


   static private boolean subsumeParents( final String subsumer,
                                          final Collection<String> subsumerRoots,
                                          final Collection<String> leafs,
                                          final Map<String,Collection<String>> uriRoots ) {

      final Collection<String> removalLeafs = new ArrayList<>();
      for ( String leaf : leafs ) {
         if ( subsumer.equals( leaf ) ) {
            // uri is already in leaf set.  Assume that this subsumption has already been done.
            return false;
         }
         final String betterLeaf = getBetterChild( subsumer, leaf, subsumerRoots, uriRoots.get( leaf ) );
         if ( betterLeaf != null && betterLeaf.equals( subsumer ) ) {
            // Maintain that insert = true, subsume this leaf.
            removalLeafs.add( leaf );
         } else if ( betterLeaf != null && betterLeaf.equals( leaf ) ) {
            // A leaf is more specific than the subsumer.  Assume that better subsumption has already been done.
            return false;
         }
      }
      if ( removalLeafs.isEmpty() ) {
         return false;
      }
      leafs.removeAll( removalLeafs );
      leafs.add( subsumer );
      return true;
   }


   /**
    * Gets a "best leaf" uri.  For example, when given "left breast", "breast"
    * it will return the common leaf "left breast".
    * This assumes that the uris are in a common branch.
    * Something like "right breast" "left breast" "breast" would result in the return of "right breast"
    *
    * @param allUris -
    * @return a uri that is the most "specific"
    */
   static public String getMostSpecificUri( final Collection<String> allUris ) {
      if ( allUris.isEmpty() ) {
         return "";
      }
      if ( allUris.size() == 1 ) {
         return new ArrayList<>( allUris ).get( 0 );
      }
      final Map<Integer,List<String>> levelMap
            = allUris.stream()
                     .collect( Collectors.groupingBy( Neo4jOntologyConceptUtil::getClassLevel ) );
      final Integer highest = levelMap.keySet()
                                     .stream()
                                     .max( Comparator.comparingInt( l -> l ) )
                                     .orElse( -1 );
      final List<String> highestUris = highest > 0 ? levelMap.get( highest ) : new ArrayList<>( allUris );
      if ( highestUris.size() == 1 ) {
//         LOGGER.info( "Highest Level URI " + highestUris.get( 0 ) + " " + highest +  " of " + String.join( ",", allUris ) );
         return highestUris.get( 0 );
      }

      final Map<String, Collection<String>> rootMap
//            = allUris.stream()
            = highestUris.stream()
                     .distinct()
                     .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getRootUris ) );
      String firstUri = "";
      String bestCloseUri = "";
      String bestCountUri = "";
      String bestLengthUri = "";
      int bestCount = 0;
//      for ( String uri : allUris ) {
      for ( String uri : highestUris ) {
         int count = rootMap.get( uri ).size();
         if ( bestCloseUri.isEmpty() ) {
            firstUri = uri;
            bestCloseUri = uri;
            bestCountUri = uri;
            bestCount = count;
            bestLengthUri = uri;
            continue;
         }
         final String betterChild = getBetterChild( uri, bestCloseUri, rootMap );
         if ( betterChild != null && betterChild.equals( uri ) ) {
            bestCloseUri = uri;
         }
         if ( count > bestCount ) {
            bestCountUri = uri;
            bestCount = count;
         }
         if ( uri.length() > bestLengthUri.length() ) {
            // Not exactly brillant, but assume that something with a longer name is more specific
            bestLengthUri = uri;
         }
      }
      if ( !firstUri.equals( bestCloseUri ) ) {
//         LOGGER.info( "Highest Level Best Close URI " + bestCloseUri + " from " + String.join( ",", highestUris ) + " " + highest + " of " + String.join( ",", allUris ) );
         return bestCloseUri;
      } else if ( !firstUri.equals( bestCountUri ) ) {
//         LOGGER.info( "Highest Level Best Count URI " + bestCloseUri + " from " + String.join( ",", highestUris ) + " " + highest + " of " + String.join( ",", allUris ) );
         return bestCountUri;
      }
//      LOGGER.info( "Highest Level Best Length URI " + bestCloseUri + " from " + String.join( ",", highestUris ) + " " + highest + " of " + String.join( ",", allUris ) );
      return bestLengthUri;
   }


   static public String getShortestRootUri( final Collection<String> allUris ) {
      if ( allUris.isEmpty() ) {
         return "";
      }
      if ( allUris.size() == 1 ) {
         return new ArrayList<>( allUris ).get( 0 );
      }
      final Map<Integer,List<String>> levelMap
            = allUris.stream()
                     .collect( Collectors.groupingBy( Neo4jOntologyConceptUtil::getClassLevel ) );
      final Integer lowest = levelMap.keySet()
                                     .stream()
                                     .min( Comparator.comparingInt( l -> l ) )
                                     .orElse( -1 );
      final List<String> lowestUris = lowest > 0 ? levelMap.get( lowest ) : new ArrayList<>( allUris );
      if ( lowestUris.size() == 1 ) {
//         LOGGER.info( "Lowest Level URI " + lowestUris.get( 0 ) + " " + lowest + " of " + String.join( ",", allUris ) );
         return lowestUris.get( 0 );
      }

      long leastLength = Integer.MAX_VALUE;
      String bestUri = "";
//      for ( String uri : allUris ) {
      for ( String uri : lowestUris ) {
         long length = Neo4jOntologyConceptUtil.getRootUris( uri ).size();
         if ( length < leastLength ) {
            leastLength = length;
            bestUri = uri;
         }
      }
//      LOGGER.info( "Lowest Level URI " + bestUri + " from " + String.join( ",", lowestUris ) + " " + lowest + " of " + String.join( ",", allUris ) );
      return bestUri;
   }


   static private final Map<String, String> HARDCODED_CLOSE_ENOUGH = new HashMap<>();

   static {
      // Synonyms hard coded for now until get more generalized solution using
      // ontology walking that will deal with these cases
      // Some are not synonyms, but "part_of" anatomicals that aren't handled well in the ontology.
      final String axilArea = "our Axillary_Lymph_Node area";
      final String breastArea = "our Breast area";
//      final String CHEST_AREA = "our Chest area";
//      final String UTERINE_AREA = "our Uterine area";
      final String OVARY_AREA = "our Ovary area";
//      final String ABDOMEN_AREA = "our Abdomen area";
//      final String COLON_AREA = "Our Colon area";
//      final String LEG_AREA = "Our Leg area";
      final String ENDO_ADENO = "Our Endometrial Adenocarcinoma";
      HARDCODED_CLOSE_ENOUGH.put( "Axilla", axilArea );
      HARDCODED_CLOSE_ENOUGH.put( "Axillary_Lymph_Node", axilArea );
      HARDCODED_CLOSE_ENOUGH.put( "Lymphatic_Vessel", axilArea );
      HARDCODED_CLOSE_ENOUGH.put( "Breast", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Left_Breast", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Right_Breast", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Nipple", breastArea );  Nipple now under Breast
//      HARDCODED_CLOSE_ENOUGH.put( "Mammary_Gland_Tissue", breastArea );  Removed
      HARDCODED_CLOSE_ENOUGH.put( "Duct", breastArea );   // TODO consider doing this one only for BrCa
//      HARDCODED_CLOSE_ENOUGH.put( "Lung", CHEST_AREA );   Lung now under Thorax
//      HARDCODED_CLOSE_ENOUGH.put( "Chest", CHEST_AREA );  "Chest" is now "Thorax"
//      HARDCODED_CLOSE_ENOUGH.put( "Uterus", UTERINE_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Cervix_Uteri", UTERINE_AREA );  Under Uterus
//      HARDCODED_CLOSE_ENOUGH.put( "Endocervix", UTERINE_AREA );  Under Uterus
      HARDCODED_CLOSE_ENOUGH.put( "Ovary", OVARY_AREA );
      HARDCODED_CLOSE_ENOUGH.put( "Fallopian_Tube", OVARY_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Abdomen", ABDOMEN_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Mesentery", ABDOMEN_AREA );   now under abdomen
//      HARDCODED_CLOSE_ENOUGH.put( "Omentum", ABDOMEN_AREA );    now under abdomen
//      HARDCODED_CLOSE_ENOUGH.put( "Peritoneal_Cavity", ABDOMEN_AREA );   now under abdomen
//      HARDCODED_CLOSE_ENOUGH.put( "Abdominal_Wall", ABDOMEN_AREA );   now under abdomen
//      HARDCODED_CLOSE_ENOUGH.put( "Inguinal_Region", ABDOMEN_AREA );  now under abdomen
//      HARDCODED_CLOSE_ENOUGH.put( "Large_Intestine", COLON_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Colon", COLON_AREA );  Colon now under lg intestine.
//      HARDCODED_CLOSE_ENOUGH.put( "Leg", LEG_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Thigh", LEG_AREA );  Thigh now under leg

      HARDCODED_CLOSE_ENOUGH.put( "Endometrial_Adenocarcinoma", ENDO_ADENO );
      HARDCODED_CLOSE_ENOUGH.put( "Endometrioid_Adenocarcinoma", ENDO_ADENO );
   }

   static public String getCloseUriLeaf( final String uri1, final String uri2 ) {
      final String close1 = getCloseUriLeaf1( uri1, uri2 );
      if ( close1 != null ) {
         return close1;
      }
      // Attempt match by part of
      return getCloseUriLeaf2( uri1, uri2,
            Neo4jOntologyConceptUtil.getRootUris( uri1 ),
            Neo4jOntologyConceptUtil.getRootUris( uri2 ) );
   }

   static public String getCloseUriLeaf( final String uri1, final String uri2, final Collection<String> roots1,
                                         final Collection<String> roots2 ) {
      final String close1 = getCloseUriLeaf1( uri1, uri2 );
      if ( close1 != null ) {
         return close1;
      }
      // Attempt match by part of
      return getCloseUriLeaf2( uri1, uri2, roots1, roots2 );
   }

   static private String getCloseUriLeaf1( final String uri1, final String uri2 ) {
      String lookup1 = HARDCODED_CLOSE_ENOUGH.get(uri1);
      if ( !uri1.equals( uri2 ) && lookup1 != null && lookup1.equals( HARDCODED_CLOSE_ENOUGH.get( uri2 ) ) ) {
         if ( uri1.length() > uri2.length() ) {
            return uri1;
         } else {
            return uri2;
         }
      }
      // Match "Sentinel" Lymph node
      if ( uri1.equals( "Sentinel_Lymph_Node" ) && uri2.endsWith( "Lymph_Node" ) ) {
         return uri2;
      } else if ( uri2.equals( "Sentinel_Lymph_Node" ) && uri1.endsWith( "Lymph_Node" ) ) {
         return uri1;
      }
//      if ( ( uri1.equals( "Chest" ) && uri2.equals( "Breast" ) ) || ( uri2.equals( "Chest" ) && uri1.equals( "Breast" ) ) ) {
//         return "Breast";
//      }
      return null;
   }

   static private String getCloseUriLeaf2( final String uri1, final String uri2, final Collection<String> roots1,
                                           final Collection<String> roots2 ) {
      // Attempt match by part of
      if ( roots1.contains( uri2 + "_Part" ) ) {
         return uri1;
      }
      if ( roots2.contains( uri1 + "_Part" ) ) {
         return uri2;
      }
      if ( roots1.contains( "Body_Part" ) && uri1.contains( uri2 ) ) {
         return uri1;
      }
      if ( roots2.contains( "Body_Part" ) && uri2.contains( uri1 ) ) {
         return uri2;
      }
      return null;
   }

   static public String getCloseUriRoot( final String uri1, final String uri2 ) {
      if  ( uri1.equals( uri2 ) ) {
         return null;
      }
      String lookup1 = HARDCODED_CLOSE_ENOUGH.get(uri1);
      if ( lookup1 != null && lookup1.equals( HARDCODED_CLOSE_ENOUGH.get( uri2 ) ) ) {
         if ( uri1.length() > uri2.length() ) {
            return uri2;
         } else {
            return uri1;
         }
      }
      // Match "Sentinel" Lymph node
      if ( uri1.equals( "Sentinel_Lymph_Node" ) && uri2.endsWith( "Lymph_Node" ) ) {
         return uri2;
      } else if ( uri2.equals( "Sentinel_Lymph_Node" ) && uri1.endsWith( "Lymph_Node" ) ) {
         return uri1;
      }
      if ( (uri1.equals( "Chest" ) && uri2.equals( "Breast" )) ||
           (uri2.equals( "Chest" ) && uri1.equals( "Breast" )) ) {
         return "Breast";
      }
      // Attempt match by part of
      final Collection<String> maybeRoots1 = Neo4jOntologyConceptUtil.getRootUris( uri1 );
      if ( maybeRoots1.contains( uri2 + "_Part" ) ) {
         return uri2;
      }
      final Collection<String> maybeRoots2 = Neo4jOntologyConceptUtil.getRootUris( uri2 );
      if ( maybeRoots2.contains( uri1 + "_Part" ) ) {
         return uri1;
      }
      if ( maybeRoots1.contains( "Body_Part" ) && uri1.contains( uri2 ) ) {
         return uri2;
      }
      if ( maybeRoots2.contains( "Body_Part" ) && uri2.contains( uri1 ) ) {
         return uri1;
      }
      return null;
   }

   static public boolean isBilaterality( final String uri1, final String uri2 ) {
      if ( uri1.isEmpty() || uri2.isEmpty() ) {
         return false;
      }
      if ( uri1.equals( uri2 ) ) {
         return true;
      }
      return uri1.equals( "Bilateral" ) || uri2.equals( "Bilateral" );
   }

   static public String getExtension( final String url ) {
      final int hashIndex = url.indexOf( '#' );
      if ( hashIndex >= 0 && hashIndex < url.length() - 1 ) {
         return url.substring( hashIndex + 1 );
      }
      return url;
   }



}
