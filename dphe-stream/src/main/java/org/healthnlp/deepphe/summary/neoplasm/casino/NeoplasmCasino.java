package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static org.healthnlp.deepphe.summary.neoplasm.casino.SitedHistologyTable.collectSiteHistologies;


/**
 * @author SPF , chip-nlp
 * @since {6/22/2022}
 */
final public class NeoplasmCasino {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmCasino" );


   private NeoplasmCasino() {}

   static public Map<ConceptAggregate,Collection<ConceptAggregate>> splitCancerTumors(
         final String patientId,
         final Collection<ConceptAggregate> neoplasms,
         final Collection<ConceptAggregate> allConcepts ) {
      LOGGER.info( "Refining Neoplasms by site and histology ... " + neoplasms.size() );
      KeyValue<SiteTable,HistologyTable> tables
            = refineNeoplasms( neoplasms, allConcepts, Collections.emptyList(), Collections.emptyList() );
      final SiteTable siteTable = tables.getKey();
      final HistologyTable histologyTable = tables.getValue();
      // Need to distribute relations from these neoplasms to neoplasms at best sites ...
      final Collection<ConceptAggregate> unwantedNeoplasms = getUnwantedNeoplasms( siteTable, histologyTable );
      NeoplasmDealer.relateUndeterminedSiteNos( patientId, unwantedNeoplasms, siteTable.getBestNeoplasmsMap(),
                                                allConcepts );

      // Create a new HistologyTable that only has the best histologies.
      // They are all distributed according to sites, so that each site shares a single histology.
      // i.e. the neoplasms on each site all share a single histology
      final SitedHistologyTable sitedHistologyTable
            = new SitedHistologyTable( siteTable, histologyTable, allConcepts );
      final TrimmedSiteTable trimmedSiteTable
            = new TrimmedSiteTable( patientId, siteTable, sitedHistologyTable );

      final Map<String,Collection<ConceptAggregate>> siteNeoplasmsMap = trimmedSiteTable.getBestNeoplasmsMap();
      final Map<String,ConceptAggregate> mergedSiteNeoplasmsMap
            = NeoplasmDealer.mergeSiteNeoplasms( patientId, siteNeoplasmsMap, allConcepts );
      final Map<String,String> allSiteBestHistologies
            = collectSiteHistologies( trimmedSiteTable, sitedHistologyTable, allConcepts );

      return DpheXnCancerTumorSplitter.splitCancerTumors( mergedSiteNeoplasmsMap, allSiteBestHistologies );
   }




//      // Get top histology per site.
//      final Map<String,String> allSiteBestHistologies
//            = SiteHistologyPit.collectSiteHistologies( _siteTable, _histologyTable, allConcepts );

      //  TODO Need to "get rid of" neoplasms on unwanted sites.
      //  If they have one of the "best" histologies,
      //  create a new merged neoplasm and copy it to all of the "best" sites that have that histology.
      //  Relations for same histologies have already been copied.
      // That way the Mentions will be preserved.
      // If they have a crap histology OR carcinoma_NOS,
      // put them in a merged "nos" neoplasm and copy it into a "best" site at "undetermined".
      // Copy non-site relations into all neoplasm concepts?
      // Hopefully undetermined never becomes something ridiculous.




   /**
    * Keeps grouping neoplasms and distributing related attributes until they can't reasonably be distributed.
    * First calculates best major sites and distributes attributes among all neoplasms with the same best site.
    * Second calculates the best histologies and distributes attributes among all neoplasms with the same best
    * histology.
    * @param neoplasms -
    * @param allConcepts -
    * @param previousUnwantedSites -
    * @param previousUnwantedHistologies -
    */
   static private KeyValue<SiteTable,HistologyTable> refineNeoplasms( final Collection<ConceptAggregate> neoplasms,
                                     final Collection<ConceptAggregate> allConcepts,
                                     final Collection<String> previousUnwantedSites,
                                     final Collection<String> previousUnwantedHistologies ) {
      final SiteTable siteTable = new SiteTable( neoplasms, allConcepts );
      final HistologyTable histologyTable = new HistologyTable( neoplasms, allConcepts );
      if ( siteTable.areAllWanted() && histologyTable.areAllWanted() ) {
//         LOGGER.info( "Done Refining Neoplasms by site and histology, all distributed.  "
//                      + SiteTable.TOPOGRAPHY_CUTOFF + " " + HistologyTable.ONE_SITE_HISTOLOGY_CUTOFF );
         return new KeyValue<>( siteTable, histologyTable );
      } else if ( previousUnwantedSites.equals( siteTable.getUnwantedCodes() )
           && previousUnwantedHistologies.equals( histologyTable.getUnwantedCodes() ) ) {
//         LOGGER.info( "Done Refining Neoplasms by site and histology, no longer distributing." );
         return new KeyValue<>( siteTable, histologyTable );
      }
//      LOGGER.info( "Continue Refining Neoplasms by site and histology ... "
//                   + siteTable.getBestNeoplasmsMap().size() + " "
//                   + siteTable.getUnwantedCodes().size() + " "
//                   + histologyTable.getBestNeoplasmsMap().size() + " "
//                   + histologyTable.getUnwantedCodes().size() );
      return refineNeoplasms( neoplasms, allConcepts,
                       siteTable.getUnwantedCodes(),
                       histologyTable.getUnwantedCodes() );
   }



