//package org.healthnlp.deepphe.util.eval;
//
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 5/23/2019
// */
//final class EvalUris extends AbstractEvalObject {
//   private final String _gold;
//   private final String _system;
//
//   EvalUris( final String gold, final String system ) {
//      _gold = gold;
//      _system = system;
//   }
//
//   String getGoldPrefix() {
//      if ( getTP_() > 0 ) {
//         return "   ";
//      }
//      return (getFN_() > 0) ? "FN " : "   ";
//   }
//
//   String getSystemPrefix() {
//      if ( getTP_() > 0 ) {
//         if ( getFP_() > 0 || getFN_() > 0 ) {
//            return "MX ";
//         }
//         return "   ";
//      }
//      return (getFP_() > 0) ? "FP " : "   ";
//   }
//
//   public double calcTP() {
//      return (getTP_() > 0) ? 1 : 0;
//   }
//
//   public double calcTN() {
//      return (getTN_() > 0) ? 1 : 0;
//   }
//
//   public double calcFP() {
//      return (getFP_() == 1) ? 1 : 0;
//   }
//
//   public double calcFN() {
//      return (getFN_() == 1) ? 1 : 0;
//   }
//
//   public double calcTP_() {
//      if ( MatchUtil.isEmptyMatch( _gold, _system ) ) {
//         return 1;
//      }
//      if ( _gold == null || _gold.isEmpty() || _system == null || _system.isEmpty() ) {
//         return 0;
//      }
//      return MatchUtil.countMatched( _gold, _system );
//   }
//
//   public double calcTN_() {
//      final boolean gEmpty = _gold == null || _gold.isEmpty();
//      final boolean sEmpty = _system == null || _system.isEmpty();
//      if ( gEmpty && sEmpty ) {
//         return 1;
//      }
//      return 0;
//   }
//
//   public double calcFP_() {
//      if ( getTN_() > 0 ) {
//         return 0;
//      }
//      return MatchUtil.countUnMatched( _system, _gold );
//   }
//
//   public double calcFN_() {
//      if ( getTN_() > 0 ) {
//         return 0;
//      }
//      return MatchUtil.countUnMatched( _gold, _system );
//   }
//
//   public double getFullScore() {
//      return getF1_s();
//   }
//
//}
