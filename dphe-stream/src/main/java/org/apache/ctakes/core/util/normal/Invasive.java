package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.TextsOwner;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
enum Invasive implements CodeOwner, TextsOwner {
   NOT_INVASIVE( "C2986496", "Noninvasive", "not invasive", "noninvasive", "non-invasive" ),
   INVASIVE( "C0205281", "Invasive", "invasive", "infiltrating" ),
   IN_SITU( "C0444498", "In_Situ", "in situ" ),
   METASTATIC( "C0036525", "Metastatic", "metastatic" );
   // The above are under Disease_Qualifier along with a lot of other classes.

   final private String _cui;
   final private String _uri;
   final private String[] _texts;

   Invasive( final String cui, final String uri, final String... texts ) {
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
