package org.healthnlp.deepphe.nlp.reader;


import org.apache.ctakes.core.pipeline.ProgressManager;
import org.apache.log4j.Logger;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/6/2020
 */
abstract public class DirWatchingReader extends MoveAbstractFileTreeReader {

   static private final Logger LOGGER = Logger.getLogger( "DirWatchingReader" );

   private WatchFileTreeHandler _handler;
   private WatchService _watcher;

   /**
    * Initialize progress with no endpoint.
    */
   @Override
   protected void initializeProgress() {
      ProgressManager.getInstance().initializeProgress( getRootPath(), Integer.MAX_VALUE );
   }

   /**
    * Starts the watcher.
    *
    * @return WatchFileTreeHandler
    */
   @Override
   protected FileTreeHandler createFileTreeHandler() {
      try {
         _watcher = FileSystems.getDefault().newWatchService();
      } catch ( IOException ioE ) {
         // What to do
      }
      _handler = new WatchFileTreeHandler( _watcher );
      return _handler;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasNext() {
      final int noteCount = getNoteCount();
      final boolean hasNext = getCurrentIndex() < noteCount;
      if ( hasNext ) {
         return true;
      }
      try {
         final WatchKey key = _watcher.take();
         _handler.handleWatchedDir( key );
         return hasNext();
      } catch ( InterruptedException intE ) {
         // ack
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      return new Progress[] {
            new ProgressImpl( getCurrentIndex(), Integer.MAX_VALUE, Progress.ENTITIES )
      };
   }

   /**
    * Does nothing
    */
   @Override
   protected void initializePatientCounts() {
   }


}
