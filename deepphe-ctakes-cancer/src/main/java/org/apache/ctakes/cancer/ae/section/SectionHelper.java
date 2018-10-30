package org.apache.ctakes.cancer.ae.section;

import org.apache.ctakes.typesystem.type.textspan.Segment;

public class SectionHelper {

    // See sections.txt in cancer/sections for the preferred text of the section names
    public static boolean isEndOfImpressionSection(Segment segment) {
        if (segment.getPreferredText() == null) return false;
        return segment.getPreferredText().trim().toLowerCase().startsWith("end of impression");
    }

    public static boolean isImpressionSection(Segment segment) {
        if (segment.getPreferredText() == null) return false;
        return segment.getPreferredText().trim().toLowerCase().startsWith("impression");
    }

    public static boolean isFindingsSection(Segment segment) {
        if (segment.getPreferredText() == null) return false;
        return segment.getPreferredText().trim().toLowerCase().startsWith("finding");
    }

    public static boolean isChiefComplaint(Segment section) {
        if (section.getPreferredText() == null) return false;
        return section.getPreferredText().trim().toLowerCase().startsWith("chief complaint");
    }

    public static boolean isClinicalInfoSection(Segment section) {
        if (section.getPreferredText() == null) return false;
        return section.getPreferredText().trim().toLowerCase().startsWith("clinical info");
    }

    public static boolean isFullTextSection(Segment section) {
        if (section.getPreferredText() == null) return false;
        return section.getPreferredText().trim().toLowerCase().startsWith("full text");
    }

    public static boolean isAddendumComment(Segment section) {
        if (section.getPreferredText() == null) return false;
        return section.getPreferredText().trim().toLowerCase().startsWith("addendum comment");
    }

    public static boolean isFinalDiagnosisSection(Segment section) {
        if (section.getPreferredText() == null) return false;
        return isFinalDiagnosisSection(section.getPreferredText());
    }

    public static boolean isFinalDiagnosisSection(String sectionPreferredText) {
        if (sectionPreferredText == null) return false;
        return sectionPreferredText.toLowerCase().startsWith("final diag");
    }

    public static boolean isHistologySummarySection(Segment section) {
        // Histo Tissue Summary
        if (section.getPreferredText() == null) return false;
        String lc = section.getPreferredText().toLowerCase();
        return lc.startsWith("histo") && lc.contains("tiss") && lc.contains("summ");
    }

    public static boolean isPrincipalDiagnosisSection(Segment section) {
        // Principal Diagnosis
        if (section.getPreferredText() == null) return false;
        String lc = section.getPreferredText().toLowerCase();
        return lc.startsWith("princi") && lc.contains("diag");
    }

    public static boolean isSimpleSegment(Segment section) {
        if (Sectionizer.SIMPLE_SEGMENT.equals(section.getId())) {
            return true;
        }
        return false;
    }

    public static String getStandardizedSectionName(Segment section) {
        return (section.getPreferredText()!=null ? section.getPreferredText() : "");
    }

}
