package org.healthnlp.deepphe.nlp.reader;


import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/6/2020
 */
final public class WatchFileTreeHandler extends FileTreeHandler {

   static private final Logger LOGGER = Logger.getLogger( "WatchFileTreeHandler" );

   final private Map<File, Integer> _dirLevels;
   final private Map<WatchKey, File> _watchDirs;
   final private WatchService _watcher;
   final private Collection<File> _knownFiles;
   private int _patientLevel;
   private ReaderFileStore _fileStore;
   private Collection<String> _validExtensions;

   public WatchFileTreeHandler( final WatchService watcher ) {
      _dirLevels = new HashMap<>();
      _watchDirs = new HashMap<>();
      _watcher = watcher;
      _knownFiles = new HashSet<>();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<File> initialize( final File parentDir,
                                 final ReaderFileStore fileStore,
                                 final Collection<String> validExtensions,
                                 final int patientLevel ) {
      _fileStore = fileStore;
      _validExtensions = validExtensions;
      _patientLevel = patientLevel;
      return super.initialize( parentDir, fileStore, validExtensions, patientLevel );
   }

   public void handleWatchedDir( final WatchKey key ) {
      final File dir = _watchDirs.get( key );
      if ( dir == null ) {
         LOGGER.error( "No Path for watch key " + key.toString() );
         return;
      }
      final Path parent = dir.toPath();
      key.pollEvents().stream()
         .filter( e -> !StandardWatchEventKinds.OVERFLOW.equals( e.kind() ) )
         .map( WatchEvent::context )
         .filter( Path.class::isInstance )
         .map( p -> (Path)p )
         .map( parent::resolve )
         .forEach( this::handlePath );
      final boolean valid = key.reset(); // IMPORTANT: The key must be reset after processed
   }

   private void handlePath( final Path path ) {
      final File file = path.toFile();
      if ( file.isDirectory() && !_dirLevels.containsKey( file ) ) {
         final File parentDir = file.getParentFile();
         final int parentLevel = _dirLevels.getOrDefault( parentDir, 0 );
         getDescendentFiles( file.getParentFile(), _fileStore, _validExtensions, _patientLevel, parentLevel );
         return;
      }
      if ( isWantedFile( _validExtensions, file ) ) {
         _knownFiles.add( file );
         _fileStore.addFile( file );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<File> getDescendentFiles( final File parentDir,
                                            final ReaderFileStore fileStore,
                                            final Collection<String> validExtensions,
                                            final int patientLevel,
                                            final int level ) {
      _dirLevels.put( parentDir, level );
      try {
         final Path path = parentDir.toPath();
         final WatchKey key = path.register( _watcher,
               new WatchEvent.Kind[] { ENTRY_CREATE },
               SensitivityWatchEventModifier.HIGH );
         _watchDirs.put( key, parentDir );
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not register directory " + parentDir.getAbsolutePath() + " with WatchService.\n"
                       + ioE.getMessage() );
      }
      final List<File> files = super.getDescendentFiles( parentDir, fileStore, validExtensions, patientLevel, level );
      _knownFiles.addAll( files );
      return files;
   }


   /**
    * Must be a file that hasn't already been handled.
    * {@inheritDoc}
    */
   @Override
   protected boolean isWantedFile( final Collection<String> validExtensions, final File file ) {
      return !_knownFiles.contains( file ) && super.isWantedFile( validExtensions, file );
   }


}
