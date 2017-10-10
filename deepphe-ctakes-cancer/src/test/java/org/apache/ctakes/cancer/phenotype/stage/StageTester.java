//package org.apache.ctakes.cancer.phenotype.stage;
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
// * @since 8/21/2015
// */
//public class StageTester {
//
//   // TODO add some decent tests
//
//   static private final Logger LOGGER = Logger.getLogger( "StageTester" );
//
//   static private final String MULTIPLE_MENTION_SENTENCE = "Patient has stage 1a liver cancer and StageIV breast cancer.";
//
//
//   @Test
//   public void testMultipleMention() {
//      final List<Stage> stages = StageFinder.findStages( MULTIPLE_MENTION_SENTENCE );
//      assertEquals( "Expect two Cancer Stages in " + MULTIPLE_MENTION_SENTENCE, 2, stages.size() );
//      StageValue value = stages.get( 0 ).getSpannedValue().getValue();
//      assertEquals( "Expected " + StageValue.I_A.getTitle() + " in " + MULTIPLE_MENTION_SENTENCE,
//            StageValue.I_A.getTitle(), value.getTitle() );
//      value = stages.get( 1 ).getSpannedValue().getValue();
//      assertEquals( "Expected " + StageValue.IV.getTitle() + " in " + MULTIPLE_MENTION_SENTENCE,
//            StageValue.IV.getTitle(), value.getTitle() );
//   }
//
//}
