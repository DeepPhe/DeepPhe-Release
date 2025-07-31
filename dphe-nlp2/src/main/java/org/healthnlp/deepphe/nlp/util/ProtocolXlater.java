package org.healthnlp.deepphe.nlp.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {5/22/2024}
 */
final public class ProtocolXlater {

   // Family history was not recorded!
//Family History of Any Cancer (famhx_anyca)
//Value set: Yes, No, Unknown

//Family History of Breast Cancer (famhx_brca)
//Value set: Yes, No, Unknown

//Family History of Ovarian Cancer (famhx_cvca)
//Value set: Yes, No, Unknown

//Family History of Skin Cancer (fhx_skinca)
//Value set: Yes, No, Unknown

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

   // This was not recorded!  ---> Not listed in categorical list.
//Smoking (comorb_smk)
//Value set: past, current, never, unknown

//Autoimmune Diseases (comorb_autoimm)
//Value set: Yes, No, Unknown/Not Found


   // This is not done!  We decided at some point to NOT do this with dphe.
//Date of diagnosis (dt_dx)
//Value set: date
//Source (structured data, clinical narrative, both): both
//Tool: DeepPhe for clinical narrative and query over structured data (tumor registry)
//Note: identification of the date of diagnosis is based on an algorithm from Warner et al.,4 plus modification to start with tumor registry date of diagnosis if available. Hierarchy: 1) date of diagnosis from tumor registry data; if not available, then by whether cancer case was diagnosed internal or external to EMR system (based on if EMR exits at least 45 days prior to the first ICD code for cancer); if internal, then 2) date of first relevant ICD code; if external, then earliest of 2) first relevant path report date or 3) date of external path review; if not available then date of first relevant ICD code.

//Histology – Tumor Registry (ca_hist_tr), Clinical Narrative (ca_hist_emr), Summary (ca_hist_sum)
//Value set:
//For breast: ductal, lobular, other, unknown (and if other, then type)
//For ovary: germ, sex cord / stromal, clear cell, endometrioid, mucinous, serous, other,
//epithelial NOS, unknown (and if other, then type)

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

   // This was not done!
//Date of assessment - Variables: date of assessment for all three tumor markers (date of first report)
//Value set: date stamp of first assessment document (for each)
//Source (structured data, clinical narrative, both): clinical narrative filenames

   // This was not recorded!
//Episode of assessment - Variables: episode of assessment for all three tumor markers
//Value set: episode (pre-diagnostic, diagnostic, decision making, treatment, follow-up, and unknown) of first assessment document (for each marker)

//Variables: BRCA1/2, PIK3CA, and TP53 if assessed, and result of assessment, for each
//Value set: if assessed (Yes/No/Unknown), if Y then result (Positive, Negative, Unknown)

   // This was not recorded!
//Variables: date of assessment for all clinical genomics (for each)
//	Value set: date stamp of first assessment document (date of first report)
//	Source (structured data, clinical narrative, both): clinical narrative filenames

   // This was not recorded!
//Variables: episode of assessment for all clinical genomics (for each)
//Value set: episode (pre-diagnostic, diagnostic, decision making, treatment, follow-up, and unknown) of first assessment document (for each)

   // Not exactly recorded!
//Number of Cancer Diagnoses (ca_n)
//Value set: 0 1, 2, etc. (continuous)

//Breast Cancer (ca_breast)
//Value set: Y/N

//Ovarian Cancer (ca_ovary)
//Value set: Y/N

//Other Cancer (ca_other)
//Value set: Y/N

   // This was not recorded!
//Other Cancer Type (ca_other)
//Value set: Endometrial, Colorectal, Lung, etc.

   // This was not recorded!
//: episode flags: pre-diagnostic, diagnostic, decision making, treatment, follow-up
//	Value set: Y/N (Yes if documents within an episode were identified, No if not identified)
//	Source (structured data, clinical narrative, both): created based on DeepPhe data

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


   final String[] PROTOCOLS = {

   };


   static private final class PatientInfo {
      private final String _patientId;
      private Map<String, String> _protocolHaves = new HashMap<>();

      private PatientInfo( final String patientId ) {
         _patientId = patientId;


      }


   }


   public static void main( String[] args ) {
      final String inputDir = args[ 0 ];
      final String outputDir = args[ 1 ];
   }


}
