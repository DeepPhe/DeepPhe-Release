package org.healthnlp.deepphe.summary.neoplasm;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.histology.Histology;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyUriInfoVisitor;
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
final public class ByAttributeMergerAbort3 {

   private ByAttributeMergerAbort3() {}

   static private final Logger LOGGER = Logger.getLogger( "ByAttributeMerger" );


   static private final double TOPOGRAPHY_CUTOFF = 0.20;
   static private final Collection<String> TOPOGRAPHY_UNDETERMINED = Arrays.asList( "C80", "C76" );
   static private final double HISTOLOGY_CUTOFF = 0.30;
   static private final double LONE_HISTOLOGY_CUTOFF = 0.50;
   // 8010 is all carcinoma nos. /0 Benign (epithelioma), /2 In Situ and /3 Malignant (DNE)
   // 800* might be unknown.
   // We only want to use the first 3 digits of the histology.  Those are the "Major" Histologic Types.
   static private final Collection<String> CARCINOMA_NOS = Arrays.asList( "800", "801" );
   static private final Collection<String> KEEP_CARCINOMA_NOS = Collections.emptyList();
   static private final double LATERALITY_CUTOFF = 0.30;
   static private final Collection<String> LATERALITY_NONE = Arrays.asList( "0", "9" );


   static private Collection<ConceptAggregate> mergeCancers( final String patientId,
                                                             final Collection<ConceptAggregate> neoplasms,
                                                             final Collection<ConceptAggregate> allConcepts ) {
      // First get best histos for all neoplasms (cancer and tumor), distributing CARCINOMA_NOS
      final Map<String,Collection<ConceptAggregate>> histologyAllNeoplasms
            = collectByHistology( neoplasms, allConcepts, CARCINOMA_NOS );

      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massUris = UriConstants.getMassUris( graphDb );
      final Predicate<ConceptAggregate> isMass = c -> c.getAllUris().stream().anyMatch( massUris::contains );

      final Collection<ConceptAggregate> tumors = neoplasms.stream().filter( isMass ).collect( Collectors.toSet() );
      final Collection<ConceptAggregate> cancers = new HashSet<>( neoplasms );
      // Any Cancer sites that end up not having tumors will have tumors "created" at those sites.
      // So, for any neoplasm that fits into cancer and tumor favor the cancer.
      // This is better for histology and (primary)  topo.
      tumors.removeAll( cancers );

      // Second, get best histos for all Cancers, distributing CARCINOMA_NOS.
      //  -- Compare them?  How to handle?   Trim the histos that are in neoplasms but not cancers?
      //  If so, why not just get histos for cancers at the getgo?
      final Map<String,Collection<ConceptAggregate>> histologyCancersOnly
            = collectByHistology( cancers, allConcepts, CARCINOMA_NOS );

      LOGGER.info( "Neopolasm Histos: " + String.join( ", ", histologyAllNeoplasms.keySet() )
                   + " Cancer Histos: " + String.join( ", ", histologyCancersOnly.keySet() ) );


      // Third, get sites for all cancers, attempt to limit to 3 based upon max mention count.
      final Map<String,Collection<ConceptAggregate>> topographyCancersOnly
            = collectByTopography( cancers, allConcepts, TOPOGRAPHY_UNDETERMINED );


      // Fourth, get histos for cancer sites, get major histo at each site.
      // Trim to one histo per cancer site?  Reaffirm best histos?  Is a #1 best histo missing?
      final Map<String,String>  topographyCancerHistologies
            = collectTopographyHistologies( topographyCancersOnly, histologyCancersOnly, allConcepts );


      // Fifth, get sites for all tumors.
      final Map<String,Collection<ConceptAggregate>> topographyTumorsOnly
            = collectByTopography( tumors, allConcepts, TOPOGRAPHY_UNDETERMINED );


      // Sixth, get histos for all tumors at each site.
      final Map<String,Collection<ConceptAggregate>> histologyTumorsOnly
            = collectByHistology( tumors, allConcepts, CARCINOMA_NOS );
      final Map<String,String>  topographyTumorHistologies
            = collectTopographyHistologies( topographyTumorsOnly, histologyTumorsOnly, allConcepts );

      LOGGER.info( "Neopolasm Histos: " + String.join( ", ", histologyAllNeoplasms.keySet() )
                   + " Cancer Histos: " + String.join( ", ", histologyCancersOnly.keySet() )
                   + " Tumor Histos: " + String.join( ", ", histologyTumorsOnly.keySet() ) );



      // Seventh, Compare Tumor sites to Cancer Sites, Tumor Histos on those sites to Cancer histos on those sites?
      // Eighth, Distribute Cancer properties to all tumors with the same histos (except site).
      // Ninth, Distribute all Tumor properties to Cancers with same Histos (except site).



      return null;
   }


   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting by histology.
   //
   /////////////////////////////////////////////////////////////////////////


