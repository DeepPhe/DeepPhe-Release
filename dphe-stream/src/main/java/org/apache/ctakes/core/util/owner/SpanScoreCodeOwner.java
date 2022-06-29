package org.apache.ctakes.core.util.owner;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public class SpanScoreCodeOwner extends SpanScoreOwner implements CodeOwner {
   static public final SpanScoreCodeOwner NO_SPAN_SCORE_CODE
         = new SpanScoreCodeOwner( NO_SPAN, 0, UNKNOWN_CUI, UNKNOWN_URI );

   private final String _cui;
   private final String _uri;
   public SpanScoreCodeOwner( final Pair<Integer> span, final int score, final String cui, final String uri ) {
      super( span, score );
      _cui = cui;
      _uri = uri;
   }
   public String getCui() {
      return _cui;
   }
   public String getUri() {
      return _uri;
   }

   public IdentifiedAnnotation createAnnotation( final JCas jCas ) {
      return this.createAnnotation( jCas, SemanticTui.T081 );
   }

   public IdentifiedAnnotation createAnnotation( final JCas jCas, final SemanticTui semanticTui ) {
      // TODO Make a URI utility that transforms uri to preftext.  Replace "_lt_", "_gt_", "_eq_", commas etc.
      return super.createAnnotation( jCas, semanticTui,
                                     getCui(), getUri(), getUri().replace( '_', ' ' ) );
   }

   static public <T extends ScoreOwner & CodeOwner & SpanFinder> List<SpanScoreCodeOwner> findSpanScoreCodeOwners(
         final T owner, final String text ) {
      return owner.findSpansInText( text )
                       .stream()
                        .filter( TextSpanUtil::isValidSpan )
                       .map( s -> new SpanScoreCodeOwner( s,
                                                          owner.getScore(),
                                                          owner.getCui(),
                                                          owner.getUri() ) )
                       .collect( Collectors.toList() );
   }

   static public <T extends ScoreOwner & CodeOwner & SpanFinder> List<SpanScoreCodeOwner> findSpanScoreCodeOwners(
         final T owner, final String text, final int startIndex ) {
      return owner.findSpansInText( text, startIndex )
                       .stream()
                  .filter( TextSpanUtil::isValidSpan )
                  .map( s -> new SpanScoreCodeOwner( s,
                                                          owner.getScore(),
                                                          owner.getCui(),
                                                          owner.getUri() ) )
                       .collect( Collectors.toList() );
   }

   static public <T extends ScoreOwner & CodeOwner & SpanFinder> SpanScoreCodeOwner findSpanScoreCodeOwner(
         final T[] owners, final int offset, final String text ) {
      for ( T owner : owners ) {
         final Pair<Integer> ownerSpan = owner.findSpanInText( text );
         if ( TextSpanUtil.isValidSpan( ownerSpan ) ) {
            final Pair<Integer> offsetSpan
                  = new Pair<>( offset + ownerSpan.getValue1(), offset + ownerSpan.getValue2() );
            return new SpanScoreCodeOwner( offsetSpan, owner.getScore(), owner.getCui(), owner.getUri() );
         }
      }
      return NO_SPAN_SCORE_CODE;
   }


//   static public <C extends ScoreOwner & PatternOwner & CodeOwner> List<SpanScoreCodeOwner> findSpanScoreCodeOwnersP(
//         final C owner, final String text ) {
//      try ( RegexSpanFinder finder = new RegexSpanFinder( owner.getPattern() ) ) {
//         return finder.findSpans( text )
//                      .stream()
//                      .filter( s -> isWholeWord( text, s ) )
//                      .map( s -> new SpanScoreCodeOwner( s,
//                                                  owner.getScore(),
//                                                  owner.getCui(),
//                                                  owner.getUri() ) )
//                      .collect( Collectors.toList() );
//       } catch ( IllegalArgumentException iaE ) {
////            LOGGER.warn( iaE.getMessage() );
//      }
////      LOGGER.info( "No grade value found in " + lower );
//      return Collections.emptyList();
//   }


//   static public <C extends ScoreOwner & TextsOwner & CodeOwner> SpanScoreCodeOwner findSpanScoreCodeOwner(
//         final C[] owners, final int offset, final String text ) {
//      final String lower = text.toLowerCase()
//                               .replace( '-', ' ' );
//      for ( C owner : owners ) {
//         for ( String valueText : owner.getTexts() ) {
//            final int index = lower.indexOf( valueText );
//            if ( index >= 0 ) {
//               if ( owner.getScore() <= 50 ) {
//                  // Check for -lone- text on values like 'i' and '1'.
//                  final boolean beginOk = index == 0 || !Character.isLetterOrDigit( text.charAt( index-1 ) );
//                  final boolean endOk =
//                        index+1 == text.length() || !Character.isLetterOrDigit( text.charAt( index+1 ) );
//                  if ( !beginOk || !endOk ) {
//                     continue;
//                  }
//               }
////                  LOGGER.info( "Found Value " + gradeValue.getUri() + " " + text.substring( index,
////                                                                                            index + valueText.length() ) );
//               return new SpanScoreCodeOwner( new Pair<>( offset + index, offset + index + valueText.length() ),
//                                              owner.getScore(),
//                                              owner.getCui(),
//                                              owner.getUri() );
//            }
//         }
//      }
////      LOGGER.info( "No grade value found in " + lower );
//      return NO_SPAN_SCORE_CODE;
//   }
//
//   // Why the heck don't word boundaries ever work in java?!
//   static public boolean isWholeWord( final String text, final Pair<Integer> span ) {
//      return isWholeWord( text, span.getValue1(), span.getValue2() );
//   }
//
//   // Why the heck don't word boundaries ever work in java?!
//   static public boolean isWholeWord( final String text, final int begin, final int end ) {
//      if ( begin > 0 ) {
//         if ( Character.isLetterOrDigit( text.charAt( begin-1 ) ) ) {
//            return false;
//         }
//      }
//      if ( end == text.length() ) {
//         return true;
//      }
//      return !Character.isLetterOrDigit( text.charAt( end ) );
//   }

}
