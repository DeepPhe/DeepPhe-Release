package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


/**
 * @author SPF , chip-nlp
 * @since {1/23/2024}
 */
@PipeBitInfo (
      name = "VumcReader",
      description = "Reads VUMC files in a directory tree and parses DocTime.",
      role = PipeBitInfo.Role.READER
)
public class VumcReader extends AbstractFileTreeReader {
   static private final Logger LOGGER = Logger.getLogger( "VumcReader" );

   private final ZoneId ZONE_ID = ZoneId.systemDefault();

   static public final String CAS_DATE_FORMAT_PARAM = "CasDateFormat";
   static public final String CAS_DATE_FORMAT_DESC = "Set a value for parameter CasDateFormat.";
   @ConfigurationParameter (
           name = CAS_DATE_FORMAT_PARAM,
           description = CAS_DATE_FORMAT_DESC,
           defaultValue = "MMddyyyykkmmss",
           mandatory = false
   )
   private String _casDateFormat;

   private DateTimeFormatter _casDateFormatter;


   static public final String FILE_DATE_FORMAT_PARAM = "FileDateFormat";
   static public final String FILE_DATE_FORMAT_DESC = "A format to parse date from a filename.  e.g. dd-MM-yyyy_HH:mm:ss";
   @ConfigurationParameter (
         name = FILE_DATE_FORMAT_PARAM,
         description = FILE_DATE_FORMAT_DESC,
         defaultValue = "yyyyMMdd",
         mandatory = false
   )
   private String _fileDateFormat;

   private DateTimeFormatter _fileDateFormatter;

   private final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern( "yyMMdd" );

   private final LocalDateTime RUN_START_DT = LocalDateTime.now();


   private final FileTreeReader _delegateReader = new FileTreeReader();

   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _fileDateFormatter = DateTimeFormatter.ofPattern( _fileDateFormat );
      _casDateFormatter = DateTimeFormatter.ofPattern( _casDateFormat );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void readFile(final JCas jCas, final File file ) throws IOException {
      String docText = _delegateReader.readFile( file );
      docText = handleQuotedDoc( docText );
      docText = handleTextEol( docText );
      jCas.setDocumentText( docText );
   }


   protected String createDocumentType( final String documentId ) {
      final String[] splits = StringUtil.fastSplit( documentId, '-' );
      return splits[ 2 ];
   }

   /**
    * @param file -
    * @return the file's last modification date as a string : {@link #getDateFormat()}
    */
   protected String createDocumentTime( final File file ) {
      final String entryName = file.getName();
      final String[] extSplits = StringUtil.fastSplit( entryName, '.' );
      final String[] splits = StringUtil.fastSplit( extSplits[ 0 ], '-' );
      final LocalDateTime ldt = getLocalDateTime( splits[ 3 ] );
      return createDocumentTime( ldt );
   }

   // TODO move to abstract reader

   private LocalDateTime getLocalDateTime( final String dateText ) {
      LocalDate date = null;
      if ( dateText.length() == 8 ) {
         try {
            date = LocalDate.parse( dateText, _fileDateFormatter );
         } catch ( DateTimeParseException dtpE ) {
            LOGGER.warn( "Bad File date yyyyMMdd " + dateText );
         }
      } else if ( dateText.length() == 6 ) {
         try {
            date = LocalDate.parse( dateText, SHORT_DATE_FORMATTER );
         } catch ( DateTimeParseException dtpE ) {
            LOGGER.warn( "Bad File date yyMMdd " + dateText );
         }
      } else {
         LOGGER.warn( "Bad File date length " + dateText );
      }
      if ( date != null ) {
         return date.atStartOfDay();
      }
      return RUN_START_DT;
   }

   // TODO move to abstract reader

   /**
    * {@inheritDoc}
    */
   @Override
   public DateFormat getDateFormat() {
//      return new SimpleDateFormat( _dateFormat );
      return new SimpleDateFormat( _casDateFormat );
   }


   protected String createDocumentTime( final LocalDate date ) {
      return _casDateFormatter.format( date );
   }

   protected String createDocumentTime( final LocalDateTime dateTime ) {
      return _casDateFormatter.format( dateTime );
   }

   /**
    * @param millis -
    * @return the file's last modification date as a string : {@link #getDateFormat()}
    */
   // TODO move to super class
   protected String createDocumentTime( final long millis ) {
//      _dateFormatter = DateTimeFormatter.ofPattern( _dateFormat );
//      return _dateFormatter.format(
      return _casDateFormatter.format(
              LocalDateTime.ofInstant( Instant.ofEpochMilli( millis ), ZONE_ID ) );
   }


}