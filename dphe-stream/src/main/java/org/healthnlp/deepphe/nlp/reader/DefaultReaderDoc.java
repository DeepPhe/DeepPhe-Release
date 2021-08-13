package org.healthnlp.deepphe.nlp.reader;


import org.apache.ctakes.core.util.doc.NoteSpecs;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/11/2020
 */
public class DefaultReaderDoc implements ReaderDoc {

   //   For compatibility with sql db : Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );

   final private File _file;
   final private File _rootDir;
   final private Collection<String> _validExtensions;
   final private String _patientId;

   public DefaultReaderDoc( final File file,
                            final File rootDir,
                            final Collection<String> validExtensions,
                            final String patientId ) {
      _file = file;
      _rootDir = rootDir;
      _validExtensions = validExtensions;
      _patientId = patientId;
   }


   /**
    * @return the file name with the longest valid extension removed
    */
   @Override
   public String getDocId() {
      final String fileName = _file.getName();
      String maxExtension = "";
      for ( String extension : _validExtensions ) {
         if ( fileName.endsWith( extension ) && extension.length() > maxExtension.length() ) {
            maxExtension = extension;
         }
      }
      int lastDot = fileName.lastIndexOf( '.' );
      if ( !maxExtension.isEmpty() ) {
         lastDot = fileName.length() - maxExtension.length();
      }
      if ( lastDot < 0 ) {
         return fileName;
      }
      return fileName.substring( 0, lastDot );
   }

   /**
    * @return the subdirectory path between the root directory and the file
    */
   @Override
   public String getDocIdPrefix() {
      final String parentPath = _file.getParent();
      final String rootPath = _rootDir.getPath();
      if ( parentPath.equals( rootPath ) || !parentPath.startsWith( rootPath ) ) {
         return "";
      }
      return parentPath.substring( rootPath.length() + 1 );
   }

   /**
    * @return the file name with the longest valid extension removed
    */
   @Override
   public String getDocType() {
      final String docId = getDocId();
      final int lastScore = docId.lastIndexOf( '_' );
      if ( lastScore < 0 || lastScore == docId.length() - 1 ) {
         return NoteSpecs.ID_NAME_CLINICAL_NOTE;
      }
      return docId.substring( lastScore + 1 );
   }

   /**
    * @return the file's last modification date as a string
    */
   @Override
   public String getDocTime() {
      final long millis = _file.lastModified();
      return getDateFormat().format( millis );
   }

   @Override
   public String getPatientId() {
      return _patientId;
   }

   @Override
   public String getDocUrl() {
      return _file.getAbsolutePath();
   }

   public DateFormat getDateFormat() {
      return DATE_FORMAT;
   }

}
