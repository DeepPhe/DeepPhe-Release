package org.healthnlp.deepphe.nlp.ae;


import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.core.document.SectionType;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.relation.RelationUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.nlp.phenotype.receptor.StatusFinder;
import org.healthnlp.deepphe.nlp.phenotype.tnm.TnmFinder;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;


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


//   From Jeremy Warner to Everyone:  12:35 PM
//https://hemonc.org/wiki/Biomarkers
//PIK3CA
//BRCA1
//BRCA2
//PALB1
//PALB2
//Genes for breast/ovarian: PIK3CA (and the synonyms/shorthand such as PI3K), BRCA1/2, PALB1/2
//Proteins: ER, PR, HER2
//Aliases for PIK3CA: PIK3CA, CLOVE, CWS5, MCAP, MCM, MCMTC, PI3K, p110-alpha, PI3K-alpha, phosphatidylinositol-4,
// 5-bisphosphate 3-kinase catalytic subunit alpha, CLAPO
//Maybe for breast Ki-67


   static private final Logger LOGGER = Logger.getLogger( "NonGraphedRelationFinder" );

   static private final boolean DEBUG_OUT = false;

   static private final Predicate<IdentifiedAnnotation> wantedForFact
         = a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT
                && a.getUncertainty() != CONST.NE_UNCERTAINTY_PRESENT
                && !a.getGeneric()
                && !a.getConditional()
                && CONST.ATTR_SUBJECT_PATIENT.equals( a.getSubject() );

   static private Collection<String> BREAST_URIS;
   static private Collection<String> QUADRANT_URIS;
   static private Collection<String> CLOCK_URIS;
   static private Collection<String> MASS_URIS;

   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      if ( BREAST_URIS == null ) {
         BREAST_URIS = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.BREAST );
//         BREAST_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Nipple") );
      }
      if ( QUADRANT_URIS == null ) {
         QUADRANT_URIS = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT ) );
         // TODO - Need the following for dphe-cr, but they toss the eval for dphe-xn
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Nipple" ) );
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Areola" ) );
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Central_Portion_Of_The_Breast" ) );
//         QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Subareolar_Region" ) );
      }
      if ( CLOCK_URIS == null ) {
         CLOCK_URIS = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.CLOCKFACE );
      }
      if ( MASS_URIS == null ) {
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                .getGraph();
         MASS_URIS = UriConstants.getMassUris( graphDb );
      }
   }





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
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                 .getGraph();
      final Collection<BinaryTextRelation> removals = new ArrayList<>();
      for ( BinaryTextRelation relation : relations ) {
         final String category = relation.getCategory();
         if ( RelationConstants.isHasSiteRelation( category ) ) {
//         if ( DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE.equals( category ) ) {
//            final Annotation a1 = relation.getArg1().getArgument();
//            final Annotation a2 = relation.getArg2().getArgument();
//            if ( ( a2 instanceof IdentifiedAnnotation
//                   && QUADRANT_URIS.contains( Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)a2 ) ))
//                 ||
//                 ( a1 instanceof IdentifiedAnnotation
//                   && QUADRANT_URIS.contains( Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)a1 ) )) ) {
//               removals.add( relation );
//            }
            // TODO: !!! Is this still right?  Status should have come from ontology and removing here gets rid of it.
         } else if ( HAS_ER_STATUS.equals( category ) || HAS_PR_STATUS.equals( category ) ||
                     HAS_HER2_STATUS.equals( category ) ) {
            removals.add( relation );
         }
      }
      removals.forEach( BinaryTextRelation::removeFromIndexes );

      LOGGER.info( "Finding Relations not defined in the Ontology ..." );
      // Get all hasBodySite relations in the document.
      final Collection<String> massUris = UriConstants.getMassUris( graphDb );
      final Collection<String> massNeoplasmUris = UriConstants.getMassNeoplasmUris( graphDb );
      final Collection<String> neoplasmUris = UriConstants.getNeoplasmUris( graphDb );
      final Collection<String> metastasisUris = UriConstants.getMetastasisUris( graphDb );

      final Collection<BinaryTextRelation> hasBodySites
            = JCasUtil.select( jCas, BinaryTextRelation.class )
                      .stream()
                      .filter( r -> RelationConstants.isHasSiteRelation( r.getCategory() ) )
                      .collect( Collectors.toList() );

      final Collection<IdentifiedAnnotation> allAnnotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );

      // TODO : Make Neo4jOntologyConceptUtil.getAnnotationsByUris( allAnnotations, uris ) that doesn't make a map
      final List<IdentifiedAnnotation> massList
            = Neo4jOntologyConceptUtil
            .getUriAnnotationsByUris( allAnnotations, massUris ).values()
            .stream()
            .flatMap( Collection::stream )
            .filter( a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT )
            .distinct()
            .sorted( Comparator.comparingInt( Annotation::getBegin )
                               .thenComparing( Annotation::getEnd ) )
            .collect( Collectors.toList() );

      final List<IdentifiedAnnotation> allMassNeoplasmList
            = Neo4jOntologyConceptUtil
            .getUriAnnotationsByUris( allAnnotations, massNeoplasmUris ).values()
            .stream()
            .flatMap( Collection::stream )
            .filter( a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT )
            .distinct()
            .sorted( Comparator.comparingInt( Annotation::getBegin )
                               .thenComparing( Annotation::getEnd ) )
            .collect( Collectors.toList() );

