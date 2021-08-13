package org.healthnlp.deepphe.nlp.pipeline;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.nlp.cr.naaccr.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/16/2020
 */
public enum EpathXmlRunner {
   INSTANCE;

   static public EpathXmlRunner getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "EpathXmlRunner" );

   private final XmlFileRunner _xmlRunner;

   EpathXmlRunner() {
      _xmlRunner = new XmlFileRunner();
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( _xmlRunner );
   }

   public String addXml( final String text )
         throws AnalysisEngineProcessException {
      if ( text == null || text.trim().isEmpty() ) {
         return "";
      }
      try {
         final NaaccrXmlFile xmlFile = readXml( text );
         return _xmlRunner.addXmlFile( xmlFile );
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }


   public String processXml( final String text )
         throws AnalysisEngineProcessException {
      if ( text == null || text.trim().isEmpty() ) {
         return "";
      }
      LOGGER.info( "EpathXmlRunner XML:\n" + text );
      try {
         final NaaccrXmlFile xmlFile = readXml( text );
         return processXmlFile( xmlFile );
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

   private String processXmlFile( final NaaccrXmlFile xmlFile ) throws AnalysisEngineProcessException {
      final NaaccrPatient naaccrPatient = xmlFile.get();
      final Patient patient = new Patient();
      patient.setId( naaccrPatient.getId() );
      final List<Note> notes = new ArrayList<>();
      while ( xmlFile.hasNextSection() ) {
         xmlFile.nextSection();

         final String noteJson = processXmlSection( xmlFile );
         final Gson gson = new GsonBuilder().create();
         notes.add( gson.fromJson( noteJson, Note.class ) );
      }
      patient.setNotes( notes );
      final Gson gson2 = new GsonBuilder().setPrettyPrinting().create();
      return gson2.toJson( patient );
   }

   private String processXmlSection( final NaaccrXmlFile xmlFile ) throws AnalysisEngineProcessException {
      try {
         final JCas jcas = xmlFile.addToBuilder( new JCasBuilder() ).build();
         xmlFile.populateJCas( jcas );
         return NoteTextRunner.getInstance().processJcas( jcas );
         } catch ( CASRuntimeException | UIMAException multE ) {
            LOGGER.error( "Error processing text." );
            throw new AnalysisEngineProcessException( multE );
         }
   }


   // TODO - move this into an independent class and use it here and in RegistryXmlReader

   /**
    * @param xml text to be read
    * @throws IOException should anything bad happen
    */
   protected NaaccrXmlFile readXml( final String xml ) throws IOException {
      try {
         final SAXParserFactory factory = SAXParserFactory.newInstance();
         final SAXParser saxParser = factory.newSAXParser();

         final NaaccrXmlHandler
               handler = new NaaccrXmlHandler( new NaaccrXmlFile( "BLANK_FOR_NOW" ) );

         saxParser.parse( xml, handler );

         return handler.getNaaccrXmlFile();

      } catch ( ParserConfigurationException | SAXException | IOException multE ) {
         throw new IOException( multE );
      }
   }


   /////////////////////////////////////////////////////////////////////////////////////
   //
   //                   Xml Handler
   //
   /////////////////////////////////////////////////////////////////////////////////////

   // Reused from Naaccr RegistryXmlReader
   static private class NaaccrXmlHandler extends DefaultHandler {
      static private final String PATIENT_TAG = "Patient";
      static private final String TUMOR_TAG = "Tumor";
      static private final String ITEM_TAG = "Item";

      private final NaaccrXmlFile _naaccrXmlFile;

      private String _currentTag;
      private NaaccrItemType _currentItemType;

      private NaaccrPatient __patient;
      private NaaccrTumor __tumor;
      private NaaccrDocument __document;
      private NaaccrSection __section;


      NaaccrXmlHandler( final NaaccrXmlFile naaccrXmlFile ) {
         _naaccrXmlFile = naaccrXmlFile;
      }

      NaaccrXmlFile getNaaccrXmlFile() {
         return _naaccrXmlFile;
      }

      /**
       * Receive notification of the start of an element.
       * {@inheritDoc}
       */
      @Override
      public void startElement( final String uri, final String localName, final String tag,
                                final Attributes attributes ) throws SAXException {
         _currentTag = tag;
         switch ( tag ) {
            case PATIENT_TAG: {
               __patient = new NaaccrPatient();
               break;
            }
            case TUMOR_TAG: {
               __document = new NaaccrDocument();
               __tumor = new NaaccrTumor();
               break;
            }
            case ITEM_TAG: {
               final String id = attributes.getValue( "naaccrId" );
               _currentItemType = NaaccrItemType.getItemType( id );
               if ( _currentItemType instanceof NaaccrSectionType ) {
                  __section = new NaaccrSection( id );
               }
               break;
            }
         }
      }

      /**
       * Receive notification of the end of an element.
       * {@inheritDoc}
       */
      @Override
      public void endElement( final String uri, final String localName, final String tag ) throws SAXException {
         switch ( tag ) {
            case PATIENT_TAG: {
               _naaccrXmlFile.add( __patient );
               break;
            }
            case TUMOR_TAG: {
               __tumor.add( __document );
               __patient.add( __tumor );
               break;
            }
            case ITEM_TAG: {
               if ( _currentItemType instanceof NaaccrSectionType ) {
                  if ( !__section.getText().isEmpty() ) {
                     __document.add( __section );
//                     Logger.getLogger( RegistryXmlReader.class ).info( "Added Section " + __section.getId() );
                  }
               }
               break;
            }
         }
         _currentTag = "";
      }

      /**
       * Receive notification of character data inside an element.
       * {@inheritDoc}
       */
      @Override
      public void characters( final char[] chars, final int start, final int length ) throws SAXException {
         if ( !_currentTag.equals( ITEM_TAG ) ) {
            return;
         }
         if ( _currentItemType == null || !_currentItemType.shouldParse() ) {
            return;
         }
         final String text = new String( chars, start, length );
         if ( _currentItemType instanceof NaaccrSectionType ) {
            __section.appendText( text );
            return;
         }
         switch ( (InfoItemType)_currentItemType ) {
            case PATIENT_ID: {
               __patient.setId( text );
               break;
            }
            case CLINIC_DOC_DATE:
            case PATH_DOC_DATE: {
               __document.setDocDate( text );
               break;
            }
            case DOCUMENT_ID: {
               __document.setId( text );
               break;
            }
         }
      }
   }


   private class XmlFileRunner implements Runnable, Closeable {
      static private final String STOP_TEXT = "STOP_PROCESSING_NOW";
      private final BlockingQueue<NaaccrXmlFile> _xmlQueue = new ArrayBlockingQueue<>( 100 );
      private boolean _stop;

      private String addXmlFile( final NaaccrXmlFile naaccrXmlFile ) {
         final String id = naaccrXmlFile.getId();
         final int sectionCount = naaccrXmlFile.getSectionCount();
         if ( sectionCount == 0 ) {
            return "No Sections in " + id + " to add to the ePath Processing Queue.";
         }
         if ( _xmlQueue.offer( naaccrXmlFile ) ) {
//            return "Added " + id + " with " + sectionCount + " Sections to the NLP Processing Queue.";
         }
         return "Problem adding " + id + " to the NLP Processing Queue.";
      }

      public void run() {
         while ( !_stop ) {
            try {
               final NaaccrXmlFile xmlFile = _xmlQueue.take();
               if ( xmlFile.getId().equals( STOP_TEXT ) ) {
                  break;
               }
               processXmlFile( xmlFile );
            } catch ( InterruptedException | AnalysisEngineProcessException multE ) {
               //
            }
         }
      }

      public void close() {
         _stop = true;
         final NaaccrXmlFile stopFile = new NaaccrXmlFile( STOP_TEXT );
         _xmlQueue.offer( stopFile );
      }
   }

}
