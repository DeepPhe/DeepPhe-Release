package org.healthnlp.deepphe.nlp.uri;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.util.RelatedUris;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.UriConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {11/12/2023}
 */
final public class UriInfoUtil {

   static private final Logger LOGGER = Logger.getLogger( "UriInfoUtil" );


   private UriInfoUtil() {}

   /**
    * \
    * @param sourceUri source of all possible relations
    * @param targetUri target to check for all possible relation
    * @return Map of relation name to strength of relation for all valid relations between source and target.
    */
   static public Map<String, Double> getRelationScores( final String sourceUri, final String targetUri ) {
//      LogFileWriter.add( "UriInfoUtil.getRelationScores for " + sourceUri + " to " + targetUri );
      final RelatedUris relatedUris = UriInfoCache.getInstance().getRelatedGraphUris( sourceUri );
      if ( relatedUris.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<String,Integer> relatedCounts = new HashMap<>();
      final Collection<String> targetRoots =  UriInfoCache.getInstance().getUriRoots( targetUri );
      for ( Map.Entry<String,Collection<String>> relatedTargets : relatedUris.getRelationTargets().entrySet() ) {
         int count = 0;
         for ( String relatedTarget : relatedTargets.getValue() ) {
            if ( targetRoots.contains( relatedTarget ) ) {
               count++;
            }
         }
         if ( count > 0 ) {
            relatedCounts.put( relatedTargets.getKey(), count );
         }
      }
      if ( relatedCounts.isEmpty() ) {
         return Collections.emptyMap();
      }
      return getRelationScores( relatedCounts );
   }

   /**
    * The count of ancestors of some relation target related to some source * 40, max 100.
    * @param relatedCounts Map of relation names to the counts of some target's parent uri having that relation
    * @return Map of relation names to scores for some target having that relation to some source
    */
   static private Map<String, Double> getRelationScores( final Map<String,Integer> relatedCounts ) {
      final Map<String,Double> relationScores = new HashMap<>();
      relatedCounts.forEach( (k,v) -> relationScores.put( k, Math.min( 100d, 40*v ) ) );
      return relationScores;
   }



      /**
       *
       * @param uris - should already be associated by dphe group
       * @return Map of each "best" uri and the existing other uris that are its roots
       */
   static public Map<String,Collection<String>> getAssociatedUriMap_1( final Collection<String> uris ) {
      if ( uris.size() == 1 ) {
         final Map<String,Collection<String>> map = new HashMap<>( 1 );
         uris.forEach( u -> map.put( u, new HashSet<>( uris ) ) );
         return map;
      }
      // Join all uris that fall within a root tree
      final Map<String,String> bestRoots = getBestRoots_1( uris );
      LogFileWriter.add( "UriInfoUtil getAssociatedUriMap bestRoots "
            + bestRoots.entrySet().stream().map( e -> e.getKey() + " : " + e.getValue() )
                       .collect( Collectors.joining(" ; ")) );
      final Map<String,Collection<String>> rootChildrenMap = new HashMap<>();
      for ( Map.Entry<String,String> bestRoot : bestRoots.entrySet() ) {
         // fill the map with each "best" root uri to a list of all leaf uris for which it is best.
         rootChildrenMap.computeIfAbsent( bestRoot.getValue(), u -> new ArrayList<>() ).add( bestRoot.getKey() );
      }
      LogFileWriter.add( "UriInfoUtil getAssociatedUriMap rootChildrenMap "
            + rootChildrenMap.entrySet().stream().map( e -> e.getKey() + " : " + String.join( " , ", e.getValue() ) )
                       .collect( Collectors.joining(" ; ")) );
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
      LogFileWriter.add( "UriInfoUtil getAssociatedUriMap bestAssociations\n   "
            + bestAssociations.entrySet().stream().map( e -> e.getKey() + " : " + String.join( " , ", e.getValue() ) )
                              .collect( Collectors.joining("\n   ")) );
      return bestAssociations;
   }


   static private Map<String,String> getBestRoots_1( final Collection<String> uris ) {
      final Map<String,Collection<String>> uriBranches
            = uris.stream()
                  .collect( Collectors.toMap( Function.identity(), UriInfoCache.getInstance()::getUriBranch ) );
      return getBestRoots_1( uriBranches );
   }

   static private final Collection<String> GROUP_ROOTS = new HashSet<>( Arrays.asList(
         LATERALITY_XN, MASS, LESION, NEOPLASM, DISEASE_XN, MEDICATION_XN, REGIMEN_XN,
         PROCEDURE_XN, BODY_FLUID_XN, TISSUE, TEST_RESULT,
         "Dose", "Severity", "Status", "Susceptibility", "PathologicProcess", "Behavior",
         "BenignNeoplasm", "MalignantNeoplasm", "Non_MalignantNeoplasm", "RecurrentNeoplasm", "RefractoryNeoplasm"
   ));

   static private Map<String,String> getBestRoots_1( final Map<String, Collection<String>> uriBranches ) {
      final List<String> uris = new ArrayList<>( uriBranches.keySet() );
      // Don't allow the most generic uris to be roots.
      final List<String> groupUris = uris.stream().filter( GROUP_ROOTS::contains ).collect( Collectors.toList() );
      uris.removeAll( groupUris );
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
            LogFileWriter.add( i + "," + j + " getBestRoots i " + iUri + " / " + bestIuri + " : " + String.join( " ",iBranch )
                  + "\n             j " + jUri + " / " + bestJuri + " : " + String.join(" ",jBranch ) );
            if ( jBranch.size() > iBranch.size() && jBranch.contains( bestIuri ) ) {
               LogFileWriter.add( i + "," + j + "    jBranch contains bestIuri " + bestIuri + " bestI is now " + bestJuri );
               replaceWithBest( bestIuri, bestJuri, uris, 0, i, uriBestRootMap );
               bestIuri = bestJuri;
               iBranch = jBranch;
            } else if ( iBranch.size() > jBranch.size() && iBranch.contains( bestJuri ) ) {
               LogFileWriter.add( i + "," + j + "    iBranch contains bestJuri " + bestJuri );
               replaceWithBest( bestJuri, bestIuri, uris, j, uris.size(), uriBestRootMap );
            } else if ( jBranch.size() > iBranch.size() && jBranch.contains( iUri ) ) {
               // Problem with using bestIuri only when we found 2 conflicting roots, and the first one used is shorter and less inclusive.
               // Should we be gathering a map of iUri to ALL best roots?  Then try to match everything with a common root?
               LogFileWriter.add( i + "," + j + "    longer jBranch contains iUri " + iUri + " should we be swapping?" );
            } else if ( iBranch.size() > jBranch.size() && iBranch.contains( jUri ) ) {
               LogFileWriter.add( i + "," + j + "    longer iBranch contains jUri " + jUri + " should we be swapping?" );
            }
         }
      }
      // put modifiers back in
      groupUris.forEach( u -> uriBestRootMap.put( u, u ) );
//      LOGGER.info( "Best root for each uri: " );
//      uriBestRootMap.forEach( (k,v) -> LOGGER.info( k + " : " + v ) );
      return uriBestRootMap;
   }


   static private Map<String,String> getBestRoots_2( final Map<String, Collection<String>> uriBranches ) {
      final List<String> uris = new ArrayList<>( uriBranches.keySet() );
      // Don't allow the most generic uris to be roots.
      final List<String> groupUris = uris.stream().filter( GROUP_ROOTS::contains ).collect( Collectors.toList() );
      uris.removeAll( groupUris );
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
            LogFileWriter.add( i + "," + j + " getBestRoots i " + iUri + " / " + bestIuri + " : " + String.join( " ",iBranch )
                  + "\n             j " + jUri + " / " + bestJuri + " : " + String.join(" ",jBranch ) );
            if ( jBranch.size() > iBranch.size() && jBranch.contains( iUri ) ) {
               LogFileWriter.add( i + "," + j + "    jBranch " + jUri + " / " + bestJuri + " contains iUri " + iUri + " bestI is now " + bestJuri );
               replaceWithBest( bestIuri, bestJuri, uris, 0, i, uriBestRootMap );
               bestIuri = bestJuri;
               iBranch = jBranch;
            } else if ( iBranch.size() > jBranch.size() && iBranch.contains( jUri ) ) {
               LogFileWriter.add( i + "," + j + "    iBranch " + iUri + " / " + bestIuri + " contains jUri " + jUri );
               replaceWithBest( bestJuri, bestIuri, uris, j, uris.size(), uriBestRootMap );
//            } else if ( jBranch.size() > iBranch.size() && jBranch.contains( iUri ) ) {
//               // Problem with using bestIuri only when we found 2 conflicting roots, and the first one used is shorter and less inclusive.
//               // Should we be gathering a map of iUri to ALL best roots?  Then try to match everything with a common root?
//               LogFileWriter.add( i + "," + j + "    longer jBranch contains iUri " + iUri + " should we be swapping?" );
//            } else if ( iBranch.size() > jBranch.size() && iBranch.contains( jUri ) ) {
//               LogFileWriter.add( i + "," + j + "    longer iBranch contains jUri " + jUri + " should we be swapping?" );
            }
         }
      }
      // put modifiers back in
      groupUris.forEach( u -> uriBestRootMap.put( u, u ) );
