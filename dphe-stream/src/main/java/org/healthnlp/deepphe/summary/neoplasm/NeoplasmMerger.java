package org.healthnlp.deepphe.summary.neoplasm;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateHandler;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;
import org.healthnlp.deepphe.summary.container.CancerContainer;
import org.healthnlp.deepphe.summary.container.TumorContainer;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_DIAGNOSIS;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_LATERALITY;
import static org.healthnlp.deepphe.summary.concept.ConceptAggregate.NeoplasmType.*;

/**
 * @author SPF , chip-nlp
 * @since {4/22/2021}
 */
final public class NeoplasmMerger {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmMerger" );

   private NeoplasmMerger() {}


   static public Map<ConceptAggregate, Collection<ConceptAggregate>> mergeNeoplasms(
         final Collection<ConceptAggregate> neoplasms,
         final Collection<ConceptAggregate> conceptAggregates ) {
//      LOGGER.info( "\n================ Merging Tumors ===================" );
//      LOGGER.info( "We want to determine existing Cancers and associate tumors with those cancers.  " +
//                   "The first thing to do is to merge ConceptAggregates into unique Tumors." );
      final Collection<ConceptAggregate> tumors = mergeTumors( neoplasms, conceptAggregates );
//      LOGGER.info( "Current ConceptAggregates:" );
//      conceptAggregates.forEach( LOGGER::info );
//      LOGGER.info( "\n================ Merging Cancers ===================" );
//      LOGGER.info( "We now have unique tumors.  We will try to associate them and classify unique Cancers accordingly." );
      final Map<ConceptAggregate,Collection<ConceptAggregate>> cancerMap = mergeCancers( neoplasms, conceptAggregates, tumors );
//      for ( Collection<ConceptInstance> cancerTumors : cancerMap.values() ) {
//         for ( ConceptInstance cancerTumor : cancerTumors ) {
//            final Map<String,Collection<ConceptInstance>> related = cancerTumor.getRelated();
//            final Collection<String> removals = related.keySet().stream()
//                                                       .filter( ConceptInstanceUtil::isCancerOnlyFact )
//                                                       .collect( Collectors.toList() );
//            LOGGER.info( "Cancer Only Removals\n" + String.join( " ; ", removals ) );
//            related.keySet().removeAll( removals );
//         }
//      }
      return cancerMap;
   }


   /////////////////////////////////////////////////////////////////////////
   //
   //             Tumor Creation
   //
   /////////////////////////////////////////////////////////////////////////


   /**
    * Merge neoplasms based entirely on location information : site and laterality.
    * @param neoplasms all neoplasm instances for the patient / doc.
    * @param allConcepts all instances for the patient / doc.
    * @return a new collection of (possibly) merged neoplasms.
    */
   static private Collection<ConceptAggregate> mergeTumors( final Collection<ConceptAggregate> neoplasms,
                                                            final Collection<ConceptAggregate> allConcepts ) {
      if ( neoplasms.isEmpty() ) {
         return neoplasms;
      }
      if ( neoplasms.size() == 1 ) {
         final Collection<ConceptAggregate> candidateLocations
               = new ArrayList<>( neoplasms ).get( 0 ).getRelatedSites();
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            return Collections.emptyList();
         }
         return neoplasms;
      }
      final Collection<ConceptAggregate> assignedNeoplasms = new HashSet<>();
      final Collection<TumorContainer> tumorContainers = new HashSet<>();
      // Combine masses on the same site.  Use Laterality.
      mergeLateralTumors( neoplasms, assignedNeoplasms, tumorContainers );
      // Combine masses on the same site.  Laterality already used.
      mergeNonLateralTumors( neoplasms, assignedNeoplasms, tumorContainers );
//      LOGGER.info( "\nTumor Containers by Laterality and Location:" );
//      tumorContainers.forEach( t -> LOGGER.info( "("
//                                                 + t._neoplasms.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining(",") ) + ")" ) );
      neoplasms.removeAll( assignedNeoplasms );
      // Now we have created TumorContainers.  Create a Merged ConceptInstance from each.
      final Collection<String> tumorIds = new HashSet<>();
      for ( TumorContainer tumor : tumorContainers ) {
         tumorIds.add( tumor.createMergedConcept( allConcepts ).getId() );
      }

