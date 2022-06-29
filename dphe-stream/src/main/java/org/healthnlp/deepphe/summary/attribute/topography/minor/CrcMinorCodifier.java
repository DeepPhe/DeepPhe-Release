package org.healthnlp.deepphe.summary.attribute.topography.minor;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Both C18 and C21 are handled here because there is some overlap
 *
 * @author SPF , chip-nlp
 * @since {3/30/2022}
 */
final public class CrcMinorCodifier {

   private CrcMinorCodifier() {}

//   C18.0	Cecum
//C18.1	Appendix
//C18.2	Ascending colon; Right colon
//C18.3	Hepatic flexure of colon
//C18.4	Transverse colon
//C18.5	Splenic flexure of colon
//C18.6	Descending colon; Left colon
//C18.7	Sigmoid colon
//C18.8	Overlapping lesion of colon
//C18.9	Colon, NOS
//ICD-O-2/3	Term
//Rectosigmoid junction
//C19.9	Rectosigmoid junction
//ICD-O-2/3	Term
//Rectum
//C20.9	Rectum, NOS
//ICD-O-2/3	Term
//Anus and Anal canal
//C21.0	Anus, NOS (excludes Skin of anus and Perianal skin C44.5)
//C21.1	Anal canal
//C21.2	Cloacogenic zone
//C21.8	Overlapping lesion of rectum, anus and anal canal


   static private Collection<String> ALL_COLON_URIS;
   static private Collection<String> CECUM_URIS;
   static private Collection<String> APPENDIX_URIS;
   static private final String ASCENDING_URI = "Right_Colon";
   static private final String HEPATIC_URI = "Hepatic_Flexure";
   static private final Collection<String> TRANSVERSE_URIS
         = Arrays.asList( "Transverse_Colon", "Transverse_Mesocolon" );
   static private final String SPLENIC_URI = "Splenic_Flexure";
   static private final Collection<String> DESCENDING_URIS
         = Arrays.asList( "Left_Colon", "Descending_Mesocolon" );
   static private Collection<String> SIGMOID_URIS;
   static private Collection<String> COLON_URIS;


   // C19.9
   static private final String RECTOSIGMOID_URI = "Rectosigmoid_Region";


   static private final Collection<String> ANORECTAL_URIS
         = Arrays.asList( "Anorectal_Junction", "Anorectal_junction" );

   static private Collection<String> ALL_ANUS_URIS;
   static private Collection<String> ANAL_CANAL_URIS;
   static private Collection<String> ANUS_URIS;
   static private final String ANORECTAL_URI = "Anorectal";

   static private void initColonUris() {
      if ( ALL_COLON_URIS != null ) {
         return;
      }
      CECUM_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Cecum" );
      APPENDIX_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Appendix" );
      COLON_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Colon" );
      COLON_URIS.remove( RECTOSIGMOID_URI );

      ALL_COLON_URIS = new HashSet<>();
      ALL_COLON_URIS.addAll( CECUM_URIS );
      ALL_COLON_URIS.addAll( APPENDIX_URIS );
      ALL_COLON_URIS.addAll( COLON_URIS );
      ALL_COLON_URIS.addAll( ANORECTAL_URIS );
      // Need laterality for ascending/descending determination,
      // but cannot use icdo laterality code as for C18 the code is always 9 (no laterality)
//      ALL_COLON_URIS.add( "Right" );
//      ALL_COLON_URIS.add( "Left" );
      COLON_URIS.remove( ASCENDING_URI );
      COLON_URIS.remove( HEPATIC_URI );
      COLON_URIS.removeAll( TRANSVERSE_URIS );
      COLON_URIS.removeAll( DESCENDING_URIS );
      COLON_URIS.remove( SPLENIC_URI );
      SIGMOID_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Sigmoid_Colon" );
      SIGMOID_URIS.remove( RECTOSIGMOID_URI );
      COLON_URIS.removeAll( SIGMOID_URIS );
   }

   static private void initAnusUris() {
//      RECTAL_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Rectal" );
//      // Rectosigmoid is C19.9, so we don't want it here.
//      RECTAL_URIS.remove( "Rectosigmoid_Colon" );
//      RECTAL_URIS.remove( RECTOSIGMOID_URI );
//      RECTAL_URIS.removeAll( ANORECTAL_URIS );
      ANUS_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Anus" );
      ANUS_URIS.removeAll( ANORECTAL_URIS );

      ALL_ANUS_URIS = new HashSet<>();
//      ALL_ANUS_URIS.addAll( RECTAL_URIS );
      ALL_ANUS_URIS.addAll( ANUS_URIS );

      ANAL_CANAL_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Anal_Canal" );
      ANUS_URIS.removeAll( ANAL_CANAL_URIS );
   }


