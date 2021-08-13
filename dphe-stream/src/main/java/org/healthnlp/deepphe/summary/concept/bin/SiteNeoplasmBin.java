package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.CANCER;
import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.TUMOR;

/**
 * @author SPF , chip-nlp
 * @since {6/3/2021}
 */
final public class SiteNeoplasmBin {

   static private final Logger LOGGER = Logger.getLogger( "SiteNeoplasmBin" );

   private final SiteChain _siteChain;
   private final Map<NeoplasmType,Collection<NeoplasmChain>> _neoplasmChainsMap;
   private boolean _valid;


   SiteNeoplasmBin( final Collection<Mention> cancers,
                    final Collection<Mention> tumors ) {
      this( new SiteChain(), createNeoplasmChains( cancers ), createNeoplasmChains( tumors ) );
   }


   SiteNeoplasmBin( final String siteUri,
                    final Map<String,Set<Mention>> neoplasmSiteUriSites,
                    final Collection<Mention> cancers,
                    final Collection<Mention> tumors ) {
      this( new SiteChain( siteUri, neoplasmSiteUriSites ),
            createNeoplasmChains( cancers ), createNeoplasmChains( tumors ) );
   }

   private SiteNeoplasmBin( final SiteChain siteChain,
                    final Collection<NeoplasmChain> cancerChains,
                    final Collection<NeoplasmChain> tumorChains ) {
      this( siteChain, createChainsMap( cancerChains, tumorChains ) );
   }

   SiteNeoplasmBin( final SiteChain siteChain,
                    final Map<NeoplasmType,Collection<NeoplasmChain>> neoplasmChainsMap ) {
      _siteChain = siteChain;
      _neoplasmChainsMap = neoplasmChainsMap;
      _valid = true;
   }

   static private Map<NeoplasmType,Collection<NeoplasmChain>> createChainsMap(
         final Collection<NeoplasmChain> cancerChains,
                                   final Collection<NeoplasmChain> tumorChains ) {
      final Map<NeoplasmType,Collection<NeoplasmChain>> chainsMap = new EnumMap<>( NeoplasmType.class );
      chainsMap.put( CANCER, cancerChains );
      chainsMap.put( TUMOR, tumorChains );
      return chainsMap;
   }

   // Using isValid should decrease introduced errors from copying things around.  Used with mergeToNew(..)
   boolean isValid() {
      return _valid
             && _neoplasmChainsMap.values()
                                  .stream()
                                  .flatMap( Collection::stream )
                                  .anyMatch( NeoplasmChain::isValid );
   }

   void invalidate() {
//      LOGGER.info( "!!!!!! MARKED INVALID " + toString() );
//      StackTraceElement[] elements = Thread.currentThread().getStackTrace();
//      for ( int i=0; i<Math.min(3,elements.length); i++ ) {
//         LOGGER.info( elements[ i ].getClassName() + " " + elements[ i ].getMethodName() + " " + elements[i].getLineNumber() );
//      }
      _valid = false;
//      _siteChain.invalidate();
//      _neoplasmChainsMap.values()
//                        .stream()
//                        .flatMap( Collection::stream )
//                        .forEach( NeoplasmChain::invalidate );
      _neoplasmChainsMap.forEach( (k,v) -> v.clear() );
   }

   void clean() {
      if ( !_valid ) {
         return;
      }
      _neoplasmChainsMap.get( CANCER )
                        .removeAll( getInvalidChains( CANCER ) );
      _neoplasmChainsMap.get( TUMOR )
                        .removeAll( getInvalidChains( TUMOR ) );
      if ( _neoplasmChainsMap.get( CANCER ).isEmpty()
           && _neoplasmChainsMap.get( TUMOR ).isEmpty() ) {
         invalidate();
      }
   }

   private Collection<NeoplasmChain> getInvalidChains( final NeoplasmType neoplasmType ) {
      return _neoplasmChainsMap.get( neoplasmType )
                                                            .stream()
                                                        .filter( c -> !c.isValid() )
                                                        .collect( Collectors.toSet() );
   }

   SiteChain getSiteChain() {
      return _siteChain;
   }



