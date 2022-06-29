package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.*;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addLargeIntFeatures;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addRatioFeature;
import static org.healthnlp.deepphe.summary.attribute.util.UriMapUtil.*;

public class AttributeInfoStore<V extends UriInfoVisitor, T extends CodeInfoStore> {

   // Uris
   final public AllUriInfoStore _allUriStore;
   final public MainUriInfoStore _mainUriStore;
   final public Map<String, Collection<String>> _uriRootsMap;
   // Codes
   final public T _codeInfoStore;
   // Concepts, Mentions
   final public Collection<ConceptAggregate> _concepts;
   final public Collection<Mention> _mentions;
   final public Map<String, List<Mention>> _uriMentions;
   final public Collection<ConceptAggregate> _neoplasmsAsBest;
   final public Collection<ConceptAggregate> _neoplasmsWithBest;
   final public Map<String, Integer> _mentionBranchCounts;
   final public Map<String, Integer> _mentionAsBestBranchCounts;
   final public Map<String, Integer> _mentionWithBestBranchCounts;
   final public int _mentionBranchCountsSum;
   final public int _mentionAsBestBranchCountsSum;
   final public int _mentionWithBestBranchCountsSum;

   public AttributeInfoStore( final ConceptAggregate neoplasm,
                              final Supplier<V> uriVisitorCreator,
                              final Supplier<T> codeInfoStoreCreator,
                              final Map<String,String> dependencies ) {
      this( Collections.singletonList( neoplasm ), uriVisitorCreator, codeInfoStoreCreator, dependencies );
   }

