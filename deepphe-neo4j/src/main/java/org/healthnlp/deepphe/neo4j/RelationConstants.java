package org.healthnlp.deepphe.neo4j;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2018
 */
final public class RelationConstants {

   static public final String IS_A = "Is_A";

   ///////////////////////////////////////////////////////////////////////////////////////////////
   //                                           DPHE
   //                              Specific Relations for Objects
   ///////////////////////////////////////////////////////////////////////////////////////////////

   static public final String CANCER_HAS_TUMOR = "Cancer_Has_Tumor";
   static public final String DISEASE_HAS_TREATMENT = "Disease_Has_Treatment";
   static public final String DISEASE_HAS_T_STAGE = "Disease_Has_T_Stage";
   static public final String DISEASE_HAS_N_STAGE = "Disease_Has_N_Stage";
   static public final String DISEASE_HAS_M_STAGE = "Disease_Has_M_Stage";
   static public final String ANATOMIC_SITE_HAS_LATERALITY = "Anatomic_Site_Has_Laterality";

   static public final String HAS_LATERALITY = "hasLaterality";
   static public final String HAS_QUADRANT = "hasQuadrant";
   static public final String HAS_CLOCKFACE = "hasClockface";
   static public final String METASTASIS_OF = "isMetastasisOf";
   static public final String HAS_SIZE = "hasSize";
   static public final String HAS_RECEPTOR_STATUS = "hasReceptorStatus";
   static public final String HAS_TNM = "hasTNM";
   static public final String HAS_TUMOR_TYPE = "hasTumorType";
   static public final String HAS_TREATMENT = "hasTreatment";
   static public final String HAS_STAGE = "hasCancerStage";
   static public final String HAS_HISTOLOGY = "hasHistologicType";
   static public final String HAS_CALCIFICATION = "hasCalcification";
   static public final String HAS_DIAGNOSIS = "hasDiagnosis";
   static public final String HAS_TUMOR_EXTENT = "hasTumorExtent";
   static public final String HAS_METHOD = "hasMethod";
   static public final String HAS_CANCER_CELL_LINE = "hasCancerCellLine";

   static public final String HAS_CLINICAL_T = "has_Clinical_T";
   static public final String HAS_CLINICAL_N = "has_Clinical_N";
   static public final String HAS_CLINICAL_M = "has_Clinical_M";
   static public final String HAS_PATHOLOGIC_T = "has_Pathologic_T";
   static public final String HAS_PATHOLOGIC_N = "has_Pathologic_N";
   static public final String HAS_PATHOLOGIC_M = "has_Pathologic_M";

   static public final String HAS_ER_STATUS = "has_ER_Status";
   static public final String HAS_PR_STATUS = "has_PR_Status";
   static public final String HAS_HER2_STATUS = "has_HER2_Status";


   ///////////////////////////////////////////////////////////////////////////////////////////////
   //                                           NCIT
   //                                   Generic Relation Types
   ///////////////////////////////////////////////////////////////////////////////////////////////

   static public final String ANATOMIC_STRUCTURE_HAS_LOCATION = "Anatomic_Structure_Has_Location";
   static public final String ANATOMIC_STRUCTURE_IS_PHYSICAL_PART_OF = "Anatomic_Structure_Is_Physical_Part_Of";


   //   static public final String DISEASE_HAS_PRIMARY_ANATOMIC_SITE = "Disease_Has_Primary_Anatomic_Site";
   static public final String DISEASE_HAS_PRIMARY_ANATOMIC_SITE = "hasBodySite";
   static public final String DISEASE_EXCLUDES_PRIMARY_ANATOMIC_SITE = "Disease_Excludes_Primary_Anatomic_Site";
   //   static public final String DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE = "Disease_Has_Associated_Anatomic_Site";
   static public final String DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE = "hasBodySite";
   static public final String DISEASE_HAS_METASTATIC_ANATOMIC_SITE = "Disease_Has_Metastatic_Anatomic_Site";

   static public final String DISEASE_HAS_FINDING = "Disease_Has_Finding";
   static public final String DISEASE_MAY_HAVE_FINDING = "Disease_May_Have_Finding";
   static public final String DISEASE_EXCLUDES_FINDING = "Disease_Excludes_Finding";

