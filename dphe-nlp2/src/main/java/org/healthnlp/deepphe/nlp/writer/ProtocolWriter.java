package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.ner.group.dphe.DpheGroupAccessor;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Episode;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.nlp.attribute.biomarker.BiomarkerNormalizer;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.DOCUMENT_ID_PREFIX;
import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_VALUE_SCHEME;
import static org.healthnlp.deepphe.nlp.writer.ProtocolWriter.CATEGORY_TITLE.*;
import static org.healthnlp.deepphe.nlp.writer.ProtocolWriter.YES_NO_TITLE.*;

/**
 * @author SPF , chip-nlp
 * @since {5/22/2024}
 */
@PipeBitInfo (
      name = "ProtocolWriter",
      description = "Writes brca, ovca, melanoma protocol files in a directory tree.",
      role = PipeBitInfo.Role.WRITER,
      usables = { DOCUMENT_ID_PREFIX }
)
public class ProtocolWriter extends AbstractJCasFileWriter {
   // If you do not need to utilize the entire cas, or need more than the doc cas, consider AbstractFileWriter<T>.
   static private final Logger LOGGER = Logger.getLogger( "ProtocolWriter" );
   //    static private final DateTimeFormatter DOCTIME_FORMATTER = DateTimeFormatter.ofPattern( "ddMMyyyykkmmssS" );
   static private final LocalDateTime TODAY_DATE = LocalDateTime.now();

   // TODO - put MaxRows (per table file) in AbstractTableFileWriter
   // TODO - put ZipFiles, ZipMaxFiles in AbstractFileWriter
   static public final String ZIP_TABLE_PARAM = "ZipTable";
   static public final String ZIP_TABLE_DESC = "Write table file in a zip file.";
   @ConfigurationParameter (
         name = ZIP_TABLE_PARAM,
         description = ZIP_TABLE_DESC,
         mandatory = false,
         defaultValue = "no"
   )
   private String _zipTable;

   static public final String ZIP_MAX_PARAM = "ZipMax";
   static public final String ZIP_MAX_DESC = "The maximum number of files per zip.";
   @ConfigurationParameter (
         name = ZIP_MAX_PARAM,
         description = ZIP_MAX_DESC,
         mandatory = false
   )
   private int _zipMax = 1000;

   static public final String PATIENT_MAX_PARAM = "PatientMax";
   static public final String PATIENT_MAX_DESC = "The maximum number of patients per table.";
   @ConfigurationParameter (
         name = PATIENT_MAX_PARAM,
         description = PATIENT_MAX_DESC,
         mandatory = false
   )
   private int _patientMax = 1000;


//    static public final String DATE_PARSE_FORMAT_PARAM = "DateParseFormat";
//    static public final String DATE_PARSE_FORMAT_DESC = "A format to parse dates.  e.g. dd-MM-yyyy_HH:mm:ss";
//    @ConfigurationParameter (
//          name = DATE_PARSE_FORMAT_PARAM,
//          description = DATE_PARSE_FORMAT_DESC,
//          defaultValue = "yyyyMMdd",
//          mandatory = false
//    )
//    private String _dateParseFormat;

   static public final String DATE_WRITE_FORMAT_PARAM = "DateWriteFormat";
   static public final String DATE_WRITE_FORMAT_DESC = "A format to write dates.  e.g. dd-MM-yyyy_HH:mm:ss";
   @ConfigurationParameter (
         name = DATE_WRITE_FORMAT_PARAM,
         description = DATE_WRITE_FORMAT_DESC,
         defaultValue = "ddMMyyyykkmmss",
         mandatory = false
   )
   private String _dateWriteFormat;

   static public final String CAS_DATE_FORMAT_PARAM = "CasDateFormat";
   static public final String CAS_DATE_FORMAT_DESC = "Set a value for parameter CasDateFormat.";
   @ConfigurationParameter (
         name = CAS_DATE_FORMAT_PARAM,
         description = CAS_DATE_FORMAT_DESC,
         defaultValue = "MMddyyyykkmmss",
         mandatory = false
   )
   private String _casDateFormat;


   static private final float MIN_CONFIDENCE = 70f;

   private DateTimeFormatter _dateWriteFormatter;
   private DateTimeFormatter _casDateFormatter;


   private int _patientIndex = 1;
   private int _patientCount = 1;
   static private final String FILE_NAME = "DeepPhe_Table";
   // Prevent asynchronous overwrite by multiple processes
   private String _runStartTime;
   // TODO - if we definitely know that jdk version is 9+, use the pid instead of millis.
//   long pid = ProcessHandle.current().pid();


   private PatientRow _patientRow;

   final private Collection<String> _brCaUris = new HashSet<>();
   final private Collection<String> _ovCaUris = new HashSet<>();
   final private Collection<String> _skinCaUris = new HashSet<>();
   final private Collection<String> _melanomaUris = new HashSet<>();
   final private Collection<String> _lentigoUris = new HashSet<>();
   final private Collection<String> _nevusUris = new HashSet<>();
   final private Collection<String> _sunburnUris = Collections.singletonList( "Sunburn" );
   final private Collection<String> _tanbedUris = Collections.singletonList( "SunlampExposure" );
   final private Collection<String> _diabetesUris = new HashSet<>();
   final private Collection<String> _hypertensionUris = new HashSet<>();
   final private Collection<String> _heartDiseaseUris = new HashSet<>();
   final private Collection<String> _lungDiseaseUris = new HashSet<>();
   final private Collection<String> _liverDiseaseUris = new HashSet<>();
   final private Collection<String> _kidneyDiseaseUris = new HashSet<>();
   final private Collection<String> _immuneDiseaseUris = new HashSet<>();
   //    final private Collection<String> _smokeUris = new HashSet<>();
//   Cardiovascular Neoplasm
   final private Collection<String> _cardioCaUris = new HashSet<>();
   //   Connective and Soft Tissue Neoplasm
   final private Collection<String> _connectSoftTissueCaUris = new HashSet<>();
   //   Digestive System Neoplasm
   final private Collection<String> _digestiveSystemCaUris = new HashSet<>();
   final private Collection<String> _analCaUris = new HashSet<>();
   final private Collection<String> _appendixCaUris = new HashSet<>();
   final private Collection<String> _esophagusCaUris = new HashSet<>();
   final private Collection<String> _gastricCaUris = new HashSet<>();
   final private Collection<String> _hepatobilaryCaUris = new HashSet<>();
   final private Collection<String> _intestineCaUris = new HashSet<>();
   final private Collection<String> _pancreasCaUris = new HashSet<>();
   final private Collection<String> _colorectalCaUris = new HashSet<>();
   //   Endocrine Neoplasm
   final private Collection<String> _endocrineSystemCaUris = new HashSet<>();
   //   Eye Neoplasm
   final private Collection<String> _eyeCaUris = new HashSet<>();
   //   Genitourinary System Neoplasm
   final private Collection<String> _genitourineCaUris = new HashSet<>();
   final private Collection<String> _fallopianeCaUris = new HashSet<>();
   final private Collection<String> _prostateCaUris = new HashSet<>();
   final private Collection<String> _testicularCaUris = new HashSet<>();
   //   Head and Neck Neoplasm
   final private Collection<String> _headNeckCaUris = new HashSet<>();
   //   Hematopoietic and Lymphoid System Neoplasm
   final private Collection<String> _hematoLymphCaUris = new HashSet<>();
   //   Nervous System Neoplasm
   final private Collection<String> _nerveCaUris = new HashSet<>();
   final private Collection<String> _brainCaUris = new HashSet<>();
   //   Peritoneal and Retroperitoneal Neoplasm
   final private Collection<String> _peritonealCaUris = new HashSet<>();
   //   Respiratory System Neoplasm
   final private Collection<String> _respiratoryCaUris = new HashSet<>();
   final private Collection<String> _lungCaUris = new HashSet<>();
   //   Spinal Neoplasm
   final private Collection<String> _spinalCaUris = new HashSet<>();
   //   Thoracic Neoplasm
   final private Collection<String> _thoracicCaUris = new HashSet<>();

