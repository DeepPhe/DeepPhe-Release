package org.apache.ctakes.dictionary.lookup.cased.wsd;


import org.apache.ctakes.dictionary.lookup.cased.lookup.DiscoveredTerm;
import org.apache.ctakes.dictionary.lookup.cased.util.textspan.MagicTextSpan;

import java.util.*;
import java.util.function.Function;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2020
 */
final public class WsdUtil {

   private WsdUtil() {
   }


   static private final Function<DiscoveredTerm, Integer> skipCompared = d -> 100 - d.getTotalSkips();
   static private final Function<DiscoveredTerm, Integer> consecutiveSkipCompared = d -> 100 - d.getConsecutiveSkips();
   static private final Function<DiscoveredTerm, Integer> rankCompared = d -> 100 - d.getOtherCuisCount();

   static public Map<MagicTextSpan, Collection<DiscoveredTerm>> getSemanticWsdSpanTerms(
         final Collection<DiscoveredTerm> semanticTerms,
         final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap ) {
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> spanTermsMap = new HashMap<>();
      for ( DiscoveredTerm term : semanticTerms ) {
         final Collection<MagicTextSpan> spans = termSpanMap.get( term );
         for ( MagicTextSpan span : spans ) {
            spanTermsMap.computeIfAbsent( span, s -> new HashSet<>() ).add( term );
         }
      }

      final Map<MagicTextSpan, Collection<DiscoveredTerm>> wsdRemovals = new HashMap<>();
      for ( Map.Entry<MagicTextSpan, Collection<DiscoveredTerm>> spanTerms : spanTermsMap.entrySet() ) {
         if ( spanTerms.getValue().size() < 2 ) {
            continue;
         }
         final DiscoveredTerm best = spanTerms.getValue().stream()
                                              .max( Comparator.comparing( DiscoveredTerm::getCapsTypeMatchCount )
                                                              .thenComparing( skipCompared )
                                                              .thenComparing( consecutiveSkipCompared )
                                                              .thenComparing( DiscoveredTerm::getTopRank )
                                                              .thenComparing( DiscoveredTerm::getEntryCounts )
                                                              .thenComparing( rankCompared ) )
                                              .orElse( null );
         if ( best != null ) {
            wsdRemovals.computeIfAbsent( spanTerms.getKey(), s -> new HashSet<>() )
                       .addAll( spanTerms.getValue() );
            wsdRemovals.get( spanTerms.getKey() ).remove( best );
         }
      }
      return wsdRemovals;
   }

}
