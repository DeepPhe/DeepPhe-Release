package org.apache.ctakes.cancer.concept.instance;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import static org.apache.ctakes.cancer.concept.instance.ConceptInstanceUtil.NeoplasmType.PRIMARY;
import static org.healthnlp.deepphe.neo4j.RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE;
import static org.healthnlp.deepphe.neo4j.RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE;
import static org.healthnlp.deepphe.neo4j.RelationConstants.HAS_LATERALITY;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/28/2019
 */
@Immutable
final public class NeoplasmLocationUtil {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmColocater" );

   private NeoplasmLocationUtil() {}

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Set sites of Primaries by Cancer, Cancer by primaries
   //
   ////////////////////////////////////////////////////////////////////////////////////////////////////

   // TODO - this can / should be done for all tumors

   /**
    * Try our best to ensure that every cancer and every tumor has a location.
    * @param diagnosisMap map with cancers (key) to tumors for each cancer (value)
    */
   static public void coLocateCancerTumors( final Map<ConceptInstance,Collection<ConceptInstance>> diagnosisMap ) {
      for ( Map.Entry<ConceptInstance,Collection<ConceptInstance>> cancerEntry : diagnosisMap.entrySet() ) {
         final ConceptInstance cancer = cancerEntry.getKey();
         locateCancerByTumors( cancer, cancerEntry.getValue() );
         locateTumorsByCancer( cancer, cancerEntry.getValue() );
      }
   }

   /**
    * If the cancer has no location, go through tumors for their locations and lateralities.
    * Attempt to assign primary tumor locations and lateralities to the cancer.
    * If there are no primary locations, assign any metastatic locations.
    * @param cancer -
    * @param tumors tumors for cancer
    */
   static private void locateCancerByTumors( final ConceptInstance cancer,
                                             final Collection<ConceptInstance> tumors ) {
      final Collection<ConceptInstance> cancerLocations
            = cancer.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
      if ( cancerLocations != null && !cancerLocations.isEmpty() ) {
         return;
      }
      final Collection<ConceptInstance> primaryLocations = new HashSet<>();
      final Collection<ConceptInstance> primaryLateralities = new HashSet<>();
      final Collection<ConceptInstance> metastasisLocations = new HashSet<>();
      final Collection<ConceptInstance> metastasisLateralities = new HashSet<>();
      for ( ConceptInstance tumor : tumors ) {
         final Collection<ConceptInstance> tumorLocations
               = ConceptInstanceUtil.getRelated( DISEASE_HAS_PRIMARY_ANATOMIC_SITE, tumor );
         final Collection<ConceptInstance> lateralities
               = ConceptInstanceUtil.getRelated( HAS_LATERALITY, tumor );
         if ( tumorLocations.isEmpty() ) {
            continue;
         }
         if ( PRIMARY == ConceptInstanceUtil.getNeoplasmType( tumor ) ) {
            primaryLocations.addAll( tumorLocations );
            primaryLateralities.addAll( lateralities );
         } else {
            metastasisLocations.addAll( tumorLocations );
            metastasisLateralities.addAll( lateralities );
         }
      }
      if ( !primaryLocations.isEmpty() ) {
         primaryLocations.forEach( l ->
               cancer.addRelated( DISEASE_HAS_PRIMARY_ANATOMIC_SITE, l ) );
         primaryLateralities.forEach( l ->
               cancer.addRelated( HAS_LATERALITY, l ) );
      } else {
         metastasisLocations.forEach( l ->
               cancer.addRelated( DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE, l ) );
         metastasisLateralities.forEach( l ->
               cancer.addRelated( HAS_LATERALITY, l ) );
      }
   }

   /**
    * If a tumor has no location, assign it to the cancer's location.
    * @param cancer -
    * @param tumors tumors for cancer
    */
   static private void locateTumorsByCancer( final ConceptInstance cancer,
                                             final Collection<ConceptInstance> tumors ) {
      final Collection<ConceptInstance> cancerLocations
            = cancer.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
      if ( cancerLocations == null || cancerLocations.isEmpty() ) {
         return;
      }
      final Collection<ConceptInstance> cancerLateralities
            = cancer.getRelated().get( HAS_LATERALITY );
      for ( ConceptInstance tumor : tumors ) {
         final boolean isPrimary = PRIMARY == ConceptInstanceUtil.getNeoplasmType( tumor );
         final Collection<ConceptInstance> tumorLocations
               = tumor.getRelated().get( DISEASE_HAS_PRIMARY_ANATOMIC_SITE );
         if ( tumorLocations != null && !tumorLocations.isEmpty() ) {
            if ( isPrimary && cancerLateralities != null ) {
               cancerLateralities.forEach( l -> tumor.addRelated( HAS_LATERALITY, l ) );
            }
            continue;
         }
         if ( isPrimary ) {
            cancerLocations.forEach( l -> tumor.addRelated( DISEASE_HAS_PRIMARY_ANATOMIC_SITE, l ) );
            if ( cancerLateralities != null ) {
               cancerLateralities.forEach( l -> tumor.addRelated( HAS_LATERALITY, l ) );
            }
         } else {
            cancerLocations.forEach( l -> tumor.addRelated( DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE, l ) );
         }
      }
   }



}
