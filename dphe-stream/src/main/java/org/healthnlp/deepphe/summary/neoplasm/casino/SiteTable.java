package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.topography.Topography;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.DISEASE_MAY_HAVE_FINDING;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2022}
 */
public class SiteTable extends AttributeTable {


//   static final double TOPOGRAPHY_CUTOFF = 0.20;
//   static final double TOPOGRAPHY_CUTOFF = 0.40;
   static final double TOPOGRAPHY_CUTOFF = 0.30;
   static final String TOPOGRAPHY_UNDETERMINED = "C80";
   static final String TOPOGRAPHY_LYMPH_NODE = "C77";
//   static final Collection<String> TOPOGRAPHY_UNDETERMINED_AND_LYMPH = Arrays.asList( "C80", "C77" );
   static final Collection<String> TOPOGRAPHY_LYMPH_NODES = Collections.singletonList( "C77" );
   static final Collection<String> TOPOGRAPHY_UNDETERMINEDS = Collections.singletonList( "C80" );


   // For "relocated" cancer attributes, change the relation type.
   // We want to keep some indication of a relation to discovered site information,
   // but we no longer want it to be used for site calculations.
   static final String ADJUSTED_SITE_RELATION = DISEASE_MAY_HAVE_FINDING;


   static private final Map<String,Collection<String>> TOPOGRAPHY_GROUPS = new HashMap<>();
   static {
      // Mouth
      TOPOGRAPHY_GROUPS.put( "C00", Arrays.asList( "C00", "C01", "C03", "C07" ) );
      // Pharynx
      TOPOGRAPHY_GROUPS.put( "C14", Arrays.asList( "C09", "C10", "C11", "C12", "C13", "C14" ) );
      // Stomach and Small Intestine - 26 is digestive organ nos
      TOPOGRAPHY_GROUPS.put( "C16", Arrays.asList( "C15", "C16", "C17", "C26" ) );
      // Colon
      TOPOGRAPHY_GROUPS.put( "C18", Arrays.asList( "C18", "C19", "C20", "C21" ) );
      // Larynx, Trachea
      TOPOGRAPHY_GROUPS.put( "C32", Arrays.asList( "C32", "C33" ) );
      // Lung, Respiratory System
      TOPOGRAPHY_GROUPS.put( "C34", Arrays.asList( "C34", "C39" ) );
//      // Female Reproduction, Pelvis Bladder and Genital NOS.  C56 is Ovary
//      TOPOGRAPHY_GROUPS.put( "C56", Arrays.asList( "C51", "C52", "C53", "C54", "C55", "C56", "C57",
//                                            "C65", "C66", "C67","C68" ) );
//      // Male Reproduction, Pelvis Bladder and Genital NOS.  C61 is Prostate
//      TOPOGRAPHY_GROUPS.put( "C61", Arrays.asList( "C60", "C61", "C62", "C63",
//                                            "C65", "C66", "C67","C68"  ) );
      // Female, Male, and overall genitals.
      TOPOGRAPHY_GROUPS.put( "C68", Arrays.asList( "C51", "C52", "C53", "C54", "C55", "C56", "C57",
                                                   "C60", "C61", "C62", "C63",
                                                   "C65", "C66", "C67","C68" ) );
      // Undetermined.  Body Tissue, Cranial Nerve, Peripheral Nerve,
      TOPOGRAPHY_GROUPS.put( "C80", Arrays.asList( "C80", "C76", "C49", "C71", "C72", "C47" ) );
   }

   static private String getSiteGroup( final String bestCode ) {
      final String code = bestCode.substring( 0, 3 );
      return TOPOGRAPHY_GROUPS.entrySet().stream()
                              .filter( e -> e.getValue().contains( code ) )
                              .map( Map.Entry::getKey )
                              .findFirst()
                              .orElse( code );
   }