   long getSimpleWeight() {
      return 2L * getSiteChain().getAllMentions().size()
             + getNeoplasmChains( NeoplasmType.CANCER )
            .stream()
            .map( NeoplasmChain::getAllMentions )
            .mapToLong( Collection::size )
            .sum()
             + getNeoplasmChains( NeoplasmType.TUMOR )
            .stream()
            .map( NeoplasmChain::getAllMentions )
            .mapToLong( Collection::size )
            .sum();
   }




   void mergeNeoplasmChains( final Map<String,Collection<String>> allUriBranches ) {
      mergeChains( CANCER );
      mergeChains( TUMOR );
      clean();
      mergeChainsByUriBranches( CANCER, allUriBranches );
      mergeChainsByUriBranches( TUMOR, allUriBranches );
      clean();
   }

   void mergeChains( final NeoplasmType neoplasmType ) {
      if ( getNeoplasmChains( neoplasmType ).size() < 2 ) {
         return;
      }
      final Map<String,Integer> uriLevelsMap
            = getNeoplasmChains( neoplasmType )
                                .stream()
                                .map( NeoplasmChain::getChainUris )
                                .flatMap( Collection::stream )
                                .collect( Collectors.toMap( Function.identity(),
                                                            Neo4jOntologyConceptUtil::getClassLevel ) );
      final Function<NeoplasmChain,Integer> getLevelingScore
            = c -> c.getUriMentions()
                    .entrySet()
                    .stream()
                    .mapToInt( e -> uriLevelsMap.get( e.getKey() ) * e.getValue().size() )
                    .sum();
      final Map<Integer,List<NeoplasmChain>> scoredChainsMap
            = getNeoplasmChains( neoplasmType ).stream()
                                .collect( Collectors.groupingBy( getLevelingScore ) );
      final int highScore = Collections.max( scoredChainsMap.keySet() );
      final Collection<NeoplasmChain> bestChains = scoredChainsMap.get( highScore );

      for ( Map.Entry<Integer,List<NeoplasmChain>> scoredChains : scoredChainsMap.entrySet() ) {
         if ( scoredChains.getKey() == highScore ) {
            continue;
         }
         for ( NeoplasmChain bestChain : bestChains ) {
            final int bestLevel = bestChain.getChainUris()
                                           .stream()
                                           .mapToInt( uriLevelsMap::get )
                                           .max()
                                           .orElse( 0 );
            for ( NeoplasmChain scoredChain : scoredChains.getValue() ) {
               final int level = scoredChain.getChainUris()
                                            .stream()
                                            .mapToInt( uriLevelsMap::get )
                                            .max()
                                            .orElse( 0 );
               if ( level >= bestLevel ) {
//                  LOGGER.info( "\nNOT Merging " + neoplasmType + " " + scoredChains.getKey() +
//                               " " + level + " " + scoredChain.toString()
//                               + "  into " + highScore + " " + bestLevel + " " + bestChain.toString() );
                  continue;
               }
//               LOGGER.info( "\nMerging " + neoplasmType + " " + scoredChains.getKey() +
//                            " " + level + " " + scoredChain.toString()
//                            + "  into " + highScore + " " + bestLevel + " " + bestChain.toString() );
               bestChain.copyHere( scoredChain );
               scoredChain.invalidate();
            }
         }
      }
   }

   void mergeChainsByUriBranches( final NeoplasmType neoplasmType,
                                 final Map<String,Collection<String>> allUriBranches ) {
      if ( getNeoplasmChains( neoplasmType ).size() < 2 ) {
         return;
      }
      final List<NeoplasmChain> neoplasmChains = new ArrayList<>( getNeoplasmChains( neoplasmType ) );
      final NeoplasmChain chain1 = neoplasmChains.get( 0 );
      for ( int i=1; i<neoplasmChains.size(); i++ ) {
//         LOGGER.info( "Merging " + neoplasmChains.get( i ).toString() + "\ninto " + chain1.toString() );
         chain1.copyHere( neoplasmChains.get( i ) );
      }
      for ( int i=1; i<neoplasmChains.size(); i++ ) {
         neoplasmChains.get( i ).invalidate();
      }
   }



