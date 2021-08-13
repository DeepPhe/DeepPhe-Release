package org.healthnlp.deepphe.summary.attribute.histology;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * @author SPF , chip-nlp
 * @since {3/12/2021}
 */
public class Histology extends DefaultAttribute<HistologyUriInfoVisitor,HistologyCodeInfoStore> {

   static private final Logger LOGGER = Logger.getLogger( "Histology" );

   public Histology( final ConceptAggregate neoplasm,
                     final Collection<ConceptAggregate> allConcepts,
                     final Collection<ConceptAggregate> patientNeoplasms ) {
      super( "histology",
             neoplasm,
             allConcepts,
             patientNeoplasms,
             HistologyUriInfoVisitor::new,
             HistologyCodeInfoStore::new,
             Collections.emptyMap() );
   }

   @Override
   protected NeoplasmAttribute createNeoplasmAttribute( final String name,
                                                       final ConceptAggregate neoplasm,
                                                       final Collection<ConceptAggregate> allConcepts,
                                                       final Collection<ConceptAggregate> patientNeoplasms,
                                                       final Supplier<HistologyUriInfoVisitor> uriVisitorCreator,
                                                       final Supplier<HistologyCodeInfoStore> codeInfoStoreCreator,
                                                      final Map<String,String> dependencies ) {
      NeoplasmSummaryCreator.DEBUG_SB.append( "#####  " ).append( name ).append( "  #####\nPatient Store\n" );
      final AttributeInfoStore<HistologyUriInfoVisitor, HistologyCodeInfoStore> patientStore
            = new HistologyInfoStore( patientNeoplasms );
      NeoplasmSummaryCreator.DEBUG_SB.append( "Neoplasm Store\n" );
      final AttributeInfoStore<HistologyUriInfoVisitor, HistologyCodeInfoStore> neoplasmStore
            = new HistologyInfoStore( neoplasm );
      NeoplasmSummaryCreator.DEBUG_SB.append( "All Concepts Store\n" );
      final AttributeInfoStore<HistologyUriInfoVisitor, HistologyCodeInfoStore> allConceptsStore
            = new HistologyInfoStore( allConcepts );

      _bestUri = neoplasmStore._mainUriStore._bestUri;
      _bestCode = neoplasmStore.getBestCode();

      final List<Integer> features = createFeatures( neoplasm,
                                                     allConcepts,
                                                     neoplasmStore,
                                                     patientStore,
                                                     allConceptsStore );

      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( neoplasmStore._concepts,
                                             patientStore._concepts,
                                             allConceptsStore._concepts );

      return SpecificAttribute.createAttribute( name,
                                                _bestCode,
                                                _bestUri,
                                                evidence,
                                                features );
   }


}
