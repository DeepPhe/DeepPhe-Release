package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.histology.Histology;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyUriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2022}
 */
final public class HistologyTable extends AttributeTable {


   static final double MULTI_SITE_HISTOLOGY_CUTOFF = 0.30;
   static final double ONE_SITE_HISTOLOGY_CUTOFF = 0.50;
   // 8010 is all carcinoma nos. /0 Benign (epithelioma), /2 In Situ and /3 Malignant (DNE)
   // 800* might be unknown.
   // We only want to use the first 3 digits of the histology.  Those are the "Major" Histologic Types.
   static final String CARCINOMA_NOS = "801";
   static final Collection<String> CARCINOMA_UNKNOWNS = Arrays.asList( "800", "801" );
   static private final Collection<String> KEEP_CARCINOMA_NOS = Collections.emptyList();


   // Get all histologies for all neoplasms (cancer and tumor)
   private final Map<String, Collection<ConceptAggregate>> _allHistologyNeoplasmsMap;
   // Get the best histologies for all neoplasms (cancer and tumor), keeping CARCINOMA_NOS
   private final Map<String, Collection<ConceptAggregate>> _bestHistologyNeoplasmsMap;
   private final Collection<String> _unwantedHistologies;


   /**
    * Holds multiple collections of neoplasms, grouped by histology.
    * On initialization this deals out the neoplasms by histology, collects the best histologies,
    * and distributes attributes among neoplasms with the best histologies.
    *
    * @param neoplasms -
    * @param allConcepts -
    */
   public HistologyTable( final Collection<ConceptAggregate> neoplasms,
                          final Collection<ConceptAggregate> allConcepts ) {
      // Get all histologies for all neoplasms (cancer and tumor)
      _allHistologyNeoplasmsMap = collectAll( neoplasms, allConcepts );
      // Get the best histologies for all neoplasms (cancer and tumor), keeping CARCINOMA_NOS
      _bestHistologyNeoplasmsMap = collectBest();
      // Distribute non-site relations for neoplasms with the same histology.  Don't worry about repeats.
      _bestHistologyNeoplasmsMap.entrySet()
                                .stream()
                                .filter( e -> !CARCINOMA_UNKNOWNS.contains( e.getKey() ) )
                                .map( Map.Entry::getValue )
                                .forEach( HistologyTable::relateForSameHistologies );
      _unwantedHistologies = new HashSet<>( _allHistologyNeoplasmsMap.keySet() );
      _unwantedHistologies.removeAll( _bestHistologyNeoplasmsMap.keySet() );
   }


   public Map<String,Collection<ConceptAggregate>> getAllNeoplasmsMap() {
      return _allHistologyNeoplasmsMap;
   }
   public Map<String,Collection<ConceptAggregate>> getBestNeoplasmsMap() {
      return _bestHistologyNeoplasmsMap;
   }
   public Collection<String> getUnwantedCodes() {
      return _unwantedHistologies;
   }

   public Collection<String> getGenericCodes() {
      return CARCINOMA_UNKNOWNS;
   }

   public String getCode( final ConceptAggregate neoplasm,
                          final Collection<ConceptAggregate> neoplasms,
                          final Collection<ConceptAggregate> allConcepts ) {
      final String bestCode = getHistologyCode( neoplasm, neoplasms, allConcepts );
      return bestCode.substring( 0, 3 );
   }

   public Map<String,Collection<ConceptAggregate>> collectBest() {
      if ( _allHistologyNeoplasmsMap.size() <= 1 ) {
         return new HashMap<>( _allHistologyNeoplasmsMap );
      }
      return getNeoplasmsAboveCutoff( _allHistologyNeoplasmsMap,
                                      MULTI_SITE_HISTOLOGY_CUTOFF,
                                      CARCINOMA_UNKNOWNS,
                                      CARCINOMA_UNKNOWNS );
   }

   public void addToBest( final String code, final ConceptAggregate neoplasm ) {
      _bestHistologyNeoplasmsMap.computeIfAbsent( code, n -> new HashSet<>() ).add( neoplasm );
   }

