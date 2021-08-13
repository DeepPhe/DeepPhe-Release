package org.healthnlp.deepphe.util.eval.old.eval;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
final public class EvalMath {

   private EvalMath() {
   }

   static public double getPRS( final double tp, final double other ) {
      if ( tp == 0 && other == 0 ) {
         return 0;
      }
      return tp / (tp + other);
   }

   static public double getF1( final double p, final double r, final double s, final double n ) {
      if ( p == 0 && r == 0 ) {
         if ( s == 1 && n == 1 ) {
            return 1;
         }
         return 0;
      }
      return 2 * p * r / (p + r);
   }

   static public double getAccuracy( final double tp, final double tn, final double fp, final double fn ) {
      return (tp + tn) / (tp + tn + fp + fn);

   }

   static public double getAudit( final double r, final double s ) {
      if ( r == 0 && s == 1 ) {
         return 1;
      }
      return 2 * r / (1 + r);
   }

}
