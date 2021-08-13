package org.healthnlp.deepphe.util;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.constant.UriConstants.LYMPH_NODE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/15/2020
 */
final public class MentionUtil {

   static private final Logger LOGGER = Logger.getLogger( "MentionUtil" );

   private MentionUtil() {}


//   static public void collateCoref(
//         final List<List<Mention>> chains,
//         final Collection<Mention> coref,
//         final Map<Mention, Collection<String>> locationUris,
//         final Map<Mention, Collection<String>> lateralityUris ) {
//
////      LOGGER.info( "\nCollating Coreferences, First by Loose Assertion." );
//
//      final Map<String, List<Mention>> assertionMap = new HashMap<>();
//      for ( Mention mention : coref ) {
//         final String assertion = getLooseAssertion( mention );
//
//
////         LOGGER.info( assertion + " " + mention.getClassUri() + " " + mention.getId() );
//
//
//         assertionMap.computeIfAbsent( assertion, a -> new ArrayList<>() ).add( mention );
//      }
//
//      for ( List<Mention> asserted : assertionMap.values() ) {
//         if ( asserted.size() <= 1 ) {
//            continue;
//         }
//         collateAsserted( chains, asserted, locationUris, lateralityUris );
//      }
//   }

   static public void collateCoref(
         final List<List<Mention>> chains,
         final Collection<Mention> coref,
         final Map<String,Map<Mention,Collection<String>>> typeLocationUris,
         final Map<Mention, Collection<String>> lateralityUris ) {

//      LOGGER.info( "\nCollating Coreferences, First by Loose Assertion." );

      final Map<String, List<Mention>> assertionMap = new HashMap<>();
      for ( Mention mention : coref ) {
         final String assertion = getLooseAssertion( mention );


//         LOGGER.info( assertion + " " + mention.getClassUri() + " " + mention.getId() );


         assertionMap.computeIfAbsent( assertion, a -> new ArrayList<>() ).add( mention );
      }

      for ( List<Mention> asserted : assertionMap.values() ) {
         if ( asserted.size() <= 1 ) {
            continue;
         }
         collateAsserted( chains, asserted, typeLocationUris, lateralityUris );
      }
   }


   static private String getLooseAssertion( final Mention mention ) {
      return mention.isNegated() ? "NEGATED" : "AFFIRMED_OR_UNCERTAIN";
   }


//   static private void collateAsserted(
//         final List<List<Mention>> chains,
//         final List<Mention> asserted,
//         final Map<Mention, Collection<String>> locationUris,
//         final Map<Mention, Collection<String>> lateralityUris ) {
//      // Gather locations and separate chain by those
//      final Map<String, Map<String, Collection<Mention>>> siteCollatedChains
//            = collateBySiteLateral( asserted, locationUris, lateralityUris );
//      if ( siteCollatedChains.size() == 1 ) {
//         // Only a single location.  Add each laterality as a chain
//         addToChains( chains, siteCollatedChains );
//         return;
//      }
//      final Map<String, Collection<Mention>> siteNeutrals = siteCollatedChains.get( SITE_NEUTRAL );
//      if ( siteNeutrals != null ) {
//         siteCollatedChains.remove( SITE_NEUTRAL );
//         if ( siteCollatedChains.size() == 1 ) {
//            // Only a single location, equate all non-located entities and add each laterality
//            mergeChains( chains, siteCollatedChains, siteNeutrals );
//            return;
//         }
//      }
//      final Map<String, Collection<Mention>> lymphNodes = siteCollatedChains.get( LYMPH_NODE );
//      if ( lymphNodes != null ) {
//
////         LOGGER.info( "\nRemoving Lymph Node chains and handling specially ..." );
//
//         siteCollatedChains.remove( LYMPH_NODE );
//         if ( siteCollatedChains.size() == 1 ) {
//            // Only a single location, equate all non-located entities and add each laterality
//            if ( siteNeutrals != null ) {
//               mergeChains( chains, siteCollatedChains, siteNeutrals );
//            }
//            // Only a single location and lymph nodes, equate all lymph node entities and add each laterality
//            mergeChains( chains, siteCollatedChains, lymphNodes );
//            return;
//         }
//      }
//      if ( siteNeutrals != null ) {
//
////         LOGGER.info( "\nAdding site-neutral chains" );
//
////         siteNeutrals.forEach( (k,v) -> LOGGER.info( k + " (" + v.stream().map( Mention::getClassUri ).collect( Collectors
////               .joining("," ) ) + ")" ) );
//         siteCollatedChains.put( SITE_NEUTRAL, siteNeutrals );
//      }
//      if ( lymphNodes != null ) {
//
////         LOGGER.info( "\nAdding lymph node chains" );
//
////         lymphNodes.forEach( (k,v) -> LOGGER.info( k + " (" + v.stream().map( Mention::getClassUri ).collect( Collectors.joining("," ) ) + ")" ) );
//         siteCollatedChains.put( LYMPH_NODE, lymphNodes );
//      }
//      // At this point we have:
//      // siteCollatedInstances with more than 1 site,
//      // lymphNodes with lateral lymph nodes,
//      // anySite with lateral non-sited
//      addToChains( chains, siteCollatedChains );
//   }

