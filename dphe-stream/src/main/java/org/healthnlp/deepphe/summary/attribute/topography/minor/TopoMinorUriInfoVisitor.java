package org.healthnlp.deepphe.summary.attribute.topography.minor;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.HashSet;


final public class TopoMinorUriInfoVisitor implements UriInfoVisitor {

   static private final int SITE_LEFT_WINDOW = 25;
   static private final int SITE_RIGHT_WINDOW = 10;

   private Collection<ConceptAggregate> _topoMinorConcepts;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _topoMinorConcepts == null ) {
         _topoMinorConcepts = new HashSet<>();
         _topoMinorConcepts.addAll( LungMinorCodifier.getLungParts( neoplasms ) );
         _topoMinorConcepts.addAll( BreastMinorCodifier.getBreastParts( neoplasms ) );
         _topoMinorConcepts.addAll( CrcMinorCodifier.getColonParts( neoplasms ) );
         _topoMinorConcepts.addAll( CrcMinorCodifier.getAnusParts( neoplasms ) );
         // Added 4/12/2022
         if ( _topoMinorConcepts.isEmpty() ) {
            return _topoMinorConcepts;
         }
         final Collection<ConceptAggregate> minors = new HashSet<>();
         for ( ConceptAggregate concept : _topoMinorConcepts ) {
            for ( Mention mention : concept.getMentions() ) {
               final int mentionBegin = mention.getBegin();
               if ( mentionBegin <= SITE_LEFT_WINDOW ) {
                  continue;
               }
               final Note note = NoteNodeStore.getInstance().get( mention.getNoteId() );
               if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
                  continue;
               }
               if ( hasExactPreText( note, mention ) || hasExactPostText( note, mention ) ) {
                  NeoplasmSummaryCreator.addDebug( "Trimming to minor candidate "
                                                          + concept.getCoveredText() + "\n" );
                  minors.add( concept );
               }
            }
         }
         if ( !minors.isEmpty() ) {
            _topoMinorConcepts.retainAll( minors );
         }
      }
      return _topoMinorConcepts;
   }

   static private boolean hasExactPreText( final Note note, final Mention mention ) {
      final int mentionBegin = mention.getBegin();
      if ( mentionBegin <= SITE_LEFT_WINDOW ) {
         return false;
      }
      final String preText = note.getText()
                                 .substring( mentionBegin - SITE_LEFT_WINDOW, mentionBegin )
                                 .toLowerCase();
      NeoplasmSummaryCreator.addDebug( "minor Candidate and pretext "
                                              + note.getText()
                                                    .substring( mentionBegin - SITE_LEFT_WINDOW,
                                                                mention.getEnd() )
                                              + "\n" );
      return preText.contains( "tumor site:" ) || preText.contains( "supportive of" );
   }

   static private boolean hasExactPostText( final Note note, final Mention mention ) {
      final int mentionEnd = mention.getEnd();
      final String noteText = note.getText();
      if ( mentionEnd + SITE_RIGHT_WINDOW > noteText.length() ) {
         return false;
      }
      final String postText = noteText
            .substring( mentionEnd, mentionEnd + SITE_RIGHT_WINDOW )
            .toLowerCase();
      NeoplasmSummaryCreator.addDebug( "minor Candidate and postext "
                                              + note.getText()
                                                    .substring( mentionEnd, mentionEnd + SITE_RIGHT_WINDOW )
                                              + "\n" );
      return postText.contains( "origin" ) || postText.contains( "primary" );
   }


}
