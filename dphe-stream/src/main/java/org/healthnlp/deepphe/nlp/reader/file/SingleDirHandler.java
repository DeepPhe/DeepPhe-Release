package org.healthnlp.deepphe.nlp.reader.file;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2020
 */
public class SingleDirHandler extends AbstractDirHandler {

   public SingleDirHandler() {
      super();
   }

   public SingleDirHandler( final File rootDir ) {
      super( rootDir );
   }

   public SingleDirHandler( final FileHandler fileHandler, final File rootDir ) {
      super( fileHandler, rootDir );
   }

   /**
    * Only handles the files in the immediate directory.
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
      // gather all of the files and set the document counts per patient.
      final File[] children = rootDir.listFiles();
      if ( children == null || children.length == 0 ) {
         return;
      }
      final List<File> files = Arrays.stream( children )
                                     .filter( File::canRead )
                                     .sorted( FileComparator.INSTANCE )
                                     .collect( Collectors.toList() );
      for ( File file : files ) {
         fileHandler.addFile( file );
      }
   }


}