   static private void collateAsserted(
         final List<List<Mention>> chains,
         final List<Mention> asserted,
         final Map<String,Map<Mention,Collection<String>>> typeLocationUris,
         final Map<Mention, Collection<String>> lateralityUris ) {
      // Gather locations and separate chain by those
      final Map<String, Map<String, Collection<Mention>>> siteCollatedChains
            = collateBySiteLateral( asserted, typeLocationUris, lateralityUris );
      if ( siteCollatedChains.size() == 1 ) {
         // Only a single location.  Add each laterality as a chain
         addToChains( chains, siteCollatedChains );
         return;
      }
      final Map<String, Collection<Mention>> siteNeutrals = siteCollatedChains.get( SITE_NEUTRAL );
      if ( siteNeutrals != null ) {
         siteCollatedChains.remove( SITE_NEUTRAL );
         if ( siteCollatedChains.size() == 1 ) {
            // Only a single location, equate all non-located entities and add each laterality
            mergeChains( chains, siteCollatedChains, siteNeutrals );
            return;
         }
      }
      final Map<String, Collection<Mention>> lymphNodes = siteCollatedChains.get( LYMPH_NODE );
      if ( lymphNodes != null ) {

//         LOGGER.info( "\nRemoving Lymph Node chains and handling specially ..." );

         siteCollatedChains.remove( LYMPH_NODE );
         if ( siteCollatedChains.size() == 1 ) {
            // Only a single location, equate all non-located entities and add each laterality
            if ( siteNeutrals != null ) {
               mergeChains( chains, siteCollatedChains, siteNeutrals );
            }
            // Only a single location and lymph nodes, equate all lymph node entities and add each laterality
            mergeChains( chains, siteCollatedChains, lymphNodes );
            return;
         }
      }
      if ( siteNeutrals != null ) {

//         LOGGER.info( "\nAdding site-neutral chains" );

//         siteNeutrals.forEach( (k,v) -> LOGGER.info( k + " (" + v.stream().map( Mention::getClassUri ).collect( Collectors
//               .joining("," ) ) + ")" ) );
         siteCollatedChains.put( SITE_NEUTRAL, siteNeutrals );
      }
      if ( lymphNodes != null ) {

//         LOGGER.info( "\nAdding lymph node chains" );

//         lymphNodes.forEach( (k,v) -> LOGGER.info( k + " (" + v.stream().map( Mention::getClassUri ).collect( Collectors.joining("," ) ) + ")" ) );
         siteCollatedChains.put( LYMPH_NODE, lymphNodes );
      }
      // At this point we have:
      // siteCollatedInstances with more than 1 site,
      // lymphNodes with lateral lymph nodes,
      // anySite with lateral non-sited
      addToChains( chains, siteCollatedChains );
   }


