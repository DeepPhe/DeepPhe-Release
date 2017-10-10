package org.apache.ctakes.cancer.phenotype.receptor;

import org.apache.ctakes.cancer.phenotype.property.SpannedType;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2015
 */
final public class ReceptorStatusFinderTester {

   static private final Logger LOGGER = Logger.getLogger( "ReceptorStatusTester" );

   static private final String[] MULTIPLE_MENTION_SENTENCES = {
//         "Patient is a 54 year old female with history of T2N0M0 left breast cancer ER-, PR-, HER2+,"
//         + " now undergoing neoadjuvant chemo with taxotere, carboplatin, Herceptin, and pertuzumab.",
//         "Patient is reported to be ER/PR Negative, Her2/neu status: positive.",
//         "Patient is Estrogen and Progesterone negative, Her-2 is positive.",
//         "The estrogen and progesterone receptors are negative and her2 strongly positive.",
//         "ER and PR are negative, HER2/neu pos.",
//         "Result           H Score          Raw Immunostaining Semiquantitation\n" +
//               "ER:\tNegative\t\t0\t\t(0 0%;  1+ 0%;  2+ 30%;  3+ 70%)\n" +
//               "PR:  Negative            5              (0 80%;  1+ 18%;  2+ 1%;  3+ 1%)\n" +
//               "HER2: Positive             1              ( bla bla bla )\n" +
//               "Estrogen receptor antibody SP1, an IVD, is performed using the IVIEW detection\n",
//         "Result           H Score          Raw Immunostaining Semiquantitation\n" +
//               "ER:  Negative           0              (0 0%;  1+ 0%;  2+ 30%;  3+ 70%)\n" +
//               "PR:  Negative            5              (0 80%;  1+ 18%;  2+ 1%;  3+ 1%)\n" +
//               "HER2: Positive             1              ( bla bla bla )\n" +
//               "Estrogen receptor antibody SP1, an IVD, is performed using the IVIEW detection\n",
//         "RESULT                       H-SCORE\n" +
//               "ESTROGEN          Negative                        75\n" +
//               "PROGESTERONE      Negative                         10\n" +
//               "^Mean ER H Scores vs Percent Cells Staining: 250 (>75%); 130 (51-75%); 40\n" +
//               "HER2/neu pos.",
//         "Addendum\n" +
//               "Stains for ER and PR were performed on Part 2:\n" +
//               " OF BREAST_URI TUMOR IMMUNOHISTOLOGY RESULTS**\n" +
//               "HORMONE RECEPTOR IMMUNOHISTOCHEMISTRY\n" +
//               "RESULT                       H-SCORE\n" +
//               "ESTROGEN          Negative                        100\n" +
//               "PROGESTERONE      Negative                         15\n" +
//               "^Mean ER H Scores vs Percent Cells Staining: 250 (>75%); 130 (51-75%); 40\n" +
//               "(10-50%; 5 (<10%)\n" +
//               "HER2/NEU          Positive                         1\n" +
//               "ESTROGEN/PROGESTERONE RECEPTORS IMMUNOHISTOCHEMICAL REPORT\n",
//         "ER/PR negative and HER-2 positive",
//         "is a 40-year-old premenopausal female with clinical T2 N0 Mx IDC diagnosed September 10, 2013, ER/PR negative  and HER-2 positive\n" +
//         "\n" +
//         " Obtain staging scans CT chest, abdomen, and pelvis \n",
         "invasive ductal\n" +
               "carcinoma ER -/PR -/Her 2+.  "
   };

   static private final String[] MULTIPLE_MENTION_SENTENCES_2 = {
         "Negative for Er, Pr, and Her-2/neu"
   };

   static private final String[] NO_RECEPTOR_SENTENCES = {
         "Patient stated her position.", "Patient sex is neuter.", "Patient is positive her pulse is rapid.",
         "Patient was rushed to the ER", "The patient rapped PR to the ER for HER2 the neu!",
         "xy complement, progesterone absorption negative",
         "ER Positive: 1+" };

