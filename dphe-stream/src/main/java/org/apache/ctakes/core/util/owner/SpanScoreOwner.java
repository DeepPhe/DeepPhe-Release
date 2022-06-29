package org.apache.ctakes.core.util.owner;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.annotation.ConceptBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_CODING_SCHEME;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2022}
 */
public class SpanScoreOwner implements SpanOwner, ScoreOwner {
   static public final SpanScoreOwner NO_SPAN_SCORE = new SpanScoreOwner( NO_SPAN, 0 );
   private final Pair<Integer> _span;
   private final int _score;
   public SpanScoreOwner( final Pair<Integer> span, final int score ) {
      _span = span;
      _score = score;
   }
   public Pair<Integer> getSpan() {
      return _span;
   }
   public int getScore() {
      return _score;
   }

   public IdentifiedAnnotation createAnnotation( final JCas jCas,
                                                 final String cui,
                                                 final String uri,
                                                 final String prefText ) {
      return this.createAnnotation( jCas, SemanticTui.T184, cui, uri, prefText );
   }

   public IdentifiedAnnotation createAnnotation( final JCas jCas,
                                                           final SemanticTui semanticTui,
                                                           final String cui,
                                                           final String uri,
                                                           final String prefText ) {
      final OntologyConcept concept = new ConceptBuilder().cui( cui )
                                                          .type( semanticTui )
                                                          .schema( DPHE_CODING_SCHEME )
                                                          .code( uri )
                                                          .preferredText( prefText )
                                                          .build( jCas );
      return new IdentifiedAnnotationBuilder().span( getSpan() )
                                              .type( semanticTui )
                                              .concept( concept )
                                              .confidence( new Integer( getScore() ).floatValue() )
                                              .build( jCas );
   }

   static public <T extends ScoreOwner & SpanFinder> SpanScoreOwner findSpanScoreOwner( final T[] owners,
                                                                                        final String text,
                                                                                        final Pair<Integer> span ) {
      if ( !TextSpanUtil.isValidSpan( span ) ) {
         return NO_SPAN_SCORE;
      }
      for ( T owner : owners ) {
         final Pair<Integer> ownerSpan = owner.findSpanInText( text );
         if ( TextSpanUtil.isValidSpan( ownerSpan ) ) {
            return new SpanScoreOwner( span, owner.getScore() );
         }
      }
      return NO_SPAN_SCORE;
   }

   static public <T extends ScoreOwner & SpanFinder> SpanScoreOwner findSpanScoreOwner( final T[] owners,
                                                                                        final int offset,
                                                                                        final String text ) {
      for ( T owner : owners ) {
         final Pair<Integer> ownerSpan = owner.findSpanInText( text );
         if ( TextSpanUtil.isValidSpan( ownerSpan ) ) {
            final Pair<Integer> offsetSpan
                  = new Pair<>( offset + ownerSpan.getValue1(), offset + ownerSpan.getValue2() );
            return new SpanScoreOwner( offsetSpan, owner.getScore() );
         }
      }
      return NO_SPAN_SCORE;
   }

//   static public <T extends ScoreOwner & TextsOwner> SpanScoreOwner findSpanScoreOwner( final T[] owners,
//                                      final String text,
//                                      final Pair<Integer> span ) {
//      if ( !TextSpanUtil.isValidSpan( span ) ) {
//         return NO_SPAN_SCORE;
//      }
//      final String lower = text.toLowerCase();
//      for ( T owner : owners ) {
//         for ( String ownerText : owner.getTexts() ) {
//            final int index = lower.indexOf( ownerText );
//            if ( index >= 0 ) {
//               return new SpanScoreOwner( span, owner.getScore() );
//            }
//         }
//      }
//      return NO_SPAN_SCORE;
//   }
//
//   static public <T extends ScoreOwner & TextsOwner> SpanScoreOwner findSpanScoreOwner( final T[] owners,
//                                             final int offset,
//                                             final String text ) {
//      final String lower = text.toLowerCase();
//      for ( T owner : owners ) {
//         for ( String ownerText : owner.getTexts() ) {
//            final int index = lower.indexOf( ownerText );
//            if ( index >= 0 ) {
//               return new SpanScoreOwner( new Pair<>( offset + index, offset + index + ownerText.length() ),
//                                          owner.getScore() );
//            }
//         }
//      }
//      return NO_SPAN_SCORE;
//   }

}