      for ( ConceptAggregate concept : allConcepts ) {
         final Map<String,Collection<ConceptAggregate>> oldRelated = concept.getRelatedConceptMap();
         for ( Map.Entry<String,Collection<ConceptAggregate>> oldRelations : oldRelated.entrySet() ) {
            final Collection<ConceptAggregate> removals = new HashSet<>();
            for ( ConceptAggregate concept1 : oldRelations.getValue() ) {
               if ( !allConcepts.contains( concept1 ) ) {
                  removals.add( concept1 );
               }
            }
            oldRelations.getValue().removeAll( removals );
         }
      }

      final Collection<ConceptAggregate> tumorConcepts = new HashSet<>( tumorIds.size() );
      for ( ConceptAggregate concept : allConcepts ) {
         if ( tumorIds.contains( concept.getId() ) ) {
            tumorConcepts.add( concept );
         }
      }
      return tumorConcepts;
   }








   /////////////////////////////////////////////////////////////////////////
   //
   //             Tumor Container
   //
   /////////////////////////////////////////////////////////////////////////


   static private void mergeLateralTumors( final Collection<ConceptAggregate> neoplasms,
                                           final Collection<ConceptAggregate> assignedNeoplasms,
                                           final Collection<TumorContainer> tumorContainers ) {
      if ( neoplasms.size() <= 1 ) {
         return;
      }


//      LOGGER.info( "\nMerging Tumors by Laterality and Site ..." );


      // Seed Bilaterals
      for ( ConceptAggregate neoplasm : neoplasms ) {
         final Collection<ConceptAggregate> lateralities
               = neoplasm.getRelatedConceptMap().get( HAS_LATERALITY );
         if ( lateralities == null || lateralities.isEmpty() ) {
            continue;
         }
         if ( lateralities.stream().map( ConceptAggregate::getUri ).noneMatch( UriConstants.BILATERAL::equals ) ) {
            continue;
         }
         final Collection<ConceptAggregate> candidateLocations = neoplasm.getRelatedSites();
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            continue;
         }
         boolean assigned = false;
         for ( TumorContainer container : tumorContainers ) {
            if ( container.isLocationMatch( neoplasm ) ) {
               container.addNeoplasm( neoplasm );
               assigned = true;


//               LOGGER.info( "Added Concept Aggregate " + neoplasm.getUri()
//                            + " to Bilateral ("
//                            + container._neoplasms.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining(",") )
//                            + ") (" + container._neoplasms.stream()
//                                                        .map( ConceptAggregate::getRelatedSites )
//                                                        .flatMap( Collection::stream )
//                                                        .map( ConceptAggregate::getUri ).collect( Collectors.joining(",") ) + ")" );


            }
         }
         if ( !assigned ) {
            tumorContainers.add( new TumorContainer( neoplasm ) );


//            LOGGER.info( "Added Concept Aggregate " + neoplasm.getUri() + " " + neoplasm.getId() + " to Bilateral new Container (" +
//                         String.join( ",", neoplasm.getRelatedSiteUris() ) + ")" );


         }
         assignedNeoplasms.add( neoplasm );
      }
      // Combine masses on the same site.  Use Laterality.
      for ( ConceptAggregate neoplasm : neoplasms ) {
         if ( assignedNeoplasms.contains( neoplasm ) ) {
            continue;
         }
         final Collection<ConceptAggregate> lateralities
               = neoplasm.getRelatedConceptMap().get( HAS_LATERALITY );
         if ( lateralities == null || lateralities.isEmpty() ) {
            continue;
         }
         final Collection<ConceptAggregate> candidateLocations = neoplasm.getRelatedSites();
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            continue;
         }
         boolean assigned = false;
         for ( TumorContainer container : tumorContainers ) {
            if ( container.isLocationMatch( neoplasm )
                 && container.isLateralityMatch( neoplasm ) ) {
               container.addNeoplasm( neoplasm );
               assigned = true;


//               LOGGER.info( "Added Concept Aggregate " + neoplasm.getUri() + " to Lateral ("
//                            + lateralities.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining(",") ) + ") ("
//                            + container._neoplasms.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining(",") ) + ") ("
//                            + container._neoplasms.stream()
//                                                                                .map( ConceptAggregate::getRelatedSites )
//                                                                                .flatMap( Collection::stream )
//                                                                                .map( ConceptAggregate::getUri ).collect( Collectors.joining(",") ) + ")" );



            }
         }
         if ( !assigned ) {
            tumorContainers.add( new TumorContainer( neoplasm ) );


//            LOGGER.info( "Added Concept Aggregate " + neoplasm.getUri() + " " + neoplasm.getId() + " to Lateral new Container ("
//                         + lateralities.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining(",") )
//                         + ") (" + String.join( ",", neoplasm.getRelatedSiteUris() ) + ")" );



         }
         assignedNeoplasms.add( neoplasm );
      }
   }


   static private void mergeNonLateralTumors(
         final Collection<ConceptAggregate> neoplasms,
         final Collection<ConceptAggregate> assignedNeoplasms,
         final Collection<TumorContainer> tumorContainers ) {
      if ( neoplasms.size() <= 1 ) {
         return;
      }
      // Combine masses on the same site.  Laterality already used.
      for ( ConceptAggregate neoplasm : neoplasms ) {
         if ( assignedNeoplasms.contains( neoplasm ) ) {
            continue;
         }
         final Collection<ConceptAggregate> candidateLocations = neoplasm.getRelatedSites();
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            continue;
         }
         boolean assigned = false;
         for ( TumorContainer container : tumorContainers ) {
            if ( container.isLocationMatch( neoplasm ) ) {
               container.addNeoplasm( neoplasm );
               assigned = true;


//               LOGGER.info( "Added Concept Aggregate " + neoplasm.getUri()
//                            + " to Non lateral ("
//                            + container._neoplasms.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining(",") )
//                            + ") (" + container._neoplasms.stream()
//                                                        .map( ConceptAggregate::getRelatedSites )
//                                                        .flatMap( Collection::stream )
//                                                        .map( ConceptAggregate::getUri ).collect( Collectors.joining(",") ) + ")" );


            }
         }
         if ( !assigned ) {
            tumorContainers.add( new TumorContainer( neoplasm ) );


//            LOGGER.info( "Added Concept Aggregate " + neoplasm.getUri() + " " + neoplasm.getId() + " to non lateral new Container (" +
//                         String.join( ",", neoplasm.getRelatedSiteUris() ) + ")" );


         }
         assignedNeoplasms.add( neoplasm );
      }
   }



   /////////////////////////////////////////////////////////////////////////
   //
   //             Cancer Creation
   //
   /////////////////////////////////////////////////////////////////////////

   // TODO : moving the cancer container creation and population to methods.
   static private Map<ConceptAggregate,Collection<ConceptAggregate>> mergeCancers(
         final Collection<ConceptAggregate> neoplasms,
         final Collection<ConceptAggregate> allConcepts,
         final Collection<ConceptAggregate> tumors ) {

      final Map<ConceptAggregate.NeoplasmType,Collection<ConceptAggregate>> typeTumors
            = new EnumMap<>( ConceptAggregate.NeoplasmType.class );
      tumors.forEach( t -> typeTumors.computeIfAbsent( t.getNeoplasmType(),
                                                       l -> new HashSet<>() ).add( t ) );


//      LOGGER.info( "Tumors by Type:" );
//      typeTumors.forEach( (k,v) -> LOGGER.info( k + " (" + v.stream().map( c -> c.getUri() + " " + c.getId() ).collect( Collectors.joining(",") ) + ")" ) );


      // Do tumors first so that tumor duplication is less frequent.
      final Collection<CancerContainer> cancerContainers = mergeCancerTumors( typeTumors );

      // Combine neoplasms by diagnosis.  Do non-tumor neoplasms second as duplication is possibly better.
      addCancerNeoplasms( neoplasms, cancerContainers );

      createCancers( typeTumors.get( ConceptAggregate.NeoplasmType.NON_CANCER ), cancerContainers, new HashSet<>() );

      final Map<ConceptAggregate,Collection<ConceptAggregate>> cancerTumorMap = new HashMap<>( cancerContainers.size() );
      for ( CancerContainer container : cancerContainers ) {
         final ConceptAggregate cancer = container.createMergedConcept( allConcepts );
         cancerTumorMap.put( cancer, container.getAllLocatedTumors() );
      }

      // Shouldn't this get an "assigned" and if not then the primary gets its own container?
      return cancerTumorMap;
   }






   // TODO for a metastasis located in X with diagnosis Y, assign to any cancers with URI Y or diagnosis Y.
   //  ----> It should be doing this.  What is up?

   static private Collection<CancerContainer> mergeCancerTumors(
         final Map<ConceptAggregate.NeoplasmType,Collection<ConceptAggregate>> typeTumors ) {
      if ( typeTumors.isEmpty() ) {
         return new HashSet<>();
      }
      final Collection<CancerContainer> cancerContainers = new HashSet<>();
      final Collection<ConceptAggregate> assignedNeoplasms = new HashSet<>();

///      // Create Containers with primary tumors.  Do tumors first so that duplication is less frequent.
      createCancers( typeTumors.get( PRIMARY ), cancerContainers, assignedNeoplasms );


//      LOGGER.info( "Creating Cancers by Primary Tumors ..." );
      createCancerContainers( typeTumors.get( PRIMARY ), cancerContainers, assignedNeoplasms );
//      LOGGER.info( "Creating Cancers by Unknown Tumors ..." );
      createCancerContainers( typeTumors.get( UNKNOWN ), cancerContainers, assignedNeoplasms );
//      LOGGER.info( "Creating Cancers by Secondary Tumors ..." );
      createCancerContainers( typeTumors.get( SECONDARY ), cancerContainers, assignedNeoplasms );


      // Add metastases to containers by diagnosis.  If there is no primary discovered, create a new "metastatic" cancer.
      addCancerMetastases( typeTumors, cancerContainers, assignedNeoplasms );

      // Add metastases to containers by diagnosis.  If there is no primary discovered, create a new "metastatic" cancer.
      addCancerMetastases( typeTumors.get( UNKNOWN ), cancerContainers, assignedNeoplasms );

      return cancerContainers;
   }




   static private void addCancerNeoplasms(
         final Collection<ConceptAggregate> neoplasms,
         final Collection<CancerContainer> cancerContainers ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return;
      }
      final Collection<ConceptAggregate> assignedNeoplasms = new HashSet<>();
      // Recursively add metastases to exiting cancer containers.
      final boolean allAssigned = addCancerNeoplasms( neoplasms, cancerContainers, assignedNeoplasms );
      if ( allAssigned ) {
         return;
      }
      // After recursion some neoplasms are still unassigned.  Create cancer containers with them as no location "primaries".
      for ( ConceptAggregate neoplasm : neoplasms ) {
         if ( assignedNeoplasms.contains( neoplasm ) ) {
            continue;
         }
         final CancerContainer noLociCancer = new CancerContainer( neoplasm );
         cancerContainers.add( noLociCancer );
      }
   }


   static private boolean addCancerNeoplasms(
         final Collection<ConceptAggregate> neoplasms,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptAggregate> assignedNeoplasms ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return true;
      }

