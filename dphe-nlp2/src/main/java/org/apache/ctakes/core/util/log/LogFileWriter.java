package org.apache.ctakes.core.util.log;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.nlp.util.DebugLogger;

import java.io.*;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @since {11/19/2023}
 */
@PipeBitInfo(
      name = "LogFileWriter",
      description = "Writes log files in a directory tree.",
      role = PipeBitInfo.Role.WRITER,
      usables = { DOCUMENT_ID_PREFIX }
)
final public class LogFileWriter extends AbstractJCasFileWriter {
   static private final Logger LOGGER = Logger.getLogger( "LogFileWriter" );

   // Currently not enabled
   @ConfigurationParameter( name = "WriteLogFiles", mandatory = false,
                            description = "Write Log information to File.", defaultValue = "yes" )
   private String _writeLogFiles;

   static private boolean _writeLogs = false;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _writeLogs = AeParamUtil.isTrue( _writeLogFiles );
   }

   static public void setWriteLogs( final boolean writeLogs ) {
      _writeLogs = writeLogs;
   }

   static public void add( final String text ) {
      LogFileWriter.add( DebugLogger.DEFAULT.ordinal(), text );
   }

   static public String get() {
      return LogFileWriter.get( DebugLogger.DEFAULT.ordinal() );
   }

   static public void add( final int logger, final String text ) {
      if ( !_writeLogs ) {
         return;
      }
      if ( logger < 0 || logger > DebugLogger.values().length ) {
         throw new IllegalArgumentException( "LogFileWriter only accepts logger values 0 - "
               + DebugLogger.values().length );
      }
      DebugLogger.values()[ logger ].addDebug( text );
   }

   static public String get( final int logger ) {
      if ( !_writeLogs ) {
         return "Logging not enabled.  Add LogFileWriter to your pipeline.";
      }
      if ( logger < 0 || logger > DebugLogger.values().length ) {
         throw new IllegalArgumentException( "LogFileWriter only accepts logger values 0 - "
               + DebugLogger.values().length );
      }
      return DebugLogger.values()[ logger ].getDebug();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      LOGGER.info( "Writing logs ..." );
      for ( DebugLogger logger : DebugLogger.values() ) {
         writeLog( logger, outputDir, documentId );
         logger.resetDebug();
      }
   }

   static private void writeLog( final DebugLogger logger,
                                 final String outputDir,
                                 final String documentId ) throws IOException {
      final String log = logger.getDebug();
      if ( log.isEmpty() ) {
         return;
      }
      final File file = new File( outputDir, documentId + "_" + logger.name() + ".log" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
            writer.write( log );
      }
   }


}

