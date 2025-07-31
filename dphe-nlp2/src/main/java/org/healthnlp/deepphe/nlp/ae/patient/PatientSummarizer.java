package org.healthnlp.deepphe.nlp.ae.patient;

import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.neo4j.node.xn.*;
import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.patient.PatientCasStore;
import org.healthnlp.deepphe.nlp.patient.PatientSummaryXnStore;
import org.healthnlp.deepphe.nlp.summary.PatientCasSummarizer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {9/20/2023}
 */
@PipeBitInfo(
      name = "PatientSummarizer",
      description = "Summarize a patient cas based upon the document cases.",
      role = PipeBitInfo.Role.SPECIAL
)
public class PatientSummarizer extends JCasAnnotator_ImplBase {
   static private final Logger LOGGER = Logger.getLogger( "PatientSummarizer" );


   static public final String MAX_CANCERS_PARAM = "MaxCancers";
   static public final String MAX_CANCERS_DESC = "Maximum number of cancers.";
   @ConfigurationParameter(
           name = MAX_CANCERS_PARAM,
           description = MAX_CANCERS_DESC,
           mandatory = false,
           defaultValue = "3"
   )
   private int _maxCancers;

   static public final String MIN_CANCERS_PARAM = "MinCancers";
   static public final String MIN_CANCERS_DESC = "Minimum number of cancers.";
   @ConfigurationParameter(
           name = MIN_CANCERS_PARAM,
           description = MIN_CANCERS_DESC,
           mandatory = false,
           defaultValue = "1"

   )
   private int _minCancers;

   static public final String MIN_CANCER_CONFIDENCE_PARAM = "MinCancerConfidence";
   static public final String MIN_CANCER_CONFIDENCE_DESC = "Minimum confidence required to recognize a Cancer.";
   @ConfigurationParameter(
         name = MIN_CANCER_CONFIDENCE_PARAM,
         description = MIN_CANCER_CONFIDENCE_DESC,
         mandatory = false,
         defaultValue = "0.75"
   )
   private String _minCancerConf;

   static public final String MIN_HAS_TUMOR_CONF_PARAM = "MinHasTumorConf";
   static public final String MIN_HAS_TUMOR_CONF_DESC = "Minimum confidence required to relate a tumor to a cancer.";
   @ConfigurationParameter(
         name = MIN_HAS_TUMOR_CONF_PARAM,
         description = MIN_HAS_TUMOR_CONF_DESC,
         mandatory = false,
         defaultValue = "0.5"
   )
   private String _minHasTumorConf;


   static public final String MAX_TUMORS_PARAM = "MaxTumors";
   static public final String MAX_TUMORS_DESC = "Maximum number of tumors.";
   @ConfigurationParameter(
           name = MAX_TUMORS_PARAM,
           description = MAX_TUMORS_DESC,
           mandatory = false,
           defaultValue = "3"
   )
   private int _maxTumors;

   static public final String MIN_TUMORS_PARAM = "MinTumors";
   static public final String MIN_TUMORS_DESC = "Minimum number of tumors.";
   @ConfigurationParameter(
           name = MIN_TUMORS_PARAM,
           description = MIN_TUMORS_DESC,
           mandatory = false,
           defaultValue = "1"
   )
   private int _minTumors;

   static public final String MIN_TUMOR_CONFIDENCE_PARAM = "MinTumorConfidence";
   static public final String MIN_TUMOR_CONFIDENCE_DESC = "Minimum confidence required to recognize a Tumor.";
   @ConfigurationParameter(
         name = MIN_TUMOR_CONFIDENCE_PARAM,
         description = MIN_TUMOR_CONFIDENCE_DESC,
         mandatory = false,
         defaultValue = "0.75"
   )
   private String _minTumorConf;

   static public final String MAX_ATTRIBUTES_PARAM = "MaxAttributes";
   static public final String MAX_ATTRIBUTES_DESC = "Maximum number of cancer attribute values.";
   @ConfigurationParameter(
           name = MAX_ATTRIBUTES_PARAM,
           description = MAX_ATTRIBUTES_DESC,
           mandatory = false,
           defaultValue = "5"
   )
   private int _maxAttributes;

   static public final String MIN_ATTRIBUTES_PARAM = "MinAttributes";
   static public final String MIN_ATTRIBUTES_DESC = "Minimum number of cancer attribute values.";
   @ConfigurationParameter(
           name = MIN_ATTRIBUTES_PARAM,
           description = MIN_ATTRIBUTES_DESC,
           mandatory = false,
           defaultValue = "1"
   )
   private int _minAttributes;

   static public final String MIN_ATTRIBUTE_CONFIDENCE_PARAM = "MinAttributeConfidence";
   static public final String MIN_ATTRIBUTE_CONFIDENCE_DESC = "Minimum confidence for attribute values.";
   @ConfigurationParameter(
         name = MIN_ATTRIBUTE_CONFIDENCE_PARAM,
         description = MIN_ATTRIBUTE_CONFIDENCE_DESC,
         mandatory = false,
         defaultValue = "0.5"
   )
   private String _minAttributeConf;

