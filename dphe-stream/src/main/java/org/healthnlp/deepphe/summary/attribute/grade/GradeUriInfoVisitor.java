package org.healthnlp.deepphe.summary.attribute.grade;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

final public class GradeUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _gradeConcepts;

   static private final int GRADE_WINDOW = 25;

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


         //  Added 3/31/2022
         //  If text contains "histologic grade: [type]" for any detected aggregates only those are returned.
         final Collection<ConceptAggregate> grades = new HashSet<>();
         for ( ConceptAggregate aggregate : _gradeConcepts ) {
            for ( Mention mention : aggregate.getMentions() ) {
               final int mentionBegin = mention.getBegin();
               if ( mentionBegin <= GRADE_WINDOW ) {
                  continue;
               }
               final Note note = NoteNodeStore.getInstance().get( mention.getNoteId() );
               if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
                  continue;
               }
               NeoplasmSummaryCreator.addDebug( "Grade Candidate and pretext "
                                                       + note.getText().substring( mentionBegin-GRADE_WINDOW, mention.getEnd() )
                                                       + "\n" );
               if ( note.getText()
                        .substring( mentionBegin-GRADE_WINDOW, mentionBegin )
                        .toLowerCase()
                        .contains( "histologic grade:" ) ) {
                  NeoplasmSummaryCreator.addDebug( "Trimming to grade candidate "
                                                          + aggregate.getCoveredText() + "\n" );
                  grades.add( aggregate );
                  break;
               }
            }
         }
         if ( !grades.isEmpty() ) {
            _gradeConcepts.retainAll( grades );
         }


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
