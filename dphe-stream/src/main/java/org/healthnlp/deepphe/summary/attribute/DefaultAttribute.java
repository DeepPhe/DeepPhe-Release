package org.healthnlp.deepphe.summary.attribute;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;

/**
 * @author SPF , chip-nlp
 * @since {3/12/2021}
 */
public class DefaultAttribute<V extends UriInfoVisitor,T extends CodeInfoStore> implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "DefaultAttribute" );

   final private NeoplasmAttribute _neoplasmAttribute;
   protected String _bestUri;
   protected String _bestCode;


   public DefaultAttribute( final String name,
                            final ConceptAggregate neoplasm,
                     final Collection<ConceptAggregate> allConcepts,
                     final Collection<ConceptAggregate> patientNeoplasms,
                            final Supplier<V> uriVisitorCreator,
                            final Supplier<T> codeInfoStoreCreator,
                            final Map<String,String> dependencies ) {
      _neoplasmAttribute = createNeoplasmAttribute( name,
                                                     neoplasm,
                                                     allConcepts,
                                                     patientNeoplasms,
                                                     uriVisitorCreator,
                                                     codeInfoStoreCreator,
                                                    dependencies );
   }

   protected NeoplasmAttribute createNeoplasmAttribute( final String name,
                                                       final ConceptAggregate neoplasm,
                                                       final Collection<ConceptAggregate> allConcepts,
                                                       final Collection<ConceptAggregate> patientNeoplasms,
                                                       final Supplier<V> uriVisitorCreator,
                                                       final Supplier<T> codeInfoStoreCreator,
                                                      final Map<String,String> dependencies ) {
      NeoplasmSummaryCreator.addDebug( "#####  " + name + "  #####\nPatient Store\n" );
      final AttributeInfoStore<V,T> patientStore
            = new AttributeInfoStore<V,T>( patientNeoplasms, uriVisitorCreator, codeInfoStoreCreator, dependencies );
      NeoplasmSummaryCreator.addDebug( "Neoplasm Store\n" );
      final AttributeInfoStore<V,T> neoplasmStore
            = new AttributeInfoStore<>( neoplasm, uriVisitorCreator, codeInfoStoreCreator, dependencies );
      NeoplasmSummaryCreator.addDebug( "All Concepts Store\n" );
      final AttributeInfoStore<V,T> allConceptsStore
            = new AttributeInfoStore<>( allConcepts, uriVisitorCreator, codeInfoStoreCreator, dependencies );

      _bestUri = neoplasmStore._mainUriStore._bestUri;
      _bestCode = neoplasmStore.getBestCode();

      final List<Integer> features
//            = createFeatures( neoplasm,
//                                                     allConcepts,
//                                                     neoplasmStore,
//                                                     patientStore,
//                                                     allConceptsStore );
            = new ArrayList<>();
      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( neoplasmStore._concepts,
                                             patientStore._concepts,
                                             allConceptsStore._concepts );
//            = new HashMap<>();
      return SpecificAttribute.createAttribute( name,
                                                _bestCode,
                                                _bestUri,
                                                evidence,
                                                features );
   }

   public String getBestUri() {
      return _bestUri;
   }

   public String getBestCode() {
      return _bestCode;
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _neoplasmAttribute;
   }

   protected List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Collection<ConceptAggregate> allConcepts,
                                         final AttributeInfoStore<V,T> neoplasmStore,
                                         final AttributeInfoStore<V,T> patientStore,
                                         final AttributeInfoStore<V,T> allConceptStore ) {
      final List<Integer> features = new ArrayList<>();

      neoplasmStore.addGeneralFeatures( features );
      patientStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, patientStore );

      allConceptStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, allConceptStore );

      final boolean noBestUri = neoplasmStore._mainUriStore._bestUri.isEmpty();
      addBooleanFeatures( features,
                          noBestUri,
                          noBestUri && !patientStore._mainUriStore._bestUri.isEmpty(),
                          noBestUri && !allConceptStore._mainUriStore._bestUri.isEmpty(),
                          neoplasmStore._mainUriStore._bestUri.equals( patientStore._mainUriStore._bestUri ),
                          neoplasmStore._mainUriStore._bestUri.equals( allConceptStore._mainUriStore._bestUri ) );

      addBooleanFeatures( features,
                          neoplasm.isNegated(),
                          neoplasm.isUncertain(),
                          neoplasm.isGeneric(),
                          neoplasm.isConditional() );
      return features;
   }


}