   //    final private Collection<String> _lymphNodes = new HashSet<>();
   final private Collection<String> _ulcer = new HashSet<>();

   final private Map<Collection<String>, String> _siteCancersMap = new HashMap<>();
   private final Map<Collection<String>, String> _brCaHistologyMap = new HashMap<>();
   private final Map<Collection<String>, String> _ovCaHistologyMap = new HashMap<>();
   private final Map<Collection<String>, String> _melHistologyMap = new HashMap<>();
   final private Map<Collection<String>, String> _stageUriMap = new HashMap<>();
   final private Map<Collection<String>, String> _gradeUriMap = new HashMap<>();
   final private Map<Collection<String>, String> _ecogUriMap = new HashMap<>();
   final private Map<Collection<String>, String> _spaceUriMap = new HashMap<>();
   final private Map<Collection<String>, String> _melanomaUriMap = new HashMap<>();
   final private Map<Collection<String>, String> _melSiteUriMap = new HashMap<>();
   final private Map<Collection<String>, String> _melMetsUriMap = new HashMap<>();


   private void addMapUris( final String name, final Collection<String> uris, final String rootUri,
                            final Map<Collection<String>, String> map, final Collection<String> targetCancers ) {
      uris.addAll( Neo4jOntologyConceptUtil.getBranchUris( rootUri ) );
      uris.removeAll( targetCancers );
      map.put( uris, name );
   }

   private void setMapUris( final String name, final String rootUri, final Map<Collection<String>, String> map ) {
      map.put( Neo4jOntologyConceptUtil.getBranchUris( rootUri ), name );
   }


   private Collection<String> getCancerTypes( final Collection<String> uris ) {
      return uris.stream().map( this::getCancerType ).collect( Collectors.toSet() );
   }

   private String getCancerType( final String uri ) {
      for ( Map.Entry<Collection<String>, String> urisName : _siteCancersMap.entrySet() ) {
         if ( urisName.getKey().contains( uri ) ) {
            return urisName.getValue();
         }
      }
      return "other";
   }

   // TODO move to IdentifiedAnnotationUtil.   Add some constant "NORMALIZED_VALUE" to CONST ?
   static private Collection<String> getValues( final IdentifiedAnnotation annotation ) {
      return IdentifiedAnnotationUtil.getCodes( annotation, DPHE_VALUE_SCHEME )
                                     .stream()
                                     .filter( Objects::nonNull )
                                     .collect( Collectors.toSet() );
   }

   private Collection<String> getCategoryValues( final Collection<IdentifiedAnnotation> annotations,
                                                 final Map<Collection<String>, String> map ) {
      return annotations.stream().map( Neo4jOntologyConceptUtil::getUris )
                        .flatMap( Collection::stream )
                        .distinct()
                        .map( u -> getCategoryValues( u, map ) )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }

   private Collection<String> getCategoryValues( final String uri,
                                                 final Map<Collection<String>, String> map ) {
      final Collection<String> values = new HashSet<>();
      for ( Map.Entry<Collection<String>, String> urisName : map.entrySet() ) {
         if ( urisName.getKey().contains( uri ) ) {
            values.add( urisName.getValue() );
         }
      }
      return values;
   }

   private boolean anyUriMatch( final Collection<IdentifiedAnnotation> annotations,
                                final Collection<String> checkUris ) {
      return getUris( annotations ).stream().anyMatch( checkUris::contains );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _casDateFormatter = DateTimeFormatter.ofPattern( _casDateFormat );
      _dateWriteFormatter = DateTimeFormatter.ofPattern( _dateWriteFormat );
//      _runStartTime = _casDateFormatter.format( OffsetDateTime.now() );
      _runStartTime = "Feb_02_06";
      _brCaUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "BreastNeoplasm" ) );
      _ovCaUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "OvarianNeoplasm" ) );
      _skinCaUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "SkinNeoplasm" ) );
      _melanomaUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Melanoma" ) );
      _lentigoUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "LentigoMalignaMelanoma" ) );
      _skinCaUris.removeAll( _lentigoUris );
      _nevusUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "MelanocyticNevus" ) );
      _diabetesUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "DiabetesMellitus" ) );
      _hypertensionUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Hypertension" ) );
      _heartDiseaseUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "HeartDisorder" ) );
      _lungDiseaseUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "LungDisorder" ) );
      _liverDiseaseUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "LiverAndIntrahepaticBileDuctDisorder" ) );
      _kidneyDiseaseUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "KidneyDisorder" ) );
      _immuneDiseaseUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "AutoimmuneDisease" ) );
