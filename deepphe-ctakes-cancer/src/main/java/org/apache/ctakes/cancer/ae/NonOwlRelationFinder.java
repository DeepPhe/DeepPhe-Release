package org.apache.ctakes.cancer.ae;


import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The ml location_of module only finds same-sentence relations.
 * Many neoplasms have locations outside the same sentence.
 * <p>
 * In a paragraph a body site mention can be associated with all targets that follow it
 * until the next body site is mentioned.
 * A body site that is mentioned after target, can only be associated with that target within a sentence.
 * A previous body site can be associated with multiple latter targets,
 * while a target is infrequently associated with multiple previous body sites.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/7/2017
 */
final public class NonOwlRelationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "NonOwlRelationFinder" );

   // hasLaterality is NOT handled by OwlRelationFinder
   static private final String HAS_LATERALITY = "hasLaterality";
   // hasClockface is NOT handled by OwlRelationFinder
   static private final String HAS_CLOCKFACE = "hasClockface";
   // hasQuadrant is NOT handled by OwlRelationFinder
   static private final String HAS_QUADRANT = "hasQuadrant";
   // isMetastasisOf is NOT handled by OwlRelationFinder
   static private final String METASTASIS_OF = "isMetastasisOf";

   static private final boolean DEBUG_OUT = false;

   // Relations currently 03/07/2017 contained in the ontology
   //      hasBIRADSCategory
   //      hasBodySite
   //      hasCalcification
   //      hasCancerCellLine
   //      hasCancerStage
   //      hasClinicalMClassification
   //      hasClinicalNClassification
   //      hasClinicalTClassification
   //      hasDiagnosis
   //      hasERStatus
   //      hasEncounter
   //      hasFocality
   //      hasGenericMClassification
   //      hasGenericNClassification
   //      hasGenericTClassification
   //      hasHeight
   //      hasHer2Status
   //      hasHistologicType
   //      hasKi67Status
   //      hasLymphovascularInvasionStatus
   //      hasManifestation
   //      hasMarginStatus
   //      hasMenopausalStatus
   //      hasMethod
   //      hasMitosesScore
   //      hasNuclearGrade
   //      hasNuclearGradeScore
   //      hasOutcome
   //      hasPRStatus
   //      hasPathologicAggregateTumorSize
   //      hasPathologicMClassification
   //      hasPathologicNClassification
   //      hasPathologicTClassification
   //      hasPathologicTumorSize
   //      hasRECISTCategory
   //      hasRadiologicTumorSize
   //      hasReasonForUse
   //      hasReceptorStatus
   //      hasRegimen
   //      hasRegimenComponent
   //      hasRegimenType
   //      hasReproductiveStatus
   //      hasSequenceVariant
   //      hasSurgicalTreatment
   //      hasTotalNottinghamScore
   //      hasTreatment
   //      hasTubuleScore
   //      hasTumorExtent
   //      hasWeight
   //      isDiagnosisOf
   //      isTumorSizeOf


   /**
    * Adds sizes and breast locations for neoplasms and location modifiers that do not already have them.
    * Previously handled the following relations:
    * Location :       hasBodySite handled by OwlRelationFinder
    * Medication :     hasRegimen and hasTreatment handled by OwlRelationFinder
    * Size :           hasTumorSize and hasRadiologicTumorSize handled by OwlRelationFinder
    * DiagnosticTest : hasMethod handled by OwlRelationFinder
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Relations not defined in the Ontology ..." );
      final Collection<String> neoplasmUris = Arrays.asList( OwlConstants.NEOPLASM_URIS );
      // Get all hasBodySite relations in the document.
      final Collection<LocationOfTextRelation> hasBodySites = JCasUtil.select( jCas, LocationOfTextRelation.class );
      // iterate over paragraphs.
      final Collection<Paragraph> paragraphs = JCasUtil.select( jCas, Paragraph.class );
      for ( Paragraph paragraph : paragraphs ) {
         final List<IdentifiedAnnotation> neoplasmList = neoplasmUris.stream()
               .map( u -> OwlOntologyConceptUtil.getAnnotationsByUriBranch( jCas, paragraph, u ) )
               .flatMap( Collection::stream )
               .sorted( ByBegin )
               .collect( Collectors.toList() );
         if ( neoplasmList.isEmpty() ) {
            continue;
         }
         findLaterality( jCas, paragraph, neoplasmList );
         // if there are breast sites then there may be breast site modifiers
         final Collection<IdentifiedAnnotation> breasts
               = OwlOntologyConceptUtil.getAnnotationsByUriBranch( jCas, paragraph, OwlConstants.BREAST_URI );
         if ( !breasts.isEmpty() ) {
            final List<IdentifiedAnnotation> breastNeoplasms
                  = breasts.stream()
                  .map( a -> RelationUtil.getAllRelated( hasBodySites, a ) )
                  .flatMap( Collection::stream )
                  .filter( neoplasmList::contains )
                  .distinct()
                  .sorted( ByBegin )
                  .collect( Collectors.toList() );
            if ( !breastNeoplasms.isEmpty() ) {
               findClockwise( jCas, paragraph, neoplasmList );
               findQuadrant( jCas, paragraph, neoplasmList );
            }
         }
         findMetastasis( jCas, paragraph, neoplasmList );
      }
      LOGGER.info( "Finished Processing" );
   }

   /**
    * Neoplasm to laterality
    *
    * @param jCas      -
    * @param paragraph -
    * @param neoplasms -
    */
   static private void findLaterality( final JCas jCas, final Paragraph paragraph,
                                       final List<IdentifiedAnnotation> neoplasms ) {
      final List<IdentifiedAnnotation> lateralityList
            = getAnnotationList( jCas, paragraph, OwlConstants.LATERALITY_URI );
      if ( lateralityList.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> lateralityMap
            = RelationUtil.createCandidateMap( jCas, lateralityList, neoplasms );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : lateralityMap.entrySet() ) {
         final IdentifiedAnnotation side = entry.getKey();
         entry.getValue().forEach( n -> RelationUtil.createRelation( jCas, n, side, HAS_LATERALITY ) );
         entry.getValue().forEach( n -> debugOut( HAS_LATERALITY + "  " + n.getCoveredText() + "  " + side.getCoveredText() ) );
      }
   }

   /**
    * Breast Neoplasm to Clockwise position
    *
    * @param jCas            -
    * @param paragraph       -
    * @param breastNeoplasms -
    */
   static private void findClockwise( final JCas jCas, final Paragraph paragraph, final List<IdentifiedAnnotation> breastNeoplasms ) {
      final List<IdentifiedAnnotation> clockList
            = getAnnotationList( jCas, paragraph, OwlConstants.CLOCKFACE_POSITION_URI );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> clockToNeoplasms
            = RelationUtil.createCandidateMap( jCas, clockList, breastNeoplasms );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : clockToNeoplasms.entrySet() ) {
         final IdentifiedAnnotation clock = entry.getKey();
         entry.getValue().forEach( n -> RelationUtil.createRelation( jCas, n, clock, HAS_CLOCKFACE ) );
         entry.getValue().forEach( n -> debugOut( HAS_CLOCKFACE + "  " + n.getCoveredText() + "  " + clock.getCoveredText() ) );
      }
   }

   /**
    * Breast Neoplasm to quadrant position
    *
    * @param jCas            -
    * @param paragraph       -
    * @param breastNeoplasms -
    */
   static private void findQuadrant( final JCas jCas, final Paragraph paragraph, final List<IdentifiedAnnotation> breastNeoplasms ) {
      final List<IdentifiedAnnotation> quadrantList
            = getAnnotationList( jCas, paragraph, OwlConstants.QUADRANT_URI );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> quadrantToNeoplasms
            = RelationUtil.createCandidateMap( jCas, quadrantList, breastNeoplasms );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : quadrantToNeoplasms.entrySet() ) {
         final IdentifiedAnnotation quadrant = entry.getKey();
         entry.getValue().forEach( n -> RelationUtil.createRelation( jCas, n, quadrant, HAS_QUADRANT ) );
         entry.getValue().forEach( n -> debugOut( HAS_QUADRANT + "  " + n.getCoveredText() + "  " + quadrant.getCoveredText() ) );
      }
   }

   /**
    * Metastasis to primary neoplasm
    *
    * @param jCas      -
    * @param paragraph -
    * @param neoplasms -
    */
   static private void findMetastasis( final JCas jCas, final Paragraph paragraph, final List<IdentifiedAnnotation> neoplasms ) {
      final List<IdentifiedAnnotation> metastasisList
            = getAnnotationList( jCas, paragraph, OwlConstants.CANCER_OWL + "#Metastatic_Neoplasm" );
      if ( metastasisList.isEmpty() ) {
         return;
      }
      // remove metastases from list of neoplasms
      neoplasms.removeAll( metastasisList );
      final List<IdentifiedAnnotation> affirmed = neoplasms.stream()
            .filter( n -> n.getPolarity() >= 0 )
            .sorted( ByBegin )
            .collect( Collectors.toList() );
      if ( affirmed.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> neoplasmToMetastases
            = RelationUtil.createCandidateMap( jCas, affirmed, metastasisList );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : neoplasmToMetastases.entrySet() ) {
         final IdentifiedAnnotation neoplasm = entry.getKey();
         entry.getValue().forEach( m -> RelationUtil.createRelation( jCas, m, neoplasm, METASTASIS_OF ) );
         entry.getValue().forEach( m -> debugOut( METASTASIS_OF + "  " + neoplasm.getCoveredText() + "  " + m.getCoveredText() ) );
      }
   }

   static private final Comparator<Annotation> ByBegin = ( a1, a2 ) -> a1.getBegin() - a2.getBegin();

   static private List<IdentifiedAnnotation> getAnnotationList( final JCas jcas,
                                                                final Annotation lookupWindow,
                                                                final String uri ) {
      return OwlOntologyConceptUtil.getAnnotationsByUriBranch( jcas, lookupWindow, uri ).stream()
            .sorted( ByBegin )
            .collect( Collectors.toList() );
   }

   static private void debugOut( final String text ) {
      if ( DEBUG_OUT ) {
         LOGGER.info( text );
      }
   }


}
