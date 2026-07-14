package org.healthnlp.deepphe.neo4j.node.xn;

/**
 * @author SPF , chip-nlp
 * @since {3/13/2024}
 */
public class ConfidenceOwner {

    private transient double _dConfidence;

   public double getdConfidence() {
      return _dConfidence;
   }

   public void setdConfidence( final double dConfidence ) {
      this._dConfidence = dConfidence;
   }

}
