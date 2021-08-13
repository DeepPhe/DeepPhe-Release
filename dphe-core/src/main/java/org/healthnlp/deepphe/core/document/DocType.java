package org.healthnlp.deepphe.core.document;


import org.apache.ctakes.typesystem.type.textspan.Segment;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/12/2018
 */
public interface DocType {


   Collection<SectionType> getWantedSections();

   boolean isThisDocType( String code );

   default boolean isWantedSection( final Segment section ) {
      final String sectionName = section.getPreferredText();
      return sectionName != null
             && !sectionName.isEmpty()
             && isWantedSection( sectionName.trim() );
   }

   default boolean isWantedSection( final String sectionName ) {
      final SectionType sectionType = SectionType.getSectionType( sectionName );
      return getWantedSections().contains( sectionType );
   }

}