   /**
    * @param mentions -
    * @return Map of related tumor concept instances, Uri is Key 1, Laterality is key 2, deep value is annotations.
    * Tumors are related if they have the same laterality and are within the same body site uri branch.
    */
//   static private Map<String, Map<String, Collection<Mention>>> collateBySiteLateral(
//         final Collection<Mention> mentions,
//         final Map<Mention, Collection<String>> locationUris,
//         final Map<Mention, Collection<String>> lateralityUris ) {
//
//      // Collection of "same-site" "same-laterality" tumors
//      final Map<String, Map<String, Collection<Mention>>> lateralSiteMentions = new HashMap<>();
//
//      // Collection of "same-site" tumors
//      final Map<String, Collection<Mention>> sitedMentions
//            = collateBySite( mentions, locationUris );
//
//      // deal with laterality
//      for ( Map.Entry<String, Collection<Mention>> siteMentions : sitedMentions.entrySet() ) {
//         // Map of laterality uris to tumor concept instances with that laterality
//         final Map<String, Collection<Mention>> lateralSited
//               = collateByLaterality( siteMentions.getValue(), lateralityUris );
//
//         lateralSiteMentions.put( siteMentions.getKey(), lateralSited );
//      }
//
//
////      LOGGER.info( "Now Have full map of Site : Laterality : Mention @ site,laterality" );
//
//
//      return lateralSiteMentions;
//   }

   static private Map<String, Map<String, Collection<Mention>>> collateBySiteLateral(
         final Collection<Mention> mentions,
         final Map<String,Map<Mention,Collection<String>>> typeLocationUris,
         final Map<Mention, Collection<String>> lateralityUris ) {

      // Collection of "same-site" "same-laterality" tumors
      final Map<String, Map<String, Collection<Mention>>> lateralSiteMentions = new HashMap<>();

      // Collection of "same-site" tumors
      final Map<String, Collection<Mention>> sitedMentions
            = collateBySite( mentions, typeLocationUris );

      // deal with laterality
      for ( Map.Entry<String, Collection<Mention>> siteMentions : sitedMentions.entrySet() ) {
         // Map of laterality uris to tumor concept instances with that laterality
         final Map<String, Collection<Mention>> lateralSited
               = collateByLaterality( siteMentions.getValue(), lateralityUris );

         lateralSiteMentions.put( siteMentions.getKey(), lateralSited );
      }


//      LOGGER.info( "Now Have full map of Site : Laterality : Mention @ site,laterality" );


      return lateralSiteMentions;
   }


//   /**
//    * Also collapses body sites
//    *
//    * @param mentions -
//    * @return map of best site uris to all annotations with that best site.
//    * annotations are relatable if they have the same laterality and are within the same bodysite uri branch.
//    */
//   static private Map<String, Collection<Mention>> collateBySite(
//         final Collection<Mention> mentions,
//         final Map<Mention, Collection<String>> locationUris ) {
//
//      // map of body site uris and annotations with those uris  -> for all tumors with this laterality
//      final Map<String, List<Mention>> uriBodySites = getUriBodySites( mentions, locationUris );
//
//
////      LOGGER.info( "Collected Body Sites for Mentions:" );
////      uriBodySites.forEach( (k,v) -> LOGGER.info( "Site: " + k
////                                                  + " (" + v.stream().map( m -> m.getClassUri() + " " + m.getId() ).collect( Collectors.joining( ",") ) + ")" ) );
//
//
//      // collate site uris
//      final Map<String, Collection<String>> associatedSiteUriMap = getAssociatedSiteUriMap( uriBodySites );
//
//
////      LOGGER.info( "Associated Body Sites:" );
////      associatedSiteUriMap.forEach( (k,v) -> LOGGER.info( "Best Site: " + k + " (" + String.join( ",", v ) + ")" ) );
//
//
//      final Map<String, Collection<Mention>> associatedBodySites
//            = new HashMap<>( associatedSiteUriMap.size() );
//      for ( Map.Entry<String, Collection<String>> associatedSiteUris : associatedSiteUriMap.entrySet() ) {
//         final Collection<Mention> associatedSites = new HashSet<>();
//         for ( String associatedSiteUri : associatedSiteUris.getValue() ) {
//            final Collection<Mention> sited = uriBodySites.get( associatedSiteUri );
//            if ( sited != null ) {
//               associatedSites.addAll( sited );
//            }
//         }
//         associatedBodySites.put( associatedSiteUris.getKey(), associatedSites );
//      }
//
//
////      LOGGER.info( "Refined Body Sites for Mentions:" );
////      associatedBodySites.forEach( (k,v) -> LOGGER.info( "Site: " + k + " ("
////                                                         + v.stream().map( m -> m.getClassUri() + " " + m.getId() ).collect( Collectors.joining(",") ) + ")" ) );
//
//
//      return associatedBodySites;
//   }

