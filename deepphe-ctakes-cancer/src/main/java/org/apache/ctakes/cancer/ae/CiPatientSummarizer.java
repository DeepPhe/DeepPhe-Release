package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/3/2018
 */
@PipeBitInfo(
      name = "CiPatientSummarizer",
      description = "Summarize using Dphe Concept Instances at a patient level instead of document level.",
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class CiPatientSummarizer extends AbstractPatientConsumer {

   static private final Logger LOGGER = Logger.getLogger( MethodHandles.lookup().lookupClass().getSimpleName() );


   public CiPatientSummarizer() {
      super( MethodHandles.lookup().lookupClass().getSimpleName(), "Summarizing Patient Concept Instances" );
   }

   static private final Predicate<ConceptInstance> wantedForFact
         = ci -> !UriConstants.EVENT.equals( ci.getUri() ) && !UriConstants.UNKNOWN.equals( ci.getUri() )
                 && !ci.isNegated() && !ci.isUncertain() && !ci.isGeneric() && !ci.isConditional()
                 && (ci.getRelated().size() + ci.getReverseRelated().size()) != 0;

   /**
    * Call necessary processing for patient
    * <p>
    * {@inheritDoc}
    */
   @Override
   protected void processPatientCas( final JCas patientCas ) throws AnalysisEngineProcessException {
      final String patientName = SourceMetadataUtil.getPatientIdentifier( patientCas );
      final Collection<JCas> docJcases = PatientViewUtil.getAllViews( patientCas );

      final Collection<ConceptInstance> patientConcepts = new ArrayList<>();

      for ( JCas jCas : docJcases ) {

         // Collect affirmed concept instances.
         final Collection<ConceptInstance> affirmedConcepts
               = ConceptInstanceFactory.createUriConceptInstanceMap( jCas ).values()
                                       .stream()
                                       .flatMap( Collection::stream )
                                       .distinct()
                                       .filter( wantedForFact )
                                       .collect( Collectors.toList() );
         // Do something with doc ci ?

         cullUnrelatableConcepts( affirmedConcepts );
         patientConcepts.addAll( affirmedConcepts );
      }

      final Collection<String> cancerUris = UriConstants.getCancerUris();
      final Collection<ConceptInstance> cancerConcepts
            = patientConcepts.stream()
                             .filter( ci -> cancerUris.contains( ci.getUri() ) )
                             .collect( Collectors.toList() );
      printInfo( "Cancer", cancerConcepts, patientConcepts );

      final Collection<String> primaryUris = UriConstants.getGenericTumorUris();
      final Collection<ConceptInstance> primaryConcepts
            = patientConcepts.stream()
                             .filter( ci -> primaryUris.contains( ci.getUri() ) )
                             .collect( Collectors.toList() );

      printInfo( "Primary Tumor", primaryConcepts, patientConcepts );

      final Collection<String> metastasisUris = UriConstants.getMetastasisUris();
      final Collection<ConceptInstance> metastasisConcepts
            = patientConcepts.stream()
                             .filter( ci -> metastasisUris.contains( ci.getUri() ) )
                             .collect( Collectors.toList() );

      printInfo( "Metastatic Tumor", metastasisConcepts, patientConcepts );

   }


   static private void printInfo( final String type, final Collection<ConceptInstance> interestConcepts,
                                  final Collection<ConceptInstance> patientConcepts ) {
      // split tumors into alike body sites.
      final Collection<Collection<ConceptInstance>> siteCollatedTumors
            = collateTumorsBySite( interestConcepts, patientConcepts );

      for ( Collection<ConceptInstance> siteTumors : siteCollatedTumors ) {
         // split tumors into alike tumor type uris.
         final Map<String, Collection<ConceptInstance>> uriTumors = new HashMap<>();
         for ( ConceptInstance tumor : siteTumors ) {
            uriTumors.computeIfAbsent( tumor.getUri(), u -> new ArrayList<>() ).add( tumor );
         }
         final Map<String, Collection<String>> uriCollatedTumors = ByUriRelationFinder
               .collateUris( uriTumors.keySet() );

         // For each site and type, log information on tumor summary.
         for ( Map.Entry<String, Collection<String>> uriTumor : uriCollatedTumors.entrySet() ) {
            // Get the individual concept instances for the tumor summary.
            final Collection<ConceptInstance> tumorInstances = new ArrayList<>();
            for ( ConceptInstance tumor : siteTumors ) {
               if ( uriTumor.getValue().contains( tumor.getUri() ) ) {
                  tumorInstances.add( tumor );
               }
            }
            final StringBuilder sb = new StringBuilder();
            final String mostSpecificUri = ConceptInstanceFactory.getMostSpecificUri( uriTumor.getValue() );
            sb.append( "\n_______________________________________________________\n" ).append( type )
              .append( " " ).append( mostSpecificUri );

            final Map<String, Collection<ConceptInstance>> uriBodySites
                  = getUriBodySites( tumorInstances, patientConcepts );
            final Map<String, Collection<String>> collatedSiteUris = ByUriRelationFinder
                  .collateUris( uriBodySites.keySet() );
            sb.append( " | " )
              .append( collatedSiteUris.values().stream()
                                       .map( ConceptInstanceFactory::getMostSpecificUri )
                                       .collect( Collectors.joining( " , " ) ) );
            final String laterality =
                  tumorInstances.stream()
                                .map( ConceptInstance::getRelated )
                                .map( Map::entrySet )
                                .flatMap( Collection::stream )
                                .filter( e -> RelationConstants.HAS_LATERALITY.equals( e.getKey() ) )
                                .map( Map.Entry::getValue )
                                .flatMap( Collection::stream )
                                .map( ConceptInstance::getUri )
                                .distinct()
                                .collect( Collectors.joining( " , " ) );
            if ( !laterality.isEmpty() ) {
               sb.append( " | " ).append( laterality );
            }

            final Map<String, Collection<ConceptInstance>> affirmedRelations = new HashMap<>();
            for ( ConceptInstance conceptInstance : tumorInstances ) {
               final Map<String, Collection<ConceptInstance>> relatedMap = conceptInstance.getRelated();
               for ( Map.Entry<String, Collection<ConceptInstance>> relatedEntry : relatedMap.entrySet() ) {
                  final Collection<ConceptInstance> affirmedRelated = new ArrayList<>( relatedEntry.getValue() );
                  affirmedRelated.retainAll( patientConcepts );
                  if ( !affirmedRelated.isEmpty() ) {
                     affirmedRelations.computeIfAbsent( relatedEntry.getKey(), r -> new ArrayList<>() )
                                      .addAll( affirmedRelated );
                  }
               }
            }
            final List<String> sortedRelationNames = new ArrayList<>( affirmedRelations.keySet() );
            Collections.sort( sortedRelationNames );
            for ( String relationName : sortedRelationNames ) {
               final Collection<ConceptInstance> relationInstances = affirmedRelations.get( relationName );
               final Collection<String> uris = relationInstances.stream()
                                                                .map( ConceptInstance::getUri )
                                                                .distinct()
                                                                .collect( Collectors.toList() );
               final Map<String, Collection<String>> collatedRelationUris = ByUriRelationFinder.collateUris( uris );
               for ( Map.Entry<String, Collection<String>> relatedUris : collatedRelationUris.entrySet() ) {
                  sb.append( "\n   " ).append( relationName )
                    .append( " " ).append( ConceptInstanceFactory.getMostSpecificUri( relatedUris.getValue() ) )
                    .append( ":      " ).append( relationInstances.stream().map( ConceptInstance::getCoveredText )
                                                                  .collect( Collectors.joining( " ; " ) ) );
               }
            }

            // Fake it 'til you make it.
            for ( Map.Entry<String, Collection<String>> histologies : UriConstants.getHistologyMap().entrySet() ) {
               if ( histologies.getValue().contains( mostSpecificUri ) ) {
                  sb.append( "\n   " ).append( RelationConstants.HAS_HISTOLOGY )
                    .append( " " ).append( histologies.getKey() );
               }
            }

            LOGGER.info( sb.toString() );
         }
      }
   }


   /**
    * Retain all concept instances that have reverse relations only to affirmed concept instances.
    * In other words, if side "right" only exists in a "hasLaterality" relation to negated "tumor", remove it.
    *
    * @param conceptInstances -
    */
   static private void cullUnrelatableConcepts( final Collection<ConceptInstance> conceptInstances ) {
      final Collection<ConceptInstance> removals = new ArrayList<>();
      final Collection<ConceptInstance> affirmedRelated = new ArrayList<>( conceptInstances );
      while ( true ) {
         for ( ConceptInstance conceptInstance : affirmedRelated ) {
            final Map<String, Collection<ConceptInstance>> reverseRelated = conceptInstance.getReverseRelated();
            int reverseAffirmed = 0;
            int reverseNegated = 0;
            for ( Collection<ConceptInstance> related : reverseRelated.values() ) {
               for ( ConceptInstance relate : related ) {
                  if ( affirmedRelated.contains( relate ) ) {
                     reverseAffirmed++;
                  } else {
                     reverseNegated++;
                  }
               }
            }
            if ( reverseNegated > reverseAffirmed / 2 ) {
               final Map<String, Collection<ConceptInstance>> related = conceptInstance.getRelated();
               final long affirmedCount
                     = related.values().stream()
                              .flatMap( Collection::stream )
                              .filter( affirmedRelated::contains )
                              .count();
               if ( affirmedCount == 0 ) {
                  removals.add( conceptInstance );
               }
            }
         }
         if ( removals.isEmpty() ) {
            break;
         }
         affirmedRelated.removeAll( removals );
         removals.clear();
      }
      conceptInstances.retainAll( affirmedRelated );
   }

   /**
    * @param conceptInstances -
    * @param legalConcepts    -
    * @return Collections of relatable tumor concept instances.
    * Tumors are relatable if they have the same laterality and are within the same bodysite uri branch.
    */
   static private Collection<Collection<ConceptInstance>> collateTumorsBySite(
         final Collection<ConceptInstance> conceptInstances, final Collection<ConceptInstance> legalConcepts ) {
      final Collection<Collection<ConceptInstance>> collatedLateralSiteTumors = new ArrayList<>();
      // Map of laterality uris to tumor concept instances with that laterality
      final Map<String, Collection<ConceptInstance>> lateralTumorMap
            = collateByLaterality( conceptInstances, legalConcepts );
      // store for tumors without laterality
      final Collection<ConceptInstance> nonLateralTumors = new ArrayList<>( conceptInstances );
      lateralTumorMap.values().forEach( nonLateralTumors::removeAll );
      lateralTumorMap.put( "NON_LATERAL", nonLateralTumors );

      // deal with lateral tumors
      for ( Map.Entry<String, Collection<ConceptInstance>> lateralTumors : lateralTumorMap.entrySet() ) {
         // map of body site uris and concept instances with those uris  -> for all tumors with this laterality
         final Map<String, Collection<ConceptInstance>> uriBodySites = getUriBodySites( lateralTumors
               .getValue(), legalConcepts );
         // Map of "major site" root uris and all children of those roots in the given body site uris
         final Map<String, Collection<String>> alikeBodySiteUriMap = ByUriRelationFinder
               .collateUris( uriBodySites.keySet() );

         for ( Collection<String> alikeBodySiteUris : alikeBodySiteUriMap.values() ) {
            final Collection<ConceptInstance> relatableTumors = new ArrayList<>();
            final Collection<ConceptInstance> alikeBodySites
                  = alikeBodySiteUris.stream()
                                     .map( uriBodySites::get )
                                     .flatMap( Collection::stream )
                                     .filter( legalConcepts::contains )
                                     .collect( Collectors.toList() );
            for ( ConceptInstance tumor : lateralTumors.getValue() ) {
               final boolean hasBodySite
                     = tumor.getRelated().entrySet().stream()
                            .filter( e -> RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE.equals( e.getKey() ) )
                            .map( Map.Entry::getValue )
                            .flatMap( Collection::stream )
                            .anyMatch( alikeBodySites::contains );
               if ( hasBodySite ) {
                  relatableTumors.add( tumor );
               }
            }
            collatedLateralSiteTumors.add( relatableTumors );
         }
      }
      return collatedLateralSiteTumors;
   }


   static private Map<String, Collection<ConceptInstance>> collateByLaterality(
         final Collection<ConceptInstance> conceptInstances, final Collection<ConceptInstance> legalConcepts ) {
      final Map<String, Collection<ConceptInstance>> lateralityMap = new HashMap<>();
      for ( ConceptInstance ci : conceptInstances ) {
         ci.getRelated().entrySet().stream()
           .filter( e -> RelationConstants.HAS_LATERALITY.equals( e.getKey() ) )
           .map( Map.Entry::getValue )
           .flatMap( Collection::stream )
           .filter( legalConcepts::contains )
           .map( ConceptInstance::getUri )
           .distinct()
           .forEach( u -> lateralityMap.computeIfAbsent( u, l -> new HashSet<>() ).add( ci ) );
      }
      return lateralityMap;
   }

   static private Map<String, Collection<ConceptInstance>> getUriBodySites(
         final Collection<ConceptInstance> conceptInstances, final Collection<ConceptInstance> legalConcepts ) {
      final Collection<ConceptInstance> bodySites
            = conceptInstances.stream()
                              .map( ConceptInstance::getRelated )
                              .map( Map::entrySet )
                              .flatMap( Collection::stream )
                              .filter( e -> RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE
                                    .equals( e.getKey() ) )
                              .map( Map.Entry::getValue )
                              .flatMap( Collection::stream )
                              .filter( legalConcepts::contains )
                              .collect( Collectors.toList() );
      // Collectors.groupingBy was not happy with ci ?
      final Map<String, Collection<ConceptInstance>> map = new HashMap<>();
      for ( ConceptInstance bodySite : bodySites ) {
         map.computeIfAbsent( bodySite.getUri(), b -> new ArrayList<>() ).add( bodySite );
      }
      return map;
   }


}