   //  THIS SENTENCE IS EXPECTED TO CREATE A FALSE POSITIVE
   static private final String[] FAIL_RECEPTOR_SENTENCES = { "The ER is positive that the patient is stable." };

   static private final String[] ER_POS_SENTENCES = {
         "Patient tested ER+ last week.", "Patient tested ER + last week.",
         "Patient tested ER+pos last week.", "Patient tested ER+positive last week.",
         "Patient tested ER pos last week.", "Patient tested ER positive last week.",
         "Patient tested ER status pos last week.", "Patient tested ER status positive last week.",
         "Patient ER status pos.", "Patient ER status is positive.",
         "ER      POS", "ER Status           POSITIVE",
         "ER: POS", "Estrogen Receptors: Positive",
         "Estrogen      POS", "Estrogen           POSITIVE",
         "Estrogen Receptor POS", "Estrogen-Receptor POSITIVE", "Estrogen Receptor-positive",
         "Estrogen Receptor status POS", "Estrogen-Receptor status is positive",
         "Patient is positive for Estrogen Receptor",
         "ER:  Positive           80              (0 50%;  1+ 10#%;  2+ 40%;  3+ 0%)",
         "The estrogen receptor was positive H.", "ER TEST: WEAKLY POSITIVE", "ER TEST: WEAK POSITIVE",
         "Estrogen Receptor:    POSITIVE.", "some: Estrogen Receptor:   POSITIVE." };

   static private final String[] ER_NEG_SENTENCES = {
         "Patient tested ER- last week.", "Patient tested ER - last week.",
         "Patient tested ER-neg last week.", "Patient tested ER-negative last week.",
         "Patient tested ER neg last week.", "Patient tested ER negative last week.",
         "Patient tested ER status neg last week.", "Patient tested ER status negative last week.",
         "Patient ER status neg.", "Patient ER status is negative.",
         "ER      NEG", "ER Status           NEGATIVE",
         "ER: NEG", "Estrogen Receptors : Negative",
         "Estrogen      NEG", "Estrogen           NEGATIVE",
         "Estrogen Receptor NEG", "Estrogen-Receptor NEGATIVE", "Estrogen Receptor-negative",
         "Estrogen Receptor status NEG", "Estrogen-Receptor status is negative",
         "ER:  Negative           0              (0 100%;  1+ 0%;  2+ 0%;  3+ 0%)",
         "Patient is negative for Estrogen Receptor" };

   static private final String[] ER_NA_SENTENCES = {
         "Patient ER status unknown.", "Patient ER status is unknown.",
         "Patient ER status indeterminate.", "Patient ER status is indeterminate.",
         "Patient ER status equivocal.", "Patient ER status is equivocal.",
         "Patient ER status not assessed.", "Patient ER status is not assessed.",
         "Patient ER status NA.", "Patient ER status is N/A.",
         "ER      unknown", "ER Status           unknown",
         "ER      indeterminate", "ER Status           indeterminate",
         "ER      equivocal", "ER Status           equivocal",
         "ER      not assessed", "ER Status           not assessed",
         "ER      NA", "ER Status           NA",
         "ER      N/A", "ER Status           N/A",
         "Estrogen      unknown", "Estrogen      indeterminate",
         "Estrogen      equivocal", "Estrogen      not assessed",
         "Estrogen      NA", "Estrogen      N/A" };