   static public final String MAX_MAJOR_ATTRIBUTES_PARAM = "MaxMajorAttributes";
   static public final String MAX_MAJOR_ATTRIBUTES_DESC = "Maximum number of major cancer attribute values.";
   @ConfigurationParameter(
         name = MAX_MAJOR_ATTRIBUTES_PARAM,
         description = MAX_MAJOR_ATTRIBUTES_DESC,
         mandatory = false,
         defaultValue = "3"
   )
   private int _maxMajorAttributes;

   static public final String MIN_MAJOR_ATTRIBUTES_PARAM = "MinMajorAttributes";
   static public final String MIN_MAJOR_ATTRIBUTES_DESC = "Minimum number of major cancer attribute values.";
   @ConfigurationParameter(
         name = MIN_MAJOR_ATTRIBUTES_PARAM,
         description = MIN_MAJOR_ATTRIBUTES_DESC,
         mandatory = false,
         defaultValue = "1"
   )
   private int _minMajorAttributes;

   static public final String MIN_MAJOR_ATTRIBUTE_CONFIDENCE_PARAM = "MinMajorAttributeConfidence";
   static public final String MIN_MAJOR_ATTRIBUTE_CONFIDENCE_DESC = "Minimum confidence for major attribute values.";
   @ConfigurationParameter(
         name = MIN_MAJOR_ATTRIBUTE_CONFIDENCE_PARAM,
         description = MIN_MAJOR_ATTRIBUTE_CONFIDENCE_DESC,
         mandatory = false,
         defaultValue = "0.8"
   )
   private String _minMajorAttributeConf;


   static public final String MAX_OPT_ATTRIBUTES_PARAM = "MaxOptAttributes";
   static public final String MAX_OPT_ATTRIBUTES_DESC = "Maximum number of optional attribute values.";
   @ConfigurationParameter(
         name = MAX_OPT_ATTRIBUTES_PARAM,
         description = MAX_OPT_ATTRIBUTES_DESC,
         mandatory = false,
         defaultValue = "100"
   )
   private int _maxOptAttributes;

   static public final String MIN_OPT_ATTRIBUTES_PARAM = "MinOptAttributes";
   static public final String MIN_OPT_ATTRIBUTES_DESC = "Minimum number of optional attribute values.";
   @ConfigurationParameter(
         name = MIN_OPT_ATTRIBUTES_PARAM,
         description = MIN_OPT_ATTRIBUTES_DESC,
         mandatory = false,
         defaultValue = "1"
   )
   private int _minOptAttributes;

   static public final String MIN_OPT_ATTRIBUTE_CONFIDENCE_PARAM = "MinOptAttributeConfidence";
   static public final String MIN_OPT_ATTRIBUTE_CONFIDENCE_DESC = "Minimum confidence for optional attribute values.";
   @ConfigurationParameter(
         name = MIN_OPT_ATTRIBUTE_CONFIDENCE_PARAM,
         description = MIN_OPT_ATTRIBUTE_CONFIDENCE_DESC,
         mandatory = false,
         defaultValue = "0.5"
   )
   private String _minOptAttributeConf;



   static public final String MAX_CONCEPTS_PARAM = "MaxConcepts";
   static public final String MAX_CONCEPTS_DESC = "Maximum number of concepts per type.";
   @ConfigurationParameter(
         name = MAX_CONCEPTS_PARAM,
         description = MAX_CONCEPTS_DESC,
         defaultValue = "100",
         mandatory = false
   )
   private int _maxConcepts;

   static public final String MIN_CONCEPTS_PARAM = "MinConcepts";
   static public final String MIN_CONCEPTS_DESC = "Minimum number of concepts per type.";
   @ConfigurationParameter(
         name = MIN_CONCEPTS_PARAM,
         description = MIN_CONCEPTS_DESC,
         defaultValue = "1",
         mandatory = false
   )
   private int _minConcepts;

   static public final String MIN_CONCEPT_CONFIDENCE_PARAM = "MinConceptConfidence";
   static public final String MIN_CONCEPT_CONFIDENCE_DESC = "Minimum confidence for asserted concepts.";
   @ConfigurationParameter(
         name = MIN_CONCEPT_CONFIDENCE_PARAM,
         description = MIN_CONCEPT_CONFIDENCE_DESC,
         defaultValue = "0.75",
         mandatory = false
   )
   private String _minConceptConf;

   static public final String MIN_NEG_CONCEPT_CONFIDENCE_PARAM = "MinNegConceptConfidence";
   static public final String MIN_NEG_CONCEPT_CONFIDENCE_DESC = "Minimum confidence for negated concepts.";
   @ConfigurationParameter(
         name = MIN_NEG_CONCEPT_CONFIDENCE_PARAM,
         description = MIN_NEG_CONCEPT_CONFIDENCE_DESC,
         defaultValue = "0.9",
         mandatory = false
   )
   private String _minNegConceptConf;

   static public final String MAX_MENTIONS_PARAM = "MaxMentions";
   static public final String MAX_MENTIONS_DESC = "Maximum number of mentions.";
   @ConfigurationParameter(
         name = MAX_MENTIONS_PARAM,
         description = MAX_MENTIONS_DESC,
         defaultValue = "1000",
         mandatory = false
   )
   private int _maxMentions;

