package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.TextsOwner;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
enum Certainty implements CodeOwner, TextsOwner {
   UNCERTAIN( "C3844322", "Uncertain", "uncertain", "not certain", "equivocal",
              "ambiguous", "undetermined", "indeterminate", "unknown",
              "no definitive", "not definitely identified" ),
   CERTAIN( "C0205423", "Certain", "certain" );

   final private String _cui;
   final private String _uri;
   final private String[] _texts;

   Certainty( final String cui, final String uri, final String... texts ) {
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