   static private final String[] PR_POS_SENTENCES = {
         "Patient tested PR+ last week.", "Patient tested PR + last week.",
         "Patient tested PR+pos last week.", "Patient tested PR+positive last week.",
         "Patient tested PR pos last week.", "Patient tested PR positive last week.",
         "Patient tested PR status pos last week.", "Patient tested PR status positive last week.",
         "Patient PR status pos.", "Patient PR status is positive.",
         "PR      POS", "PR Status           POSITIVE",
         "PR: POS", "Progesterone Receptors : Positive",
         "Progesterone      POS", "Progesterone           POSITIVE",
         "Progesterone Receptor POS", "Progesterone-Receptor POSITIVE", "Progesterone Receptor-positive",
         "Progesterone Receptor status POS", "Progesterone-Receptor status is positive",
         "Patient is positive for Progesterone Receptor" };

   static private final String[] PR_NEG_SENTENCES = {
         "Patient tested PR- last week.", "Patient tested PR - last week.",
         "Patient tested PR-neg last week.", "Patient tested PR-negative last week.",
         "Patient tested PR neg last week.", "Patient tested PR negative last week.",
         "Patient tested PR status neg last week.", "Patient tested PR status negative last week.",
         "Patient PR status neg.", "Patient PR status is negative.",
         "PR      NEG", "PR Status           NEGATIVE",
         "PR: NEG", "Progesterone Receptors : Negative",
         "Progesterone      NEG", "Progesterone           NEGATIVE             0",
         "Progesterone Receptor NEG", "Progesterone-Receptor NEGATIVE", "Progesterone Receptor-negative",
         "Progesterone Receptor status NEG", "Progesterone-Receptor status is negative",
         "Patient is negative for Progesterone Receptor" };

   static private final String[] PR_NA_SENTENCES = {
         "Patient PR status unknown.", "Patient PR status is unknown.",
         "Patient PR status indeterminate.", "Patient PR status is indeterminate.",
         "Patient PR status equivocal.", "Patient PR status is equivocal.",
         "Patient PR status not assessed.", "Patient PR status is not assessed.",
         "Patient PR status NA.", "Patient PR status is N/A.",
         "PR      unknown", "PR Status           unknown",
         "PR      indeterminate", "PR Status           indeterminate",
         "PR      equivocal", "PR Status           equivocal",
         "PR      not assessed", "PR Status           not assessed",
         "PR      NA", "PR Status           NA",
         "PR      N/A", "PR Status           N/A",
         "Progesterone      unknown", "Progesterone      indeterminate",
         "Progesterone      equivocal", "Progesterone      not assessed",
         "Progesterone      NA", "Progesterone      N/A" };


   static private final String[] HER2_POS_SENTENCES = {
         "Patient tested HER2+ last week.", "Patient tested HER2 + last week.",
         "Patient tested HER2+pos last week.", "Patient tested HER2+positive last week.",
         "Patient tested HER2 pos last week.", "Patient tested HER2 positive last week.",
         "Patient tested HER2 status pos last week.", "Patient tested HER2 status positive last week.",
         "Patient HER2 status pos.", "Patient HER2 status is positive.",
         "HER-2      POS", "HER2 Status           POSITIVE",
         "Patient tested HER2/neu+ last week.", "Patient tested HER2/neu + last week.",
         "Patient tested HER2/neu+pos last week.", "Patient tested HER2/neu+positive last week.",
         "Patient tested HER2/neu pos last week.", "Patient tested HER2/neu positive last week.",
         "Patient tested HER2/neu status pos last week.", "Patient tested HER2/neu status positive last week.",
         "Patient HER2/neu status pos.", "Patient HER2/neu status is positive.",
         "HER-2/neu      POS", "HER2/neu Status           POSITIVE",
         "HER2: POS", "HER2/neu Receptors : Positive",
         "HER2/neu Receptor POS", "HER2/neu-Receptor POSITIVE",
         "HER2/neu Receptor status POS", "HER2/neu-Receptor status is positive",
         "HER2 PROTEIN EXPRESSION (0-3+):    3+",
         "Patient is positive for Her-2/neu",
         "HER-2/NEU          Positive                        3+",
         " IMMUNOHISTOCHEMISTRY [NEGATIVE:0,1+; EQUIVOCAL 2+; POSITIVE 3+]\n" +
               "RESULT                       SCORE\n" +
               "HER-2/NEU           Positive                         3+\n" +
               "TUMOR CELL PROLIFERATION INDEX (Ki-67)\n", "HER-2/neu expression is positive",
   };

