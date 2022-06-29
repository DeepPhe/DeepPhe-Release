package org.healthnlp.deepphe.summary.attribute.infostore;



import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addLargeIntFeatures;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addRatioFeature;

public abstract class UriInfoStore {

   // URIs
   public Collection<String> _uris;
   // URI Strengths
   public Map<String, Integer> _uriStrengths;
   public Map<Integer, Collection<String>> _strengthUriMap;
   public List<Integer> _sortedStrengths;
   public int _maxUriStrength;
   public int _2ndUriStrength;
   public String _bestUri;
   public Map<String, Integer> _classLevelMap;
   public int _maxDepth;

   public UriInfoStore( final Collection<String> uris ) {
      NeoplasmSummaryCreator.addDebug( "  URIs:  "
                                     +  String.join( " ", uris )
                                     +  "\n" );
      _uris = uris;
   }

   public void setUriStrengths( final Map<String, Integer> uriStrengths ) {
      NeoplasmSummaryCreator.addDebug( "  URI Strengths:  "
                                     +  uriStrengths.entrySet()
                                                          .stream()
                                                          .map( e -> e.getKey() + "=" + e.getValue() )
                                                          .collect( Collectors.joining( " " ) )
                                     +  "\n" );
      _uriStrengths = uriStrengths;
      _strengthUriMap = getStrengthUriMap( _uriStrengths );
      _sortedStrengths = getSortedStrengths( _strengthUriMap.keySet() );
      _maxUriStrength = getMaxUriStrength( _sortedStrengths );
      _2ndUriStrength = get2ndUriStrength( _sortedStrengths );
      if ( _sortedStrengths.isEmpty() ) {
         _bestUri = "";
      } else {
         final Collection<String> bestUris = _strengthUriMap.get( _maxUriStrength );
         _bestUri = bestUris.stream()
                            .max( Comparator.comparingInt( String::length ) )
                            .orElse( "" );
      }
//      NeoplasmSummaryCreator.addDebug( "   " )+  _bestUri.isEmpty() ? "NONE" : _bestUri )
//                                     +  "   " )
//                                     +  uriStrengths.entrySet()
//                                                          .stream()
//                                                          .map( e -> e.getKey() +  ":" + e.getValue() )
//                                                          .collect(  Collectors.joining(" " ) ) )
//                                     +  "\n" );
   }

   static private Map<Integer,Collection<String>> getStrengthUriMap( final Map<String,Integer> uriStrengths ) {
      final Map<Integer,Collection<String>> strengthUriMap = new HashMap<>();
      for ( Map.Entry<String,Integer> uriStrength : uriStrengths.entrySet() ) {
         strengthUriMap.computeIfAbsent( uriStrength.getValue(), s -> new HashSet<>() ).add( uriStrength.getKey() );
      }
      return strengthUriMap;
   }

   public void setClassLevelMap( final Map<String, Integer> classLevelMap ) {
      _classLevelMap = classLevelMap;
      _maxDepth = _uris.stream()
                       .mapToInt(
                             u -> _classLevelMap.getOrDefault(
                                   u, 0 ) )
                       .max()
                       .orElse( 0 );
   }

   static private List<Integer> getSortedStrengths( final Collection<Integer> strengths ) {
      return strengths.stream()
                      .sorted()
                      .collect( Collectors.toList() );
   }

   static private int getMaxUriStrength( final List<Integer> sortedStrengths ) {
      if ( sortedStrengths.isEmpty() ) {
         return 0;
      }
      return sortedStrengths.get( sortedStrengths.size() - 1 );
   }

   static private int get2ndUriStrength( final List<Integer> sortedStrengths ) {
      if ( sortedStrengths.size() < 2 ) {
         return 0;
      }
      return sortedStrengths.get( sortedStrengths.size() - 2 );
   }

   public void addStrengthFeatures( final List<Integer> features ) {
      addLargeIntFeatures( features, _sortedStrengths.size() );
      // Neoplasm Uri Strengths ; How strong is the weight, quotient of each uri compared to others.
      // All Uris with the maximum strength, as some Uris may be equally strong.
      final Collection<String> urisWithMaxStrength = _strengthUriMap.getOrDefault( _maxUriStrength,
                                                                                   Collections.emptyList() );
      // The number of Uris with the maximum strength.  For single-concept neoplasm is NOT always 1.
      final int urisWithMaxStrengthCount = urisWithMaxStrength.size();
      // 2nd highest strength.  This is NOT the max strength even if max strength has > 1 uris.
      final Collection<String> urisWith2ndStrength = _strengthUriMap.getOrDefault( _2ndUriStrength,
                                                                                   Collections.emptyList() );
      final int urisWith2ndStrengthCount = urisWith2ndStrength.size();
      addLargeIntFeatures( features,
                           _maxUriStrength,
                           urisWithMaxStrengthCount,
                           _2ndUriStrength,
                           urisWith2ndStrengthCount );
      addRatioFeature( features, urisWithMaxStrengthCount, _strengthUriMap.size() );
      addRatioFeature( features, urisWith2ndStrengthCount, _strengthUriMap.size() );
   }



   // For debugging
//   final private ToIntFunction<String> getStrength = u -> _uriStrengths.getOrDefault( u, 0 );
//   public Collection<DebugHelper.UriDebugObject> createUriDebugObjects() {
//      return _uris.stream()
//                  .sorted( Comparator.comparingInt( getStrength ).reversed() )
//                  .map( u ->  new DebugHelper.UriDebugObject( u, _uriStrengths.getOrDefault( u, 0 ) ) )
//                  .collect( Collectors.toList() );
//   }

}
