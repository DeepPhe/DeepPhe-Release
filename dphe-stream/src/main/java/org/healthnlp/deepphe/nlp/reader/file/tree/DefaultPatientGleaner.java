package org.healthnlp.deepphe.nlp.reader.file.tree;

import org.healthnlp.deepphe.nlp.reader.file.Doc;
import org.healthnlp.deepphe.nlp.reader.file.DocStore;
import org.healthnlp.deepphe.nlp.reader.file.PatientGleaner;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2020
 */
public class DefaultPatientGleaner implements PatientGleaner {

   final private DocStore _docStore;

   public DefaultPatientGleaner( final DocStore docStore ) {
      _docStore = docStore;
   }

   public int getPatientCount() {
      return StreamSupport.stream( _docStore.spliterator(), false )
                          .collect( Collectors.groupingBy( Doc::getPatientId ) )
                          .size();
   }


}
