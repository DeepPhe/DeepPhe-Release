package org.healthnlp.deepphe.summary.neoplasm;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.histology.Histology;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralityCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.Topography;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {6/9/2022}
 */
final public class ByAttributeMergerAborted {

   private ByAttributeMergerAborted() {}


   static private Collection<ConceptAggregate> mergeCancers( final Collection<ConceptAggregate> neoplasms,
                                                             final Collection<ConceptAggregate> allConcepts ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massUris = UriConstants.getMassUris( graphDb );
      final Predicate<ConceptAggregate> isMass = c -> c.getAllUris().stream().anyMatch( massUris::contains );

      final Collection<ConceptAggregate> tumors = neoplasms.stream().filter( isMass ).collect( Collectors.toSet() );
      final Collection<ConceptAggregate> cancers = new HashSet<>( neoplasms );
      cancers.removeAll( tumors );
      // sited neoplasms are a map of tumors by laterality within topo.  e.g. Breast<Right,tumors>  ==  C50<1,tumors>
//      final Map<String, Map<String,ConceptAggregate>> sitedTumors = siteMergeNeoplasms( tumors, allConcepts );
//      final Map<String, Map<String,ConceptAggregate>> sitedCancers = siteMergeNeoplasms( cancers, allConcepts );

      // sited neoplasms are a map of tumors by laterality within topo.  e.g. Breast<Right,tumor>  ==  C50<1,tumor>
      final Map<String, Map<String,Collection<ConceptAggregate>>> sitedTumors
            = collectBySite( tumors, allConcepts );
      final Map<String, Map<String,Collection<ConceptAggregate>>> sitedCancers
            = collectBySite( cancers, allConcepts );

      final Map<String,Collection<ConceptAggregate>> histoTumors
            = collectByHistology( tumors, allConcepts );
      final Map<String,Collection<ConceptAggregate>> histoCancers
            = collectByHistology( cancers, allConcepts );

      // Now we have collections of ConceptAggregates sorted by site and also sorted by histology.
      // The tricky part is joining them together.
      // Try distribution by Histo first:  BrCa<Breast<Right,tumors>> == 850<C50<1,tumors>>


      return null;
   }

   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting by site (major topography, laterality).
   //
   /////////////////////////////////////////////////////////////////////////


//   /**
//    * Merge neoplasms based entirely on location information : site and laterality.
//    * @param neoplasms all neoplasm instances for the patient / doc.
//    * @param allConcepts all instances for the patient / doc.
//    * @return a new collection of (possibly) merged neoplasms.
//    */
//   static private Map<String, Map<String,ConceptAggregate>> siteMergeNeoplasms(
//         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
//      if ( neoplasms.isEmpty() ) {
//         return Collections.emptyMap();
//      }
//      final Map<String, Map<String,Collection<ConceptAggregate>>> sitedConcepts = sortBySite( allConcepts, neoplasms );
//      // map of tumors by laterality within topo.  e.g. Breast<Right,tumor>  ==  C50<1,tumor>
//      // Todo :  use minor site?  That may be going too far.
//
//      final Map<String, Map<String,TumorContainer>> sitedContainers = new HashMap<>();
//      for ( Map.Entry<String,Map<String,Collection<ConceptAggregate>>> siteConcepts
//            : sitedConcepts.entrySet() ) {
//         final String topo = siteConcepts.getKey();
//         for ( Map.Entry<String,Collection<ConceptAggregate>> latConcepts
//               : siteConcepts.getValue().entrySet() ) {
//            sitedContainers.computeIfAbsent( topo, t -> new HashMap<>() )
//                             .put( latConcepts.getKey(), new TumorContainer( latConcepts.getValue() ) );
//         }
//      }
//      // todo ? what is this for?  Was it necessary for old hasLat re-assignments?
////      for ( ConceptAggregate concept : allConcepts ) {
////         final Map<String,Collection<ConceptAggregate>> oldRelated = concept.getRelatedConceptMap();
////         for ( Map.Entry<String,Collection<ConceptAggregate>> oldRelations : oldRelated.entrySet() ) {
////            final Collection<ConceptAggregate> removals = new HashSet<>();
////            for ( ConceptAggregate concept1 : oldRelations.getValue() ) {
////               if ( !allConcepts.contains( concept1 ) ) {
////                  removals.add( concept1 );
////               }
////            }
////            oldRelations.getValue().removeAll( removals );
////         }
////      }
//
//      // Now we have created TumorContainers.  Create a Merged ConceptInstance from each.
//      final Map<String, Map<String,ConceptAggregate>> mergedConcepts = new HashMap<>();
//      for ( Map.Entry<String, Map<String,TumorContainer>> siteContainers
//            : sitedContainers.entrySet() ) {
//         final String topo = siteContainers.getKey();
//         for ( Map.Entry<String,TumorContainer> latContainers
//               : siteContainers.getValue().entrySet() ) {
//            mergedConcepts.computeIfAbsent( topo, t -> new HashMap<>() )
//                             .put( latContainers.getKey(), latContainers.getValue().createMergedConcept( allConcepts ) );
//         }
//      }
//      return mergedConcepts;
//   }


