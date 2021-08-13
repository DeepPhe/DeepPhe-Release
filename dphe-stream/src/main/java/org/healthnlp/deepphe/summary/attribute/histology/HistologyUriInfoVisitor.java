package org.healthnlp.deepphe.summary.attribute.histology;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_DIAGNOSIS;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_TUMOR_EXTENT;

final public class HistologyUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _histologyConcepts;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _histologyConcepts == null ) {
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                .getGraph();
         final Collection<String> neoplasmUris = UriConstants.getNeoplasmUris( graphDb );
         final Collection<ConceptAggregate> certainNeoplasm = neoplasms.stream()
//                                                                         .filter( c -> !c.isNegated() )
                                                                         .filter( c -> c.getAllUris()
                                                                                        .stream()
                                                                                        .anyMatch( neoplasmUris::contains ) )
                                                                         .collect( Collectors.toSet() );
         _histologyConcepts = new HashSet<>( certainNeoplasm );
         neoplasms.stream()
                    .map( c -> c.getRelated( HAS_DIAGNOSIS, HAS_TUMOR_EXTENT ) )
                    .flatMap( Collection::stream )
//                    .filter( c -> !c.isNegated() )
//                   .filter( c -> !c.isUncertain() )
                   .forEach( _histologyConcepts::add );
//         if ( !_histologyConcepts.isEmpty() ) {
//            return _histologyConcepts;
//         }
//         neoplasms.stream()
//                 .filter( c -> !c.isNegated() )
//                  .filter( c -> c.getAllUris()
//                                 .stream()
//                                 .anyMatch( neoplasmUris::contains ) )
//                  .forEach( _histologyConcepts::add );
//         _histologyConcepts = new HashSet<>( certainNeoplasm );
//         neoplasms.stream()
//                  .map( c -> c.getRelated( HAS_DIAGNOSIS, HAS_TUMOR_EXTENT ) )
//                  .flatMap( Collection::stream )
//                  .filter( c -> !c.isNegated() )
//                  .forEach( _histologyConcepts::add );
      }
      return _histologyConcepts;
   }


//   @Override
//   public boolean applySectionStrengths() {
//      return true;
//   }

//   /**
//    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
//    * @param neoplasms -
//    * @return the grade score (0-5) * 20.
//    */
//   @Override
//   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
//      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
//      return concepts.stream()
//                     .map( ConceptAggregate::getAllUris )
//                     .flatMap( Collection::stream )
//                     .distinct()
//                     .collect( Collectors.toMap( Function.identity(),
//                                                 HistologyUriInfoVisitor::getHistologyStrength ) );
//   }
//
//   static private int getHistologyStrength( final String uri ) {
//      return Math.max( 0, Math.min( 100, GradeCodeInfoStore.getUriGradeNumber( uri ) * 20 ) );
//   }

}