   private long scoreChainBranchesMatch( final Collection<String> chain1,
                                         final Collection<String> chain2,
                                        final Map<String, Collection<String>> allUriBranches ) {
      final Map<String,Integer> chainBranchCounts = new HashMap<>();
      final Map<String,Integer> otherBranchCounts = new HashMap<>();
//      LOGGER.info( "SiteChain.scoreSiteBranchesMatch This SiteChain ..." );
      for ( String chainUri : chain1 ) {
         final Collection<String> branches = allUriBranches.get( chainUri );
//         LOGGER.info( chainUri + " : " + String.join( ",", branches ) );
         for ( String branch : branches ) {
            final int count = chainBranchCounts.getOrDefault( branch, 0 );
            chainBranchCounts.put( branch, count+1 );
         }
      }
//      LOGGER.info( "SiteChain.scoreSiteBranchesMatch Other SiteChain ..." );
      for ( String chainUri : chain2 ) {
         final Collection<String> branches = allUriBranches.get( chainUri );
//         LOGGER.info( chainUri + " : " + String.join( ",", branches ) );
         for ( String branch : branches ) {
            final int count = otherBranchCounts.getOrDefault( branch, 0 );
            otherBranchCounts.put( branch, count+1 );
         }
      }
      long score = 0;
      for ( Map.Entry<String,Integer> chainBranchCount : chainBranchCounts.entrySet() ) {
         final Integer otherCount = otherBranchCounts.get( chainBranchCount.getKey() );
         if ( otherCount != null ) {
            score += otherCount + chainBranchCount.getValue();
         }
      }
//      LOGGER.info( "SiteChain.scoreSiteBranchesMatch Score : " + score );
      return 100 * score;
   }





   Collection<NeoplasmChain> getNeoplasmChains( final NeoplasmType neoplasmType ) {
      return _neoplasmChainsMap.get( neoplasmType )
                               .stream()
                               .filter( NeoplasmChain::isValid )
                               .collect( Collectors.toSet() );
   }

   static private Collection<NeoplasmChain> createNeoplasmChains( final Collection<Mention> neoplasms ) {
//      LOGGER.info( "CREATING NEOPLASM CHAINS WITH : " + neoplasms.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
      final Map<String, Collection<Mention>> neoplasmUriMentions = new HashMap<>();
      for ( Mention neoplasm : neoplasms ) {
         final String neoplasmUri = neoplasm.getClassUri();
         neoplasmUriMentions.computeIfAbsent( neoplasmUri, u -> new HashSet<>() )
                    .add( neoplasm );
      }
      return createNeoplasmChains( neoplasmUriMentions );
   }

   static private Collection<NeoplasmChain> createNeoplasmChains(
         final Map<String,Collection<Mention>> neoplasmUriMentions ) {
      final Collection<NeoplasmChain> neoplasmChains = new HashSet<>();
      final Map<String, Collection<String>> neoplasmUriChains
//            = UriUtil.getAssociatedUriMap( neoplasmUriMentions.keySet() );
            = UriUtil.getAllAssociatedUriMap( neoplasmUriMentions.keySet() );
      for ( Map.Entry<String, Collection<String>> neoplasmUriChain : neoplasmUriChains.entrySet() ) {
         final Map<String,Collection<Mention>> chainUriMentions
               = neoplasmUriChain.getValue()
                                 .stream()
                                 .collect( Collectors.toMap(
                                       Function.identity(),
                                       neoplasmUriMentions::get ) );
         neoplasmChains.add( new NeoplasmChain( neoplasmUriChain.getKey(),
                                                 chainUriMentions ) );
      }
      return neoplasmChains;
   }

   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SITE MATCHING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   // Done By Site
   KeyValue<Long,Collection<SiteNeoplasmBin>> scoreBestMatchingSites( final Collection<SiteNeoplasmBin> otherBins ) {
      long bestScore = 0;
      final Collection<SiteNeoplasmBin> bestBins = new HashSet<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
         if ( otherBin.equals( this ) ) {
            continue;
         }
         final long score = scoreSiteMatch( otherBin );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestBins.clear();
            }
            bestBins.add( otherBin );
         }
      }
      return new KeyValue<>( bestScore, bestBins );
   }

   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SITE MATCHING     BY ROOTS
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   // Done By Site
   KeyValue<Long,Collection<SiteNeoplasmBin>> scoreBestMatchingSitesByRoots(
         final Collection<SiteNeoplasmBin> otherBins,
         final Map<String,Collection<String>> allUriRoots,
         final Map<String,Collection<String>> allUriBranches) {
      long bestScore = 0;
      final Collection<SiteNeoplasmBin> bestBins = new HashSet<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
//         LOGGER.info( "SiteNeoplasmBin.scoreBestMatchingSitesByRoots otherBin " + otherBin.toString() );
         if ( otherBin.equals( this ) ) {
            continue;
         }
         final long score = scoreSiteRootsMatch( otherBin, allUriRoots, allUriBranches );
//         LOGGER.info( "SiteNeoplasmBin.scoreBestMatchingSitesByRoots score " + otherBin.toString() );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestBins.clear();
            }
            bestBins.add( otherBin );
         }
      }
