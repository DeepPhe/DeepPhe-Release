package org.healthnlp.deepphe.nlp.cr.naaccr;


import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.healthnlp.deepphe.core.document.SectionType;

import static org.healthnlp.deepphe.core.document.SectionType.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
public enum NaaccrSectionType implements NaaccrItemType {
   // ePath
   PATH_TEXT( "textPathFullText", "PATH", FullText, true ),
   SPECIMEN_NATURE( "textPathNatureOfSpecimens", "PATH", OtherSection, false ),  // when true, scores were the same
   MICRO_DESCRIPTION( "textPathMicroscopicDesc", "PATH", Microscopic, false ),   // Try true, need to modify relation finders
   FORMAL_DIAGNOSIS( "textPathFormalDx", "PATH", FinalDiagnosis, true ),
   CLINICAL_HISTORY( "textPathClinicalHistory", "PATH", ClinicalHistory, true ),
   // clinical
   PATH_HISTORY( "textDxProcPe", "DS", OtherSection, false ),
   PATH_X_RAY( "textDxProcXRayScan", "DS", OtherSection, false ),
   PATH_MICROSCOPY( "textDxProcScopes", "DS", OtherSection, false ),
   PATH_LAB_TESTS( "textDxProcLabTests", "DS", OtherSection, false ),
   PATH_OPERATION( "textDxProcOp", "DS", OtherSection, false ),
   PATH_DIAGNOSIS( "textDxProcPath", "DS", Pathology, true ),
   PATH_PRIMARY( "textPrimarySiteTitle", "DS", Finding, true ),
   PATH_HISTOLOGY( "textHistologyTitle", "DS", Finding, true ),
   PATH_STAGE( "textStaging", "DS", PreOpDiagnosis, true ),
   PATH_REMARKS( "textRemarks", "DS", AddendumComment, false ),
   PATH_PROVIDER( "textPlaceOfDiagnosis", "DS", OtherSection, false ),

   UNKNOWN( "UNKNOWN", NoteSpecs.ID_NAME_CLINICAL_NOTE, OtherSection, false );


   private final String _id;
   private final String _noteType;
   private final SectionType _sectionType;
   private final boolean _parse;

   NaaccrSectionType( final String id, final String noteType, final SectionType sectionType, final boolean parse ) {
      _id = id;
      _noteType = noteType;
      _sectionType = sectionType;
      _parse = parse;
   }

   public String getNoteType() {
      return _noteType;
   }

   public SectionType getSectionType() {
      return _sectionType;
   }

   public boolean shouldParse() {
      return _parse;
   }

   static public NaaccrSectionType getSectionType( final String id ) {
      for ( NaaccrSectionType naaccrSectionType : values() ) {
         if ( naaccrSectionType._id.equals( id ) ) {
            return naaccrSectionType;
         }
      }
      return UNKNOWN;
   }


}
