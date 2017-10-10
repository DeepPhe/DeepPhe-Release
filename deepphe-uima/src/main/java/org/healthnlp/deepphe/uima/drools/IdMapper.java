package org.healthnlp.deepphe.uima.drools;

public class IdMapper {
   private String oldId, newId, oldDocumantType, oldSummaryId, newSummaryId;

   public IdMapper( String oldId, String newId, String oldDocumantType, String oldSummaryId, String newSummaryId ) {
      this.oldId = oldId;
      this.newId = newId;
      this.oldDocumantType = oldDocumantType;
      this.oldSummaryId = oldSummaryId;
      this.newSummaryId = newSummaryId;
   }

   public String getOldId() {
      return oldId;
   }

   public void setOldId( String oldId ) {
      this.oldId = oldId;
   }

   public String getNewId() {
      return newId;
   }

   public void setNewId( String newId ) {
      this.newId = newId;
   }

   public String getOldDocumantType() {
      return oldDocumantType;
   }

   public void setOldDocumantType( String oldDocumantType ) {
      this.oldDocumantType = oldDocumantType;
   }

   public String getOldSummaryId() {
      return oldSummaryId;
   }

   public void setOldSummaryId( String oldSummaryId ) {
      this.oldSummaryId = oldSummaryId;
   }

   public String getNewSummaryId() {
      return newSummaryId;
   }

   public void setNewSummaryId( String newSummaryId ) {
      this.newSummaryId = newSummaryId;
   }
}
