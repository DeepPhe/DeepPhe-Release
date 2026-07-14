package org.healthnlp.deepphe.neo4j.node;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/9/2020
 */
public class MentionCoref {
   private String id;
   private String[] idChain;

   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }

   public String[] getIdChain() {
      return idChain;
   }

   public void setIdChain( final String[] idChain ) {
      this.idChain = idChain;
   }


}
