package org.healthnlp.deepphe.neo4j.constant;

import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.MultipleFoundException;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.NAME_KEY;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/3/2018
 */
final public class UriConstants {

   private UriConstants() {
   }

   static public final String THING = "Thing";

   static public final String MEDICAL_RECORD = "MedicalRecord";
   static public final String PATIENT = "Patient";


   //
   //          ANATOMY              Body Part, Organ, or Organ Component
   //

   static public final String ORGAN = "Organ";

   //
   //          ANATOMY              Body Location or Region
   //

   static public final String BODY_CAVITY = "Body_Cavity";
   static public final String BODY_REGION = "Body_Region";
   static public final String ORGAN_SYSTEM = "Organ_System_Subdivision";
   static public final String ANATOMICAL_SET = "Anatomical_Set";
   static public final String ORGAN_PART = "Cardinal_Organ_Part";
   static public final String ACQUIRED_BODY_STRUCTURE = "Acquired_Body_Structure";
   static public final String ANATOMY_GROUP = "Group_Of_Anatomical_Entities";
   static public final String VARIANT_ANATOMY = "Variant_Anatomical_Structure";

   //          ANATOMY              Remove from Locations as they may be in anatomy trees

   static public final String BODY_TISSUE = "Body_Tissue";
   static public final String TISSUE = "Tissue";

   static public final String BODY_FLUID = "Body_Fluid";
   static public final String BODY_FLUID_XN = "BodyFluidOrSubstance";

   static public final String BODY_MISC = "Body_Miscellaneous";
   static private final String CELL = "Entire_Cell";


   //
   //          DISEASE              Disease or Syndrome
   //

   static public final String DISEASE = "Disease";
   static public final String DISEASE_XN = "DiseaseOrDisorder";

   static public final String NEOPLASM = "Neoplasm";
   // Neoplasm should have everything we want re tumor, cancer.  Neo disease is diseases (named) that have neoplasms.

   // The following refinements are under Neoplasm by Special Category, which is under Neoplasm
   static public final String BENIGN_NEOPLASM = "Benign_Neoplasm";
   static public final String MALIGNANT_NEOPLASM = "Malignant_Neoplasm";
   static public final String PRIMARY_NEOPLASM = "Primary_Neoplasm";
   static public final String METASTATIC_NEOPLASM = "Secondary_Neoplasm";
   static public final String METASTASIS = "Metastasis";

   static public final String IN_SITU_NEOPLASM = "In_Situ_Neoplasm";
   static public final String RECURRENT_TUMOR = "Recurrent_Tumor";


   //
   //          FINDING        Sign/Symptom
   //

   static public final String SIGN_SYMPTOM = "Finding";
   //   static public final String SIGN_SYMPTOM = "Finding";
   static public final String PHENOTYPIC_ABNORMALITY = "Phenotypic_Abnormality";

   static public final String LESION = "Lesion";
   // Mass is subclass of Lesion.  Lesion has way more than mass, e.g. ulcer.
   static public final String MASS = "Mass";

   // Maybe migrate to Qualitative entity
   static public final String TNM = "TNM_Stage";
   static public final String C_TNM = "Generic_TNM";
   static public final String P_TNM = "Pathologic_TNM";

   static public final String BIOMARKER = "Tumor_Biomarkers";

   static public final String TEST_RESULT = "ClinicalTestResult";
   //Maybe migrate to Lab Result
   static public final String RECEPTOR_STATUS = "Receptor_Status";
   static public final String ER_STATUS = "Estrogen_Receptor_Status";
   // #Estrogen_Receptor_Positive , _Negative, _Status_Unknown
   static public final String PR_STATUS = "Progesterone_Receptor_Status";
   // #Progesterone_Receptor_Negative , _Positive , _Status_Unknown
   static public final String HER2_STATUS = "HER2_Neu_Status";
   // #HER2_Neu_Negative , _Positive , _Status_Unknown
   static public final String TRIPLE_NEGATIVE = "Triple_Negative_Breast_Cancer_Finding";

