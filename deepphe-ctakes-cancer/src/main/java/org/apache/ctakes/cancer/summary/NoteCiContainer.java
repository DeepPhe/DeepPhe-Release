package org.apache.ctakes.cancer.summary;

import org.apache.ctakes.core.note.NoteSpecs;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/11/2019
 */
final public class NoteCiContainer extends AbstractCiContainer {

   static private final Logger LOGGER = Logger.getLogger( "NoteCiSummary" );

   static private final String NOTE_TYPE = "Document";

   static private AtomicLong _ID_NUM = new AtomicLong( 0 );

   private final NoteSpecs _noteSpecs;
   private final String _episodeType;


   /**
    * @param noteSpecs -
    */
   public NoteCiContainer( final NoteSpecs noteSpecs, final String episodeType ) {
      super( NOTE_TYPE, noteSpecs.getDocumentType(), noteSpecs.getDocumentId() );
      _noteSpecs = noteSpecs;
      _episodeType = episodeType;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final long createUniqueIdNum() {
      return _ID_NUM.incrementAndGet();
   }

   public NoteSpecs getNoteSpecs() {
      return _noteSpecs;
   }

   public String getEpisodeType() {
      return _episodeType;
   }

}
