package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.CANCER;


/**
 * @author SPF , chip-nlp
 * @since {6/3/2021}
 */
final public class SiteTypeBin {

   static private final Logger LOGGER = Logger.getLogger( "SiteTypeBin" );


   private final SiteType _siteType;
   private final Collection<SiteNeoplasmBin> _siteNeoplasmBins = new ArrayList<>();


   SiteTypeBin( final SiteType siteType ) {
      _siteType = siteType;
   }


   static Map<SiteType,Collection<Mention>> getSiteTypeMentions( final Collection<Mention> neoplasms,
                                                                 final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      final Map<SiteType,Collection<Mention>> siteTypes = new EnumMap<>( SiteType.class );
      for ( Mention neoplasm : neoplasms ) {
         final Map<String, Collection<Mention>> relations = relationsMap.get( neoplasm );
         if ( relations == null || relations.isEmpty() ) {
            siteTypes.computeIfAbsent( SiteType.NO_SITE, b -> new HashSet<>() )
                        .add( neoplasm );
         } else {
            siteTypes.computeIfAbsent( SiteType.getSiteType( relations.keySet() ), b -> new HashSet<>() )
                     .add( neoplasm );
         }
      }
      return siteTypes;
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<String,Set<Mention>> uriMentionsMap,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      if ( _siteType == SiteType.NO_SITE ) {
         setNoSiteNeoplasms( cancers, tumors, relationsMap );
         return;
      }
      final Collection<String> allSiteUris = new HashSet<>();
      final Map<String,Collection<Mention>> siteUriCancersMap = new HashMap<>();
      final Map<String,Collection<Mention>> siteUriTumorsMap = new HashMap<>();
      final Map<Mention,Map<String,Collection<Mention>>> neoplasmSiteUriSitesMap = new HashMap<>();
//      LOGGER.info( "SiteTypeBin.setNeoplasms " + _siteType + " Line #50 Cancer Mentions : "
//                   + cancers.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
//      LOGGER.info( "SiteTypeBin.setNeoplasms " + _siteType + " Line #50 Tumor Mentions : "
//                   + tumors.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
      for ( Mention cancer : cancers ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( cancer );
         if ( relations == null || relations.isEmpty() ) {
            continue;
         }
         final Collection<String> siteUris = _siteType.getMatchingSiteUris( relations );
         if ( !siteUris.isEmpty() ) {
            allSiteUris.addAll( siteUris );
            siteUris.forEach( s -> siteUriCancersMap.computeIfAbsent( s, n -> new HashSet<>() )
                                                     .add( cancer ) );
            // Uses all mentions with a given uri, not just the mentions explicitly in a relation.
            final Map<String,Collection<Mention>> siteUriSitesMap
                  = siteUris.stream()
                            .collect( Collectors.toMap( Function.identity(), uriMentionsMap::get ) );
            neoplasmSiteUriSitesMap.put( cancer, siteUriSitesMap );
         }
      }
      for ( Mention tumor : tumors ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( tumor );
         if ( relations == null || relations.isEmpty() ) {
            continue;
         }
         final Collection<String> siteUris = _siteType.getMatchingSiteUris( relations );
         if ( !siteUris.isEmpty() ) {
            allSiteUris.addAll( siteUris );
            siteUris.forEach( s -> siteUriTumorsMap.computeIfAbsent( s, n -> new HashSet<>() )
                                                    .add( tumor ) );
            final Map<String,Collection<Mention>> siteUriSitesMap
                  = siteUris.stream()
                            .collect( Collectors.toMap( Function.identity(), uriMentionsMap::get ) );
            neoplasmSiteUriSitesMap.put( tumor, siteUriSitesMap );
         }
      }
      createSiteNeoplasmBins( allSiteUris, siteUriCancersMap, siteUriTumorsMap, neoplasmSiteUriSitesMap );

      // TODO - need a better way to merge mentions.  Possibly after some other manipulations?
//      resolveMentionConflicts( CANCER );
//      resolveMentionConflicts( TUMOR );
   }


   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<String,Set<Mention>> uriMentionsMap,
                      final Map<String,Collection<String>> associatedSitesMap,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      if ( _siteType == SiteType.NO_SITE ) {
         setNoSiteNeoplasms( cancers, tumors, relationsMap );
         return;
      }
      final Collection<String> allSiteUris = new HashSet<>();
      final Map<String,Collection<Mention>> siteUriCancersMap = new HashMap<>();
      final Map<String,Collection<Mention>> siteUriTumorsMap = new HashMap<>();
      final Map<Mention,Map<String,Collection<Mention>>> neoplasmSiteUriSitesMap = new HashMap<>();
//      LOGGER.info( "SiteTypeBin.setNeoplasms " + _siteType + " Line #50 Cancer Mentions : "
//                   + cancers.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
//      LOGGER.info( "SiteTypeBin.setNeoplasms " + _siteType + " Line #50 Tumor Mentions : "
//                   + tumors.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
      for ( Mention cancer : cancers ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( cancer );
         if ( relations == null || relations.isEmpty() ) {
            continue;
         }
         final Collection<String> siteUris = _siteType.getMatchingSiteUris( relations );
         if ( !siteUris.isEmpty() ) {
            allSiteUris.addAll( siteUris );
            siteUris.forEach( s -> siteUriCancersMap.computeIfAbsent( s, n -> new HashSet<>() )
                                                    .add( cancer ) );
            final Map<String,Collection<Mention>> siteUriSitesMap
                  = siteUris.stream()
                            .collect( Collectors.toMap( Function.identity(), uriMentionsMap::get ) );
            neoplasmSiteUriSitesMap.put( cancer, siteUriSitesMap );
         }
      }
      for ( Mention tumor : tumors ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( tumor );
         if ( relations == null || relations.isEmpty() ) {
            continue;
         }
         final Collection<String> siteUris = _siteType.getMatchingSiteUris( relations );
         if ( !siteUris.isEmpty() ) {
            allSiteUris.addAll( siteUris );
            siteUris.forEach( s -> siteUriTumorsMap.computeIfAbsent( s, n -> new HashSet<>() )
                                                   .add( tumor ) );
            final Map<String,Collection<Mention>> siteUriSitesMap
                  = siteUris.stream()
                            .collect( Collectors.toMap( Function.identity(), uriMentionsMap::get ) );
            neoplasmSiteUriSitesMap.put( tumor, siteUriSitesMap );
         }
      }
      createSiteNeoplasmBins( allSiteUris, siteUriCancersMap, siteUriTumorsMap, associatedSitesMap,
                              neoplasmSiteUriSitesMap );

