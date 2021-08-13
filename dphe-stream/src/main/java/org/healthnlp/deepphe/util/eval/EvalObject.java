//package org.healthnlp.deepphe.util.eval;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 5/23/2019
// */
//interface EvalObject {
//
//   double getTP();
//
//   double getTN();
//
//   double getFP();
//
//   double getFN();
//
//   double getTP_();
//
//   double getTN_();
//
//   double getFP_();
//
//   double getFN_();
//
//   double calcTP();
//
//   double calcTN();
//
//   double calcFP();
//
//   double calcFN();
//
//   double calcTP_();
//
//   double calcTN_();
//
//   double calcFP_();
//
//   double calcFN_();
//
//   double getFullScore();
//
//   default double getP() {
//      return EvalMath.getPRS( getTP(), getFP() );
//   }
//
//   default double getR() {
//      return EvalMath.getPRS( getTP(), getFN() );
//   }
//
//   default double getS() {
//      return EvalMath.getPRS( getTN(), getFP() );
//   }
//
//   default double getN() {
//      return EvalMath.getPRS( getTN(), getFN() );
//   }
//
//   default double getP_s() {
//      return EvalMath.getPRS( getTP_(), getFP_() );
//   }
//
//   default double getP_j() {
//      return EvalMath.getPRS( getTP_(), getFP() );
//   }
//
//   default double getR_s() {
//      return EvalMath.getPRS( getTP_(), getFN_() );
//   }
//
//   default double getR_j() {
//      return EvalMath.getPRS( getTP_(), getFN() );
//   }
//
//   // Specificity
//   default double getS_s() {
//      return EvalMath.getPRS( getTN_(), getFP_() );
//   }
//
//   // Full Negate
//   default double getN_s() {
//      return EvalMath.getPRS( getTN_(), getFN_() );
//   }
//
//   default double getF1() {
//      return EvalMath.getF1( getP(), getR(), getS(), getN() );
//   }
//
//   default double getF1_s() {
//      return EvalMath.getF1( getP_s(), getR_s(), getS_s(), getN_s() );
//   }
//
//   default double getF1_j() {
//      return EvalMath.getF1( getP_j(), getR_j(), 1, 1 );
//   }
//
//   default double getAccuracy() {
//      return EvalMath.getAccuracy( getTP(), getTN(), getFP(), getFN() );
//   }
//
//   default double getAccuracy_() {
//      return EvalMath.getAccuracy( getTP_(), getTN_(), getFP_(), getFN_() );
//   }
//
//   default double getAudit() {
//      return EvalMath.getAudit( getR(), getS() );
//   }
//
//}
