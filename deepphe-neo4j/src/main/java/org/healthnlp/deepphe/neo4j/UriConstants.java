package org.healthnlp.deepphe.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/3/2018
 */
final public class UriConstants {

   private UriConstants() {
   }


   static public boolean isUri( final String uri, Collection<String> toMatch ) {
      return toMatch.stream().anyMatch( uri::equals );
   }

   static public boolean isUri( final String uri, final String... toMatch ) {
      return Arrays.stream( toMatch ).anyMatch( uri::equals );
   }

   static public final String DPHE_SCHEME = "DPHE_URI";
   static public final String HEALTH_NLP_URL = "http://cancer.healthnlp.org/";

   static public final String OWL_ROOT = "http://www.w3.org/2002/07/owl";
   static public final String DPHE_ROOT = HEALTH_NLP_URL + "DeepPhe.owl";
   static public final String DPHE_ROOT_HASH = "";

   static public final String THING = "Thing";

   static public final String OWL_THING = THING;

   static public final String MEDICAL_RECORD = DPHE_ROOT_HASH + "MedicalRecord";
   static public final String PATIENT = DPHE_ROOT_HASH + "Patient";


   ////  Subclasses of "General Qualifier"
   // Semantic Type "Idea or Concept"
   static public final String UNKNOWN = DPHE_ROOT_HASH + "Unknown";
   // Filler Cui Constants
   static public final String CUI_UNKNOWN = "C0000000";
   static public final String CUI_TIME = "C0000001";
   static public final String CUI_EVENT = "C0000002";

   // Semantic Type "Qualitative Concept"
   static public final String POSITIVE = DPHE_ROOT_HASH + "Positive";
   static public final String NEGATIVE = DPHE_ROOT_HASH + "Negative";
   static public final String UNEQUIVOCAL = DPHE_ROOT_HASH + "Unequivocal";
   // Semantic Type "Conceptual Entity"
   static public final String TRUE = "True";
   static public final String FALSE = "False";

   // Children of #Unit_of_Length
   static public final String MILLIMETER = DPHE_ROOT_HASH + "Millimeter";
   static public final String CENTIMETER = DPHE_ROOT_HASH + "Centimeter";


   ////  Subclass of "Disease_Disorder_or_Finding"
   // Semantic Type "Neoplastic Process"
   static public final String NEOPLASM = DPHE_ROOT_HASH + "Neoplasm";
   static public final String BREAST_NEOPLASM = DPHE_ROOT_HASH + "Breast_Neoplasm";
   static public final String SKIN_NEOPLASM = DPHE_ROOT_HASH + "Skin_Neoplasm";
   static public final String MALIGNANT_NEOPLASM = DPHE_ROOT_HASH + "Malignant_Neoplasm";
   // Semantic Type "Neoplastic Process"

   static public final String LESION = DPHE_ROOT_HASH + "Lesion";
   static public final String MASS = DPHE_ROOT_HASH + "Mass";
   // Tumor is now Tumor_Mass, under Mass
   static public final String TNM_FINDING = DPHE_ROOT_HASH + "Cancer_TNM_Finding";
   // Only Generic and Pathologic are split into actual scores.  May need old dphe addition.
   static public final String TNM = DPHE_ROOT_HASH + "Cancer_TNM_Finding";
   static public final String C_TNM = DPHE_ROOT_HASH + "Clinical_TNM_Finding";
   static public final String P_TNM = DPHE_ROOT_HASH + "Pathologic_TNM_Finding";
   // Semantic Type "Laboratory or Test Result"
   static public final String RECEPTOR_STATUS = DPHE_ROOT_HASH + "Receptor_Status";
   static public final String ER_STATUS = DPHE_ROOT_HASH + "Estrogen_Receptor_Status";
   // #Estrogen_Receptor_Positive , _Negative, _Status_Unknown
   static public final String PR_STATUS = DPHE_ROOT_HASH + "Progesterone_Receptor_Status";
   // #Progesterone_Receptor_Negative , _Positive , _Status_Unknown
   static public final String HER2_STATUS = DPHE_ROOT_HASH + "HER2_Neu_Status";
   // #HER2_Neu_Negative , _Positive , _Status_Unknown

   // Semantic Type "Finding"
   static public final String TRIPLE_NEGATIVE = DPHE_ROOT_HASH + "Triple_Negative_Breast_Cancer_Finding";

   //  Cancer Stage is (antiquated) in ncit and a time.  Need to find the parent of "Stage_IIA"
   static public final String STAGE = DPHE_ROOT_HASH + "Cancer_Stage";
   static public final String STAGE_UNKNOWN = DPHE_ROOT_HASH + "Stage_Unknown";

