package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.UriConstants.*;
import static org.healthnlp.deepphe.summary.concept.bin.LateralityType.BILATERAL;
import static org.healthnlp.deepphe.summary.concept.bin.LateralityType.LEFT;
import static org.healthnlp.deepphe.summary.concept.bin.LateralityType.RIGHT;
import static org.healthnlp.deepphe.summary.concept.bin.LateralityType.*;
import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.CANCER;
import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.TUMOR;
import static org.healthnlp.deepphe.summary.concept.bin.SiteType.ALL_SITES;
import static org.healthnlp.deepphe.summary.concept.bin.SiteType.NO_SITE;


/**
 * @author SPF , chip-nlp
 * @since {5/24/2021}
 */
final public class AssertionBin {

   static private final Logger LOGGER = Logger.getLogger( "AssertionBin" );


private final Map<LateralityType, LateralityTypeBin> _lateralityBins = new EnumMap<>( LateralityType.class );

   void clean() {
      getOrCreateLateralityTypeBins().values()
                               .forEach( LateralityTypeBin::clean );
   }


   public void clear() {
      getOrCreateLateralityTypeBins().values()
                                     .forEach( LateralityTypeBin::clear );
   }

   static Map<Boolean,List<Mention>> getAssertionTypes( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .collect( Collectors.groupingBy( Mention::isNegated  ) );
   }

   static public Collection<Mention> getAffirmedMentions( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .filter( m -> !m.isNegated() )
                     .collect( Collectors.toSet() );
   }

   static public Collection<Mention> getAffirmedCurrentMentions( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .filter( m -> !m.isNegated() )
                     .filter( m -> !m.isHistoric() )
                     .collect( Collectors.toSet() );
   }


   static private final Collection<String> DONT_CULL_NEOPLASM_URIS = Arrays.asList(
         NEOPLASM, BENIGN_NEOPLASM, MALIGNANT_NEOPLASM, PRIMARY_NEOPLASM, IN_SITU_NEOPLASM,
         METASTATIC_NEOPLASM, METASTASIS, MASS, RECURRENT_TUMOR, "Carcinoma", "Adenocarcinoma" );

   public Collection<Mention> splitMentions( final Collection<Mention> mentions,
                                             final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
                                             final Map<String,Collection<String>> allUriRoots ) {
      clear();
      final Map<BinDistributor.MentionType,Collection<Mention>> categoryMentionsMap
            = splitMentionTypes( mentions );

      final Collection<String> acceptedSites = new HashSet<>();
      trimNeoplasmsBySite( relationsMap, categoryMentionsMap, acceptedSites );

      trimNeoplasmsByUri( BinDistributor.MentionType.CANCER, categoryMentionsMap );
      trimNeoplasmsByUri( BinDistributor.MentionType.TUMOR, categoryMentionsMap );

//      acceptedSites.clear();
      trimNeoplasmsBySitedNeoplasm( relationsMap, categoryMentionsMap, acceptedSites );

//      trimNeoplasmsBySite( relationsMap, categoryMentionsMap, acceptedSites );

      final Collection<Mention> remainingNeoplasms
            = new HashSet<>( categoryMentionsMap.get( BinDistributor.MentionType.CANCER ) );
      remainingNeoplasms.addAll( categoryMentionsMap.get( BinDistributor.MentionType.TUMOR ) );
      final Collection<String> remainingSiteUris = new HashSet<>();
      fillNeoplasmSites( remainingNeoplasms, relationsMap, remainingSiteUris );
      remainingSiteUris.retainAll( acceptedSites );

      final Map<String,Collection<String>> remainingAssociatedSites
            = UriUtil.getAllAssociatedUriMap( remainingSiteUris );
      moveFemaleGenitalia( remainingAssociatedSites );
//      LOGGER.info( "\nAssertionBin.splitMentions line #725, AllAssociatedSites:\n"
//                   + remainingAssociatedSites.entrySet()
//                                       .stream()
//                                       .map( e -> e.getKey() + " : " + String.join( " ", e.getValue() ) )
//                                       .collect( Collectors.joining("\n") ) );

      final Map<String,Set<Mention>> allUriMentionsMap
            = mentions.stream()
                      .collect( Collectors.groupingBy( Mention::getClassUri, Collectors.toSet() ) );
      setNeoplasms( categoryMentionsMap.get( BinDistributor.MentionType.CANCER ),
                    categoryMentionsMap.get( BinDistributor.MentionType.TUMOR ),
                    allUriMentionsMap,
                    remainingAssociatedSites,
                    relationsMap  );
      return categoryMentionsMap.get( BinDistributor.MentionType.OTHER );
   }

   static private void fillAllSites( final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
                                     final Map<BinDistributor.MentionType,Collection<Mention>> categoryMentionsMap,
                                     final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
                                     final Map<String,Collection<Mention>> regionUriNeoplasmMentions,
                                     final Collection<Mention> noStartingSiteNeoplasms,
                                     final Map<String,Collection<Mention>> siteUriSiteMentions,
                                     final Map<String,Collection<Mention>> regionUriRegionMentions ) {
      final List<Mention> neoplasmMentions
            = new ArrayList<>( categoryMentionsMap.get( BinDistributor.MentionType.CANCER ) );
      neoplasmMentions.addAll( categoryMentionsMap.get( BinDistributor.MentionType.TUMOR ) );
      neoplasmMentions.sort( Comparator.comparing( Mention::getClassUri ) );
      final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions = new HashMap<>();
      fillNeoplasmSiteRelations( neoplasmMentions, relationsMap,
                                 Collections.emptyList(), Collections.emptyList(),
                                 neoplasmUriNeoplasmMentions,
                                 noStartingSiteNeoplasms,
                                 siteUriNeoplasmMentions, regionUriNeoplasmMentions,
                                 siteUriSiteMentions, regionUriRegionMentions );
      redistributeRegionsToSites( siteUriNeoplasmMentions, regionUriNeoplasmMentions,
                                  siteUriSiteMentions, regionUriRegionMentions );
//      LOGGER.info( "\nAssertionBin.splitMentions line #150 Remaining Regions that aren't also sites:\n"
//                   + regionUriNeoplasmMentions.entrySet().stream()
//                                              .sorted( Map.Entry.comparingByKey() )
//                                              .map( e -> e.getKey() + " "
//                                                         + e.getValue().stream().distinct()
//                                                            .sorted( Comparator.comparing( Mention::getClassUri ))
//                                                            .map( m -> m.getClassUri() + " " + m.getId() )
//                                                            .collect( Collectors.joining("\n   ") ) )
//                                              .collect( Collectors.joining("\n") ) );
   }

   static private void fillAllSitesWithNeoplasms( final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
                                     final Map<BinDistributor.MentionType,Collection<Mention>> categoryMentionsMap,
                                                  final Collection<Mention> noStartingSiteNeoplasms,
                                     final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
                                     final Map<String,Collection<Mention>> regionUriNeoplasmMentions ) {
      final List<Mention> neoplasmMentions
            = new ArrayList<>( categoryMentionsMap.get( BinDistributor.MentionType.CANCER ) );
      neoplasmMentions.addAll( categoryMentionsMap.get( BinDistributor.MentionType.TUMOR ) );

      fillNeoplasmSiteRelations( neoplasmMentions, relationsMap, noStartingSiteNeoplasms, siteUriNeoplasmMentions,
                                 regionUriNeoplasmMentions );
      redistributeRegionsToSites( siteUriNeoplasmMentions, regionUriNeoplasmMentions );
//      LOGGER.info( "\nAssertionBin.fillAllSitesWithNeoplasms line #150 Remaining Regions that aren't also sites:\n"
//                   + regionUriNeoplasmMentions.entrySet().stream()
//                                              .sorted( Map.Entry.comparingByKey() )
//                                              .map( e -> e.getKey() + " "
//                                                         + e.getValue().stream().distinct()
//                                                            .sorted( Comparator.comparing( Mention::getClassUri ))
//                                                            .map( m -> m.getClassUri() + " " + m.getId() )
//                                                            .collect( Collectors.joining("\n   ") ) )
//                                              .collect( Collectors.joining("\n") ) );
   }

   static private void trimNeoplasmsBySite(
         final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
         final Map<BinDistributor.MentionType,Collection<Mention>> categoryMentionsMap,
         final Collection<String> acceptableSites ) {

       final Map<String,Collection<Mention>> siteUriNeoplasmMentions = new HashMap<>();
      final Map<String,Collection<Mention>> regionUriNeoplasmMentions = new HashMap<>();
      final Collection<Mention> noStartingSiteNeoplasms = new HashSet<>();
      final Map<String,Collection<Mention>> siteUriSiteMentions = new HashMap<>();
      final Map<String,Collection<Mention>> regionUriRegionMentions = new HashMap<>();

      fillAllSites( relationsMap, categoryMentionsMap, siteUriNeoplasmMentions, regionUriNeoplasmMentions,
                    noStartingSiteNeoplasms, siteUriSiteMentions, regionUriRegionMentions );

      cullBySiteCount( siteUriSiteMentions, siteUriNeoplasmMentions );
      cullBySiteCount( regionUriRegionMentions, regionUriNeoplasmMentions );

      acceptableSites.addAll( siteUriSiteMentions.keySet() );
      acceptableSites.addAll( regionUriRegionMentions.keySet() );

      final List<Mention> remainingNeoplasms = new ArrayList<>();
      final Map<Mention,Collection<String>> neoplasmMentionSiteUris = new HashMap<>();
      final Map<Mention,Collection<String>> neoplasmMentionRegionUris = new HashMap<>();
      fillRemainingNeoplasms( siteUriNeoplasmMentions, remainingNeoplasms, neoplasmMentionSiteUris );
      fillRemainingNeoplasms( regionUriNeoplasmMentions, remainingNeoplasms, neoplasmMentionRegionUris );

      remainingNeoplasms.sort( Comparator.comparing( Mention::getClassUri )
                                         .thenComparing( Mention::getId ) );
//      LOGGER.info( "\nsplitMentions Neoplasm Adjusted Remaining Sites and Regions:" );
//      for ( Mention neoplasm : remainingNeoplasms ) {
//         LOGGER.info( neoplasm.getClassUri() + " " + neoplasm.getId()
//                      + " Sites : "
//                      + neoplasmMentionSiteUris.getOrDefault( neoplasm, Collections.emptyList() )
//                                               .stream()
//                                               .sorted()
//                                               .collect( Collectors.joining(" ") ) );
//         LOGGER.info( neoplasm.getClassUri() + " " + neoplasm.getId()
//                      + " Regions : "
//                      + neoplasmMentionRegionUris.getOrDefault( neoplasm, Collections.emptyList() )
//                                                 .stream()
//                                                 .sorted()
//                                                 .collect( Collectors.joining(" ") ) );
//      }
//
//      LOGGER.info( "\nsplitMentions No Starting Sites:\n"
//                   + noStartingSiteNeoplasms.stream()
//                                            .sorted( Comparator.comparing( Mention::getClassUri ) )
//                                            .map( m -> m.getClassUri() + " " + m.getId() )
//                                            .collect( Collectors.joining(" ") ) );

      remainingNeoplasms.addAll( noStartingSiteNeoplasms );
      categoryMentionsMap.get( BinDistributor.MentionType.CANCER ).retainAll( remainingNeoplasms );
      categoryMentionsMap.get( BinDistributor.MentionType.TUMOR ).retainAll( remainingNeoplasms );
   }




   static private void trimNeoplasmsBySitedNeoplasm(
         final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
         final Map<BinDistributor.MentionType,Collection<Mention>> categoryMentionsMap,
         final Collection<String> acceptableSites ) {
      final Map<String,Collection<Mention>> siteUriNeoplasmMentions = new HashMap<>();
      final Map<String,Collection<Mention>> regionUriNeoplasmMentions = new HashMap<>();
      final Collection<Mention> noStartingSiteNeoplasms = new HashSet<>();
      fillAllSitesWithNeoplasms( relationsMap, categoryMentionsMap, noStartingSiteNeoplasms,
                                 siteUriNeoplasmMentions,
                                 regionUriNeoplasmMentions );
      siteUriNeoplasmMentions.keySet().retainAll( acceptableSites );
      regionUriNeoplasmMentions.keySet().retainAll( acceptableSites );

      cullBySitedNeoplasmCount( siteUriNeoplasmMentions );
      cullBySitedNeoplasmCount( regionUriNeoplasmMentions );

      final Collection<String> retainSites
            = acceptableSites.stream()
                             .filter( u -> siteUriNeoplasmMentions.containsKey( u )
                                           || regionUriNeoplasmMentions.containsKey( u ) )
                             .collect( Collectors.toSet() );
      acceptableSites.retainAll( retainSites );

      final List<Mention> remainingNeoplasms = new ArrayList<>();
      final Map<Mention,Collection<String>> neoplasmMentionSiteUris = new HashMap<>();
      final Map<Mention,Collection<String>> neoplasmMentionRegionUris = new HashMap<>();
      fillRemainingNeoplasms( siteUriNeoplasmMentions, remainingNeoplasms, neoplasmMentionSiteUris );
      fillRemainingNeoplasms( regionUriNeoplasmMentions, remainingNeoplasms, neoplasmMentionRegionUris );

      remainingNeoplasms.sort( Comparator.comparing( Mention::getClassUri )
                                         .thenComparing( Mention::getId ) );
//      LOGGER.info( "\ntrimNeoplasmsBySitedNeoplasm Neoplasm Adjusted Remaining Sites and Regions:" );
//      for ( Mention neoplasm : remainingNeoplasms ) {
//         LOGGER.info( neoplasm.getClassUri() + " " + neoplasm.getId()
//                      + " Sites : "
//                      + neoplasmMentionSiteUris.getOrDefault( neoplasm, Collections.emptyList() )
//                                               .stream()
//                                               .sorted()
//                                               .collect( Collectors.joining(" ") ) );
//         LOGGER.info( neoplasm.getClassUri() + " " + neoplasm.getId()
//                      + " Regions : "
//                      + neoplasmMentionRegionUris.getOrDefault( neoplasm, Collections.emptyList() )
//                                                 .stream()
//                                                 .sorted()
//                                                 .collect( Collectors.joining(" ") ) );
//      }
//
//      LOGGER.info( "\ntrimNeoplasmsBySitedNeoplasm No Starting Sites:\n"
//                   + noStartingSiteNeoplasms.stream()
//                                            .sorted( Comparator.comparing( Mention::getClassUri ) )
//                                            .map( m -> m.getClassUri() + " " + m.getId() )
//                                            .collect( Collectors.joining(" ") ) );

      remainingNeoplasms.addAll( noStartingSiteNeoplasms );
      categoryMentionsMap.get( BinDistributor.MentionType.CANCER ).retainAll( remainingNeoplasms );
      categoryMentionsMap.get( BinDistributor.MentionType.TUMOR ).retainAll( remainingNeoplasms );
   }






