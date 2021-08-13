package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {6/8/2021}
 */
final public class NeoplasmChain {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmChain" );

   private boolean _valid;
   private String _headUri;
   final private Map<String, Collection<Mention>> _uriMentions = new HashMap<>();


   NeoplasmChain( final String headUri,
                  final Map<String,Collection<Mention>> uriMentions ) {
      _headUri = headUri;
      _uriMentions.putAll( uriMentions );
      _valid = true;
   }

   boolean isValid() {
      return _valid;
   }

   void invalidate() {
//      LOGGER.info( "!!!!!! MARKED INVALID " + toString() );
      _valid = false;
   }


   String getHeadUri() {
      return _headUri;
   }

   Collection<String> getChainUris() {
      return getUriMentions().keySet();
   }

   Map<String,Collection<Mention>> getUriMentions() {
      if ( !isValid() ) {
         return Collections.emptyMap();
      }
      return _uriMentions;
   }

   Collection<Mention> getMentions( final String uri ) {
      return getUriMentions().getOrDefault( uri, Collections.emptyList() );
   }

   Collection<Mention> getAllMentions() {
      return getUriMentions().values()
                         .stream()
                         .flatMap( Collection::stream )
                         .collect( Collectors.toSet() );
   }


   public void copyHere( final Collection<NeoplasmChain> otherChains ) {
//      LOGGER.info( "--- COPYING NEOPLASM CHAINS " + otherChains.stream()
//                                                  .map( NeoplasmChain::toString )
//                                                  .collect( Collectors.joining("\n") )
//                   + "\n       INTO NEOPLASM CHAIN " + toString() );
       NeoplasmChain bestHeadUriChain = this;
      for ( NeoplasmChain otherChain : otherChains ) {
         bestHeadUriChain = getBestHeadUriChain( bestHeadUriChain, otherChain );
         otherChain.getUriMentions().forEach( (k,v) -> _uriMentions.computeIfAbsent( k, m -> new HashSet<>() )
                                                              .addAll( v ) );
      }
      _headUri = bestHeadUriChain._headUri;
   }

   public void copyHere( final NeoplasmChain otherChain ) {
//      LOGGER.info( "--- COPYING NEOPLASM CHAIN " + otherChain.toString() + "\n       INTO NEOPLASM CHAIN " + toString() );
      _headUri = getBestHeadUriChain( this, otherChain )._headUri;
      otherChain.getUriMentions().forEach( (k,v) -> _uriMentions.computeIfAbsent( k, m -> new HashSet<>() )
                                                           .addAll( v ) );
   }

   public void copyHere( final Mention mention ) {
//      LOGGER.info( "--- COPYING MENTION " + mention.getClassUri() + "\n       INTO NEOPLASM CHAIN " + toString() );
      // Don't change the head uri.
      _uriMentions.computeIfAbsent( mention.getClassUri(), m -> new HashSet<>() )
                  .add( mention );
   }

   public void remove( final Collection<Mention> mentions ) {
//      LOGGER.info( "--- REMOVING MENTIONS " + mentions.stream()
//                                                  .map( Mention::getClassUri )
//                                                  .collect( Collectors.joining(";") )
//                   + "\n       FROM NEOPLASM CHAIN " + toString() );
      mentions.forEach( m -> _uriMentions.getOrDefault( m.getClassUri(), new HashSet<>() )
                                         .remove( m ) );
      final Collection<String> empties =
            _uriMentions.entrySet()
                        .stream()
                        .filter( e -> e.getValue().isEmpty() )
                        .map( Map.Entry::getKey )
                        .collect( Collectors.toSet() );
      _uriMentions.keySet().removeAll( empties );
      if ( _uriMentions.isEmpty() ) {
         invalidate();
      } else if ( empties.contains( _headUri ) ) {
         _headUri = new ArrayList<>( UriUtil.getAssociatedUriMap( _uriMentions.keySet() )
                                            .keySet() ).get( 0 );
      }
   }

   public void remove( final Mention mention ) {
      remove( Collections.singletonList( mention ) );
   }

   long scoreNeoplasmByUrisMatch( final NeoplasmChain otherChain ) {
      if ( otherChain.equals( this ) ) {
         return 0;
      }
//      LOGGER.info( "scoreNeoplasmByUrisMatch " + toString() + " vs. " + otherChain.toString() + " = " + scoreNeoplasmUrisMatch( otherChain.getChainUris() ) );
      return scoreNeoplasmUrisMatch( otherChain.getChainUris() );
   }

   long scoreNeoplasmUrisMatch( final Collection<String> neoplasmUris ) {
      return neoplasmUris.stream()
                         .map( this::scoreNeoplasmUriMatch )
                         .mapToLong( l -> l )
                         .sum();
   }

   long scoreNeoplasmUriMatch( final String neoplasmUri ) {
      final Collection<Mention> matches = getUriMentions().get( neoplasmUri );
      if ( matches == null ) {
         return 0;
      }
      long score = neoplasmUri.equals( _headUri ) ? 1000 : 0;
      score += 100L * matches.size();
      return score;
   }

