package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.healthnlp.deepphe.neo4j.UriConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/9/2019
 */
final public class PatientCiContainer extends AbstractCiContainer {

   static private final String PATIENT_TYPE = "Patient";

   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyyMMddhhmm" );
   static private final DateFormat SLASH_DATE_FORMAT = new SimpleDateFormat( "yyyy/MM/dd" );
   static private final DateTimeFormatter SLASH_LOCAL_FORMAT = DateTimeFormatter.ofPattern( "yyyy/MM/dd" );

   static private final int MIN_AGE = 20;
   static private final int MAX_AGE = 60;

   static private AtomicLong _ID_NUM = new AtomicLong( 0 );

   private final Collection<CancerCiContainer> _cancers;
   private final Collection<NoteCiContainer> _notes;
   private final Collection<ConceptInstance> _allInstances;

   private final String _patientName;

   private final String _gender;


   private LocalDate _birthday;
   private LocalDate _deathday;

   private Date _firstDocDate;
   private Date _lastDocDate;


   public PatientCiContainer( final String patientId,
                              final Collection<NoteCiContainer> notes,
                              final Collection<CancerCiContainer> cancers,
                              final Collection<ConceptInstance> allInstances ) {
      super( PATIENT_TYPE, UriConstants.PATIENT, patientId );
      _cancers = cancers;
      _notes = notes;
      _allInstances = allInstances;
      _patientName = createPatientName( notes );
      _gender = generateRandom( 0, 2 ) > 1 ? "Male" : "Female";
      initDocDates( notes );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final long createUniqueIdNum() {
      return _ID_NUM.incrementAndGet();
   }

   public String getId() {
      return getType() + "_" + getWorldId() + "_" + getUniqueIdNum();
   }

   public Collection<CancerCiContainer> getCancers() {
      return _cancers;
   }

   public Collection<NoteCiContainer> getNotes() {
      return _notes;
   }

   public Collection<ConceptInstance> getAllInstances() {
      return _allInstances;
   }

   static private String createPatientName( final Collection<NoteCiContainer> notes ) {
      for ( NoteCiContainer note : notes ) {
         final NoteSpecs noteSpecs = note.getNoteSpecs();
         final String name = noteSpecs.getPatientName();
         if ( name != null && !name.isEmpty() ) {
            return name;
         }
      }
      return NoteSpecs.DEFAULT_PATIENT_NAME;
   }

   public String getPatientName() {
      return _patientName;
   }

   public String getFilledGender() {
      return _gender;
   }


   public void initLifeDates() {
      if ( _birthday != null && _deathday != null ) {
         return;
      }

      final Date lastEncounter = getLastDate();
      final int nowYear = LocalDate.now().getYear();
      final int lastYear = 1900 + lastEncounter.getYear();
      final boolean isAlive = (lastYear >= nowYear - 3) || generateRandom( 0, 2 ) > 1;
      if ( isAlive ) {
         _deathday = LocalDate.now();
      } else {
         final int deathYear = generateRandom( lastYear + 1, nowYear - 1 );
         final int deathMonth = generateRandom( 1, 12 );
         _deathday = LocalDate.of( deathYear, deathMonth, generateDay( deathMonth ) );
      }

      final Date firstEncounter = getFirstDate();
      final int firstYear = 1900 + firstEncounter.getYear();
      final int birthYear = generateRandom( firstYear - MAX_AGE, firstYear - MIN_AGE );
      final int birthMonth = generateRandom( 1, 12 );
      _birthday = LocalDate.of( birthYear, birthMonth, generateDay( birthMonth ) );
   }

   private void initDocDates( final Collection<NoteCiContainer> notes ) {
      Date firstDate = null;
      Date lastDate = null;
      for ( NoteCiContainer note : notes ) {
         final NoteSpecs noteSpecs = note.getNoteSpecs();
         final Date noteDate = noteSpecs.getNoteDate();
         // setup dates
         if ( firstDate == null || firstDate.after( noteDate ) ) {
            firstDate = noteDate;
         }
         if ( lastDate == null || lastDate.before( noteDate ) ) {
            lastDate = noteDate;
         }

      }
      _firstDocDate = firstDate;
      _lastDocDate = lastDate;
   }

   /**
    * For viz tool
    *
    * @return year/month/day
    */
   public String getBirthday() {
      initLifeDates();
      return getDateSlashText( _birthday );
   }

   /**
    * For viz tool
    *
    * @return year/month/day
    */
   public String getDeathday() {
      initLifeDates();
      return getDateSlashText( _deathday );
   }

   final public Date getFirstDate() {
      return _firstDocDate;
   }

   final public String getFirstDateText() {
      return getDateText( _firstDocDate );
   }

   final public String getFirstDateSlashText() {
      return getDateSlashText( _firstDocDate );
   }

   final public Date getLastDate() {
      return _lastDocDate;
   }

   final public String getLastDateText() {
      return getDateText( _lastDocDate );
   }

   final public String getLastDateSlashText() {
      return getDateSlashText( _lastDocDate );
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

   public String toString() {
      return "=======  Patient  =======\n"
             + _cancers.stream()
                      .map( CancerCiContainer::toString )
                      .collect( Collectors.joining(  ) );
   }

   static private int generateDay( final int month ) {
      final int maxDay = (month == 2) ? 28 : 30;
      return generateRandom( 1, maxDay );
   }

   static private int generateRandom( final int min, final int max ) {
      return min + (int)(Math.random() * (max - min));
   }

}