//        _smokeUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "SmokeExposure" ) );

      final Collection<String> targetCancers = new HashSet<>( _brCaUris );
      targetCancers.addAll( _ovCaUris );
      targetCancers.addAll( _skinCaUris );
      targetCancers.addAll( _melanomaUris );
      // Other Cancer Types.
      addMapUris( "cardiovascular", _cardioCaUris, "CardiovascularNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "connective_soft_tissue", _connectSoftTissueCaUris, "ConnectiveAndSoftTissueNeoplasm",
            _siteCancersMap, targetCancers );
      addMapUris( "digestive", _digestiveSystemCaUris, "DigestiveSystemNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "anal", _analCaUris, "AnalNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "appendix", _appendixCaUris, "AppendixNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "esophageal", _esophagusCaUris, "EsophagealNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "gastric", _gastricCaUris, "GastricNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "hepatobiliary", _hepatobilaryCaUris, "HepatobiliaryNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "intestinal", _intestineCaUris, "IntestinalNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "pancreatic", _pancreasCaUris, "PancreaticNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "colorectal", _colorectalCaUris, "ColorectalNeoplasm", _siteCancersMap, targetCancers );
      _digestiveSystemCaUris.removeAll( _analCaUris );
      _digestiveSystemCaUris.removeAll( _appendixCaUris );
      _digestiveSystemCaUris.removeAll( _esophagusCaUris );
      _digestiveSystemCaUris.removeAll( _gastricCaUris );
      _digestiveSystemCaUris.removeAll( _hepatobilaryCaUris );
      _digestiveSystemCaUris.removeAll( _intestineCaUris );
      _digestiveSystemCaUris.removeAll( _pancreasCaUris );
      _intestineCaUris.removeAll( _colorectalCaUris );
      addMapUris( "endocrine", _endocrineSystemCaUris, "EndocrineNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "eye", _eyeCaUris, "EyeNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "genitourinary", _genitourineCaUris, "GenitourinarySystemNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "fallopian", _fallopianeCaUris, "FallopianTubeNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "prostate", _prostateCaUris, "ProstateNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "testicular", _testicularCaUris, "TesticularNeoplasm", _siteCancersMap, targetCancers );
      _genitourineCaUris.removeAll( _ovCaUris );
      _genitourineCaUris.removeAll( _fallopianeCaUris );
      _genitourineCaUris.removeAll( _prostateCaUris );
      _genitourineCaUris.removeAll( _testicularCaUris );
      addMapUris( "head_neck", _headNeckCaUris, "HeadAndNeckNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "hematolymph", _hematoLymphCaUris, "HematopoieticAndLymphoidSystemNeoplasm", _siteCancersMap,
            targetCancers );
      addMapUris( "nervous", _nerveCaUris, "NervousSystemNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "brain", _brainCaUris, "BrainNeoplasm", _siteCancersMap, targetCancers );
      _nerveCaUris.removeAll( _brainCaUris );
      addMapUris( "peritoneal", _peritonealCaUris, "PeritonealAndRetroperitonealNeoplasm", _siteCancersMap,
            targetCancers );
      addMapUris( "respiratory", _respiratoryCaUris, "RespiratorySystemNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "lung", _lungCaUris, "LungNeoplasm", _siteCancersMap, targetCancers );
      _respiratoryCaUris.removeAll( _lungCaUris );
      addMapUris( "spinal", _spinalCaUris, "SpinalNeoplasm", _siteCancersMap, targetCancers );
      addMapUris( "thoracic", _thoracicCaUris, "ThoracicNeoplasm", _siteCancersMap, targetCancers );

      _stageUriMap.put( createUriSet( "Stage0", "InSitu", "StageIs" ), "in_situ" );
      setMapUris( "stage_1", "StageI", _stageUriMap );
      setMapUris( "stage_2", "StageII", _stageUriMap );
      _stageUriMap.put( createUriSet( "StageIII", "LocallyMetastatic" ), "stage_3" );
      _stageUriMap.put( createUriSet( "StageIV", "Metastatic", "DistantlyMetastatic", "AdvancedStage" ), "stage_4" );
      _stageUriMap.put(
            new HashSet<>( Arrays.asList( "StageX", "StageUnknown", "StageUnspecified", "StagingIncomplete" ) ),
            "unknown" );

      _gradeUriMap.put(
            new HashSet<>( Arrays.asList( "Grade1", "LowGrade", "WellDifferentiated" ) ), "G1" );
      _gradeUriMap.put(
            new HashSet<>( Arrays.asList( "Grade2", "IntermediateGrade", "ModeratelyDifferentiated" ) ), "G2" );
      _gradeUriMap.put(
            new HashSet<>( Arrays.asList( "Grade3", "Grade3a", "Grade3b", "HighGrade", "PoorlyDifferentiated" ) ),
            "G3" );
      _gradeUriMap.put( new HashSet<>( Arrays.asList( "Grade4", "Undifferentiated" ) ), "G4" );

      _ecogUriMap.put( Collections.singleton( "ECOGPerformanceStatus0" ), "0" );
      _ecogUriMap.put( Collections.singleton( "ECOGPerformanceStatus1" ), "1" );
      _ecogUriMap.put( Collections.singleton( "ECOGPerformanceStatus2" ), "2" );
      _ecogUriMap.put( Collections.singleton( "ECOGPerformanceStatus3" ), "3" );
      _ecogUriMap.put( Collections.singleton( "ECOGPerformanceStatus4" ), "4" );
      _ecogUriMap.put( Collections.singleton( "ECOGPerformanceStatus5" ), "5" );

      _spaceUriMap.put( Collections.singleton( "local" ), "local" );
      _spaceUriMap.put( new HashSet<>( Arrays.asList( "Regional", "Local_sub_Regional" ) ), "regional" );
      _spaceUriMap.put( Collections.singleton( "Remote" ), "distant" );

      setMapUris( "ductal", "BreastDuctalCarcinoma", _brCaHistologyMap );
      setMapUris( "lobular", "BreastLobularCarcinoma", _brCaHistologyMap );
      setMapUris( "mixed_d_l", "BreastMixedDuctalAndLobularCarcinoma", _brCaHistologyMap );
      setMapUris( "papillary", "BreastPapillaryNeoplasm", _brCaHistologyMap );
      setMapUris( "tubular", "BreastTubularCarcinoma", _brCaHistologyMap );
      setMapUris( "mucinous", "BreastMucinousCarcinoma", _brCaHistologyMap );
      setMapUris( "metaplastic", "BreastMetaplasticCarcinoma", _brCaHistologyMap );
      setMapUris( "phyllodes", "BreastPhyllodesTumor", _brCaHistologyMap );
      setMapUris( "paget", "BreastPagetDisease", _brCaHistologyMap );
      setMapUris( "inflammatory", "BreastInflammatoryCarcinoma", _brCaHistologyMap );

      setMapUris( "germ", "OvarianGermCellTumor", _ovCaHistologyMap );
      setMapUris( "stromal", "OvarianSexCord_sub_StromalTumor", _ovCaHistologyMap );
      setMapUris( "neuroendocrine", "OvarianNeuroendocrineNeoplasm", _ovCaHistologyMap );
      setMapUris( "soft_tissue", "OvarianSoftTissueNeoplasm", _ovCaHistologyMap );
      final Collection<String> clearCell =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "OvarianClearCellTumor" ) );
      final Collection<String> endometrioid =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "OvarianEndometrioidTumor" ) );
      final Collection<String> mucinous =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "OvarianMucinousTumor" ) );
      final Collection<String> serous = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "OvarianSerousTumor" ) );
      final Collection<String> papillary =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "OvarianPapillaryTumor" ) );
      final Collection<String> seromucinous =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "OvarianSeromucinousTumor" ) );
      _ovCaHistologyMap.put( clearCell, "clear" );
      _ovCaHistologyMap.put( endometrioid, "endometrioid" );
      _ovCaHistologyMap.put( mucinous, "mucinous" );
      _ovCaHistologyMap.put( serous, "serous" );
      _ovCaHistologyMap.put( papillary, "papillary" );
      _ovCaHistologyMap.put( seromucinous, "seromucinous" );
      final Collection<String> epithelial =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "OvarianEpithelialTumor" ) );
      epithelial.removeAll( clearCell );
      epithelial.removeAll( endometrioid );
      epithelial.removeAll( mucinous );
      epithelial.removeAll( serous );
      epithelial.removeAll( papillary );
      epithelial.removeAll( seromucinous );
      _ovCaHistologyMap.put( epithelial, "epithelial_nos" );

      final Collection<String> acrals =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "AcralLentiginousMelanoma" ) );
      _melanomaUriMap.put( acrals, "acral" );
      final Collection<String> cutaneous =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "CutaneousMelanoma" ) );
      cutaneous.removeAll( acrals );
      _melanomaUriMap.put( cutaneous, "cutaneous" );
      setMapUris( "mucosal", "MucosalMelanoma", _melanomaUriMap );
      setMapUris( "ocular", "OcularMelanoma", _melanomaUriMap );

      final Collection<String> lowCsd =
            new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Low_sub_CSDMelanoma" ) );
      final Collection<String> desmoplastic = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Desmoplastic" ) );
      _melHistologyMap.put( desmoplastic, "desmoplastic" );
      _melHistologyMap.put( _lentigoUris, "lentigo_maligna" );
      lowCsd.removeAll( desmoplastic );
      lowCsd.removeAll( _lentigoUris );
      _melHistologyMap.put( lowCsd, "superficial_spreading" );
      _melHistologyMap.put( acrals, "acral_lentiginous" );
      setMapUris( "nodular", "CutaneousNodularMelanoma", _melHistologyMap );

      final Collection<String> face = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Face" ) );
      _melSiteUriMap.put( face, "face" );
      final Collection<String> scalp = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Scalp" ) );
      _melSiteUriMap.put( scalp, "scalp" );
      final Collection<String> headAndNeck = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "HeadAndNeck" ) );
      headAndNeck.removeAll( face );
      headAndNeck.removeAll( scalp );
      _melSiteUriMap.put( headAndNeck, "head_nos" );
      _melSiteUriMap.put(
            createUriSet( "Trunk", "Chest", "Abdomen", "Thorax", "Shoulder", "Hip", "SpinalRegion" ),
            "torso" );
      _melSiteUriMap.put( createUriSet( "Limb", "FemoralRegion" ), "extremities" );

      final Collection<String> subcutaneous = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Subcutaneous" ) );
      final Collection<String> skin = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "SkinTissue" ) );
      skin.removeAll( subcutaneous );
      _melMetsUriMap.put( skin, "skin" );
      _melMetsUriMap.put( subcutaneous, "subcutaneous" );
      setMapUris( "bladder", "Bladder", _melMetsUriMap );
      setMapUris( "brain", "Brain", _melMetsUriMap );
      setMapUris( "heart", "Heart", _melMetsUriMap );
      setMapUris( "intestine", "Intestine", _melMetsUriMap );
      setMapUris( "kidney", "Kidney", _melMetsUriMap );
      setMapUris( "liver", "Liver", _melMetsUriMap );
      setMapUris( "lung", "Lung", _melMetsUriMap );
      setMapUris( "pancreas", "Pancreas", _melMetsUriMap );
      setMapUris( "spleen", "Spleen", _melMetsUriMap );
      setMapUris( "stomach", "Stomach", _melMetsUriMap );
      setMapUris( "uterus", "Uterus", _melMetsUriMap );

