package org.healthnlp.deepphe.summary.attribute.histology;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.TopoMorphValidator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class HistologyCodeInfoStore implements CodeInfoStore {

   public String _bestCode;

   protected Map<String, List<String>> _uriOntoMorphCodes;
   protected Map<String, List<String>> _uriBroadMorphCodes;
   protected Map<String, List<String>> _uriExactMorphCodes;
   protected int _ontoMorphCodesSum;
   protected int _broadMorphCodesSum;
   protected int _exactMorphCodesSum;
   protected List<String> _sortedMorphCodes;
   protected String _bestMorphCode;


   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
      _uriOntoMorphCodes = getUrisOntoMorphCodesMap( uriInfoStore._uris );
      _uriBroadMorphCodes = getUrisBroadMorphCodesMap( uriInfoStore._uris );
      _uriExactMorphCodes = getUrisExactMorphCodesMap( uriInfoStore._uris );

      _ontoMorphCodesSum = _uriOntoMorphCodes.values()
                                             .stream()
                                             .flatMap( Collection::stream )
                                             .collect( Collectors.toSet() )
                                             .size();
      _broadMorphCodesSum = _uriBroadMorphCodes.values()
                                               .stream()
                                               .flatMap( Collection::stream )
                                               .collect( Collectors.toSet() )
                                               .size();
      _exactMorphCodesSum = _uriExactMorphCodes.values()
                                               .stream()
                                               .flatMap( Collection::stream )
                                               .collect( Collectors.toSet() )
                                               .size();
      _sortedMorphCodes = getSortedMorphCodes( this );
      _bestMorphCode = getBestMorphCode( _uriOntoMorphCodes,
                                         _uriBroadMorphCodes,
                                         _uriExactMorphCodes,
                                         uriInfoStore._uriStrengths );
      _bestCode = _bestMorphCode.isEmpty()
                       ? getBestHistology( _sortedMorphCodes )
                       : _bestMorphCode.substring( 0, 4 );
   }

   public String getBestCode() {
      return _bestCode;
   }


   static protected List<String> getOntoMorphCodes( final String uri ) {
      return Neo4jOntologyConceptUtil.getIcdoCodes( uri ).stream()
                                     .filter( i -> !i.startsWith( "C" ) )
                                     .filter( i -> !i.contains( "-" ) )
                                     .filter( i -> i.length() > 3 )
                                     .distinct()
                                     .sorted()
                                     .collect( Collectors.toList() );
   }

   static private Map<String, List<String>> getUrisOntoMorphCodesMap( final Collection<String> uris ) {
      final Map<String,List<String>> uriOntoMorphCodesMap = new HashMap<>( uris.size() );
      for ( String uri : uris ) {
         final List<String> codes = getOntoMorphCodes( uri )
               .stream()
               .filter( m -> !m.startsWith( "800" ) )
               .filter( m -> !m.isEmpty() )
               .distinct()
               .sorted()
               .collect( Collectors.toList() );
         NeoplasmSummaryCreator.DEBUG_SB.append( "Onto URI: " )
                                        .append( uri )
                                        .append( " " )
                                        .append( String.join( " ",codes ) )
                                        .append( "\n" );
         uriOntoMorphCodesMap.put( uri, codes );
      }
      return uriOntoMorphCodesMap;
   }