   static private void loseSiteUris( final Collection<String> unwantedSiteUris,
                                     final Collection<Mention> neoplasmMentions,
                                     final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions = new HashMap<>();
      final Map<String,Collection<Mention>> siteUriNeoplasmMentions = new HashMap<>();
      final Map<String,Collection<Mention>> regionUriNeoplasmMentions = new HashMap<>();
      final Collection<Mention> noStartingSiteNeoplasms = new HashSet<>();
      final Map<String,Collection<Mention>> siteUriSiteMentions = new HashMap<>();
      final Map<String,Collection<Mention>> regionUriRegionMentions = new HashMap<>();

      fillNeoplasmSiteRelations( neoplasmMentions, relationsMap,
                                 Collections.emptyList(), Collections.emptyList(),
                                 neoplasmUriNeoplasmMentions,
                                 noStartingSiteNeoplasms,
                                 siteUriNeoplasmMentions, regionUriNeoplasmMentions,
                                 siteUriSiteMentions, regionUriRegionMentions );

      redistributeRegionsToSites( siteUriNeoplasmMentions, regionUriNeoplasmMentions,
                                  siteUriSiteMentions, regionUriRegionMentions );

      final Collection<String> lostSiteUris = new HashSet<>();
      for ( Map.Entry<String,Collection<Mention>> siteUriNeoplasms
            : siteUriNeoplasmMentions.entrySet() ) {
         final Collection<Mention> wantedMentions
               = siteUriNeoplasms.getValue()
                                 .stream()
                                 .filter( m -> !unwantedSiteUris.contains( m.getClassUri() ) )
                                 .collect( Collectors.toSet() );
         siteUriNeoplasms.getValue().retainAll( wantedMentions );
         if ( siteUriNeoplasms.getValue().isEmpty() ) {
            lostSiteUris.add( siteUriNeoplasms.getKey() );
         }
      }
      for ( Map.Entry<String,Collection<Mention>> regionUriNeoplasms
            : regionUriNeoplasmMentions.entrySet() ) {
         final Collection<Mention> wantedMentions
               = regionUriNeoplasms.getValue()
                                   .stream()
                                   .filter( m -> !unwantedSiteUris.contains( m.getClassUri() ) )
                                   .collect( Collectors.toSet() );
         regionUriNeoplasms.getValue().retainAll( wantedMentions );
         if ( regionUriNeoplasms.getValue().isEmpty() ) {
            lostSiteUris.add( regionUriNeoplasms.getKey() );
         }
      }
      siteUriSiteMentions.keySet().removeAll( lostSiteUris );
      siteUriNeoplasmMentions.keySet().removeAll( lostSiteUris );
      regionUriRegionMentions.keySet().removeAll( lostSiteUris );
      regionUriNeoplasmMentions.keySet().removeAll( lostSiteUris );
   }

   static private void trimNeoplasmsByUri(
         final BinDistributor.MentionType neoplasmType,
         final Map<BinDistributor.MentionType,Collection<Mention>> categoryMentionsMap ) {
      final List<Mention> neoplasmMentions
            = new ArrayList<>( categoryMentionsMap.get( neoplasmType ) );
      final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions
            = neoplasmMentions.stream()
                              .collect( Collectors.groupingBy( Mention::getClassUri,
                                                               Collectors.toCollection( HashSet::new ) ) );
      final Map<String,Collection<Mention>> toCullNeoplasmUriNeoplasms
            = new HashMap<>( neoplasmUriNeoplasmMentions );
      toCullNeoplasmUriNeoplasms.keySet().removeAll( DONT_CULL_NEOPLASM_URIS );

//      cullByNeoplasmCount( toCullNeoplasmUriNeoplasms, neoplasmUriNeoplasmMentions );

      cullByAssociatedNeoplasmCount( toCullNeoplasmUriNeoplasms, neoplasmUriNeoplasmMentions );

      final Collection<Mention> currentNeoplasms = neoplasmUriNeoplasmMentions.values()
                                                                              .stream()
                                                                              .flatMap( Collection::stream )
                                                                              .collect( Collectors.toSet() );
       categoryMentionsMap.get( neoplasmType ).retainAll( currentNeoplasms );
   }


   static private void fillNeoplasmSiteRelations(
         final Collection<Mention> neoplasmMentions,
         final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
         final Collection<String> acceptableNeoplasmUris,
         final Collection<String> acceptableSiteUris,
         final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions,
         final Collection<Mention> noStartingSiteNeoplasms,
         final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
         final Map<String,Collection<Mention>> regionUriNeoplasmMentions,
         final Map<String,Collection<Mention>> siteUriSiteMentions,
         final Map<String,Collection<Mention>> regionUriRegionMentions ) {
      for ( Mention neoplasmMention : neoplasmMentions ) {
         if ( !acceptableNeoplasmUris.isEmpty()
              && !acceptableNeoplasmUris.contains( neoplasmMention.getClassUri() ) ) {
            continue;
         }
         neoplasmUriNeoplasmMentions.computeIfAbsent( neoplasmMention.getClassUri(), m -> new HashSet<>() )
                                    .add( neoplasmMention );
         final Map<String,Collection<Mention>> relations = relationsMap.get( neoplasmMention );
         if ( relations == null ) {
            noStartingSiteNeoplasms.add( neoplasmMention );
            continue;
         }
         boolean hasSite = false;
         for ( Map.Entry<String,Collection<Mention>> relation : relations.entrySet() ) {
            if ( RelationConstants.isHasSiteRelation( relation.getKey() ) ) {
//               LOGGER.info( "Neoplasm " + neoplasmMention.getClassUri() + " " + neoplasmMention.getId()
//                            + " " + relation.getKey()
//                            + " " + relation.getValue().stream().map( Mention::getClassUri )
//                                            .distinct().collect( Collectors.joining(" ") ) );
               final String relationType = relation.getKey();
               if ( relationType.endsWith( "Region" ) || relationType.endsWith( "Cavity" ) ) {
                  fillNeoplasmSiteRelations( acceptableSiteUris, neoplasmMention,
                                             relationType, relation.getValue(),
                                             regionUriNeoplasmMentions,
                                             regionUriRegionMentions );
               } else {
                  fillNeoplasmSiteRelations( acceptableSiteUris, neoplasmMention,
                                             relationType, relation.getValue(),
                                             siteUriNeoplasmMentions,
                                             siteUriSiteMentions );
               }
               hasSite = true;
            }
         }
         if ( !hasSite ) {
            noStartingSiteNeoplasms.add( neoplasmMention );
         }
      }
   }


   static private void fillNeoplasmSiteRelations(
         final Collection<Mention> neoplasmMentions,
         final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
         final Collection<Mention> noStartingSiteNeoplasms,
         final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
         final Map<String,Collection<Mention>> regionUriNeoplasmMentions ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      for ( Mention neoplasmMention : neoplasmMentions ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( neoplasmMention );
         if ( relations == null ) {
            noStartingSiteNeoplasms.add( neoplasmMention );
            continue;
         }
         boolean hasSite = false;
         for ( Map.Entry<String,Collection<Mention>> relation : relations.entrySet() ) {
            if ( RelationConstants.isHasSiteRelation( relation.getKey() ) ) {
//               LOGGER.info( "Neoplasm " + neoplasmMention.getClassUri() + " " + neoplasmMention.getId()
//                            + " " + relation.getKey()
//                            + " " + relation.getValue().stream().map( Mention::getClassUri )
//                                            .distinct().collect( Collectors.joining(" ") ) );
               final String relationType = relation.getKey();
               if ( relationType.endsWith( "Region" ) || relationType.endsWith( "Cavity" ) ) {
                  for ( Mention region : relation.getValue() ) {
                     if ( !UriConstants.getLocationUris( graphDb ).contains( region.getClassUri() ) ) {
                        continue;
                     }
                     regionUriNeoplasmMentions.computeIfAbsent( region.getClassUri(), u -> new HashSet<>() )
                           .add( neoplasmMention );
                  }
               } else {
                  for ( Mention site : relation.getValue() ) {
                     if ( !UriConstants.getLocationUris( graphDb ).contains( site.getClassUri() ) ) {
                        continue;
                     }
                     siteUriNeoplasmMentions.computeIfAbsent( site.getClassUri(), u -> new HashSet<>() )
                                              .add( neoplasmMention );
                  }
               }
               hasSite = true;
            }
         }
         if ( !hasSite ) {
            noStartingSiteNeoplasms.add( neoplasmMention );
         }
      }
   }



   static private void fillNeoplasmSites(
         final Collection<Mention> neoplasmMentions,
         final Map<Mention,Map<String,Collection<Mention>>> relationsMap,
         final Collection<String> siteUris ) {
      for ( Mention neoplasmMention : neoplasmMentions ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( neoplasmMention );
         if ( relations == null ) {
            continue;
         }
         for ( Map.Entry<String,Collection<Mention>> relation : relations.entrySet() ) {
            if ( RelationConstants.isHasSiteRelation( relation.getKey() ) ) {
               relation.getValue().forEach( m -> siteUris.add( m.getClassUri() ) );
            }
         }
      }
   }






   static private void fillNeoplasmSiteRelations( final Collection<String> acceptableSiteUris,
                                                  final Mention neoplasmMention,
                                                  final String relationType,
                                                  final Collection<Mention> siteMentions,
//                                                  final Map<Mention,Map<String,Collection<Mention>>> neoplasmSiteRelations,
                                                  final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
                                                  final Map<String,Collection<Mention>> siteUriSiteMentions ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      for ( Mention site : siteMentions ) {
         final String siteUri = site.getClassUri();
         if ( !acceptableSiteUris.isEmpty()
              && !acceptableSiteUris.contains( siteUri ) ) {
            continue;
         }
         if ( !UriConstants.getLocationUris( graphDb ).contains( site.getClassUri() ) ) {
            continue;
//         neoplasmSiteRelations.computeIfAbsent( neoplasmMention, m -> new HashMap<>()
         }
//                                .computeIfAbsent( relationType, m -> new HashSet<>() )
//                                .add( site );
         siteUriNeoplasmMentions.computeIfAbsent( siteUri, s -> new HashSet<>() )
                                  .add( neoplasmMention );
         siteUriSiteMentions.computeIfAbsent( siteUri, r -> new HashSet<>() )
                                .add( site );
      }
   }


   static private void redistributeRegionsToSites( final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
                                                   final Map<String,Collection<Mention>> regionUriNeoplasmMentions,
                                                   final Map<String,Collection<Mention>> siteUriSiteMentions,
                                                   final Map<String,Collection<Mention>> regionUriRegionMentions ) {
      final Collection<String> removalRegionUris = new HashSet<>();
      for ( Map.Entry<String,Collection<Mention>> regionUriNeoplasms : regionUriNeoplasmMentions.entrySet() ) {
         final Collection<Mention> equalSiteNeoplasms = siteUriNeoplasmMentions.get( regionUriNeoplasms.getKey() );
         if ( equalSiteNeoplasms == null ) {
            continue;
         }
         equalSiteNeoplasms.addAll( regionUriNeoplasms.getValue() );
         final Collection<Mention> regionRegions = regionUriRegionMentions.get( regionUriNeoplasms.getKey() );
         final Collection<Mention> equalSiteSites = siteUriSiteMentions.get( regionUriNeoplasms.getKey() );
         if ( regionRegions == null || equalSiteSites == null ) {
            LOGGER.error( "redistributeRegionsToSites Regions or Sites are null" );
         } else {
            equalSiteSites.addAll( regionRegions );
         }
         removalRegionUris.add( regionUriNeoplasms.getKey() );
      }
      regionUriNeoplasmMentions.keySet().removeAll( removalRegionUris );
      regionUriRegionMentions.keySet().removeAll( removalRegionUris );
   }

   static private void redistributeRegionsToSites( final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
                                                   final Map<String,Collection<Mention>> regionUriNeoplasmMentions ) {
      final Collection<String> removalRegionUris = new HashSet<>();
      for ( Map.Entry<String,Collection<Mention>> regionUriNeoplasms : regionUriNeoplasmMentions.entrySet() ) {
         final Collection<Mention> equalSiteNeoplasms = siteUriNeoplasmMentions.get( regionUriNeoplasms.getKey() );
         if ( equalSiteNeoplasms == null ) {
            continue;
         }
         equalSiteNeoplasms.addAll( regionUriNeoplasms.getValue() );
         removalRegionUris.add( regionUriNeoplasms.getKey() );
      }
      regionUriNeoplasmMentions.keySet().removeAll( removalRegionUris );
   }



   static private void cullBySiteCount( final Map<String,Collection<Mention>> siteUriSiteMentions,
                                        final Map<String,Collection<Mention>> siteUriNeoplasmMentions ) {
      final Map<String,Double> loneSiteSiteCounts = new HashMap<>();
      final double loneSiteSiteCutoff = Math.ceil( getStandardDeviation( siteUriSiteMentions,
                                                                         loneSiteSiteCounts ) / 2 );
      final Collection<String> badLoneSiteUris = getCutoffSiteUris( siteUriSiteMentions );
//      LOGGER.info( "\ncullBySiteCount, removing Lone Site Site Uris below " + loneSiteSiteCutoff +
//                   ":\n" + String.join( "\n   ", badLoneSiteUris ) );
      siteUriSiteMentions.keySet().removeAll( badLoneSiteUris );
      siteUriNeoplasmMentions.keySet().removeAll( badLoneSiteUris );
   }


   static private void cullBySitedNeoplasmCount( final Map<String,Collection<Mention>> siteUriNeoplasmMentions ) {
      final Map<String,Double> loneSiteSiteCounts = new HashMap<>();
      final double loneSiteSiteCutoff = Math.ceil( getStandardDeviation( siteUriNeoplasmMentions,
                                                                         loneSiteSiteCounts ) );
      final Collection<String> badLoneSiteUris = getCutoffSiteUris( siteUriNeoplasmMentions );
//      LOGGER.info( "\ncullBySiteCount, removing Lone Site by Neoplasm Uris below " + loneSiteSiteCutoff +
//                   ":\n" + String.join( "\n   ", badLoneSiteUris ) );
      siteUriNeoplasmMentions.keySet().removeAll( badLoneSiteUris );
   }


