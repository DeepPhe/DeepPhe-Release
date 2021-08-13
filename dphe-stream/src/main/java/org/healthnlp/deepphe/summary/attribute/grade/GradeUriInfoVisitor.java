package org.healthnlp.deepphe.summary.attribute.grade;

import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

final public class GradeUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _gradeConcepts;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _gradeConcepts == null ) {
         _gradeConcepts = neoplasms.stream()
                                   .map( c -> c.getRelated( HAS_GLEASON_SCORE,
                                                DISEASE_IS_GRADE,
                                                DISEASE_HAS_FINDING ) )
                                   .flatMap( Collection::stream )
                                   .filter( c -> GradeCodeInfoStore.getGradeNumber( c ) >= 0  )
                                   .collect( Collectors.toSet() );
      }
      return _gradeConcepts;
   }

   /**
    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
    * @param neoplasms -
    * @return the grade score (0-5) * 20.
    */
   @Override
   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> attributes = getAttributeConcepts( neoplasms );
      final Map<String,Integer> uriStrengths = attributes.stream()
                     .map( ConceptAggregate::getAllUris )
                     .flatMap( Collection::stream )
                     .distinct()
                     .collect( Collectors.toMap( Function.identity(),
                                                 GradeUriInfoVisitor::getGradeStrength ) );
      UriInfoVisitor.applySectionAttributeUriStrengths( attributes, uriStrengths );
      UriInfoVisitor.applyHistoryAttributeUriStrengths( attributes, uriStrengths );
      return uriStrengths;
   }

   static private int getGradeStrength( final String uri ) {
      if ( uri.startsWith( "Gleason_Score_" ) ) {
         // 60 - 90, Gleason should be favored.
         final int gleason = GradeCodeInfoStore.getUriGradeNumber( uri );
         if ( gleason < 0 ) {
            return 0;
         }
         return Math.max( 0, Math.min( 100, 50+(gleason*10) ) );
      }
      // Other grade types are not quite as favored as the more exact gleason score.
      return Math.max( 0, Math.min( 100, GradeCodeInfoStore.getUriGradeNumber( uri )*10 ) );
   }

}