   static public final String MIN_MENTIONS_PARAM = "MinMentions";
   static public final String MIN_MENTIONS_DESC = "Minimum number of mentions.";
   @ConfigurationParameter(
         name = MIN_MENTIONS_PARAM,
         description = MIN_MENTIONS_DESC,
         defaultValue = "0",
         mandatory = false
   )
   private int _minMentions;

   static public final String MIN_MENTION_CONF_PARAM = "MinMentionConfidence";
   static public final String MIN_MENTION_CONF_DESC = "Minimum mention confidence.";
   @ConfigurationParameter(
         name = MIN_MENTION_CONF_PARAM,
         description = MIN_MENTION_CONF_DESC,
         defaultValue = "0.4",
         mandatory = false
   )
   private String _minMentionConf;

//   static public final String MAX_MENTION_RELS_PARAM = "MaxMentionRelations";
//   static public final String MAX_MENTION_RELS_DESC = "Maximum number of mention relations.";
//   @ConfigurationParameter(
//         name = MAX_MENTION_RELS_PARAM,
//         description = MAX_MENTION_RELS_DESC,
//         defaultValue = "2",
//         mandatory = false
//   )
//   private int _maxMentionRels;
//
//   static public final String MIN_MENTION_RELS_PARAM = "MinMentionRelations";
//   static public final String MIN_MENTION_RELS_DESC = "Minimum number of mention relations.";
//   @ConfigurationParameter(
//         name = MIN_MENTION_RELS_PARAM,
//         description = MIN_MENTION_RELS_DESC,
//         defaultValue = "0",
//         mandatory = false
//   )
//   private int _minMentionRels;

   static public final String MIN_MENTION_REL_CONF_PARAM = "MinMentionRelConfidence";
   static public final String MIN_MENTION_REL_CONF_DESC = "Minimum mention relation confidence.";
   @ConfigurationParameter(
         name = MIN_MENTION_REL_CONF_PARAM,
         description = MIN_MENTION_REL_CONF_DESC,
         defaultValue = "0.1",
         mandatory = false
   )
   private String _minMentionRelConf;

//   static public final String MAX_CONCEPT_RELS_PARAM = "MaxConceptRelations";
//   static public final String MAX_CONCEPT_RELS_DESC = "Maximum number of concept relations.";
//   @ConfigurationParameter(
//         name = MAX_CONCEPT_RELS_PARAM,
//         description = MAX_CONCEPT_RELS_DESC,
//         defaultValue = "2",
//         mandatory = false
//   )
//   private int _maxConceptRels;
//
//   static public final String MIN_CONCEPT_RELS_PARAM = "MinConceptRelations";
//   static public final String MIN_CONCEPT_RELS_DESC = "Minimum number of concept relations.";
//   @ConfigurationParameter(
//         name = MIN_CONCEPT_RELS_PARAM,
//         description = MIN_CONCEPT_RELS_DESC,
//         defaultValue = "0",
//         mandatory = false
//   )
//   private int _minConceptRels;
//
   static public final String MIN_CONCEPT_REL_CONF_PARAM = "MinConceptRelConfidence";
   static public final String MIN_CONCEPT_REL_CONF_DESC = "Minimum concept relation confidence.";
   @ConfigurationParameter(
         name = MIN_CONCEPT_REL_CONF_PARAM,
         description = MIN_CONCEPT_REL_CONF_DESC,
         defaultValue = "0.1",
         mandatory = false
   )
   private String _minConceptRelConf;


   static private final Collection<String> MAJOR_TRIM_ATTRIBUTES = new HashSet<>( Arrays.asList(
         "Location", "Topography, major",
         "Histology", "Grade", "Stage", "T Stage", "N Stage", "M Stage",
         "Tissue", "Behavior", "Course" ) );

   //   static private final int MAX_RELATION_COUNT = 100;
//   static private final Collection<String> USE_100_RELATIONS = new HashSet<>( Arrays.asList(
//         RelationConstants.HAS_PROCEDURE, RelationConstants.HAS_FINDING, RelationConstants.HAS_TEST_RESULT,
//         RelationConstants.HAS_TREATMENT, RelationConstants.HAS_METHOD ) );
   static private final Collection<String> TRIM_ATTRIBUTES = new HashSet<>( Arrays.asList(
      "Topography, minor", "Clockface",  "Quadrant", "Laterality" ) );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
       if (!PatientDocCounter.getInstance().isPatientFull(patientId)) {
           return;
       }
      LOGGER.info( "Summarizing Patient " + patientId + " ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         // The big deal ...
         final JCas patientCas = PatientCasStore.getInstance().get( patientId );
         final PatientSummaryXn patientSummary
                 = PatientCasSummarizer.summarizePatient( patientCas, new SummaryPrefs() );
         PatientSummaryXnStore.getInstance().add( patientId, patientSummary );
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }


