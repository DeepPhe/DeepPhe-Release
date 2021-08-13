package org.healthnlp.deepphe.nlp.reader;


import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/6/2020
 */
public class FileTreeHandler {

   static private final Logger LOGGER = Logger.getLogger( "FileTreeHandler" );


   public List<File> initialize( final File parentDir,
                                 final ReaderFileStore fileStore,
                                 final Collection<String> validExtensions,
                                 final int patientLevel ) {
      return getDescendentFiles( parentDir, fileStore, validExtensions, patientLevel, 0 );
   }

   /**
    * @return Comparator to sort Files and Directories.  The default Comparator sorts by filename with {@link NumberedSuffixComparator}.
    */
   protected Comparator<File> getFileComparator() {
      return FileComparator.INSTANCE;
   }

   /**
    * @param parentDir       -
    * @param fileStore       -
    * @param validExtensions collection of valid extensions or empty collection if all extensions are valid
    * @param level           directory level beneath the root directory
    * @return List of files descending from the parent directory
    */
   protected List<File> getDescendentFiles( final File parentDir,
                                            final ReaderFileStore fileStore,
                                            final Collection<String> validExtensions,
                                            final int patientLevel,
                                            final int level ) {
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
         if ( isWantedFile( validExtensions, child ) ) {
            files.add( child );
         }
      }
      final Comparator<File> fileComparator = getFileComparator();
      childDirs.sort( fileComparator );
      files.sort( fileComparator );
      files.forEach( fileStore::addFile );
      final List<File> descendentFiles = new ArrayList<>( files );
      for ( File childDir : childDirs ) {
         descendentFiles.addAll(
               getDescendentFiles( childDir, fileStore, validExtensions, patientLevel, level + 1 ) );
      }
      if ( level == patientLevel ) {
         final String patientId = parentDir.getName();
         for ( File docFile : descendentFiles ) {
            countPatientDocs( docFile, fileStore, patientId );
         }
         descendentFiles.forEach( f -> fileStore.setPatientId( f, patientId ) );
      }
      return descendentFiles;
   }

   protected boolean isWantedFile( final Collection<String> validExtensions, final File file ) {
      return isExtensionValid( file, validExtensions ) && !file.isHidden();
   }

   /**
    * Add to the count of documents for each patient.
    * By default this adds 1 per document.  However, it can be overridden to handle multiple-document xml, etc.
    *
    * @param docFile          some document file
    * @param fileStore        -
    * @param defaultPatientId the name of the patient-level directory
    */
   protected void countPatientDocs( final File docFile,
                                    final ReaderFileStore fileStore,
                                    final String defaultPatientId ) {
      final Map<String, Integer> patientDocCounts = fileStore.getPatientDocCounts();
      final int count = patientDocCounts.getOrDefault( defaultPatientId, 0 );
      patientDocCounts.put( defaultPatientId, count + 1 );
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return true if validExtensions is empty or contains an extension belonging to the given file
    */
   protected boolean isExtensionValid( final File file, final Collection<String> validExtensions ) {
      if ( validExtensions.isEmpty() ) {
         return true;
      }
      final String fileName = file.getName();
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) ) {
            if ( fileName.equals( extension ) ) {
               LOGGER.warn( "File " + file.getPath()
                            + " name exactly matches extension " + extension + " so it will not be read." );
               return false;
            }
            return true;
         }
      }
      return false;
   }

   private enum FileComparator implements Comparator<File> {
      INSTANCE;
      private final Comparator<String> __delegate = new NumberedSuffixComparator();

      public int compare( final File file1, final File file2 ) {
         return __delegate.compare( file1.getName(), file2.getName() );
      }
   }


}
