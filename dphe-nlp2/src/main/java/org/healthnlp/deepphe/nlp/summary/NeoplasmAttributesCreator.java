package org.healthnlp.deepphe.nlp.summary;

import org.apache.ctakes.core.util.IdCounter;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.xn.AttributeValue;
import org.healthnlp.deepphe.neo4j.node.xn.AttributeXn;
import org.healthnlp.deepphe.nlp.ae.patient.PatientSummarizer;
import org.healthnlp.deepphe.nlp.attribute.biomarker.BiomarkerInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.biomarker.BiomarkerNormalizer;
import org.healthnlp.deepphe.nlp.attribute.grade.GradeNormalizer;
import org.healthnlp.deepphe.nlp.attribute.stage.StageNormalizer;
import org.healthnlp.deepphe.nlp.attribute.histology.HistologyNormalizer;
import org.healthnlp.deepphe.nlp.attribute.tnm.TnmNormalizer;
import org.healthnlp.deepphe.nlp.attribute.topo_major.TopoMajorNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.*;
import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.newInfoStore.DefaultInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.xn.receptor.ReceptorInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.TopoMinorTypeSelector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {10/23/2023}
 */
final public class NeoplasmAttributesCreator {

   private NeoplasmAttributesCreator() {}

   static private final IdCounter ID_COUNTER = new IdCounter();

   static public void resetCounter() {
      ID_COUNTER.reset();
   }

