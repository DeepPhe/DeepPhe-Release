package org.healthnlp.deepphe.nlp.document;


import org.healthnlp.deepphe.nlp.section.DefinedSectionType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.healthnlp.deepphe.nlp.section.DefinedSectionType.*;


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

   private final Collection<DefinedSectionType> _validSections;

   MajorDocType( final DefinedSectionType... validSections ) {
      if ( validSections.length == 0 ) {
         _validSections = Collections.emptyList();
      } else {
         _validSections = Arrays.asList( validSections );
      }
   }

   public Collection<DefinedSectionType> getWantedSections() {
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
