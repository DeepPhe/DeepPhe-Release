package org.healthnlp.deepphe.neo4j.node;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/8/2020
 */
public class Mention {
   private String id;
   private String classUri;
   private String noteId;
   private String noteType;
   private int begin;
   private int end;
   private boolean negated;
   private boolean uncertain;
   private boolean generic;
   private boolean conditional;
   private boolean historic;
   private String temporality;

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

   public String getNoteId() {
      return noteId;
   }

   public void setNoteId( final String noteId ) {
      this.noteId = noteId;
   }

   public String getNoteType() {
      return noteType;
   }

   public void setNoteType( final String noteType ) {
      this.noteType = noteType;
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

   public boolean isNegated() {
      return negated;
   }

   public void setNegated( final boolean negated ) {
      this.negated = negated;
   }

   public boolean isUncertain() {
      return uncertain;
   }

   public void setUncertain( final boolean uncertain ) {
      this.uncertain = uncertain;
   }

   public boolean isGeneric() {
      return generic;
   }

   public void setGeneric( final boolean generic ) {
      this.generic = generic;
   }

   public boolean isConditional() {
      return conditional;
   }

   public void setConditional( final boolean conditional ) {
      this.conditional = conditional;
   }

   public boolean isHistoric() {
      return historic;
   }

   public void setHistoric( final boolean historic ) {
      this.historic = historic;
   }

   public String getTemporality() {
      return temporality;
   }

   public void setTemporality( final String temporality ) {
      this.temporality = temporality;
   }

}
