package org.healthnlp.deepphe.nlp.attribute.topo_minor.breast;


import org.healthnlp.deepphe.nlp.attribute.topo_minor.TopoMinorNormalizer;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
public class BreastNormalizer extends TopoMinorNormalizer {


   static private final Collection<String> CENTERS
         = Arrays.asList( "n_12O_qt_clockPosition", "n_3O_qt_clockPosition",
         "n_6O_qt_clockPosition", "n_9O_qt_clockPosition" );
   static private final Collection<String> C_12_3
         = Arrays.asList( "n_12_dot_30O_qt_clockPosition", "n_1O_qt_clockPosition", "n_1_dot_30O_qt_clockPosition",
         "n_2O_qt_clockPosition", "n_2_dot_30O_qt_clockPosition" );
   static private final Collection<String> C_3_6
         = Arrays.asList( "n_3_dot_30O_qt_clockPosition", "n_4O_qt_clockPosition", "n_4_dot_30O_qt_clockPosition",
         "n_5O_qt_clockPosition", "n_5_dot_30O_qt_clockPosition" );
   static private final Collection<String> C_6_9
         = Arrays.asList( "n_6_dot_30O_qt_clockPosition", "n_7O_qt_clockPosition", "n_7_dot_30O_qt_clockPosition",
         "n_8O_qt_clockPosition", "n_8_dot_30O_qt_clockPosition" );
   static private final Collection<String> C_9_12
         = Arrays.asList(  "n_9_dot_30O_qt_clockPosition", "n_10O_qt_clockPosition", "n_10_dot_30O_qt_clockPosition",
         "n_11O_qt_clockPosition", "n_11_dot_30O_qt_clockPosition" );

//   private String _lateralityCode = "";
   private Collection<String> _lateralityCodes;

   public void init( final AttributeInfoCollector infoCollector, final Map<String, List<XnAttributeValue>> dependencies,
                     final long mentionCount ) {
      _lateralityCodes = dependencies.getOrDefault( "Laterality", Collections.emptyList() )
                                     .stream().map( XnAttributeValue::getValue ).collect( Collectors.toSet() );
      super.init( infoCollector, dependencies, mentionCount );
   }


   @Override
   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      if ( _lateralityCodes.isEmpty() ) {
         return String.valueOf( getBreastCode( uri, "" ) );
      }
      return _lateralityCodes.stream().map( l -> getBreastCode( uri, l ) ).distinct()
                             .map( String::valueOf ).collect( Collectors.joining( "," ) );
   }

   static private int getBreastCode( final String uri, final String lateralityCode ) {
//      https://training.seer.cancer.gov/breast/anatomy/quadrants.html
//      https://training.seer.cancer.gov/breast/abstract-code-stage/codes.html
      if ( uri.contains( "Nipple" ) ) {
         return 0;
      } else if ( uri.startsWith( "CentralPortionOfTheBreast" )
                  || uri.contains( "Areola" )
                  || uri.contains( "SubareolarRegion" )) {
         return 1;
      } else if ( uri.startsWith( "Upper_sub_InnerQuadrant" ) ) {
         return 2;
      } else if ( uri.startsWith( "Lower_sub_InnerQuadrant" ) ) {
         return 3;
      } else if ( uri.startsWith( "Upper_sub_OuterQuadrant" ) ) {
         return 4;
      } else if ( uri.startsWith( "Lower_sub_OuterQuadrant" ) ) {
         return 5;
      } else if ( uri.contains( "AxillaryTail" ) ) {
         return 6;
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
      return 9;
   }

   static private int getLateralBreastCode( final String uri, final String lateralityCode ) {
//      https://training.seer.cancer.gov/breast/anatomy/quadrants.html
//      https://training.seer.cancer.gov/breast/abstract-code-stage/codes.html
      if ( uri.contains( "Nipple" ) ) {
         return 0;
      } else if ( uri.startsWith( "CentralPortionOfTheBreast" )
            || uri.contains( "Areola" )
            || uri.contains( "SubareolarRegion" )) {
         return 1;
      } else if ( uri.startsWith( "Upper_sub_InnerQuadrant" ) ) {
         return 2;
      } else if ( uri.startsWith( "Lower_sub_InnerQuadrant" ) ) {
         return 3;
      } else if ( uri.startsWith( "Upper_sub_OuterQuadrant" ) ) {
         return 4;
      } else if ( uri.startsWith( "Lower_sub_OuterQuadrant" ) ) {
         return 5;
      } else if ( uri.contains( "AxillaryTail" ) ) {
         return 6;
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
      return 9;
   }






//   ICDO Codes for Breast:
//   //   C50.0	Nipple
////C50.1	Central portion of breast
////C50.2	Upper-inner quadrant of breast (UIQ)
////C50.3	Lower-inner quadrant of breast (LIQ)
////C50.4	Upper-outer quadrant of breast (UOQ)
////C50.5	Lower-outer quadrant of breast (LOQ)
////C50.6	Axillary tail of breast
////C50.8	Overlapping lesion of breast
////C50.9	Breast, NOS (excludes Skin of breast C44.5);
// multi-focal neoplasm in more than one quadrant of the breast.

//   Branch Uris for Breast_Quadrant
//Upper_inner_Quadrant 4
//Lower_Inner_Quadrant_Of_Left_Breast 5
//Upper_Outer_Quadrant_Of_Right_Breast 5
//Lower_Outer_Quadrant 4
//Lower_Inner_Quadrant_Of_Right_Breast 5
//Lower_Outer_Quadrant_Of_Male_Breast 4
//Upper_Outer_Quadrant 4
//Lower_Inner_Quadrant 4
//Upper_Inner_Quadrant_Of_Right_Breast 5
//Upper_Inner_Quadrant_Of_Left_Breast 5
//Lower_Outer_Quadrant_Of_Left_Breast 5
//Upper_Outer_Quadrant_Of_Male_Breast 4
//Upper_Inner_Quadrant_Of_Male_Breast 4
//Upper_Outer_Quadrant_Of_Left_Breast 5
//Lower_Inner_Quadrant_Of_Male_Breast 4
//Lower_Outer_Quadrant_Of_Right_Breast 5
//Breast_Quadrant 3
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Nipple" ) );
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Areola" ) );
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Central_Portion_Of_The_Breast" ) );
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Subareolar_Region" ) );
//   QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Axillary_Tail_Of_The_Breast" ) );

}