   public final class SummaryPrefs {
      private final double _minCancerConf;
      private final double _minTumorConf;
      private final double _minMajorAttrConf;
      private final double _minAttrConf;
      private final double _minOptAttrConf;
      private final int _maxCancerCount;
      private final int _minCancerCount;
      private final int _maxTumorCount;
      private final int _minTumorCount;
      private final int _maxMajorAttrCount;
      private final int _minMajorAttrCount;
      private final int _maxAttrCount;
      private final int _minAttrCount;
      private final int _maxOptAttrCount;
      private final int _minOptAttrCount;
      private SummaryPrefs() {
         this._minCancerConf = 100*AeParamUtil.parseDouble( PatientSummarizer.this._minCancerConf );
         this._minTumorConf = 100*AeParamUtil.parseDouble( PatientSummarizer.this._minTumorConf );
         this._minMajorAttrConf = 100*AeParamUtil.parseDouble( PatientSummarizer.this._minMajorAttributeConf );
         this._minAttrConf = 100*AeParamUtil.parseDouble( PatientSummarizer.this._minAttributeConf );
         this._minOptAttrConf = 100*AeParamUtil.parseDouble( PatientSummarizer.this._minOptAttributeConf );
         this._maxCancerCount = _maxCancers;
         this._minCancerCount = _minCancers;
         this._maxTumorCount = _maxTumors;
         this._minTumorCount = _minTumors;
         this._maxMajorAttrCount = _maxMajorAttributes;
         this._minMajorAttrCount = _minMajorAttributes;
         this._maxAttrCount = _maxAttributes;
         this._minAttrCount = _minAttributes;
         this._maxOptAttrCount = _maxOptAttributes;
         this._minOptAttrCount = _minOptAttributes;
      }

      public Collection<UriConcept> trimCancers( final Collection<UriConcept> cancers ) {
         if ( cancers == null ) {
            return Collections.emptyList();
         }
//         return trimUriConceptsByGroup( cancers, _minCancerCount, _maxCancerCount, _minCancerConf );
         return trimUriConcepts( cancers, _minCancerCount, _maxCancerCount, _minCancerConf );
      }

      public Collection<UriConcept> trimTumors( final Collection<UriConcept> tumors ) {
         if ( tumors == null ) {
            return Collections.emptyList();
         }
//         return trimUriConceptsByGroup( tumors, _minTumorCount, _maxTumorCount, _minTumorConf );
         return trimUriConcepts( tumors, _minTumorCount, _maxTumorCount, _minTumorConf );
      }

//      public List<CancerSummaryXn> trimCancerSummaries( final Collection<CancerSummaryXn> cancers ) {
//         return trimByGroupedConfidence( cancers, _minCancerCount, _maxCancerCount, _minCancerConf );
//      }
//
//      public List<TumorSummaryXn> trimTumorSummaries( final Collection<TumorSummaryXn> tumors ) {
//         return trimByGroupedConfidence( tumors, _minTumorCount, _maxTumorCount, _minTumorConf );
//      }


      public List<AttributeXn> trimAttributes( final List<AttributeXn> attributes ) {
         if ( attributes == null ) {
            return Collections.emptyList();
         }
         for ( AttributeXn attribute : attributes ) {
            if ( MAJOR_TRIM_ATTRIBUTES.contains( attribute.getName() ) ) {
               attribute.setValues(
                     trimAttributeValues( attribute.getValues(), _minMajorAttrCount, _maxMajorAttrCount, _minMajorAttrConf ) );
            } else if ( TRIM_ATTRIBUTES.contains( attribute.getName() ) ) {
               attribute.setValues(
                     trimAttributeValues( attribute.getValues(), _minAttrCount, _maxAttrCount, _minAttrConf ) );
            } else {
               attribute.setValues(
                     trimAttributeValues( attribute.getValues(), _minOptAttrCount, _maxOptAttrCount, _minOptAttrConf ) );
            }
         }
         return attributes.stream()
                 .filter( a -> a.getValues().size() > 0 )
                 .collect( Collectors.toList() );
      }

      public List<XnAttributeValue> trimMajorValues( final List<XnAttributeValue> attributeValues ) {
         if ( attributeValues == null || _maxMajorAttrCount <= 0 ) {
            // Don't want any more nodes.
            return Collections.emptyList();
         }
         if ( attributeValues.size() <= _minMajorAttrCount ) {
            // Even if the confidences are tiny, we want at least some minimum count.
            return new ArrayList<>( attributeValues );
         }
         final List<XnAttributeValue> sorted = attributeValues.stream()
                                     .sorted( Comparator.comparingDouble( XnAttributeValue::getConfidence ).reversed() )
                                     .collect( Collectors.toList() );
         final List<XnAttributeValue> good = new ArrayList<>();
         final int max = Math.min( attributeValues.size(), _maxMajorAttrCount );
         for  ( int i=0; i<max; i++ ) {
            if ( good.size() < _minMajorAttrCount ) {
               good.add( sorted.get( i ) );
               continue;
            }
            if ( sorted.get( i ).getConfidence() >= _minMajorAttrConf ) {
               good.add( sorted.get( i ) );
            } else {
//               LOGGER.error( sorted.get( i ).getConfidence() );
               break;
            }
         }
         return good;
      }

