package org.apache.ctakes.cancer.ae.section;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/6/2016
 */
public enum SectionHolder {
   INSTANCE;

   static public SectionHolder getInstance() { INSTANCE.name() ; return INSTANCE; }

   static private final String CLASS_NAME = MethodHandles.lookup().lookupClass().getSimpleName();
   static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

   private final Map<String, Collection<Segment>> _hiddenSections = new ConcurrentHashMap<>();

   /**
    * @return Section Segments that have been removed from the Cas (hidden)
    */
   public Collection<Segment> getHiddenSections( final String documentId ) {
      final Collection<Segment> sections = _hiddenSections.get( documentId );
      if ( sections != null ) {
         return Collections.unmodifiableCollection( sections );
      }
      return Collections.emptyList();
   }

   /**
    * @param documentId document containing section
    * @param section    section to cache
    */
   public void addHiddenSection( final String documentId, final Segment section ) {
      Collection<Segment> sections = _hiddenSections.computeIfAbsent( documentId, s -> new HashSet<>( 1 ) );
      sections.add( section );
   }

   public void removeHiddenSection( final String documentId, final Segment section ) {
      final Collection<Segment> sections = _hiddenSections.get( documentId );
      if ( sections == null ) {
         return;
      }
      sections.remove( section );
   }

   public void removeHiddenSection( final String documentId, final String name ) {
      final Collection<Segment> sections = _hiddenSections.get( documentId );
      if ( sections == null ) {
         return;
      }
      Segment hiddenSection = null;
      for ( Segment section : sections ) {
         if ( section.getPreferredText().equals( name ) ) {
            hiddenSection = section;
            break;
         }
      }
      if ( hiddenSection != null ) {
         sections.remove( hiddenSection );
      }
   }

   public void clear( final String documentId ) {
      _hiddenSections.remove( documentId );
   }

}