   long scoreNeoplasmMentionMatch( final Mention mention ) {
      long score = 0;
      if ( _headUri.equals( mention.getClassUri() ) ) {
         score = 100;
      }
      return score + 10L * getUriMentions()
            .getOrDefault( mention.getClassUri(), Collections.emptyList() )
            .size()
             + getAllMentions().size();
   }

   long scoreNeoplasmRootsMatch( final NeoplasmChain otherChain, final Map<String,Collection<String>> allUriRoots ) {
      if ( otherChain.equals( this ) ) {
         return 0;
      }
//      LOGGER.info( "scoreNeoplasmRootsMatch1 " + toString() + " vs. " + otherChain.toString() + " = "
//                   + otherChain.getChainUris().stream()
//                                            .map( u -> scoreNeoplasmRootsMatch( u, allUriRoots ) )
//                                            .mapToLong( l -> l )
//                                            .sum() );
//      return otherChain.getChainUris().stream()
//                       .map( u -> scoreNeoplasmRootsMatch( u, allUriRoots ) )
//                       .mapToLong( l -> l )
//                       .sum();
//      LOGGER.info( "scoreNeoplasmRootsMatch2 " + toString() + " vs. " + otherChain.toString() + " = "
//                   + scoreNeoplasmRootsMatch( otherChain.getChainUris(), allUriRoots ) );
      return scoreNeoplasmRootsMatch( otherChain.getChainUris(), allUriRoots );
   }

   long scoreNeoplasmRootsMatch( final String neoplasmUri, final Map<String,Collection<String>> allUriRoots ) {
      long score = 0;
      for ( Map.Entry<String, Collection<Mention>> uriMentions : getUriMentions().entrySet() ) {
         final Collection<String> roots = allUriRoots.get( uriMentions.getKey() );
         if ( roots.contains( neoplasmUri ) ) {
            score += uriMentions.getValue().size();
            if (  uriMentions.getKey().equals( _headUri ) ) {
               score *= 10;
            }
         }
      }
      return score;
   }

   private long scoreNeoplasmRootsMatch( final Collection<String> otherChain,
                                 final Map<String, Collection<String>> allUriRoots ) {
      final Map<String,Integer> chainRootCounts = new HashMap<>();
      final Map<String,Integer> otherRootCounts = new HashMap<>();
      for ( String chainUri : getChainUris() ) {
         final Collection<String> roots = allUriRoots.get( chainUri );
         for ( String root : roots ) {
            final int count = chainRootCounts.getOrDefault( root, 0 );
            chainRootCounts.put( root, count+1 );
         }
      }
      for ( String chainUri : otherChain ) {
         final Collection<String> roots = allUriRoots.get( chainUri );
         for ( String root : roots ) {
            final int count = otherRootCounts.getOrDefault( root, 0 );
            otherRootCounts.put( root, count+1 );
         }
      }
      long score = 0;
      for ( Map.Entry<String,Integer> chainRootCount : chainRootCounts.entrySet() ) {
         final Integer otherCount = otherRootCounts.get( chainRootCount.getKey() );
         if ( otherCount != null ) {
            score += otherCount + chainRootCount.getValue();
         }
      }
      return score;
   }


   ConceptAggregate createConceptAggregate( final String patientId,
                                             final Map<Mention, String> patientMentionNoteIds,
                                             final Map<String,Collection<String>> allUriRoots ) {
//      LOGGER.info( "Creating Concept Aggregate for " + toString() );
      final Map<String, Collection<Mention>> noteIdMentionsMap
            = getUriMentions().values()
                           .stream()
                           .flatMap( Collection::stream )
                           .collect( Collectors.groupingBy( patientMentionNoteIds::get,
                                                            Collectors.toCollection( HashSet::new ) ) );
      // smaller map of uris to roots that only contains pertinent uris.
      final Map<String,Collection<String>> uriRoots = new HashMap<>();
      getUriMentions().keySet().forEach( u -> uriRoots.put( u, allUriRoots.get( u ) ) );
      return new DefaultConceptAggregate( patientId, uriRoots, noteIdMentionsMap );
   }


