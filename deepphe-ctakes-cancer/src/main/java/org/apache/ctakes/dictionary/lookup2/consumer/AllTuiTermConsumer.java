package org.apache.ctakes.dictionary.lookup2.consumer;


import org.apache.ctakes.core.semantic.SemanticGroup;
import org.apache.ctakes.core.semantic.SemanticTui;
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

/**
 * Traditional ctakes semantic groups ignore a large number of semantic types.
 * This class adds extra semantic groups to ctakes from a dictionary.
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2018
 */
public class AllTuiTermConsumer extends AbstractTermConsumer {

   static private final Logger LOGGER = Logger.getLogger( "AllTuiTermConsumer" );

   static private final Long LEFT_CUI = 1552822l;
   static private final Long RIGHT_CUI = 1552823l;
   static private final Long BILATERAL_CUI = 238767l;


   final private UmlsConceptCreator _umlsConceptCreator;

   public AllTuiTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      this( uimaContext, properties, new DefaultUmlsConceptCreator() );
   }

   public AllTuiTermConsumer( final UimaContext uimaContext, final Properties properties,
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
      final Collection<Integer> usedcTakesSemantics = getUsedcTakesSemantics( cuiConcepts );
      final Map<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> groupedSemanticCuis
            = new HashMap<>();
      // The dictionary may have more than one type, create a map of types to terms and use them all
      for ( Integer cTakesSemantic : usedcTakesSemantics ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticTerms = new HashSetMap<>();
         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
            for ( Long cuiCode : spanCuis.getValue() ) {
               final Collection<Concept> concepts = cuiConcepts.getCollection( cuiCode );
               if ( hascTakesSemantic( cTakesSemantic, concepts ) ) {
                  semanticTerms.placeValue( spanCuis.getKey(), cuiCode );
               }
            }
         }
         groupedSemanticCuis.put( cTakesSemantic, semanticTerms );
      }

      // Clean up sign/symptom and finding spans that are also anatomical sites
      removeUnwantedSpans( CONST.NE_TYPE_ID_ANATOMICAL_SITE, CONST.NE_TYPE_ID_FINDING, groupedSemanticCuis );
      removeUnwantedSpans( CONST.NE_TYPE_ID_ANATOMICAL_SITE, CONST.NE_TYPE_ID_DISORDER, groupedSemanticCuis );
      // Clean up findings that are also disorders
      removeUnwantedSpans( CONST.NE_TYPE_ID_DISORDER, CONST.NE_TYPE_ID_FINDING, groupedSemanticCuis );
      // Clean up Findings and labs that are also procedures
      removeUnwantedSpans( CONST.NE_TYPE_ID_PROCEDURE, CONST.NE_TYPE_ID_FINDING, groupedSemanticCuis );
      removeUnwantedSpans( CONST.NE_TYPE_ID_PROCEDURE, CONST.NE_TYPE_ID_LAB, groupedSemanticCuis );

      // Clean up findings that are also within anatomical sites
      if ( groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_FINDING )
            && ( groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_ANATOMICAL_SITE )
            || groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_DISORDER )
            || groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_PROCEDURE ) ) ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copiedTerms = new HashSetMap<>();
         copyTerms( CONST.NE_TYPE_ID_ANATOMICAL_SITE, groupedSemanticCuis, copiedTerms );
         copyTerms( CONST.NE_TYPE_ID_DISORDER, groupedSemanticCuis, copiedTerms );
         copyTerms( CONST.NE_TYPE_ID_PROCEDURE, groupedSemanticCuis, copiedTerms );
         copyTerms( CONST.NE_TYPE_ID_FINDING, groupedSemanticCuis, copiedTerms );
         // We just created a collection with only the largest Textspans.
         // Any smaller Finding textspans are therefore within a larger d/d textspan and should be removed.
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseTerms
               = PrecisionTermConsumer.createPreciseTerms( copiedTerms );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> findingSpanCuis
               = groupedSemanticCuis.get( CONST.NE_TYPE_ID_FINDING );
         final Collection<TextSpan> findingSpans = new ArrayList<>( findingSpanCuis.keySet() );
         for ( TextSpan span : findingSpans ) {
            if ( !preciseTerms.containsKey( span ) ) {
               if ( findingSpanCuis.get( span ).contains( LEFT_CUI )
                    || findingSpanCuis.get( span ).contains( RIGHT_CUI )
                    || findingSpanCuis.get( span ).contains( BILATERAL_CUI ) ) {
                  continue;
               }
               findingSpanCuis.remove( span );
            }
         }
      }

      // Clean up findings and labs that are also within procedure spans
      if ( groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_LAB )
            && groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_PROCEDURE ) ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copiedTerms = new HashSetMap<>();
         copyTerms( CONST.NE_TYPE_ID_PROCEDURE, groupedSemanticCuis, copiedTerms );
         copyTerms( CONST.NE_TYPE_ID_LAB, groupedSemanticCuis, copiedTerms );
         // We just created a collection with only the largest Textspans.
         // Any smaller Finding textspans are therefore within a larger d/d textspan and should be removed.
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseTerms
               = PrecisionTermConsumer.createPreciseTerms( copiedTerms );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> labSpanCuis
               = groupedSemanticCuis.get( CONST.NE_TYPE_ID_LAB );
         final Collection<TextSpan> labSpans = new ArrayList<>( labSpanCuis.keySet() );
         for ( TextSpan span : labSpans ) {
            if ( !preciseTerms.containsKey( span ) ) {
               labSpanCuis.remove( span );
            }
         }
      }

      // Finally deal with everything at once
      for ( Map.Entry<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> group : groupedSemanticCuis
            .entrySet() ) {
         consumeTypeIdHits( jcas, codingScheme, group.getKey(),
                            PrecisionTermConsumer.createPreciseTerms( group.getValue() ), cuiConcepts );
      }
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


}