   // Maybe migrate to qualitative entity
   static public final String LATERALITY = "Laterality";
   static public final String LATERALITY_XN = "Side";

   //          FINDING        Body Substance

   static public final String SPECIMEN = "Specimen";
   static public final String ABNORMAL_CELL = "Abnormal_Cell";

   //          FINDING        Gene or Genome

   static public final String GENE = "Gene";
   static public final String MOLECULAR_ABNORMALITY = "Molecular Abnormality";


   //
   //          MEDICATION        Pharmacologic Substance
   //

   static public final String MEDICATION = "Pharmacologic_Substance";
   static public final String MEDICATION_XN = "PharmacologicSubstance";


   //
   //          PROCEDURE         Therapeutic or Interventional Procedure
   //

   static public final String DIAGNOSTIC_TEST = "Diagnostic_Procedure";
   static public final String PROCEDURE = "Interventional_Procedure";
   static public final String PROCEDURE_XN = "InterventionOrProcedure";
   static public final String CHEMOTHERAPY_REGIMEN = "Chemotherapy_Regimen";
   static public final String REGIMEN_XN = "Chemo_sl_immuno_sl_hormoneTherapyRegimen";
   static public final String AGENT_COMBINATION = "Agent_Combination";


   //
   //          DEVICE        Medical Device
   //

   static public final String DEVICE = "Device";


   //
   //          LAB_MODIFIER        Quantitative Concept
   //

   static public final String CLINICAL_MODIFIER = "Clinical_Modifier";
   static public final String GENERAL_QUALIFIER = "General_Qualifier";


   //
   //          ENTITY        Entity
   //

   static public final String IMMATERIAL_ANATOMIC_ENTITY = "Immaterial_Anatomical_Entity";


   /////////////////////////////////////////////////////////////////////////////////////
   //
   //                         VALUES
   //
   /////////////////////////////////////////////////////////////////////////////////////

   // Semantic Type
//   static public final String POSITIVE = "Positive";      May want to add synonyms
//   static public final String NEGATIVE = "Negative";         With Pref Text = 50
   static public final String POSITIVE = "Positive_Measurement_Finding";
   static public final String NEGATIVE = "Negative_Measurement_Finding";
   static public final String UNEQUIVOCAL = "Unequivocal";
   static public final String TRUE = "True";
   static public final String FALSE = "False";


   static public final String UNKNOWN = "Unknown";


//
   // TODO  LOST Adenocarcinomas.   Lost from mass? malignant? Cancer container?  Diagnosis problem?


   // For histology, can traverse down from each child of Neoplasm by Histology.  The child will be the type.


   static public boolean isUri( final String uri, Collection<String> toMatch ) {
      return toMatch.stream().anyMatch( uri::equals );
   }

   static public boolean isUri( final String uri, final String... toMatch ) {
      return Arrays.stream( toMatch ).anyMatch( uri::equals );
   }





   ////  Subclasses of "General Qualifier"
   // Semantic Type "Idea or Concept"
   // Filler Cui Constants
   static public final String CUI_UNKNOWN = "C0000000";
   static public final String CUI_TIME = "C0000001";
   static public final String CUI_EVENT = "C0000002";


   // Children of #Unit_of_Length
//   static public final String MILLIMETER = DPHE_ROOT_HASH + "Millimeter";
//   static public final String CENTIMETER = DPHE_ROOT_HASH + "Centimeter";


   ////  Subclass of "Disease_Disorder_or_Finding"
   // Semantic Type "Neoplastic Process"
   static public final String BREAST_NEOPLASM = "Breast_Neoplasm";
   static public final String SKIN_NEOPLASM = "Skin_Neoplasm";
   // Semantic Type "Neoplastic Process"

   static public final String STAGE = "Tumor_Stage_Finding";
   static public final String STAGE_UNKNOWN = "Stage_Unknown";

   static public final String GRADE = "Finding_Of_Grade";


   static public final String LYMPH_NODE = "Lymph_Node";

