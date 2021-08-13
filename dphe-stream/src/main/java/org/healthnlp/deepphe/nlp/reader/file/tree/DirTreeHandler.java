package org.healthnlp.deepphe.nlp.reader.file.tree;



import org.healthnlp.deepphe.nlp.reader.file.AbstractDirHandler;
import org.healthnlp.deepphe.nlp.reader.file.FileHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/20/2020
 */
public class DirTreeHandler extends AbstractDirHandler {

   public DirTreeHandler() {
      super();
   }

   public DirTreeHandler( final File rootDir ) {
      super( rootDir );
   }

   public DirTreeHandler( final FileHandler fileHandler, final File rootDir ) {
      super( fileHandler, rootDir );
   }

   /**
    * Handles all descending directories.
    * {@inheritDoc}
    */
   @Override
   public void readDir( final File rootDir ) throws IOException {
      final FileHandler fileHandler = getFileHandler();
      if ( fileHandler == null ) {
         throw new IOException( "No FileHandler exists." );
      }
      if ( rootDir.isFile() ) {
         // does not check for valid extensions.  With one file just trust the user.
         fileHandler.addFile( rootDir );
         return;
      }
      final List<File> files = getDescendentFiles( rootDir );
      for ( File file : files ) {
         fileHandler.addFile( file );
      }
   }

   /**
    * @param parentDir -
    * @return List of files descending from the parent directory
    */
   protected List<File> getDescendentFiles( final File parentDir ) {
      final File[] children = parentDir.listFiles();
      if ( children == null || children.length == 0 ) {
         return Collections.emptyList();
      }
      final List<File> childDirs = new ArrayList<>();
      final List<File> files = new ArrayList<>();
      for ( File child : children ) {
         if ( child.isDirectory() ) {
            childDirs.add( child );
            continue;
         }
         files.add( child );
      }
      files.sort( FileComparator.INSTANCE );
      final List<File> descendentFiles = new ArrayList<>( files );
      childDirs.sort( FileComparator.INSTANCE );
      for ( File childDir : childDirs ) {
         descendentFiles.addAll( getDescendentFiles( childDir ) );
      }
      return descendentFiles;
   }


}
