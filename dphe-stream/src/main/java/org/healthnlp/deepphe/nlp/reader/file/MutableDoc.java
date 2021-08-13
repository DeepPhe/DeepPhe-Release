package org.healthnlp.deepphe.nlp.reader.file;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/19/2020
 */
public class MutableDoc implements Doc {

   private String _docId;
   private String _docIdPrefix;
   private String _docType;
   private String _docTime;
   private String _patientId;
   private String _docUrl;
   private String _docText;

   public String getDocId() {
      return _docId;
   }

   public void setDocId( final String docId ) {
      _docId = docId;
   }

   public String getDocIdPrefix() {
      return _docIdPrefix;
   }

   public void setDocIdPrefix( final String docIdPrefix ) {
      _docIdPrefix = docIdPrefix;
   }

   public String getDocType() {
      return _docType;
   }

   public void setDocType( final String docType ) {
      _docType = docType;
   }

   public String getDocTime() {
      return _docTime;
   }

   public void setDocTime( final String docTime ) {
      _docTime = docTime;
   }

   public String getPatientId() {
      return _patientId;
   }

   public void setPatientId( final String patientId ) {
      _patientId = patientId;
   }

   public String getDocUrl() {
      return _docUrl;
   }

   public void setDocUrl( final String docUrl ) {
      _docUrl = docUrl;
   }

   public String getDocText() {
      return _docText;
   }

   public void setDocText( final String docText ) {
      _docText = docText;
   }


}
