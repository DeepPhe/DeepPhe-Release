package org.healthnlp.deepphe.summary;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.summary.CiSummary;
import org.apache.ctakes.cancer.summary.CiSummaryFactory;
import org.apache.ctakes.core.note.NoteSpecs;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactFactory;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/12/2018
 */
final public class SummaryFactory {

   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

   private SummaryFactory() {
   }


   static private Collection<Fact> createSummaryFacts( final CiSummary ciSummary,
                                                       final NoteSpecs noteSpecs ) {
      // Build map of all ConceptInstances in the ci summary
      final Map<String, Collection<ConceptInstance>> ciMap = new HashMap<>();
      for ( ConceptInstance conceptInstance : ciSummary.getConceptInstances() ) {
         ciMap.computeIfAbsent( conceptInstance.getUri(), ci -> new ArrayList<>() )
              .add( conceptInstance );
      }
      for ( Map.Entry<String,Collection<ConceptInstance>> relatedInstances : ciSummary.getRelations().entrySet() ) {
         final String relationName = relatedInstances.getKey();
         if ( relationName.equals( RelationConstants.DISEASE_HAS_NORMAL_TISSUE_ORIGIN )
              || relationName.equals( RelationConstants.DISEASE_HAS_NORMAL_CELL_ORIGIN ) ) {
            // Facts get overloaded by anatomy concepts and can't tell the difference between e.g. tissues and sites
            continue;
         }
         for ( ConceptInstance relatedInstance : relatedInstances.getValue() ) {
            ciMap.computeIfAbsent( relatedInstance.getUri(), ci -> new ArrayList<>() )
                 .add( relatedInstance );
         }
      }

      final Collection<Fact> facts = new ArrayList<>( FactFactory.createCiFacts2( ciMap, noteSpecs ).values() );
      final Fact histologyFact = FactFactory.createHistologyFact( ciSummary );
      if ( histologyFact != null ) {
         facts.add( histologyFact );
      }
      final Fact cancerTypeFact = FactFactory.createCancerTypeFact( ciSummary );
      if ( cancerTypeFact != null ) {
         facts.add( cancerTypeFact );
      }
      return facts;
   }

   static public Collection<TumorSummary> createTumorSummaries( final JCas jCas, final NoteSpecs noteSpecs ) {
      final Collection<CiSummary> ciSummaries = CiSummaryFactory.createPatientSummaries( jCas );
      final Collection<TumorSummary> tumorSummaries = new ArrayList<>( ciSummaries.size() );
      for ( CiSummary ciSummary : ciSummaries ) {
         final TumorSummary tumorSummary = new TumorSummary( ciSummary.getId() );
         tumorSummary.setConceptURI( ciSummary.getUri() );
         final Collection<Fact> facts = createSummaryFacts( ciSummary, noteSpecs );
         facts.forEach( f -> addFactAndContainedFacts( tumorSummary, f ) );
         tumorSummaries.add( tumorSummary );
      }
      return tumorSummaries;
   }



   static private void addFactAndContainedFacts(Summary summary, Fact fact) {
      List<Fact> facts = new ArrayList<>();
      facts.add( fact );
      facts.addAll( fact.getContainedFacts() );

      final List<String> categoryList = new ArrayList<>( fact.getAllRelatedFacts().keySet() );
      categoryList.sort( String.CASE_INSENSITIVE_ORDER );
      for ( String category : categoryList ) {
           facts.addAll(fact.getRelatedFacts(category));
       }

      facts.sort( FactFactory.FACT_SORTER );

      for ( Fact f : facts ) {
           f.setSummaryId(summary.getId());
           fact.setSummaryType(summary.getSummaryType());
           summary.addFact(f);
       }

   }



}

