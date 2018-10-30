package org.apache.ctakes.cancer.summary;

import org.apache.ctakes.cancer.ae.ByUriRelationFinder;
import org.apache.ctakes.cancer.ae.section.SectionHelper;
import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.ctakes.cancer.summary.CiSummaryInitializer.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/5/2018
 */
final public class CiSummaryFactory {

   static private final Logger LOGGER = Logger.getLogger( "CiSummaryFactory" );

   static private final boolean COLLAPSE = true;

   static private final boolean CULL_SMALL = true;

   static private final int REQUIRED_SAME_ATTRIBUTES = 2;

   static private final double SITE_CONCEPT_STD_DEV_DIV = 0;
   static private final double SITE_ANNOTATION_STD_DEV_DIV = 0;

   static private final double CANCER_CONCEPT_STD_DEV_DIV = 0;
   static private final double CANCER_ANNOTATION_STD_DEV_DIV = 0;

   static private boolean APPLY_FILTERING_AT_POINT_3 = true;

   private CiSummaryFactory() {
   }

   static private final Predicate<ConceptInstance> wantedForFact
         = ci -> !UriConstants.EVENT.equals( ci.getUri() ) && !UriConstants.UNKNOWN.equals( ci.getUri() )
                 && !ci.isNegated() && !ci.isUncertain() && !ci.isGeneric() && !ci.isConditional()



                 && !ci.getUri().contains( "Breslow" )



                 && (ci.getRelated().size() + ci.getReverseRelated().size()) != 0;

   static public Collection<CiSummary> createPatientSummaries( final JCas patientCas ) {
      final Collection<ConceptInstance> patientConcepts = createPatientConcepts( patientCas );
      return createSummaries( patientConcepts );
   }

   static public Collection<CiSummary> createDocSummaries( final JCas docCas ) {
      final Collection<ConceptInstance> docConcepts = createDocConcepts( docCas );
      return createSummaries( docConcepts );
   }

   static private Collection<ConceptInstance> createPatientConcepts( final JCas patientCas ) {
      final Collection<ConceptInstance> patientConcepts = new ArrayList<>();
      final Collection<JCas> docJcases = PatientViewUtil.getAllViews( patientCas );
      docJcases.forEach( dc -> patientConcepts.addAll( createDocConcepts( dc ) ) );
      return patientConcepts;
   }

   static private Collection<ConceptInstance> createDocConcepts( final JCas docCas ) {
      // Collect affirmed concept instances.
      final Collection<ConceptInstance> affirmedConcepts
            = ConceptInstanceFactory.createUriConceptInstanceMap( docCas ).values()
                                    .stream()
                                    .flatMap( Collection::stream )
                                    .distinct()
                                    .filter( wantedForFact )
                                    .collect( Collectors.toList() );

      // Get rid of unwanted relations in affirmed instances.
      affirmRelations( affirmedConcepts );

      return affirmedConcepts;
   }


   static private double getThreshold( final UimaContext context, final String name, final boolean useCalculatedThresholds ) {
      double value = 0;
      if ( !useCalculatedThresholds) return value;
      final Object threshold = context.getConfigParameterValue( name );
      if ( threshold == null ) {
         LOGGER.warn( "No value for threshold " + name );
      }
      if ( threshold instanceof Double ) {
         value = (Double)threshold;
      } else if ( threshold instanceof String ) {
         try {
            value = Double.parseDouble( (String)threshold );
         } catch ( NumberFormatException nfE ) {
            LOGGER.warn( "Could not convert " + threshold + " to value for threshold " + name );
         }
      } else {
         LOGGER.warn( "Could not convert " + threshold.getClass().getSimpleName() + " " + threshold +
                      " to value for threshold " + name );
      }
      LOGGER.debug( "Threshold " + name + " = " + value );
      return value;
   }


