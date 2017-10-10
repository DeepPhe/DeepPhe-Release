//package org.apache.ctakes.cancer.location;
//
//import org.apache.log4j.Logger;
//import org.junit.Test;
//
//import java.util.List;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 5/16/2016
// */
//public class ModifierFinderTester {
//
//   static private final Logger LOGGER = Logger.getLogger( "ModifierFinderTester" );
//
//   static private final String[] LEFT_SENTENCES = { "Tumor on the left breast.", "Tumor: breast, left",
//                                                    "Tumor: breast, left.", "Tumor levo mastis.",
//                                                    "Breast (left)", "Breast (lt)." };
//
//   static private final String[] RIGHT_SENTENCES = { "Tumor on the right breast.", "Tumor: breast, right",
//                                                     "Tumor: breast, right.", "Tumor dextro mastis.",
//                                                     "Breast (right)", "Breast (rt)." };
//
//   static private final String[] NON_SENTENCES = { "The nasty piratest fired on our port side.",
//                                                   "There is no escaping the machinations of that Duddley Doright this night.",
//                                                   "Halt!",
//                                                   "Harry shouted \"Spectro Leftonum!\" and the creature laughed.",
//                                                   "My favorite G.I. Joey was that weapons buyer Dextros." };
//
//
//   static private final String[] UPPER_INNER_SENTENCES = { "Tumor in the upper inner quadrant",
//                                                           "Tumor in the upper-inner quadrant",
//                                                           "Tumor in the inner upper quadrant",
//                                                           "Tumor in the inner-upper quadrant",
//                                                           "Tumor in the inner-upper quadrant.",
//                                                           "Tumor at breast upper-inner area." };
//   static private final int[] UPPER_INNER_BEGINS = { 13, 13, 13, 13, 13, 16 };
//   static private final int[] UPPER_INNER_ENDS = { 33, 33, 33, 33, 33, 27 };
//
//
//   static private final String[] UPPER_LOWER_INNER_SENTENCES = { "Tumor at breast upper/lower inner quadrant.",
//                                                                 "Tumor at breast upper/lower-inner quadrant." };
//   static private final int[] UPPER_LOWER_INNER_BEGINS = { 16, 16 };
//   static private final int[] UPPER_LOWER_INNER_ENDS = { 42, 42 };
//
//
//   static private final String[] UPPER_LOWER_OUTER_SENTENCES = { "Tumor at breast upper/lower outer quadrant.",
//                                                                 "Tumor at breast upper/lower-outer quadrant." };
//   static private final int[] UPPER_LOWER_OUTER_BEGINS = { 16, 16 };
//   static private final int[] UPPER_LOWER_OUTER_ENDS = { 42, 42 };
//
//
//
//   static private final String[] LOWER_INNER_SENTENCES = { "Tumor in the lower inner quadrant",
//                                                           "Tumor in the lower-inner quadrant",
//                                                           "Tumor in the inner lower quadrant",
//                                                           "Tumor in the inner-lower quadrant",
//                                                           "Tumor at breast lower-inner area." };
//   static private final int[] LOWER_INNER_BEGINS = { 13, 13, 13, 13, 16 };
//   static private final int[] LOWER_INNER_ENDS = { 33, 33, 33, 33, 27 };
//
//   static private final String[] UPPER_OUTER_SENTENCES = { "Tumor in the upper outer quadrant",
//                                                           "Tumor in the upper-outer quadrant",
//                                                           "Tumor in the outer upper quadrant",
//                                                           "Tumor in the outer-upper quadrant",
//                                                           "Tumor at breast upper-outer area." };
//   static private final int[] UPPER_OUTER_BEGINS = { 13, 13, 13, 13, 16 };
//   static private final int[] UPPER_OUTER_ENDS = { 33, 33, 33, 33, 27 };
//
//   static private final String[] LOWER_OUTER_SENTENCES = { "Tumor in the lower outer quadrant",
//                                                           "Tumor in the lower-outer quadrant",
//                                                           "Tumor in the outer lower quadrant",
//                                                           "Tumor in the outer-lower quadrant",
//                                                           "Tumor at breast lower-outer area." };
//   static private final int[] LOWER_OUTER_BEGINS = { 13, 13, 13, 13, 16 };
//   static private final int[] LOWER_OUTER_ENDS = { 33, 33, 33, 33, 27 };
//
//   static private final String LEFT_SENTENCE = "Patient has left breast cancer.";
//   static private final String RIGHT_SENTENCE = "Patient has right breast cancer.";
//   static private final String BILATERAL_SENTENCE = "Patient has bilateral breast cancer.";
//   static private final String BOTH_SENTENCE = "Patient has breast cancer on both sides.";
//   static private final String LEFT_RIGHT_SENTENCE = "Patient has left and right breast cancer.";
//
//
//   static private final String[] ONE_OCLOCK_SENTENCES = {// "Patient has tumor at left breast, 1 o'clock.",
//                                                          "Patient has tumor at left breast, 1 o'clock position.",
//                                                          "Patient has tumor at left breast, 1 o' clock position.",
//                                                          "Patient has tumor at left breast, 1 o clock position.",
//                                                          "Patient has tumor at left breast, 1 oclock position." };
//   //   static private final int[] ONE_OCLOCK_BEGINS = { 34, 34, 34, 34, 34 };
////   static private final int[] ONE_OCLOCK_ENDS = { 43, 52, 53, 52, 51 };
//   static private final int[] ONE_OCLOCK_BEGINS = { 34, 34, 34, 34 };
//   static private final int[] ONE_OCLOCK_ENDS = { 52, 53, 52, 51 };
//
//   static private final String[] ONE_THIRTY_SENTENCES = { //"Patient has tumor at left breast, 1:30 o'clock.",
//                                                          "Patient has tumor at left breast, 1.30 o'clock position.",
//                                                          "Patient has tumor at left breast, 1:30 o' clock position.",
//                                                          "Patient has tumor at left breast, 1.30 o clock position.",
//                                                          "Patient has tumor at left breast, 1:30 oclock position." };
//   //   static private final int[] ONE_THIRTY_BEGINS = { 34, 34, 34, 34, 34 };
////   static private final int[] ONE_THIRTY_ENDS = { 46, 55, 56, 55, 54 };
//   static private final int[] ONE_THIRTY_BEGINS = { 34, 34, 34, 34 };
//   static private final int[] ONE_THIRTY_ENDS = { 55, 56, 55, 54 };
//
//   @Test
//   public void testLeft() {
//      for ( int i = 0; i < LEFT_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> sides = ModifierFinder
//               .findModifiers( LEFT_SENTENCES[ i ], LocationModifier.BodySide.values() );
//         assertEquals( "Expect one side in " + LEFT_SENTENCES[ i ], 1, sides.size() );
//         assertEquals( "Expect left side in " + LEFT_SENTENCES[ i ],
//               LocationModifier.BodySide.LEFT.getUri(),
//               sides.get( 0 ).getModifier().getUri() );
//      }
//   }
//
//   @Test
//   public void testRight() {
//      for ( int i = 0; i < RIGHT_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> sides = ModifierFinder
//               .findModifiers( RIGHT_SENTENCES[ i ], LocationModifier.BodySide.values() );
//         assertEquals( "Expect one side in " + RIGHT_SENTENCES[ i ], 1, sides.size() );
//         assertEquals( "Expect left side in " + RIGHT_SENTENCES[ i ],
//               LocationModifier.BodySide.RIGHT.getUri(),
//               sides.get( 0 ).getModifier().getUri() );
//      }
//   }
//
//   @Test
//   public void testNoSide() {
//      for ( String sentence : NON_SENTENCES ) {
//         final List<SpannedModifier> sides = ModifierFinder
//               .findModifiers( sentence, LocationModifier.BodySide.values() );
//         assertTrue( "Expect no side in " + sentence, sides.isEmpty() );
//      }
//   }
//
//   @Test
//   public void testOneQuadrant() {
//      for ( int i = 0; i < UPPER_INNER_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> quadrants = ModifierFinder
//               .findModifiers( UPPER_INNER_SENTENCES[ i ], LocationModifier.Quadrant.values() );
//         assertEquals( "Expect one quadrant in " + UPPER_INNER_SENTENCES[ i ], 1, quadrants.size() );
//         testModifier( quadrants
//               .get( 0 ), UPPER_INNER_SENTENCES[ i ], UPPER_INNER_BEGINS[ i ], UPPER_INNER_ENDS[ i ], LocationModifier.Quadrant.UPPER_INNER );
//      }
//      for ( int i = 0; i < LOWER_INNER_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> quadrants = ModifierFinder
//               .findModifiers( LOWER_INNER_SENTENCES[ i ], LocationModifier.Quadrant.values() );
//         assertEquals( "Expect one quadrant in " + LOWER_INNER_SENTENCES[ i ], 1, quadrants.size() );
//         testModifier( quadrants
//               .get( 0 ), LOWER_INNER_SENTENCES[ i ], LOWER_INNER_BEGINS[ i ], LOWER_INNER_ENDS[ i ], LocationModifier.Quadrant.LOWER_INNER );
//      }
//      for ( int i = 0; i < UPPER_OUTER_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> quadrants = ModifierFinder
//               .findModifiers( UPPER_OUTER_SENTENCES[ i ], LocationModifier.Quadrant.values() );
//         assertEquals( "Expect one quadrant in " + UPPER_OUTER_SENTENCES[ i ], 1, quadrants.size() );
//         testModifier( quadrants
//               .get( 0 ), UPPER_OUTER_SENTENCES[ i ], UPPER_OUTER_BEGINS[ i ], UPPER_OUTER_ENDS[ i ], LocationModifier.Quadrant.UPPER_OUTER );
//      }
//      for ( int i = 0; i < LOWER_OUTER_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> quadrants = ModifierFinder
//               .findModifiers( LOWER_OUTER_SENTENCES[ i ], LocationModifier.Quadrant.values() );
//         assertEquals( "Expect one quadrant in " + LOWER_OUTER_SENTENCES[ i ], 1, quadrants.size() );
//         testModifier( quadrants
//               .get( 0 ), LOWER_OUTER_SENTENCES[ i ], LOWER_OUTER_BEGINS[ i ], LOWER_OUTER_ENDS[ i ], LocationModifier.Quadrant.LOWER_OUTER );
//      }
//   }
//
//   @Test
//   public void testTwoQuadrants() {
//      for ( int i = 0; i < UPPER_LOWER_INNER_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> quadrants = ModifierFinder
//               .findModifiers( UPPER_LOWER_INNER_SENTENCES[ i ], LocationModifier.Quadrant.values() );
//         assertEquals( "Expect two quadrants in " + UPPER_LOWER_INNER_SENTENCES[ i ], 2, quadrants.size() );
//         testModifier( quadrants
//               .get( 0 ), UPPER_LOWER_INNER_SENTENCES[ i ], UPPER_LOWER_INNER_BEGINS[ i ], UPPER_LOWER_INNER_ENDS[ i ], LocationModifier.Quadrant.UPPER_INNER );
//         testModifier( quadrants
//               .get( 1 ), UPPER_LOWER_INNER_SENTENCES[ i ],
//               UPPER_LOWER_INNER_BEGINS[ i ] + 6, UPPER_LOWER_INNER_ENDS[ i ], LocationModifier.Quadrant.LOWER_INNER );
//      }
//      for ( int i = 0; i < UPPER_LOWER_OUTER_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> quadrants = ModifierFinder
//               .findModifiers( UPPER_LOWER_OUTER_SENTENCES[ i ], LocationModifier.Quadrant.values() );
//         assertEquals( "Expect two quadrants in " + UPPER_LOWER_OUTER_SENTENCES[ i ], 2, quadrants.size() );
//         testModifier( quadrants
//               .get( 0 ), UPPER_LOWER_OUTER_SENTENCES[ i ], UPPER_LOWER_OUTER_BEGINS[ i ], UPPER_LOWER_OUTER_ENDS[ i ], LocationModifier.Quadrant.UPPER_OUTER );
//         testModifier( quadrants
//               .get( 1 ), UPPER_LOWER_OUTER_SENTENCES[ i ],
//               UPPER_LOWER_OUTER_BEGINS[ i ] + 6, UPPER_LOWER_OUTER_ENDS[ i ], LocationModifier.Quadrant.LOWER_OUTER );
//      }
//   }
//
//
//   @Test
//   public void testBodySides() {
//      final List<SpannedModifier> lefts = ModifierFinder.findBodySides( LEFT_SENTENCE );
//      assertEquals( "Expect one side in " + LEFT_SENTENCE, 1, lefts.size() );
//      testModifier( lefts.get( 0 ), LEFT_SENTENCE, 12, 16, LocationModifier.BodySide.LEFT );
//      final List<SpannedModifier> rights = ModifierFinder.findBodySides( RIGHT_SENTENCE );
//      assertEquals( "Expect one side in " + RIGHT_SENTENCE, 1, rights.size() );
//      testModifier( rights.get( 0 ), RIGHT_SENTENCE, 12, 17, LocationModifier.BodySide.RIGHT );
//      final List<SpannedModifier> bilaterals = ModifierFinder.findBodySides( BILATERAL_SENTENCE );
//      assertEquals( "Expect one side in " + BILATERAL_SENTENCE, 1, bilaterals.size() );
//      testModifier( bilaterals.get( 0 ), BILATERAL_SENTENCE, 12, 21, LocationModifier.BodySide.BILATERAL );
//      final List<SpannedModifier> both = ModifierFinder.findBodySides( BOTH_SENTENCE );
//      assertEquals( "Expect one side in " + BOTH_SENTENCE, 1, both.size() );
//      testModifier( both.get( 0 ), BOTH_SENTENCE, 29, 39, LocationModifier.BodySide.BILATERAL );
//      final List<SpannedModifier> leftRight = ModifierFinder.findBodySides( LEFT_RIGHT_SENTENCE );
//      assertEquals( "Expect three sides in " + LEFT_RIGHT_SENTENCE, 3, leftRight.size() );
////      testModifier( leftRight.get( 0 ), LEFT_RIGHT_SENTENCE, 12, 16, LocationModifier.BodySide.LEFT );
////      testModifier( leftRight.get( 1 ), LEFT_RIGHT_SENTENCE, 21, 26, LocationModifier.BodySide.RIGHT );
//   }
//
//   @Test
//   public void testClockwise() {
//      for ( int i = 0; i < ONE_OCLOCK_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> clockwises = ModifierFinder
//               .findModifiers( ONE_OCLOCK_SENTENCES[ i ], LocationModifier.Clockwise.values() );
//         assertEquals( "Expect one quadrant in " + ONE_OCLOCK_SENTENCES[ i ], 1, clockwises.size() );
//         testModifier( clockwises
//                     .get( 0 ), ONE_OCLOCK_SENTENCES[ i ], ONE_OCLOCK_BEGINS[ i ], ONE_OCLOCK_ENDS[ i ],
//               LocationModifier.Clockwise.values()[ 0 ] );
//      }
//      for ( int i = 0; i < ONE_THIRTY_SENTENCES.length; i++ ) {
//         final List<SpannedModifier> clockwises = ModifierFinder
//               .findModifiers( ONE_THIRTY_SENTENCES[ i ], LocationModifier.Clockwise.values() );
//         assertEquals( "Expect one quadrant in " + ONE_THIRTY_SENTENCES[ i ], 1, clockwises.size() );
//         testModifier( clockwises
//                     .get( 0 ), ONE_THIRTY_SENTENCES[ i ], ONE_THIRTY_BEGINS[ i ], ONE_THIRTY_ENDS[ i ],
//               LocationModifier.Clockwise.values()[ 1 ] );
//      }
//   }
//
//
//   static private void testModifier( final SpannedModifier spannedModifier, final String sentence,
//                                     final int begin, final int end,
//                                     final LocationModifier modifier ) {
//      assertEquals( "Incorrect modifier for " + sentence, modifier.getUri(), spannedModifier.getModifier().getUri() );
//      assertEquals( "Incorrect begin offset for " + sentence, begin, spannedModifier.getStartOffset() );
//      assertEquals( "Incorrect end offset for " + sentence, end, spannedModifier.getEndOffset() );
//   }
//
//
//}