      // TODO - need a better way to merge mentions.  Possibly after some other manipulations?
//      resolveMentionConflicts( CANCER );
//      resolveMentionConflicts( TUMOR );
   }



   void setNoSiteNeoplasms( final Collection<Mention> cancers,
                            final Collection<Mention> tumors,
                            final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      final Collection<Mention> noSiteCancers = new HashSet<>();
      final Collection<Mention> noSiteTumors = new HashSet<>();
      for ( Mention cancer : cancers ) {
         final Map<String, Collection<Mention>> relations = relationsMap.get( cancer );
         if ( relations == null || relations.isEmpty() ) {
            if ( _siteType == SiteType.NO_SITE ) {
               noSiteCancers.add( cancer );
            }
            continue;
         }
         if ( SiteType.isNoSiteType( relations.keySet() ) ) {
            noSiteCancers.add( cancer );
         }
      }
      for ( Mention tumor : tumors ) {
         final Map<String, Collection<Mention>> relations = relationsMap.get( tumor );
         if ( relations == null || relations.isEmpty() ) {
            if ( _siteType == SiteType.NO_SITE ) {
               noSiteTumors.add( tumor );
            }
            continue;
         }
         if ( SiteType.isNoSiteType( relations.keySet() ) ) {
            noSiteTumors.add( tumor );
         }
      }
      final SiteNeoplasmBin noSiteNeoplasmBin = new SiteNeoplasmBin( noSiteCancers,
                                                                     noSiteTumors );
      _siteNeoplasmBins.add( noSiteNeoplasmBin );
//      LOGGER.info( "SiteTypeBin.setNoSiteNeoplasms " + _siteType + " Line #140 Cancer Mentions : "
//                   + noSiteCancers.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
//      LOGGER.info( "SiteTypeBin.setNoSiteNeoplasms " + _siteType + " Line #140 Tumor Mentions : "
//                   + noSiteTumors.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
   }


   void createSiteNeoplasmBins( final Collection<String> allSiteUris,
                                final Map<String,Collection<Mention>> siteUriCancersMap,
                                final Map<String,Collection<Mention>> siteUriTumorsMap,
                                final Map<Mention,Map<String,Collection<Mention>>> neoplasmSiteUriSitesMap ) {
//      final Map<String,Collection<String>> siteChains = UriUtil.getAssociatedUriMap( allSiteUris );
      final Map<String,Collection<String>> siteChains = UriUtil.getAllAssociatedUriMap( allSiteUris );
      AssertionBin.moveFemaleGenitalia( siteChains );
//      siteChains.forEach( (k,v) -> LOGGER.info( "SiteTypeBin.createSiteNeoplasmBins " + _siteType
//                                                + "SiteChain " + k + " :"
//                                                + " " + String.join( ";", v ) ) );
      for ( Map.Entry<String,Collection<String>> siteChain : siteChains.entrySet() ) {
         // Create a map with all site sites.  Otherwise siteChains get out of step.
         final Map<String,Set<Mention>> neoplasmSiteUriSites
               = siteChain.getValue()
                           .stream()
                           .collect( Collectors.toMap( Function.identity(), s -> new HashSet<>() ) );
         final Collection<Mention> cancers = siteChain.getValue()
                                                        .stream()
                                                        .map( siteUriCancersMap::get )
                                                      .filter( Objects::nonNull )
                                                      .flatMap( Collection::stream )
                                                        .collect( Collectors.toSet() );
//         LOGGER.info( "\n\nSiteTypeBin.createSiteNeoplasmBins " + _siteType +  " Cancer Mentions :\n"
//                      + siteChain.toString() + "\n"
//                      + cancers.stream().map( Mention::getClassUri )
//                               .sorted().collect( Collectors.joining(";") ) );
         for ( Mention cancer : cancers ) {
            final Map<String,Collection<Mention>> siteUriSites = neoplasmSiteUriSitesMap.get( cancer );
            siteUriSites.keySet().retainAll( siteChain.getValue() );
            siteUriSites.forEach( (k,v) -> neoplasmSiteUriSites.get( k )
                                                              .addAll( v ) );
         }
         final Collection<Mention> tumors = siteChain.getValue()
                                                      .stream()
                                                      .map( siteUriTumorsMap::get )
                                                     .filter( Objects::nonNull )
                                                     .flatMap( Collection::stream )
                                                      .collect( Collectors.toSet() );
//         LOGGER.info( "SiteTypeBin.createSiteNeoplasmBins " + _siteType + " Tumor Mentions : "
//                      + tumors.stream().map( Mention::getClassUri )
//                              .sorted().collect( Collectors.joining(";") ) );
         for ( Mention tumor : tumors ) {
            final Map<String,Collection<Mention>> siteUriSites = neoplasmSiteUriSitesMap.get( tumor );
            siteUriSites.keySet().retainAll( siteChain.getValue() );
            siteUriSites.forEach( (k,v) -> neoplasmSiteUriSites.get( k )
                                                               .addAll( v ) );
         }
//         LOGGER.info( "SiteNeoplasmBin Sites " + siteChain.getKey() + " : " + String.join( ";",
//                                                                                           neoplasmSiteUriSites.keySet() ) );
         final SiteNeoplasmBin siteNeoplasmBin = new SiteNeoplasmBin( siteChain.getKey(),
                                                                      neoplasmSiteUriSites,
                                                                      cancers,
                                                                      tumors );
         _siteNeoplasmBins.add( siteNeoplasmBin );
      }
   }


   void createSiteNeoplasmBins( final Collection<String> allSiteUris,
                                final Map<String,Collection<Mention>> siteUriCancersMap,
                                final Map<String,Collection<Mention>> siteUriTumorsMap,
                                final Map<String,Collection<String>> associatedSitesMap,
                                final Map<Mention,Map<String,Collection<Mention>>> neoplasmSiteUriSitesMap ) {
//      final Map<String,Collection<String>> siteChains = UriUtil.getAssociatedUriMap( allSiteUris );
//      associatedSitesMap.forEach( (k,v) -> LOGGER.info( "SiteTypeBin.createSiteNeoplasmBins " + _siteType
//                                                + "SiteChain " + k + " :"
//                                                + " " + String.join( ";", v ) ) );
      for ( Map.Entry<String,Collection<String>> siteChain : associatedSitesMap.entrySet() ) {
         // Create a map with all site sites.  Otherwise siteChains get out of step.
         final Map<String,Set<Mention>> neoplasmSiteUriSites
               = siteChain.getValue()
                          .stream()
                          .collect( Collectors.toMap( Function.identity(), s -> new HashSet<>() ) );
         final Collection<Mention> cancers = siteChain.getValue()
                                                      .stream()
                                                      .map( siteUriCancersMap::get )
                                                      .filter( Objects::nonNull )
                                                      .flatMap( Collection::stream )
                                                      .collect( Collectors.toSet() );
//         LOGGER.info( "\n\nSiteTypeBin.createSiteNeoplasmBins " + _siteType +  " Cancer Mentions :\n"
//                      + siteChain.toString() + "\n"
//                      + cancers.stream().map( Mention::getClassUri )
//                               .sorted().collect( Collectors.joining(";") ) );
         for ( Mention cancer : cancers ) {
            final Map<String,Collection<Mention>> siteUriSites = neoplasmSiteUriSitesMap.get( cancer );
            siteUriSites.keySet().retainAll( siteChain.getValue() );
            siteUriSites.forEach( (k,v) -> neoplasmSiteUriSites.get( k )
                                                               .addAll( v ) );
         }
         final Collection<Mention> tumors = siteChain.getValue()
                                                     .stream()
                                                     .map( siteUriTumorsMap::get )
                                                     .filter( Objects::nonNull )
                                                     .flatMap( Collection::stream )
                                                     .collect( Collectors.toSet() );
//         LOGGER.info( "SiteTypeBin.createSiteNeoplasmBins " + _siteType + " Tumor Mentions : "
//                      + tumors.stream().map( Mention::getClassUri )
//                              .sorted().collect( Collectors.joining(";") ) );
         for ( Mention tumor : tumors ) {
            final Map<String,Collection<Mention>> siteUriSites = neoplasmSiteUriSitesMap.get( tumor );
            siteUriSites.keySet().retainAll( siteChain.getValue() );
            siteUriSites.forEach( (k,v) -> neoplasmSiteUriSites.get( k )
                                                               .addAll( v ) );
         }
//         LOGGER.info( "SiteNeoplasmBin Sites " + siteChain.getKey() + " : " + String.join( ";",
//                                                                                           neoplasmSiteUriSites.keySet() ) );
         final SiteNeoplasmBin siteNeoplasmBin = new SiteNeoplasmBin( siteChain.getKey(),
                                                                      neoplasmSiteUriSites,
                                                                      cancers,
                                                                      tumors );
         _siteNeoplasmBins.add( siteNeoplasmBin );
      }
   }


   // For neoplasms that are in 2 different sites,
   // try to resolve to the best sites and remove the mention from the not best sites.
   void resolveMentionConflicts( final NeoplasmType neoplasmType ) {
      final Map<Mention,Collection<NeoplasmChain>> mentionNeoplasmChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         for ( NeoplasmChain neoplasmChain : siteNeoplasmBin.getNeoplasmChains( neoplasmType ) ) {
            neoplasmChain.getAllMentions()
                         .forEach( m -> mentionNeoplasmChainsMap.computeIfAbsent( m, b -> new HashSet<>() )
                                                       .add( neoplasmChain ) );
         }
      }
      final Collection<Mention> handled = new HashSet<>();
      boolean change = false;
      for ( Map.Entry<Mention,Collection<NeoplasmChain>> mentionNeoplasmChains : mentionNeoplasmChainsMap.entrySet() ) {
         if ( mentionNeoplasmChains.getValue().size() == 1 || handled.contains( mentionNeoplasmChains.getKey() ) ) {
            // Mention is only in a single chain. or was already dealt with.
            continue;
         }
         // Mention is in multiple sites.  Attempt to find the best one.
         final Mention mention = mentionNeoplasmChains.getKey();
         final Map<Long,List<NeoplasmChain>> scoredChains
               = mentionNeoplasmChains.getValue()
                                      .stream()
                                      .collect( Collectors.groupingBy( c -> c.scoreNeoplasmMentionMatch( mention ) ) );
         if ( scoredChains.size() == 1 ) {
            // All the neoplasm chains have the same score.
            continue;
         }

//         LOGGER.info( "resolveMentionConflicts scoredChains: "
//                      + scoredChains.entrySet()
//                                    .stream()
//                                    .map( e -> "\n  Score " + e.getKey() + "\n   for\n"
//                                               + e.getValue().stream()
//                                                  .map( NeoplasmChain::toString )
//                                                  .collect( Collectors.joining("\n  " ) ) )
//                                    .collect( Collectors.joining("\n" ) ) );


         change = true;
         final Collection<NeoplasmChain> bestChains
               = scoredChains.entrySet()
                              .stream()
                              .max( Comparator.comparingLong( Map.Entry::getKey ) )
                             .map( Map.Entry::getValue )
                             .orElse( Collections.emptyList() );
         final Collection<NeoplasmChain> worseChains = scoredChains.values()
                                                                   .stream()
                                                                   .flatMap( Collection::stream )
                                                                   .filter( c -> !bestChains.contains( c ) )
                                                                   .collect( Collectors.toSet() );
//         LOGGER.info( "SiteTypeBin.resolveMentionConflicts " + _siteType + " Moving Mention: " + mention.getClassUri() + " "
//                      + "from\n"
//                      + worseChains.stream()
//                                   .map( NeoplasmChain::toString )
//                                   .sorted()
//                                    .collect( Collectors.joining("\n" ) ) + "\nto\n"
//                      + bestChains.stream()
//                                    .map( NeoplasmChain::toString )
//                                  .sorted()
//                                    .collect( Collectors.joining("\n" ) ));

         bestChains.forEach( c -> c.copyHere( mention ) );
         worseChains.forEach( c -> c.remove( mention ) );

         final String uri = mention.getClassUri();
         final Collection<Mention> moreMentions = worseChains.stream()
                                                             .map( c -> c.getMentions( uri ) )
                                                             .flatMap( Collection::stream )
                                                             .collect( Collectors.toSet() );
         if ( !moreMentions.isEmpty() ) {
//            LOGGER.info( "SiteTypeBin.resolveMentionConflicts " + _siteType
//                         + " Moving more mentions with the same URI : " + moreMentions.size() );
            for ( Mention moreMention : moreMentions ) {
               bestChains.forEach( c -> c.copyHere( moreMention ) );
            }
            worseChains.forEach( c -> c.remove( moreMentions ) );
            handled.addAll( moreMentions );
         }
      }
      if ( change ) {
         clean();
      }
