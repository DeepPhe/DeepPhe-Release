package org.healthnlp.deepphe.summary.attribute.behavior;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;


final public class BehaviorCodeInfoStore implements CodeInfoStore {

   public String _bestCode;


   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
      _bestCode = getBestBehaviorCode( uriInfoStore._uriStrengths );
   }

   public String getBestCode() {
      return _bestCode;
   }


   static private String getBestBehaviorCode( final Map<String,Integer> uriStrengths ) {
      if ( uriStrengths.isEmpty() ) {
         return "3";
      }
      final Map<Integer, List<String>> hitCounts = new HashMap<>();
      for ( Map.Entry<String,Integer> uriStrength : uriStrengths.entrySet() ) {
         hitCounts.computeIfAbsent( uriStrength.getValue(), u -> new ArrayList<>() )
                  .add( uriStrength.getKey() );
      }
      return "" + hitCounts.keySet()
                      .stream()
                      .sorted( Comparator.comparingInt( Integer::intValue )
                                         .reversed() )
                      .map( hitCounts::get )
                      .map( BehaviorCodeInfoStore::getBestBehaviorNumber )
                      .filter( n -> n >= 0 )
                      .findFirst()
                      .orElse( 3 );
   }

   static private int getBestBehaviorNumber( final Collection<String> uris ) {
      return uris.stream()
          .mapToInt( BehaviorCodeInfoStore::getUriBehaviorNumber )
          .max()
          .orElse( 3 );
   }

   static public int getBehaviorNumber( final ConceptAggregate behavior ) {
      return getBestBehaviorNumber( behavior.getAllUris() );
//      return behavior.getAllUris()
//                  .stream()
//                  .mapToInt( BehaviorCodeInfoStore::getUriBehaviorNumber )
//                  .max()
//                  .orElse( 3 );
   }

   static public int getUriBehaviorNumber( final String uri ) {
//      if ( uri.startsWith( "Malignant" ) ) {
//         return 3;
//      }
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> metastasisUris = UriConstants.getMetastasisUris( graphDb );
      if ( metastasisUris.contains( uri ) ) {
         return 6;
      }
      final Collection<String> malignantUris = UriConstants.getMalignantTumorUris( graphDb );
      if ( malignantUris.contains( uri ) ) {
         return 3;
      }
      switch ( uri ) {
         case "Borderline" :
         case "Microinvasive_Tumor" :
            return 1;
         case "In_Situ" :
         case "Premalignant" :
         case "Non_Malignant" :
         case "Noninvasive" :
            return 2;
         case "Invasive" : return 3;
         case "Benign" : return 0;
         case "Metaplastic" :
         case "Metastasis" :
         case "Metastatatic" :
            return 6;
         case "Carcinoma" :
         case "Adenocarcinoma" :
            return 3;
      }
      return -1;
   }


}
