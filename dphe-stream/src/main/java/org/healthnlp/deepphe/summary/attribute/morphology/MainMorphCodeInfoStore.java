//package org.healthnlp.deepphe.summary.attribute.morphology;
//
//import org.healthnlp.deepphe.summary.attribute.infostore.MainUriInfoStore;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//
//import java.util.Collection;
//import java.util.HashMap;
//
//class MainMorphCodeInfoStore extends MorphCodeInfoStore {
//
//   protected MainMorphCodeInfoStore( final Collection<ConceptAggregate> neoplasms,
//                                     final MainUriInfoStore uriInfoStore,
//                                     final AllMorphCodeInfoStore allMorphInfoStore,
//                                     final Collection<String> validTopoMorphs ) {
//      _uriOntoMorphCodes = new HashMap<>( allMorphInfoStore._uriOntoMorphCodes );
//      _uriOntoMorphCodes.keySet()
//                        .retainAll( uriInfoStore._uris );
//      _uriBroadMorphCodes = new HashMap<>( allMorphInfoStore._uriBroadMorphCodes );
//      _uriBroadMorphCodes.keySet()
//                         .retainAll( uriInfoStore._uris );
//      _uriExactMorphCodes = new HashMap<>( allMorphInfoStore._uriExactMorphCodes );
//      _uriExactMorphCodes.keySet()
//                         .retainAll( uriInfoStore._uris );
//      init( neoplasms, uriInfoStore, validTopoMorphs );
//   }
//
//}
