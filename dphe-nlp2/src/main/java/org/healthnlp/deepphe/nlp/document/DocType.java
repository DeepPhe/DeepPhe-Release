package org.healthnlp.deepphe.nlp.document;


import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.healthnlp.deepphe.nlp.section.DefinedSectionType;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/12/2018
 */
public interface DocType {


   Collection<DefinedSectionType> getWantedSections();

   boolean isThisDocType( String code );

   default boolean isWantedSection( final Segment section ) {
      final String sectionName = section.getPreferredText();
      return sectionName != null
             && !sectionName.isEmpty()
             && isWantedSection( sectionName.trim() );
   }

   default boolean isWantedSection( final String sectionName ) {
      final DefinedSectionType sectionType = DefinedSectionType.getSectionType( sectionName );
      return getWantedSections().contains( sectionType );
   }

}