   /**
    * Merge neoplasms based entirely on location information : site and laterality.
    * @param allNeoplasms all neoplasm instances for the patient / doc.
    * @param allConcepts all instances for the patient / doc.
    * @return a new collection of (possibly) merged neoplasms.
    */
   static private Map<String, Map<String,Collection<ConceptAggregate>>> collectBySite(
         final Collection<ConceptAggregate> allNeoplasms, final Collection<ConceptAggregate> allConcepts ) {
      if ( allNeoplasms.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<String, Map<String,Collection<ConceptAggregate>>> sitedConcepts = sortBySite( allNeoplasms, allConcepts );
      // map of tumors by laterality within topo.  e.g. Breast<Right,tumor>  ==  C50<1,tumor>
      // Todo :  use minor site?  That may be going too far.

      trimSites( sitedConcepts );
      return sitedConcepts;
   }


   static private Map<String, Map<String,Collection<ConceptAggregate>>> sortBySite(
         final Collection<ConceptAggregate> allNeoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> topoNeoplasms = sortByTopo( allNeoplasms, allConcepts );
      final Map<String, Map<String,Collection<ConceptAggregate>>> topoLateralNeoplasms
            = new HashMap<>( topoNeoplasms.size() );
      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : topoNeoplasms.entrySet() ) {
         final String topo = entry.getKey();
         final Map<String,Collection<ConceptAggregate>> lateralNeoplasms = new HashMap<>();
         for ( ConceptAggregate neoplasm : entry.getValue() ) {
            final String laterality = getLaterality( topo, neoplasm, allNeoplasms, allConcepts );
            lateralNeoplasms.computeIfAbsent( laterality, l -> new ArrayList<>() ).add( neoplasm );
         }
         topoLateralNeoplasms.put( topo, lateralNeoplasms );
      }
      distributeUnkownTopos( topoLateralNeoplasms );
      distributeOrphanLaterals( topoLateralNeoplasms );
      return topoLateralNeoplasms;
   }

   /**
    * ICDO code is a higher level than unique uri.  e.g. br, left br, nipple, etc.  This should allow better merges.
    * @param allNeoplasms -
    * @param allConcepts -
    * @return map of major topo codes to list of concepts with those topo codes.
    */
   static private Map<String, Collection<ConceptAggregate>> sortByTopo(
         final Collection<ConceptAggregate> allNeoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> topoConcepts = new HashMap<>();
      for ( ConceptAggregate neoplasm : allNeoplasms ) {
         final Topography topography = new Topography( neoplasm, allConcepts );
         final String bestCode = topography.getBestMajorTopoCode();
         topoConcepts.computeIfAbsent( bestCode, c -> new ArrayList<>() ).add( neoplasm );
      }
      return topoConcepts;
   }


   // deal with C80 : Unknown Site.
   static private void distributeUnkownTopos(
         final Map<String, Map<String,Collection<ConceptAggregate>>> sitedConcepts ) {
      final Map<String,Collection<ConceptAggregate>> noSiteConcepts = sitedConcepts.get( "C80" );
      if ( noSiteConcepts == null || sitedConcepts.size() <= 1 ) {
         return;
      }
      if ( sitedConcepts.size() == 2 ) {
         for ( Map.Entry<String, Map<String, Collection<ConceptAggregate>>> siteConcepts : sitedConcepts.entrySet() ) {
            if ( siteConcepts.getKey().equals( "C80" ) ) {
               continue;
            }
            for ( Map.Entry<String,Collection<ConceptAggregate>> latConcepts : noSiteConcepts.entrySet() ) {
               siteConcepts.getValue()
                               .computeIfAbsent( latConcepts.getKey(), u -> new HashSet<>() )
                               .addAll( latConcepts.getValue() );
            }
         }
         sitedConcepts.remove( "C80" );
//      } else {
         // Todo  : distribute properties across all sites?  Get Histology (and behavior?) and distribute by that?
      }
   }

