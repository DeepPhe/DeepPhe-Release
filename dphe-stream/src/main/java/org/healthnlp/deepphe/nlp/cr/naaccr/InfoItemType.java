package org.healthnlp.deepphe.nlp.cr.naaccr;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
public enum InfoItemType implements NaaccrItemType {
   // Patient
   PATIENT_ID( "patientIdNumber" ),

   // Tumor
   CLINIC_DOC_DATE( "dateCaseReportExported" ),
   //   TUMOR_NUMBER( "tumorRecordNumber" ),
   PATH_DOC_DATE( "dateEpathMessage" ),
   DOCUMENT_ID( "recordDocumentId" ),

   UNKNOWN( "UNKNOWN" );


   private final String _id;

   InfoItemType( final String id ) {
      _id = id;
   }

   public boolean shouldParse() {
      return UNKNOWN != this;
   }

   static public InfoItemType getItemType( final String id ) {
      for ( InfoItemType itemType : values() ) {
         if ( itemType._id.equals( id ) ) {
            return itemType;
         }
      }
      return UNKNOWN;
   }

}
