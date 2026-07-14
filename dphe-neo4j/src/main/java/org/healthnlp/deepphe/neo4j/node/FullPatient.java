package org.healthnlp.deepphe.neo4j.node;


import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/8/2020
 */
public class FullPatient {
   private String id;
   private String name;
   private String gender;
   private String birth;
   private String death;
   private String firstEncounter;
   private String lastEncounter;
   private List<Note> notes;

   public String getId() {
      return id;
   }

   public void setId( final String id ) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName( final String name ) {
      this.name = name;
   }

   public String getGender() {
      return gender;
   }

   public void setGender( final String gender ) {
      this.gender = gender;
   }

   public String getBirth() {
      return birth;
   }

   public void setBirth( final String birth ) {
      this.birth = birth;
   }

   public String getDeath() {
      return death;
   }

   public void setDeath( final String death ) {
      this.death = death;
   }

   public String getFirstEncounter() {
      return firstEncounter;
   }

   public void setFirstEncounter( final String firstEncounter ) {
      this.firstEncounter = firstEncounter;
   }

   public String getLastEncounter() {
      return lastEncounter;
   }

   public void setLastEncounter( final String lastEncounter ) {
      this.lastEncounter = lastEncounter;
   }

   public List<Note> getNotes() {
      return notes;
   }

   public void setNotes( final List<Note> notes ) {
      this.notes = notes;
   }


}
