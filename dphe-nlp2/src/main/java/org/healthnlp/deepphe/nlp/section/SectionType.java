package org.healthnlp.deepphe.nlp.section;


import org.apache.ctakes.typesystem.type.textspan.Segment;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/12/2018
 */
public interface SectionType {

   String getName();

   default boolean isThisSectionType( final Segment segment ) {
      final String sectionName = segment.getPreferredText();
      return sectionName != null
             && !sectionName.isEmpty()
             && isThisSectionType( sectionName.trim() );
   }

   default boolean isThisSectionType( final String sectionName ) {
      return isSectionName( this, sectionName );
   }

   default boolean isSectionName( final SectionType sectionType, final String sectionName ) {
      return sectionType.getName().equalsIgnoreCase( sectionName );
   }

}
