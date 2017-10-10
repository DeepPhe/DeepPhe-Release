//package org.apache.ctakes.cancer.phenotype.tnm;
//
//import org.junit.Test;
//
//import java.util.List;
//import java.util.logging.Logger;
//
//import static org.junit.Assert.assertEquals;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 8/23/2015
// */
//public class TnmTester {
//
//   static private final Logger LOGGER = Logger.getLogger( "TnmTester" );
//
//   static private final String FULL_SENTENCE
//         = "Patient is a 54 year old female with history of T2N0M0 left breast cancer ER-neg, PR-neg, HER2+,"
//           + " now undergoing neoadjuvant chemo with taxotere, carboplatin, Herceptin, and pertuzumab.";
//
//   static private final String[] NO_TNM_SENTENCES = {
//         "Patient has Taken aspirin for her headache.", "The patient drives a lamborghini LXM1 and boy is it fast." };
//
//   static private final String[] PUNCTUATION_SENTENCES = {
//         "Patient's Cancer diagnosed as pT1 N1.",
//         "Test confirmed Primary T0, Metastasis M1.",
//         "Father had T1, son is worse (T3)"
//   };
//
//   static private final String[] ROMAN_SENTENCES = {
//         "Patient's Cancer diagnosed as pTI NI.",
//         "Test confirmed Primary T0, Metastasis MI.",
//         "Father had TIII, son is worse (TIV)"
//   };
//
//
//   @Test
//   public void testTnmClasses() {
//      final List<Tnm> tnms = OldTnmFinder.getTnms( FULL_SENTENCE );
//      assertEquals( "Expect 3 TNM Classes from " + FULL_SENTENCE, 3, tnms.size() );
//      testTnmTypeValue( tnms.get( 0 ), TnmType.T, "2" );
//      testTnmTypeValue( tnms.get( 1 ), TnmType.N, "0" );
//      testTnmTypeValue( tnms.get( 2 ), TnmType.M, "0" );
//   }
//
//   @Test
//   public void testNoTnm() {
//      for ( String sentence : NO_TNM_SENTENCES ) {
//         final List<Tnm> tnms = TnmFinder.getTnms( sentence );
//         assertEquals( "Expect no TNM in " + sentence, 0, tnms.size() );
//      }
//   }
//
//   @Test
//   public void testPunctuation() {
//      for ( String sentence : PUNCTUATION_SENTENCES ) {
//         final List<Tnm> tnms = OldTnmFinder.getTnms( sentence );
//         assertEquals( "Expect 2 TNM Classes from " + sentence, 2, tnms.size() );
//      }
//   }
//
//   @Test
//   public void testPunctuation1() {
//      final List<Tnm> tnms = OldTnmFinder.getTnms( PUNCTUATION_SENTENCES[ 0 ] );
//      assertEquals( "Expect 2 TNM Classes from " + PUNCTUATION_SENTENCES[ 0 ], 2, tnms.size() );
//      testTnmTypeValue( tnms.get( 0 ), TnmType.T, "1" );
//      testTnmTypeValue( tnms.get( 1 ), TnmType.N, "1" );
//      final SpannedTnmPrefix prefix = OldTnmFinder.getTnmPrefix( tnms.get( 0 ), PUNCTUATION_SENTENCES[ 0 ] );
//      assertEquals( "Expect prefix p for pT1 in " + PUNCTUATION_SENTENCES[ 0 ], TnmPrefix.P, prefix.getTest() );
//   }
//
//   @Test
//   public void testPunctuation2() {
//      final List<Tnm> tnms = OldTnmFinder.getTnms( PUNCTUATION_SENTENCES[ 1 ] );
//      assertEquals( "Expect 2 TNM Classes from " + PUNCTUATION_SENTENCES[ 1 ], 2, tnms.size() );
//      testTnmTypeValue( tnms.get( 0 ), TnmType.T, "0" );
//      testTnmTypeValue( tnms.get( 1 ), TnmType.M, "1" );
//   }
//
//   @Test
//   public void testPunctuation3() {
//      final List<Tnm> tnms = OldTnmFinder.getTnms( PUNCTUATION_SENTENCES[ 2 ] );
//      assertEquals( "Expect 2 TNM Classes from " + PUNCTUATION_SENTENCES[ 2 ], 2, tnms.size() );
//      testTnmTypeValue( tnms.get( 0 ), TnmType.T, "1" );
//      testTnmTypeValue( tnms.get( 1 ), TnmType.T, "3" );
//   }
//
//   @Test
//   public void testRoman() {
//      for ( String sentence : ROMAN_SENTENCES ) {
//         final List<Tnm> tnms = OldTnmFinder.getTnms( sentence );
//         assertEquals( "Expect 2 TNM Classes from " + sentence, 2, tnms.size() );
//      }
//   }
//
//   @Test
//   public void testRoman1() {
//      final List<Tnm> tnms = OldTnmFinder.getTnms( ROMAN_SENTENCES[ 0 ] );
//      assertEquals( "Expect 2 TNM Classes from " + ROMAN_SENTENCES[ 0 ], 2, tnms.size() );
//      testTnmTypeValue( tnms.get( 0 ), TnmType.T, "1" );
//      testTnmTypeValue( tnms.get( 1 ), TnmType.N, "1" );
//      final SpannedTnmPrefix prefix = OldTnmFinder.getTnmPrefix( tnms.get( 0 ), ROMAN_SENTENCES[ 0 ] );
//      assertEquals( "Expect prefix p for pTI in " + ROMAN_SENTENCES[ 0 ], TnmPrefix.P, prefix.getTest() );
//   }
//
//   @Test
//   public void testRoman2() {
//      final List<Tnm> tnms = OldTnmFinder.getTnms( ROMAN_SENTENCES[ 1 ] );
//      assertEquals( "Expect 2 TNM Classes from " + ROMAN_SENTENCES[ 1 ], 2, tnms.size() );
//      testTnmTypeValue( tnms.get( 0 ), TnmType.T, "0" );
//      testTnmTypeValue( tnms.get( 1 ), TnmType.M, "1" );
//   }
//
//   @Test
//   public void testRoman3() {
//      final List<Tnm> tnms = OldTnmFinder.getTnms( ROMAN_SENTENCES[ 2 ] );
//      assertEquals( "Expect 2 TNM Classes from " + ROMAN_SENTENCES[ 2 ], 2, tnms.size() );
//      testTnmTypeValue( tnms.get( 0 ), TnmType.T, "3" );
//      testTnmTypeValue( tnms.get( 1 ), TnmType.T, "4" );
//   }
//
//
////   static private void testTnmClass( final TnmClass tnmClass,
////                                     final TnmClassPrefixType expectedPrefix,
////                                     final TnmType expectedClassType,
////                                     final String expectedValue ) {
////      assertEquals( "Expecting prefix " + expectedPrefix.getCharacterCode() + " : " + expectedPrefix.getTitle(),
////            expectedPrefix, tnmClass.getPrefix() );
////      assertEquals( "Expected class type " + expectedClassType.name() + " : " + expectedClassType.getTitle(),
////            expectedClassType, tnmClass.getClassType() );
////      assertEquals( "Expected value " + expectedValue, expectedValue, tnmClass.getValue() );
////   }
//
//   static private void testTnmTypeValue( final Tnm tnm,
//                                         final TnmType expectedType,
//                                         final String expectedValueTitle ) {
//      assertEquals( "Expected TNM type " + expectedType.name() + " : " + expectedType.getTitle(),
//            expectedType, tnm.getSpannedType().getType() );
//      assertEquals( "Expected TNM value " + expectedValueTitle, expectedValueTitle, tnm.getSpannedValue().getValue()
//            .getTitle() );
//   }
//
//
//}