   // TODO
   static private Collection<String> getCutoffSiteUris( final Map<String,Collection<Mention>> siteUriSiteMentions ) {
      final Map<String,Double> loneSiteSiteCounts = new HashMap<>();
      final double loneSiteSiteCutoff = Math.ceil( getStandardDeviation( siteUriSiteMentions,
                                                                         loneSiteSiteCounts ) / 2 );
      return loneSiteSiteCounts.entrySet()
                                .stream()
                                .filter( e -> e.getValue() <= loneSiteSiteCutoff )
                                .map( Map.Entry::getKey )
                                .collect( Collectors.toSet() );
   }

//   static private void cullByNeoplasmCount( final Map<String,Collection<Mention>> toCullNeoplasmUriNeoplasms,
//                                            final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions,
//                                            final Map<String,Collection<Mention>> siteUriSiteMentions,
//                                            final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
//                                            final Map<String,Collection<Mention>> regionUriRegionMentions,
//                                            final Map<String,Collection<Mention>> regionUriNeoplasmMentions ) {
//      final Map<String,Double> loneNeoplasmCounts = new HashMap<>();
//      final double loneNeoplasmCutoff = Math.ceil( getStandardDeviation( toCullNeoplasmUriNeoplasms,
//                                                                         loneNeoplasmCounts ) / 2 );
//      final Collection<String> badLoneNeoplasmUris
//            = loneNeoplasmCounts.entrySet()
//                                .stream()
//                                .filter( e -> e.getValue() <= loneNeoplasmCutoff )
//                                .map( Map.Entry::getKey )
//                                .collect( Collectors.toSet() );
//      LOGGER.info( "\ncullByNeoplasmCount, removing Lone Neoplasm Uris below " + loneNeoplasmCutoff +
//                   ":\n" + String.join( "\n   ", badLoneNeoplasmUris ) );
//      neoplasmUriNeoplasmMentions.keySet().removeAll( badLoneNeoplasmUris );
//      final Collection<String> lostSiteUris = new HashSet<>();
//      for ( Map.Entry<String,Collection<Mention>> siteUriNeoplasms
//            : siteUriNeoplasmMentions.entrySet() ) {
//         final Collection<Mention> unwantedMentions
//               = siteUriNeoplasms.getValue()
//                                  .stream()
//                                  .filter( m -> !badLoneNeoplasmUris.contains( m.getClassUri() ) )
//                                 .collect( Collectors.toSet() );
//               siteUriNeoplasms.getValue().removeAll( unwantedMentions );
//         if ( siteUriNeoplasms.getValue().isEmpty() ) {
//            lostSiteUris.add( siteUriNeoplasms.getKey() );
//         }
//      }
//      for ( Map.Entry<String,Collection<Mention>> regionUriNeoplasms
//            : regionUriNeoplasmMentions.entrySet() ) {
//         final Collection<Mention> unwantedMentions
//               = regionUriNeoplasms.getValue()
//                                 .stream()
//                                 .filter( m -> !badLoneNeoplasmUris.contains( m.getClassUri() ) )
//                                 .collect( Collectors.toSet() );
//         regionUriNeoplasms.getValue().removeAll( unwantedMentions );
//         if ( regionUriNeoplasms.getValue().isEmpty() ) {
//            lostSiteUris.add( regionUriNeoplasms.getKey() );
//         }
//      }
//      siteUriSiteMentions.keySet().removeAll( lostSiteUris );
//      siteUriNeoplasmMentions.keySet().removeAll( lostSiteUris );
//      regionUriRegionMentions.keySet().removeAll( lostSiteUris );
//      regionUriNeoplasmMentions.keySet().removeAll( lostSiteUris );
//   }


   static private void cullByNeoplasmCount( final Map<String,Collection<Mention>> toCullNeoplasmUriNeoplasms,
                                            final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions ) {
      final Map<String,Double> loneNeoplasmCounts = new HashMap<>();
      final double loneNeoplasmCutoff = Math.ceil( getStandardDeviation( toCullNeoplasmUriNeoplasms,
                                                                         loneNeoplasmCounts ) / 2 );
      final Collection<String> badLoneNeoplasmUris
            = loneNeoplasmCounts.entrySet()
                                .stream()
                                .filter( e -> e.getValue() <= loneNeoplasmCutoff )
                                .map( Map.Entry::getKey )
                                .collect( Collectors.toSet() );
//      LOGGER.info( "\ncullByNeoplasmCount, removing Lone Neoplasm Uris below " + loneNeoplasmCutoff +
//                   ":\n" + String.join( "\n   ", badLoneNeoplasmUris ) );
      toCullNeoplasmUriNeoplasms.keySet().removeAll( badLoneNeoplasmUris );
      neoplasmUriNeoplasmMentions.keySet().removeAll( badLoneNeoplasmUris );
   }

   static void moveFemaleGenitalia(
         final Map<String,Collection<String>> associatedSites ) {
      if ( associatedSites.containsKey( "Female_Genitalia" ) && associatedSites.containsKey( "Ovary" ) ) {
         associatedSites.get( "Ovary" ).addAll( associatedSites.get( "Female_Genitalia" ) );
         associatedSites.remove( "Female_Genitalia" );
      }
      if ( associatedSites.containsKey( "Cervix_Uteri" ) && associatedSites.containsKey( "Uterus" ) ) {
         associatedSites.get( "Uterus" ).addAll( associatedSites.get( "Cervix_Uteri" ) );
         associatedSites.remove( "Cervix_Uteri" );
      }
   }

   static private void cullByAssociatedNeoplasmCount(
         final Map<String,Collection<Mention>> toCullNeoplasmUriNeoplasms,
         final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions ) {
      final Map<String,Collection<String>> toCullAssociatedNeoplasmUris =
            UriUtil.getAllAssociatedUriMap( toCullNeoplasmUriNeoplasms.keySet() );
      final Map<String,Collection<Mention>> toCullAssociatedNeoplasmUriNeoplasms = new HashMap<>();
      for ( Map.Entry<String,Collection<String>> toCullAssociated : toCullAssociatedNeoplasmUris.entrySet() ) {
         final Collection<Mention> neoplasms = toCullAssociated.getValue()
                                                               .stream()
                                                               .map( toCullNeoplasmUriNeoplasms::get )
                                                               .flatMap( Collection::stream )
                                                               .collect( Collectors.toSet() );
         toCullAssociatedNeoplasmUriNeoplasms.computeIfAbsent( toCullAssociated.getKey(), u -> new HashSet<>() )
                                             .addAll( neoplasms );
      }
      final Map<String,Double> headNeoplasmCounts = new HashMap<>();
      final double headNeoplasmCutoff = Math.ceil( getStandardDeviation( toCullAssociatedNeoplasmUriNeoplasms,
                                                                         headNeoplasmCounts ) );
//                                                                         headNeoplasmCounts ) / 2 );
      final Collection<String> badAssociatedNeoplasmUris
            = headNeoplasmCounts.entrySet()
                                .stream()
                                .filter( e -> e.getValue() <= headNeoplasmCutoff )
                                .map( Map.Entry::getKey )
                                .map( toCullAssociatedNeoplasmUris::get )
                                .flatMap( Collection::stream )
                                .collect( Collectors.toSet() );
//      LOGGER.info( "\ncullByAssociatedNeoplasmCount, removing Associated Neoplasm Uris below " + headNeoplasmCutoff +
//                   ":\n" + String.join( "\n   ", badAssociatedNeoplasmUris ) );
      toCullNeoplasmUriNeoplasms.keySet().removeAll( badAssociatedNeoplasmUris );
      neoplasmUriNeoplasmMentions.keySet().removeAll( badAssociatedNeoplasmUris );
   }





   static private void cullByAssociatedNeoplasmCount( final Map<String,Collection<Mention>> toCullNeoplasmUriNeoplasms,
                                            final Map<String,Collection<Mention>> neoplasmUriNeoplasmMentions,
                                            final Map<String,Collection<Mention>> siteUriSiteMentions,
                                            final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
                                            final Map<String,Collection<Mention>> regionUriRegionMentions,
                                            final Map<String,Collection<Mention>> regionUriNeoplasmMentions ) {
      final Map<String,Collection<String>> toCullAssociatedNeoplasmUris =
            UriUtil.getAllAssociatedUriMap( toCullNeoplasmUriNeoplasms.keySet() );
      final Map<String,Collection<Mention>> toCullAssociatedNeoplasmUriNeoplasms = new HashMap<>();
      for ( Map.Entry<String,Collection<String>> toCullAssociated : toCullAssociatedNeoplasmUris.entrySet() ) {
         final Collection<Mention> neoplasms = toCullAssociated.getValue()
                                                               .stream()
                                                               .map( toCullNeoplasmUriNeoplasms::get )
                                                               .flatMap( Collection::stream )
                                                               .collect( Collectors.toSet() );
         toCullAssociatedNeoplasmUriNeoplasms.computeIfAbsent( toCullAssociated.getKey(), u -> new HashSet<>() )
                                             .addAll( neoplasms );
      }
      final Map<String,Double> loneNeoplasmCounts = new HashMap<>();
      final double loneNeoplasmCutoff = Math.ceil( getStandardDeviation( toCullAssociatedNeoplasmUriNeoplasms,
                                                                         loneNeoplasmCounts ) / 2 );
      final Collection<String> badLoneNeoplasmUris
            = loneNeoplasmCounts.entrySet()
                                .stream()
                                .filter( e -> e.getValue() <= loneNeoplasmCutoff )
                                .map( Map.Entry::getKey )
                                .map( toCullAssociatedNeoplasmUris::get )
                                .flatMap( Collection::stream )
                                .collect( Collectors.toSet() );
//      LOGGER.info( "\ncullByAssociatedNeoplasmCount, removing Associated Neoplasm Uris below " + loneNeoplasmCutoff +
//                   ":\n" + String.join( "\n   ", badLoneNeoplasmUris ) );
      neoplasmUriNeoplasmMentions.keySet().removeAll( badLoneNeoplasmUris );
      final Collection<String> lostSiteUris = new HashSet<>();
      for ( Map.Entry<String,Collection<Mention>> siteUriNeoplasms : siteUriNeoplasmMentions.entrySet() ) {
         final Collection<Mention> unwantedMentions
               = siteUriNeoplasms.getValue()
                                 .stream()
                                 .filter( m -> !badLoneNeoplasmUris.contains( m.getClassUri() ) )
                                 .collect( Collectors.toSet() );
         siteUriNeoplasms.getValue().removeAll( unwantedMentions );
         if ( siteUriNeoplasms.getValue().isEmpty() ) {
            lostSiteUris.add( siteUriNeoplasms.getKey() );
         }
      }
      for ( Map.Entry<String,Collection<Mention>> regionUriNeoplasms
            : regionUriNeoplasmMentions.entrySet() ) {
         final Collection<Mention> unwantedMentions
               = regionUriNeoplasms.getValue()
                                   .stream()
                                   .filter( m -> !badLoneNeoplasmUris.contains( m.getClassUri() ) )
                                   .collect( Collectors.toSet() );
         regionUriNeoplasms.getValue().removeAll( unwantedMentions );
         if ( regionUriNeoplasms.getValue().isEmpty() ) {
            lostSiteUris.add( regionUriNeoplasms.getKey() );
         }
      }
      siteUriSiteMentions.keySet().removeAll( lostSiteUris );
      siteUriNeoplasmMentions.keySet().removeAll( lostSiteUris );
      regionUriRegionMentions.keySet().removeAll( lostSiteUris );
      regionUriNeoplasmMentions.keySet().removeAll( lostSiteUris );
   }


   static private void fillRemainingNeoplasms(
         final Map<String,Collection<Mention>> siteUriNeoplasmMentions,
         final Collection<Mention> remainingNeoplasms,
         final Map<Mention,Collection<String>> neoplasmMentionSiteUris ) {
      for ( Map.Entry<String,Collection<Mention>> siteNeoplasms : siteUriNeoplasmMentions.entrySet() ) {
         remainingNeoplasms.addAll( siteNeoplasms.getValue() );
         siteNeoplasms.getValue()
                            .forEach( m -> neoplasmMentionSiteUris.computeIfAbsent( m, s -> new HashSet<>() )
                                                                  .add( siteNeoplasms.getKey() ) );
      }
   }

   static double getStandardDeviation( final Map<String,Collection<Mention>> siteUriMentions,
                                 final Map<String,Collection<String>> allAssociatedSites,
                                 final Map<String,Double> associatedCounts ) {
      // Calculate the standard deviation for mentions in associated branches.
      double sum = 0.0;
      double deviation = 0.0;
      for ( Map.Entry<String,Collection<String>> associatedSites : allAssociatedSites.entrySet() ) {
         double count = 0;
         for ( String site : associatedSites.getValue() ) {
            count += siteUriMentions.get( site ).size();
         }
         associatedCounts.put( associatedSites.getKey(), count );
         sum += count;
      }
      double mean = sum / associatedCounts.size();

      for( double count : associatedCounts.values() ) {
         deviation += Math.pow( count - mean, 2 );
      }

      final double standardDeviation = Math.sqrt( deviation / associatedCounts.size() );
//      LOGGER.info( "\nAssertionBin.getStandardDeviation line #315 Chains:\n"
//                   + allAssociatedSites.entrySet().stream()
//                                       .map( e -> e.getKey() + " : " + String.join( " ", e.getValue() ) )
//                                       .sorted()
//                                       .collect( Collectors.joining("\n") ) );
//      LOGGER.info( "AssertionBin.getStandardDeviation line #320, mean = " + mean
//                   + " deviation = " + standardDeviation
//                   + " Fully Associated Mention Counts:\n"
//                   + associatedCounts.entrySet()
//                                     .stream()
//                                     .map( e -> e.getKey() + " = " + e.getValue() )
//                                     .sorted()
//                                     .collect( Collectors.joining("\n") ) );
      return standardDeviation;
   }

   static double getStandardDeviation( final Map<String,Collection<Mention>> siteUriMentions,
                                       final Map<String,Double> associatedCounts ) {
      // Calculate the standard deviation for mentions in associated branches.
      double sum = 0.0;
      double deviation = 0.0;
      for ( Map.Entry<String,Collection<Mention>> siteMentions : siteUriMentions.entrySet() ) {
         double count = siteMentions.getValue().size();
         associatedCounts.put( siteMentions.getKey(), count );
         sum += count;
      }
      double mean = sum / associatedCounts.size();

      for( double count : associatedCounts.values() ) {
         deviation += Math.pow( count - mean, 2 );
      }

      final double standardDeviation = Math.sqrt( deviation / associatedCounts.size() );
//      LOGGER.info( "\nAssertionBin.getStandardDeviation line #350, mean = " + mean
//                   + " deviation = " + standardDeviation
//                   + " Mention Counts:\n"
//                   + associatedCounts.entrySet()
//                                     .stream()
//                                     .map( e -> e.getKey() + " = " + e.getValue() )
//                                     .sorted()
//                                     .collect( Collectors.joining("\n") ) );
      return standardDeviation;
   }


   static Map<BinDistributor.MentionType,Collection<Mention>> splitMentionTypes(
         final Collection<Mention> mentions ) {
      final Map<BinDistributor.MentionType,Collection<Mention>> categoryMap = new EnumMap<>( BinDistributor.MentionType.class );
      final Collection<Mention> cancers = categoryMap.computeIfAbsent( BinDistributor.MentionType.CANCER, c -> new HashSet<>() );
      final Collection<Mention> tumors = categoryMap.computeIfAbsent( BinDistributor.MentionType.TUMOR, c -> new HashSet<>() );
      final Collection<Mention> others = categoryMap.computeIfAbsent( BinDistributor.MentionType.OTHER, c -> new HashSet<>() );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> neoplasmUris = UriConstants.getNeoplasmUris( graphDb );
      final Collection<String> massUris = UriConstants.getMassUris( graphDb );
      for ( Mention mention : mentions ) {
         final String uri = mention.getClassUri();
         // Some uris are both mass and neoplasm because of the meaning of "tumor".
         // Favor Neoplasm (as Cancer) over Mass (as Tumor).
         if ( neoplasmUris.contains( uri ) ) {
            cancers.add( mention );
         } else if ( massUris.contains( uri ) ) {
            tumors.add( mention );
         } else {
            others.add( mention );
         }
      }
      return categoryMap;
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<String,Set<Mention>> uriMentionsMap,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      getOrCreateLateralityTypeBins();
      final Map<LateralityType,Collection<Mention>> cancerLateralities
            = LateralityTypeBin.getLateralityTypes( cancers, relationsMap );
      final Map<LateralityType,Collection<Mention>> tumorLateralities
            = LateralityTypeBin.getLateralityTypes( tumors, relationsMap );
      for ( LateralityType lateralityType : LateralityType.values() ) {
         _lateralityBins.get( lateralityType )
                        .setNeoplasms( cancerLateralities.getOrDefault( lateralityType,
                                                                        new HashSet<>( 0 ) ),
                                       tumorLateralities.getOrDefault( lateralityType,
                                                                       new HashSet<>( 0 ) ),
                                       uriMentionsMap,
                                       relationsMap );
      }
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<String,Set<Mention>> uriMentionsMap,
                      final Map<String,Collection<String>> associatedSitesMap,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      getOrCreateLateralityTypeBins();
      final Map<LateralityType,Collection<Mention>> cancerLateralities
            = LateralityTypeBin.getLateralityTypes( cancers, relationsMap );
      final Map<LateralityType,Collection<Mention>> tumorLateralities
            = LateralityTypeBin.getLateralityTypes( tumors, relationsMap );
      for ( LateralityType lateralityType : LateralityType.values() ) {
         _lateralityBins.get( lateralityType )
                        .setNeoplasms( cancerLateralities.getOrDefault( lateralityType,
                                                                        new HashSet<>( 0 ) ),
                                       tumorLateralities.getOrDefault( lateralityType,
                                                                       new HashSet<>( 0 ) ),
                                       uriMentionsMap,
                                       associatedSitesMap,
                                       relationsMap );
      }
   }