//      LOGGER.info( "Matching " + neoplasms.size() + " Neoplasms to best of " + cancerContainers.size() + " Cancer Containers ..." );

      final Collection<ConceptAggregate> assigned = new HashSet<>();
      final Collection<CancerContainer> bestContainers = new HashSet<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         int bestRating = 0;
         bestContainers.clear();
         for ( CancerContainer container : cancerContainers ) {
            final int rating = container.rateDiagnosisMatch( neoplasm );
            if ( rating > bestRating ) {
               bestRating = rating;
               bestContainers.clear();
               bestContainers.add( container );
            } else if ( rating == bestRating ) {
               bestContainers.add( container );
            }
         }
         if ( bestRating > 0 ) {
//            LOGGER.info( "Best Diagnosis Match Rating " + bestRating + " for neoplasm " + neoplasm.getUri() + " adding to\n" +
//                         bestContainers.stream().map( Object::toString ).collect( Collectors.joining( "\n" ) ) );
            bestContainers.forEach( c -> c.addNeoplasm( neoplasm ) );
            assigned.add( neoplasm );
         }
      }
      assignedNeoplasms.addAll( assigned );
      if ( assigned.size() == neoplasms.size() ) {
         return true;
      }
      final Collection<ConceptAggregate> unassigned = new HashSet<>( neoplasms );
      unassigned.removeAll( assigned );
