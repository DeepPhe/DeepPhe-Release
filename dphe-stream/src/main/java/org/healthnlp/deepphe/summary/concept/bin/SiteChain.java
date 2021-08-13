package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {6/9/2021}
 */
final public class SiteChain {

   static private final Logger LOGGER = Logger.getLogger( "SiteChain" );

   static final String NO_SITE_URI = "NO_SITE_URI";

   private boolean _valid;
   private String _headUri;
   private final Map<String,Set<Mention>> _siteUriSites = new HashMap<>();

   SiteChain() {
      _headUri = NO_SITE_URI;
      _siteUriSites.put( NO_SITE_URI, Collections.emptySet() );
      _valid = true;
   }

   SiteChain( final String headUri, final Map<String, Set<Mention>> siteUriSites ) {
      _headUri = headUri;
      _siteUriSites.putAll( siteUriSites );
      _valid = true;
   }

   boolean isValid() {
      return _valid;
   }

   void invalidate() {
      _valid = false;
   }

   boolean isUnsited() {
      return _headUri.equals( NO_SITE_URI );
   }

   boolean isEqual( final SiteChain otherChain ) {
      return false;
//      return getAllMentions().containsAll( otherChain.getAllMentions() )
//             && otherChain.getAllMentions().containsAll( getAllMentions() );
   }

   String getHeadUri() {
      return _headUri;
   }

   private Collection<String> getAllChainUris() {
      return _siteUriSites.keySet();
   }

   Collection<String> getChainUris() {
      return getUriSites().keySet();
   }

   Map<String,Set<Mention>> getUriSites() {
      if ( isUnsited() ) {
         return Collections.emptyMap();
      }
      return _siteUriSites.entrySet()
                          .stream()
                          .filter( e -> !e.getValue().isEmpty() )
                          .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
   }

   Collection<Mention> getAllMentions() {
      return getUriSites().values()
                          .stream()
                          .flatMap( Collection::stream )
                          .collect( Collectors.toSet() );
   }

   long scoreSiteByUrisMatch( final SiteChain otherChain ) {
      if ( otherChain.equals( this ) || isEqual( otherChain ) ) {
         return 0;
      }
//      LOGGER.info( "scoreSiteByUriMatch " + toString() + " vs. " + otherChain.toString() + " = " + scoreSiteByUrisMatch( otherChain.getChainUris() ) );
      return scoreSiteByUrisMatch( otherChain.getAllChainUris() );
   }

   private long scoreSiteByUrisMatch( final Collection<String> siteUris ) {
      return siteUris.stream()
                         .map( this::scoreSiteUriMatch )
                         .mapToLong( l -> l )
                         .sum();
   }

   private long scoreSiteUriMatch( final String siteUri ) {
      if ( isUnsited() || siteUri.equals( NO_SITE_URI ) ) {
         // Return 1 so that unsited laterality distribution can be done.
         return 1;
      }
      final Collection<Mention> matches = _siteUriSites.get( siteUri );
      if ( matches == null ) {
         return 0;
      }
      long score = siteUri.equals( _headUri ) ? 100000 : 0;
      score += 10000L * matches.size();
      return score;
   }

   long scoreSiteRootsMatch( final SiteChain otherChain, final Map<String,Collection<String>> allUriRoots ) {
      if ( otherChain.equals( this ) || isEqual( otherChain ) ) {
         return 0;
      }
//      LOGGER.info( "scoreSiteByUriRootsMatch1 " + toString() + " vs. " + otherChain.toString() + " = " + otherChain.getChainUris().stream()
//                                                                                                             .map( u -> scoreSiteRootsMatch( u, allUriRoots ) )
//                                                                                                             .mapToLong( l -> l )
//                                                                                                             .sum() );
//      return otherChain.getChainUris().stream()
//                       .map( u -> scoreSiteRootsMatch( u, allUriRoots ) )
//                       .mapToLong( l -> l )
//                       .sum();
//      LOGGER.info( "scoreSiteByUriRootsMatch2 " + toString() + " vs. " + otherChain.toString() + " = "
//                   + scoreSiteRootsMatch( otherChain.getChainUris(), allUriRoots ) );
//      LOGGER.info( "\nscoreSiteRootsMatch " + this.toString() + "\nvs " + otherChain.toString() );
      return scoreSiteRootsMatch( otherChain.getAllChainUris(), allUriRoots );
   }