   Map<LateralityType,LateralityTypeBin> getOrCreateLateralityTypeBins() {
      if ( _lateralityBins.isEmpty() ) {
         Arrays.stream( LateralityType.values() )
               .forEach( l -> _lateralityBins.put( l, new LateralityTypeBin( l ) ) );
      }
      return _lateralityBins;
   }

   public void distributeSites( final Map<String,Collection<String>> allUriRoots,
                                final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "Assertion Bin Distributing Sites ..." );
      // Within each laterality bin, attempt to assign sites to any neoplasms that do not already have sites.
//      getOrCreateLateralityTypeBins().values().forEach( b -> b.distributeSites( allUriRoots ) );
//      LOGGER.info( "Assertion Bin Distributing Sites with No Lateralities ..." );
//      getSiteNeoplasmBins().forEach( LOGGER::info );
//      distributeNoLateralities( NO_SITE, NO_SITE, allUriRoots, allUriBranches );
//      distributeNoLateralities( NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      LOGGER.info( "\nAssertionBin.distributeSites Line #130, Pre-DistributeNoLateralities CANCERS\n"
//                   + getSiteNeoplasmBins().stream()
//                                          .map( b -> b.getNeoplasmChains( CANCER ) )
//                                          .flatMap( Collection::stream )
//                                          .map( NeoplasmChain::getAllMentions )
//                                          .flatMap( Collection::stream )
//                                          .map( Mention::getClassUri )
//                                          .collect( Collectors.joining(";") ) );

//      distributeNoLateralities( ALL_SITES, ALL_SITES, allUriRoots, allUriBranches );
//      LOGGER.info( "\nAssertionBin.distributeSites Line #130, Post-DistributeNoLateralities CANCERS\n"
//                   + getSiteNeoplasmBins().stream()
//                                          .map( b -> b.getNeoplasmChains( CANCER ) )
//                                          .flatMap( Collection::stream )
//                                          .map( NeoplasmChain::getAllMentions )
//                                          .flatMap( Collection::stream )
//                                          .map( Mention::getClassUri )
//                                          .collect( Collectors.joining(";") ) );
//      LOGGER.info( "AssertionBin.distributeSites Line #130, Post-DistributeNoLateralities TUMORS\n"
//                   + getSiteNeoplasmBins().stream()
//                                          .map( b -> b.getNeoplasmChains( TUMOR ) )
//                                          .flatMap( Collection::stream )
//                                          .map( NeoplasmChain::getAllMentions )
//                                          .flatMap( Collection::stream )
//                                          .map( Mention::getClassUri )
//                                          .collect( Collectors.joining(";") ) );
//      LOGGER.info( "Assertion Bin Improving Site Lateralities ..." );
//      getSiteNeoplasmBins().forEach( LOGGER::info );
//      improveLateralities( allUriRoots, allUriBranches );

//      improveLateralities( allUriRoots, allUriBranches );

      // Move around lateralities that have no site
//      improveLateralities( LEFT, BILATERAL, NO_SITE, allUriRoots, allUriBranches );
//      improveLateralities( RIGHT, BILATERAL, NO_SITE, allUriRoots, allUriBranches );
//      improveLateralities( LEFT, RIGHT, NO_SITE, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, BILATERAL, NO_SITE, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, LEFT, NO_SITE, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, RIGHT, NO_SITE, allUriRoots, allUriBranches );
//      improveLateralities( LEFT, RIGHT, NO_SITE, allUriRoots, allUriBranches );

      // Move around lateralities that have a site
//      improveLateralities( LEFT, BILATERAL, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( RIGHT, BILATERAL, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( LEFT, RIGHT, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, BILATERAL, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, LEFT, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, RIGHT, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( LEFT, RIGHT, ALL_SITES, allUriRoots, allUriBranches );

      // Move Around no sites to sites for each laterality - be careful as this could move bins with sites to bins
      // without sites.
//      improveLateralities( BILATERAL, BILATERAL, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( LEFT, LEFT, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( RIGHT, RIGHT, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
      improveLateralities( NO_LATERALITY, NO_LATERALITY, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );

      // Move Around no sites and no laterality to sites for each laterality - be careful as this could move bins with
      // sites to bins without sites.
//      improveLateralities( NO_LATERALITY, BILATERAL, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, LEFT, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( NO_LATERALITY, RIGHT, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( BILATERAL, NO_LATERALITY, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( LEFT, NO_LATERALITY, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//      improveLateralities( RIGHT, NO_LATERALITY, NO_SITE, ALL_SITES, allUriRoots, allUriBranches );
//
//      improveLateralities( LEFT, RIGHT, ALL_SITES, allUriRoots, allUriBranches );

//      LOGGER.info( "\nAssertionBin.distributeSites Line #150, Post-ImproveLateralities CANCERS\n"
//                   + getSiteNeoplasmBins().stream()
//                                          .map( b -> b.getNeoplasmChains( CANCER ) )
//                                          .flatMap( Collection::stream )
//                                          .map( NeoplasmChain::getAllMentions )
//                                          .flatMap( Collection::stream )
//                                          .map( Mention::getClassUri )
//                                          .collect( Collectors.joining(";") ) );
//      LOGGER.info( "AssertionBin.distributeSites Line #150, Post-ImproveLateralities TUMORS\n"
//                   + getSiteNeoplasmBins().stream()
//                                          .map( b -> b.getNeoplasmChains( TUMOR ) )
//                                          .flatMap( Collection::stream )
//                                          .map( NeoplasmChain::getAllMentions )
//                                          .flatMap( Collection::stream )
//                                          .map( Mention::getClassUri )
//                                          .collect( Collectors.joining(";") ) );

//      distributeNoLateralities( ALL_SITES, ALL_SITES, allUriRoots, allUriBranches );
//      LOGGER.info( "\nAssertionBin.distributeSites Line #170, Post-DistributeNoLateralities 2 CANCERS\n"
//                   + getSiteNeoplasmBins().stream()
//                                          .map( b -> b.getNeoplasmChains( CANCER ) )
//                                          .flatMap( Collection::stream )
//                                          .map( NeoplasmChain::getAllMentions )
//                                          .flatMap( Collection::stream )
//                                          .map( Mention::getClassUri )
//                                          .collect( Collectors.joining(";") ) );
//      LOGGER.info( "AssertionBin.distributeSites Line #170, Post-DistributeNoLateralities 2 TUMORS\n"
//                   + getSiteNeoplasmBins().stream()
//                                          .map( b -> b.getNeoplasmChains( TUMOR ) )
//                                          .flatMap( Collection::stream )
//                                          .map( NeoplasmChain::getAllMentions )
//                                          .flatMap( Collection::stream )
//                                          .map( Mention::getClassUri )
//                                          .collect( Collectors.joining(";") ) );

      // TODO reduce mentions spread across 2 sites.
//      _lateralityBins.values().forEach( LateralityTypeBin::resolveMentionConflicts );

//      getSiteNeoplasmBins().forEach( LOGGER::info );
//      mergeQuadrants();   // Handled in SiteType getMatchingSiteUris.   Read TODO there for another migration.
   }

      // TODO before making final conceptInstances, attempt to merge SiteNeoplasmBins with a site type Region
   //  with SiteNeoplasmBins with a site type organ.  Merge by cancer and tumor match.
//   private void mergeOrganRegion() {
//      _lateralityBins.values().forEach( LateralityTypeBin::mergeOrganRegion );
//   }

//   private void mergeQuadrants() {
//      _lateralityBins.values().forEach( LateralityTypeBin::mergeQuadrants );
//   }


   private SiteTypeBin getSiteTypeBin( final LateralityType lateralityType, final SiteType siteType ) {
      return getOrCreateLateralityTypeBins().get( lateralityType )
                                            .getSiteTypeBin( siteType );
   }



