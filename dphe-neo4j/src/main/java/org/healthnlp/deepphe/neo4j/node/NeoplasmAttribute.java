package org.healthnlp.deepphe.neo4j.node;

import java.util.List;

public class NeoplasmAttribute {

   private String id;
   private String classUri;

   private String name;
   private String value;

   private List<Mention> directEvidence;
   private List<Mention> indirectEvidence;
   private List<Mention> notEvidence;

   private List<Integer> confidenceFeatures;
   private Integer confidence;

   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }
   public String getName() {
      return name;
   }

   public String getClassUri() {
      return classUri;
   }

   public void setClassUri( final String classUri ) {
      this.classUri = classUri;
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

   public List<Mention> getDirectEvidence() {
      return directEvidence;
   }

   public void setDirectEvidence( final List<Mention> directEvidence ) {
      this.directEvidence = directEvidence;
   }

   public List<Mention> getIndirectEvidence() {
      return indirectEvidence;
   }

   public void setIndirectEvidence( final List<Mention> indirectEvidence ) {
      this.indirectEvidence = indirectEvidence;
   }

   public List<Mention> getNotEvidence() {
      return notEvidence;
   }

   public void setNotEvidence( final List<Mention> notEvidence ) {
      this.notEvidence = notEvidence;
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