   static private Collection<CiSummary> createSummaries( final Collection<ConceptInstance> conceptInstances ) {

      final UimaContext context = UimaContextHolder.getContext();
      Boolean b = (Boolean) context.getConfigParameterValue( USE_THRESHOLD_PARAMETERS );
      final boolean useCalculatedThresholds =  b != null && b;
      final double allCancerSiteT = getThreshold( context, ALL_CANCER_SITE_THRESHOLD, useCalculatedThresholds );
      final double cancerSiteT = getThreshold( context, CANCER_SITE_THRESHOLD, useCalculatedThresholds );
      final double primarySiteT = getThreshold( context, PRIMARY_SITE_THRESHOLD, useCalculatedThresholds );
      final double metastasisSiteT = getThreshold( context, METASTASIS_SITE_THRESHOLD, useCalculatedThresholds );
      final double genericSiteT = getThreshold( context, GENERIC_SITE_THRESHOLD, useCalculatedThresholds );
      final double allCancerTumorT = getThreshold( context, ALL_CANCER_TUMOR_THRESHOLD, useCalculatedThresholds );
      final double cancerTumorT = getThreshold( context, CANCER_TUMOR_THRESHOLD, useCalculatedThresholds );
      final double primaryTumorT = getThreshold( context, PRIMARY_TUMOR_THRESHOLD, useCalculatedThresholds );
      final double metastasisTumorT = getThreshold( context, METASTASIS_TUMOR_THRESHOLD, useCalculatedThresholds );
      final double genericTumorT = getThreshold( context, GENERIC_TUMOR_THRESHOLD, useCalculatedThresholds );

      final Collection<String> canceryUris = new HashSet<>( UriConstants.getCancerUris() );
      canceryUris.addAll( UriConstants.getTumorUris() );
      // Using all cancer type concept instances, refine to only the most common.
      final Collection<ConceptInstance> canceryConcepts
            = createCanceryConcepts( "All", conceptInstances, canceryUris, useCalculatedThresholds, allCancerSiteT, allCancerTumorT );

      // Using each cancer type concept instances, refine to only the most common.
      final Collection<CiSummary> allSummaries = new ArrayList<>();
      if ( canceryConcepts.isEmpty() ) {
         return allSummaries;
      }
      final Collection<ConceptInstance> cancerConcepts = filterConcepts( canceryConcepts, UriConstants.getCancerUris() );
      final Collection<ConceptInstance> primaryConcepts = filterConcepts( canceryConcepts, UriConstants.getPrimaryUris() );
      primaryConcepts.removeAll( cancerConcepts );
      final Collection<ConceptInstance> metastasisConcepts = filterConcepts( canceryConcepts, UriConstants.getMetastasisUris() );
      metastasisConcepts.removeAll( cancerConcepts );
      metastasisConcepts.removeAll( primaryConcepts );
      final Collection<ConceptInstance> genericTumorConcepts = filterConcepts( canceryConcepts, UriConstants.getGenericTumorUris() );
      genericTumorConcepts.removeAll( cancerConcepts );
      genericTumorConcepts.removeAll( primaryConcepts );
      genericTumorConcepts.removeAll( metastasisConcepts );
      // TODO need to reconcile the primary and cancer uri overlap, plus the generic tumor uris
      allSummaries.addAll(
            createCancerySummaries2( CiSummary.TYPE_CANCER,
                  cancerConcepts, UriConstants.getCancerUris(), useCalculatedThresholds, cancerSiteT, cancerTumorT ) );
      allSummaries.addAll(
            createCancerySummaries2( CiSummary.TYPE_PRIMARY,
                  primaryConcepts, UriConstants.getPrimaryUris(), useCalculatedThresholds, primarySiteT, primaryTumorT ) );
      allSummaries.addAll(
            createCancerySummaries2( CiSummary.TYPE_METASTASIS,
                  metastasisConcepts, UriConstants.getMetastasisUris(), useCalculatedThresholds, metastasisSiteT, metastasisTumorT ) );
      allSummaries.addAll(
            createCancerySummaries2( CiSummary.TYPE_GENERIC,
                  genericTumorConcepts, UriConstants.getGenericTumorUris(), useCalculatedThresholds, genericSiteT, genericTumorT ) );

      return allSummaries;
   }


