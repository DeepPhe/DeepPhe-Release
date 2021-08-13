//package org.healthnlp.deepphe.summary.attribute.morphology;
//
//import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
//import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
//import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//
//import java.util.*;
//
//import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addLargeIntFeatures;
//
//public class MorphologyInfoStore extends AttributeInfoStore<MorphUriInfoVisitor,MorphCodeInfoStore> {
//
//   // Morphologies
//   final public AllMorphCodeInfoStore _allMorphStore;
//   final public MainMorphCodeInfoStore _mainMorphStore;
//
//   public MorphologyInfoStore( final ConceptAggregate neoplasm,
//                               final Collection<String> validTopoMorphs ) {
//      this( Collections.singletonList( neoplasm ), validTopoMorphs );
//   }
//
//   public MorphologyInfoStore( final Collection<ConceptAggregate> neoplasms,
//                        final Collection<String> validTopoMorphs ) {
//      super( neoplasms, MorphUriInfoVisitor::new, MorphCodeInfoStore::new, Collections.emptyMap() );
//      _allMorphStore = new AllMorphCodeInfoStore( neoplasms, _allUriStore, validTopoMorphs );
//      _mainMorphStore = new MainMorphCodeInfoStore( neoplasms, _mainUriStore, _allMorphStore,
//                                                               validTopoMorphs );
//   }
//
//
//   @Override
//   public void initCodeInfoStore( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
//   }
//
//   public String getBestCode() {
//      return _mainMorphStore.getBestCode();
//   }
//
//   protected void addMorphFeatures( final List<Integer> features ) {
//      _mainMorphStore.addMorphFeatures( features );
//      _allMorphStore.addMorphFeatures( features );
//      _mainMorphStore.addMorphRatioFeatures( features, _allMorphStore );
//   }
//
//   protected void addMorphRatioFeatures( final List<Integer> features,
//                                         final MorphologyInfoStore fullMorphStore2 ) {
//      _mainMorphStore.addMorphRatioFeatures( features, fullMorphStore2._mainMorphStore );
//      _allMorphStore.addMorphRatioFeatures( features, fullMorphStore2._allMorphStore );
//   }
//
//   protected void addMorphStrengthFeatures( final List<Integer> features ) {
//      addMorphStrengthFeatures( features, _mainUriStore, _mainMorphStore );
//      addMorphStrengthFeatures( features, _allUriStore, _allMorphStore );
//   }
//
//   static private void addMorphStrengthFeatures( final List<Integer> features,
//                                                 final UriInfoStore uriInfoStore,
//                                                 final MorphCodeInfoStore morphCodeInfoStore ) {
//      final Map<String, Integer> ontoMorphStrengths
//            = createMorphStrengthMap( morphCodeInfoStore._uriOntoMorphCodes, uriInfoStore._uriStrengths );
//      final int maxOntoMorphStrength = ontoMorphStrengths.isEmpty()
//                                       ? 0
//                                       :
//                                       Collections.max( ontoMorphStrengths.values() );
//      final Map<String, Integer> broadMorphStrengths
//            = createMorphStrengthMap( morphCodeInfoStore._uriBroadMorphCodes, uriInfoStore._uriStrengths );
//      final int maxBroadMorphStrength = broadMorphStrengths.isEmpty()
//                                        ? 0
//                                        :
//                                        Collections.max( broadMorphStrengths.values() );
//      final Map<String, Integer> exactMorphStrengths
//            = createMorphStrengthMap( morphCodeInfoStore._uriExactMorphCodes, uriInfoStore._uriStrengths );
//      final int maxExactMorphStrength = exactMorphStrengths.isEmpty()
//                                        ? 0
//                                        :
//                                        Collections.max( exactMorphStrengths.values() );
//      addLargeIntFeatures( features,
//                           maxOntoMorphStrength,
//                           maxBroadMorphStrength,
//                           maxExactMorphStrength );
//   }
//
//   static private Map<String, Integer> createMorphStrengthMap( final Map<String, List<String>> uriMorphCodesMap,
//                                                               final Map<String, Integer> uriStrengthsMap ) {
//      final Map<String, Integer> morphStrengths = new HashMap<>();
//      for ( Map.Entry<String, List<String>> uriMorphCodes : uriMorphCodesMap.entrySet() ) {
//         final int strength = uriStrengthsMap.getOrDefault( uriMorphCodes.getKey(), 0 );
//         for ( String morph : uriMorphCodes.getValue() ) {
//            morphStrengths.compute( morph, ( k, v ) -> ( v == null )
//                                                       ? strength
//                                                       : Math.min( strength, v ) );
//         }
//      }
//      return morphStrengths;
//   }
//
//}