//      LOGGER.info( "Best root for each uri: " );
//      uriBestRootMap.forEach( (k,v) -> LOGGER.info( k + " : " + v ) );
      return uriBestRootMap;
   }






   /**
    *
    * @param uris - should already be associated by dphe group
    * @return Map of each "best" uri and the existing other uris that are its roots
    */
   static public Map<String,Collection<String>> getAssociatedUriMap( final Collection<String> uris ) {
      if ( uris.size() == 1 ) {
         final Map<String,Collection<String>> map = new HashMap<>( 1 );
         uris.forEach( u -> map.put( u, new HashSet<>( uris ) ) );
         return map;
      }
//             * Occipital_Lobe : [Brain, Head_Cavity]  <- where Head_Cavity does not enclose Brain
//             * Parietal_Lobe : [Brain, Head_Cavity]
//      final Map<String,Collection<String>> bestRootsMap = getBestRootsMap( uris );
      final Map<String,Collection<String>> allRootsMap = getAllRootsMap( uris );
      final Collection<String> usedRoots = new HashSet<>();
      for ( Map.Entry<String,Collection<String>> allRoots :allRootsMap.entrySet() ) {
         final Collection<String> roots = new HashSet<>( allRoots.getValue() );
         roots.remove( allRoots.getKey() );
         usedRoots.addAll( roots );
      }
      allRootsMap.keySet().removeAll( usedRoots );
      LogFileWriter.add( "Chain for each uri: " );
      allRootsMap.entrySet().stream().map( e -> "   " + e.getKey() + " : " + String.join( " , ", e.getValue() ) )
               .forEach( LogFileWriter::add );
      return allRootsMap;
//
//
//
//
//
//
////             * Occipital_Lobe : Brain
////             * Parietal_Lobe : Brain
//      final Map<String,String> singleRootMap = getSingleRootMap( uris, bestRootsMap );
////             * Occipital_Lobe : [Brain, Occipital_Lobe]
////             * Parietal_Lobe : [Brain, Parietal_Lobe]
////             * Head_Cavity : [Head_Cavity]
//      return getUriChains( uris, singleRootMap );
   }

   static private Map<String,Collection<String>> getAllRootsMap( final Collection<String> uris ) {
      // Don't allow the most generic uris to be roots.
//      final List<String> uriList = uris.stream().filter( u -> !GROUP_ROOTS.contains( u ) )
//                                       .sorted().collect( Collectors.toList() );
      final Map<String,Collection<String>> allRootsMap = new HashMap<>();
      for ( String uri : uris ) {
         final Collection<String> roots = new HashSet<>( UriInfoCache.getInstance().getUriRoots( uri ) );
         roots.retainAll( uris );
         allRootsMap.put( uri, roots );
      }
      LogFileWriter.add( "All roots for each uri: " );
      allRootsMap.entrySet().stream().map( e -> "   " + e.getKey() + " : " + String.join( " , ", e.getValue() ) )
                  .forEach( LogFileWriter::add );
      return allRootsMap;
   }



   /**
    * @param uris uris of interest.
    * @return Map of Uris to their best 'root' uris in the same list.  Uris without 'roots' are NOT in this map.
    */
   static private Map<String,Collection<String>> getBestRootsMap( final Collection<String> uris ) {
      // Don't allow the most generic uris to be roots.
      final List<String> uriList = uris.stream().filter( u -> !GROUP_ROOTS.contains( u ) )
                                       .sorted().collect( Collectors.toList() );
      // Create map seeded with each uri as best of itself
      final Map<String,Collection<String>> bestRootsMap = new HashMap<>();
      for ( int i=0; i<uriList.size()-1; i++ ) {
         final String iUri = uriList.get( i );
         final Collection<String> iBranch = UriInfoCache.getInstance().getUriBranch( iUri );
         for ( int j = i + 1; j < uriList.size(); j++ ) {
            final String jUri = uriList.get( j );
            final Collection<String> jBranch = UriInfoCache.getInstance().getUriBranch( jUri );
            if ( jBranch.size() > iBranch.size() && jBranch.contains( iUri ) ) {
               bestRootsMap.put( iUri, fillBestRoots( iUri, jUri, jBranch, bestRootsMap ) );
            } else if ( iBranch.size() > jBranch.size() && iBranch.contains( jUri ) ) {
               bestRootsMap.put( jUri, fillBestRoots( jUri, iUri, iBranch, bestRootsMap ) );
            }
         }
      }
      LogFileWriter.add( "Best roots for each uri: " );
      bestRootsMap.entrySet().stream().map( e -> "   " + e.getKey() + " : " + String.join( " , ", e.getValue() ) )
                  .forEach( LogFileWriter::add );
      return bestRootsMap;
   }


   static private Collection<String> fillBestRoots( final String uri, final String newBestRoot,
                                                    final Collection<String> newBestBranch,
                                                    final Map<String,Collection<String>> bestRootsMap ) {
      final Collection<String> bestUris = bestRootsMap.getOrDefault( uri, new HashSet<>() );
      if ( bestUris.contains( newBestRoot ) ) {
         return bestUris;
      }
      if ( bestUris.isEmpty() ) {
         bestUris.add( newBestRoot );
         return bestUris;
      }
      // Make sure new best uri is better than existing best uris.  Check existing best uris for subsumption.
      final Collection<String> removals = new HashSet<>();
      for ( String bestUri : bestUris ) {
         final Collection<String> bestBranch = UriInfoCache.getInstance().getUriBranch( bestUri );
         if ( bestBranch.size() > newBestBranch.size() && bestBranch.contains( bestUri ) ) {
            // An existing best branch is better than the new possible best branch.
            return bestUris;
         }
         if ( newBestBranch.size() > bestBranch.size() && newBestBranch.contains( bestUri ) ) {
            removals.add( bestUri );
         }
      }
      // new best uri is indeed a new best uri.
      bestUris.add( newBestRoot );
      // remove any old best uris that the new best uri subsumed.
      bestUris.removeAll( removals );
      return bestUris;
   }

   static private Map<String,String> getSingleRootMap( final Collection<String> uris,
                                                       final Map<String,Collection<String>> bestRootsMap ) {
      final Map<String,Collection<String>> rootChildrenMap = new HashMap<>();
      for ( Map.Entry<String,Collection<String>> bestRoots : bestRootsMap.entrySet() ) {
         for ( String root : bestRoots.getValue() ) {
            rootChildrenMap.computeIfAbsent( root, r -> new HashSet<>() ).add( bestRoots.getKey() );
         }
      }
      final Map<String,String> singleRootMap = new HashMap<>();
      for ( Map.Entry<String,Collection<String>> bestRoots : bestRootsMap.entrySet() ) {
         if ( bestRoots.getValue().isEmpty() ) {
            LOGGER.error( "No roots for " + bestRoots.getKey() );
            singleRootMap.put( bestRoots.getKey(), bestRoots.getKey() );
         } else if ( bestRoots.getValue().size() == 1 ) {
            singleRootMap.put( bestRoots.getKey(), bestRoots.getValue().stream().findFirst().get() );
         } else {
            final RootComparator rootComparator = new RootComparator( rootChildrenMap );
            singleRootMap.put( bestRoots.getKey(),  bestRoots.getValue().stream()
                                                                .max( rootComparator ).get() );
         }
      }
      LogFileWriter.add( "Best single root for each uri: " );
      singleRootMap.forEach( (k,v) -> LogFileWriter.add( "   " + k + " : " + v ) );
      return singleRootMap;
   }


   static private class RootComparator implements Comparator<String> {
      private final Map<String,Collection<String>> _rootChildrenMap;
      private RootComparator( final Map<String,Collection<String>> rootChildrenMap ) {
         _rootChildrenMap = rootChildrenMap;
      }
      public int compare( final String root1, final String root2 ) {
         final int size1 = _rootChildrenMap.get( root1 ).size();
         final int size2 = _rootChildrenMap.get( root2 ).size();
         if ( size1 == size2 ) {
            // compare by root length
            final int roots1 = UriInfoCache.getInstance().getUriRoots( root1 ).size();
            final int roots2 = UriInfoCache.getInstance().getUriRoots( root2 ).size();
            if ( roots1 != roots2 ) {
               // longer length preferred (ICDO).  length1 (20) - length2 (10) = 10 ~ 1 is higher.
               return roots1 - roots2;
            }
            // compare by branch length
            final int length1 = UriInfoCache.getInstance().getUriBranch( root1 ).size();
            final int length2 = UriInfoCache.getInstance().getUriBranch( root2 ).size();
            if ( length1 != length2 ) {
               // shorter length preferred (less refinement).  length2 (20) - length1 (10) = 10 ~ 1 is higher.
               return length2 - length1;
            }
         }
         if ( size1 != 1 && size2 != 1 ) {
            // fewer children preferred.  children2 (20) - children1 (10) = 10 ~ 1 is higher.
            return size2 - size1;
         }
         // One (or both) of the child counts is 1.  Prefer the other.  size1 (1) - size2 (2) = -1 ~ 1 is lower.
         return size1 - size2;
      }
   }

   static private Map<String,Collection<String>> getUriChains( final Collection<String> uris,
                                                               final Map<String,String> singleRootMap ) {
      final Collection<String> usedInChain = new HashSet<>();
      final Map<String,Collection<String>> uriChains = new HashMap<>();
      for ( Map.Entry<String,String> bestRoots : singleRootMap.entrySet() ) {
         final Collection<String> chain = getChildren( bestRoots.getValue(), uris );
         chain.retainAll( UriInfoCache.getInstance().getUriRoots( bestRoots.getKey() ) );
         usedInChain.addAll( chain );
         chain.add( bestRoots.getKey() );
         uriChains.put( bestRoots.getKey(), chain );
      }
      uris.stream().filter( u -> !usedInChain.contains( u ) ).filter( u -> !uriChains.containsKey( u ) )
          .forEach( u -> uriChains.put( u, Collections.singletonList( u ) ) );
      uriChains.keySet().removeAll( usedInChain );
      LogFileWriter.add( "Chain for each uri: " );
      uriChains.entrySet().stream().map( e -> "   " + e.getKey() + " : " + String.join( " , ", e.getValue() ) )
                  .forEach( LogFileWriter::add );
      return uriChains;
   }

   static private Collection<String> getChildren( final String root, final Collection<String> uris ) {
      final Collection<String> branch = new HashSet<>( UriInfoCache.getInstance().getUriBranch( root ) );
      branch.retainAll( uris );
      branch.add( root );
      return branch;
   }


