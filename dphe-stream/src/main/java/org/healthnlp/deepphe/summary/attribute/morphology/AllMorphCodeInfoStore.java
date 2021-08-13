//package org.healthnlp.deepphe.summary.attribute.morphology;
//
//import org.healthnlp.deepphe.summary.attribute.infostore.AllUriInfoStore;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.healthnlp.deepphe.util.TopoMorphValidator;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//class AllMorphCodeInfoStore extends MorphCodeInfoStore {
//
//   protected AllMorphCodeInfoStore( final Collection<ConceptAggregate> neoplasms,
//                                    final AllUriInfoStore uriInfoStore,
//                                    final Collection<String> validTopoMorphs ) {
//      _uriOntoMorphCodes = getUrisOntoMorphCodesMap( uriInfoStore._uris );
//      _uriBroadMorphCodes = getUrisBroadMorphCodesMap( uriInfoStore._uris );
//      _uriExactMorphCodes = getUrisExactMorphCodesMap( uriInfoStore._uris );
//      init( neoplasms, uriInfoStore, validTopoMorphs );
//   }
//
//   static private Map<String, List<String>> getUrisOntoMorphCodesMap( final Collection<String> uris ) {
//      final Map<String,List<String>> uriOntoMorphCodesMap = new HashMap<>( uris.size() );
//      for ( String uri : uris ) {
//         final List<String> codes = getOntoMorphCodes( uri )
//                                              .stream()
//                                              .filter( m -> !m.startsWith( "800" ) )
//                                              .filter( m -> !m.isEmpty() )
//                                              .distinct()
//                                              .sorted()
//                                              .collect( Collectors.toList() );
//         uriOntoMorphCodesMap.put( uri, codes );
//      }
//      return uriOntoMorphCodesMap;
//   }
//
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
//         uriBroadMorphCodesMap.put( uri, codes );
//      }
//      return uriBroadMorphCodesMap;
//   }
//
//   static private Map<String, List<String>> getUrisExactMorphCodesMap( final Collection<String> uris ) {
//      final Map<String,List<String>> uriExactMorphCodesMap = new HashMap<>( uris.size() );
//      for ( String uri : uris ) {
//         final String exactCode = TopoMorphValidator.getInstance().getExactMorphCode( uri );
//         if ( !exactCode.isEmpty() && !exactCode.startsWith( "800" ) ) {
//            uriExactMorphCodesMap.put( uri, Collections.singletonList( exactCode ) );
//         }
//      }
//      return uriExactMorphCodesMap;
//   }
//
//}
