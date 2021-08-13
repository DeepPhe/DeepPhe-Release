package org.healthnlp.deepphe.neo4j.node;


import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
public class PatientSummaryXn {

   private String id;
   private Patient patient;
   private List<Cancer> cancers;
   private List<Fact> facts;

   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }

   public Patient getPatient() {
      return patient;
   }

   public void setPatient( final Patient patient ) {
      this.patient = patient;
   }

   public List<Cancer> getCancers() {
      return cancers;
   }

   public void setCancers( final List<Cancer> cancers ) {
      this.cancers = cancers;
   }

   public List<Fact> getFacts() {
      return facts;
   }

   public void setFacts( final List<Fact> facts ) {
      this.facts = facts;
   }


}
