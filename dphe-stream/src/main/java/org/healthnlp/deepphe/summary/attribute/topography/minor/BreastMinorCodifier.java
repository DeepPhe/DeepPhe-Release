package org.healthnlp.deepphe.summary.attribute.topography.minor;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_CLOCKFACE;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_QUADRANT;

/**
 * @author SPF , chip-nlp
 * @since {3/30/2022}
 */
final public class BreastMinorCodifier {

   private BreastMinorCodifier() {}

//   C50.0	Nipple
//C50.1	Central portion of breast
//C50.2	Upper-inner quadrant of breast (UIQ)
//C50.3	Lower-inner quadrant of breast (LIQ)
//C50.4	Upper-outer quadrant of breast (UOQ)
//C50.5	Lower-outer quadrant of breast (LOQ)
//C50.6	Axillary tail of breast
//C50.8	Overlapping lesion of breast
//C50.9	Breast, NOS (excludes Skin of breast C44.5); multi-focal neoplasm in more than one quadrant of the breast.

   static private final Collection<String> CENTERS
         = Arrays.asList( "_12_O_clock", "_3_O_clock", "_6_O_clock", "_9_O_clock" );
   static private final Collection<String> C_12_3
         = Arrays.asList( "_12_30_O_clock", "_1_O_clock", "_1_30_O_clock", "_2_O_clock", "_2_30_O_clock" );
   static private final Collection<String> C_3_6
         = Arrays.asList( "_3_30_O_clock", "_4_O_clock", "_4_30_O_clock", "_5_O_clock", "_5_30_O_clock" );
   static private final Collection<String> C_6_9
         = Arrays.asList( "_6_30_O_clock", "_7_O_clock", "_7_30_O_clock", "_8_O_clock", "_8_30_O_clock" );
   static private final Collection<String> C_9_12
         = Arrays.asList( "_9_30_O_clock", "_10_O_clock", "_10_30_O_clock", "_11_O_clock", "_11_30_O_clock" );

   static private Collection<String> QUADRANT_URIS;

   static private void initQuadrantUris() {
      if ( QUADRANT_URIS != null ) {
         return;
      }
      QUADRANT_URIS = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT );
   }


   static public String getQuadrant( final String code ) {
      if ( code.isEmpty() ) {
         return "";
      }
      switch ( code ) {
         case "2":
            return "Upper_Inner_Quadrant";
         case "3":
            return "Lower_Inner_Quadrant";
         case "4":
            return "Upper_Outer_Quadrant";
         case "5":
            return "Lower_Outer_Quadrant";
         case "8":
            return "Overlapping_Quadrant";
      }
      return "";
      }

   static public Collection<ConceptAggregate> getBreastParts( final Collection<ConceptAggregate> neoplasms ) {
      initQuadrantUris();
      final Collection<ConceptAggregate> breastConcepts = neoplasms.stream()
                                                                   .map( c -> c.getRelated( HAS_CLOCKFACE, HAS_QUADRANT ) )
                                                                   .flatMap( Collection::stream )
//                                                                      .filter( c -> !c.isNegated() )
                                                                   .collect( Collectors.toSet() );
      breastConcepts.addAll( neoplasms.stream()
                                      .map( ConceptAggregate::getRelatedSites )
                                      .flatMap( Collection::stream )
                                      .filter( c -> QUADRANT_URIS.contains( c.getUri() ) ).collect(
                  Collectors.toSet() ) );
      return breastConcepts;
   }

   static String getBestBreast( final Map<String,Integer> uriStrengths, final String lateralityCode ) {
      if ( uriStrengths.isEmpty() ) {
         return "9";
      }
      final Map<Integer, List<String>> hitCounts = TopoMinorCodeInfoStore.getHitCounts( uriStrengths );
      // Prefer quadrants to nipple.
      final int bestMinorCode = hitCounts.keySet()
                                         .stream()
                                         .sorted( Comparator.comparingInt( Integer::intValue )
                                                            .reversed() )
                                         .map( hitCounts::get )
                                         .map( u -> getBestMinorNumber( u, lateralityCode ) )
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
                           .map( BreastMinorCodifier::getOtherMinorNumber )
                           .filter( n -> n >= 0 )
                           .findFirst()
                           .orElse( 9 );
   }

   static int getBestMinorNumber( final Collection<String> uris, final String lateralityCode ) {
      return uris.stream()
                 .mapToInt( u -> getUriMinorNumber( u, lateralityCode ) )
                 .max()
                 .orElse( -1 );
   }

   static private int getOtherMinorNumber(  final Collection<String> uris ) {
      return uris.stream()
                 .mapToInt( BreastMinorCodifier::getOtherUriMinorNumber )
                 .max()
                 .orElse( -1 );
   }


   static private int getUriMinorNumber( final String uri, final String lateralityCode ) {
//      https://training.seer.cancer.gov/breast/anatomy/quadrants.html
//      https://training.seer.cancer.gov/breast/abstract-code-stage/codes.html
      if ( uri.startsWith( "Upper_Inner_Quadrant" ) ) {
         return 2;
      } else if ( uri.startsWith( "Lower_Inner_Quadrant" ) ) {
         return 3;
      } else if ( uri.startsWith(  "Upper_Outer_Quadrant" ) ) {
         return 4;
      } else if ( uri.startsWith(  "Lower_Outer_Quadrant" ) ) {
         return 5;
      } else if ( CENTERS.contains( uri ) ) {
         return 8;
      } else if ( C_12_3.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 2;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 4;
         }
      } else if ( C_3_6.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 3;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 5;
         }
      } else if ( C_6_9.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 5;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 3;
         }
      } else if ( C_9_12.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 4;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 2;
         }
      }
      return -1;
   }

   static private int getOtherUriMinorNumber( final String uri ) {
//      https://training.seer.cancer.gov/breast/anatomy/quadrants.html
//      https://training.seer.cancer.gov/breast/abstract-code-stage/codes.html
      if ( uri.equals( "Nipple" ) ) {
         return 0;
      } else if ( uri.startsWith( "Central_Region_Of_" ) || uri.contains( "reola" ) ) {
         return 1;
      }
      return -1;
   }

}
