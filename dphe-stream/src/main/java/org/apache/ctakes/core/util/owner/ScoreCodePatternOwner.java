package org.apache.ctakes.core.util.owner;

import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @since {4/6/2022}
 */
public class ScoreCodePatternOwner implements ScoreOwner, CodeOwner, PatternOwner {

   // Use delegates in case any owner is mutable.
   final private ScoreOwner _scoreDelegate;
   final private CodeOwner _codeDelegate;
   final private PatternOwner _patternDelegate;

   public <T extends ScoreOwner & CodeOwner & PatternOwner> ScoreCodePatternOwner( final T owner ) {
      this( owner, owner, owner );
   }

   public ScoreCodePatternOwner( final ScoreOwner scoreOwner,
                                    final CodeOwner codeOwner,
                                    final PatternOwner patternOwner ) {
      _scoreDelegate = scoreOwner;
      _codeDelegate = codeOwner;
      _patternDelegate = patternOwner;
   }

   public ScoreCodePatternOwner( final int score, final String cui, final String uri, final String regex ) {
      this( score, cui, uri,  Pattern.compile( regex, Pattern.CASE_INSENSITIVE ) );
   }

   public ScoreCodePatternOwner( final int score, final String cui, final String uri, final Pattern pattern ) {
      final SimpleDelegate delegate = new SimpleDelegate( score, cui, uri, pattern );
      _scoreDelegate = delegate;
      _codeDelegate = delegate;
      _patternDelegate = delegate;
   }


   public int getScore() {
      return _scoreDelegate.getScore();
   }

   public String getCui() {
      return _codeDelegate.getCui();
   }

   public String getUri() {
      return _codeDelegate.getUri();
   }

   public Pattern getPattern() {
      return _patternDelegate.getPattern();
   }


   static private final class SimpleDelegate implements ScoreOwner, CodeOwner, PatternOwner {
      final private int _score;
      final private String _cui;
      final private String _uri;
      final private Pattern _pattern;
      public SimpleDelegate( final int score, final String cui, final String uri, final Pattern pattern ) {
         _score = score;
         _cui = cui;
         _uri = uri;
         _pattern = pattern;
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

   }

}
