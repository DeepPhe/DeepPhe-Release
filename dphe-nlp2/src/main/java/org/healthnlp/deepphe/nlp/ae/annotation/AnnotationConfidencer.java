package org.healthnlp.deepphe.nlp.ae.annotation;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IntSummaryStatistics;

/**
 * @author SPF , chip-nlp
 * @since {1/12/2024}
 */
@PipeBitInfo(
        name = "AnnotationConfidencer",
        description = "Does something.",
        role = PipeBitInfo.Role.ANNOTATOR
)
public class AnnotationConfidencer extends JCasAnnotator_ImplBase {

    static private final Logger LOGGER = Logger.getLogger("AnnotationConfidencer");

    // See RareWordUtil for examples of these parts of speech.  Maybe less with JJ, JJR, JJS ?
    static private final Collection<String> BAD_POS = new HashSet<>( Arrays.asList(
            "CC", "DT", "EX", "IN", "MD", "PDT", "PP$", "PPZ", "PRP", "PRP$", "TO", "WDT", "WP", "WP$", "WRB"
    ) );

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final JCas jcas) throws AnalysisEngineProcessException {
        LOGGER.info( "Setting annotation confidence scores ..." );
        JCasUtil.indexCovered( jcas, EventMention.class, BaseToken.class )
                .forEach( AnnotationConfidencer::setConfidence );
    }

    static private void setConfidence( final EventMention mention, final Collection<BaseToken> tokens ) {
        final long posDeficit = getPosDeficit( tokens );
        final long lengthDeficit = getTokenLengthDeficit( tokens );
//        final long caseDeficit = getCaseDeficit( tokens );
        final float score = Math.max( 1, 100 - posDeficit - lengthDeficit ) / 100f;
        mention.setConfidence( score );
    }

    /**
     * @param tokens -
     * @return 20 if the term is only one word and is of a part of speech that shouldn't represent a term.
     */
    static private long getPosDeficit( final Collection<BaseToken> tokens ) {
        if ( tokens.size() > 1 ) {
            return 0;
        }
        return tokens.stream()
                .map( BaseToken::getPartOfSpeech )
                .anyMatch( BAD_POS::contains ) ? 20 : 0;
    }

    /**
     *
     * @param tokens -
     * @return deficit based upon: total length of the term (non-whitespace), length of max token, number of tokens.
     */
    static private long getTokenLengthDeficit( final Collection<BaseToken> tokens ) {
        if ( tokens.size() >= 3 ) {
            return 0;
        }
        final IntSummaryStatistics stats = tokens.stream()
                .map( BaseToken::getCoveredText )
                .mapToInt( String::length )
                .summaryStatistics();
        // term length deficit: 3 = 15, 4 = 10, 5 = 5   = 5 * (6-length)
        final long termLengthDeficit = Math.max( 0, 5*(7-stats.getSum()) );
        // token length deficit: 3 = 10, 4 = 5   = 5 * (5-length)
        final long tokenLengthDeficit = Math.max( 0, 5*(5-stats.getMax()) );
        // count deficit: 1 = 2, 2 = 1   = 3-count
        final long tokenCountDeficit = Math.max( 0, 3-tokens.size() );
        return (termLengthDeficit + tokenLengthDeficit) * tokenCountDeficit;
    }

    /**
     * dPhe has a method that compares the covered text to the actual dictionary entry text.  This AE can't do that.
     * @param tokens -
     * @return -
     */
    static private long getCaseDeficit( final Collection<BaseToken> tokens ) {
        if ( tokens.size() > 1 ) {
            return 0;
        }
        return tokens.stream()
                .map( BaseToken::getCoveredText )
                .map( AnnotationConfidencer::getCaseDeficit )
                .findFirst()
                .orElse(0L );
    }


    /**
     * dPhe has a method that compares the covered text to the actual dictionary entry text.  This AE can't do that.
     * @param token -
     * @return -
     */
    static private long getCaseDeficit( final String token ) {
        if ( token.length() > 7 ) {
            return 0;
        }
        // If a token length > 3 start with 2nd char, force match the first char in case of capitalization.
        int start = token.length() > 3 ? 1 : 0;
        // The shorter the token length the greater the penalty.
        long penalty = 7 - Math.min( 6, token.length() );
        final long lowerCount = Math.max( 0, token.chars().filter( Character::isLowerCase ).count() - start );
        return penalty * lowerCount;
    }


}
