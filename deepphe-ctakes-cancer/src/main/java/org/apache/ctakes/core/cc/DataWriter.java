package org.apache.ctakes.core.cc;

import org.apache.uima.jcas.JCas;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/10/2019
 */
public interface DataWriter<T> {

   void createData( JCas jCas );

   T getData();

   default T createAndGetData( final JCas jCas ) {
      createData( jCas );
      return getData();
   }

   void write( T data );

   void writeComplete( T data );

}
