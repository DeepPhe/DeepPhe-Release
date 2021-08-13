package org.healthnlp.deepphe.summary.attribute.tnm;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.stream.Collectors;

final public class N_UriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _nConcepts;

   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _nConcepts == null ) {
         _nConcepts = neoplasms.stream()
                                   .map( c -> c.getRelated( RelationConstants.HAS_CLINICAL_N ) )
                                   .flatMap( Collection::stream )
                                   .collect( Collectors.toSet() );
         neoplasms.stream()
                  .map( c -> c.getRelated( RelationConstants.HAS_PATHOLOGIC_N ) )
                  .forEach( _nConcepts::addAll );
      }
      return _nConcepts;
   }

}
