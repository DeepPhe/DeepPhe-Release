package org.healthnlp.deepphe.nlp.ae.relation.penalty;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.healthnlp.deepphe.nlp.section.DefinedSectionType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.healthnlp.deepphe.nlp.section.DefinedSectionType.*;

/**
 * @author SPF , chip-nlp
 * @since {11/14/2023}
 */
final class SectionPenalty {
   static private final double PREFERRED_SECTION_BUMP = -30;
   static private final double LESSER_SECTION_PENALTY = 20;

   static private final Collection<DefinedSectionType> PREFERRED_SECTIONS = new HashSet<>( Arrays.asList(
         FinalDiagnosis, ClinicalHistory, PreOpDiagnosis, PostOpDiagnosis, PrincipalDiagnosis, ClinicalData
   ) );
   static private final Collection<DefinedSectionType> LESSER_SECTIONS = new HashSet<>( Arrays.asList(
         PriorTherapy, ReviewSystems, PastSurgicalHistory, HistologySummary,
         CurrentTherapy, FamilyHistory
   ) );
   // Not in ctakes, in dphe
   static private final Collection<String> LESSER_SECTION_NAMES =new HashSet<>( Arrays.asList(
         "specimen information", "microscopic description", "biopsy results"
   ) );
   static private final Collection<DefinedSectionType> NEOPLASM_LESSER_SECTIONS = new HashSet<>( Arrays.asList(
         Microscopic, ClinicalInfo
   ) );


   private SectionPenalty() {}

   static double getPenalty( final IdentifiedAnnotation annotation ) {
      final DefinedSectionType sectionType = getSectionType( annotation );
      if ( PREFERRED_SECTIONS.contains( sectionType ) ) {
         return PREFERRED_SECTION_BUMP;
      }
      if ( LESSER_SECTIONS.contains( sectionType )
            || LESSER_SECTION_NAMES.contains( getSectionId( annotation ).toLowerCase() ) ) {

         return LESSER_SECTION_PENALTY;
      }
      return 0;
   }

   static double getNeoplasmSectionPenalty( final IdentifiedAnnotation annotation ) {
      final DefinedSectionType sectionType = SectionPenalty.getSectionType( annotation );
      if ( NEOPLASM_LESSER_SECTIONS.contains( sectionType ) ) {
         return LESSER_SECTION_PENALTY;
      }
      return getPenalty( annotation );
   }

   static double getLocationSectionPenalty( final IdentifiedAnnotation annotation ) {
      return getNeoplasmSectionPenalty( annotation );
   }


   static DefinedSectionType getSectionType( final IdentifiedAnnotation annotation ) {
      final String sectionId = getSectionId( annotation );
      final DefinedSectionType sectionType = DefinedSectionType.getSectionType( sectionId );
//      NeoplasmSummaryCreator.addDebug( "Section: " + sectionType.getName() + " "
//                                       + sectionId + " "
//                                       + Neo4jOntologyConceptUtil.getUri( annotation ) + "\n" );
      return sectionType;
   }

   static private String getSectionId( final IdentifiedAnnotation annotation ) {
      final String sectionId = annotation.getSegmentID();
      if ( sectionId == null ) {
         return "";
      }
      final int scoreIndex = sectionId.indexOf( '_' );
      if ( scoreIndex > 0 ) {
         return sectionId.substring( 0, scoreIndex );
      }
      return sectionId;
   }

}
