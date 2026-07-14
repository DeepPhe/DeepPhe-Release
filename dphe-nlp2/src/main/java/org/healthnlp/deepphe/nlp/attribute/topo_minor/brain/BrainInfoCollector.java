package org.healthnlp.deepphe.nlp.attribute.topo_minor.brain;

import org.healthnlp.deepphe.nlp.attribute.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/5/2023}
 */
public class BrainInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<UriConceptRelation> getRelations() {
      return getNeoplasm().getUriConceptRelations( getRelationTypes() )
                          .stream()
                          .filter( BrainInfoCollector::hasBrainTarget )
                          .collect( Collectors.toSet() );
   }

   static private boolean hasBrainTarget( final UriConceptRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return BrainUriCollection.getInstance().getBrainUris().contains( uri );
   }

}
