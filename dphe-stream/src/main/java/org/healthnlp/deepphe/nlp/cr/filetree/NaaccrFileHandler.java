package org.healthnlp.deepphe.nlp.cr.filetree;//package org.apache.ctakes.cancer.cr.filetree;
//
//import org.apache.ctakes.cancer.cr.naaccr.*;
//import org.apache.ctakes.core.cr.file.AbstractFileHandler;
//import org.apache.ctakes.core.cr.file.Doc;
//import org.apache.log4j.Logger;
//import org.xml.sax.Attributes;
//import org.xml.sax.SAXException;
//import org.xml.sax.helpers.DefaultHandler;
//
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.parsers.SAXParser;
//import javax.xml.parsers.SAXParserFactory;
//import java.io.File;
//import java.io.IOException;
//
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 5/19/2020
// */
//public class NaaccrFileHandler extends AbstractFileHandler {
//
//   static private final Logger LOGGER = Logger.getLogger( "NaaccrFileHandler" );
//
//   private NaaccrXmlFile _currentXmlFile;
//
//   /**
//    * @param file file to be read
//    * @throws IOException should anything bad happen
//    */
//   public void readFile( final File file ) throws IOException {
//      try {
//         final SAXParserFactory factory = SAXParserFactory.newInstance();
//         final SAXParser saxParser = factory.newSAXParser();
//
//         final NaaccrXmlHandler handler = new NaaccrXmlHandler( new NaaccrXmlFile( file.getPath() ) );
//
//         saxParser.parse( file, handler );
//
//         _currentXmlFile = handler.getNaaccrXmlFile();
//
//      } catch ( ParserConfigurationException | SAXException | IOException multE ) {
//         throw new IOException( multE );
//      }
//   }
//
//
//
//   public Doc getCurrentDoc();
//
//   public Doc nextDoc();
//
//   public int getDocIndex();
//
//   public int getDocCount();
//
//
//
//   /////////////////////////////////////////////////////////////////////////////////////
//   //
//   //                   Xml Handler
//   //
//   /////////////////////////////////////////////////////////////////////////////////////
//
//
//   static private class NaaccrXmlHandler extends DefaultHandler {
//      static private final String PATIENT_TAG = "Patient";
//      static private final String TUMOR_TAG = "Tumor";
//      static private final String ITEM_TAG = "Item";
//
//      private final NaaccrXmlFile _naaccrXmlFile;
//
//      private String _currentTag;
//      private NaaccrItemType _currentItemType;
//
//      private NaaccrPatient __patient;
//      private NaaccrTumor __tumor;
//      private NaaccrDocument __document;
//      private NaaccrSection __section;
//
//
//      NaaccrXmlHandler( final NaaccrXmlFile naaccrXmlFile ) {
//         _naaccrXmlFile = naaccrXmlFile;
//      }
//
//      NaaccrXmlFile getNaaccrXmlFile() {
//         return _naaccrXmlFile;
//      }
//
//      /**
//       * Receive notification of the start of an element.
//       * {@inheritDoc}
//       */
//      @Override
//      public void startElement( final String uri, final String localName, final String tag,
//                                final Attributes attributes ) throws SAXException {
//         _currentTag = tag;
//         switch ( tag ) {
//            case PATIENT_TAG: {
//               __patient = new NaaccrPatient();
//               break;
//            }
//            case TUMOR_TAG: {
//               __document = new NaaccrDocument();
//               __tumor = new NaaccrTumor();
//               break;
//            }
//            case ITEM_TAG: {
//               final String id = attributes.getValue( "naaccrId" );
//               _currentItemType = NaaccrItemType.getItemType( id );
//               if ( _currentItemType instanceof NaaccrSectionType ) {
//                  __section = new NaaccrSection( id );
//               }
//               break;
//            }
//         }
//      }
//
//      /**
//       * Receive notification of the end of an element.
//       * {@inheritDoc}
//       */
//      @Override
//      public void endElement( final String uri, final String localName, final String tag ) throws SAXException {
//         switch ( tag ) {
//            case PATIENT_TAG: {
//               _naaccrXmlFile.add( __patient );
//               break;
//            }
//            case TUMOR_TAG: {
//               __tumor.add( __document );
//               __patient.add( __tumor );
//               break;
//            }
//            case ITEM_TAG: {
//               if ( _currentItemType instanceof NaaccrSectionType ) {
//                  if ( !__section.getText().isEmpty() ) {
//                     __document.add( __section );
////                     Logger.getLogger( RegistryXmlReader.class ).info( "Added Section " + __section.getId() );
//                  }
//               }
//               break;
//            }
//         }
//         _currentTag = "";
//      }
//
//      /**
//       * Receive notification of character data inside an element.
//       * {@inheritDoc}
//       */
//      @Override
//      public void characters( final char[] chars, final int start, final int length ) throws SAXException {
//         if ( !_currentTag.equals( ITEM_TAG ) ) {
//            return;
//         }
//         if ( _currentItemType == null || !_currentItemType.shouldParse() ) {
//            return;
//         }
//         final String text = new String( chars, start, length );
//         if ( _currentItemType instanceof NaaccrSectionType ) {
//            __section.appendText( text );
//            return;
//         }
//         switch ( (InfoItemType)_currentItemType ) {
//            case PATIENT_ID: {
//               __patient.setId( text );
//               break;
//            }
//            case CLINIC_DOC_DATE:
//            case PATH_DOC_DATE: {
//               __document.setDocDate( text );
//               break;
//            }
//            case DOCUMENT_ID: {
//               __document.setId( text );
//               break;
//            }
//         }
//      }
//   }
//
//}