   public Collection<ConceptAggregate> getNotBestNeoplasms() {
      final Collection<ConceptAggregate> notBest = new HashSet<>();
      _unwantedHistologies.stream()
                          .map( _allHistologyNeoplasmsMap::get )
                          .forEach( notBest::addAll );
      CARCINOMA_UNKNOWNS.forEach( c -> notBest.addAll(
            _bestHistologyNeoplasmsMap.getOrDefault( c, Collections.emptyList() ) ) );
      return notBest;
   }



   /**
    * For all the neoplasms with the same histology, distribute all the relations except for sites.
    * @param sameHistologyNeoplasms either neoplasms with the same histology or neoplasms with the same site.
    */
   static void relateForSameHistologies( final Collection<ConceptAggregate> sameHistologyNeoplasms ) {
      final Map<String,Collection<ConceptAggregate>> nonSiteRelations = new HashMap<>();
      for ( ConceptAggregate neoplasm : sameHistologyNeoplasms ) {
         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
            if ( RelationConstants.isHasSiteRelation( relatedConcepts.getKey() ) ) {
               continue;
            }
            nonSiteRelations.computeIfAbsent( relatedConcepts.getKey(), r -> new HashSet<>() )
                            .addAll( relatedConcepts.getValue() );
         }
      }
      for ( ConceptAggregate neoplasm : sameHistologyNeoplasms ) {
         nonSiteRelations.forEach( neoplasm::addRelated );
      }
   }





   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting by histology.
   //
   /////////////////////////////////////////////////////////////////////////


   static private Map<String,Collection<ConceptAggregate>> collectBestByHistology(
         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
         final Collection<String> keepCodes ) {
      return collectBestByHistology( histologyNeoplasms, MULTI_SITE_HISTOLOGY_CUTOFF, keepCodes );
   }


   static Map<String,Collection<ConceptAggregate>> collectBestByHistology(
         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
         final double cutoff,
         final Collection<String> keepCodes ) {
      if ( histologyNeoplasms.size() <= 1 ) {
         return new HashMap<>( histologyNeoplasms );
      }
      return getNeoplasmsAboveCutoff( histologyNeoplasms, cutoff, keepCodes, CARCINOMA_UNKNOWNS );
   }



   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting by site histology.
   //
   /////////////////////////////////////////////////////////////////////////

   // All 850 = Ductal Carcinoma.  8500 = BrCa in situ or invasive.  8501 = Comedocarcinoma.
   // 8502 = Secretory_Breast_Carcinoma.  8503 = Intraductal_Papillary_Breast_Carcinoma.  etc.
   // todo - should there be a more exact/narrow sort?
   static String getHistologyCode( final ConceptAggregate neoplasm,
                                           final Collection<ConceptAggregate> allNeoplasms,
                                           final Collection<ConceptAggregate> allConcepts ) {
      final DefaultAttribute<HistologyUriInfoVisitor, HistologyCodeInfoStore> histology
            = new Histology( neoplasm,
                             allConcepts,
                             allNeoplasms );
      return histology.getBestCode();
   }


   static private Map<String,Collection<ConceptAggregate>> getBestHistoOneSite(
         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms,
         final Collection<String> keepCodes ) {
      if ( histologyNeoplasms.size() <= 1 ) {
         return new HashMap<>( histologyNeoplasms );
      }
      return getNeoplasmsAboveCutoff( histologyNeoplasms, ONE_SITE_HISTOLOGY_CUTOFF, keepCodes, CARCINOMA_UNKNOWNS );
   }


   static private Map<String,Collection<String>> collectAllSiteHistologies(
         final Map<String,Collection<ConceptAggregate>> allSiteNeoplasms,
         final Map<String,Collection<ConceptAggregate>> allHistologyNeoplasms ) {
      final Map<ConceptAggregate,String> neoplasmHistologies = createNeoplasmCodeMap( allHistologyNeoplasms );
      final Map<String,Collection<String>> allSiteHistologies = new HashMap<>( allSiteNeoplasms.size() );
      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : allSiteNeoplasms.entrySet() ) {
         final Collection<String> histologies = siteNeoplasms.getValue().stream()
                                                             .map( neoplasmHistologies::get )
                                                             .collect( Collectors.toSet() );
         allSiteHistologies.put( siteNeoplasms.getKey(), histologies );
      }
      return allSiteHistologies;
   }







}
