package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.ae.section.SectionHelper;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * Sets segmentID attribute of all IdentifiedAnnotation that are in a section other than SIMPLE_SEGMENT
 * Makes it possible to determine if a singleton mention is in a final diagnosis section
 * where singletons often are more important than in other sections
 *
 * @author JJM , chip-nlp
 * @version %I%
 * @since 9/11/2018
 */
final public class SetSegmentID extends JCasAnnotator_ImplBase {


    private static final String CLASS_NAME = MethodHandles.lookup().lookupClass().getSimpleName();

    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public void processCas(final JCas cas) {

        LOGGER.info("Setting segmentID attribute for each IdentifiedAnnotation that's not in a SIMPLE_SEGMENT ....");

        final Collection<Segment> sections = JCasUtil.select(cas, Segment.class);
        for (Segment section : sections) {
            // Don't spend the time to set the segment ID for every IdentifiedAnnotation
            if (!SectionHelper.isSimpleSegment(section)) {
                Collection<IdentifiedAnnotation> annotations = JCasUtil.selectCovered(cas, IdentifiedAnnotation.class, section);
                for (IdentifiedAnnotation ia : annotations) {
                    // Need the standardized section name, not just the segment ID
                    String standardizedSectionName = SectionHelper.getStandardizedSectionName(section);
                    if (standardizedSectionName == null || standardizedSectionName.trim().length() == 0) {
                        LOGGER.warn("Standardized section name not set. This can happen for a SIMPLE_SEGMENT.");
                        ia.setSegmentID(section.getId());
                    } else {
                        ia.setSegmentID(standardizedSectionName);
                    }
                }
            }
        }

        LOGGER.info("Finished processing.");

    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        processCas(jCas);
    }
}

