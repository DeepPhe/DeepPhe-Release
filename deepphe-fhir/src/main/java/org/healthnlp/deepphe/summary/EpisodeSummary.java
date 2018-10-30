package org.healthnlp.deepphe.summary;

import org.healthnlp.deepphe.fact.FactList;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.util.HashMap;
import java.util.Map;

public class EpisodeSummary extends MultiNoteSummary implements TumorOwner {

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private String type, episodeType;
   private TumorSummary tumorSummary;


   public EpisodeSummary( final String id ) {
      super( id );
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
    * set episode type
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


   @Override
   public TumorSummary getTumorSummary() {
      return tumorSummary;
   }

   @Override
   public void setTumorSummary( TumorSummary tumorSummary ) {
      this.tumorSummary = tumorSummary;
   }

   @Override
   public void append( final Summary summary ) {
      if ( isAppendable( summary ) ) {
         final EpisodeSummary episode = (EpisodeSummary)summary;
         episode.getAllNoteSpecs()
                .forEach( this::addNoteSpecs );
      }
   }

   public String getDisplayText() {
      return getType() != null ? getType() : super.getDisplayText();
   }

   @Override
   public Map<String, FactList> getSummaryFacts() {
      Map<String, FactList> indexedByCategory = new HashMap();
      return indexedByCategory;
   }

   // This is a long summary, including all document texts.
   public String getSummaryText() {
      final StringBuilder sb = new StringBuilder();
      sb.append( getDisplayText() )
        .append( ":\t" )
        .append( getFirstDate() )
        .append( " to " )
        .append( getLastDate() )
        .append( "\n\t\t" );
      getAllNoteSpecs().forEach( ns -> sb.append( ns.getDocumentText() )
                                         .append( ", " ) );
      sb.append( "\n" );
      return sb.toString();
   }

	@Override
	public void cleanSummary() {
		// No cleaning needed or EpisodeSummary
	}

}
