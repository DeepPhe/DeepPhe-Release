package org.healthnlp.deepphe.summary;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.healthnlp.deepphe.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.time.LocalDate;
import java.util.Date;

public class PatientSummary extends MultiNoteSummary {

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   static private final int MIN_AGE = 20;
   static private final int MAX_AGE = 60;

   private String summaryType = getClass().getSimpleName();
   private String uuid = String.valueOf( Math.abs( hashCode() ) );

   private final String _gender;

   private LocalDate _birthday;
   private LocalDate _deathday;

   public PatientSummary( final String id ) {
      super( id );
      _gender = generateRandom( 0, 2 ) > 1 ? "Male" : "Female";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected long createUniqueIdNum() {
      synchronized ( ID_NUM_LOCK ) {
         _ID_NUM++;
         return _ID_NUM;
      }
   }

   @Override
   protected String getDefaultUri() {
      return FHIRConstants.PATIENT_SUMMARY_URI;
   }

   public FactList getName() {
      return getOrCreateFacts( FHIRConstants.HAS_NAME );
   }

   public String getNameText() {
      final FactList facts = getName();
      if ( facts.isEmpty() ) {
         return SourceMetadataUtil.UNKNOWN_PATIENT;
      }
      return facts.get( 0 ).getId();
   }

   public FactList getGender() {
      return getOrCreateFacts( FHIRConstants.HAS_GENDER );
   }

   public String getGenderText() {
      final FactList facts = getGender();
      if ( facts.isEmpty() ) {
         return UriConstants.UNKNOWN;
      }
      return facts.get( 0 ).getFullName();
   }

   public String getFilledGender() {
      return _gender;
   }


   public void initDates() {
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


   /**
    * For viz tool
    *
    * @return year/month/day
    */
   public String getBirthday() {
      initDates();
      return getDateSlashText( _birthday );
   }

   /**
    * For viz tool
    *
    * @return year/month/day
    */
   public String getDeathday() {
      initDates();
      return getDateSlashText( _deathday );
   }


   public FactList getOutcomes() {
      return getFacts( FHIRConstants.HAS_OUTCOME );
   }

   public FactList getSequenceVariant() {
      return getFacts( FHIRConstants.HAS_SEQUENCE_VARIENT );
   }

   public String getDisplayText() {
      return summaryType;
   }

   public String getSummaryType() {
      return summaryType;
   }

   public void setSummaryType( final String summaryType ) {
      this.summaryType = summaryType;
   }

   public String getUuid() {
      return uuid;
   }

   public void setUuid( final String uuid ) {
      this.uuid = uuid;
   }

	@Override
	public void cleanSummary() {
	}

   static private int generateDay( final int month ) {
      final int maxDay = (month == 2) ? 28 : 30;
      return generateRandom( 1, maxDay );
   }

   static private int generateRandom( final int min, final int max ) {
      return min + (int)(Math.random() * (max - min));
   }

}
