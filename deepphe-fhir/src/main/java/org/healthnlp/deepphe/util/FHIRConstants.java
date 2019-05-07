package org.healthnlp.deepphe.util;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * list of misc FHIR and ontology related constants
 * @author tseytlin
 */
public class FHIRConstants {
   public static final String LINE = "_______________________________________________________";

   public static final String NLP_CANCER_URL = "http://ontologies.dbmi.pitt.edu/deepphe/nlpCancer.owl";
   public static final String SCHEMA_URL = "http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl";
   public static final String CONTEXT_URL = "http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl";
   public static final String MODEL_CANCER_URL = NLP_CANCER_URL;

   public static final String ANNOTATION_TYPE_MENTION = "mention";
   public static final String ANNOTATION_TYPE_DOCUMENT = "document";
   public static final String ANNOTATION_TYPE_RECORD = "record";


   public static final String SUMMARY_LEVEL_CONCEPT = "concept";
   public static final String SUMMARY_LEVEL_DOCUMENT = "document";
   public static final String SUMMARY_LEVEL_PATIENT = "patient";

   public static final String INTERPRETATION_POSITIVE = "Positive";
   public static final String INTERPRETATION_NEGATIVE = "Negative";

   public static final String ELEMENT = "DpheElement";
   public static final String EVENT ="Event";
   public static final String COMPOSITION = "Composition";
   public static final String PATIENT = "DphePatient";
   public static final String TUMOR = "Tumor";
   public static final String CANCER = "Cancer";
   public static final String TUMOR_PHENOTYPE = "TumorPhenotype";
   public static final String CANCER_PHENOTYPE = "CancerPhenotype";
   public static final String PATIENT_PHENOTYPE = "PatientPhenotype";
   public static final String EPISODE = "Episode";
   public static final String EPISODE_DIAGNOSTIC = "DiagnosticEpisode";
   public static final String EPISODE_TREATMENT = "TreatmentEpisode";
   public static final String EPISODE_FOLLOW_UP = "FollowUpEpisode";
   public static final String EPISODE_PREDIAGNOSTIC = "PrediagnosticEpisode";
   public static final Map<String,String> EPISODE_TYPE_MAP = new LinkedHashMap<String,String>();
   public static final String FHIR_TYPE = "FHIR";

   static{
      EPISODE_TYPE_MAP.put("Diagnostic",EPISODE_DIAGNOSTIC);
      EPISODE_TYPE_MAP.put("Pre-Diagnostic",EPISODE_PREDIAGNOSTIC);
      EPISODE_TYPE_MAP.put("Treatment",EPISODE_TREATMENT);
      EPISODE_TYPE_MAP.put("Follow-up",EPISODE_FOLLOW_UP);
   }

   static public final String MEDICATION = "Medication";
   static public final String DEVICE = "Device";


   public static final String HISTOLOGIC_TYPE = "HistologicType";
   public static final String CONDITION = "Condition";
   public static final String DIAGNOSIS = "DiseaseDisorder";
   public static final String PROCEDURE = "Procedure";
   public static final String OBSERVATION = "Observation";
   public static final String FINDING = "Finding";
   public static final String MEDICATION_STATEMENT = "MedicationStatement";
   public static final String BODY_SITE = "BodySite";
   public static final String TUMOR_SIZE = "Tumor_Size";
   public static final String QUANTITY = "Quantity";
   public static final String STAGE = "Generic_TNM_Finding";
   public static final String TNM_STAGE = "TNM_Staging_System";
   public static final String AGE = "Age";
   public static final String GENDER = "Gender";
   public static final String PHENOTYPIC_FACTOR = "PhenotypicFactor";
   public static final String GENOMIC_FACTOR = "GenomicFactor";
   public static final String TREATMENT_FACTOR = "TreatmentFactor";
   public static final String RELATED_FACTOR = "RelatedFactor";
   public static final String TREATMENT = "Treatment";
   public static final String MANIFISTATION = "ManifestationOfDisease";
   public static final String MEDICAL_RECORD = "MedicalRecord";
   public static final String MODIFIER = "Modifier";
   public static final String BODY_MODIFIER = "BodyModifier";
   public static final String LATERALITY = "Laterality";
   public static final String ORDINAL_INTERPRETATION = "OrdinalInterpretation";
   public static final String QUADRANT = "Quadrant";
   public static final String CLOCKFACE_POSITION = UriConstants.CLOCKFACE;
   public static final String UNIT = "Unit";
   public static final String TNM_MODIFIER = "TNM_Modifier";
   public static final String HAS_BODY_SITE = RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE;
   public static final String HAS_LATERALITY = RelationConstants.HAS_LATERALITY;
   public static final String HAS_QUADRANT = RelationConstants.HAS_QUADRANT;
   public static final String HAS_CLOCKFACE = RelationConstants.HAS_CLOCKFACE;
   public static final String HAS_BODY_MODIFIER = "hasBodySiteModifier";
   public static final String HAS_TREATMENT = RelationConstants.REGIMEN_HAS_ACCEPTED_USE_FOR_DISEASE;
   public static final String HAS_OUTCOME = "hasOutcome";
   public static final String HAS_CANCER_STAGE = RelationConstants.HAS_STAGE;
   public static final String HAS_CANCER_TYPE = "hasCancerCellLine";
   public static final String HAS_TUMOR_EXTENT = "hasTumorExtent";
   public static final String HAS_CALCIFICATION = "hasCalcification";
   public static final String HAS_T_CLASSIFICATION = "hasGenericTClassification";
   public static final String HAS_N_CLASSIFICATION = "hasGenericNClassification";
   public static final String HAS_M_CLASSIFICATION = "hasGenericMClassification";
   
