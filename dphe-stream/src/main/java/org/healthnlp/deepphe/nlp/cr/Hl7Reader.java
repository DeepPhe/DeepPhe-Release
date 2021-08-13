package org.healthnlp.deepphe.nlp.cr;


import org.apache.ctakes.core.cr.AbstractFileTreeReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Attempts to build ctakes - compatible metadata and document text from hl7 files.
 * See http://www.hl7.org/special/committees/vocab/v26_appendix_a.pdf
 * <p>
 * For cancer reports (ePath), see http://www.health.state.mn.us/divs/hpcd/cdee/mcss/documents/BHL7implementationguide.vers.5.pdf
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/9/2017
 */
@PipeBitInfo(
      name = "HL7 Dir Tree Reader",
      description = "Reads document texts from HL7 files in a directory tree.",
      role = PipeBitInfo.Role.READER
)
final public class Hl7Reader extends AbstractFileTreeReader {

   // TODO - move to ctakes

   static public final String HL7_DOC_VIEW = "HL7_DOC_VIEW";

   static private final Logger LOGGER = Logger.getLogger( "Hl7Reader" );

   static private final String ANNOTATION_SUFFIX = ".hl7";

   /**
    * hl7 separates segments with newline \r and/or \r\n.  It uses a long code to represent newlines within fields.
    */
   static private final Pattern CR_PATTERN = Pattern.compile( "\\\\X0D\\\\ *" );
   static private final Pattern LF_PATTERN = Pattern.compile( "\\\\X0A\\\\ *" );
   static private final Pattern TAB_PATTERN = Pattern.compile( "\\\\X09\\\\ *" );
   static private final Pattern ALT_TAB_PATTERN = Pattern.compile( "\\\\F\\\\ *" );
   /**
    * In some places 5 spaces are used to denote newline
    */
   static private final Pattern ALT_LF_PATTERN = Pattern.compile( "(?<!:) {2,}" );

   /**
    * integer for empty or malformed date parameter.  LocalDate requires 1 or greater for all fields, so using 1.
    */
   static private final int NULL_DATE_INT = 1;

   /**
    * {@inheritDoc}
    *
    * @return the gold anafora extension
    */
   @Override
   protected Collection<String> getValidExtensions() {
      return Arrays.asList( ANNOTATION_SUFFIX, ANNOTATION_SUFFIX.toUpperCase() );
   }


   /**
    * Using OBX Text (TX) and Formatted Text (FT).  Keep an eye out for valid String Data (ST).
    * {@inheritDoc}
    */
   @Override
   protected void readFile( final JCas jCas, final File file ) throws IOException {
      final Collection<String> lines = readAllLines( file );
      final String text = readFileText( lines );
      jCas.reset();
      jCas.setDocumentText( text );
      // create a view containing the original hl7 text
      try {
         final JCas hl7view = jCas.createView( HL7_DOC_VIEW );
         hl7view.setDocumentText( readFile( file ) );
      } catch ( CASException casE ) {
         LOGGER.warn( "Could not create HL7 View: " + casE.getMessage() );
      }
   }


   // TODO throw this rubber stamped (ctakes FileTreeReader) into a FileReaderUtil in ctakes
   /**
    * Reads file using a Path and stream.  Failing that it calls {@link #readByBuffer(File)}
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readFile( final File file ) throws IOException {
      LOGGER.info( "Reading " + file.getPath() );
      try {
         return readByPath( file );
      } catch ( IOException ioE ) {
         // This is a pretty bad way to handle a MalformedInputException, but that can be thrown by the collector
         // in the stream, and java streams and exceptions do not go well together
         LOGGER.warn( "Bad characters in " + file.getPath() );
      }
      return readByBuffer( file );
   }

   /**
    * Reads file using a Path and stream.
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readByPath( final File file ) throws IOException {
         return safeReadByPath( file );
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
   private String readByBuffer( final File file ) throws IOException {
      // Use 8KB as the default buffer size
      byte[] buffer = new byte[ 8192 ];
      final StringBuilder sb = new StringBuilder();
      try ( final InputStream inputStream = new BufferedInputStream( new FileInputStream( file ), buffer.length ) ) {
         while ( true ) {
            final int length = inputStream.read( buffer );
            if ( length < 0 ) {
               break;
            }
               sb.append( new String( buffer, 0, length ) );
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      return sb.toString();
   }



   /**
    * Representation of an hl7 segment
    */
   static private class Hl7Segment {
      static private final Pattern DELIM = Pattern.compile( "\\|" );
      static private final String UNKNOWN_TYPE = "UNK";
      final private String[] _fields;