   static public Collection<ConceptAggregate> getColonParts( final Collection<ConceptAggregate> neoplasms ) {
      initColonUris();
      return neoplasms.stream()
                      .map( ConceptAggregate::getRelatedSites )
                      .flatMap( Collection::stream )
                      .filter( c -> ALL_COLON_URIS.contains( c.getUri() ) )
                      .collect( Collectors.toSet() );
   }

   static public Collection<ConceptAggregate> getAnusParts( final Collection<ConceptAggregate> neoplasms ) {
      initAnusUris();
      return neoplasms.stream()
                      .map( ConceptAggregate::getRelatedSites )
                      .flatMap( Collection::stream )
                      .filter( c -> ALL_ANUS_URIS.contains( c.getUri() ) )
                      .collect( Collectors.toSet() );
   }


   static String getBestColon( final Map<String,Integer> uriStrengths ) {
      if ( uriStrengths.isEmpty() ) {
         return "9";
      }
      final Map<Integer, List<String>> hitCounts = TopoMinorCodeInfoStore.getHitCounts( uriStrengths );
      // Start first with most "specific" parts of the branches and work up to general colon parts
      final int bestMinorCode = hitCounts.keySet()
                                         .stream()
                                         .sorted( Comparator.comparingInt( Integer::intValue )
                                                            .reversed() )
                                         .map( hitCounts::get )
                                         .map( CrcMinorCodifier::getBestColonNumber )
                                         .filter( n -> n >= 0 )
                                         .findFirst()
                                         .orElse( -1 );
      if ( bestMinorCode >= 0 ) {
         return "" + bestMinorCode;
      }
      final int otherMinorCode = hitCounts.keySet()
                                          .stream()
                                          .sorted( Comparator.comparingInt( Integer::intValue )
                                                             .reversed() )
                                          .map( hitCounts::get )
                                          .map( CrcMinorCodifier::getOtherColonNumber )
                                          .filter( n -> n >= 0 )
                                          .findFirst()
                                          .orElse( -1 );
      if ( otherMinorCode >= 0 ) {
         return "" + otherMinorCode;
      }
      return "" + hitCounts.keySet()
                           .stream()
                           .sorted( Comparator.comparingInt( Integer::intValue )
                                              .reversed() )
                           .map( hitCounts::get )
                           .map( CrcMinorCodifier::getFinalColonNumber )
                           .filter( n -> n >= 0 )
                           .findFirst()
                           .orElse( 9 );
   }


   static int getBestColonNumber( final Collection<String> uris ) {
      return uris.stream()
                 .mapToInt( CrcMinorCodifier::getUriColonNumber )
                 .max()
                 .orElse( -1 );
   }

   static private int getOtherColonNumber(  final Collection<String> uris ) {
      return uris.stream()
                 .mapToInt( CrcMinorCodifier::getOtherUriColonNumber )
                 .max()
                 .orElse( -1 );
   }

   static private int getFinalColonNumber(  final Collection<String> uris ) {
      return uris.stream()
                 .mapToInt( CrcMinorCodifier::getFinalUriColonNumber )
                 .max()
                 .orElse( -1 );
   }

   static private int getUriColonNumber( final String uri ) {
      if ( uri.equals( HEPATIC_URI ) ) {
         return 3;
      } else if ( TRANSVERSE_URIS.contains( uri ) ) {
         return 4;
      } else if ( SPLENIC_URI.equals( uri ) ) {
         return 5;
      } else if ( SIGMOID_URIS.contains( uri ) ) {
         return 7;
      }
      return -1;
   }

   static private int getOtherUriColonNumber( final String uri ) {
      if ( uri.equals( ASCENDING_URI ) ) {
         return 2;
      } else if ( DESCENDING_URIS.contains( uri ) ) {
         return 6;
      }
      return -1;
   }

   static private int getFinalUriColonNumber( final String uri ) {
      if ( CECUM_URIS.contains( uri ) ) {
         return 0;
      } else if ( APPENDIX_URIS.contains( uri ) ) {
         return 1;
      }
      return -1;
   }


   static String getBestAnus( final Map<String,Integer> uriStrengths ) {
      if ( uriStrengths.isEmpty() ) {
         return "0";
      }
      if ( uriStrengths.keySet().stream().anyMatch( ANAL_CANAL_URIS::contains ) ) {
         // Anal canal is the only thing that we can test at this point.  Otherwise, return anus, NOS.
         return "1";
      }
      if ( uriStrengths.containsKey( ANORECTAL_URI ) ) {
         return "8";
      }
      return "0";
   }


//   public static void main( String[] args ) {
//      System.out.println( String.join( ",", Neo4jOntologyConceptUtil.getBranchUris( "Cecum" ) ) );
//   }

}
