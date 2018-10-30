package org.apache.ctakes.cancer.ae.section;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates segment aka section annotations based on a sections.txt file.
 * This class is based on CDASegmentAnnotator from cTAKES trunk 7/2018
 */
@PipeBitInfo(
        name = "Sectionizer",
        description = "Annotates Document Sections by detecting Section Headers using Regular Expressions provided in a File.",
        dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID },
        products = { PipeBitInfo.TypeProduct.SECTION }
)

public class Sectionizer extends JCasAnnotator_ImplBase {

    Logger logger = Logger.getLogger(this.getClass());
    protected static HashMap<String, Pattern> patterns = new HashMap<>();
    protected static HashMap<String, String> section_names = new HashMap<>();
    protected static final String DEFAULT_SECTION_FILE_NAME = "org/apache/ctakes/cancer/sections/sections.txt";
    public static final String PARAM_FIELD_SEPARATOR = ",";
    public static final String PARAM_COMMENT = "#";
    public static final String SIMPLE_SEGMENT = "SIMPLE_SEGMENT";

    public static final String PARAM_SECTIONS_FILE = "sections_file";
    @ConfigurationParameter(name = PARAM_SECTIONS_FILE,
            description = "Path to File that contains the section header mappings",
            defaultValue=DEFAULT_SECTION_FILE_NAME,
            mandatory=false)
    protected String sections_path;

    /**
     * Init and load the sections mapping file and precompile the regex matches
     */
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {

        super.initialize(aContext);

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(FileLocator.getAsStream(sections_path)));

