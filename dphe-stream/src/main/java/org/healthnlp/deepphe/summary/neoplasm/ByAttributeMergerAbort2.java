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
final public class ByAttributeMergerAbort2 {

   private ByAttributeMergerAbort2() {}

   static private final double TOPOGRAPHY_CUTOFF = 0.20;
   static private final String TOPOGRAPHY_UNKNOWN = "C80";
   static private final String TOPOGRAPHY_ILL_DEFINED = "C76";
   static private final double HISTOLOGY_CUTOFF = 0.30;
   static private final String CARCINOMA_NOS = "801";
   static private final double LATERALITY_CUTOFF = 0.30;
   static private final String LATERALITY_NONE = "0";


   static private Collection<ConceptAggregate> mergeCancers( final Collection<ConceptAggregate> neoplasms,
                                                             final Collection<ConceptAggregate> allConcepts ) {

      final Map<String,Collection<ConceptAggregate>> topographyNeoplasms
            = collectByTopography( neoplasms, allConcepts );
      final Map<String,Collection<ConceptAggregate>> histologyNeoplasms
            = collectByHistology( neoplasms, allConcepts );
      // Neoplasms with given histologies at each site.  e.g.  breast<brca,cancers>
      final Map<String,Map<String,Collection<ConceptAggregate>>> topoHistoNeoplasms
            = collectTopoHistos( topographyNeoplasms, histologyNeoplasms );
      // Distribute Lateralities within sites.
      final Map<String,Map<String,Map<String,Collection<ConceptAggregate>>>> topoLatHistoNeoplasms
            = new HashMap<>( topoHistoNeoplasms.size() );
      for ( Map.Entry<String,Map<String,Collection<ConceptAggregate>>> entry : topoHistoNeoplasms.entrySet() ) {
         final Map<String,Map<String,Collection<ConceptAggregate>>> latHistoNeoplasms
               = collectLaterals( entry.getKey(), entry.getValue(), allConcepts );
         topoLatHistoNeoplasms.put( entry.getKey(), latHistoNeoplasms );
      }


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

      //  Sort by topo major first and do reductions.  <breast<histology,tumors>>.   Reductions include lat.
      //  Then do lat reductions within the major topo.
      //  Then do distribution of topo C80.
      //  Then distribution of lat 0.

      final Map<String,Collection<ConceptAggregate>> topoMajorTumors = collectByTopography( tumors, allConcepts );
      final Map<String,Collection<ConceptAggregate>> histologyTumors = collectByHistology( tumors, allConcepts );


      // Now we have collections of ConceptAggregates sorted by site and also sorted by histology.
      // The tricky part is joining them together.
      // Try distribution by Histo first:  BrCa<Breast<Right,tumors>> == 850<C50<1,tumors>>


      return null;
   }

   /////////////////////////////////////////////////////////////////////////
   //
   //         Neoplasm Sorting.
   //
   /////////////////////////////////////////////////////////////////////////