   long scoreSiteBranchesMatch( final SiteChain otherChain, final Map<String,Collection<String>> allUriBranches ) {
      if ( otherChain.equals( this ) || isEqual( otherChain ) ) {
         return 0;
      }
//      LOGGER.info( "scoreSiteByUriRootsMatch1 " + toString() + " vs. " + otherChain.toString() + " = " + otherChain.getChainUris().stream()
//                                                                                                             .map( u -> scoreSiteRootsMatch( u, allUriRoots ) )
//                                                                                                             .mapToLong( l -> l )
//                                                                                                             .sum() );
//      return otherChain.getChainUris().stream()
//                       .map( u -> scoreSiteRootsMatch( u, allUriRoots ) )
//                       .mapToLong( l -> l )
//                       .sum();
//      LOGGER.info( "scoreSiteByUriRootsMatch2 " + toString() + " vs. " + otherChain.toString() + " = "
//                   + scoreSiteRootsMatch( otherChain.getChainUris(), allUriRoots ) );
//      LOGGER.info( "\nscoreSiteBranchesMatch " + this.toString() + "\nvs " + otherChain.toString() );
      return scoreSiteBranchesMatch( otherChain.getAllChainUris(), allUriBranches );
   }



   private long scoreSiteRootsMatch( final String siteUri, final Map<String,Collection<String>> allUriRoots ) {
      long score = 0;
      for ( Map.Entry<String,Set<Mention>> uriSites : _siteUriSites.entrySet() ) {
         final Collection<String> roots = allUriRoots.get( uriSites.getKey() );
         if ( roots.contains( siteUri ) ) {
            score += uriSites.getValue().size();
            if (  uriSites.getKey().equals( _headUri ) ) {
               score *= 10;
            }
         }
      }
      return score;
   }

