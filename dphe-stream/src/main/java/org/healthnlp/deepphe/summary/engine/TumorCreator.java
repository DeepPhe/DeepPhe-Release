package org.healthnlp.deepphe.summary.engine;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Fact;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.Tumor;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.behavior.BehaviorCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.behavior.BehaviorUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.biomarker.Biomarker;
import org.healthnlp.deepphe.summary.attribute.grade.GradeCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.grade.GradeUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.histology.Histology;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralityCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.stage.StageCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.stage.StageUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.M_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.N_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.T_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.TnmCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.Topography;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorUriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
final public class TumorCreator {

   static private final Logger LOGGER = Logger.getLogger( "TumorCreator" );

   static public final StringBuilder DEBUG_SB = new StringBuilder();

   private TumorCreator() {}


   static public Tumor createTumor( final ConceptAggregate neoplasm,
                                    final Map<ConceptAggregate, Fact> factMap ) {
      DEBUG_SB.append( "=======================================================================\n" )
              .append( neoplasm.getPatientId() )
              .append( "\n" );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massNeoplasmUris = UriConstants.getMassNeoplasmUris( graphDb );
      final Predicate<ConceptAggregate> isNeoplasm = c -> c.getAllUris()
                                                           .stream()
                                                           .anyMatch( massNeoplasmUris::contains );
      final Collection<ConceptAggregate> allConcepts = factMap.keySet();
      final Collection<ConceptAggregate> patientNeoplasms
            = allConcepts.stream()
                         .filter( isNeoplasm )
                         .collect( Collectors.toList() );

      final Tumor tumor = new Tumor();
      tumor.setId( neoplasm.getUri() + "_" + System.currentTimeMillis() );
      tumor.setClassUri( neoplasm.getUri() );
      tumor.setFactIds( Collections.singletonList( factMap.get( neoplasm ).getId() ) );
      final Map<String,List<String>> factRelations = FactCreator.getFactRelations( factMap, neoplasm );
      tumor.setRelatedFactIds( factRelations );


      final List<NeoplasmAttribute> attributes = new ArrayList<>();

      final String topoCode = addTopography( neoplasm, tumor, attributes, allConcepts );
      final String lateralityCode = addLaterality( neoplasm, tumor, attributes, allConcepts, patientNeoplasms, topoCode );
      addTopoMinor( neoplasm, tumor, attributes, allConcepts, patientNeoplasms, topoCode, lateralityCode );
      addMorphology( neoplasm, tumor, attributes, allConcepts, patientNeoplasms, topoCode );
      addBehavior( neoplasm, tumor, attributes, allConcepts, patientNeoplasms );
      addGrade( neoplasm, tumor, attributes, allConcepts, patientNeoplasms );
      addStage( neoplasm, tumor, attributes, allConcepts, patientNeoplasms );
      addTnmT( neoplasm, tumor, attributes, allConcepts, patientNeoplasms );
      addTnmN( neoplasm, tumor, attributes, allConcepts, patientNeoplasms );
      addTnmM( neoplasm, tumor, attributes, allConcepts, patientNeoplasms );
      addBiomarkers( neoplasm, tumor, attributes, allConcepts, patientNeoplasms );

      tumor.setAttributes( attributes );
      return tumor;
   }

   static private String addTopography( final ConceptAggregate neoplasm,
                                      final Tumor tumor,
                                      final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts ) {
      final Topography topography = new Topography( neoplasm, allConcepts );
      final NeoplasmAttribute majorTopoAttr = topography.toNeoplasmAttribute();
      attributes.add( majorTopoAttr );

      // TODO as NeoplasmAttribute

      return majorTopoAttr.getValue() + "3";
   }

   static private void addTopoMinor( final ConceptAggregate neoplasm,
                                    final Tumor tumor,
                                    final List<NeoplasmAttribute> attributes,
                                    final Collection<ConceptAggregate> allConcepts,
                                    final Collection<ConceptAggregate> patientNeoplasms,
                                     final String topographyMajor,
                                     final String lateralityCode ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      dependencies.put( "laterality", lateralityCode );
      final DefaultAttribute<TopoMinorUriInfoVisitor, TopoMinorCodeInfoStore> topoMinor
            = new DefaultAttribute<>( "topography_minor",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      TopoMinorUriInfoVisitor::new,
                                      TopoMinorCodeInfoStore::new,
                                      dependencies );
      attributes.add( topoMinor.toNeoplasmAttribute() );
   }

   static private void addMorphology( final ConceptAggregate neoplasm,
                                        final Tumor tumor,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms,
                                        final String topographyCode ) {
      final DefaultAttribute<HistologyUriInfoVisitor, HistologyCodeInfoStore> histology
            = new Histology( neoplasm,
                             allConcepts,
                             patientNeoplasms );
      attributes.add( histology.toNeoplasmAttribute() );
   }