   static public final String DISEASE_IS_STAGE = "Disease_Is_Stage";
   static public final String DISEASE_IS_GRADE = "Disease_Is_Grade";

   static public final String DISEASE_HAS_NORMAL_TISSUE_ORIGIN = "Disease_Has_Normal_Tissue_Origin";
   static public final String DISEASE_EXCLUDES_NORMAL_TISSUE_ORIGIN = "Disease_Excludes_Normal_Tissue_Origin";

   static public final String DISEASE_HAS_ABNORMAL_CELL = "Disease_Has_Abnormal_Cell";
   static public final String DISEASE_MAY_HAVE_ABNORMAL_CELL = "Disease_May_Have_Abnormal_Cell";
   static public final String DISEASE_EXCLUDES_ABNORMAL_CELL = "Disease_Excludes_Abnormal_Cell";

   static public final String DISEASE_HAS_ASSOCIATED_DISEASE = "Disease_Has_Associated_Disease";
   static public final String DISEASE_MAY_HAVE_ASSOCIATED_DISEASE = "Disease_May_Have_Associated_Disease";

   static public final String DISEASE_HAS_MOLECULAR_ABNORMALITY = "Disease_Has_Molecular_Abnormality";
   static public final String DISEASE_MAY_HAVE_MOLECULAR_ABNORMALITY = "Disease_May_Have_Molecular_Abnormality";
   static public final String DISEASE_EXCLUDES_MOLECULAR_ABNORMALITY = "Disease_Excludes_Molecular_Abnormality";

   static public final String DISEASE_HAS_NORMAL_CELL_ORIGIN = "Disease_Has_Normal_Cell_Origin";
   static public final String DISEASE_MAY_HAVE_NORMAL_CELL_ORIGIN = "Disease_May_Have_Normal_Cell_Origin";
   static public final String DISEASE_EXCLUDES_NORMAL_CELL_ORIGIN = "Disease_Excludes_Normal_Cell_Origin";

   static public final String DISEASE_HAS_CYTOGENETIC_ABNORMALITY = "Disease_Has_Cytogenetic_Abnormality";
   static public final String DISEASE_MAY_HAVE_CYTOGENETIC_ABNORMALITY = "Disease_May_Have_Cytogenetic_Abnormality";
   static public final String DISEASE_EXCLUDES_CYTOGENETIC_ABNORMALITY = "Disease_Excludes_Cytogenetic_Abnormality";

   static public final String DISEASE_MAPPED_TO_CHROMOSOME = "Disease_Mapped_To_Chromosome";
   static public final String DISEASE_MAPPED_TO_GENE = "Disease_Mapped_To_Gene";


   static public final String PROCEDURE_HAS_TARGET_ANATOMY = "Procedure_Has_Target_Anatomy";
   static public final String PROCEDURE_HAS_EXCISED_ANATOMY = "Procedure_Has_Excised_Anatomy";
   static public final String PROCEDURE_HAS_COMPLETELY_EXCISED_ANATOMY = "Procedure_Has_Completely_Excised_Anatomy";
   static public final String PROCEDURE_HAS_PARTIALLY_EXCISED_ANATOMY = "Procedure_Has_Partially_Excised_Anatomy";
   static public final String PROCEDURE_MAY_HAVE_COMPLETELY_EXCISED_ANATOMY = "Procedure_May_Have_Completely_Excised_Anatomy";
   static public final String PROCEDURE_MAY_HAVE_EXCISED_ANATOMY = "Procedure_May_Have_Excised_Anatomy";
   static public final String PROCEDURE_MAY_HAVE_PARTIALLY_EXCISED_ANATOMY = "Procedure_May_Have_Partially_Excised_Anatomy";
   static public final String PROCEDURE_HAS_IMAGED_ANATOMY = "Procedure_Has_Imaged_Anatomy";


