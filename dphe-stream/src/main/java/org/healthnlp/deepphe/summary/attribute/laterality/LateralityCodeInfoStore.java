package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;

import java.util.*;

final public class LateralityCodeInfoStore implements CodeInfoStore {

   public String _bestCode;

   //  https://seer.cancer.gov/manuals/primsite.laterality.pdf
   static private final Collection<String> LATERALITIES = new HashSet<>();
   static private final int[] FACILITY0 = new int[]{ 7, 8, 9 };
   static private final int[] FACILITY = new int[]{
         30, 31, 34, 38, 40, 41, 44, 47, 49, 50, 56, 57, 62, 63, 64, 65, 66, 69, 70, 71, 72, 74, 75
   };
   static {
      Arrays.stream( FACILITY0 ).forEach( n -> LATERALITIES.add( "C0" + n ) );
      Arrays.stream( FACILITY ).forEach( n -> LATERALITIES.add( "C" + n ) );
   }

   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
      String topographyMajor = dependencies.getOrDefault( "topography_major", "" )
                                           .toUpperCase();
      if ( topographyMajor.length() > 3 ) {
         topographyMajor = topographyMajor.substring( 0, 3 );
      }
      final boolean hasLaterality = LATERALITIES.contains( topographyMajor );
      if ( !hasLaterality ) {
         _bestCode = "0";
      } else {
         _bestCode = getBestLateralityCode( uriInfoStore._uriStrengths );
      }
   }

   public String getBestCode() {
      return _bestCode;
   }

   static private String getBestLateralityCode( final Map<String,Integer> uriStrengths ) {
      if ( uriStrengths.isEmpty() ) {
         return "0";
      }
      final Map<Integer,List<String>> hitCounts = new HashMap<>();
      uriStrengths.forEach( (k,v) -> hitCounts.computeIfAbsent( v, l -> new ArrayList<>() )
                                              .add( k ) );
      return hitCounts.keySet()
               .stream()
               .sorted( Comparator.comparingInt( Integer::intValue )
                                  .reversed() )
               .map( hitCounts::get )
               .map( LateralityCodeInfoStore::getBestLateralityCode )
               .filter( n -> !n.isEmpty() )
               .findFirst()
                      .orElse( "0" );
   }

   static public String getBestLateralityCode( final Collection<String> uris ) {
      if ( uris.isEmpty() ) {
         return "0";
      }
      if ( uris.contains( UriConstants.BILATERAL ) ) {
         return "4";
      }
      if ( uris.contains( UriConstants.RIGHT ) ) {
         if ( uris.contains( UriConstants.LEFT ) ) {
            return "4";
         }
         return "1";
      }
      if ( uris.contains( UriConstants.LEFT ) ) {
         return "2";
      }
      for ( String uri : uris ) {
         if ( uri.contains( "Left" ) ) {
            return "2";
         } else if ( uri.contains( "Right" ) ) {
            return "1";
         }
      }
      // What else could it be?
      return "0";
   }



}
