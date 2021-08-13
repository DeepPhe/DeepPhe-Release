package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.concept.bin.LateralityType.NO_LATERALITY;
import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.CANCER;
import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.TUMOR;
import static org.healthnlp.deepphe.summary.concept.bin.SiteType.ALL_SITES;
import static org.healthnlp.deepphe.summary.concept.bin.SiteType.NO_SITE;

/**
 * @author SPF , chip-nlp
 * @since {5/24/2021}
 */
final public class LateralityTypeBin {

   static private final Logger LOGGER = Logger.getLogger( "LateralityTypeBin" );

   static public final String NO_LATERALITY_URI = "NO_LATERALITY_URI";

   private final LateralityType _lateralityType;

   private final Map<SiteType,SiteTypeBin> _siteTypeBins = new EnumMap<>( SiteType.class );

   public LateralityTypeBin( final LateralityType lateralityType ) {
      _lateralityType = lateralityType;
   }

   static Map<LateralityType,Collection<Mention>> getLateralityTypes(
         final Collection<Mention> neoplasms,
         final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      final Map<LateralityType,Collection<Mention>> lateralityTypes = new EnumMap<>( LateralityType.class );
      lateralityTypes.put( NO_LATERALITY, neoplasms );

//      for ( Mention neoplasm : neoplasms ) {
//         final Map<String, Collection<Mention>> relations = relationsMap.get( neoplasm );
//         LateralityType.getLateralityTypes( relations )
//                 .forEach( l -> lateralityTypes.computeIfAbsent( l, b -> new HashSet<>() )
//                                            .add( neoplasm ) );
//      }
      return lateralityTypes;
   }

   void clean() {
      getOrCreateSiteTypeBins().values()
                               .forEach( SiteTypeBin::clean );
   }

   void clear() {
      getOrCreateSiteTypeBins().values()
                               .forEach( SiteTypeBin::clear );
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<String,Set<Mention>> uriMentionsMap,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      clear();
      final Map<SiteType,Collection<Mention>> siteTypeCancersMap
            = SiteTypeBin.getSiteTypeMentions( cancers, relationsMap );
      final Map<SiteType,Collection<Mention>> siteTypeTumorsMap
            = SiteTypeBin.getSiteTypeMentions( tumors, relationsMap );
      for ( SiteType siteType : SiteType.values() ) {
//         LOGGER.info( "LateralityTypeBin.setNeoplasms Line #65 " + _lateralityType + " " + siteType );
         _siteTypeBins.get( siteType )
                      .setNeoplasms( siteTypeCancersMap.getOrDefault( siteType, new HashSet<>() ),
                                     siteTypeTumorsMap.getOrDefault( siteType, new HashSet<>() ),
                                     uriMentionsMap,
                                     relationsMap );

      }
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<String,Set<Mention>> uriMentionsMap,
                      final Map<String,Collection<String>> associatedSitesMap,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      clear();
      final Map<SiteType,Collection<Mention>> siteTypeCancersMap
            = SiteTypeBin.getSiteTypeMentions( cancers, relationsMap );
      final Map<SiteType,Collection<Mention>> siteTypeTumorsMap
            = SiteTypeBin.getSiteTypeMentions( tumors, relationsMap );
      for ( SiteType siteType : SiteType.values() ) {
//         LOGGER.info( "LateralityTypeBin.setNeoplasms Line #65 " + _lateralityType + " " + siteType );
         _siteTypeBins.get( siteType )
                      .setNeoplasms( siteTypeCancersMap.getOrDefault( siteType, new HashSet<>() ),
                                     siteTypeTumorsMap.getOrDefault( siteType, new HashSet<>() ),
                                     uriMentionsMap,
                                     associatedSitesMap,
                                     relationsMap );

      }
   }


   LateralityType getLateralityType() {
      return _lateralityType;
   }

   SiteTypeBin getSiteTypeBin( final SiteType siteType ) {
      return getOrCreateSiteTypeBins().get( siteType );
   }

