package org.healthnlp.deepphe.nlp.cr.filetree;//package org.apache.ctakes.cancer.cr.filetree;
//
//import org.apache.ctakes.cancer.cr.naaccr.*;
//import org.apache.ctakes.core.cr.FileTreeHandler;
//import org.apache.ctakes.core.cr.file.tree.AbstractFileTreeReader;
//import org.apache.ctakes.core.cr.file.DirHandler;
//import org.apache.ctakes.core.util.doc.DocIdUtil;
//import org.apache.ctakes.core.util.doc.JCasBuilder;
//import org.apache.log4j.Logger;
//import org.apache.uima.collection.CollectionException;
//import org.apache.uima.jcas.JCas;
//
//import java.io.File;
//import java.io.IOException;
//
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 1/10/2020
// */
//public class NaaccrXmlReader extends AbstractFileTreeReader {
//
//   private DirHandler _dirHandler;
//
//   public NaaccrXmlReader() {
//   }
//
//   protected DirHandler createDirHandler( final File rootDir ) {
//      if ( _dirHandler == null ) {
//         _dirHandler = new NaaccrDirHandler( rootDir );
//      }
//      return _dirHandler;
//   }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   protected FileTreeHandler createFileTreeHandler() {
//      return new RegistryXmlHandler();
//   }
//
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   protected JCasBuilder createJCasBuilder( final File file ) {
//      // In NaaccrSection
////      final String id = createDocumentID( file, getValidExtensions() );
//      // In NaaccrDocument
////      final String idPrefix = createDocumentIdPrefix( file, getRootDir() );
//      // In NaaccrSection
////      final String docType = createDocumentType( id );
//      // In NaaccrDocument
////      final String docTime = createDocumentTime( file );
//      // In NaaccrPatient
////      final String patientId = getPatientId( file );
//      return new JCasBuilder();
//   }
//
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public boolean hasNext() {
//      boolean xmlHasNext = false;
//      if ( _currentXmlFile != null ) {
//         xmlHasNext = _currentXmlFile.hasNextSection();
//      }
//      return xmlHasNext || super.hasNext();
//   }
//
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void getNext( final JCas jcas ) throws IOException, CollectionException {
//      if ( _currentXmlFile == null || !_currentXmlFile.hasNextSection() ) {
//         super.getNext( jcas );
//      }
//      if ( !_currentXmlFile.hasNextSection() ) {
//         return;
//      }
//      // Advance to the next section and Populate the jcas.
//      _currentXmlFile.nextSection();
//      _currentXmlFile.addToBuilder( new JCasBuilder() ).rebuild( jcas );
//      _currentXmlFile.populateJCas( jcas );
//      Logger.getLogger( NaaccrXmlReader.class )
//            .info( "Processing " + DocIdUtil.getDocumentID( jcas ) );
//   }
//
//
//
//
//}
