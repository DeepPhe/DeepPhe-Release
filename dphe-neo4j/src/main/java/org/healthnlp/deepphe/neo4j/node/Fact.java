package org.healthnlp.deepphe.neo4j.node;

import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {4/30/2021}
 */
public class Fact {

   private String id;
   private String classUri;

   private String name;
   private String value;

   private Map<String,List<String>> relatedFactIds;

   private List<String> directEvidenceIds;

   private List<Integer> confidenceFeatures;
   private Integer confidence;

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

   public String getName() {
      return name;
   }

   public void setName( final String name ) {
      this.name = name;
   }

   public String getValue() {
      return value;
   }

   public void setValue( final String value ) {
      this.value = value;
   }

   public Map<String,List<String>> getRelatedFactIds() {
      return relatedFactIds;
   }

   public void setRelatedFactIds( final Map<String,List<String>> relatedFactIds ) {
      this.relatedFactIds = relatedFactIds;
   }

   public List<String> getDirectEvidenceIds() {
      return directEvidenceIds;
   }

   public void setDirectEvidenceIds( final List<String> directEvidenceIds ) {
      this.directEvidenceIds = directEvidenceIds;
   }

   public List<Integer> getConfidenceFeatures() {
      return confidenceFeatures;
   }

   public void setConfidenceFeatures( final List<Integer> confidenceFeatures ) {
      this.confidenceFeatures = confidenceFeatures;
   }

   public Integer getConfidence() {
      return confidence;
   }

   public void setConfidence( final Integer confidence ) {
      this.confidence = confidence;
   }

}