//      LOGGER.info( "SiteTypeBin.resolveMentionConflicts " + _siteType
//                   +  " Post-Clean Remaining " + neoplasmType + " Mentions: "
//                 + getSiteNeoplasmBins().stream()
//                                        .map( n -> n.getNeoplasmChains( neoplasmType ) )
//                                        .flatMap( Collection::stream )
//                                        .map( NeoplasmChain::getAllMentions )
//                                        .flatMap( Collection::stream )
//                                        .map( Mention::getClassUri )
//                                        .sorted()
//                                        .collect( Collectors.joining( ";" ) ) );
   }


   // For neoplasms that are in 2 different sites,
   // try to resolve to the best sites and remove the mention from the not best sites.
   void resolveMentionSiteConflicts( final NeoplasmType neoplasmType ) {
      final Map<Mention,Collection<NeoplasmChain>> mentionNeoplasmChainsMap = new HashMap<>();
      final Map<NeoplasmChain,Integer> neoplasmToSiteChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         for ( NeoplasmChain neoplasmChain : siteNeoplasmBin.getNeoplasmChains( neoplasmType ) ) {
            neoplasmChain.getAllMentions()
                         .forEach( m -> mentionNeoplasmChainsMap.computeIfAbsent( m, b -> new HashSet<>() )
                                                                .add( neoplasmChain ) );
            neoplasmToSiteChainsMap.put( neoplasmChain, siteNeoplasmBin.getSiteChain().getAllMentions().size() );
         }
      }
      final Collection<Mention> handled = new HashSet<>();
      boolean change = false;
      for ( Map.Entry<Mention,Collection<NeoplasmChain>> mentionNeoplasmChains : mentionNeoplasmChainsMap.entrySet() ) {
         if ( mentionNeoplasmChains.getValue().size() == 1 || handled.contains( mentionNeoplasmChains.getKey() ) ) {
            // Mention is only in a single chain. or was already dealt with.
            continue;
         }
         // Mention is in multiple sites.  Attempt to find the best one.
         final Mention mention = mentionNeoplasmChains.getKey();
         final Map<Long,List<NeoplasmChain>> scoredChains
               = mentionNeoplasmChains.getValue()
                                      .stream()
                                      .collect( Collectors.groupingBy( c -> c.scoreNeoplasmMentionMatch( mention ) ) );
         if ( scoredChains.size() == 1 ) {
            // All the neoplasm chains have the same score.
            continue;
         }

//         LOGGER.info( "resolveMentionConflicts scoredChains: "
//                      + scoredChains.entrySet()
//                                    .stream()
//                                    .map( e -> "\n  Score " + e.getKey() + "\n   for\n"
//                                               + e.getValue().stream()
//                                                  .map( NeoplasmChain::toString )
//                                                  .collect( Collectors.joining("\n  " ) ) )
//                                    .collect( Collectors.joining("\n" ) ) );


         change = true;
         final Collection<NeoplasmChain> bestChains
               = scoredChains.entrySet()
                             .stream()
                             .max( Comparator.comparingLong( Map.Entry::getKey ) )
                             .map( Map.Entry::getValue )
                             .orElse( Collections.emptyList() );
         final Collection<NeoplasmChain> worseChains = scoredChains.values()
                                                                   .stream()
                                                                   .flatMap( Collection::stream )
                                                                   .filter( c -> !bestChains.contains( c ) )
                                                                   .collect( Collectors.toSet() );
//         LOGGER.info( "\nMoving Mention " + mention.getClassUri() + " from\n"
//                      + worseChains.stream()
//                                   .map( NeoplasmChain::toString )
//                                    .collect( Collectors.joining("\n" ) ) + " to\n"
//                      + bestChains.stream()
//                                    .map( NeoplasmChain::toString )
//                                    .collect( Collectors.joining("\n" ) ));

         bestChains.forEach( c -> c.copyHere( mention ) );
         worseChains.forEach( c -> c.remove( mention ) );

         final String uri = mention.getClassUri();
         final Collection<Mention> moreMentions = worseChains.stream()
                                                             .map( c -> c.getMentions( uri ) )
                                                             .flatMap( Collection::stream )
                                                             .collect( Collectors.toSet() );
         if ( !moreMentions.isEmpty() ) {
//            LOGGER.info( "Moving more mentions with the same URI : " + moreMentions.size() );
            for ( Mention moreMention : moreMentions ) {
               bestChains.forEach( c -> c.copyHere( moreMention ) );
            }
            worseChains.forEach( c -> c.remove( moreMentions ) );
            handled.addAll( moreMentions );
         }

      }
      if ( change ) {
         clean();
      }
   }


   void clean() {
      _siteNeoplasmBins.forEach( SiteNeoplasmBin::clean );
      final Collection<SiteNeoplasmBin> invalids = _siteNeoplasmBins.stream()
                                                                   .filter( b -> !b.isValid() )
                                                                   .collect( Collectors.toSet() );
      _siteNeoplasmBins.removeAll( invalids );
   }

   void clear() {
      _siteNeoplasmBins.clear();
   }


   /**
    * A kludge to combine differing quadrant sites with some dominant breast site.
    */
   void mergeQuadrants() {
      final Collection<String> quadrantUris = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT );
      final Collection<String> breastUris = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.BREAST );
      final Collection<SiteNeoplasmBin> quadrants
            = getSiteNeoplasmBins().stream()
                                   .filter( b -> quadrantUris.contains( b.getSiteChain().getHeadUri() ) )
                                   .collect( Collectors.toSet() );
      boolean copied = false;
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         if ( quadrants.contains( siteNeoplasmBin ) ) {
            continue;
         }
         if ( breastUris.contains( siteNeoplasmBin.getSiteChain().getHeadUri() ) ) {
            quadrants.forEach( siteNeoplasmBin::copyHere );
            copied = true;
         }
      }
      if ( copied ) {
         quadrants.forEach( SiteNeoplasmBin::invalidate );
         clean();
      }
   }


   void mergeExtents( final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
      getSiteNeoplasmBins().stream()
                           .filter( SiteNeoplasmBin::isValid )
                           .forEach( b -> b.mergeExtents( mentionRelations ) );
      clean();
   }


   void mergeExtents(
         final SiteTypeBin otherSiteTypeBin,
         final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         if ( !siteNeoplasmBin.isValid() ) {
            continue;
         }
         for ( SiteNeoplasmBin otherSiteNeoplasmBin : otherSiteTypeBin.getSiteNeoplasmBins() ) {
            siteNeoplasmBin.mergeExtents( otherSiteNeoplasmBin, mentionRelations );
         }
      }
      clean();
      otherSiteTypeBin.clean();
   }


   Map<NeoplasmChain,Map<Integer,Collection<SiteNeoplasmBin>>> scoreBestCancerSites() {
      final Map<NeoplasmChain,Map<Integer,Collection<SiteNeoplasmBin>>> bestMatchingBinsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         int maxBinMentions = 0;
         for ( NeoplasmChain neoplasmChain : siteNeoplasmBin.getNeoplasmChains( CANCER ) ) {
            maxBinMentions = Math.max( maxBinMentions, neoplasmChain.getAllMentions().size() );
         }
         for ( NeoplasmChain neoplasmChain : siteNeoplasmBin.getNeoplasmChains( CANCER ) ) {
            bestMatchingBinsMap.computeIfAbsent( neoplasmChain, c -> new HashMap<>() )
                               .computeIfAbsent( maxBinMentions, m -> new HashSet<>() )
                               .add( siteNeoplasmBin );
         }
      }
      return bestMatchingBinsMap;
   }





   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreBestMatchingSiteNeoplasmBins(
         final SiteTypeBin otherSiteTypeBin ) {
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap
            = scoreMatchingSiteNeoplasmBins( otherSiteTypeBin );
      return getBestScoresMatchingSiteNeoplasmBins( matchingBinsMap );
   }


