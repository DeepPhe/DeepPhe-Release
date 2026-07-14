package org.healthnlp.deepphe.nlp.attribute.topo_minor.crc;

import org.healthnlp.deepphe.nlp.attribute.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class ColonInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<UriConceptRelation> getRelations() {
      return getNeoplasm().getUriConceptRelations( getRelationTypes() )
                          .stream()
                  .filter( ColonInfoCollector::hasColonTarget )
                  .collect( Collectors.toSet() );
   }

   static private boolean hasColonTarget( final UriConceptRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return CrcUriCollection.getInstance().getAllColonUris().contains( uri );
   }

}
