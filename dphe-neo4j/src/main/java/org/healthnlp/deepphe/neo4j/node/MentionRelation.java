package org.healthnlp.deepphe.neo4j.node;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/9/2020
 */
public class MentionRelation {
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

   public void setSourceId( final String sourceId ) {
      this.sourceId = sourceId;
   }

   public String getTargetId() {
      return targetId;
   }

   public void setTargetId( final String targetId ) {
      this.targetId = targetId;
   }

}
