package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.neoplasm.casino.HistologyTable.*;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2022}
 */
class SitedHistologyTable extends AttributeTable {

   // Get top histology per site.  This attempts to use only the best histologies.
   // If a best histology is not possible then it uses Carcinoma NOS.
   private final Map<String,String> _allSiteBestHistologies;

   // Get the best histologies for all neoplasms (cancer and tumor), keeping CARCINOMA_NOS
   private final Map<String, Collection<ConceptAggregate>> _bestHistologyNeoplasmsMap;

   // Create a new Histology Table that only has the best histologies.
   // They are all distributed according to sites, so that each site shares a single histology.
   // i.e. the neoplasms on each site all share a single histology.
   SitedHistologyTable( final SiteTable siteTable,
                        final HistologyTable histologyTable,
                        final Collection<ConceptAggregate> allConcepts ) {
      // Get top histology per site.  This attempts to use only the best histologies.
      // If a best histology is not possible then it uses Carcinoma NOS.
      _allSiteBestHistologies = collectSiteHistologies( siteTable, histologyTable, allConcepts );

      _bestHistologyNeoplasmsMap = new HashMap<>();
      siteTable.getAllNeoplasmsMap().forEach( (k,v) -> _bestHistologyNeoplasmsMap.computeIfAbsent(
            _allSiteBestHistologies.get( k ), h -> new HashSet<>() ).addAll( v ) );

      // Distribute non-site relations for neoplasms with the same histology.  Don't worry about repeats.
      _bestHistologyNeoplasmsMap.entrySet()
                                .stream()
                                .filter( e -> !CARCINOMA_UNKNOWNS.contains( e.getKey() ) )
                                .map( Map.Entry::getValue )
                                .forEach( HistologyTable::relateForSameHistologies );
   }

   public Map<String,String> getSiteHistologies() {
      return _allSiteBestHistologies;
   }


   public Map<String,Collection<ConceptAggregate>> getAllNeoplasmsMap() {
      return _bestHistologyNeoplasmsMap;
   }
   public Map<String,Collection<ConceptAggregate>> getBestNeoplasmsMap() {
      return _bestHistologyNeoplasmsMap;
   }
   public Collection<String> getUnwantedCodes() {
      return Collections.emptyList();
   }

   public Collection<String> getGenericCodes() {
      return CARCINOMA_UNKNOWNS;
   }

   public String getCode( final ConceptAggregate neoplasm,
                          final Collection<ConceptAggregate> neoplasms,
                          final Collection<ConceptAggregate> allConcepts ) {
      final String bestCode = HistologyTable.getHistologyCode( neoplasm, neoplasms, allConcepts );
      return bestCode.substring( 0, 3 );
   }

   public Map<String,Collection<ConceptAggregate>> collectBest() {
      return null;
   }

   public void addToBest( final String code, final ConceptAggregate neoplasm ) {
      _bestHistologyNeoplasmsMap.computeIfAbsent( code, n -> new HashSet<>() ).add( neoplasm );
   }

   public Collection<ConceptAggregate> getNotBestNeoplasms() {
      return CARCINOMA_UNKNOWNS.stream()
                               .map( _bestHistologyNeoplasmsMap::get )
                               .filter( Objects::nonNull )
                               .flatMap( Collection::stream )
                               .collect( Collectors.toList() );
   }






