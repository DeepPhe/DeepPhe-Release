package org.healthnlp.deepphe.summary;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceMerger;
import org.apache.ctakes.cancer.summary.CiContainer;
import org.apache.ctakes.cancer.summary.CiContainerFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactFactory;
import org.healthnlp.deepphe.fact.FactList;
import org.healthnlp.deepphe.neo4j.RelationConstants;
//import org.healthnlp.deepphe.neo4j.UriConstants;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.util.FHIRConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/12/2018
 */
final public class SummaryFactory {

   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

   private SummaryFactory() {
   }


   static private Collection<Fact> createSummaryFacts( final CiContainer ciContainer,
                                                       final NoteSpecs noteSpecs ) {
//      // Build map of all ConceptInstances in the ci summary
//      final Map<String, Collection<ConceptInstance>> ciMap = new HashMap<>();
//      for ( ConceptInstance conceptInstance : ciContainer.getConceptInstances() ) {
//         ciMap.computeIfAbsent( conceptInstance.getUri(), ci -> new ArrayList<>() )
//              .add( conceptInstance );
//      }
//      for ( Map.Entry<String,Collection<ConceptInstance>> relatedInstances : ciContainer.getRelations().entrySet() ) {
//         final String relationName = relatedInstances.getKey();
//         if ( relationName.equals( RelationConstants.DISEASE_HAS_NORMAL_TISSUE_ORIGIN )
//              || relationName.equals( RelationConstants.DISEASE_HAS_NORMAL_CELL_ORIGIN ) ) {
//            // Facts get overloaded by anatomy concepts and can't tell the difference between e.g. tissues and sites
//            continue;
//         }
//         for ( ConceptInstance relatedInstance : relatedInstances.getValue() ) {
//            ciMap.computeIfAbsent( relatedInstance.getUri(), ci -> new ArrayList<>() )
//                 .add( relatedInstance );
//         }
//      }
//
//      final Collection<Fact> facts = new ArrayList<>( FactFactory.createCiFacts2( ciMap, noteSpecs ).values() );
//      final Fact histologyFact = FactFactory.createHistologyFact( ciContainer );
//      if ( histologyFact != null ) {
//         facts.add( histologyFact );
//      }
//      final Fact cancerTypeFact = FactFactory.createCancerTypeFact( ciContainer );
//      if ( cancerTypeFact != null ) {
//         facts.add( cancerTypeFact );
//      }
//      return facts;
      return Collections.emptyList();
   }

   static public Collection<TumorSummary> createPatientTumorSummaries( final JCas jCas, final NoteSpecs noteSpecs ) {
      final Collection<CiContainer> ciSummaries = CiContainerFactory.createPatientSummaries( jCas, Collections.singletonList( noteSpecs ) );
      final Collection<TumorSummary> tumorSummaries = new ArrayList<>( ciSummaries.size() );
      for ( CiContainer ciContainer : ciSummaries ) {
         final TumorSummary tumorSummary = new TumorSummary( ciContainer.getId() );
         tumorSummary.setConceptURI( ciContainer.getUri() );
         final Collection<Fact> facts = createSummaryFacts( ciContainer, noteSpecs );
         facts.forEach( f -> addFactAndContainedFacts( tumorSummary, f ) );
         tumorSummaries.add( tumorSummary );
      }
      return tumorSummaries;
   }

