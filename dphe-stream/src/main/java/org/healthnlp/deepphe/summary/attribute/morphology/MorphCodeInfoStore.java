//package org.healthnlp.deepphe.summary.attribute.morphology;
//
//import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
//import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
//import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;
//import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addLargeIntFeatures;
//
//class MorphCodeInfoStore implements CodeInfoStore {
//
//   // Morphology Codes
//   protected Map<String, List<String>> _uriOntoMorphCodes;
//   protected Map<String, List<String>> _uriBroadMorphCodes;
//   protected Map<String, List<String>> _uriExactMorphCodes;
//   protected int _ontoMorphCodesSum;
//   protected int _broadMorphCodesSum;
//   protected int _exactMorphCodesSum;
//   protected List<String> _sortedMorphCodes;
//   protected String _bestMorphCode;
//   protected String _bestHistoCode;
////   protected String _bestBehaviorCode;
//
//   private Map<String,Integer> _uriOntoStrengths = new HashMap<>();
//   private Map<String,Integer> _uriBroadStrengths = new HashMap<>();
//   private Map<String,Integer> _uriExactStrengths = new HashMap<>();
//   private Map<String,Integer> _uriTotalStrengths = new HashMap<>();
//
//
//   protected void init( final Collection<ConceptAggregate> neoplasms,
//                        final UriInfoStore uriInfoStore,
//                        final Collection<String> validTopoMorphs ) {
//      _ontoMorphCodesSum = _uriOntoMorphCodes.values()
//                                             .stream()
//                                             .flatMap( Collection::stream )
//                                             .collect( Collectors.toSet() )
//                                             .size();
//      _broadMorphCodesSum = _uriBroadMorphCodes.values()
//                                               .stream()
//                                               .flatMap( Collection::stream )
//                                               .collect( Collectors.toSet() )
//                                               .size();
//      _exactMorphCodesSum = _uriExactMorphCodes.values()
//                                               .stream()
//                                               .flatMap( Collection::stream )
//                                               .collect( Collectors.toSet() )
//                                               .size();
//      _sortedMorphCodes = getSortedMorphCodes( this, validTopoMorphs );
////         _bestMorphCode = getBestMorphCode( neoplasms, uriInfoStore._uriStrengths, validTopoMorphs );
//      _bestMorphCode = getBestMorphCode( _uriOntoMorphCodes, _uriBroadMorphCodes, _uriExactMorphCodes,
//                                                    uriInfoStore._uriStrengths, validTopoMorphs );
//      _bestHistoCode = _bestMorphCode.isEmpty()
//                       ? getBestHistology( _sortedMorphCodes )
//                       : _bestMorphCode.substring( 0, 4 );
////      _bestBehaviorCode = getBestBehavior( _sortedMorphCodes );
//   }
//
//   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
//      // do nothing
//   }
//   public String getBestCode() {
//      return _bestMorphCode;
//   }
//
//   protected void addMorphFeatures( final List<Integer> features ) {
//      final int ontoCountTotal = _uriOntoMorphCodes.values()
//                                                   .stream()
//                                                   .mapToInt( Collection::size )
//                                                   .sum();
//      final int broadCountTotal = _uriBroadMorphCodes.values()
//                                                     .stream()
//                                                     .mapToInt( Collection::size )
//                                                     .sum();
//      final int exactCountTotal = _uriExactMorphCodes.values()
//                                                     .stream()
//                                                     .mapToInt( Collection::size )
//                                                     .sum();
//      addBooleanFeatures( features, !_bestMorphCode.isEmpty() );
//      addLargeIntFeatures( features,
//                           _uriOntoMorphCodes.size(),
//                           _uriBroadMorphCodes.size(),
//                           _uriExactMorphCodes.size(),
//                           _ontoMorphCodesSum,
//                           _broadMorphCodesSum,
//                           _exactMorphCodesSum,
//                           ontoCountTotal,
//                           broadCountTotal,
//                           exactCountTotal,
//                           _sortedMorphCodes.size() );
//      addBooleanFeatures( features, !_bestHistoCode.equals( "8000" ) );
//   }
//
//   protected void addMorphRatioFeatures( final List<Integer> features, final MorphCodeInfoStore morphCodeInfoStore2 ) {
//      addBooleanFeatures( features, !_bestMorphCode.isEmpty()
//                                    && _bestMorphCode.equals( morphCodeInfoStore2._bestMorphCode ) );
//      addBooleanFeatures( features, !_bestHistoCode.isEmpty()
//                                    && _bestHistoCode.equals( morphCodeInfoStore2._bestHistoCode ) );
//   }
//
//   static protected List<String> getOntoMorphCodes( final String uri ) {
//      return Neo4jOntologyConceptUtil.getIcdoCodes( uri ).stream()
//                                     .filter( i -> !i.startsWith( "C" ) )
//                                     .filter( i -> !i.contains( "-" ) )
//                                     .filter( i -> i.length() > 3 )
//                                     .distinct()
//                                     .sorted()
//                                     .collect( Collectors.toList() );
//   }
//
//   static private List<String> getSortedMorphCodes( final MorphCodeInfoStore morphCodeInfoStore,
//                                                    final Collection<String> validTopoMorphs ) {
//      final Collection<String> allOntoCodes =
//            morphCodeInfoStore._uriOntoMorphCodes.values()
//                                                 .stream()
//                                                 .flatMap( Collection::stream )
//                                                 .collect( Collectors.toSet() );
//      final Collection<String> allExactCodes =
//            morphCodeInfoStore._uriExactMorphCodes.values()
//                                                  .stream()
//                                                  .flatMap( Collection::stream )
//                                                  .collect( Collectors.toSet() );
//      final Collection<String> allBroadCodes =
//            morphCodeInfoStore._uriBroadMorphCodes.values()
//                                                  .stream()
//                                                  .flatMap( Collection::stream )
//                                                  .collect( Collectors.toSet() );
//      final Collection<String> allCodes = new HashSet<>( allOntoCodes );
//      allCodes.addAll( allExactCodes );
//      allCodes.addAll( allBroadCodes );
//      trimMorphCodes( allCodes, validTopoMorphs );
//      final List<String> codes = new ArrayList<>( allCodes );
//      codes.sort( HISTO_COMPARATOR );
//      return codes;
//   }
//
//   static private String getBestMorphCode( final Map<String,List<String>> uriOntoMorphCodes,
//                                           final Map<String,List<String>> uriBroadMorphCodes,
//                                           final Map<String,List<String>> uriExactMorphCodes,
//                                           final Map<String,Integer> uriStrengths,
//                                           final Collection<String> validTopoMorphs ) {
//      final Map<Integer,Collection<String>> hitCounts = getHitCounts( uriOntoMorphCodes,
//                                                                      uriBroadMorphCodes,
//                                                                      uriExactMorphCodes,
//                                                                      uriStrengths,
//                                                                      validTopoMorphs );
//      if ( hitCounts.isEmpty() ) {
//         return "";
//      }
//      final List<Integer> counts = new ArrayList<>( hitCounts.keySet() );
//      Collections.sort( counts );
//      return hitCounts.get( counts.get( counts.size()-1 ) )
//                      .stream()
//                      .max( HISTO_COMPARATOR )
//                      .orElse( "" );
//   }
//
//
//
//   static private Map<Integer,Collection<String>> getHitCounts( final Map<String,List<String>> uriOntoMorphCodes,
//                                                                final Map<String,List<String>> uriBroadMorphCodes,
//                                                                final Map<String,List<String>> uriExactMorphCodes,
//                                                                final Map<String,Integer> uriStrengths,
//                                                                final Collection<String> validTopoMorphs ) {
//      final Collection<String> allUris = new HashSet<>( uriStrengths.keySet() );
//      final Map<String,Integer> ontoStrengths = getUrisOntoMorphCodeStrengthMap( allUris, uriOntoMorphCodes,
//                                                                                 uriStrengths );
//      final Map<String,Integer> exactStrengths = getUrisExactMorphCodeStrengthMap( allUris, uriExactMorphCodes,
//                                                                                   uriStrengths );
//      final Map<String,Integer> broadStrengths = getUrisBroadMorphCodeStrengthMap( allUris, uriBroadMorphCodes,
//                                                                                   uriStrengths );
//      final Collection<String> allCodes = new HashSet<>( ontoStrengths.keySet() );
//      allCodes.addAll( exactStrengths.keySet() );
//      allCodes.addAll( broadStrengths.keySet() );
//      trimMorphCodes( allCodes, validTopoMorphs );
//      final Map<String,Integer> multiCodesMap = getMultiHistoCodeMap( allCodes );
//
//      final Map<Integer,Collection<String>> hitCounts = new HashMap<>();
//      for ( String code : allCodes ) {
//         int count = 0;
//         count += ontoStrengths.getOrDefault( code, 0 );
//         count += exactStrengths.getOrDefault( code, 0 );
//         count += broadStrengths.getOrDefault( code, 0 );
//         count += multiCodesMap.getOrDefault( code, 0 );
//         if ( code.startsWith( "801" ) ) {
//            count -= 40;
//         }
//         hitCounts.computeIfAbsent( count, c -> new HashSet<>() ).add( code );
//      }
//
////      private Map<String,Integer> _uriBroadStrengths = new HashMap<>();
////      private Map<String,Integer> _uriExactStrengths = new HashMap<>();
////      private Map<String,Integer> _uriTotalStrengths = new HashMap<>();
//
//      HIT_COUNT_TEXT = "";
//      ontoStrengths.keySet().retainAll( allCodes );
//      exactStrengths.keySet().retainAll( allCodes );
//      broadStrengths.keySet().retainAll( allCodes );
//      HIT_COUNT_TEXT += "OntoStrengths: " + ontoStrengths.entrySet().stream()
//                                                         .map( e -> e.getKey() +" " + e.getValue() )
//                                                         .collect(  Collectors.joining(",") ) + "\n";
//      HIT_COUNT_TEXT += "ExctStrengths: " + exactStrengths.entrySet().stream()
//                                                          .map( e -> e.getKey() +" " + e.getValue() )
//                                                          .collect(  Collectors.joining(",") ) + "\n";
//      HIT_COUNT_TEXT += "BrodStrengths: " + broadStrengths.entrySet().stream()
//                                                          .map( e -> e.getKey() +" " + e.getValue() )
//                                                          .collect(  Collectors.joining(",") ) + "\n";
//      HIT_COUNT_TEXT += "MultStrengths: " + multiCodesMap.entrySet().stream()
//                                                         .map( e -> e.getKey() + " " + e.getValue()*10 )
//                                                         .collect(  Collectors.joining(",") ) + "\n";
//      HIT_COUNT_TEXT += "   Hit Counts: " + hitCounts.entrySet().stream()
//                                                     .map( e -> e.getKey() +" " + e.getValue() )
//                                                     .collect(  Collectors.joining(",") ) + "\n";
//      return hitCounts;
//   }
//
//   static public String URI_ONTO_TEXT = "";
//   static public String URI_BROD_TEXT = "";
//   static public String URI_EXCT_TEXT = "";
//   static public String HIT_COUNT_TEXT = "";
//
//   static private Map<String,Integer> getUrisOntoMorphCodeStrengthMap( final Collection<String> uris,
//                                                                       final Map<String,List<String>> uriOntoMorphCodes,
//                                                                       final Map<String,Integer> uriStrengths ) {
//      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
//      URI_ONTO_TEXT = "";
//      for ( String uri : uris ) {
//         final int strength = uriStrengths.get( uri );
////         final Collection<String> codes = getOntoMorphCodes( uri );
//         final Collection<String> codes = uriOntoMorphCodes.getOrDefault( uri, Collections.emptyList() );
//         URI_ONTO_TEXT += codes.isEmpty() ? "" : "Onto " + uri + " " + codes + "\n";
//         for ( String code : codes ) {
////            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
////            ontoMorphStrengths.put( code, previousStrength + strength );
//            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
//         }
//      }
//      return ontoMorphStrengths;
//   }
//
//   static private Map<String,Integer> getUrisExactMorphCodeStrengthMap( final Collection<String> uris,
//                                                                        final Map<String,List<String>> uriExactMorphCodes,
//                                                                        final Map<String,Integer> uriStrengths ) {
//      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
//      URI_EXCT_TEXT = "";
//      for ( String uri : uris ) {
//         final int strength = uriStrengths.get( uri );
////         final String code = TopoMorphValidator.getInstance().getExactMorphCode( uri );
//         final List<String> codes = uriExactMorphCodes.getOrDefault( uri, Collections.emptyList() );
//         URI_BROD_TEXT += codes.isEmpty() ? "" : "Exact " + uri + " " + codes + "\n";
//         for ( String code : codes ) {
////            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
////            ontoMorphStrengths.put( code, previousStrength + strength );
//            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
//         }
//      }
//      return ontoMorphStrengths;
//   }
//
//   static private Map<String,Integer> getUrisBroadMorphCodeStrengthMap( final Collection<String> uris,
//                                                                        final Map<String,List<String>> uriBroadMorphCodes,
//                                                                        final Map<String,Integer> uriStrengths ) {
//      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
//      URI_BROD_TEXT = "";
//      for ( String uri : uris ) {
//         final int strength = uriStrengths.get( uri );
////         final Collection<String> codes = TopoMorphValidator.getInstance().getBroadMorphCode( uri );
//         final Collection<String> codes = uriBroadMorphCodes.getOrDefault( uri, Collections.emptyList() );
//         URI_BROD_TEXT += codes.isEmpty() ? "" : "Broad " + uri + " " + codes + "\n";
//         for ( String code : codes ) {
////            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
////            ontoMorphStrengths.put( code, previousStrength + strength );
//            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
//         }
//      }
//      return ontoMorphStrengths;
//   }
//
//
//   static private void trimMorphCodes( final Collection<String> morphs,
//                                       final Collection<String> validTopoMorphs ) {
//      morphs.remove( "" );
//      final Collection<String> removals = morphs.stream()
//                                                .filter( m -> m.startsWith( "800" ) )
//                                                .collect( Collectors.toSet() );
//      morphs.removeAll( removals );
//   }
//
//
//   static private Map<String,Integer> getMultiHistoCodeMap( final Collection<String> codes ) {
//      final Map<String,Collection<String>> histoMorphMap = new HashMap<>();
//      for ( String code : codes ) {
//         histoMorphMap.computeIfAbsent( code.substring( 0,4 ), t -> new ArrayList<>() ).add( code );
//      }
//      return codes.stream()
//                  .collect( Collectors.toMap( Function.identity(),
//                                              c -> histoMorphMap.get( c.substring( 0,4 ) ).size() ) );
//   }
//
//
//
//
//
//
//   static private final Function<String, String> getHisto
//         = m -> {
//      final int i = m.indexOf( "/" );
//      return i > 0 ? m.substring( 0, i ) : m;
//   };
//   static private final Function<String, String> getBehave
//         = m -> {
//      final int i = m.indexOf( "/" );
//      return i > 0 ? m.substring( i + 1 ) : "";
//   };
//
//   static public String getBestHistology( final Collection<String> morphs ) {
////      LOGGER.info( "Getting Best Histology from Morphology codes " + String.join( ",", morphs ) );
//      final HistoComparator comparator = new HistoComparator();
//
////      LOGGER.info( "The preferred histology is the first of the following OR the first in numerically sorted order:" );
////      LOGGER.info( "8071 8070 8520 8575 8500 8503 8260 8250 8140 8480 8046 8041 8240 8012 8000 8010" );
////      LOGGER.info( "This ordering came from the best overall fit to gold annotations." );
//
//      return morphs.stream()
//                   .map( getHisto )
//                   .filter( h -> !h.isEmpty() )
////                   .max( String.CASE_INSENSITIVE_ORDER )
//                   .max( comparator )
//                   .orElse( "8000" );
//   }
//
//
//   // Should be 3 (instead of 2) : 2
//   static private String getBestBehavior( final Collection<String> morphs ) {
////      LOGGER.info( "Behavior comes from Histology." );
//      final String histo = getBestHistology( morphs );
//      if ( histo.isEmpty() ) {
//         return "";
//      }
//      final List<String> behaves = morphs.stream()
//                                         .filter( m -> m.startsWith( histo ) )
//                                         .map( getBehave )
//                                         .filter( b -> !b.isEmpty() )
//                                         .distinct()
//                                         .sorted()
//                                         .collect( Collectors.toList() );
//      if ( behaves.isEmpty() ) {
//         return "";
//      }
//      if ( behaves.size() == 1 ) {
////         LOGGER.info( "Only one possible behavior." );
//         return behaves.get( 0 );
//      }
//      if ( behaves.size() == 2 && behaves.contains( "2" ) && behaves.contains( "3" ) ) {
////         LOGGER.info( "Only Behaviors 2 and 3, and Behavior of 3 trumps a behavior of 2." );
//         return "3";
//      }
////      LOGGER.info( "Removing Behavior 3 (if present) in favor of other highest value." );
//      behaves.remove( "3" );
//      return behaves.get( behaves.size() - 1 );
//   }
//
//
//   static private final HistoComparator HISTO_COMPARATOR = new HistoComparator();
//
//   // TODO - use?
//   static private final class HistoComparator implements Comparator<String> {
//      public int compare( final String histo1, final String histo2 ) {
//         final List<String> HISTO_ORDER
////               = Arrays.asList( "8070", "8520", "8503", "8500", "8260", "8250", "8140", "8480", "8046", "8000", "8010" );
////               = Arrays.asList( "8071", "8070", "8520", "8575", "8500", "8503", "8260", "8250", "8140", "8480",
////                                "8046", "8041", "8240", "8012", "8000", "8010" );
////               = Arrays.asList( "804", "848", "814", "824", "825", "826", "850", "875", "852", "807" );
//               = Arrays.asList( "807", "814", "804", "848", "824", "825", "826", "850", "875", "852" );
//         if ( histo1.equals( histo2 ) ) {
//            return 0;
//         }
//         final String sub1 = histo1.substring( 0, 3 );
//         final String sub2 = histo2.substring( 0, 3 );
//         if ( !sub1.equals( sub2 ) ) {
//            for ( String order : HISTO_ORDER ) {
//               if ( sub1.equals( order ) ) {
//                  return 1;
//               } else if ( sub2.equals( order ) ) {
//                  return -1;
//               }
//            }
//         }
//         return String.CASE_INSENSITIVE_ORDER.compare( histo1, histo2 );
//      }
//   }
//
//
//}
