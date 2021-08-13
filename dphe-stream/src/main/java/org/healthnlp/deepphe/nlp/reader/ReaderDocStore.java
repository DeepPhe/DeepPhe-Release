package org.healthnlp.deepphe.nlp.reader;


import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/6/2020
 */
public class ReaderDocStore {

   static private final Logger LOGGER = Logger.getLogger( "ReaderDocStore" );

   final private List<ReaderDoc> _readerDocs = new ArrayList<>();
   final private Map<String, Integer> _patientDocCounts = new HashMap<>();
   private int _currentIndex = -1;


   /**
    * @param index -
    * @return the doc at index.
    */
   public ReaderDoc getDoc( final int index ) {
      return _readerDocs.get( index );
   }

   /**
    * @return the number of docs in the collection.
    */
   public int getDocCount() {
      return _readerDocs.size();
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


   public Map<String, Integer> getPatientDocCounts() {
      return _patientDocCounts;
   }


}