//      LOGGER.info( "Unassigned Neoplasms : " + unassigned.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining( "," ) )
//                   + " attempting cell match ..." );
      for ( ConceptAggregate neoplasm : unassigned ) {
         int bestRating = 0;
         bestContainers.clear();
         for ( CancerContainer container : cancerContainers ) {
            final int rating = container.rateCellMatch( neoplasm );
            if ( rating > bestRating ) {
               bestRating = rating;
               bestContainers.clear();
               bestContainers.add( container );
            } else if ( rating == bestRating ) {
               bestContainers.add( container );
            }
         }
         if ( bestRating > 0 ) {
//            LOGGER.info( "Best Cell Match Rating " + bestRating + " for neoplasm " + neoplasm.getUri() + " adding to\n" +
//                         bestContainers.stream().map( Object::toString ).collect( Collectors.joining( "\n" ) ) );
            bestContainers.forEach( c -> c.addNeoplasm( neoplasm ) );
            assigned.add( neoplasm );
         }
      }
      if ( assigned.isEmpty() ) {
         // There was no change in distribution.  Exit the recursion.
         return false;
      }
      assignedNeoplasms.addAll( assigned );
      if ( assigned.size() == neoplasms.size() ) {
         return true;
      }
      unassigned.removeAll( assigned );
      // It is possible that neoplasms added to cancer containers extended them enough to add unassigned neoplasms.
      return addCancerNeoplasms( unassigned, cancerContainers, assignedNeoplasms );
   }





   static private void createCancers(
         final Collection<ConceptAggregate> primaries,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptAggregate> assignedNeoplasms ) {


//      LOGGER.info( "Creating Cancers from Primary Tumors ..." );


      if ( primaries == null || primaries.isEmpty() ) {
//         LOGGER.info( "No Primaries." );
         return;
      }
      for ( ConceptAggregate primary : primaries ) {


//         LOGGER.info( "Creating primary cancer " + primary.getUri() + " " + primary.getId() );


         final CancerContainer primaryCancer = new CancerContainer( primary );
         cancerContainers.add( primaryCancer );
         assignedNeoplasms.add( primary );
      }
   }




   static private void createCancerContainers(
         final Collection<ConceptAggregate> neoplasms,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptAggregate> assignedNeoplasms ) {
      if ( cancerContainers.isEmpty() ) {
         // There were no primaries.  Attempt to best candidates as primaries
         addOwnDiagnosisPrimaries( neoplasms, cancerContainers, assignedNeoplasms );
         if ( cancerContainers.isEmpty() ) {
            addMostMentionPrimaries( neoplasms, cancerContainers, assignedNeoplasms );
         }
      }
   }


   static private boolean addOwnDiagnosisPrimaries( final Collection<ConceptAggregate> neoplasms,
                                                    final Collection<CancerContainer> cancerContainers,
                                                    final Collection<ConceptAggregate> assignedNeoplasms ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return false;
      }
      int assignedCount = 0;
      for ( ConceptAggregate neoplasm : neoplasms ) {
         if ( assignedNeoplasms.contains( neoplasm ) ) {
            continue;
         }
         final Collection<ConceptAggregate> diagnoses = neoplasm.getRelated( HAS_DIAGNOSIS );
         // For some reason Collection.contains(..) does not work here !!
         if ( diagnoses.stream().anyMatch( neoplasm::equals ) ) {


//            LOGGER.info( "Creating Primary cancer based upon Tumor diagnosed as self (it is a cancer) " + neoplasm.getUri() +  " " + neoplasm.getId() );
//            LOGGER.info( "Own Diagnosis (" +
//                         diagnoses.stream().map( c -> c.getUri() + " " + c.getId() ).collect( Collectors.joining( "," ) ) + ")" );


            final CancerContainer primaryCancer = new CancerContainer( neoplasm );
            cancerContainers.add( primaryCancer );
            assignedNeoplasms.add( neoplasm );
            assignedCount++;
         }
      }
      return assignedCount == neoplasms.size();
   }

   static private boolean addMostMentionPrimaries( final Collection<ConceptAggregate> neoplasms,
                                                   final Collection<CancerContainer> cancerContainers,
                                                   final Collection<ConceptAggregate> assignedNeoplasms ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return false;
      }
      ConceptAggregate mostPrimary = null;
      int mostMentions = 0;
      int mostRelated = 0;
      for ( ConceptAggregate neoplasm : neoplasms ) {
         if ( assignedNeoplasms.contains( neoplasm ) ) {
            continue;
         }
         int mentionCount = neoplasm.getMentions().size();
         if ( mentionCount > mostMentions
              || (mentionCount == mostMentions && neoplasm.getRelatedConceptMap().size() >= mostRelated) ) {
            mostPrimary = neoplasm;
            mostMentions = mentionCount;
            mostRelated = neoplasm.getRelatedConceptMap().size();
         }
      }



