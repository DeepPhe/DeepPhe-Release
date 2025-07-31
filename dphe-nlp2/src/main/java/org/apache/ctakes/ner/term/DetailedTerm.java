package org.apache.ctakes.ner.term;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.ner.detail.Details;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
final public class DetailedTerm {

   private final Pair<Integer> _textSpan;
   private final DiscoveredTerm _discoveredTerm;
   private final Collection<Details> _allDetails;

   public DetailedTerm( final Pair<Integer> textSpan,
                        final DiscoveredTerm discoveredTerm,
                        final Collection<Details> allDetails ) {
      _textSpan = textSpan;
      _discoveredTerm = discoveredTerm;
      _allDetails = allDetails;
   }

   public Pair<Integer> getTextSpan() {
      return _textSpan;
   }

   public String getUri() {
      return _discoveredTerm.getUri();
   }

   public double getScore() {
      return _discoveredTerm.calculateScore();
   }

   public Collection<Details> getDetails() {
      return _allDetails;
   }

   public Collection<String> getGroupNames() {
      return _allDetails.stream()
                 .map( Details::getGroupName )
                 .filter( Objects::nonNull )
                 .filter( g -> !g.isEmpty() )
                 .collect( Collectors.toSet() );
   }

}