   // deal with 0 : no laterality.
   static private void distributeOrphanLaterals(
         final Map<String, Map<String,Collection<ConceptAggregate>>> sitedConcepts ) {
      for ( Map<String,Collection<ConceptAggregate>> latConcepts : sitedConcepts.values() ) {
         if ( latConcepts.size() == 1 || !latConcepts.containsKey( "0" ) ) {
            continue;
         }
         final Collection<ConceptAggregate> noLatConcepts = latConcepts.get( "0" );
         for ( Map.Entry<String,Collection<ConceptAggregate>> neoplasms : latConcepts.entrySet() ) {
            if ( neoplasms.getKey().equals( "0" ) ) {
               continue;
            }
            neoplasms.getValue().addAll( noLatConcepts );
         }
         latConcepts.remove( "0" );
      }
   }


   static private String getLaterality(
         final String topoCode, final ConceptAggregate neoplasm, final Collection<ConceptAggregate> allNeoplasms,
         final Collection<ConceptAggregate> allConcepts ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topoCode );
      final DefaultAttribute<LateralUriInfoVisitor, LateralityCodeInfoStore> lateralityCode
            = new DefaultAttribute<>( "laterality_code",
                                      neoplasm,
                                      allConcepts,
                                      allNeoplasms,
                                      LateralUriInfoVisitor::new,
                                      LateralityCodeInfoStore::new,
                                      dependencies );
      return lateralityCode.getBestCode();
   }



   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting by histology.
   //
   /////////////////////////////////////////////////////////////////////////


   static private Map<String,Collection<ConceptAggregate>> collectByHistology(
         final Collection<ConceptAggregate> allNeoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> histoNeoplasms = sortByHistology( allNeoplasms, allConcepts );
      distributeUnknownHistos( histoNeoplasms );
      trimHistologies( histoNeoplasms );
      return histoNeoplasms;
   }


   /**
    * ICDO code is a higher level than unique uri.  e.g. br, left br, nipple, etc.  This should allow better merges.
    * @param allNeoplasms -
    * @param allConcepts -
    * @return map of major topo codes to list of concepts with those topo codes.
    */
   static private Map<String, Collection<ConceptAggregate>> sortByHistology(
         final Collection<ConceptAggregate> allNeoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> histologyConcepts = new HashMap<>();
      for ( ConceptAggregate neoplasm : allNeoplasms ) {
         final String histologyCode = getHistologyCode( neoplasm, allNeoplasms, allConcepts );
         final String shortCode = histologyCode.substring( 0, 3 );
         histologyConcepts.computeIfAbsent( shortCode, c -> new ArrayList<>() ).add( neoplasm );
      }
      return histologyConcepts;
   }

   // All 850 = Ductal Carcinoma.  8500 = BrCa in situ or invasive.  8501 = Comedocarcinoma.
   // 8502 = Secretory_Breast_Carcinoma.  8503 = Intraductal_Papillary_Breast_Carcinoma.  etc.
   // todo - should there be a more exact/narrow sort?
   static private String getHistologyCode( final ConceptAggregate neoplasm,
                                       final Collection<ConceptAggregate> allNeoplasms,
                                       final Collection<ConceptAggregate> allConcepts ) {
      final DefaultAttribute<HistologyUriInfoVisitor, HistologyCodeInfoStore> histology
            = new Histology( neoplasm,
                             allConcepts,
                             allNeoplasms );
      return histology.getBestCode();
   }


   // deal with 8010 : Carcinoma NOS.  Should sort by first 3 numbers.
   // All 850 = Ductal Carcinoma.  8500 = BrCa in situ or invasive.  8501 = Comedocarcinoma.
   // 8502 = Secretory_Breast_Carcinoma.  8503 = Intraductal_Papillary_Breast_Carcinoma.  etc.
   // todo - should there be a more exact/narrow sort?
   static private void distributeUnknownHistos(
         final Map<String,Collection<ConceptAggregate>> histologyConcepts ) {
      final Collection<ConceptAggregate> noHistoConcepts = histologyConcepts.get( "801" );
      if ( noHistoConcepts == null || histologyConcepts.size() <= 1 ) {
         return;
      }
      if ( histologyConcepts.size() == 2 ) {
         for ( Map.Entry<String,Collection<ConceptAggregate>> histoConcepts : histologyConcepts.entrySet() ) {
            if ( histoConcepts.getKey().equals( "801" ) ) {
               continue;
            }
            histoConcepts.getValue().addAll( noHistoConcepts );
         }
         histologyConcepts.remove( "801" );
//      } else {
         // Todo  : distribute properties across all histologies?  Get Histology (and behavior?) and distribute by that?
      }
   }


   /////////////////////////////////////////////////////////////////////////
   //
   //    Neoplasm trimming.  If there are many sites, distribute low occurrence sites.
   //
   /////////////////////////////////////////////////////////////////////////

   // Trim by <30% of max mentions
   static private final double topoMentionCutoff = .3;
   static private final double latMentionCutoff = .3;

   static private void trimSites( final Map<String,Map<String,Collection<ConceptAggregate>>> sitedNeoplasms ) {
      trimByTopology( sitedNeoplasms );
      for ( Map<String,Collection<ConceptAggregate>> lateralNeoplasms : sitedNeoplasms.values() ) {
         trimByLaterality( lateralNeoplasms );
      }
   }

   static private void trimByTopology( final Map<String,Map<String,Collection<ConceptAggregate>>> sitedNeoplasms ) {
      if ( sitedNeoplasms.size() <= 1 ) {
         return;
      }
      final Map<String,Integer> topoCounts = new HashMap<>();
      int max = 0;
      int total = 0;
      for ( Map.Entry<String,Map<String,Collection<ConceptAggregate>>> siteNeoplasms : sitedNeoplasms.entrySet() ) {
         final String topo = siteNeoplasms.getKey();
         int count = 0;
         for ( Map.Entry<String,Collection<ConceptAggregate>> latNeoplasms : siteNeoplasms.getValue().entrySet() ) {
            count += latNeoplasms.getValue().stream()
                                 .map( ConceptAggregate::getMentions )
                                 .mapToInt( Collection::size )
                                 .count();
         }
         topoCounts.put( topo, count );
         max = Math.max( max, count );
         total += count;
      }
      if ( topoCounts.size() <= 1 ) {
         return;
      }
      final double cutoff = max * topoMentionCutoff;
      final Collection<String> lowTopos = topoCounts.entrySet().stream()
                                                     .filter( e -> e.getValue() < cutoff )
                                                     .map( Map.Entry::getKey )
                                                     .collect( Collectors.toList() );
      if ( lowTopos.isEmpty() ) {
         return;
      }
      // Add all low topo neoplasm concepts to all higher topo neoplasm concepts.
      // This may end up with neoplasm concepts repeated at different sites,
      // but that should (hopefully) end in better results in the end.
      // For instance, this should minimize biopsy sites while distributing biopsy results.
      // I hope that doesn't bite us with multiple negative vs. one positive results.
      for ( Map.Entry<String,Map<String,Collection<ConceptAggregate>>> siteNeoplasms : sitedNeoplasms.entrySet() ) {
         if ( lowTopos.contains( siteNeoplasms.getKey() ) ) {
            continue;
         }
         final Map<String,Collection<ConceptAggregate>> latsNeoplasms = siteNeoplasms.getValue();
         for ( String lowTopo : lowTopos ) {
            final Map<String,Collection<ConceptAggregate>> lowLats = sitedNeoplasms.get( lowTopo );
            for ( Map.Entry<String,Collection<ConceptAggregate>> lowLat : lowLats.entrySet() ) {
               latsNeoplasms.computeIfAbsent( lowLat.getKey(), l -> new ArrayList<>() ).addAll( lowLat.getValue() );
            }
         }
      }
      sitedNeoplasms.keySet().removeAll( lowTopos );
   }


   static private void trimByLaterality( final Map<String,Collection<ConceptAggregate>> lateralNeoplasms ) {
      if ( lateralNeoplasms.size() <= 1 ) {
         return;
      }
      final Map<String,Integer> latCounts = new HashMap<>();
      int max = 0;
      int total = 0;
      for ( Map.Entry<String,Collection<ConceptAggregate>> latNeoplasms : lateralNeoplasms.entrySet() ) {
         final String lat = latNeoplasms.getKey();
         int count = latNeoplasms.getValue().stream()
                                   .map( ConceptAggregate::getMentions )
                                   .mapToInt( Collection::size )
                                   .sum();
         latCounts.put( lat, count );
         max = Math.max( max, count );
         total += count;
      }
      if ( latCounts.size() <= 1 ) {
         return;
      }
      final double cutoff = max * latMentionCutoff;
      final Collection<String> lowLats = latCounts.entrySet().stream()
                                                      .filter( e -> e.getValue() < cutoff )
                                                      .map( Map.Entry::getKey )
                                                      .collect( Collectors.toList() );
      if ( lowLats.isEmpty() ) {
         return;
      }
      // Add all low lat neoplasm concepts to all higher lat neoplasm concepts.
      // This may end up with neoplasm concepts repeated at different lats,
      // but that should (hopefully) end in better results in the end.
      for ( Map.Entry<String,Collection<ConceptAggregate>> latNeoplasms : lateralNeoplasms.entrySet() ) {
         if ( lowLats.contains( latNeoplasms.getKey() ) ) {
            continue;
         }
         for ( String lowHisto : lowLats ) {
            latNeoplasms.getValue().addAll( lateralNeoplasms.get( lowHisto ) );
         }
      }
      lateralNeoplasms.keySet().removeAll( lowLats );
   }



   /////////////////////////////////////////////////////////////////////////
   //
   //    Neoplasm trimming.  If there are many histos, distribute low occurrence histos.
   //
   /////////////////////////////////////////////////////////////////////////

   // Trim by <30% of max mentions
   static private final double histoMentionCutoff = .3;

   static private void trimHistologies( final Map<String,Collection<ConceptAggregate>> histologyNeoplasms ) {
      trimByHisto( histologyNeoplasms );
   }


   static private void trimByHisto( final Map<String,Collection<ConceptAggregate>> histologyNeoplasms ) {
      if ( histologyNeoplasms.size() <= 1 ) {
         return;
      }
      final Map<String,Integer> histoCounts = new HashMap<>();
      int max = 0;
      int total = 0;
      for ( Map.Entry<String,Collection<ConceptAggregate>> histoNeoplasms : histologyNeoplasms.entrySet() ) {
         final String histo = histoNeoplasms.getKey();
         int count = histoNeoplasms.getValue().stream()
                                 .map( ConceptAggregate::getMentions )
                                 .mapToInt( Collection::size )
                                 .sum();
         histoCounts.put( histo, count );
         max = Math.max( max, count );
         total += count;
      }
      if ( histoCounts.size() <= 1 ) {
         return;
      }
      final double cutoff = max * histoMentionCutoff;
      final Collection<String> lowHistos = histoCounts.entrySet().stream()
                                                    .filter( e -> e.getValue() < cutoff )
                                                    .map( Map.Entry::getKey )
                                                    .collect( Collectors.toList() );
      if ( lowHistos.isEmpty() ) {
         return;
      }
      // Add all low histo neoplasm concepts to all higher histo neoplasm concepts.
      // This may end up with neoplasm concepts repeated at different histos,
      // but that should (hopefully) end in better results in the end.
      for ( Map.Entry<String,Collection<ConceptAggregate>> histoNeoplasms : histologyNeoplasms.entrySet() ) {
         if ( lowHistos.contains( histoNeoplasms.getKey() ) ) {
            continue;
         }
         for ( String lowHisto : lowHistos ) {
            histoNeoplasms.getValue().addAll( histologyNeoplasms.get( lowHisto ) );
         }
      }
      histologyNeoplasms.keySet().removeAll( lowHistos );
   }


   /////////////////////////////////////////////////////////////////////////
   //
   //    Neoplasm initial merging.
   //    Try distribution by Histo first:  BrCa<Breast<Right,tumors>>, BrCa<Breast<Left,tumors>
   //
   /////////////////////////////////////////////////////////////////////////


   static private void mergeByHistology( final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
                                         final Map<String,Map<String,Collection<ConceptAggregate>>> sitedNeoplasms ) {
      final Map<ConceptAggregate,String> neoplasmHistologies = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptAggregate>> histoNeoplasms : histologyNeoplasms.entrySet() ) {
         histoNeoplasms.getValue().forEach( n -> neoplasmHistologies.put( n, histoNeoplasms.getKey() ) );
      }
      final Map<String,Map<String,Map<String,Collection<ConceptAggregate>>>> histoSiteNeoplasms = new HashMap<>();
      for ( Map.Entry<String,Map<String,Collection<ConceptAggregate>>> siteNeoplasms : sitedNeoplasms.entrySet() ) {
         final String topo = siteNeoplasms.getKey();
         for ( Map.Entry<String,Collection<ConceptAggregate>> latNeoplasms : siteNeoplasms.getValue().entrySet() ) {
            final String lat = latNeoplasms.getKey();
            for ( ConceptAggregate neoplasm : latNeoplasms.getValue() ) {
               final String histo = neoplasmHistologies.getOrDefault( neoplasm, "801" );
               histoSiteNeoplasms.computeIfAbsent( histo, h -> new HashMap<>() )
                                 .computeIfAbsent( topo, t -> new HashMap<>() )
                                 .computeIfAbsent( lat, l -> new ArrayList<>() )
                                 .add( neoplasm );
            }
         }
      }

      //  Sort by topo major first and do reductions.  <breast<histology,tumors>>.   Reductions include lat.
      //  Then do lat reductions within the major topo.
      //  Then do distribution of topo C80.
      //  Then distribution of lat 0.




      // reduce laterality, within each major topo, not counting laterality 0 (unknown)
      // reduce histology, not counting histology 801 (carcinoma nos)
      // reduce topography major, within each histology, not counting C50 (unknown)

      // swap major topo and histology.  e.g. <breast,lat>,<brca,tumors>
      // reduce histology, within each topography major.

      // distribute C80 - unknown major topography, within each histology.
      // distribute lat 0 - no laterality, within each major topography.
      // distribute 801 - carcinoma NOS, across all histologies




   }