//      if ( mostPrimary != null ) {
//         LOGGER.info(
//               "Creating Cancer " + mostPrimary.getUri() + " " + mostPrimary.getId() + " based upon it having the most Mentions "
//               + mostMentions + " and most related " + mostRelated );
//      }



      final CancerContainer primaryCancer = new CancerContainer( mostPrimary );
      cancerContainers.add( primaryCancer );
      assignedNeoplasms.add( mostPrimary );
      return neoplasms.size() == 1;
   }



   static private void addCancerMetastases(
         final Map<ConceptAggregate.NeoplasmType,Collection<ConceptAggregate>> typeTumors,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptAggregate> assignedNeoplasms ) {
      final Collection<ConceptAggregate> metastases = typeTumors.get( SECONDARY );
      if ( metastases == null ) {
         return;
      }
      // Recursively add metastases to exiting cancer containers.
      final boolean allAssigned = addCancerMetastases( metastases, cancerContainers, assignedNeoplasms );
      if ( allAssigned ) {
         return;
      }
      // After recursion some metastases may still be unassigned.  Create cancer containers with them as primaries.
      for ( ConceptAggregate metastasis : metastases ) {
         if ( assignedNeoplasms.contains( metastasis ) ) {
            continue;
         }
//         LOGGER.info( "No cancer assignable for " + metastasis.getUri() + " " + metastasis.getId() + " creating cancer container for it." );
         final CancerContainer metastaticCancer = new CancerContainer( metastasis, true );
         cancerContainers.add( metastaticCancer );
         assignedNeoplasms.add( metastasis );
      }
   }


   static private boolean addCancerMetastases(
         final Collection<ConceptAggregate> metastases,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptAggregate> assignedNeoplasms ) {
      if ( metastases == null || metastases.isEmpty() ) {
         return true;
      }
      final Collection<ConceptAggregate> assigned = new HashSet<>();
      final Collection<CancerContainer> bestContainers = new HashSet<>();
      for ( ConceptAggregate metastasis : metastases ) {
         int bestRating = 0;
         bestContainers.clear();
         for ( CancerContainer container : cancerContainers ) {
//            LOGGER.warn( "Metastasis: " + metastasis );
            final int rating = container.rateDiagnosisMatch( metastasis );
            if ( rating > bestRating ) {
               bestRating = rating;
               bestContainers.clear();
               bestContainers.add( container );
            } else if ( rating == bestRating ) {
               bestContainers.add( container );
            }
         }
         if ( bestRating > 0 ) {
//            LOGGER.info( "Best Rating 1 " + bestRating + " for " + metastasis.getUri() + " adding to " +
//                         bestContainers.stream()
//                                       .map( CancerContainer::getPrimary )
//                                       .map( ConceptInstance::getUri )
//                                       .collect( Collectors.joining( " " ) ) );
            bestContainers.forEach( c -> c.addMetastasis( metastasis ) );
            assigned.add( metastasis );
         }
      }
      assignedNeoplasms.addAll( assigned );
      if ( assigned.size() == metastases.size() ) {
         return true;
      }
      final Collection<ConceptAggregate> unassigned = new HashSet<>( metastases );
      unassigned.removeAll( assigned );
      for ( ConceptAggregate metastasis : unassigned ) {
         int bestRating = 0;
         bestContainers.clear();
         for ( CancerContainer container : cancerContainers ) {
            final int rating = container.rateCellMatch( metastasis );
            if ( rating > bestRating ) {
               bestRating = rating;
               bestContainers.clear();
               bestContainers.add( container );
            } else if ( rating == bestRating ) {
               bestContainers.add( container );
            }
         }
         if ( bestRating > 0 ) {
//            LOGGER.info( "Best Rating 2 " + bestRating + " for " + metastasis.getUri() + " adding to " +
//                         bestContainers.stream()
//                                       .map( CancerContainer::getPrimary )
//                                       .map( ConceptInstance::getUri )
//                                       .collect( Collectors.joining() ) );
            bestContainers.forEach( c -> c.addMetastasis( metastasis ) );
            assigned.add( metastasis );
         }
      }
      if ( assigned.isEmpty() ) {
         // There was no change in distribution.  Exit the recursion.
         return false;
      }
      unassigned.removeAll( assigned );
      assignedNeoplasms.addAll( assigned );
      if ( assigned.size() == metastases.size() ) {
         return true;
      }
      // It is possible that metastases added to cancer containers extended them enough to add unassigned metastases.
      return addCancerMetastases( unassigned, cancerContainers, assignedNeoplasms );
   }




   /**
    * Merge all of the tumor instances and their cancer diagnoses into a single neoplasm.
    * !!! Note !!!   This undoes previous concept separations, including those based upon site and laterality.
    *
    * @param patientId    -
    * @param diagnosisMap -
    * @param allConcepts -
    * @return -
    */
   static public ConceptAggregate createNaaccrCancer( final String patientId,
                                                      final Map<ConceptAggregate, Collection<ConceptAggregate>> diagnosisMap,
                                                      final Collection<ConceptAggregate> allConcepts ) {
      final Collection<ConceptAggregate> naaccrNeoplasms = new HashSet<>( diagnosisMap.keySet() );
      diagnosisMap.values().forEach( naaccrNeoplasms::addAll );

//      final String bestUri = UriUtil.getMostSpecificUri( naaccrNeoplasms.stream().map( ConceptAggregate::getUri ).collect( Collectors.toSet() ) );
//      LOGGER.error( "\n                                                 BestUri " + bestUri + "\n" );


      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
      naaccrNeoplasms.stream()
                     .map( ConceptAggregate::getUriRootsMap )
                     .map( Map::entrySet )
                     .flatMap( Collection::stream )
                     .forEach( e -> allUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() ).addAll( e.getValue() ) );
      final Map<String, Collection<Mention>> docMentions = ConceptAggregateHandler.collectDocMentions( naaccrNeoplasms );
      final ConceptAggregate naaccrNeoplasm = new DefaultConceptAggregate( patientId, allUriRoots, docMentions );