//      LOGGER.info( "SiteNeoplasmBin.scoreBestMatchingSitesByRoots best " + bestScore + " "
//                   + bestBins.stream().map( SiteNeoplasmBin::toString ).collect( Collectors.joining( "\n" ) ) );
      return new KeyValue<>( bestScore, bestBins );
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            TUMOR EXTENT MATCHING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   void mergeExtents( final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
      if ( !isValid() || getNeoplasmChains( TUMOR ).size() < 2 ) {
         return;
      }
      final Map<NeoplasmChain,NeoplasmChain> alreadyMatchedChains = new HashMap<>();
      final Map<NeoplasmChain,Collection<NeoplasmChain>> matchingChainsMap = new HashMap<>();
      final List<NeoplasmChain> chainList = new ArrayList<>( getNeoplasmChains( TUMOR ) );
      for ( int i=0; i<chainList.size()-1; i++ ) {
         final NeoplasmChain neoplasmChain = chainList.get( i );
         if ( !neoplasmChain.isValid() ) {
            continue;
         }
         for ( int j=i+1; j<chainList.size(); j++ ) {
            final NeoplasmChain otherChain = chainList.get( j );
            if ( otherChain.isValid()
                 && neoplasmChain.scoreExtentMatch( otherChain, mentionRelations ) > 0 ) {
               final NeoplasmChain finalMatch
                     = alreadyMatchedChains.computeIfAbsent( otherChain, c -> neoplasmChain );
               matchingChainsMap.computeIfAbsent( finalMatch, c -> new HashSet<>() )
                                .add( otherChain );
            }
         }
      }
      for ( Map.Entry<NeoplasmChain,Collection<NeoplasmChain>> matchingChains : matchingChainsMap.entrySet() ) {
         matchingChains.getKey().copyHere( matchingChains.getValue() );
         matchingChains.getValue().forEach( NeoplasmChain::invalidate );
      }
      if ( !matchingChainsMap.isEmpty() ) {
         clean();
      }
   }


   void mergeExtents( final SiteNeoplasmBin otherBin,
                      final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
      if ( !isValid() || !otherBin.isValid() || otherBin.equals( this ) ) {
         return;
      }
      final Map<NeoplasmChain,NeoplasmChain> alreadyMatchedChains = new HashMap<>();
      final Map<NeoplasmChain,Collection<NeoplasmChain>> matchingChainsMap = new HashMap<>();
      for ( NeoplasmChain neoplasmChain : getNeoplasmChains( TUMOR ) ) {
         if ( !neoplasmChain.isValid() ) {
            continue;
         }
         for ( NeoplasmChain otherChain : otherBin.getNeoplasmChains( TUMOR ) ) {
            if ( !otherChain.isValid()
                 && neoplasmChain.scoreExtentMatch( otherChain, mentionRelations ) > 0 ) {
               NeoplasmChain finalMatch = alreadyMatchedChains.getOrDefault( otherChain, neoplasmChain );
               matchingChainsMap.computeIfAbsent( finalMatch, c -> new HashSet<>() )
                                .add( otherChain );
               alreadyMatchedChains.put( otherChain, finalMatch );
            }
         }
      }
      for ( Map.Entry<NeoplasmChain,Collection<NeoplasmChain>> matchingChains : matchingChainsMap.entrySet() ) {
         matchingChains.getKey().copyHere( matchingChains.getValue() );
         matchingChains.getValue().forEach( NeoplasmChain::invalidate );
      }
      if ( !matchingChainsMap.isEmpty() ) {
         clean();
         otherBin.clean();
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NEOPLASM MATCHING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasms(
         final NeoplasmType neoplasmType,
         final Collection<SiteNeoplasmBin> otherBins ) {
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
         if ( otherBin.equals( this ) ) {
            continue;
         }
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap =
               scoreBestMatchingNeoplasms( neoplasmType, otherBin );
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

   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasms(
         final NeoplasmType neoplasmType,
         final SiteNeoplasmBin otherBin ) {
      if ( otherBin.equals( this ) ) {
         return Collections.emptyMap();
      }
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      final Collection<NeoplasmChain> otherNeoplasmChains = otherBin.getNeoplasmChains( neoplasmType );
      for ( NeoplasmChain neoplasmChain : getNeoplasmChains( neoplasmType ) ) {
         final KeyValue<Long,Collection<NeoplasmChain>> bestMatches
               = neoplasmChain.getBestMatchingChains( otherNeoplasmChains );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         bestMatchingChainsMap.put( neoplasmChain, bestMatches );
      }
      return bestMatchingChainsMap;
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NEOPLASM MATCHING   BY ROOTS
   //
   ////////////////////////////////////////////////////////////////////////////////////////



   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmsByRoots(
         final NeoplasmType neoplasmType,
         final Collection<SiteNeoplasmBin> otherBins,
         final Map<String,Collection<String>> allUriRoots ) {
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
         if ( otherBin.equals( this ) ) {
            continue;
         }
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap =
               scoreBestMatchingNeoplasmsByRoots( neoplasmType, otherBin, allUriRoots );
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

   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmsByRoots(
         final NeoplasmType neoplasmType,
         final SiteNeoplasmBin otherBin,
         final Map<String,Collection<String>> allUriRoots ) {
      if ( otherBin.equals( this ) ) {
         return Collections.emptyMap();
      }
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      final Collection<NeoplasmChain> otherNeoplasmChains = otherBin.getNeoplasmChains( neoplasmType );
      for ( NeoplasmChain neoplasmChain : getNeoplasmChains( neoplasmType ) ) {
         final KeyValue<Long,Collection<NeoplasmChain>> bestMatches
               = neoplasmChain.getBestMatchingChainsByRoots( otherNeoplasmChains, allUriRoots );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         bestMatchingChainsMap.put( neoplasmChain, bestMatches );
      }
      return bestMatchingChainsMap;
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SITE SCORING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   long scoreSiteMatch( final SiteNeoplasmBin otherBin ) {
      if ( otherBin.equals( this ) ) {
         return 0;
      }
      long score = _siteChain.scoreSiteByUrisMatch( otherBin._siteChain );
      if ( score > 0 ) {
         score += scoreNeoplasmsMatch( CANCER, otherBin.getNeoplasmChains( CANCER )
                                               .stream()
                                               .map( NeoplasmChain::getHeadUri )
                                               .collect( Collectors.toSet() ) );
         score += scoreNeoplasmsMatch( TUMOR, otherBin.getNeoplasmChains( TUMOR )
                                                             .stream()
                                                             .map( NeoplasmChain::getHeadUri )
                                                             .collect( Collectors.toSet() ) );
//         LOGGER.info( "Site Match " + _siteChain.toString() + " score vs "
//                      + otherBin._siteChain.toString() + " = " + score );
      }
      return score;
   }

   long scoreSiteRootsMatch( final SiteNeoplasmBin otherBin,
                             final Map<String,Collection<String>> allUriRoots,
                             final Map<String,Collection<String>> allUriBranches ) {
      if ( otherBin.equals( this ) ) {
         return 0;
      }
//      long score = _siteChain.scoreSiteRootsMatch( otherBin._siteChain, allUriRoots );
//      LOGGER.info( "SiteNeoplasmBin.scoreRootsMatch switched to Branch Match ..." );
      long score = _siteChain.scoreSiteBranchesMatch( otherBin._siteChain, allUriRoots );
      if ( score > 0 ) {
         score += scoreNeoplasmsMatch( CANCER, otherBin.getNeoplasmChains( CANCER )
                                                 .stream()
                                                 .map( NeoplasmChain::getHeadUri )
                                                 .collect( Collectors.toSet() ) );
         score += scoreNeoplasmsMatch( TUMOR, otherBin.getNeoplasmChains( TUMOR )
                                                           .stream()
                                                           .map( NeoplasmChain::getHeadUri )
                                                           .collect( Collectors.toSet() ) );

//         LOGGER.info( "Site Roots Match " + _siteChain.toString() + " score vs "
//                      + otherBin._siteChain.toString() + " = " + score );
      }
      return score;
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NEOPLASM SCORING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   long scoreNeoplasmsMatch( final NeoplasmType neoplasmType, final Collection<String> neoplasmUris ) {
      return getNeoplasmChains( neoplasmType ).stream()
                          .mapToLong( c -> c.scoreNeoplasmUrisMatch( neoplasmUris ) )
                          .sum();
   }




   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MERGING
   //
   ////////////////////////////////////////////////////////////////////////////////////////



   boolean copyHere( final SiteNeoplasmBin otherBin ) {
      if ( !isValid() || !otherBin.isValid() ) {
//         LOGGER.warn( "CopyHere skipped: SiteNeoplasmBin " + hashCode() + " " + isValid()
//                      + " Other SiteNeoplasmBin " + otherBin.hashCode() + " " + otherBin.isValid() );
         return false;
      }
//      LOGGER.info( "--- COPYING SITE NEOPLASM BIN\n" + otherBin.toString()
//                   + "\n       INTO SITE NEOPLASM BIN " + toString() );
      _siteChain.copyHere( otherBin.getSiteChain() );
      copyNeoplasmChains( CANCER, otherBin );
      copyNeoplasmChains( TUMOR, otherBin );
//      LOGGER.info( "Post-CopyNeoplasmChains : " + toString() );
      return true;
   }

   void copyNeoplasmChains( final NeoplasmType neoplasmType, final SiteNeoplasmBin otherBin ) {
      if ( !otherBin.isValid() ) {
         return;
      }
      final Collection<Mention> neoplasmMentions = getNeoplasmChains( neoplasmType ).stream()
                                                                  .map( NeoplasmChain::getAllMentions )
                                                                  .flatMap( Collection::stream )
                                                                  .collect( Collectors.toSet() );
//      LOGGER.info( "THESE NEOPLASM MENTIONS : " + neoplasmMentions.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
      otherBin.getNeoplasmChains( neoplasmType )
              .stream()
              .map( NeoplasmChain::getAllMentions )
              .forEach( neoplasmMentions::addAll );
//      LOGGER.info( "WITH OTHER NEOPLASM MENTIONS : " + neoplasmMentions.stream().map( Mention::getClassUri ).collect( Collectors.joining(";") ) );
      _neoplasmChainsMap.get( neoplasmType ).clear();
      _neoplasmChainsMap.get( neoplasmType ).addAll( createNeoplasmChains( neoplasmMentions ) );
   }


   boolean copyNeoplasmChain( final NeoplasmType neoplasmType, final NeoplasmChain neoplasmChain ) {
      if ( !isValid() || !neoplasmChain.isValid() ) {
         return false;
      }
//      LOGGER.info( "--- COPYING NEOPLASM CHAIN\n" + neoplasmChain.toString()
//                   + "\n       INTO NEOPLASM CHAIN " + toString() );
      final Collection<Mention> neoplasmMentions = getNeoplasmChains( neoplasmType ).stream()
                                                                  .map( NeoplasmChain::getAllMentions )
                                                                  .flatMap( Collection::stream )
                                                                  .collect( Collectors.toSet() );
      neoplasmMentions.addAll( neoplasmChain.getAllMentions() );
      _neoplasmChainsMap.get( neoplasmType ).clear();
      _neoplasmChainsMap.get( neoplasmType ).addAll( createNeoplasmChains( neoplasmMentions ) );
      return true;
   }

   void removeNeoplasmChain( final NeoplasmType neoplasmType, final NeoplasmChain neoplasmChain ) {
      _neoplasmChainsMap.get( neoplasmType ).remove( neoplasmChain );
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            CONCEPT AGGREGATE FORMATION
   //
   ////////////////////////////////////////////////////////////////////////////////////////





   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            UTIL
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   public String toString() {
      return "SiteNeoplasmBin " + hashCode() + " " + getSimpleWeight() + " " + isValid() + " at "
             + _siteChain.toString()
             + "\n  Cancers :\n"
             + getNeoplasmChains( CANCER ).stream()
                               .map( NeoplasmChain::toString )
                               .collect( Collectors.joining("\n") )
             + "\n  Tumors :\n"
             + getNeoplasmChains( TUMOR ).stream()
                                          .map( NeoplasmChain::toString )
                                          .collect( Collectors.joining( "\n" ) );
   }


}