   // Get all topographies for all neoplasms (cancer and tumor)
   private final Map<String, Collection<ConceptAggregate>> _allSiteNeoplasmsMap;
//   private final Collection<String> _bestSites;
//   private final Collection<String> _notBestSites;
   // Get the best site major for all neoplasms (cancer and tumor), keeping TOPOGRAPHY_UNDETERMINED
   private final Map<String, Collection<ConceptAggregate>> _bestSiteNeoplasmsMap;
//   private final Map<String,Collection<ConceptAggregate>> _notBestSiteNeoplasmsMap;
//   private final Collection<ConceptAggregate> _undeterminedSiteNeoplasms;


   // Sites that aren't the best.
   private final Collection<String> _unwantedSites;


   /**
    * Holds multiple collections of neoplasms, grouped by major topography.
    * On initialization this deals out the neoplasms by site, collects the best sites,
    * and distributes attributes among neoplasms on the best sites.
    *
    * @param neoplasms -
    * @param allConcepts -
    */
   public SiteTable( final Collection<ConceptAggregate> neoplasms,
                     final Collection<ConceptAggregate> allConcepts ) {
      // Get all topographies for all neoplasms (cancer and tumor)
      _allSiteNeoplasmsMap = collectAll( neoplasms, allConcepts );
      // Get the best site major for all neoplasms (cancer and tumor), keeping TOPOGRAPHY_UNDETERMINED
      _bestSiteNeoplasmsMap = collectBest();
      // For each site that is not undetermined, distribute relations across all site neoplasms.
      // Do this before working with histologies to reduce nos histologies at best sites.
      _bestSiteNeoplasmsMap.entrySet()
                           .stream()
                           .filter( e -> !e.getKey()
                                           .equals( TOPOGRAPHY_UNDETERMINED ) )
                           .map( Map.Entry::getValue )
                           .forEach( SiteTable::relateForSameSite );
      _unwantedSites = new HashSet<>( _allSiteNeoplasmsMap.keySet() );
      _unwantedSites.removeAll( _bestSiteNeoplasmsMap.keySet() );
   }


   public Map<String,Collection<ConceptAggregate>> getAllNeoplasmsMap() {
      return _allSiteNeoplasmsMap;
   }
   public Map<String,Collection<ConceptAggregate>> getBestNeoplasmsMap() {
      return _bestSiteNeoplasmsMap;
   }
   public Collection<String> getUnwantedCodes() {
      return _unwantedSites;
   }

   public Collection<String> getGenericCodes() {
      return Collections.singletonList( TOPOGRAPHY_UNDETERMINED );
   }


   public String getCode( final ConceptAggregate neoplasm,
                          final Collection<ConceptAggregate> neoplasms,
                          final Collection<ConceptAggregate> allConcepts ) {
      final Topography topography = new Topography( neoplasm, allConcepts );
      final String bestCode = topography.getBestMajorTopoCode();
      return getSiteGroup( bestCode );
   }

   public Map<String,Collection<ConceptAggregate>> collectBest() {
      if ( _allSiteNeoplasmsMap.size() <= 1 ) {
         return new HashMap<>( _allSiteNeoplasmsMap );
      }
      return getNeoplasmsAboveCutoff( _allSiteNeoplasmsMap, TOPOGRAPHY_CUTOFF, TOPOGRAPHY_LYMPH_NODES, TOPOGRAPHY_UNDETERMINEDS );
   }

   public void addToBest( final String code, final ConceptAggregate neoplasm ) {
      _bestSiteNeoplasmsMap.computeIfAbsent( code, n -> new HashSet<>() ).add( neoplasm );
   }


   public Collection<ConceptAggregate> getNotBestNeoplasms() {
      final Collection<ConceptAggregate> notBest = new HashSet<>();
      _unwantedSites.stream()
                   .map( _allSiteNeoplasmsMap::get )
                   .forEach( notBest::addAll );
      notBest.addAll( _bestSiteNeoplasmsMap.getOrDefault( TOPOGRAPHY_UNDETERMINED,
                                                                      Collections.emptyList() ) );
      return notBest;
   }











   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting by site.
   //
   /////////////////////////////////////////////////////////////////////////

