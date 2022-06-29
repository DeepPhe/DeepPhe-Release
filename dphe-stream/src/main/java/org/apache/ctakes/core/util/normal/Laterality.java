package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.TextsOwner;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
enum Laterality implements CodeOwner, TextsOwner {
   // Bilateral is under Anatomy_Qualifier while right and left are under Spatial_Qualifier
   BILATERAL( "C0238767", "Bilateral", "bilateral", "left and right", "right and left" ),
   RIGHT( "C0205090", "Right", "right" ),
   LEFT( "C0205091", "Left", "left" );

   final private String _cui;
   final private String _uri;
   final private String[] _texts;
   Laterality( final String cui, final String uri, final String... texts ) {
      _cui = cui;
      _uri = uri;
      _texts = texts;
   }

   public String getCui() {
      return _cui;
   }

   public String getUri() {
      return _uri;
   }

   public String[] getTexts() {
      return _texts;
   }

}