      private Hl7Segment( final String data ) {
         _fields = DELIM.split( data );
      }

      final public String getType() {
         return getOrDefault( 0, UNKNOWN_TYPE );
      }

      final public int getIndex() {
         return getInt( 1 );
      }

      final protected String get( final int index ) {
         return getOrDefault( index, "" );
      }

      final protected String getOrDefault( final int index, final String defaultValue ) {
         if ( index < _fields.length ) {
            return _fields[ index ];
         }
         return defaultValue;
      }

      final protected int getInt( final int index ) {
         final String text = get( index );
         if ( text.isEmpty() ) {
            return -1;
         }
         try {
            return Integer.parseInt( text );
         } catch ( NumberFormatException nfE ) {
            return -1;
         }
      }
   }

   /**
    * Adds spanless TimeMention representing Document Creation Time.
    * see https://tl7.intelliware.ca/public/messages/dataTypes/ts.faces
    *
    * @param jCas     ye olde ...
    * @param dateTime DateTime in MSH header
    */
   static private void createDocTime( final JCas jCas, final String dateTime ) {
      final int length = dateTime.length();
      if ( length < 4 ) {
         return;
      }
      final String year = dateTime.substring( 0, 4 );
      String month = "01";
      String day = "01";
      String hour = "12";
      String minute = "00";
      if ( length >= 6 ) {
         month = dateTime.substring( 4, 6 );
      }
      if ( length >= 8 ) {
         day = dateTime.substring( 6, 8 );
      }
      final Date date = new Date( jCas );
      date.setYear( year );
      date.setMonth( month );
      date.setDay( day );
      final TimeMention timeMention = new TimeMention( jCas, -1, -1 );
      timeMention.setDate( date );
      timeMention.addToIndexes();
      SourceMetadataUtil.getOrCreateSourceData( jCas ).setSourceOriginalDate( year + month + day + hour + minute );
   }


   /**
    * @param text element text
    * @return text with hl7 newline representations replaced with standard newline characters
    */
   static private String normalizeText( final String text ) {
      if ( text.isEmpty() ) {
         return "";
      }
      final String crFixed = Arrays.stream( CR_PATTERN.split( text ) ).collect( Collectors.joining( "\r" ) );
      final String lfFixed = Arrays.stream( LF_PATTERN.split( crFixed ) ).collect( Collectors.joining( "\n" ) );
      final String altFixed = Arrays.stream( ALT_LF_PATTERN.split( lfFixed ) ).collect( Collectors.joining( "\n" ) );
      final String tabFixed = Arrays.stream( TAB_PATTERN.split( altFixed ) ).collect( Collectors.joining( "\t" ) );
      return Arrays.stream( ALT_TAB_PATTERN.split( tabFixed ) ).collect( Collectors.joining( "\t" ) );
   }

   /**
    * Parse Message Header.  Adds spanless TimeMention representing Document Creation Time.
    * See https://corepointhealth.com/resource-center/hl7-resources/hl7-msh-message-header
    *
    * @param jCas ye olde
    * @param msh  message header segment
    */
   static private void parseMsh( final JCas jCas, final Hl7Segment msh ) {
      // TODO : in lsu msh is empty.  In uky msh time is in column 6, not 7
      final String dateTime = msh.get( 6 );
      if ( !dateTime.isEmpty() ) {
         createDocTime( jCas, dateTime );
      }
   }

