package org.apache.ctakes.dictionary.lookup2.consumer;

import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.concept.OwlConcept;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.MultiTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Refine a collection of dictionary terms to only contain the most specific variations:
 * "colon cancer" instead of "cancer", performed by span inclusion / complete containment, not overlap
 * Also a start at wsd by trim of overlapping terms of conflicting but related semantic group.
 * In this incarnation, any sign / symptom that is within a disease / disorder is assumed to be
 * less specific than the disease disorder and is discarded.
 * In addition, any s/s or d/d that has the same span as an anatomical site is discarded.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/24/2014
 */
final public class OwlPrecisionTermConsumer extends AbstractTermConsumer {

   static private final Logger LOGGER = Logger.getLogger( "OwlPrecisionTermConsumer" );

   private final TermConsumer _idHitConsumer;

   public OwlPrecisionTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      super( uimaContext, properties );
      _idHitConsumer = new SemanticCleanupTermConsumer( uimaContext, properties );
   }

   /**
    * Refine a collection of dictionary terms to only contain the most specific variations:
    * "colon cancer" instead of "cancer", performed by span inclusion /complete containment, not overlap
    *
    * @param cuiTerms terms in the dictionary
    * @return terms with the longest spans
    */
   static public CollectionMap<TextSpan, Long, ? extends Collection<Long>> createPreciseTerms(
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> cuiTerms,
         final Map<Long, Collection<String>> cuiUriRoots ) {

      final CollectionMap<TextSpan, Long, Set<Long>> discardCuiTerms = new HashSetMap<>();

      final List<TextSpan> textSpans = new ArrayList<>( cuiTerms.keySet() );
      final int count = textSpans.size();
      for ( int i = 0; i < count; i++ ) {
         final TextSpan spanKeyI = textSpans.get( i );
         final Collection<Long> cuisI = cuiTerms.getCollection( spanKeyI );

         for ( int j = i + 1; j < count; j++ ) {
            final TextSpan spanKeyJ = textSpans.get( j );
            if ( spanOverlaps( spanKeyI, spanKeyJ ) ) {
               final Collection<Long> cuisJ = cuiTerms.getCollection( spanKeyJ );
               for ( Long cuiI : cuisI ) {
                  for ( Long cuiJ : cuisJ ) {
                     if ( isInRoot( cuiI, cuiJ, cuiUriRoots ) ) {
                        discardCuiTerms.placeValue( spanKeyI, cuiI );
                     }
                  }
               }
            }
            if ( spanOverlaps( spanKeyJ, spanKeyI ) ) {
               final Collection<Long> cuisJ = cuiTerms.getCollection( spanKeyJ );
               for ( Long cuiI : cuisI ) {
                  for ( Long cuiJ : cuisJ ) {
                     if ( isInRoot( cuiJ, cuiI, cuiUriRoots ) ) {
                        discardCuiTerms.placeValue( spanKeyJ, cuiJ );
                     }
                  }
               }
            }
         }
      }

      final CollectionMap<TextSpan, Long, Set<Long>> preciseHitMap = new HashSetMap<>();
      for ( TextSpan textSpan : cuiTerms.keySet() ) {
         final Set<Long> cuis = new HashSet<>( cuiTerms.getCollection( textSpan ) );
         cuis.removeAll( discardCuiTerms.getCollection( textSpan ) );
         if ( !cuis.isEmpty() ) {
            preciseHitMap.put( textSpan, cuis );
         }
      }
      return preciseHitMap;
   }

   /**
    * @param cuiI        cui to check for subsumption
    * @param cuiJ        cui that might be a child of (more specific than) cuiI
    * @param cuiUriRoots collection of uri branches from cui uri to uri root
    * @return true if cuiI should be subsumed by cuiJ
    */
   static private boolean isInRoot( final Long cuiI, final Long cuiJ,
                                    final Map<Long, Collection<String>> cuiUriRoots ) {
      if ( cuiI.equals( cuiJ ) ) {
         return false;
      }
      final Collection<String> uriRootsJ = cuiUriRoots.get( cuiJ );
      if ( uriRootsJ == null ) {
         // can't be subsumed by a uri-free cui
         LOGGER.warn( "CUI " + cuiJ + " appears to have no associated uri placement" );
         return false;
      }
      final Collection<String> uriRootsI = cuiUriRoots.get( cuiI );
      if ( uriRootsI == null ) {
         // no uris ... remove cui.  Dangerous if cuiJ also has no uris
         LOGGER.warn( "CUI " + cuiI + " appears to have no associated uri placement" );
         return true;
      }
      // if uriRootsI is some subset of uriRootsJ -- closer to root, therefore a parent.
      return uriRootsI.containsAll( uriRootsJ );
   }


   static private boolean spanOverlaps( final TextSpan textSpanI, final TextSpan textSpanJ ) {
      if ( (textSpanJ.getStart() <= textSpanI.getStart() && textSpanJ.getEnd() > textSpanI.getEnd())
            || (textSpanJ.getStart() < textSpanI.getStart() && textSpanJ.getEnd() >= textSpanI.getEnd()) ) {
         // J contains I, discard less precise concepts for span I and move on to next span I
         if ( !(textSpanJ instanceof MultiTextSpan) ) {
            return true;
         }
         for ( TextSpan missingSpanKey : ((MultiTextSpan) textSpanJ).getMissingSpans() ) {
            if ( (missingSpanKey.getStart() >= textSpanI.getStart()
                  && missingSpanKey.getStart() < textSpanI.getEnd())
                  || (missingSpanKey.getEnd() > textSpanI.getStart()
                  && missingSpanKey.getEnd() <= textSpanI.getEnd()) ) {
               // I overlaps a missing span, so it is actually ok
               return false;
            }
         }
         return true;
      }
      return false;
   }


   /**
    * Refine a collection of dictionary terms to only contain the most specific variations:
    * "colon cancer" instead of "cancer", performed by span inclusion /complete containment, not overlap.
    * For instance:
    * "54 year old woman with left breast cancer."
    * in the above sentence, "breast" as part of "breast cancer" is an anatomical site and should not be a S/S
    * "Breast:
    * "lump, cyst"
    * in the above, breast is a list header, denoting findings on exam.
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas,
                            final RareWordDictionary dictionary,
                            final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                            final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException {
      // create a map of uris to cuis
      final Map<String, Long> uriCuiMap = new HashMap<>();
      for ( Map.Entry<Long, ? extends Collection<Concept>> cuiConceptEntry : cuiConcepts.entrySet() ) {
         final Long cui = cuiConceptEntry.getKey();
         cuiConceptEntry.getValue().stream()
               .map( c -> c.getCodes( OwlConcept.URI_CODING_SCHEME ) )
               .flatMap( Collection::stream )
               .forEach( uri -> uriCuiMap.put( uri, cui ) );
      }
      if ( uriCuiMap.isEmpty() ) {
         // no uris
         _idHitConsumer.consumeHits( jcas, dictionary, textSpanCuis, cuiConcepts );
         return;
      }
      // function to collect uri roots
      final Function<String, Collection<String>> getUriRoots
            = u -> OwlOntologyConceptUtil.getUriRootsStream( u ).collect( Collectors.toSet() );
      final Map<Long, Collection<String>> cuiRootsMap = uriCuiMap.keySet().stream()
            .collect( Collectors.toMap( uriCuiMap::get, getUriRoots ) );
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseTerms
            = createPreciseTerms( textSpanCuis, cuiRootsMap );
      _idHitConsumer.consumeHits( jcas, dictionary, preciseTerms, cuiConcepts );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeTypeIdHits( final JCas jcas, final String defaultScheme, final int cTakesSemantic,
                                  final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticTerms,
                                  final CollectionMap<Long, Concept, ? extends Collection<Concept>> conceptMap )
         throws AnalysisEngineProcessException {
      _idHitConsumer.consumeTypeIdHits( jcas, defaultScheme, cTakesSemantic, semanticTerms, conceptMap );
   }


}
