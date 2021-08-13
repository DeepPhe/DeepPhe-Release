package org.healthnlp.deepphe.nlp.reader;


import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/6/2020
 */
public class ReaderFileStore {

   static private final Logger LOGGER = Logger.getLogger( "ReaderFileStore" );

   final private List<File> _files = new ArrayList<>();
   final private Map<File, String> _filePatients = new HashMap<>();
   final private Map<String, Integer> _patientDocCounts = new HashMap<>();
   private int _currentIndex = -1;

   /**
    * Use with care.
    *
    * @param file -
    * @return true if the file was added.
    */
   public boolean addFile( final File file ) {
      return _files.add( file );
   }

   /**
    * Convenience method to add a file and set its patient Id.
    *
    * @param file      -
    * @param patientId -
    * @return true if the file was added and the patientId is ok.
    */
   public boolean addFile( final File file, final String patientId ) {
      if ( !addFile( file ) ) {
         return false;
      }
      return setPatientId( file, patientId );
   }

   /**
    * @param index -
    * @return the file at index.
    */
   public File getFile( final int index ) {
      return _files.get( index );
   }

   /**
    * @return the number of files in the collection.
    */
   public int getFileCount() {
      return _files.size();
   }

   /**
    * @return the index of the file currently being processed.
    */
   public int getCurrentIndex() {
      return _currentIndex;
   }

   /**
    * increment the current index.
    */
   public void incrementIndex() {
      _currentIndex++;
   }

   /**
    * Use with care.
    *
    * @param index of the file currently being processed.
    * @return true if the new index is ok; the index is the current index + 1;
    */
   public boolean setCurrentIndex( final int index ) {
      if ( index == _currentIndex + 1 ) {
         incrementIndex();
         return true;
      }
      return false;
   }

   /**
    * Use with care.
    *
    * @param file -
    * @param id   -
    * @return true if the id is ok: new for the given file or the same as it was previously.
    */
   public boolean setPatientId( final File file, final String id ) {
      final String previous = _filePatients.put( file, id );
      if ( previous == null ) {
         return true;
      }
      return previous.equals( id );
   }

   /**
    * @return the patientId for that file.  By default this is the name of the directory containing the file.
    */
   public String getPatientId( final File file ) {
      return _filePatients.get( file );
   }

   public Map<String, Integer> getPatientDocCounts() {
      return _patientDocCounts;
   }


}
