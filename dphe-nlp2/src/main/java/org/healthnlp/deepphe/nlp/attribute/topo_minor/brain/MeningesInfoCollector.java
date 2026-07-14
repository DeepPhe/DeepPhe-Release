package org.healthnlp.deepphe.nlp.attribute.topo_minor.brain;

import org.healthnlp.deepphe.nlp.attribute.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/5/2023}
 */
public class MeningesInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<UriConceptRelation> getRelations() {
      return getNeoplasm().getUriConceptRelations( getRelationTypes() )
                          .stream()
                          .filter( MeningesInfoCollector::hasMeningesTarget )
                          .collect( Collectors.toSet() );
   }

   static private boolean hasMeningesTarget( final UriConceptRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return BrainUriCollection.getInstance().getMeningesUris().contains( uri );
   }

}
