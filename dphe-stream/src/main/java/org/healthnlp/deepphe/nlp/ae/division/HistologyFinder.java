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
      name = "HistologyFinder",
      description = "Finds Neoplasm Type and Histology values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class HistologyFinder extends AbstractSectionProcessor implements SectionProcessor,
                                                                               ListProcessor,
                                                                               SentenceProcessor {

   static private final Logger LOGGER = Logger.getLogger( "HistologyFinder" );


   //   O.R. CONSULTATION:                                                    <-- LOW CONFIDENCE
//SPECIMEN LABELED "#2 RIGHT EXTERNAL ILIAC LYMPHADENECTOMY CONTENTS, ?
//CARCINOMA, ? MELANOMA":

//   MELANOMA OF THE SKIN SYNOPTIC REPORT                         <-- LOW CONFIDENCE
//    Ovarian Carcinoma Summary Findings                        <-- LOW CONFIDENCE


//   A. SKIN, LEFT XIPHOID CHEST, BIOPSY (D17-5683; 5/23/2017):   <-- LOW CONFIDENCE
//MELANOMA, invasive to 4.4 mm (at least), anatomic level IV; transected at the
//base.

//   A. SKIN, LEFT DISTAL RADIAL DORSAL FOREARM, SHAVE (D21-0141563; 6/15/2021):    <-- LOW CONFIDENCE
//MALIGNANT MELANOMA, invasive to a depth of at least 1.7 mm, at least anatomic
//level IV; present at side and deep margins (see synoptic report and NOTE).

//   A. RIGHT FEMORAL LYMPHADENECTOMY CONTENTS:    <-- LOW CONFIDENCE
//     METASTATIC MELANOMA, involving four (4) of eight (8) lymph
//      nodes.
//     Largest metastatic focus is 4.0 cm.
//     No definitive extranodal extension.

//   B. RIGHT EXTERNAL ILIAC LYMPHADENECTOMY CONTENTS (including FSB):    <-- LOW CONFIDENCE
//     Glomus tumor (0.6 cm), incidental.

//D. RIGHT FOURTH AND FIFTH TOES:          <-- LOW CONFIDENCE
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
//E. DISTAL SKIN DOG EAR, RIGHT FOOT:         <-- LOW CONFIDENCE
//     Skin, negative for melanoma.
//
//F. PROXIMAL SKIN DOG EAR, RIGHT FOOT:       <-- LOW CONFIDENCE
//     Skin, negative for melanoma.

//   PATIENT HISTORY:
//CHIEF COMPLAINT/PRE-OP/POST-OP DIAGNOSIS: Preop diagnosis: Probably benign.
//Postop diagnosis: Same.

//   FINAL DIAGNOSIS:
//PART 1: LEFT BREAST, 9 O'CLOCK, STEREOTACTIC-GUIDED CORE BIOPSY WITH
//CALCIFICATIONS
//DUCTAL CARCINOMA IN SITU, SOLID AND CRIBRIFORM TYPES, NUCLEAR GRADE 3
//ASSOCIATED WITH COMEDO-TYPE NECROSIS AND MICROCALCIFICATIONS.
//PART 2: LEFT BREAST, 9 O'CLOCK, STEREOTACTIC-GUIDED CORE BIOPSY WITHOUT
//CALCIFICATIONS
//DUCTAL CARCINOMA IN SITU, SOLID TYPE, NUCLEAR GRADE 3.

//   ADDENDUM:
//FINDINGS:
//Pathology report demonstrates DCIS, solid and cribriform types,



//Tumor Size:  Cannot be determined: Extensive squamous differentiation and squamous metaplasia are noted. Largest
// contiguous focus of carcinoma measures 5 mm in extent.               <-- REALLY LOW CONFIDENCE, IF ANY

//             Cell type                         Epithelioid       <-- LOW CONFIDENCE
//History: Melanoma
//History: MELANOMA

   //Breast Carcinoma Summary Findings
//Histologic type: no special type
//Presence of Invasive Carcinoma:      Present
//Histologic Type:                     Invasive mammary carcinoma, no special type
//   Ductal Carcinoma In Situ:            DCIS is present
//DCIS Architectural Patterns:         Cribriform
//Ductal Carcinoma In Situ:        DCIS is present
//Presence of Invasive Carcinoma:  Present
//Histologic Type:                 Invasive mammary carcinoma, no special type with  tubular features
//Histologic Type:    Unclassified
//   Cell type: Epithelioid                   <-- LOW CONFIDENCE
//History: Melanoma
//Histologic Type:    Superficial spreading
//     Cell type: Epithelioid                 <-- LOW CONFIDENCE

//Histologic Type:  Endometrioid carcinoma with secretory differentiation

//   Associated Ductal Carcinoma in Situ type and grade: cribriform and solid
//types, intermediate-grade, 10% of tumor volume

//History:
//Preoperative Diagnosis:  Malignant melanoma of skin of foot, right.
//Secondary malignant neoplasm of inguinal and lower limb lymph nodes.
//Postoperative Diagnosis:  Malignant melanoma of skin of foot, right.
//Secondary malignant neoplasm of inguinal and lower limb lymph nodes.

//   Clinical Diagnosis: Not given.

//   Histologic type: papillary serous carcinoma

//Histologic Type:  Endometrioid carcinoma



//   REASON FOR VISIT:  DCIS, discussion of nipple-sparing mastectomy
//
//HISTORY:  Ms. Person6 is a very pleasant 49-year-old lady who was recently diagnosed with a small focus of ductal
// carcinoma in situ within her left breast.

//   Ms. Person6 notes that she does have a paternal aunt who had postmenopausal breast cancer and a paternal
//   grandmother who had ovarian cancer.
//Otherwise, she has no significant family or personal history of breast cancer in the past.

//ASSESSMENT AND PLAN:  Ms. Person6 is a 49-year-old woman with ductal carcinoma in situ of the left breast, who is considering bilateral nipple-sparing mastectomy.

//   v--- DISCUSSION
//1. I discussed with Ms. Person6 and her husband today her physical, pathologic, and radiologic findings.
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


//   The patient is a pleasant 49-year-old who was brought in for elective bilateral
//mastectomies following __________ breast cancer.  She underwent the above


//   CLINICAL HISTORY:
//64-year-old female who had a recent screening mammogram on 11/28/2012
//and was asked to return for additional imaging of a new spiculated
//mass in the upper outer left breast. The patient's daughter was
//recently diagnosed and treated for breast cancer at age 38, and has
//tested positive for a BRCA mutation.

//   FINDINGS:
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

//Ultrasound-guided core biopsy of the left probable primary breast
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