   private static final Collection<String> SITE_RELATIONS
         = Arrays.asList( "Disease_Has_Primary_Anatomic_Site",
                         "Disease_Has_Associated_Anatomic_Site",
                         "Disease_Has_Metastatic_Anatomic_Site",
                         "Disease_Has_Associated_Region",
                         "Disease_Has_Associated_Cavity",
                         "Finding_Has_Associated_Site",
                         "Finding_Has_Associated_Region",
                         "Finding_Has_Associated_Cavity" );

   /**
    * Also collapses body sites
    *
    * @param mentions -
    * @return map of best site uris to all annotations with that best site.
    * annotations are relatable if they have the same laterality and are within the same bodysite uri branch.
    */
   static private Map<String, Collection<Mention>> collateBySite(
         final Collection<Mention> mentions,
         final Map<String,Map<Mention,Collection<String>>> typeLocationUris ) {

      // map of body site uris and annotations with those uris  -> for all tumors with this laterality
      final Map<String, Map<String, List<Mention>>> typeUriBodySites = new HashMap<>();
      for ( Map.Entry<String, Map<Mention, Collection<String>>> typeLocationUri : typeLocationUris.entrySet() ) {
         final Map<String, List<Mention>> uriBodySites = getUriBodySites( mentions, typeLocationUri.getValue() );
         typeUriBodySites.put( typeLocationUri.getKey(), uriBodySites );
      }

//      LOGGER.info( "Collected Body Sites for Mentions:" );
//      uriBodySites.forEach( (k,v) -> LOGGER.info( "Site: " + k
//                                                  + " (" + v.stream().map( m -> m.getClassUri() + " " + m.getId() ).collect( Collectors.joining( ",") ) + ")" ) );


      // collate site uris
      final Map<String, Map<String, Collection<String>>> typeAssociatedSiteUriMap = new HashMap<>();
      for ( Map.Entry<String, Map<String, List<Mention>>> typeUriBodySite : typeUriBodySites.entrySet() ) {
         final Map<String, Collection<String>> associatedSiteUris
               = getAssociatedSiteUriMap( typeUriBodySite.getValue() );
         typeAssociatedSiteUriMap.put( typeUriBodySite.getKey(), associatedSiteUris );
      }

//      LOGGER.info( "Associated Body Sites:" );
//      associatedSiteUriMap.forEach( (k,v) -> LOGGER.info( "Best Site: " + k + " (" + String.join( ",", v ) + ")" ) );

      // We want to go through the site relation types, locating mentions.
      // If a mention has been located using a higher order type, skip it.
      final Collection<Mention> alreadySited = new HashSet<>();
      final Map<String, Collection<Mention>> associatedBodySites = new HashMap<>();
      for ( String type : SITE_RELATIONS ) {
         final Map<String,Collection<String>> associatedSiteUriMap = typeAssociatedSiteUriMap.get( type );
         if ( associatedSiteUriMap == null || associatedSiteUriMap.isEmpty() ) {
            continue;
         }
         final Map<String, List<Mention>> uriBodySites = typeUriBodySites.get( type );
         if ( uriBodySites == null || uriBodySites.isEmpty() ) {
            continue;
         }
         for ( Map.Entry<String, Collection<String>> associatedSiteUris : associatedSiteUriMap.entrySet() ) {
            for ( String associatedSiteUri : associatedSiteUris.getValue() ) {
               final Collection<Mention> sited = uriBodySites.get( associatedSiteUri );
               if ( sited != null ) {
                  sited.removeAll( alreadySited );
                  if ( !sited.isEmpty() ) {
                     associatedBodySites.computeIfAbsent( associatedSiteUris.getKey(), s -> new HashSet<>() )
                                        .addAll( sited );
                     alreadySited.addAll( sited );
                  }
               }
            }
         }
      }

//      LOGGER.info( "Refined Body Sites for Mentions:" );
//      associatedBodySites.forEach( (k,v) -> LOGGER.info( "Site: " + k + " ("
//                                                         + v.stream().map( m -> m.getClassUri() + " " + m.getId() )
//                                                            .collect( Collectors.joining( "," ) ) + ")" ) );


      return associatedBodySites;
   }


