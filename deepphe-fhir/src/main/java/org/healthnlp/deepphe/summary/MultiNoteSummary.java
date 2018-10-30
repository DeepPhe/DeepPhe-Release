package org.healthnlp.deepphe.summary;

import org.apache.ctakes.core.note.NoteSpecs;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/11/2018
 */
abstract public class MultiNoteSummary extends Summary {

   static private final Logger LOGGER = Logger.getLogger( "MultiNoteSummary" );

   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyyMMddhhmm" );
   static private final DateFormat SLASH_DATE_FORMAT = new SimpleDateFormat( "yyyy/MM/dd" );
   static private final DateTimeFormatter SLASH_LOCAL_FORMAT = DateTimeFormatter.ofPattern( "yyyy/MM/dd" );

   private Collection<NoteSpecs> _noteSpecs;

   private Date _firstDate;
   private Date _lastDate;


   public MultiNoteSummary( final String id ) {
      super( id );
   }


   final public Collection<NoteSpecs> getAllNoteSpecs() {
      if ( _noteSpecs == null ) {
         _noteSpecs = new ArrayList<>();
      }
      return _noteSpecs;
   }

   final public void addNoteSpecs( final NoteSpecs noteSpecs ) {
      getAllNoteSpecs().add( noteSpecs );
      final Date noteDate = noteSpecs.getNoteDate();
      // setup dates
      if ( _firstDate == null || _firstDate.after( noteDate ) ) {
         _firstDate = noteDate;
      }
      if ( _lastDate == null || _lastDate.before( noteDate ) ) {
         _lastDate = noteDate;
      }
   }


   final public Date getFirstDate() {
      return _firstDate;
   }

   final public String getFirstDateText() {
      return getDateText( _firstDate );
   }

   final public String getFirstDateSlashText() {
      return getDateSlashText( _firstDate );
   }

   final public Date getLastDate() {
      return _lastDate;
   }

   final public String getLastDateText() {
      return getDateText( _lastDate );
   }

   final public String getLastDateSlashText() {
      return getDateSlashText( _lastDate );
   }

   final protected String getDateText( final Date date ) {
      return DATE_FORMAT.format( date );
   }

   final protected String getDateSlashText( final Date date ) {
      return SLASH_DATE_FORMAT.format( date );
   }

   final protected String getDateText( final LocalDate date ) {
      return DATE_FORMAT.format( date );
   }

   final protected String getDateSlashText( final LocalDate date ) {
      return SLASH_LOCAL_FORMAT.format( date );
   }

}
