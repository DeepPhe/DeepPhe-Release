package org.apache.ctakes.core.ae;

import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer;
import com.mdimension.jchronic.utils.Span;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2018
 */
@PipeBitInfo(
      name = "DocTimeApproximator",
      description = "Sets the document time based upon the latest normalized date.",
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class DocTimeApproximator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DocTimeApproximator" );

   static private final Calendar NULL_CALENDAR = new Calendar.Builder().setDate( 1, 1, 1 ).build();
   static private final Options PAST_OPTIONS = new Options( Pointer.PointerType.PAST );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final SourceData sourceData = SourceMetadataUtil.getOrCreateSourceData( jCas );
      final String docTime = sourceData.getSourceOriginalDate();
      if ( docTime != null && !docTime.isEmpty() ) {
         LOGGER.info( "Document Time is " + docTime );
         return;
      }

      final Collection<Calendar> calendars = new HashSet<>();
      final Collection<TimeMention> timeMentions = JCasUtil.select( jCas, TimeMention.class );
      for ( TimeMention timeMention : timeMentions ) {
         boolean gotDate = false;
         final org.apache.ctakes.typesystem.type.refsem.Date typeDate = timeMention.getDate();
         if ( typeDate != null ) {
            final int year = parseInt( typeDate.getYear() );
            final int month = parseInt( typeDate.getMonth() );
            final int day = parseInt( typeDate.getDay() );
            if ( year == Integer.MIN_VALUE || month == Integer.MIN_VALUE || day == Integer.MIN_VALUE ) {
               continue;
            }
            LOGGER.info( "TimeMention Date " + year + "" + month + "" + day );
            calendars.add( new Calendar.Builder().setDate( year, month - 1, day ).build() );
            gotDate = true;
         }
         if ( !gotDate ) {
            calendars.add( getCalendar( timeMention.getCoveredText() ) );
         }
      }

      JCasUtil.select( jCas, DateAnnotation.class ).stream()
              .map( Annotation::getCoveredText )
              .map( DocTimeApproximator::getCalendar )
              .forEach( calendars::add );

      if ( calendars.isEmpty() ) {
         LOGGER.info( "Could not parse Document Time." );
         return;
      }
      final Calendar nineteen = new Calendar.Builder().setDate( 1900, 0, 1 ).build();
      final Calendar now = Calendar.getInstance();
      now.add( Calendar.DAY_OF_MONTH, -1 );
      final List<Calendar> calendarList = calendars.stream()
                                                   .filter( c -> !NULL_CALENDAR.equals( c ) )
                                                   .filter( c -> c.compareTo( nineteen ) > 0 )
                                                   .filter( c -> c.compareTo( now ) < 0 )
                                                   .distinct()
                                                   .sorted()
                                                   .collect( Collectors.toList() );
      if ( calendarList.isEmpty() ) {
         LOGGER.info( "Could not parse Document Time." );
         return;
      }
      final Calendar lastCalendar = calendarList.get( calendarList.size() - 1 );
      final String parsedDocTime = String.format( "%04d%02d%02d1200",
            lastCalendar.get( Calendar.YEAR ),
            lastCalendar.get( Calendar.MONTH ) + 1,
            lastCalendar.get( Calendar.DAY_OF_MONTH ) );
      sourceData.setSourceOriginalDate( parsedDocTime );
      LOGGER.info( "Parsed Document Time is " + parsedDocTime );
   }

   static private boolean isLousyDateText( final String text ) {
      if ( text.length() < 7 ) {
         return true;
      }
      for ( char c : text.toCharArray() ) {
         if ( Character.isDigit( c ) ) {
            return false;
         }
      }
      return true;
   }

   static private Calendar getCalendar( final String text ) {
      if ( isLousyDateText( text ) ) {
         return NULL_CALENDAR;
      }
      final Span span = Chronic.parse( text, PAST_OPTIONS );
      if ( span == null ) {
         return NULL_CALENDAR;
      }
      return span.getEndCalendar();
   }

   static private int parseInt( final String text ) {
      if ( text == null || text.isEmpty() ) {
         return Integer.MIN_VALUE;
      }
      for ( char c : text.toCharArray() ) {
         if ( !Character.isDigit( c ) ) {
            return Integer.MIN_VALUE;
         }
      }
      try {
         return Integer.parseInt( text );
      } catch ( NumberFormatException nfE ) {
         return Integer.MIN_VALUE;
      }
   }

}