//   static private Map<String,Collection<String>> getBestToChainMap( final Collection<String> uris,
//                                                                    final Map<String,Collection<String>> bestRootsMap ) {
//      final Collection<String> usedInChain = new HashSet<>();
//      final Map<String,Collection<Collection<String>>> uriChains = new HashMap<>();
//      for ( Map.Entry<String,Collection<String>> bestRoots : bestRootsMap.entrySet() ) {
//         for ( String bestRoot : bestRoots.getValue() ) {
//            final Collection<String> chain = getChildren( bestRoot, uris );
//            usedInChain.addAll( chain );
//            chain.add( bestRoots.getKey() );
//            uriChains.computeIfAbsent( bestRoots.getKey(), u -> new HashSet<>() ).add( chain );
//         }
//      }
//      uriChains.keySet().removeAll( usedInChain );
//
//      final Map<String,Collection<String>> bestToChainMap = new HashMap<>();
//      for ( Collection<String> chain : rootToChainMap.values() ) {
//         final String best = getBestUri( chain );
//         bestToChainMap.put( best, chain );
//      }
//      return bestToChainMap;
//   }




   // Map of each uri to the uris above it in the graph.
   // ParietalLobe:[Brain,NervousSystem] , GyriBrevus:[Brain,NervousSystem]
   // --> Then we can use confidence of each leaf uri to decide the best grouping.
   // Iterate through, using reverse size of ancestors as the order.  e.g. u1[a,b,x,y] before u2[x,y,z]
   // For each sizing, use reverse confidence to aggregate.  Remove those aggregated as going.  Place in used list.
   static public Map<String,Collection<String>> getAssociatedUriMap( final Collection<String> uris,
                                                               final Map<String,Double> uriConfidences ) {
      final Map<String,Collection<String>> leafAncestorsMap = getLeafAncestors( uris );

      final Map<Integer,List<String>> countUris = leafAncestorsMap.keySet().stream()
              .collect( Collectors.groupingBy( u -> leafAncestorsMap.get(u).size() ) );
      final int maxCount = countUris.keySet().stream().mapToInt( d -> d ).max().orElse(0 );

      final Map<Double,Collection<String>> scoreUris = new HashMap<>();
      uris.forEach( u -> scoreUris.computeIfAbsent( uriConfidences.get( u ), c -> new HashSet<>() ).add( u ) );
      final Map<String, Collection<String>> uriUrisMap = new HashMap<>();
      final Collection<String> usedUris = new HashSet<>();
      for ( int i=maxCount; i>0; i-- ) {
         final List<String> urisAtCount = countUris.get( i );
         if  ( urisAtCount == null ) {
            continue;
         }
//         LOGGER.info( "Count = " + i + " , uris at count = " + urisAtCount.size() + " : "
//                 + String.join( ", ", urisAtCount ) );
         if ( urisAtCount.size() == 1 ) {
            final Collection<String> leafAncestors = leafAncestorsMap.get( urisAtCount.get(0) );
            if ( leafAncestors == null ) {
//               LOGGER.error( "No ancestors for " + urisAtCount.get(0) );
               continue;
            }
            final Collection<String> unusedAncestors = new HashSet<>( leafAncestors );
            unusedAncestors.removeAll( usedUris );
            uriUrisMap.put( urisAtCount.get(0), unusedAncestors );
//            LOGGER.info( "Uri with confidence " + uriConfidences.get( urisAtCount.get(0) )
//                    + " " + urisAtCount.get(0) + " : " + String.join( ", ", unusedAncestors ) );
            usedUris.add( urisAtCount.get(0) );
            usedUris.addAll( unusedAncestors );
            continue;
         }
         final List<Double> scores = urisAtCount.stream()
                 .map( uriConfidences::get )
                 .filter( Objects::nonNull )
                 .distinct()
                 .sorted( Comparator.reverseOrder() )
                 .collect( Collectors.toList() );
         for ( double score : scores ) {
            final Collection<String> urisWIthScore = scoreUris.get( score );
            if ( urisWIthScore == null ) {
               LOGGER.error( "No Uris at confidence " + score );
               continue;
            }
            final List<String> countUrisWithScore = urisWIthScore.stream()
                    .filter( urisAtCount::contains )
                    .sorted()
                    .collect( Collectors.toList() );
            // Sort for repeatability.
            for ( String uriWithScore : countUrisWithScore ) {
               if ( usedUris.contains( uriWithScore ) ) {
                  continue;
               }
               final Collection<String> leafAncestors = leafAncestorsMap.get( uriWithScore );
               if ( leafAncestors == null ) {
                  LOGGER.error( "No ancestors for " + uriWithScore );
                  continue;
               }
               final Collection<String> unusedAncestors = new HashSet<>( leafAncestors );
               unusedAncestors.removeAll( usedUris );
               uriUrisMap.put( uriWithScore, unusedAncestors );
//               LOGGER.info( "Uri with confidence " + score + " " + uriWithScore + " : " + String.join( ", ", unusedAncestors ) );
               usedUris.add( uriWithScore );
               usedUris.addAll( unusedAncestors );
            }
         }
      }
      return uriUrisMap;
   }


   static private Map<String,Collection<String>> getLeafAncestors( final Collection<String> uris ) {
      final List<String> sortedUris = uris.stream().sorted().collect( Collectors.toList() );
      final Map<String,Collection<String>> leafAncestorsMap = new HashMap<>( uris.size() );
      sortedUris.forEach( l -> leafAncestorsMap.computeIfAbsent( l, u -> new HashSet<>() ).add( l ) );
      for ( int i=0; i<sortedUris.size()-1; i++ ) {
         final String iUri = sortedUris.get( i );
         for ( int j = i + 1; j < sortedUris.size(); j++ ) {
            final String jUri = sortedUris.get( j );
            final int distance = UriInfoCache.getInstance().getUriDistance( iUri, jUri );
            if ( distance == Integer.MAX_VALUE ) {
               // Not in the same branch
               continue;
            }
            if ( distance > 0 ) {
               // iUri is an ancestor of jUri
               leafAncestorsMap.computeIfAbsent( jUri, u -> new HashSet<>() ).add( iUri );
//               LOGGER.info( jUri + " has ancestor " + iUri );
            } else if ( distance < 0 ) {
               leafAncestorsMap.computeIfAbsent( iUri, u -> new HashSet<>() ).add( jUri );
//               LOGGER.info( iUri + " has ancestor " + jUri );
            }
         }
      }
      return leafAncestorsMap;
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
   static private Map<String,Collection<String>> getUniqueChildren( final Collection<String> uris ) {
      if ( uris.size() == 1 ) {
         final String uri = new ArrayList<>( uris ).get( 0 );
         return Collections.singletonMap( uri, uris );
//         final Map<String,Collection<String>> singleMap = new HashMap<>( 1 );
//         singleMap.put( uri, Collections.singletonList( uri ) );
//         return singleMap;
      }
      final Map<String, Collection<String>> uriRoots
            = uris.stream()
                  .collect( Collectors.toMap( Function.identity(), UriInfoCache.getInstance()::getUriRoots ) );
      return getUniqueChildren( uriRoots );
   }


   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
   static private Map<String,Collection<String>> getUniqueChildren( final Map<String, Collection<String>> uriRoots ) {
      final List<String> uris = new ArrayList<>( uriRoots.keySet() );
      final List<String> groupUris = uris.stream().filter( GROUP_ROOTS::contains ).collect( Collectors.toList() );
      uris.removeAll( groupUris );
      // Create map seeded with each uri as best of itself
      final Map<String, Collection<String>> uniqueChildren = getUniqueChildren( uris, uriRoots );
      // put modifiers back in
      // TODO - Should this put them back with the original leafs that contained them (instead of by themselves)?
      groupUris.forEach( u -> uniqueChildren.computeIfAbsent( u, Collections::singletonList ) );
      return uniqueChildren;
   }




   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
   static private Map<String,Collection<String>> getUniqueChildren( final List<String> uris, final Map<String, Collection<String>> uriRoots ) {
      // Create map seeded with each uri as best of itself
      final Map<String, Collection<String>> uniqueChildren = new HashMap<>( uris.size() );
      uris.forEach( u -> uniqueChildren.computeIfAbsent( u, l -> new ArrayList<>() ).add( u ) );
      final Collection<String> subsumers = new HashSet<>();
      for ( int i=0; i<uris.size()-1; i++ ) {
         final String iUri = uris.get( i );     // e.g. Brain
         final Collection<String> iLeafs = uniqueChildren.computeIfAbsent( iUri, u -> new HashSet<>() );  // e.g. Parietal_Lobe
         Collection<String> iRoots = uriRoots.get( iUri );    //  e.g. Body_Part > Anatomy
         for ( int j = i + 1; j < uris.size(); j++ ) {
            final String jUri = uris.get( j );  // e.g. Occipital_Lobe
            final Collection<String> jLeafs = uniqueChildren.computeIfAbsent( jUri, u -> new HashSet<>() );  // e.g. empty
            LogFileWriter.add( i + "," + j + " getUniqueChildren i " + iUri + " : " + String.join( " , ",iLeafs )
                  + "\n     j " + jUri + " : " + String.join(" , ",jLeafs )  );
            // check for new j subsumes older i
            final boolean jSubsumesI = subsumeParents( jUri, iLeafs, uriRoots );
            if ( jSubsumesI ) {
               LogFileWriter.add( "   " + iUri + " is subsumed by " + jUri );
               subsumers.add( jUri );
            }
            if ( !jSubsumesI ) {
               // check for older i subsumes newer j
               if ( subsumeParents( iUri, iRoots, jLeafs, uriRoots ) ) {
                  LogFileWriter.add( "   " + jUri + " is subsumed by " + iUri );
                  subsumers.add( iUri );
               }
            }
         }
      }
      uniqueChildren.keySet().removeAll( subsumers );
      return uniqueChildren;
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
//      leafs.removeAll( removalLeafs );
      leafs.add( subsumer );
      return true;
   }


//   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
//   static private Map<String,Collection<String>> getUniqueChildren( final List<String> uris, final Map<String, Collection<String>> uriRoots ) {
//      // Create map seeded with each uri as best of itself
//      final Map<String, Collection<String>> uniqueChildren = new HashMap<>( uris.size() );
//      uris.forEach( u -> uniqueChildren.computeIfAbsent( u, l -> new ArrayList<>() ).add( u ) );
//      final Collection<String> subsumed = new HashSet<>();
//      for ( int i=0; i<uris.size()-1; i++ ) {
//         final String iUri = uris.get( i );     // e.g. Brain
//         final Collection<String> iLeafs = uniqueChildren.computeIfAbsent( iUri, u -> new HashSet<>() );  // e.g. Parietal_Lobe
//         Collection<String> iRoots = uriRoots.get( iUri );    //  e.g. Body_Part > Anatomy
//         for ( int j = i + 1; j < uris.size(); j++ ) {
//            final String jUri = uris.get( j );  // e.g. Occipital_Lobe
//            final Collection<String> jLeafs = uniqueChildren.computeIfAbsent( jUri, u -> new HashSet<>() );  // e.g. empty
//            LogFileWriter.add( i + "," + j + " getUniqueChildren i " + iUri + " : " + String.join( " , ",iLeafs )
//                  + "\n     j " + jUri + " : " + String.join(" , ",jLeafs )  );
//            // check for new j subsumes older i
//            final Collection<String> subsumedByJ = subsumeParents( jUri, iLeafs, uriRoots );
//            if ( !subsumedByJ.isEmpty() ) {
//               LogFileWriter.add( "   " + iUri + " is subsumed by " + jUri );
//               subsumed.addAll( subsumedByJ );
//            } else {
//               // check for older i subsumes newer j
//               final Collection<String> subsumedByI = subsumeParents( iUri, iRoots, jLeafs, uriRoots );
//               if ( !subsumedByI.isEmpty() ) {
//                  LogFileWriter.add( "   " + jUri + " is subsumed by " + iUri );
//                  subsumed.addAll( subsumedByI );
//               }
//            }
//         }
//      }
//      uniqueChildren.keySet().retainAll( subsumed );
//      return uniqueChildren;
//   }
//
//
//   static private Collection<String> subsumeParents( final String subsumer,
//                                          final Collection<String> leafs,
//                                          final Map<String,Collection<String>> uriRoots ) {
//      final Collection<String> subsumerRoots = uriRoots.get( subsumer );
//      return subsumeParents( subsumer, subsumerRoots, leafs, uriRoots );
//   }
//
//
//   static private Collection<String> subsumeParents( final String subsumer,
//                                          final Collection<String> subsumerRoots,
//                                          final Collection<String> leafs,
//                                          final Map<String,Collection<String>> uriRoots ) {
//
//      final Collection<String> removalLeafs = new ArrayList<>();
//      for ( String leaf : leafs ) {
//         if ( subsumer.equals( leaf ) ) {
//            // uri is already in leaf set.  Assume that this subsumption has already been done.
//            return Collections.emptyList();
//         }
//         final String betterLeaf = getBetterChild( subsumer, leaf, subsumerRoots, uriRoots.get( leaf ) );
//         if ( betterLeaf != null && betterLeaf.equals( subsumer ) ) {
//            // Maintain that insert = true, subsume this leaf.
//            removalLeafs.add( leaf );
//         } else if ( betterLeaf != null && betterLeaf.equals( leaf ) ) {
//            // A leaf is more specific than the subsumer.  Assume that better subsumption has already been done.
//            return Collections.emptyList();
//         }
//      }
//      if ( removalLeafs.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      leafs.add( subsumer );
//      return removalLeafs;
//   }







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
      return null;
   }


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
    * @param uri1 -
    * @param uri2 -
    * @return the vertical distance between the two uris.  -1 if they are not in the same branch.
    */
   static int getUriDistance( final String uri1, final String uri2 ) {
      if ( uri1.equals( uri2 ) ) {
         return 0;
      }
      final Collection<String> uri1Branch = UriInfoCache.getInstance().getUriBranch( uri1 );
      if ( uri1Branch.contains( uri2 ) ) {
         return getUriDistanceHiLo( uri1, uri2, uri1Branch, UriInfoCache.getInstance().getUriRoots( uri2 ) );
      }
      final Collection<String> uri2Branch = UriInfoCache.getInstance().getUriBranch( uri2 );
      if ( uri2Branch.contains( uri1 ) ) {
         return -1 * getUriDistanceHiLo( uri2, uri1, uri2Branch, UriInfoCache.getInstance().getUriRoots( uri1 ) );
      }
      return Integer.MAX_VALUE;
   }


   /**
    * The distance between 2 uris in the same branch is the path to root of the lower uri
    * that overlaps the branch of the higher uri.
    * This handles multiple paths to root.
    * pathToRootLo: [root,cancer,breast cancer, Ductal BC, recurrent ductal BC]
    * branchHi: [breast cancer, Ductal BC, recurrent ductal BC, .....]
    * betwixt BrCa and Recurrent Ductal BD = 1
    * @param uriHi uri higher in the tree
    * @param uriLo uri lower in the tree (under uriHi)
    * @param uriHiBranch -
    * @param uriLoRoots -
    * @return the vertical distance between the two uris.  MAX_VALUE if they are not in the same branch.
    */
   static private int getUriDistanceHiLo( final String uriHi, final String uriLo,
                                   final Collection<String> uriHiBranch,
                                   final Collection<String> uriLoRoots ) {
      final Collection<String> betwixt = new HashSet<>( uriHiBranch );
      betwixt.retainAll( uriLoRoots );
      if ( betwixt.isEmpty() ) {
         // The two uris are not related by isa relationship.
         return Integer.MAX_VALUE;
      }
      betwixt.remove( uriHi );
      betwixt.remove( uriLo );
      // add 1 since uris next to each other still have a distance.  Only the distance between a uri and itself is 0.
      return betwixt.size()+1;
   }


}
