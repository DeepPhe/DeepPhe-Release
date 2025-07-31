package org.healthnlp.deepphe.neo4j.node.xn;


import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {11/17/2023}
 */
public class AttributeXn {

   private String name;
   private String id;
   private List<AttributeValue> values;

   public String getName() {
      return name;
   }

   public void setName( final String name ) {
      this.name = name;
   }

   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }

   public List<AttributeValue> getValues() {
      return values;
   }

   public void setValues( final List<AttributeValue> values ) {
      this.values = values;
   }

}