      private List<AttributeValue> trimAttributeValues( final List<AttributeValue> attributeValues,
                                                        final int minCount,
                                                        final int maxCount,
                                                        final double minConf ) {
         if ( attributeValues == null ) {
            return Collections.emptyList();
         }
         final List<AttributeValue> good
               = trimConfidenceOwners( attributeValues, minCount, maxCount, minConf );
//               = trimByGroupedConfidence( attributeValues, _minAttrCount, _maxAttrCount, _minAttrConf );
         if ( good.size() < attributeValues.size() ) {
            LogFileWriter.add( "! PatientSummarizer.trimAttributeValues Removed Attributes\n   " + attributeValues
                  .stream()
                  .filter( a -> !good.contains( a ) )
                  .map( a -> a.getClassUri() + " " + a.getdConfidence() )
//                               + " " + a.getGroupedConfidence() )
                  .collect( Collectors.joining( "\n   " ))
                  + "\n------------ Removed Attributes --------------" );
         }
         return good;
      }

//      public List<UriConcept> trimAllUriConcepts( final Collection<UriConcept> allConcepts,
//                                                  final Collection<UriConcept> requiredConcepts ) {
//         final double minConf = AeParamUtil.parseDouble( _minConceptConf );
//         final int maxCount = Integer.MAX_VALUE;
//         final double minNegConf = AeParamUtil.parseDouble( _minNegConceptConf );
//         final List<UriConcept> good = new ArrayList<>();
//         final Map<DpheGroup,List<UriConcept>> groupConceptsMap
//               = allConcepts.stream()
//                         .filter( c -> !c.isNegated() )
//                         .collect( Collectors.groupingBy( UriConcept::getDpheGroup ) );
//         for ( Collection<UriConcept> groupConcepts : groupConceptsMap.values() ) {
//            good.addAll( trimUriConceptGroup( groupConcepts, requiredConcepts, _minConcepts, maxCount, minConf ) );
//         }
//         final Map<DpheGroup,List<UriConcept>> negatedGroupConceptsMap
//               = allConcepts.stream()
//                         .filter( UriConcept::isNegated )
//                         .collect( Collectors.groupingBy( UriConcept::getDpheGroup ) );
//         for ( Collection<UriConcept> groupConcepts : negatedGroupConceptsMap.values() ) {
//            good.addAll( trimUriConceptGroup( groupConcepts, requiredConcepts, _minConcepts, maxCount, minNegConf ) );
//         }
//         return good;
//      }

      public List<UriConcept> trimAllUriConcepts( final Collection<UriConcept> allConcepts,
                                                  final Collection<String> requiredConceptIDs ) {
         if ( allConcepts == null ) {
            return Collections.emptyList();
         }
         final double minConf = 100*AeParamUtil.parseDouble( _minConceptConf );
         final double minNegConf = 100*AeParamUtil.parseDouble( _minNegConceptConf );
         final List<UriConcept> good = new ArrayList<>();
         final Map<DpheGroup,List<UriConcept>> groupConceptsMap
               = allConcepts.stream()
                            .filter( c -> !c.isNegated() )
                            .collect( Collectors.groupingBy( UriConcept::getDpheGroup ) );
         for ( Collection<UriConcept> groupConcepts : groupConceptsMap.values() ) {
            good.addAll( trimUriConceptGroup( groupConcepts, requiredConceptIDs, _minConcepts, _maxConcepts, minConf ) );
         }
         final Map<DpheGroup,List<UriConcept>> negatedGroupConceptsMap
               = allConcepts.stream()
                            .filter( UriConcept::isNegated )
                            .collect( Collectors.groupingBy( UriConcept::getDpheGroup ) );
         for ( Collection<UriConcept> groupConcepts : negatedGroupConceptsMap.values() ) {
            good.addAll( trimUriConceptGroup( groupConcepts, requiredConceptIDs, _minConcepts, _maxConcepts, minNegConf ) );
         }
         return good;
      }


//      public List<Concept> trimConcepts( final Collection<Concept> concepts,
//                                         final Collection<Concept> requiredConcepts ) {
//         final double minConf = AeParamUtil.parseDouble( _minConceptConf );
//         final int minCount = _minConcepts;
//         final int maxCount = _maxConcepts;
//         final Map<DpheGroup,Collection<Concept>> groupConceptsMap = new HashMap<>();
//         final List<Concept> good = new ArrayList<>();
//         for ( Concept concept : concepts ) {
//            if ( requiredConcepts.contains( concept ) ) {
//               good.add( concept );
//               continue;
//            }
//            final DpheGroup group = DpheGroupAccessor.getInstance().getByName( concept.getDpheGroup() );
//            groupConceptsMap.computeIfAbsent( group, g -> new ArrayList<>() ).add( concept );
//         }
//         for ( Collection<Concept> groupConcepts : groupConceptsMap.values() ) {
//            good.addAll( trimConfidenceOwners( groupConcepts, minCount, maxCount, minConf ) );
//         }
//         return good;
//      }