//   static private Map<String, List<String>> getUrisBroadMorphCodesMap( final Collection<String> uris ) {
//      final Map<String,List<String>> uriBroadMorphCodesMap = new HashMap<>( uris.size() );
//      for ( String uri : uris ) {
//         final List<String> codes = TopoMorphValidator.getInstance().getBroadMorphCode( uri )
//                                                      .stream()
//                                                      .filter( m -> !m.startsWith( "800" ) )
//                                                      .filter( m -> !m.isEmpty() )
//                                                      .distinct()
//                                                      .sorted()
//                                                      .collect( Collectors.toList() );
//         NeoplasmSummaryCreator.DEBUG_SB.append( "Broad URI: " )
//                                        .append( uri )
//                                        .append( " " )
//                                        .append( String.join( " ",codes ) )
//                                        .append( "\n" );
//         uriBroadMorphCodesMap.put( uri, codes );
//      }
//      return uriBroadMorphCodesMap;
//   }
static private Map<String, List<String>> getUrisBroadMorphCodesMap( final Collection<String> uris ) {
   final Map<String,List<String>> uriBroadMorphCodesMap = new HashMap<>( uris.size() );
   for ( String uri : uris ) {
      final List<String> codes = TopoMorphValidator.getInstance().getBroadHistoCode( uri )
                                                   .stream()
                                                   .filter( m -> !m.startsWith( "800" ) )
                                                   .filter( m -> !m.isEmpty() )
                                                   .distinct()
                                                   .sorted()
                                                   .map( c -> c + "/3" )
                                                   .collect( Collectors.toList() );
      NeoplasmSummaryCreator.DEBUG_SB.append( "Broad URI: " )
                                     .append( uri )
                                     .append( " " )
                                     .append( String.join( " ",codes ) )
                                     .append( "\n" );
      uriBroadMorphCodesMap.put( uri, codes );
   }
   return uriBroadMorphCodesMap;
}

   static private Map<String, List<String>> getUrisExactMorphCodesMap( final Collection<String> uris ) {
      final Map<String,List<String>> uriExactMorphCodesMap = new HashMap<>( uris.size() );
      for ( String uri : uris ) {
         final String exactCode = TopoMorphValidator.getInstance().getExactMorphCode( uri );
         if ( !exactCode.isEmpty() && !exactCode.startsWith( "800" ) ) {
            uriExactMorphCodesMap.put( uri, Collections.singletonList( exactCode ) );
         }
         NeoplasmSummaryCreator.DEBUG_SB.append( "Exact URI: " )
                                        .append( uri )
                                        .append( " " )
                                        .append( exactCode )
                                        .append( "\n" );
      }
      return uriExactMorphCodesMap;
   }


   static private List<String> getSortedMorphCodes( final HistologyCodeInfoStore morphCodeInfoStore ) {
      final Collection<String> allOntoCodes =
            morphCodeInfoStore._uriOntoMorphCodes.values()
                                                 .stream()
                                                 .flatMap( Collection::stream )
                                                 .collect( Collectors.toSet() );
      final Collection<String> allExactCodes =
            morphCodeInfoStore._uriExactMorphCodes.values()
                                                  .stream()
                                                  .flatMap( Collection::stream )
                                                  .collect( Collectors.toSet() );
      final Collection<String> allBroadCodes =
            morphCodeInfoStore._uriBroadMorphCodes.values()
                                                  .stream()
                                                  .flatMap( Collection::stream )
                                                  .collect( Collectors.toSet() );
      final Collection<String> allCodes = new HashSet<>( allOntoCodes );
      allCodes.addAll( allExactCodes );
      allCodes.addAll( allBroadCodes );
      trimMorphCodes( allCodes );
      final List<String> codes = new ArrayList<>( allCodes );
      codes.sort( HISTO_COMPARATOR );
      return codes;
   }

   static private String getBestMorphCode( final Map<String,List<String>> uriOntoMorphCodes,
                                           final Map<String,List<String>> uriBroadMorphCodes,
                                           final Map<String,List<String>> uriExactMorphCodes,
                                           final Map<String,Integer> uriStrengths ) {
      final Map<Integer,Collection<String>> hitCounts = getHitCounts( uriOntoMorphCodes,
                                                                      uriBroadMorphCodes,
                                                                      uriExactMorphCodes,
                                                                      uriStrengths );
      if ( hitCounts.isEmpty() ) {
         return "";
      }
      final List<Integer> counts = new ArrayList<>( hitCounts.keySet() );
      Collections.sort( counts );
      return hitCounts.get( counts.get( counts.size()-1 ) )
                      .stream()
                      .max( HISTO_COMPARATOR )
                      .orElse( "" );
   }


   static private Map<Integer,Collection<String>> getHitCounts( final Map<String,List<String>> uriOntoMorphCodes,
                                                                final Map<String,List<String>> uriBroadMorphCodes,
                                                                final Map<String,List<String>> uriExactMorphCodes,
                                                                final Map<String,Integer> uriStrengths ) {
      final Collection<String> allUris = new HashSet<>( uriStrengths.keySet() );
      final Map<String,Integer> ontoStrengths = getUrisOntoMorphCodeStrengthMap( allUris, uriOntoMorphCodes,
                                                                                 uriStrengths );
      final Map<String,Integer> exactStrengths = getUrisExactMorphCodeStrengthMap( allUris, uriExactMorphCodes,
                                                                                   uriStrengths );
      final Map<String,Integer> broadStrengths = getUrisBroadMorphCodeStrengthMap( allUris, uriBroadMorphCodes,
                                                                                   uriStrengths );
      final Collection<String> allCodes = new HashSet<>( ontoStrengths.keySet() );
      allCodes.addAll( exactStrengths.keySet() );
      allCodes.addAll( broadStrengths.keySet() );
      trimMorphCodes( allCodes );
      final Map<String,Integer> multiCodesMap = getMultiHistoCodeMap( allCodes );

      final Map<Integer,Collection<String>> hitCounts = new HashMap<>();
      for ( String code : allCodes ) {
         int count = 0;
         count += ontoStrengths.getOrDefault( code, 0 );
         count += exactStrengths.getOrDefault( code, 0 );
         count += broadStrengths.getOrDefault( code, 0 );
//         final int broadCount = broadStrengths.getOrDefault( code, 0 );
//         if ( count > 0 ) {
//            // Try to prevent a less 'strong' uri code from overpowering a stronger uri code just because it appears
//            // in broad
//            count += broadCount / 2;
//         } else {
//            count += broadCount;
//         }
         count += multiCodesMap.getOrDefault( code, 0 );
         if ( code.startsWith( "801" ) ) {
            count = 1;
         }
         hitCounts.computeIfAbsent( count, c -> new HashSet<>() ).add( code );
      }

      ontoStrengths.keySet().retainAll( allCodes );
      exactStrengths.keySet().retainAll( allCodes );
      broadStrengths.keySet().retainAll( allCodes );
      NeoplasmSummaryCreator.DEBUG_SB.append( "\n    Hit Counts: " )
                                     .append( hitCounts.entrySet().stream()
                                                     .map( e -> e.getKey() +" " + e.getValue() )
                                                     .collect(  Collectors.joining(",") ) )
                                     .append( "\n" );
      return hitCounts;
   }

   static private Map<String,Integer> getUrisOntoMorphCodeStrengthMap( final Collection<String> uris,
                                                                       final Map<String,List<String>> uriOntoMorphCodes,
                                                                       final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
//         final Collection<String> codes = getOntoMorphCodes( uri );
         final Collection<String> codes = uriOntoMorphCodes.getOrDefault( uri, Collections.emptyList() );
         NeoplasmSummaryCreator.DEBUG_SB.append( codes.isEmpty() ? "" : ("  Onto " + uri + " " + codes + "\n") );
         for ( String code : codes ) {
//            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
//            ontoMorphStrengths.put( code, previousStrength + strength );
            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
         }
      }
      return ontoMorphStrengths;
   }

   static private Map<String,Integer> getUrisExactMorphCodeStrengthMap( final Collection<String> uris,
                                                                        final Map<String,List<String>> uriExactMorphCodes,
                                                                        final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
//         final String code = TopoMorphValidator.getInstance().getExactMorphCode( uri );
         final List<String> codes = uriExactMorphCodes.getOrDefault( uri, Collections.emptyList() );
         NeoplasmSummaryCreator.DEBUG_SB.append( codes.isEmpty() ? "" : ("  Exact " + uri + " " + codes + "\n") );
         for ( String code : codes ) {
//            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
//            ontoMorphStrengths.put( code, previousStrength + strength );
            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
         }
      }
      return ontoMorphStrengths;
   }

   static private Map<String,Integer> getUrisBroadMorphCodeStrengthMap( final Collection<String> uris,
                                                                        final Map<String,List<String>> uriBroadMorphCodes,
                                                                        final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
//         final Collection<String> codes = TopoMorphValidator.getInstance().getBroadMorphCode( uri );
         final Collection<String> codes = uriBroadMorphCodes.getOrDefault( uri, Collections.emptyList() );
         NeoplasmSummaryCreator.DEBUG_SB.append( codes.isEmpty() ? "" : ("  Broad " + uri + " " + codes + "\n") );
         for ( String code : codes ) {
//            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
//            ontoMorphStrengths.put( code, previousStrength + strength );
            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
         }
      }
      return ontoMorphStrengths;
   }

   static private Map<String,Integer> getMultiHistoCodeMap( final Collection<String> codes ) {
      final Map<String,Collection<String>> histoMorphMap = new HashMap<>();
      for ( String code : codes ) {
         histoMorphMap.computeIfAbsent( code.substring( 0,4 ), t -> new ArrayList<>() ).add( code );
      }
      return codes.stream()
                  .collect( Collectors.toMap( Function.identity(),
                                              c -> histoMorphMap.get( c.substring( 0,4 ) ).size() ) );
   }

   static private void trimMorphCodes( final Collection<String> morphs  ) {
      morphs.remove( "" );
      final Collection<String> removals = morphs.stream()
                                                .filter( m -> m.startsWith( "800" ) )
                                                .collect( Collectors.toSet() );
      morphs.removeAll( removals );
   }


   static private final Function<String, String> getHisto
         = m -> {
      final int i = m.indexOf( "/" );
      return i > 0 ? m.substring( 0, i ) : m;
   };
   static private final Function<String, String> getBehave
         = m -> {
      final int i = m.indexOf( "/" );
      return i > 0 ? m.substring( i + 1 ) : "";
   };

   static public String getBestHistology( final Collection<String> morphs ) {
//      LOGGER.info( "Getting Best Histology from Morphology codes " + String.join( ",", morphs ) );
      final HistoComparator comparator = new HistoComparator();

//      LOGGER.info( "The preferred histology is the first of the following OR the first in numerically sorted order:" );
//      LOGGER.info( "8071 8070 8520 8575 8500 8503 8260 8250 8140 8480 8046 8041 8240 8012 8000 8010" );
//      LOGGER.info( "This ordering came from the best overall fit to gold annotations." );

      return morphs.stream()
                   .map( getHisto )
                   .filter( h -> !h.isEmpty() )
//                   .max( String.CASE_INSENSITIVE_ORDER )
                   .max( comparator )
                   .orElse( "8000" );
   }





   static private final HistoComparator HISTO_COMPARATOR = new HistoComparator();

   // TODO - use?
   static private final class HistoComparator implements Comparator<String> {
      public int compare( final String histo1, final String histo2 ) {
         final List<String> HISTO_ORDER
//               = Arrays.asList( "8070", "8520", "8503", "8500", "8260", "8250", "8140", "8480", "8046", "8000", "8010" );
//               = Arrays.asList( "8071", "8070", "8520", "8575", "8500", "8503", "8260", "8250", "8140", "8480",
//                                "8046", "8041", "8240", "8012", "8000", "8010" );
//               = Arrays.asList( "804", "848", "814", "824", "825", "826", "850", "875", "852", "807" );
               = Arrays.asList( "807", "814", "804", "848", "824", "825", "826", "850", "875", "852" );
         if ( histo1.equals( histo2 ) ) {
            return 0;
         }
         final String sub1 = histo1.substring( 0, 3 );
         final String sub2 = histo2.substring( 0, 3 );
         if ( sub1.equals( "801" ) ) {
            return -1;
         } else if ( sub2.equals( "801" ) ) {
            return 1;
         }
         if ( !sub1.equals( sub2 ) ) {
            for ( String order : HISTO_ORDER ) {
               if ( sub1.equals( order ) ) {
                  return 1;
               } else if ( sub2.equals( order ) ) {
                  return -1;
               }
            }
         }
         return String.CASE_INSENSITIVE_ORDER.compare( histo1, histo2 );
      }
   }


}