//   Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoreBestMatchingSiteNeoplasmBins(
//         final SiteTypeBin otherSiteTypeBin ) {
//      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap =
//            scoreMatchingSiteNeoplasmBins( otherSiteTypeBin );
//      return getBestScoresMatchingSiteNeoplasmBins( matchingBinsMap );
//   }

//   Map<SiteNeoplasmBin, Collection<SiteNeoplasmBin>> getBestMatchingSiteNeoplasmBins(
//         final SiteTypeBin otherSiteTypeBin ) {
//      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap =
//            scoreMatchingSiteNeoplasmBins( otherSiteTypeBin );
//      return getBestScoresMatchingSiteNeoplasmBins( matchingBinsMap );
//   }



   // For moving no-site neoplasm chains here.
   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreMatchingSiteNeoplasmBins(
         final SiteTypeBin otherSiteTypeBin ) {
      final Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap = new HashMap<>();
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = otherSiteTypeBin.getSiteNeoplasmBins();
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bestMatches
               = siteNeoplasmBin.scoreBestMatchingSites( otherSiteNeoplasmBins );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         matchingBinsMap.put( siteNeoplasmBin, bestMatches );
      }
      return matchingBinsMap;
   }

   /**
    * Given Bin A and scores to collections of Bin B, returns a map of Bins B and each Bin A score
    * @param matchingBinsMap -
    * @return -
    */
   static Map<SiteNeoplasmBin, Map<SiteNeoplasmBin,Long>> createReverseScoredMap(
         final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap) {
      final Map<SiteNeoplasmBin, Map<SiteNeoplasmBin,Long>> reverseMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBins
            : matchingBinsMap.entrySet() ) {
         final SiteNeoplasmBin siteNeoplasmBin = matchingBins.getKey();
         for ( Map.Entry<Long,Collection<SiteNeoplasmBin>> scoreMap : matchingBins.getValue()
                                                                                  .entrySet() ) {
            final Long score = scoreMap.getKey();
            for ( SiteNeoplasmBin otherBin : scoreMap.getValue() ) {
               reverseMap.computeIfAbsent( otherBin, b -> new HashMap<>() )
                         .put( siteNeoplasmBin, score );
            }
         }
      }
      return reverseMap;
   }



   static Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> createReverseScoreMap(
         final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap) {
      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> reverseMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBins
            : matchingBinsMap.entrySet() ) {
         final SiteNeoplasmBin siteNeoplasmBin = matchingBins.getKey();
         for ( Map.Entry<Long,Collection<SiteNeoplasmBin>> scoreMap : matchingBins.getValue()
                                                                                  .entrySet() ) {
            final Long score = scoreMap.getKey();
            for ( SiteNeoplasmBin otherBin : scoreMap.getValue() ) {
               reverseMap.computeIfAbsent( otherBin, b -> new HashMap<>() )
                         .computeIfAbsent( score, s -> new HashSet<>() ).add( siteNeoplasmBin );
            }
         }
      }
      return reverseMap;
   }

   private Map<SiteNeoplasmBin,Long> createReverseMaxScoreMap(
         final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap) {
      final Map<SiteNeoplasmBin,Long> reverseScoreMap = new HashMap<>();
      for ( Map<Long,Collection<SiteNeoplasmBin>> matchingBins : matchingBinsMap.values() ) {
         for ( Map.Entry<Long, Collection<SiteNeoplasmBin>> scoredBins
               : matchingBins.entrySet() ) {
             final Long score = scoredBins.getKey();
            for ( SiteNeoplasmBin otherBin : scoredBins.getValue() ) {
               final long reverseScore = reverseScoreMap.getOrDefault( otherBin, 0L );
               reverseScoreMap.put( otherBin, reverseScore+score );
            }
         }
      }
      return reverseScoreMap;
   }



   static private final long SNB_MATCH_FACTOR = 2;