   static public List<AttributeXn> createTumorAttributes( final UriConcept tumor,
                                                          final String patientId, final String patientTime,
                                                          final PatientSummarizer.SummaryPrefs prefs,
                                                          final long mentionCount ) {
      final List<AttributeXn> attributes = new ArrayList<>();
      final Map<String,List<XnAttributeValue>> dependencies = new HashMap<>();
      attributes.add( getAttribute( "Location", tumor, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_SITE, RelationConstants.HAS_ASSOCIATED_SITE ) );
      attributes.add( getAttribute( "Topography, major", tumor, patientId, patientTime,
            DefaultInfoCollector::new, TopoMajorNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_SITE, RelationConstants.HAS_ASSOCIATED_SITE ) );
      attributes.add( getAttribute( "Laterality", tumor, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_LATERALITY ) );
      // laterality_code
      // Trim attributes here to prevent discovery of topo minor associated with unwanted topo major
      final List<XnAttributeValue> topoMajors = dependencies.getOrDefault( "Topography, major", Collections.emptyList() );
      dependencies.put( "Topography, major", prefs.trimMajorValues( topoMajors ) );
      final List<XnAttributeValue> lats = dependencies.getOrDefault( "Laterality", Collections.emptyList() );
      dependencies.put( "Laterality", prefs.trimMajorValues( lats ) );
//      final Supplier<AttributeInfoCollector> infoCollector =
//            TopoMinorTypeSelector.getAttributeInfoCollector( dependencies );
//      final Supplier<XnAttributeNormalizer> infoNormalizer =
//            TopoMinorTypeSelector.getAttributeNormalizer( dependencies );
//      attributes.add( getAttribute( "Topography, minor", tumor, patientId, patientTime,
//            infoCollector, infoNormalizer, dependencies ) );
      attributes.add( getTopoMinor( "Topography, minor", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getAttribute( "Clockface", tumor, patientId, patientTime,
//            DefaultInfoCollector::new, ClockfaceNormalizer::new, dependencies,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_CLOCKFACE ) );
      attributes.add( getAttribute( "Quadrant", tumor, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_QUADRANT ) );
      // diagnosis
      attributes.add( getAttribute( "Grade", tumor, patientId, patientTime,
            DefaultInfoCollector::new, GradeNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_GRADE_XN, RelationConstants.HAS_GLEASON_GRADE ) );
      attributes.add( getAttribute( "Tissue", tumor, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount, RelationConstants.HAS_TISSUE ) );
      attributes.add( getAttribute( "Behavior", tumor, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount, RelationConstants.HAS_BEHAVIOR ) );
      attributes.add( getAttribute( "Receptor Status", tumor, patientId, patientTime,
            ReceptorInfoCollector::new, DefaultXnAttributeNormalizer::new, dependencies, mentionCount, RelationConstants.HAS_FINDING ) );
      attributes.add( getAttribute( "Test Results", tumor, patientId, patientTime,
            DefaultInfoCollector::new, DefaultXnAttributeNormalizer::new, dependencies, mentionCount, RelationConstants.HAS_TEST_RESULT ) );
      // tumor type
      // tumor size
      // tumor size procedure
      // calcifications
      // todo: just build a list from all of the biomarkers in BiomarkerFinder
      attributes.add( getBiomarker( "EstrogenReceptorStatus", "Estrogen Receptor Status", tumor, patientId, patientTime, dependencies, mentionCount ) );
      // ER amount , ER procedure
      attributes.add( getBiomarker( "ProgesteroneReceptorStatus", "Progesterone Receptor Status", tumor,patientId, patientTime,  dependencies, mentionCount ) );
      // PR amount , PR procedure
      attributes.add( getBiomarker( "HER2_sl_NeuStatus", "HER2/Neu Status", tumor, patientId, patientTime, dependencies, mentionCount ) );
      // HER2 amount , HER2 procedure
      attributes.add( getBiomarker( "AntigenKI_sub_67", "Antigen KI-67", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "BreastCancerType1SusceptibilityProtein", "Breast Cancer Type 1 Susceptibility Protein", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "BreastCancerType2SusceptibilityProtein", "Breast Cancer Type 2 Susceptibility Protein", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "ALKTyrosineKinaseReceptor", "ALK Tyrosine Kinase Receptor", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "EpidermalGrowthFactorReceptor", "Epidermal Growth Factor Receptor", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "Serine_sl_Threonine_sub_ProteinKinaseB_sub_Raf", "Serine/Threonine-Protein Kinase B-Raf", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "Proto_sub_OncogeneTyrosine_sub_ProteinKinaseROS", "Proto-Oncogene Tyrosine-Protein Kinase ROS", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "ProgrammedCellDeathProtein1", "Programmed Cell Death Protein 1", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "MicrosatelliteStable", "Microsatellite Stable", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "GTPaseKRas", "GTPase KRas", tumor, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getBiomarker( "Prostate_sub_SpecificAntigen", "Prostate-Specific Antigen", tumor, patientId, patientTime, dependencies, mentionCount ) );
//      attributes.add( getBiomarker( "Prostate_sub_SpecificAntigenEl", tumor, patientId, patientTime, dependencies ) );
      // treatment
      trimAttributes( attributes );
      return attributes;
   }

   static public List<AttributeXn> createCancerAttributes( final UriConcept cancer, final String patientId,
                                                           final String patientTime,
                                                           final PatientSummarizer.SummaryPrefs prefs,
                                                           final long mentionCount ) {
      List<AttributeXn> attributes = new ArrayList<>();
      final Map<String, List<XnAttributeValue>> dependencies = new HashMap<>();
      attributes.add( getAttribute( "Location", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_SITE, RelationConstants.HAS_ASSOCIATED_SITE ) );
      attributes.add( getAttribute( "Topography, major", cancer, patientId, patientTime,
            DefaultInfoCollector::new, TopoMajorNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_SITE, RelationConstants.HAS_ASSOCIATED_SITE ) );
      attributes.add( getAttribute( "Laterality", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_LATERALITY ) );
      // laterality_code
      // Trim attributes here to prevent discovery of topo minor associated with unwanted topo major
      final List<XnAttributeValue> topoMajors = dependencies.getOrDefault( "Topography, major", Collections.emptyList() );
      dependencies.put( "Topography, major", prefs.trimMajorValues( topoMajors ) );
      final List<XnAttributeValue> lats = dependencies.getOrDefault( "Laterality", Collections.emptyList() );
      dependencies.put( "Laterality", prefs.trimMajorValues( lats ) );
//      final Supplier<AttributeInfoCollector> infoCollector =
//            TopoMinorTypeSelector.getAttributeInfoCollector( dependencies );
//      final Supplier<XnAttributeNormalizer> infoNormalizer =
//            TopoMinorTypeSelector.getAttributeNormalizer( dependencies );
//      attributes.add( getAttribute( "Topography, minor", cancer, patientId, patientTime,
//            infoCollector, infoNormalizer, dependencies ) );
      attributes.add( getTopoMinor( "Topography, minor", cancer, patientId, patientTime, dependencies, mentionCount ) );
      attributes.add( getAttribute( "Lymph Involvement", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_LYMPH_NODE ) );
      attributes.add( getAttribute( "Metastatic Site", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_METASTATIC_SITE ) );
      attributes.add( getAttribute( "Histology", cancer, patientId, patientTime,
            DefaultInfoCollector::new, HistologyNormalizer::new, dependencies, mentionCount ) );
      attributes.add( getAttribute( "Grade", cancer, patientId, patientTime,
//            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            DefaultInfoCollector::new, GradeNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_GRADE_XN, RelationConstants.HAS_GLEASON_GRADE ) );
      attributes.add( getAttribute( "Stage", cancer, patientId, patientTime,
//            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            DefaultInfoCollector::new, StageNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_STAGE_XN ) );
      // extent   - covered by behavior and T (from TNM)
      attributes.add( getAttribute( "T Stage", cancer, patientId, patientTime,
            DefaultInfoCollector::new, TnmNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_CLINICAL_T_XN, RelationConstants.HAS_PATHOLOGIC_T_XN ) );
      attributes.add( getAttribute( "N Stage", cancer, patientId, patientTime,
            DefaultInfoCollector::new, TnmNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_CLINICAL_N_XN, RelationConstants.HAS_PATHOLOGIC_N_XN ) );
      attributes.add( getAttribute( "M Stage", cancer, patientId, patientTime,
            DefaultInfoCollector::new, TnmNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_CLINICAL_M_XN, RelationConstants.HAS_PATHOLOGIC_M_XN ) );
      attributes.add( getAttribute( "Course", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_COURSE ) );
      // Not exactly the same as Tumor, but still mostly a repeat.
//      attributes.add( getAttribute( "Mass", cancer, patientId, patientTime,
//            DefaultInfoCollector::new, DefaultXnAttributeNormalizer::new, dependencies,
//            RelationConstants.HAS_MASS ) );
      attributes.add( getAttribute( "Test Results", cancer, patientId, patientTime,
            DefaultInfoCollector::new, DefaultXnAttributeNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_TEST_RESULT ) );
//      attributes.add( createHistoricAttribute( cancer, patientTime ) );
      attributes.add( getAttribute( "Treatments", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_TREATMENT ) );
      attributes.add( getAttribute( "Procedures", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_PROCEDURE ) );
      attributes.add( getAttribute( "Genes", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            RelationConstants.HAS_GENE ) );
      attributes.add( getAttribute( "Comorbidities", cancer, patientId, patientTime,
            DefaultInfoCollector::new, PrefTextNormalizer::new, dependencies, mentionCount,
            HAS_COMORBIDITY ) );
      trimAttributes( attributes );
      return attributes;
   }

//   static private <C extends AttributeInfoCollector,
//         N extends AttributeNormalizer> NeoplasmAttributeXn getAttribute( final String name,
//                                                                        final UriConcept neoplasm,
//                                                                        final String patientTime,
//                                                                        final Supplier<C> attributeInfoCollector,
//                                                                        final Supplier<N> attributeNormalizer,
//                                                                        final Map<String,String> dependencies ) {
//      return getAttribute( name, neoplasm, patientTime, attributeInfoCollector, attributeNormalizer, dependencies, new String[0] );
//   }

   static private <C extends AttributeInfoCollector,
         N extends XnAttributeNormalizer> AttributeXn getAttribute( final String name,
                                                                  final UriConcept neoplasm,
                                                                  final String patientId,
                                                                  final String patientTime,
                                                                  final Supplier<C> attributeInfoCollector,
                                                                  final Supplier<N> attributeNormalizer,
                                                                  final Map<String,List<XnAttributeValue>> dependencies,
                                                                  final long mentionCount,
                                                                  final String... relationTypes ) {
      final DefaultXnAttribute<C, N> attribute
            = new DefaultXnAttribute<>( name, neoplasm, patientId, patientTime,
            attributeInfoCollector, attributeNormalizer, dependencies, mentionCount, relationTypes );
      dependencies.put( name, attribute.getXnValues() );
      return attribute.toAttributeXn();
   }

   static private <C extends AttributeInfoCollector,
         N extends XnAttributeNormalizer> AttributeXn getTumorAttribute( final String name,
                                                                    final UriConcept tumor,
                                                                    final UriConcept cancer,
                                                                    final String patientId,
                                                                    final String patientTime,
                                                                    final Supplier<C> attributeInfoCollector,
                                                                    final Supplier<N> attributeNormalizer,
                                                                    final Map<String,List<XnAttributeValue>> dependencies,
                                                                    final long mentionCount,
                                                                    final String... relationTypes ) {
      DefaultXnAttribute<C, N> attribute
            = new DefaultXnAttribute<>( name, tumor, patientId, patientTime,
            attributeInfoCollector, attributeNormalizer, dependencies, mentionCount, relationTypes );
      if ( attribute.getXnValues().isEmpty() ) {
         attribute = new DefaultXnAttribute<>( name, cancer, patientId, patientTime,
               attributeInfoCollector, attributeNormalizer, dependencies, mentionCount, relationTypes );
      }
      dependencies.put( name, attribute.getXnValues() );
      return attribute.toAttributeXn();
   }

   static private AttributeXn getTopoMinor( final String name,
                                            final UriConcept neoplasm,
                                            final String patientId,
                                            final String patientTime,
                                            final Map<String,List<XnAttributeValue>> dependencies,
                                            final long mentionCount ) {
      final MultiCollectorNormalizer multiMinor = TopoMinorTypeSelector.getMultiCollectorNormalizer( dependencies );
      final DefaultXnAttribute<MultiCollectorNormalizer, MultiCollectorNormalizer> attribute
            = new DefaultXnAttribute<>( name, neoplasm, patientId, patientTime,
            multiMinor, multiMinor, dependencies, mentionCount, RelationConstants.HAS_SITE, RelationConstants.HAS_ASSOCIATED_SITE );
      dependencies.put( name, attribute.getXnValues() );
      return attribute.toAttributeXn();
   }

   static private AttributeXn getBiomarker( final String uri,
                                            final String name,
                                            final UriConcept neoplasm,
                                            final String patientId,
                                            final String patientTime,
                                            final Map<String,List<XnAttributeValue>> dependencies,
                                            final long mentionCount ) {
      final BiomarkerInfoCollector infoCollector = new BiomarkerInfoCollector();
      infoCollector.init( neoplasm, HAS_FINDING, HAS_TEST_RESULT, HAS_GENE );
      infoCollector.setWantedUris( uri );
//      final String name = UriInfoCache.getInstance().getPrefText( uri );
//      if ( name.isEmpty() ) {
//         LogFileWriter.add( "NeoplasmAttributeCreator empty prefText for " + uri );
//      }
      final DefaultXnAttribute<BiomarkerInfoCollector, BiomarkerNormalizer> attribute
            = new DefaultXnAttribute<>( name, neoplasm, patientId, patientTime,
            infoCollector, new BiomarkerNormalizer(), dependencies, mentionCount, RelationConstants.HAS_FINDING, HAS_TEST_RESULT,
            HAS_GENE );
      dependencies.put( name, attribute.getXnValues() );
      return attribute.toAttributeXn();
   }

   static private void trimAttributes( final List<AttributeXn> attributes ) {
      final Map<String,AttributeXn> namedAttributes
            = attributes.stream()
                        .filter( a -> !a.getName().isEmpty() )
                        .collect( Collectors.toMap( AttributeXn::getName, Function.identity() ) );
      trimAttribute( namedAttributes.get( "Estrogen Receptor Status" ), namedAttributes.get( "Test Results" ) );
      trimAttribute( namedAttributes.get( "Progesterone Receptor Status" ), namedAttributes.get( "Test Results" ) );
      trimAttribute( namedAttributes.get( "HER2/Neu Status" ), namedAttributes.get( "Test Results" ) );
   }

   static private void trimAttribute( final AttributeXn toKeep, final AttributeXn toRemove ) {
      if ( toKeep == null || toRemove == null ) {
         return;
      }
      final Collection<String> keepUris = toKeep.getValues()
                                                 .stream()
                                                 .map( AttributeValue::getClassUri )
                                                 .collect( Collectors.toSet() );
      final List<AttributeValue> notRemove = toRemove.getValues()
                                           .stream()
                                           .filter( v -> !keepUris.contains( v.getClassUri() ) )
                                           .collect( Collectors.toList() );
      toRemove.setValues( notRemove );
   }


//   static private AttributeXn createHistoricAttribute( final UriConcept cancer, final String patientTime ) {
//      final String historic = cancer.inPatientHistory() ? "True" : "False";
//      return AttributeCreator.createConceptAttribute( cancer, patientId, patientTime, "Historic", historic, 0.75 );
//   }


}
