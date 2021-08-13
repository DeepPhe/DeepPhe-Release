package org.apache.ctakes.dictionary.lookup2.consumer;


import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.MultiTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Traditional ctakes semantic groups ignore a large number of semantic types.
 * This class adds extra semantic groups to ctakes from a dictionary.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2018
 */
public class ByTuiTermConsumer extends AbstractTermConsumer {

   static private final Logger LOGGER = Logger.getLogger( "ByTuiTermConsumer" );

   //   static private final Long LEFT_CUI = 1552822l;
//   static private final Long RIGHT_CUI = 1552823l;
//   static private final Long BILATERAL_CUI = 238767l;
   static private final Long LEFT_CUI_1 = 205091l;
   static private final Long LEFT_CUI_2 = 443246l;
   static private final Long RIGHT_CUI_1 = 205090l;
   static private final Long RIGHT_CUI_2 = 444532l;
   static private final Long BILATERAL_CUI = 281267l;


   final private UmlsConceptCreator _umlsConceptCreator;

   public ByTuiTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      this( uimaContext, properties, new DefaultUmlsConceptCreator() );
   }

   public ByTuiTermConsumer( final UimaContext uimaContext, final Properties properties,
                             final UmlsConceptCreator umlsConceptCreator ) {
      super( uimaContext, properties );
      _umlsConceptCreator = umlsConceptCreator;
   }

   /**
    * Refine a collection of dictionary terms to only contain the most specific variations:
    * "colon cancer" instead of "cancer", performed by span inclusion /complete containment, not overlap.
    * For instance:
    * "54 year old woman with left breast cancer."
    * in the above sentence, "breast" as part of "breast cancer" is an anatomical site and should not be a S/S
    * "Breast:
    * "lump, cyst"
    * in the above, breast is a list header, denoting findings on exam.
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas,
                            final RareWordDictionary dictionary,
                            final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                            final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException {
      final String codingScheme = getCodingScheme();
//      final Collection<Integer> usedcTakesSemantics = getUsedcTakesSemantics( cuiConcepts );
//      final Map<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> groupedSemanticCuis
//            = new HashMap<>();
//      // The dictionary may have more than one type, create a map of types to terms and use them all
//      for ( Integer cTakesSemantic : usedcTakesSemantics ) {
//         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticTerms = new HashSetMap<>();
//         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
//            for ( Long cuiCode : spanCuis.getValue() ) {
//               final Collection<Concept> concepts = cuiConcepts.getCollection( cuiCode );
//               if ( hascTakesSemantic( cTakesSemantic, concepts ) ) {
//                  semanticTerms.placeValue( spanCuis.getKey(), cuiCode );
//               }
//            }
//         }
//         groupedSemanticCuis.put( cTakesSemantic, semanticTerms );
//      }

//      LOGGER.info( "PRE-CLEANUP" );
//      textSpanCuis.entrySet().stream().map( tsc -> toTestText( tsc, cuiConcepts) ).forEach( LOGGER::info );

      final Collection<SemanticTui> usedTuis = getUsedTuis( cuiConcepts );
      final Map<SemanticTui, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> groupedTuiCuis
            = new HashMap<>();
      // The dictionary may have more than one type, create a map of types to terms and use them all
      for ( SemanticTui usedTui : usedTuis ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> tuiTerms = new HashSetMap<>();
         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
            for ( Long cuiCode : spanCuis.getValue() ) {
               final Collection<Concept> concepts = cuiConcepts.getCollection( cuiCode );
               if ( hasTui( usedTui, concepts ) ) {
                  tuiTerms.placeValue( spanCuis.getKey(), cuiCode );
               }
            }
         }
         groupedTuiCuis.put( usedTui, tuiTerms );
      }

      // Clean up sign/symptom that are also within anatomical sites
      if (     // sign/symptom
            groupedTuiCuis.containsKey( SemanticTui.T184 )
            // Anatomy
            && (groupedTuiCuis.containsKey( SemanticTui.T023 )
                || groupedTuiCuis.containsKey( SemanticTui.T030 )
                || groupedTuiCuis.containsKey( SemanticTui.T029 )
                || groupedTuiCuis.containsKey( SemanticTui.T022 )
                // Disorder
                || groupedTuiCuis.containsKey( SemanticTui.T191 )
                || groupedTuiCuis.containsKey( SemanticTui.T047 )
                // Procedure
                || groupedTuiCuis.containsKey( SemanticTui.T060 )
                || groupedTuiCuis.containsKey( SemanticTui.T061 ))
      ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copiedTerms = new HashSetMap<>();
         copyTerms( SemanticTui.T184, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T023, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T030, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T029, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T022, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T191, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T047, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T060, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T061, groupedTuiCuis, copiedTerms );
         // We just created a collection with only the largest Textspans.
         // Any smaller Finding textspans are therefore within a larger d/d textspan and should be removed.
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseTerms
               = PrecisionTermConsumer.createPreciseTerms( copiedTerms );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> findingSpanCuis
               = groupedTuiCuis.get( SemanticTui.T184 );
         final Collection<TextSpan> findingSpans = new ArrayList<>( findingSpanCuis.keySet() );
         // Clean up findings that are also within anatomical sites
         for ( TextSpan span : findingSpans ) {
            if ( !preciseTerms.containsKey( span ) ) {
//               // Keep right/left even if it is within some larger span  These should actually be T082 or T080.
//               if ( findingSpanCuis.get( span ).contains( LEFT_CUI_1 )
//                    || findingSpanCuis.get( span ).contains( LEFT_CUI_2 )
//                    || findingSpanCuis.get( span ).contains( RIGHT_CUI_1 )
//                    || findingSpanCuis.get( span ).contains( RIGHT_CUI_2 )
//                    || findingSpanCuis.get( span ).contains( BILATERAL_CUI ) ) {
//                  continue;
//               }
               findingSpanCuis.remove( span );
            }
         }
      }
      // Clean up labs that are also within procedure spans
      if ( groupedTuiCuis.containsKey( SemanticTui.T034 )
           && (groupedTuiCuis.containsKey( SemanticTui.T060 )
               || groupedTuiCuis.containsKey( SemanticTui.T061 )) ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copiedTerms = new HashSetMap<>();
         copyTerms( SemanticTui.T034, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T060, groupedTuiCuis, copiedTerms );
         copyTerms( SemanticTui.T061, groupedTuiCuis, copiedTerms );
         // We just created a collection with only the largest Textspans.
         // Any smaller Finding textspans are therefore within a larger d/d textspan and should be removed.
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseTerms
               = PrecisionTermConsumer.createPreciseTerms( copiedTerms );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> labSpanCuis
               = groupedTuiCuis.get( SemanticTui.T034 );
         final Collection<TextSpan> labSpans = new ArrayList<>( labSpanCuis.keySet() );
         for ( TextSpan span : labSpans ) {
            if ( !preciseTerms.containsKey( span ) ) {
               labSpanCuis.remove( span );
            }
         }
      }
      final Map<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> groupedSemanticCuis
            = new HashMap<>();

      for ( Map.Entry<SemanticTui, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> group
            : groupedTuiCuis.entrySet() ) {
         final SemanticTui tui = group.getKey();
         if ( tui == SemanticTui.T024
              || tui == SemanticTui.T031
              || tui == SemanticTui.T025
              || tui == SemanticTui.T028 ) {
            // skip tissue, fluid/misc anatomy, cell, gene
            continue;
         }
         final int cTakesConst = tui.getGroupCode();
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> map
               = groupedSemanticCuis.computeIfAbsent( cTakesConst, c -> new HashSetMap<>() );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> innerMap = group.getValue();
         for ( TextSpan span : innerMap.keySet() ) {
            map.addAllValues( span, innerMap.getCollection( span ) );
         }
      }
      // Finally deal with everything at once
      for ( Map.Entry<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> group : groupedSemanticCuis
            .entrySet() ) {
         consumeTypeIdHits( jcas, codingScheme, group.getKey(),
               PrecisionTermConsumer.createPreciseTerms( group.getValue() ), cuiConcepts );
      }


      groupedSemanticCuis.clear();
      for ( Map.Entry<SemanticTui, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> group
            : groupedTuiCuis.entrySet() ) {
         final SemanticTui tui = group.getKey();
         if ( tui != SemanticTui.T024
              && tui != SemanticTui.T031
              && tui != SemanticTui.T025
              && tui != SemanticTui.T028 ) {
            // only handle tissue, fluid/misc anatomy, cell, gene
            continue;
         }
         final int cTakesConst = tui.getGroupCode();
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> map
               = groupedSemanticCuis.computeIfAbsent( cTakesConst, c -> new HashSetMap<>() );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> innerMap = group.getValue();
         for ( TextSpan span : innerMap.keySet() ) {
            map.addAllValues( span, innerMap.getCollection( span ) );
         }
      }
      // Finally deal with everything at once
      for ( Map.Entry<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> group : groupedSemanticCuis
            .entrySet() ) {
         consumeTypeIdHits( jcas, codingScheme, group.getKey(),
               PrecisionTermConsumer.createPreciseTerms( group.getValue() ), cuiConcepts );
      }
   }


   protected static Collection<SemanticTui> getUsedTuis(
         final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts ) {
      final Collection<SemanticTui> usedTuis = new HashSet<>();
      for ( Collection<Concept> concepts : cuiConcepts.getAllCollections() ) {
         concepts.stream()
                 .map( c -> c.getCodes( Concept.TUI ) )
                 .flatMap( Collection::stream )
                 .filter( Objects::nonNull )
                 .map( SemanticTui::getTuiFromCode )
                 .forEach( usedTuis::add );
      }
      return usedTuis;
   }

   static protected boolean hasTui( final SemanticTui tui, final Collection<Concept> concepts ) {
      return concepts.stream()
                     .map( c -> c.getCodes( Concept.TUI ) )
                     .flatMap( Collection::stream )
                     .filter( Objects::nonNull )
                     .map( SemanticTui::getTuiFromCode )
                     .anyMatch( t -> t == tui );
   }


   static private void removeUnwantedSpans( final int wantedTypeId, final int unwantedTypeId,
                                            final Map<Integer,
                                                  CollectionMap<TextSpan,
                                                        Long, ? extends Collection<Long>>> groupedSemanticCuis ) {
      if ( !groupedSemanticCuis.containsKey( wantedTypeId ) || !groupedSemanticCuis.containsKey( unwantedTypeId ) ) {
         return;
      }
      final Iterable<TextSpan> wantedSpans = groupedSemanticCuis.get( wantedTypeId )
                                                                .keySet();
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> typeTextSpanCuis
            = groupedSemanticCuis.get( unwantedTypeId );
      for ( TextSpan wantedSpan : wantedSpans ) {
         typeTextSpanCuis.remove( wantedSpan );
      }
   }

   static private void copyTerms( final int typeId,
                                  final Map<Integer, CollectionMap<TextSpan,
                                        Long, ? extends Collection<Long>>> groupedSemanticCuis,
                                  final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copyTermsMap ) {
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> spanCuis
            = groupedSemanticCuis.get( typeId );
      if ( spanCuis == null ) {
         return;
      }
      for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCui : spanCuis ) {
         copyTermsMap.addAllValues( spanCui.getKey(), spanCui.getValue() );
      }
   }

   static private void copyTerms(
         final SemanticTui tui,
         final Map<SemanticTui, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> groupedTuiCuis,
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copyTermsMap ) {
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> spanCuis = groupedTuiCuis.get( tui );
      if ( spanCuis == null ) {
         return;
      }
      for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCui : spanCuis ) {
         copyTermsMap.addAllValues( spanCui.getKey(), spanCui.getValue() );
      }
   }


   /**
    * Only uses the largest spans for the type
    * {@inheritDoc}
    */
   @Override
   public void consumeTypeIdHits( final JCas jcas, final String defaultScheme, final int cTakesSemantic,
                                  final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticTerms,
                                  final CollectionMap<Long, Concept, ? extends Collection<Concept>> conceptMap )
         throws AnalysisEngineProcessException {
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseTerms
            = createPreciseTerms( semanticTerms );
      consumePreciseTypeIdHits( jcas, defaultScheme, cTakesSemantic, preciseTerms, conceptMap );
   }

   public void consumePreciseTypeIdHits( final JCas jcas, final String codingScheme, final int cTakesSemantic,
                                         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                                         final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException {

//      LOGGER.info( "POST-CLEANUP" );
//      textSpanCuis.entrySet().stream().map( tsc -> toTestText( tsc, cuiConcepts) ).forEach( LOGGER::info );

      // Collection of UmlsConcept objects
      final Collection<UmlsConcept> umlsConceptList = new ArrayList<>();
      try {
         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
            umlsConceptList.clear();
            for ( Long cuiCode : spanCuis.getValue() ) {
               umlsConceptList.addAll(
                     createUmlsConcepts( jcas, codingScheme, cTakesSemantic, cuiCode, cuiConcepts ) );
            }
            final FSArray conceptArr = new FSArray( jcas, umlsConceptList.size() );
            int arrIdx = 0;
            for ( UmlsConcept umlsConcept : umlsConceptList ) {
               conceptArr.set( arrIdx, umlsConcept );
               arrIdx++;
            }
            final IdentifiedAnnotation annotation = createSemanticAnnotation( jcas, cTakesSemantic );
            annotation.setTypeID( cTakesSemantic );
            annotation.setBegin( spanCuis.getKey().getStart() );
            annotation.setEnd( spanCuis.getKey().getEnd() );
            annotation.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
            annotation.setOntologyConceptArr( conceptArr );
            annotation.addToIndexes();
         }
      } catch ( CASRuntimeException crtE ) {
         // What is really thrown?  The jcas "throwFeatMissing" is not a great help
         throw new AnalysisEngineProcessException( crtE );
      }
   }

   static public IdentifiedAnnotation createSemanticAnnotation( final JCas jcas, final int cTakesSemantic ) {
      return SemanticGroup.getGroup( cTakesSemantic ).getCreator().apply( jcas );
   }

   private Collection<UmlsConcept> createUmlsConcepts( final JCas jcas,
                                                       final String codingScheme,
                                                       final int cTakesSemantic,
                                                       final Long cuiCode,
                                                       final CollectionMap<Long, Concept, ? extends Collection<Concept>> conceptMap ) {
      final Collection<Concept> concepts = conceptMap.getCollection( cuiCode );
      if ( concepts == null || concepts.isEmpty() ) {
         return Collections.singletonList( createSimpleUmlsConcept( jcas, codingScheme,
               CuiCodeUtil.getInstance().getAsCui( cuiCode ) ) );
      }
      final Collection<UmlsConcept> umlsConcepts = new HashSet<>();
      for ( Concept concept : concepts ) {
         final Collection<Integer> allSemantics = concept.getCtakesSemantics();
         if ( !allSemantics.contains( cTakesSemantic ) ) {
            continue;
         }
         final Collection<String> tuis = concept.getCodes( Concept.TUI );
         if ( !tuis.isEmpty() ) {
            for ( String tui : tuis ) {
               if ( SemanticTui.getTuiFromCode( tui )
                               .getGroupCode() == cTakesSemantic ) {
                  umlsConcepts.addAll( _umlsConceptCreator.createUmlsConcepts( jcas, codingScheme, tui, concept ) );
               }
            }
         }
      }
      return umlsConcepts;
   }


   static private UmlsConcept createSimpleUmlsConcept( final JCas jcas, final String codingScheme, final String cui ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCodingScheme( codingScheme );
      umlsConcept.setCui( cui );
      return umlsConcept;
   }


   /**
    * Refine a collection of dictionary terms to only contain the most specific variations:
    * "colon cancer" instead of "cancer", performed by span inclusion /complete containment, not overlap
    *
    * @param semanticTerms terms in the dictionary
    * @return terms with the longest spans
    */
   static public CollectionMap<TextSpan, Long, ? extends Collection<Long>> createPreciseTerms(
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticTerms ) {
      final Collection<TextSpan> discardSpans = new HashSet<>();
      final List<TextSpan> textSpans = new ArrayList<>( semanticTerms.keySet() );
      final int count = textSpans.size();
      for ( int i = 0; i < count; i++ ) {
         final TextSpan spanKeyI = textSpans.get( i );
         for ( int j = i + 1; j < count; j++ ) {
            final TextSpan spanKeyJ = textSpans.get( j );
            if ( (spanKeyJ.getStart() <= spanKeyI.getStart() && spanKeyJ.getEnd() > spanKeyI.getEnd())
                 || (spanKeyJ.getStart() < spanKeyI.getStart() && spanKeyJ.getEnd() >= spanKeyI.getEnd()) ) {
               // J contains I, discard less precise concepts for span I and move on to next span I
               if ( spanKeyJ instanceof MultiTextSpan ) {
                  boolean spanIok = false;
                  for ( TextSpan missingSpanKey : ((MultiTextSpan)spanKeyJ).getMissingSpans() ) {
                     if ( (missingSpanKey.getStart() >= spanKeyI.getStart()
                           && missingSpanKey.getStart() < spanKeyI.getEnd())
                          || (missingSpanKey.getEnd() > spanKeyI.getStart()
                              && missingSpanKey.getEnd() <= spanKeyI.getEnd()) ) {
                        // I overlaps a missing span, so it is actually ok
                        spanIok = true;
                        break;
                     }
                  }
                  if ( !spanIok ) {
                     discardSpans.add( spanKeyI );
                     break;
                  }
               } else {
                  discardSpans.add( spanKeyI );
                  break;
               }
            }
            if ( ((spanKeyI.getStart() <= spanKeyJ.getStart() && spanKeyI.getEnd() > spanKeyJ.getEnd())
                  || (spanKeyI.getStart() < spanKeyJ.getStart() && spanKeyI.getEnd() >= spanKeyJ.getEnd())) ) {
               // I contains J, discard less precise concepts for span J and move on to next span J
               if ( spanKeyI instanceof MultiTextSpan ) {
                  boolean spanJok = false;
                  for ( TextSpan missingSpanKey : ((MultiTextSpan)spanKeyI).getMissingSpans() ) {
                     if ( (missingSpanKey.getStart() >= spanKeyJ.getStart()
                           && missingSpanKey.getStart() < spanKeyJ.getEnd())
                          || (missingSpanKey.getEnd() > spanKeyJ.getStart()
                              && missingSpanKey.getEnd() <= spanKeyJ.getEnd()) ) {
                        // J overlaps a missing span, so it is actually ok
                        spanJok = true;
                        break;
                     }
                  }
                  if ( !spanJok ) {
                     discardSpans.add( spanKeyJ );
                  }
               } else {
                  discardSpans.add( spanKeyJ );
               }
            }
         }
      }
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseHitMap = new HashSetMap<>(
            textSpans.size() - discardSpans.size() );
      for ( Map.Entry<TextSpan, ? extends Collection<Long>> entry : semanticTerms ) {
         if ( !discardSpans.contains( entry.getKey() ) ) {
            preciseHitMap.addAllValues( entry.getKey(), entry.getValue() );
         }
      }
      return preciseHitMap;
   }


   static private String toTestText( final Map.Entry<TextSpan, ? extends Collection<Long>> textSpanCuis,
                                     final Map<Long, ? extends Collection<Concept>> cuiConcepts ) {
      final String ts = textSpanCuis.getKey().getStart() + "," + textSpanCuis.getKey().getEnd() + " ";
      final String concepts = textSpanCuis.getValue().stream()
                                          .map( cuiConcepts::get )
                                          .flatMap( Collection::stream )
                                          .map( concept -> concept.getCui() + " " + concept.getPreferredText() )
                                          .collect( Collectors.joining( " " ) );
      return ts + concepts;
   }

}
