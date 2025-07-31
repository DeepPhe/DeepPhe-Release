package org.healthnlp.deepphe.neo4j.node.xn;

/**
 * @author SPF , chip-nlp
 * @since {3/14/2024}
 */
public interface GroupConfidenceOwner {

   double getGroupedConfidence();

   void setGroupedConfidence( final double groupedConfidence );

   }