   /**
    * ICDO code is a higher level than unique uri.  e.g. br, left br, nipple, etc.  This should allow better merges.
    * @param neoplasms -
    * @param allConcepts -
    * @return map of major topo codes to list of concepts with those topo codes.
    */
   static private Map<String, Collection<ConceptAggregate>> collectAllBySite(
         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> siteNeoplasms = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final Topography topography = new Topography( neoplasm, allConcepts );
         final String bestCode = topography.getBestMajorTopoCode();
         final String bestGroup = getSiteGroup( bestCode );
         siteNeoplasms.computeIfAbsent( bestGroup, c -> new ArrayList<>() ).add( neoplasm );
      }
      return siteNeoplasms;
   }

   static private Map<String,Collection<ConceptAggregate>> collectBestBySite(
         final Map<String,Collection<ConceptAggregate>> siteNeoplasms,
         final Collection<String> keepCodes ) {
      if ( siteNeoplasms.size() <= 1 ) {
         return new HashMap<>( siteNeoplasms );
      }
      return getNeoplasmsAboveCutoff( siteNeoplasms, TOPOGRAPHY_CUTOFF, keepCodes, TOPOGRAPHY_UNDETERMINEDS );
   }



   /**
    * For all the neoplasms with the same histology, distribute all the relations except for sites.
    * @param sameSiteNeoplasms either neoplasms with the same histology or neoplasms with the same site.
    */
   static private void relateForSameSite( final Collection<ConceptAggregate> sameSiteNeoplasms ) {
      final Map<String,Collection<ConceptAggregate>> allRelations = new HashMap<>();
      for ( ConceptAggregate neoplasm : sameSiteNeoplasms ) {
         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
            allRelations.computeIfAbsent( relatedConcepts.getKey(), r -> new HashSet<>() )
                        .addAll( relatedConcepts.getValue() );
         }
      }
      for ( ConceptAggregate neoplasm : sameSiteNeoplasms ) {
         allRelations.forEach( neoplasm::addRelated );
      }
   }



   /**
    * For all the neoplasms with an undetermined (or minority) site and nos histology,
    * distribute all the relations except for sites.
    * @param patientId -
    * @param undeterminedNosNeoplasms -
    * @param bestSiteNeoplasmsMap -
    */
   static private void relateUndeterminedSiteNos( final String patientId,
                                                  final Collection<ConceptAggregate> undeterminedNosNeoplasms,
                                                  final Map<String,Collection<ConceptAggregate>> bestSiteNeoplasmsMap,
                                                  final Collection<ConceptAggregate> allConcepts ) {
      final Map<String,Collection<ConceptAggregate>> adjustedRelations = new HashMap<>();
      for ( ConceptAggregate neoplasm : undeterminedNosNeoplasms ) {
         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
            final String name = RelationConstants.isHasSiteRelation( relatedConcepts.getKey() )
                                ? ADJUSTED_SITE_RELATION : relatedConcepts.getKey();
            adjustedRelations.computeIfAbsent( name, r -> new HashSet<>() )
                             .addAll( relatedConcepts.getValue() );
         }
      }
      for ( Map.Entry<String,Collection<ConceptAggregate>> relations : adjustedRelations.entrySet() ) {
         bestSiteNeoplasmsMap.values().stream()
                             .flatMap( Collection::stream )
                             .forEach( n -> n.addRelated( relations.getKey(), relations.getValue() ) );
      }
      final ConceptAggregate undeterminedNosNeoplasm
            = NeoplasmDealer.createMergedNeoplasm( patientId, undeterminedNosNeoplasms, Collections.emptyList() );
      bestSiteNeoplasmsMap.values().forEach( c -> c.add( undeterminedNosNeoplasm ) );
      NeoplasmDealer.replaceRelated( undeterminedNosNeoplasm, undeterminedNosNeoplasms, allConcepts );
      allConcepts.removeAll( undeterminedNosNeoplasms );
      allConcepts.add( undeterminedNosNeoplasm );
   }



}