//   void distributeNoLateralities( final SiteType siteTypeFrom,
//                                  final SiteType siteTypeTo,
//                                  final Map<String, Collection<String>> allUriRoots ) {
//      LOGGER.info( "!!! Assertion Bin Distributing " + siteTypeFrom.name()
//                   + " with No Lateralities to " + siteTypeTo.name() +  " ..." );
//      final SiteTypeBin noLateralityBin = getSiteTypeBin( NO_LATERALITY, siteTypeFrom );
//      final Collection<SiteNeoplasmBin> notLocated = new HashSet<>();
////      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoresOnLeft
////            = noLateralityBin.scoreMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteTypeTo ) );
////      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoresOnRight
////            = noLateralityBin.scoreMatchingSiteNeoplasmBins( getSiteTypeBin( RIGHT, siteTypeTo ) );
////      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoresOnBilateral
////            = noLateralityBin.scoreMatchingSiteNeoplasmBins( getSiteTypeBin( BILATERAL, siteTypeTo ) );
//      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoresOnLeft
//            = getSiteTypeBin( LEFT, siteTypeTo ).scoreMatchingSiteNeoplasmBins( noLateralityBin );
//      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoresOnRight
//            = getSiteTypeBin( RIGHT, siteTypeTo ).scoreMatchingSiteNeoplasmBins( noLateralityBin );
//      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> scoresOnBilateral
//            = getSiteTypeBin( BILATERAL, siteTypeTo ).scoreMatchingSiteNeoplasmBins( noLateralityBin );
//      final Map<SiteNeoplasmBin,Map<SiteNeoplasmBin,Long>> reverseScoresOnLeft =
//            SiteTypeBin.createReverseScoredMap( scoresOnLeft );
//      final Map<SiteNeoplasmBin,Map<SiteNeoplasmBin,Long>> reverseScoresOnRight =
//            SiteTypeBin.createReverseScoredMap( scoresOnRight );
//      final Map<SiteNeoplasmBin,Map<SiteNeoplasmBin,Long>> reverseScoresOnBilateral =
//            SiteTypeBin.createReverseScoredMap( scoresOnBilateral );
//      final Collection<SiteNeoplasmBin> noLateralitySites = noLateralityBin.getSiteNeoplasmBins();
////      for ( SiteNeoplasmBin noSiteBin : noLateralitySites ) {
////         final Map<Long,Collection<SiteNeoplasmBin>> leftScoresMap = scoresOnLeft.get( noSiteBin );
////         final Map<Long,Collection<SiteNeoplasmBin>> rightScoresMap = scoresOnRight.get( noSiteBin );
////         final Map<Long,Collection<SiteNeoplasmBin>> bilateralScoresMap = scoresOnBilateral.get( noSiteBin );
////         boolean located = false;
////         if ( leftScoresMap != null ) {
//      final Collection<SiteNeoplasmBin> assigned = new HashSet<>();
//            for ( Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> leftScores : scoresOnLeft.entrySet() ) {
//
//
//
//
//            }
//
//
//
//
//
//
//         }
//
//
//         if ( leftScoresMap != null && leftScoresMap.getKey() > 0 ) {
//            if ( rightScore == null || leftScoresMap.getKey() > rightScore.getKey() ) {
//               if ( bilateralScore == null || leftScoresMap.getKey() > bilateralScore.getKey() ) {
////                  LOGGER.info( "\nLeft Score " + leftScore.getKey() + " for " + noSiteBin.toString() + " left\n"
////                               + leftScore.getValue()
////                                          .stream()
////                                          .map( SiteNeoplasmBin::toString )
////                                          .collect( Collectors.joining("\n  " ) )
////                               + "\nbetter than Right Score "
////                               + (rightScore == null ? "null"
////                                                     : (rightScore.getKey() + "\n  "
////                                                        + rightScore.getValue()
////                                                                    .stream()
////                                                                    .map( SiteNeoplasmBin::toString )
////                                                                    .collect( Collectors.joining(
////                                                                          "\n  " ) ))) );
//                  if ( leftScore.getValue()
//                                .stream()
//                                .map( l -> l.copyHere( noSiteBin ) )
//                                .anyMatch( l -> true ) ) {
//                     noSiteBin.invalidate();
//                     located = true;
//                  }
//               }
//            }
//         }
//         if ( rightScore != null && rightScore.getKey() > 0 ) {
//            if ( leftScore == null || rightScore.getKey() > leftScore.getKey() ) {
//               if ( bilateralScore == null || rightScore.getKey() > bilateralScore.getKey() ) {
////                  LOGGER.info( "\nRight Score " + rightScore.getKey() + " for " + noSiteBin.toString() + " right\n"
////                               + rightScore.getValue()
////                                           .stream()
////                                           .map( SiteNeoplasmBin::toString )
////                                           .collect( Collectors.joining("\n  " ) )
////                               + "\nbetter than Left Score "
////                               + (leftScore == null ? "null"
////                                                    : (leftScore.getKey() + "\n  "
////                                                       + leftScore.getValue()
////                                                                   .stream()
////                                                                   .map( SiteNeoplasmBin::toString )
////                                                                   .collect( Collectors.joining(
////                                                                         "\n  " ) ))) );
//                  if ( rightScore.getValue()
//                                 .stream()
//                                 .map( r -> r.copyHere( noSiteBin ) )
//                                 .anyMatch( r -> true ) ) {
//                     noSiteBin.invalidate();
//                     located = true;
//                  }
//               }
//            }
//         }
//         if ( bilateralScore != null && bilateralScore.getKey() > 0 ) {
//            final boolean leftEqualRight = leftScore != null && rightScore != null
//                                           && leftScore.getKey()
//                                                       .equals( rightScore.getKey() );
//            final boolean leftIsLow = leftScore == null || bilateralScore.getKey() >= leftScore.getKey();
//            final boolean rightIsLow = rightScore == null || bilateralScore.getKey() >= rightScore.getKey();
//            if ( leftEqualRight || ( leftIsLow && rightIsLow ) ) {
////                  LOGGER.info( "\nBilateral Score " + bilateralScore.getKey() + " for " + noSiteBin.toString() + " "
////                               + "bilateral\n"
////                               + bilateralScore.getValue()
////                                               .stream()
////                                               .map( SiteNeoplasmBin::toString )
////                                               .collect( Collectors.joining("\n  " ) ) );
//               if ( bilateralScore.getValue()
//                                  .stream()
//                                  .map( b -> b.copyHere( noSiteBin ) )
//                                  .anyMatch( b -> true ) ) {
//                  noSiteBin.invalidate();
//                  located = true;
//               }
//            }
//         }
//         if ( !located ) {
//            notLocated.add( noSiteBin );
//         }
//      }
//      noLateralityBin.clean();
//      if ( !notLocated.isEmpty() ) {
////         notLocated.forEach( s -> LOGGER.info( "  Could not distribute laterality for " + s.toString() ) );
//         distributeNoLateralitiesByRoots( siteTypeFrom, siteTypeTo, notLocated, allUriRoots );
//      }
//      noLateralityBin.clean();
//   }



   void distributeNoLateralities( final SiteType siteTypeFrom,
                                  final SiteType siteTypeTo,
                                  final Map<String, Collection<String>> allUriRoots,
                                  final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Distributing " + siteTypeFrom.name()
//                   + " with No Lateralities to " + siteTypeTo.name() +  " ..." );
      final SiteTypeBin noLateralityBin = getSiteTypeBin( NO_LATERALITY, siteTypeFrom );
      final Collection<SiteNeoplasmBin> notLocated = new HashSet<>();
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteTypeTo ) );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> rightScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( RIGHT, siteTypeTo ) );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> bilateralScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( BILATERAL, siteTypeTo ) );
      final Collection<SiteNeoplasmBin> noLateralitySites = noLateralityBin.getSiteNeoplasmBins();
      for ( SiteNeoplasmBin noLateralitySite : noLateralitySites ) {
         final KeyValue<Long,Collection<SiteNeoplasmBin>> leftScore = leftScores.get( noLateralitySite );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> rightScore = rightScores.get( noLateralitySite );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bilateralScore = bilateralScores.get( noLateralitySite );
         boolean located = false;
         if ( leftScore != null && leftScore.getKey() > 0 ) {
            if ( rightScore == null || leftScore.getKey() > rightScore.getKey() ) {
               if ( bilateralScore == null || leftScore.getKey() > bilateralScore.getKey() ) {
//                  LOGGER.info( "\nLEFT SCORE " + leftScore.getKey() + " for " + noLateralitySite.toString() + "\n"
//                               + leftScore.getValue()
//                                          .stream()
//                                          .map( SiteNeoplasmBin::toString )
//                                          .collect( Collectors.joining("\n  " ) )
//                               + "\nBETTER THAN RIGHT SCORE "
//                               + (rightScore == null ? "null"
//                                                     : (rightScore.getKey() + "\n  "
//                                                        + rightScore.getValue()
//                                                                    .stream()
//                                                                    .map( SiteNeoplasmBin::toString )
//                                                                    .collect( Collectors.joining(
//                                                                          "\n  " ) ))) );
                  boolean copied = false;
                  for ( SiteNeoplasmBin vsLeftBin : leftScore.getValue() ) {
                     copied |= vsLeftBin.copyHere( noLateralitySite );
                  }
                  if ( copied ) {
                     noLateralitySite.invalidate();
                     located = true;
                  }
               }
            }
         }
         if ( rightScore != null && rightScore.getKey() > 0 ) {
            if ( leftScore == null || rightScore.getKey() > leftScore.getKey() ) {
               if ( bilateralScore == null || rightScore.getKey() > bilateralScore.getKey() ) {
//                  LOGGER.info( "\nREIGHT SCORE " + rightScore.getKey() + " for " + noLateralitySite.toString() + "\n"
//                               + rightScore.getValue()
//                                           .stream()
//                                           .map( SiteNeoplasmBin::toString )
//                                           .collect( Collectors.joining("\n  " ) )
//                               + "\nBETTER THAN LEFT SCORE "
//                               + (leftScore == null ? "null"
//                                                    : (leftScore.getKey() + "\n  "
//                                                       + leftScore.getValue()
//                                                                   .stream()
//                                                                   .map( SiteNeoplasmBin::toString )
//                                                                   .collect( Collectors.joining(
//                                                                         "\n  " ) ))) );
                  boolean copied = false;
                  for ( SiteNeoplasmBin vsRightBin : rightScore.getValue() ) {
                     copied |= vsRightBin.copyHere( noLateralitySite );
                  }
                  if ( copied ) {
                     noLateralitySite.invalidate();
                     located = true;
                  }
               }
            }
         }
         if ( bilateralScore != null && bilateralScore.getKey() > 0 ) {
            final boolean leftEqualRight = leftScore != null && rightScore != null
                                           && leftScore.getKey()
                                                       .equals( rightScore.getKey() );
            final boolean leftIsLow = leftScore == null || bilateralScore.getKey() >= leftScore.getKey();
            final boolean rightIsLow = rightScore == null || bilateralScore.getKey() >= rightScore.getKey();
            if ( leftEqualRight || ( leftIsLow && rightIsLow ) ) {
//                  LOGGER.info( "\nBilateral Score " + bilateralScore.getKey() + " for " + noLateralitySite.toString() + " "
//                               + "bilateral\n"
//                               + bilateralScore.getValue()
//                                               .stream()
//                                               .map( SiteNeoplasmBin::toString )
//                                               .collect( Collectors.joining("\n  " ) ) );
               boolean copied = false;
               for ( SiteNeoplasmBin vsBiLatBin : bilateralScore.getValue() ) {
                  copied |= vsBiLatBin.copyHere( noLateralitySite );
               }
               if ( copied ) {
                  noLateralitySite.invalidate();
                  located = true;
               }
           }
         }
         if ( !located ) {
            notLocated.add( noLateralitySite );
         }
      }
      noLateralityBin.clean();
      if ( !notLocated.isEmpty() ) {
//         notLocated.forEach( s -> LOGGER.info( "  Could not distribute laterality for " + s.toString() ) );
         distributeNoLateralitiesByRoots( siteTypeFrom, siteTypeTo, notLocated, allUriRoots, allUriBranches );
      }
      noLateralityBin.clean();
   }

   void distributeNoLateralitiesByRoots( final SiteType siteTypeFrom,
                                         final SiteType siteTypeTo,
                                        final Collection<SiteNeoplasmBin> notLocated,
                                         final Map<String, Collection<String>> allUriRoots,
                                         final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Distributing " + siteTypeFrom.name()
//                   + " with No Lateralities By Roots to " + siteTypeTo.name() + " ..." );
      final SiteTypeBin noLateralityBin = getSiteTypeBin( NO_LATERALITY, siteTypeFrom );
//      final Collection<SiteNeoplasmBin> notLocated = new HashSet<>();
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBinsByRoots( getSiteTypeBin( LEFT, siteTypeTo ),
                                                                        notLocated,
                                                                        allUriRoots,
                                                                        allUriBranches );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> rightScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBinsByRoots( getSiteTypeBin( RIGHT, siteTypeTo ),
                                                                        notLocated,
                                                                        allUriRoots,
                                                                        allUriBranches );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> bilateralScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBinsByRoots( getSiteTypeBin( BILATERAL, siteTypeTo ),
                                                                        notLocated,
                                                                        allUriRoots,
                                                                        allUriBranches );
      final Collection<SiteNeoplasmBin> noLateralitySites = noLateralityBin.getSiteNeoplasmBins();
      for ( SiteNeoplasmBin noLateralitySite : noLateralitySites ) {
         final KeyValue<Long,Collection<SiteNeoplasmBin>> leftScore = leftScores.get( noLateralitySite );
//         LOGGER.info( "\nLeft Score " + (leftScore==null ? "null" : leftScore.getKey()) + " for " + noSiteBin.toString() );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> rightScore = rightScores.get( noLateralitySite );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bilateralScore = bilateralScores.get( noLateralitySite );
         boolean located = false;
         if ( leftScore != null && leftScore.getKey() > 0 ) {
            if ( rightScore == null || leftScore.getKey() > rightScore.getKey() ) {
               if ( bilateralScore == null || leftScore.getKey() > bilateralScore.getKey() ) {
//                  LOGGER.info( "\nLEFT SCORE " + leftScore.getKey() + " for " + noLateralitySite.toString() + "\n"
//                               + leftScore.getValue()
//                                          .stream()
//                                          .map( SiteNeoplasmBin::toString )
//                                          .collect( Collectors.joining("\n  " ) )
//                               + "\nBETTER THAN RIGHT SCORE "
//                               + (rightScore == null ? "null"
//                                                     : (rightScore.getKey() + "\n  "
//                                                        + rightScore.getValue()
//                                                                    .stream()
//                                                                    .map( SiteNeoplasmBin::toString )
//                                                                    .collect( Collectors.joining(
//                                                                          "\n  " ) ))) );
                  boolean copied = false;
                  for ( SiteNeoplasmBin vsLeftBin : leftScore.getValue() ) {
                     copied |= vsLeftBin.copyHere( noLateralitySite );
                  }
                  if ( copied ) {
                     noLateralitySite.invalidate();
                     notLocated.remove( noLateralitySite );
                     located = true;
                  }
               }
            }
         }
         if ( rightScore != null && rightScore.getKey() > 0 ) {
            if ( leftScore == null || rightScore.getKey() > leftScore.getKey() ) {
               if ( bilateralScore == null || rightScore.getKey() > bilateralScore.getKey() ) {
//                  LOGGER.info( "\nRIGHT SCORE " + rightScore.getKey() + " for " + noLateralitySite.toString() + "\n"
//                               + rightScore.getValue()
//                                           .stream()
//                                           .map( SiteNeoplasmBin::toString )
//                                           .collect( Collectors.joining("\n  " ) )
//                               + "\nBETTER THAN LEFT SCORE "
//                               + (leftScore == null ? "null"
//                                                    : (leftScore.getKey() + "\n  "
//                                                       + leftScore.getValue()
//                                                                   .stream()
//                                                                   .map( SiteNeoplasmBin::toString )
//                                                                   .collect( Collectors.joining(
//                                                                         "\n  " ) ))) );
                  boolean copied = false;
                  for ( SiteNeoplasmBin vsRightBin : rightScore.getValue() ) {
                     copied |= vsRightBin.copyHere( noLateralitySite );
                  }
                  if ( copied ) {
                     noLateralitySite.invalidate();
                     notLocated.remove( noLateralitySite );
                     located = true;
                  }
               }
            }
         }
         if ( bilateralScore != null && bilateralScore.getKey() > 0 ) {
            final boolean leftEqualRight = leftScore != null && rightScore != null
                                           && leftScore.getKey()
                                                       .equals( rightScore.getKey() );
            final boolean leftIsLow = leftScore == null || bilateralScore.getKey() >= leftScore.getKey();
            final boolean rightIsLow = rightScore == null || bilateralScore.getKey() >= rightScore.getKey();
            if ( leftEqualRight || (leftIsLow && rightIsLow ) ) {
//               LOGGER.info( "\nBilateral Score " + bilateralScore.getKey() + " for " + noLateralitySite.toString() + " "
//                            + "bilateral\n"
//                            + bilateralScore.getValue()
//                                            .stream()
//                                            .map( SiteNeoplasmBin::toString )
//                                            .collect( Collectors.joining("\n  " ) ) );
               boolean copied = false;
               for ( SiteNeoplasmBin vsBiLatBin : bilateralScore.getValue() ) {
                  copied |= vsBiLatBin.copyHere( noLateralitySite );
               }
               if ( copied ) {
                  noLateralitySite.invalidate();
                  notLocated.remove( noLateralitySite );
                  located = true;
               }
            }
         }
         if ( !located ) {
            if ( leftScore != null && rightScore != null
                 && leftScore.getKey() > 0
                 && leftScore.getKey().equals( rightScore.getKey() ) ) {
               boolean copied = false;
               for ( SiteNeoplasmBin vsLeftBin : leftScore.getValue() ) {
                  copied |= vsLeftBin.copyHere( noLateralitySite );
               }
               for ( SiteNeoplasmBin vsRightBin : rightScore.getValue() ) {
                  copied |= vsRightBin.copyHere( noLateralitySite );
               }
               if ( copied ) {
                  noLateralitySite.invalidate();
                  notLocated.remove( noLateralitySite );
                  located = true;
               }
            }
            if ( !located ) {
               notLocated.add( noLateralitySite );
            }
         }
      }
//      if ( !notLocated.isEmpty() ) {
//         notLocated.forEach( s -> LOGGER.info( "  Could not distribute laterality for " + s.toString() ) );
//      }
      noLateralityBin.clean();
   }


//   void improveLateralities( final Map<String, Collection<String>> allUriRoots,
//                             final Map<String,Collection<String>> allUriBranches ) {
//      improveLateralities( ALL_SITES, allUriRoots, allUriBranches );
//   }

   static private final int SWITCH_FACTOR = 3;

   static private final int ROOTS_SWITCH_FACTOR = 4;


//   void improveLateralities( final SiteType siteType, final Map<String, Collection<String>> allUriRoots ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities ..." );
//      final Map<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> leftRightScoresMap
//            = getSiteTypeBin( LEFT, siteType ).scoreMatchingSiteNeoplasmBins( getSiteTypeBin( RIGHT, siteType ) );
//      Map<SiteNeoplasmBin, Map<SiteNeoplasmBin,Long>> rightLeftScoredMap
//            = SiteTypeBin.createReverseScoredMap( leftRightScoresMap );
//      for ( Map.Entry<SiteNeoplasmBin, Map<Long,Collection<SiteNeoplasmBin>>> leftRightScores
//            : leftRightScoresMap.entrySet() ) {
//         final SiteNeoplasmBin leftBin = leftRightScores.getKey();
//         for ( Map.Entry<Long,Collection<SiteNeoplasmBin>> rightScoresMap : leftRightScores.getValue().entrySet() ) {
//            final long scoreForLeft = rightScoresMap.getKey();
//            for ( SiteNeoplasmBin rightBin : )
//         }
//
//
//
//      }
//
//
//
//
//
//
//
//      final Collection<SiteNeoplasmBin> allLateralitySites = new HashSet<>( leftRightScores.keySet() );
//      allLateralitySites.addAll( rightLeftScored.keySet() );
//      for ( SiteNeoplasmBin siteNeoplasmBin : allLateralitySites ) {
//         final Map<Long,Collection<SiteNeoplasmBin>> leftScores = leftRightScores.get( siteNeoplasmBin );
//         final Map<Long,Collection<SiteNeoplasmBin>> rightScores = rightLeftScores.get( siteNeoplasmBin );
//         if ( leftScores == null ) {
//            // There were no conflicts.  Continue.
//            continue;
//         }
//         for ( Map.Entry)
//
//
//         if ( leftScores != null ) {
//            if ( rightScores == null ) {
//
//
//               for ( Collection<SiteNeoplasmBin> otherBins : leftScores.values() ) {
//
//               }
//
//
//
//
//
//            }
//            if ( rightScores == null || leftScore.getKey() >= SWITCH_FACTOR * rightScore.getKey() ) {
////               LOGGER.info( "\nLeft Score " + leftScore.getKey() + " for " + siteBin.toString() + " left\n"
////                            + leftScore.getValue()
////                                       .stream()
////                                       .map( SiteNeoplasmBin::toString )
////                                       .collect( Collectors.joining("\n  " ) )
////                            + "\nbetter than Right Score "
////                            + (rightScore == null ? "null"
////                                                  : (rightScore.getKey() + "\n  "
////                                                     + rightScore.getValue()
////                                                                 .stream()
////                                                                 .map( SiteNeoplasmBin::toString )
////                                                                 .collect( Collectors.joining(
////                                                                       "\n  " ) ))) );
//
//               if ( leftScore.getValue()
//                             .stream()
//                             .map( l -> l.copyHere( siteBin ) )
//                             .anyMatch( l -> true ) ) {
//                  siteBin.invalidate();
//               }
//            }
//         }
//         if ( rightScore != null && rightScore.getKey() > 0 ) {
//            if ( leftScore == null || rightScore.getKey() >= SWITCH_FACTOR * leftScore.getKey() ) {
////               LOGGER.info( "\nRight Score " + rightScore.getKey() + " for " + siteBin.toString() + " right\n"
////                            + rightScore.getValue()
////                                        .stream()
////                                        .map( SiteNeoplasmBin::toString )
////                                        .collect( Collectors.joining("\n  " ) )
////                            + "\nbetter than Left Score "
////                            + (leftScore == null ? "null"
////                                                 : (leftScore.getKey() + "\n  "
////                                                    + leftScore.getValue()
////                                                               .stream()
////                                                               .map( SiteNeoplasmBin::toString )
////                                                               .collect( Collectors.joining(
////                                                                     "\n  " ) ))) );
//               if ( rightScore.getValue()
//                              .stream()
//                              .map( r -> r.copyHere( siteBin ) )
//                              .anyMatch( r -> true ) ) {
//                  siteBin.invalidate();
//               }
//            }
//         }
//      }
//      getSiteTypeBin( LEFT, siteType ).clean();
//      getSiteTypeBin( RIGHT, siteType ).clean();
//   }



