package org.healthnlp.deepphe.nlp.reader.file;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/19/2020
 */
public interface FileHandler extends DocStore, DocCreator<File> {

   String UNKNOWN_ENCODING = "Unknown";

   /**
    * @param file file to be read
    * @throws IOException should anything bad happen
    */
   List<Doc> createDocs( final File file ) throws IOException;

   /**
    * @param encoding The character encoding used by the input files.
    */
   void setValidEncoding( final String encoding );

   /**
    * @return any specified valid file encodings.
    * If none are specified then the default is {@link FileHandler#UNKNOWN_ENCODING}.
    */
   default String getValidEncoding() {
      return UNKNOWN_ENCODING;
   }

   /**
    * @return any specified valid file extensions.
    */
   default Collection<String> getValidExtensions() {
      return Collections.emptyList();
   }

   /**
    * @param explicitExtensions array of file extensions as specified in the uima parameters
    * @return a collection of dot-prefixed extensions or none if {@code explicitExtensions} is null or empty
    */
   Collection<String> createValidExtensions( final String... explicitExtensions );


   void setKeepCrChar( final boolean keepCrChar );

   default boolean isKeepCrChar() {
      return true;
   }

   /**
    * @param file -
    * @return true if the file was added.
    * @throws IOException -
    */
   boolean addFile( final File file ) throws IOException;

   /**
    * @return number of files in store.
    */
   int getFileCount();


}
