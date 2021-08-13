package org.healthnlp.deepphe.nlp.pipeline;


import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.JCasPool;
import org.healthnlp.deepphe.core.json.JsonNoteWriter;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/14/2020
 */
public enum NoteTextRunner {
   INSTANCE;

   static public NoteTextRunner getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "NoteTextRunner" );


   // Use a constant piper name.
   static private final String NLP_PIPER_PATH = "pipeline/DmsNoteNlp.piper";

   private final AnalysisEngine _engine;
   private final JCasPool _pool;
   private final TextRunner _textRunner;

   private long _index = 0;

   NoteTextRunner() throws ExceptionInInitializerError {
      try {
         final PiperFileReader reader = new PiperFileReader( NLP_PIPER_PATH );
         final PipelineBuilder builder = reader.getBuilder();

         final AnalysisEngineDescription pipeline = builder.getAnalysisEngineDesc();
         _engine = UIMAFramework.produceAnalysisEngine( pipeline );
         _pool = new JCasPool( 2, _engine );
      } catch ( IOException | UIMAException multE ) {
         Logger.getLogger( "NoteTextRunner" ).error( multE.getMessage() );
         throw new ExceptionInInitializerError( multE );
      }

      _textRunner = new TextRunner();
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( _textRunner );
   }


   /**
    * This is the preferred method for processing a document as the patient ID
    * and doc ID are very important for neo4j storage.
    * @param patientId -
    * @param docId -
    * @param text -
    * @return -
    */
   public String addText( final String patientId, final String docId, final String text ) {
      return _textRunner.addText( patientId, docId, text );
   }


   /**
    * This is the preferred method for processing a document as the patient ID
    * and doc ID are very important for neo4j storage.
    * @param patientId -
    * @param docId -
    * @param text -
    * @return -
    */
   public String processText( final String patientId, final String docId, final String text ) {
      synchronized ( NLP_PIPER_PATH ) {
         try {
            final JCas jcas = _pool.getJCas( -1 );
            if ( jcas == null ) {
               throw new AnalysisEngineProcessException( new Throwable( "Could not acquire JCas from pool." ) );
            }
            _index++;
            new JCasBuilder().setPatientId( patientId )
                             .setDocId( docId )
                             .setDocText( text )
                             .rebuild( jcas );
            final String json = processJcas( jcas );
            _pool.releaseJCas( jcas );
            return json;
         } catch ( UIMAException | CASRuntimeException multE ) {
//            LOGGER.error( "Error processing text." );
//            throw new AnalysisEngineProcessException( multE );
            return multE.getMessage();
         }
      }
   }

   /**
    * runs a cas through the pipeline.
    * @param jCas The cas should already be populated with text, patientId, docId, etc.
    * @return json populated with information extracted from the note.
    */
   public String processJcas( final JCas jCas ) {
      synchronized ( NLP_PIPER_PATH ) {
         LOGGER.info( "Processing " + DocIdUtil.getDocumentID( jCas ) );
         try {
            _engine.process( jCas );
            return JsonNoteWriter.createNoteJson( jCas );
         } catch ( UIMAException | CASRuntimeException multE ) {
            return multE.getMessage();
         }
      }
   }

   public void close() {
      _textRunner.close();
   }


   private class TextRunner implements Runnable, Closeable {
      static private final String STOP_TEXT = "STOP_PROCESSING_NOW";
      private final SimpleDoc STOP_DOC = new SimpleDoc( STOP_TEXT, STOP_TEXT, STOP_TEXT );
      private final BlockingQueue<SimpleDoc> _textQueue = new ArrayBlockingQueue<>( 100 );
      private boolean _stop;

      private String addText( final String patientId, final String docId, final String text ) {
         if ( _textQueue.offer( new SimpleDoc( patientId, docId, text ) ) ) {
            return "Added " + patientId + " " + docId + " to the Text Processing Queue.";
         }
         return "Problem adding " + patientId + " " + docId + "to the Text Processing Queue.";
      }

      public void run() {
         while ( !_stop ) {
            try {
               // Use poll instead of take just in case we need to close using _stop
               final SimpleDoc simpleDoc = _textQueue.poll( 1, TimeUnit.MINUTES );
               if ( simpleDoc == null ) {
                  continue;
               }
               if ( simpleDoc.equals( STOP_DOC ) ) {
                  break;
               }
               processText( simpleDoc._patientID, simpleDoc._docID, simpleDoc._text );
            } catch ( InterruptedException intE ) {
               //
            }
         }
      }

      public void close() {
         _stop = true;
         _textQueue.offer( STOP_DOC );
      }

      private final class SimpleDoc {
         private final String _patientID;
         private final String _docID;
         private final String _text;
         private SimpleDoc( final String patientID, final String docID, final String text ) {
            _patientID = patientID;
            _docID = docID;
            _text = text;
         }
      }
   }


}
