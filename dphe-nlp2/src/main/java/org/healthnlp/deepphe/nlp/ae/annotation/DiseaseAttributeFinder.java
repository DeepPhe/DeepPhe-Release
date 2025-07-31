package org.healthnlp.deepphe.nlp.ae.annotation;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/10/2016
 */
final public class DiseaseAttributeFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DiseaseAttributeFinder" );

   private interface AttributeKey {
      Pattern getPattern();
      default Pattern compile( final String regex ) { return Pattern.compile( regex, Pattern.CASE_INSENSITIVE ); }
      default boolean matches( final String text ) { return getPattern().matcher( text ).find(); }
   }


   private enum GenericKey implements AttributeKey {
      EVALUATE( "evaluat" ),
      TEST( "\\btest(?:|s|ed|ing)\\b" ),
      DISCUSS( "discuss" ),
      TALK( "\\btalk(?:ed)?\\b" ),
      INSTRUCT( "instruct" ),
      SUGGEST( "suggest" ),
      GENERALLY( "generally" );

      private final Pattern __pattern;
      public Pattern getPattern() { return __pattern; }
      GenericKey( final String regex ) {
         __pattern = compile( regex );
      }
   }

   private enum ConditionalKey implements AttributeKey {
      IF( "\\bif\\b" );

      private final Pattern __pattern;
      public Pattern getPattern() { return __pattern; }
      ConditionalKey( final String regex ) {
         __pattern = compile( regex );
      }
   }

   private enum UncertainKey implements AttributeKey {
      PERHAPS( "perhaps" ),
      COULD( "\\bcould\\b" ),
      MAY( "\\bmay(?:be)?\\b" ),
      MIGHT( "\\bmight\\b" ),
      POTENTIAL( "potential" ),
      POSSIBLE( "\\bpossibl" ),
      PROBABLE( "probabl" ),
      LIKELY( "likely" ),
      PROSPECTIVE( "prospective" ),
      SUSPECT( "susp(?:ect|icious)" ),
      SEEMS( "\\bseems\\b" ),
      RULE_OUT("ruled? out" ),
      //      INDICATE( "indicate" ),
      VERSUS( "\\bv(?:ersu)?s.?\\b" );

      private final Pattern __pattern;
      public Pattern getPattern() { return __pattern; }
      UncertainKey( final String regex ) {
         __pattern = compile( regex );
      }
   }

   static private AttributeKey getKeyMatch( final AttributeKey[] keys, final String text ) {
      return Arrays.stream( keys )
            .filter( k -> k.matches( text ) )
            .findFirst()
            .orElse( null );
   }

   /**
    * Removes Metastasis to breast locations
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Disease/Disorder Attributes ..." );

      final Collection<Sentence> sentences = JCasUtil.select( jcas, Sentence.class );
      if ( sentences == null || sentences.isEmpty() ) {
         return;
      }
      for ( Sentence sentence : sentences ) {
         final List<DiseaseDisorderMention> diseases
               = JCasUtil.selectCovered( jcas, DiseaseDisorderMention.class, sentence );
         final List<SignSymptomMention> findings
               = JCasUtil.selectCovered( jcas, SignSymptomMention.class, sentence );
         if ( diseases.isEmpty() && findings.isEmpty() ) {
            continue;
         }
         // get all tokens in the sentence.  They are used for pattern matching and window sizing
         final List<WordToken> wordTokens
               = new ArrayList<>( JCasUtil.selectCovered( jcas, WordToken.class, sentence ) );
         processType( diseases, wordTokens );
         processType( findings, wordTokens );
      }
   }


   static private void processType( final Collection<? extends IdentifiedAnnotation> annotations, final List<WordToken> wordTokens ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return;
      }
      // get indexes of various attribute keys within the sentence
      final Map<Integer, AttributeKey> generics = getKeyMatches( GenericKey.values(), wordTokens );
      final Map<Integer, AttributeKey> conditionals = getKeyMatches( ConditionalKey.values(), wordTokens );
      final Map<Integer, AttributeKey> uncertains = getKeyMatches( UncertainKey.values(), wordTokens );
      if ( generics.isEmpty() && conditionals.isEmpty() && uncertains.isEmpty() ) {
         return;
      }
      wordTokens.sort( Comparator.comparingInt( WordToken::getBegin ) );
      // set attributes for diseases
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Pair<Integer> bounds = getAnnotationBounds( annotation, wordTokens );
         if ( inAttributeWindow( bounds, generics.keySet() ) ) {
            annotation.setGeneric( true );
         }
         if ( inAttributeWindow( bounds, conditionals.keySet() ) ) {
            annotation.setConditional( true );
         }
         if ( inAttributeWindow( bounds, uncertains.keySet() ) ) {
            annotation.setUncertainty( CONST.NE_UNCERTAINTY_PRESENT );
         }
      }
   }


   static private Pair<Integer> getAnnotationBounds( final IdentifiedAnnotation annotation,
                                                     final List<WordToken> wordTokens ) {
      final int annotationBegin = annotation.getBegin();
      final int annotationEnd = annotation.getEnd();
      int beginWordIndex = 0;
      int endWordIndex = 0;
      for ( int i = 0; i < wordTokens.size(); i++ ) {
         final WordToken token = wordTokens.get( i );
         if ( token.getBegin() <= annotationBegin ) {
            beginWordIndex = i;
         }
         if ( token.getEnd() <= annotationEnd ) {
            endWordIndex = i;
         } else {
            break;
         }
      }
      return new Pair<>( beginWordIndex, endWordIndex );
   }


   static private Map<Integer, AttributeKey> getKeyMatches( final AttributeKey[] attributeKeys, final List<WordToken> wordTokens ) {
      final Map<Integer, AttributeKey> keyMap = new HashMap<>();
      for ( int i = 0; i < wordTokens.size(); i++ ) {
         AttributeKey key = getKeyMatch( attributeKeys, wordTokens.get( i ).getCoveredText() );
         if ( key != null ) {
            keyMap.put( i, key );
         }
      }
      return keyMap;
   }

   static private boolean inAttributeWindow( final Pair<Integer> annotationBounds,
                                             final Collection<Integer> attributeIndexes ) {
      final int preceding = annotationBounds.getValue1() - 15;
      final int following = annotationBounds.getValue2() + 7;
      for ( int i : attributeIndexes ) {
         if ( i > preceding && i < following ) {
            return true;
         }
      }
      return false;
   }


}