      public List<UriConceptRelation> trimTumorRelations( final Collection<UriConceptRelation> hasTumors ) {
         final int maxCount = _maxTumorCount;
         if ( hasTumors == null || maxCount <= 0 ) {
            // Don't want any more nodes.
            return Collections.emptyList();
         }
         final int minCount = _minTumorCount;
         if ( hasTumors.size() <= minCount ) {
            // Even if the confidences are tiny, we want at least some minimum count.
            return new ArrayList<>( hasTumors );
         }
         final double minConf = 100*AeParamUtil.parseDouble( _minHasTumorConf );
         final List<UriConceptRelation> sorted = hasTumors.stream()
                                                 .sorted( Comparator.comparingDouble( UriConceptRelation::getConfidence ).reversed() )
                                                 .collect( Collectors.toList() );
         final List<UriConceptRelation> good = new ArrayList<>();
         final int max = Math.min( hasTumors.size(), maxCount );
         for  ( int i=0; i<max; i++ ) {
            if ( good.size() < minCount ) {
               good.add( sorted.get( i ) );
               continue;
            }
            if ( sorted.get( i ).getConfidence() >= minConf ) {
               good.add( sorted.get( i ) );
            } else {
               break;
            }
         }
         if ( good.size() < sorted.size() ) {
            LogFileWriter.add( "! PatientSummarizer.trimTumorRelations Removed TumorRelations\n   "
                  + sorted.stream().filter( c -> !good.contains( c ) ).map( UriConceptRelation::getConfidence )
                          .map( Object::toString ).collect( Collectors.joining(" ") ) + "\n   "
                  + sorted.stream().filter( c -> !good.contains( c ) )
                               .map( UriConceptRelation::getTarget )
                          .map( UriConcept::toLongText )
                          .collect( Collectors.joining( "\n   " ))
                  + "\n------- Removed TumorRelations -------------" );
         }
         return good;
      }

      public List<ConceptRelation> trimConceptRelations( final Collection<ConceptRelation> relations,
                                                         final Collection<String> requiredConceptIDs ) {
         if ( relations == null ) {
            // Don't want any more nodes.
            return Collections.emptyList();
         }
         final double minConf = 100*AeParamUtil.parseDouble( _minConceptRelConf );
         final Map<String,Collection<ConceptRelation>> sourceRelationsMap = new HashMap<>();
         for ( ConceptRelation relation : relations ) {
            if ( !requiredConceptIDs.contains( relation.getSourceId() )
                  || !requiredConceptIDs.contains( relation.getTargetId() ) ) {
               continue;
            }
            sourceRelationsMap.computeIfAbsent( relation.getSourceId() + "," + relation.getType(),
                  r -> new HashSet<>() ).add( relation );
         }
         final List<ConceptRelation> good = new ArrayList<>();
         for ( Map.Entry<String,Collection<ConceptRelation>> typeRelations : sourceRelationsMap.entrySet() ) {
            good.addAll( trimRelations( typeRelations.getValue(), minConf ) );
         }
         return good;
      }

//      public List<Mention> trimMentions( final Collection<Mention> mentions,
//                                         final Collection<Concept> requiredConcepts ) {
//         final double minConf = AeParamUtil.parseDouble( _minMentionConf );
//         final Collection<String> requiredIDs = requiredConcepts.stream()
//                                                                .map( Concept::getMentionIds )
//                                                                .flatMap( Collection::stream )
//                                                                .collect( Collectors.toSet() );
//         final Map<DpheGroup,Collection<Mention>> groupMentionsMap = new HashMap<>();
//         final List<Mention> good = new ArrayList<>();
//         for ( Mention mention : mentions ) {
//            final DpheGroup group = DpheGroupAccessor.getInstance().getByName( mention.getDpheGroup() );
//            groupMentionsMap.computeIfAbsent( group, g -> new ArrayList<>() ).add( mention );
//         }
//         for ( Collection<Mention> groupMentions : groupMentionsMap.values() ) {
//            good.addAll( trimInfoNodes( groupMentions, _minMentions, Integer.MAX_VALUE, minConf, requiredIDs ) );
//         }
//         return good;
//      }

//      public List<MentionRelation> trimMentionRelations( final Collection<MentionRelation> relations,
//                                                         final Collection<String> requiredMentionIDs ) {
//         final double minConf = AeParamUtil.parseDouble( _minMentionRelConf ) * 100;
//         final Map<String,Collection<MentionRelation>> sourceRelationsMap = new HashMap<>();
//         for ( MentionRelation relation : relations ) {
//            if ( !requiredMentionIDs.contains( relation.getSourceId() )
//                  || !requiredMentionIDs.contains( relation.getTargetId() ) ) {
//               continue;
//            }
//            sourceRelationsMap.computeIfAbsent( relation.getSourceId() + "," + relation.getType(),
//                  r -> new HashSet<>() ).add( relation );
//         }
//         final List<MentionRelation> good = new ArrayList<>();
//         for ( Map.Entry<String,Collection<MentionRelation>> typeRelations : sourceRelationsMap.entrySet() ) {
//            good.addAll( trimRelations( typeRelations.getValue(), minConf ) );
//         }
//         return good;
//      }
   }

//   static private Collection<UriConcept> trimUriConceptGroup( final Collection<UriConcept> concepts,
//                                                       final Collection<UriConcept> requiredConcepts,
//                                                       final int minCount,
//                                                       final int maxCount,
//                                                       final double minConf ) {
//      if ( concepts.size() <= minCount ) {
//         // Even if the confidences are tiny, we want at least some minimum count.
//         return new ArrayList<>( concepts );
//      }
//      final List<UriConcept> good = new ArrayList<>( concepts );
//      good.retainAll( requiredConcepts );
//      if ( good.size() >= maxCount ) {
//         // Even if the confidences of the remaining nodes are high, we have hit the maximum count.
//         return good;
//      }
//      final List<UriConcept> remainder = new ArrayList<>( concepts );
//      remainder.removeAll( good );
//      final int min = Math.max( 0, minCount - good.size() );
//      final int max = Math.max( 0, maxCount - good.size() );
//      good.addAll( trimUriConcepts( remainder, min, max, minConf ) );
//      if ( good.size() < concepts.size() ) {
//         LogFileWriter.add( "! Removed UriConcepts\n" + concepts
//               .stream()
//               .filter( a -> !good.contains( a ) )
//               .map( UriConcept::toLongText )
//               .collect( Collectors.joining( "\n" )) );
//      }
//      return good;
//   }

