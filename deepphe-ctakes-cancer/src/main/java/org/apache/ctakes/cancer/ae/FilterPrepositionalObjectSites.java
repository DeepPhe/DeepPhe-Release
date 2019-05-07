package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Removes anatomic site annotations that appear within a preposition phrase
 * as the object of certain prepositions provided there is a site in the noun phrase
 * just before the preposition
 *
 * @author JJM , chip-nlp
 * @version %I%
 * @since 8/27/2018
 */
final public class FilterPrepositionalObjectSites extends JCasAnnotator_ImplBase {

    private static final String CLASS_NAME = MethodHandles.lookup().lookupClass().getSimpleName();

    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public void processCas(final JCas cas) {

        LOGGER.info("Removing sites that appear as the object of certain prepositions   ....");

        // For each PP (preposition phrase) if there is a site within the PP and
        // the text of the first word, ignoring case, of the preposition phrase is "in",
        // or another preposition that indicates the object of the preposition is of less interest,
        // look for another site in the "chunk" just before the PP.
        // If there is such a site, toss out the site that is part of the PP (the site will be in a NP after the PP Chunk)
        // This is so we ignore site such as "abdomen" in the phrase like "lymph node in the abdomen"

        final Collection<Chunk> phrases = JCasUtil.select(cas, Chunk.class);
        final Map<Annotation, Collection<AnatomicalSiteMention>> toRemove = new LinkedHashMap<>();
        Chunk previous = null;
        Chunk prevPrev = null;
       Chunk prevPrevPrev = null;
        for (Chunk phrase: phrases) {
            if (previous!=null && prevPrev!=null) {
                // Check if the phrase is a prepositional phrase started by "in"
                // or one of the other prepositions we want to treat specially
               if ( isPrepositionalPhraseOfInterest( previous ) ) {
                  final Collection<AnatomicalSiteMention> sitesBeforeIn
                        = findSitesInPhraseBeforeAnnotation( cas, prevPrev, previous );
                  if ( sitesBeforeIn != null && !sitesBeforeIn.isEmpty() ) {
                     final Collection<AnatomicalSiteMention> sitesObjectOfIn = findSitesInPhraseAfterAnnotation(cas, phrase, previous);
                     for (Annotation prepSite : sitesObjectOfIn) {
                        toRemove.put(prepSite, sitesBeforeIn);
                        LOGGER.debug( "Found a site to remove " + toStringInclText(prepSite));
                     }
                  }
               } else if ( prevPrevPrev != null && isToOfInterest( prevPrev, previous ) ) {
                  final Collection<AnatomicalSiteMention> sitesBeforeTo
                        = findSitesInPhraseBeforeAnnotation( cas, prevPrevPrev, previous );
                  if ( sitesBeforeTo != null && !sitesBeforeTo.isEmpty() ) {
                     final Collection<AnatomicalSiteMention> sitesObjectOfTo
                           = findSitesInPhraseAfterAnnotation( cas, phrase, previous );
                     for ( Annotation prepSite : sitesObjectOfTo ) {
                        toRemove.put( prepSite, sitesBeforeTo );
                        LOGGER.debug( "Found a site to remove " + toStringInclText( prepSite ) );
                     }
                  }

               }
            }
           prevPrevPrev = prevPrev;
            prevPrev = previous;
            previous = phrase;

        }
        for (Annotation toRmv: toRemove.keySet()) {
            Collection<AnatomicalSiteMention> leaving = toRemove.get(toRmv);
            LOGGER.info("Removing anatomic site " + toStringInclText(toRmv)+ ",");
            toRmv.removeFromIndexes();
        }

        LOGGER.info("Finished processing.");
    }

    // include the covered text
    private String toStringInclText(Annotation anno) {
       return anno.getCoveredText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final JCas cas) throws AnalysisEngineProcessException {
        try {
            processCas(cas);
        } catch (NullPointerException npe) {
            throw new AnalysisEngineProcessException(npe);
        }

    }

    private static Collection<String> prepositionsOfInterest = Arrays.asList(
            "near",
            "under",
            "over",
          "above",
            "below",
            "between",
            "in"
            );

   private static Collection<String> adjectivePhrasesTo = Arrays.asList(
         "adjacent",
         "anterior",
         "superior"
   );

   private boolean isPrepositionalPhraseOfInterest( final Chunk potentialPP ) {
      if ( !potentialPP.getChunkType().equals( "PP" ) ) {
         return false;
      }
        LOGGER.trace("ChunkType " + potentialPP.getChunkType() + " for '" + potentialPP.getCoveredText() + "'");
      String word = potentialPP.getCoveredText().toLowerCase();
      word = word.replaceAll( "just ", "" );
      return prepositionsOfInterest.contains( word );
   }

   private boolean isToOfInterest( final Chunk potentialAdjp, final Chunk potentialPpTo ) {
      if ( !potentialPpTo.getChunkType().equals( "PP" )
           || !potentialPpTo.getCoveredText().toLowerCase().equals( "to" )
           || !potentialAdjp.getChunkType().equals( "ADJP" ) ) {
         return false;
      }
      LOGGER.info( "Have 'ADJP PP' " + potentialAdjp.getCoveredText() + " to" );
      String word = potentialAdjp.getCoveredText().toLowerCase();
      word = word.replaceAll( "just ", "" );
      return adjectivePhrasesTo.contains( word );
   }

    private Collection<AnatomicalSiteMention> findSitesInPhrase(JCas jCas, Chunk phrase) {
        Collection<AnatomicalSiteMention> sites = JCasUtil.selectCovered(jCas, AnatomicalSiteMention.class, phrase);
        return sites;
    }

    // pancreas in abdomen will have
    // NP pancreas in abdomen
    // PP          in
    // NP             abdomen
    // so we want to find sites in the 1st NP that are not in the NP just after the PP
    private Collection<AnatomicalSiteMention> findSitesInPhraseBeforeAnnotation( final JCas cas,
                                                                                 final Chunk phrase,
                                                                                 final Annotation nextAnnotation ) {
       return findSitesInPhrase( cas, phrase ).stream()
                                              .filter( s -> s.getBegin() < nextAnnotation.getBegin() )
                                              .collect( Collectors.toSet() );
    }

    // pancreas in abdomen will have
    // NP pancreas in abdomen
    // PP          in
    // NP             abdomen
    // so we want to find sites in the 1st NP that are not in the NP just after the PP
    private Collection<AnatomicalSiteMention> findSitesInPhraseAfterAnnotation( final JCas cas,
                                                                                final Chunk phrase,
                                                                                final Annotation precedingAnnotation ) {
       return findSitesInPhrase( cas, phrase ).stream()
                                              .filter( s -> s.getBegin() >= precedingAnnotation.getBegin() )
                                              .collect( Collectors.toSet() );
    }


}
