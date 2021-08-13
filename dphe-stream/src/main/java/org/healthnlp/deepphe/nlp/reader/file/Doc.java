package org.healthnlp.deepphe.nlp.reader.file;

import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.uima.jcas.JCas;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/19/2020
 */
public interface Doc {

   //   For compatibility with sql db : Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
   DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );

   String getDocId();

   String getDocIdPrefix();

   String getDocType();

   String getDocTime();

   String getPatientId();

   String getDocUrl();

   String getDocText();

   default void populateCas( final JCas jCas ) {
      getJCasBuilder().rebuild( jCas );
   }

   default JCasBuilder getJCasBuilder() {
      return new JCasBuilder()
            .setDocId( getDocId() )
            .setDocIdPrefix( getDocIdPrefix() )
            .setDocType( getDocType() )
            .setDocTime( getDocTime() )
            .setPatientId( getPatientId() )
            .setDocPath( getDocUrl() )
            .setDocText( getDocText() );
   }


   Doc EMPTY_DOC = new Doc() {
      @Override
      public String getDocId() {
         return null;
      }

      @Override
      public String getDocIdPrefix() {
         return null;
      }

      @Override
      public String getDocType() {
         return null;
      }

      @Override
      public String getDocTime() {
         return null;
      }

      @Override
      public String getPatientId() {
         return null;
      }

      @Override
      public String getDocUrl() {
         return null;
      }

      @Override
      public String getDocText() {
         return null;
      }
   };


}