   static private final String[] HER2_NEG_SENTENCES = {
         "Patient tested HER2- last week.", "Patient tested HER2 - last week.",
         "Patient tested HER2-neg last week.", "Patient tested HER2-negative last week.",
         "Patient tested HER2 neg last week.", "Patient tested HER2 negative last week.",
         "Patient tested HER2 status neg last week.", "Patient tested HER2 status negative last week.",
         "Patient HER2 status neg.", "Patient HER2 status is negative.",
         "HER-2      NEG", "HER2 Status           NEGATIVE",
         "Patient tested HER2/neu- last week.", "Patient tested HER2/neu - last week.",
         "Patient tested HER2/neu-neg last week.", "Patient tested HER2/neu-negative last week.",
         "Patient tested HER2/neu neg last week.", "Patient tested HER2/neu negative last week.",
         "Patient tested HER2/neu status neg last week.", "Patient tested HER2/neu status negative last week.",
         "Patient HER2/neu status neg.", "Patient HER2/neu status is negative.",
         "HER2: NEG", "HER2/neu Receptors : Negative",
         "HER-2/neu      NEG", "HER2/neu Status           NEGATIVE",
         "HER2/neu Receptor NEG", "HER2/neu-Receptor NEGATIVE",
         "HER2/neu Receptor status NEG", "HER2/neu-Receptor status is negative",
         "HER2 PROTEIN EXPRESSION (0-3+):    0+", "HER2 PROTEIN EXPRESSION (0-3+):    1+",
         "Patient is negative for Her-2/neu", "Patient is negative for Her-2", "Her-2/neu -" };

   static private final String[] HER2_NA_SENTENCES = {
         "Patient HER2 status unknown.", "Patient HER2 status is unknown.",
         "Patient HER2 status indeterminate.", "Patient HER2 status is indeterminate.",
         "Patient HER2 status equivocal.", "Patient HER2 status is equivocal.",
         "Patient HER2 status not assessed.", "Patient HER2 status is not assessed.",
         "Patient HER2 status NA.", "Patient HER2 status is N/A.",
         "HER2      unknown", "HER2 Status           unknown",
         "HER2      indeterminate", "HER2 Status           indeterminate",
         "HER2      equivocal", "HER2 Status           equivocal",
         "HER2      not assessed", "HER2 Status           not assessed",
         "HER2      NA", "HER2 Status           NA",
         "HER-2      N/A", "HER2 Status           N/A",
         "Patient HER2/neu status unknown.", "Patient HER2/neu status is unknown.",
         "Patient HER2/neu status indeterminate.", "Patient HER2/neu status is indeterminate.",
         "Patient HER2/neu status equivocal.", "Patient HER2/neu status is equivocal.",
         "Patient HER2/neu status not assessed.", "Patient HER2/neu status is not assessed.",
         "Patient HER2/neu status NA.", "Patient HER2/neu status is N/A.",
         "HER2/neu      unknown", "HER2/neu Status           unknown",
         "HER2/neu      indeterminate", "HER2/neu Status           indeterminate",
         "HER2/neu      equivocal", "HER2/neu Status           equivocal",
         "HER2/neu      not assessed", "HER2/neu Status           not assessed",
         "HER-2/neu      NA", "HER2/neu Status           NA",
         "HER2/neu      N/A", "HER2/neu Status           N/A",
         "HER2 PROTEIN EXPRESSION (0-3+):    2+" };


   static private final String[] TRIPLE_NEG_SENTENCES = {
         "Patient tested Triple- last week.", "Patient tested Triple - last week.",
         "Patient tested Triple-neg last week.", "Patient tested Triple-negative last week.",
         "Patient tested Triple neg last week.", "Patient tested Triple negative last week."
   };

