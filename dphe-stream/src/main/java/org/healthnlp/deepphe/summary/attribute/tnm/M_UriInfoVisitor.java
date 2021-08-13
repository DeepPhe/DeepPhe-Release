package org.healthnlp.deepphe.summary.attribute.tnm;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.stream.Collectors;

final public class M_UriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _mConcepts;

   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _mConcepts == null ) {
         _mConcepts = neoplasms.stream()
                                   .map( c -> c.getRelated( RelationConstants.HAS_CLINICAL_M ) )
                                   .flatMap( Collection::stream )
                                   .collect( Collectors.toSet() );
         neoplasms.stream()
                  .map( c -> c.getRelated( RelationConstants.HAS_PATHOLOGIC_M ) )
                  .forEach( _mConcepts::addAll );
      }
      return _mConcepts;
   }

}
