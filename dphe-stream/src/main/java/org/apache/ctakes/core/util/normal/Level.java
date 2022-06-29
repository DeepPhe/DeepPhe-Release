package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.TextsOwner;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
public enum Level implements CodeOwner, TextsOwner {
//  MHigh CUI "C1561957 : 422"@en
   LOW( "C0205251", "Low", "low", "lowered", "weak", "weakly", "decreased" ),
   MEDIUM( "C0439536", "Medium", "medium", "intermediate", "intermediately",
           "moderate", "moderately", "average", "not amplified", "borderline" ),
   HIGH( "C1561957", "High", "high", "highly", "strong", "strongly", "great", "greatly",
         "amplified", "extensive", "elevated", "elvtd", "raised", "increased" );

   final private String _cui;
   final private String _uri;
   final private String[] _texts;
   Level( final String cui, final String uri, final String... texts ) {
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
