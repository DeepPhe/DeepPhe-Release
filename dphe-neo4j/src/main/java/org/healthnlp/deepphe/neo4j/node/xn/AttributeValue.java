package org.healthnlp.deepphe.neo4j.node.xn;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {1/2/2024}
 */
public class AttributeValue extends InfoNode {
//      implements GroupConfidenceOwner {

   private String value;
   private List<String> conceptIds;
//   transient private double _groupedConfidence;

   public String getValue() {
      return value;
   }

   public void setValue( final String value ) {
      this.value = value;
   }

   public List<String> getConceptIds() {
      return conceptIds;
   }

   public void setConceptIds(final List<String> conceptIds) {
      this.conceptIds = conceptIds;
   }

//   public double getGroupedConfidence() {
//      return _groupedConfidence;
//   }
//
//   public void setGroupedConfidence( final double groupedConfidence ) {
//      _groupedConfidence = groupedConfidence;
//   }

}
