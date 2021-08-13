package org.healthnlp.deepphe.nlp.reader.file;

import org.apache.ctakes.core.util.NumberedSuffixComparator;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

/**
 * This separates the handling of a directory of files from the handling of files themselves.
 * A flat DirHandler can be fed a FileHandler that handles multi-Doc xmls or one that handles single-Doc plaintext files.
 * A tree DirHandler can be passed those same FileHandler but will handle the directory differently.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/19/2020
 */
public interface DirHandler extends DocStore {


   FileHandler createFileHandler();

   FileHandler getFileHandler();

   void setRootDir( final File dir );

   /**
    * @return the root input directory as a File.
    */
   File getRootDir();

   void readDir( final File rootDir ) throws IOException;

   default int getFileCount() {
      return getFileHandler().getFileCount();
   }


   /**
    * Read the directory, then initialize the FileHandler.
    * {@inheritDoc}
    */
   @Override
   default void initialize() throws IOException {
      createFileHandler();
      readDir( getRootDir() );
      getFileHandler().initialize();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default Doc getDoc( final int index ) {
      return getFileHandler().getDoc( index );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default int getDocCount() {
      return getFileHandler().getDocCount();
   }


   enum FileComparator implements Comparator<File> {
      INSTANCE;
      private final Comparator<String> __delegate = new NumberedSuffixComparator();

      public int compare( final File file1, final File file2 ) {
         return __delegate.compare( file1.getName(), file2.getName() );
      }
   }


}