   @Test
   public void testMultipleMention() {
      for ( String sentence : MULTIPLE_MENTION_SENTENCES ) {
         testMultiples( sentence );
      }
   }

   static private void testMultiples( final String sentence ) {
      final List<Status> statuses
            = StatusFinder.getReceptorStatuses( sentence );
      assertEquals( "Expect three Hormone Receptors in " + sentence, 3, statuses.size() );
      assertTrue( "First receptor is Estrogen in " + sentence,
            statuses.get( 0 ).getSpannedType().getType() == StatusType.ER );
      assertTrue( "Second receptor is Progesterone in " + sentence,
            statuses.get( 1 ).getSpannedType().getType() == StatusType.PR );
      assertTrue( "Third receptor is HER2/neu in " + sentence,
            statuses.get( 2 ).getSpannedType().getType() == StatusType.HER2 );
      assertTrue( "First receptor is Negative in " + sentence,
            statuses.get( 0 ).getSpannedValue().getValue() == StatusValue.NEGATIVE );
      assertTrue( "Second receptor is Negative in " + sentence,
            statuses.get( 1 ).getSpannedValue().getValue() == StatusValue.NEGATIVE );
      assertTrue( "Third receptor is Positive in " + sentence,
            statuses.get( 2 ).getSpannedValue().getValue() == StatusValue.POSITIVE );
   }

   @Test
   public void testMultipleMention2() {
      for ( String sentence : MULTIPLE_MENTION_SENTENCES_2 ) {
         testMultiples2( sentence );
      }
   }

   static private void testMultiples2( final String sentence ) {
      final List<Status> statuses
            = StatusFinder.getReceptorStatuses2( sentence );
      assertEquals( "Expect three Hormone Receptors in " + sentence, 3, statuses.size() );
      assertTrue( "First receptor is Estrogen in " + sentence,
            statuses.get( 0 ).getSpannedType().getType() == StatusType.ER );
      assertTrue( "Second receptor is Progesterone in " + sentence,
            statuses.get( 1 ).getSpannedType().getType() == StatusType.PR );
      assertTrue( "Third receptor is HER2/neu in " + sentence,
            statuses.get( 2 ).getSpannedType().getType() == StatusType.HER2 );
      assertTrue( "First receptor is Negative in " + sentence,
            statuses.get( 0 ).getSpannedValue().getValue() == StatusValue.NEGATIVE );
      assertTrue( "Second receptor is Negative in " + sentence,
            statuses.get( 1 ).getSpannedValue().getValue() == StatusValue.NEGATIVE );
      assertTrue( "Third receptor is Positive in " + sentence,
            statuses.get( 2 ).getSpannedValue().getValue() == StatusValue.NEGATIVE );
   }

   @Test
   public void testNoReceptor() {
      for ( String sentence : NO_RECEPTOR_SENTENCES ) {
         assertEquals( "Expect no Hormone Receptors in " + sentence, 0,
               StatusFinder.getReceptorStatuses( sentence ).size() );
      }
   }

   @Test
   public void testFailReceptor() {
      for ( String sentence : FAIL_RECEPTOR_SENTENCES ) {
         assertEquals( "Expect an unwanted Hormone Receptor in " + sentence, 1,
               StatusFinder.getReceptorStatuses( sentence ).size() );
      }
   }

