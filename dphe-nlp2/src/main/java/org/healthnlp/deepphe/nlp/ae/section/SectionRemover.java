package org.healthnlp.deepphe.nlp.ae.section;


import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.nlp.document.MajorDocType;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.nlp.document.MajorDocType.*;
import static org.healthnlp.deepphe.nlp.section.DefinedSectionType.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2016
 */
final public class SectionRemover extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( MethodHandles.lookup().lookupClass().getSimpleName() );


   // TODO  Add cancer-to-disease threshold in docType enums - or even SectType enums
   // TODO  Add cancer-weight to sectType enums, maybe even DocType enums


   /**
    * Where Sentences are in certain unwanted sections, they are removed.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Removing unwanted Sections ..." );

      adjustImpressionSections( jcas );

      final String noteTypeCode = SourceMetadataUtil.getOrCreateSourceData( jcas ).getNoteTypeCode();
      final MajorDocType majorDocType = MajorDocType.getMajorDocType( noteTypeCode );
      final Collection<Segment> sections = JCasUtil.select( jcas, Segment.class );
      if ( sections == null || sections.isEmpty() ) {
         LOGGER.info( "No Sections in " + majorDocType.name().toLowerCase() + " note." );
         return;
      }

      // If there is a final dx (or its variants) and/or impression (or its variants) and/or findings section (or its variants)
      // in a RAD or SP (Pathology) document, only these sections are passed for processing.
      // For other types such as clinical notes (NOTE, PGN, DS), if no section was found to be a section of interest, ignore all sections.
      final List<Segment> sectionsOfInterest
            = sections.stream()
                      .filter( majorDocType::isWantedSection )
                      .collect( Collectors.toList() );

      final Collection<Segment> sectionsToRemove = new ArrayList<>();
      if ( sectionsOfInterest.isEmpty() && (majorDocType == RADIOLOGY || majorDocType == PATHOLOGY) ) {
         // For radiology or pathology reports,
         // use the entire report if we didn't find any sections of particular interest)
         final Predicate<Segment> alwaysRemove = s -> PittsburghHeader.isThisSectionType( s )
                                                      || FamilyHistory.isThisSectionType( s );
         sections.stream()
                 .filter( alwaysRemove )
                 .forEach( sectionsToRemove::add );
      } else {
         // but if found certain sections, then just use those certain sections
         // Hide sections other than those of particular interest
         sections.stream()
                 .filter( s -> !sectionsOfInterest.contains( s ) )
                 .forEach( sectionsToRemove::add );
      }

      sectionsToRemove.forEach( Segment::removeFromIndexes );
   }


   static private void adjustImpressionSections( final JCas jCas ) {
      final Collection<Segment> sections = JCasUtil.select( jCas, Segment.class );
      if ( sections == null || sections.isEmpty() ) {
         return;
      }
      final Map<Segment, Segment> impressionsMap = new HashMap<>();

      final List<Segment> sortedSections = new ArrayList<>( sections );
      sortedSections.sort( Comparator.comparingInt( Annotation::getBegin ) );

      final Collection<Segment> subSectionsToRemove = new ArrayList<>();
      final Collection<Segment> subSections = new ArrayList<>();
      Segment currentImpression = null;
      for ( Segment section : sortedSections ) {
         if ( Impression.isThisSectionType( section ) ) {
            subSections.clear();
            currentImpression = section;
         } else if ( currentImpression != null ) {
            if ( EndOfImpression.isThisSectionType( section ) ) {
               if ( !subSections.isEmpty() ) {
                  impressionsMap.put( currentImpression, section );
                  subSectionsToRemove.addAll( subSections );
                  LOGGER.info( "Extended an 'Impression' section to include what had been subsections: " );
                  subSections.forEach( s -> LOGGER.info( "   " + s.getPreferredText() + "  =  " + s.getTagText() ) );
                  subSections.clear();
               }
               currentImpression = null;
            } else {
               subSections.add( section );
            }
         }
      }

      for ( Map.Entry<Segment, Segment> impressionEntry : impressionsMap.entrySet() ) {
         final Segment sourceImpression = impressionEntry.getKey();
         final Segment impression = new Segment( jCas,
               sourceImpression.getBegin(),
               impressionEntry.getValue().getBegin() );
         impression.setPreferredText( sourceImpression.getPreferredText() );
         impression.setTagText( sourceImpression.getTagText() );
         impression.addToIndexes();
         subSectionsToRemove.add( sourceImpression );
         // Decided not to remove the END OF IMPRESSION sections in case there is a missing header after the END OF IMPRESSION
//         sectionsToRemove.add( impressionEntry.getValue() );
      }
      subSectionsToRemove.forEach( Annotation::removeFromIndexes );
   }


}