   static Map<String,String> collectSiteHistologies( final SiteTable siteTable,
                                                     final HistologyTable histologyTable,
                                                     final Collection<ConceptAggregate> allConcepts ) {
      final Map<String,String> allSiteBestHistologies = new HashMap<>( siteTable.getAllNeoplasmsMap().size() );
      final Map<ConceptAggregate,String> neoplasmHistologies
            = AttributeTable.createNeoplasmCodeMap( histologyTable.getAllNeoplasmsMap() );
      final Collection<String> bestHistologies = histologyTable.getBestNeoplasmsMap().keySet();
      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : siteTable.getAllNeoplasmsMap().entrySet() ) {
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms = new HashMap<>();
         for ( ConceptAggregate neoplasm : siteNeoplasms.getValue() ) {
            siteHistologyNeoplasms.computeIfAbsent( neoplasmHistologies.get( neoplasm ), h -> new ArrayList<>() )
                                  .add( neoplasm );
         }
         final String bestSiteHistology = getBestSiteHistology( siteHistologyNeoplasms,
                                                                bestHistologies,
                                                                allConcepts );
         allSiteBestHistologies.put( siteNeoplasms.getKey(), bestSiteHistology );
      }
      return allSiteBestHistologies;
   }

   static Map<String,String> collectSiteHistologies( final TrimmedSiteTable siteTable,
                                                     final SitedHistologyTable histologyTable,
                                                     final Collection<ConceptAggregate> allConcepts ) {
      final Map<String,String> allSiteBestHistologies = new HashMap<>( siteTable.getAllNeoplasmsMap().size() );
      final Map<ConceptAggregate,String> neoplasmHistologies
            = AttributeTable.createNeoplasmCodeMap( histologyTable.getAllNeoplasmsMap() );
      final Collection<String> bestHistologies = histologyTable.getBestNeoplasmsMap().keySet();
      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : siteTable.getAllNeoplasmsMap().entrySet() ) {
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms = new HashMap<>();
         for ( ConceptAggregate neoplasm : siteNeoplasms.getValue() ) {
            siteHistologyNeoplasms.computeIfAbsent( neoplasmHistologies.get( neoplasm ), h -> new ArrayList<>() )
                                  .add( neoplasm );
         }
         final String bestSiteHistology = getBestSiteHistology( siteHistologyNeoplasms,
                                                                bestHistologies,
                                                                allConcepts );
         allSiteBestHistologies.put( siteNeoplasms.getKey(), bestSiteHistology );
      }
      return allSiteBestHistologies;
   }


   /**
    * @param siteHistologyNeoplasms Neoplasms at the site major site.
    * @param bestHistologies Best histologies for the patient.
    * @param allConcepts all patient concepts.
    * @return best histology at a site major that matches one of the best histologies
    * as determined for the patient.
    */
   static private String getBestSiteHistology( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
                                               final Collection<String> bestHistologies,
                                               final Collection<ConceptAggregate> allConcepts ) {
      // If there are none of the patient's best histologies at this site, return CARCINOMA_NOS.
      final List<String> onlyBestHistologies = new ArrayList<>( siteHistologyNeoplasms.keySet() );
      onlyBestHistologies.retainAll( bestHistologies );
      if ( onlyBestHistologies.isEmpty() ) {
         return CARCINOMA_NOS;
      }
      // If there is a lone histology at the site, use that.
      // If there is a histology that can be determined by (re)calculating the "best" histology
      // at the site using the currently determined histologies, use that.
      String bestHistology = getBestSiteHistologyLoneOrCalc( siteHistologyNeoplasms, bestHistologies );
      if ( !bestHistology.isEmpty() ) {
         return bestHistology;
      }
      // if there is a histology that can be determined by lone or (re)calculating the "best" histology
      // at the site using recalculated histologies for the site, use that.
      // Right now this always returns empty.
      bestHistology = getBestSiteHistologyByRecalc( siteHistologyNeoplasms, bestHistologies, allConcepts );
      if ( !bestHistology.isEmpty() ) {
         return bestHistology;
      }
      // Get the histology that has either the max mention count or the max mention and max relation count.
      bestHistology = getBestSiteHistologyByCount( siteHistologyNeoplasms, bestHistologies );
      if ( !bestHistology.isEmpty() ) {
         return bestHistology;
      }
      // If we get to this point we have low representations, e.g.:
      //    BrCa has 1 Concept with 1 mention, 1 relation.
      //    OvCa has 1 Concept with 1 mention, 1 relation.
      // We can't really favor one over another.  Return NOS to distribute attributes across all merged neoplasms.
      return CARCINOMA_NOS;
   }



   // If we are lucky there will be only 1 histology at the site matching one of the best histologies of the patient.
   static private String getLoneSiteHistology( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
                                               final Collection<String> bestHistologies ) {
      final List<String> onlyBestHistologies = new ArrayList<>( siteHistologyNeoplasms.keySet() );
      onlyBestHistologies.retainAll( bestHistologies );
      if ( onlyBestHistologies.size() == 1 ) {
         return onlyBestHistologies.get( 0 );
      }
      onlyBestHistologies.removeAll( CARCINOMA_UNKNOWNS );
      if ( onlyBestHistologies.size() == 1 ) {
         return onlyBestHistologies.get( 0 );
      }
      return "";
   }


   static private String getBestSiteHistologyLoneOrCalc( final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
                                                         final Collection<String> bestHistologies  ) {
      // If we are lucky there will be only 1 histology at the site matching one of the best histologies of the patient.
      String loneHistology = getLoneSiteHistology( siteHistologyNeoplasms, bestHistologies );
      if ( !loneHistology.isEmpty() ) {
         return loneHistology;
      }
      // Calculate the best histologies using all the existing histologies of neoplasms at the site
      loneHistology = getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies );
      if ( !loneHistology.isEmpty() ) {
         return loneHistology;
      }
      return "";
   }


   // Calculate the best histologies using all the existing histologies of neoplasms at the site
   static private String getBestSiteHistologyByCalc(
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
         final Collection<String> bestHistologies ) {
      final String bestSiteHistology
            = getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies, MULTI_SITE_HISTOLOGY_CUTOFF );
      if ( !bestSiteHistology.isEmpty() ) {
         return bestSiteHistology;
      }
      return getBestSiteHistologyByCalc( siteHistologyNeoplasms, bestHistologies, ONE_SITE_HISTOLOGY_CUTOFF );
   }

   // Calculate the best histologies using all the existing histologies of neoplasms at the site
   static private String getBestSiteHistologyByCalc(
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
         final Collection<String> bestHistologies, final double cutoff ) {
      final Map<String,Collection<ConceptAggregate>> siteBestHistologyNeoplasms
            = HistologyTable.collectBestByHistology( siteHistologyNeoplasms, cutoff, CARCINOMA_UNKNOWNS );
      return getLoneSiteHistology( siteBestHistologyNeoplasms, bestHistologies );
   }

   // Recalculate histologies using all the neoplasms at the site.
   // THIS IS A PLACEHOLDER.
   // The idea is that the neoplasms at the site are used as the "patientNeoplasms" in Attribute (Histology)
   // Value Calculation.  At this time that is not done, so just return empty.
   static private String getBestSiteHistologyByRecalc(
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
         final Collection<String> bestHistologies,
         final Collection<ConceptAggregate> allConcepts ) {
      return "";
      // Recalculate histologies using all the neoplasms at the site
//      final Collection<ConceptAggregate> siteNeoplasms = siteHistologyNeoplasms.values().stream()
//                                                                               .flatMap( Collection::stream )
//                                                                               .collect( Collectors.toSet() );
//      final Map<String,Collection<ConceptAggregate>> newSiteHistologyNeoplasms
//            = collectAllByHistology( siteNeoplasms, allConcepts );
//      if ( siteHistologyNeoplasms.keySet().equals( newSiteHistologyNeoplasms.keySet() ) ) {
//         // Don't bother to recalculate based upon "best" as that has already been done.
//         return "";
//      }
//      return getBestSiteHistologyLoneOrCalc( newSiteHistologyNeoplasms, bestHistologies );
   }

   static private Map<String,Collection<ConceptAggregate>> getBestSiteHistologyByMention(
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
         final Collection<String> bestHistologies) {
      final Map<String,Collection<ConceptAggregate>> maxMentionNeoplasms = new HashMap<>();
      int max = 0;
      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : siteHistologyNeoplasms.entrySet() ) {
         final String code = entry.getKey();
         if ( !bestHistologies.contains( code ) || code.equals( CARCINOMA_NOS ) ) {
            continue;
         }
         int count = entry.getValue().stream()
                          .map( ConceptAggregate::getMentions )
                          .mapToInt( Collection::size )
                          .sum();
         if ( count > max ) {
            max = count;
            maxMentionNeoplasms.clear();
         }
         if ( count >= max ) {
            maxMentionNeoplasms.computeIfAbsent( code, n -> new HashSet<>() ).addAll( entry.getValue() );
         }
      }
      return maxMentionNeoplasms;
   }

   static private String getBestSiteHistologyByCount(
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
         final Collection<String> bestHistologies ) {
      final ToIntFunction<ConceptAggregate> countMentions = c -> c.getMentions().size();
      final Map<String,Collection<ConceptAggregate>> maxMentionNeoplasms
            = getBestSiteHistologyByCount( siteHistologyNeoplasms, bestHistologies, countMentions );
      if ( maxMentionNeoplasms.size() == 1 ) {
         return new ArrayList<>( maxMentionNeoplasms.keySet() ).get( 0 );
      }
      final ToIntFunction<ConceptAggregate> countRelations = c -> c.getRelatedConceptMap()
                                                                   .values()
                                                                   .stream()
                                                                   .mapToInt( Collection::size )
                                                                   .sum();
      final Map<String,Collection<ConceptAggregate>> maxRelationNeoplasms
            = getBestSiteHistologyByCount( maxMentionNeoplasms, bestHistologies, countRelations );
      if ( maxRelationNeoplasms.size() == 1 ) {
         return new ArrayList<>( maxRelationNeoplasms.keySet() ).get( 0 );
      }
      return "";
   }

   static private Map<String,Collection<ConceptAggregate>> getBestSiteHistologyByCount(
         final Map<String,Collection<ConceptAggregate>> siteHistologyNeoplasms,
         final Collection<String> bestHistologies,
         final ToIntFunction<ConceptAggregate> countFunction ) {
      final Map<String,Collection<ConceptAggregate>> maxCountNeoplasms = new HashMap<>();
      int max = 0;
      for ( Map.Entry<String,Collection<ConceptAggregate>> entry : siteHistologyNeoplasms.entrySet() ) {
         final String code = entry.getKey();
         if ( !bestHistologies.contains( code ) || code.equals( CARCINOMA_NOS ) ) {
            continue;
         }
         int count = entry.getValue().stream()
                          .mapToInt( countFunction )
                          .sum();
         if ( count > max ) {
            max = count;
            maxCountNeoplasms.clear();
         }
         if ( count >= max ) {
            maxCountNeoplasms.computeIfAbsent( code, n -> new HashSet<>() )
                             .addAll( entry.getValue() );
         }
      }
      return maxCountNeoplasms;
   }





}