   /**
    * Parse PatientIdentification.
    * See https://corepointhealth.com/resource-center/hl7-resources/hl7-pid-segment
    * See http://www.hosinc.com/products/interfaces/interface_documentation.htm
    *
    * @param jCas ye olde
    * @param pid  patient identification segment
    */
   static private void parsePid( final JCas jCas, final Hl7Segment pid ) {
      // TODO in lsu pid is empty.  in uky pid is missing entirely
      createPatientName( jCas, pid );
      createPatientBirth( jCas, pid );
      final String sex = pid.get( 7 );
      final String race = pid.get( 9 );
      createPatientDeath( jCas, pid );
   }

   /**
    * Document Section Headers may be stored in Observation Result element 3 : Observation Identifier.
    * Document Text is stored in Observation Result element 5 : Value.
    * See https://corepointhealth.com/resource-center/hl7-resources/hl7-obx-segment
    * See http://www.hosinc.com/products/interfaces/interface_documentation.htm
    * See https://issues.openmrs.org/secure/attachment/34246/OBX07.pdf for value types
    *
    * @param obx Observation Result
    * @return normalized text for the result
    */
   static private String parseObxDoc( final Hl7Segment obx ) {
      final StringBuilder sb = new StringBuilder();
      final String source = obx.get( 3 );
      // fields are identifier ^ name ^ coding system
      final String[] fields = source.split( "\\^" );
      if ( fields.length >= 2 && !fields[ 1 ].isEmpty() ) {
         // name is doc type . [?.] observation type name
         final String[] sourceTexts = fields[ 1 ].split( "\\." );
         sb.append( sourceTexts[ sourceTexts.length - 1 ] ).append( "\n" );
      }
      final String value = obx.get( 5 );
      return sb.append( normalizeText( value ) ).toString();
   }

   /**
    * Document Text is stored in Notes and Comments element 3 : Comment.
    * See https://corepointhealth.com/resource-center/hl7-resources/hl7-nte-notes-comments
    *
    * @param nte notes and comments
    * @return normalized text for the notes
    */
   static private String parseNteDoc( final Hl7Segment nte ) {
      final String text = nte.get( 3 );
      return normalizeText( text );
   }


   /**
    * Create Patient Name from PID.
    * See http://www.hosinc.com/products/interfaces/interface_documentation.htm
    *
    * @param jCas ye olde
    * @param pid  patient identification segment
    */
   static private void createPatientName( final JCas jCas, final Hl7Segment pid ) {
      final String patientName = pid.get( 4 );
      if ( patientName.isEmpty() ) {
         return;
      }
      final String[] fields = patientName.split( "\\^" );
      final String lastName = fields[ 0 ];
      if ( fields.length >= 2 ) {
         final String firstName = fields[ 1 ];
      }
      if ( fields.length >= 3 ) {
         final String middleName = fields[ 2 ];
      }
      if ( fields.length >= 4 ) {
         final String suffix = fields[ 3 ];
      }
      if ( fields.length >= 5 ) {
         final String prefix = fields[ 4 ];
      }
   }

   /**
    * Create Patient birth date from PID.
    * See http://www.hosinc.com/products/interfaces/interface_documentation.htm
    *
    * @param jCas ye olde
    * @param pid  patient identification segment
    */
   static private void createPatientBirth( final JCas jCas, final Hl7Segment pid ) {
      final LocalDate birthday = parseLocalDate( pid.get( 6 ) );
   }

   /**
    * Create Patient date of death from PID.
    * See http://www.hosinc.com/products/interfaces/interface_documentation.htm
    *
    * @param jCas ye olde
    * @param pid  patient identification segment
    */
   static private void createPatientDeath( final JCas jCas, final Hl7Segment pid ) {
      final LocalDate deathDay = parseLocalDate( pid.get( 28 ) );
   }