   // Semantic Type "Body Part, Organ, or Organ Component".  Parent of "Left_Breast", "Right_Breast".
   static public final String BREAST = "Breast";
   // Parent of quadrants "#Central_Portion_of_the_Breast" , "#Lower_Inner_Quadrant_of_the_Breast", etc. also "#Nipple"
//   static public final String QUADRANT = "Quadrant_of_Breast";
   static public final String QUADRANT = "Breast_Quadrant";
   static public final String BREAST_PART = "Breast_Part";
   // Semantic Type "Intellectual Product".  Has no children ... either dPhe import or use time annotation or something.
   static public final String CLOCKFACE = "Clockface_Position";

   // Semantic Type "Anatomical Structure".  Parent of many Skin classes.

   //// Subclass of "Drug_Food_Chemical_or_Biomedical_Material"
   // Semantic Type "Pharmacological Substance"

   //// Subclass of "Conceptual_Entity"
   // Semantic Type "Event"
   static public final String EVENT = "Event";

   //// Subclass of "Biological_Process" and later "Tumor_Associated_Process"
   // Semantic Type "Pathologic Function"

   //// Subclass of "Disease_Morphology_Qualifier"
   // Semantic Type "Qualitative Concept"
   static public final String MALIGNANT = "Malignant";
   // #Benign, #In_Situ, #Invasive, etc.

   //// Subclass of "Property_or_Attribute"
   // Semantic Type "Lab Modifier" - why not quantitative concept ?
   static public final String QUANTITY = "Quantity";

   //// Subclass of "Property_or_Attribute"
   // Semantic Type "Quantitative Concept"
   static public final String SIZE = "Tumor_Size";

   //// Property or attribute
   //// Bilateral and Left and Right are all under class "Qualifier" which is  Semantic Type "Conceptual Entity"
   //// Under "Anatomy_Qualifier"
   // Semantic Type "Spatial Concept"
   static public final String BILATERAL = "Bilateral";
   //// Spatial_Qualifier
   // Semantic Type "Spatial Concept".    #Right is the same.
   static public final String LEFT = "Left";
   static public final String RIGHT = "Right";

   static public final String NO_LATERAL = "No_Laterality";

   // Semantic Type "Organism Attribute"
//   static public final String LATERALITY = DPHE_ROOT_HASH + "Laterality";


//   static private final Collection<String> MASS_URIS = new HashSet<>();
//
//   static public Collection<String> getMassUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return MASS_URIS;
//   }
//
//   static private final Collection<String> CANCER_URIS = new HashSet<>();
//
//   static public Collection<String> getCancerUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return CANCER_URIS;
//   }

   static private final Collection<String> MASS_NEOPLASMS = new HashSet<>();

//   static public Collection<String> getMassNeoplasmUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return MASS_NEOPLASMS;
//   }

//   static private final Collection<String> PRIMARY_URIS = new HashSet<>();
//   static public Collection<String> getPrimaryUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return PRIMARY_URIS;
//   }

   static private final Collection<String> UNKOWN_PRIMARY_URIS = new HashSet<>();
//   static public Collection<String> getUnknownPrimaryUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return PRIMARY_URIS;
//   }

   static private final Collection<String> MALIGNANT_URIS = new HashSet<>();
//   static public Collection<String> getMalignantTumorUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return MALIGNANT_URIS;
//   }


//   static private final Collection<String> METASTASIS_URIS = new HashSet<>();
//
//   static public Collection<String> getMetastasisUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return METASTASIS_URIS;
//   }
//
//   static private final Collection<String> BENIGN_URIS = new HashSet<>();
//   static public Collection<String> getBenignTumorUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return BENIGN_URIS;
//   }

