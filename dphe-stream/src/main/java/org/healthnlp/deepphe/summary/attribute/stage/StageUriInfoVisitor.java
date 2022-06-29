package org.healthnlp.deepphe.summary.attribute.stage;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

final public class StageUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _stageConcepts;

   static private final int STAGE_WINDOW = 16;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _stageConcepts == null ) {
         _stageConcepts = neoplasms.stream()
                                   .map( c -> c.getRelated( RelationConstants.HAS_STAGE ) )
                                   .flatMap( Collection::stream )
//                                   .filter( c -> !c.isNegated() )
                                   .collect( Collectors.toSet() );


         //  Added 3/31/2022
         //  If text contains "histologic grade: [type]" for any detected aggregates only those are returned.
         final Collection<ConceptAggregate> stages = new HashSet<>();
         for ( ConceptAggregate aggregate : _stageConcepts ) {
            for ( Mention mention : aggregate.getMentions() ) {
               final int mentionBegin = mention.getBegin();
               if ( mentionBegin <= STAGE_WINDOW ) {
                  continue;
               }
               final Note note = NoteNodeStore.getInstance().get( mention.getNoteId() );
               if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
                  continue;
               }
               if ( note.getText()
                        .substring( mentionBegin-STAGE_WINDOW, mentionBegin )
                        .toLowerCase()
                        .contains( "figo stage:" ) ) {
                  stages.add( aggregate );
                  break;
               }
            }
         }
         if ( !stages.isEmpty() ) {
            _stageConcepts.retainAll( stages );
         }


      }
      return _stageConcepts;
   }

}