   static private final String SITE_NEUTRAL = "Site_Neutral";
   static private final String SIDE_NEUTRAL = "Side_Neutral";

   static private Map<String, List<Mention>> getUriBodySites(
         final Collection<Mention> mentions,
         final Map<Mention, Collection<String>> locationUris ) {
      final Map<String, List<Mention>> uriSites = new HashMap<>();
      for ( Mention mention : mentions ) {
         final Collection<String> sites = locationUris.get( mention );
         if ( sites == null ) {
            uriSites.computeIfAbsent( SITE_NEUTRAL, s -> new ArrayList<>() ).add( mention );
            continue;
         }
         for ( String site : sites ) {
            uriSites.computeIfAbsent( site, s -> new ArrayList<>() ).add( mention );
         }
      }
      return uriSites;
   }




   static public Map<String, Collection<String>> getAssociatedSiteUriMap(
         final Map<String, List<Mention>> uriMentions ) {
      return getAssociatedSiteUriMap( uriMentions.keySet() );
   }

   /**
    * @param uris -
    * @return Map of each "best" uri and the existing other uris that are its roots
    */
   static public Map<String, Collection<String>> getAssociatedSiteUriMap( final Collection<String> uris ) {
      // Join all uris that fall within a root tree
      final Map<String, String> bestRoots = UriUtil.getBestRoots( uris );


//      LOGGER.info( "Best Root URIs for Site URIs: " );
//      bestRoots.forEach( (k,v) -> LOGGER.info( k + " " + v ) );


      final Map<String, Collection<String>> rootChildrenMap = new HashMap<>();
      for ( Map.Entry<String, String> bestRoot : bestRoots.entrySet() ) {
         // fill the map of each "best" root uri to a list of all leaf uris for which it is best.
         rootChildrenMap.computeIfAbsent( bestRoot.getValue(), u -> new ArrayList<>() ).add( bestRoot.getKey() );
      }

      final Map<String, Collection<String>> bestAssociations = new HashMap<>();
      for ( Map.Entry<String, Collection<String>> rootLeafs : rootChildrenMap.entrySet() ) {
         final Map<String, Collection<String>> uniqueChildren = UriUtil.getUniqueChildren( rootLeafs.getValue() );
         for ( Map.Entry<String, Collection<String>> rootBest : uniqueChildren.entrySet() ) {
            rootBest.getValue().forEach( u -> bestAssociations.computeIfAbsent( u, l -> new HashSet<>() )
                                                              .add( rootBest.getKey() ) );
         }
      }
      // For Naaccr, If there is only one site, add the site-neutral.  Should we just add site-neutrals to every set?
      if ( bestAssociations.size() == 2 && bestAssociations.containsKey( SITE_NEUTRAL ) ) {
         final Collection<String> siteNeutrals = bestAssociations.get( SITE_NEUTRAL );
         bestAssociations.remove( SITE_NEUTRAL );
         final List<String> keys = new ArrayList<>( bestAssociations.keySet() );
         bestAssociations.put( keys.get( 0 ), siteNeutrals );
      }
      return bestAssociations;
   }