   // First get best histos for all neoplasms (cancer and tumor)
   static private Map<String,Collection<ConceptAggregate>> collectByHistology(
         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts,
         final Collection<String> distributableCodes ) {
      final Map<String, Collection<ConceptAggregate>> histologyNeoplasms = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final String bestCode = getHistologyCode( neoplasm, neoplasms, allConcepts );
         final String code = bestCode.substring( 0, 3 );
         histologyNeoplasms.computeIfAbsent( code, c -> new ArrayList<>() ).add( neoplasm );
      }
      distributeLowHistoAllSites( histologyNeoplasms, distributableCodes );
      return histologyNeoplasms;
   }

   // First get best histos for all neoplasms (cancer and tumor)
   static private Map<String,Collection<ConceptAggregate>> collectBySiteHistology(
         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts,
         final Collection<String> distributableCodes ) {
      final Map<String, Collection<ConceptAggregate>> histologyNeoplasms = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final String bestCode = getHistologyCode( neoplasm, neoplasms, allConcepts );
         final String code = bestCode.substring( 0, 3 );
         histologyNeoplasms.computeIfAbsent( code, c -> new ArrayList<>() ).add( neoplasm );
      }
      distributeLowHistoOneSite( histologyNeoplasms, distributableCodes );
      return histologyNeoplasms;
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


   static private void distributeLowHistoAllSites( final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
                                                   final Collection<String> distributableCodes ) {
      if ( histologyNeoplasms.size() <= 1 ) {
         return;
      }
      distributeByLowCount( histologyNeoplasms, HISTOLOGY_CUTOFF, 3, distributableCodes );
   }

   static private void distributeLowHistoOneSite( final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
                                                  final Collection<String> distributableCodes ) {
      if ( histologyNeoplasms.size() <= 1 ) {
         return;
      }
      distributeByLowCount( histologyNeoplasms, LONE_HISTOLOGY_CUTOFF, 1, distributableCodes );
   }






   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting by topography.
   //
   /////////////////////////////////////////////////////////////////////////

   /**
    * ICDO code is a higher level than unique uri.  e.g. br, left br, nipple, etc.  This should allow better merges.
    * @param neoplasms -
    * @param allConcepts -
    * @return map of major topo codes to list of concepts with those topo codes.
    */
   static private Map<String, Collection<ConceptAggregate>> collectByTopography(
         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts,
         final Collection<String> distributableCodes ) {
      final Map<String, Collection<ConceptAggregate>> topographyNeoplasms = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final Topography topography = new Topography( neoplasm, allConcepts );
         final String bestCode = topography.getBestMajorTopoCode();
         topographyNeoplasms.computeIfAbsent( bestCode, c -> new ArrayList<>() ).add( neoplasm );
      }
      distributeLowTopos( topographyNeoplasms, distributableCodes );
      return topographyNeoplasms;
   }


   static private void distributeLowTopos( final Map<String,Collection<ConceptAggregate>> topographyNeoplasms,
                                           final Collection<String> distributableCodes  ) {
      if ( topographyNeoplasms.size() <= 1 ) {
         return;
      }
      distributeByLowCount( topographyNeoplasms, TOPOGRAPHY_CUTOFF, 3, distributableCodes );
   }


   /**
    * Note that these are histologies created per site using temporary merged neoplasm concepts.
    * Neoplasms are not actually merged at this stage.
    * @param topographyNeoplasms -
    * @param histologyNeoplasms -
    * @param allConcepts -
    * @return -
    */
   static private Map<String,String> collectTopographyHistologies(
         final Map<String,Collection<ConceptAggregate>> topographyNeoplasms,
         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
         final Collection<ConceptAggregate> allConcepts ) {
      final Map<ConceptAggregate,String> neoplasmHistologyMap = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptAggregate>> histologyNeoplasm : histologyNeoplasms.entrySet() ) {
         final String histology = histologyNeoplasm.getKey();
         histologyNeoplasm.getValue().forEach( n -> neoplasmHistologyMap.put( n, histology ) );
      }

      final Map<String,String> topographyHistologies = new HashMap<>( topographyNeoplasms.size() );

      for ( Map.Entry<String,Collection<ConceptAggregate>> topographyNeoplasm : topographyNeoplasms.entrySet() ) {
         topographyHistologies.put( topographyNeoplasm.getKey(),
                                    getSingleSiteHistology( topographyNeoplasm.getValue(),
                                                            neoplasmHistologyMap,
                                                            allConcepts ) );
      }
      return topographyHistologies;
   }


   static private String getSingleSiteHistology(
         final Collection<ConceptAggregate> siteNeoplasms,
         final Map<ConceptAggregate,String> neoplasmHistologyMap,
         final Collection<ConceptAggregate> allConcepts ) {
      final Map<ConceptAggregate,String> siteHistologies = new HashMap<>( neoplasmHistologyMap );
      siteHistologies.keySet().retainAll( siteNeoplasms );
      final Collection<String> histologies = new HashSet<>( siteHistologies.values() );
      if ( histologies.size() == 1 ) {
         return new ArrayList<>( histologies ).get( 0 );
      }
      final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms
            = collectBySiteHistology( siteNeoplasms, allConcepts, CARCINOMA_NOS );
      if ( siteHistologyNeoplasms.size() == 1 ) {
         return new ArrayList<>( siteHistologyNeoplasms.keySet() ).get( 0 );
      }
      histologies.retainAll( siteHistologyNeoplasms.keySet() );
      if ( histologies.size() == 1 ) {
         return new ArrayList<>( histologies ).get( 0 );
      }
      siteHistologyNeoplasms.keySet().retainAll( histologies );
      final Map<Integer,List<String>> countHistologies = new HashMap<>();
      int max = 0;
      for ( Map.Entry<String,Collection<ConceptAggregate>> siteHistologyNeoplasm : siteHistologyNeoplasms.entrySet() ) {
         countHistologies.computeIfAbsent( siteHistologyNeoplasm.getValue().size(), c -> new ArrayList<>() )
                         .add( siteHistologyNeoplasm.getKey() );
         max = Math.max( max, siteHistologyNeoplasm.getValue().size() );
      }
      final List<String> maxHistology = countHistologies.get( max );
      if ( maxHistology.size() == 1 ) {
         return maxHistology.get( 0 );
      }

      LOGGER.warn( "Could Not find a best histology " + siteNeoplasms.stream()
                                                                     .map( ConceptAggregate::toShortText )
                                                                     .collect( Collectors.joining( "\n" ) ) );
      return maxHistology.get( 0 );
   }




   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting Utilities.
   //
   /////////////////////////////////////////////////////////////////////////



   static private void distributeByLowCount( final Map<String,Collection<ConceptAggregate>> codeNeoplasms,
                                             final double cutoff_constant,
                                             final int maxDesired,
                                             final Collection<String> distributableCodes ) {
      if ( codeNeoplasms.size() <= 1 ) {
         return;
      }
      final Map<String,Integer> codeMentionCounts = new HashMap<>();
      int max = 0;
      int total = 0;
      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : codeNeoplasms.entrySet() ) {
         final String code = entry.getKey();
         int count = entry.getValue().stream()
                          .map( ConceptAggregate::getMentions )
                          .mapToInt( Collection::size )
                          .sum();
         codeMentionCounts.put( code, count );
         max = Math.max( max, count );
         total += count;
      }
      if ( codeMentionCounts.size() <= 1 ) {
         return;
      }
      double cutoff = max * cutoff_constant;
      distributeByLowCount( codeNeoplasms, codeMentionCounts, cutoff, distributableCodes );
      if ( codeNeoplasms.size() > maxDesired ) {
         // Try a higher filter.
         cutoff = total * cutoff_constant;
         distributeByLowCount( codeNeoplasms, codeMentionCounts, cutoff, distributableCodes );
      }
   }


   static private void distributeByLowCount( final Map<String,Collection<ConceptAggregate>> codeNeoplasms,
                                             final Map<String,Integer> codeMentionCounts,
                                             final double cutoff,
                                             final Collection<String> distributableCodes ) {
      final Collection<String> lowCountCodes = codeMentionCounts.entrySet().stream()
                                                                .filter( e -> e.getValue() < cutoff )
                                                                .map( Map.Entry::getKey )
                                                                .collect( Collectors.toSet() );
      codeNeoplasms.keySet().stream()
                   .filter( distributableCodes::contains )
                   .forEach( lowCountCodes::add );
      if ( lowCountCodes.isEmpty() ) {
         return;
      }
      // Add all low count neoplasm concepts to all higher count neoplasm concepts.
      // This may end up with neoplasm concepts repeated at different topos, histos, etc.,
      // but that should still (hopefully) end in better results in the end.
      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : codeNeoplasms.entrySet() ) {
         if ( lowCountCodes.contains( entry.getKey() ) ) {
            continue;
         }
         for ( String lowTopo : lowCountCodes ) {
            entry.getValue().addAll( codeNeoplasms.get( lowTopo ) );
         }
      }
      codeNeoplasms.keySet().removeAll( lowCountCodes );
   }











