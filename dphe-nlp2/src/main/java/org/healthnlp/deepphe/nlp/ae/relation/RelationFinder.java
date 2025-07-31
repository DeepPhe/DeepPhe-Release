package org.healthnlp.deepphe.nlp.ae.relation;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.core.util.relation.RelationBuilder;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.ListEntry;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.util.RelatedUris;
import org.healthnlp.deepphe.nlp.ae.relation.penalty.AnnotationPenalties;
import org.healthnlp.deepphe.nlp.ae.relation.penalty.NeoplasmPenalties;
import org.healthnlp.deepphe.nlp.ae.relation.penalty.PlacementPenalty;
import org.healthnlp.deepphe.nlp.ae.relation.score.RelationScore;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.nlp.uri.UriInfoUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {3/7/2023}
 */
@PipeBitInfo(
      name = "RelationFinder",
      description = "Finds text relations.",
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class RelationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "RelationFinder" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Relations ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         final Map<String,Collection<IdentifiedAnnotation>> uriAnnotationsMap = new HashMap<>();
         final Collection<Pair<Integer>> paragraphBounds = new HashSet<>();
         final Collection<Pair<Integer>> listEntryBounds = new HashSet<>();
         final Map<String,Sentence> sentences = new HashMap<>();
         final Map<Integer,Integer> tokenBeginToTokenNums = new HashMap<>();
         final Map<Integer,Integer> tokenEndToTokenNums = new HashMap<>();
//         LOGGER.info( "Filling Section Maps ..." );
         fillSectionsMaps( jCas, uriAnnotationsMap, paragraphBounds, listEntryBounds, sentences,
                           tokenBeginToTokenNums, tokenEndToTokenNums );
         if ( uriAnnotationsMap.size() < 2 ) {
            return;
         }
         final int docLength = jCas.getDocumentText().length();
         // Loop through URIs and Annotations.
//         LOGGER.info( "Looping through URIs and Annotations ..." );
         for ( Map.Entry<String,Collection<IdentifiedAnnotation>> uriSourceAnnotations : uriAnnotationsMap.entrySet() ) {
            final String sourceUri = uriSourceAnnotations.getKey();
            final RelatedUris relatedUris = UriInfoCache.getInstance().getRelatedGraphUris( sourceUri );
            if ( relatedUris.isEmpty() ) {
               continue;
            }
//            LOGGER.info( "Working with " + sourceUri + " ..." );
            for ( Map.Entry<String, Collection<IdentifiedAnnotation>> uriTargetAnnotations :
                  uriAnnotationsMap.entrySet() ) {
               final String targetUri = uriTargetAnnotations.getKey();
               if ( sourceUri.equals( targetUri ) ) {
                  continue;
               }
               final Map<String,Double> relationScoresMap = UriInfoUtil.getRelationScores( sourceUri, targetUri );
               if ( relationScoresMap.isEmpty() ) {
                  // No relations between source and target.  Move on to next target URI.
                  continue;
               }
               for ( IdentifiedAnnotation sourceAnnotation : uriSourceAnnotations.getValue() ) {
                  final int beginTokenNum = getBeginTokenNum( sourceAnnotation.getBegin(), tokenBeginToTokenNums );
                  final int endTokenNum = getEndTokenNum( sourceAnnotation.getEnd(), tokenEndToTokenNums, docLength );
                  final Pair<String> sentenceText = getSentenceText( sourceAnnotation, sentences );
                  final AnnotationPenalties sourcePenalties = new NeoplasmPenalties( sourceAnnotation,
                        beginTokenNum,
                        endTokenNum,
                        sentenceText.getValue1(),
                        sentenceText.getValue2(),
                        docLength );
                  final Map<String,Collection<RelationScore>> relatedAnnotationScoresMap = new HashMap<>();
                  for ( Map.Entry<String,Double> relationScores : relationScoresMap.entrySet() ) {
                     final Collection<RelationScore> annotationRelationScores =
                           createAnnotationRelationScores( relationScores.getKey(),
                                                           relationScores.getValue(),
                                                           sourceAnnotation,
                                                           sourcePenalties,
                                                           uriTargetAnnotations.getValue(),
                                                           sentences,
                                                           paragraphBounds,
                                                           listEntryBounds,
                                                           tokenBeginToTokenNums,
                                                           tokenEndToTokenNums,
                                                           docLength );
                     relatedAnnotationScoresMap.put( relationScores.getKey(), annotationRelationScores );
                  }
                  for ( Map.Entry<String,Collection<RelationScore>> relatedAnnotationScores
                        : relatedAnnotationScoresMap.entrySet() ) {
                     createAllRelations( jCas, sourceAnnotation,
                                          relatedAnnotationScores.getKey(),
                                          relatedAnnotationScores.getValue() );
                  }
               }
            }
         }
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

   static private void initUriAnnotation( final String uri,
                                          final IdentifiedAnnotation annotation,
                                          final DpheGroup dpheGroup ) {
      final String prefText = IdentifiedAnnotationUtil.getPreferredTexts( annotation )
                                                      .stream()
                                                      .max( Comparator.comparingInt( String::length ) )
                                                      .orElse( "Unknown" );
      final String cui = IdentifiedAnnotationUtil.getCuis( annotation ).stream().findFirst().orElse( "Unknown" );
//      LogFileWriter.add( "RelationFinder initializing " + uri + " " + dpheGroup + " " + prefText );
      UriInfoCache.getInstance().initBasics( uri, cui, dpheGroup, prefText );
   }


   static private void fillSectionsMaps( final JCas jCas,
                                 final Map<String,Collection<IdentifiedAnnotation>> uriAnnotationsMap,
                                 final Collection<Pair<Integer>> paragraphBounds,
                                         final Collection<Pair<Integer>> listEntryBounds,
                                         final Map<String,Sentence> sentences,
                                 final Map<Integer,Integer> tokenBeginToTokenNums,
                                 final Map<Integer,Integer> tokenEndToTokenNums ) {
      for ( IdentifiedAnnotation annotation : JCasUtil.select( jCas, IdentifiedAnnotation.class ) ) {
         if ( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT
               || !annotation.getSubject().equals( CONST.ATTR_SUBJECT_PATIENT ) ) {
//            LogFileWriter.add( "RelationFinder ignoring " + annotation.getCoveredText()
//                  + " " + annotation.getSubject() + " " + annotation.getPolarity() );
            // Don't bother checking negated or family sources / targets.
            continue;
         }
         final String uri = Neo4jOntologyConceptUtil.getUris( annotation )
                                                    .stream().findFirst().orElse( UriConstants.UNKNOWN );
         if ( uri.equals( UriConstants.UNKNOWN ) ) {
            continue;
         }
         uriAnnotationsMap.computeIfAbsent( uri, a -> new HashSet<>() ).add( annotation );
         DpheGroup group = UriInfoCache.getInstance().getDpheGroup( uri );
         if ( group == DpheGroup.UNKNOWN ) {
            LogFileWriter.add( "RelationFinder no group for " + uri );
            group = DpheGroup.getBestAnnotationGroup( annotation );
            initUriAnnotation( uri, annotation, group );
         }
      }
      if ( uriAnnotationsMap.size() < 2 ) {
         // Only a single uri, no relations can be made.
         return;
      }
      // Just initializes.  Probably not necessary but may save some time locking.

      UriInfoCache.getInstance().initGraphPlacement( uriAnnotationsMap.keySet() );
      // Paragraphs.  For confidence.  Todo - ctakes: add paragraphID to IdentifiedAnnotation.
      paragraphBounds.addAll( JCasUtil.select( jCas, Paragraph.class )
                                                                .stream()
                                                                .filter( Objects::nonNull )
                                                                .map( p -> new Pair<>( p.getBegin(), p.getEnd() ) )
                                                                .collect( Collectors.toList() ) );
      // ListEntries.  For confidence.
      listEntryBounds.addAll( JCasUtil.select( jCas, ListEntry.class )
                                      .stream()
                                      .filter( Objects::nonNull )
                                      .map( p -> new Pair<>( p.getBegin(), p.getEnd() ) )
                                      .collect( Collectors.toList() ) );
      JCasUtil.select( jCas, Sentence.class ).forEach( s -> sentences.put( ""+s.getSentenceNumber(), s ) );
      // Token indices.  For confidence.
      int i = 1;
      for ( BaseToken token : JCasUtil.select( jCas, BaseToken.class ) ) {
         tokenBeginToTokenNums.put( token.getBegin(), i );
         tokenEndToTokenNums.put( token.getEnd(), i );
         i++;
      }
   }

   static private final Collection<String> LOCATION_RELATIONS
         = new HashSet<>( Arrays.asList( HAS_LATERALITY, HAS_SITE,
                               HAS_ASSOCIATED_SITE ) );
//         , HAS_QUADRANT, HAS_CLOCKFACE ) );

   static private Collection<RelationScore> createAnnotationRelationScores(
         final String relationName,
         final double uriRelationScore,
                             final IdentifiedAnnotation sourceAnnotation,
                             final AnnotationPenalties sourcePenalties,
                             final Collection<IdentifiedAnnotation> targetAnnotations,
                             final Map<String,Sentence> sentences,
                             final Collection<Pair<Integer>> paragraphBounds,
                               final Collection<Pair<Integer>> listEntryBounds,
                               final Map<Integer,Integer> tokenBeginToTokenNums,
                             final Map<Integer,Integer> tokenEndToTokenNums,
                             final int docLength ) {
      final boolean isLocation = LOCATION_RELATIONS.contains( relationName );
      final Collection<RelationScore> relationScores = new HashSet<>();
      for ( IdentifiedAnnotation targetAnnotation : targetAnnotations ) {
         final int beginTokenNum = getBeginTokenNum( targetAnnotation.getBegin(), tokenBeginToTokenNums );
         final int endTokenNum = getEndTokenNum( targetAnnotation.getEnd(), tokenEndToTokenNums, docLength );
         final Pair<String> sentenceText = getSentenceText( targetAnnotation, sentences );
         final AnnotationPenalties targetPenalties = AnnotationPenalties.createAnnotationPenalties( isLocation,
                                                                                targetAnnotation,
                                                                               beginTokenNum,
                                                                               endTokenNum,
                                                                               sentenceText.getValue1(),
                                                                               sentenceText.getValue2(),
                                                                               docLength );
         final Pair<Double> placementPenalty = PlacementPenalty.getPenalty( sourceAnnotation, targetAnnotation,
                                                                    paragraphBounds, listEntryBounds );
         final double placementScore = PlacementPenalty.getPlacementScore( sourcePenalties.getBeginEndTokens(),
                                                          targetPenalties.getBeginEndTokens(),
                                                          placementPenalty );
         if ( isLocation && placementScore <= 1 ) {
            continue;
         }
         final RelationScore targetScore = RelationScore.createRelationScore( isLocation,
                                                                targetAnnotation,
                                                              uriRelationScore,
                                                              placementScore,
                                                              sourcePenalties,
                                                              targetPenalties );
         if ( targetScore.getTotalScore() > 1 ) {
            relationScores.add( targetScore );
         }
      }
      return relationScores;
   }

   private void createAllRelations( final JCas jCas,
                                     final IdentifiedAnnotation source,
                                     final String relationName,
                                     final Collection<RelationScore> relationScores ) {
      final RelationBuilder<BinaryTextRelation> builder
            = new RelationBuilder<>().creator( BinaryTextRelation::new )
                                     .name( relationName )
                                     .annotation( source )
                                     .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE );
      for ( RelationScore relationScore : relationScores ) {
         builder.hasRelated( relationScore.getTargetAnnotation() )
               .confidence( relationScore.getTotalScore() )
                .build( jCas );
      }
   }


   static private int getBeginTokenNum( final int annotationBegin, final Map<Integer,Integer> tokenBeginToTokenNum ) {
      for ( int i=annotationBegin; i>=0; i-- ) {
         if ( tokenBeginToTokenNum.containsKey( i ) ) {
            return tokenBeginToTokenNum.get( i );
         }
      }
      return 0;
   }

   static private int getEndTokenNum( final int annotationEnd, final Map<Integer,Integer> tokenEndToTokenNum,
                                      final int docLength ) {
      for ( int i=annotationEnd; i<docLength; i++ ) {
         if ( tokenEndToTokenNum.containsKey( i ) ) {
            return tokenEndToTokenNum.get( i );
         }
      }
      return tokenEndToTokenNum.size()-1;
   }


   static private Pair<String> getSentenceText( final IdentifiedAnnotation annotation,
                                           final Map<String,Sentence> sentences ) {
      final String sentenceId = annotation.getSentenceID();
      final Sentence sentence = sentences.getOrDefault( sentenceId, null );
      if ( sentence == null ) {
//            LOGGER.warn( "No Sentence for Annotation " + annotation.getCoveredText() );
         return new Pair<>( "", "" );
      }
      final String sentenceText = sentence.getCoveredText()
                                          .toLowerCase();
      final int begin = annotation.getBegin() - sentence.getBegin();
      final String precedingText = begin <= 0 ? "" : sentenceText.substring( 0, begin );
      return new Pair<>( precedingText, sentenceText );
   }


}
