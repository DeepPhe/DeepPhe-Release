package org.apache.ctakes.ner.creator;


import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.annotation.ConceptBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.detail.Details;
import org.apache.ctakes.ner.group.dphe.DpheGroupAccessor;
import org.apache.ctakes.ner.term.DetailedTerm;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceCalculator;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.ner.group.dphe.DpheGroup.DPHE_GROUPING_SCHEME;
import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_CODING_SCHEME;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2020
 */
final public class DpheAnnotationCreator implements AnnotationCreator {

   static private final Logger LOGGER = Logger.getLogger( "DpheAnnotationCreator" );


   public DpheAnnotationCreator() {
   }

   public void createAnnotations( final JCas jCas,
                                  final Collection<DetailedTerm> terms ) {
      if ( terms.isEmpty() ) {
         return;
      }
      for ( DetailedTerm term : terms ) {
         createAnnotation( jCas, term );
      }
   }

   static private void createAnnotation(final JCas jCas, final DetailedTerm term ) {
      final IdentifiedAnnotationBuilder builder = new IdentifiedAnnotationBuilder()
            .discoveredBy( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP )
            .span( term.getTextSpan() )
            .confidence( (float) term.getScore() )
            .tui( getGroupTui( term ).getCode() );
      addConcepts( jCas, builder, term );
      final IdentifiedAnnotation annotation = builder.build( jCas );
      initUriAnnotation( annotation );
   }

   static private void addConcepts( final JCas jCas,
                                    final IdentifiedAnnotationBuilder builder,
                                    final DetailedTerm term ) {
      term.getDetails()
          .stream()
          .map( d -> createConcepts( jCas, d ) )
          .flatMap( Collection::stream )
          .forEach( builder::concept );
   }

   static private Collection<OntologyConcept> createConcepts( final JCas jCas,
                                                              final Details details ) {
      final Collection<OntologyConcept> concepts = new HashSet<>();
      String cui = details.getCui();
      final String[] cuis = StringUtil.fastSplit( cui, ' ' );
      if ( cuis.length > 1 ) {
         cui = cuis[0];
      }
      final int tui = details.getTui();
      final String prefText = details.getPreferredText();
      final ConceptBuilder builder = new ConceptBuilder()
            .cui( cui )
            .tui( tui )
            .preferredText( prefText );
      concepts.add( builder.schema( DPHE_CODING_SCHEME )
                           .code( details.getUri() )
                           .build( jCas ) );
      concepts.add( builder.schema( DPHE_GROUPING_SCHEME )
                           .code( details.getGroupName() )
                           .build( jCas ) );
      for ( Map.Entry<String,Collection<String>> codeEntry : details.getCodesMap().entrySet() ) {
         builder.schema( codeEntry.getKey() );
         for ( String value : codeEntry.getValue() ) {
            concepts.add( builder.code( value ).build( jCas ) );
         }
      }
      // Added this because there can be multiple cuis (old/new, vocab1/vocab2, manual/auto mapping, etc.)
      builder.schema( "CUI" );
      for ( final String s : cuis ) {
         concepts.add( builder.code( s ).build( jCas ) );
      }
      return concepts;
   }

   static private SemanticTui getGroupTui( final DetailedTerm term ) {
      return getBestGroup( term ).getTui();
   }

   static private DpheGroup getBestGroup( final DetailedTerm term ) {
      return DpheGroupAccessor.getInstance().getBestGroup(
            DpheGroupAccessor.getInstance().getDetailedTermGroups( term ) );
   }

   static private void initUriAnnotation( final IdentifiedAnnotation annotation ) {
      final String uri = Neo4jOntologyConceptUtil.getUris( annotation )
                                                 .stream().findFirst().orElse( UriConstants.UNKNOWN );
      final DpheGroup group = DpheGroup.getBestAnnotationGroup( annotation );
      final String prefText = IdentifiedAnnotationUtil.getPreferredTexts( annotation )
                                                      .stream()
                                                      .max( Comparator.comparingInt( String::length ) )
                                                      .orElse( "Unknown" );
//      LogFileWriter.add( "DpheAnnotationCreator initializing " + uri + " " + group.getName() + " " + prefText );
      final String cui = IdentifiedAnnotationUtil.getCuis( annotation ).stream().findFirst().orElse( "Unknown" );
      UriInfoCache.getInstance().initBasics( uri, cui, group, prefText );
   }

//   /**
//    *
//    * @param termScore -
//    * @param termScores -
//    * @return golden mean of termScore (TS) over TS + average of scores > TS and TS / TS + average of scores < TS
//    */
//   static private float calculateConfidence( final double termScore, final List<Double> termScores ) {
//      // Using some sum of term scores as denominators can make the term score very small if there are a lot of terms.
//      final double fakeP = termScore * termScore / ( termScore + getGreaterAve( termScore, termScores ) );
//      final double fakeR = termScore * termScore / ( termScore + getLesserAve( termScore, termScores ) );
//      final double fakeF1 = 2 * fakeP * fakeR / ( fakeP + fakeR );
//      return (float)fakeF1;
//   }
//
//   static private double getGreaterAve( final double termScore, final List<Double> termScores ) {
//      final List<Double> greaters = termScores.stream()
//              .filter( d -> d>=termScore )
//              .collect( Collectors.toList() );
//      return (greaters.stream().mapToDouble( d -> d ).sum()-termScore) / (greaters.size()-1);
//   }
//
//   static private double getLesserAve( final double termScore, final List<Double> termScores ) {
//      final List<Double> lessers = termScores.stream()
//              .filter( d -> d<=termScore )
//              .collect( Collectors.toList() );
//      return (lessers.stream().mapToDouble( d -> d ).sum()-termScore) / (lessers.size()-1);
//   }


}