   /**
    * parses out an hl7 date YYYYMMDD
    *
    * @param date date text
    * @return a LocalDate
    */
   static private LocalDate parseLocalDate( final String date ) {
      final int length = date.length();
      if ( length != 8 ) {
         // malformed
         return LocalDate.of( NULL_DATE_INT, NULL_DATE_INT, NULL_DATE_INT );
      }
      final int year = parseDateInt( date.substring( 0, 4 ) );
      final int month = parseDateInt( date.substring( 4, 6 ) );
      final int day = parseDateInt( date.substring( 6, 8 ) );
      return LocalDate.of( year, month, day );

   }

   /**
    * @param date date text
    * @return an integer for the text.  If one could not be parsed then {@link #NULL_DATE_INT}.
    */
   static private int parseDateInt( final String date ) {
      if ( date.isEmpty() ) {
         return NULL_DATE_INT;
      }
      try {
         return Integer.parseInt( date );
      } catch ( NumberFormatException nfE ) {
         return NULL_DATE_INT;
      }
   }


   static public String readFileText( final File file ) throws IOException {
      final Collection<String> lines = readAllLines( file );
      return readFileText( lines );
   }

   static private Collection<String> readAllLines( final File file ) throws IOException {
      final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput( CodingErrorAction.IGNORE );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( Files.newInputStream( file.toPath() ), decoder ) ) ) {
         return reader.lines().collect( Collectors.toList());
      }
   }

   static private String readFileText( final Collection<String> lines ) throws IOException {
      final Collection<Hl7Segment> segments = lines.stream().map( Hl7Segment::new ).collect( Collectors.toList() );
      // document text is a concatenation of observation result values and notes / comments.
      //  Add a double newline (paragraph) between each.
      final String obxText = segments.stream()
            .filter( s -> s.getType().equals( "OBX" ) )
            .filter( s -> s.get( 2 ).equals( "TX" ) || s.get( 2 ).equals( "FT" ) )
            .sorted( Comparator.comparingInt( Hl7Segment::getIndex ) )
            .map( Hl7Reader::parseObxDoc )
            .collect( Collectors.joining( "\n\n" ) );
      final String nteText = segments.stream()
            .filter( s -> s.getType().equals( "NTE" ) )
            .sorted( Comparator.comparingInt( Hl7Segment::getIndex ) )
            .map( Hl7Reader::parseNteDoc )
            .collect( Collectors.joining( "\n\n" ) );
      // Add a paragraph between observation text and comments.  Ensure that document ends with newline.
      return obxText + (nteText.isEmpty() ? "" : "\n\n") + nteText + "\n";
   }

//   /**
//    * Writes a plain text file for each hl7 file
//    *
//    * @param args input root directory ; output root directory
//    */
//   public static void main( final String... args ) {
//      final File rootDir = new File( args[ 0 ] );
//      final File outDir = new File( args[ 1 ] );
//      final List<File> files = new Hl7Reader().getDescendentFiles( rootDir, Arrays.asList( ANNOTATION_SUFFIX, ANNOTATION_SUFFIX.toUpperCase() ) );
//      for ( File file : files ) {
//         try {
//            final String text = readFileText( file );
//            final Collection<String> outLines = Arrays.asList( text.split( "\\n" ) );
//            final String name = file.getPath().substring( rootDir.getPath().length() );
//            final File outFile = new File( outDir, name + ".txt" );
//            outFile.getParentFile().mkdirs();
//            final Path outPath = outFile.toPath();
//            Files.write( outPath, outLines, StandardOpenOption.CREATE );
//         } catch ( IOException ioE ) {
//            LOGGER.error( ioE.getMessage(), ioE );
//            System.exit( 1 );
//         }
//      }
//   }


}