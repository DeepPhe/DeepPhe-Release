package org.healthnlp.deepphe.nlp.cr.naaccr;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
public interface NaaccrItemType {

   default boolean shouldParse() {
      return false;
   }


   static NaaccrItemType getItemType( final String id ) {
      final NaaccrSectionType type = NaaccrSectionType.getSectionType( id );
      if ( type != NaaccrSectionType.UNKNOWN ) {
         return type;
      }
      return InfoItemType.getItemType( id );
   }


   static boolean shouldParse( final String id ) {
      return getItemType( id ).shouldParse();
   }


}
