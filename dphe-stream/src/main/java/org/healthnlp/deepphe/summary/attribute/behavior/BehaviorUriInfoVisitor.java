package org.healthnlp.deepphe.summary.attribute.behavior;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.DISEASE_HAS_FINDING;

final public class BehaviorUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _behaviorConcepts;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _behaviorConcepts == null ) {
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                .getGraph();
         final Collection<String> malignantUris = new HashSet<>( UriConstants.getMalignantTumorUris( graphDb ) );
         // The registry (KY) uses carcinoma as proof of invasion
         malignantUris.add( "Carcinoma" );
         malignantUris.add( "Adenocarcinoma" );
         final Collection<ConceptAggregate> malignantConcepts = neoplasms.stream()
//                                                                          .filter( c -> !c.isNegated() )
                                                                          .filter( c -> c.getAllUris()
                                                                                         .stream()
                                                                                         .anyMatch( malignantUris::contains ) )
                                                                          .collect( Collectors.toSet() );
         final Collection<String> metastasisUris = UriConstants.getMetastasisUris( graphDb );
         final Collection<ConceptAggregate> metastasisConcepts = neoplasms.stream()
//                                                                          .filter( c -> !c.isNegated() )
                        .filter( c -> c.getAllUris()
                                       .stream()
                                       .anyMatch( metastasisUris::contains ) )
                          .collect( Collectors.toSet() );
         final Collection<ConceptAggregate> behaviorConcepts = neoplasms.stream()
                                      .map( c -> c.getRelated( DISEASE_HAS_FINDING ) )
                                      .flatMap( Collection::stream )
//                                                                        .filter( c -> !c.isNegated() )
                                                                        .filter( c -> BehaviorCodeInfoStore.getBehaviorNumber( c ) >= 0 )
                                      .collect( Collectors.toSet() );
         _behaviorConcepts = new HashSet<>( malignantConcepts );
         _behaviorConcepts.addAll( metastasisConcepts );
         _behaviorConcepts.addAll( behaviorConcepts );
      }
      return _behaviorConcepts;
   }


}