   static private final Collection<String> GENERIC_URIS = new HashSet<>();

//   static public Collection<String> getGenericUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return GENERIC_URIS;
//   }

//   static private final Collection<String> LOCATION_URIS = new HashSet<>();
//
//   static public Collection<String> getLocationUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return LOCATION_URIS;
//   }

//   static private final Collection<String> POSITIVE_VALUE_URIS = new HashSet<>();
//
//   static public Collection<String> getPositiveValueUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return POSITIVE_VALUE_URIS;
//   }

//   static private final Collection<String> REGULAR_VALUE_URIS = new HashSet<>();
//
//   static public Collection<String> getRegaultValueUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return REGULAR_VALUE_URIS;
//   }

//   static private final Collection<String> NORMAL_VALUE_URIS = new HashSet<>();
//
//   static public Collection<String> getNormalValueUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return NORMAL_VALUE_URIS;
//   }

//   static private final Collection<String> STABLE_VALUE_URIS = new HashSet<>();
//
//   static public Collection<String> getStableValueUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return STABLE_VALUE_URIS;
//   }

//   static private final Collection<String> HIGH_VALUE_URIS = new HashSet<>();
//
//   static public Collection<String> getHighValueUris( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return HIGH_VALUE_URIS;
//   }

//   static private final Map<String, Collection<String>> HISTOLOGY_MAP = new HashMap<>();
//
//   static public Map<String, Collection<String>> getHistologyMap( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return HISTOLOGY_MAP;
//   }

//   static private final Map<String, Collection<String>> CANCER_TYPE_MAP = new HashMap<>();
//   static public Map<String, Collection<String>> getCancerTypeMap( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return CANCER_TYPE_MAP;
//   }

   static private final Map<String, String> DIAGNOSIS_GROUP_NAMES = new HashMap<>();
   static public Map<String, String> getDiagnosisGroupNames( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return DIAGNOSIS_GROUP_NAMES;
   }

//   static public String getDiagnosisGroupName( final GraphDatabaseService graphDb, final String uri ) {
//      final Map<String, String> map = getDiagnosisGroupNames( graphDb );
//      final String category = map.get( uri );
//      if ( category != null && !category.isEmpty() ) {
//         return category;
//      }
//      return "Unknown";
//   }


   static private final Collection<String> CANCER_STAGES = new ArrayList<>();

//   static public Collection<String> getCancerStages( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return CANCER_STAGES;
//   }

   static private final Collection<String> CANCER_GRADES = new ArrayList<>();

//   static public Collection<String> getCancerGrades( final GraphDatabaseService graphDb ) {
//      initializeUris( graphDb );
//      return CANCER_GRADES;
//   }


   static private final Object URI_LOCK = new Object();