            // Read in the Section Mappings File
            // And load the RegEx Patterns into a Map
            logger.info("Reading Section File " + sections_path);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.trim().startsWith(PARAM_COMMENT)) {
                    String[] fields = line.split(PARAM_FIELD_SEPARATOR);
                    // First column is the id/name
                    if (fields != null && fields.length > 1 && fields[0] != null
                            && fields[0].length() > 0
                            && !line.endsWith(PARAM_FIELD_SEPARATOR)) {
                        String id = fields[0].trim();
                        // Make a giant alternator (|) regex group for each line
                        Pattern p = buildPattern(fields);
                        Pattern oldPattern = patterns.put(id, p);
                        if (oldPattern!=null) logger.info("WARNING: already had a pattern '" + oldPattern + "' for " + id + " which was just replaced by " + p);

                        if (fields.length > 1 && fields[1] != null) {
                            String temp = fields[1].trim();
                            String previous = section_names.put(id, temp);
                            if (previous!=null) logger.info("WARNING: already had a section '" + previous + "' for " + id + " which was just replaced by " + temp);

                        }

                    } else {
                        logger.info("Warning: Skipped reading sections config row: "
                                + Arrays.toString(fields));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ResourceInitializationException(e);
        }
    }

    /**
     * Build a regex pattern from a list of section names. used only during init
     */
    private static Pattern buildPattern(String[] line) {
        StringBuffer sb = new StringBuffer();
        for (int i = 1; i < line.length; i++) {
            // Build the RegEx pattern for each comma delimited header name
            // Suffixed with a aggregator pipe
            sb.append("\\s*" + line[i].trim() + "(\\n|\\s\\s|\\s:|:|\\s-|-)");
            if (i != line.length - 1) {
                sb.append("|");
            }
        }
        int patternFlags = 0;
        //patternFlags |= Pattern.CASE_INSENSITIVE;
        patternFlags |= Pattern.DOTALL;
        patternFlags |= Pattern.MULTILINE;
        Pattern p = Pattern.compile("^(" + sb + ")", patternFlags);
        return p;
    }

    List<Segment> findSectionsHeadersByPattern(JCas jCas, final String text) {
        final ArrayList<Segment> sections = new ArrayList<>();
        for (String patternName : patterns.keySet()) {
            Pattern p = patterns.get(patternName);
            //System.out.println("Pattern " + p);
            Matcher m = p.matcher(text);
            while (m.find()) {
                int begin = m.start();
                int end = m.end();
                Segment segmentHeader = new Segment(jCas);
                boolean isAllWhiteSpace = true;
                if (begin==end) throw new RuntimeException("Empty section header?!?!?!?!?");
                for (int index=begin; index < end; index++) {
                    if (!Character.isWhitespace(text.charAt(index))) {
                        isAllWhiteSpace = false;
                    }
                }

                if (isAllWhiteSpace) begin = end-1;
                char c = text.charAt(begin);
                while ((c=='\r' || c=='\n') && !isAllWhiteSpace) {
                    begin++;
                    if (begin > text.length()) {
                        throw new RuntimeException("Skipping CRs and LFs at the start of a section heading extended us past the end of the document!?!?" +
                                "\nbegin=" + begin + "\nsegmentHeader.getEnd()="+segmentHeader.getEnd());
                    }
                    c = text.charAt(begin);
                }
                segmentHeader.setBegin(begin);

                int last = end-1;
                c = text.charAt(last);  // heading should be at least one character so can look at character at end-1 safely
                while ((c=='\r' || c=='\n') && !isAllWhiteSpace) {
                    end--;
                    last = end-1;
                    c = text.charAt(last);
                    if (end < segmentHeader.getBegin()) {
                        throw new RuntimeException("Skipping CRs and LFs at the end of a section heading took us back before the beginning of the section heading!?!?" +
                                "\nm.start=" + m.start() +
                                "\nm.end=" + m.end() +
                                "\nend=" + end + "\nsegmentHeader.getBegin()="+segmentHeader.getBegin() + "\nsegmentHeader.getEnd()="+segmentHeader.getEnd());
                        //+ " hdr=" + segmentHeader.getCoveredText());
                    }
                }
                segmentHeader.setEnd(end);

                segmentHeader.setTagText(text.substring(segmentHeader.getBegin(), segmentHeader.getEnd()));
                segmentHeader.setId(patternName);
                sections.add(segmentHeader);
            }
        }

        // Need the sections in sorted order to determine the end of the text within the section,
        // which is assumed to be the beginning of the next section header
        Collections.sort(sections, new Comparator<Segment>() {
            public int compare(Segment s1, Segment s2) {
                return s1.getBegin() - (s2.getBegin());
            }
        });

        return sections;
    }

    private void addSingleSectionToCas(JCas cas, List<Segment> sectionHeaders) {

        String text = cas.getDocumentText();
        // If there are no segment (section) headers, create a Segment that spans the entire doc
        if (sectionHeaders.size() <= 0) {
            Segment simpleSegment = new Segment(cas);
            simpleSegment.setBegin(0);
            simpleSegment.setEnd(text.length());
            simpleSegment.setId(SIMPLE_SEGMENT);
            // simpleSegment.setPreferredText(getId());
            // segment.setTagText();          // no tag text for SIMPLE_SEGMENT
            simpleSegment.addToIndexes();
            return;
        }

        if (sectionHeaders.size()==1) {
            Segment segment = sectionHeaders.get(0);
            // segment.setBegin(###); // already set when found
            segment.setEnd(text.length()); // since only 1 section header, this section's text goes to end of document
            // segment.setId(xxxxxxxxxxxxxx); // already set when it was found by pattern
            segment.setPreferredText(segment.getId());
            // segment.setTagText();          // already set when it was found by pattern
            segment.addToIndexes();
            return;

        }
    }

    // throws NPE if JCas is null or its documentText is null
    private void addMultipleSectionsToCas(JCas cas, List<Segment> sectionHeaders) {
        // Can assumes at least two segments because the case of a single segment of any type was already handled.
        for (int index=0; index < sectionHeaders.size(); index++) {
            Segment header = sectionHeaders.get(index);
            //logger.info("Processing segment header: " + header.getId() + " tagText = " + header.getTagText() + " " + header.getBegin() + ", " + header.getEnd());
            int end = cas.getDocumentText().length(); // handle case for last section
            if (index + 1 < sectionHeaders.size()) {
                end = sectionHeaders.get(index + 1).getBegin();
            }
            Segment segment = new Segment(cas);
            segment.setBegin(header.getEnd()); // text of section starts after the section heading
            segment.setEnd(end);
            segment.setId(header.getId());
            // Only create (add to CAS) a segment if there is some text (at least a new line or any other char).
            // Ignore the segment header if it is immediately followed by another header or by EOL
            if (segment.getEnd() > segment.getBegin()) {
                segment.setPreferredText(header.getId());
                segment.setTagText(header.getTagText());
                segment.addToIndexes();
            }
        }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

        final String text = jCas.getDocumentText();
        if (text == null) {
            String docId = DocumentIDAnnotationUtil.getDocumentID(jCas);
            logger.info("text is null for docId=" + docId);
            return;
        }

        List<Segment> sectionHeaders = findSectionsHeadersByPattern(jCas, text);

        if (sectionHeaders.size() < 2) {
            addSingleSectionToCas(jCas, sectionHeaders);
        } else {
            addMultipleSectionsToCas(jCas, sectionHeaders);
        }

    }

}

