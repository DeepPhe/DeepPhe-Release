package org.healthnlp.deepphe.summary;


import org.apache.ctakes.cancer.ae.DocEpisodeTagger;
import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.type.textspan.Episode;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * A note-level summary.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/30/2018
 */
final public class NoteSummary extends Summary {

   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private List<CancerSummary> _rawCancerSummaries;
   private Collection<TumorSummary> tumorSummaries;
   private final String _episodeType;

   private NoteSpecs _noteSpecs;

   public NoteSummary( final JCas jCas, final boolean forDrools ) {
      super( DocumentIDAnnotationUtil.getDocumentID( jCas ) );

      final NoteSpecs noteSpecs = new NoteSpecs( jCas );
      setNoteSpecs( noteSpecs );

      // Document's single-most important Cancer Episode type
      _episodeType = JCasUtil.select( jCas, Episode.class ).stream()
                             .map( Episode::getEpisodeType )
                             .distinct()
                             .sorted()
                             .findFirst()
                             .orElse( DocEpisodeTagger.NO_CATEGORY );

      if ( !forDrools ) {
         return;
      }
      final CancerSummary cancerSummary = new CancerSummary( noteSpecs.getDocumentId(), noteSpecs.getPatientName() );
      // Create Tumor Summaries
      tumorSummaries = SummaryFactory.createDocTumorSummaries( jCas, noteSpecs );

      LOGGER.debug( getSummaryType() + " " + getId() + " created "
                   + tumorSummaries.size() + " tumorSummaries "
                   + getContainedFacts().size() + " facts" );

      // copy all the tumor summary facts to this NoteSummary.
      for ( TumorSummary tumorSummary : tumorSummaries ) {
         final List<String> categoryList = new ArrayList<>( tumorSummary.getContent().keySet() );
         for ( String category : categoryList ) {
            for ( Fact f : tumorSummary.getContent().get( category ) ) {
               this.addFact( category, f );
            }
         }
      }

      tumorSummaries.forEach( cancerSummary::addTumor );

      LOGGER.info( "Before Drools CancerSummary for Note: " + noteSpecs.getDocumentId() + "\n" + cancerSummary.getSummaryText() );

      _rawCancerSummaries = Collections.singletonList( cancerSummary );
   }

   final public NoteSpecs getNoteSpecs() {
      return _noteSpecs;
   }

   public void setNoteSpecs( final NoteSpecs noteSpecs ) {
      _noteSpecs = noteSpecs;
      setPatientIdentifier( noteSpecs.getPatientName() );
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

   static public final CiSorter CI_SORTER = new CiSorter();

   static private class CiSorter implements Comparator<ConceptInstance> {
      public int compare( final ConceptInstance ci1, final ConceptInstance ci2 ) {
         final String uri1 = ci1.getUri();
         final String uri2 = ci2.getUri();
         final int uriDiff = uri1.compareTo( uri2 );
         if ( uriDiff != 0 ) {
            return uriDiff;
         }
         final String text1 = ci1.toText();
         final String text2 = ci2.toText();
         int textDiff = text1.compareTo( text2 );
         if ( textDiff != 0 ) {
            return textDiff;
         }
         int span1 = ci1.getAnnotations().stream().map( a -> a.getEnd() * 10 + a.getBegin() )
                        .mapToInt( Integer::valueOf ).sum();
         int span2 = ci2.getAnnotations().stream().map( a -> a.getEnd() * 10 + a.getBegin() )
                        .mapToInt( Integer::valueOf ).sum();
         int diff = span1 - span2;
         if ( diff != 0 ) {
            return diff;
         }
         return ci1.getCoveredText().compareTo( ci2.getCoveredText() );
      }
   }

   @Override
   protected String getDefaultUri() {
      return FHIRConstants.NOTE_URI;
   }

   protected void addIdentifiersToFact( final Fact fact, final String category ) {
      // Do not add any identifiers for NoteSummary.
   }

   public String getEpisodeType() {
      return _episodeType;
   }

   /**
    *
    * @return a Cancer Summary as it exists based upon only this NoteSummary's information
    */
   public List<CancerSummary> getRawCancerSummaries() {
      return _rawCancerSummaries;
   }

   public Collection<TumorSummary> getTumorSummaries() {
      if ( tumorSummaries == null ) {
         return Collections.emptyList();
      }
      return tumorSummaries;
   }

   /**
    * Add just to NoteSummary, not to any CancerSummary
    * @param summary
    */
   public void addTumorSummary(TumorSummary summary) {
      if (tumorSummaries==null) throw new RuntimeException("tumorSummaries==null for NoteSummary " + this.getId() + ". " +
              "Before using this method, try creating tumor summaries " +
              "for this NoteSummary using SummaryFactory::createTumorSummaries first. " +
              "then if needed, create a blank one using createEmptyTumorSummary().");

      if (!tumorSummaries.contains(summary))  tumorSummaries.add(summary);
      // not all facts go with each tumor. Can have multiple tumors per note

   }

	@Override
	public void cleanSummary() {
		// no cleaning needed for NoteSummary
		
	}

}
