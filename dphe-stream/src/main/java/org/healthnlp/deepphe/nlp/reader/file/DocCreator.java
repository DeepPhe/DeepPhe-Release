package org.healthnlp.deepphe.nlp.reader.file;


import java.io.IOException;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2020
 */
public interface DocCreator<T> {

   /**
    * @param input some object from which one or more Docs should be created.
    * @throws IOException should anything bad happen
    */
   List<Doc> createDocs( final T input ) throws IOException;

}