   static public final String REGIMEN_HAS_ACCEPTED_USE_FOR_DISEASE = "Regimen_Has_Accepted_Use_For_Disease";
   static public final String CHEMOTHERAPY_REGIMEN_HAS_COMPONENT = "Chemotherapy_Regimen_Has_Component";
   static public final String CHEMICAL_OR_DRUG_AFFECTS_CELL_TYPE_OR_TISSUE = "Chemical_Or_Drug_Affects_Cell_Type_Or_Tissue";
   static public final String CHEMICAL_OR_DRUG_HAS_MECHANISM_OF_ACTION = "Chemical_Or_Drug_Has_Mechanism_Of_Action";
   static public final String CHEMICAL_OR_DRUG_AFFECTS_GENE_PRODUCT = "Chemical_Or_Drug_Affects_Gene_Product";
   static public final String CHEMICAL_OR_DRUG_HAS_PHYSIOLOGICAL_EFFECT = "Chemical_Or_Drug_Has_Physiologic_Effect";
   static public final String CHEMICAL_OR_DRUG_IS_METABOLIZED_BY_ENZYME = "Chemical_Or_Drug_Is_Metabolized_By_Enzyme";
   static public final String CHEMICAL_OR_DRUG_AFFECTS_ABNORMAL_CELL = "Chemical_Or_Drug_Affects_Abnormal_Cell";
   static public final String CHEMICAL_OR_DRUG_PLAYS_ROLE_IN_BIOLOGICAL_PROCESS = "Chemical_Or_Drug_Plays_Role_In_Biological_Process";


   static public final String GENE_PRODUCT_HAS_STRUCTURAL_DOMAIN = "Gene_Product_Has_Structural_Domain_Or_Motif";
   static public final String GENE_PRODUCT_ENCODED_BY_GENE = "Gene_Product_Encoded_By_Gene";
   static public final String GENE_PLAYS_ROLE_IN_PROCESS = "Gene_Plays_Role_In_Process";
   static public final String GENE_IN_CHROMOSOMAL_LOCATION = "Gene_In_Chromosomal_Location";
   static public final String GENE_IS_ELEMENT_IN_PATHWAY = "Gene_Is_Element_In_Pathway";
   static public final String GENE_PRODUCT_IS_ELEMENT_IN_PATHWAY = "Gene_Product_Is_Element_In_Pathway";
   static public final String GENE_PRODUCT_PLAYS_ROLE_IN_BIOLOGICAL_PROCESS = "Gene_Product_Plays_Role_In_Biological_Process";
   static public final String GENE_PRODUCT_HAS_BIOCHEMICAL_FUNCTION = "Gene_Product_Has_Biochemical_Function";
   static public final String GENE_PRODUCT_HAS_ASSOCIATED_ANATOMY = "Gene_Product_Has_Associated_Anatomy";
   static public final String GENE_PRODUCT_EXPRESSED_IN_TISSUE = "Gene_Product_Expressed_In_Tissue";
   static public final String GENE_PRODUCT_IS_BIOMARKER_TYPE = "Gene_Product_Is_Biomarker_Type";
   static public final String GENE_PRODUCT_MALFUNCTION_ASSOCIATED_WITH_DISEASE = "Gene_Product_Malfunction_Associated_With_Disease";
   static public final String GENE_PRODUCT_IS_BIOMARKER_OF = "Gene_Product_Is_Biomarker_Of";
   static public final String GENE_PRODUCT_HAS_ORGANISM_SOURCE = "Gene_Product_Has_Organism_Source";
   static public final String GENE_PRODUCT_HAS_CHEMICAL_CLASSIFICATION = "Gene_Product_Has_Chemical_Classification";
   static public final String GENE_FOUND_IN_ORGANISM = "Gene_Found_In_Organism";
   static public final String GENE_ASSOCIATED_WITH_DISEASE = "Gene_Associated_With_Disease";
   static public final String GENE_PRODUCT_IS_PHYSICAL_PART_OF = "Gene_Product_Is_Physical_Part_Of";
   static public final String GENE_PRODUCT_HAS_ABNORMALITY = "Gene_Product_Has_Abnormality";
   static public final String GENE_INVOLVED_IN_PATHOGENESIS_OF_DISEASE = "Gene_Involved_In_Pathogenesis_Of_Disease";
   static public final String GENE_HAS_ABNORMALITY = "Gene_Has_Abnormality";
   static public final String GENE_PRODUCT_SEQUENCE_VARIATION_ENCODED_BY_GENE_MUTANT = "Gene_Product_Sequence_Variation_Encoded_By_Gene_Mutant";
   static public final String GENE_MUTANT_ENCODES_GENE_PRODUCT_SEQUENCE_VARIATION = "Gene_Mutant_Encodes_Gene_Product_Sequence_Variation";
   static public final String GENE_PRODUCT_VARIANT_OF_GENE_PRODUCT = "Gene_Product_Variant_Of_Gene_Product";
   static public final String GENE_IS_BIOMARKER_TYPE = "Gene_Is_Biomarker_Type";
   static public final String GENE_IS_BIOMARKER_OF = "Gene_Is_Biomarker_Of";
   static public final String GENE_HAS_PHYSICAL_LOCATION = "Gene_Has_Physical_Location";