   static private void addBehavior( final ConceptAggregate neoplasm,
                                      final Tumor tumor,
                                      final List<NeoplasmAttribute> attributes,
                                      final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<BehaviorUriInfoVisitor, BehaviorCodeInfoStore> behavior
            = new DefaultAttribute<>( "behavior",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      BehaviorUriInfoVisitor::new,
                                      BehaviorCodeInfoStore::new,
                                      Collections.emptyMap() );
      attributes.add( behavior.toNeoplasmAttribute() );
   }


   static private String addLaterality( final ConceptAggregate neoplasm,
                                 final Tumor tumor,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms,
                                        final String topographyMajor ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      final DefaultAttribute<LateralUriInfoVisitor, LateralityCodeInfoStore> laterality
            = new DefaultAttribute<>( "laterality",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      LateralUriInfoVisitor::new,
                                      LateralityCodeInfoStore::new,
                                      dependencies );
      attributes.add( laterality.toNeoplasmAttribute() );
      return laterality.getBestCode();
   }


   static private void addGrade( final ConceptAggregate neoplasm,
                                        final Tumor tumor,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<GradeUriInfoVisitor, GradeCodeInfoStore> grade
            = new DefaultAttribute<>( "grade",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      GradeUriInfoVisitor::new,
                                      GradeCodeInfoStore::new,
                                      Collections.emptyMap() );
      attributes.add( grade.toNeoplasmAttribute() );
   }

   static private void addStage( final ConceptAggregate neoplasm,
                                 final Tumor tumor,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<StageUriInfoVisitor, StageCodeInfoStore> stage
            = new DefaultAttribute<>( "stage",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      StageUriInfoVisitor::new,
                                      StageCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( stage.getBestUri().isEmpty() || stage.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( stage.toNeoplasmAttribute() );
   }

   static private void addTnmT( final ConceptAggregate neoplasm,
                                 final Tumor tumor,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<T_UriInfoVisitor, TnmCodeInfoStore> t
            = new DefaultAttribute<>( "t",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      T_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( t.getBestUri().isEmpty() || t.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( t.toNeoplasmAttribute() );
   }

   static private void addTnmN( final ConceptAggregate neoplasm,
                                final Tumor tumor,
                                final List<NeoplasmAttribute> attributes,
                                final Collection<ConceptAggregate> allConcepts,
                                final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<N_UriInfoVisitor, TnmCodeInfoStore> n
            = new DefaultAttribute<>( "n",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      N_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( n.getBestUri().isEmpty() || n.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( n.toNeoplasmAttribute() );
   }

   static private void addTnmM( final ConceptAggregate neoplasm,
                                final Tumor tumor,
                                final List<NeoplasmAttribute> attributes,
                                final Collection<ConceptAggregate> allConcepts,
                                final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<M_UriInfoVisitor, TnmCodeInfoStore> m
            = new DefaultAttribute<>( "m",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      M_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( m.getBestUri().isEmpty() || m.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( m.toNeoplasmAttribute() );
   }


//   static private String getT( final ConceptAggregate summary ) {
//      final Collection<String> ts = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_T, HAS_PATHOLOGIC_T ) );
//      return String.join( ";", getTnmValue( ts, 't', true ) );
//   }
//
//   static private String getN( final ConceptAggregate summary ) {
//      final Collection<String> ns = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_N, HAS_PATHOLOGIC_N ) );
//      return String.join( ";", getTnmValue( ns, 'n', true ) );
//   }
//
//   static private String getM( final ConceptAggregate summary ) {
//      final Collection<String> ms = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_M, HAS_PATHOLOGIC_M ) );
//      return String.join( ";", getTnmValue( ms, 'm', false ) );
//   }
//
//   static private String getTnmValue( final Collection<String> tnms, final char type, final boolean allowX ) {
//      final Collection<String> values = new HashSet<>();
//      for ( String tnm : tnms ) {
//         final String lower = tnm.toLowerCase().replace( "_stage", "" );
//         final int typeIndex = lower.indexOf( type );
//         if ( typeIndex < 0 || typeIndex >= tnm.length() - 1 ) {
//            continue;
//         }
//         final String value = lower.substring( typeIndex + 1 );
//         values.add( value );
//      }
//      values.remove( "x_category" );
//      if ( !allowX ) {
//         values.remove( "x" );
//         values.remove( "X" );
//      }
//      return String.join( ";", values );
//   }


   static private final Collection<String> BIOMARKERS = Arrays.asList(
         "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
         "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" );

   static private void addBiomarkers( final ConceptAggregate neoplasm,
                                     final Tumor tumor,
                                     final List<NeoplasmAttribute> attributes,
                                     final Collection<ConceptAggregate> allConcepts,
                                     final Collection<ConceptAggregate> patientNeoplasms ) {
       BIOMARKERS.forEach( b -> addBiomarker( b, neoplasm, tumor, attributes, allConcepts, patientNeoplasms ) );
   }

   static private void addBiomarker( final String biomarkerName,
                                       final ConceptAggregate neoplasm,
                                        final Tumor tumor,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                        final Collection<ConceptAggregate> patientNeoplasms ) {
      final Biomarker biomarker  = new Biomarker( biomarkerName,
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms );
      if ( biomarker.getBestUri().isEmpty() || biomarker.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( biomarker.toNeoplasmAttribute() );
   }




}
