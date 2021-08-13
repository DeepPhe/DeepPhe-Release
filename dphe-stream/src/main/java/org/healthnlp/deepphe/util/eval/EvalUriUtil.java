//package org.healthnlp.deepphe.util.eval;
//
//import jdk.nashorn.internal.ir.annotations.Immutable;
//import org.apache.log4j.Logger;
//import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
//import org.healthnlp.deepphe.neo4j.constant.UriConstants;
//
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import static org.healthnlp.deepphe.neo4j.constant.UriConstants.*;
//
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 1/23/2018
// */
//@Immutable
//final public class EvalUriUtil {
//
//   static private final Logger LOGGER = Logger.getLogger( "UriUtil" );
//
//   private EvalUriUtil() {
//   }
//
//   static public boolean containsUri( final String uri, Collection<String> toMatch ) {
//      return toMatch.contains( uri );
//   }
//
//   static public boolean containsUri( final String uri, final String... toMatch ) {
//      return Arrays.asList( toMatch ).contains( uri );
//   }
//
//
//   static public boolean isUriBranchMatch( final String uri1, final String uri2 ) {
//      return uri1.equals( uri2 ) || isUriBranchMatch( Collections.singletonList( uri1 ), Collections.singletonList( uri2 ) );
//   }
//
//   static public boolean isUriBranchMatch( final Collection<String> uriSet1, final Collection<String> uriSet2 ) {
//      final Map<String,Collection<String>> uri2BranchMap = new HashMap<>( uriSet2.size() );
//      for ( String uri1 : uriSet1 ) {
//         final Collection<String> branch1 = Neo4jOntologyConceptUtil.getBranchUris( uri1 );
//         for ( String uri2 : uriSet2 ) {
//            if ( isUriBranchMatch( uri1, branch1, uri2, uri2BranchMap ) ) {
//               return true;
//            }
//         }
//      }
//      return false;
//   }
//
//   static private boolean isUriBranchMatch( final String uri1, final Collection<String> uriBranch1,
//                                           final String uri2, final Map<String,Collection<String>> uri2BranchMap ) {
//      if ( uriBranch1.contains( uri2 ) ) {
//         return true;
//      }
//      final Collection<String> branch2
//            = uri2BranchMap.computeIfAbsent( uri2, u -> Neo4jOntologyConceptUtil.getBranchUris( uri2 ) );
//      return branch2.contains( uri1 );
//   }
//
//   static public boolean isUriInBranch( final String uri, final String rootUri ) {
//      return uri.equals( rootUri ) || isUriInBranch( uri, Neo4jOntologyConceptUtil.getBranchUris( rootUri ) );
//   }
//
//   static public boolean isUriInBranch( final String uri, final Collection<String> branch ) {
//      return branch.contains( uri );
//   }
//
//
//
//   static public Collection<Collection<String>> getAssociatedUris( final Collection<String> uris ) {
//      return getAssociatedUriMap( uris ).values();
//   }
//
//   /**
//    *
//    * @param uris -
//    * @return Map of each "best" uri and the existing other uris that are its roots
//    */
//   static public Map<String,Collection<String>> getAssociatedUriMap( final Collection<String> uris ) {
//      // Join all uris that fall within a root tree
//      final Map<String,String> bestRoots = getBestRoots( uris );
//      final Map<String,Collection<String>> rootChildrenMap = new HashMap<>();
//      for ( Map.Entry<String,String> bestRoot : bestRoots.entrySet() ) {
//         // fill the map of each "best" root uri to a list of all leaf uris for which it is best.
//         rootChildrenMap.computeIfAbsent( bestRoot.getValue(), u -> new ArrayList<>() ).add( bestRoot.getKey() );
//      }
//
//      final Map<String,Collection<String>> bestAssociations = new HashMap<>();
//      for ( Map.Entry<String,Collection<String>> rootLeafs : rootChildrenMap.entrySet() ) {
//         final Map<String, Collection<String>> uniqueChildren = getUniqueChildren( rootLeafs.getValue() );
////             * Occipital_Lobe : [Occipital_Lobe]
////             * Parietal_Lobe : [Parietal_Lobe]
////             * Brain : [Occipital_Lobe, Parietal_Lobe]
////             * Nervous_System : [Occipital_Lobe, Parietal_Lobe]
//         for ( Map.Entry<String,Collection<String>> rootBest : uniqueChildren.entrySet() ) {
//            rootBest.getValue().forEach( u -> bestAssociations.computeIfAbsent( u, l -> new HashSet<>() )
//                                                              .add( rootBest.getKey() ) );
//         }
//      }
////             * Occipital_Lobe : [Occipital_Lobe, Brain, Nervous_System]
////             * Parietal_Lobe : [Parietal_Lobe, Brain, Nervous_System]
//      return bestAssociations;
//   }
//
//
//   static public Map<String,String> getBestRoots( final Collection<String> uris ) {
//      final Map<String, Collection<String>> uriBranches
//            = uris.stream()
//                  .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getBranchUris ) );
//      return getBestRoots( uriBranches );
//   }
//
//
//   static private Map<String,String> getBestRoots( final Map<String, Collection<String>> uriBranches ) {
//      final List<String> uris = new ArrayList<>( uriBranches.keySet() );
//      // We don't want to mess up modifiers with things like "Left" -> "Left_Arm"
//      // TODO : BODY_MODIFIER does not exist
////      final Collection<String> modifiers = new ArrayList<>( Neo4jOntologyConceptUtil.getBranchUris( BODY_MODIFIER ) );
//      final Collection<String> modifiers = new HashSet<>();
//      modifiers.addAll( Neo4jOntologyConceptUtil.getBranchUris( LATERALITY ) );
//      modifiers.add( MASS );
//      modifiers.add( DISEASE );
//      modifiers.add( NEOPLASM );
////      modifiers.add( NEOPLASTIC_DISEASE );
//      modifiers.add( MALIGNANT_NEOPLASM );
//      final List<String> modifierUris = uris.stream().filter( modifiers::contains ).collect( Collectors.toList() );
//      uris.removeAll( modifierUris );
//      // Create map seeded with each uri as best of itself
//      final Map<String, String> uriBestRootMap
//            = uriBranches.keySet().stream()
//                         .collect( Collectors.toMap( Function.identity(), Function.identity() ) );
//      for ( int i=0; i<uris.size()-1; i++ ) {
//         final String iUri = uris.get( i );
//         // A better iUri may have already been set when it was a j
//         String bestIuri = uriBestRootMap.get( iUri );
//         Collection<String> iBranch = uriBranches.get( bestIuri );
//         for ( int j = i + 1; j < uris.size(); j++ ) {
//            final String jUri = uris.get( j );
//            // A better jUri may have already been set when previously compared to an i
//            final String bestJuri = uriBestRootMap.get( jUri );
//            Collection<String> jBranch = uriBranches.get( bestJuri );
//            if ( jBranch.size() > iBranch.size() && jBranch.contains( bestIuri ) ) {
//               replaceWithBest( bestIuri, bestJuri, uris, 0, i, uriBestRootMap );
//               bestIuri = bestJuri;
//               iBranch = jBranch;
//            } else if ( iBranch.size() > jBranch.size() && iBranch.contains( bestJuri ) ) {
//               replaceWithBest( bestJuri, bestIuri, uris, j, uris.size(), uriBestRootMap );
//            } else {
//               final String closeEnoughUri = getCloseUriRoot( bestIuri, bestJuri );
//               if ( closeEnoughUri != null ) {
//                  if ( bestJuri.equals( closeEnoughUri ) ) {
//                     replaceWithBest( bestIuri, bestJuri, uris, 0, i, uriBestRootMap );
//                     bestIuri = bestJuri;
//                     iBranch = jBranch;
//                  } else {
//                     replaceWithBest( bestJuri, bestIuri, uris, j, uris.size(), uriBestRootMap );
//                  }
//               }
//            }
//         }
//      }
//      // put modifiers back in
//      modifierUris.forEach( u -> uriBestRootMap.put( u, u ) );
//      return uriBestRootMap;
//   }
//
//   static private void replaceWithBest( final String previousBest,
//                                        final String newBest,
//                                        final List<String> uris,
//                                        final int startIndex,
//                                        final int stopIndex,
//                                        final Map<String, String> uriBestRootMap ) {
//      final int max = Math.min( stopIndex, uris.size()-1 );
//      for ( int k = startIndex; k<=max; k++ ) {
//         final String kUri = uris.get( k );
//         final String bestKuri = uriBestRootMap.get( kUri );
//         if ( bestKuri.equals( previousBest ) ) {
//            uriBestRootMap.put( kUri, newBest );
//         }
//      }
//   }
//
//   /**
//    *
//    * @param uris -
//    * @return Map of each uri and the best existing parent leafs in its ancestry.
//    * Occipital_Lobe : [Occipital_Lobe]
//    * Parietal_Lobe : [Parietal_Lobe]
//    * Brain : [Occipital_Lobe, Parietal_Lobe] --> Brain has 2 equal children that cannot subsume each other
//    * Nervous_System : [Occipital_Lobe, Parietal_Lobe]
//    */
//   static public Map<String,Collection<String>> getUniqueChildren( final Collection<String> uris ) {
//      if ( uris.size() == 1 ) {
//         final String uri = new ArrayList<>( uris ).get( 0 );
//         final Map<String,Collection<String>> singleMap = new HashMap<>( 1 );
//         singleMap.put( uri, Collections.singletonList( uri ) );
//         return singleMap;
//      }
//      final Map<String, Collection<String>> uriRoots
//            = uris.stream()
//                  .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getRootUris ) );
//      return getUniqueChildren( uriRoots );
//   }
//
//
//   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
//   static private Map<String,Collection<String>> getUniqueChildren( final Map<String, Collection<String>> uriRoots ) {
//      final List<String> uris = new ArrayList<>( uriRoots.keySet() );
//      // We don't want to mess up modifiers with things like "Left" -> "Left_Arm"
////      final Collection<String> modifiers = new ArrayList<>( Neo4jOntologyConceptUtil.getBranchUris( BODY_MODIFIER ) );
//      final Collection<String> modifiers = new ArrayList<>();
//      modifiers.addAll( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.LATERALITY ) );
//      modifiers.add( UriConstants.MASS );
//      modifiers.add( UriConstants.DISEASE );
//      modifiers.add( UriConstants.NEOPLASM );
////      modifiers.add( UriConstants.NEOPLASTIC_DISEASE );
//      modifiers.add( UriConstants.MALIGNANT_NEOPLASM );
//      final List<String> modifierUris = uris.stream().filter( modifiers::contains ).collect( Collectors.toList() );
//      uris.removeAll( modifierUris );
//      // Create map seeded with each uri as best of itself
//      final Map<String, Collection<String>> uniqueChildren = getUniqueChildren( uris, uriRoots );
//      // put modifiers back in
//      modifierUris.forEach( u -> uniqueChildren.computeIfAbsent( u, Collections::singletonList ) );
//      return uniqueChildren;
//   }
//
//   // TODO  This might be better done by collecting uris that subsume other uris, then doing the replacement
//   static private Map<String,Collection<String>> getUniqueChildren( final List<String> uris, final Map<String, Collection<String>> uriRoots ) {
//      // Create map seeded with each uri as best of itself
//      final Map<String, Collection<String>> uniqueChildren = new HashMap<>( uris.size() );
//      uris.forEach( u -> uniqueChildren.computeIfAbsent( u, l -> new ArrayList<>() ).add( u ) );
//
//      for ( int i=0; i<uris.size()-1; i++ ) {
//         final String iUri = uris.get( i );     // e.g. Brain
//         final Collection<String> iLeafs = uniqueChildren.computeIfAbsent( iUri, u -> new HashSet<>() );  // e.g. Parietal_Lobe
//         Collection<String> iRoots = uriRoots.get( iUri );    //  e.g. Body_Part > Anatomy
//         for ( int j = i + 1; j < uris.size(); j++ ) {
//            final String jUri = uris.get( j );  // e.g. Occipital_Lobe
//            final Collection<String> jLeafs = uniqueChildren.computeIfAbsent( jUri, u -> new HashSet<>() );  // e.g. empty
//            // check for new j subsumes older i
//            final boolean jSubsumesI = subsumeParents( jUri, iLeafs, uriRoots );
//            if ( !jSubsumesI ) {
//               // check for older i subsumes newer j
//               subsumeParents( iUri, iRoots, jLeafs, uriRoots );
//            }
//         }
//      }
//      return uniqueChildren;
//   }
//
//
//   static private String getBetterChild( final String uri1,
//                                         final String uri2,
//                                         final Map<String,Collection<String>> uriRoots ) {
//      if ( uri1.equals( uri2 ) ) {
//         return null;
//      }
//      return getBetterChild( uri1, uri2, uriRoots.get( uri1 ), uriRoots.get( uri2 ) );
//   }
//
//   static private String getBetterChild( final String uri1,
//                                         final String uri2,
//                                         final Collection<String> uri1Roots,
//                                         final Collection<String> uri2Roots ) {
//      if ( uri1.equals( uri2 ) ) {
//         return null;
//      }
//      if ( uri1Roots.contains( uri2 ) ) {
//         // The uri2 is a parent of the uri1.  Uri1 is the leaf.
//         return uri1;
//      } else if ( uri2Roots.contains( uri1 ) ) {
//         // The uri1 is a parent of the uri2.  Uri2 is the leaf.
//         return uri2;
//      }
//      return getCloseUriLeaf( uri1, uri2 );
//   }
//
//
//   static private boolean subsumeParents( final String subsumer,
//                                          final Collection<String> leafs,
//                                          final Map<String,Collection<String>> uriRoots ) {
//      final Collection<String> subsumerRoots = uriRoots.get( subsumer );
//      return subsumeParents( subsumer, subsumerRoots, leafs, uriRoots );
//   }
//
//
//   static private boolean subsumeParents( final String subsumer,
//                                          final Collection<String> subsumerRoots,
//                                          final Collection<String> leafs,
//                                          final Map<String,Collection<String>> uriRoots ) {
//
//      final Collection<String> removalLeafs = new ArrayList<>();
//      for ( String leaf : leafs ) {
//         if ( subsumer.equals( leaf ) ) {
//            // uri is already in leaf set.  Assume that this subsumption has already been done.
//            return false;
//         }
//         final String betterLeaf = getBetterChild( subsumer, leaf, subsumerRoots, uriRoots.get( leaf ) );
//         if ( betterLeaf != null && betterLeaf.equals( subsumer ) ) {
//            // Maintain that insert = true, subsume this leaf.
//            removalLeafs.add( leaf );
//         } else if ( betterLeaf != null && betterLeaf.equals( leaf ) ) {
//            // A leaf is more specific than the subsumer.  Assume that better subsumption has already been done.
//            return false;
//         }
//      }
//      if ( removalLeafs.isEmpty() ) {
//         return false;
//      }
//      leafs.removeAll( removalLeafs );
//      leafs.add( subsumer );
//      return true;
//   }
//
//
//   /**
//    * Gets a "best leaf" uri.  For example, when given "left breast", "breast"
//    * it will return the common leaf "left breast".
//    * This assumes that the uris are in a common branch.
//    * Something like "right breast" "left breast" "breast" would result in the return of "right breast"
//    *
//    * @param allUris -
//    * @return a uri that is the most "specific"
//    */
//   static public String getMostSpecificUri( final Collection<String> allUris ) {
//      if ( allUris.size() == 1 ) {
//         return new ArrayList<>( allUris ).get( 0 );
//      }
//      final Map<String, Collection<String>> rootMap
//            = allUris.stream()
//                     .distinct()
//                     .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getRootUris ) );
//      String firstUri = "";
//      String bestCloseUri = "";
//      String bestCountUri = "";
//      String bestLengthUri = "";
//      int bestCount = 0;
//      for ( String uri : allUris ) {
//         int count = rootMap.get( uri ).size();
//         if ( bestCloseUri.isEmpty() ) {
//            firstUri = uri;
//            bestCloseUri = uri;
//            bestCountUri = uri;
//            bestCount = count;
//            bestLengthUri = uri;
//            continue;
//         }
//         final String betterChild = getBetterChild( uri, bestCloseUri, rootMap );
//         if ( betterChild != null && betterChild.equals( uri ) ) {
//            bestCloseUri = uri;
//         }
//         if ( count > bestCount ) {
//            bestCountUri = uri;
//            bestCount = count;
//         }
//         if ( uri.length() > bestLengthUri.length() ) {
//            // Not exactly brillant, but assume that something with a longer name is more specific
//            bestLengthUri = uri;
//         }
//      }
//      if ( !firstUri.equals( bestCloseUri ) ) {
//         return bestCloseUri;
//      } else if ( !firstUri.equals( bestCountUri ) ) {
//         return bestCountUri;
//      }
//      return bestLengthUri;
//   }
//
//
//   static public String getShortestRootUri( final Collection<String> allUris ) {
//      long leastLength = Integer.MAX_VALUE;
//      String bestUri = "";
//      for ( String uri : allUris ) {
//         long length = Neo4jOntologyConceptUtil.getRootUris( uri ).size();
//         if ( length < leastLength ) {
//            leastLength = length;
//            bestUri = uri;
//         }
//      }
//      return bestUri;
//   }
//
//
//   private static Map<String, String> HARDCODED_CLOSE_ENOUGH = new HashMap<>();
//
//   static {
//      // Synonyms hard coded for now until get more generalized solution using
//      // ontology walking that will deal with these cases
//      // Some are not synonyms, but "part_of" anatomicals that aren't handled well in the ontology.
//      final String axilArea = "our Axillary_Lymph_Node Axilla area";
//      final String breastArea = "our Breast area";
//      final String CHEST_AREA = "our Chest area";
//      final String UTERINE_AREA = "our Uterine area";
//      final String OVARY_AREA = "our Ovary area";
//      final String ABDOMEN_AREA = "our Abdomen area";
//      final String COLON_AREA = "Our Colon area";
//      final String LEG_AREA = "Our Leg area";
//      final String ENDO_ADENO = "Our Endometrial Adenocarcinoma";
//      HARDCODED_CLOSE_ENOUGH.put( "Axilla", axilArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Axillary_Lymph_Node", axilArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Lymphatic_Vessel", axilArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Breast", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Left_Breast", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Right_Breast", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Nipple", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Mammary_Gland_Tissue", breastArea );
//      HARDCODED_CLOSE_ENOUGH.put( "Duct", breastArea );   // TODO consider doing this one only for BrCa
//      HARDCODED_CLOSE_ENOUGH.put( "Lung", CHEST_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Chest", CHEST_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Uterus", UTERINE_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Cervix_Uteri", UTERINE_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Endocervix", UTERINE_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Ovary", OVARY_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Fallopian_Tube", OVARY_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Abdomen", ABDOMEN_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Mesentery", ABDOMEN_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Omentum", ABDOMEN_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Peritoneal_Cavity", ABDOMEN_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Abdominal_Wall", ABDOMEN_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Inguinal_Region", ABDOMEN_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Large_Intestine", COLON_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Colon", COLON_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Leg", LEG_AREA );
//      HARDCODED_CLOSE_ENOUGH.put( "Thigh", LEG_AREA );
//
//      HARDCODED_CLOSE_ENOUGH.put( "Endometrial_Adenocarcinoma", ENDO_ADENO );
//      HARDCODED_CLOSE_ENOUGH.put( "Endometrioid_Adenocarcinoma", ENDO_ADENO );
//   }
//
//   static public String getCloseUriLeaf( final String uri1, final String uri2 ) {
//      final String close1 = getCloseUriLeaf1( uri1, uri2 );
//      if ( close1 != null ) {
//         return close1;
//      }
//      // Attempt match by part of
//      return getCloseUriLeaf2( uri1, uri2,
//            Neo4jOntologyConceptUtil.getRootUris( uri1 ),
//            Neo4jOntologyConceptUtil.getRootUris( uri2 ) );
//   }
//
//   static public String getCloseUriLeaf( final String uri1, final String uri2, final Collection<String> roots1,
//                                         final Collection<String> roots2 ) {
//      final String close1 = getCloseUriLeaf1( uri1, uri2 );
//      if ( close1 != null ) {
//         return close1;
//      }
//      // Attempt match by part of
//      return getCloseUriLeaf2( uri1, uri2, roots1, roots2 );
//   }
//
//   static private String getCloseUriLeaf1( final String uri1, final String uri2 ) {
//      String lookup1 = HARDCODED_CLOSE_ENOUGH.get(uri1);
//      if ( !uri1.equals( uri2 ) && lookup1 != null && lookup1.equals( HARDCODED_CLOSE_ENOUGH.get( uri2 ) ) ) {
//         if ( uri1.length() > uri2.length() ) {
//            return uri1;
//         } else {
//            return uri2;
//         }
//      }
//      // Match "Sentinel" Lymph node
//      if ( uri1.equals( "Sentinel_Lymph_Node" ) && uri2.endsWith( "Lymph_Node" ) ) {
//         return uri2;
//      } else if ( uri2.equals( "Sentinel_Lymph_Node" ) && uri1.endsWith( "Lymph_Node" ) ) {
//         return uri1;
//      }
////      if ( ( uri1.equals( "Chest" ) && uri2.equals( "Breast" ) ) || ( uri2.equals( "Chest" ) && uri1.equals( "Breast" ) ) ) {
////         return "Breast";
////      }
//      return null;
//   }
//
//   static private String getCloseUriLeaf2( final String uri1, final String uri2, final Collection<String> roots1,
//                                           final Collection<String> roots2 ) {
//      // Attempt match by part of
//      if ( roots1.contains( uri2 + "_Part" ) ) {
//         return uri1;
//      }
//      if ( roots2.contains( uri1 + "_Part" ) ) {
//         return uri2;
//      }
//      if ( roots1.contains( "Body_Part" ) && uri1.contains( uri2 ) ) {
//         return uri1;
//      }
//      if ( roots2.contains( "Body_Part" ) && uri2.contains( uri1 ) ) {
//         return uri2;
//      }
//      return null;
//   }
//
//   static public String getCloseUriRoot( final String uri1, final String uri2 ) {
//      if  ( uri1.equals( uri2 ) ) {
//         return null;
//      }
//      String lookup1 = HARDCODED_CLOSE_ENOUGH.get(uri1);
//      if ( lookup1 != null && lookup1.equals( HARDCODED_CLOSE_ENOUGH.get( uri2 ) ) ) {
//         if ( uri1.length() > uri2.length() ) {
//            return uri2;
//         } else {
//            return uri1;
//         }
//      }
//      // Match "Sentinel" Lymph node
//      if ( uri1.equals( "Sentinel_Lymph_Node" ) && uri2.endsWith( "Lymph_Node" ) ) {
//         return uri2;
//      } else if ( uri2.equals( "Sentinel_Lymph_Node" ) && uri1.endsWith( "Lymph_Node" ) ) {
//         return uri1;
//      }
//      if ( (uri1.equals( "Chest" ) && uri2.equals( "Breast" )) ||
//           (uri2.equals( "Chest" ) && uri1.equals( "Breast" )) ) {
//         return "Breast";
//      }
//      // Attempt match by part of
//      final Collection<String> maybeRoots1 = Neo4jOntologyConceptUtil.getRootUris( uri1 );
//      if ( maybeRoots1.contains( uri2 + "_Part" ) ) {
//         return uri2;
//      }
//      final Collection<String> maybeRoots2 = Neo4jOntologyConceptUtil.getRootUris( uri2 );
//      if ( maybeRoots2.contains( uri1 + "_Part" ) ) {
//         return uri1;
//      }
//      if ( maybeRoots1.contains( "Body_Part" ) && uri1.contains( uri2 ) ) {
//         return uri2;
//      }
//      if ( maybeRoots2.contains( "Body_Part" ) && uri2.contains( uri1 ) ) {
//         return uri1;
//      }
//      return null;
//   }
//
//   static public boolean isBilaterality( final String uri1, final String uri2 ) {
//      if ( uri1.isEmpty() || uri2.isEmpty() ) {
//         return false;
//      }
//      if ( uri1.equals( uri2 ) ) {
//         return true;
//      }
//      return uri1.equals( "Bilateral" ) || uri2.equals( "Bilateral" );
//   }
//
//   static public String getExtension( final String url ) {
//      final int hashIndex = url.indexOf( '#' );
//      if ( hashIndex >= 0 && hashIndex < url.length() - 1 ) {
//         return url.substring( hashIndex + 1 );
//      }
//      return url;
//   }
//
//
//
//}
