package org.healthnlp.deepphe.uima.ae;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.summary.CancerSummary;
import org.healthnlp.deepphe.fhir.summary.Episode;
import org.healthnlp.deepphe.fhir.summary.MedicalRecord;
import org.healthnlp.deepphe.fhir.summary.TumorSummary;
import org.healthnlp.deepphe.util.FHIRConstants;

public class EpisodeClassifier {


   /**
    * get an episode representing this single document
    *
    * @return
    */
   public Episode createEpisode( TumorSummary tumor ) {
      Episode episode = new Episode();
      episode.setType( tumor.getProperty( FHIRConstants.HAS_EPISODE_TYPE ) );
      episode.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_DOCUMENT );
      episode.addReports( tumor.getReport() );
      return episode;
   }


   /**
    * classify episode type based on content of report and tumor summary
    *
    * @param report
    * @param tumor
    * @return
    */
   public String getEpisodeType( TumorSummary tumor ) {
      Report report = tumor.getReport();

      // TODO: this is an auto-generated episode type for now
      List<String> episodeTypes = new ArrayList<String>( FHIRConstants.EPISODE_TYPE_MAP.values() );
      int typeOffset = (int) Math.round( Math.random() * (episodeTypes.size() - 1) );
      return episodeTypes.get( typeOffset );
   }


   /**
    * add episode objects to medical record summary
    *
    * @param cancerSummary
    */
   public void addEpisodes( MedicalRecord record ) {
      for ( TumorSummary tumor : record.getCancerSummary().getTumors() ) {
         addEpisodes( record, tumor );
      }
   }


   /**
    * add episodes to tumor summary
    *
    * @param tumor
    */
   public void addEpisodes( MedicalRecord record, TumorSummary tumor ) {
      // lets get provinence tumors for a given summary
      for ( TumorSummary ts : getProvinenceTumors( record, tumor ) ) {
         tumor.addEpisode( createEpisode( ts ) );
      }
   }


   private List<TumorSummary> getProvinenceTumors( MedicalRecord record, TumorSummary tumor ) {
      Set<String> provinenceTumorIds = new HashSet<String>();
      for ( Fact fact : tumor.getContainedFacts() ) {
         for ( Fact prov : fact.getProvenanceFacts() ) {
            provinenceTumorIds.addAll( prov.getContainerIdentifier() );
         }
      }

      // create a list of tumor summaries
      List<TumorSummary> list = new ArrayList<TumorSummary>();
      for ( Report r : record.getReports() ) {
         for ( CancerSummary cs : r.getCancerSummaries() ) {
            for ( TumorSummary ts : cs.getTumors() ) {
               if ( provinenceTumorIds.contains( ts.getResourceIdentifier() ) ) {
                  list.add( ts );
               }
            }
         }
      }
      return list;
   }

}
