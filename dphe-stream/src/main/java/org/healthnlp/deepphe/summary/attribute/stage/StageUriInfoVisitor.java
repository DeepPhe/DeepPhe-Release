package org.healthnlp.deepphe.summary.attribute.stage;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.stream.Collectors;

final public class StageUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _stageConcepts;

   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _stageConcepts == null ) {
         _stageConcepts = neoplasms.stream()
                                   .map( c -> c.getRelated( RelationConstants.HAS_STAGE ) )
                                   .flatMap( Collection::stream )
//                                   .filter( c -> !c.isNegated() )
                                   .collect( Collectors.toSet() );
      }
      return _stageConcepts;
   }

}