   public static final String HAS_PATHOLOGIC_T_CLASSIFICATION = "hasPathologicTClassification";
   public static final String HAS_PATHOLOGIC_N_CLASSIFICATION = "hasPathologicNClassification";
   public static final String HAS_PATHOLOGIC_M_CLASSIFICATION = "hasPathologicMClassification";
   public static final String HAS_PATHOLOGIC_TNM_PATTERN = ".*hasPathologic.*";
   public static final String HAS_GENERIC_TNM_PATTERN = ".*hasGeneric.*";
   public static final String HAS_CLINICAL_TNM_PATTERN = ".*hasClinical.*";
   public static final String HAS_CLINICAL_T_CLASSIFICATION = "hasClinicalTClassification";
   public static final String HAS_CLINICAL_N_CLASSIFICATION = "hasClinicalNClassification";
   public static final String HAS_CLINICAL_M_CLASSIFICATION = "hasClinicalMClassification";
   
   public static final String HAS_TUMOR_TYPE = "hasTumorType";
   public static final String HAS_SEQUENCE_VARIENT = "hasSequenceVarient";
   public static final String HAS_HISTOLOGIC_TYPE = "hasHistologicType";
   public static final String HAS_MANIFESTATION = "hasManifestation";
   public static final String HAS_NAME = "hasName";
   public static final String HAS_GENDER = "hasGender";
   public static final String HAS_BIRTH_DATE = "hasBirthDate";
   public static final String HAS_DEATH_DATE = "hasDeathDate";
   public static final String HAS_INTERPRETATION = "hasInterpretation";
   public static final String HAS_NUM_VALUE = "hasNumValue";
   public static final String HAS_METHOD = "hasMethod";
   public static final String HAS_TNM_PREFIX = "hasTNMPrefix";
   public static final String HAS_TNM_SUFFIX = "hasTNMSuffix";
   public static final String HAS_DIAGNOSIS = "hasDiagnosis";
   public static final String HAS_RECURRENCE = "hasRecurrence";
   
   public static final String HAS_RECEPTOR_STATUS = "hasReceptorStatus";
   public static final String HAS_PR_STATUS = "hasPRStatus";
   public static final String HAS_ER_STATUS = "hasERStatus";
   public static final String HAS_HER2_STATUS = "hasHer2Status";
   
   public static final String T_STAGE = "T_Stage";
   public static final String M_STAGE = "M_Stage";
   public static final String N_STAGE = "N_Stage";
   public static final String P_MODIFIER = "p_modifier";
   public static final String C_MODIFIER = "c_modifier";
   
   
   // predefined URIs
   public static final String PATIENT_SUMMARY_URI = MODEL_CANCER_URL + "#" + PATIENT;
   public static final String CANCER_SUMMARY_URI = MODEL_CANCER_URL + "#" + CANCER;
   public static final String TUMOR_SUMMARY_URI = MODEL_CANCER_URL + "#" + TUMOR;
   public static final String EPISODE_URI = MODEL_CANCER_URL + "#" + EPISODE;
   public static final String CANCER_PHENOTYPE_SUMMARY_URI = MODEL_CANCER_URL + "#" + CANCER_PHENOTYPE;
   public static final String TUMOR_PHENOTYPE_SUMMARY_URI = MODEL_CANCER_URL + "#" + TUMOR_PHENOTYPE;

