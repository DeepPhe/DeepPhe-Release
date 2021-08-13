package org.healthnlp.deepphe.summary.attribute.tnm;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.stream.Collectors;

final public class T_UriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _tConcepts;

   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _tConcepts == null ) {
         _tConcepts = neoplasms.stream()
                                   .map( c -> c.getRelated( RelationConstants.HAS_CLINICAL_T ) )
                                   .flatMap( Collection::stream )
                                   .collect( Collectors.toSet() );
         neoplasms.stream()
                  .map( c -> c.getRelated( RelationConstants.HAS_PATHOLOGIC_T ) )
                  .forEach( _tConcepts::addAll );
      }
      return _tConcepts;
   }

}
