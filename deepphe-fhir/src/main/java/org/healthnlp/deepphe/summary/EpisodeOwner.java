package org.healthnlp.deepphe.summary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/28/2018
 */
public interface EpisodeOwner {

   Map<String, EpisodeSummary> getEpisodeMap();

   void addEpisode( final EpisodeSummary episode );

   /**
    * @return all episodes associated with this owner
    */
   default Collection<EpisodeSummary> getEpisodes() {
      final List<EpisodeSummary> list = new ArrayList<>();
      getEpisodeMap().keySet()
                     .forEach( k -> list.add( getEpisode( k ) ) );
      return list;
   }

   /**
    * get episode by episode type:
    * FHIRConstants.EPISODE_PREDIAGNOSTIC
    * FHIRConstants.EPISODE_DIAGNOSTIC
    * FHIRConstants.EPISODE_TREATMENT
    * FHIRConstants.EPISODE_FOLLOW_UP
    *
    * @param type -
    * @return -
    */
   default EpisodeSummary getEpisode( final String type ) {
      return getEpisodeMap().get( type );
   }

}