   static public Collection<TumorSummary> createDocTumorSummaries( final JCas jCas, final NoteSpecs noteSpecs ) {
      final Collection<CiContainer> ciSummaries = CiContainerFactory.createDocSummaries( jCas, noteSpecs );
LOGGER.warn( DocumentIDAnnotationUtil.getDeepDocumentId( jCas )
             + " ciSummary count: " + ciSummaries.size()
             + " .  IdentifiedAnnotation count: " + JCasUtil.select( jCas, IdentifiedAnnotation.class ).size() );
      final Collection<TumorSummary> tumorSummaries = new ArrayList<>( ciSummaries.size() );
      for ( CiContainer ciContainer : ciSummaries ) {
         final TumorSummary tumorSummary = new TumorSummary( ciContainer.getId() );
         tumorSummary.setConceptURI( ciContainer.getUri() );
         final Collection<Fact> facts = createSummaryFacts( ciContainer, noteSpecs );
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

   static public Collection<TumorSummary> mergeTumorSummaries( final NoteSpecs noteSpecs,
                                                               final Collection<TumorSummary> originalSummaries ) {
      final List<TumorSummary> summaryList = new ArrayList<>( originalSummaries );
      final Collection<TumorSummary> newSummaries = new ArrayList<>( originalSummaries );
      int previousCount = newSummaries.size();
      int count = 0;
      while ( previousCount != count ) {
         summaryList.retainAll( newSummaries );
         for ( int i = 0; i < summaryList.size() - 1; i++ ) {
            final TumorSummary tumorI = summaryList.get( i );
            tumorI.getBodySite();
            tumorI.getDiagnosis();
            for ( int j = i+1; j < summaryList.size(); j++ ) {
               final TumorSummary tumorJ = summaryList.get( j );
               if ( isCategoryMatch( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE, tumorI, tumorJ )
                    && isCategoryMatch( RelationConstants.HAS_DIAGNOSIS, tumorI, tumorJ )
                    && isCategoryMatch( HAS_BODY_MODIFIER, tumorI, tumorJ ) ) {
                  newSummaries.add( createMergedSummary( noteSpecs, tumorI, tumorJ ) );
                  newSummaries.remove( tumorI );
                  newSummaries.remove( tumorJ );
               }
            }
         }
         previousCount = count;
         count = newSummaries.size();
      }
      return newSummaries;
   }


   static private TumorSummary createMergedSummary( final NoteSpecs noteSpecs,
                                                    final TumorSummary tumor1, final TumorSummary tumor2 ) {
      final String bestUri = getBestUri( tumor1.getFacts( RelationConstants.HAS_DIAGNOSIS ),
            tumor2.getFacts( RelationConstants.HAS_DIAGNOSIS ) );
      final TumorSummary newSummary = new TumorSummary( createTumorId( bestUri, tumor1, tumor2 ) );
      newSummary.setConceptURI( bestUri );

      final Collection<String> categories = new HashSet<>( tumor1.getFactCategories() );
      categories.addAll( tumor2.getFactCategories() );
      for ( String category : categories ) {
         if ( category.equals( RelationConstants.HAS_HISTOLOGY )
              || category.equals( FHIRConstants.HAS_CANCER_TYPE ) ) {
            continue;
         }
         final Collection<ConceptInstance> conceptInstances
               = tumor1.getFacts( category ).stream()
                       .map( Fact::getConceptInstance )
                       .filter( Objects::nonNull )
                       .collect( Collectors.toSet() );
         tumor2.getFacts( category ).stream()
               .map( Fact::getConceptInstance )
               .filter( Objects::nonNull )
               .forEach( conceptInstances::add );
         final Map<String, Collection<ConceptInstance>> ciMap
               = ConceptInstanceMerger.createMergedInstances( conceptInstances );
         FactFactory.createCiFacts2( ciMap, noteSpecs ).values()
                    .forEach( f -> newSummary.addFact( category, f ) );
      }

      FactList histology = tumor1.getFacts( RelationConstants.HAS_HISTOLOGY );
      if ( histology.isEmpty() ) {
         histology = tumor2.getFacts( RelationConstants.HAS_HISTOLOGY );
      }
      histology.forEach( f -> newSummary.addFact( RelationConstants.HAS_HISTOLOGY, f ) );
      FactList cancerType = tumor1.getFacts( FHIRConstants.HAS_CANCER_TYPE );
      if ( cancerType.isEmpty() ) {
         cancerType = tumor2.getFacts( FHIRConstants.HAS_CANCER_TYPE );
      }
      cancerType.forEach( f -> newSummary.addFact( FHIRConstants.HAS_CANCER_TYPE, f ) );

      return newSummary;
   }

   static private String getBestUri( final FactList facts1, final FactList facts2 ) {
      final Collection<String> uris = facts1.stream().map( Fact::getUri ).collect( Collectors.toSet() );
      facts2.stream().map( Fact::getUri ).forEach( uris::add );
      return UriUtil.getMostSpecificUri( uris );
   }


   static private boolean isCategoryMatch( final String category, final TumorSummary tumor1, final TumorSummary tumor2 ) {
      final FactList facts1 = tumor1.getFacts( category );
      final FactList facts2 = tumor2.getFacts( category );
      if ( facts1 == null || facts2 == null ) {
         return facts1 == null && facts2 == null;
      }
      return ( facts1.isEmpty() && facts2.isEmpty() ) || facts1.intersects( facts2 );
   }

   static private boolean isUriMatch( final String uri1, final String uri2 ) {
      return uriContainsUri( uri1, uri2 ) || uriContainsUri( uri2, uri1 );
   }

   static private boolean uriContainsUri( final String uri1, final String uri2 ) {
      return Neo4jOntologyConceptUtil.getBranchUris( uri1 ).contains( uri2 );
   }

   static public Collection<CancerSummary> createCancerSummaries( final JCas jCas, final NoteSpecs noteSpecs ) {
      final Collection<CiContainer> ciSummaries = CiContainerFactory.createPatientSummaries( jCas, Collections.singletonList( noteSpecs ) );
      final Collection<TumorSummary> tumorSummaries = new ArrayList<>( ciSummaries.size() );
      for ( CiContainer ciContainer : ciSummaries ) {
         final TumorSummary tumorSummary = new TumorSummary( ciContainer.getId() );
         tumorSummary.setConceptURI( ciContainer.getUri() );
         final Collection<Fact> facts = createSummaryFacts( ciContainer, noteSpecs );
         facts.forEach( f -> addFactAndContainedFacts( tumorSummary, f ) );
         tumorSummaries.add( tumorSummary );
      }

      final Collection<TumorSummary> mergedSummaries = mergeTumorSummaries( noteSpecs, tumorSummaries );
      tumorSummaries.clear();
      tumorSummaries.addAll( mergedSummaries );

      final Map<String,CancerSummary> diagnosedCancers = new HashMap<>();
      for ( TumorSummary tumorSummary : tumorSummaries ) {
         final FactList diagnoses = tumorSummary.getContent().get( RelationConstants.HAS_DIAGNOSIS );
         if ( diagnoses == null || diagnoses.isEmpty() ) {
            diagnosedCancers.putIfAbsent( UriConstants.NEOPLASM,
                  new CancerSummary( UriConstants.NEOPLASM, noteSpecs.getPatientName() ) );
            continue;
         }
         for ( Fact diagnosis : diagnoses ) {
            final String uri = diagnosis.getUri();
            if ( uri.equals( UriConstants.METASTASIS ) ) {
               continue;
            }
            diagnosedCancers.putIfAbsent( uri, new CancerSummary( uri, noteSpecs.getPatientName() ) );
         }
      }
      final Collection<TumorSummary> unplacedTumors = new ArrayList<>( tumorSummaries );
      for ( TumorSummary tumorSummary : tumorSummaries ) {
         final Map<String,FactList> factLists = tumorSummary.getSummaryFacts();
         final FactList diagnoses = tumorSummary.getContent().get( RelationConstants.HAS_DIAGNOSIS );
         if ( diagnoses != null ) {
            for ( Fact diagnosis : diagnoses ) {
               final String uri = diagnosis.getUri();
               if ( uri.equals( UriConstants.METASTASIS ) ) {
                  continue;
               }
               final CancerSummary cancer = diagnosedCancers.get( uri );
               for ( Map.Entry<String,FactList> entry : factLists.entrySet() ) {
                  if ( isCancerFact( entry.getKey() ) || isCancerOnlyFact( entry.getKey() ) ) {
                     for ( Fact fact : entry.getValue() ) {
                        cancer.addFact( entry.getKey(), fact );
                        fact.getContainedFacts().forEach( cancer::addFact );
                     }
                  }
               }
               cancer.addTumor( tumorSummary );
               unplacedTumors.remove( tumorSummary );
            }
         }
      }
      if ( !unplacedTumors.isEmpty() ) {
         diagnosedCancers.putIfAbsent( UriConstants.NEOPLASM,
               new CancerSummary( UriConstants.NEOPLASM, noteSpecs.getPatientName() ) );
         final CancerSummary cancer = diagnosedCancers.get( UriConstants.NEOPLASM );
         for ( TumorSummary unplacedTumor : unplacedTumors ) {
            final Map<String,FactList> factLists = unplacedTumor.getSummaryFacts();
            for ( Map.Entry<String,FactList> entry : factLists.entrySet() ) {
               if ( isCancerFact( entry.getKey() ) || isCancerOnlyFact( entry.getKey() ) ) {
                  for ( Fact fact : entry.getValue() ) {
                     cancer.addFact( entry.getKey(), fact );
                     fact.getContainedFacts().forEach( cancer::addFact );
                  }
               }
            }
            cancer.addTumor( unplacedTumor );
         }
      }
      for ( TumorSummary tumorSummary : tumorSummaries ) {
         for ( String cancerOnly : CANCER_ONLY ) {
            tumorSummary.getContent().remove( cancerOnly );
         }
      }

      return diagnosedCancers.values();
   }




//   static public Collection<CancerSummary> createPatientDiagnosedSummaries( final JCas jCas, final NoteSpecs noteSpecs ) {
////      final Map<CiSummary,Collection<CiSummary>> ciDiagnosisMap = CiSummaryFactory.createPatientDiagnoses( jCas );
//      final Map<CiSummary,Collection<CiSummary>> ciDiagnosisMap = CiSummaryFactory.createPatientDiagnoses2( jCas, Collections.singletonList( noteSpecs ) );
//      return createDiagnosedCancerSummaries( ciDiagnosisMap, noteSpecs );
//   }

//   static public Collection<CancerSummary> createDocDiagnosedSummaries( final JCas jCas, final NoteSpecs noteSpecs ) {
//      final Map<CiSummary,Collection<CiSummary>> ciDiagnosisMap = CiSummaryFactory.createDocDiagnoses( jCas, noteSpecs );
//      return createDiagnosedCancerSummaries( ciDiagnosisMap, noteSpecs );
//   }

   static public Collection<CancerSummary> createDiagnosedCancerSummaries(
         final Map<CiContainer,Collection<CiContainer>> ciDiagnosisMap,
         final NoteSpecs noteSpecs ) {
      final Collection<CancerSummary> cancerSummaries = new ArrayList<>( ciDiagnosisMap.size() );
      final String patientName = noteSpecs.getPatientName();
      for ( Map.Entry<CiContainer,Collection<CiContainer>> cancerTumors : ciDiagnosisMap.entrySet() ) {
         final CiContainer cancer = cancerTumors.getKey();
         final CancerSummary cancerSummary = new CancerSummary( cancer.getId(), patientName );
         cancerSummary.setCiContainer( cancer );
         cancerSummary.setConceptURI( cancer.getUri() );
         final Collection<Fact> cancerFacts = createSummaryFacts( cancer, noteSpecs );
         cancerFacts.forEach( f -> addFactAndContainedFacts( cancerSummary, f ) );
         for ( CiContainer tumor : cancerTumors.getValue() ) {
            final TumorSummary tumorSummary = new TumorSummary( tumor.getId() );
            tumorSummary.setCiContainer( tumor );
            tumorSummary.setConceptURI( tumor.getUri() );
            final Collection<Fact> facts = createSummaryFacts( tumor, noteSpecs );
            facts.forEach( f -> addFactAndContainedFacts( tumorSummary, f ) );
            cancerSummary.addTumor( tumorSummary );
         }
         cancerSummaries.add( cancerSummary );
      }
      return cancerSummaries;
   }


   static private final  Collection<String> CANCER
         = Arrays.asList( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
         HAS_BODY_MODIFIER,
         RelationConstants.HAS_LATERALITY,
         RelationConstants.HAS_QUADRANT,
         RelationConstants.HAS_CLOCKFACE,
         RelationConstants.HAS_DIAGNOSIS,
         RelationConstants.HAS_CANCER_TYPE
         );
   static private boolean isCancerFact( final String category ) {
      return CANCER.contains( category );
   }

   static private final Collection<String> CANCER_ONLY
         = Arrays.asList( HAS_CLINICAL_T_CLASSIFICATION,
         HAS_CLINICAL_N_CLASSIFICATION,
         HAS_CLINICAL_M_CLASSIFICATION,
         HAS_PATHOLOGIC_T_CLASSIFICATION,
         HAS_PATHOLOGIC_N_CLASSIFICATION,
         HAS_PATHOLOGIC_M_CLASSIFICATION,
         RelationConstants.HAS_STAGE );
   static private boolean isCancerOnlyFact( final String category ) {
      return CANCER_ONLY.contains( category );
   }

   /**
    * @return some unique id for this summary.
    */
   static private String createTumorId( final String bestUri, final TumorSummary tumor1, final TumorSummary tumor2 ) {
      final Collection<String> patients = new HashSet<>();
      patients.add( tumor1.getPatientIdentifier() );
      patients.add( tumor2.getPatientIdentifier() );
      final Collection<String> docs = tumor1.getAllNoteSpecs().stream()
                                             .map( NoteSpecs::getDocumentId )
                                             .collect( Collectors.toSet() );
      tumor2.getAllNoteSpecs().stream()
            .map( NoteSpecs::getDocumentId )
            .forEach( docs::add );
      final String patient = patients.stream().sorted().collect( Collectors.joining( "_" ) );
      final String doc = docs.stream().distinct().sorted().collect( Collectors.joining( "_" ) );
      return patient + "_" + doc + "_" + bestUri;
   }

}