//            = createConceptInstance( patientId, bestUri, docAnnotations );


//      LOGGER.error( "Created new Naaccr Cancer with URI " + naaccrNeoplasm.getUri() + " " + naaccrNeoplasm.getId() + " uri score: " + naaccrNeoplasm.getUriScore() );


      final Map<String, Collection<ConceptAggregate>> neoplasmRelations = new HashMap<>();

      for ( ConceptAggregate concept : allConcepts ) {
         final boolean isNaaccr = naaccrNeoplasms.contains( concept );
         final Map<String, Collection<ConceptAggregate>> oldRelations = concept.getRelatedConceptMap();
         final Map<String, Collection<ConceptAggregate>> newRelations = new HashMap<>( oldRelations.size() );
         for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelation : oldRelations.entrySet() ) {
            if ( isNaaccr ) {
               for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
                  final Collection<ConceptAggregate> related
                        = neoplasmRelations.computeIfAbsent( oldRelation.getKey(), r -> new HashSet<>() );
                  if ( naaccrNeoplasms.contains( oldConcept ) ) {
                     related.add( naaccrNeoplasm );
                  } else {
                     related.add( oldConcept );
                  }
               }
               continue;
            }
            final Collection<ConceptAggregate> newRelated = new HashSet<>();
            for ( ConceptAggregate oldConcept : oldRelation.getValue() ) {
               if ( naaccrNeoplasms.contains( oldConcept ) ) {
                  newRelated.add( naaccrNeoplasm );
               } else {
                  newRelated.add( oldConcept );
               }
            }
            newRelations.put( oldRelation.getKey(), newRelated );
         }
         concept.setRelated( newRelations );
      }
      naaccrNeoplasm.setRelated( neoplasmRelations );
      allConcepts.removeAll( naaccrNeoplasms );
      allConcepts.add( naaccrNeoplasm );

//      LOGGER.info( "createNaaccrCancer : NAACCR NEOPLASM\n" + naaccrNeoplasm );

      return naaccrNeoplasm;
   }







}
