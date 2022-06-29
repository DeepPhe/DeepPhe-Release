package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;

import static org.healthnlp.deepphe.summary.neoplasm.casino.SiteTable.TOPOGRAPHY_UNDETERMINED;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2022}
 */
public class TrimmedSiteTable {

   private final Map<String, Collection<ConceptAggregate>> _bestSiteNeoplasmsMap;

   TrimmedSiteTable( final String patientId,
                     final SiteTable siteTable,
                     final SitedHistologyTable sitedHistologyTable ) {

      _bestSiteNeoplasmsMap = new HashMap<>( siteTable.getBestNeoplasmsMap() );

      final Map<String,String> siteHistologies = sitedHistologyTable.getSiteHistologies();

      // Get unwanted site neoplasms.
//      final Map<String,Collection<ConceptAggregate>> unwantedSiteNeoplasmsMap = siteTable.getUnwantedNeoplasmsMap();
      final Map<String,Collection<ConceptAggregate>> unwantedSiteNeoplasmsMap
            = new HashMap<>( siteTable.getUnwantedNeoplasmsMap() );
      if ( siteTable.getBestNeoplasmsMap().size() > 1
           && siteTable.getBestNeoplasmsMap().containsKey( SiteTable.TOPOGRAPHY_UNDETERMINED ) ) {
         unwantedSiteNeoplasmsMap.put( SiteTable.TOPOGRAPHY_UNDETERMINED, siteTable.getGenericNeoplasms() );
         _bestSiteNeoplasmsMap.remove( SiteTable.TOPOGRAPHY_UNDETERMINED );
      }
      final Map<String,Collection<ConceptAggregate>> unwantedSiteNeoplasmsByHistology = new HashMap<>();
      // Build map of histology and neoplasms of that histology that are on unwanted sites.
      unwantedSiteNeoplasmsMap.forEach( (k,v) -> unwantedSiteNeoplasmsByHistology.computeIfAbsent(
            siteHistologies.get( k ), s -> new HashSet<>() ).addAll( v ) );

      // Create merged neoplasms for each histology.
      final Map<String,ConceptAggregate> mergedUnwantedSiteNeoplasmsByHistology = new HashMap<>();
      unwantedSiteNeoplasmsByHistology.forEach( (k,v) -> mergedUnwantedSiteNeoplasmsByHistology
            .put( k, NeoplasmDealer.createMergedNeoplasm( patientId, v ) ) );

      // Copy each new unwanted site neoplasm by histology to all best sites with that histology
      final Collection<String> bestSites = new HashSet<>( _bestSiteNeoplasmsMap.keySet() );
      for ( String bestSite : bestSites ) {
         final String histology = siteHistologies.get( bestSite );
         final ConceptAggregate mergedNeoplasm = mergedUnwantedSiteNeoplasmsByHistology.get( histology );
         if ( mergedNeoplasm != null ) {
            _bestSiteNeoplasmsMap.get( bestSite ).add( mergedNeoplasm );
         }
      }
   }


   public Map<String,Collection<ConceptAggregate>> getAllNeoplasmsMap() {
      return _bestSiteNeoplasmsMap;
   }

   public Map<String,Collection<ConceptAggregate>> getBestNeoplasmsMap() {
      return _bestSiteNeoplasmsMap;
   }


   public Collection<ConceptAggregate> getNotBestNeoplasms() {
      return _bestSiteNeoplasmsMap.getOrDefault( TOPOGRAPHY_UNDETERMINED, Collections.emptyList() );
   }



}
