package org.healthnlp.deepphe.nlp.reader.file;

import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.nlp.reader.util.FileReaderUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/22/2020
 */
//@Immutable
final public class DefaultFileDoc implements Doc {

   final private File _file;
   final private boolean _keepCrChar;
   final private Collection<String> _validExtensions;

   public DefaultFileDoc( final File file ) {
      this( file, Collections.emptyList(), true );
   }

   public DefaultFileDoc( final File file, final Collection<String> validExtensions, final boolean keepCrChar ) {
      _file = file;
      _validExtensions = validExtensions;
      _keepCrChar = keepCrChar;
   }

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
      if ( lastDot <= 0 ) {
         return fileName;
      }
      return fileName.substring( 0, lastDot );
   }

   public String getDocIdPrefix() {
      return _file.getParentFile().getName();
   }

   public String getDocType() {
      final String docId = getDocId();
      final int lastScore = docId.lastIndexOf( '_' );
      if ( lastScore < 0 || lastScore == docId.length() - 1 ) {
         return NoteSpecs.ID_NAME_CLINICAL_NOTE;
      }
      return docId.substring( lastScore + 1 );
   }

   public String getDocTime() {
      final long millis = _file.lastModified();
      return DATE_FORMAT.format( millis );
   }

   public String getPatientId() {
      return _file.getParentFile().getName();
   }

   public String getDocUrl() {
      return _file.getAbsolutePath();
   }

   public String getDocText() {
      try {
         final String docText = FileReaderUtil.readFile( _file );
         return FileReaderUtil.handleTextEol( docText, _keepCrChar );
      } catch ( IOException ioE ) {
         Logger.getLogger( DefaultFileDoc.class )
               .error( "Could not read " + _file.getAbsolutePath(), ioE );
         return "";
      }
   }


}