   static public final String ALLELE_IN_CHROMOSOMAL_LOCATION = "Allele_In_Chromosomal_Location";
   static public final String ALLELE_HAS_ACTIVITY = "Allele_Has_Activity";
   static public final String ALLELE_HAS_ABNORMALITY = "Allele_Has_Abnormality";
   static public final String ALLELE_PLAYS_ALTERED_ROLE_IN_PROCESS = "Allele_Plays_Altered_Role_In_Process";
   static public final String ALLELE_ABSENT_FROM_WILD_TYPE_CHROMOSOMAL_LOCATION = "Allele_Absent_From_Wild_type_Chromosomal_Location";
   static public final String ALLELE_PLAYS_ROLE_IN_METABOLISM_OF_CHEMICAL_OR_DRUG = "Allele_Plays_Role_In_Metabolism_Of_Chemical_Or_Drug";

   static public final String BIOLOGICAL_PROCESS_HAS_ASSOCIATED_LOCATION = "Biological_Process_Has_Associated_Location";
   static public final String BIOLOGICAL_PROCESS_IS_PART_OF_PROCEDURE = "Biological_Process_Is_Part_Of_Process";
   static public final String CYTOGENIC_ABNORMALITY_INVOLVES_CHROMOSOME = "Cytogenetic_Abnormality_Involves_Chromosome";
   static public final String HAS_FREE_ACID_OR_BASE_FORM = "Has_Free_Acid_Or_Base_Form";
   static public final String HAS_TARGET = "Has_Target";
   static public final String MOLECULAR_ABNORMALITY_INVOLVES_GENE = "Molecular_Abnormality_Involves_Gene";
   static public final String BIOLOGICAL_PROCESS_HAS_INITIATOR_CHEMICAL_OR_DRUG = "Biological_Process_Has_Initiator_Chemical_Or_Drug";
   static public final String BIOLOGICAL_PROCESS_HAS_RESULT_BIOLOGICAL_PROCESS = "Biological_Process_Has_Result_Biological_Process";
   static public final String BIOLOGICAL_PROCESS_HAS_INITIATOR_PROCESS = "Biological_Process_Has_Initiator_Process";
   static public final String HAS_SALT_FORM = "Has_Salt_Form";
   static public final String BIOLOGICAL_PROCESS_HAS_RESULT_CHEMICAL_OR_DRUG = "Biological_Process_Has_Result_Chemical_Or_Drug";
   static public final String BIOLOGICAL_PROCESS_HAS_RESULT_ANATOMY = "Biological_Process_Has_Result_Anatomy";
   static public final String RELATED_TO_GENETIC_BIOMARKER = "Related_To_Genetic_Biomarker";

   private RelationConstants() {
   }


   static public final Collection<String> REQUIRED_RELATIONS = Arrays.asList(
         HAS_LATERALITY,
         HAS_TUMOR_TYPE,
         HAS_CALCIFICATION,
         DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
         DISEASE_HAS_FINDING, DISEASE_MAY_HAVE_FINDING,
         DISEASE_HAS_NORMAL_TISSUE_ORIGIN, DISEASE_HAS_NORMAL_CELL_ORIGIN
   );

   static public Collection<String> getReverseRelations() {
      return Collections.singletonList( DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE );
   }


}

