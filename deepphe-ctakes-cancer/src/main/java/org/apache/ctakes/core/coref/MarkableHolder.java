package org.apache.ctakes.core.coref;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/18/2016
 */
public enum MarkableHolder {
   INSTANCE;

   public static MarkableHolder getInstance() {
      return INSTANCE;
   }

   static private final Map<String, Map<Markable, IdentifiedAnnotation>> _markedAnnotations = new ConcurrentHashMap<>();

   static public void addMarkable( final String documentId, final Markable markable, final IdentifiedAnnotation annotation ) {
      final Map<Markable, IdentifiedAnnotation> map = _markedAnnotations.computeIfAbsent( documentId, m -> new HashMap<>() );
      map.put( markable, annotation );
   }

   static public void addMarkables( final String documentId, final Map<Markable, IdentifiedAnnotation> markables ) {
      final Map<Markable, IdentifiedAnnotation> map = _markedAnnotations.computeIfAbsent( documentId, m -> new HashMap<>() );
      map.putAll( markables );
   }

   static public Collection<Markable> getMarkables( final String documentId ) {
      Map<Markable, IdentifiedAnnotation> map = _markedAnnotations.get( documentId );
      if ( map != null ) {
         return Collections.unmodifiableCollection( map.keySet() );
      }
      return Collections.emptyList();
   }

   static public Collection<IdentifiedAnnotation> getAnnotations( final String documentId ) {
      Map<Markable, IdentifiedAnnotation> map = _markedAnnotations.get( documentId );
      if ( map != null ) {
         return Collections.unmodifiableCollection( map.values() );
      }
      return Collections.emptyList();
   }

   static public Collection<IdentifiedAnnotation> getAnnotations( final String documentId,
                                                                  final Collection<IdentifiedAnnotation> markables ) {
      Map<Markable, IdentifiedAnnotation> map = _markedAnnotations.get( documentId );
      if ( map != null ) {
         return Collections.unmodifiableCollection( map.entrySet().stream()
               .filter( e -> markables.contains( e.getKey() ) )
               .map( Map.Entry::getValue )
               .collect( Collectors.toList() ) );
      }
      return Collections.emptyList();
   }

   static public void clearMarkables( final String documentId ) {
      // Attempt a brute force clearing of elements / hashcodes for better garbage collection
      final Map<Markable, IdentifiedAnnotation> map = _markedAnnotations.get( documentId );
      if ( map == null ) {
         return;
      }
      map.clear();
      _markedAnnotations.remove( documentId );
   }

}