   /**
    * @param conceptInstances -
    * @param legalConcepts    -
    * @return Collections of relatable tumor concept instances.
    * Tumors are relatable if they have the same laterality and are within the same bodysite uri branch.
    */
   static private Collection<Collection<ConceptInstance>> collateInstancesBySite(
         final Collection<ConceptInstance> conceptInstances, final Collection<ConceptInstance> legalConcepts ) {
      // Collection of "same-site" "same-laterality" tumors
      final Collection<Collection<ConceptInstance>> collatedLateralSiteTumors = new ArrayList<>();

      // Collection of "same-site" tumors
      final Collection<Collection<ConceptInstance>> collatedSiteConcepts
            = collateBySite( conceptInstances, legalConcepts );
      // deal with laterality
      for ( Collection<ConceptInstance> siteTumors : collatedSiteConcepts ) {
         // Map of laterality uris to tumor concept instances with that laterality
         final Map<String, Collection<ConceptInstance>> lateralTumorMap
               = collateByLaterality( siteTumors, legalConcepts );
         if ( lateralTumorMap.isEmpty() ) {
            collatedLateralSiteTumors.add( siteTumors );
            continue;
         }
         // store for tumors without laterality
         final Collection<ConceptInstance> nonLateralTumors = new ArrayList<>( siteTumors );
         lateralTumorMap.values().forEach( nonLateralTumors::removeAll );

         final Collection<ConceptInstance> bilaterals = lateralTumorMap.get( UriConstants.BILATERAL );
         final Collection<ConceptInstance> lefts = lateralTumorMap.get( UriConstants.LEFT );
         final Collection<ConceptInstance> rights = lateralTumorMap.get( UriConstants.RIGHT );
         boolean usedNonLaterals = false;
         boolean usedLefts = false;
         boolean usedRights = false;
         final int biLateralCount = bilaterals == null ? 0 : bilaterals.stream().map( ConceptInstance::getAnnotations ).mapToInt( Collection::size ).sum();
         final int leftCount = lefts == null ? 0 : lefts.stream().map( ConceptInstance::getAnnotations ).mapToInt( Collection::size ).sum();
         final int rightCount = rights == null ? 0 : rights.stream().map( ConceptInstance::getAnnotations ).mapToInt( Collection::size ).sum();
         final int nonLateralCount = nonLateralTumors == null ? 0 : nonLateralTumors.stream().map( ConceptInstance::getAnnotations ).mapToInt( Collection::size ).sum();
         if ( bilaterals != null && !bilaterals.isEmpty() ) {
            final int bilateralFactor = biLateralCount/3;
            if ( lateralTumorMap.size() == 1 && bilateralFactor > nonLateralCount ) {
               bilaterals.addAll( nonLateralTumors );
               usedNonLaterals = true;
            } else if ( lefts == null && rights != null ) {
               if ( bilateralFactor > rightCount ) {
                  bilaterals.addAll( rights );
                  usedRights = true;
               }
               if ( bilateralFactor > nonLateralCount ) {
                  bilaterals.addAll( nonLateralTumors );
                  usedNonLaterals = true;
               }
            } else if ( lefts != null && rights == null ) {
               if ( bilateralFactor > leftCount ) {
                  bilaterals.addAll( lefts );
                  usedLefts = true;
               }
               if ( bilateralFactor > nonLateralCount ) {
                  bilaterals.addAll( nonLateralTumors );
                  usedNonLaterals = true;
               }
            }
            collatedLateralSiteTumors.add( bilaterals );
         }

         if ( rights != null && !usedRights ) {
            if ( lefts == null && !usedNonLaterals && rightCount/3 > nonLateralCount ) {
               rights.addAll( nonLateralTumors );
               usedNonLaterals = true;
            }
            collatedLateralSiteTumors.add( rights );
         }
         if ( lefts != null && !usedLefts ) {
            if ( rights == null && !usedNonLaterals && leftCount/3 > nonLateralCount ) {
               lefts.addAll( nonLateralTumors );
               usedNonLaterals = true;
            }
            collatedLateralSiteTumors.add( lefts );
         }
         if ( !usedNonLaterals ) {
            collatedLateralSiteTumors.add( nonLateralTumors );
         }
      }
      return collatedLateralSiteTumors;
   }




   static private Collection<ConceptInstance> filterConcepts( final Collection<ConceptInstance> conceptInstances,
                                                              final Collection<String> uris ) {
      return conceptInstances.stream()
                             .filter( ci -> uris.contains( ci.getUri() ) )
                             .collect( Collectors.toList() );
   }

   static private Collection<ConceptInstance> createCanceryConcepts( final String type,
                                                                     final Collection<ConceptInstance> conceptInstances,
                                                                     final Collection<String> uris,
                                                                     final boolean useCalculatedThresholds,
                                                                     final double fewSitedThreshold,
                                                                     final double fewAlikeThreshold ) {
      final Collection<ConceptInstance> summaryConcepts = filterConcepts( conceptInstances, uris );
      if ( summaryConcepts.isEmpty() ) {
         return summaryConcepts;
      }
      LOGGER.debug( type + " Starting summary concept count " + summaryConcepts.size() );

      // Get rid of summary type concepts that have few similar concept sites or few similar annotation sites
      if (APPLY_FILTERING_AT_POINT_3) {
         removeFewSited(summaryConcepts, useCalculatedThresholds, fewSitedThreshold);
      } else {
         LOGGER.debug("Skipping the removal of 'FewSited'");
      }

      LOGGER.debug( type + " Post-site-threshold summary concept count " + summaryConcepts.size() + "\n" );

      // Get rid of summary type concepts that have few similar concepts or few similar annotations
      if (APPLY_FILTERING_AT_POINT_3) {
         removeFewAlike( summaryConcepts, fewAlikeThreshold );
      } else {
         LOGGER.debug("Skipping the removal of 'FewAlike'");
      }

      LOGGER.debug( type + " Post-tumor-threshold summary concept count " + summaryConcepts.size() + "\n" );

      return summaryConcepts;
   }


   static private Collection<ConceptInstance> removeTissueSited( final Collection<ConceptInstance> summaryTypeInstances ) {
      final Collection<ConceptInstance> removalTumors = new ArrayList<>();
      for ( ConceptInstance concept : summaryTypeInstances ) {
         final Map<String,Collection<ConceptInstance>> related = concept.getRelated();
         final Collection<ConceptInstance> sites = related.get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( sites == null ) {
            removalTumors.add( concept );
            continue;
         }
         concept.getRelated().put( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE, sites );
      }
      summaryTypeInstances.removeAll( removalTumors );
      return summaryTypeInstances;
   }