   static private Collection<ConceptAggregate> getUnwantedNeoplasms( final SiteTable siteTable,
                                                                     final HistologyTable histologyTable ) {
      final Collection<ConceptAggregate> notBestSiteNeoplasms = siteTable.getNotBestNeoplasms();
      final Collection<ConceptAggregate> notBestHistologyNeoplasms = histologyTable.getNotBestNeoplasms();
      final Collection<ConceptAggregate> unwantedNeoplasms = new HashSet<>( notBestSiteNeoplasms );
      unwantedNeoplasms.retainAll( notBestHistologyNeoplasms );
      return unwantedNeoplasms;
   }





//
//   // If we are lucky there will be only 1 histology at the site matching one of the best histologies of the patient.
//   static private String getLoneSiteHistology( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//                                               final Collection<String> bestHistologies ) {
//      final List<String> onlyBestHistologies = new ArrayList<>( siteHistologyNeoplasms.keySet() );
//      onlyBestHistologies.retainAll( bestHistologies );
//      if ( onlyBestHistologies.size() == 1 ) {
//         return onlyBestHistologies.get( 0 );
//      }
//      onlyBestHistologies.removeAll( CARCINOMA_UNKNOWNS );
//      if ( onlyBestHistologies.size() == 1 ) {
//         return onlyBestHistologies.get( 0 );
//      }
//      return "";
//   }
//
//   static private String getBestSiteHistologyLoneOrCalc( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//                                                         final Collection<String> bestHistologies  ) {
//      // If we are lucky there will be only 1 histology at the site matching one of the best histologies of the patient.
//      String loneHistology = getLoneSiteHistology( siteHistologyNeoplasms, bestHistologies );
//      if ( !loneHistology.isEmpty() ) {
//         return loneHistology;
//      }
//      // Calculate the best histologies using all the existing histologies of neoplasms at the site
//      loneHistology = getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies );
//      if ( !loneHistology.isEmpty() ) {
//         return loneHistology;
//      }
//      return "";
//   }
//
//   // Calculate the best histologies using all the existing histologies of neoplasms at the site
//   static private String getBestSiteHistologyByCalc(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies ) {
//      final String bestSiteHistology
//            = getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies, LOW_HISTOLOGY_CUTOFF );
//      if ( !bestSiteHistology.isEmpty() ) {
//         return bestSiteHistology;
//      }
//      return getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies, HIGH_HISTOLOGY_CUTOFF );
//   }
//
//   // Calculate the best histologies using all the existing histologies of neoplasms at the site
//   static private String getBestSiteHistologyByCalc(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies, final double cutoff ) {
//      final Map<String,Collection<ConceptAggregate>> siteBestHistologyNeoplasms
//            = collectBestByHistology( siteHistologyNeoplasms, cutoff, CARCINOMA_UNKNOWNS );
//      return getLoneSiteHistology( siteBestHistologyNeoplasms, bestHistologies );
//   }
//
//   // Recalculate histologies using all the neoplasms at the site.
//   // THIS IS A PLACEHOLDER.
//   // The idea is that the neoplasms at the site are used as the "patientNeoplasms" in Attribute (Histology)
//   // Value Calculation.  At this time that is not done, so just return empty.
//   static private String getBestSiteHistologyByRecalc(
//         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
//         final Collection<String> bestHistologies,
//         final Collection<ConceptAggregate> allConcepts ) {
//      return "";
//      // Recalculate histologies using all the neoplasms at the site
////      final Collection<ConceptAggregate> siteNeoplasms = siteHistologyNeoplasms.values().stream()
////                                                                               .flatMap( Collection::stream )
////                                                                               .collect( Collectors.toSet() );
////      final Map<String,Collection<ConceptAggregate>> newSiteHistologyNeoplasms
////            = collectAllByHistology( siteNeoplasms, allConcepts );
////      if ( siteHistologyNeoplasms.keySet().equals( newSiteHistologyNeoplasms.keySet() ) ) {
////         // Don't bother to recalculate based upon "best" as that has already been done.
////         return "";
////      }
////      return getBestSiteHistologyLoneOrCalc( newSiteHistologyNeoplasms, bestHistologies );
//   }
//
//
//
//
//   static private Map<String,Collection<ConceptAggregate>> collectBestByHistology(
//         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
//         final Collection<String> keepCodes ) {
//      return collectBestByHistology( histologyNeoplasms, LOW_HISTOLOGY_CUTOFF, keepCodes );
//   }
//
//
//   static private Map<String,Collection<ConceptAggregate>> collectBestByHistology(
//         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
//         final double cutoff,
//         final Collection<String> keepCodes ) {
//      if ( histologyNeoplasms.size() <= 1 ) {
//         return new HashMap<>( histologyNeoplasms );
//      }
//      return getNeoplasmsAboveCutoff( histologyNeoplasms, cutoff, keepCodes );
//   }


}
