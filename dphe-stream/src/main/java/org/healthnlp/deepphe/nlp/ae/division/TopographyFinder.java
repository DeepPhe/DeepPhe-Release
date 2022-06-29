package org.healthnlp.deepphe.nlp.ae.division;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.core.util.paragraph.ParagraphProcessor;
import org.apache.ctakes.core.util.section.AbstractSectionProcessor;
import org.apache.ctakes.core.util.section.SectionProcessor;
import org.apache.ctakes.core.util.sentence.SentenceProcessor;
import org.apache.ctakes.core.util.topic.TopicProcessor;
import org.apache.ctakes.core.util.treelist.ListProcessor;
import org.apache.ctakes.typesystem.type.textspan.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {4/15/2022}
 */
@PipeBitInfo(
      name = "TopographyFinder",
      description = "Finds Anatomic Site values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class TopographyFinder extends AbstractSectionProcessor implements SectionProcessor,
                                                                              ListProcessor,
                                                                              SentenceProcessor {

   static private final Logger LOGGER = Logger.getLogger( "TopographyFinder" );

//   A. SKIN, LEFT XIPHOID CHEST, BIOPSY (D17-5683; 5/23/2017):
//MELANOMA, invasive to 4.4 mm (at least), anatomic level IV; transected at the
//base.                                                                       <--  LOW CONFIDENCE

//   A. SKIN, LEFT DISTAL RADIAL DORSAL FOREARM, SHAVE (D21-0141563; 6/15/2021):
//MALIGNANT MELANOMA, invasive to a depth of at least 1.7 mm, at least anatomic
//level IV; present at side and deep margins (see synoptic report and NOTE).   <--  LOW CONFIDENCE

//   Specimens:A: SKIN, LEFT DISTAL RADIAL DORSAL FOREARM, SHAVE BIOPSY 6/15/2021   <--  LOW CONFIDENCE

//   A. RIGHT FEMORAL LYMPHADENECTOMY CONTENTS:                                     <--  LOW CONFIDENCE
//     METASTATIC MELANOMA, involving four (4) of eight (8) lymph
//      nodes.
//     Largest metastatic focus is 4.0 cm.
//     No definitive extranodal extension.

//   B. RIGHT EXTERNAL ILIAC LYMPHADENECTOMY CONTENTS (including FSB):
//     Glomus tumor (0.6 cm), incidental.

//   TISSUE SUBMITTED:                                                     <-- LOW CONFIDENCE
//A/1.  Right femoral lymphadenectomy contents.
//B/2.  Right external iliac lymphadenectomy contents, ? carcinoma, ? melanoma.
//C/3.  Additional right external iliac lymphadenectomy contents.
//D/4.  Melanoma, right fourth and fifth toes, requiring wide excision.
//E/5.  Distal skin dog ear, right foot, wide excision.
//F/6.  Proximal skin dog ear, right foot, wide excision.

//   EXAMINATION PERFORMED:
//BREAST STEREOTACTIC BIOPSY VACUUM ASSIST WITH CLIP PLACEMENT LEFT        <-- LOW

//GROSS DESCRIPTION:
//The specimen is received in formalin in two parts.                 <--  LOW CONFIDENCE
//Part 1 is received labeled with the patient's name, initials KP and "left
//breast at 9 o'clock with calcifications". The specimen is filtered through the
// [...]
//   Part 2 is received labeled with the patient's name, initials KP and "left
//breast at 9 o'clock without calcifications".  The specimen is filtered through
// [...]
//   HISTO TISSUE SUMMARY/SLIDES REVIEWED:
//Part 1: Left Breast 9:00 w/ Calcs
// [...]
//Part 2: Left Breast 9:00 w/o Calcs

//EXAMINATION PERFORMED:
//BREAST STEREOTACTIC BIOPSY VACUUM ASSIST WITH CLIP PLACEMENT LEFT

//IMPRESSION:
//1. MALIGNANT CONCORDANT PATHOLOGY OF LEFT BREAST CALCIFICATIONS AT 9
//O'CLOCK POSITION.




//   O.R. CONSULTATION:                                                    <-- LOW CONFIDENCE
//SPECIMEN LABELED "#2 RIGHT EXTERNAL ILIAC LYMPHADENECTOMY CONTENTS, ?
//CARCINOMA, ? MELANOMA":

//Synoptic Diagnosis                                                <-- LOW CONFIDENCE
//1)  LEFT FALLOPIAN TUBE AND OVARY:
//SPECIMEN
//   [...]
//   2)  UTERUS, CERVIX, RIGHT FALLOPAIN TUBE, RIGHT OVARY:
//SPECIMEN

//    Specimen Integrity of Right Ovary:    Capsule intact              <-- LOW CONFIDENCE
// Specimen Integrity of Left Ovary:    Capsule intact
// Specimen Integrity of the Right Fallopian Tube:    Serosa intact
// Specimen Integrity of the Left Fallopian Tube:    Serosa intact

//   IMPORTANT INFORMATION REGARDING COMMUNICATION OF BREAST BIOPSY RESULTS:
//Please be aware that an important component of percutaneous breast biopsy

//   Summary of organs/tissues microscopically involved by tumor: Ovaries,    <-- REALLY LOW CONFIDENCE
//Fallopian tubes, uterine serosa, omentum, right pelvic sidewall, posterior
//cul-de-sac, colonic and appendiceal serosa, mesentery, left colon gutter,
//diaphragm.

//   D. RIGHT FOURTH AND FIFTH TOES:
//     MALIGNANT MELANOMA, invasive to a depth of 4.30 mm, anatomic
//      Level IV; early V cannot be excluded.
//     The resection margins are negative for tumor.
//     Tumor is 0.9 cm to skin margin, 0.2 cm to soft tissue margin, and 1.5
//      cm to the bone resection margin.
//
//     Other Features:
//          Subtype                           Unclassified
//          Intraepidermal component          Focal
//          Vertical growth phase             Present
//          Ulceration                        Not definitely identified
//          Regression                        Present, extensive
//          Mitotic rate                      3/mm2
//          Tumor-infiltrating lymphocytes    Present, non-brisk
//          Vascular/lymphatic invasion       Not identified
//          Microscopic satellites            Not identified
//          Cell type                         Epithelioid
//          Precursor                         Not identified
//
//    Extensive perineural invasion is noted.
//
//E. DISTAL SKIN DOG EAR, RIGHT FOOT:
//     Skin, negative for melanoma.
//
//F. PROXIMAL SKIN DOG EAR, RIGHT FOOT:
//     Skin, negative for melanoma.


//   6)  RIGHT BREAST:

//Specimen Laterality:                 Right
//Tumor Site of Invasive Carcinoma:      Other: RETROAREOLAR
//   Laterality: left
//Tumor site: 11 o'clock
//Specimen Laterality:             Right
//Specimen Laterality:  Left
//Tumor Site:    Distal radial dorsal forearm
//Specimen Laterality:  Left
//Tumor Site:    Distal radial dorsal forearm
//Specimen Laterality:  Right
//Tumor Site:    Mid upper mid back

//History:
//Preoperative Diagnosis:  Malignant melanoma of skin of foot, right.
//Secondary malignant neoplasm of inguinal and lower limb lymph nodes.
//Postoperative Diagnosis:  Malignant melanoma of skin of foot, right.
//Secondary malignant neoplasm of inguinal and lower limb lymph nodes.

//Primary tumor site: Ovaries
//Specimen Integrity:
//     Right Ovary: intact
//     Left Ovary: intact

//   Tumor size: 4.0 cm (right ovary); 3.5 cm (left ovary)

//   TUMOR
//Tumor Site:  Left ovary

//TUMOR
//Tumor Site:  Endometrium

//FINAL DIAGNOSIS:
//PART 1: LEFT BREAST, 9 O'CLOCK, STEREOTACTIC-GUIDED CORE BIOPSY WITH
//CALCIFICATIONS
//DUCTAL CARCINOMA IN SITU, SOLID AND CRIBRIFORM TYPES, NUCLEAR GRADE 3
//ASSOCIATED WITH COMEDO-TYPE NECROSIS AND MICROCALCIFICATIONS.
//PART 2: LEFT BREAST, 9 O'CLOCK, STEREOTACTIC-GUIDED CORE BIOPSY WITHOUT
//CALCIFICATIONS
//DUCTAL CARCINOMA IN SITU, SOLID TYPE, NUCLEAR GRADE 3.

//Postprocedure CC and 90 degree mammographic views of the left breast        <-- Can we use procedure proximity to
// <-- lower confidence?
//were performed, demonstrating the cylindrical shaped clip to be in
//the appropriate position.

//SUCCESSFUL STEREOTACTIC-GUIDED BIOPSY AND CYLINDRICAL SHAPED CLIP        <-- Procedure proximity?
//PLACEMENT FOR A SMALL GROUP OF INDETERMINATE CALCIFICATIONS WITHIN
//THE POSTERIOR OUTER LEFT BREAST AT THE 9 O'CLOCK LOCATION.

//   She had a well-healed subcentimeter stab incision in the lower outer quadrant of her left breast.  <PROCEDURE
//   Otherwise, she had no skin changes, nipple changes, or nipple discharge bilaterally.
//She had no masses within either breast.          <-- Negation
//   There are no other abnormalities within either breast.   <-- NEGATION

//   REASON FOR VISIT:  DCIS, discussion of nipple-sparing mastectomy      <-- Procedure proximity?  Discussion?
//
//HISTORY:  Ms. Person6 is a very pleasant 49-year-old lady who was recently diagnosed with a small focus of ductal
// carcinoma in situ within her left breast.


//Ms. Person6 was seen by Dr. Person39 La Person23 for discussion of immediate reconstruction, who suggested that she
// would be an excellent candidate for a nipple sparing mastectomy and that she was referred to me for discussion of
// bilateral nipple-sparing mastectomy with left sentinel lymph node biopsy.     <-- Procedure proximity?  Discussion?

//Ms. Person6 notes that she does have a paternal aunt who had postmenopausal breast cancer and a paternal
// grandmother who had ovarian cancer.
//Otherwise, she has no significant family or personal history of breast cancer in the past.

//   PHYSICAL EXAM:  A comprehensive breast exam was performed with the patient in a sitting and supine position.

//   IMAGING REVIEW:  The patient has breast imaging consisting of bilateral mammography and diagnostic mammography
//   of the left breast which reveal clustered calcifications in the lower outer quadrant of the left breast, some of
//   which are coarse.

//ASSESSMENT AND PLAN:  Ms. Person6 is a 49-year-old woman with ductal carcinoma in situ of the left breast, who is considering bilateral nipple-sparing mastectomy.

//   v--- Discussion
//   1. I discussed with Ms. Person6 and her husband today her physical, pathologic, and radiologic findings.
// 2. We also discussed the pathophysiology of ductal carcinoma in situ and the fact that in as much as 15%-25% of
// the time ductal carcinoma in situ identified on a core biopsy can be upstaged to invasive ductal carcinoma upon
// final resection, and that with mastectomy, once this is performed, we lose the opportunity to perform a sentinel
// lymph node should that occur, and thus at the time of the mastectomy we would perform a sentinel lymph node biopsy.
//We discussed the various risks and benefits of sentinel lymph node biopsy, including lymphedema and nerve damage,
// and its relation to a full axillary lymph node dissection.
// 3. We specifically discussed nipple-sparing mastectomy, for which I do think Ms. Person6 would be an excellent
// candidate, considering her body habitus, her minimal amount of breast ptosis, and her small focus of ductal
// carcinoma in situ that is remote from the nipple-areolar complex.
//We discussed that nipple-sparing mastectomy really only has 5 year followup data, in most studies, however, that it
// shows at this time that nipple-sparing mastectomy is equivalent to skin-sparing or traditional mastectomy in local
// recurrence and overall survival.
//We discussed the methods by which nipple-sparing mastectomy is performed and we discussed the possibility of loss
// of the nipple-areolar complex, either because of involvement with cancer or significant necrosis; this risks being
// approximately 5%-6%.
// 4. Ms. Person6 and her husband asked several excellent questions in the office, all of which were answered, and
// they expressed understanding of all that we discussed at the end of our visit.


//   PROCEDURES PERFORMED DURING THIS ADMISSION:
//
//1. Bilateral nipple sparing left mastectomy done by Dr. Person81 on
//07/13/2010.
//2. Immediate bilateral breast reconstruction using bilateral tissue expanders
//was done by Dr. Person153 on 07/13/2010 following the mastectomy.

//   CLINICAL HISTORY:
//64-year-old female who had a recent screening mammogram on 11/28/2012
//and was asked to return for additional imaging of a new spiculated
//mass in the upper outer left breast. The patient's daughter was
//recently diagnosed and treated for breast cancer at age 38, and has
//tested positive for a BRCA mutation.

//   TECHNIQUE:
//A left 90 degree lateral full field digital mammogram was obtained
//along with tomosynthesis imaging in the 90 degree lateral and
//exaggerated CC planes. Left breast ultrasound, ultrasound of the left
//axilla and ultrasound-guided core biopsy of the left breast lesion
//with clip placement and a postprocedure mammogram were also performed.

//FINDINGS:
//The left breast is predominantly fatty replaced. There is an 18x10mm
//new dense spiculated mass in the upper outer left breast, the
//appearance of which is highly suspicious for malignancy. The
//remainder of the left mammogram is unchanged. There are no associated
//calcifications.

//Left breast ultrasound showed an irregularly marginated hypoechoic
//lesion at the 2 o'clock position of the left breast measuring
//18x8x13mm, the mammographic and sonographic appearance of which is
//highly suspicious for malignancy. Ultrasound-guided core biopsy was
//recommended.

//   biopsy of the 18 mm hypoechoic lesion at the 2 o'clock position of
//the left breast. 6 core samples were obtained. A Inrad wing shaped

//   Ultrasound-guided core biopsy of the left probable primary breast
//malignancy. An addendum will be issued when the pathology report
//becomes available.

//   Left breast: ACR BI-RADS category 5-highly suspicious for malignancy.

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Grade Values ..." );
      super.process( jCas );
   }


   @Override
   public TopicProcessor getTopicProcessor() {
      return null;
   }

   @Override
   public ParagraphProcessor getParagraphProcessor() {
      return null;
   }

   @Override
   public ListProcessor getListProcessor() {
      return this;
   }

   @Override
   public SentenceProcessor getSentenceProcessor() {
      return this;
   }

   @Override
   public Collection<Pair<Integer>> processSentence( final JCas jCas, final Segment section, final Topic topic,
                                                     final Paragraph paragraph,
                                                     final Sentence sentence, final NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      LOGGER.info( "Processed Spans: " + jCas.getDocumentText()
                                             .length() + "\n"
                   + noteDivisions.getProcessedSpans()
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() + " " )
                                  .collect( Collectors.joining( " ; " ) ) );
      LOGGER.info( "Available Spans: " + jCas.getDocumentText()
                                             .length() + "\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );
      final String text = sentence.getCoveredText();

      return Collections.emptyList();
   }

   @Override
   public Collection<Pair<Integer>> processList( final JCas jCas, final Segment section, final Topic topic,
                                                 final Paragraph paragraph,
                                                 final FormattedList list, final NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      LOGGER.info( "List Type " + list.getListType() );
      LOGGER.info( "DocText length: " + jCas.getDocumentText()
                                            .length() + " Processed spans:\n"
                   + noteDivisions.getProcessedSpans()
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );

      LOGGER.info( "DocText length: " + jCas.getDocumentText()
                                            .length() + " Available spans:\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );

      return Collections.emptyList();
   }

}
