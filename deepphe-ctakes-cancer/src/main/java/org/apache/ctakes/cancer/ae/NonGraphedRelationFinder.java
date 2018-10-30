package org.apache.ctakes.cancer.ae;


import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

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
final public class NonGraphedRelationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "NonGraphedRelationFinder" );

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

   static private final Predicate<IdentifiedAnnotation> wantedForFact
         = a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT
                && a.getUncertainty() != CONST.NE_UNCERTAINTY_PRESENT
                && !a.getGeneric()
                && !a.getConditional();

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
      LOGGER.info( "Removing improper Relations ..." );
      final Collection<BinaryTextRelation> locations = JCasUtil.select( jCas, BinaryTextRelation.class );
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      final Collection<String> quadrantUris = SearchUtil.getBranchUris( graphDb, UriConstants.QUADRANT );
      final Collection<BinaryTextRelation> removals = new ArrayList<>();
      for ( BinaryTextRelation relation : locations ) {
         if ( RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE.equals( relation.getCategory() ) ) {
            final Annotation a1 = relation.getArg1().getArgument();
            final Annotation a2 = relation.getArg2().getArgument();
            if ( (IdentifiedAnnotation.class.isInstance( a2 )
                  && quadrantUris.contains( Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)a2 ) ))
                 ||
                 (IdentifiedAnnotation.class.isInstance( a1 )
                  && quadrantUris.contains( Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)a1 ) )) ) {
               removals.add( relation );
            }
         }
      }
      removals.forEach( BinaryTextRelation::removeFromIndexes );

      LOGGER.info( "Finding Relations not defined in the Ontology ..." );
      // Get all hasBodySite relations in the document.
      final Collection<String> cancerUris = UriConstants.getCancerUris();
      final Collection<String> primaryUris = UriConstants.getPrimaryUris();
      final Collection<String> metastasisUris = UriConstants.getMetastasisUris();
      final Collection<String> genericUris = UriConstants.getGenericTumorUris();

      final Collection<LocationOfTextRelation> hasBodySites = JCasUtil.select( jCas, LocationOfTextRelation.class );

      final List<IdentifiedAnnotation> allPrimaryList
            = Neo4jOntologyConceptUtil
            .getUriAnnotationsByUris( JCasUtil.select( jCas, IdentifiedAnnotation.class ), primaryUris ).values()
            .stream()
            .flatMap( Collection::stream )
            .filter( wantedForFact )
            .distinct()
            .sorted( Comparator.comparingInt( Annotation::getBegin ) )
            .collect( Collectors.toList() );

      // iterate over paragraphs.
      final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap
            = JCasUtil.indexCovered( jCas, Paragraph.class, IdentifiedAnnotation.class );
      final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences
            = JCasUtil.indexCovering( jCas, IdentifiedAnnotation.class, Sentence.class );

      for ( Map.Entry<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotations : paragraphAnnotationMap
            .entrySet() ) {
         final Paragraph paragraph = paragraphAnnotations.getKey();
         final Collection<IdentifiedAnnotation> annotations = paragraphAnnotations.getValue();
         final List<IdentifiedAnnotation> cancerList
               = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, cancerUris ).values().stream()
                                         .flatMap( Collection::stream )
                                         .filter( wantedForFact )
                                                                     .distinct()
                                                                     .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                                                     .collect( Collectors.toList() );

         final List<IdentifiedAnnotation> primaryList
               = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, primaryUris ).values().stream()
                                         .flatMap( Collection::stream )
                                         .filter( wantedForFact )
                                         .distinct()
                                         .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                         .collect( Collectors.toList() );

         final List<IdentifiedAnnotation> metastasisList
               = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, metastasisUris ).values().stream()
                                         .flatMap( Collection::stream )
                                         .filter( wantedForFact )
                                         .distinct()
                                         .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                         .collect( Collectors.toList() );
         final List<IdentifiedAnnotation> genericList
               = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, genericUris ).values().stream()
                                         .flatMap( Collection::stream )
                                         .filter( wantedForFact )
                                         .distinct()
                                         .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                         .collect( Collectors.toList() );
         final List<IdentifiedAnnotation> tumorList = new ArrayList<>( primaryList );
         tumorList.addAll( metastasisList );
         tumorList.addAll( genericList );
         tumorList.sort( Comparator.comparingInt( Annotation::getBegin ) );

         cancerList.addAll( primaryList );
         cancerList.addAll( genericList );
         cancerList.sort( Comparator.comparingInt( Annotation::getBegin ) );

         if ( !cancerList.isEmpty() ) {
            findDiagnoses( jCas, coveringSentences, paragraph, cancerList, tumorList );
            findMetastasis( jCas, coveringSentences, paragraph, cancerList, metastasisList );
            findTnm( jCas, coveringSentences, paragraph, cancerList );
            findStage( jCas, paragraph, cancerList );
         }
         findTumorExtent( jCas, coveringSentences, paragraph, tumorList );
         findReceptorStatus( jCas, coveringSentences, paragraph, cancerList, allPrimaryList );

         // if there are breast sites then there may be breast site modifiers
         final List<IdentifiedAnnotation> breastList
               = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, paragraph, UriConstants.BREAST ).stream()
                                         .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                         .collect( Collectors.toList() );
         if ( !breastList.isEmpty() ) {
            final List<IdentifiedAnnotation> breastTumors
                  = breastList.stream()
                              .map( a -> RelationUtil.getAllRelated( hasBodySites, a ) )
                              .flatMap( Collection::stream )
                              .filter( tumorList::contains )
                              .distinct()
                              .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                              .collect( Collectors.toList() );
            if ( !breastTumors.isEmpty() ) {
               findClockwise( jCas, coveringSentences, paragraph, tumorList );
               findQuadrant( jCas, coveringSentences, paragraph, tumorList );
            }
         }
      }

      LOGGER.info( "Finished Processing" );
   }


   /**
    * Tumor to size
    *
    * @param jCas      -
    * @param paragraph -
    * @param tumors -
    */
   static private void findSize( final JCas jCas,
                                 final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                 final Paragraph paragraph,
                                 final List<IdentifiedAnnotation> tumors ) {
      final List<IdentifiedAnnotation> sizeList
            = getAnnotationList( jCas, paragraph, UriConstants.SIZE );
      if ( sizeList.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sizeMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, tumors, sizeList, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : sizeMap.entrySet() ) {
         final IdentifiedAnnotation tumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, tumor, s, HAS_SIZE ) );
         entry.getValue()
              .forEach( s -> debugOut( HAS_SIZE + "  " + tumor.getCoveredText() + "  " + s.getCoveredText() ) );
      }
   }

   /**
    * tumor to lesion (tumor extent)
    *
    * @param jCas      -
    * @param paragraph -
    * @param tumors    -
    */
   static private void findTumorExtent( final JCas jCas,
                                        final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                        final Paragraph paragraph,
                                        final List<IdentifiedAnnotation> tumors ) {
      final List<IdentifiedAnnotation> lesionList
            = getWantedAnnotationList( jCas, paragraph, UriConstants.LESION );
      if ( lesionList.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> lesionMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, tumors, lesionList, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : lesionMap.entrySet() ) {
         final IdentifiedAnnotation tumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, tumor, s, HAS_TUMOR_EXTENT ) );
         entry.getValue()
              .forEach( l -> debugOut( HAS_TUMOR_EXTENT + "  " + tumor.getCoveredText() + "  " + l.getCoveredText() ) );
      }
   }


   /**
    * Tumor to receptor status
    *
    * @param jCas      -
    * @param paragraph -
    * @param tumors    -
    */
   static private void findReceptorStatus( final JCas jCas,
                                           final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                           final Paragraph paragraph,
                                           final List<IdentifiedAnnotation> tumors ) {
      final List<IdentifiedAnnotation> statusList
            = getAnnotationList( jCas, paragraph, UriConstants.RECEPTOR_STATUS );
      if ( statusList.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> statusMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, tumors, statusList, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : statusMap.entrySet() ) {
         final IdentifiedAnnotation tumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, tumor, s, HAS_RECEPTOR_STATUS ) );
         entry.getValue().forEach( s -> debugOut(
               HAS_RECEPTOR_STATUS + "  " + tumor.getCoveredText() + "  " + s.getCoveredText() ) );
      }
   }

   /**
    * Tumor to receptor status.  Any receptor status that has not gone to a tumor can go to a cancer.
    *
    * @param jCas      -
    * @param paragraph -
    * @param tumors    -
    * @param cancers   -
    */
   static private void findReceptorStatus( final JCas jCas,
                                           final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                           final Paragraph paragraph,
                                           final List<IdentifiedAnnotation> tumors,
                                           final List<IdentifiedAnnotation> cancers ) {
      final List<IdentifiedAnnotation> statusList
            = getAnnotationList( jCas, paragraph, UriConstants.RECEPTOR_STATUS );
      if ( statusList.isEmpty() ) {
         return;
      }
      final List<IdentifiedAnnotation> unusedStatus = new ArrayList<>( statusList );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> statusMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, tumors, statusList, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : statusMap.entrySet() ) {
         final IdentifiedAnnotation tumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, tumor, s, HAS_RECEPTOR_STATUS ) );
         entry.getValue().forEach( s -> debugOut(
               HAS_RECEPTOR_STATUS + "  " + tumor.getCoveredText() + "  " + s.getCoveredText() ) );

         for ( IdentifiedAnnotation status : entry.getValue() ) {
            final String uri = Neo4jOntologyConceptUtil.getUri( status );
            if ( uri.startsWith( "Estrogen" ) ) {
               RelationUtil.createRelation( jCas, tumor, status, HAS_ER_STATUS );
            } else if ( uri.startsWith( "Progesterone" ) ) {
               RelationUtil.createRelation( jCas, tumor, status, HAS_PR_STATUS );
            } else if ( uri.startsWith( "HER2_Neu" ) ) {
               RelationUtil.createRelation( jCas, tumor, status, HAS_HER2_STATUS );
            }
         }

         unusedStatus.removeAll( entry.getValue() );
      }
      if ( unusedStatus.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> cancerStatusMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, cancers, unusedStatus, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : cancerStatusMap.entrySet() ) {
         final IdentifiedAnnotation cancer = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, cancer, s, HAS_RECEPTOR_STATUS ) );
         entry.getValue().forEach( s -> debugOut( "Cancer " +
                                                  HAS_RECEPTOR_STATUS + "  " + cancer.getCoveredText() + "  " +
                                                  s.getCoveredText() ) );

         for ( IdentifiedAnnotation status : entry.getValue() ) {
            final String uri = Neo4jOntologyConceptUtil.getUri( status );
            if ( uri.startsWith( "Estrogen" ) ) {
               RelationUtil.createRelation( jCas, cancer, status, HAS_ER_STATUS );
            } else if ( uri.startsWith( "Progesterone" ) ) {
               RelationUtil.createRelation( jCas, cancer, status, HAS_PR_STATUS );
            } else if ( uri.startsWith( "HER2_Neu" ) ) {
               RelationUtil.createRelation( jCas, cancer, status, HAS_HER2_STATUS );
            }
         }

      }
   }

   /**
    * neoplasm to tnm
    *
    * @param jCas      -
    * @param paragraph -
    * @param cancers -
    */
   static private void findTnm( final JCas jCas,
                                final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                final Paragraph paragraph,
                                final List<IdentifiedAnnotation> cancers ) {
      final List<IdentifiedAnnotation> tnmList
            = getAnnotationList( jCas, paragraph, UriConstants.TNM );
      if ( tnmList.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> tnmMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, cancers, tnmList, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : tnmMap.entrySet() ) {
         final IdentifiedAnnotation cancer = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, cancer, s, HAS_TNM ) );
         entry.getValue()
              .forEach( s -> debugOut( HAS_TNM + "  " + cancer.getCoveredText() + "  " + s.getCoveredText() ) );
         for ( IdentifiedAnnotation tnm : entry.getValue() ) {
            final String text = tnm.getCoveredText();
            if ( text.startsWith( "p" ) ) {
               final char c = text.charAt( 1 );
               switch ( c ) {
                  case 'T' : {
                     RelationUtil.createRelation( jCas, cancer, tnm, RelationConstants.HAS_PATHOLOGIC_T );
                     break;
                  }
                  case 'N' : {
                     RelationUtil.createRelation( jCas, cancer, tnm, RelationConstants.HAS_PATHOLOGIC_N );
                     break;
                  }
                  case 'M' : {
                     RelationUtil.createRelation( jCas, cancer, tnm, RelationConstants.HAS_PATHOLOGIC_M );
                     break;
                  }
               }
            } else {
               char c = text.charAt( 0 );
               if ( text.startsWith( "c" ) ) {
                  c = text.charAt( 1 );
               }
               switch ( c ) {
                  case 'T' : {
                     RelationUtil.createRelation( jCas, cancer, tnm, RelationConstants.HAS_CLINICAL_T );
                     break;
                  }
                  case 'N' : {
                     RelationUtil.createRelation( jCas, cancer, tnm, RelationConstants.HAS_CLINICAL_N );
                     break;
                  }
                  case 'M' : {
                     RelationUtil.createRelation( jCas, cancer, tnm, RelationConstants.HAS_CLINICAL_M );
                     break;
                  }
               }
            }
         }
      }
   }

   /**
    * Neoplasm to stage
    *
    * @param jCas      -
    * @param paragraph -
    * @param cancers -
    */
   static private void findStage( final JCas jCas, final Paragraph paragraph,
                                  final List<IdentifiedAnnotation> cancers ) {

      final List<IdentifiedAnnotation> stageList
              = getAnnotationList( jCas, paragraph, UriConstants.STAGE );
      if ( stageList.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> stageMap
              = RelationUtil.createSourceTargetMap( jCas, cancers, stageList, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : stageMap.entrySet() ) {
         final IdentifiedAnnotation cancer = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, cancer, s, HAS_STAGE ) );
         entry.getValue()
                 .forEach( s -> debugOut( HAS_STAGE + "  " + cancer.getCoveredText() + "  " + s.getCoveredText() ) );
      }
   }

   /**
    * Breast Tumor to Clockwise position
    *
    * @param jCas         -
    * @param paragraph    -
    * @param breastTumors -
    */
   static private void findClockwise( final JCas jCas,
                                      final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                      final Paragraph paragraph,
                                      final List<IdentifiedAnnotation> breastTumors ) {
      final List<IdentifiedAnnotation> clockList
            = getAnnotationList( jCas, paragraph, UriConstants.CLOCKFACE );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> clockMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, breastTumors, clockList, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : clockMap.entrySet() ) {
         final IdentifiedAnnotation breastTumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, breastTumor, s, HAS_CLOCKFACE ) );
         entry.getValue().forEach( s -> debugOut(
               HAS_CLOCKFACE + "  " + breastTumor.getCoveredText() + "  " + s.getCoveredText() ) );
      }
   }

   /**
    * Breast Tumor to quadrant position
    *
    * @param jCas            -
    * @param paragraph       -
    * @param breastTumors -
    */
   static private void findQuadrant( final JCas jCas,
                                     final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                     final Paragraph paragraph,
                                     final List<IdentifiedAnnotation> breastTumors ) {
      final List<IdentifiedAnnotation> quadrantList
            = getAnnotationList( jCas, paragraph, UriConstants.QUADRANT );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> quadrantMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, breastTumors, quadrantList, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : quadrantMap.entrySet() ) {
         final IdentifiedAnnotation breastTumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, breastTumor, s, HAS_QUADRANT ) );
         entry.getValue().forEach( s -> debugOut(
               HAS_QUADRANT + "  " + breastTumor.getCoveredText() + "  " + s.getCoveredText() ) );
      }
   }

   /**
    * Metastasis to primary neoplasm
    *
    * @param jCas      -
    * @param paragraph -
    * @param cancers -
    * @param metastases  -
    */
   static private void findMetastasis( final JCas jCas,
                                       final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                       final Paragraph paragraph,
                                       final List<IdentifiedAnnotation> cancers,
                                       final List<IdentifiedAnnotation> metastases ) {
      if ( cancers.isEmpty() || metastases.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> metastasisMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, cancers, metastases, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : metastasisMap.entrySet() ) {
         final IdentifiedAnnotation cancer = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, s, cancer, METASTASIS_OF ) );
         entry.getValue()
              .forEach( s -> debugOut( METASTASIS_OF + " " + s.getCoveredText() + "  " + cancer.getCoveredText() ) );
      }
   }

   /**
    * Metastasis to primary neoplasm
    *
    * @param jCas      -
    * @param paragraph -
    * @param cancers   -
    * @param tumors    -
    */
   static private void findDiagnoses( final JCas jCas,
                                      final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                      final Paragraph paragraph,
                                      final List<IdentifiedAnnotation> cancers,
                                      final List<IdentifiedAnnotation> tumors ) {
      if ( cancers.isEmpty() || tumors.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> metastasisMap
            = RelationUtil.createSourceTargetMap( jCas, coveringSentences, cancers, tumors, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : metastasisMap.entrySet() ) {
         final IdentifiedAnnotation cancer = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, s, cancer, HAS_DIAGNOSIS ) );
         entry.getValue()
              .forEach( s -> debugOut( HAS_DIAGNOSIS + " " + s.getCoveredText() + "  " + cancer.getCoveredText() ) );
      }
   }

   static private List<IdentifiedAnnotation> getAnnotationList( final JCas jcas,
                                                                final Annotation lookupWindow,
                                                                final String uri ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jcas, lookupWindow, uri )
                                     .stream()
                                     .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                     .collect( Collectors.toList() );
   }

   static private List<IdentifiedAnnotation> getWantedAnnotationList( final JCas jcas,
                                                                      final Annotation lookupWindow,
                                                                      final String uri ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jcas, lookupWindow, uri )
                                     .stream()
                                     .filter( wantedForFact )
                                     .distinct()
                                     .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                     .collect( Collectors.toList() );
   }

   static private void debugOut( final String text ) {
      if ( DEBUG_OUT ) {
         LOGGER.info( text );
      }
   }


}
