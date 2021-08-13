package org.healthnlp.deepphe.nlp.ae.section;

import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;
import static org.healthnlp.deepphe.core.document.SectionType.PittsburghHeader;

/**
 * Grabs the document time from the pittsburgh header
 */
final public class PittHeaderAnnotator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PittHeaderAnnotator" );

   static private final Pattern DATE_PATTERN = Pattern.compile( ".*Principal Date\\D+(\\d+)(?: (\\d+))?.*", DOTALL );

   private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddhhmm");
   private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
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
            String simplestDate = text.substring( dateBegin, dateEnd ).replace( " ", "" );
            //   Check for time, using 12:00 as a default if it isn't there.
            if ( simplestDate.length() != 12 ) {
               simplestDate += "1200";
            }
            String sourceDataDate = simplestDate;
            try {
               final Date date = DATE_FORMAT.parse( simplestDate );
               sourceDataDate = TIMESTAMP_FORMAT.format( date );
            } catch ( ParseException pE ) {
               // Ignore
            }
            SourceMetadataUtil.getOrCreateSourceData( jcas ).setSourceOriginalDate( sourceDataDate );
         }
//         break;
      }
      // We don't want the Pitt headers to hang around for the rest of our processing.
      pghHeaders.forEach( Segment::removeFromIndexes );
   }


}
