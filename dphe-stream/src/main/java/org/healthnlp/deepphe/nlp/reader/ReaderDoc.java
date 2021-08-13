package org.healthnlp.deepphe.nlp.reader;


import org.apache.ctakes.core.util.doc.JCasBuilder;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/11/2020
 */
public interface ReaderDoc {

   String getDocId();

   String getDocIdPrefix();

   String getDocType();

   String getDocTime();

   String getPatientId();

   String getDocUrl();

   default JCasBuilder getJCasBuilder() {
      return new JCasBuilder()
            .setDocId( getDocId() )
            .setDocIdPrefix( getDocIdPrefix() )
            .setDocType( getDocType() )
            .setDocTime( getDocTime() )
            .setPatientId( getPatientId() )
            .setDocPath( getDocUrl() );
   }

}