   static private void initializeUris( final GraphDatabaseService graphDb ) {
      synchronized ( URI_LOCK ) {
//         if ( MASS_URIS.isEmpty() ) {
//
//            CANCER_URIS.addAll( SearchUtil.getBranchUris( graphDb, NEOPLASM ) );
//            MASS_URIS.addAll( SearchUtil.getBranchUris( graphDb, MASS ) );
//            // A lot of cancers are branched from mass.  We want to keep those as cancers.
//            final Collection<String> malignancyUris = SearchUtil.getBranchUris( graphDb, "Malignant_Neoplasm" );
//            MASS_URIS.removeAll( malignancyUris );
//            // Collect individual (not branch) uris under cancer that are called "Tumor" and "Mass".
//            final Collection<String> namedTumorUris
//                  = CANCER_URIS.stream()
//                               .filter( u -> !u.contains( "Malignant" )
//                                             && !u.contains( "adeno" )
//                                             && !u.contains( "Carc" ) )
//                               .filter( u -> u.contains( "Tumor" )
//                                             || u.contains( "Mass" )
//                                             || u.contains( "Cyst" ) )
//                               .collect( Collectors.toSet() );
//            MASS_URIS.addAll( namedTumorUris );
//            // Get rid of neoplastic cell.
//            final Collection<String> neoplasticCell = SearchUtil.getBranchUris( graphDb, "Neoplastic_Cell" );
//            MASS_URIS.removeAll( neoplasticCell );
//            CANCER_URIS.removeAll( neoplasticCell );
//            // Here is another trick:  The root node NEOPLASM should also be considered a mass.
//            MASS_URIS.add( NEOPLASM );
//            // We don't want mentions of metastasis to be seen as [primary] cancers.
//            MASS_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Metastasis" ) );
//            // Separate Mass from Cancer
//            CANCER_URIS.removeAll( MASS_URIS );
//
//            MASS_NEOPLASMS.addAll( MASS_URIS );
//            MASS_NEOPLASMS.addAll( CANCER_URIS );

//            MALIGNANT_URIS.addAll( SearchUtil.getBranchUris( graphDb, MALIGNANT_NEOPLASM ) );
//            BENIGN_URIS.addAll( SearchUtil.getBranchUris( graphDb, BENIGN_NEOPLASM ) );
//            PRIMARY_URIS.addAll( SearchUtil.getBranchUris( graphDb, PRIMARY_NEOPLASM ) );
//            PRIMARY_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, MASS, "Disease_Has_Finding", "Primary_Lesion" ) );
//            PRIMARY_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, NEOPLASM, "Disease_Has_Finding", "Primary_Lesion" ) );
//
//            METASTASIS_URIS.addAll( SearchUtil.getBranchUris( graphDb, METASTATIC_NEOPLASM ) );
//            METASTASIS_URIS.addAll( SearchUtil.getBranchUris( graphDb, METASTASIS ) );
//            METASTASIS_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, MASS, "Disease_Has_Finding", "Secondary_Lesion" ) );
//            METASTASIS_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, MASS, "Disease_Has_Finding", "Metastatic_Lesion" ) );
//            METASTASIS_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, NEOPLASM, "Disease_Has_Finding", "Secondary_Lesion" ) );
//            METASTASIS_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, NEOPLASM, "Disease_Has_Finding", "Metastatic_Lesion" ) );
//            PRIMARY_URIS.removeAll( METASTASIS_URIS );
//
//            GENERIC_URIS.addAll( CANCER_URIS );
//            GENERIC_URIS.removeAll( MALIGNANT_URIS );
//            GENERIC_URIS.removeAll( BENIGN_URIS );
//            GENERIC_URIS.removeAll( PRIMARY_URIS );
//            GENERIC_URIS.removeAll( METASTASIS_URIS );
//
//            UNKOWN_PRIMARY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Neoplasms__Unknown_Primary" ) );
//
//            CANCER_TYPE_MAP.put( "Carcinoma", SearchUtil.getBranchUris( graphDb, "Carcinoma" ) );
//            CANCER_TYPE_MAP.put( "Sarcoma", SearchUtil.getBranchUris( graphDb, "Sarcoma" ) );
//            CANCER_TYPE_MAP
//                  .put( "Plasma_Cell_Myeloma", SearchUtil.getBranchUris( graphDb, "Plasma_Cell_Myeloma" ) );
//            CANCER_TYPE_MAP
//                  .put( "Melanoma", SearchUtil.getBranchUris( graphDb, "Melanocytic_Neoplasm" ) );
//            CANCER_TYPE_MAP.put( "Leukemia", SearchUtil.getBranchUris( graphDb, "Leukemia" ) );
//            CANCER_TYPE_MAP.put( "Lymphoma", SearchUtil.getBranchUris( graphDb, "Lymphoma" ) );

            // 3/30/2023 completely ignoring these histology uris.
            // 9 "Histologic Types" are new to ontology
            // BrCa overspecification
//            HISTOLOGY_MAP.put( "Ductal", SearchUtil.getBranchUris( graphDb, "Ductal_Breast_Carcinoma" ) );
//            HISTOLOGY_MAP.put( "Lobular", SearchUtil.getBranchUris( graphDb, "Lobular_Breast_Carcinoma" ) );
//            HISTOLOGY_MAP.put( "Mucinous", SearchUtil.getBranchUris( graphDb, "Mucinous_Neoplasm" ) );
//            HISTOLOGY_MAP.put( "Papillary", SearchUtil.getBranchUris( graphDb, "Papillary_Breast_Carcinoma" ) );
//            HISTOLOGY_MAP.put( "Tubular", SearchUtil.getBranchUris( graphDb, "Tubular_Breast_Carcinoma" ) );
//            // Ovary overspecification
//            HISTOLOGY_MAP.put( "Borderline", SearchUtil
//                  .getBranchUris( graphDb, "Borderline_Ovarian_Epithelial_Tumor" ) );
//            HISTOLOGY_MAP.put( "Brenner_Tumor", SearchUtil.getBranchUris( graphDb, "Brenner_Tumor" ) );
//            HISTOLOGY_MAP.put( "Carcinosarcoma", SearchUtil.getBranchUris( graphDb, "Carcinosarcoma" ) );
//            HISTOLOGY_MAP.put( "Clear_Cell_Sarcoma", SearchUtil
//                  .getBranchUris( graphDb, "Clear_Cell_Neoplasm" ) );
//            HISTOLOGY_MAP.put( "Dysgerminoma", SearchUtil.getBranchUris( graphDb, "Dysgerminoma" ) );
//            HISTOLOGY_MAP.put( "Endometrioid", SearchUtil
//                  .getBranchUris( graphDb, "Ovarian_Endometrioid_Adenocarcinoma" ) );
//            HISTOLOGY_MAP.put( "Epithelial_Stromal", SearchUtil
//                  .getBranchUris( graphDb, "Epithelial_Neoplasm" ) );
//            HISTOLOGY_MAP.put( "Granulosa_Cell", SearchUtil.getBranchUris( graphDb, "Granulosa_Cell" ) );
//            HISTOLOGY_MAP.put( "Immature_Teratoma", SearchUtil.getBranchUris( graphDb, "Immature_Teratoma" ) );
//            HISTOLOGY_MAP.put( "Leiomyosarcoma", SearchUtil.getBranchUris( graphDb, "Leiomyosarcoma" ) );
//            HISTOLOGY_MAP.put( "Mixed_Mesodermal_Mullerian_Tumor", SearchUtil
//                  .getBranchUris( graphDb, "Mixed_Mesodermal_Mullerian_Tumor" ) );
//
//            HISTOLOGY_MAP.put( "Papillary_Serous", SearchUtil
//                  .getBranchUris( graphDb, "Ovarian_Serous_Surface_Papillary_Adenocarcinoma" ) );
//            HISTOLOGY_MAP
//                  .put( "Serous", SearchUtil.getBranchUris( graphDb, "Serous_Neoplasm" ) );
//            HISTOLOGY_MAP.put( "Sertoli_Leydig", SearchUtil
//                  .getBranchUris( graphDb, "Ovarian_Sertoli_Leydig_Cell_Tumor" ) );
//            HISTOLOGY_MAP
//                  .put( "Squamous_Cell", SearchUtil.getBranchUris( graphDb, "Squamous_Cell_Carcinoma" ) );
//            HISTOLOGY_MAP.put( "Undifferentiated", SearchUtil
//                  .getBranchUris( graphDb, "Undifferentiated_Ovarian_Carcinoma" ) );
//            HISTOLOGY_MAP.put( "Yolk_Sac", SearchUtil.getBranchUris( graphDb, "Yolk_Sac_Tumor" ) );

//            DIAGNOSIS_GROUP_NAMES
//            final Collection<String> sites = getChildren( graphDb, "Neoplasm_by_Site" );
//            final Collection<String> morphs = getChildren( graphDb,"Neoplasm_by_Morphology" );
//            final Collection<String> masses = getChildren( graphDb, MASS );

//            for ( String mass : masses ) {
//               final String groupText = SearchUtil.getPreferredText( graphDb, mass );
//               final Collection<String> branch = SearchUtil.getBranchUris( graphDb, mass );
//               for ( String node : branch ) {
//                  final String prefText = SearchUtil.getPreferredText( graphDb, node );
//                  DIAGNOSIS_GROUP_NAMES.put( prefText, groupText );
//               }
//               DIAGNOSIS_GROUP_NAMES.put( MASS, MASS );
//            }
//            for ( String morph : morphs ) {
//               final String groupText = SearchUtil.getPreferredText( graphDb, morph );
//               final Collection<String> branch = SearchUtil.getBranchUris( graphDb, morph );
//               for ( String node : branch ) {
//                  final String prefText = SearchUtil.getPreferredText( graphDb, node );
//                  DIAGNOSIS_GROUP_NAMES.put( prefText, groupText );
//               }
//            }
//            for ( String site : sites ) {
//               final String groupText = SearchUtil.getPreferredText( graphDb, site );
//               final Collection<String> branch = SearchUtil.getBranchUris( graphDb, site );
//               for ( String node : branch ) {
//                  final String prefText = SearchUtil.getPreferredText( graphDb, node );
//                  DIAGNOSIS_GROUP_NAMES.put( prefText, groupText );
//               }
//            }

//            SearchUtil.getBranchUris( graphDb, STAGE ).stream()
//                      .filter( u -> u.length() < 12 )
//                      .forEach( CANCER_STAGES::add );
//
//            SearchUtil.getBranchUris( graphDb, GRADE ).stream()
////                      .filter( u -> u.length() < 12 )
//                      .filter( u -> u.length() < 26 )
//                      .forEach( CANCER_GRADES::add );

//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, ORGAN ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, BODY_REGION ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, BODY_CAVITY ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, ORGAN_SYSTEM ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, ANATOMICAL_SET ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, ORGAN_PART ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, ACQUIRED_BODY_STRUCTURE ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, ANATOMY_GROUP ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, VARIANT_ANATOMY ) );
//            // Skin part is where exocrine gland is removed so remove it first.
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Skin_Part" ) );
//            // Some Glands (e.g. prostate) are exocrine, under cutaneous, under skin part.  Not quite right.
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Exocrine_Gland" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, BODY_TISSUE ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, TISSUE ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, BODY_FLUID ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, BODY_MISC ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, CELL ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Anatomic_Border" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Wall_Of_Colon" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Reticuloendothelial_Cell" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Bone_Cell" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Bone_Marrow" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Aponeurosis" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Cranial_Epidural_Space" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Ligament" ) );
//            // Broad and Round Ligament are part of Genitalia and wanted for C57
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Broad_Ligament" ) );
//            LOCATION_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Round_Ligament" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Cartilage" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Muscle" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Mucous_Membrane" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Serous_Membrane" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Synovial_Bursa" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Labyrinth_Supporting_Cells" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, LYMPH_NODE ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Lymphatic_Vessel" ) );
//            LOCATION_URIS.removeAll( SearchUtil.getBranchUris( graphDb, "Occipital_Segment_Of_Fusiform_Gyrus" ) );
//            final Collection<String> locationRemovals = LOCATION_URIS.stream()
//                                                                     .filter( u -> u.toLowerCase().contains( "tissue" ) )
//                                                                     .collect( Collectors.toSet() );
//            LOCATION_URIS.removeAll( locationRemovals );
//
//            POSITIVE_VALUE_URIS.add( "Positive" );
//            POSITIVE_VALUE_URIS.add( "Negative" );
//            POSITIVE_VALUE_URIS.add( "Unknown" );
//            POSITIVE_VALUE_URIS.add( "Indeterminate" );
//            POSITIVE_VALUE_URIS.add( "Equivocal" );
//
//
//            REGULAR_VALUE_URIS.add( "Regular" );
//            REGULAR_VALUE_URIS.add( "Irregular" );
//
//            NORMAL_VALUE_URIS.add( "Normal" );
//            NORMAL_VALUE_URIS.add( "Abnormal" );
//
//            STABLE_VALUE_URIS.add( "Stable" );
//            STABLE_VALUE_URIS.add( "Unstable" );
//
//            HIGH_VALUE_URIS.add( "High" );
//            HIGH_VALUE_URIS.add( "Low" );
//            HIGH_VALUE_URIS.add( "Elevated" );

//         }
      }
   }

   static private Collection<String> getChildren( final GraphDatabaseService graphDb, final String uri ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         Collection<String> children = SearchUtil.getChildClassNodes( graphDb, uri )
                                                 .stream()
                                                 .map( n -> n.getProperty( NAME_KEY ) )
                                                 .filter( Objects::nonNull )
                                                 .map( Object::toString )
                                                 .collect( Collectors.toList() );
         tx.success();
         return children;
      } catch ( MultipleFoundException mfE ) {
         System.out.println( mfE.getMessage() );
      }
      return Collections.emptyList();
   }

}
