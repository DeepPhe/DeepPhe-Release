package org.healthnlp.deepphe.nlp.cr.naaccr;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.nlp.reader.FileTreeHandler;
import org.healthnlp.deepphe.nlp.reader.ReaderFileStore;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.healthnlp.deepphe.nlp.cr.naaccr.InfoItemType.PATIENT_ID;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/8/2020
 */
public class RegistryXmlHandler extends FileTreeHandler {


   /**
    * Scan xml files for multiple sections and patients.
    * {@inheritDoc}
    */
   protected void countPatientDocs( final File file,
                                    final ReaderFileStore fileStore,
                                    final String defaultPatientId ) {
      final Map<String, Integer> patientDocCounts = fileStore.getPatientDocCounts();
      try {
         final SAXParserFactory factory = SAXParserFactory.newInstance();
         final SAXParser saxParser = factory.newSAXParser();
         final NaaccrXmlScanner scanner = new NaaccrXmlScanner();
         saxParser.parse( file, scanner );

         final Map<String, Integer> scannerCounts = scanner.getPatientSectionCounts();
         for ( Map.Entry<String, Integer> scannerCount : scannerCounts.entrySet() ) {
            final int count = patientDocCounts.getOrDefault( scannerCount.getKey(), 0 );
            patientDocCounts.put( scannerCount.getKey(), count + scannerCount.getValue() );
         }
      } catch ( ParserConfigurationException | SAXException | IOException multE ) {
         Logger.getLogger( RegistryXmlHandler.class ).error( multE.getMessage() );
         final int count = patientDocCounts.getOrDefault( defaultPatientId, 0 );
         patientDocCounts.put( defaultPatientId, count + 1 );
      }
   }


   static private class NaaccrXmlScanner extends DefaultHandler {
      static private final String ITEM_TAG = "Item";

      private final Map<String, Integer> _patientSectionCounts = new HashMap<>();

      private String _currentTag;
      private NaaccrItemType _currentItemType;

      private String __patientId;
      private boolean _validSection = false;

      Map<String, Integer> getPatientSectionCounts() {
         return _patientSectionCounts;
      }

      /**
       * Receive notification of the start of an element.
       * {@inheritDoc}
       */
      @Override
      public void startElement( final String uri, final String localName, final String tag,
                                final Attributes attributes ) throws SAXException {
         _currentTag = tag;
         if ( tag.equals( ITEM_TAG ) ) {
            final String id = attributes.getValue( "naaccrId" );
            _currentItemType = NaaccrItemType.getItemType( id );
            if ( _currentItemType instanceof NaaccrSectionType ) {
               _validSection = false;
            }
         }
      }

      /**
       * Receive notification of the end of an element.
       * {@inheritDoc}
       */
      @Override
      public void endElement( final String uri, final String localName, final String tag ) throws SAXException {
         if ( tag.equals( ITEM_TAG )
              && _currentItemType instanceof NaaccrSectionType
              && _validSection ) {
            final int count = _patientSectionCounts.getOrDefault( __patientId, 0 );
            _patientSectionCounts.put( __patientId, count + 1 );
//            Logger.getLogger( RegistryXmlReader.class ).info( "Sections " + __patientId + " " + (count + 1) );
         }
         _currentTag = "";
      }

      /**
       * Receive notification of character data inside an element.
       * {@inheritDoc}
       */
      @Override
      public void characters( final char[] chars, final int start, final int length ) throws SAXException {
         if ( !_currentTag.equals( ITEM_TAG ) || _currentItemType == null || !_currentItemType.shouldParse() ) {
            return;
         }
         if ( _currentItemType instanceof NaaccrSectionType ) {
            _validSection |= length > 0;
            return;
         }
         if ( _currentItemType.equals( PATIENT_ID ) ) {
            __patientId = new String( chars, start, length );
         }
      }
   }


}
