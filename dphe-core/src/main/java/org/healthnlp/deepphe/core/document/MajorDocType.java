package org.healthnlp.deepphe.core.document;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.healthnlp.deepphe.core.document.SectionType.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/12/2018
 */
public enum MajorDocType implements DocType {
   RADIOLOGY( ClinicalHistory, FinalDiagnosis, Finding, HistologySummary, Impression ),
   PATHOLOGY( AddendumComment, ClinicalData, ClinicalHistory, ClinicalInfo, FinalDiagnosis, Finding, FullText,
         HistoryPresentIllness,
         HistologySummary, Impression, Microscopic, PreOpDiagnosis, PostOpDiagnosis ),
   CLINICAL( BasicInformation, ChiefComplaint, FinalDiagnosis, Finding, HistoryPresentIllness, Impression, Pathology,
         PrincipalDiagnosis,
         PreOpDiagnosis, HistoryPresentIllness, PostOpDiagnosis, PriorTherapy, CurrentTherapy ),
   UNKNOWN();

   private final Collection<SectionType> _validSections;

   MajorDocType( final SectionType... validSections ) {
      if ( validSections.length == 0 ) {
         _validSections = Collections.emptyList();
      } else {
         _validSections = Arrays.asList( validSections );
      }
   }

   public Collection<SectionType> getWantedSections() {
      return _validSections;
   }

   public boolean isThisDocType( final String code ) {
      return this.equals( MinorDocType.getMinorDocType( code ).getMajorDocType() );
   }

   static public MajorDocType getMajorDocType( final String code ) {
      if ( code == null || code.isEmpty() ) {
         return UNKNOWN;
      }
      final String name = code.trim();
      for ( MajorDocType type : values() ) {
         if ( type.isThisDocType( name ) ) {
            return type;
         }
      }
      return UNKNOWN;
   }

}