   private Map<SiteType,SiteTypeBin> getOrCreateSiteTypeBins() {
      if ( _siteTypeBins.isEmpty() ) {
         _siteTypeBins.put( NO_SITE, new SiteTypeBin( NO_SITE ) );
         _siteTypeBins.put( ALL_SITES, new SiteTypeBin( ALL_SITES ) );
      }
      return _siteTypeBins;
   }

   void distributeSites( final Map<String, Collection<String>> allUriRoots ) {
      distributeNoSites( CANCER, allUriRoots );
      distributeNoSites( TUMOR, allUriRoots );
      _siteTypeBins.get( NO_SITE ).clean();
   }

//   void resolveMentionConflicts() {
//      _siteTypeBins.values().forEach( b -> b.resolveMentionConflicts( CANCER ) );
//      _siteTypeBins.values().forEach( b -> b.resolveMentionConflicts( TUMOR ) );
//   }
//
//   void mergeQuadrants() {
//      _siteTypeBins.get( ALL_SITES ).mergeQuadrants();
//   }


   void distributeNoSites( final NeoplasmType neoplasmType, final Map<String, Collection<String>> allUriRoots ) {
//      LOGGER.info( "!!! Laterality Type Bin " + _lateralityType.name() + " Distributing UnSited "
//                   + neoplasmType.name() + " ..." );
      final SiteTypeBin noSiteBin = _siteTypeBins.get( NO_SITE );
      final SiteTypeBin allSiteBin = _siteTypeBins.get( ALL_SITES );
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> allSiteScores
            = noSiteBin.scoreBestMatchingNeoplasmChains( neoplasmType, allSiteBin );
      final Map<NeoplasmChain,Collection<SiteNeoplasmBin>> allSiteChainBinsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : allSiteBin.getSiteNeoplasmBins() ) {
         siteNeoplasmBin.getNeoplasmChains( neoplasmType )
                        .forEach( c -> allSiteChainBinsMap.computeIfAbsent( c, s -> new HashSet<>() )
                                                           .add( siteNeoplasmBin ) );
      }
      final Collection<NeoplasmChain> noSiteChains = noSiteBin.getSiteNeoplasmBins()
                                                              .stream()
                                                              .map( b -> b.getNeoplasmChains( neoplasmType ) )
                                                              .flatMap( Collection::stream )
                                                              .collect( Collectors.toSet() );
      if ( noSiteChains.isEmpty() ) {
         return;
      }
      for ( NeoplasmChain noSiteChain : noSiteChains ) {
         final KeyValue<Long,Collection<NeoplasmChain>> allSiteScore = allSiteScores.get( noSiteChain );
         if ( allSiteScore != null && allSiteScore.getKey() > 0 ) {
//               LOGGER.info( _lateralityType.name() + " " + neoplasmType.name()
//                            + "\n   Standard Score " + allSiteScore.getKey()
//                            + "\n     for unsited chain\n"
//                            + noSiteChain.toString()
//                            + "\n     to sited chains\n"
//                            + allSiteScore.getValue()
//                                        .stream()
//                                        .map( NeoplasmChain::toString )
//                                        .collect( Collectors.joining("\n    " ) ) );

            boolean copied = false;
            for ( NeoplasmChain vsAllChain : allSiteScore.getValue() ) {
               for ( SiteNeoplasmBin vsAllBin : allSiteChainBinsMap.get( vsAllChain ) ) {
                  copied |= vsAllBin.copyNeoplasmChain( neoplasmType, noSiteChain );
               }
            }
            if ( copied ) {
               noSiteChain.invalidate();
            }
         }
      }
      noSiteBin.clean();
      allSiteBin.clean();

      allSiteBin.resolveMentionConflicts( NeoplasmType.CANCER );
      allSiteBin.resolveMentionConflicts( TUMOR );

      distributeNoSitesByRoots( neoplasmType, allUriRoots );

