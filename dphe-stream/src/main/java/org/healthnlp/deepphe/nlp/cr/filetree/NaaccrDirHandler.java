package org.healthnlp.deepphe.nlp.cr.filetree;//package org.apache.ctakes.cancer.cr.filetree;
//
//
//import org.apache.ctakes.core.cr.file.Doc;
//import org.apache.ctakes.core.cr.file.FileHandler;
//import org.apache.ctakes.core.cr.file.DirHandler;
//import org.apache.log4j.Logger;
//
//import javax.annotation.Nonnull;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 5/19/2020
// */
//public class NaaccrDirHandler implements DirHandler {
//
//   static private final Logger LOGGER = Logger.getLogger( "NaaccrTreeHandler" );
//
//   final private File _rootDir;
//   final private List<File> _files;
//   private FileHandler _fileHandler;
//
//
//   public NaaccrDirHandler( final File rootDir ) {
//      _rootDir = rootDir;
//      _files = new ArrayList<>();
//   }
//
//   public FileHandler createFileHandler() {
//      if ( _fileHandler == null ) {
//         _fileHandler = new NaaccrFileHandler();
//      }
//      return _fileHandler;
//   }
//
//   public FileHandler getFileHandler() {
//      return createFileHandler();
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public File getRootDir() {
//      return _rootDir;
//   }
//
//   public void readDir( final File rootDir ) throws IOException {
//      if ( _rootDir.isFile() ) {
//         // does not check for valid extensions.  With one file just trust the user.
//         _files.add( rootDir );
//
//         // TODO : mimic the following
////         final String patient = _rootDir.getParentFile().getName();
////         _fileStore.addFile( _rootDir, patient );
////         PatientNoteStore.getInstance().setWantedDocCount( patient, 1 );
//      } else {
//         // gather all of the files and set the document counts per patient.
//         final File[] children = _rootDir.listFiles();
//         if ( children == null || children.length == 0 ) {
//            return;
//         }
//         if ( Arrays.stream( children ).noneMatch( File::isDirectory ) ) {
//            _patientLevel = 0;
//         }
//         createFileTreeHandler()
//               .initialize( _rootDir, _fileStore, _validExtensions, _patientLevel );
//         initializePatientCounts();
//      }
//
//   }
//
//
//
//
//
//
//   int getFileCount();
//
//   int getDocCount();
//
//
//
//
//
//}
