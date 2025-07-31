package org.healthnlp.deepphe.nlp.ae.section;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.nlp.section.DefinedSectionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
// TODO  Why the Heck is there a new sectionizer that uses regex?
// The RegexSectionizer in ctakes does the same thing but without bugs!!
// TODO make a copy of the dphe sections.txt using BSV instead of CSV (improper as headers CAN have commas).
// TODO test and get rid of this thing.
// TODO This can drop initial sections if there are others that follow.
final public class Sectionizer extends JCasAnnotator_ImplBase {

    static private final Logger LOGGER = Logger.getLogger( "Sectionizer" );
    protected static HashMap<String, Pattern> patterns = new HashMap<>();
    protected static HashMap<String, String> section_names = new HashMap<>();
    protected static final String DEFAULT_SECTION_FILE_NAME = "org/healthnlp/deepphe/sections/sections.txt";
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
            LOGGER.info( "Reading Section File " + sections_path);
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
                        if (oldPattern!=null) LOGGER.info( "WARNING: already had a pattern '" + oldPattern + "' for " + id + " which was just replaced by " + p);

                        if (fields.length > 1 && fields[1] != null) {
                            String temp = fields[1].trim();
                            String previous = section_names.put(id, temp);
                            if (previous!=null) LOGGER.info( "WARNING: already had a section '" + previous + "' for " + id + " which was just replaced by " + temp);

                        }

                    } else {
                        LOGGER.info( "Warning: Skipped reading sections config row: "
                                     + Arrays.toString(fields));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ResourceInitializationException(e);
        }
    }

    static private final Function<String,String> toRegex
          = t -> t.equals( t.toUpperCase() )
                 ? "^[ \\t]*" + t + "[ \\t\\r\\n:-]+"
                 : "^[ \\t]*" + t + "[ \\t]*(?:(?:[:-]*[ \\t]*\\r?\\n)|(?:[:-]+[ \\t\\r\\n]+))";

    /**
     * Build a regex pattern from a list of section names. used only during init
     */
    private static Pattern buildPattern(String[] line) {
//        StringBuffer sb = new StringBuffer();
//        for (int i = 0; i < line.length; i++) {
//            // Build the RegEx pattern for each comma delimited header name
//            // Suffixed with a aggregator pipe
//            sb.append( "^[ \\t]*" )
//              .append( line[i].trim() )
////              .append( "(\\s?((:|-)?(\\s\\s|\\r?\\n))|(:\\s|-\\s))" );
//              .append( "[ \\t:-]*\\r?\\n" );
//            if (i != line.length - 1) {
//                sb.append( "|" );
//            }
//        }
        final String regex = Arrays.stream( line )
                                   .map( String::trim )
//                                   .map( e -> "^[ \\t]*" + e + "[ \\t:-]*\\r?\\n?" )
                                    .map( toRegex )
                                   .collect( Collectors.joining( "|" ) );
//        int patternFlags = 0;
//        patternFlags |= Pattern.DOTALL;
//        patternFlags |= Pattern.MULTILINE;
//        return Pattern.compile("^(" + sb + ")", patternFlags);
        return Pattern.compile( regex, Pattern.MULTILINE );
    }

    static private List<Segment> findSectionsHeadersByPattern(JCas jCas, final String text) {
        final List<Segment> sections = new ArrayList<>();
        for (String patternName : patterns.keySet()) {
            Pattern p = patterns.get(patternName);
            Matcher m = p.matcher(text);
            while (m.find()) {
                int begin = m.start();
                int preTitleLines = 0;
                if ( begin == 0 ) {
                    preTitleLines = 100;
                } else {
                    for ( int i=begin-1; i>=0; i-- ) {
                        if ( text.charAt( i ) == '\n' ) {
                            preTitleLines++;
                        } else if ( !Character.isWhitespace( text.charAt( i ) ) ) {
                            break;
                        }
                    }
                }
//                if ( preTitleLines < 2 ) {
                if ( preTitleLines < 1 ) {
                    continue;
                }
                int end = m.end();
                Segment segmentHeader = new Segment(jCas);
                boolean isAllWhiteSpace = true;
                if (begin==end) throw new RuntimeException("Empty section header?!?!?!?!?");
                for (int index=begin; index < end; index++) {
                    if (!Character.isWhitespace(text.charAt(index))) {
                        isAllWhiteSpace = false;
                        break;
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
                segmentHeader.setPreferredText( patternName );
                sections.add(segmentHeader);
            }
        }

        // Need the sections in sorted order to determine the end of the text within the section,
        // which is assumed to be the beginning of the next section header
        sections.sort( Comparator.comparingInt( Segment::getBegin ) );

        // Fix for missing untitled first section.
       if ( sections.isEmpty() ) {
          return sections;
       }
       final Segment firstSection = sections.get( 0 );
       if ( firstSection.getBegin() > 2 ) {
          final Segment missing = new Segment( jCas, 0, 0 );
          missing.setTagText( "" );
          missing.setId( DefinedSectionType.HistoryPresentIllness.getName() );
          missing.setPreferredText( DefinedSectionType.HistoryPresentIllness.getName() );
          sections.add( 0, missing );
//          LOGGER.info( "Adding first section as " + SectionType.HistoryPresentIllness.getName() + " " + missing.getBegin() + "," + missing.getEnd() );
       }
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
             simpleSegment.setPreferredText( SIMPLE_SEGMENT );
            simpleSegment.setTagText( "" );          // no tag text for SIMPLE_SEGMENT
            simpleSegment.addToIndexes();
            return;
        }

        if (sectionHeaders.size()==1) {
            Segment header = sectionHeaders.get(0);
            Segment segment = new Segment(cas);
            segment.setBegin( Math.max( 0, header.getEnd() ) ); // text of section starts after the section heading
            segment.setEnd(text.length());
            segment.setId(header.getId());
            segment.setPreferredText( header.getPreferredText() );
            segment.setTagText( header.getTagText() );
//            Segment segment = sectionHeaders.get( 0 );
//            // segment.setBegin(###); // already set when found  // NOT CORRECTLY IT WASN'T !!!
//            segment.setEnd(text.length()); // since only 1 section header, this section's text goes to end of document
//            // segment.setId(xxxxxxxxxxxxxx); // already set when it was found by pattern
////            segment.setPreferredText(segment.getId());
//            // segment.setTagText();          // already set when it was found by pattern
            segment.addToIndexes();
        }
    }

    // throws NPE if JCas is null or its documentText is null
    private void addMultipleSectionsToCas(JCas cas, List<Segment> sectionHeaders) {
        // Can assumes at least two segments because the case of a single segment of any type was already handled.
        for (int index=0; index < sectionHeaders.size(); index++) {
            Segment header = sectionHeaders.get(index);
//            LOGGER.info("Processing segment header: " + header.getId() + " tagText = " + header.getTagText() + " " + header.getBegin() + ", " + header.getEnd());
            int end = cas.getDocumentText().length(); // handle case for last section
            if (index + 1 < sectionHeaders.size()) {
                end = sectionHeaders.get(index + 1).getBegin();
            }
            final int begin = Math.max( 0, header.getEnd() );
            // Only create (add to CAS) a segment if there is some text (at least a new line or any other char).
            if ( end > begin ) {
                Segment segment = new Segment(cas);
                segment.setBegin( begin ); // text of section starts after the section heading
                segment.setEnd(end);
                segment.setId(header.getId());
                segment.setPreferredText( header.getPreferredText() );
                segment.setTagText( header.getTagText() );
                // Ignore the segment header if it is immediately followed by another header or by EOL
                segment.setId( header.getId() );
                segment.setPreferredText( header.getPreferredText() );
                segment.setTagText(header.getTagText());
                segment.addToIndexes();
//                LOGGER.info( "Adding " + segment.getPreferredText() + " " + segment.getId() + " " + segment.getTagText() );
            } else {
                LOGGER.info( "Ignoring " + header.getPreferredText() + " " + header.getId() + " "
                             + header.getTagText() + " " + begin + ", " + end );
            }
        }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        LOGGER.info( "Creating Sections ..." );
        final String text = jCas.getDocumentText();
        if (text == null) {
           String docId = DocIdUtil.getDocumentID( jCas );
            LOGGER.info( "text is null for docId=" + docId);
            return;
        }

        List<Segment> sectionHeaders = findSectionsHeadersByPattern(jCas, text);
        if (sectionHeaders.size() < 2) {
            addSingleSectionToCas(jCas, sectionHeaders);
        } else {
            addMultipleSectionsToCas(jCas, sectionHeaders);
        }
        sectionHeaders.forEach( Segment::removeFromIndexes );
//        JCasUtil.select( jCas, Segment.class ).forEach( LOGGER::error );
    }

}

