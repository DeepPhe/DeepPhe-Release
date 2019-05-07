package org.apache.ctakes.cancer.concept.instance;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.cancer.concept.instance.ConceptInstanceUtil.NeoplasmType.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/28/2019
 */
@Immutable
final public class NeoplasmMerger {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmMerger" );

   private NeoplasmMerger() {}

   static public Map<ConceptInstance,Collection<ConceptInstance>> mergeNeoplasms(
         final Collection<ConceptInstance> neoplasms,
         final Collection<ConceptInstance> allInstances ) {
      final Collection<ConceptInstance> tumors = mergeTumors( neoplasms, allInstances );

      final Map<ConceptInstance,Collection<ConceptInstance>> cancerMap = mergeCancers( neoplasms, allInstances, tumors );

      for ( Collection<ConceptInstance> cancerTumors : cancerMap.values() ) {
         for ( ConceptInstance cancerTumor : cancerTumors ) {
            final Map<String,Collection<ConceptInstance>> related = cancerTumor.getRelated();
            final Collection<String> removals = related.keySet().stream()
                                                       .filter( ConceptInstanceUtil::isCancerOnlyFact )
                                                       .collect( Collectors.toList() );
            related.keySet().removeAll( removals );
         }
      }

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
    * @param allInstances all instances for the patient / doc.
    * @return a new collection of (possibly) merged neoplasms.
    */
   static private Collection<ConceptInstance> mergeTumors( final Collection<ConceptInstance> neoplasms,
                                                          final Collection<ConceptInstance> allInstances ) {
      if ( neoplasms.isEmpty() ) {
         return neoplasms;
      }
      if ( neoplasms.size() == 1 ) {
         final Collection<ConceptInstance> candidateLocations
               = new ArrayList<>( neoplasms ).get( 0 ).getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            return Collections.emptyList();
         }
         return neoplasms;
      }
      final Collection<ConceptInstance> assignedNeoplasms = new HashSet<>();
      final Collection<TumorContainer> tumorContainers = new HashSet<>();
      // Combine masses on the same site.  Use Laterality.
      mergeLateralTumors( neoplasms, assignedNeoplasms, tumorContainers );
      // Combine masses on the same site.  Laterality already used.
      mergeNonLateralTumors( neoplasms, assignedNeoplasms, tumorContainers );

      neoplasms.removeAll( assignedNeoplasms );
      // Now we have created TumorContainers.  Create a Merged ConceptInstance from each.
      final Collection<String> tumorIds = new HashSet<>();
      for ( TumorContainer tumor : tumorContainers ) {
         tumorIds.add( tumor.createTumor( allInstances ).getId() );
      }

      for ( ConceptInstance instance : allInstances ) {
         final Map<String,Collection<ConceptInstance>> oldRelated = instance.getRelated();
         for ( Map.Entry<String,Collection<ConceptInstance>> oldRelations : oldRelated.entrySet() ) {
            final Collection<ConceptInstance> removals = new HashSet<>();
            for ( ConceptInstance instance1 : oldRelations.getValue() ) {
               if ( !allInstances.contains( instance1 ) ) {
                  removals.add( instance1 );
               }
            }
            oldRelations.getValue().removeAll( removals );
         }
      }

      final Collection<ConceptInstance> tumorInstances = new HashSet<>( tumorIds.size() );
      for ( ConceptInstance instance : allInstances ) {
         if ( tumorIds.contains( instance.getId() ) ) {
            tumorInstances.add( instance );
         }
      }
      return tumorInstances;
   }


