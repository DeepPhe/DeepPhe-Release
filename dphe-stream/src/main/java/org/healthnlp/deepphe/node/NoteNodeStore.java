package org.healthnlp.deepphe.node;


import org.apache.ctakes.core.store.DefaultObjectStore;
import org.apache.ctakes.core.store.ObjectStore;
import org.apache.ctakes.core.store.SelfCleaningStore;
import org.healthnlp.deepphe.neo4j.node.Note;

import java.util.List;

/**
 * Stores Note Nodes.
 * The Note node cache is cleaned up every 15 minutes,
 * with all nodes not accessed within the last hour removed.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/4/2020
 */
public enum NoteNodeStore implements ObjectStore<Note> {
   INSTANCE;

   public static NoteNodeStore getInstance() {
      return INSTANCE;
   }


   private final ObjectStore<Note> _delegate;

   NoteNodeStore() {
      _delegate = new SelfCleaningStore<>( new DefaultObjectStore<>() );
   }

   public void close() {
      _delegate.close();
   }

   public List<String> getStoredIds() {
      return _delegate.getStoredIds();
   }

   public Note get( final String noteId ) {
      return _delegate.get( noteId );
   }

   public boolean add( final String noteId, final Note note ) {
      return _delegate.add( noteId, note );
   }

}
