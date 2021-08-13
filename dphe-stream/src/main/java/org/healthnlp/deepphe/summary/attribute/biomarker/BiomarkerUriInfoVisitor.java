package org.healthnlp.deepphe.summary.attribute.biomarker;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/9/2021}
 */
final public class BiomarkerUriInfoVisitor implements UriInfoVisitor {

   final private String _biomarkerName;
   private Collection<ConceptAggregate> _biomarkerConcepts;

   public BiomarkerUriInfoVisitor( final String biomarkerName ) {
      _biomarkerName = biomarkerName;
   }

   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _biomarkerConcepts == null ) {
         Collection<ConceptAggregate> biomarkerConcepts = neoplasms.stream()
                                        .map( c -> c.getRelated( RelationConstants.has_Biomarker ) )
                                        .flatMap( Collection::stream )
                                       .filter( c -> c.getUri().equals( _biomarkerName ) )
                                        .collect( Collectors.toSet() );
         // For the "allConcepts" store the biomarker may not be tied to anything via a relation
         biomarkerConcepts.addAll( neoplasms.stream()
                                      .filter( c -> c.getUri().equals( _biomarkerName ) )
                                      .collect( Collectors.toSet() ) );
         _biomarkerConcepts = biomarkerConcepts;
      }
      return _biomarkerConcepts;
   }

}
