package org.healthnlp.deepphe.neo4j.node;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/18/2020
 */
public class Section {
   private String id;
   private String type;
   private int begin;
   private int end;

   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }

   public String getType() {
      return type;
   }

   public void setType( final String type ) {
      this.type = type;
   }

   public int getBegin() {
      return begin;
   }

   public void setBegin( final int begin ) {
      this.begin = begin;
   }

   public int getEnd() {
      return end;
   }

   public void setEnd( final int end ) {
      this.end = end;
   }

}
