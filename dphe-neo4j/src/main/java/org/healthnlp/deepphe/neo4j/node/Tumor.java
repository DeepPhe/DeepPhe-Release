package org.healthnlp.deepphe.neo4j.node;

import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {5/6/2021}
 */
public class Tumor {

   private String id;
   private String classUri;
   private List<String> factIds;
   private Map<String,List<String>> relatedFactIds;
   private List<NeoplasmAttribute> attributes;


   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }

   public String getClassUri() {
      return classUri;
   }

   public void setClassUri( final String classUri ) {
      this.classUri = classUri;
   }

   public List<String> getFactIds() {
      return factIds;
   }

   public void setFactIds( final List<String> factIds ) {
      this.factIds = factIds;
   }

   public Map<String,List<String>> getRelatedFactIds() {
      return relatedFactIds;
   }

   public void setRelatedFactIds( final Map<String,List<String>> relatedFactIds ) {
      this.relatedFactIds = relatedFactIds;
   }

   public List<NeoplasmAttribute> getAttributes() {
      return attributes;
   }

   public void setAttributes( final List<NeoplasmAttribute> attributes ) {
      this.attributes = attributes;
   }


}
