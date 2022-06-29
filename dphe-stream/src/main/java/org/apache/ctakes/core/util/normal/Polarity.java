package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.PatternOwner;
import org.apache.ctakes.core.util.owner.ScoreOwner;

import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @since {3/15/2022}
 */
public enum Polarity implements ScoreOwner, CodeOwner, PatternOwner {
   //  N'Not Available' CUI "CL505125 : 422"@en
   NEGATIVE( 80, "C3853545", "Negative",
             "negative|(?:not amplified)|(?:no [a-z] detected)|(?:non-? ?detected)|(?:not detected)" ),
   NEGATIVE_ABBR( 50, "C3853545", "Negative",  "-?neg" ),
   POSITIVE( 80, "CL448866", "Positive", "positive|positivity|overexpression" ),
   POSITIVE_ABBR( 50, "CL448866", "Positive", "\\+?pos" ),
   UNKNOWN( 70, "C0439673", "Unknown", "unknown|indeterminate|equivocal|borderline" ),
   NOT_AVAILABLE( 70, "CL505125",
                  "Not_Available", "(?:not assessed|requested|applicable)|insufficient|pending|\\sN\\/?A" );

   final private int _score;
   final private String _cui;
   final private String _uri;
   final private Pattern _pattern;
   Polarity( final int score, final String cui, final String uri, final String regex ) {
      _score = score;
      _cui = cui;
      _uri = uri;
      _pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
   }

   public int getScore() {
      return _score;
   }

   public String getCui() {
      return _cui;
   }

   public String getUri() {
      return _uri;
   }

   public Pattern getPattern() {
      return _pattern;
   }

   /**
    * If your polarity value can be [source]+ or [source]- then make sure that the candidate text includes the character
    * immediately after [source]
    * {@inheritDoc}
    */
   public boolean areBoundariesValid( final String text, final int begin, final int end ) {
      if ( end-begin == 1 & (this.equals( NEGATIVE_ABBR ) || this.equals( POSITIVE_ABBR ) ) ) {
         if ( text.charAt( begin ) == '-' || text.charAt( begin ) == '+' ) {
            return true;
         }
      }
      return PatternOwner.super.areBoundariesValid( text, begin, end );
   }

}
