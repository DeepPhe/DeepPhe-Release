package org.healthnlp.deepphe.nlp.attribute.newInfoStore;

import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public interface AttributeInfoCollector {

   void init( UriConcept neoplasm, String... relationType );

   UriConcept getNeoplasm();

   String[] getRelationTypes();

   default Collection<UriConceptRelation> getRelations() {
//      LogFileWriter.add( "AttributeInfoCollector Relations\n   " + getNeoplasm().getUriConceptRelations( getRelationTypes() )
//                                                     .stream().map( r -> r.getType() + " : " + r.getTarget() )
//                                                     .collect( Collectors.joining("\n   ")) );
      return getNeoplasm().getUriConceptRelations( getRelationTypes() );
   }

   default Collection<UriConcept> getUriConcepts() {
      return getRelations().stream()
                           .map( UriConceptRelation::getTarget )
                           .collect( Collectors.toSet() );
   }

}