//   static private void trimByHistoSite(
//         final Map<String,Map<String,Map<String,Collection<ConceptAggregate>>>> histoSiteNeoplasms ) {
//      if ( histoSiteNeoplasms.size() <= 1 ) {
//         return;
//      }
//      final Map<String,Integer> histoCounts = new HashMap<>();
//      int max = 0;
//      int total = 0;
//      for ( Map.Entry<String,Collection<ConceptAggregate>> histoNeoplasms : histologyNeoplasms.entrySet() ) {
//         final String histo = histoNeoplasms.getKey();
//         int count = histoNeoplasms.getValue().stream()
//                                   .map( ConceptAggregate::getMentions )
//                                   .mapToInt( Collection::size )
//                                   .sum();
//         histoCounts.put( histo, count );
//         max = Math.max( max, count );
//         total += count;
//      }
//      if ( histoCounts.size() <= 1 ) {
//         return;
//      }
//      final double cutoff = max * histoMentionCutoff;
//      final Collection<String> lowHistos = histoCounts.entrySet().stream()
//                                                      .filter( e -> e.getValue() < cutoff )
//                                                      .map( Map.Entry::getKey )
//                                                      .collect( Collectors.toList() );
//      if ( lowHistos.isEmpty() ) {
//         return;
//      }
//      // Add all low histo neoplasm concepts to all higher histo neoplasm concepts.
//      // This may end up with neoplasm concepts repeated at different histos,
//      // but that should (hopefully) end in better results in the end.
//      for ( Map.Entry<String,Collection<ConceptAggregate>> histoNeoplasms : histologyNeoplasms.entrySet() ) {
//         if ( lowHistos.contains( histoNeoplasms.getKey() ) ) {
//            continue;
//         }
//         for ( String lowHisto : lowHistos ) {
//            histoNeoplasms.getValue().addAll( histologyNeoplasms.get( lowHisto ) );
//         }
//      }
//      histologyNeoplasms.keySet().removeAll( lowHistos );
//   }
//


}
