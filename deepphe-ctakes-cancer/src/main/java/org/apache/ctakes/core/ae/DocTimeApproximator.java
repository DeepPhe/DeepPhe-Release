package org.apache.ctakes.core.ae;

import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer;
import com.mdimension.jchronic.utils.Span;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.ctakes.temporal.utils.CalendarUtil;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.temporal.utils.CalendarUtil.NULL_CALENDAR;

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



   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final SourceData sourceData = SourceMetadataUtil.getOrCreateSourceData( jCas );
      final String docTime = sourceData.getSourceOriginalDate();
      if ( docTime != null && !docTime.isEmpty() ) {
         return;
      }

      final List<Pair<Integer>> spans = new ArrayList<>();
      final Collection<Calendar> calendars = new ArrayList<>();
      final Collection<Annotation> annotations = JCasUtil.select( jCas, Annotation.class );
      for ( Annotation annotation : annotations ) {
         final Pair<Integer> span = new Pair<>( annotation.getBegin(), annotation.getEnd() );
         if ( (annotation instanceof DateAnnotation || annotation instanceof TimeMention)
              && !spans.contains( span ) ) {
            final Calendar calendar = CalendarUtil.createTimexCalendar( annotation );
            if ( !NULL_CALENDAR.equals( calendar ) ) {
               spans.add( span );
               calendars.add( calendar );
            }
         } else if ( annotation instanceof Chunk ) {
            final Calendar calendar = CalendarUtil.getTextCalendar( annotation.getCoveredText() );
            if ( !NULL_CALENDAR.equals( calendar ) ) {
               spans.add( span );
               calendars.add( calendar );
            }
         }
      }

      if ( calendars.isEmpty() ) {
         return;
      }

      final Calendar nineteen = new Calendar.Builder().setDate( 1900, 0, 1 ).build();
      final Calendar now = Calendar.getInstance();
      final List<Calendar> calendarList = calendars.stream()
                                                   .filter( c -> !NULL_CALENDAR.equals( c ) )
                                                   .filter( c -> c.compareTo( nineteen ) > 0 )
                                                   .filter( c -> c.compareTo( now ) <= 0 )
                                                   .distinct()
                                                   .sorted()
                                                   .collect( Collectors.toList() );
      if ( calendarList.isEmpty() ) {
         return;
      }
//      NoteSpecs expects the following date format
      final Calendar lastCalendar = calendarList.get( calendarList.size() - 1 );
      final String parsedDocTime = String.format( "%04d-%02d-%02d 12:00:00",
            lastCalendar.get( Calendar.YEAR ),
            lastCalendar.get( Calendar.MONTH ) + 1,
            lastCalendar.get( Calendar.DAY_OF_MONTH ) );
      sourceData.setSourceOriginalDate( parsedDocTime );
   }

}