      allSiteBin.resolveMentionConflicts( NeoplasmType.CANCER );
      allSiteBin.resolveMentionConflicts( TUMOR );

   }


   void distributeNoSitesByRoots( final NeoplasmType neoplasmType,
                                  final Map<String, Collection<String>> allUriRoots ) {
//      LOGGER.info( "!!! Laterality Type Bin " + _lateralityType.name() + " Distributing UnSited By Roots "
//                   + neoplasmType.name() + " ..." );
      final SiteTypeBin noSiteBin = _siteTypeBins.get( NO_SITE );
      final SiteTypeBin allSiteBin = _siteTypeBins.get( ALL_SITES );
      final Map<NeoplasmChain,Collection<SiteNeoplasmBin>> allSiteChainBinsMap = new HashMap<>();
      for ( SiteNeoplasmBin allSiteNeoplasmBin : allSiteBin.getSiteNeoplasmBins() ) {
         allSiteNeoplasmBin.getNeoplasmChains( neoplasmType )
                        .forEach( c -> allSiteChainBinsMap.computeIfAbsent( c, s -> new HashSet<>() )
                                                           .add( allSiteNeoplasmBin ) );
      }
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> allSiteScores
            = noSiteBin.scoreBestMatchingNeoplasmChainsByRoots( neoplasmType, allSiteBin, allUriRoots );

//      allSiteScores.forEach( (k,v) -> LOGGER.info( _lateralityType.name() + " " + neoplasmType.name()
//                                                   + "\n   By Roots Score1 " + v.getKey()
//                                                   + "\n     for unsited chain\n"
//                                                   + k.toString()
//                                                   + "\n     to sited chains\n"
//                                                   + v.getValue().stream()
//                                                                 .map( NeoplasmChain::toString )
//                                                                 .collect( Collectors.joining("\n    " ) ) ) );

      final Collection<NeoplasmChain> noSiteChains = noSiteBin.getSiteNeoplasmBins()
                                                              .stream()
                                                              .map( b -> b.getNeoplasmChains( neoplasmType ) )
                                                              .flatMap( Collection::stream )
                                                              .collect( Collectors.toSet() );
      if ( noSiteChains.isEmpty() ) {
//         LOGGER.info( "noSiteChains isEmpty." );
         return;
      }
//      LOGGER.info( "No Site Chains:\n  "
//                   + noSiteChains.stream()
//                                 .map( NeoplasmChain::toString )
//                                 .collect( Collectors.joining("\n  ") ) );
      for ( NeoplasmChain noSiteChain : noSiteChains ) {
         final KeyValue<Long,Collection<NeoplasmChain>> allSiteScore = allSiteScores.get( noSiteChain );
         if ( allSiteScore != null && allSiteScore.getKey() > 0 ) {
//               LOGGER.info(  _lateralityType.name() + " " + neoplasmType.name()
//                             + "\n   By Roots Score2 " + allSiteScore.getKey()
//                             + "\n     for unsited chain\n"
//                             + noSiteChain.toString()
//                             + "\n     to sited chains\n"
//                            + allSiteScore.getValue()
//                                       .stream()
//                                       .map( NeoplasmChain::toString )
//                                       .collect( Collectors.joining("\n    " ) ) );
            boolean copied = false;
            for ( NeoplasmChain vsAllChain : allSiteScore.getValue() ) {
               for ( SiteNeoplasmBin vsAllBin : allSiteChainBinsMap.get( vsAllChain ) ) {
                  copied |= vsAllBin.copyNeoplasmChain( neoplasmType, noSiteChain );
               }
            }
            if ( copied ) {
               noSiteChain.invalidate();
            }
         }
      }
      noSiteBin.clean();
      allSiteBin.clean();
   }

   void mergeExtents( final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
      final SiteTypeBin noSiteBin = getSiteTypeBin( NO_SITE );
      final SiteTypeBin allSiteBin = getSiteTypeBin( ALL_SITES );
      noSiteBin.mergeExtents( mentionRelations );
      allSiteBin.mergeExtents( mentionRelations );
      allSiteBin.mergeExtents( noSiteBin, mentionRelations );
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






   public Collection<SiteNeoplasmBin> getSiteNeoplasmBins() {
      return getOrCreateSiteTypeBins().values()
                                     .stream()
                                     .map( SiteTypeBin::getSiteNeoplasmBins )
                                     .flatMap( Collection::stream )
                                     .collect( Collectors.toSet() );
   }


}