   static private Collection<UriConcept> trimUriConceptGroup( final Collection<UriConcept> concepts,
                                                              final Collection<String> requiredConceptIDs,
                                                              final int minCount,
                                                              final int maxCount,
                                                              final double minConf ) {
      if ( concepts == null || maxCount <= 0 ) {
         // Don't want any more nodes.
         return Collections.emptyList();
      }
      if ( concepts.size() <= minCount ) {
         // Even if the confidences are tiny, we want at least some minimum count.
         return new ArrayList<>( concepts );
      }
      final List<UriConcept> good = new ArrayList<>();
      concepts.stream().filter( c -> requiredConceptIDs.contains( c.getId() ) ).forEach( good::add );
      if ( good.size() >= maxCount ) {
         // Even if the confidences of the remaining nodes are high, we have hit the maximum count.
         return good;
      }
      final List<UriConcept> remainder = new ArrayList<>( concepts );
      remainder.removeAll( good );
      final int min = Math.max( 0, minCount - good.size() );
      final int max = Math.max( 0, maxCount - good.size() );
      good.addAll( trimUriConcepts( remainder, min, max, minConf ) );
      if ( good.size() < concepts.size() ) {
         LogFileWriter.add( "! PatientSummarizer.trimUriConceptGroup Removed UriConcepts\n   " + concepts
               .stream()
               .filter( a -> !good.contains( a ) )
               .map( UriConcept::toLongText )
               .collect( Collectors.joining( "\n   " ))
               + "\n-------------- Removed UriConcepts --------------");
      }
      return good;
   }


   static private Collection<UriConcept> trimUriConcepts( final Collection<UriConcept> concepts,
                                                   final int minCount,
                                                   final int maxCount,
                                                   final double minConf ) {
      if ( concepts == null || maxCount <= 0 ) {
         // Don't want any more nodes.
         return Collections.emptyList();
      }
      if ( concepts.size() <= minCount ) {
         // Even if the confidences are tiny, we want at least some minimum count.
         return new ArrayList<>( concepts );
      }
      final List<UriConcept> sorted = concepts.stream()
                                  .sorted( Comparator.comparingDouble( UriConcept::getConfidence ).reversed() )
                                  .collect( Collectors.toList() );
      final List<UriConcept> good = new ArrayList<>();
      final int max = Math.min( concepts.size(), maxCount );
      for  ( int i=0; i<max; i++ ) {
         if ( good.size() < minCount ) {
            good.add( sorted.get( i ) );
            continue;
         }
         if ( sorted.get( i ).getConfidence() >= minConf ) {
            good.add( sorted.get( i ) );
         } else {
            break;
         }
      }
      if ( good.size() < sorted.size() ) {
         LogFileWriter.add( sorted.stream().filter( c -> !good.contains( c ) )
                                  .map( c -> "! PatientSummarizer.trimUriConcepts Removing:\n" + c.toLongText() ).collect(
               Collectors.joining( "\n   " )) + "\n--------------- Removed -------------" );
      }
      return good;
   }


//   static private <N extends InfoNode> List<N> trimInfoNodes( final Collection<N> nodes,
//                                                                        final int minCount,
//                                                                        final int maxCount,
//                                                                        final double minConf,
//                                                                        final Collection<String> requiredIDs ) {
//      if ( nodes.size() <= minCount ) {
//         // Even if the confidences are tiny, we want at least some minimum count.
//         return new ArrayList<>( nodes );
//      }
//      final List<N> good = new ArrayList<>();
//      nodes.stream().filter( m -> requiredIDs.contains( m.getId() ) ).forEach( good::add );
//      if ( good.size() >= maxCount ) {
//         // Even if the confidences of the remaining nodes are high, we have hit the maximum count.
//         return good;
//      }
//      final List<N> remainder = new ArrayList<>( nodes );
//      remainder.removeAll( good );
//      final int min = Math.max( 0, minCount - good.size() );
//      final int max = Math.max( 0, maxCount - good.size() );
//      good.addAll( trimConfidenceOwners( remainder, min, max, minConf ) );
//      return good;
//   }