//   /**
//    * For distributing unsited neoplasms to sited neoplasms.
//    * @param matchingBinsMap -
//    * @return -
//    */
//   Map<SiteNeoplasmBin, Collection<SiteNeoplasmBin>> getBestScoresMatchingSiteNeoplasmBins(
//         final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap ) {
////      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> reverseMatchingBinsMap
////            = createReverseScoreMap( matchingBinsMap );
////      final Map<SiteNeoplasmBin,SiteNeoplasmBin> alreadyMatchedChains = new HashMap<>();
//      final Map<SiteNeoplasmBin,Long> reverseMaxScores = createReverseMaxScoreMap( matchingBinsMap );
//      final Map<SiteNeoplasmBin, Collection<SiteNeoplasmBin>> bestMatchingBinsMap = new HashMap<>();
//
//      for ( Map.Entry<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> matchingBins
//            : matchingBinsMap.entrySet() ) {
//         final SiteNeoplasmBin siteNeoplasmBin = matchingBins.getKey();
//         for ( Map.Entry<Long, Collection<SiteNeoplasmBin>> scoredBins : matchingBins.getValue()
//                                                                                     .entrySet() ) {
//            final long score = scoredBins.getKey();
//            for ( SiteNeoplasmBin otherBin : scoredBins.getValue() ) {
//               final long reverseMaxScore = reverseMaxScores.get( otherBin );
//               if ( score * SNB_MATCH_FACTOR >= reverseMaxScore ) {
//                  bestMatchingBinsMap.computeIfAbsent( siteNeoplasmBin, b -> new HashSet<>() )
//                                     .add( otherBin );
//               }
//            }
//         }
//      }
//      return bestMatchingBinsMap;
//   }
//
//
//
   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> getBestScoresMatchingSiteNeoplasmBins(
         final Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> bestMatchingBinsMap ) {

//      LOGGER.info( "getBestScoresMatchingSiteNeoplasmBins bestMatchingChainsMap:" );
//      bestMatchingBinsMap.forEach( (k,v) -> LOGGER.info( "\nChain " + k.toString() + " Score: "
//                                                           + v.getKey() + "\n  "
//                                                           + v.getValue().stream()
//                                                              .map( SiteNeoplasmBin::toString )
//                                                              .collect( Collectors.joining("\n  " ) ) ) );

      Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> adjustedMatchingBinsMap = new HashMap<>();
      final Map<SiteNeoplasmBin,Long> bestOpposingScores = getBestOpposingScores( bestMatchingBinsMap );

//      LOGGER.info( "getBestScoresMatchingSiteNeoplasmBins bestOpposingScores:" );
//      bestOpposingScores.forEach( (k,v) -> LOGGER.info( v + " : " + k.toString() ) );

      final Map<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> alreadyMatchedBins = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> bestMatchingBins
            : bestMatchingBinsMap.entrySet() ) {
         final Long score = bestMatchingBins.getValue().getKey();
         if ( score <= 0 ) {
            continue;
         }
         final Collection<SiteNeoplasmBin> bestBins = new HashSet<>();
         final List<SiteNeoplasmBin> otherBins = new ArrayList<>( bestMatchingBins.getValue()
                                                                     .getValue() );
         if ( otherBins.size() == 1 ) {
            final Collection<SiteNeoplasmBin> finalMatches
                  = alreadyMatchedBins.computeIfAbsent( otherBins.get( 0 ), b-> otherBins );
            alreadyMatchedBins.put( otherBins.get( 0 ), finalMatches );
            // There was only one best match.  Go ahead and use it.
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, finalMatches ) );
            continue;
         }
         // There were multiple possible best matches.
         for ( SiteNeoplasmBin otherBin : bestMatchingBins.getValue()
                                                        .getValue() ) {
            final Long bestScore = bestOpposingScores.get( otherBin );
            if ( bestScore != null && bestScore.equals( score ) ) {
               final Collection<SiteNeoplasmBin> finalMatches
                     = alreadyMatchedBins.computeIfAbsent( otherBin,
                                                           b -> new ArrayList<>( Collections.singletonList( otherBin ) ) );
               bestBins.addAll( finalMatches );
            }
         }
         if ( bestBins.isEmpty() ) {
            final Collection<SiteNeoplasmBin> finalMatches
                  = otherBins.stream()
                             .map( b -> alreadyMatchedBins.computeIfAbsent( b,
                                                                            c -> new ArrayList<>( Collections.singletonList( b ) ) ) )
                             .flatMap( Collection::stream )
                             .collect( Collectors.toSet() );
            // None of those best matches also had this as a best match.  Use them all.
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, finalMatches ) );
         } else {
            // One or more of those best matches also picks this as a best match.  Use only it/them.
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, bestBins ) );
         }
      }