   private long scoreSiteRootsMatch( final Collection<String> otherChain,
                                 final Map<String, Collection<String>> allUriRoots ) {
      if ( isUnsited() || otherChain.contains( NO_SITE_URI ) ) {
         return 1;
      }
      final Map<String,Integer> chainRootCounts = new HashMap<>();
      final Map<String,Integer> otherRootCounts = new HashMap<>();
//      LOGGER.info( "SiteChain.scoreSiteRootsMatch This SiteChain ..." );
      for ( String chainUri : getAllChainUris() ) {
         final Collection<String> roots = allUriRoots.get( chainUri );
//         LOGGER.info( chainUri + " : " + String.join( ",", roots ) );
         for ( String root : roots ) {
            final int count = chainRootCounts.getOrDefault( root, 0 );
            chainRootCounts.put( root, count+1 );
         }
      }
//      LOGGER.info( "SiteChain.scoreSiteRootsMatch Other SiteChain ..." );
      for ( String chainUri : otherChain ) {
         final Collection<String> roots = allUriRoots.get( chainUri );
//         LOGGER.info( chainUri + " : " + String.join( ",", roots ) );
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
//      LOGGER.info( "SiteChain.scoreSiteRootsMatch Score : " + score );
      return 100 * score;
   }

   private long scoreSiteBranchesMatch( final Collection<String> otherChain,
                                     final Map<String, Collection<String>> allUriBranches ) {
      if ( isUnsited() || otherChain.contains( NO_SITE_URI ) ) {
         return 1;
      }
      final Map<String,Integer> chainBranchCounts = new HashMap<>();
      final Map<String,Integer> otherBranchCounts = new HashMap<>();
//      LOGGER.info( "SiteChain.scoreSiteBranchesMatch This SiteChain ..." );
      for ( String chainUri : getAllChainUris() ) {
         final Collection<String> branches = allUriBranches.get( chainUri );
//         LOGGER.info( chainUri + " : " + String.join( ",", branches ) );
         for ( String branch : branches ) {
            final int count = chainBranchCounts.getOrDefault( branch, 0 );
            chainBranchCounts.put( branch, count+1 );
         }
      }
//      LOGGER.info( "SiteChain.scoreSiteBranchesMatch Other SiteChain ..." );
      for ( String chainUri : otherChain ) {
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



   void copyHere( final SiteChain otherChain ) {
      if ( otherChain.isUnsited() ) {
         return;
      }
      _headUri = getBestHeadUriChain( this, otherChain )._headUri;
      otherChain._siteUriSites.forEach( (k,v) -> _siteUriSites.computeIfAbsent( k, s -> new HashSet<>() )
                                                              .addAll( v ) );
      _siteUriSites.remove( NO_SITE_URI );
   }


   // This should be faster than trying to split a UriUtil.getAssociatedUri
   static private SiteChain getBestHeadUriChain( final SiteChain chain1, final SiteChain chain2 ) {
      if ( chain1._headUri.equals( chain2._headUri ) || chain2.isUnsited() ) {
         return chain1;
      }
      if ( chain1.isUnsited() ) {
         return chain2;
      }
      final boolean head2In1 = chain1.getChainUris().contains( chain2.getHeadUri() );
      final boolean head1In2 = chain2.getChainUris().contains( chain1.getHeadUri() );
      if ( head2In1 && !head1In2 ) {
         return chain1;
      }
      if ( !head2In1 && head1In2 ) {
         return chain2;
      }
      final int headDiff = chain1._siteUriSites.getOrDefault( chain1._headUri, Collections.emptySet() )
                                               .size()
                           - chain2._siteUriSites.getOrDefault( chain2._headUri, Collections.emptySet() )
                                                 .size();
      if ( headDiff > 0 ) {
         return chain1;
      } else if ( headDiff < 0 ) {
         return chain2;
      }
      final int allDiff = chain1._siteUriSites.values().stream().mapToInt( Collection::size ).sum()
                          - chain2._siteUriSites.values().stream().mapToInt( Collection::size ).sum();
      if ( allDiff > 0 ) {
         return chain1;
      } else if ( allDiff < 0 ) {
         return chain2;
      }
      final int uriDiff = chain1._siteUriSites.size() - chain2._siteUriSites.size();
      if ( uriDiff > 0 ) {
         return chain1;
      } else if ( uriDiff < 0 ) {
         return chain2;
      }
      return chain1;
   }


   ConceptAggregate createConceptAggregate( final String patientId,
                                            final Map<Mention, String> patientMentionNoteIds,
                                            final Map<String,Collection<String>> allUriRoots ) {
//      LOGGER.info( "Creating Concept Aggregate for " + toString() );
      final Map<String, Collection<Mention>> noteIdMentionsMap
            = getUriSites().values()
                          .stream()
                          .flatMap( Collection::stream )
                          .collect( Collectors.groupingBy( patientMentionNoteIds::get,
                                                           Collectors.toCollection( HashSet::new ) ) );
      // smaller map of uris to roots that only contains pertinent uris.
      final Map<String,Collection<String>> uriRoots = new HashMap<>();
      getUriSites().keySet().forEach( u -> uriRoots.put( u, allUriRoots.get( u ) ) );
      return new DefaultConceptAggregate( patientId, uriRoots, noteIdMentionsMap ) ;
   }



   public String toString() {
      return "SiteChain " + _headUri + " " + hashCode() + " : " +
             getUriSites().entrySet()
                          .stream()
                          .map( e -> e.getKey() + " " + e.getValue().size() )
                          .collect( Collectors.joining( ",") );
   }


}