   KeyValue<Long,Collection<NeoplasmChain>> getBestMatchingChains( final Collection<NeoplasmChain> otherChains ) {
      long bestScore = 0;
      final Collection<NeoplasmChain> bestChains = new HashSet<>();
      for ( NeoplasmChain otherChain : otherChains ) {
         if ( otherChain.equals( this ) ) {
            continue;
         }
         final long score = scoreNeoplasmByUrisMatch( otherChain );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestChains.clear();
            }
            bestChains.add( otherChain );
         }
      }
      return new KeyValue<>( bestScore, bestChains );
   }


   KeyValue<Long,Collection<NeoplasmChain>> getBestMatchingChainsByRoots( final Collection<NeoplasmChain> otherChains,
                                                                          final Map<String,Collection<String>> allUriRoots ) {
      long bestScore = 0;
      final Collection<NeoplasmChain> bestChains = new HashSet<>();
      for ( NeoplasmChain otherChain : otherChains ) {
         if ( otherChain.equals( this ) ) {
            continue;
         }
         final long score = scoreNeoplasmRootsMatch( otherChain, allUriRoots );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestChains.clear();
            }
            bestChains.add( otherChain );
         }
      }
      return new KeyValue<>( bestScore, bestChains );
   }



   long scoreExtentMatch( final NeoplasmChain otherChain,
                          final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
      if ( !otherChain.isValid() || !isValid() || otherChain.equals( this ) ) {
         return 0;
      }
      final Collection<Mention> otherMentions = otherChain.getAllMentions();
      final Collection<Mention> otherRelatedExtents = new HashSet<>();
      final Collection<String> otherRelatedExtentUris = new HashSet<>();
      for ( Mention otherMention : otherMentions ) {
         final Map<String, Collection<Mention>> extentRelations = mentionRelations.get( otherMention );
         if ( extentRelations == null ) {
            continue;
         }
         final Collection<Mention> extents = extentRelations.get( RelationConstants.HAS_TUMOR_EXTENT );
         if ( extents == null ) {
            continue;
         }
         otherRelatedExtents.addAll( extents );
         extents.stream()
                .map( Mention::getClassUri )
                .forEach( otherRelatedExtentUris::add );
      }
      return scoreExtentMatch( otherMentions, otherRelatedExtents, otherRelatedExtentUris, mentionRelations );
   }

   long scoreExtentMatch( final Collection<Mention> otherMentions,
                          final Collection<Mention> otherRelatedExtents,
                          final Collection<String> otherRelatedExtentUris,
                          final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
      if ( !isValid() ) {
         return 0;
      }
      long score = 0;
      for ( Mention mention : getAllMentions() ) {
         if ( otherMentions.contains( mention ) ) {
            return 0;
         }
         if ( otherRelatedExtents.contains( mention ) ) {
            score += 10;
//         } else if ( otherRelatedExtentUris.contains( mention.getClassUri() ) ) {
//            score += 5;
         }
         final Map<String, Collection<Mention>> extentRelations = mentionRelations.get( mention );
         if ( extentRelations == null ) {
            continue;
         }
         final Collection<Mention> extents = extentRelations.get( RelationConstants.HAS_TUMOR_EXTENT );
         if ( extents == null || extents.isEmpty() ) {
            continue;
         }
         final Collection<String> extentUris = extents.stream()
                                                      .map( Mention::getClassUri )
                                                      .collect( Collectors.toSet() );
         for ( Mention otherMention : otherMentions ) {
            if ( extents.contains( otherMention ) ) {
               score += 10;
//            } else if ( extentUris.contains( otherMention.getClassUri() ) ) {
//               score += 5;
            }
         }
      }
      return score;
   }


   // This should be faster than trying to split a UriUtil.getAssociatedUri
   static private NeoplasmChain getBestHeadUriChain( final NeoplasmChain chain1, final NeoplasmChain chain2 ) {
      if ( chain1._headUri.equals( chain2._headUri ) ) {
         return chain1;
      }
      final boolean head2In1 = chain1.getChainUris().contains( chain2.getHeadUri() );
      final boolean head1In2 = chain2.getChainUris().contains( chain1.getHeadUri() );
      if ( head2In1 && !head1In2 ) {
         return chain1;
      }
      if ( !head2In1 && head1In2 ) {
         return chain2;
      }
      final int headDiff = chain1.getUriMentions().getOrDefault( chain1._headUri, Collections.emptyList() ).size()
                           - chain2.getUriMentions().getOrDefault( chain2._headUri, Collections.emptyList() ).size();
      if ( headDiff > 0 ) {
         return chain1;
      } else if ( headDiff < 0 ) {
         return chain2;
      }
      final int allDiff = chain1.getUriMentions().values()
                                      .stream()
                                      .mapToInt( Collection::size )
                                      .sum()
                          - chain2.getUriMentions().values()
                                                   .stream()
                                                   .mapToInt( Collection::size )
                                                   .sum();
      if ( allDiff > 0 ) {
         return chain1;
      } else if ( allDiff < 0 ) {
         return chain2;
      }
      final int uriDiff = chain1.getUriMentions().size() - chain2.getUriMentions().size();
      if ( uriDiff > 0 ) {
         return chain1;
      } else if ( uriDiff < 0 ) {
         return chain2;
      }
      return chain1;
   }



   public String toString() {
      return "NeoplasmChain "
             + _headUri  + " " + hashCode() + " " + isValid()
             + " : " + getUriMentions().entrySet()
                                              .stream()
                                              .map( e -> e.getKey() + " " + e.getValue().size() )
                                              .collect( Collectors.joining( ";" ) );
   }


}
