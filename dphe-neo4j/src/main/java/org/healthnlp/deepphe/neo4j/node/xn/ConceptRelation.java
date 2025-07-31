package org.healthnlp.deepphe.neo4j.node.xn;

/**
 * @author SPF , chip-nlp
 * @since {1/11/2024}
 */
public class ConceptRelation extends ConfidenceOwner {

   private String type;
   private String sourceId;
   private String targetId;

   public String getType() {
      return type;
   }

   public void setType( final String type ) {
      this.type = type;
   }

   public String getSourceId() {
      return sourceId;
   }

   public void setSourceId(final String sourceId) {
      this.sourceId = sourceId;
   }

   public String getTargetId() {
      return targetId;
   }

   public void setTargetId(final String targetId) {
      this.targetId = targetId;
   }

}
