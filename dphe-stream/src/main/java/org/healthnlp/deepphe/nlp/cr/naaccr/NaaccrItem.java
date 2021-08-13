package org.healthnlp.deepphe.nlp.cr.naaccr;

import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.uima.jcas.JCas;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
public interface NaaccrItem {

   String getId();

   void setId( String id );

   default JCasBuilder addToBuilder( JCasBuilder builder ) {
      return builder;
   }

   default void populateJCas( JCas jCas ) {
   }

}
