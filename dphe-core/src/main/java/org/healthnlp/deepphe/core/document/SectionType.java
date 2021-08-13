package org.healthnlp.deepphe.core.document;


import org.apache.ctakes.typesystem.type.textspan.Segment;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/12/2018
 */
public enum SectionType implements SectType {
   AddendumComment( "Addendum Comment" ),
   BasicInformation( "Basic Information" ),
   ChiefComplaint( "Chief Complaint" ),
   ClinicalHistory( "Clinical History" ),
   ClinicalData( "Clinical Data" ),
   ClinicalInfo( "Clinical Info" ),
   CurrentTherapy( "Current Therapy" ),
   EndOfImpression( "End of Impression" ),
   Examination( "Examination" ),
   FamilyHistory( "Family Medical History" ),
   FinalDiagnosis( "Final Diagnosis" ),
   Finding( "Findings" ),
   FullText( "Full Text" ),
   HistologySummary( "Histo Tissue Summary" ),
   History( "History" ),
   HistoryPresentIllness( "History of Present Illness" ),
   Impression( "Impression" ),
   Microscopic( "Microscopic"),
   PastMedicalHistory( "Past Medical History" ),
   PastSurgicalHistory( "Past Surgical History" ),
   Pathology( "Pathology" ),
   PittsburghHeader( "Pittsburgh Header" ),
   PostOpDiagnosis( "Post-op Diagnosis" ),
   PreOpDiagnosis( "Pre-op Diagnosis" ),
   PrincipalDiagnosis( "Principal Diagnosis" ),
   PriorTherapy( "Prior Therapy" ),
   ReviewSystems( "Review of Systems" ),
//   SimpleSegment( Sectionizer.SIMPLE_SEGMENT ),
   SimpleSegment( "SIMPLE_SEGMENT" ),
   OtherSection( "Other Section" );

   final private String _name;

   SectionType( final String name ) {
     _name = name;
   }

   public String getName() {
      return _name;
   }

   static public SectionType getSectionType( final Segment segment ) {
      return getSectionType( segment.getPreferredText() );
   }

   static public SectionType getSectionType( final String sectionName ) {
      if ( sectionName == null || sectionName.isEmpty() || sectionName.equals( "SIMPLE_SEGMENT" ) ) {
         return SimpleSegment;
      }
      final String name = sectionName.trim();
      for ( SectionType type : values() ) {
         if ( type.isThisSectionType( name ) ) {
            return type;
         }
      }
      return OtherSection;
   }

   static public String getStandardizedSectionName( final Segment segment ) {
      return segment.getPreferredText() != null
             ? segment.getPreferredText().trim().toLowerCase()
             : "SIMPLE_SEGMENT";
   }


}