//      LOGGER.info( "getBestScoresMatchingSiteNeoplasmBins " + _siteType + " adjustedMatchingBinsMap:" );
//      adjustedMatchingBinsMap.forEach( (k,v) -> LOGGER.info( "\nBin " + k.toString() + " Score: "
//                                                             + v.getKey() + "\n    matches Bins\n"
//                                                             + v.getValue().stream()
//                                                                .map( SiteNeoplasmBin::toString )
//                                                                .collect( Collectors.joining("\n  " ) ) ) );

      return adjustedMatchingBinsMap;
   }



   ////  TODO  - don't use keyvalue for scoring.
   // Score A : BB = 100, Score B : BB = 200   -->  Want to merge A and BB, B and BB  ===> A, B, BB
   // A : BB = 100,  B : BB = 200,  A : DD = 400   -->  Want to merge A and DD, B and BB ... are A and BB are still ok?
   // Anyway, keep track of "alreadyMatched".  Use i++, j++ for matching instead of the current object.score( object ).
   // Doing this should allow chains to be built without some secondary match getting lost.
   // Site, NeoplasmChain, SiteNeoplasmChain.   Straight, By Roots.  Mentions.

   // For moving no-site neoplasm chains here.
//   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreMatchingSiteNeoplasmBins(
//   Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoreMatchingSiteNeoplasmBins(
//         final SiteTypeBin otherSiteTypeBin ) {
//      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> binScoresMap = new HashMap<>();
//      final List<SiteNeoplasmBin> binList = new ArrayList<>( getSiteNeoplasmBins() );
//      final List<SiteNeoplasmBin> otherBinList = new ArrayList<>( otherSiteTypeBin.getSiteNeoplasmBins() );
//      for ( final SiteNeoplasmBin siteNeoplasmBin : binList ) {
//         if ( !siteNeoplasmBin.isValid() ) {
//            continue;
//         }
//         for ( final SiteNeoplasmBin otherBin : otherBinList ) {
//            if ( !otherBin.isValid() ) {
//               continue;
//            }
//            final long score = siteNeoplasmBin.scoreSiteMatch( otherBin );
//            if ( score <= 0 ) {
//               continue;
//            }
//            binScoresMap.computeIfAbsent( siteNeoplasmBin, b -> new HashMap<>() )
//                        .computeIfAbsent( score, s-> new HashSet<>() )
//                        .add( otherBin );
//         }
//      }
//      return binScoresMap;
//   }


