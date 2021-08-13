package org.healthnlp.deepphe.nlp.reader.file;


import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2020
 */
public class PlainTextFileHandler extends AbstractFileHandler {

   /**
    * @param file file to be read
    * @throws IOException should anything bad happen
    */
   public List<Doc> createDocs( final File file ) throws IOException {
      return Collections.singletonList( new DefaultFileDoc( file, getValidExtensions(), isKeepCrChar() ) );
   }


}