   /**
    * ICDO code is a higher level than unique uri.  e.g. br, left br, nipple, etc.  This should allow better merges.
    * @param neoplasms -
    * @param allConcepts -
    * @return map of major topo codes to list of concepts with those topo codes.
    */
   static private Map<String, Collection<ConceptAggregate>> collectByTopography(
         final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> topographyNeoplasms = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final Topography topography = new Topography( neoplasm, allConcepts );
         final String bestCode = topography.getBestMajorTopoCode();
         topographyNeoplasms.computeIfAbsent( bestCode, c -> new ArrayList<>() ).add( neoplasm );
      }
      distributeLowTopos( topographyNeoplasms );
      return topographyNeoplasms;
   }


   static private void distributeLowTopos( final Map<String,Collection<ConceptAggregate>> topographyNeoplasms ) {
      if ( topographyNeoplasms.size() <= 1 ) {
         return;
      }
      distributeByLowCount( topographyNeoplasms, TOPOGRAPHY_CUTOFF, 3, TOPOGRAPHY_UNKNOWN );
    }


    static private Map<String,Collection<ConceptAggregate>> collectByHistology(
          final Collection<ConceptAggregate> neoplasms, final Collection<ConceptAggregate> allConcepts ) {
      final Map<String, Collection<ConceptAggregate>> histologyNeoplasms = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final String bestCode = getHistologyCode( neoplasm, neoplasms, allConcepts );
         histologyNeoplasms.computeIfAbsent( bestCode, c -> new ArrayList<>() ).add( neoplasm );
      }
       distributeLowHistoAllSites( histologyNeoplasms );
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


   static private void distributeLowHistoAllSites( final Map<String,Collection<ConceptAggregate>> histologyNeoplasms ) {
      if ( histologyNeoplasms.size() <= 1 ) {
         return;
      }
      distributeByLowCount( histologyNeoplasms, HISTOLOGY_CUTOFF, 3, CARCINOMA_NOS );
   }

   static private void distributeLowHistoOneSite( final Map<String,Collection<ConceptAggregate>> histologyNeoplasms ) {
      if ( histologyNeoplasms.size() <= 1 ) {
         return;
      }
      distributeByLowCount( histologyNeoplasms, HISTOLOGY_CUTOFF, 1, CARCINOMA_NOS );
   }







   static private void distributeByLowCount( final Map<String,Collection<ConceptAggregate>> codeNeoplasms,
                                              final double cutoff_constant,
                                             final int maxDesired,
                                             final String unknown_key ) {
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
      distributeByLowCount( codeNeoplasms, codeMentionCounts, cutoff, unknown_key );
      if ( codeNeoplasms.size() > maxDesired ) {
         // Try a higher filter.
         cutoff = total * cutoff_constant;
         distributeByLowCount( codeNeoplasms, codeMentionCounts, cutoff, unknown_key );
      }
   }


   static private void distributeByLowCount( final Map<String,Collection<ConceptAggregate>> codeNeoplasms,
                                             final Map<String,Integer> codeMentionCounts,
                                             final double cutoff,
                                             final String unknown_key ) {
      final Collection<String> lowCountCodes = codeMentionCounts.entrySet().stream()
                                                                .filter( e -> e.getValue() < cutoff )
                                                                .map( Map.Entry::getKey )
                                                                .collect( Collectors.toList() );
      if ( codeNeoplasms.containsKey( unknown_key ) ) {
         lowCountCodes.add( unknown_key );
      }
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





   static private Map<String,Map<String,Collection<ConceptAggregate>>> collectTopoHistos(
         final Map<String,Collection<ConceptAggregate>> topographyNeoplasms,
         final Map<String,Collection<ConceptAggregate>> histologyNeoplasms ) {
      final Map<ConceptAggregate,Collection<String>> neoplasmHistologies = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptAggregate>> histoNeoplasms : histologyNeoplasms.entrySet() ) {
         final String histology = histoNeoplasms.getKey();
         histoNeoplasms.getValue()
                       .forEach( n -> neoplasmHistologies.computeIfAbsent( n, h -> new ArrayList<>() )
                                                         .add( histology ) );
      }
      final Map<String,Map<String,Collection<ConceptAggregate>>> topoHistoNeoplasms
            = new HashMap<>( topographyNeoplasms.size() );
      for ( Map.Entry<String,Collection<ConceptAggregate>> topoNeoplasms : topographyNeoplasms.entrySet() ) {
         final Map<String,Collection<ConceptAggregate>> inTopoHistos = new HashMap<>();
         for ( ConceptAggregate neoplasm : topoNeoplasms.getValue() ) {
            final Collection<String> histos = neoplasmHistologies.getOrDefault( neoplasm, Collections.emptyList() );
            for ( String histo : histos ) {
               inTopoHistos.computeIfAbsent( histo, h -> new HashSet<>() ).add( neoplasm );
            }
         }
         // Try to get the number of histologies within the site down to 1.
         distributeLowHistoOneSite( inTopoHistos );

         topoHistoNeoplasms.put( topoNeoplasms.getKey(), inTopoHistos );
      }
      return topoHistoNeoplasms;
   }


   static private Map<String,Map<String,Collection<ConceptAggregate>>> collectLaterals(
         final String topoCode,
         final Map<String,Collection<ConceptAggregate>> histoNeoplasms,
         final Collection<ConceptAggregate> allConcepts ) {
      final Map<String,Map<String,Collection<ConceptAggregate>>> latHistoNeoplasms = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptAggregate>> histoNeoplasmsEntry : histoNeoplasms.entrySet() ) {
         final String histology = histoNeoplasmsEntry.getKey();
         final Collection<ConceptAggregate> neoplasms = histoNeoplasmsEntry.getValue();
         for ( ConceptAggregate neoplasm : neoplasms ) {
            final String laterality = getLaterality( topoCode, neoplasm, neoplasms, allConcepts );
            latHistoNeoplasms.computeIfAbsent( laterality, l -> new HashMap<>() )
                                     .computeIfAbsent( histology, h -> new ArrayList<>() )
                                     .add( neoplasm );
         }
      }
      return latHistoNeoplasms;
   }

   static private String getLaterality(
         final String topoCode, final ConceptAggregate neoplasm, final Collection<ConceptAggregate> neoplasms,
         final Collection<ConceptAggregate> allConcepts ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topoCode );
      final DefaultAttribute<LateralUriInfoVisitor, LateralityCodeInfoStore> lateralityCode
            = new DefaultAttribute<>( "laterality_code",
                                      neoplasm,
                                      allConcepts,
                                      neoplasms,
                                      LateralUriInfoVisitor::new,
                                      LateralityCodeInfoStore::new,
                                      dependencies );
      return lateralityCode.getBestCode();
   }

}