//   //todo   DCIS is lost HERE  !!!
//   void improveLateralitiesOrig( final SiteType siteType, final Map<String, Collection<String>> allUriRoots ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities ..." );
//      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftToRightScoresMap
//            = getSiteTypeBin( RIGHT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteType ) );
//      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> rightToLeftScoresMap
//            = getSiteTypeBin( LEFT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( RIGHT, siteType ) );
//      final Collection<SiteNeoplasmBin> siteNeoplasmBins = new HashSet<>( leftToRightScoresMap.keySet() );
//      siteNeoplasmBins.addAll( rightToLeftScoresMap.keySet() );
//      for ( SiteNeoplasmBin siteNeoplasmBin : siteNeoplasmBins ) {
//         final KeyValue<Long, Collection<SiteNeoplasmBin>> vsLeftBins = leftToRightScoresMap.get( siteNeoplasmBin );
//         final KeyValue<Long, Collection<SiteNeoplasmBin>> vsRightBins = rightToLeftScoresMap.get( siteNeoplasmBin );
//         if ( vsLeftBins != null && vsLeftBins.getKey() > 0 ) {
//            if ( vsRightBins == null || vsLeftBins.getKey() >= SWITCH_FACTOR * vsRightBins.getKey() ) {
////               LOGGER.info( "\nLeft Score " + leftScore.getKey() + " for " + siteBin.toString() + " left\n"
////                            + leftScore.getValue()
////                                       .stream()
////                                       .map( SiteNeoplasmBin::toString )
////                                       .collect( Collectors.joining("\n  " ) )
////                            + "\nbetter than Right Score "
////                            + (rightScore == null ? "null"
////                                                  : (rightScore.getKey() + "\n  "
////                                                     + rightScore.getValue()
////                                                                 .stream()
////                                                                 .map( SiteNeoplasmBin::toString )
////                                                                 .collect( Collectors.joining(
////                                                                       "\n  " ) ))) );
//// TODO --> Is this reversed?  Bins might be getting copied twice, or back and forth.
//               // TODO yeah, this is probably correct in score direction but reversed in action.
//               //  Should make reverseMaps and handle that way.
//               //  OR -> copy A, B, C to X.  Keep track of map A->X, B->X, C->X
//               //  Then if anything later maps to B, copy it into X
//               LOGGER.info( "\n!!! Copying Right into Left ...." );
//               boolean copied = false;
//               for ( SiteNeoplasmBin vsLeftBin : vsLeftBins.getValue() ) {
//                  copied |= vsLeftBin.copyHere( siteNeoplasmBin );
//               }
//               if ( copied ) {
//                  siteNeoplasmBin.invalidate();
//               }
//            }
//            continue;
//         }
//         if ( vsRightBins != null && vsRightBins.getKey() > 0 ) {
//            if ( vsLeftBins == null || vsRightBins.getKey() >= SWITCH_FACTOR * vsLeftBins.getKey() ) {
////               LOGGER.info( "\nRight Score " + rightScore.getKey() + " for " + siteBin.toString() + " right\n"
////                            + rightScore.getValue()
////                                        .stream()
////                                        .map( SiteNeoplasmBin::toString )
////                                        .collect( Collectors.joining("\n  " ) )
////                            + "\nbetter than Left Score "
////                            + (leftScore == null ? "null"
////                                                 : (leftScore.getKey() + "\n  "
////                                                    + leftScore.getValue()
////                                                               .stream()
////                                                               .map( SiteNeoplasmBin::toString )
////                                                               .collect( Collectors.joining(
////                                                                     "\n  " ) ))) );
//               LOGGER.info( "\n!!! Copying Left into Right ...." );
//               boolean copied = false;
//               for ( SiteNeoplasmBin vsRightBin : vsRightBins.getValue() ) {
//                  copied |= vsRightBin.copyHere( siteNeoplasmBin );
//               }
//               if ( copied ) {
//                  siteNeoplasmBin.invalidate();
//               }
//            }
//         }
//      }
//      getSiteTypeBin( LEFT, siteType ).clean();
//      getSiteTypeBin( RIGHT, siteType ).clean();
//   }



   void improveLateralities( final SiteType siteType,
                             final Map<String, Collection<String>> allUriRoots,
                             final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities ..." );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftToRightScoresMap
            = getSiteTypeBin( RIGHT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteType ) );
      // Create map of left to right bins to be copied.
      final Map<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMovedMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>>
            leftToRightScores : leftToRightScoresMap.entrySet() ) {
         final SiteNeoplasmBin left = leftToRightScores.getKey();
//         final long leftScore = left.getSimpleWeight();
         final Collection<Mention> leftSiteMentions = new HashSet<>( left.getSiteChain()
                                                      .getAllMentions() );
         for ( SiteNeoplasmBin right : leftToRightScores.getValue()
                                                        .getValue() ) {
//            final long rightScore = right.getSimpleWeight();
            final Collection<Mention> rightSiteMentions = new HashSet<>( right.getSiteChain()
                                                                            .getAllMentions() );
            final int rightCount = rightSiteMentions.size();
            rightSiteMentions.removeAll( leftSiteMentions );
            final int common = rightCount - rightSiteMentions.size();
            final int leftScore = leftSiteMentions.size() - common;
            final int rightScore = rightCount - common;
            if ( leftScore >= SWITCH_FACTOR * rightScore ) {
               // move right to left
//               LOGGER.info( "\n!! Move Right " + rightScore + "\n" + right.toString() +
//                            "\nto Left " + leftScore + "\n" + left.toString() );
               toBeMovedMap.computeIfAbsent( right, l -> new HashSet<>() ).add( left );
            } else if ( rightScore >= SWITCH_FACTOR * leftScore ) {
               // move left to right
//               LOGGER.info( "\n!! Move Left " + leftScore + "\n" + left.toString() +
//                            "\nto Right " + rightScore + "\n" + right.toString() );
               toBeMovedMap.computeIfAbsent( left, l -> new HashSet<>() ).add( right );
            } else {
//               LOGGER.info( "\n!!!! Not Moving Left " + leftScore + "\n" + left.toString() +
//                            "\nor Right " + rightScore + "\n" + right.toString() );
            }
         }
      }
      for ( Map.Entry<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMoved : toBeMovedMap.entrySet() ) {
         toBeMoved.getValue().forEach( t -> t.copyHere( toBeMoved.getKey() ) );
      }
      toBeMovedMap.keySet().forEach( SiteNeoplasmBin::invalidate );
      getSiteTypeBin( LEFT, siteType ).clean();
      getSiteTypeBin( RIGHT, siteType ).clean();
      final Collection<SiteNeoplasmBin> notMoved
            = new HashSet<>( getSiteTypeBin( LEFT, siteType ).getSiteNeoplasmBins() );
      notMoved.addAll( getSiteTypeBin( RIGHT, siteType ).getSiteNeoplasmBins() );
      notMoved.removeAll( toBeMovedMap.keySet() );
      improveLateralitiesByBranch( siteType, notMoved, allUriRoots, allUriBranches );
   }

   void improveLateralitiesByBranch( final SiteType siteType,
                                     final Collection<SiteNeoplasmBin> notMoved,
                                     final Map<String,Collection<String>> allUriRoots,
                                     final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities by Branch ..." );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftToRightScoresMap
//            = getSiteTypeBin( RIGHT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteType ) );
            = getSiteTypeBin( RIGHT, siteType ).scoreBestMatchingSiteNeoplasmBinsByRoots( getSiteTypeBin( LEFT,
                                                                                                          siteType ),
                                                                                          notMoved,
                                                                                          allUriRoots,
                                                                                          allUriBranches );
      // Create map of left to right bins to be copied.
      final Map<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMovedMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>>
            leftToRightScores : leftToRightScoresMap.entrySet() ) {
         final SiteNeoplasmBin left = leftToRightScores.getKey();
//         final long leftScore = left.getSimpleWeight();
         final Collection<Mention> leftSiteMentions = new HashSet<>( left.getSiteChain()
                                                                         .getAllMentions() );
         for ( SiteNeoplasmBin right : leftToRightScores.getValue()
                                                        .getValue() ) {
//            final long rightScore = right.getSimpleWeight();
            final Collection<Mention> rightSiteMentions = new HashSet<>( right.getSiteChain()
                                                                              .getAllMentions() );
            final int rightCount = rightSiteMentions.size();
            rightSiteMentions.removeAll( leftSiteMentions );
            final int common = rightCount - rightSiteMentions.size();
            final int leftScore = leftSiteMentions.size() - common;
            final int rightScore = rightCount - common;
            if ( leftScore >= ROOTS_SWITCH_FACTOR * rightScore ) {
               // move right to left
//               LOGGER.info( "\n!! Move Right " + rightScore + "\n" + right.toString() +
//                            "\nto Left " + leftScore + "\n" + left.toString() );
               toBeMovedMap.computeIfAbsent( right, l -> new HashSet<>() ).add( left );
            } else if ( rightScore >= ROOTS_SWITCH_FACTOR * leftScore ) {
               // move left to right
//               LOGGER.info( "\n!! Move Left " + leftScore + "\n" + left.toString() +
//                            "\nto Right " + rightScore + "\n" + right.toString() );
               toBeMovedMap.computeIfAbsent( left, l -> new HashSet<>() ).add( right );
//            } else {
//               LOGGER.info( "\n!!!! Not Moving Left " + leftScore + "\n" + left.toString() +
//                            "\nor Right " + rightScore + "\n" + right.toString() );
            }
         }
      }
      for ( Map.Entry<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMoved : toBeMovedMap.entrySet() ) {
         toBeMoved.getValue().forEach( t -> t.copyHere( toBeMoved.getKey() ) );
      }
      toBeMovedMap.keySet().forEach( SiteNeoplasmBin::invalidate );
      getSiteTypeBin( LEFT, siteType ).clean();
      getSiteTypeBin( RIGHT, siteType ).clean();
   }


   // TODO !!!  Merge lateralities that have the same site.