   // main branch roots
   static public final String ANATOMY = DPHE_ROOT_HASH + "Anatomic_Structure_System_or_Substance";
   static public final String[] ANATOMIES = {
         "Organ",
         "Body_Part",
         "Body_Region",
         "Abdomen",
         "Pelvis"
   };
   static public final String REGIMEN = DPHE_ROOT_HASH + "Chemotherapy_Regimen_or_Agent_Combination";
   static public final String HEALTH_ACTIVITY = DPHE_ROOT_HASH + "Healthcare_Activity";
   static public final String PROCEDURE = DPHE_ROOT_HASH + "Intervention_or_Procedure";
   static public final String DISEASE = DPHE_ROOT_HASH + "Disease_or_Disorder";
   static public final String FINDING = DPHE_ROOT_HASH + "Finding";
   static public final String DRUG = DPHE_ROOT_HASH + "Drug_Food_Chemical_or_Biomedical_Material";


   static public final String LYMPH_NODE = DPHE_ROOT_HASH + "Lymph_Node";

   // Semantic Type "Body Part, Organ, or Organ Component".  Parent of "Left_Breast", "Right_Breast".
   static public final String BREAST = DPHE_ROOT_HASH + "Breast";
   // Parent of quadrants "#Central_Portion_of_the_Breast" , "#Lower_Inner_Quadrant_of_the_Breast", etc. also "#Nipple"
   static public final String QUADRANT = DPHE_ROOT_HASH + "Quadrant";
   static public final String BREAST_PART = DPHE_ROOT_HASH + "Breast_Part";
   // Semantic Type "Intellectual Product".  Has no children ... either dPhe import or use time annotation or something.
   static public final String CLOCKFACE = DPHE_ROOT_HASH + "Clockface_Position";

   // Semantic Type "Anatomical Structure".  Parent of many Skin classes.
   static public final String SKIN = DPHE_ROOT_HASH + "Skin";
   // Semantic Type "Tissue".  Parent of many tissue classes.
   static public final String TISSUE = DPHE_ROOT_HASH + "Tissue";

   //// Subclass of "Drug_Food_Chemical_or_Biomedical_Material"
   // Semantic Type "Pharmacological Substance"

   // Semantic Type "Diagnostic Procedure"
   static public final String TEST = DPHE_ROOT_HASH + "Diagnostic_Procedure";
   // Semantic Type "Activity", subclass of "Diagnostic_Procedure"
   static public final String OBSERVATION = DPHE_ROOT_HASH + "Vital_Signs_Measurement";
   // Some of "characteristic" is not wanted, looks like research /bench lab stuff (Speed, Precision, etc.)
   // Otherwise should be used. as it has height, weight, gait ...
   static public final String OBSERVATION_2 = DPHE_ROOT_HASH + "Characteristic";

   // under Property_or_Attribute
   static public final String CONDITION = DPHE_ROOT_HASH + "Condition";

   //// Subclass of "Conceptual_Entity"
   // Semantic Type "Event"
   static public final String EVENT = DPHE_ROOT_HASH + "Event";

   //// Subclass of "Biological_Process" and later "Tumor_Associated_Process"
   // Semantic Type "Pathologic Function"
   static public final String METASTASIS = DPHE_ROOT_HASH + "Secondary_Neoplasm";

   static public final String BODY_MODIFIER = DPHE_ROOT_HASH + "Body_Modifier";


   //// Subclass of "Disease_Morphology_Qualifier"
   // Semantic Type "Qualitative Concept"
   static public final String MALIGNANT = DPHE_ROOT_HASH + "Malignant";
   // #Benign, #In_Situ, #Invasive, etc.

   //// Subclass of "Property_or_Attribute"
   // Semantic Type "Lab Modifier" - why not quantitative concept ?
   static public final String QUANTITY = DPHE_ROOT_HASH + "Quantity";

   //// Subclass of "Property_or_Attribute"
   // Semantic Type "Quantitative Concept"
   static public final String SIZE = DPHE_ROOT_HASH + "Tumor_Size";

   //// Property or attribute
   //// Bilateral and Left and Right are all under class "Qualifier" which is  Semantic Type "Conceptual Entity"
   //// Under "Anatomy_Qualifier"
   // Semantic Type "Spatial Concept"
   static public final String BILATERAL = DPHE_ROOT_HASH + "Bilateral";
   //// Spatial_Qualifier
   // Semantic Type "Spatial Concept".    #Right is the same.
   static public final String LEFT = DPHE_ROOT_HASH + "Left";
   static public final String RIGHT = DPHE_ROOT_HASH + "Right";