//        _lymphNodes.addAll( Neo4jOntologyConceptUtil.getBranchUris( "LymphNode" ) );
      _ulcer.addAll( Neo4jOntologyConceptUtil.getBranchUris( " TissueDamage" ) );

      final File protocolFile = new File( getRootDirectory(), getFileName() );
      try ( Writer writer = new BufferedWriter( new FileWriter( protocolFile ) ) ) {
         writer.write( getHeader() );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   private Collection<String> createUriSet( final String... roots ) {
      final Collection<String> set = new HashSet<>();
      for ( String root : roots ) {
         set.addAll( Neo4jOntologyConceptUtil.getBranchUris( root ) );
      }
      return set;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      if ( _patientRow != null ) {
         try {
            writePatientRow();
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
            throw new AnalysisEngineProcessException( ioE );
         }
      }
      if ( _patientCount > 1 ) {
         final File protocolFile = new File( getRootDirectory(), getFileName() );
         zipFile( protocolFile );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      if ( _patientRow == null ) {
         _patientRow = new PatientRow( patientId );
      }
      _patientRow.addCasInfo( jCas );
      if (!PatientDocCounter.getInstance().isPatientFull(patientId)) {
         return;
      }
      writePatientRow();
      _patientRow = null;
   }

   /**
    * If we are zipping the files returns the root directory.  Otherwise, runs the super method.
    * {@inheritDoc}
    */
   @Override
   protected String getOutputDirectory( final JCas jcas, final String rootPath, final String documentId ) {
      return rootPath;
   }

   private void writePatientRow() throws IOException {
      LOGGER.info( "Writing Patient " + _patientRow._patientId );
      final File protocolFile = new File( getRootDirectory(), getFileName() );
      try ( Writer writer = new BufferedWriter( new FileWriter( protocolFile, true ) ) ) {
         writer.append( _patientRow.getRow() );
      }
      incrementCount( protocolFile );
   }


   private String getFileName() {
      final String filename = FILE_NAME + "_Protocol_" + _runStartTime;
      if ( _patientMax <= 0 ) {
         return filename + ".bsv";
      }
      return filename + "_" + String.format( "%09d", _patientIndex ) + ".bsv";
   }

   private void incrementCount( final File protocolFile ) {
      _patientCount++;
      if ( _patientCount > _patientMax ) {
         zipFile( protocolFile );
         _patientCount = 1;
         _patientIndex++;
      }
   }

   // TODO move to ctakes system utils
   private void zipFile( final File protocolFile ) {
      if ( !AeParamUtil.isTrue( _zipTable ) ) {
         return;
      }
      LOGGER.info( "Zipping " + protocolFile.getAbsolutePath() );
      try ( FileOutputStream fos = new FileOutputStream( protocolFile.getPath() + ".zip" );
            ZipOutputStream zipOut = new ZipOutputStream( fos );
            FileInputStream fis = new FileInputStream( protocolFile ) ) {
         final ZipEntry zipEntry = new ZipEntry( protocolFile.getName() );
         zipOut.putNextEntry( zipEntry );
         byte[] bytes = new byte[ 1024 ];
         int length;
         while ( (length = fis.read( bytes )) >= 0 ) {
            zipOut.write( bytes, 0, length );
         }
      } catch ( IOException ioE ) {
         LOGGER.warn( "Could not zip file " + protocolFile.getAbsolutePath() );
         return;
      }
      if ( !protocolFile.delete() ) {
         LOGGER.warn( "Could not delete " + protocolFile.getAbsolutePath() );
      }
   }

   private Collection<String> getUris( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( Neo4jOntologyConceptUtil::getUris )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }

   static private boolean inUris( final Collection<String> uris, final IdentifiedAnnotation annotation ) {
      return Neo4jOntologyConceptUtil.getUris( annotation )
                                     .stream()
                                     .anyMatch( uris::contains );
   }


   private Collection<IdentifiedAnnotation> getFamily( final DpheGroup group,
                                                       final Map<DpheGroup, List<IdentifiedAnnotation>> groupAnnotations ) {
      return groupAnnotations.getOrDefault( group, Collections.emptyList() )
                             .stream()
                             .filter( a -> a.getSubject().equals( CONST.ATTR_SUBJECT_FAMILY_MEMBER ) )
                             .filter( a -> a.getConfidence() > MIN_CONFIDENCE )
                             .collect( Collectors.toList() );
   }

   //Family History of Any Cancer (famhx_anyca)
   //Value set: Yes, No, Unknown

   //Family History of Breast Cancer (famhx_brca)
   //Value set: Yes, No, Unknown

   //Family History of Ovarian Cancer (famhx_cvca)
   //Value set: Yes, No, Unknown

   //Family History of Skin Cancer (fhx_skinca)
   //Value set: Yes, No, Unknown


   private Collection<IdentifiedAnnotation> getPatient( final DpheGroup group,
                                                        final Map<DpheGroup, List<IdentifiedAnnotation>> groupAnnotations ) {
      return groupAnnotations.getOrDefault( group, Collections.emptyList() )
                             .stream()
                             .filter( a -> a.getSubject().equals( CONST.ATTR_SUBJECT_PATIENT ) )
                             .filter( a -> a.getConfidence() > MIN_CONFIDENCE )
                             .collect( Collectors.toList() );
   }

   private Collection<IdentifiedAnnotation> getYesAllPatient( final DpheGroup group,
                                                              final Map<DpheGroup, List<IdentifiedAnnotation>> groupAnnotations ) {
      return groupAnnotations.getOrDefault( group, Collections.emptyList() )
                             .stream()
                             .filter( a -> a.getSubject().equals( CONST.ATTR_SUBJECT_PATIENT ) )
                             .collect( Collectors.toList() );
   }


   private Collection<IdentifiedAnnotation> getYesPatient( final DpheGroup group,
                                                           final Map<DpheGroup, List<IdentifiedAnnotation>> groupAnnotations ) {
      return groupAnnotations.getOrDefault( group, Collections.emptyList() )
                             .stream()
                             .filter( a -> a.getSubject().equals( CONST.ATTR_SUBJECT_PATIENT ) )
                             .filter( IdentifiedAnnotationUtil::isRealAffirmed )
                             .filter( a -> a.getConfidence() > MIN_CONFIDENCE )
                             .collect( Collectors.toList() );
   }

   //Personal History of Lentigo Maligna (phx_maligna)
   //Value set: Yes, No, Unknown (or noted and not noted?)

   //Personal History of Large Nevi or Moles (phx_nevi)
   //Value set: Yes, No, Unknown (or noted and not noted?)

   //Personal History of Sunburns (phx_sun)
   //Value set: Yes, No, Unknown (or noted and not noted?)

   //Personal History of Tanning Booth Use (phx_tan)
   //Value set: Yes, No, Unknown (or noted and not noted?)


   //Diabetes (comorb_diabetes)
   //Value set: Yes, No, Unknown/Not Found

   //Hypertension (comorb_htn)
   //Value set: Yes, No, Unknown/Not Found

   //Heart Disease (comorb_heart)
   //Value set: Yes, No, Unknown/Not Found

   //Lung Disease (comorb_lung)
   //Value set: Yes, No, Unknown/Not Found

   //Liver Disease (comorb_liver)
   //Value set: Yes, No, Unknown/Not Found

   //Kidney Disease (comorb_kidney)
   //Value set: Yes, No, Unknown/Not Found

   //Autoimmune Diseases (comorb_autoimm)
   //Value set: Yes, No, Unknown/Not Found

   //Smoking (comorb_smk)
   //Value set: past, current, never, unknown


   private enum SMOKING_STATUS {
      Unknown,
      Never,
      Past,
      Current;
      static private final Collection<String> NON_SMOKER_KEYS = Arrays.asList(
            "nonsmoker", "non-smoker", "non - smoker", "doesn't smoke", "does not smoke", "never smoked" );
      static private final Collection<String> PAST_SMOKER_KEYS = Arrays.asList(
            "previous smoker", "past smoker", "quit smoking", "stopped smoking", "used to smoke" );
      static private final Collection<String> SMOKER_KEYS = Arrays.asList(
            "smoker", "smokes" );

      static private SMOKING_STATUS getSmokingStatus( final JCas jCas ) {
         final String text = jCas.getDocumentText().toLowerCase();
         for ( String key : NON_SMOKER_KEYS ) {
            if ( text.contains( key ) ) {
               return SMOKING_STATUS.Past;
            }
         }
         for ( String key : PAST_SMOKER_KEYS ) {
            if ( text.contains( key ) ) {
               return SMOKING_STATUS.Past;
            }
         }
         for ( String key : SMOKER_KEYS ) {
            if ( text.contains( key ) ) {
               return SMOKING_STATUS.Current;
            }
         }
         return SMOKING_STATUS.Unknown;
      }
   }


   //Date of diagnosis (dt_dx)
   //Value set: date
   //Source (structured data, clinical narrative, both): both
   //Tool: DeepPhe for clinical narrative and query over structured data (tumor registry)
   //Note: identification of the date of diagnosis is based on an algorithm from Warner et al.,
   //   4 plus modification to start with tumor registry date of diagnosis if available. Hierarchy: 1) date of diagnosis from tumor registry data; if not available, then by whether cancer case was diagnosed internal or external to EMR system (based on if EMR exits at least 45 days prior to the first ICD code for cancer); if internal, then 2) date of first relevant ICD code; if external, then earliest of 2) first relevant path report date or 3) date of external path review; if not available then date of first relevant ICD code.

   //Episode of assessment - Variables: episode of assessment for all three tumor markers
   //Value set: episode (pre-diagnostic, diagnostic, decision making, treatment, follow-up, and unknown) of first assessment document (for each marker)


   //Histology – Tumor Registry (ca_hist_tr), Clinical Narrative (ca_hist_emr), Summary (ca_hist_sum)
   //Value set:
   //For breast: ductal, lobular, other, unknown (and if other, then type)
   //For ovary: germ, sex cord / stromal, clear cell, endometrioid, mucinous, serous, other,
   //   epithelial NOS, unknown (and if other, then type)

   //Stage of Disease – Tumor Registry (ca_stage_tr), Clinical (ca_stage_clin), Pathological (ca_stage_path), and Summary (ca_stage_sum)
   //Value set: in situ, stage 1, 2, 3, or 4, unknown

   //Grade – tumor registry (ca_grade_tr), clinical (ca_grade_emr), summary (ca_grade_sum)
   //Value set: G1 (low grade or well differentiated), G2 (moderately differentiated), G3 (high grade), G4 (undifferentiated), G9 (unknown / not found)

   //Eastern Cooperative Group (ECOG) Performance Status
   //	Value set: 0, 1, 2, 3, 4

   //Stage of Disease (mel_stage)
   //Value set: local (in situ, 1A, 1B, 1 NOS, 2A, 2B, 2C, 2 NOS); regional (3A, 3B, 3C, 3D, 3 NOS); distant (4), unstaged/unstageable or unknown

   // Surgeries, Therapies,  are marked for structured data only.

   //Tumor Markers - Variables: if ER, PR, and HER2 assessed, and result of assessment, for each
   //Value set: if assessed (Yes/No/Unknown), if Y then result (Positive, Negative, Unknown)

   //Date of assessment - Variables: date of assessment for all three tumor markers (date of first report)
   //Value set: date stamp of first assessment document (for each)
   //Source (structured data, clinical narrative, both): clinical narrative filenames


   //Variables: BRCA1/2, PIK3CA, and TP53 if assessed, and result of assessment, for each
   //Value set: if assessed (Yes/No/Unknown), if Y then result (Positive, Negative, Unknown)

   //Variables: date of assessment for all clinical genomics (for each)
   //	Value set: date stamp of first assessment document (date of first report)
   //	Source (structured data, clinical narrative, both): clinical narrative filenames

   //Variables: episode of assessment for all clinical genomics (for each)
   //Value set: episode (pre-diagnostic, diagnostic, decision making, treatment, follow-up, and unknown) of first assessment document (for each)

   // It seems like this should be a sum of the recorded cancer diagnoses.
//Number of Cancer Diagnoses (ca_n)
//Value set: 0 1, 2, etc. (continuous)

   //Breast Cancer (ca_breast)
   //Value set: Y/N

   //Ovarian Cancer (ca_ovary)
   //Value set: Y/N

   //Other Cancer (ca_other)
   //Value set: Y/N

   // You can't just say "etc.".  There are thousands of cancer classes in dphe.
   //Other Cancer Type (ca_other)
   //Value set: Endometrial, Colorectal, Lung, etc.

   //: episode flags: pre-diagnostic, diagnostic, decision making, treatment, follow-up
   //	Value set: Y/N (Yes if documents within an episode were identified, No if not identified)

   //Type of Melanoma Site (mel_type)
   //Value set: cutaneous, acral (non-hair-bearing skin, hands and feet), mucosal (oral/buccal, rectal, vulvovaginal), ocular (uveal or retinal)

   //Histologic Type (mel_hist)
   //Value set: superficial spreading, nodular, lentigo maligna, acral lentiginous, desmoplastic, and other

   //Anatomic Site (cut_mel_site)
   //Value set: face, scalp, torso, extremities, and other regions

   //Site of Metastasis (mel_met)
   //Value set: skin, subcutaneous and lymph node, lung, visceral (liver and other solid organ locations other than lung), brain

   // This was not recorded!
//Breslow Depth (mel_breslow)
//Value set: numeric value in millimeters

   //Ulceration (mel_ulcer)
   //Value set: Present/Not present

   private enum YES_NO_UNKNOWN {
      Unknown,
      No,
      Yes;

      static private int getOrdinal( final IdentifiedAnnotation annotation ) {
         if ( IdentifiedAnnotationUtil.isNegated( annotation ) ) {
            return No.ordinal();
         }
         if ( IdentifiedAnnotationUtil.isUncertain( annotation ) ) {
            return Unknown.ordinal();
         }
         return Yes.ordinal();
      }

      static private YES_NO_UNKNOWN getCancerStatus( final Collection<IdentifiedAnnotation> cancers ) {
         final int ordinal = cancers.stream()
                                    .mapToInt( YES_NO_UNKNOWN::getOrdinal )
                                    .max()
                                    .orElse( 0 );
         return values()[ ordinal ];
      }

      static private YES_NO_UNKNOWN getBranchStatus( final Collection<String> uris,
                                                     final Collection<IdentifiedAnnotation> annotations ) {
         final int ordinal = annotations.stream()
                                        .filter( a -> inUris( uris, a ) )
                                        .mapToInt( YES_NO_UNKNOWN::getOrdinal )
                                        .max()
                                        .orElse( 0 );
         return values()[ ordinal ];
      }
   }


   static private Map<DpheGroup, List<IdentifiedAnnotation>> makeDpheGroupMap(
         final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream().collect( Collectors.groupingBy( a ->
            DpheGroupAccessor.getInstance().getBestGroup(
                  DpheGroupAccessor.getInstance().getAnnotationGroups( a ) ) ) );
   }

   public enum YES_NO_TITLE {
      famhx_anyca, famhx_brca, famhx_cvca, fhx_skinca,
      ca_breast, ca_ovary, ca_skin, ca_mel,
      phx_maligna, phx_nevi, phx_sun, phx_tan,
      comorb_htn, comorb_diabetes, comorb_heart, comorb_lung, comorb_liver, comorb_kidney, comorb_autoimm,
      mel_ulcer
   }

   //   TODO: For Melanoma add BRAF, NRAS, KIT, CTTNB1, GNA11, GNAQ and NF1   See 7.5 of incomplete protocol.


   public enum CATEGORY_TITLE {
      ca_other,
      ca_hist_emr_brca, ca_hist_emr_ovca,
      ca_stage_sum, ca_grade_emr, ecog, mel_stage, mel_type, mel_hist, cut_mel_site, mel_met,
      ER, PR, HER2, BRCA1, BRCA2, PIK3CA, TP53,
      BRAF, NRAS, KIT, CTTNB1, GNA11, GNAQ, NF1
   }

   private String getHeader() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "PatientID|" );
      Arrays.stream( YES_NO_TITLE.values() )
            .map( Enum::name )
            .forEach( t -> sb.append( t ).append( '|' ) );
      Arrays.stream( CATEGORY_TITLE.values() )
            .map( Enum::name )
            .forEach( t -> sb.append( t ).append( '|' ) );
      // Invented "episodes" to replace protocol's non-specific
      sb.append( "episodes" ).append( '|' )
        .append( "comorb_smk" ).append( '|' ).append( "doc_count" ).append( "\n" );
      return sb.toString();
   }

   private final class PatientRow {
      final private Map<YES_NO_TITLE, YesNoInfo> _yesNoInfoMap = new EnumMap<>( YES_NO_TITLE.class );
      final private Map<CATEGORY_TITLE, CategoryInfo> _categoryInfoMap = new EnumMap<>( CATEGORY_TITLE.class );
      final private SmokerInfo _smokingStatus = new SmokerInfo();
      final private Collection<String> _episodes = new HashSet<>();
      final private String _patientId;
      private int _docCount = 0;

      private PatientRow( final String patientId ) {
         _patientId = patientId;
      }

      private void addCasInfo( final JCas jCas ) {
         _docCount++;
         final LocalDateTime docDate = getDocDate( jCas );
         final String episode = getEpisode( jCas );
         final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );
         final Map<DpheGroup, List<IdentifiedAnnotation>> dpheGroupMap = makeDpheGroupMap( annotations );
         handleFamily( docDate, episode, dpheGroupMap );
         handlePatient( jCas, docDate, episode, dpheGroupMap );
         _episodes.add( episode );
      }

      private boolean hasYes( final YES_NO_TITLE title ) {
         final YesNoInfo yesNo = _yesNoInfoMap.get( title.name() );
         return yesNo != null && yesNo.hasYes();
      }

      private LocalDateTime getDocDate( final JCas jCas ) {
         final SourceData sourceData = SourceMetadataUtil.getOrCreateSourceData( jCas );
         String casDate = sourceData.getSourceRevisionDate();
         if ( casDate == null || casDate.isEmpty() ) {
            casDate = sourceData.getSourceOriginalDate();
         }
         if ( casDate == null || casDate.isEmpty() ) {
            return TODAY_DATE;
         }
//            LOGGER.info( "Cas Date " + casDate + "  Run Start " + _runStartTime );
         LocalDateTime dateTime;
         try {
            dateTime = LocalDateTime.parse( casDate, _casDateFormatter );
         } catch ( DateTimeParseException dtpE ) {
            final LocalDate date = LocalDate.parse( casDate, _casDateFormatter );
            dateTime = date.atStartOfDay();
         }
         return dateTime;
      }

      private String getEpisode( final JCas jCas ) {
         final Collection<Episode> episodes = JCasUtil.select( jCas, Episode.class );
         if ( episodes == null ) {
            return "unknown";
         }
         return episodes.stream()
                        .map( Episode::getEpisodeType )
                        .filter( Objects::nonNull )
                        .filter( t -> !t.isEmpty() )
                        .map( e -> e.replace( "Medical ", "" ) )
                        .map( String::toLowerCase )
                        .findFirst()
                        .orElse( "unknown" );
      }

      private void handleFamily( final LocalDateTime docDate, final String episode,
                                 final Map<DpheGroup, List<IdentifiedAnnotation>> groupAnnotations ) {
         final Collection<IdentifiedAnnotation> familyCancers = getFamily( DpheGroup.CANCER, groupAnnotations );
         _yesNoInfoMap.computeIfAbsent( famhx_anyca, v -> new YesNoInfo() )
                      .update( docDate, episode, YES_NO_UNKNOWN.getCancerStatus( familyCancers ) );
         updateYesNoInfo( famhx_brca, docDate, episode, _brCaUris, familyCancers );
         updateYesNoInfo( famhx_cvca, docDate, episode, _ovCaUris, familyCancers );
         updateYesNoInfo( fhx_skinca, docDate, episode, _skinCaUris, familyCancers );
      }

      private void updateYesNoInfo( final YES_NO_TITLE title, final LocalDateTime docDate, final String episode,
                                    final Collection<String> uris,
                                    final Collection<IdentifiedAnnotation> annotations ) {
         _yesNoInfoMap.computeIfAbsent( title, v -> new YesNoInfo() )
                      .update( docDate, episode, YES_NO_UNKNOWN.getBranchStatus( uris, annotations ) );
      }

      private void updateCategory( final CATEGORY_TITLE title, final LocalDateTime docDate, final String episode,
                                   final Collection<IdentifiedAnnotation> annotations,
                                   final Map<Collection<String>, String> map ) {
         final Collection<String> values = getCategoryValues( annotations, map );
         if ( values.isEmpty() ) {
            return;
         }
         _categoryInfoMap.computeIfAbsent( title, v -> new CategoryInfo() )
                         .update( docDate, episode, values );
      }


      private void updateBiomarker( final CATEGORY_TITLE title, final LocalDateTime docDate, final String episode,
                                    final Collection<IdentifiedAnnotation> annotations,
                                    final String uri ) {
         final Collection<String> values = annotations.stream()
                                                      .filter(
                                                            a -> Neo4jOntologyConceptUtil.getUris( a ).contains( uri ) )
                                                      .map( ProtocolWriter::getValues )
                                                      .flatMap( Collection::stream )
                                                      .distinct()
                                                      .map( BiomarkerNormalizer::getNormalValues )
                                                      .flatMap( Collection::stream )
                                                      .collect( Collectors.toSet() );
         // Positive, Negative, Elevated, Unknown, Equivocal, Not Assessed
         if ( values.isEmpty() ) {
            return;
         }
         // Positive, Negative, Elevated, Unknown, Equivocal, Not Assessed ...
         // ... Assessed, Can Assess, Will Assess, Will Not Assess, NO VALUE.
         if ( values.contains( "Can Assess" ) ) {
            values.remove( "Can Assess" );
            values.add( "Not Assessed" );
         }
         if ( values.contains( "Will Assess" ) ) {
            values.remove( "Will Assess" );
            values.add( "Not Assessed" );
         }
         if ( values.contains( "Will Not Assess" ) ) {
            values.remove( "Will Not Assess" );
            values.add( "Not Assessed" );
         }
         values.remove( "NO_VALUE" );
         if ( values.isEmpty() ) {
            return;
         }
         _categoryInfoMap.computeIfAbsent( title, v -> new CategoryInfo() )
                         .update( docDate, episode, values );
      }

      private void handlePatient( final JCas jCas, final LocalDateTime docDate, final String episode,
                                  final Map<DpheGroup, List<IdentifiedAnnotation>> groupAnnotations ) {
         final Collection<IdentifiedAnnotation> cancers = getPatient( DpheGroup.CANCER, groupAnnotations );
         updateYesNoInfo( ca_breast, docDate, episode, _brCaUris, cancers );
         updateYesNoInfo( ca_ovary, docDate, episode, _ovCaUris, cancers );
         // Invented.  Not in melanoma protocol.  Are they trying to use lentigo?  or melanoma?
         updateYesNoInfo( ca_skin, docDate, episode, _skinCaUris, cancers );
         updateYesNoInfo( ca_mel, docDate, episode, _melanomaUris, cancers );
         updateCategory( ca_other, docDate, episode, cancers, _siteCancersMap );
         updateYesNoInfo( phx_maligna, docDate, episode, _lentigoUris, cancers );
         updateYesNoInfo( phx_nevi, docDate, episode, _nevusUris, cancers );
         final Collection<IdentifiedAnnotation> findings = getPatient( DpheGroup.FINDING, groupAnnotations );
         updateYesNoInfo( phx_sun, docDate, episode, _sunburnUris, findings );
         updateYesNoInfo( phx_tan, docDate, episode, _tanbedUris, findings );
         updateYesNoInfo( comorb_htn, docDate, episode, _hypertensionUris, findings );
         final Collection<IdentifiedAnnotation> disorders = getPatient( DpheGroup.DISEASE_OR_DISORDER,
               groupAnnotations );
         updateYesNoInfo( comorb_diabetes, docDate, episode, _diabetesUris, disorders );
         updateYesNoInfo( comorb_heart, docDate, episode, _heartDiseaseUris, disorders );
         updateYesNoInfo( comorb_lung, docDate, episode, _lungDiseaseUris, disorders );
         updateYesNoInfo( comorb_liver, docDate, episode, _liverDiseaseUris, disorders );
         updateYesNoInfo( comorb_kidney, docDate, episode, _kidneyDiseaseUris, disorders );
         updateYesNoInfo( comorb_autoimm, docDate, episode, _immuneDiseaseUris, disorders );
         // Modified Protocol's generic "Histology – Clinical Narrative (ca_hist_emr)" for brca vs. ovca
         final Collection<IdentifiedAnnotation> yesCancers = getPatient( DpheGroup.CANCER, groupAnnotations );
         updateCategory( ca_hist_emr_brca, docDate, episode, yesCancers, _brCaHistologyMap );
         updateCategory( ca_hist_emr_ovca, docDate, episode, yesCancers, _ovCaHistologyMap );
         final Collection<IdentifiedAnnotation> stages
               = new ArrayList<>( getYesAllPatient( DpheGroup.DISEASE_STAGE_QUALIFIER, groupAnnotations ) );
         stages.addAll( getYesPatient( DpheGroup.BEHAVIOR, groupAnnotations ) );
         updateCategory( ca_stage_sum, docDate, episode, stages, _stageUriMap );
         final Collection<IdentifiedAnnotation> grades = getYesAllPatient( DpheGroup.DISEASE_GRADE_QUALIFIER,
               groupAnnotations );
         updateCategory( ca_grade_emr, docDate, episode, grades, _gradeUriMap );
         final Collection<IdentifiedAnnotation> results = getYesPatient( DpheGroup.TEST_RESULT, groupAnnotations );
         updateCategory( ecog, docDate, episode, results, _ecogUriMap );
         if ( hasYes( ca_mel ) ) {
            final Collection<IdentifiedAnnotation> spaces =
                  getYesPatient( DpheGroup.SPATIAL_QUALIFIER, groupAnnotations );
            // Don't want to check melanoma "local, regional, distant" if no melanoma.
            updateCategory( mel_stage, docDate, episode, spaces, _spaceUriMap );
            updateCategory( mel_type, docDate, episode, cancers, _melanomaUriMap );
            updateCategory( mel_hist, docDate, episode, cancers, _melHistologyMap );
            final Collection<IdentifiedAnnotation> sites
                  = new ArrayList<>( getYesPatient( DpheGroup.BODY_PART, groupAnnotations ) );
            sites.addAll( getYesPatient( DpheGroup.ORGAN_SYSTEM, groupAnnotations ) );
            sites.addAll( getYesPatient( DpheGroup.BODY_CAVITY, groupAnnotations ) );
            sites.addAll( getYesPatient( DpheGroup.BODY_REGION, groupAnnotations ) );
            sites.addAll( getYesPatient( DpheGroup.ORGAN, groupAnnotations ) );
            updateCategory( cut_mel_site, docDate, episode, sites, _melSiteUriMap );
            updateCategory( mel_met, docDate, episode, sites, _melMetsUriMap );
            // Modified from Protocol, yes,No,Unk instead of "present / Not Present"
            updateYesNoInfo( mel_ulcer, docDate, episode, Collections.singleton( "Ulceration" ),
                  getPatient( DpheGroup.PATHOLOGIC_PROCESS, groupAnnotations ) );
            updateYesNoInfo( mel_ulcer, docDate, episode, _ulcer, findings );
         }
         // Different from protocol.  Instead of y/n/u and value if yes:
         // Positive, Negative, Elevated, Unknown, Equivocal, Not Assessed
         updateBiomarker( ER, docDate, episode, results, "EstrogenReceptorStatus" );
         updateBiomarker( PR, docDate, episode, results, "ProgesteroneReceptorStatus" );
         updateBiomarker( HER2, docDate, episode, results, "HER2_sl_NeuStatus" );
         final Collection<IdentifiedAnnotation> geneProducts = getPatient( DpheGroup.GENE_PRODUCT, groupAnnotations );
         updateBiomarker( BRCA1, docDate, episode, geneProducts, "BreastCancerType1SusceptibilityProtein" );
         updateBiomarker( BRCA2, docDate, episode, geneProducts, "BreastCancerType2SusceptibilityProtein" );
         updateBiomarker( PIK3CA, docDate, episode, geneProducts,
               "Phosphatidylinositol4_cma_5_sub_Bisphosphate3_sub_KinaseCatalyticSubunitAlphaIsoform" );
         updateBiomarker( TP53, docDate, episode, geneProducts, "CellularTumorAntigenP53" );

         //   TODO: For Melanoma add BRAF, NRAS, KIT, CTTNB1, GNA11, GNAQ and NF1   See 7.5 of incomplete protocol.
         updateBiomarker( BRAF, docDate, episode, geneProducts, "Serine_sl_Threonine_sub_ProteinKinaseB_sub_Raf" );
         updateBiomarker( NRAS, docDate, episode, geneProducts, "GTPaseNras" );
         updateBiomarker( KIT, docDate, episode, geneProducts, "Mast_sl_StemCellGrowthFactorReceptorKit" );
         updateBiomarker( CTTNB1, docDate, episode, geneProducts, "CateninBeta_sub_1" );
         updateBiomarker( GNA11, docDate, episode, geneProducts,
               "GuanineNucleotide_sub_BindingProteinSubunitAlpha_sub_11" );
         updateBiomarker( GNAQ, docDate, episode, geneProducts,
               "GuanineNucleotide_sub_BindingProteinG_lpn_q_rpn_SubunitAlpha" );
         updateBiomarker( NF1, docDate, episode, geneProducts, "Neurofibromin" );


         _smokingStatus.update( docDate, episode, SMOKING_STATUS.getSmokingStatus( jCas ) );
      }


      private String getRow() {
         final StringBuilder sb = new StringBuilder();
         sb.append( _patientId ).append( '|' );
         for ( YES_NO_TITLE yesNo : YES_NO_TITLE.values() ) {
            final YesNoInfo info = _yesNoInfoMap.getOrDefault( yesNo, EMPTY_YES_NO_INFO );
            sb.append( info.getCell() ).append( '|' );
         }
         for ( CATEGORY_TITLE category : CATEGORY_TITLE.values() ) {
            final CategoryInfo info = _categoryInfoMap.getOrDefault( category, EMPTY_HISTO_INFO );
            sb.append( info.getCell() ).append( '|' );
         }
         sb.append( _episodes.stream().sorted().collect( Collectors.joining( ";" ) ) ).append( '|' )
           .append( _smokingStatus.getCell() ).append( '|' )
           .append( _docCount ).append( "\n" );
         return sb.toString();
      }
   }

   private final YesNoInfo EMPTY_YES_NO_INFO = new YesNoInfo();
   private final CategoryInfo EMPTY_HISTO_INFO = new CategoryInfo();

   private class YesNoInfo {
      final private Map<YES_NO_UNKNOWN, LocalDateTime> _dateMap = new EnumMap<>( YES_NO_UNKNOWN.class );
      final private Map<YES_NO_UNKNOWN, String> _episodeMap = new EnumMap<>( YES_NO_UNKNOWN.class );

      private void update( final LocalDateTime docDate, final String episode, final YES_NO_UNKNOWN status ) {
         final LocalDateTime statusDate = _dateMap.get( status );
         if ( statusDate == null || docDate.isBefore( statusDate ) ) {
            _dateMap.put( status, docDate );
            _episodeMap.put( status, episode );
         }
      }

      private boolean hasYes() {
         return _dateMap.containsKey( YES_NO_UNKNOWN.Yes );
      }

      private String getCell() {
         return Arrays.stream( YES_NO_UNKNOWN.values() )
                      .map( this::getValueCell )
                      .filter( c -> !c.isEmpty() )
                      .collect( Collectors.joining( ";" ) );
      }

      private String getValueCell( final YES_NO_UNKNOWN value ) {
         final LocalDateTime date = _dateMap.get( value );
         if ( date == null ) {
            return "";
         }
         return value.name().toLowerCase()
               + "," + _episodeMap.getOrDefault( value, "" )
               + "," + date.format( _dateWriteFormatter );
      }
   }

   private class CategoryInfo {
      final private Map<String, LocalDateTime> _dateMap = new HashMap<>();
      final private Map<String, String> _episodeMap = new HashMap<>();

      private void update( final LocalDateTime docDate, final String episode, final Collection<String> values ) {
         values.forEach( h -> update( docDate, episode, h ) );
      }

      private void update( final LocalDateTime docDate, final String episode, final String value ) {
         final LocalDateTime statusDate = _dateMap.get( value );
         if ( statusDate == null || docDate.isBefore( statusDate ) ) {
            _dateMap.put( value, docDate );
            _episodeMap.put( value, episode );
         }
      }

      private boolean hasInfo() {
         return !_dateMap.isEmpty();
      }

      private String getCell() {
         return _dateMap.keySet()
                        .stream()
                        .sorted()
                        .map( this::getValueCell )
                        .collect( Collectors.joining( ";" ) );
      }

      private String getValueCell( final String value ) {
         final LocalDateTime date = _dateMap.get( value );
         if ( date == null ) {
            return "";
         }
         return value
               + "," + _episodeMap.getOrDefault( value, "" )
               + "," + date.format( _dateWriteFormatter );
      }
   }


   private class SmokerInfo {
      final private Map<SMOKING_STATUS, LocalDateTime> _dateMap = new EnumMap<>( SMOKING_STATUS.class );
      final private Map<SMOKING_STATUS, String> _episodeMap = new EnumMap<>( SMOKING_STATUS.class );

      private void update( final LocalDateTime docDate, final String episode, final SMOKING_STATUS status ) {
         final LocalDateTime statusDate = _dateMap.get( status );
         if ( statusDate == null || docDate.isBefore( statusDate ) ) {
            _dateMap.put( status, docDate );
            _episodeMap.put( status, episode );
         }
      }

      private String getCell() {
         return Arrays.stream( SMOKING_STATUS.values() )
                      .map( this::getValueCell )
                      .filter( c -> !c.isEmpty() )
                      .collect( Collectors.joining( ";" ) );
      }

      private String getValueCell( final SMOKING_STATUS value ) {
         final LocalDateTime date = _dateMap.get( value );
         if ( date == null ) {
            return "";
         }
         return value.name().toLowerCase()
               + "," + _episodeMap.getOrDefault( value, "" )
               + "," + date.format( _dateWriteFormatter );
      }
   }


}