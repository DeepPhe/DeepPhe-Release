package org.healthnlp.deepphe.nlp.ae.section;

import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;
import static org.healthnlp.deepphe.nlp.section.DefinedSectionType.PittsburghHeader;

/**
 * Grabs the document time from the pittsburgh header
 */
final public class PittHeaderAnnotator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PittHeaderAnnotator" );

   static private final Pattern DATE_PATTERN = Pattern.compile( ".*Principal Date\\D+(\\d+)(?: (\\d+))?.*", DOTALL );

   //   private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddhhmm");
   static private final DateTimeFormatter DATE_TIME_PARSER = DateTimeFormatter.ofPattern( "yyyyMMddkkmm" );
   static private final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern( "yyyyMMdd" );

//   private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

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

   private String _runStart = "";


   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _casDateFormatter = DateTimeFormatter.ofPattern( _casDateFormat );
      _runStart = LocalDateTime.now().format( _casDateFormatter );
   }


   /**
    * Grabs the document time from the header
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Parsing the Document Header ..." );
      final Collection<Segment> sections = JCasUtil.select( jcas, Segment.class );
      final Collection<Segment> pghHeaders = new HashSet<>();
      for ( Segment section : sections ) {
         if ( !PittsburghHeader.isThisSectionType( section ) ) {
            if ( section.getPreferredText().equals( "Signature" ) ) {
               pghHeaders.add( section );
            }
            continue;
         }
         pghHeaders.add( section );
         final String text = section.getCoveredText();
         final Matcher dateMatcher = DATE_PATTERN.matcher( text );
         if ( dateMatcher.matches() ) {
            final int dateBegin = dateMatcher.start( 1 );
            // The header may not have a time.
            final int dateEnd = (dateMatcher.end( 2 ) > dateBegin)
                  ? dateMatcher.end( 2 )
                  : dateMatcher.end( 1 );
//            String simplestDate = text.substring( dateBegin, dateEnd ).replace( " ", "" );
//            //   Check for time, using 12:00 as a default if it isn't there.
//            if ( simplestDate.length() != 12 ) {
//               simplestDate += "1200";
//            }
//            String sourceDataDate = simplestDate;
//            try {
//               final Date date = DATE_FORMAT.parse( simplestDate );
//               sourceDataDate = TIMESTAMP_FORMAT.format( date );
//            } catch ( ParseException pE ) {
//               // Ignore
//            }
            final String docTimeText = text.substring( dateBegin, dateEnd ).replace( " ", "" );
            final String casTimeText = getCasTimeText( docTimeText );
            SourceMetadataUtil.getOrCreateSourceData( jcas ).setSourceOriginalDate( casTimeText );
            SourceMetadataUtil.getOrCreateSourceData( jcas ).setSourceRevisionDate( casTimeText );
            LOGGER.info( "Setting cas date date to " + casTimeText );
         }
//         break;
      }
      // We don't want the Pitt headers to hang around for the rest of our processing.
      pghHeaders.forEach( Segment::removeFromIndexes );
   }


   private String getCasTimeText( String docTimeText ) {
      if ( docTimeText == null || docTimeText.isEmpty() ) {
         return _runStart;
      }
      if ( docTimeText.length() == 8 ) {
         docTimeText = docTimeText + "1200";
      }
      LocalDateTime dateTime;
      try {
         dateTime = LocalDateTime.parse( docTimeText, DATE_TIME_PARSER );
      } catch ( DateTimeParseException dtpE ) {
         final LocalDate date = LocalDate.parse( docTimeText, DATE_PARSER );
         dateTime = date.atStartOfDay();
      }
      return dateTime.format( _casDateFormatter );
   }


}
