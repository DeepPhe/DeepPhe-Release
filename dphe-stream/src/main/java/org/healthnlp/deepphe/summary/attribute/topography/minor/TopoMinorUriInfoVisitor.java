package org.healthnlp.deepphe.summary.attribute.topography.minor;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_CLOCKFACE;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_QUADRANT;

final public class TopoMinorUriInfoVisitor implements UriInfoVisitor {

   static Collection<String> LUNG_URIS;
   static Collection<String> BRONCHUS_URIS;
   static Collection<String> UPPER_LOBE_URIS;
   static Collection<String> MIDDLE_LOBE_URIS;
   static Collection<String> LOWER_LOBE_URIS;
   static Collection<String> TRACHEA_URIS;
   static Collection<String> QUADRANT_URIS;
   static private final Predicate<ConceptAggregate> topoUri
         = c -> c.getAllUris()
                 .stream()
                 .anyMatch( u -> LUNG_URIS.contains( u )
                                 || BRONCHUS_URIS.contains( u )
                                 || TRACHEA_URIS.contains( u ) );

   private Collection<ConceptAggregate> _topoMinorConcepts;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( LUNG_URIS == null ) {
         LUNG_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Lung" );
         BRONCHUS_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Bronchus" );
         UPPER_LOBE_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Upper_Lobe_Of_The_Lung" );
         MIDDLE_LOBE_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Middle_Lobe_Of_The_Right_Lung" );
         LOWER_LOBE_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Lower_Lung_Lobe" );
         TRACHEA_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Trachea" );
         QUADRANT_URIS = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT );
      }
      if ( _topoMinorConcepts == null ) {
         final Collection<ConceptAggregate> lungPartConcepts = neoplasms.stream()
                                                                          .map( ConceptAggregate::getRelatedSites )
                                                                          .flatMap( Collection::stream )
//                                                                        .filter( c -> !c.isNegated() )
                                                                        .filter( topoUri )
                                                                          .collect( Collectors.toSet() );
         final Collection<ConceptAggregate> breastConcepts = neoplasms.stream()
                                      .map( c -> c.getRelated( HAS_CLOCKFACE, HAS_QUADRANT ) )
                                      .flatMap( Collection::stream )
//                                                                      .filter( c -> !c.isNegated() )
                                                                      .collect( Collectors.toSet() );
         breastConcepts.addAll( neoplasms.stream()
                                         .map( ConceptAggregate::getRelatedSites )
                                         .flatMap( Collection::stream )
                                         .filter( c -> QUADRANT_URIS.contains( c.getUri() ) ).collect(
                     Collectors.toSet() ) );
//         final Collection<ConceptAggregate> locations = neoplasms.stream()
//                                                                      .map( ConceptAggregate::getRelatedSites )
//                                                                      .flatMap( Collection::stream )
////                                                                      .filter( c -> !c.isNegated() )
//                                                                      .collect( Collectors.toSet() );
         _topoMinorConcepts = new HashSet<>( breastConcepts );
         _topoMinorConcepts.addAll( lungPartConcepts );
//         _topoMinorConcepts.addAll( locations );
      }
      return _topoMinorConcepts;
   }


}
