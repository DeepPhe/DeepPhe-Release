package org.healthnlp.deepphe.fhir.summary;

import java.net.URI;
import java.util.*;

import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;

public class Episode extends Summary {
   private String type, episodeType;
   private Date startDate, endDate;
   private Set<Report> reports;
   private TumorSummary tumorSummary;

   public URI getConceptURI() {
      return FHIRConstants.EPISODE_URI;
   }

   /**
    * get episode type
    *
    * @return
    */
   public String getType() {
      return type;
   }

   /**
    * set episod etype
    *
    * @param type
    */
   public void setType( String type ) {
      this.type = type;
   }


   public String getEpisodeType() {
      return episodeType;
   }

   public void setEpisodeType( String episodeType ) {
      this.episodeType = episodeType;
   }

   /**
    * get start date
    *
    * @return
    */
   public Date getStartDate() {
      return startDate;
   }

   public void setStartDate( Date startDate ) {
      this.startDate = startDate;
   }

   public Date getEndDate() {
      return endDate;
   }

   public void setEndDate( Date endDate ) {
      this.endDate = endDate;
   }


   public Set<Report> getReports() {
      if ( reports == null )
         reports = new LinkedHashSet<Report>();
      return reports;
   }

   public void addReports( Report r ) {
      getReports().add( r );

      // setup dates
      if ( startDate == null || startDate.after( r.getDate() ) )
         startDate = r.getDate();
      if ( endDate == null || endDate.before( r.getDate() ) )
         endDate = r.getDate();
   }

   public void setComposition( Report r ) {
      super.setComposition( r );
      addReports( r );
   }


   public TumorSummary getTumorSummary() {
      return tumorSummary;
   }


   public void setTumorSummary( TumorSummary tumorSummary ) {
      this.tumorSummary = tumorSummary;
   }


   public boolean isAppendable( Summary s ) {
      return s instanceof Episode; // && ((Episode)s).getType().equals(getType());
   }

   public void append( Summary ep ) {
      if ( isAppendable( ep ) ) {
         Episode episode = (Episode) ep;
         for ( Report r : episode.getReports() ) {
            addReports( r );
         }
      }
   }


   public String getDisplayText() {
      return getType() != null ? getType() : super.getDisplayText();
   }

   public String getSummaryText() {
      StringBuffer st = new StringBuffer();
      st.append( getDisplayText() + ":\t" + getStartDate() + " to " + getEndDate() + "\n\t\t" );
      for ( Report r : getReports() ) {
         st.append( r.getTitle() + ", " );
      }
      st.append( "\n" );
      return st.toString();
   }

}