   static private Map<String, Collection<Mention>> collateByLaterality(
         final Collection<Mention> mentions,
         final Map<Mention, Collection<String>> lateralityUris ) {
      final Map<String, Collection<Mention>> lateralityMap = new HashMap<>( 3 );
      final Collection<Mention> sideNeutral = new HashSet<>();
      for ( Mention mention : mentions ) {
         final Collection<String> lateralities = lateralityUris.get( mention );
         if ( lateralities == null || lateralities.isEmpty() ) {
            sideNeutral.add( mention );
         } else {
            for ( String laterality : lateralities ) {
               lateralityMap.computeIfAbsent( laterality, s -> new HashSet<>() ).add( mention );
            }
         }
      }
      if ( !sideNeutral.isEmpty() ) {
         if ( lateralityMap.size() == 1 ) {
            lateralityMap.values().forEach( s -> s.addAll( sideNeutral ) );
         } else {
            lateralityMap.put( SIDE_NEUTRAL, sideNeutral );
         }
      }


//      LOGGER.info( "Associated Lateralities:" );
//      lateralityMap.forEach( (k,v) -> LOGGER.info( k + " ("
//                                                   + v.stream().map( m -> m.getClassUri() + " " + m.getId() ).collect( Collectors.joining( "," ) ) + ")" ) );


      return lateralityMap;
   }


   static private void addToChains(
         final List<List<Mention>> chains,
         final Map<String, Map<String, Collection<Mention>>> siteLateralChains ) {
      for ( Map<String, Collection<Mention>> map : siteLateralChains.values() ) {
         if ( map == null ) {
            continue;
         }
         map.values().stream()
            .filter( Objects::nonNull )
            .filter( c -> c.size() > 1 )
//            .peek( c -> LOGGER.info( "Adding site-based Coreference Chain ("
//                                     + c.stream().map( m -> m.getClassUri() + " " + m.getId() ).collect( Collectors.joining(",") ) + ")" ) )
            .forEach( c -> chains.add( new ArrayList<>( c ) ) );
      }
   }


   static private void mergeChains(
         final List<List<Mention>> chains,
         final Map<String, Map<String, Collection<Mention>>> siteLateralChains,
         final Map<String, Collection<Mention>> lateralOnly ) {
      if ( lateralOnly == null ) {
         return;
      }
      // Only a single location, equate all non-located entities and add each laterality
      for ( Map<String, Collection<Mention>> lateralCollatedChains : siteLateralChains.values() ) {
         for ( Map.Entry<String, Collection<Mention>> lateralChain : lateralCollatedChains.entrySet() ) {
            if ( lateralOnly.get( lateralChain.getKey() ) != null ) {
               lateralChain.getValue().addAll( lateralOnly.get( lateralChain.getKey() ) );
            }
         }
      }
//      LOGGER.info( "\nMerging Chains ..." );
      addToChains( chains, siteLateralChains );
   }



   static public Map<MentionRelation, String> getRelatedTargetIdsMap(
         final Collection<MentionRelation> relations, final Mention mention ) {
      final Map<MentionRelation, String> map = new HashMap<>();
      relations.stream()
               .map( r -> getRelationTargetIdEntry( r, mention ) )
               .filter( e -> !e.getValue().isEmpty() )
               .forEach( e -> map.put( e.getKey(), e.getValue() ) );
      return map;
   }

   static private Map.Entry<MentionRelation, String> getRelationTargetIdEntry(
         final MentionRelation relation, final Mention mention ) {
      return new AbstractMap.SimpleEntry<>( relation, getTargetId( relation, mention ) );
   }

   static private String getTargetId( final MentionRelation relation, final Mention mention ) {
      if ( !relation.getSourceId().equals( mention.getId() ) ) {
         return "";
      }
      return relation.getTargetId();
   }


}
