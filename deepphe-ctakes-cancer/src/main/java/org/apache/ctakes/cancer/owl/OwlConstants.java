package org.apache.ctakes.cancer.owl;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/16/2016
 */
final public class OwlConstants {

   private OwlConstants() {
   }


   static public final String ROOT_ELEMENT_NAME = "Annotation";
   static public final String MODIFIER_ELEMENT_NAME = "Modifier";
   static public final String LEXICON_ELEMENT_NAME = "Lexicon";

   static public final String CONTEXT_OWL = "http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl";
   static public final String SCHEMA_OWL = "http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl";
   static public final String CANCER_OWL = "http://ontologies.dbmi.pitt.edu/deepphe/nlpCancer.owl";
   static public final String BREAST_CANCER_OWL = "http://ontologies.dbmi.pitt.edu/deepphe/nlpBreastCancer.owl";
   static public final String MELANOMA_OWL = "http://ontologies.dbmi.pitt.edu/deepphe/nlpMelanoma.owl";

   static public final String SECTIONS_URI = SCHEMA_OWL + "#DocumentSection";

   static public final String UNKNOWN_URI = SCHEMA_OWL + "#" + ROOT_ELEMENT_NAME;
   static public final String BODY_SITE_URI = CONTEXT_OWL + "#BodySite";
   static public final String MEDICATION_URI = SCHEMA_OWL + "#MedicationStatement";
   static public final String PROCEDURE_URI = SCHEMA_OWL + "#Procedure";
   static public final String SIGN_SYMPTOM_URI = SCHEMA_OWL + "#SignSymptom";
   static public final String DISEASE_DISORDER_URI = SCHEMA_OWL + "#DiseaseDisorder";
   static public final String CONDITION_URI = SCHEMA_OWL + "#Condition";
   static public final String TEST_URI = SCHEMA_OWL + "#DiagnosticProcedure";
   static public final String FINDING_URI = SCHEMA_OWL + "#Finding";
   static public final String CANCER_PHENOTYPE_URI = CANCER_OWL + "#CancerPhenotype";
   static public final String CANCER_URI = CANCER_OWL + "#Cancer";

   static public final String EVENT_URI = SCHEMA_OWL + "#Event";
   static public final String DIAGNOSTIC_TEST_URI = SCHEMA_OWL + "#DiagnosticProcedure";
   static public final String OBSERVATION_URI = SCHEMA_OWL + "#Observation";

   static public final String METASTASIS_URI = CANCER_OWL + "#Metastatic_Neoplasm";

   static public final String TNM_STAGING_URI = CANCER_OWL + "#TNM_Staging_System";
   static public final String T_STAGE_URI = CANCER_OWL + "#T_Stage";
   static public final String N_STAGE_URI = CANCER_OWL + "#N_Stage";
   static public final String M_STAGE_URI = CANCER_OWL + "#M_Stage";

   static public final String CANCER_STAGE_URI = CANCER_OWL + "#CancerStage";

   static public final String RECEPTOR_STATUS_URI = BREAST_CANCER_OWL + "#Receptor_Status";
   static public final String ER_STATUS_URI = BREAST_CANCER_OWL + "#Estrogen_Receptor_Status";
   static public final String PR_STATUS_URI = BREAST_CANCER_OWL + "#Progesterone_Receptor_Status";
   static public final String HER2_STATUS_URI = BREAST_CANCER_OWL + "#HER2_Neu_Status";
   // No longer exists
//   static public final String TRIPLE_NEGATIVE_URI = CANCER_OWL + "#Triple_Negative_Breast_Carcinoma";

   static public final String[] BC_PHENOTYPE_URIS = { TNM_STAGING_URI, CANCER_STAGE_URI,
         RECEPTOR_STATUS_URI };

   static public final String QUANTITY_URI = CONTEXT_OWL + "#Quantity";
   //static public final String SIZE_URI = CONTEXT_OWL + "#DimensionalMeasurement";
   static public final String SIZE_URI = CANCER_OWL + "#Tumor_Size";

   static public final String[] OBSERVATION_URIS = { OBSERVATION_URI, SIZE_URI };

   static public final String LESION_URI = CANCER_OWL + "#Lesion";

   static public final String[] NEOPLASM_URIS = { CANCER_OWL + "#Neoplasm",
//         CANCER_OWL + "#Carcinoma",
         CANCER_OWL + "#CancerType",
         LESION_URI };


   // new definitions
   static public final String BODY_MODIFIER_URI = CONTEXT_OWL + "#BodyModifier";
   static public final String LATERALITY_URI = CONTEXT_OWL + "#Laterality";

   static public final String QUADRANT_URI = OwlConstants.CONTEXT_OWL + "#Quadrant";
   static public final String CLOCKFACE_POSITION_URI = OwlConstants.CONTEXT_OWL + "#ClockfacePosition";

   static public final String[] SEMANTIC_ROOT_URIS = {
         BODY_SITE_URI,
         MEDICATION_URI,
         PROCEDURE_URI,
         SIGN_SYMPTOM_URI,
         DISEASE_DISORDER_URI,
         CONDITION_URI,
         FINDING_URI,
         OBSERVATION_URI
   };


   static public final String IMMUNO_TEST_URI = OwlConstants.BREAST_CANCER_OWL + "#" + "Immunohistochemical_Test";
   static public final String FISH_TEST_URI = OwlConstants.BREAST_CANCER_OWL + "#" + "Fluorescence_In_Situ_Hybridization";
   static public final String CISH_TEST_URI = OwlConstants.BREAST_CANCER_OWL + "#" + "Chromogenic_In_Situ_Hybridization";
   static public final String DISH_TEST_URI = DIAGNOSTIC_TEST_URI;
   static public final String UNSPECIFIED_TEST_URI = DIAGNOSTIC_TEST_URI;

   static public final String C_MODIFIER_URI = OwlConstants.CANCER_OWL + "#" + "c_modifier";
   static public final String P_MODIFIER_URI = OwlConstants.CANCER_OWL + "#" + "p_modifier";

   static public final String BREAST_URI = CANCER_OWL + "#Breast";

   static public final String BODY_SITE_RELATION = "hasBodySite";
}
