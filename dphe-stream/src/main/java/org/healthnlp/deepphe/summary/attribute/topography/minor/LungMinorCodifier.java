package org.healthnlp.deepphe.summary.attribute.topography.minor;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/30/2022}
 */
final public class LungMinorCodifier {

   static private Collection<String> LUNG_URIS;
   static private Collection<String> BRONCHUS_URIS;
   static private Collection<String> UPPER_LOBE_URIS;
   static private Collection<String> MIDDLE_LOBE_URIS;
   static private Collection<String> LOWER_LOBE_URIS;
   static private Collection<String> TRACHEA_URIS;
   static private final Predicate<ConceptAggregate> isLungPart
         = c -> c.getAllUris()
                 .stream()
                 .anyMatch( u -> LUNG_URIS.contains( u )
                                 || BRONCHUS_URIS.contains( u )
                                 || TRACHEA_URIS.contains( u ) );

   private LungMinorCodifier() {}

   static private void initLungUris() {
      if ( LUNG_URIS != null ) {
         return;
      }
      LUNG_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Lung" );
      BRONCHUS_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Bronchus" );
      UPPER_LOBE_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Upper_Lobe_Of_The_Lung" );
      MIDDLE_LOBE_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Middle_Lobe_Of_The_Right_Lung" );
      LOWER_LOBE_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Lower_Lung_Lobe" );
      TRACHEA_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Trachea" );
   }


   static public Collection<ConceptAggregate> getLungParts( final Collection<ConceptAggregate> neoplasms ) {
      initLungUris();
      return neoplasms.stream()
                        .map( ConceptAggregate::getRelatedSites )
                        .flatMap( Collection::stream )
                        .filter( isLungPart )
                        .collect( Collectors.toSet() );
   }

//   Lung = pneumo-, pulmono-, broncho-, bronchiolo-, alveolar, hilar, Breathing = -pnea
//
//ICD-O-2/3	Term
//C34.0	Main bronchus
//C34.1	Upper lobe, lung
//C34.2	Middle lobe, lung (right lung only)
//C34.3	Lower lobe, lung
//C34.8	Overlapping lesion of lung
//C34.9	Lung, NOS
//C33.9	Trachea, NOS


   static String getBestLung( final Map<String,Integer> uriStrengths ) {
      if ( uriStrengths.isEmpty() ) {
         return "9";
      }
      final Map<Integer, List<String>> hitCounts = TopoMinorCodeInfoStore.getHitCounts( uriStrengths );
      // Prefer lobes to bronchus
      final int bestMinorCode = hitCounts.keySet()
                                         .stream()
                                         .sorted( Comparator.comparingInt( Integer::intValue )
                                                            .reversed() )
                                         .map( hitCounts::get )
                                         .map( LungMinorCodifier::getBestMinorNumber )
                                         .filter( n -> n >= 0 )
                                         .findFirst().orElse( -1 );
      if ( bestMinorCode >= 0 ) {
         return "" + bestMinorCode;
      }
      return "" + hitCounts.keySet()
                           .stream()
                           .sorted( Comparator.comparingInt( Integer::intValue )
                                              .reversed() )
                           .map( hitCounts::get )
                           .map( LungMinorCodifier::getOtherMinorNumber )
                           .filter( n -> n >= 0 )
                           .findFirst()
                           .orElse( 9 );
   }

   static int getBestMinorNumber( final Collection<String> uris ) {
      return uris.stream()
                 .mapToInt( LungMinorCodifier::getUriMinorNumber )
                 .max()
                 .orElse( -1 );
   }

   static private int getOtherMinorNumber(  final Collection<String> uris ) {
      return uris.stream()
                 .mapToInt( LungMinorCodifier::getOtherUriMinorNumber )
                 .max()
                 .orElse( -1 );
   }


   static private int getUriMinorNumber( final String uri ) {
      if ( UPPER_LOBE_URIS.contains( uri ) ) {
         return 1;
      } else if ( MIDDLE_LOBE_URIS.contains( uri ) ) {
         return 2;
      } else if ( LOWER_LOBE_URIS.contains( uri ) ) {
         return 3;
      }
      return -1;
   }

   static private int getOtherUriMinorNumber( final String uri ) {
      if ( BRONCHUS_URIS.contains( uri ) ) {
         return 0;
      } else if ( LUNG_URIS.contains( uri )
                  || TRACHEA_URIS.contains( uri ) ) {
         return 9;
      }
      return -1;
   }

}
