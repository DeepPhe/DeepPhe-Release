package org.healthnlp.deepphe.summary.attribute.biomarker;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author SPF , chip-nlp
 * @since {4/9/2021}
 */
final public class Biomarker extends DefaultAttribute<BiomarkerUriInfoVisitor, BiomarkerCodeInfoStore> {

   static private final Logger LOGGER = Logger.getLogger( "Biomarker" );

   public Biomarker( final String biomarkerName,
                     final ConceptAggregate neoplasm,
                     final Collection<ConceptAggregate> allConcepts,
                     final Collection<ConceptAggregate> patientNeoplasms ) {
      super( biomarkerName,
             neoplasm,
             allConcepts,
             patientNeoplasms,
             () -> new BiomarkerUriInfoVisitor( biomarkerName ),
             BiomarkerCodeInfoStore::new,
             Collections.emptyMap() );
   }


   @Override
   protected NeoplasmAttribute createNeoplasmAttribute( final String name,
                                                        final ConceptAggregate neoplasm,
                                                        final Collection<ConceptAggregate> allConcepts,
                                                        final Collection<ConceptAggregate> patientNeoplasms,
                                                        final Supplier<BiomarkerUriInfoVisitor> uriVisitorCreator,
                                                        final Supplier<BiomarkerCodeInfoStore> codeInfoStoreCreator,
                                                        final Map<String,String> dependencies ) {
      NeoplasmSummaryCreator.addDebug( "#####  " + name + "  #####\nPatient Store\n" );
      final AttributeInfoStore<BiomarkerUriInfoVisitor, BiomarkerCodeInfoStore> patientStore
            = new BiomarkerInfoStore( patientNeoplasms, uriVisitorCreator, codeInfoStoreCreator, dependencies );
      NeoplasmSummaryCreator.addDebug( "Neoplasm Store\n" );
      final AttributeInfoStore<BiomarkerUriInfoVisitor, BiomarkerCodeInfoStore> neoplasmStore
            = new BiomarkerInfoStore( neoplasm, uriVisitorCreator, codeInfoStoreCreator, dependencies );
      NeoplasmSummaryCreator.addDebug( "All Concepts Store\n" );
      final AttributeInfoStore<BiomarkerUriInfoVisitor, BiomarkerCodeInfoStore> allConceptsStore
            = new BiomarkerInfoStore( allConcepts, uriVisitorCreator, codeInfoStoreCreator, dependencies );

      String bestUri = neoplasmStore._mainUriStore._bestUri;
      if ( bestUri == null || bestUri.isEmpty() ) {
         bestUri = patientStore._mainUriStore._bestUri;
      }
      if ( bestUri == null || bestUri.isEmpty() ) {
         bestUri = allConceptsStore._mainUriStore._bestUri;
      }
      _bestUri = bestUri;
      String bestCode = neoplasmStore.getBestCode();
      if ( bestCode == null || bestCode.isEmpty() ) {
         bestCode = patientStore.getBestCode();
      }
      if ( bestCode == null || bestCode.isEmpty() ) {
         bestCode = allConceptsStore.getBestCode();
      }
      _bestCode = bestCode;

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




}