   public static final String PATIENT_PHENOTYPE_SUMMARY_URI = MODEL_CANCER_URL + "#" + PATIENT_PHENOTYPE;
   public static final String MEDICAL_RECORD_URI = SCHEMA_URL + "#MedicalRecord";

   static public final String NOTE_URI = "Temporary#NOTE";

   // mention level URIs
   public static final URI OBSERVATION_URI = URI.create(SCHEMA_URL+"#"+OBSERVATION);
   public static final URI FINDING_URI = URI.create(SCHEMA_URL+"#"+FINDING);
   public static final URI DIAGNOSIS_URI = URI.create(SCHEMA_URL+"#"+DIAGNOSIS);
   public static final URI CONDITION_URI = URI.create(SCHEMA_URL+"#"+CONDITION);
   public static final URI PROCEDURE_URI = URI.create(SCHEMA_URL+"#"+PROCEDURE);
   public static final URI MEDICATION_URI = URI.create(SCHEMA_URL+"#"+MEDICATION);
   public static final URI BODY_SITE_URI = URI.create( CONTEXT_URL + "#" + BODY_SITE );
   public static final URI PATIENT_URI = URI.create(SCHEMA_URL+"#"+PATIENT);
   public static final URI TREATMENT_URI = URI.create(MODEL_CANCER_URL+"#"+TREATMENT);
   public static final URI MANIFISTATION_URI = URI.create(MODEL_CANCER_URL+"#"+MANIFISTATION);
   
   public static final URI HISTOLOGIC_TYPE_URI = URI.create(MODEL_CANCER_URL+"#"+HISTOLOGIC_TYPE);
   public static final URI TUMOR_EXTENT_URI = URI.create(MODEL_CANCER_URL+"#TumorExtent");
   public static final URI CANCER_TYPE_URI = URI.create(MODEL_CANCER_URL+"#CancerType");
   
   public static final URI PRIMARY_TUMOR_URI = URI.create(MODEL_CANCER_URL+"#PrimaryTumor");
   public static final URI RECURRENT_TUMOR_URI = URI.create(MODEL_CANCER_URL+"#Recurrent_Tumor");
   public static final URI MALE_URI = URI.create(MODEL_CANCER_URL+"#Male_Gender");
   public static final URI FEMALE_URI = URI.create(MODEL_CANCER_URL+"#Female_Gender");
   public static final URI QUANTITY_URI = URI.create("http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl#Quantity");
   public static final URI NUMERIC_MODIFIER_URI = URI.create("http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl#NumericModifier");
   public static final URI GENERIC_TNM = URI.create(MODEL_CANCER_URL+"#TNM_Staging_System");
   public static final URI ORDINAL_INTERPRETATION_URI = URI.create("http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl#OrdinalInterpretation");

   public static final List<String> BODY_SIDE_LIST = Arrays.asList("Right","Left","Bilateral");
   public static final List<String> TNM_MODIFIER_LIST = Arrays.asList("p","c","y","r","sn");
   public static final String BREAST = "Breast";

   
   public static final String PRIMARY_TUMOR = "PrimaryTumor";
   public static final String DISTANT_METASTASIS = "Distant_Metastasis";
   public static final String LOCAL_RECURRENCE = "LocalRecurrence";
   public static final String REGIONAL_METASTASIS = "Regional_Metastasis";
//   public static final String METASTATIC_TUMOR = "Metastatic_Tumor";
   public static final String METASTATIC_TUMOR = "Secondary_Neoplasm";