   static private Collection<ConceptInstance> removeFewSited( final Collection<ConceptInstance> summaryTypeInstances,
                                                              final boolean useCalculatedThresholds,
                                                              final double fewSitedThreshold ) {
      final Collection<ConceptInstance> siteConcepts = new HashSet<>();
      for ( ConceptInstance cancery : summaryTypeInstances ) {
         final Collection<ConceptInstance> sites
               = cancery.getRelated().get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( sites != null ) {
            siteConcepts.addAll( sites );
         }
      }
      // collate site uris
      final Map<String, Collection<ConceptInstance>> alikeSiteMap
            = ByUriRelationFinder.collateUriConceptsCloseEnough( siteConcepts );

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      final Collection<String> lymphNodeBranch = SearchUtil.getBranchUris( graphDb, "Lymph_Node" );
      final Map<String,Collection<ConceptInstance>> alikeLymphs = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptInstance>> alikeSites : alikeSiteMap.entrySet() ) {
         if ( lymphNodeBranch.contains( alikeSites.getKey() ) ) {
            alikeLymphs.put( alikeSites.getKey(), alikeSites.getValue() );
         }
      }
      if ( alikeLymphs.size() == alikeSiteMap.size() ) {
         return summaryTypeInstances;
      }
      alikeLymphs.keySet().forEach( alikeSiteMap::remove );

      final Map<String, Integer> siteConceptCounts = new HashMap<>( alikeSiteMap.size() );
      final Map<String, Integer> siteAnnotationCounts = new HashMap<>( alikeSiteMap.size() );
      final Collection<Integer> subSiteConceptCounts = new ArrayList<>( siteConcepts.size() );
      final Collection<Integer> subSiteAnnotationCounts = new ArrayList<>( siteConcepts.size() );
      int siteAnnotationTotal = 0;
      for ( Map.Entry<String, Collection<ConceptInstance>> entry : alikeSiteMap.entrySet() ) {
         siteConceptCounts.put( entry.getKey(), entry.getValue().size() );
         subSiteConceptCounts.add( entry.getValue().size() );
         final int annotationCount = entry.getValue().stream()
                                          .map( ConceptInstance::getAnnotations )
                                          .mapToInt( Collection::size )
                                          .sum();
         siteAnnotationCounts.put( entry.getKey(), annotationCount );
         subSiteAnnotationCounts.add( annotationCount );
         siteAnnotationTotal += annotationCount;
      }
      final double siteConceptMean = ((double)siteConcepts.size()) / ((double)alikeSiteMap.size());
      double siteConceptAdjust= 0;
      if ( SITE_CONCEPT_STD_DEV_DIV > 0 ) {
         double siteConceptDeviation = 0;
         for ( int value : subSiteConceptCounts ) {
            siteConceptDeviation += Math.pow( value - siteConceptMean, 2 );
         }
         final double siteConceptStandard = Math.sqrt( siteConceptDeviation / (double)subSiteConceptCounts.size() );
         siteConceptAdjust = siteConceptStandard / SITE_CONCEPT_STD_DEV_DIV;
      }
      final double siteAnnotationMean = ((double)siteAnnotationTotal) / ((double)alikeSiteMap.size());
      double siteAnnotationAdjust = 0;
      double userSiteAnnotationAdjust = 0;
         double siteAnnotationDeviation = 0;
         for ( int value : subSiteAnnotationCounts ) {
            siteAnnotationDeviation += Math.pow( value - siteAnnotationMean, 2 );
         }
         final double siteAnnotationStandard
               = Math.sqrt( siteAnnotationDeviation / (double)subSiteAnnotationCounts.size() );
         userSiteAnnotationAdjust = siteAnnotationStandard * fewSitedThreshold;

      final double siteConceptThreshold = Math.floor( siteConceptMean - siteConceptAdjust );
      final double siteAnnotationThreshold = Math.floor( siteAnnotationMean - siteAnnotationAdjust ); // Used 1.01 for experimenting
      final double userSiteAnnotationThreshold = Math.floor( siteAnnotationMean + userSiteAnnotationAdjust );

      final Collection<ConceptInstance> keeperSites = new HashSet<>();
      boolean keepEvenThoSingleton;

      boolean enableSingletonsForCertainSections = true;
      LOGGER.debug("useCalculatedThresholds="+useCalculatedThresholds + "   " + "enableSingletonsForCertainSections="+enableSingletonsForCertainSections);
      if (useCalculatedThresholds) {
         LOGGER.debug( "Site Threshold values:  mean=" + siteAnnotationMean + " std=" + siteAnnotationStandard + " userAdjust=" + userSiteAnnotationAdjust
                 + " Site Threshold=" + siteAnnotationThreshold + " User Site Threshold=" + userSiteAnnotationThreshold );
      }
      for ( Map.Entry<String, Collection<ConceptInstance>> alike : alikeSiteMap.entrySet() ) {
         keepEvenThoSingleton = false;
         final int siteConceptCount = siteConceptCounts.get( alike.getKey() );
         final int siteAnnotationCount = siteAnnotationCounts.get( alike.getKey() );
         if (enableSingletonsForCertainSections && siteAnnotationCount==1) {
            ConceptInstance ci = alike.getValue().iterator().next();
            for (IdentifiedAnnotation a: ci.getAnnotations()) {
               if (SectionHelper.isFinalDiagnosisSection(a.getSegmentID())) {
                  keepEvenThoSingleton = true;
               }
            }

         }

         if ( keepEvenThoSingleton || (!useCalculatedThresholds && siteAnnotationCount > 1 ) ||
                 (useCalculatedThresholds && siteAnnotationCount >= siteAnnotationThreshold && siteAnnotationCount >= userSiteAnnotationThreshold)) {
            keeperSites.addAll( alike.getValue() );
            LOGGER.debug( "  Keeping " + alike.getKey() + " " + siteAnnotationCount );
         } else {
            LOGGER.debug( "  Removing " + alike.getKey() + " " + siteAnnotationCount );
         }
      }