   static private void testReceptorStatus( final String[] sentences,
                                           final StatusType expectedType,
                                           final StatusValue expectedValue ) {
      final List<Status> statuses = new ArrayList<>( 1 );
      for ( String sentence : sentences ) {
         statuses.addAll( StatusFinder.getReceptorStatuses( sentence ) );
         statuses.addAll( StatusFinder.getReceptorStatuses2( sentence ) );
         assertEquals( "Expect one Hormone Receptor in " + sentence, 1, statuses.size() );
         assertTrue( "Receptor is " + expectedType.getTitle() + " in " + sentence,
               statuses.get( 0 ).getSpannedType().getType() == expectedType );
         assertTrue( "Receptor is " + statuses.get( 0 ).getSpannedValue().getValue().getTitle()
                     + " not " + expectedValue.getTitle() + " in " + sentence,
               statuses.get( 0 ).getSpannedValue().getValue() == expectedValue );
         statuses.clear();
      }
   }

   static private void testTripleStatus( final String[] sentences,
                                         final StatusValue expectedValue ) {
      final List<Status> statuses = new ArrayList<>( 3 );
      for ( String sentence : sentences ) {
         statuses.addAll( StatusFinder.getReceptorStatuses( sentence ) );
         assertEquals( "Expect one Triple Negative in " + sentence, 1, statuses.size() );
         assertTrue( "No Triple Negative in " + sentence, hasStatusType( statuses, StatusType.NEG_3 ) );
//         assertEquals( "Expect three Hormone Receptor in " + sentence, 3, statuses.size() );
//         assertTrue( "No PR status in " + sentence, hasStatusType( statuses, StatusType.PR ) );
//         assertTrue( "No ER status in " + sentence, hasStatusType( statuses, StatusType.ER ) );
//         assertTrue( "No HER2 status in " + sentence, hasStatusType( statuses, StatusType.HER2 ) );
//         assertTrue( "Receptor is " + expectedValue.getTitle() + " in " + sentence,
//               statuses.get( 0 ).getSpannedValue().getValue() == expectedValue );
//         assertTrue( "Receptor is " + expectedValue.getTitle() + " in " + sentence,
//               statuses.get( 1 ).getSpannedValue().getValue() == expectedValue );
//         assertTrue( "Receptor is " + expectedValue.getTitle() + " in " + sentence,
//               statuses.get( 2 ).getSpannedValue().getValue() == expectedValue );
         statuses.clear();
      }
   }

   static private boolean hasStatusType( final Collection<Status> statuses, final StatusType statusType ) {
      return statuses.stream()
            .map( Status::getSpannedType )
            .map( SpannedType::getType )
            .anyMatch( statusType::equals );
   }


   @Test
   public void testErPos() {
      testReceptorStatus( ER_POS_SENTENCES, StatusType.ER, StatusValue.POSITIVE );
   }

   @Test
   public void testErNeg() {
      testReceptorStatus( ER_NEG_SENTENCES, StatusType.ER, StatusValue.NEGATIVE );
   }

   @Test
   public void testErNA() {
      testReceptorStatus( ER_NA_SENTENCES, StatusType.ER, StatusValue.UNKNOWN );
   }

   @Test
   public void testPrPos() {
      testReceptorStatus( PR_POS_SENTENCES, StatusType.PR, StatusValue.POSITIVE );
   }

   @Test
   public void testPrNeg() {
      testReceptorStatus( PR_NEG_SENTENCES, StatusType.PR, StatusValue.NEGATIVE );
   }

   @Test
   public void testPrNA() {
      testReceptorStatus( PR_NA_SENTENCES, StatusType.PR, StatusValue.UNKNOWN );
   }


   @Test
   public void testHer2Pos() {
      testReceptorStatus( HER2_POS_SENTENCES, StatusType.HER2, StatusValue.POSITIVE );
   }

   @Test
   public void testHer2Neg() {
      testReceptorStatus( HER2_NEG_SENTENCES, StatusType.HER2, StatusValue.NEGATIVE );
   }

   @Test
   public void testHer2NA() {
      testReceptorStatus( HER2_NA_SENTENCES, StatusType.HER2, StatusValue.UNKNOWN );
   }

   @Test
   public void testTripleNeg() {
      testTripleStatus( TRIPLE_NEG_SENTENCES, StatusValue.NEGATIVE );
   }
}