   static private <N extends ConfidenceOwner> List<N> trimConfidenceOwners( final Collection<N> nodes,
                                                                        final int minCount,
                                                                        final int maxCount,
                                                                        final double minConf ) {
      if ( nodes == null || maxCount <= 0 ) {
         // Don't want any more nodes.
         return Collections.emptyList();
      }
      if ( nodes.size() <= minCount ) {
         // Even if the confidences are tiny, we want at least some minimum count.
         return new ArrayList<>( nodes );
      }
      final List<N> sorted = nodes.stream()
                                  .sorted( Comparator.comparingDouble( N::getdConfidence ).reversed() )
                                  .collect( Collectors.toList() );
      final List<N> good = new ArrayList<>();
      final int max = Math.min( nodes.size(), maxCount );
      for  ( int i=0; i<max; i++ ) {
         if ( good.size() < minCount ) {
            good.add( sorted.get( i ) );
            continue;
         }
         if ( sorted.get( i ).getdConfidence() >= minConf ) {
            good.add( sorted.get( i ) );
         } else {
            break;
         }
      }
      return good;
   }

//   static private List<UriConcept> trimUriConceptsByGroup( final Collection<UriConcept> grouped,
//                                                                                    final int minCount,
//                                                                                    final int maxCount,
//                                                                                    final double minConf ) {
//      if ( grouped == null || maxCount <= 0 ) {
//         // Don't want any more nodes.
//         return Collections.emptyList();
//      }
//      if ( grouped.size() <= minCount ) {
//         // Even if the confidences are tiny, we want at least some minimum count.
//         return new ArrayList<>( grouped );
//      }
//      final List<UriConcept> sorted = grouped.stream()
//                                    .sorted( Comparator.comparingDouble( UriConcept::getGroupedConfidence ).reversed() )
//                                    .collect( Collectors.toList() );
//      final List<UriConcept> good = new ArrayList<>();
//      final int max = Math.min( grouped.size(), maxCount );
//      for  ( int i=0; i<max; i++ ) {
//         if ( good.size() < minCount ) {
//            good.add( sorted.get( i ) );
//            continue;
//         }
//         if ( sorted.get( i ).getGroupedConfidence() >= minConf ) {
//            good.add( sorted.get( i ) );
//         } else {
//            break;
//         }
//      }
//      return good;
//   }

//   static private <N extends GroupConfidenceOwner> List<N> trimByGroupedConfidence( final Collection<N> grouped,
//                                                                            final int minCount,
//                                                                            final int maxCount,
//                                                                            final double minConf ) {
//      if ( grouped == null || maxCount <= 0 ) {
//         // Don't want any more nodes.
//         return Collections.emptyList();
//      }
//      if ( grouped.size() <= minCount ) {
//         // Even if the confidences are tiny, we want at least some minimum count.
//         return new ArrayList<>( grouped );
//      }
//      final List<N> sorted = grouped.stream()
//                                  .sorted( Comparator.comparingDouble( GroupConfidenceOwner::getGroupedConfidence ).reversed() )
//                                  .collect( Collectors.toList() );
//      final List<N> good = new ArrayList<>();
//      final int max = Math.min( grouped.size(), maxCount );
//      for  ( int i=0; i<max; i++ ) {
//         if ( good.size() < minCount ) {
//            good.add( sorted.get( i ) );
//            continue;
//         }
//         if ( sorted.get( i ).getGroupedConfidence() >= minConf ) {
//            good.add( sorted.get( i ) );
//         } else {
//            break;
//         }
//      }
//      return good;
//   }

   static public List<Mention> trimMentions( final Collection<Mention> mentions,
                                             final Collection<String> requiredIDs ) {
      if ( mentions == null ) {
         return Collections.emptyList();
      }
      return mentions.stream().filter( m -> requiredIDs.contains( m.getId() ) ).collect( Collectors.toList() );
   }

   static public List<MentionRelation> trimMentionRelations( final Collection<MentionRelation> mentionRelations,
                                             final Collection<String> requiredIDs ) {
      if ( mentionRelations == null ) {
         return Collections.emptyList();
      }
      return mentionRelations.stream().filter( r -> requiredIDs.contains( r.getSourceId() ) )
                             .filter( r -> requiredIDs.contains( r.getTargetId() ) )
                             .collect( Collectors.toList() );
   }

   static private  <R extends ConfidenceOwner> Collection<R> trimRelations( final Collection<R> relations,
                                                                final double minConf ) {
      if ( relations == null ) {
         return Collections.emptyList();
      }
      return relations.stream().filter( r -> r.getdConfidence() >= minConf ).collect( Collectors.toList() );
   }



}