      alikeLymphs.values().forEach( keeperSites::addAll );

      final Collection<ConceptInstance> fewSiteCanceries = new ArrayList<>();
      for ( ConceptInstance cancery : summaryTypeInstances ) {
         final Map<String,Collection<ConceptInstance>> related = cancery.getRelated();
         final Collection<ConceptInstance> removalSites = new ArrayList<>();
         final Collection<ConceptInstance> sites = related.get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( sites == null ) {
            fewSiteCanceries.add( cancery );
         } else {
            for ( ConceptInstance site : sites ) {
               if ( !keeperSites.contains( site ) ) {
                  removalSites.add( site );
               }
            }
            if ( removalSites.size() == sites.size() ) {
               fewSiteCanceries.add( cancery );
            } else {
               sites.removeAll( removalSites );
               cancery.getRelated().put( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE, sites );
            }
         }
      }
      summaryTypeInstances.removeAll( fewSiteCanceries );
      return summaryTypeInstances;
   }



   static private Collection<ConceptInstance> removeFewAlike( final Collection<ConceptInstance> summaryTypeInstances,
                                                              final double fewAlikeThreshold ) {
      // collate site uris
      final Map<String, Collection<ConceptInstance>> alikeCancerMap
            = ByUriRelationFinder.collateUriConceptsCloseEnough( summaryTypeInstances );

      final Map<String, Integer> cancerConceptCounts = new HashMap<>( alikeCancerMap.size() );
      final Map<String, Integer> cancerAnnotationCounts = new HashMap<>( alikeCancerMap.size() );
      final Collection<Integer> subCancerConceptCounts = new ArrayList<>( alikeCancerMap.size() );
      final Collection<Integer> subCancerAnnotationCounts = new ArrayList<>( alikeCancerMap.size() );
      int cancerAnnotationTotal = 0;
      for ( Map.Entry<String, Collection<ConceptInstance>> entry : alikeCancerMap.entrySet() ) {
         cancerConceptCounts.put( entry.getKey(), entry.getValue().size() );
         subCancerConceptCounts.add( entry.getValue().size() );
         final int annotationCount = entry.getValue().stream()
                                          .map( ConceptInstance::getAnnotations )
                                          .mapToInt( Collection::size )
                                          .sum();
         cancerAnnotationCounts.put( entry.getKey(), annotationCount );
         subCancerAnnotationCounts.add( annotationCount );
         cancerAnnotationTotal += annotationCount;
      }
      final double cancerConceptMean = ((double)summaryTypeInstances.size()) / ((double)alikeCancerMap.size());
      double cancerConceptAdjust = 0;
      if ( CANCER_CONCEPT_STD_DEV_DIV > 0 ) {
         double cancerConceptDeviation = 0;
         for ( int value : subCancerConceptCounts ) {
            cancerConceptDeviation += Math.pow( value - cancerConceptMean, 2 );
         }
         final double cancerConceptStandard = Math.sqrt( cancerConceptDeviation / (double)subCancerConceptCounts.size() );
         cancerConceptAdjust = cancerConceptStandard / CANCER_CONCEPT_STD_DEV_DIV;
      }
      final double cancerAnnotationMean = ((double)cancerAnnotationTotal) / ((double)alikeCancerMap.size());
      double cancerAnnotationAdjust = 0;
      double userAnnotationAdjust = 0;
         double cancerAnnotationDeviation = 0;
         for ( int value : subCancerAnnotationCounts ) {
            cancerAnnotationDeviation += Math.pow( value - cancerAnnotationMean, 2 );
         }
         final double cancerAnnotationStandard
               = Math.sqrt( cancerAnnotationDeviation / (double)subCancerAnnotationCounts.size() );
         userAnnotationAdjust = cancerAnnotationStandard * fewAlikeThreshold;
      final double cancerConceptThreshold = Math.floor( cancerConceptMean - cancerConceptAdjust );
      final double cancerAnnotationThreshold = Math.floor( cancerAnnotationMean - cancerAnnotationAdjust );
      final double userAnnotationThreshold = Math.floor( cancerAnnotationMean + userAnnotationAdjust );
      LOGGER.debug( "Tumor Threshold values:  mean=" + cancerAnnotationMean + " std=" + cancerAnnotationStandard + " userAdjust="
                   + userAnnotationAdjust + " Tumor Threshold=" + cancerAnnotationThreshold + " User Tumor Threshold=" + userAnnotationThreshold );

      final Collection<ConceptInstance> keeperCancers = new HashSet<>();
      for ( Map.Entry<String, Collection<ConceptInstance>> alike : alikeCancerMap.entrySet() ) {
         final int cancerConceptCount = cancerConceptCounts.get( alike.getKey() );
         final int cancerAnnotationCount = cancerAnnotationCounts.get( alike.getKey() );
         if ( cancerAnnotationCount >= cancerAnnotationThreshold
              && cancerAnnotationCount >= userAnnotationThreshold
               ) {
            keeperCancers.addAll( alike.getValue() );
            LOGGER.debug( "  Keeping " + alike.getKey() + " " + cancerAnnotationCount );
         } else {
            LOGGER.debug( "  Removing " + alike.getKey() + " " + cancerAnnotationCount );
         }
      }

      summaryTypeInstances.retainAll( keeperCancers );
      return summaryTypeInstances;
   }


   static private Collection<CiSummary> mergeSecondDegreeRelated( final String type, final List<CiSummary> summaries ) {
      String summaryType = type;
      final Map<CiSummary,Collection<CiSummary>> relatedSummaries = new HashMap<>();
      // For each summary, how many contained concept instances are the same by relation
      // if SummaryA is a Tumor with size 2x3x4 and SummaryB is another tumor with size 2x3x4 ... should the summaries be merged?
      for ( int i=0; i<summaries.size()-1; i++ ) {
         final CiSummary iSummary = summaries.get( i );
         relatedSummaries.computeIfAbsent( iSummary, s -> new HashSet<>() ).add( iSummary );

         final Map<String,Collection<ConceptInstance>> iRelations = iSummary.getRelations();
         for ( int j=i+1; j<summaries.size(); j++ ) {
            int same = 0;
            int half = 0;
            int different = 0;
            final CiSummary jSummary = summaries.get( j );
            relatedSummaries.computeIfAbsent( jSummary, s -> new HashSet<>() ).add( jSummary );
            final Map<String,Collection<ConceptInstance>> jRelations = jSummary.getRelations();
            for ( Map.Entry<String,Collection<ConceptInstance>> iEntry : iRelations.entrySet() ) {
               if ( iEntry.getKey().equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE )
                    || iEntry.getKey().equals( RelationConstants.HAS_DIAGNOSIS )) {
                  continue;
               }
               final Collection<ConceptInstance> jRelated = jRelations.get( iEntry.getKey() );
               if ( jRelated != null ) {
                  int jSame = 0;
                  for ( ConceptInstance iInstance : iEntry.getValue() ) {
                     if ( jRelated.contains( iInstance ) ) {
                        jSame++;
                     }
                  }
                  if ( jSame == jRelated.size() && jSame == iEntry.getValue().size() ) {
                     same++;
                  } else if ( jSame > 1 ) {
                     half++;
                  } else {
                     different++;
                  }
               }
            }
            if ( same > 0 ) {
               relatedSummaries.computeIfAbsent( iSummary, s -> new HashSet<>() ).add( jSummary );
               relatedSummaries.computeIfAbsent( jSummary, s -> new HashSet<>() ).add( iSummary );
            }
         }
      }

      final Collection<CiSummary> notConsumed = new ArrayList<>( summaries );
      final Collection<CiSummary> finalSummaries = new ArrayList<>();

      for ( Map.Entry<CiSummary,Collection<CiSummary>> alikeEntry : relatedSummaries.entrySet() ) {
         if ( notConsumed.contains( alikeEntry.getKey() ) && alikeEntry.getValue().size() > 1 ) {
            notConsumed.removeAll( alikeEntry.getValue() );
            final Collection<ConceptInstance> alikeInstances = new ArrayList<>();
            for ( CiSummary alike : alikeEntry.getValue() ) {
               alikeInstances.addAll( alike.getConceptInstances() );
            }
            final Collection<String> typeUris = alikeEntry.getValue().stream().map( CiSummary::getUri ).collect( Collectors.toSet() );
            final String typeUri = ConceptInstanceFactory.getMostSpecificUri( typeUris );
            if ( type.equals( CiSummary.TYPE_GENERIC ) ) {
               summaryType = getType( typeUri );
            }
            if ( COLLAPSE ) {
               finalSummaries.add( new CiSummary( summaryType, typeUri, CiCollapser.collapse( alikeInstances ) ) );
            } else {
               finalSummaries.add( new CiSummary( summaryType, typeUri, alikeInstances ) );
            }
         }
      }
      finalSummaries.addAll( notConsumed );
      return finalSummaries;
   }






   static private Collection<CiSummary> createCancerySummaries2( final String type,
                                                                 final Collection<ConceptInstance> allInstances,
                                                                 final Collection<String> uris,
                                                                 final boolean useCalculatedThresholds,
                                                                 final double fewSitedThreshold,
                                                                 final double fewAlikeThreshold  ) {
      String summaryType = type;
      final Collection<ConceptInstance> refinedCanceryConcepts = createCanceryConcepts( type, allInstances, uris, useCalculatedThresholds, fewSitedThreshold, fewAlikeThreshold );

      LOGGER.debug( "ConceptInstance Summary Count: " + type + " " + refinedCanceryConcepts.size() );

      if ( refinedCanceryConcepts.isEmpty() ) {
         return Collections.emptyList();
      }
      // split summaries into alike body sites, including laterality.
      final Collection<Collection<ConceptInstance>> siteCollatedInstances
            = collateInstancesBySite( refinedCanceryConcepts, allInstances );

      LOGGER.debug( "ConceptInstance Site Collated Summary Count: " + type + " " + siteCollatedInstances.size() );
      String s = "";
      for (Collection<ConceptInstance> coll: siteCollatedInstances) {
         s = s + "  Collection: ";
         for (ConceptInstance ci: coll) {
            s = s + ci + "; ";
         }
         s = s + "\n";
      }

      LOGGER.debug(s);

      if ( siteCollatedInstances.isEmpty() ) {
         return Collections.emptyList();
      }

      final List<CiSummary> summaries = new ArrayList<>();
      for ( Collection<ConceptInstance> siteInstances : siteCollatedInstances ) {
         final Collection<String> typeUris = siteInstances.stream().map( ConceptInstance::getUri ).collect( Collectors.toSet() );
         final String typeUri = ConceptInstanceFactory.getMostSpecificUri( typeUris );
         if ( type.equals( CiSummary.TYPE_GENERIC ) ) {
            summaryType = getType( typeUri );
         }
        if ( COLLAPSE ) {
            summaries.add( new CiSummary( summaryType, typeUri, CiCollapser.collapse( siteInstances ) ) );
         } else {
            summaries.add( new CiSummary( summaryType, typeUri, siteInstances ) );
         }
      }

      LOGGER.debug( "CiSummaries Count: " + type + " " + summaries.size() );

      final Collection<CiSummary> mergedSummaries = mergeSecondDegreeRelated( type, summaries );

      LOGGER.debug( "Merged CiSummaries Count: " + type + " " + mergedSummaries.size() );

      return mergedSummaries;
   }


   static private String getType( final String uri ) {
      if (  UriConstants.getCancerUris().contains( uri ) ) {
         return CiSummary.TYPE_CANCER;
      }
      if (  UriConstants.getPrimaryUris().contains( uri ) ) {
         return CiSummary.TYPE_PRIMARY;
      }
      if (  UriConstants.getMetastasisUris().contains( uri ) ) {
         return CiSummary.TYPE_METASTASIS;
      }
      return CiSummary.TYPE_GENERIC;
   }


   /**
    * Also collapses body sites
    * @param conceptInstances -
    * @param legalConcepts    -
    * @return Collections of relatable tumor concept instances.
    * Tumors are relatable if they have the same laterality and are within the same bodysite uri branch.
    */
   static private Collection<Collection<ConceptInstance>> collateBySite(
         final Collection<ConceptInstance> conceptInstances, final Collection<ConceptInstance> legalConcepts ) {

      // map of body site uris and concept instances with those uris  -> for all tumors with this laterality
      final Map<String, Collection<ConceptInstance>> uriBodySites = getUriBodySites( conceptInstances, legalConcepts );

      // collate site uris
      final Map<String,Collection<ConceptInstance>> alikeBodySites
            = ByUriRelationFinder.collateUriConceptsCloseEnough( uriBodySites );

      final Map<ConceptInstance,ConceptInstance> collapsedSites = new HashMap<>( alikeBodySites.size() );
      for ( Map.Entry<String,Collection<ConceptInstance>> alikeBodySite : alikeBodySites.entrySet() ) {
         final ConceptInstance collapsedBodySite
               = CiCollapser.collapseConcepts( alikeBodySite.getKey(), alikeBodySite.getValue() );
         alikeBodySite.getValue().forEach( s -> collapsedSites.put( s, collapsedBodySite ) );
      }
      final Map<String,Collection<ConceptInstance>> alikeSiteTumors = new HashMap<>();
         for ( ConceptInstance tumor : conceptInstances ) {
            final Collection<ConceptInstance> tumorSites = tumor.getRelated().get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
            if ( tumorSites == null || tumorSites.isEmpty() ) {
               continue;
            }
            final Collection<ConceptInstance> tumorCollapsedSites = new HashSet<>();
            for ( ConceptInstance tumorSite : tumorSites ) {
               final ConceptInstance collapsedSite = collapsedSites.get( tumorSite );
               tumorCollapsedSites.add( collapsedSite );
               alikeSiteTumors.computeIfAbsent( collapsedSite.getUri(), s -> new HashSet<>() ).add( tumor );
            }
            tumor.getRelated().put( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE, tumorCollapsedSites );
         }

      return alikeSiteTumors.values();
   }


   static private Map<String, Collection<ConceptInstance>> collateByLaterality(
         final Collection<ConceptInstance> conceptInstances, final Collection<ConceptInstance> legalConcepts ) {
      final Map<ConceptInstance,Collection<String>> tumorLateralityMap = new HashMap<>( conceptInstances.size() );
      final Map<String, Collection<ConceptInstance>> lateralityMap = new HashMap<>( 3 );
      for ( ConceptInstance tumor : conceptInstances ) {
         final Collection<ConceptInstance> lateralities = tumor.getRelated().get( RelationConstants.HAS_LATERALITY );
         if ( lateralities == null || lateralities.isEmpty() ) {
            continue;
         }
         for ( ConceptInstance laterality : lateralities ) {
            if ( !legalConcepts.contains( laterality ) ) {
               continue;
            }
            tumorLateralityMap.computeIfAbsent( tumor, t -> new HashSet<>() ).add( laterality.getUri() );
            lateralityMap.computeIfAbsent( laterality.getUri(), l -> new HashSet<>() ).add( laterality );
         }
      }
      final Map<String,ConceptInstance> collapsedLateralities = new HashMap<>( 3 );
      for ( Map.Entry<String,Collection<ConceptInstance>> lateralities : lateralityMap.entrySet() ) {
         collapsedLateralities.put( lateralities.getKey(), CiCollapser.collapseConcepts( lateralities.getValue() ) );
      }
      for ( Map.Entry<ConceptInstance,Collection<String>> lateralTumors : tumorLateralityMap.entrySet() ) {
         final Collection<ConceptInstance> collapsedLaterality
               = lateralTumors.getValue().stream()
                              .map( collapsedLateralities::get )
                              .collect( Collectors.toList());
         lateralTumors.getKey().getRelated().put( RelationConstants.HAS_LATERALITY, collapsedLaterality );
      }
      return lateralityMap;
   }


   static private void printSiblings( final Collection<String> uris  ) {
      final Map<String,Collection<String>> uriRoots = new HashMap<>( uris.size() );
      for ( String uri : uris ) {
         final String rootCode = Neo4jOntologyConceptUtil.getRootUris( uri ).stream().sorted().collect( Collectors.joining( "-" ) );
         uriRoots.computeIfAbsent( rootCode, r -> new ArrayList<>() ).add( uri );
      }
      for ( Collection<String> siblings : uriRoots.values() ) {
         LOGGER.debug( "Siblings: " + String.join( " ", siblings ) );
      }
   }

   static private Collection<Collection<ConceptInstance>> combineSiblings(
         final Map<String,Collection<ConceptInstance>> instanceMap ) {
      final Map<String,Collection<ConceptInstance>> rootMap = new HashMap<>();
      for ( Map.Entry<String,Collection<ConceptInstance>> instances : instanceMap.entrySet() ) {
         final String root
               = Neo4jOntologyConceptUtil.getRootUris( instances.getKey() ).stream()
                                         .sorted()
                                         .collect( Collectors.joining() );
         rootMap.computeIfAbsent( root, r -> new ArrayList<>() ).addAll( instances.getValue() );
      }
      return rootMap.values();
   }



   static private Map<String, Collection<ConceptInstance>> getUriBodySites(
         final Collection<ConceptInstance> conceptInstances, final Collection<ConceptInstance> legalConcepts ) {
      final Map<String,Collection<ConceptInstance>> uriSites = new HashMap<>();
      for ( ConceptInstance concept : conceptInstances ) {
         final Collection<ConceptInstance> sites
               = concept.getRelated().get( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( sites == null ) {
            continue;
         }
         for ( ConceptInstance site : sites ) {
            uriSites.computeIfAbsent( site.getUri(), s -> new HashSet<>() ).add( site );
         }
      }
      return uriSites;
   }


   static private void affirmRelations( final Collection<ConceptInstance> allInstances ) {
      final Map<String, Collection<ConceptInstance>> affirmedRelations = new HashMap<>();
      final Map<String, Collection<ConceptInstance>> affirmedReverses = new HashMap<>();
      for ( ConceptInstance instance : allInstances ) {
         final Map<String, Collection<ConceptInstance>> relatedMap = instance.getRelated();
         for ( Map.Entry<String, Collection<ConceptInstance>> relatedEntry : relatedMap.entrySet() ) {
            final Collection<ConceptInstance> affirmedRelated = new ArrayList<>( relatedEntry.getValue() );
            affirmedRelated.retainAll( allInstances );
            if ( !affirmedRelated.isEmpty() ) {
               affirmedRelations.computeIfAbsent( relatedEntry.getKey(), r -> new ArrayList<>() )
                                .addAll( affirmedRelated );
            }
         }
         // reset the relations for the ci.
         instance.setRelated( affirmedRelations );
         affirmedRelations.clear();

         final Map<String, Collection<ConceptInstance>> reversedMap = instance.getReverseRelated();
         for ( Map.Entry<String, Collection<ConceptInstance>> reversedEntry : reversedMap.entrySet() ) {
            final Collection<ConceptInstance> affirmedReversed = new ArrayList<>( reversedEntry.getValue() );
            affirmedReversed.retainAll( allInstances );
            if ( !affirmedReversed.isEmpty() ) {
               affirmedReverses.computeIfAbsent( reversedEntry.getKey(), r -> new ArrayList<>() )
                                .addAll( affirmedReversed );
            }
         }
         // reset the reverse relations for the ci.
         instance.setReverseRelated( affirmedReverses );
         affirmedReverses.clear();
      }
   }

}