//   void mergeExtents( final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
//      if ( !isValid() || getNeoplasmChains( TUMOR ).size() < 2 ) {
//         return;
//      }
//      final Map<NeoplasmChain,NeoplasmChain> alreadyMatchedChains = new HashMap<>();
//      final Map<NeoplasmChain,Collection<NeoplasmChain>> matchingChainsMap = new HashMap<>();
//      final List<NeoplasmChain> chainList = new ArrayList<>( getNeoplasmChains( TUMOR ) );
//      for ( int i=0; i<chainList.size()-1; i++ ) {
//         final NeoplasmChain neoplasmChain = chainList.get( i );
//         if ( !neoplasmChain.isValid() ) {
//            continue;
//         }
//         for ( int j=i+1; j<chainList.size(); j++ ) {
//            final NeoplasmChain otherChain = chainList.get( j );
//            if ( otherChain.isValid()
//                 && neoplasmChain.scoreExtentMatch( otherChain, mentionRelations ) > 0 ) {
//               final NeoplasmChain finalMatch
//                     = alreadyMatchedChains.computeIfAbsent( otherChain, c -> neoplasmChain );
//               matchingChainsMap.computeIfAbsent( finalMatch, c -> new HashSet<>() )
//                                .add( otherChain );
//            }
//         }
//      }
//      for ( Map.Entry<NeoplasmChain,Collection<NeoplasmChain>> matchingChains : matchingChainsMap.entrySet() ) {
//         matchingChains.getKey().copyHere( matchingChains.getValue() );
//         matchingChains.getValue().forEach( NeoplasmChain::invalidate );
//      }
//      if ( !matchingChainsMap.isEmpty() ) {
//         clean();
//      }
//   }






   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreBestMatchingSiteNeoplasmBinsByRoots(
         final SiteTypeBin otherSiteTypeBin,
         final Collection<SiteNeoplasmBin> notLocated,
         final Map<String,Collection<String>> allUriRoots,
         final Map<String,Collection<String>> allUriBranches ) {
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap
            = scoreMatchingSiteNeoplasmBinsByRoots( otherSiteTypeBin, notLocated, allUriRoots, allUriBranches );
      return getBestScoresMatchingSiteNeoplasmBins( matchingBinsMap );
   }


   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreMatchingSiteNeoplasmBinsByRoots(
         final SiteTypeBin otherSiteTypeBin,
         final Collection<SiteNeoplasmBin> notLocated,
         final Map<String,Collection<String>> allUriRoots,
         final Map<String,Collection<String>> allUriBranches ) {
      final Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap = new HashMap<>();
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = otherSiteTypeBin.getSiteNeoplasmBins();
//      LOGGER.info( "SiteTypeBin.scoreMatchingSiteNeoplasmBinsByRoots Other Site Neoplasms ...\n"
//                   + otherSiteNeoplasmBins.stream().map( SiteNeoplasmBin::toString ).collect( Collectors.joining( "\n" ) ) );
//      LOGGER.info( "SiteTypeBin.scoreMatchingSiteNeoplasmBinsByRoots Retained Other Site Neoplasms ...\n"
//                   + otherSiteNeoplasmBins.stream().map( SiteNeoplasmBin::toString ).collect( Collectors.joining( "\n" ) ) );
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         if ( !notLocated.contains( siteNeoplasmBin ) ) {
            continue;
         }
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bestMatches
               = siteNeoplasmBin.scoreBestMatchingSitesByRoots( otherSiteNeoplasmBins, allUriRoots, allUriBranches );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         matchingBinsMap.put( siteNeoplasmBin, bestMatches );
      }
      return matchingBinsMap;
   }



   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmChains(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin ) {
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> matchingBinsMap
            = scoreMatchingNeoplasmChains( neoplasmType, otherSiteTypeBin );
      return getBestScoresMatchingNeoplasmChains( matchingBinsMap );
   }


   // For moving no-site neoplasm chains here.
   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreMatchingNeoplasmChains(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin ) {
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = otherSiteTypeBin.getSiteNeoplasmBins();
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap
               = siteNeoplasmBin.scoreBestMatchingNeoplasms( neoplasmType, otherSiteNeoplasmBins );

         for ( Map.Entry<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchEntry :
               bestMatchMap.entrySet() ) {
            final KeyValue<Long, Collection<NeoplasmChain>> bestMatch =
                  bestMatchingChainsMap.get( bestMatchEntry.getKey() );
            if ( bestMatch == null ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
               continue;
            }
            if ( bestMatchEntry.getValue()
                               .getKey() > bestMatch.getKey() ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
            } else if ( bestMatchEntry.getValue()
                                      .getKey()
                                      .equals( bestMatch.getKey() ) ) {
               bestMatch.getValue()
                        .addAll( bestMatchEntry.getValue()
                                               .getValue() );
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatch );
            }
         }
      }
      return bestMatchingChainsMap;
   }


   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> getBestScoresMatchingNeoplasmChains(
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap ) {

//      LOGGER.info( "getBestScoresMatchingNeoplasmChains bestMatchingChainsMap:" );
//      bestMatchingChainsMap.forEach( (k,v) -> LOGGER.info( "Chain " + k.toString() + " Score: "
//                                                   + v.getKey() + "\n  "
//                                                   + v.getValue().stream()
//                                                      .map( NeoplasmChain::toString )
//                                                      .collect( Collectors.joining("\n  " ) ) ) );

      Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> adjustedMatchingBinsMap = new HashMap<>();
      final Map<NeoplasmChain,Long> bestOpposingScores = getBestOpposingChainScores( bestMatchingChainsMap );

//      LOGGER.info( "getBestScoresMatchingNeoplasmChains bestOpposingScores:" );
//      bestOpposingScores.forEach( (k,v) -> LOGGER.info( v + " : " + k.toString() ) );

      for ( Map.Entry<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingBins
            : bestMatchingChainsMap.entrySet() ) {
         final Long score = bestMatchingBins.getValue().getKey();
         if ( score <= 0 ) {
            continue;
         }
         final Collection<NeoplasmChain> bestBins = new HashSet<>();
         final Collection<NeoplasmChain> otherBins = bestMatchingBins.getValue()
                                                                     .getValue();
         if ( otherBins.size() == 1 ) {
            // There was only one best match.  Go ahead and use it.
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, otherBins ) );
            continue;
         }
         // There were multiple possible best matches.
         for ( NeoplasmChain otherBin : bestMatchingBins.getValue()
                                                          .getValue() ) {
            final Long bestScore = bestOpposingScores.get( otherBin );
            if ( bestScore != null && bestScore.equals( score ) ) {
               bestBins.add( otherBin );
            }
         }
         if ( bestBins.isEmpty() ) {
            // None of those best matches also had this as a best match.  Use them all.
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, otherBins ) );
         } else {
            // One or more of those best matches also picks this as a best match.  Use only it/them.
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, bestBins ) );
         }
      }