//   void mergeLateralities() {
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final Map<SiteChain>
//
//
//      leftAll.getSiteNeoplasmBins().stream( b -> b)
//
//
//      final SiteTypeBin rightAll = getSiteTypeBin( RIGHT, ALL_SITES );
//
//
//
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//      final SiteTypeBin leftAll = getSiteTypeBin( LEFT, ALL_SITES );
//   }


   void improveLateralities( final LateralityType lateralityType1,
                             final LateralityType lateralityType2,
                             final SiteType siteType,
                             final Map<String, Collection<String>> allUriRoots,
                             final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities "
//                   + lateralityType1 + " " + siteType + " "
//                   + lateralityType2 + " ..." );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftToRightScoresMap
            = getSiteTypeBin( lateralityType1, siteType )
            .scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( lateralityType2, siteType ) );
      // Create map of left to right bins to be copied.
      final Map<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMovedMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>>
            leftToRightScores : leftToRightScoresMap.entrySet() ) {
         final SiteNeoplasmBin left = leftToRightScores.getKey();
//         final long leftScore = left.getSimpleWeight();
         final Collection<Mention> leftSiteMentions = new HashSet<>( left.getSiteChain()
                                                                         .getAllMentions() );
         for ( SiteNeoplasmBin right : leftToRightScores.getValue()
                                                        .getValue() ) {
//            final long rightScore = right.getSimpleWeight();
            final Collection<Mention> rightSiteMentions = new HashSet<>( right.getSiteChain()
                                                                              .getAllMentions() );
            final int rightCount = rightSiteMentions.size();
            rightSiteMentions.removeAll( leftSiteMentions );
            final int common = rightCount - rightSiteMentions.size();
            final int leftScore = leftSiteMentions.size() - common;
            final int rightScore = rightCount - common;
            if ( leftScore >= SWITCH_FACTOR * rightScore ) {
               // move right to left
//               LOGGER.info( "\n!! Move Right " + rightScore + "\n" + right.toString() +
//                            "\nto Left " + leftScore + "\n" + left.toString() );
               toBeMovedMap.computeIfAbsent( right, l -> new HashSet<>() ).add( left );
            } else if ( rightScore >= SWITCH_FACTOR * leftScore ) {
               // move left to right
//               LOGGER.info( "\n!! Move Left " + leftScore + "\n" + left.toString() +
//                            "\nto Right " + rightScore + "\n" + right.toString() );
               toBeMovedMap.computeIfAbsent( left, l -> new HashSet<>() ).add( right );
//            } else {
//               LOGGER.info( "\n!!!! Not Moving Left " + leftScore + "\n" + left.toString() +
//                            "\nor Right " + rightScore + "\n" + right.toString() );
            }
         }
      }
      for ( Map.Entry<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMoved : toBeMovedMap.entrySet() ) {
         toBeMoved.getValue().forEach( t -> t.copyHere( toBeMoved.getKey() ) );
      }
      toBeMovedMap.keySet().forEach( SiteNeoplasmBin::invalidate );
      getSiteTypeBin( lateralityType1, siteType ).clean();
      getSiteTypeBin( lateralityType2, siteType ).clean();
      final Collection<SiteNeoplasmBin> notMoved
            = new HashSet<>( getSiteTypeBin( lateralityType1, siteType ).getSiteNeoplasmBins() );
      notMoved.addAll( getSiteTypeBin( lateralityType2, siteType ).getSiteNeoplasmBins() );
      notMoved.removeAll( toBeMovedMap.keySet() );
      improveLateralitiesByBranch( lateralityType1, lateralityType2, siteType, notMoved, allUriRoots, allUriBranches );
   }

   void improveLateralitiesByBranch( final LateralityType lateralityType1,
                                     final LateralityType lateralityType2,
                                     final SiteType siteType,
                                     final Collection<SiteNeoplasmBin> notMoved,
                                     final Map<String,Collection<String>> allUriRoots,
                                     final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities by Branch "
//                   + lateralityType1 + " " + siteType + " "
//                   + lateralityType2 + " ..." );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftToRightScoresMap
//            = getSiteTypeBin( RIGHT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteType ) );
            = getSiteTypeBin( lateralityType1, siteType )
            .scoreBestMatchingSiteNeoplasmBinsByRoots( getSiteTypeBin( lateralityType2,
                                                                       siteType ),
                                                                        notMoved,
                                                                        allUriRoots,
                                                                        allUriBranches );
      // Create map of left to right bins to be copied.
      final Map<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMovedMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>>
            leftToRightScores : leftToRightScoresMap.entrySet() ) {
         final SiteNeoplasmBin left = leftToRightScores.getKey();
//         final long leftScore = left.getSimpleWeight();
         final Collection<Mention> leftSiteMentions = new HashSet<>( left.getSiteChain()
                                                                         .getAllMentions() );
         for ( SiteNeoplasmBin right : leftToRightScores.getValue()
                                                        .getValue() ) {
//            final long rightScore = right.getSimpleWeight();
            final Collection<Mention> rightSiteMentions = new HashSet<>( right.getSiteChain()
                                                                              .getAllMentions() );
            final int rightCount = rightSiteMentions.size();
            rightSiteMentions.removeAll( leftSiteMentions );
            final int common = rightCount - rightSiteMentions.size();
            final int leftScore = leftSiteMentions.size() - common;
            final int rightScore = rightCount - common;
            if ( leftScore >= ROOTS_SWITCH_FACTOR * rightScore ) {
               // move right to left
//               LOGGER.info( "\n!! Move Right " + rightScore + "\n" + right.toString() +
//                            "\nto Left " + leftScore + "\n" + left.toString() );
               toBeMovedMap.computeIfAbsent( right, l -> new HashSet<>() ).add( left );
            } else if ( rightScore >= ROOTS_SWITCH_FACTOR * leftScore ) {
               // move left to right
//               LOGGER.info( "\n!! Move Left " + leftScore + "\n" + left.toString() +
//                            "\nto Right " + rightScore + "\n" + right.toString() );
               toBeMovedMap.computeIfAbsent( left, l -> new HashSet<>() ).add( right );
//            } else {
//               LOGGER.info( "\n!!!! Not Moving Left " + leftScore + "\n" + left.toString() +
//                            "\nor Right " + rightScore + "\n" + right.toString() );
            }
         }
      }
      for ( Map.Entry<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMoved : toBeMovedMap.entrySet() ) {
         toBeMoved.getValue().forEach( t -> t.copyHere( toBeMoved.getKey() ) );
      }
      toBeMovedMap.keySet().forEach( SiteNeoplasmBin::invalidate );
      getSiteTypeBin( lateralityType1, siteType ).clean();
      getSiteTypeBin( lateralityType2, siteType ).clean();
   }

   void improveLateralities( final LateralityType lateralityType1,
                             final LateralityType lateralityType2,
                             final SiteType siteType1,
                             final SiteType siteType2,
                             final Map<String, Collection<String>> allUriRoots,
                             final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities "
//                   + lateralityType1 + " " + siteType1 + " "
//                   + lateralityType2 + " " + siteType2 + " ..." );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftToRightScoresMap
            = getSiteTypeBin( lateralityType1, siteType1 )
            .scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( lateralityType2, siteType2 ) );
      // Create map of left to right bins to be copied.
      final Map<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMovedMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>>
            leftToRightScores : leftToRightScoresMap.entrySet() ) {
         final SiteNeoplasmBin left = leftToRightScores.getKey();
//         final long leftScore = left.getSimpleWeight();
         final Collection<Mention> leftSiteMentions = new HashSet<>( left.getSiteChain()
                                                                         .getAllMentions() );
         for ( SiteNeoplasmBin right : leftToRightScores.getValue()
                                                        .getValue() ) {
//            final long rightScore = right.getSimpleWeight();
            final Collection<Mention> rightSiteMentions = new HashSet<>( right.getSiteChain()
                                                                              .getAllMentions() );
            final int rightCount = rightSiteMentions.size();
            rightSiteMentions.removeAll( leftSiteMentions );
            final int common = rightCount - rightSiteMentions.size();
            final int leftScore = leftSiteMentions.size() - common;
            final int rightScore = rightCount - common;
            if ( leftScore >= SWITCH_FACTOR * rightScore ) {
               // move right to left
//               LOGGER.info( "\n!! Move Right " + rightScore + "\n" + right.toString() +
//                            "\nto Left " + leftScore + "\n" + left.toString() );
               toBeMovedMap.computeIfAbsent( right, l -> new HashSet<>() ).add( left );
            } else if ( rightScore >= SWITCH_FACTOR * leftScore ) {
               // move left to right
//               LOGGER.info( "\n!! Move Left " + leftScore + "\n" + left.toString() +
//                            "\nto Right " + rightScore + "\n" + right.toString() );
               toBeMovedMap.computeIfAbsent( left, l -> new HashSet<>() ).add( right );
//            } else {
//               LOGGER.info( "\n!!!! Not Moving Left " + leftScore + "\n" + left.toString() +
//                            "\nor Right " + rightScore + "\n" + right.toString() );
            }
         }
      }
      for ( Map.Entry<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMoved : toBeMovedMap.entrySet() ) {
         toBeMoved.getValue().forEach( t -> t.copyHere( toBeMoved.getKey() ) );
      }
      toBeMovedMap.keySet().forEach( SiteNeoplasmBin::invalidate );
      getSiteTypeBin( lateralityType1, siteType1 ).clean();
      getSiteTypeBin( lateralityType2, siteType2 ).clean();
      final Collection<SiteNeoplasmBin> notMoved
            = new HashSet<>( getSiteTypeBin( lateralityType1, siteType1 ).getSiteNeoplasmBins() );
      notMoved.addAll( getSiteTypeBin( lateralityType2, siteType2 ).getSiteNeoplasmBins() );
      notMoved.removeAll( toBeMovedMap.keySet() );
      improveLateralitiesByBranch( lateralityType1, lateralityType2, siteType1, siteType2, notMoved, allUriRoots,
                                   allUriBranches );
   }

   void improveLateralitiesByBranch( final LateralityType lateralityType1,
                                     final LateralityType lateralityType2,
                                     final SiteType siteType1,
                                     final SiteType siteType2,
                                     final Collection<SiteNeoplasmBin> notMoved,
                                     final Map<String,Collection<String>> allUriRoots,
                                     final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!! Assertion Bin Improving Lateralities By Branch "
//                   + lateralityType1 + " " + siteType1 + " "
//                   + lateralityType2 + " " + siteType2 + " ..." );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftToRightScoresMap
//            = getSiteTypeBin( RIGHT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteType ) );
            = getSiteTypeBin( lateralityType1, siteType1 )
            .scoreBestMatchingSiteNeoplasmBinsByRoots( getSiteTypeBin( lateralityType2,
                                                                       siteType2 ),
                                                       notMoved,
                                                       allUriRoots,
                                                       allUriBranches );
      // Create map of left to right bins to be copied.
      final Map<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMovedMap = new HashMap<>();
      for ( Map.Entry<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>>
            leftToRightScores : leftToRightScoresMap.entrySet() ) {
         final SiteNeoplasmBin left = leftToRightScores.getKey();
//         final long leftScore = left.getSimpleWeight();
         final Collection<Mention> leftSiteMentions = new HashSet<>( left.getSiteChain()
                                                                         .getAllMentions() );
         for ( SiteNeoplasmBin right : leftToRightScores.getValue()
                                                        .getValue() ) {
//            final long rightScore = right.getSimpleWeight();
            final Collection<Mention> rightSiteMentions = new HashSet<>( right.getSiteChain()
                                                                              .getAllMentions() );
            final int rightCount = rightSiteMentions.size();
            rightSiteMentions.removeAll( leftSiteMentions );
            final int common = rightCount - rightSiteMentions.size();
            final int leftScore = leftSiteMentions.size() - common;
            final int rightScore = rightCount - common;
            if ( leftScore >= ROOTS_SWITCH_FACTOR * rightScore ) {
               // move right to left
//               LOGGER.info( "\n!! Move Right " + rightScore + "\n" + right.toString() +
//                            "\nto Left " + leftScore + "\n" + left.toString() );
               toBeMovedMap.computeIfAbsent( right, l -> new HashSet<>() ).add( left );
            } else if ( rightScore >= ROOTS_SWITCH_FACTOR * leftScore ) {
               // move left to right
//               LOGGER.info( "\n!! Move Left " + leftScore + "\n" + left.toString() +
//                            "\nto Right " + rightScore + "\n" + right.toString() );
               toBeMovedMap.computeIfAbsent( left, l -> new HashSet<>() ).add( right );
//            } else {
//               LOGGER.info( "\n!!!! Not Moving Left " + leftScore + "\n" + left.toString() +
//                            "\nor Right " + rightScore + "\n" + right.toString() );
            }
         }
      }
      for ( Map.Entry<SiteNeoplasmBin,Collection<SiteNeoplasmBin>> toBeMoved : toBeMovedMap.entrySet() ) {
         toBeMoved.getValue().forEach( t -> t.copyHere( toBeMoved.getKey() ) );
      }
      toBeMovedMap.keySet().forEach( SiteNeoplasmBin::invalidate );
      getSiteTypeBin( lateralityType1, siteType1 ).clean();
      getSiteTypeBin( lateralityType2, siteType2 ).clean();
   }







   public void mergeExtents( final Map<Mention,Map<String,Collection<Mention>>> mentionRelations ) {
//      LOGGER.info( "!!!!!  MERGING EXTENTS ..." );
      getOrCreateLateralityTypeBins().values()
                                     .forEach( b -> b.mergeExtents( mentionRelations ) );
//      getSiteNeoplasmBins().forEach( LOGGER::info );
   }


   // Collection of all unique cancer and tumor neoplasm chains and their valid site uris.
   static private class DiagnosisChains {
      private final Map<NeoplasmChain,Collection<NeoplasmChain>> _cancerTumorsMap;
      private final Map<NeoplasmChain,Collection<String>> _cancerSiteUrisMap;
      private final Map<NeoplasmChain,Collection<String>> _tumorSiteUrisMap;
      private DiagnosisChains( final Map<NeoplasmChain,Collection<NeoplasmChain>> cancerTumorsMap,
                     final Map<NeoplasmChain,Collection<String>> cancerSiteUrisMap,
                     final Map<NeoplasmChain,Collection<String>> tumorSiteUrisMap ) {
         _cancerTumorsMap = cancerTumorsMap;
         _cancerSiteUrisMap = cancerSiteUrisMap;
         _tumorSiteUrisMap = tumorSiteUrisMap;
      }
   }

   // Collection of all unique cancer and tumor neoplasm chains and their valid site uris.
   static public class DiagnosisConcepts {
      public final Map<ConceptAggregate,Collection<ConceptAggregate>> _cancerTumorsMap;
      public final Map<ConceptAggregate,Collection<String>> _cancerSiteUrisMap;
      public final Map<ConceptAggregate,Collection<String>> _tumorSiteUrisMap;
      DiagnosisConcepts( final Map<ConceptAggregate,Collection<ConceptAggregate>> cancerTumorsMap,
                       final Map<ConceptAggregate,Collection<String>> cancerSiteUrisMap,
                       final Map<ConceptAggregate,Collection<String>> tumorSiteUrisMap ) {
         _cancerTumorsMap = cancerTumorsMap;
         _cancerSiteUrisMap = cancerSiteUrisMap;
         _tumorSiteUrisMap = tumorSiteUrisMap;
      }
      public Collection<ConceptAggregate> getLoneTumors() {
         final Collection<ConceptAggregate> loneTumors = new HashSet<>( _tumorSiteUrisMap.keySet() );
         _cancerTumorsMap.values().forEach( loneTumors::removeAll );
         return loneTumors;
      }
   }


   public void mergeSiteNeoplasmChains( final Map<String,Collection<String>> allUriBranches ) {
//      LOGGER.info( "!!!!!!!   MERGING SITE NEOPLASM CHAINS ...." );
      getSiteNeoplasmBins().forEach( b -> b.mergeNeoplasmChains( allUriBranches ) );
//      getSiteNeoplasmBins().forEach( LOGGER::info );
   }

   public DiagnosisConcepts createDiagnosisConcepts( final String patientId,
                                                      final Map<Mention, String> patientMentionNoteIds,
                                                      final Map<String, Collection<String>> allUriRoots ) {
      return createDiagnosisConcepts( createDiagnosisChains(),
                                      patientId,
                                      patientMentionNoteIds,
                                      allUriRoots );
   }



   public DiagnosisChains createDiagnosisChains() {
      // Collect all unique cancer and tumor neoplasm chains and their valid site uris.
      final Map<NeoplasmChain, Collection<NeoplasmChain>> cancerTumorsMap = new HashMap<>();
      final Map<NeoplasmChain, Collection<String>> cancerSiteUrisMap = new HashMap<>();
      final Map<NeoplasmChain, Collection<String>> tumorSiteUrisMap = new HashMap<>();
      for ( LateralityTypeBin lateralityTypeBin : _lateralityBins.values() ) {
         final String lateralityUri = lateralityTypeBin.getLateralityType()._uri;
         for ( SiteNeoplasmBin siteNeoplasmBin : lateralityTypeBin.getSiteNeoplasmBins() ) {
            if ( !siteNeoplasmBin.isValid() ) {
               continue;
            }
            final Collection<String> siteUris = new HashSet<>( siteNeoplasmBin.getSiteChain()
                                                               .getChainUris() );
            siteUris.add( lateralityUri );

            final Collection<NeoplasmChain> cancers = siteNeoplasmBin.getNeoplasmChains( CANCER );
            final Collection<NeoplasmChain> tumors = siteNeoplasmBin.getNeoplasmChains( TUMOR );
            for ( NeoplasmChain cancer : cancers ) {
               cancerTumorsMap.computeIfAbsent( cancer, t -> new HashSet<>() )
                              .addAll( tumors );
               cancerSiteUrisMap.computeIfAbsent( cancer, s -> new HashSet<>() )
                                .addAll( siteUris );
            }
            tumors.forEach( t -> tumorSiteUrisMap.computeIfAbsent( t, s -> new HashSet<>() )
                                                 .addAll( siteUris ) );
         }
      }
      return new DiagnosisChains( cancerTumorsMap, cancerSiteUrisMap, tumorSiteUrisMap );
   }


   public DiagnosisConcepts createDiagnosisConcepts( final DiagnosisChains diagnosisChains,
         final String patientId,
         final Map<Mention, String> patientMentionNoteIds,
         final Map<String, Collection<String>> allUriRoots ) {
      final Map<NeoplasmChain,ConceptAggregate> allCancerConcepts
            = createChainConcepts( diagnosisChains._cancerSiteUrisMap.keySet(),
                                         patientId,
                                         patientMentionNoteIds,
                                         allUriRoots );
      final Map<NeoplasmChain,ConceptAggregate> allTumorConcepts
            = createChainConcepts(  diagnosisChains._tumorSiteUrisMap.keySet(),
                                        patientId,
                                        patientMentionNoteIds,
                                        allUriRoots );
      final Map<ConceptAggregate,Collection<ConceptAggregate>> diagnosisMap = new HashMap<>();
      for ( Map.Entry<NeoplasmChain,Collection<NeoplasmChain>> cancerTumors
            : diagnosisChains._cancerTumorsMap.entrySet() ) {
         final ConceptAggregate cancer = allCancerConcepts.get( cancerTumors.getKey() );
         final Collection<ConceptAggregate> tumors = cancerTumors.getValue()
                                                                 .stream()
                                                                 .map( allTumorConcepts::get )
                                                                 .collect( Collectors.toSet() );
         diagnosisMap.put( cancer, tumors );
      }
      final Map<ConceptAggregate,Collection<String>> cancerSiteUrisMap
            = mapConceptSiteUris( allCancerConcepts, diagnosisChains._cancerSiteUrisMap );
      final Map<ConceptAggregate,Collection<String>> tumorSiteUrisMap
            = mapConceptSiteUris( allTumorConcepts, diagnosisChains._tumorSiteUrisMap );
      return new DiagnosisConcepts( diagnosisMap, cancerSiteUrisMap, tumorSiteUrisMap );
   }

   static private Map<NeoplasmChain,ConceptAggregate> createChainConcepts(
         final Collection<NeoplasmChain> neoplasmChains,
         final String patientId,
         final Map<Mention, String> patientMentionNoteIds,
         final Map<String, Collection<String>> allUriRoots ) {
      return neoplasmChains.stream()
                           .filter( NeoplasmChain::isValid )
                            .collect( Collectors.toMap( Function.identity(),
                                                        c -> c.createConceptAggregate( patientId,
                                                                                       patientMentionNoteIds,
                                                                                       allUriRoots ) ) );
   }


   public Map<Mention,Collection<ConceptAggregate>> mapSiteConcepts( final String patientId,
                                                                    final Map<Mention, String> patientMentionNoteIds,
                                                                    final Map<String, Collection<String>> allUriRoots ) {
      final Map<String,Collection<SiteChain>> uriSiteChainsMap = new HashMap<>();
//      final Map<SiteChain,Integer> chainSizeMap = new HashMap<>();
      final Map<SiteChain,Collection<SiteChain>> chainsToMergeMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : getSiteNeoplasmBins() ) {
         if ( !siteNeoplasmBin.isValid() ) {
            continue;
         }
         final SiteChain siteChain = siteNeoplasmBin.getSiteChain();
         siteChain.getChainUris()
                  .forEach( u -> uriSiteChainsMap.computeIfAbsent( u, c -> new HashSet<>() )
                                                .add( siteChain ) );
//         chainSizeMap.put( siteChain, siteChain.getAllMentions().size() );
         chainsToMergeMap.computeIfAbsent( siteChain, c -> new HashSet<>() ).add( siteChain );
      }
      for ( Map.Entry<String,Collection<SiteChain>> uriSiteChains : uriSiteChainsMap.entrySet() ) {
         final Collection<SiteChain> matchingHeads = uriSiteChains.getValue().stream()
                                                               .filter( c -> c.getHeadUri()
                                                                              .equals( uriSiteChains.getKey() ) )
                                                               .collect( Collectors.toSet() );
         if ( matchingHeads.isEmpty() ) {
            continue;
         }
         for ( SiteChain head : matchingHeads ) {
            chainsToMergeMap.computeIfAbsent( head, c -> new HashSet<>() ).addAll( matchingHeads );
         }
      }
      final Map<String,Long> uriCountsMap = getSiteNeoplasmBins().stream()
                                                                 .map( SiteNeoplasmBin::getSiteChain )
                                                                 .map( SiteChain::getAllMentions )
                                                                 .flatMap( Collection::stream )
                                                                 .map( Mention::getClassUri )
                                                                 .collect(
                                                                       Collectors.groupingBy(
                                                                             Function.identity(),
                                                                             Collectors.counting() ) );
      final Map<Long,Collection<String>> countsPerUri = new HashMap<>();
      for ( Map.Entry<String,Long> uriCount : uriCountsMap.entrySet() ) {
         countsPerUri.computeIfAbsent( uriCount.getValue(), l -> new HashSet<>() )
                     .add( uriCount.getKey() );
      }
      final List<Long> countList = new ArrayList<>( countsPerUri.keySet() );
      countList.sort( Comparator.comparingLong( l -> (long) l ).reversed() );
      for ( Long counts : countList ) {
         for ( String uri : countsPerUri.get( counts ) ) {
            final Collection<SiteChain> siteChains = uriSiteChainsMap.get( uri );
            final Collection<SiteChain> tooFewMentions = new HashSet<>();
            for ( SiteChain siteChain : siteChains ) {
               final int siteMentions = siteChain.getUriSites()
                                                 .get( uri )
                                                 .size();
               final int totalMentions = siteChain.getAllMentions()
                                                  .size();
               if ( siteMentions * 3 < totalMentions ) {
                  tooFewMentions.add( siteChain );
               }
            }
            siteChains.removeAll( tooFewMentions );
            final Collection<SiteChain> allMergers = new HashSet<>();
            siteChains.forEach( c -> allMergers.addAll( chainsToMergeMap.get( c ) ) );
            siteChains.forEach( c -> chainsToMergeMap.put( c, allMergers ) );
         }
      }
      final Collection<SiteChain> handled = new HashSet<>();
      final Collection<SiteChain> goodSiteChains = new HashSet<>();
      for ( Map.Entry<SiteChain,Collection<SiteChain>> chainsToMerge : chainsToMergeMap.entrySet() ) {
         if ( handled.contains( chainsToMerge.getKey() ) ) {
            continue;
         }
         chainsToMerge.getValue().stream()
                      .filter( c -> !chainsToMerge.getKey().equals( c ) )
                      .forEach( chainsToMerge.getKey()::copyHere );
         goodSiteChains.add( chainsToMerge.getKey() );
         handled.addAll( chainsToMerge.getValue() );
      }
      final Map<Mention,Collection<ConceptAggregate>> mentionConcepts = new HashMap<>();
      for ( SiteChain siteChain : goodSiteChains ) {
         final ConceptAggregate site = siteChain.createConceptAggregate( patientId,
                                                                         patientMentionNoteIds,
                                                                         allUriRoots );
         for ( Mention mention : site.getMentions() ) {
            mentionConcepts.computeIfAbsent( mention, m -> new HashSet<>() )
                           .add( site );
         }
      }
      return mentionConcepts;
   }




   public Map<Mention,Collection<ConceptAggregate>> mapSiteConceptsOrig( final String patientId,
                                                                     final Map<Mention, String> patientMentionNoteIds,
                                                                     final Map<String, Collection<String>> allUriRoots ) {
      final Map<String,Collection<SiteChain>> uriSiteChainsMap = new HashMap<>();
      for ( LateralityTypeBin lateralityTypeBin : _lateralityBins.values() ) {
         for ( SiteNeoplasmBin siteNeoplasmBin : lateralityTypeBin.getSiteNeoplasmBins() ) {
            if ( !siteNeoplasmBin.isValid() ) {
               continue;
            }
            final SiteChain siteChain = siteNeoplasmBin.getSiteChain();
            siteChain.getChainUris()
                     .forEach( u -> uriSiteChainsMap.computeIfAbsent( u, c -> new HashSet<>() )
                                                    .add( siteChain ) );
         }
      }
      final Collection<SiteChain> headSites = new HashSet<>();
      final Collection<SiteChain> goodSiteChains = new HashSet<>();
      for ( Map.Entry<String,Collection<SiteChain>> uriSiteChains : uriSiteChainsMap.entrySet() ) {
         if ( uriSiteChains.getValue()
                           .isEmpty() ) {
            continue;
//         } else if ( uriSiteChains.getValue()
//                                  .size() == 1 ) {
//            goodSiteChains.addAll( uriSiteChains.getValue() );
//            headSites.addAll( uriSiteChains.getValue() );
//            continue;
         }
         // headUri
         final Collection<SiteChain> headChains = uriSiteChains.getValue()
                                                               .stream()
                                                               .filter( c -> c.getHeadUri()
                                                                              .equals( uriSiteChains.getKey() ) )
                                                               .collect( Collectors.toSet() );
         if ( headChains.isEmpty() ) {
            continue;
         }
//         LOGGER.info( uriSiteChains.getKey() + " Merging equal SiteChains (head) " + headChains
//               .stream()
//               .map( SiteChain::toString )
//               .collect( Collectors.joining( "\n" ) ) );
         final List<SiteChain> siteChainList = new ArrayList<>( headChains );
         for ( int i = 1; i < siteChainList.size(); i++ ) {
            siteChainList.get( 0 )
                         .copyHere( siteChainList.get( i ) );
         }
         goodSiteChains.add( siteChainList.get( 0 ) );
         headSites.addAll( headChains );
      }
      final Map<SiteChain,SiteChain> oldToNewMap = new HashMap<>();
      for ( Map.Entry<String,Collection<SiteChain>> uriSiteChains : uriSiteChainsMap.entrySet() ) {
         uriSiteChains.getValue()
                      .removeAll( headSites );
         if ( uriSiteChains.getValue()
                           .isEmpty() ) {
            continue;
         } else if ( uriSiteChains.getValue()
                                  .size() == 1 ) {
//            goodSiteChains.addAll( uriSiteChains.getValue() );
            continue;
         }
         // number of mentions with the uri
         final int maxM = uriSiteChains.getValue().stream()
                                       .mapToInt( c -> c.getUriSites()
                                                        .get( uriSiteChains.getKey() )
                                                        .size() )
                                       .max()
                                       .orElse( 0 );
         final Collection<SiteChain> biggerChains = uriSiteChains.getValue().stream()
                                                                 .filter( c -> c.getUriSites()
                                                                                .get( uriSiteChains.getKey() )
                                                                                .size() >= maxM )
                                                                 .collect( Collectors.toSet() );
         if ( biggerChains.size() == 1 ) {
//            LOGGER.info(  uriSiteChains.getKey() +" Moving by uri mention SiteChains (bigger) " + uriSiteChains.getValue()
//                                                                                                               .stream()
//                                                                                                               .filter( c -> !biggerChains.contains( c ) )
//                                                                                                               .map( SiteChain::toString )
//                                                                                                               .collect( Collectors.joining( "\n" ) )
//                          + " into " + biggerChains.stream()
//                                                   .map( SiteChain::toString )
//                                                   .collect( Collectors.joining( "\n" ) ));
            uriSiteChains.getValue()
                         .stream()
                         .filter( c -> !biggerChains.contains( c ) )
                         .forEach( c -> biggerChains.forEach( b -> b.copyHere( c ) ) );
            goodSiteChains.addAll( biggerChains );
            continue;
         }
         // Total number of mentions
         final int maxM2 = biggerChains.stream()
                                       .mapToInt( c -> c.getUriSites()
                                                        .values()
                                                        .stream()
                                                        .mapToInt( Collection::size )
                                                        .sum() )
                                       .max()
                                       .orElse( 0 );
         final Collection<SiteChain> biggestChains = biggerChains.stream()
                                                                 .filter( c -> c.getChainUris().size() == maxM2 )
                                                                 .collect( Collectors.toSet() );
         if ( biggestChains.size() == 1 ) {
//            LOGGER.info(  uriSiteChains.getKey() +" Moving by mentions SiteChains (biggest) " + uriSiteChains.getValue()
//                                                                                                             .stream()
//                                                                                                             .filter( c -> !biggestChains.contains( c ) )
//                                                                                                             .map( SiteChain::toString )
//                                                                                                             .collect( Collectors.joining( "\n" ) )
//                          + " into " + biggestChains.stream()
//                                                    .map( SiteChain::toString )
//                                                    .collect( Collectors.joining( "\n" ) ));
            uriSiteChains.getValue()
                         .stream()
                         .filter( c -> !biggestChains.contains( c ) )
                         .forEach( c -> biggestChains.forEach( b -> b.copyHere( c ) ) );
            goodSiteChains.addAll( biggestChains);
            continue;
         }
         // number of uris
         final int max = uriSiteChains.getValue()
                                      .stream()
                                      .map( SiteChain::getChainUris )
                                      .mapToInt( Collection::size )
                                      .max().orElse( 0 );
         final Collection<SiteChain> bigChains = uriSiteChains.getValue()
                                                              .stream()
                                                              .filter( c -> c.getChainUris().size() == max )
                                                              .collect( Collectors.toSet() );
         if ( bigChains.size() == 1 ) {
//            LOGGER.info(  uriSiteChains.getKey() +" Moving by uri SiteChains (big) " + uriSiteChains.getValue()
//                                                                                                    .stream()
//                                                                                                    .filter( c -> !bigChains.contains( c ) )
//                                                                                                    .map( SiteChain::toString )
//                                                                                                    .collect( Collectors.joining( "\n" ) )
//                          + " into " + bigChains.stream()
//                                                .map( SiteChain::toString )
//                                                .collect( Collectors.joining( "\n" ) ));
            uriSiteChains.getValue()
                         .stream()
                         .filter( c -> !bigChains.contains( c ) )
                         .forEach( c -> bigChains.forEach( b -> b.copyHere( c ) ) );
            goodSiteChains.addAll( bigChains );
            continue;
         }

         // The siteChains are equivalent, so just merge them into the first chain.
//         LOGGER.info( "Merging equal SiteChains " + uriSiteChains.getValue()
//                                                                      .stream()
//                                                                      .map( SiteChain::toString )
//                                                                      .collect( Collectors.joining( "\n" ) ) );
//         final List<SiteChain> siteChainList = new ArrayList<>( uriSiteChains.getValue() );
//         for ( int i=1; i< siteChainList.size(); i++  ) {
//            siteChainList.get( 0 ).copyHere( siteChainList.get( i ) );
//         }
//         goodSiteChains.add( siteChainList.get( 0 ) );
      }
//      LOGGER.info( "!!!!!!!!  Site Chains ..." );
//      goodSiteChains.forEach( LOGGER::info );
      final Map<Mention,Collection<ConceptAggregate>> mentionConcepts = new HashMap<>();
      for ( SiteChain siteChain : goodSiteChains ) {
         final ConceptAggregate site = siteChain.createConceptAggregate( patientId,
                                                                         patientMentionNoteIds,
                                                                         allUriRoots );
         for ( Mention mention : site.getMentions() ) {
            mentionConcepts.computeIfAbsent( mention, m -> new HashSet<>() )
                           .add( site );
         }
      }
      return mentionConcepts;
   }










   static private Map<ConceptAggregate,Collection<String>> mapConceptSiteUris(
         final Map<NeoplasmChain,ConceptAggregate> chainConceptsMap,
         final Map<NeoplasmChain,Collection<String>> chainSiteUrisMap ) {
      return chainConceptsMap.entrySet()
                            .stream()
                            .collect( Collectors.toMap( Map.Entry::getValue,
                                                        e -> chainSiteUrisMap.get( e.getKey() ) ) );
   }


   public Collection<SiteNeoplasmBin> getSiteNeoplasmBins() {
      return getOrCreateLateralityTypeBins().values()
                                            .stream()
                                            .map( LateralityTypeBin::getSiteNeoplasmBins )
                                            .flatMap( Collection::stream )
                                            .collect( Collectors.toSet() );
   }


}