   static private String toConceptText( final ConceptAggregate concept ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "  " ).append( concept.getUri() );
      final Map<String,Double> uriQuotients
            = concept.getUriQuotients()
                     .stream()
                     .collect( Collectors.toMap( KeyValue::getKey, KeyValue::getValue ) );
      final Map<String,List<Mention>> uriMentionsMap = concept.getUriMentions();
      for ( Map.Entry<String,List<Mention>> uriMentions : uriMentionsMap.entrySet() ) {
         sb.append( "\n    " )
           .append( uriMentions.getKey() )
           .append( " = " )
           .append( uriQuotients.get( uriMentions.getKey() ) )
           .append( " : " );
         for( Mention mention : uriMentions.getValue() ) {
            sb.append( "[" ).append( concept.getCoveredText( mention ) ).append( "]" );
         }
      }
      return sb.append( "\n" ).toString();
   }

   public AttributeInfoStore( final Collection<ConceptAggregate> neoplasms,
                              final Supplier<V> uriVisitorCreator,
                              final Supplier<T> codeInfoStoreCreator,
                              final Map<String,String> dependencies ) {
      NeoplasmSummaryCreator.addDebug( "  AllUriStore\n" );
      _allUriStore = new AllUriInfoStore( neoplasms, uriVisitorCreator.get() );
      NeoplasmSummaryCreator.addDebug( "  MainUriStore\n" );
      _mainUriStore = new MainUriInfoStore( neoplasms, _allUriStore, uriVisitorCreator.get() );
      _uriRootsMap = UriUtil.mapUriRoots( _allUriStore._uris );
      _concepts = uriVisitorCreator.get().getAttributeConcepts( neoplasms );
      _mentions = getMentions( _concepts );
      _uriMentions = mapUriMentions( _mentions );
//         _uriMentionCounts = UriScoreUtil.mapUriMentionCounts( _mentions );
      _neoplasmsAsBest = getIfUriIsMain( _mainUriStore._bestUri, _concepts );
      _neoplasmsWithBest = getIfUriIsAny( _mainUriStore._bestUri, _concepts );
      _mentionBranchCounts = mapAllUriBranchMentionCounts( _uriMentions, _uriRootsMap );
      _mentionAsBestBranchCounts = mapUriBranchConceptCounts( _neoplasmsAsBest, _uriRootsMap );
      _mentionWithBestBranchCounts = mapUriBranchConceptCounts( _neoplasmsWithBest, _uriRootsMap );
      _mentionBranchCountsSum = getBranchCountsSum( _mentionBranchCounts );
      _mentionAsBestBranchCountsSum = getBranchCountsSum( _mentionAsBestBranchCounts );
      _mentionWithBestBranchCountsSum = getBranchCountsSum( _mentionWithBestBranchCounts );
      _codeInfoStore = codeInfoStoreCreator.get();
      initCodeInfoStore( dependencies );

      _concepts.stream()
               .map( AttributeInfoStore::toConceptText )
               .forEach( NeoplasmSummaryCreator::addDebug );
   }

   public void initCodeInfoStore( final Map<String,String> dependencies ) {
      initCodeInfoStore( _mainUriStore, dependencies );
   }

   public void initCodeInfoStore( final UriInfoStore uriInfoStore,
                                  final Map<String,String> dependencies ) {
      _codeInfoStore.init( uriInfoStore, dependencies );
   }

   public String getBestUri() {
      return _mainUriStore._bestUri;
   }

   public String getBestCode() {
      return _codeInfoStore.getBestCode();
   }

   public void addGeneralFeatures( final List<Integer> features ) {
      // Neoplasm Unique URI Counts.   Main Uris for single-concept neoplasm is always 1.
      final int mainUrisCount = _mainUriStore._uris.size();
      final int allUrisCount = _allUriStore._uris.size();
      addLargeIntFeatures( features, mainUrisCount, allUrisCount );
      addRatioFeature( features, mainUrisCount, allUrisCount );
      // Neoplasm Concepts, Mentions
      addLargeIntFeatures( features, _concepts.size(), _mentions.size() );
      addRatioFeature( features, _concepts.size(), _mentions.size() );
      // Neoplasm unique Uris compared to Concepts.
      addRatioFeature( features, mainUrisCount, _concepts.size() );
      // Neoplasm unique Uris compared to Mentions.
      addRatioFeature( features, mainUrisCount, _mentions.size() );
      addRatioFeature( features, allUrisCount, _mentions.size() );
      addLargeIntFeatures( features,
                           _neoplasmsAsBest.size(),
                           _neoplasmsWithBest.size() );
      addRatioFeature( features,
                       _neoplasmsAsBest.size(),
                       _mentions.size() );
      addRatioFeature( features,
                       _neoplasmsWithBest.size(),
                       _mentions.size() );
      addLargeIntFeatures( features,
                           _mentionBranchCounts.size(),
                           _mentionAsBestBranchCounts.size(),
                           _mentionWithBestBranchCounts.size() );
      addLargeIntFeatures( features,
                           _mentionBranchCountsSum,
                           _mentionAsBestBranchCountsSum,
                           _mentionWithBestBranchCountsSum );
      addLargeIntFeatures( features,
                           _mainUriStore._maxDepth,
                           _allUriStore._maxDepth );
      addRatioFeature( features, _mainUriStore._maxDepth, _allUriStore._maxDepth );
      addMentionStrengthFeatures( features, _mainUriStore );
      addMentionStrengthFeatures( features, _allUriStore );
   }

   public void addMentionStrengthFeatures( final List<Integer> features, final UriInfoStore uriInfoStore ) {
      // The number of Mentions with the maximum strength.  For single-concept neoplasm is NOT always 1.
      addMentionStrengthFeatures( features, uriInfoStore, uriInfoStore._maxUriStrength );
      // 2nd highest strength.  This is NOT the max strength even if max strength has > 1 uris.
      addMentionStrengthFeatures( features, uriInfoStore, uriInfoStore._2ndUriStrength );
   }

   public void addMentionStrengthFeatures( final List<Integer> features, final UriInfoStore uriInfoStore,
                                           final int strength ) {
      final Collection<String> urisWithStrength
            = uriInfoStore._strengthUriMap.getOrDefault( strength,
                                                         Collections.emptyList() );
      final int mentionsWithStrengthCount =
            urisWithStrength.stream()
                            .map( u -> _uriMentions.getOrDefault( u, Collections.emptyList() ) )
                            .mapToInt( Collection::size )
                            .sum();
      final int mentionBranchWithStrengthCount
            = urisWithStrength.stream()
                              .mapToInt( u -> _mentionBranchCounts.getOrDefault( u, 0 ) )
                              .sum();
      addLargeIntFeatures( features,
                           mentionsWithStrengthCount,
                           mentionBranchWithStrengthCount );
      addRatioFeature( features, mentionsWithStrengthCount, _mentions.size() );
      addRatioFeature( features, mentionBranchWithStrengthCount, _mentionBranchCountsSum );
   }

   public void addGeneralRatioFeatures( final List<Integer> features,
                                        final AttributeInfoStore attributeInfoStore2 ) {
      // Neoplasm Concepts, Mentions
      addRatioFeature( features, _concepts.size(), attributeInfoStore2._concepts.size() );
      addRatioFeature( features, _mentions.size(), attributeInfoStore2._mentions.size() );
      // Neoplasm URI Counts.   Main Uris for single-concept neoplasm is always 1.
      final int mainUrisCount2 = attributeInfoStore2._mainUriStore._uris.size();
      final int allUrisCount2 = attributeInfoStore2._allUriStore._uris.size();
      addRatioFeature( features, _mainUriStore._uris.size(), mainUrisCount2 );
      addRatioFeature( features, _allUriStore._uris.size(), allUrisCount2 );
      addRatioFeature( features,
                       _neoplasmsAsBest.size(),
                       attributeInfoStore2._neoplasmsAsBest.size() );
      addRatioFeature( features,
                       _neoplasmsWithBest.size(),
                       attributeInfoStore2._neoplasmsWithBest.size() );
      addRatioFeature( features, _mentionBranchCounts.size(), attributeInfoStore2._mentionBranchCounts.size() );
      addRatioFeature( features, _mentionAsBestBranchCounts.size(), attributeInfoStore2._mentionAsBestBranchCounts.size() );
      addRatioFeature( features, _mentionWithBestBranchCounts.size(),
                       attributeInfoStore2._mentionWithBestBranchCounts.size() );
      addRatioFeature( features, _mentionBranchCountsSum, attributeInfoStore2._mentionBranchCountsSum );
      addRatioFeature( features, _mentionAsBestBranchCountsSum, attributeInfoStore2._mentionAsBestBranchCountsSum );
      addRatioFeature( features, _mentionWithBestBranchCountsSum, attributeInfoStore2._mentionWithBestBranchCountsSum );
      addRatioFeature( features, _mainUriStore._maxDepth, attributeInfoStore2._mainUriStore._maxDepth );
      addRatioFeature( features, _allUriStore._maxDepth, attributeInfoStore2._allUriStore._maxDepth );
   }

   public void addUriStrengthFeatures( final List<Integer> features ) {
      _mainUriStore.addStrengthFeatures( features );
      _allUriStore.addStrengthFeatures( features );
   }

}
