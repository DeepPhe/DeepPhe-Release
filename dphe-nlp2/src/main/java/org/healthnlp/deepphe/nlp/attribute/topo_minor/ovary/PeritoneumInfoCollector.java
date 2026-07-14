package org.healthnlp.deepphe.nlp.attribute.topo_minor.ovary;


import org.healthnlp.deepphe.nlp.attribute.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 *
 * NOTE:  Peritoneum is treated as a tissue, therefore this will never be reached.
 *
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class PeritoneumInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<UriConceptRelation> getRelations() {
      return getNeoplasm().getUriConceptRelations( getRelationTypes() )
                          .stream()
                  .filter( PeritoneumInfoCollector::hasPeritoneumTarget )
                  .collect( Collectors.toSet() );
   }

   static private boolean hasPeritoneumTarget( final UriConceptRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return OvaryUriCollection.getInstance().getAllPeritoneumUris().contains( uri );
   }

}