//      final Collection<IdentifiedAnnotation> allBreasts
//            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.BREAST );
//      final Collection<IdentifiedAnnotation> allNipples
//            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, "Nipple" );
      final List<IdentifiedAnnotation> allBreastList
            = Neo4jOntologyConceptUtil
            .getUriAnnotationsByUris( allAnnotations, BREAST_URIS ).values()
            .stream()
            .flatMap( Collection::stream )
            .sorted( Comparator.comparingInt( Annotation::getBegin )
                                               .thenComparing( Annotation::getEnd ) )
                            .collect( Collectors.toList() );
//          = new ArrayList<>( allBreasts );
//      allBreastList.addAll( allNipples );
//      allBreastList.sort( Comparator.comparingInt( Annotation::getBegin )
//                                    .thenComparing( Annotation::getEnd ) );

      final List<IdentifiedAnnotation> allBreastTumors
            = allBreastList.stream()
                           .map( a -> RelationUtil.getAllRelated( hasBodySites, a ) )
                           .flatMap( Collection::stream )
                           .filter( allMassNeoplasmList::contains )
                           .distinct()
                           .sorted( Comparator.comparingInt( Annotation::getBegin )
                                              .thenComparing( Annotation::getEnd ) )
                           .collect( Collectors.toList() );

      if ( !allBreastTumors.isEmpty() ) {
         ClockFaceFinder.findSimpleClocks( jCas );
      }

      // iterate over paragraphs.
      final Map<Segment, Collection<Paragraph>> sectionParagraphMap
            = JCasUtil.indexCovered( jCas, Segment.class, Paragraph.class );
      final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphAnnotationMap
            = JCasUtil.indexCovered( jCas, Paragraph.class, IdentifiedAnnotation.class );

      for ( Map.Entry<Segment, Collection<Paragraph>> sectionParagraphs : sectionParagraphMap.entrySet() ) {
         final boolean isMicroscopic = SectionType.Microscopic.isThisSectionType( sectionParagraphs.getKey() );
         final boolean isFinding = SectionType.Finding.isThisSectionType( sectionParagraphs.getKey() );
         final boolean isHistology = SectionType.HistologySummary.isThisSectionType( sectionParagraphs.getKey() );
         final boolean isReview = SectionType.ReviewSystems.isThisSectionType( sectionParagraphs.getKey() );
         final boolean isFamilyHistory = SectionType.FamilyHistory.isThisSectionType( sectionParagraphs.getKey() );
         for ( Paragraph paragraph : sectionParagraphs.getValue() ) {
            final Collection<IdentifiedAnnotation> annotations = paragraphAnnotationMap.get( paragraph );
            final List<IdentifiedAnnotation> massNeoplasmList
                  = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, massNeoplasmUris ).values().stream()
                                            .flatMap( Collection::stream )
                                            .filter( a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT )
                                            .distinct()
                                            .sorted( Comparator.comparingInt( Annotation::getBegin )
                                                               .thenComparing( Annotation::getEnd ) )
                                            .collect( Collectors.toList() );
            final List<IdentifiedAnnotation> neoplasmList
                  = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, neoplasmUris ).values().stream()
                                            .flatMap( Collection::stream )
                                            .filter( a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT )
                                            .distinct()
                                            .sorted( Comparator.comparingInt( Annotation::getBegin )
                                                               .thenComparing( Annotation::getEnd ) )
                                            .collect( Collectors.toList() );

            final List<IdentifiedAnnotation> metastasisList
                  = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, metastasisUris ).values().stream()
                                            .flatMap( Collection::stream )
                                            .filter( a -> a.getPolarity() != CONST.NE_POLARITY_NEGATION_PRESENT )
                                            .distinct()
                                            .sorted( Comparator.comparingInt( Annotation::getBegin )
                                                               .thenComparing( Annotation::getEnd ) )
                                            .collect( Collectors.toList() );

            if ( !massNeoplasmList.isEmpty() ) {
               findAllStages( jCas, annotations, massNeoplasmList, allMassNeoplasmList );
            }

            // finding all tumor extent slightly improves tumor scores
            findAllTumorExtent( jCas, annotations, massNeoplasmList, allMassNeoplasmList );
            if ( !allMassNeoplasmList.isEmpty() ) {
               findGleasonGrades( jCas, paragraph, massNeoplasmList, allMassNeoplasmList );
               findTnms( jCas, paragraph, massNeoplasmList, allMassNeoplasmList );
            }

            if ( isMicroscopic || isFinding || isHistology || isReview || isFamilyHistory ) {
               continue;
            }
            if ( !neoplasmList.isEmpty() ) {
               // Adding diagnoses leads to a tonne of FPs, both cancer and tumor.
               findAllDiagnoses( jCas, neoplasmList, massList );
               findMetastasis( jCas, neoplasmList, metastasisList );
            }
            if ( !allBreastTumors.isEmpty() ) {
               findAllStatus( jCas, paragraph, allMassNeoplasmList, allBreastTumors );
               // if there are breast sites then there may be breast site modifiers
//               final Collection<IdentifiedAnnotation> breasts
//                     = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, paragraph, UriConstants.BREAST );
//               final Collection<IdentifiedAnnotation> nipples
//                     = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, paragraph, "Nipple" );
//               final List<IdentifiedAnnotation> breastList = new ArrayList<>( breasts );
//               breastList.addAll( nipples );
//               breastList.sort( Comparator.comparingInt( Annotation::getBegin )
//                                          .thenComparing( Annotation::getEnd ) );
               final List<IdentifiedAnnotation> breastList
                     = allBreastList.stream()
                                    .filter( annotations::contains )
                                    .collect( Collectors.toList() );

               if ( !breastList.isEmpty() ) {
                  final List<IdentifiedAnnotation> breastTumors
                        = breastList.stream()
                                    .map( a -> RelationUtil.getAllRelated( hasBodySites, a ) )
                                    .flatMap( Collection::stream )
                                    .filter( allMassNeoplasmList::contains )
                                    .distinct()
                                    .sorted( Comparator.comparingInt( Annotation::getBegin )
                                                       .thenComparing( Annotation::getEnd ) )
                                    .collect( Collectors.toList() );
                  if ( !breastTumors.isEmpty() || !allBreastTumors.isEmpty() ) {
                     findAllClockwise( jCas, annotations, allMassNeoplasmList, allBreastTumors );
                     findAllQuadrants( jCas, annotations, allMassNeoplasmList, allBreastTumors );
                  }
               }
            }
         }
      }
   }


   /**
    * tumor to lesion (tumor extent)
    *
    * @param jCas      -
    * @param annotations -
    * @param windowMasses    -
    */
   static private void findAllTumorExtent( final JCas jCas,
                                           final Collection<IdentifiedAnnotation> annotations,
                                           final List<IdentifiedAnnotation> windowMasses,
                                           final List<IdentifiedAnnotation> allMasses ) {
      final List<IdentifiedAnnotation> massList
            = getWantedAnnotationList( annotations, MASS_URIS );
      if ( massList.isEmpty() ) {
         return;
      }
      final List<IdentifiedAnnotation> unusedMasses = new ArrayList<>( massList );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> massMap
            = RelationUtil.createSourceTargetMap( windowMasses, unusedMasses, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : massMap.entrySet() ) {
         final IdentifiedAnnotation tumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, tumor, s, HAS_TUMOR_EXTENT ) );
         unusedMasses.removeAll( entry.getValue() );
      }
      if ( unusedMasses.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> allMassesMap
            = RelationUtil.createSourceTargetMap( allMasses, unusedMasses, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : allMassesMap.entrySet() ) {
         final IdentifiedAnnotation tumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, tumor, s, HAS_TUMOR_EXTENT ) );
      }
   }


   static private void findAllStages( final JCas jCas,
                                      final Collection<IdentifiedAnnotation> windowAnnotations,
                                      final List<IdentifiedAnnotation> windowNeoplasms,
                                      final List<IdentifiedAnnotation> allNeoplasms ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                 .getGraph();
      final List<IdentifiedAnnotation> stageList
            = getAnnotationList( windowAnnotations, UriConstants.getCancerStages( graphDb ) );
      if ( stageList.isEmpty() ) {
         return;
      }
      final Collection<IdentifiedAnnotation> unusedStages = new ArrayList<>( stageList );
      findStage( jCas, stageList, windowNeoplasms, unusedStages );
      if ( unusedStages.isEmpty() ) {
         return;
      }
      findStage( jCas, stageList, allNeoplasms, unusedStages );
   }


   static private void findStage( final JCas jCas,
                                  final List<IdentifiedAnnotation> stages,
                                  final List<IdentifiedAnnotation> neoplasms,
                                  final Collection<IdentifiedAnnotation> unusedStages ) {

      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> stageMap
            = RelationUtil.createSourceTargetMap( neoplasms, stages, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : stageMap.entrySet() ) {
         final IdentifiedAnnotation cancer = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, cancer, s, HAS_STAGE ) );
         unusedStages.removeAll( entry.getValue() );
      }
   }

   static private void findAllStatus( final JCas jCas,
                                      final Paragraph paragraph,
                                         final List<IdentifiedAnnotation> breastTumors,
                                         final List<IdentifiedAnnotation> allBreastTumors ) {
      final List<IdentifiedAnnotation> statusList = StatusFinder.addReceptorStatuses( jCas, paragraph );
      if ( statusList.isEmpty() || (breastTumors.isEmpty() && allBreastTumors.isEmpty() ) ) {
         return;
      }
      SectionIdSetter.setContainerIds( jCas, statusList );
      final List<IdentifiedAnnotation> unusedStatus = new ArrayList<>( statusList );
      if ( !breastTumors.isEmpty() ) {
         final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> statusMap
               = RelationUtil.createSourceTargetMap( unusedStatus, breastTumors, false );
         for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : statusMap.entrySet() ) {
            final IdentifiedAnnotation status = entry.getKey();
//            final String uri = Neo4jOntologyConceptUtil.getUri( status );
            for ( IdentifiedAnnotation tumor : entry.getValue() ) {
//               if ( uri.contains( UriConstants.ER_STATUS ) || uri.equals( UriConstants.TRIPLE_NEGATIVE ) ) {
//                  RelationUtil.createRelation( jCas, tumor, status, HAS_ER_STATUS );
//               }
//               if ( uri.contains( UriConstants.PR_STATUS ) || uri.equals( UriConstants.TRIPLE_NEGATIVE ) ) {
//                  RelationUtil.createRelation( jCas, tumor, status, HAS_PR_STATUS );
//               }
//               if ( uri.contains( UriConstants.HER2_STATUS ) || uri.equals( UriConstants.TRIPLE_NEGATIVE ) ) {
//                  RelationUtil.createRelation( jCas, tumor, status, HAS_HER2_STATUS );
//               }
               RelationUtil.createRelation( jCas, tumor, status, has_Biomarker );
            }
            unusedStatus.remove( status );
         }
         if ( unusedStatus.isEmpty() || allBreastTumors.isEmpty() ) {
            return;
         }
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> allStatusMap
            = RelationUtil.createSourceTargetMap( unusedStatus, allBreastTumors, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : allStatusMap.entrySet() ) {
         final IdentifiedAnnotation status = entry.getKey();
//         final String uri = Neo4jOntologyConceptUtil.getUri( status );
         for ( IdentifiedAnnotation tumor : entry.getValue() ) {
//            if ( uri.contains( UriConstants.ER_STATUS ) || uri.equals( UriConstants.TRIPLE_NEGATIVE ) ) {
//               RelationUtil.createRelation( jCas, tumor, status, HAS_ER_STATUS );
//            }
//            if ( uri.contains( UriConstants.PR_STATUS ) || uri.equals( UriConstants.TRIPLE_NEGATIVE ) ) {
//               RelationUtil.createRelation( jCas, tumor, status, HAS_PR_STATUS );
//            }
//            if ( uri.contains( UriConstants.HER2_STATUS ) || uri.equals( UriConstants.TRIPLE_NEGATIVE ) ) {
//               RelationUtil.createRelation( jCas, tumor, status, HAS_HER2_STATUS );
//            }
            RelationUtil.createRelation( jCas, tumor, status, has_Biomarker );
         }
      }
   }

   static private void findAllClockwise( final JCas jCas,
                                         final Collection<IdentifiedAnnotation> annotations,
                                         final List<IdentifiedAnnotation> breastTumors,
                                         final List<IdentifiedAnnotation> allBreastTumors ) {
      final List<IdentifiedAnnotation> clockList
            = getAnnotationList( annotations, CLOCK_URIS );
      if ( clockList.isEmpty() ) {
         return;
      }
      final List<IdentifiedAnnotation> unusedClocks = new ArrayList<>( clockList );
      findClockwise( jCas, unusedClocks, breastTumors );
      if ( unusedClocks.isEmpty() ) {
         return;
      }
      findClockwise( jCas, unusedClocks, allBreastTumors );
   }


   static private void findClockwise( final JCas jCas,
                                      final List<IdentifiedAnnotation> unusedClocks,
                                      final List<IdentifiedAnnotation> breastTumors ) {
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> clockMap
            = RelationUtil.createSourceTargetMap( breastTumors, unusedClocks, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : clockMap.entrySet() ) {
         final IdentifiedAnnotation breastTumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, breastTumor, s, HAS_CLOCKFACE ) );
         unusedClocks.removeAll( entry.getValue() );
      }
   }


   static private void findAllQuadrants( final JCas jCas,
                                         final Collection<IdentifiedAnnotation> annotations,
                                         final List<IdentifiedAnnotation> breastTumors,
                                         final List<IdentifiedAnnotation> allBreastTumors ) {
      final List<IdentifiedAnnotation> quadrantList
            = getAnnotationList( annotations, QUADRANT_URIS );
      if ( quadrantList.isEmpty() ) {
         return;
      }
      final List<IdentifiedAnnotation> unusedQuadrants = new ArrayList<>( quadrantList );
      findQuadrant( jCas, unusedQuadrants, breastTumors );
      if ( unusedQuadrants.isEmpty() ) {
         return;
      }
      findQuadrant( jCas, unusedQuadrants, allBreastTumors );
   }


   static private void findQuadrant( final JCas jCas,
                                     final List<IdentifiedAnnotation> unusedQuadrants,
                                     final List<IdentifiedAnnotation> breastTumors ) {
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> quadrantMap
            = RelationUtil.createSourceTargetMap( breastTumors, unusedQuadrants, true );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : quadrantMap.entrySet() ) {
         final IdentifiedAnnotation breastTumor = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, breastTumor, s, HAS_QUADRANT ) );
         unusedQuadrants.removeAll( entry.getValue() );
      }
   }


   /**
    * Metastasis to primary neoplasm
    *
    * @param jCas      -
    * @param windowNeoplasms -
    * @param windowMetastases  -
    */
   static private void findMetastasis( final JCas jCas,
                                       final List<IdentifiedAnnotation> windowNeoplasms,
                                       final List<IdentifiedAnnotation> windowMetastases ) {
      if ( windowNeoplasms.isEmpty() || windowMetastases.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> metastasisMap
            = RelationUtil.createSourceTargetMap( windowNeoplasms, windowMetastases, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : metastasisMap.entrySet() ) {
         final IdentifiedAnnotation neoplasm = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, s, neoplasm, METASTASIS_OF ) );
      }
   }


   /**
    * Metastasis to primary neoplasm
    *
    * @param jCas      -
    * @param windowNeoplasms   -
    * @param windowMasses    -
    */
   static private void findAllDiagnoses( final JCas jCas,
                                         final List<IdentifiedAnnotation> windowNeoplasms,
                                         final List<IdentifiedAnnotation> windowMasses ) {
      if ( windowNeoplasms.isEmpty() || windowMasses.isEmpty() ) {
         return;
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> diagnosisMap
            = RelationUtil.createSourceTargetMap( windowNeoplasms, windowMasses, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : diagnosisMap.entrySet() ) {
         final IdentifiedAnnotation cancer = entry.getKey();
         entry.getValue().forEach( s -> RelationUtil.createRelation( jCas, s, cancer, HAS_DIAGNOSIS ) );
      }
   }


   static private void findGleasonGrades( final JCas jCas,
                                          final Paragraph paragraph,
                                          final List<IdentifiedAnnotation> windowMasses,
                                          final List<IdentifiedAnnotation> allMasses ) {
      final Collection<IdentifiedAnnotation> gleasons = GleasonFinder.findGleasonGrades( jCas, paragraph );
//      final Collection<IdentifiedAnnotation> gleasons
//            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, paragraph, "Gleason_Grade" );
      if ( gleasons.isEmpty() || (windowMasses.isEmpty() && allMasses.isEmpty()) ) {
         return;
      }
//      SectionIdSetter.setContainerIds( jCas, gleasons );
      final List<IdentifiedAnnotation> unusedGleasons = new ArrayList<>( gleasons );
      if ( !windowMasses.isEmpty() ) {
         final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> gradeMap
               = RelationUtil.createSourceTargetMap( unusedGleasons, windowMasses, false );
         for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : gradeMap.entrySet() ) {
            final IdentifiedAnnotation gleasonGrade = entry.getKey();
            entry.getValue().forEach( t -> RelationUtil.createRelation( jCas, t, gleasonGrade, HAS_GLEASON_SCORE ) );
            unusedGleasons.remove( gleasonGrade );
         }
         if ( unusedGleasons.isEmpty() || allMasses.isEmpty() ) {
            return;
         }
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> allGradesMap
            = RelationUtil.createSourceTargetMap( unusedGleasons, allMasses, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : allGradesMap.entrySet() ) {
         final IdentifiedAnnotation gleasonGrade = entry.getKey();
         entry.getValue().forEach( t -> RelationUtil.createRelation( jCas, t, gleasonGrade, HAS_GLEASON_SCORE ) );
      }
   }


   static private void findTnms( final JCas jCas,
                                          final Paragraph paragraph,
                                          final List<IdentifiedAnnotation> windowNeoplasmMasses,
                                          final List<IdentifiedAnnotation> allNeoplasmMasses ) {
      Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas,"TNM_Stage" )
                              .forEach( Annotation::removeFromIndexes );
      final Collection<IdentifiedAnnotation> tnms = TnmFinder.addTnms( jCas, paragraph );
      if ( tnms.isEmpty() || (windowNeoplasmMasses.isEmpty() && allNeoplasmMasses.isEmpty()) ) {
         return;
      }
//      SectionIdSetter.setContainerIds( jCas, tnms );
      final List<IdentifiedAnnotation> unusedTnms = new ArrayList<>( tnms );
      if ( !windowNeoplasmMasses.isEmpty() ) {
         final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> tnmMap
               = RelationUtil.createSourceTargetMap( unusedTnms, windowNeoplasmMasses, false );
         for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : tnmMap.entrySet() ) {
            final IdentifiedAnnotation tnm = entry.getKey();
            final String uri = Neo4jOntologyConceptUtil.getUri( tnm );
            entry.getValue().forEach( t -> RelationUtil.createRelation( jCas, t, tnm, getTnmRelation( uri ) ) );
            unusedTnms.remove( tnm );
         }
         if ( unusedTnms.isEmpty() || allNeoplasmMasses.isEmpty() ) {
            return;
         }
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> allTnmMap
            = RelationUtil.createSourceTargetMap( unusedTnms, allNeoplasmMasses, false );
      for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : allTnmMap.entrySet() ) {
         final IdentifiedAnnotation tnm = entry.getKey();
         final String uri = Neo4jOntologyConceptUtil.getUri( tnm );
         entry.getValue().forEach( t -> RelationUtil.createRelation( jCas, t, tnm, getTnmRelation( uri ) ) );
      }
   }

   static private String getTnmRelation( final String uri ) {
      final char c = uri.charAt( 0 );
      switch ( c ) {
         case 'T' : return HAS_CLINICAL_T;
         case 'N' : return HAS_CLINICAL_N;
         case 'M' : return HAS_CLINICAL_M;
         case 'P' : {
            final char c2 = uri.charAt( 1 );
            switch ( c2 ) {
               case 'T':
                  return HAS_PATHOLOGIC_T;
               case 'N':
                  return HAS_PATHOLOGIC_N;
               case 'M':
                  return HAS_PATHOLOGIC_M;
            }
         }
      }
      return HAS_UNKNOWN_STAGE;
   }


   static private List<IdentifiedAnnotation> getAnnotationList( final Collection<IdentifiedAnnotation> annotations,
                                                                final Collection<String> uris ) {
      return Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, uris ).values()
                                     .stream()
                                     .flatMap( Collection::stream )
                                     .distinct()
                                     .sorted( Comparator.comparingInt( Annotation::getBegin )
                                                        .thenComparing( Annotation::getEnd ) )
                                     .collect( Collectors.toList() );
   }



   static private List<IdentifiedAnnotation> getWantedAnnotationList( final Collection<IdentifiedAnnotation> annotations,
                                                                      final Collection<String> uris ) {
      return Neo4jOntologyConceptUtil.getUriAnnotationsByUris( annotations, uris ).values()
                                     .stream()
                                     .flatMap( Collection::stream )
                                     .filter( wantedForFact )
                                     .distinct()
                                     .sorted( Comparator.comparingInt( Annotation::getBegin )
                                                        .thenComparing( Annotation::getEnd ) )
                                     .collect( Collectors.toList() );
   }


}