//      LOGGER.info( "getBestScoresMatchingNeoplasmChains " + _siteType + " adjustedMatchingBinsMap:" );
//      adjustedMatchingBinsMap.forEach( (k,v) -> LOGGER.info( "\n   Chain " + k.toString() + " Score: "
//                                                           + v.getKey() + "\n     matches chains\n"
//                                                           + v.getValue().stream()
//                                                              .map( NeoplasmChain::toString )
//                                                              .collect( Collectors.joining("\n  " ) ) ) );

      return adjustedMatchingBinsMap;
   }



   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmChainsByRoots(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin,
         final Map<String,Collection<String>> allUriRoots ) {
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> matchingBinsMap
            = scoreMatchingNeoplasmChainsByRoots( neoplasmType, otherSiteTypeBin, allUriRoots );
      return getBestScoresMatchingNeoplasmChains( matchingBinsMap );
   }


   // For moving no-site neoplasm chains here.
   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreMatchingNeoplasmChainsByRoots(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin,
         final Map<String,Collection<String>> allUriRoots ) {
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = otherSiteTypeBin.getSiteNeoplasmBins();
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap
               = siteNeoplasmBin.scoreBestMatchingNeoplasmsByRoots( neoplasmType, otherSiteNeoplasmBins, allUriRoots );

         for ( Map.Entry<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchEntry :
               bestMatchMap.entrySet() ) {
            final KeyValue<Long, Collection<NeoplasmChain>> bestMatch =
                  bestMatchingChainsMap.get( bestMatchEntry.getKey() );
            if ( bestMatch == null ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
               continue;
            }
            if ( bestMatchEntry.getValue()
                               .getKey() > bestMatch.getKey() ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
            } else if ( bestMatchEntry.getValue()
                                      .getKey()
                                      .equals( bestMatch.getKey() ) ) {
               bestMatch.getValue()
                        .addAll( bestMatchEntry.getValue()
                                               .getValue() );
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatch );
            }
         }
      }
      return bestMatchingChainsMap;
   }






   static Map<SiteNeoplasmBin,Long> getBestOpposingScores( final Map<SiteNeoplasmBin,KeyValue<Long,
         Collection<SiteNeoplasmBin>>> bestMatchingBinsMap ) {
      final Map<SiteNeoplasmBin,Long> bestScores = new HashMap<>();
      for ( KeyValue<Long,Collection<SiteNeoplasmBin>> bestMatchingBins
            : bestMatchingBinsMap.values() ) {
         if ( bestMatchingBins.getKey() <= 0 ) {
            continue;
         }
         final Long score = bestMatchingBins.getKey();
         for ( SiteNeoplasmBin oppositeBin : bestMatchingBins.getValue() ) {
            Long bestScore = bestScores.get( oppositeBin );
            if ( bestScore == null || bestScore < score ) {
               bestScores.put( oppositeBin, score );
            }
         }
      }
      return bestScores;
   }



   static Map<NeoplasmChain,Long> getBestOpposingChainScores( final Map<NeoplasmChain,KeyValue<Long,
         Collection<NeoplasmChain>>> bestMatchingChainsMap ) {
      final Map<NeoplasmChain,Long> bestScores = new HashMap<>();
      for ( KeyValue<Long,Collection<NeoplasmChain>> bestMatchingChains
            : bestMatchingChainsMap.values() ) {
         if ( bestMatchingChains.getKey() <= 0 ) {
            continue;
         }
         final Long score = bestMatchingChains.getKey();
         for ( NeoplasmChain oppositeBin : bestMatchingChains.getValue() ) {
            Long bestScore = bestScores.get( oppositeBin );
            if ( bestScore == null || bestScore < score ) {
               bestScores.put( oppositeBin, score );
            }
         }
      }
      return bestScores;
   }




   Collection<SiteNeoplasmBin> getSiteNeoplasmBins() {
      return _siteNeoplasmBins.stream()
                              .filter( SiteNeoplasmBin::isValid )
                              .collect( Collectors.toSet() );
   }


   public String toString() {
      return "SiteTypeBin site neoplasm chains :\n"
             + getSiteNeoplasmBins().stream()
                                .map( SiteNeoplasmBin::toString )
                                .collect( Collectors.joining( "\n" ) );
   }


}
