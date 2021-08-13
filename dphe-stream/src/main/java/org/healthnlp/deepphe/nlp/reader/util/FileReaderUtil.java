package org.healthnlp.deepphe.nlp.reader.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO move to ctakes trunk and refactor FileTreeReader to use/delegate to this class.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/10/2020
 */
final public class FileReaderUtil {

   static private final Logger LOGGER = Logger.getLogger( "FileReader" );
   static public final String UNKNOWN = "Unknown";
   static private final Pattern CR_LF = Pattern.compile( "\\r\\n" );

   private FileReaderUtil() {
   }

   /**
    * Reads file using a Path and stream.  Failing that it calls readByBuffer(..)
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   static public String readFile( final File file ) throws IOException {
      return readFile( file, true );
   }

   /**
    * Reads file using a Path and stream.  Failing that it calls readByBuffer(..)
    *
    * @param file       file to read
    * @param keepCrChar true to keep carriage return characters
    * @return text in file
    * @throws IOException if the file could not be read
    */
   static public String readFile( final File file, final boolean keepCrChar ) throws IOException {
      return readFile( file, keepCrChar, UNKNOWN );
   }


   /**
    * Reads file using a Path and stream.  Failing that it calls readByBuffer(..)
    *
    * @param file       file to read
    * @param keepCrChar true to keep carriage return characters
    * @param encoding   character set encoding
    * @return text in file
    * @throws IOException if the file could not be read
    */
   static public String readFile( final File file, final boolean keepCrChar, final String encoding )
         throws IOException {
      if ( !keepCrChar ) {
         try {
            return readByPath( file, encoding );
         } catch ( IOException ioE ) {
            // This is a pretty bad way to handle a MalformedInputException, but that can be thrown by the collector
            // in the stream, and java streams and exceptions do not go well together
            LOGGER.warn( "Bad characters in " + file.getPath() );
         }
      }
      try {
         return readByStreamReader( file );
      } catch ( IOException ioE ) {
         // ignore for now, try to read by buffer.
      }
      return readByBuffer( file, encoding );
   }


   /**
    * @param text document text
    * @return the document text with end of line characters replaced if needed
    */
   static public String handleTextEol( final String text, final boolean keepCrChar ) {
      String docText = text;
      if ( !keepCrChar && !docText.isEmpty() && docText.contains( "\r" ) ) {
         docText = CR_LF.matcher( docText ).replaceAll( "\n" );
      }
      if ( !docText.isEmpty() && !docText.endsWith( "\n" ) ) {
         // Make sure that we end with a newline
         docText += "\n";
      }
      return docText;
   }


   /**
    * Reads file using a Path and stream.
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   static private String readByPath( final File file, final String encoding ) throws IOException {
      if ( encoding != null && !encoding.isEmpty() && !UNKNOWN.equals( encoding ) ) {
         final Charset charset = Charset.forName( encoding );
         try ( Stream<String> stream = Files.lines( file.toPath(), charset ) ) {
            return stream.collect( Collectors.joining( "\n" ) );
         }
      } else {
         return safeReadByPath( file );
      }
   }

   static private String safeReadByPath( final File file ) throws IOException {
      final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput( CodingErrorAction.IGNORE );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( Files.newInputStream( file.toPath() ), decoder ) ) ) {
         return reader.lines().collect( Collectors.joining( "\n" ) );
      }
   }

   /**
    * Reads file using buffered input stream
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   static private String readByBuffer( final File file, final String encoding ) throws IOException {
      // Use 8KB as the default buffer size
      byte[] buffer = new byte[ 8192 ];
      final StringBuilder sb = new StringBuilder();
      try ( final InputStream inputStream = new BufferedInputStream( new FileInputStream( file ), buffer.length ) ) {
         while ( true ) {
            final int length = inputStream.read( buffer );
            if ( length < 0 ) {
               break;
            }
            if ( encoding != null && !encoding.isEmpty() && !UNKNOWN.equals( encoding ) ) {
               sb.append( new String( buffer, 0, length, encoding ) );
            } else {
               sb.append( new String( buffer, 0, length ) );
            }
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      return sb.toString();
   }

   /**
    * Reads file using a stream reader
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   static private String readByStreamReader( final File file ) throws IOException {
      final StringBuilder sb = new StringBuilder();
      final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput( CodingErrorAction.IGNORE );
      try ( BufferedReader reader
                  = new BufferedReader( new InputStreamReader( Files.newInputStream( file.toPath() ), decoder ) ) ) {
         int i = reader.read();
         while ( i != -1 ) {
            sb.append( Character.toChars( i ) );
            i = reader.read();
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      return sb.toString();
   }


}
