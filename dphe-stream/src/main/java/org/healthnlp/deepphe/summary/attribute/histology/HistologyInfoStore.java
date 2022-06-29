package org.healthnlp.deepphe.summary.attribute.histology;

import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;

import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addLargeIntFeatures;

/**
 * @author SPF , chip-nlp
 * @since {3/13/2021}
 */
public class HistologyInfoStore extends AttributeInfoStore<HistologyUriInfoVisitor, HistologyCodeInfoStore> {


   public HistologyInfoStore( final ConceptAggregate neoplasm ) {
      this( Collections.singletonList( neoplasm ) );
   }

   public HistologyInfoStore( final Collection<ConceptAggregate> neoplasms ) {
      super( neoplasms, HistologyUriInfoVisitor::new, HistologyCodeInfoStore::new, Collections.emptyMap() );
   }

   public void initCodeInfoStore( final Map<String,String> dependencies ) {
//      NeoplasmSummaryCreator.addDebug( "  Initializing All Uri Store  (Not Standard):\n" );
      initCodeInfoStore( _allUriStore, dependencies );
//      NeoplasmSummaryCreator.addDebug( "  Initializing Main Uri Store  (Standard):\n" );
//      initCodeInfoStore( _mainUriStore, dependencies );
   }

   public void addGeneralFeatures( final List<Integer> features ) {
      super.addGeneralFeatures( features );
      addHistologyFeatures( features );
   }

   protected void addHistologyFeatures( final List<Integer> features ) {
      final int ontoCountTotal = _codeInfoStore._uriOntoMorphCodes.values()
                                                   .stream()
                                                   .mapToInt( Collection::size )
                                                   .sum();
      final int broadCountTotal = _codeInfoStore._uriBroadMorphCodes.values()
                                                     .stream()
                                                     .mapToInt( Collection::size )
                                                     .sum();
      final int exactCountTotal = _codeInfoStore._uriExactMorphCodes.values()
                                                     .stream()
                                                     .mapToInt( Collection::size )
                                                     .sum();
      addBooleanFeatures( features, !_codeInfoStore._bestMorphCode.isEmpty() );
      addLargeIntFeatures( features,
                           _codeInfoStore._uriOntoMorphCodes.size(),
                           _codeInfoStore._uriBroadMorphCodes.size(),
                           _codeInfoStore._uriExactMorphCodes.size(),
                           _codeInfoStore._ontoMorphCodesSum,
                           _codeInfoStore._broadMorphCodesSum,
                           _codeInfoStore._exactMorphCodesSum,
                           ontoCountTotal,
                           broadCountTotal,
                           exactCountTotal,
                           _codeInfoStore._sortedMorphCodes.size() );
      addBooleanFeatures( features, !_codeInfoStore._bestCode.equals( "8000" ) );
   }

//   protected void addMorphRatioFeatures( final List<Integer> features, final MorphCodeInfoStore morphCodeInfoStore2 ) {
//      addBooleanFeatures( features, !_bestMorphCode.isEmpty()
//                                    && _bestMorphCode.equals( morphCodeInfoStore2._bestMorphCode ) );
//      addBooleanFeatures( features, !_bestHistoCode.isEmpty()
//                                    && _bestHistoCode.equals( morphCodeInfoStore2._bestHistoCode ) );
//   }
//
//
//
//   protected void addMorphFeatures( final List<Integer> features ) {
//      _mainMorphStore.addMorphFeatures( features );
//      _allMorphStore.addMorphFeatures( features );
//      _mainMorphStore.addMorphRatioFeatures( features, _allMorphStore );
//   }
//
//   protected void addMorphRatioFeatures( final List<Integer> features,
//                                         final HistologyInfoStore fullMorphStore2 ) {
//      _mainMorphStore.addMorphRatioFeatures( features, fullMorphStore2._mainMorphStore );
//      _allMorphStore.addMorphRatioFeatures( features, fullMorphStore2._allMorphStore );
//   }

   protected void addMorphStrengthFeatures( final List<Integer> features ) {
      addMorphStrengthFeatures( features, _mainUriStore, _codeInfoStore );
//      addMorphStrengthFeatures( features, _allUriStore, _allMorphStore );
   }

   static private void addMorphStrengthFeatures( final List<Integer> features,
                                                 final UriInfoStore uriInfoStore,
                                                 final HistologyCodeInfoStore morphCodeInfoStore ) {
      final Map<String, Integer> ontoMorphStrengths
            = createMorphStrengthMap( morphCodeInfoStore._uriOntoMorphCodes, uriInfoStore._uriStrengths );
      final int maxOntoMorphStrength = ontoMorphStrengths.isEmpty()
                                       ? 0
                                       :
                                       Collections.max( ontoMorphStrengths.values() );
      final Map<String, Integer> broadMorphStrengths
            = createMorphStrengthMap( morphCodeInfoStore._uriBroadMorphCodes, uriInfoStore._uriStrengths );
      final int maxBroadMorphStrength = broadMorphStrengths.isEmpty()
                                        ? 0
                                        :
                                        Collections.max( broadMorphStrengths.values() );
      final Map<String, Integer> exactMorphStrengths
            = createMorphStrengthMap( morphCodeInfoStore._uriExactMorphCodes, uriInfoStore._uriStrengths );
      final int maxExactMorphStrength = exactMorphStrengths.isEmpty()
                                        ? 0
                                        :
                                        Collections.max( exactMorphStrengths.values() );
      addLargeIntFeatures( features,
                           maxOntoMorphStrength,
                           maxBroadMorphStrength,
                           maxExactMorphStrength );
   }

   static private Map<String, Integer> createMorphStrengthMap( final Map<String, List<String>> uriMorphCodesMap,
                                                               final Map<String, Integer> uriStrengthsMap ) {
      final Map<String, Integer> morphStrengths = new HashMap<>();
      for ( Map.Entry<String, List<String>> uriMorphCodes : uriMorphCodesMap.entrySet() ) {
         final int strength = uriStrengthsMap.getOrDefault( uriMorphCodes.getKey(), 0 );
         for ( String morph : uriMorphCodes.getValue() ) {
            morphStrengths.compute( morph, ( k, v ) -> ( v == null )
                                                       ? strength
                                                       : Math.min( strength, v ) );
         }
      }
      return morphStrengths;
   }


}
