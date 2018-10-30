package org.apache.ctakes.cancer.ae.section;


import org.apache.ctakes.core.ae.RegexSectionizer;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2016
 */
final public class SectionRemover extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());


   // TODO  Add cancer-to-disease threshold in docType enums - or even SectType enums
   // TODO  Add cancer-weight to sectType enums, maybe even DocType enums

   /**
    * Where Sentences are in certain unwanted sections, they are removed.
    * {@inheritDoc}
    */
   @Override
   public void process(final JCas jcas) throws AnalysisEngineProcessException {
      LOGGER.info("Temporarily removing unwanted Sections ...");
      final Collection<Segment> sections = JCasUtil.select(jcas, Segment.class);
      if (sections == null || sections.isEmpty()) {
         LOGGER.info("Finished Processing");
         return;
      }
      final String documentId = DocumentIDAnnotationUtil.getDocumentID(jcas);
      final Collection<Segment> sectionsToRemoveFromIndexes = new HashSet<>();

      // Remove those sections that always get removed
      for (Segment section : sections) {
         final String title = section.getPreferredText();
         if (title != null) {
            if (title.equals("Pittsburgh Header") || title.equals("Histology Summary") ||
                    title.equals("Family Medical History")) {
               LOGGER.info("Hiding section in fixed list " + section.getTagText());
               SectionHolder.getInstance().addHiddenSection(documentId, section);
               sectionsToRemoveFromIndexes.add(section);
            } else if (title.equals(RegexSectionizer.DIVIDER_LINE_NAME)) {
               sectionsToRemoveFromIndexes.add(section);
            }
         }
      }

      Collection<Segment> narrowedListOfSections = new HashSet<>();
      narrowedListOfSections.addAll(sections);
      if (sectionsToRemoveFromIndexes != null) narrowedListOfSections.removeAll(sectionsToRemoveFromIndexes);

      List<Segment> impressionSections = new ArrayList<>();
      List<Segment> endOfImpressionSections = new ArrayList<>();
      // IMPRESSION and END OF IMPRESSION might not pair up nicely, for example if there is an END OF IMPRESSION before any IMPRESSION,
      // or 2 END OF IMPRESSION and only 1 IMPRESSION
      List<Segment> matchingEndingSections = new ArrayList<>();
      for (Segment section : narrowedListOfSections) {
         if (SectionHelper.isImpressionSection(section)) {
            impressionSections.add(section);
         } else if (SectionHelper.isEndOfImpressionSection(section)) {
            endOfImpressionSections.add(section);
         }
      }

      for (Segment impression : impressionSections) {
         Segment potentialMatch = null;
         for (Segment endImpression : endOfImpressionSections) {
            if (endImpression.getBegin() >= impression.getEnd()) {
               // select the closest "END OF IMPRESSION" that is after the end of the section that is called IMPRESSION or its variants
               if (potentialMatch == null || potentialMatch.getBegin() > endImpression.getBegin()) {
                  potentialMatch = endImpression;
               }
            }
         }
         if (potentialMatch != null) {
            endOfImpressionSections.remove(potentialMatch);
            matchingEndingSections.add(potentialMatch);
         } else {
            matchingEndingSections.add(null); // keep them in sync by index number
         }

      }

      // If there is a heading between IMPRESSION and END OF IMPRESSION, remove those and extend the IMPRESSION section to include
      // the (sub)sections that IMPRESSION used to include
      List<Segment> toRemoveFromList = new ArrayList<Segment>();

      int index = -1;
      for (Segment sectionImpression : impressionSections) {
         index++;
         Segment matchingEndOfImpression = matchingEndingSections.get(index);
         if (matchingEndOfImpression != null) {
            if (!SectionHelper.isEndOfImpressionSection(matchingEndOfImpression))
               throw new RuntimeException("Unexpected section type here");
            int beginOfEOI = matchingEndOfImpression.getBegin();
            if (beginOfEOI >= sectionImpression.getEnd()) { // extend IMPRESSION section to the END OF IMPRESSION "tag"
               sectionImpression.setEnd(beginOfEOI);
               List<Segment> subsections = JCasUtil.selectCovered(Segment.class, sectionImpression);
               toRemoveFromList.addAll(subsections);
               // Do not add these to the SectionHolder because they are subsumed now by IMPRESSION.
               // If added them back later their contents would appear in both IMPRESSION and themselves
               for (Segment s : subsections) {
                  LOGGER.info("Extended an 'Impression' section to include what had been subsection " + s.getTagText());
                  sectionsToRemoveFromIndexes.add(s);
               }
               // Don't add those subsections as HiddenSections, because their text is captured by IMPRESSION now

               // Decided not to remove the END OF IMPRESSION sections in case there is a missing header after the END OF IMPRESSION
               // instead of just ignore any text there
               ////// Remove the End of Impression section, which should not contain any meaningful text
               ////toRemoveFromList.add(matchingEndOfImpression);
               ////LOGGER.info("Hiding End of Impression section " + matchingEndOfImpression.getTagText());
               ////SectionHolder.getInstance().addHiddenSection(documentId, matchingEndOfImpression);
               ////sectionsToRemoveFromIndexes.add(matchingEndOfImpression);
            }
         }
      }

      if (toRemoveFromList != null) {
         narrowedListOfSections.removeAll(toRemoveFromList);
         toRemoveFromList.clear();
      }

      // If there is a final dx (or its variants) and/or impression (or its variants) and/or findings section (or its variants)
      // in a RAD or SP (Pathology) document, only these sections are passed for processing.
      // For other types such as clinical notes (NOTE, PGN, DS), if no section was found to be a section of interest, ignore all sections.
      List<Segment> sectionsOfInterest = new ArrayList<>();

      String documentType = DocumentType.getDocumentType(jcas);
      for (Segment section : narrowedListOfSections) {
         if (isSectionOfInterest(section, documentType)) {
            sectionsOfInterest.add(section);
         }
      }
      // For radiology or pathology reports, use the entire report if we didn't find any sections of particular interest)
      // but if found certain sections, then just use those certain sections
      if (!sectionsOfInterest.isEmpty() || !DocumentType.isRadOrPath(documentType)) {
         // Hide sections other than those of particular interest
         for (Segment section : narrowedListOfSections) {
            if (!sectionsOfInterest.contains(section)) {
               LOGGER.info("Hiding section that is not one of the sections specifically interested in " + section.getTagText());  // null indicates SIMPLE_SEGMENT
               SectionHolder.getInstance().addHiddenSection(documentId, section);
               sectionsToRemoveFromIndexes.add(section);
            }
         }
      }

      // sectionsToRemoveFromIndexes.forEach( Annotation::removeFromIndexes );
      for (Segment s : sectionsToRemoveFromIndexes) {
         LOGGER.info("Removing section from indexes " + s.getTagText());
         s.removeFromIndexes();
      }
      LOGGER.info("Finished Processing");
   }

   // TODO refactor to use DocType and SectType:
   // TODO        return MajorDocType.getMajorDocType.isWantedSection( section );
   private boolean isSectionOfInterest( Segment section, String documentType) {
      return (DocumentType.isRadOrPath(documentType) && SectionHelper.isFinalDiagnosisSection(section) ||
              DocumentType.isRadOrPath(documentType) && SectionHelper.isImpressionSection(section) ||
              DocumentType.isRadOrPath(documentType) && SectionHelper.isFindingsSection(section) ||
              DocumentType.isRadOrPath(documentType) && SectionHelper.isHistologySummarySection(section) ||
              DocumentType.isPath(documentType) && SectionHelper.isClinicalInfoSection(section) ||
              DocumentType.isPath(documentType) && SectionHelper.isFullTextSection(section) ||
              DocumentType.isPath(documentType) && SectionHelper.isAddendumComment(section) ||
              DocumentType.isClinicalNote(documentType) && SectionHelper.isPrincipalDiagnosisSection(section) ||
              DocumentType.isClinicalNote(documentType) && SectionHelper.isChiefComplaint(section) ||
              DocumentType.isClinicalNote(documentType) && SectionHelper.isFinalDiagnosisSection(section) ||// needed for patientX staging, laterality and clockface
              // put any additional sections here....
              //DocumentType.isClinicalNote(documentType) && SectionHelper.isImpressionSection(section) ||

              // the following lists those types that are not going to have all sections processed
              // thereby allowing any other types to have all their sections processed
              (!DocumentType.isRadOrPath(documentType) && !DocumentType.isClinicalNote(documentType))
      );
   }

   // TODO why this internal class?  remove and use DocType and SectType
   static class DocumentType {

      static private boolean isClinicalNote(String documentType) {
         if (documentType.contains("NOTE") || documentType.contains("PGN") || documentType.contains("DS")) {
            return true;
         }
         return false;
      }

      static private boolean isPath(String documentType) {
         if (documentType.contains("SP") || documentType.contains("PATH")) {
            return true;
         }
         return false;
      }
      static private boolean isRadOrPath(String documentType) {
         if (documentType.contains("RAD") || isPath(documentType)) {
            return true;
         }
         return false;
      }
      static private String getDocumentType(JCas cas) {
         String documentType = DocumentIDAnnotationUtil.getDocumentID(cas);
         if (documentType==null || documentType.trim().length()==0) {
            LOGGER.warn("Unable to determine document type from document ID. DocumentIDAnnotationUtil.getDocumentID returned '" + documentType + "'");
            documentType = "";
         }
         return documentType;  // for now just use the document name as the document type
      }

   }
}
