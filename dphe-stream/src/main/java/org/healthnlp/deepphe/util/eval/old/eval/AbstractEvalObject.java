package org.healthnlp.deepphe.util.eval.old.eval;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
abstract class AbstractEvalObject implements EvalObject {
   private double _tp = -1;
   private double _tn = -1;
   private double _fp = -1;
   private double _fn = -1;
   private double _tp_ = -1;
   private double _tn_ = -1;
   private double _fp_ = -1;
   private double _fn_ = -1;

   final public double getTP() {
      if ( _tp < 0 ) {
         _tp = calcTP();
      }
      return _tp;
   }

   final public double getTN() {
      if ( _tn < 0 ) {
         _tn = calcTN();
      }
      return _tn;
   }

   final public double getFP() {
      if ( _fp < 0 ) {
         _fp = calcFP();
      }
      return _fp;
   }

   final public double getFN() {
      if ( _fn < 0 ) {
         _fn = calcFN();
      }
      return _fn;
   }

   final public double getTP_() {
      if ( _tp_ < 0 ) {
         _tp_ = calcTP_();
      }
      return _tp_;
   }

   final public double getTN_() {
      if ( _tn_ < 0 ) {
         _tn_ = calcTN_();
      }
      return _tn_;
   }

   final public double getFP_() {
      if ( _fp_ < 0 ) {
         _fp_ = calcFP_();
      }
      return _fp_;
   }

   final public double getFN_() {
      if ( _fn_ < 0 ) {
         _fn_ = calcFN_();
      }
      return _fn_;
   }

}
