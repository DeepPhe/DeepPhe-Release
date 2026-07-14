package org.healthnlp.deepphe.nlp.attribute.topo_minor.lung;

import org.healthnlp.deepphe.nlp.attribute.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class LungInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<UriConceptRelation> getRelations() {
      return getNeoplasm().getUriConceptRelations( getRelationTypes() )
                          .stream()
                          .filter( LungInfoCollector::hasLungTarget )
                          .collect( Collectors.toSet() );
   }

   static private boolean hasLungTarget( final UriConceptRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return LungUriCollection.getInstance().getAllLungUris().contains( uri );
   }

}
