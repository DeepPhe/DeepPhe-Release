package org.healthnlp.deepphe.fact;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/28/2018
 */
public interface ProvenanceOwner {

   List<Fact> getProvenanceFacts();

   default void addProvenanceFact( final Fact fact ) {
      final List<Fact> provenanceFacts = getProvenanceFacts();
      if ( provenanceFacts.contains( fact ) ) {
         return;
      }
      provenanceFacts.add( fact );
      provenanceFacts.addAll( fact.getProvenanceFacts() );
   }

   default void addProvenanceFacts( final List<Fact> facts ) {
      facts.forEach( this::addProvenanceFact );
   }

   List<TextMention> getProvenanceText();

   default List<String> getProvenanceMentions() {
      return getProvenanceText().stream()
                                .map( TextMention::toString )
                                .collect( Collectors.toList() );
   }

   default void addProvenanceText( final TextMention mention ) {
      if ( !getProvenanceText().contains( mention ) ) {
         getProvenanceText().add( mention );
      }
   }

}