   public static final String HAS_CANCER_CELL_LINE = "hasCancerCellLine";
   public static final String HAS_COMPOSITION_ATTIBUTE = "hasCompositionAttribute";
   public static final String HAS_DOC_TYPE = "hasDocType";
   public static final String HAS_EPISODE_TYPE = "hasEpisodeType";
   public static final String HAS_SECTION = "hasSection";
   public static final String HAS_LINGUISTIC_MODIFIERS = "hasLinguisticModifier";
   public static final String DOCUMENT_TYPE = "DocumentType";
   public static final String HAS_POLARITY = "hasPolarity";
   public static final String HAS_TEMPORALITY = "hasTemporality";
   public static final String HAS_EXPERIENCER = "hasExperiencer";
   public static final String HAS_CONTEXTUAL_MODALITY = "hasContextualModality";
   public static final String POLARITY_POSITIVE = "Positive_Polarity";
   public static final String POLARITY_NEGATIVE = "Negative_Polarity";
   public static final String EXPERIENCER_PATIENT = "Patient_Experiencer";
   public static final String EXPERIENCER_FAMILY_MEMBER = "FamilyMember_Experiencer";
   public static final String EXPERIENCER_DONOR_FAMILY_MEMBER = "DonorFamilyMember_Experiencer";
   public static final String EXPERIENCER_DONOR_OTHER_MEMBER = "DonorOtherMember_Experiencer";
   public static final String EXPERIENCER_OTHER_MEMBER = "OtherMember_Experiencer";
   
   public static final String TEMPORALITY_BEFORE = "Before_DocTimeRel";
   public static final String TEMPORALITY_BEFORE_OVERLAP = "Before-Overlap_DocTimeRel";
   public static final String TEMPORALITY_OVERLAP = "Overlap_DocTimeRel";
   public static final String TEMPORALITY_AFTER = "After_DocTimeRel";
   public static final String MODALITY_ACTUAL = "Actual_ContextualModality";
   public static final String MODALITY_GENERIC = "Generic_ContextualModality";
   public static final String MODALITY_HEDGED = "Hedged_ContextualModality";
   public static final String MODALITY_HYPOTHETICAL = "Hypothetical_ContextualModality";

   public static final String HAS_COMPOSITION = "hasComposition";
   public static final String HAS_PATIENT = "hasPatient";

   public static final String SYNOPTIC_SECTION = "Synoptic_Section";

   public static final String HAS_PHENOTYPE = "hasPhenotype";

   public static final String BILATERAL = "Bilateral";

   public static final String PREF_TERM = "preferredTerm";
   
   //document type
   public static final String PATHOLOGY_REPORT = "Pathology_Report";
   public static final String SURGICAL_PATHOLOGY_REPORT = "Surgical_Pathology_Report";
   public static final String RADIOLOGY_REPORT = "Radiology_report";
   public static final String DISCHARGE_SUMMARY_REPORT = "Discharge_summary";
   public static final String PROGRESS_NOTE_REPORT = "Progress_note";

   public static final String HAS_DOC_TITLE = "hasDocumentTitle";
   
   //drools
   public static final String DOMAIN_BREAST = "Breast";
   public static final String NIPPLE = "Nipple";
   public static final String CANCER_SUMMARY = "CancerSummary";
   public static final String TUMOR_SUMMARY = "TumorSummary";
   public static final String RECORD_SUMMARY = "RecordSummary";
   public static final String CLINICAL_NOTE = "ClinicalNote";
   public static final String TUMOR_TYPE = "TumorType";
   public static final String DIAGNOSTIC_PROCEDURE = "DiagnosticProcedure";
   public static final String PR_STATUS_PATTERN = ".*Progesterone_Receptor_.*";
   public static final String ER_STATUS_PATTERN = ".*Estrogen_Receptor_.*";
   public static final String HER2_STATUS_PATTERN = ".*HER2_Neu_.*";
   
   public static final String PR_STATUS = "Progesterone_Receptor";
   public static final String ER_STATUS = "Estrogen_Receptor";
   public static final String HER2_STATUS = "HER2_Neu";
   
   
   public static final String PATHOLOGIC_TNM = "Pathologic_TNM_Finding";

   public static final String DOMAIN_MELANOMA = "Melanoma";
   public static final String DOMAIN_OVARIAN = "Ovarian";
   public static final List<String> DOMAINS = Arrays.asList(DOMAIN_BREAST,DOMAIN_MELANOMA,DOMAIN_OVARIAN);

   public static final String LESION = "Lesion";
   
}
