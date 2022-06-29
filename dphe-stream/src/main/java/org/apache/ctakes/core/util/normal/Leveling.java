package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.TextsOwner;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
public enum Leveling implements CodeOwner, TextsOwner {
   // TODO - get Decreasing, Increasing in the ontology and change codes below
   DECREASING( "C0205251", "Low", "decreasing", "lowering", "falling" ),  // RADLEX C0442797, no uri
   UNSTABLE( "C0443343", "Unstable", "not stable", "unstable" ),
   STABLE( "C0205360", "Stable", "stable" ),
   INCREASING( "C1561957", "High",  "increasing", "rising", "elevating" );  // RADLEX C0442808, no uri

   final private String _cui;
   final private String _uri;
   final private String[] _texts;
   Leveling( final String cui, final String uri, final String... texts ) {
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
