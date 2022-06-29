package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.TextsOwner;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
enum Affirmation implements CodeOwner, TextsOwner {
   NEGATED( "C0205237", "False",  "negative for", "negative", "negated", "not true", "false", "no" ),
   NOT_AFFIRMED( "C0205426", "Not_Classified", "not affirmed" ),
   AFFIRMED( "C0205238", "True", "affirmed", "affirmative", "true", "yes", "positive for", "positive" );

   final private String _cui;
   final private String _uri;
   final private String[] _texts;
   Affirmation( final String cui, final String uri, final String... texts ) {
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