   static private void mergeLateralTumors( final Collection<ConceptInstance> neoplasms,
                                          final Collection<ConceptInstance> assignedNeoplasms,
                                          final Collection<TumorContainer> tumorContainers ) {
      if ( neoplasms.size() <= 1 ) {
         return;
      }
      // Combine masses on the same site.  Use Laterality.
      for ( ConceptInstance neoplasm : neoplasms ) {
         final Collection<ConceptInstance> lateralities
               = neoplasm.getRelated().get( RelationConstants.HAS_LATERALITY );
         if ( lateralities == null || lateralities.isEmpty() ) {
            continue;
         }
         final Collection<ConceptInstance> candidateLocations
               = neoplasm.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            continue;
         }
         boolean assigned = false;
         for ( TumorContainer container : tumorContainers ) {
            if ( container.isLocationMatch( neoplasm )
                 && container.hasRelated( RelationConstants.HAS_LATERALITY, neoplasm ) ) {
               container.addNeoplasm( neoplasm );
               assigned = true;
            }
         }
         if ( !assigned ) {
            tumorContainers.add( new TumorContainer( neoplasm ) );
         }
         assignedNeoplasms.add( neoplasm );
      }
   }


   static private void mergeNonLateralTumors(
         final Collection<ConceptInstance> neoplasms,
         final Collection<ConceptInstance> assignedNeoplasms,
         final Collection<TumorContainer> tumorContainers ) {
      if ( neoplasms.size() <= 1 ) {
         return;
      }
      // Combine masses on the same site.  Laterality already used.
      for ( ConceptInstance neoplasm : neoplasms ) {
         if ( assignedNeoplasms.contains( neoplasm ) ) {
            continue;
         }
         final Collection<ConceptInstance> candidateLocations
               = neoplasm.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( candidateLocations == null || candidateLocations.isEmpty() ) {
            // no location to use for orphan.
            continue;
         }
         boolean assigned = false;
         for ( TumorContainer container : tumorContainers ) {
            if ( container.isLocationMatch( neoplasm ) ) {
               container.addNeoplasm( neoplasm );
               assigned = true;
            }
         }
         if ( !assigned ) {
            tumorContainers.add( new TumorContainer( neoplasm ) );
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
   static private Map<ConceptInstance,Collection<ConceptInstance>> mergeCancers(
         final Collection<ConceptInstance> neoplasms,
         final Collection<ConceptInstance> allInstances,
         final Collection<ConceptInstance> tumors ) {

      final Map<ConceptInstanceUtil.NeoplasmType,Collection<ConceptInstance>> typeTumors
            = new EnumMap<>( ConceptInstanceUtil.NeoplasmType.class );
      tumors.forEach( t -> typeTumors.computeIfAbsent( ConceptInstanceUtil.getNeoplasmType( t ),
            l -> new HashSet<>() ).add( t ) );

      // Do tumors first so that tumor duplication is less frequent.
      final Collection<CancerContainer> cancerContainers = mergeCancerTumors( typeTumors );
      // Combine neoplasms by diagnosis.  Do non-tumor neoplasms second as duplication is possibly better.
      addCancerNeoplasms( neoplasms, cancerContainers );

      createCancers( typeTumors.get( NON_CANCER ), cancerContainers, new HashSet<>() );

      final Map<ConceptInstance,Collection<ConceptInstance>> cancerTumorMap = new HashMap<>( cancerContainers.size() );
      for ( CancerContainer container : cancerContainers ) {
         final ConceptInstance cancer = container.createCancer( allInstances );
         cancerTumorMap.put( cancer, container.getAllTumors() );
      }

      // Shouldn't this get an "assigned" and if not then the primary gets its own container?
      return cancerTumorMap;
   }


   static private void createCancers(
         final Collection<ConceptInstance> primaries,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptInstance> assignedNeoplasms ) {
      if ( primaries == null || primaries.isEmpty() ) {
         return;
      }
      for ( ConceptInstance primary : primaries ) {
//         LOGGER.info( "Creating primary cancer " + primary.getUri() );
         final CancerContainer primaryCancer = new CancerContainer( primary );
         cancerContainers.add( primaryCancer );
         assignedNeoplasms.add( primary );
      }
   }

   static private void createBenignCancers(
         final Collection<ConceptInstance> benigns,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptInstance> assignedNeoplasms ) {
      if ( benigns == null || benigns.isEmpty() ) {
         return;
      }
      for ( ConceptInstance benign : benigns ) {
//         LOGGER.info( "Creating benign cancer " + benign.getUri() );
         final CancerContainer benignCancer = new CancerContainer( benign );
         cancerContainers.add( benignCancer );
         assignedNeoplasms.add( benign );
      }
   }


   static private void addCancerMetastases(
         final Map<ConceptInstanceUtil.NeoplasmType,Collection<ConceptInstance>> typeTumors,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptInstance> assignedNeoplasms ) {
      final Collection<ConceptInstance> metastases = typeTumors.get( SECONDARY );
      if ( metastases == null ) {
         return;
      }
      // Recursively add metastases to exiting cancer containers.
      final boolean allAssigned = addCancerMetastases( metastases, cancerContainers, assignedNeoplasms );
      if ( allAssigned ) {
         return;
      }
      // After recursion some metastases may still be unassigned.  Create cancer containers with them as primaries.
      for ( ConceptInstance metastasis : metastases ) {
         if ( assignedNeoplasms.contains( metastasis ) ) {
            continue;
         }
         final CancerContainer metastaticCancer = new CancerContainer( metastasis, true );
         cancerContainers.add( metastaticCancer );
         assignedNeoplasms.add( metastasis );
      }
   }

   static private boolean addCancerMetastases(
         final Collection<ConceptInstance> metastases,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptInstance> assignedNeoplasms ) {
      if ( metastases == null || metastases.isEmpty() ) {
         return true;
      }
      final Collection<ConceptInstance> assigned = new HashSet<>();
      final Collection<CancerContainer> bestContainers = new HashSet<>();
      for ( ConceptInstance metastasis : metastases ) {
         int bestRating = 0;
         bestContainers.clear();
         for ( CancerContainer container : cancerContainers ) {
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
            bestContainers.forEach( c -> c.addMetastasis( metastasis ) );
            assigned.add( metastasis );
         }
      }
      assignedNeoplasms.addAll( assigned );
      if ( assigned.size() == metastases.size() ) {
         return true;
      }
      final Collection<ConceptInstance> unassigned = new HashSet<>( metastases );
      unassigned.removeAll( assigned );
      for ( ConceptInstance metastasis : unassigned ) {
         int bestRating = 0;
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
            bestContainers.forEach( c -> c.addMetastasis( metastasis ) );
            assigned.add( metastasis );
         }
      }
      if ( assigned.isEmpty() ) {
         // There was no change in distribution.  Exit the recursion.
         return false;
      }
      assignedNeoplasms.addAll( assigned );
      if ( assigned.size() == metastases.size() ) {
         return true;
      }
      // It is possible that metastases added to cancer containers extended them enough to add unassigned metastases.
      return addCancerMetastases( unassigned, cancerContainers, assignedNeoplasms );
   }


   static private boolean addOwnDiagnosisPrimaries( final Collection<ConceptInstance> neoplasms,
                                                    final Collection<CancerContainer> cancerContainers,
                                                    final Collection<ConceptInstance> assignedNeoplasms ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return false;
      }
      int assignedCount = 0;
      for ( ConceptInstance neoplasm : neoplasms ) {
         final Collection<ConceptInstance> diagnoses = ConceptInstanceUtil.getRelated( HAS_DIAGNOSIS, neoplasm );
         // For some reason Collection.contains(..) does not work here !!
         if ( diagnoses.stream().anyMatch( neoplasm::equals ) ) {
//            LOGGER.info( "Creating own diagnosis primary cancer " + neoplasm.getUri() );
            final CancerContainer primaryCancer = new CancerContainer( neoplasm );
            cancerContainers.add( primaryCancer );
            assignedNeoplasms.add( neoplasm );
            assignedCount++;
         }
      }
      return assignedCount == neoplasms.size();
   }

   static private boolean addMostAnnotationPrimaries( final Collection<ConceptInstance> neoplasms,
                                                      final Collection<CancerContainer> cancerContainers,
                                                      final Collection<ConceptInstance> assignedNeoplasms ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return false;
      }
      ConceptInstance mostPrimary = null;
      int mostAnnotations = 0;
      int mostRelated = 0;
      for ( ConceptInstance neoplasm : neoplasms ) {
         int annotationCount = neoplasm.getAnnotations().size();
         if ( annotationCount > mostAnnotations
              || (annotationCount == mostAnnotations && neoplasm.getRelated().size() >= mostRelated) ) {
            mostPrimary = neoplasm;
            mostAnnotations = annotationCount;
            mostRelated = neoplasm.getRelated().size();
         }
      }
//      LOGGER.info( "Creating most annotation primary cancer " + mostPrimary.getUri() );
      final CancerContainer primaryCancer = new CancerContainer( mostPrimary );
      cancerContainers.add( primaryCancer );
      assignedNeoplasms.add( mostPrimary );
      return neoplasms.size() == 1;
   }

   // TODO for a metastasis located in X with diagnosis Y, assign to any cancers with URI Y or diagnosis Y.
   //  ----> It should be doing this.  What is up?

   static private Collection<CancerContainer> mergeCancerTumors(
         final Map<ConceptInstanceUtil.NeoplasmType,Collection<ConceptInstance>> typeTumors ) {
      if ( typeTumors.isEmpty() ) {
         return new HashSet<>();
      }
      final Collection<CancerContainer> cancerContainers = new HashSet<>();
      final Collection<ConceptInstance> assignedNeoplasms = new HashSet<>();

      // Create Containers with primary tumors.  Do tumors first so that duplication is less frequent.
      createCancers( typeTumors.get( PRIMARY ), cancerContainers, assignedNeoplasms );

      if ( cancerContainers.isEmpty() ) {
         // There were no primaries.  Attempt to best unknown candidates as primaries
         addOwnDiagnosisPrimaries( typeTumors.get( UNKNOWN ), cancerContainers, assignedNeoplasms );
         if ( cancerContainers.isEmpty() ) {
            addMostAnnotationPrimaries( typeTumors.get( UNKNOWN ), cancerContainers, assignedNeoplasms );
         }
      }

      if ( cancerContainers.isEmpty() ) {
         // There were no primaries.  Attempt to best metastasis candidates as primaries
         addOwnDiagnosisPrimaries( typeTumors.get( SECONDARY ), cancerContainers, assignedNeoplasms );
         if ( cancerContainers.isEmpty() ) {
            addMostAnnotationPrimaries( typeTumors.get( SECONDARY ), cancerContainers, assignedNeoplasms );
         }
      }

      // Add metastases to containers by diagnosis.  If there is no primary discovered, create a new "metastatic" cancer.
      addCancerMetastases( typeTumors, cancerContainers, assignedNeoplasms );

      // Add metastases to containers by diagnosis.  If there is no primary discovered, create a new "metastatic" cancer.
      addCancerMetastases( typeTumors.get( UNKNOWN ), cancerContainers, assignedNeoplasms );

      return cancerContainers;
   }



   static private void addCancerNeoplasms(
         final Collection<ConceptInstance> neoplasms,
         final Collection<CancerContainer> cancerContainers ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return;
      }
      final Collection<ConceptInstance> assignedNeoplasms = new HashSet<>();
      // Recursively add metastases to exiting cancer containers.
      final boolean allAssigned = addCancerNeoplasms( neoplasms, cancerContainers, assignedNeoplasms );
      if ( allAssigned ) {
         return;
      }
      // After recursion some neoplasms are still unassigned.  Create cancer containers with them as no location "primaries".
      for ( ConceptInstance neoplasm : neoplasms ) {
         if ( assignedNeoplasms.contains( neoplasm ) ) {
            continue;
         }
         final CancerContainer noLociCancer = new CancerContainer( neoplasm );
         cancerContainers.add( noLociCancer );
      }
   }

   static private boolean addCancerNeoplasms(
         final Collection<ConceptInstance> neoplasms,
         final Collection<CancerContainer> cancerContainers,
         final Collection<ConceptInstance> assignedNeoplasms ) {
      if ( neoplasms == null || neoplasms.isEmpty() ) {
         return true;
      }
      final Collection<ConceptInstance> assigned = new HashSet<>();
      for ( ConceptInstance neoplasm : neoplasms ) {
         for ( CancerContainer container : cancerContainers ) {
            if ( container.isDiagnosisMatch( neoplasm ) ) {
               container.addNeoplasm( neoplasm );
               assigned.add( neoplasm );
            }
         }
      }
      assignedNeoplasms.addAll( assigned );
      if ( assigned.size() == neoplasms.size() ) {
         return true;
      }
      final Collection<ConceptInstance> unassigned = new HashSet<>( neoplasms );
      unassigned.removeAll( assigned );
      for ( ConceptInstance neoplasm : unassigned ) {
         for ( CancerContainer container : cancerContainers ) {
            if ( container.isCellMatch( neoplasm ) && container.isStageMatch( neoplasm )  ) {
               container.addNeoplasm( neoplasm );
               assigned.add( neoplasm );
            }
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






   /////////////////////////////////////////////////////////////////////////
   //
   //             Tumor Container
   //
   /////////////////////////////////////////////////////////////////////////


   static private class TumorContainer extends AbstractContainer {
      final Collection<ConceptInstance> _neoplasms = new HashSet<>();
      final private Map<String,Collection<String>> _relatedUris = new HashMap<>();

      private TumorContainer( final ConceptInstance neoplasm ) {
         addNeoplasm( neoplasm );
      }

      protected Collection<String> getRelatedUris( final String relationName ) {
         final Collection<String> related = _relatedUris.get( relationName );
         if ( related == null ) {
            return Collections.emptyList();
         }
         return related;
      }

      private boolean isLocationMatch( final ConceptInstance neoplasm ) {
         if ( hasRelated( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE, neoplasm ) ) {
            return true;
         }
         final Collection<ConceptInstance> locations
               = neoplasm.getRelated().get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( locations == null || locations.isEmpty() ) {
            return false;
         }
         final Collection<String> locationUris = _relatedUris.get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         for ( String weUri : locationUris ) {
            for ( ConceptInstance location : locations ) {
               final String theyUri = location.getUri();
               if ( UriUtil.getCloseUriLeaf( weUri, theyUri ) != null ) {
                  return true;
               }
            }
         }
         return false;
      }

      private void addNeoplasm( final ConceptInstance neoplasm ) {
         _neoplasms.add( neoplasm );
         final Map<String,Collection<ConceptInstance>> related = neoplasm.getRelated();
         for ( Map.Entry<String,Collection<ConceptInstance>> relation : related.entrySet() ) {
            final Collection<String> relationUris
                  = ConceptInstanceUtil.getUris( relation.getValue() );
            final Collection<String> relatedUris = _relatedUris.get( relation.getKey() );
            if ( relatedUris == null ) {
               _relatedUris.put( relation.getKey(), relationUris );
            } else {
               relatedUris.addAll( relationUris );
            }
         }
      }

      private ConceptInstance createTumor( final Collection<ConceptInstance> allInstances ) {
         if ( _neoplasms.size() == 1 ) {
            return new ArrayList<>( _neoplasms ).get( 0 );
         }
         final String patientId = new ArrayList<>( _neoplasms ).get( 0 ).getPatientId();
         final String bestUri = UriUtil.getMostSpecificUri( ConceptInstanceUtil.getUris( _neoplasms ) );
         final Map<String,Collection<IdentifiedAnnotation>> allDocAnnotations = new HashMap<>();
         final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>();
         for ( ConceptInstance neoplasm : _neoplasms ) {
            final Map<String,Collection<IdentifiedAnnotation>> docAnnotations = neoplasm.getDocAnnotations();
            for ( Map.Entry<String,Collection<IdentifiedAnnotation>> docAnnotation : docAnnotations.entrySet() ) {
               allDocAnnotations.computeIfAbsent( docAnnotation.getKey(), d -> new HashSet<>() )
                                .addAll( docAnnotation.getValue() );
            }
            final Map<String,Collection<ConceptInstance>> related = neoplasm.getRelated();
            for ( Map.Entry<String,Collection<ConceptInstance>> relation : related.entrySet() ) {
               allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() ).addAll( relation.getValue() );
            }
         }
         final ConceptInstance merge = new CorefConceptInstance( patientId, bestUri, allDocAnnotations );
         merge.setRelated( allRelated );
         updateAllInstances( allInstances, _neoplasms, merge );
         return merge;
      }
   }





   /////////////////////////////////////////////////////////////////////////
   //
   //             Cancer Container
   //
   /////////////////////////////////////////////////////////////////////////


   static private class CancerContainer extends AbstractContainer {
      final private ConceptInstance _primary;
      final private boolean _isMetastatic;
      final private Collection<ConceptInstance> _metastases = new HashSet<>();
      final private Collection<ConceptInstance> _neoplasms = new HashSet<>();
      final private Map<String,Collection<String>> _nonSiteRelatedUris = new HashMap<>();

      private CancerContainer( final ConceptInstance primary ) {
         this( primary, false );
      }

      private CancerContainer( final ConceptInstance primary, final boolean isMetastatic ) {
         _primary = primary;
         addRelatedUris( primary );
         _isMetastatic = isMetastatic;
      }

      protected Collection<String> getRelatedUris( final String relationName ) {
         final Collection<String> related = _nonSiteRelatedUris.get( relationName );
         if ( related == null ) {
            return Collections.emptyList();
         }
         return related;
      }

      /**
       *
       * @param metastasis has a site
       */
      private void addMetastasis( final ConceptInstance metastasis ) {
//         LOGGER.info( "Adding metastasis " +  metastasis.getUri() + " to " + getPrimary().getUri() );
         _metastases.add( metastasis );
         addRelatedUris( metastasis );
      }

      private void addNeoplasm( final ConceptInstance neoplasm ) {
//         LOGGER.info( "Adding neoplasm " +  neoplasm.getUri() + " to " + getPrimary().getUri() );
         _neoplasms.add( neoplasm );
         addRelatedUris( neoplasm );
      }

       private void addRelatedUris( final ConceptInstance neoplasm ) {
         final Map<String,Collection<ConceptInstance>> nonSiteRelated = ConceptInstanceUtil.getNonLocationFacts( neoplasm );
         for ( Map.Entry<String,Collection<ConceptInstance>> relation : nonSiteRelated.entrySet() ) {
            final Collection<String> relationUris = ConceptInstanceUtil.getUris( relation.getValue() );
            final Collection<String> relatedUris = _nonSiteRelatedUris.get( relation.getKey() );
            if ( relatedUris != null ) {
               relatedUris.addAll( relationUris );
            } else {
               _nonSiteRelatedUris.put( relation.getKey(), relationUris );
            }
         }
      }

      private Collection<ConceptInstance> getAllTumors() {
         final Collection<ConceptInstance> tumors = new HashSet<>( _metastases );
         tumors.add( _primary );
         return tumors;
      }

      /**
       * compares main uris to diagnoses to stages.
       * @param neoplasm -
       * @return -
       */
      private boolean isDiagnosisMatch( final ConceptInstance neoplasm ) {
         final Collection<String> containerUris = _neoplasms.stream()
                                                            .map( ConceptInstance::getUri )
                                                            .collect( Collectors.toSet() );
         containerUris.add( _primary.getUri() );
         containerUris.addAll( getRelatedUris( RelationConstants.HAS_DIAGNOSIS ) );
         final Collection<String> neoplasmUris = new HashSet<>();
         neoplasmUris.add( neoplasm.getUri() );
         neoplasmUris.addAll( ConceptInstanceUtil.getRelatedUris( RelationConstants.HAS_DIAGNOSIS, neoplasm ) );
         if ( neoplasmUris.stream().anyMatch( containerUris::contains ) ) {
            return true;
         }
         if ( containerUris.stream().anyMatch( neoplasmUris::contains ) ) {
            return true;
         }
         final Collection<String> containerBranch = containerUris.stream()
                                                                 .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                                 .flatMap( Collection::stream )
                                                                 .collect( Collectors.toSet() );
         if ( neoplasmUris.stream().anyMatch( containerBranch::contains ) ) {
            return true;
         }
         final Collection<String> neoplasmBranch = neoplasmUris.stream()
                                                               .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                               .flatMap( Collection::stream )
                                                               .collect( Collectors.toSet() );
         if ( containerUris.stream().anyMatch( neoplasmBranch::contains ) ) {
            return true;
         }
         return false;
      }

      /**
       * compares main uris to diagnoses to stages.
       * @param neoplasm -
       * @return -
       */
      private int rateDiagnosisMatch( final ConceptInstance neoplasm ) {
         final Collection<String> containerUris = new HashSet<>();
         containerUris.add( _primary.getUri() );

         final Collection<String> neoplasmUris
               = new HashSet<>( ConceptInstanceUtil.getRelatedUris( RelationConstants.HAS_DIAGNOSIS, neoplasm ) );
         if ( neoplasmUris.stream().anyMatch( containerUris::contains ) ) {
            return 10;
         }

         _neoplasms.stream()
                   .map( ConceptInstance::getUri )
                   .forEach( containerUris::add );
         if ( neoplasmUris.stream().anyMatch( containerUris::contains ) ) {
            return 9;
         }

         containerUris.addAll( getRelatedUris( RelationConstants.HAS_DIAGNOSIS ) );
         neoplasmUris.add( neoplasm.getUri() );
         if ( neoplasmUris.stream().anyMatch( containerUris::contains ) ) {
            return 7;
         }

         if ( containerUris.stream().anyMatch( neoplasmUris::contains ) ) {
            return 5;
         }
         final Collection<String> containerBranch = containerUris.stream()
                                                                 .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                                 .flatMap( Collection::stream )
                                                                 .collect( Collectors.toSet() );
         if ( neoplasmUris.stream().anyMatch( containerBranch::contains ) ) {
            return 3;
         }
         final Collection<String> neoplasmBranch = neoplasmUris.stream()
                                                               .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                               .flatMap( Collection::stream )
                                                               .collect( Collectors.toSet() );
         if ( containerUris.stream().anyMatch( neoplasmBranch::contains ) ) {
            return 1;
         }
//         return isTnmMatch( neoplasm );
         return -1;
      }

      private boolean isStageMatch( final ConceptInstance neoplasm ) {
         return CancerMatchUtil.isStageMatch( neoplasm, Collections.singletonList( _primary ) );
      }

      private boolean isTnmMatch( final ConceptInstance neoplasm ) {
         CancerMatchUtil.MatchType match = CancerMatchUtil.getTnmMatchType( neoplasm, Collections.singletonList( _primary ) );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         match = CancerMatchUtil.getTnmMatchType( neoplasm, _neoplasms );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         match = CancerMatchUtil.getTnmMatchType( neoplasm, _metastases );
         return match == CancerMatchUtil.MatchType.MATCH;
      }

      private boolean isCellMatch( final ConceptInstance neoplasm ) {
         CancerMatchUtil.MatchType match = CancerMatchUtil.getHistologyMatchType( neoplasm, Collections.singletonList( _primary ) );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         boolean empty = match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getHistologyMatchType( neoplasm, _metastases );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getHistologyMatchType( neoplasm, _neoplasms );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, Collections.singletonList( _primary ) );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _metastases );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _neoplasms );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return true;
         }
         return empty && match == CancerMatchUtil.MatchType.EMPTY;
      }

      private int rateCellMatch( final ConceptInstance neoplasm ) {
         CancerMatchUtil.MatchType match = CancerMatchUtil.getHistologyMatchType( neoplasm, Collections.singletonList( _primary ) );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return 10;
         }
         boolean empty = match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getHistologyMatchType( neoplasm, _metastases );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return 8;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getHistologyMatchType( neoplasm, _neoplasms );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return 6;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, Collections.singletonList( _primary ) );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return 4;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _metastases );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return 3;
         }
         empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

         match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _neoplasms );
         if ( match == CancerMatchUtil.MatchType.MATCH ) {
            return 2;
         }
         if ( empty && match == CancerMatchUtil.MatchType.EMPTY ) {
            return 1;
         }
         return -1;
      }

      private Collection<ConceptInstance> getMetastases() {
         return _metastases;
      }

      private ConceptInstance getPrimary() {
         return _primary;
      }


      private ConceptInstance createCancer( final Collection<ConceptInstance> allInstances ) {
         final ConceptInstance primary = getPrimary();
         String patientId = primary.getPatientId();

         final Collection<ConceptInstance> cancerContributors = new HashSet<>( _neoplasms );
         final Map<String,Collection<IdentifiedAnnotation>> allDocAnnotations = new HashMap<>();
         // Best URI for Cancer derived from primary and all non-sited neoplasms.  Don't use metastasis uris.
         final Map<String,Collection<ConceptInstance>> allRelated = new HashMap<>();
         for ( ConceptInstance neoplasm : cancerContributors ) {
            final Map<String,Collection<IdentifiedAnnotation>> docAnnotations = neoplasm.getDocAnnotations();
            for ( Map.Entry<String,Collection<IdentifiedAnnotation>> docAnnotation : docAnnotations.entrySet() ) {
               allDocAnnotations.computeIfAbsent( docAnnotation.getKey(), d -> new HashSet<>() )
                                .addAll( docAnnotation.getValue() );
            }
            final Map<String,Collection<ConceptInstance>> related = neoplasm.getRelated();
            for ( Map.Entry<String,Collection<ConceptInstance>> relation : related.entrySet() ) {
               final String relationName = relation.getKey();
//               LOGGER.info( relationName + " for cancer ... " );
               if ( !ConceptInstanceUtil.isTumorOnlyFact( relationName )
                    && !ConceptInstanceUtil.isLocationFact( relationName ) ) {
//                  LOGGER.info( "Yes" );
                  allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() ).addAll( relation.getValue() );
               }
            }
         }
         final Map<String,Collection<IdentifiedAnnotation>> docAnnotations = primary.getDocAnnotations();
         for ( Map.Entry<String,Collection<IdentifiedAnnotation>> docAnnotation : docAnnotations.entrySet() ) {
            allDocAnnotations.computeIfAbsent( docAnnotation.getKey(), d -> new HashSet<>() )
                             .addAll( docAnnotation.getValue() );
         }
         final Map<String,Collection<ConceptInstance>> related = primary.getRelated();
         for ( Map.Entry<String,Collection<ConceptInstance>> relation : related.entrySet() ) {
            if ( !ConceptInstanceUtil.isTumorOnlyFact( relation.getKey() ) ) {
               allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() ).addAll( relation.getValue() );
            }
         }

         // Best URI for Cancer derived from primary and all non-sited neoplasms.  Don't use metastasis uris.        ---> Wasn't this already done above?
         cancerContributors.add( primary );
         String bestUri = primary.getUri();
         if ( _isMetastatic || UriConstants.METASTASIS.equals( bestUri ) ) {
            final Collection<String> diagnoses = new HashSet<>();
            diagnoses.add( bestUri );
            diagnoses.addAll( getRelatedUris( RelationConstants.HAS_DIAGNOSIS ) );
            diagnoses.addAll( getRelatedUris( RelationConstants.METASTASIS_OF ) );
            bestUri = UriUtil.getMostSpecificUri( diagnoses );
         } else {
            bestUri = UriUtil.getMostSpecificUri( ConceptInstanceUtil.getUris( cancerContributors ) );
         }

         final Map<String,Collection<ConceptInstance>> cancerOnlyRelated = ConceptInstanceUtil.getCancerOnlyFacts( cancerContributors );
         for ( Map.Entry<String,Collection<ConceptInstance>> relation : cancerOnlyRelated.entrySet() ) {
            allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() ).addAll( relation.getValue() );
         }

         cancerOnlyRelated.clear();
         cancerOnlyRelated.putAll( ConceptInstanceUtil.getCancerOnlyFacts( _metastases ) );
         for ( Map.Entry<String,Collection<ConceptInstance>> relation : cancerOnlyRelated.entrySet() ) {
            allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() ).addAll( relation.getValue() );
         }

         cancerOnlyRelated.clear();
         cancerOnlyRelated.putAll( ConceptInstanceUtil.getCancerFacts( Collections.singletonList( primary ) ) );
         for ( Map.Entry<String,Collection<ConceptInstance>> relation : cancerOnlyRelated.entrySet() ) {
            allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() ).addAll( relation.getValue() );
         }

         final ConceptInstance cancer = new CorefConceptInstance( patientId, bestUri, allDocAnnotations );
         cancer.setRelated( allRelated );
         updateAllInstances( allInstances, Collections.singletonList( _primary ), cancer );
         allInstances.add( _primary );
         // Need to create merged primary?
         return cancer;
      }

   }



   static private abstract class AbstractContainer {

      abstract protected Collection<String> getRelatedUris( final String relationName );

      boolean hasRelated( final String relationName, final ConceptInstance neoplasm ) {
         return hasRelated( relationName, neoplasm.getRelated().get( relationName ) );
      }

      boolean hasRelated( final String relationName, final Collection<ConceptInstance> relations ) {
         final boolean theyEmpty = relations == null || relations.isEmpty();
         final Collection<String> relatedUris = getRelatedUris( relationName );
         final boolean weEmpty = relatedUris == null || relatedUris.isEmpty();
         if ( theyEmpty || weEmpty ) {
            // It is a match if both relation sets are empty.
            return theyEmpty && weEmpty;
         }
         for ( ConceptInstance relation : relations ) {
            final String relationUri = relation.getUri();
            if ( UriUtil.isUriBranchMatch( Collections.singletonList( relationUri ), relatedUris ) ) {
               return true;
            }
         }
         return false;
      }

      void updateAllInstances( final Collection<ConceptInstance> allInstances,
                                         final Collection<ConceptInstance> mergedNeoplasms,
                                         final ConceptInstance newNeoplasm ) {
         allInstances.add( newNeoplasm );
         for ( ConceptInstance instance : allInstances ) {
            if ( mergedNeoplasms.contains( instance ) ) {
               continue;
            }
            final Map<String,Collection<ConceptInstance>> oldRelated = instance.getRelated();
            for ( Map.Entry<String,Collection<ConceptInstance>> oldRelations : oldRelated.entrySet() ) {
               if ( oldRelations.getValue().removeAll( mergedNeoplasms ) ) {
                  oldRelations.getValue().add( newNeoplasm );
               }
            }
         }
         allInstances.removeAll( mergedNeoplasms );
      }

   }


}