//   static private final class HistologyNeoplasms {
//      private final String _histology;
//      private final Collection<ConceptAggregate> _neoplasms;
//      private HistologyNeoplasms( final String histology, final Collection<ConceptAggregate> neoplasms ) {
//         _histology = histology;
//         _neoplasms = neoplasms;
//      }
//   }







//   /**
//    * Merge given neoplasms into a single neoplasm.
//    * !!! Note !!!   This undoes previous concept separations.
//    *
//    * @param patientId    -
//    * @param neoplasms -
//    * @param allConcepts -
//    * @return -
//    */
//   static public ConceptAggregate createMergedNeoplasm( final String patientId,
//                                                         final Collection<ConceptAggregate> neoplasms,
//                                                         final Collection<ConceptAggregate> allConcepts ) {
//      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
//      neoplasms.stream()
//                     .map( ConceptAggregate::getUriRootsMap )
//                     .map( Map::entrySet )
//                     .flatMap( Collection::stream )
//                     .forEach( e -> allUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() ).addAll( e.getValue() ) );
//      final Map<String, Collection<Mention>> docMentions = ConceptAggregateHandler.collectDocMentions( neoplasms );
//      final ConceptAggregate newNeoplasm = new DefaultConceptAggregate( patientId, allUriRoots, docMentions );
//
//      final Map<String, Collection<ConceptAggregate>> neoplasmRelations = new HashMap<>();
//
//      for ( ConceptAggregate concept : allConcepts ) {
//         final boolean isNeoplasm = neoplasms.contains( concept );
//         final Map<String, Collection<ConceptAggregate>> oldRelations = concept.getRelatedConceptMap();
//         final Map<String, Collection<ConceptAggregate>> newRelations = new HashMap<>( oldRelations.size() );
//         for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
//            if ( isNeoplasm ) {
//               for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
//                  final Collection<ConceptAggregate> related
//                        = neoplasmRelations.computeIfAbsent( oldRelation.getKey(), r -> new HashSet<>() );
//                  if ( neoplasms.contains( oldConcept ) ) {
//                     related.add( newNeoplasm );
//                  } else {
//                     related.add( oldConcept );
//                  }
//               }
//               continue;
//            }
//            final Collection<ConceptAggregate> newRelated = new HashSet<>();
//            for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
//               if ( neoplasms.contains( oldConcept ) ) {
//                  newRelated.add( newNeoplasm );
//               } else {
//                  newRelated.add( oldConcept );
//               }
//            }
//            newRelations.put( oldRelation.getKey(), newRelated );
//         }
//         concept.setRelated( newRelations );
//      }
//      newNeoplasm.setRelated( neoplasmRelations );
//      allConcepts.removeAll( neoplasms );
//      allConcepts.add( newNeoplasm );
//
////      LOGGER.info( "mergeNeoplasms : New NEOPLASM\n" + newNeoplasm );
//
//      return newNeoplasm;
//   }
//
//
//
//   /**
//    * Merge given neoplasms into a single neoplasm.
//    * !!! Note !!!   This undoes previous concept separations.
//    *
//    * @param patientId    -
//    * @param tempNeoplasms -
//    * @param tempAllConcepts -
//    * @return -
//    */
//   static public ConceptAggregate createTempNeoplasm( final String patientId,
//                                                        final Collection<ConceptAggregate> tempNeoplasms,
//                                                        final Collection<ConceptAggregate> tempAllConcepts ) {
//      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
//      tempNeoplasms.stream()
//               .map( ConceptAggregate::getUriRootsMap )
//               .map( Map::entrySet )
//               .flatMap( Collection::stream )
//               .forEach( e -> allUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() ).addAll( e.getValue() ) );
//      final Map<String, Collection<Mention>> docMentions = ConceptAggregateHandler.collectDocMentions( tempNeoplasms );
//      final ConceptAggregate newNeoplasm = new DefaultConceptAggregate( patientId, allUriRoots, docMentions );
//
//      final Map<String, Collection<ConceptAggregate>> neoplasmRelations = new HashMap<>();
//
//      for ( ConceptAggregate concept : tempAllConcepts ) {
//         final boolean isNeoplasm = tempNeoplasms.contains( concept );
//         final Map<String, Collection<ConceptAggregate>> oldRelations = concept.getRelatedConceptMap();
//         final Map<String, Collection<ConceptAggregate>> newRelations = new HashMap<>( oldRelations.size() );
//         for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
//            if ( isNeoplasm ) {
//               for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
//                  final Collection<ConceptAggregate> related
//                        = neoplasmRelations.computeIfAbsent( oldRelation.getKey(), r -> new HashSet<>() );
//                  if ( tempNeoplasms.contains( oldConcept ) ) {
//                     related.add( newNeoplasm );
//                  } else {
//                     related.add( oldConcept );
//                  }
//               }
//               continue;
//            }
//            final Collection<ConceptAggregate> newRelated = new HashSet<>();
//            for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
//               if ( tempNeoplasms.contains( oldConcept ) ) {
//                  newRelated.add( newNeoplasm );
//               } else {
//                  newRelated.add( oldConcept );
//               }
//            }
//            newRelations.put( oldRelation.getKey(), newRelated );
//         }
//         concept.setRelated( newRelations );
//      }
//      newNeoplasm.setRelated( neoplasmRelations );
//      tempAllConcepts.removeAll( tempNeoplasms );
//      tempAllConcepts.add( newNeoplasm );
//
//      LOGGER.info( "createTempNeoplasm : New TEMP NEOPLASM\n" + newNeoplasm );
//
//      return newNeoplasm;
//   }






}
