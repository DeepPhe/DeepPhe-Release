package org.healthnlp.deepphe.nlp.reader.file;


import java.io.File;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/20/2020
 */
abstract public class AbstractDirHandler implements DirHandler {

   private File _rootDir;
   private FileHandler _fileHandler;

   public AbstractDirHandler() {
   }

   public AbstractDirHandler( final File rootDir ) {
      _rootDir = rootDir;
   }

   public AbstractDirHandler( final FileHandler fileHandler ) {
      setFileHandler( fileHandler );
   }

   public AbstractDirHandler( final FileHandler fileHandler, final File rootDir ) {
      _rootDir = rootDir;
      setFileHandler( fileHandler );
   }

   public void setFileHandler( final FileHandler fileHandler ) {
      _fileHandler = fileHandler;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public FileHandler createFileHandler() {
      return _fileHandler;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public FileHandler getFileHandler() {
      return _fileHandler;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   final public void setRootDir( final File dir ) {
      _rootDir = dir;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public File getRootDir() {
      return _rootDir;
   }


}
