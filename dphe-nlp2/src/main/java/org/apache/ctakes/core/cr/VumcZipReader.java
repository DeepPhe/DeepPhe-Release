package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.zip.ZipEntry;


/**
 * @author SPF , chip-nlp
 * @since {1/23/2024}
 */
@PipeBitInfo (
      name = "VumcZipReader",
      description = "Reads VUMC zipped files in a directory tree and parses DocTime.",
      role = PipeBitInfo.Role.READER
)
public class VumcZipReader extends ZipFileTreeReader {
   static private final Logger LOGGER = Logger.getLogger( "VumcZipReader" );



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

   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _fileDateFormatter = DateTimeFormatter.ofPattern( _fileDateFormat );
   }


   protected String createDocumentType( final String documentId ) {
      final String[] splits = StringUtil.fastSplit( documentId, '-' );
      return splits[ 2 ];
   }

   /**
    * @param entry -
    * @return the file's last modification date as a string : {@link #getDateFormat()}
    */
   protected String createDocumentTime( final ZipEntry entry ) {
      final File entryFile = new File( entry.getName() );
      final String entryName = entryFile.getName();
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


}