   static public final String NO_LATERAL = DPHE_ROOT_HASH + "No_Laterality";

   // Semantic Type "Organism Attribute"
   static public final String LATERALITY = DPHE_ROOT_HASH + "Laterality";



   static private final Collection<String> CANCER_URIS = new HashSet<>();

   static public Collection<String> getCancerUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return CANCER_URIS;
   }

   static private final Collection<String> TUMOR_URIS = new HashSet<>();

   static public Collection<String> getTumorUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return TUMOR_URIS;
   }

   static private final Collection<String> METASTASIS_URIS = new HashSet<>();

   static public Collection<String> getMetastasisUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return METASTASIS_URIS;
   }

   static private final Collection<String> PRIMARY_URIS = new HashSet<>();

   static public Collection<String> getPrimaryUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return PRIMARY_URIS;
   }

   static private final Collection<String> GENERIC_TUMOR_URIS = new HashSet<>();

   static public Collection<String> getGenericTumorUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return GENERIC_TUMOR_URIS;
   }

   static private final Collection<String> ANATOMY_URIS = new HashSet<>();

   static public Collection<String> getAnatomyUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return ANATOMY_URIS;
   }

   static private final Collection<String> UNWANTED_ANATOMY_URIS = new HashSet<>();

   static public Collection<String> getUnwantedAnatomyUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return UNWANTED_ANATOMY_URIS;
   }

   static private final Collection<String> UNWANTED_TISSUE_URIS = new HashSet<>();

   static public Collection<String> getUnwantedTissueUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return UNWANTED_TISSUE_URIS;
   }

   static public final String HISTOLOGY = "Histologic_Type";
   static private final Map<String, Collection<String>> HISTOLOGY_MAP = new HashMap<>();

   static public Map<String, Collection<String>> getHistologyMap( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return HISTOLOGY_MAP;
   }

   static private final Map<String, Collection<String>> CANCER_TYPE_MAP = new HashMap<>();

   static public Map<String, Collection<String>> getCancerTypeMap( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return CANCER_TYPE_MAP;
   }


   static private final Object URI_LOCK = new Object();

   static private void initializeUris( final GraphDatabaseService graphDb ) {
      synchronized ( URI_LOCK ) {
         if ( CANCER_URIS.isEmpty() ) {

            PRIMARY_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, MASS, "Disease_Has_Finding", "Primary_Lesion" ) );
            PRIMARY_URIS.addAll( SearchUtil.getBranchUrisWithRelation( graphDb, NEOPLASM, "Disease_Has_Finding", "Primary_Lesion" ) );

            METASTASIS_URIS.addAll( SearchUtil.getBranchUris( graphDb, METASTASIS ) );
            PRIMARY_URIS.removeAll( METASTASIS_URIS );

            GENERIC_TUMOR_URIS.addAll( SearchUtil.getBranchUris( graphDb, MASS ) );
            GENERIC_TUMOR_URIS.addAll( SearchUtil.getBranchUris( graphDb, NEOPLASM ) );
            GENERIC_TUMOR_URIS.removeAll( PRIMARY_URIS );
            GENERIC_TUMOR_URIS.removeAll( METASTASIS_URIS );

            TUMOR_URIS.addAll( PRIMARY_URIS );
            TUMOR_URIS.addAll( METASTASIS_URIS );
            TUMOR_URIS.addAll( GENERIC_TUMOR_URIS );

            CANCER_URIS.addAll( SearchUtil.getBranchUris( graphDb, MALIGNANT_NEOPLASM ) );
            CANCER_URIS.removeAll( TUMOR_URIS );

            CANCER_TYPE_MAP.put( "Carcinoma", SearchUtil.getBranchUris( graphDb, "Carcinoma" ) );
            CANCER_TYPE_MAP.put( "Sarcoma", SearchUtil.getBranchUris( graphDb, "Sarcoma" ) );
            CANCER_TYPE_MAP
                  .put( "Plasma_Cell_Myeloma", SearchUtil.getBranchUris( graphDb, "Plasma_Cell_Myeloma" ) );
            CANCER_TYPE_MAP.put( "Leukemia", SearchUtil.getBranchUris( graphDb, "Leukemia" ) );
            CANCER_TYPE_MAP.put( "Lymphoma", SearchUtil.getBranchUris( graphDb, "Lymphoma" ) );

            // 9 "Histologic Types" are new to ontology
            // BrCa overspecification
            HISTOLOGY_MAP.put( "Ductal", SearchUtil.getBranchUris( graphDb, "Ductal_Breast_Carcinoma" ) );
            HISTOLOGY_MAP.put( "Lobular", SearchUtil.getBranchUris( graphDb, "Lobular_Breast_Carcinoma" ) );
            HISTOLOGY_MAP.put( "Mucinous", SearchUtil.getBranchUris( graphDb, "Mucinous_Neoplasm" ) );
            HISTOLOGY_MAP.put( "Papillary", SearchUtil.getBranchUris( graphDb, "Papillary_Breast_Carcinoma" ) );
            HISTOLOGY_MAP.put( "Tubular", SearchUtil.getBranchUris( graphDb, "Tubular_Breast_Carcinoma" ) );
            // Ovary overspecification
            HISTOLOGY_MAP.put( "Borderline", SearchUtil
                  .getBranchUris( graphDb, "Borderline_Ovarian_Epithelial_Tumor" ) );
            HISTOLOGY_MAP.put( "Brenner_Tumor", SearchUtil.getBranchUris( graphDb, "Brenner_Tumor" ) );
            HISTOLOGY_MAP.put( "Carcinosarcoma", SearchUtil.getBranchUris( graphDb, "Carcinosarcoma" ) );
            HISTOLOGY_MAP.put( "Clear_Cell_Sarcoma", SearchUtil
                  .getBranchUris( graphDb, "Clear_Cell_Neoplasm" ) );
            HISTOLOGY_MAP.put( "Dysgerminoma", SearchUtil.getBranchUris( graphDb, "Dysgerminoma" ) );
            HISTOLOGY_MAP.put( "Endometrioid", SearchUtil
                  .getBranchUris( graphDb, "Ovarian_Endometrioid_Adenocarcinoma" ) );
            HISTOLOGY_MAP.put( "Epithelial_Stromal", SearchUtil
                  .getBranchUris( graphDb, "Epithelial_Neoplasm" ) );
            HISTOLOGY_MAP.put( "Granulosa_Cell", SearchUtil.getBranchUris( graphDb, "Granulosa_Cell" ) );
            HISTOLOGY_MAP.put( "Immature_Teratoma", SearchUtil.getBranchUris( graphDb, "Immature_Teratoma" ) );
            HISTOLOGY_MAP.put( "Leiomyosarcoma", SearchUtil.getBranchUris( graphDb, "Leiomyosarcoma" ) );
            HISTOLOGY_MAP.put( "Mixed_Mesodermal_Mullerian_Tumor", SearchUtil
                  .getBranchUris( graphDb, "Mixed_Mesodermal_Mullerian_Tumor" ) );

            HISTOLOGY_MAP.put( "Papillary_Serous", SearchUtil
                  .getBranchUris( graphDb, "Ovarian_Serous_Surface_Papillary_Adenocarcinoma" ) );
            HISTOLOGY_MAP
                  .put( "Serous", SearchUtil.getBranchUris( graphDb, "Serous_Neoplasm" ) );
            HISTOLOGY_MAP.put( "Sertoli_Leydig", SearchUtil
                  .getBranchUris( graphDb, "Ovarian_Sertoli_Leydig_Cell_Tumor" ) );
            HISTOLOGY_MAP
                  .put( "Squamous_Cell", SearchUtil.getBranchUris( graphDb, "Squamous_Cell_Carcinoma" ) );
            HISTOLOGY_MAP.put( "Undifferentiated", SearchUtil
                  .getBranchUris( graphDb, "Undifferentiated_Ovarian_Carcinoma" ) );
            HISTOLOGY_MAP.put( "Yolk_Sac", SearchUtil.getBranchUris( graphDb, "Yolk_Sac_Tumor" ) );


            Arrays.stream( ANATOMIES )
                  .map( u -> SearchUtil.getBranchUris( graphDb, u ) )
                  .forEach( ANATOMY_URIS::addAll );

            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Anatomic_Border" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Cell_Part" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Soft_Tissue" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Gland" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Blood_Brain_Barrier" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Abnormal_Cell" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Microanatomic_Structure" ) );

            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Skin" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Skin_Part" ) );

            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Other_Anatomic_Concept" ) );
            UNWANTED_ANATOMY_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Body_Fluid_or_Substance" ) );
            UNWANTED_ANATOMY_URIS.remove( "Scalp" );


            ANATOMY_URIS.removeAll( UNWANTED_ANATOMY_URIS );

         }
      }
   }


}
